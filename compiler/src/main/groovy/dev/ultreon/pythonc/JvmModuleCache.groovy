package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.Module
import dev.ultreon.pythonc.modules.JvmModule

class JvmModuleCache {
    private final Map<ModulePath, JvmModule> cache = new HashMap<>()

    void put(String name, JvmModule module) {
        cache.put(new ModulePath(name), module)
    }

    JvmModule get(ModulePath modulePath, Location location) {
        if (modulePath == null) throw new NullPointerException()
        JvmModule jvmModule = cache.get(modulePath)
        if (jvmModule == null) {
            return PythonCompiler.expectations.expectModule(PythonCompiler.current, modulePath, location)
        }
        return jvmModule
    }

    JvmModule getOrCreate(ModulePath path) {
        String pathName = path.toString()
        if (cache.containsKey(pathName)) {
            return cache.get(pathName)
        }
        return Module.create(path, new Location())
    }
}
