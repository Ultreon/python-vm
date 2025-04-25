from org.python._internal.time import TimeNative


def time():
    return TimeNative.factory().time()


def clock():
    return TimeNative.factory().clock()


def ctime(secs=None):
    return TimeNative.factory().ctime(secs)


def gmtime(secs=None):
    return TimeNative.factory().gmtime(secs)


def localtime(secs=None):
    return TimeNative.factory().localtime(secs)


def asctime(secs=None):
    return TimeNative.factory().asctime(secs)


def sleep(secs):
    return TimeNative.factory().sleep(secs)
