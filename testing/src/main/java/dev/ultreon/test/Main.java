package dev.ultreon.test;

import example.HelloPy;
import example.TestClass;

public class Main {
    public static void main(String[] args) {
        HelloPy.init();

        TestClass testClass = new TestClass(3, 5);

        Object a1 = HelloPy.TEST_CLASS.a;
        Object b1 = HelloPy.TEST_CLASS.b;

        System.out.println("a1 = " + a1);
        System.out.println("b1 = " + b1);

        Object a = testClass.a;
        Object b = testClass.b;

        System.out.println(a);
        System.out.println(b);
    }
}