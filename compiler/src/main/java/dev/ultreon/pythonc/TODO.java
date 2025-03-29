package dev.ultreon.pythonc;

public class TODO extends Error {
    public TODO(String message) {
        super(message);
    }

    public TODO() {
        this("Not implemented yet");
    }
}
