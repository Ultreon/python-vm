package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.Location

abstract class PyCompoundStatement implements PyStatement {
    private Location location;

    PyCompoundStatement(Location location) {
        this.location = location;
    }

    @Override
    Location getLocation() {
        return location;
    }
}
