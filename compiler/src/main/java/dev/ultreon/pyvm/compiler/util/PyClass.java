package dev.ultreon.pyvm.compiler.util;

import dev.ultreon.pyvm.compiler.Main;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PyClass {
    private final Type type;
    private final String simpleName;
    private final String qualifiedName;
    private final Type superClass;
    private final Type[] interfaces;
    private final List<PyFunction> methods = new ArrayList<>();
    private final List<PyAttribute> attributes = new ArrayList<>();
    private final List<PyAttribute> instanceAttributes = new ArrayList<>();
    private final ClassNode classNode;

    public PyClass(Type type, String simpleName, String qualifiedName, Type superClass, Type[] interfaces) {
        this.type = type;
        this.simpleName = simpleName;
        this.qualifiedName = qualifiedName;
        this.superClass = superClass;
        this.interfaces = interfaces;

        classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.name = this.qualifiedName.replace('.', '/');
        classNode.superName = this.superClass.getInternalName();
        classNode.interfaces = new ArrayList<>();
        for (Type t : this.interfaces) {
            classNode.interfaces.add(t.getInternalName());
        }
    }

    public void addMethod(PyFunction method) {
        this.methods.add(method);
    }

    public void addAttribute(PyAttribute attribute) {
        this.attributes.add(attribute);
    }

    public void addInstanceAttribute(PyAttribute attribute) {
        this.instanceAttributes.add(attribute);
    }

    public Type getType() {
        return this.type;
    }

    public String getSimpleName() {
        return this.simpleName;
    }

    public String getQualifiedName() {
        return this.qualifiedName;
    }

    public Type getSuperClass() {
        return this.superClass;
    }

    public Type[] getInterfaces() {
        return this.interfaces;
    }

    public List<PyFunction> getMethods() {
        return this.methods;
    }

    public List<PyAttribute> getAttributes() {
        return this.attributes;
    }

    public List<PyAttribute> getInstanceAttributes() {
        return this.instanceAttributes;
    }

    public void finish(Path outputDir) {
        Main.writeClassToFile(outputDir.resolve(this.qualifiedName.replace('.', '/') + ".class"), classNode);
    }
}
