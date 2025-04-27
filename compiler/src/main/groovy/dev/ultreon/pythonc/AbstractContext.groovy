package dev.ultreon.pythonc

import org.objectweb.asm.Type

abstract class AbstractContext implements Context {
    private static final stack = ThreadLocal.withInitial { new Stack<Type>() }
    private static ThreadLocal<Boolean> prot = ThreadLocal.withInitial { true }

    static void unprotect() {
        if (!stack.get().empty)
            throw new IllegalArgumentException("Stack is not empty!")
        prot.set(false)
    }

    static void protect() {
        if (!stack.get().empty)
            throw new IllegalArgumentException("Stack is not empty!")
        prot.set(true)
    }

    static void reset() {
        stack.set(new Stack<Type>())
    }

    static boolean isProtected() {
        return prot.get()
    }

    @Override
    boolean isPopNeeded() {
        return !stack.get().empty
    }

    @Override
    void push(Type type) {
        if (prot.get()) throw new IllegalArgumentException("Stack is protected!")
        if (type.sort == Type.METHOD) throw new IllegalArgumentException("Method is not an object!")
        stack.get().push(type)
        if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) stack.get().push(type)
    }

    @Override
    Type pop() {
        if (prot.get()) throw new IllegalArgumentException("Stack is protected!")
        if (stack.get().empty)
            throw new RuntimeException("Stack is empty")
        def pop = stack.get().pop()
        if (pop == Type.LONG_TYPE || pop == Type.DOUBLE_TYPE) stack.get().pop()
        return pop
    }

    @Override
    Type peek() {
        if (prot.get()) throw new IllegalArgumentException("Stack is protected!")
        if (stack.get().empty)
            throw new RuntimeException("Stack is empty")
        return stack.get().peek()
    }

    @Override
    int getStackSize() {
        if (prot.get()) throw new IllegalArgumentException("Stack is protected!")
        return stack.get().size()
    }
}
