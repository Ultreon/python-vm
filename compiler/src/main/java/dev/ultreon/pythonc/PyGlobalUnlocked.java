package dev.ultreon.pythonc;

import java.util.*;

public class PyGlobalUnlocked {
    private final Stack<Set<String>> globalUnlocked = new Stack<>();

    public PyGlobalUnlocked() {
        globalUnlocked.push(new HashSet<>());
    }

    public void push() {
        globalUnlocked.push(new HashSet<>());
    }

    public void pop() {
        if (globalUnlocked.size() == 1)
            throw new RuntimeException("Cannot pop root scope");
        globalUnlocked.pop();
    }

    public void add(String name) {
        if (globalUnlocked.size() == 1)
            throw new RuntimeException("Cannot add to root scope");
        globalUnlocked.peek().add(name);
    }

    public boolean contains(String name) {
        return globalUnlocked.peek().contains(name);
    }

    public void clear() {
        globalUnlocked.clear();
    }
}
