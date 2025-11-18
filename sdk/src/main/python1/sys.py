from java.lang import System as __System


version = "3.12.0"
version_info = (3, 12, 0, "final", 0)
if __System.getProperty("os.name").lower().startswith("windows"):
    executable = "\\\\?\\NonExistent\\python.exe"
elif __System.getProperty("os.name").lower().startswith("darwin"):
    executable = "/usr/bin/python3"
else:
    executable = "/usr/bin/python3"

prefix = ""
exec_prefix = ""
base_prefix = ""
base_exec_prefix = ""

def __exit__(code=None):
    __System.exit(code)

def exit(code=None):
    __exit__(code)
