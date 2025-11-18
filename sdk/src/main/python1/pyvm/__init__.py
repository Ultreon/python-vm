from java.lang.reflect import Array
from types import Type


def native(f):
    return f

def jclass(t: Type):
    return getattr(t, '__javaclass__')

def jarray(t: Type):
    return Array.newInstance(t, 0)

def jstring(s: str):
    return s.__jvmvalue__

def jboolean(b: bool):
    return b.__jvmvalue__

def jbyte(b: int):
    return b.__jbyte__

def jshort(s: int):
    return s.__jshort__

def jint(i: int):
    return i.__jint__

def jlong(l: int):
    return l.__jlong__

def jfloat(f: float):
    return f.__jfloat__

def jdouble(d: float):
    return d.__jdouble__

def jchar(c: str):
    return c.__jchar__

def jnull():
    return None.__jnull__
