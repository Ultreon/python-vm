class TestButton:
    def __init__(self, a: int, b: int):
        print("Hello, world!")

        self.a = a
        # self.b = b

    def sum(self) -> int:
        return self.a + self.b


def init():
    a = TestButton(3, 5).sum()
