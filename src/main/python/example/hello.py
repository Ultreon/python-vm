from java.awt import Button, Frame
from java.lang import Runnable, System, Long, Exception
from java.util import Objects
from java.util import Random

STRING = "Hello, world!"

HASHCODE = Objects.hashCode("Hello, world!")
INTEGER = 42
FLOAT = 3.14


def init():
    print(Objects.hashCode("Hello, world!"))

    print(HASHCODE)
    print(INTEGER)
    print(FLOAT)
    print(STRING)

    number = 3.141592653589793

    frame = Frame()
    frame.setVisible(True)

    button = Button("Click me!")
    button.setLabel("Click me!")
    frame.add(button)

    print("Hey there!", number)

    Exception("Hello, world!").printStackTrace()

    if number != 3.14:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number == 3.141592653589793:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number > 3.14:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number < 4.0:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number >= 3.14:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number >= 3.141592653589793:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number <= 4.0:
        print("Lucky!")
    else:
        print("Unlucky!")

    if number <= 3.141592653589793:
        print("Lucky!")
    else:
        print("Unlucky!")

    value = 10 * 3.14
    print(value)

    value = 10 + 3.14
    print(value)

    value = 10 - number
    print(value)

    value = 10 / number
    print(value)

    value = 104
    print(value)

    # Floor division
    value = 10 // 3
    print(value)

    # Modulo
    value = 10 % 3
    print(value)

    # Exponentiation
    value = 10 ** 3
    print(value)

    # Bitwise AND
    value = 10 & 3
    print(value)

    # Bitwise OR
    value = 10 | 3
    print(value)

    # Bitwise XOR
    value = 10 ^ 3
    print(value)

    # Bitwise NOT
    value = ~10
    print(value)

    # Bitwise left shift
    value = 10 << 3
    print(value)

    # Bitwise right shift
    value = 10 >> 3
    print(value)

    i = 0
    # while i < 10:
    #     print(i)
    #     print("Hello, world!")


