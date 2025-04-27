package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.Deletion
import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler

class DelStatement implements PyStatement {
    private final List<Deletion> deletions
    private final Location location

    DelStatement(List<Deletion> deletions, Location location) {
        this.deletions = deletions
        this.location = location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        for (Deletion deletion : deletions) {
            deletion.write(compiler, writer)
        }
    }

    @Override
    Location getLocation() {
        return location
    }
}
