from java.io import File as __File
from java.lang import System as __System, Runtime as __Runtime
from java.nio.file import Paths as __Paths, Path as __Path, Files as __Files
from java.nio.file.attribute import PosixFilePermission as __PosixFilePermission


class __FSHandler:
    def __init__(self):
        self.cwd = __System.getProperty("user.dir")


__FS_HANDLER = __FSHandler()


def getcwd():
    return __FS_HANDLER.cwd


def chdir(path):
    __FS_HANDLER.cwd = path


def mkdir(path):
    if not __Paths.get(path).isAbsolute():
        __Paths.get(__FS_HANDLER.cwd, path).toFile().mkdirs()
    else:
        __Paths.get(path).toFile().mkdirs()


def listdir(path):
    if not __Paths.get(path).isAbsolute():
        files = __Paths.get(__FS_HANDLER.cwd, path).toFile().listFiles()
    else:
        files = __Paths.get(path).toFile().listFiles()
    names = []
    for file in files:
        names.append(file.getName())
    return names


def remove(path):
    if not __Paths.get(path).isAbsolute():
        __Paths.get(__FS_HANDLER.cwd, path).toFile().delete()
    else:
        __Paths.get(path).toFile().delete()


def rmdir(path):
    if not __Paths.get(path).isAbsolute():
        __Paths.get(__FS_HANDLER.cwd, path).toFile().delete()
    else:
        __Paths.get(path).toFile().delete()


def stat(path):
    pass  # TODO


def rename(old, new):
    if not __Paths.get(old).isAbsolute():
        __Files.move(__Paths.get(__FS_HANDLER.cwd, old), __Paths.get(__FS_HANDLER.cwd, new))
    else:
        __Files.move(__Paths.get(old), __Paths.get(new))


def system(command):
    __Runtime.getRuntime().exec(command)


def getenv(name):
    # java_map = __System.getenv(name)
    # dict: dict[str, str] = dict()
    # for key in java_map.keySet():
    #     dict[key] = java_map.get(key)
    # return dict
    pass  # TODO


def environ():
    # java_map = __System.getenv()
    # dict: dict[str, str] = dict()
    # for key in java_map.keySet():
    #     dict[key] = java_map.get(key)
    # return dict
    pass  # TODO


def walk(top, topdown=True, onerror=None, followlinks=False):
    pass  # TODO


def pathsep():
    return __File.pathSeparator


def linesep():
    return __System.getProperty("line.separator")


def devnull():
    if __System.getProperty("os.name").lower().startswith("windows"):
        return "NUL"
    else:
        return "/dev/null"


def abspath(path):
    if not __Paths.get(path).isAbsolute():
        return __Paths.get(__FS_HANDLER.cwd, path).toString()
    else:
        return path


def realpath(path):
    if not __Paths.get(path).isAbsolute():
        return __Paths.get(__FS_HANDLER.cwd, path).toRealPath().toString()
    else:
        return path.toRealPath().toString()


F_OK = 0
R_OK = 4
W_OK = 2
X_OK = 1


def access(path, mode):
    if not __Paths.get(path).isAbsolute():
        path_obj = __Paths.get(__FS_HANDLER.cwd, path)
    else:
        path_obj = __Paths.get(path)

    if not __Files.exists(path_obj):
        return False

    perms = __Files.getPosixFilePermissions(path_obj)  # returns: set[PosixFilePermission]

    if (mode & F_OK) and not __Files.exists(path_obj):
        return False

    if (mode & R_OK) and "OWNER_READ" not in perms and "GROUP_READ" not in perms and "OTHERS_READ" not in perms:
        return False

    if (mode & W_OK) and "OWNER_WRITE" not in perms and "GROUP_WRITE" not in perms and "OTHERS_WRITE" not in perms:
        return False

    if (mode & X_OK) and "OWNER_EXECUTE" not in perms and "GROUP_EXECUTE" not in perms and "OTHERS_EXECUTE" not in perms:
        return False

    return True


def chmod(path, mode):
    if not __Paths.get(path).isAbsolute():
        __Files.setPosixFilePermissions(__Paths.get(__FS_HANDLER.cwd, path), mode)
    else:
        __Files.setPosixFilePermissions(__Paths.get(path), mode)


def chown(path, uid, gid):
    if not __Paths.get(path).isAbsolute():
        __Paths.get(__FS_HANDLER.cwd, path).toFile().setOwner(uid, gid)
    else:
        __Paths.get(path).toFile().setOwner(uid, gid)


def makedirs(path, mode=0o777, exist_ok=False):
    pass  # TODO


def removedirs(path):
    pass  # TODO


def unlink(path):
    pass  # TODO


def utime(path, ns=None):
    pass  # TODO


def get_terminal_size(fd=1):
    pass  # TODO


def get_blocking(fd):
    pass  # TODO


def set_blocking(fd, blocking):
    pass  # TODO


def get_inheritable(fd):
    pass  # TODO


def set_inheritable(fd, inheritable):
    pass  # TODO
