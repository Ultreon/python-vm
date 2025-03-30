package dev.ultreon.pythonc;

import dev.ultreon.pythonc.statement.PyStatement;

abstract class PyCompoundStatement extends PyStatement {
    private Location location;

    PyCompoundStatement(Location location) {
        this.location = location;
    }

    @Override
    Location getLocation() {
        return location;
    }
}
