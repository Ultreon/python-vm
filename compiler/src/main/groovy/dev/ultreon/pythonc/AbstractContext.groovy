package dev.ultreon.pythonc

import org.objectweb.asm.Type

abstract class AbstractContext implements Context {
    private static final stack = new Stack<Type>()

    @Override
    boolean isPopNeeded() {
        return !stack.empty
    }

    @Override
    void push(Type type) {
        if (type.sort == Type.METHOD) throw new IllegalArgumentException("Method is not an object!")
        stack.push(type)
        if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) stack.push(type)
    }

    @Override
    Type pop() {
        if (stack.empty)
            throw new RuntimeException("Stack is empty")
        def pop = stack.pop()
        if (pop == Type.LONG_TYPE || pop == Type.DOUBLE_TYPE) stack.pop()
        return pop
    }

    @Override
    Type peek() {
        if (stack.empty)
            throw new RuntimeException("Stack is empty")
        return stack.peek()
    }

    @Override
    int getStackSize() {
        return stack.size()
    }
}
