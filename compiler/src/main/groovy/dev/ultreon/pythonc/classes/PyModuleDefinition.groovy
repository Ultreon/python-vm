package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.statement.PyCompoundStatement
import dev.ultreon.pythonc.statement.PyStatement

class PyModuleDefinition extends PyCompoundStatement {
    private List<PyStatement> statements = new ArrayList<>()
    private List<PyCompoundStatement> compoundStatements = new ArrayList<>()
    private String name
    private ModulePath parent
    private Module type
    PyClasses classes = new PyClasses()
    PyFunctions functions = new PyFunctions()
    PyFields fields = new PyFields()
    ModulePath path

    PyModuleDefinition(ModulePath path, Location location, Module type) {
        super(location)
        this.path = path
        this.parent = path.parent
        this.name = path.name
        this.type = type
    }

    def addStatement(statement) {
        if (statement instanceof PyClassDefinition) {
            def classDefinition = statement
            addClass classDefinition.type
        } else if (statement instanceof PyFunction) {
            def function = statement
            addFunction function
        } else if (statement instanceof PyStatement) {
            statements.add statement
        } else {
            throw new IllegalArgumentException("Not a statement: ${statement.class}")
        }
    }

    def addCompoundStatement(PyCompoundStatement statement) {
        compoundStatements.add(statement)
    }

    def addClass(PyClass clazz) {
        classes.add(clazz)
    }

    def addFunction(PyFunction function) {
        functions.add(function)
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        println "Writing module: ${path}"
        println "Serialized:\n${toString()}"

        compiler.moduleDefinition path, {
            writeClassInit(compiler, writer)

            for (function in functions) {
                function.writeFunction compiler, writer
                it.methods.add function.node()
            }
        }

        for (PyClass clazz : classes) {
            clazz.writeClass compiler, writer
        }
    }

    def writeClassInit(PythonCompiler compiler, JvmWriter writer) {
        compiler.classInit {
            for (PyStatement statement : statements) {
                if (statement instanceof PyCompoundStatement) continue
                statement.writeStatement(compiler, writer)
                compiler.checkPop(statement.location)
            }
        }
    }

    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Module ").append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET).append(" {\n")

        for (PyStatement statement : statements) {
            builder.append("  ").append(statement.toString().replace("\n", "\n  ")).append("\n")
        }

        for (PyCompoundStatement statement : compoundStatements) {
            builder.append("  ").append(statement.toString().replace("\n", "\n  ")).append("\n")
        }

        for (PyClass clazz : classes) {
            builder.append("  ").append(clazz.toString().replace("\n", "\n  ")).append("\n")
        }

        for (PyFunction function : functions) {
            builder.append("  ").append(function.toString().replace("\n", "\n  ")).append("\n")
        }

        builder.append(Location.ANSI_RESET).append("}")

        return builder.toString()
    }
}
