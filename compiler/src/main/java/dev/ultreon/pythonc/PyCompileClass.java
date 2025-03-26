package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.nio.file.Path;

public interface PyCompileClass extends JvmClass {
    Path getOutputPath();

    default boolean canAgreeWithParameters(PythonCompiler compiler, JvmFunction function, Type[] input) {
        Type[] types = function.parameterTypes(compiler);
        if (types.length != input.length) {
            return false;
        }
        for (int i = 0; i < input.length; i++) {
            if (types[i] == null) {
                return true;
            }
            if (!types[i].equals(input[i])) {
                throw new CompilerException("Constructor parameter " + i + " is invalid (" + compiler.getLocation(this) + ")");
            }
            JvmClass inputClass = PythonCompiler.classCache.get(input[i]);

            if (i > types.length - 1) {
                return false;
            }

            if (!inputClass.doesInherit(compiler, types[i])) {
                return false;
            }
            JvmClass functionClass = PythonCompiler.classCache.get(types[i]);
        }

        return true;
    }

    boolean isModule();
}
