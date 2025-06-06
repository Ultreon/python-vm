package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.ConstantExpr
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.fields.PyField
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.statement.PyCompoundStatement
import dev.ultreon.pythonc.statement.PyStatement
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode

import static org.objectweb.asm.Opcodes.*

class PyModuleDefinition extends PyCompoundStatement {
    private List<PyStatement> statements = new ArrayList<>()
    private String name
    private ModulePath parent
    private Module type
    PyClasses classes = new PyClasses()
    PyFunctions functions = new PyFunctions()
    PyFields fields = new PyFields()
    ModulePath path
    String doc = ""

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
            statements.add classDefinition
        } else if (statement instanceof PyFunction) {
            def function = statement
            addFunction function
            statements.add function
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
        clazz.parent = this.type
        clazz.definition.parent = this
    }

    def addFunction(PyFunction function) {
        functions.add(function)
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        if (compiler.debug) {
            println "Writing module: ${path}"
            println "Serialized:\n${toString()}"
        }

        compiler.moduleDefinition path, {
            ModuleContext context
            context = type.context()
            compiler.pushContext(context)
            for (builtinClass in compiler.builtins.classes) {
                context.setSymbol(builtinClass.pyName, builtinClass)
            }
            for (builtinFunction in compiler.builtins.functions) {
                context.setSymbol(builtinFunction.name, builtinFunction)
            }

            it.innerClasses = []
            it.nestMembers = []
            for (type in classes) {
                it.innerClasses.add(new InnerClassNode(type.type.internalName, this.type.type.internalName, type.name.substring(type.name.lastIndexOf('$') + 1), Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC))
                it.nestMembers.add(type.type.internalName)

                context.setSymbol(type.name, type)
            }
            for (field in functions) {
                context.setSymbol(field.name, field)

                if (field.parameters().length == 0 && field.name == "__main__") {
                    def mainMethod = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)
                    mainMethod.visitCode()
                    mainMethod.visitLdcInsn(type.type)
                    mainMethod.visitVarInsn(ALOAD, 0)
                    mainMethod.visitMethodInsn(INVOKESTATIC, "org/python/_internal/PySystem", "pyInit", "(Ljava/lang/Class;[Ljava/lang/String;)V", false)
                    mainMethod.visitMethodInsn(INVOKESTATIC, type.type.internalName, "__main__", "()" + field.returnType.descriptor, false)
                    mainMethod.visitInsn(RETURN)
                    mainMethod.visitMaxs(1, 1)
                    mainMethod.visitEnd()
                    it.methods.add(mainMethod)
                }
            }
            writeClassInit(compiler, writer)

//            for (function in functions) {
//                function.writeFunction compiler, writer
//                compiler.checkPop(function.location)
//                it.methods.add function.node
//            }
//
//            for (PyClass clazz : classes) {
//                clazz.definition.write compiler, writer
//                compiler.checkPop(clazz.location)
//            }
            compiler.checkPop(location)
            context.popContext(compiler)
        }


        compiler.checkPop(location)
    }

    def writeClassInit(PythonCompiler compiler, JvmWriter writer) {
        compiler.classInit {
            it.visitLdcInsn(type.type)
            it.visitMethodInsn(INVOKESTATIC, "org/python/_internal/ClassUtils", "newModule", "(Ljava/lang/Class;)Ljava/lang/Object;", false)
            it.visitInsn(POP)

            initClassValue("__module__", new ConstantExpr(path.toString(), location))
            initClassValue("__file__", new ConstantExpr("compiled://" + path.path.join("/") + ".py", location))
            initClassValue("__name__", new ConstantExpr(path.name, location))
            initClassValue("__package__", new ConstantExpr(parent?.toString() ?: "", location))
            initClassValue("__doc__", new ConstantExpr(doc, location))
            initClassValue("__loader__", new ConstantExpr(NoneType.None, location))
            initClassValue("__spec__", new ConstantExpr(NoneType.None, location))
            initClassValue("__cached__", new ConstantExpr(NoneType.None, location))
            initClassValue("__builtins__", new ConstantExpr("builtins", location))


            for (PyStatement statement : statements) {
                statement.write(compiler, writer)
                compiler.checkPop(statement.location)
            }

            compiler.checkPop(location)
        }

        compiler.checkPop(location)
    }

    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Module ").append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET).append(" {\n")

        for (PyStatement statement : statements) {
            builder.append("  ").append(statement.toString().replace("\n", "\n  ")).append("\n")
        }

        builder.append(Location.ANSI_RESET).append("}")

        return builder.toString()
    }

    void defineVariable(String name, Location location) {
        fields.plusEquals new PyField(type, name, Type.getType(Object), true, location)
    }

    void initClassValue(String name, PyExpression expr) {
        def compiler = PythonCompiler.current
        def writer = compiler.writer
        
        writer.pushClass(type.type)
        expr.write(compiler, writer)
        writer.dynamicSetAttr(name)
    }
}
