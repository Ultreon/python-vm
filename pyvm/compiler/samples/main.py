#! /usr/bin/env python3
"""
Hello World!
"""


print("Hello World!")


def main() -> float:
    print("Hello World!")

    return ((48 + 58 * 45 / 65) ** 2) % 5


def foo() -> int:
    return 1234567890


def bar() -> str:
    var = "Hello "
    var += "World!"
    return var


def baz() -> bool:
    return True and not False


def qux() -> None:
    pass


def test() -> int:
    a = 1
    a += 54
    a *= 2
    a /= 3
    a %= 4
    a **= 5
    a <<= 6
    a >>= 7
    a &= 8
    a ^= 9
    a |= 10
    return a


match 1:
    case 1:
        print("Hello World!")
    case _:
        print("Hello World!")

i = 0
while True:
    print("Hello World!")
    i += 1
    if i > 10:
        break
else:
    print("Hello World!")

for i in range(10):
    print("Hello World!")
else:
    print("Hello World!")

a, b = 1, 2
a, b = b, a

c: int = 1
d, e = "Hello ", "World!"

g = [1, 2, 3, "Hello", "World", True, False, None, 1.23456789, (1, 2, 3), {1: 2, 3: 4}, {1, 2, 3}]

h = {"a": 1, "b": 2, "c": 3}

k = {3, 2, 1}

l = [i for i in range(10)]

m = {i: i for i in range(10)}

n = {i for i in range(10)}

o = {i: i for i in range(10) if i % 2 == 0}

p = {i: i for i in range(10) if i % 2 == 0 and i % 3 == 0}

q = {i: i for i in range(10) if i % 2 == 0 or i % 3 == 0}

try:
    raise Exception("Hello World!")
except Exception as e:
    print(e)
else:
    print("Hello World!")
finally:
    print("Hello World!")

c = a + b
d = a - b
e = a * b
f = a / b
g = a % b
h = a ** b
i = a << b
j = a >> b
k = a & b
l = a ^ b
m = a | b

a = b = c = d = e = f = g = h = i = j = k = l = m = 1

try:
    from math import sqrt

    a = sqrt(b)
except ImportError:
    a = b ** 0.5

try:
    import math as m

    a = m.sqrt(b)
except ImportError:
    a = b ** 0.5

try:
    from math import *

    a = sqrt(b)
except ImportError:
    a = b ** 0.5

try:
    from math import sqrt as s

    a = s(b)
except ImportError:
    a = b ** 0.5

print(a, b, sep=" ")


def f():
    return 1


class C:
    def __init__(self):
        self.x = 1

    def f(self):
        return self.x

    def g(self):
        return f()


c = C()
print(c.f(), c.g())

lamb = lambda: 1


async def foo() -> int:
    return 1


async def bar() -> int:
    return foo()


def generator_test():
    yield 1
    yield 2
    yield 3


def fuzz(foo, bar: int, baz: str = "Hello World!", /):
    print(foo, bar, baz)


def func1(a: int, b, /, c: float, d: bool = True, r=3, *args, s, t: int, e: str = "Hello World!", **kwargs):
    print(a, b, c, d)


with open("foo.txt", "r") as f:
    print(f.read())

with open("foo.txt", "w"):
    pass

assert True

assert 1 == 1

assert True, "Hello World!"

assert 1 == 1, "Hello World!"


def glob():
    global a
    a = 1


def nonloc():
    nonlocal a


def del_():
    del a


def err():
    raise Exception("Hello World!")


if __name__ == "__main__":
    main()


# Comments
