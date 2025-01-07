from java.awt import Button
from java.awt import Frame
from java.lang import Runnable, String

STRING: str = "Hello, world!"


# noinspection PyMethodMayBeStatic
class MyRunnable(Runnable):
    def run(self):
        print("Hello, world from a runnable!")


def init():
    number: int = 42

    frame: Frame = Frame()
    frame.setVisible(True)

    button: Button = Button()
    button.setLabel("Hello, world!")
    # frame.add(button)  # TODO

    MyRunnable().run()

    String.valueOf(Button().getLabel())

    print("Hey there!", STRING)
