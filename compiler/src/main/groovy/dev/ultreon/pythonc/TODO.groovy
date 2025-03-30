package dev.ultreon.pythonc

class TODO extends Error {
    TODO(String message) {
        super(message)
    }

    TODO() {
        this("Not implemented yet")
    }
}
