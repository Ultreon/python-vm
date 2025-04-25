package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression

class KvPair {
    PyExpression key
    PyExpression value
    Location location

    KvPair(PyExpression key, PyExpression value, Location location) {
        this.key = key
        this.value = value
        this.location = location
    }
}
