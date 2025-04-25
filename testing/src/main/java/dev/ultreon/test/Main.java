package dev.ultreon.test;

import example.HelloPy;
import example.hello.TestClass;
import example.hello.Testing;
import example.to_load.TestButton;

import java.awt.*;

public class Main {
    public static void main(String[] args) {
        String string = example.HelloPy.getString();
        System.out.println("YEET: " + string);

        TestClass testClass = new TestClass(3, 5);

        HelloPy.run();

        Object a1 = ((TestClass) example.HelloPy.getTestClass()).__getattr__("a");
        Object b1 = ((TestClass) example.HelloPy.getTestClass()).__getattr__("b");

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