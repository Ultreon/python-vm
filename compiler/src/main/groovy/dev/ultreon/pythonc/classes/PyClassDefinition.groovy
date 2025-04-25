package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.statement.ClassContext
import dev.ultreon.pythonc.statement.PyCompoundStatement
import dev.ultreon.pythonc.statement.PyStatement

class PyClassDefinition extends PyCompoundStatement {
    private final List<PyStatement> statements = new ArrayList<>()
    private final List<PyCompoundStatement> compoundStatements = new ArrayList<>()
    private final String name
    private final ModulePath module
    public final PyClass type
    public final PyFunctions functions = new PyFunctions()
    public final PyFields fields = new PyFields()

    PyClassDefinition(ClassPath path, Location location, PyClass type) {
        super(location)
        this.name = path.name()
        this.module = path.path()
        this.type = type
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        ClassContext context = new ClassContext(this, SymbolContext.current())
        compiler.pushContext(context)
        compiler.classDefinition(type.type, classNode -> {
            for (extendingClass in type.extendingClasses) {
                if (extendingClass == null) continue
                if (extendingClass.interface) {
                    classNode.interfaces.add extendingClass.type.internalName
                } else {
                    if (classNode.superName != null && classNode.superName != "java/lang/Object") throw new CompilerException("Class " + classNode.name + " cannot extend " + classNode.superName + " and " + extendingClass.type.internalName + " at the same time!", location)
                    classNode.superName = extendingClass.type.internalName
                }
            }

            writerClassInit(compiler, writer)

            for (PyCompoundStatement statement : compoundStatements) {
                throw new TODO()
            }

            for (PyFunction function : functions) {
                function.writeFunction(compiler, writer)
                classNode.methods.add function.node
            }

            compiler.popContext()
        })
    }

    private void writerClassInit(PythonCompiler compiler, JvmWriter writer) {
        compiler.classInit(methodNode -> {
            for (PyStatement statement : statements) {
                if (statement instanceof PyCompoundStatement) continue
                statement.write compiler, writer
            }
        })
    }
}
