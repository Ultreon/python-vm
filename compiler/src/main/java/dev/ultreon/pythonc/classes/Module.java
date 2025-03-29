package dev.ultreon.pythonc.classes;

import com.google.common.base.CaseFormat;
import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.functions.FunctionDefiner;
import dev.ultreon.pythonc.functions.PyFunction;
import dev.ultreon.pythonc.modules.JvmModule;
import dev.ultreon.pythonc.statement.PyStatement;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.List;

public class Module extends JvmModule implements JvmClassCompilable, FunctionDefiner {
    private final ClassNode classNode;
    private final ModuleContext context;
    public ModuleExpectations expectations = new ModuleExpectations(this);
    private final PyModuleDefinition definition;

    public Module(ClassNode classNode, ModulePath path, Location location) {
        super(path, location);
        this.classNode = classNode;
        ModulePath parentPath = path.getParent();
        this.context = new ModuleContext();

        this.definition = new PyModuleDefinition(path, location, this);
    }

    public void addStatement(PyStatement statement) {
        this.definition.addStatement(statement);
    }

    public void addClass(LangClass type) {
        this.definition.classes.add(type);
    }

    public void addFunction(PyFunction function) {
        this.definition.functions.add(function);
    }

    public static @NotNull Module create(ModulePath path, Location location) {
        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.name = path.getName();
        classNode.signature = null;
        classNode.interfaces.add("Lorg/python/_internal/PyModule;");
        return new Module(classNode, path, location);
    }

    public ClassNode classNode() {
        return classNode;
    }

    @Override
    public void addProperty(PythonCompiler compiler, String name, Type type) {
        MethodNode getter = new MethodNode(
                Opcodes.ACC_STATIC,
                "get" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name),
                "()" + type.getDescriptor(),
                null,
                null
        );

        getter.visitCode();

        compiler.swapMethod(getter, () -> {
            compiler.writer.loadClass(type());
            compiler.writer.dynamicGetAttr(name);
            compiler.writer.returnValue(type, location());
        });

        getter.visitMaxs(1, 1);
        getter.visitEnd();

        classNode.methods.add(getter);

        MethodNode setter = new MethodNode(
                Opcodes.ACC_STATIC,
                "set" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name),
                "(" + type.getDescriptor() + ")V",
                null,
                null
        );

        setter.visitCode();
        setter.parameters = List.of(new ParameterNode("value", 0));

        compiler.swapMethod(setter, () -> {
            compiler.writer.loadClass(type());
            compiler.writer.loadValue(0, type);
            compiler.writer.dynamicSetAttr(name);
            compiler.writer.returnVoid();
        });

        setter.visitMaxs(1, 1);
        setter.visitEnd();

        classNode.methods.add(setter);
    }

    @Override
    public void writeClass(PythonCompiler compiler, JvmWriter writer) {
        definition.write(compiler, writer);
        compiler.writeClass(type(), classNode);
    }

    public ModuleContext context() {
        return context;
    }
}
