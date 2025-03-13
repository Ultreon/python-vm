from java.awt import Button, Frame
from java.lang import Runnable, System, Long, Exception
from java.util import Objects

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

    if True:
        print("3 + 5 == 8")
    else:
        print("3 + 5 != 8")
