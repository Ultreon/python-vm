package dev.ultreon.pythonc.modules;

import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.ModulePath;

public class PyBuiltinModule extends JvmModule {
    public PyBuiltinModule(ModulePath path) {
        super(path, new Location());
    }
}
