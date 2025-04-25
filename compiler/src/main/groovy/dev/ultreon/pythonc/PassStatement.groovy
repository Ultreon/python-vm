package dev.ultreon.pythonc

import dev.ultreon.pythonc.statement.PyStatement

class PassStatement implements PyStatement {
    private final Location location

    PassStatement(Location location) {
        this.location = location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        // Do nothing
    }

    @Override
    Location getLocation() {
        return location
    }
}
