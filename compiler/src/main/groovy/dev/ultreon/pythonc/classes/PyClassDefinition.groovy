package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.ConstantExpr
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.statement.ClassContext
import dev.ultreon.pythonc.statement.PyCompoundStatement
import dev.ultreon.pythonc.statement.PyStatement
import groovyjarjarasm.asm.tree.InnerClassNode
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

import static org.objectweb.asm.Opcodes.*

class PyClassDefinition extends PyCompoundStatement {
    private final List<PyStatement> statements = new ArrayList<>()
    private final List<PyCompoundStatement> compoundStatements = new ArrayList<>()
    private final String name
    private final ModulePath module
    public final PyClass type
    public final PyFunctions functions = new PyFunctions()
    public final PyFields fields = new PyFields()
    String doc
    PyModuleDefinition parent

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
            classNode.nestMembers = [parent.path.asType().internalName]
            classNode.innerClasses = [new org.objectweb.asm.tree.InnerClassNode(type.type.internalName, parent.path.asType().internalName, type.name, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)]
            def extendingPyClasses = []
            for (extendingClass in type.extendingClasses) {
                if (extendingClass == null) continue
                if (extendingClass.interface) {
                    classNode.interfaces.add extendingClass.type.internalName
                } else {
                    if (extendingClass instanceof JavaClass) {
                        if (classNode.superName != null && classNode.superName != "java/lang/Object") throw new CompilerException("Class " + classNode.name + " cannot extend " + classNode.superName + " and " + extendingClass.type.internalName + " at the same time!", location)
                        classNode.superName = extendingClass.type.internalName
                    } else {
                        extendingPyClasses.add(extendingClass)
                    }
                }
            }

            writerClassInit(compiler, writer, extendingPyClasses)

            for (PyCompoundStatement statement : compoundStatements) {
                throw new TODO()
            }

            for (PyFunction function : functions) {
                function.writeFunction(compiler, writer)
                compiler.checkPop(function.location)
            }

            {
                MethodNode methodNode = new MethodNode()
                methodNode.name = "<<-parent->>"
                methodNode.desc = "()Lorg/python/_internal/PyObject;"
                methodNode.access = ACC_PUBLIC | ACC_SYNTHETIC
                methodNode.visitCode()
                methodNode.visitFieldInsn(GETFIELD, type.type.internalName, "<<-parent->>", "Lorg/python/_internal/PyObject;")
                methodNode.visitInsn(ARETURN)
                methodNode.visitMaxs(0, 0)
                methodNode.visitEnd()
                methodNode.signature = null
                methodNode.exceptions = null
                methodNode.visibleAnnotations = null
                methodNode.invisibleAnnotations = null
                methodNode.visibleTypeAnnotations = null
                methodNode.invisibleTypeAnnotations = null
            }

            {
                MethodNode methodNode = new MethodNode()
                methodNode.name = "<<-module->>"
                methodNode.desc = "()Lorg/python/_internal/PyModule;"
                methodNode.access = ACC_PUBLIC | ACC_SYNTHETIC
                methodNode.visitCode()
                methodNode.visitFieldInsn(GETFIELD, type.type.internalName, "<<-parent->>", "Lorg/python/_internal/PyObject;")

                Label start = new Label()
                Label end = new Label()
                Label nullErr = new Label()
                methodNode.visitLabel(start)
                methodNode.visitInsn(DUP)
                methodNode.visitJumpInsn(IFNULL, nullErr)
                methodNode.visitTypeInsn(INSTANCEOF, "org/python/_internal/PyModule")
                methodNode.visitJumpInsn(IFEQ, end)
                methodNode.visitFieldInsn(GETFIELD, type.type.internalName, "<<-parent->>", "Lorg/python/_internal/PyObject;")
                methodNode.visitJumpInsn(GOTO, start)
                methodNode.visitLabel(nullErr)
                methodNode.visitTypeInsn(NEW, "org/python/builtins/ModuleNotFoundError")
                methodNode.visitInsn(DUP)
                methodNode.visitLdcInsn("Module for class " + classNode.name + " has vanished!")
                methodNode.visitMethodInsn(INVOKESPECIAL, "org/python/builtins/ModuleNotFoundError", "<init>", "(Ljava/lang/String;)V", false)
                methodNode.visitInsn(ATHROW)
                methodNode.visitLabel(end)
                methodNode.visitTypeInsn(CHECKCAST, "org/python/_internal/PyModule")
                methodNode.visitInsn(ARETURN)
                methodNode.visitMaxs(0, 0)
                methodNode.visitEnd()
                methodNode.signature = null
                methodNode.exceptions = null
                methodNode.visibleAnnotations = null
                methodNode.invisibleAnnotations = null
                methodNode.visibleTypeAnnotations = null
            }

            {
                FieldNode fieldNode = new FieldNode(ACC_PUBLIC | ACC_SYNTHETIC, "<<-parent->>", "Lorg/python/_internal/PyObject;", null, null)
                fieldNode.name = "<<-parent->>"
                fieldNode.desc = "Lorg/python/_internal/PyObject;"
                fieldNode.access = ACC_PUBLIC | ACC_SYNTHETIC
                fieldNode.signature = null
                fieldNode.visibleAnnotations = null
                fieldNode.invisibleAnnotations = null
                fieldNode.visibleTypeAnnotations = null
                fieldNode.invisibleTypeAnnotations = null
                classNode.fields.add(fieldNode)
            }

            compiler.checkPop(location)
            compiler.popContext()
        })
    }

    private void writerClassInit(PythonCompiler compiler, JvmWriter writer, List extendingPyClasses) {
        compiler.classInit(it -> {
            initClassValue("__doc__", new ConstantExpr(doc ?: NoneType.None, location))
            initClassValue("__builtins__", new ConstantExpr("builtins", location))

            writer.loadClass(type)
            writer.createArray(extendingPyClasses as List<PyClass>, Type.getType(Class))
            writer.cast(Object)
            writer.dynamicSetAttr("#superClasses")

            it.visitLdcInsn(type.type)
            it.visitMethodInsn(INVOKESTATIC, "org/python/_internal/ClassUtils", "newClass", "(Ljava/lang/Class;)Ljava/lang/Object;", false)
            it.visitInsn(POP)

            for (PyStatement statement : statements) {
                if (statement instanceof PyCompoundStatement) continue
                statement.write compiler, writer
                compiler.checkPop(statement.location)
            }
            compiler.checkPop(location)
        })
    }

    void initClassValue(String name, PyExpression expr) {
        def compiler = PythonCompiler.current
        def writer = compiler.writer

        writer.pushClass(type.type)
        expr.write(compiler, writer)
        writer.dynamicSetAttr(name)
    }
}
