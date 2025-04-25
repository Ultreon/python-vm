package org.python._internal.math;

import org.apache.commons.math3.special.BesselJ;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.special.Gamma;

public class MathNative {
    public static double round(double number) {
        return Math.round(number);
    }
    public static double cos(double number) {
        return Math.cos(number);
    }

    public static double sin(double number) {
        return Math.sin(number);
    }

    public static double tan(double number) {
        return Math.tan(number);
    }

    public static double sqrt(double number) {
        return Math.sqrt(number);
    }

    public static double acos(double number) {
        return Math.acos(number);
    }

    public static double asin(double number) {
        return Math.asin(number);
    }

    public static double atan(double number) {
        return Math.atan(number);
    }

    public static double log(double number) {
        return Math.log(number);
    }

    public static double exp(double number) {
        return Math.exp(number);
    }

    public static double pow(double base, double exp) {
        return Math.pow(base, exp);
    }

    public static double floor(double number) {
        return Math.floor(number);
    }

    public static double ceil(double number) {
        return Math.ceil(number);
    }

    public static double abs(double number) {
        return Math.abs(number);
    }

    public static double trunc(double number) {
        return number < 0 ? Math.ceil(number) : Math.floor(number);
    }

    public static double modf(double number) {
        return number - trunc(number);
    }

    public static double ldexp(double number, int exp) {
        return Math.scalb(number, exp);
    }

    public static double copysign(double number, double sign) {
        return Math.copySign(number, sign);
    }

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static double hypot(double x, double y) {
        return Math.hypot(x, y);
    }

    public static double erf(double number) {
        return Erf.erf(number);
    }

    public static double erfc(double number) {
        return Erf.erfc(number);
    }

    public static double lgamma(double number) {
        return Gamma.logGamma(number);
    }

    public static double gamma(double number) {
        return Gamma.gamma(number);
    }

    public static double j0(double number) {
        return BesselJ.value(0, number);
    }

    public static double j1(double number) {
        return BesselJ.value(1, number);
    }

    public static double jn(int n, double number) {
        return BesselJ.value(n, number);
    }

    public static double sinc(double number) {
        return number == 0.0 ? 1.0 : Math.sin(number) / number;
    }

    public static double sinh(double number) {
        return Math.sinh(number);
    }

    public static double cosh(double number) {
        return Math.cosh(number);
    }

    public static double asinh(double number) {
        return Math.log(number + Math.sqrt(number * number + 1.0));
    }

    public static double acosh(double number) {
        return Math.log(number + Math.sqrt(number * number - 1.0));
    }

    public static double atanh(double number) {
        return 0.5 * Math.log((1.0 + number) / (1.0 - number));
    }

    public static double acsc(double number) {
        return Math.asin(1.0 / number);
    }

    public static double asec(double number) {
        return Math.acos(1.0 / number);
    }

    public static double acot(double number) {
        return Math.atan(1.0 / number);
    }
}
