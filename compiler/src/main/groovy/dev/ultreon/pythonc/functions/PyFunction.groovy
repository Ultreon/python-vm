package dev.ultreon.pythonc.functions


import dev.ultreon.pythonc.FunctionContext
import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.VariableExpr
import dev.ultreon.pythonc.functions.param.PyParameter
import dev.ultreon.pythonc.statement.PyBlock
import dev.ultreon.pythonc.statement.PyStatement
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

import java.util.function.Consumer

class PyFunction extends PyBaseFunction {
    final FunctionDefiner owner
    PyBlock body
    final MethodNode node
    final boolean isStatic
    final Location location
    int index
    final PyVariables variables = new PyVariables()

    PyFunction(FunctionDefiner owner, String name, PyParameter[] parameters, @Nullable JvmClass returnType, boolean isStatic, Location location) {
        super(name, parameters, returnType)
        this.owner = owner
        this.body = body

        node = new MethodNode(Opcodes.ACC_PUBLIC | (isStatic || owner.module ? Opcodes.ACC_STATIC : 0), name, signature(), null, null)
        this.isStatic = isStatic
        this.location = location

        index = isStatic ? 0 : 1

        for (PyParameter parameter : parameters) {
            VariableExpr variableExpr = defineVariable(parameter.name, location)
            parameter.index = variableExpr.index
        }
    }

    static PyFunction withContent(FunctionDefiner owner, String name, PyParameter[] parameters, JvmClass returnType, boolean isStatic, Location location, Consumer<MethodNode> content) {
        return new PyFunction(owner, name, parameters, returnType, owner.module || isStatic, location) {
            @Override
            void writeContent(PythonCompiler compiler, MethodNode node) {
                content.accept(node)
            }
        }
    }

    MethodNode node() {
        return node
    }

    FunctionDefiner owner() {
        return owner
    }

    boolean isStatic() {
        return isStatic
    }

    PyBlock body() {
        return body
    }

    @Override
    void writeFunction(PythonCompiler compiler, JvmWriter writer) {
        FunctionContext functionContext = compiler.startFunction owner, this, name, parameters()
        compiler.swapMethod node, {
            writeContent(compiler, node)
        }
        compiler.endFunction functionContext
    }

    @Override
    void writeContent(PythonCompiler compiler, MethodNode node) {
        body.writeStatement(compiler, compiler.writer)
        List<PyStatement> statements = body.statements
        compiler.checkPop(!statements.empty ? statements.last.location : body.location)
    }

    VariableExpr defineVariable(String name, Location location) {
        if (variables.contains(name)) {
            return variables.get(name)
        }
        this.variables.add(index++, name, location)
        return this.variables.get(name)
    }

    @Override
    Location getLocation() {
        return location
    }

    boolean returnType(Type type) {
        JvmClass returnType = PythonCompiler.classCache.require(PythonCompiler.current, type)
        if (returnType == null) {
            return false
        }
        if (returnType.doesInherit(PythonCompiler.current, returnClass())) {
            this.returnType = returnType
            return true
        }
        return false
    }

    VariableExpr getVariable(String name) {
        return variables.get(name)
    }

    void body(PyBlock visit) {
        body = visit
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        owner.write(compiler, writer)
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall(name)
    }
}
