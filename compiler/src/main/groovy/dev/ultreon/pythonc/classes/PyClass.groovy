package dev.ultreon.pythonc.classes

import com.google.common.base.CaseFormat
import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.MemberAttrExpr
import dev.ultreon.pythonc.expr.MemberCallExpr
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.fields.PyField
import dev.ultreon.pythonc.functions.FunctionDefiner
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.functions.StaticLevel
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode

class PyClass extends JvmClass implements JvmClassCompilable, FunctionDefiner {
    private final ClassNode classNode
    private final Module owner
    public final PyClassDefinition definition
    private final ClassPath path
    private final ClassReference[] extending
    private JvmClass[] extendingClasses
    private JvmClass[] superClasses
    private JvmClass[] interfaces
    Module parent

    PyClass(ClassNode classNode, Module owner, ClassReference[] extending, Type type, String name, Location location) {
        super(type, name, location)
        this.classNode = classNode
        this.owner = owner
        this.extending = extending
        path = new ClassPath(owner.path(), name)
        definition = new PyClassDefinition(path, location, this)
    }

    private ClassPath path() {
        return path
    }

    static PyClass create(List<ClassReference> extending, Type objectType, Type host, String text, Location location) {
        ClassNode classNode = new ClassNode()
        classNode.version = Opcodes.V1_8
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
        classNode.name = text
        classNode.outerClass = host
        Module definingModule = PythonCompiler.current.definingModule
        if (definingModule == null) {
            throw new RuntimeException("definingModule is null")
        }
        return new PyClass(classNode, definingModule, extending.toArray(ClassReference[]::new), objectType, text, location)
    }

    JvmClass[] superClasses() {
        if (superClasses != null) {
            return superClasses
        }

        JvmClass[] extending = getExtendingClasses()
        List<JvmClass> superClasses = new ArrayList<>()
        for (JvmClass extend : extending) {
            if (!extend.interface) {
                superClasses.add(extend)
            }
        }

        return this.superClasses = superClasses.toArray(JvmClass[]::new)
    }

    JvmClass[] getExtendingClasses() {
        if (extendingClasses != null) {
            return extendingClasses
        }
        JvmClass[] classes = new JvmClass[extending.length]
        def i = 0
        for (ClassReference reference : extending) {
            classes[i] = reference.resolve(PythonCompiler.current)
            i++
        }

        return extendingClasses = classes
    }

    JvmClass[] interfaces() {
        if (interfaces != null) {
            return interfaces
        }

        JvmClass[] extending = extendingClasses
        List<JvmClass> interfaces = new ArrayList<>()
        for (JvmClass extend : extending) {
            if (extend.interface) {
                interfaces.add(extend)
            }
        }

        return this.interfaces = interfaces.toArray(JvmClass[]::new)
    }

    ClassNode classNode() {
        return classNode
    }

    void addProperty(PythonCompiler compiler, String name, Type type, StaticLevel staticLevel) {
        MethodNode getter = new MethodNode(
                staticLevel == StaticLevel.STATIC ? Opcodes.ACC_STATIC : Opcodes.ACC_PUBLIC,
                "get" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name),
                "()" + type.descriptor,
                null,
                null
        )

        getter.visitCode()

        compiler.swapMethod(getter, () -> {
            if (staticLevel == StaticLevel.STATIC) compiler.writer.loadClass(this.type)
            else compiler.writer.loadThis(this)
            compiler.writer.dynamicGetAttr(name)
            compiler.writer.returnValue(type, location)
        })

        getter.visitMaxs(1, 1)
        getter.visitEnd()

        classNode.methods.add(getter)

        MethodNode setter = new MethodNode(
                staticLevel == StaticLevel.STATIC ? Opcodes.ACC_STATIC : Opcodes.ACC_PUBLIC,
                "set" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name),
                "(" + type.descriptor + ")V",
                null,
                null
        )

        setter.visitCode()
        setter.parameters.add(new ParameterNode("value", 0))

        compiler.swapMethod(setter, () -> {
            if (staticLevel == StaticLevel.STATIC) compiler.writer.loadClass(type)
            else compiler.writer.loadThis(this)
            compiler.writer.loadValue(staticLevel == StaticLevel.STATIC ? 0 : 1, type)
            compiler.writer.dynamicSetAttr(name)
            compiler.writer.returnVoid()
        })

        setter.visitMaxs(1, 1)
        setter.visitEnd()

        classNode.methods.add(setter)
    }

    @Override
    void addProperty(PythonCompiler compiler, String name, Type type) {
        addProperty(compiler, name, type, StaticLevel.INSTANCE)
    }

    @Override
    MemberCallExpr call(String name, List<PyExpression> args, Map<String, PyExpression> kwargs, Location location) {
        return new MemberCallExpr(new MemberAttrExpr(this, name, location), args, kwargs, location)
    }

    MemberCallExpr call(List<PyExpression> args, Map<String, PyExpression> kwargs, Location location) {
        return new MemberCallExpr(this, args, kwargs, location)
    }

    Module owner() {
        return owner
    }

    @Override
    void writeClass(PythonCompiler compiler, JvmWriter writer) {
        throw new RuntimeException("DEBUG")
    }

    @Override
    boolean isInterface() {
        return false
    }

    @Override
    boolean isAbstract() {
        // TODO add abstract class support
        return false
    }

    @Override
    boolean isEnum() {
        // TODO add enum support
        return false
    }

    @Override
    boolean isAnnotation() {
        return false
    }

    @Override
    boolean isRecord() {
        return false
    }

    @Override
    boolean isSealed() {
        return false
    }

    @Override
    boolean isModule() {
        return false
    }

    @Override
    boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        for (JvmClass superClass : superClasses) {
            if (superClass == null) continue
            if (superClass == type) return true
            if (superClass.doesInherit(compiler, type)) return true
        }
        for (JvmClass anInterface : interfaces) {
            if (anInterface == null) continue
            if (anInterface == type) return true
            if (anInterface.doesInherit(compiler, type)) return true
        }
        return false
    }

    PyClassDefinition definition() {
        return definition
    }

    @Override
    void addFunction(PyFunction function) {
        definition.functions.add(function)
    }

    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Class ").append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET).append(" <")

        if (superClasses == null || superClasses.length == 0) {
            builder.append(Location.ANSI_PURPLE).append("object")
        } else for (int i = 0; i < superClasses.length; i++) {
            if (i > 0) builder.append(Location.ANSI_RESET).append(", ")
            builder.append(Location.ANSI_PURPLE).append(superClasses[i].name)
        }

        builder.append(Location.ANSI_RESET).append("> {")

//        for (PyStatement statement : definition.statements) {
//            builder.append("\n  ").append(statement.toString().replace("\n", "\n  "))
//        }

        for (PyFunction function : definition.functions) {
            builder.append("\n  ").append(function.toString().replace("\n", "\n  "))
        }

        builder.append("\n}")
    }

    MemberAttrExpr defineVariable(String s, Location location, Type type = Type.getType(Object)) {
        if (definition.fields.find { it.name == s }) return attr(s, location)
        def field = new PyField(this, s, type, true, location)
        definition.fields.plusEquals field
        return attr(name, location)
    }

    DynamicAttrExpr dynAttr(String s, Location location) {
        return new DynamicAttrExpr(this, s, location)
    }
}
