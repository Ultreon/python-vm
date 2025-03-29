package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.Module;
import dev.ultreon.pythonc.modules.JvmModule;
import org.objectweb.asm.tree.ClassNode;

public class ExpectedModule extends Module {
    public ExpectedModule(JvmModule parent, ModulePath path, Location location) {
        super(new ClassNode(), path, location);
    }
}
