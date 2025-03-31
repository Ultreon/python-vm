from java.awt import Button, Frame
from java.awt.event import ActionListener
from java.lang import Runnable, System, Long, Exception, Long
from java.util import Objects
from java.util import Random

STRING: str = "Hello, world!"

HASHCODE: int = Objects.hashCode("Hello, world!")
INTEGER: int = 42
FLOAT: float = 3.14


class Testing(ActionListener):
    def __init__(self):
        self.frame = Frame()
        self.button = Button("Click me!")

        self.frame.add(self.button)

        self.button.setLabel("Click me!")
        self.button.addActionListener(self)

        self.frame.setVisible(True)

    def actionPerformed(self, event):
        print("Button clicked!")


class TestClass(Runnable):
    def __init__(self, a, b):
        self.a = a
        self.b = b
        self.c = None

    def run(self):
        print("Hello, world!")
        print("LMAO")

        print(Objects.hashCode("Hello, world!"))

        # print(HASHCODE)
        # print(INTEGER)
        # print(FLOAT)
        # print(STRING)



# TEST_CLASS = TestClass(3, 6)


def init():
    # print(Objects.hashCode("Hello, world!"))
    #
    # # print(HASHCODE)
    # # print(INTEGER)
    # # print(FLOAT)
    # # print(STRING)
    #
    # number = 3.141592653589793
    #
    # # testing = Testing()
    # # frame = testing.frame
    # # frame.hello = "Hello Señor"
    #
    # hello = "Hello World"
    #
    # # print("Frame says:", frame.hello)
    # print("Me says:", hello)
    #
    # # test_class = TestClass(1, 2)
    # # test_class.run()
    #
    # # print(test_class.a)
    # # print(test_class.b)
    #
    # # frame = Frame()
    # # frame.setVisible(True)
    # #
    # # button = Button("Click me!")
    # # button.setLabel("Click me!")
    # # frame.add(button)
    # #
    # # listeners = button.getActionListeners()
    # # print(listeners)
    #
    # print("Hey there!", number)
    #
    # # if number != 3.14:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number == 3.141592653589793:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number > 3.14:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number < 4.0:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number >= 3.14:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number >= 3.141592653589793:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number <= 4.0:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    # #
    # # if number <= 3.141592653589793:
    # #     print("Lucky!")
    # # else:
    # #     print("Unlucky!")
    #
    # value = 10 * 3.14
    # print(value)
    #
    # value = 10 + 3.14
    # print(value)
    #
    # value = 10 - number
    # print(value)
    #
    # value = 10 / number
    # print(value)
    #
    # value = 104
    # print(value)
    #
    # # Floor division
    # value = 10 // 3
    # print(value)
    #
    # # Modulo
    # value = 10 % 3
    # print(value)
    #
    # # Exponentiation
    # value = 10 ** 3
    # print(value)
    #
    # # Bitwise AND
    # value = 10 & 3
    # print(value)
    #
    # # Bitwise OR
    # value = 10 | 3
    # print(value)
    #
    # # Bitwise XOR
    # value = 10 ^ 3
    # print(value)
    #
    # # Bitwise NOT
    # value = ~10
    # print(value)
    #
    # # Bitwise left shift
    # value = 10 << 3
    # print(value)
    #
    # # Bitwise right shift
    # value = 10 >> 3
    # print(value)
    #
    # another_value = None
    # print(another_value)
    #
    # name = "world"
    # print(f"Hello, {name}!")
    #
    # i = 0
    # while i < 10:
    #     print(i)
    #     print("Hello, world!")
    #     i = i + 1
    # else:
    #     print("Done!")
    #
    # i = 0
    # while i < 10:
    #     print(i)
    #     print("Hello, world!")
    #     i = i + 1
    #     if i == 5:
    #         print("Break!")
    #         break
    # else:
    #     print("Done!")
    #
    # for i in range(10):
    #     print(i)
    #     print("Hello, world!")
    # else:
    #     print("Done!")

    for i in range(10):
        print(i)
        print("Hello, world!")
        if i == 5:
            print("Break!")
            break
    else:
        print("Done!")

