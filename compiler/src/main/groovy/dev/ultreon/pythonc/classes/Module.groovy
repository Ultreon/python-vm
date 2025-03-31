package dev.ultreon.pythonc.classes


import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.functions.FunctionDefiner
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.modules.JvmModule
import dev.ultreon.pythonc.statement.PyStatement
import org.jetbrains.annotations.NotNull
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode

import static com.google.common.base.CaseFormat.UPPER_CAMEL
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import static org.objectweb.asm.Opcodes.ACC_PUBLIC
import static org.objectweb.asm.Opcodes.ACC_STATIC

class Module extends JvmModule implements JvmClassCompilable, FunctionDefiner {
    private final ClassNode classNode
    private final ModuleContext context
    public ModuleExpectations expectations = new ModuleExpectations(this)
    private final PyModuleDefinition definition

    Module(ClassNode classNode, ModulePath path, Location location) {
        super(path, location)
        this.classNode = classNode
        this.context = new ModuleContext()

        this.definition = new PyModuleDefinition(path, location, this)
    }

    void addStatement(PyStatement statement) {
        this.definition.addStatement(statement)
    }

    void addClass(PyClass type) {
        this.definition.classes.add(type)
    }

    void addFunction(PyFunction function) {
        this.definition.functions.add(function)
    }

    static @NotNull
    Module create(ModulePath path, Location location) {
        ClassNode classNode = new ClassNode()
        classNode.access = ACC_PUBLIC
        classNode.name = path.name
        classNode.signature = null
        classNode.interfaces.add("Lorg/python/_internal/PyModule;")
        return new Module(classNode, path, location)
    }

    ClassNode classNode() {
        return classNode
    }

    @Override
    void addProperty(PythonCompiler compiler, String name, Type type) {
        MethodNode getter = new MethodNode(
                ACC_STATIC,
                "get${UPPER_UNDERSCORE.to UPPER_CAMEL, name}",
                "()${type.descriptor}",
                null,
                null
        )

        getter.visitCode()

        compiler.swapMethod getter, {
            compiler.writer.loadClass this.type
            compiler.writer.dynamicGetAttr name
            compiler.writer.returnValue type, location
        }

        getter.visitMaxs(1, 1)
        getter.visitEnd()

        classNode.methods.add(getter)

        MethodNode setter = new MethodNode(
                ACC_STATIC,
                "set" + UPPER_UNDERSCORE.to(UPPER_CAMEL, name),
                "(" + type.descriptor + ")V",
                null,
                null
        )

        setter.visitCode()
        setter.parameters = [new ParameterNode("value", 0)]

        compiler.swapMethod setter, {
            compiler.writer.loadClass this.type
            compiler.writer.loadValue 0, type
            compiler.writer.dynamicSetAttr name
            compiler.writer.returnVoid()
        }

        setter.visitMaxs 1, 1
        setter.visitEnd()

        classNode.methods.add(setter)
    }

    @Override
    void writeClass(PythonCompiler compiler, JvmWriter writer) {
        definition.write compiler, writer
        compiler.writeClass type, classNode
    }

    ModuleContext context() {
        return context
    }
}
