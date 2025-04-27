package dev.ultreon.test;

import example.HelloPy;
import example.hello.TestClass;
import example.hello.Testing;

import java.awt.*;

import static org.python._internal.ClassUtils.getAttribute;

public class Main {
    public static void main(String[] args) {
        String string = (String) getAttribute(HelloPy.class, "STRING");
        System.out.println("YEET: " + string);

        TestClass testClass = new TestClass(3, 5);

        HelloPy.run();

        Object a1 = ((TestClass) getAttribute(example.HelloPy.class, "TEST_CLASS")).__getattr__("a");
        Object b1 = ((TestClass) getAttribute(example.HelloPy.class, "TEST_CLASS")).__getattr__("b");

        Testing testing = new Testing();
        Frame o = (Frame) testing.__getattr__("frame");
        o.setVisible(true);

        System.out.println("a1 = " + a1);
        System.out.println("b1 = " + b1);

        Object a = testClass.__getattr__("a");
        Object b = testClass.__getattr__("b");

        System.out.println(a);
        System.out.println(b);
    }
}