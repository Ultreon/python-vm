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

