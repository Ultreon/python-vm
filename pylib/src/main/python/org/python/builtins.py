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

    lmao: str = Objects.toString(joiner.toString() + end)
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
    inp = getattr(System, "in")  # TODO Add support for class instance passing
    if prompt is not None:
        print(prompt)

    bos: ByteArrayOutputStream = ByteArrayOutputStream()
    inp.read(bos)
    return String(bos.toByteArray())  # ? NOTE: This returns a 'str' instead of a 'String' due to PythonVM conversion


def type(obj):
    return ClassUtils.getType(obj)


def isinstance(obj, cls):
    return ClassUtils.isInstance(obj, cls)


def issubclass(cls, base):
    return ClassUtils.isSubclass(cls, base)


def repr(obj):
    return ClassUtils.getRepr(obj)


def hash(obj):
    return ClassUtils.getHash(obj)


def exec(code, globals=None, locals=None):
    ClassUtils.exec(code, globals, locals)


def eval(expr, globals=None, locals=None):
    return ClassUtils.eval(expr, globals, locals)


def open(file, mode="r", buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None):
    return ClassUtils.open(file, mode, buffering, encoding, errors, newline, closefd, opener)


def sum(iterable, start=0):
    for i in iterable:
        start = start + i
    return start


def all(iterable):
    for i in iterable:
        if i.__not__():
            return False
    return True


def any(iterable):
    for i in iterable:
        if i:
            return True
    return False


def zip(*iterables):
    return ClassUtils.zip(iterables)


def enumerate(iterable, start=0):
    return ClassUtils.enumerate(iterable)


def filter(function, iterable):
    return ClassUtils.filter(function, iterable)


def map(function, iterable):
    return ClassUtils.map(function, iterable)


def sorted(iterable, key=None, reverse=False):
    return ClassUtils.sorted(iterable)


def reversed(sequence):
    return ClassUtils.reversed(sequence)


def slice(sequence, start=None, stop=None, step=None):
    return ClassUtils.slice(sequence)


def next(iterable, default=None):
    return ClassUtils.next(iterable)
