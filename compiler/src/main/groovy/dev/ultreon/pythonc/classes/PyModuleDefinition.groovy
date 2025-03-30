package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.statement.PyStatement

class PyModuleDefinition extends PyCompoundStatement {
    private final List<PyStatement> statements = new ArrayList<>()
    private final List<PyCompoundStatement> compoundStatements = new ArrayList<>()
    private final String name
    private final ModulePath parent
    private final Module type
    public final PyClasses classes = new PyClasses()
    public final PyFunctions functions = new PyFunctions()

    PyModuleDefinition(ModulePath path, Location location, Module type) {
        super(location)
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

    def addClass(LangClass clazz) {
        classes.add(clazz)
    }

    def addFunction(PyFunction function) {
        functions.add(function)
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        compiler.moduleDefinition type.type, {
            writeClassInit(compiler, writer)

            for (function in functions) {
                function.writeFunction compiler, writer
                it.methods.add function.node()
            }
        }

        for (LangClass clazz : classes) {
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
}
