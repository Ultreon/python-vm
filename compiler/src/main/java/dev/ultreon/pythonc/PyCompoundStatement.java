package dev.ultreon.pythonc;

import dev.ultreon.pythonc.statement.PyStatement;

public abstract class PyCompoundStatement extends PyStatement {
    private Location location;

    public PyCompoundStatement(Location location) {
        this.location = location;
    }

    @Override
    public Location location() {
        return location;
    }
}
