from java.util import StringJoiner, Objects
from java.io import ByteArrayOutputStream
from java.lang import String, System
from org.python._internal import ClassUtils, PythonException

# __all__ = ["print", "input"]  # TODO: Add list support to PythonVM


def len(obj) -> int:
    return ClassUtils.getLength(obj)


def print(*args, sep: str = ' ', end: str = '\n', file=None, flush=False):
    joiner = StringJoiner(sep)
    # i = 0
    # len_ = len(args)
    # while i < len_:
    #     joiner.add(args[i].toString())
    #     i = i + 1

    # TODO: Add support for file
    if file is not None:
        file.write(joiner.toString() + end)
        if flush:
            file.flush()

    # lmao: str = Objects.toString(joiner.toString() + end)
    lmao: str = "meow"
    System.out.print("meow")
    if flush:
        System.out.flush()

def getattr(obj, name: str) -> object:
    return ClassUtils.getAttribute(obj, name)


def setattr(obj, name: str, value) -> None:
    ClassUtils.setAttribute(obj, name, value)


def delattr(obj, name: str) -> None:
    ClassUtils.delAttribute(obj, name)


def hasattr(obj, name: str) -> bool:
    return ClassUtils.hasAttribute(obj, name)


def input(prompt=None):
    # inp = getattr(System, "in")  # TODO Add support for class instance passing
    # if prompt is not None:
    #     print(prompt)
    #
    # bos: ByteArrayOutputStream = ByteArrayOutputStream()
    # inp.read(bos)
    # return String(bos.toByteArray())  # ? NOTE: This returns a 'str' instead of a 'String' due to PythonVM conversion
    return "<input>"


def type(obj):
    return ClassUtils.getType(obj)
