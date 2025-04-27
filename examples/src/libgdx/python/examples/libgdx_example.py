from com.badlogic.gdx.graphics.g2d import SpriteBatch
from com.badlogic.gdx.graphics import Color, Texture
from com.badlogic.gdx.utils import ScreenUtils
from com.badlogic.gdx import ApplicationAdapter


class DesktopLauncher(ApplicationAdapter):
    def __init__(self):
        self.batch = None
        self.texture = None
        self.color = None

    def create(self):
        self.batch = SpriteBatch()
        self.texture = Texture("badlogic.jpg")
        self.color = Color(1, 1, 1, 1)

    def render(self):
        ScreenUtils.clear(self.color)
        self.batch.begin()
        self.batch.draw(self.texture, 0, 0)
        self.batch.end()

    def dispose(self):
        self.batch.dispose()
        self.texture.dispose()

    def resize(self, width: int, height: int):
        self.batch.setProjectionMatrix(self.batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height))


def __main__():
    from com.badlogic.gdx.backends.lwjgl3 import Lwjgl3Application, Lwjgl3ApplicationConfiguration

    config = Lwjgl3ApplicationConfiguration()
    config.setTitle("LibGDX Example")
    config.useVsync(True)
    Lwjgl3Application(DesktopLauncher(), config)