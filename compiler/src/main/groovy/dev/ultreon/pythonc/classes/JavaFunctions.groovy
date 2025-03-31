package dev.ultreon.pythonc.classes


import dev.ultreon.pythonc.CompilerException
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.PyExpression

import org.objectweb.asm.Type

import java.lang.reflect.Method

import static dev.ultreon.pythonc.ClassUtils.getMethodsByName
import static dev.ultreon.pythonc.PythonCompiler.classCache

class JavaFunctions {
    private final aClass

    JavaFunctions(aClass) {
        this.aClass = aClass.javaClass
    }

    Method locate(name, arguments, location) {
        if (!arguments instanceof List<PyExpression>)
            throw new IllegalArgumentException("Not a list of expressions!")

        def methods = getMethodsByName aClass as Class<?>, name as String

        methodLoop: for (method in methods) {
            if (method.parameterCount != arguments) continue
            def i = 0
            for (expr in arguments) {
                if (!expr instanceof PyExpression)
                    throw new IllegalArgumentException("Not a list of expressions!")

                def type = expr.type
                def paramClass = classCache.require method.parameterTypes[i], location as Location
                def argClass = classCache.require type as Type
                if (!argClass.doesInherit(PythonCompiler.current, paramClass)) {
                    continue methodLoop
                }

                i++
            }

            return method
        }

        throw new CompilerException("Unknown Java method", location as Location)
    }
}
