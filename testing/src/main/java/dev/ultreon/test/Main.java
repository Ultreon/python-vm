package dev.ultreon.test;

import example.HelloPy;
//import example.TestButton;
//import example.TestClass;
import example.TestButton;
import example.Testing;
//import example.Testing;

import java.awt.*;

public class Main {
    public static void main(String[] args) {
        HelloPy.init();

//        TestClass testClass = new TestClass(3, 5);

//        Object a1 = HelloPy.TEST_CLASS.a;
//        Object b1 = HelloPy.TEST_CLASS.b;

        Frame frame = new Frame();
//        TestButton btn = new TestButton();

        TestButton btn = new TestButton(3, 5);
        long sum = btn.sum();

        System.out.println(sum);

        Testing testing = new Testing();
        Frame o = (Frame) testing.__getattr__("frame");
        o.setVisible(true);
//        frame.add(btn);

//        System.out.println("a1 = " + a1);
//        System.out.println("b1 = " + b1);

//        Object a = testClass.a;
//        Object b = testClass.b;

//        System.out.println(a);
//        System.out.println(b);
    }
}