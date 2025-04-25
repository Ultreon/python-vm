from org.python._internal.math import MathNative
from java.lang import Math, Double
from org.apache.commons.math3.special import Erf, Gamma, BesselJ


def round(number):
    return Math.round(number)


def sqrt(number):
    return Math.sqrt(number)


def ceil(number):
    return Math.ceil(number)


def floor(number):
    return Math.floor(number)


def exp(number):
    return Math.exp(number)


def log(number):
    return Math.log(number)


def log10(number):
    return Math.log10(number)


def pow(number, exponent):
    return Math.pow(number, exponent)


def sin(number):
    return Math.sin(number)


def cos(number):
    return Math.cos(number)


def tan(number):
    return Math.tan(number)


def asin(number):
    return Math.asin(number)


def acos(number):
    return Math.acos(number)


def atan(number):
    return Math.atan(number)


def atan2(y, x):
    return Math.atan2(y, x)


def hypot(x, y):
    return Math.hypot(x, y)


def degrees(number):
    return Math.toDegrees(number)


def radians(number):
    return Math.toRadians(number)


def isclose(a, b, rel_tol=0.0000000001, abs_tol=0.0):
    raise TypeError("Not Implemented")


def copysign(x, y):
    return Math.copySign(x, y)


def fmod(x, y):
    raise TypeError("Not Implemented")


def modf(x):
    return x - trunc(x)


def frexp(x):
    raise TypeError("Not Implemented")


def ldexp(x, i):
    raise TypeError("Not Implemented")


def trunc(x):
    if x < 0:
        return Math.ceil(x)
    else:
        return Math.floor(x)


def isinf(x):
    return Double.isInfinite(x)


def isnan(x):
    return Double.isNaN(x)


def isfinite(x):
    return Double.isFinite(x)


def signbit(x):
    return Math.signum(x)


def nextafter(x, y):
    raise TypeError("Not Implemented")


def nexttoward(x, y):
    raise TypeError("Not Implemented")


def remainder(x, y):
    return x % y


def remainderf(x, y):
    raise TypeError("Not Implemented")


def erfc(x):
    return Erf.erfc(x)


def erf(x):
    return Erf.erf(x)


def gamma(x):
    return Gamma.gamma(x)


def lgamma(x):
    return Gamma.logGamma(x)


def lgammaf(x):
    raise TypeError("Not Implemented")


def j0(x):
    return BesselJ.value(0, x)


def j1(x):
    return BesselJ.value(1, x)


def jn(n, x):
    return BesselJ.value(n, x)


def y0(x):
    raise TypeError("Not Implemented")


def y1(x):
    raise TypeError("Not Implemented")


def yn(n, x):
    raise TypeError("Not Implemented")
