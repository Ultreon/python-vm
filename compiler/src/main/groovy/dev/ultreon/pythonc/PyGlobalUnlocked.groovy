package dev.ultreon.pythonc

class PyGlobalUnlocked {
    private final Stack<Set<String>> globalUnlocked = new Stack<>()

    PyGlobalUnlocked() {
        globalUnlocked.push(new HashSet<>())
    }

    void push() {
        globalUnlocked.push(new HashSet<>())
    }

    void pop() {
        if (globalUnlocked.size() == 1)
            throw new RuntimeException("Cannot pop root scope")
        globalUnlocked.pop()
    }

    void add(String name) {
        if (globalUnlocked.size() == 1)
            throw new RuntimeException("Cannot add to root scope")
        globalUnlocked.peek().add(name)
    }

    boolean contains(String name) {
        return globalUnlocked.peek().contains(name)
    }

    void clear() {
        globalUnlocked.clear()
    }
}
