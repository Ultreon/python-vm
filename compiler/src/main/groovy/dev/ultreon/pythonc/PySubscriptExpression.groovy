package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.Settable
import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Type

import static org.objectweb.asm.Type.getType

class PySubscriptExpression extends PyExpression implements Settable {
    private final PyAST ast
    private final List<PyExpression> expressions

    PySubscriptExpression(PyAST ast, List<PyExpression> expressions, Location location) {
        super(location)
        this.ast = ast
        this.expressions = expressions
    }

    PyAST getAst() {
        return ast
    }

    List<PyExpression> getStarExpressions() {
        return expressions
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        ast.write(compiler, writer)

        if (expressions.size() == 1) {
            expressions[0].write compiler, writer
        } else if (expressions.size() > 1) {
            throw new CompilerException("Too many expressions in subscript", expressions[2].location)
        } else if (expressions.empty) {
            throw new CompilerException("Expected an expression in subscript", location)
        }

        writer.dynamicGetItem()
    }

    @Override
    Type getType() {
        return getType(Object.class)
    }

    @Override
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
        ast.write(compiler, writer)
        if (expressions.size() > 1) {
            throw new CompilerException("Too many expressions in subscript", expressions[2].location)
        } else if (expressions.empty) {
            throw new CompilerException("Expected an expression in subscript", location)
        }

        if (expressions.size() == 1) {
            expressions[0].write compiler, writer
            writer.cast Object
        }

        expr.write compiler, writer
        writer.dynamicSetItem()
    }

    @Override
    String getName() {
        return null
    }

    @Override
    void delete(PythonCompiler pythonCompiler, JvmWriter jvmWriter) {
        ast.write(pythonCompiler, jvmWriter)
        if (expressions.size() > 1) {
            throw new CompilerException("Too many expressions in subscript", expressions[2].location)
        } else if (expressions.empty) {
            throw new CompilerException("Expected an expression in subscript", location)
        }

        if (expressions.size() == 1) {
            expressions[0].write(pythonCompiler, jvmWriter)
            jvmWriter.cast(getType(Object))
        }

        jvmWriter.dynamicDelItem()
    }
}
