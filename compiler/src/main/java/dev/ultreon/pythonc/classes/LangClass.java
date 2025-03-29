package dev.ultreon.pythonc.classes;

import com.google.common.base.CaseFormat;
import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.functions.FunctionDefiner;
import dev.ultreon.pythonc.functions.StaticLevel;
import dev.ultreon.pythonc.functions.PyFunction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.ArrayList;
import java.util.List;

public class LangClass extends JvmClass implements JvmClassCompilable, FunctionDefiner {
    private final ClassNode classNode;
    private final Module owner;
    public final PyClassDefinition definition;
    private final ClassPath path;
    private final ClassReference[] extending;
    private JvmClass[] extendingClasses;
    private JvmClass[] superClasses;
    private JvmClass[] interfaces;

    public LangClass(ClassNode classNode, Module owner, ClassReference[] extending, Type type, String name, Location location) {
        super(type, name, location);
        this.classNode = classNode;
        this.owner = owner;
        this.extending = extending;
        path = new ClassPath(owner.path(), name);
        definition = new PyClassDefinition(path, location, this);
    }

    private ClassPath path() {
        return path;
    }

    public static LangClass create(List<ClassReference> extending, Type objectType, String text, Location location) {
        ClassNode classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.name = text;
        Module definingModule = PythonCompiler.current().definingModule;
        if (definingModule == null) {
            throw new RuntimeException("definingModule is null");
        }
        return new LangClass(classNode, definingModule, extending.toArray(ClassReference[]::new), objectType, text, location);
    }

    public JvmClass[] superClasses() {
        if (superClasses != null) {
            return superClasses;
        }

        JvmClass[] extending = extendingClasses();
        List<JvmClass> superClasses = new ArrayList<>();
        for (JvmClass extend : extending) {
            if (!extend.isInterface()) {
                superClasses.add(extend);
            }
        }

        return this.superClasses = superClasses.toArray(JvmClass[]::new);
    }

    private JvmClass[] extendingClasses() {
        if (extendingClasses != null) {
            return extendingClasses;
        }
        JvmClass[] classes = new JvmClass[extending.length];
        for (ClassReference reference : extending) {
            reference.resolve(PythonCompiler.current());
        }

        return extendingClasses = classes;
    }

    public JvmClass[] interfaces() {
        if (interfaces != null) {
            return interfaces;
        }

        JvmClass[] extending = extendingClasses();
        List<JvmClass> interfaces = new ArrayList<>();
        for (JvmClass extend : extending) {
            if (extend.isInterface()) {
                interfaces.add(extend);
            }
        }

        return this.interfaces = interfaces.toArray(JvmClass[]::new);
    }

    public ClassNode classNode() {
        return classNode;
    }

    public void addProperty(PythonCompiler compiler, String name, Type type, StaticLevel staticLevel) {
        MethodNode getter = new MethodNode(
                staticLevel == StaticLevel.STATIC ? Opcodes.ACC_STATIC : Opcodes.ACC_PUBLIC,
                "get" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name),
                "()" + type.getDescriptor(),
                null,
                null
        );

        getter.visitCode();

        compiler.swapMethod(getter, () -> {
            if (staticLevel == StaticLevel.STATIC) compiler.writer.loadClass(type());
            else compiler.writer.loadThis(this);
            compiler.writer.dynamicGetAttr(name);
            compiler.writer.returnValue(type, location());
        });

        getter.visitMaxs(1, 1);
        getter.visitEnd();

        classNode.methods.add(getter);

        MethodNode setter = new MethodNode(
                staticLevel == StaticLevel.STATIC ? Opcodes.ACC_STATIC : Opcodes.ACC_PUBLIC,
                "set" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name),
                "(" + type.getDescriptor() + ")V",
                null,
                null
        );

        setter.visitCode();
        setter.parameters.add(new ParameterNode("value", 0));

        compiler.swapMethod(setter, () -> {
            if (staticLevel == StaticLevel.STATIC) compiler.writer.loadClass(type());
            else compiler.writer.loadThis(this);
            compiler.writer.loadValue(staticLevel == StaticLevel.STATIC ? 0 : 1, type);
            compiler.writer.dynamicSetAttr(name);
            compiler.writer.returnVoid();
        });

        setter.visitMaxs(1, 1);
        setter.visitEnd();

        classNode.methods.add(setter);
    }

    @Override
    public void addProperty(PythonCompiler compiler, String name, Type type) {
        addProperty(compiler, name, type, StaticLevel.INSTANCE);
    }

    public Module owner() {
        return owner;
    }

    @Override
    public void writeClass(PythonCompiler compiler, JvmWriter writer) {
        compiler.writeClass(type(), classNode);
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        // TODO add abstract class support
        return false;
    }

    @Override
    public boolean isEnum() {
        // TODO add enum support
        return false;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public boolean isRecord() {
        return false;
    }

    @Override
    public boolean isSealed() {
        return false;
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        for (JvmClass superClass : superClasses) {
            if (superClass == null) continue;
            if (superClass.equals(type)) return true;
            if (superClass.doesInherit(compiler, type)) return true;
        }
        for (JvmClass anInterface : interfaces) {
            if (anInterface == null) continue;
            if (anInterface.equals(type)) return true;
            if (anInterface.doesInherit(compiler, type)) return true;
        }
        return false;
    }

    public PyClassDefinition definition() {
        return definition;
    }

    @Override
    public void addFunction(PyFunction function) {
        definition.functions.add(function);
    }
}
