package dev.ultreon.pythonc.modules

import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.ModulePath

class PyBuiltinModule extends JvmModule {
    PyBuiltinModule(ModulePath path) {
        super(path, new Location())
    }
}
