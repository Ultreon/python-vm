from java.lang import System as __System
from java.lang.management import ManagementFactory as __ManagementFactory


class __PythonVM:
    def __init__(self):
        from org.python._internal import PythonVMUtils
        self.version = PythonVMUtils.getVersion()


argv = __ManagementFactory.getRuntimeMXBean().inputArguments
path = []  # Unable to manipulate the path directly due to compiler limitations
__pythonvm__ = __PythonVM()


def exit():
    __System.exit(0)


def exit(code=0):
    __System.exit(code)


# def __init():
#     start = True
#     for arg in range(len(argv)):
#         if argv[arg].startsWith('-'):
#             argv.remove(arg)
#             continue
#
#         if start:
#             start = False
#             argv.remove(arg)  # Remove the main class
#             continue
#
#         break
#
#
# __init()
