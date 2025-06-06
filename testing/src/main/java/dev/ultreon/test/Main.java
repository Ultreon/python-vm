package dev.ultreon.test;

import example.HelloPy;

import java.awt.*;

import static example.HelloPy.*;
import static example.ToLoadPy.*;
import static org.python._internal.ClassUtils.getAttribute;

public class Main {
    public static void main(String[] args) {
        TestButton2 testButton2 = new TestButton2(2, 5);
        System.out.println("testButton2 = " + testButton2);

        String string = (String) getAttribute(HelloPy.class, "STRING");
        System.out.println("YEET: " + string);

        TestClass testClass = new TestClass(3, 5);

        run();

        Object a1 = ((TestClass) getAttribute(HelloPy.class, "TEST_CLASS")).__getattr__("a");
        Object b1 = ((TestClass) getAttribute(HelloPy.class, "TEST_CLASS")).__getattr__("b");

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