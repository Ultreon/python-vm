class TestButton:
    """
    Hello Sir!
    """

    def __init__(self, a: int, b: int):
        print("Hello, world!")

        self.a = a
        self.b = b

    def sum(self) -> int:
        return self.a + self.b


class HelloWorld:
    def __init__(self, a: int, b: int):
        print("a = ", a, " b = ", b, " sum = ", a + b, "")


class TestButton2(HelloWorld):
    def __init__(self, a: int, b: int):
        while True:
            super().__init__(a, b)


def init():
    a = TestButton(3, 5).sum()
