package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.Stack;

public abstract class AbstractContext implements Context {
    private static final Stack<Type> stack = new Stack<>();

    @Override
    public boolean needsPop() {
        return !stack.isEmpty();
    }

    @Override
    public void push(Type type) {
        stack.push(type);
        if (type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE)) stack.push(type);
    }

    @Override
    public Type pop() {
        if (stack.isEmpty())
            throw new RuntimeException("Stack is empty");
        Type pop = stack.pop();
        if (pop.equals(Type.LONG_TYPE) || pop.equals(Type.DOUBLE_TYPE)) stack.pop();
        return pop;
    }

    @Override
    public Type peek() {
        return stack.peek();
    }

    @Override
    public int stackSize() {
        return stack.size();
    }
}
