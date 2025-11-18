from abc import ABC, abstractmethod
from typing import List

import voc.java as java

from pythonvm.compiler import Type, nodes
from .type import *



class Context(ABC):
    def __init__(self):
        self.instructions: List[java.Opcode] = []

    @abstractmethod
    def add_var(self, name: str, value: 'nodes.ExprNode'):
        pass

    @property
    @abstractmethod
    def method(self):
        pass

    @property
    @abstractmethod
    def klass(self):
        pass

    def add_instruction(self, op: java.Opcode):
        self.instructions.append(op)

    @abstractmethod
    def __getitem__(self, item: str) -> List[java.Opcode]:
        pass


class FileContext(Context):
    def __init__(self, package: str, filename: str):
        super().__init__()
        self.__klass = java.Class(name=f"{package}/{filename[:filename.rindex('.')]}", extends="java/lang/Object",
                                  implements=["python/_core/VMObject"])

        self.__clinits = java.Method(name="<clinit>", descriptor="()V", public=True, static=True)
        self.__init = java.Method(name="<init>", descriptor="()V", public=True)
        self.__klass.methods.append(self.__init)

    def add_var(self, name: str, type_: Type = Type("Ljava/lang/Object;")):
        self.__klass.fields.append(java.Field(name=name, descriptor=type_.internal_name))

    @property
    def method(self) -> java.Method:
        return self.__init

    @property
    def klass(self) -> java.Class:
        return self.__klass

    def __getitem__(self, item) -> List[java.Opcode]:
        return [
            java.ALOAD_0(),
            java.LDC(item),
            java.INVOKESTATIC("python/_core/Py", "getAttr", "(Ljava/lang/Object;Ljava/lang/String;)Lpython/_core/VMObject;")
        ]


class ClassContext(Context):
    def __init__(self, klass: java.Class):
        super().__init__()
        self.__klass = klass
        self.__init = java.Method(name="!init", descriptor="()V", public=True)

    def add_var(self, name: str, value: 'nodes.ExprNode'):
        self.__klass.fields.append(java.Field(name=name, descriptor=value.resolved_type.internal_name, static=True))

    @property
    def method(self) -> java.Method:
        return self.__init

    @property
    def klass(self) -> java.Class:
        return self.__klass

    def __getitem__(self, item) -> List[java.Opcode]:
        return [
            java.ALOAD_0(),
            java.LDC(item),
            java.INVOKESTATIC("python/_core/Py", "getAttr", "(Ljava/lang/Object;Ljava/lang/String;)Lpython/_core/VMObject;")
        ]


def parse_type(descriptor: str, from_index: int = 0) -> Type | tuple[Type, int]:
    i = from_index
    sort = descriptor[i]
    i += 1
    if sort == "L":
        name = "L"
        while descriptor[i] != ";":
            name += descriptor[i]
            i += 1
        return Type(name + ";"), i
    elif sort == "[":
        return parse_type(descriptor, i)
    else:
        return Type(sort), i


class MethodDescriptor:
    def __init__(self, descriptor: str):
        self.descriptor = descriptor

        i = 0
        if descriptor[i] == "(":
            i += 1
        else:
            raise ValueError("Method descriptor must start with '('")
        self.args = []
        while i < len(descriptor):
            type_, i = parse_type(descriptor, i)
            self.args.append(type_)
            if descriptor[i] == ")":
                break
        else:
            raise ValueError("Method descriptor arguments must end with ')'")

        if i == len(descriptor):
            raise ValueError("Method descriptor must have a return type")

        self.returns, i = parse_type(descriptor, i)
        if i != len(descriptor):
            raise ValueError("Expected end of descriptor")


def argument_count(descriptor: str) -> int:
    count = 0
    i = 0
    while i < len(descriptor):
        if descriptor[i] == ")":
            break

        type, i = parse_type(descriptor, i)
        if type.is_array:
            count += 1
        else:
            count += 1


class MethodContext(Context):
    def __init__(self, method: java.Method, klass: java.Class):
        super().__init__()
        self.__method = method
        self.__klass = klass
        self.__localsVar = len(MethodDescriptor(method.descriptor).args)

    def add_var(self, name: str, value: 'nodes.ExprNode'):
        self.instructions.append(java.ALOAD(self.__localsVar))
        self.instructions.append(java.LDC(name))
        value.compile(self.__method, self)
        self.instructions.append(java.CHECKCAST("python/_core/VMObject"))
        self.instructions.append(java.INVOKEVIRTUAL("python/_core/PyLocals", "set", "(Ljava/lang/String;Lpython/_core/VMObject;)V"))

    @property
    def method(self) -> java.Method:
        return self.__method

    @property
    def klass(self) -> java.Class:
        return self.__klass

    def __getitem__(self, item: str) -> List[java.Opcode]:
        return [
            java.ALOAD(self.__localsVar),
            java.LDC(item),
            java.INVOKEVIRTUAL("python/_core/PyLocals", "get", "(Ljava/lang/String;)Lpython/_core/VMObject;"),
        ]
