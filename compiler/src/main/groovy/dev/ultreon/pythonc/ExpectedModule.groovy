package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.Module
import dev.ultreon.pythonc.modules.JvmModule
import org.objectweb.asm.tree.ClassNode

class ExpectedModule extends Module {
    ExpectedModule(JvmModule parent, ModulePath path, Location location) {
        super(new ClassNode(), path, location)
    }
}
