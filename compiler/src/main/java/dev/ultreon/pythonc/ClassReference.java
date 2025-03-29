package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.expr.MemberAttrExpr;
import dev.ultreon.pythonc.expr.MemberCallExpr;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.PySymbol;
import org.objectweb.asm.Type;
import org.python._internal.Py;

import java.util.List;
import java.util.Map;

public class ClassReference extends JvmClass implements SymbolReference {
    private final SymbolReferenceExpr name;
    private final Location location;
    private JvmClass resolved;

    public ClassReference(SymbolReferenceExpr reference) {
        super(null, null, reference.location());
        this.name = reference;
        this.location = reference.location();
    }

    @Override
    public JvmClass resolve(PythonCompiler compiler) {
        if (resolved != null) return resolved;
        PySymbol symbol = name.symbol();
        if (symbol instanceof JvmClass) {
            this.resolved = (JvmClass) symbol;
            return (JvmClass) symbol;
        }
        throw new CompilerException("Unresolved class: " + name, location);
    }

    @Override
    public Type type() {
        return resolve().type();
    }

    @Override
    public String name() {
        return resolve().name();
    }

    @Override
    public String className() {
        return resolve().className();
    }

    @Override
    @Deprecated
    public String simpleName() {
        return resolve().simpleName();
    }

    @Override
    public void writeReference(PythonCompiler compiler, JvmWriter writer) {
        resolve().writeReference(compiler, writer);
    }

    @Override
    public MemberCallExpr call(String name, Location location, PyExpression... arguments) {
        return resolve().call(name, location, arguments);
    }

    @Override
    public MemberCallExpr call(String name, List<PyExpression> arguments, Location location) {
        return resolve().call(name, arguments, location);
    }

    @Override
    public MemberCallExpr call(Location location, PyExpression... arguments) {
        return resolve().call(location, arguments);
    }

    @Override
    public MemberCallExpr call(List<PyExpression> arguments, Location location) {
        return resolve().call(arguments, location);
    }

    @Override
    public MemberAttrExpr attr(String name, Location location) {
        return resolve().attr(name, location);
    }

    @Override
    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        resolve().writeCall(compiler, writer, args, kwargs);
    }

    @Override
    public void prepare(PythonCompiler compiler, JvmWriter writer) {
        resolve().prepare(compiler, writer);
    }

    @Override
    public Type write(PythonCompiler compiler, JvmWriter writer) {
        return resolve().write(compiler, writer);
    }

    @Override
    public boolean isInterface() {
        return resolve().isInterface();
    }

    private JvmClass resolve() {
        return resolve(PythonCompiler.current());
    }

    @Override
    public boolean isAbstract() {
        return resolve().isAbstract();
    }

    @Override
    public boolean isEnum() {
        return resolve().isEnum();
    }

    @Override
    public boolean isAnnotation() {
        return resolve().isAnnotation();
    }

    @Override
    public boolean isRecord() {
        return resolve().isRecord();
    }

    @Override
    public boolean isSealed() {
        return resolve().isSealed();
    }

    @Override
    public boolean isModule() {
        return resolve().isModule();
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        return resolve().doesInherit(compiler, type);
    }
}
