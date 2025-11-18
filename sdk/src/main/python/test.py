with open("test.txt", "r") as f:
    print(f.read())

for i in range(10):
    print(i)
else:
    print("done")

for i in range(10):
    print(i)
    if i == 5:
        break

    if i == 7:
        continue

i = 0
while i < 10:
    print(i)
    i += 1

a = b = c = 3
a, b = b, a
print(a, b)

def f():
    print("f")

async def g():
    print("f")

def gen_func():
    yield 1
    yield 2
    yield 3

match 1:
    case 1:
        print("1")
    case 2:
        print("2")
    case _:
        print("default")

print(f"hello {f.__name__}")
print(f"hello {g.__name__}")
print(f"hello {gen_func.__name__}")

try:
    raise Exception("hello")
except Exception as e:
    print(e)

from _pyio import open_code

print(open_code)

import _pyio

import math as _math

from _pyio import _open_code_with_warning as lol
from _pyio import (a, b, c)
from _pyio import (a as b, c as d)

a = input(3)

class A:
    def __init__(self):
        self.a = 1

    def __repr__(self):
        return "A"

print(A())

class B(A):
    def __repr__(self):
        return "B"

print(B())

import abc

class C(metaclass=abc.ABCMeta):
    @abc.abstractmethod
    def f(self):
        pass

c = C()
c.f()

class D(C):
    def f(self):
        print("f")

d = D()
d.f()

class E(D):
    def f(self):
        print("f")

e = E()
e.f()

a = lambda x: x + 1
c = lambda x, y: x + y

def keyword_only_func(a, *, b):
    return a + b

def keyword_only_func_with_defaults(a, *, b=1):
    return a + b

def keyword_only_func_with_defaults_and_varargs(a, *args, b=1):
    return a + b

def keyword_only_func_with_defaults_and_varargs_and_kwargs(a, *args, b=1, **kwargs):
    return a + b

def keyword_only_func_with_varargs_and_kwargs(a, *args, **kwargs):
    return a + len(args) + len(kwargs)

def keyword_only_func_with_varargs(a, *args):
    return a + len(args)

def keyword_only_func_with_kwargs(a, **kwargs):
    return a + len(kwargs)

def keyword_only_func_with_no_args():
    return 0

def keyword_only_func_with_all_args(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z):
    return a + b + c + d + e + f + g + h + i + j + k + l + m + n + o + p + q + r + s + t + u + v + w + x + y + z


a += 1
a *= 1
a /= 1
a -= 1
a %= 1
a <<= 1
a >>= 1
a // 1
a ** 1
a &= 1
a |= 1
a ^= 1
a = a + 1 * 1 / 1 - 1 % 1 << 1 >> 1 // 1 ** 1 & 1 | 1 ^ 1

if a:
    print("a")
else:
    print("b")


if a == 1:
    print("a")
elif a == 2:
    print("b")
else:
    pass

print("c")

x: int = 3

try:
    raise Exception("hello")
except Exception as e:
    print(e)
else:
    print("else")
finally:
    print("finally")
import os
import sys
from pathlib import Path

if __name__ == '__main__':
    PYVM_COMPILER_PATH = Path(__file__).parent.absolute() / "pyvm-compiler.jar"

    def compile(path: str):
        for file in os.listdir(os.path.join(sys.argv[1], path)):
            if file.endswith(".py"):
                print(f'Compiling {file}')
                if os.system(f'{sys.executable} "{os.environ["PYVM_PARSER"]}" -o "{sys.argv[2]}" -p "{path.replace("/", ".")}" "{path}/{file}"') != 0:
                    # exit(1)
                    continue
                a = f'java -cp {str(PYVM_COMPILER_PATH).replace("\\", "/")}{os.pathsep}{os.environ.get("PYVM_CLASSPATH").replace("\\", "/")} dev.ultreon.pyvm.compiler.AstCompiler {sys.argv[2].replace("\\", "/")}/{sys.argv[3].replace(".", "/")}/{file[:file.rfind(".")]}.ast.json "{sys.argv[3].replace("\\", "/").replace("/", ".")}.{file[:file.rfind(".")]}" {sys.argv[2].replace("\\", "/")}/classes/'

                import shlex
                for c in shlex.split(a):
                    print(c)
                if os.system(a) != 0:
                    # exit(1)
                    continue
            elif os.path.isdir(f'{path}/{file}'):
                print(f'Entering {file}')
                compile(f'{path}/{file}')
                print(f'Exiting {file}')


    for file in os.listdir(sys.argv[1]):
        if os.path.isdir(f'{sys.argv[1]}/{file}'):
            print(f'Entering {file}')
            compile(f'{sys.argv[1]}/{file}')
            print(f'Exiting {file}')
        elif file.endswith(".py"):
            print(f'Compiling {file}')
            if os.system(f'{sys.executable} "{os.environ["PYVM_PARSER"]}" -o "{sys.argv[2]}" -p "{sys.argv[3]}" "{file}"') != 0:
                # exit(1)
                continue
            a = f'java -cp {str(PYVM_COMPILER_PATH).replace("\\", "/")}{os.pathsep}{os.environ.get("PYVM_CLASSPATH").replace("\\", "/")} dev.ultreon.pyvm.compiler.AstCompiler {sys.argv[2].replace("\\", "/")}/{sys.argv[3].replace(".", "/")}/{file[:file.rfind(".")]}.ast.json "{sys.argv[3].replace("\\", "/").replace("/", ".")}.{file[:file.rfind(".")]}" {sys.argv[2].replace("\\", "/")}/classes/'

            # import shlex
            # for c in shlex.split(a):
            #     print(c)
            if os.system(a) != 0:
                # exit(1)
                continue
