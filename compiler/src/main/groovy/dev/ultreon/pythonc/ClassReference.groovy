package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.expr.MemberAttrExpr
import dev.ultreon.pythonc.expr.MemberCallExpr
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.statement.PyFromImportStatement
import org.objectweb.asm.Type

class ClassReference extends JvmClass implements SymbolReference {
    private final SymbolReferenceExpr name
    private final Location location
    private JvmClass resolved

    ClassReference(SymbolReferenceExpr reference) {
        super(null, null, reference.location)
        this.name = reference
        this.location = reference.location
    }

    @Override
    JvmClass resolve(PythonCompiler compiler) {
        if (resolved != null) return resolved
        def name = name.name
        PySymbol symbol = this.name.symbol()
        if (symbol instanceof JvmClass) {
            this.resolved = (JvmClass) symbol
            return (JvmClass) symbol
        } else if (symbol instanceof PyFromImportStatement.ImportedSymbol) {
            PyFromImportStatement.ImportedSymbol importedSymbol = (PyFromImportStatement.ImportedSymbol) symbol
            if (importedSymbol.value instanceof JvmClass) return (JvmClass) importedSymbol.value
            else {
                throw new CompilerException("Unresolved class: " + this.name, location)
            }
        }
        throw new CompilerException("Unresolved class: " + this.name, location)
    }

    @Override
    Type getType() {
        return resolve().type
    }

    @Override
    String getName() {
        return resolve().name
    }

    @Override
    String className() {
        return resolve().className()
    }

    @Override
    void writeReference(PythonCompiler compiler, JvmWriter writer) {
        resolve().writeReference(compiler, writer)
    }

    @Override
    MemberCallExpr call(String name, Location location, PyExpression... arguments) {
        return resolve().call(name, location, arguments)
    }

    @Override
    MemberCallExpr call(String name, List<PyExpression> arguments, Location location) {
        return resolve().call(name, arguments, location)
    }

    @Override
    MemberCallExpr call(Location location, PyExpression... arguments) {
        return resolve().call(location, arguments)
    }

    @Override
    MemberCallExpr call(List<PyExpression> arguments, Location location) {
        return resolve().call(arguments, location)
    }

    @Override
    MemberAttrExpr attr(String name, Location location) {
        return resolve().attr(name, location)
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        resolve().writeCall(compiler, writer, args, kwargs)
    }

    @Override
    void prepare(PythonCompiler compiler, JvmWriter writer) {
        resolve().prepare(compiler, writer)
    }

    @Override
    Type write(PythonCompiler compiler, JvmWriter writer) {
        return resolve().write(compiler, writer) as Type
    }

    @Override
    boolean isInterface() {
        return resolve().interface
    }

    JvmClass resolve() {
        return resolve(PythonCompiler.current)
    }

    @Override
    boolean isAbstract() {
        return resolve().abstract
    }

    @Override
    boolean isEnum() {
        return resolve().enum
    }

    @Override
    boolean isAnnotation() {
        return resolve().annotation
    }

    @Override
    boolean isRecord() {
        return resolve().record
    }

    @Override
    boolean isSealed() {
        return resolve().sealed
    }

    @Override
    boolean isModule() {
        return resolve().module
    }

    @Override
    boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        return resolve().doesInherit(compiler, type)
    }
}
