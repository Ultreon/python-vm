from _jasm import jvmnative, jvmasm
from java.lang import System
from java.util import String, Scanner
from java.io import File, RandomAccessFile


class object:
    def __init__(self):
        self.__dict__: dict

    def __setattr__(self, key, value):
        self.__dict__[key] = value

    def __getattr__(self, key):
        return self.__dict__[key]

    def __repr__(self):
        return self.__class__.__name__


class range:
    def __init__(self, start, stop, step=1):
        self.start = start
        self.stop = stop
        self.step = step

    @jvmnative
    def __iter__(self):
        pass

    @jvmnative
    def __next__(self):
        pass

    def __len__(self):
        return (self.stop - self.start) // self.step

    def __getitem__(self, index):
        return self.start + index * self.step


class dict:
    @jvmnative
    def __init__(self):
        pass

    @jvmnative
    def __setitem__(self, key, value):
        pass

    @jvmnative
    def __getitem__(self, key):
        pass

    @jvmnative
    def __len__(self):
        pass

    @jvmnative
    def __iter__(self):
        pass

    @jvmnative
    def __next__(self):
        pass

    @jvmnative
    def keys(self):
        pass


def eval(expr):
    raise NotImplementedError()


def exec(expr):
    raise NotImplementedError()


def print(*args, sep=' ', end='\n', file=None):
    if file is not None:
        raise NotImplementedError("Not implemented!")
    System.out.print(String.join(sep, args) + end)


__SCANNER = Scanner(getattr(System, "in"))


def input(prompt=None) -> str:
    if prompt is not None:
        System.out.print(prompt)
    return __SCANNER.nextLine()


def exit(code=0):
    System.exit(code)


def open(file, mode='r'):
    accessfile = RandomAccessFile(File(file), mode)


