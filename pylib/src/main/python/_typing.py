from java.io import RandomAccessFile


class TextIOFileImpl:
    def __init__(self, accessfile: RandomAccessFile):
        self.__accessfile = accessfile
        self.encoding = "utf-8"
        self.errors = "strict"
        self.line_buffering = False

    def __enter__(self):
        return self

    @property
    def closed(self):
        return self.__accessfile is None

    def close(self):
        if self.__accessfile is not None:
            self.__accessfile.close()
            self.__accessfile = None

    def flush(self):
        if self.__accessfile is not None:
            self.__accessfile.getFD().sync()

    def isatty(self):
        return False

    def read(self, n: int = -1):
        if self.__accessfile is not None:
            return self.__accessfile.read(n)

    def __exit__(self, exc_type, exc_value, traceback):
        if self.__accessfile is not None:
            self.__accessfile.close()

    def __str__(self):
        return "<internal TextIO object>"

    def __repr__(self):
        return "<internal TextIO object>"

    def readline(self, limit: int = -1):
        if self.__accessfile is not None:
            return self.__accessfile.readLine()  # TODO : limit

        return

    def readlines(self, hint: int = -1):
        if self.__accessfile is not None:
            for line in range(hint):
                yield self.readline()

        return

    def seek(self, offset: int, whence: int = 0):
        if self.__accessfile is not None:
            self.__accessfile.seek(offset)  # TODO : whence

        return

    def tell(self):
        if self.__accessfile is not None:
            return self.__accessfile.getFilePointer()

        return

    def truncate(self, size: int = None):
        if self.__accessfile is not None:
            self.__accessfile.setLength(size)

        return

    def write(self, s):
        if self.__accessfile is not None:
            self.__accessfile.write(s.encode("utf-8"))

        return
