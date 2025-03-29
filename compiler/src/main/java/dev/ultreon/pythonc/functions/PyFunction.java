package dev.ultreon.pythonc.functions;

import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.VariableExpr;
import dev.ultreon.pythonc.functions.param.PyParameter;
import dev.ultreon.pythonc.statement.PyBlock;
import dev.ultreon.pythonc.statement.PyStatement;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PyFunction extends PyBaseFunction {
    private final FunctionDefiner owner;
    private PyBlock body;
    private final MethodNode node;
    private final boolean isStatic;
    private final Location location;
    private int index;
    private final PyVariables variables = new PyVariables();

    public PyFunction(FunctionDefiner owner, String name, PyParameter[] parameters, @Nullable JvmClass returnType, boolean isStatic, Location location) {
        super(name, parameters, returnType);
        this.owner = owner;
        this.body = body;

        node = new MethodNode(Opcodes.ACC_PUBLIC | (isStatic || owner.isModule() ? Opcodes.ACC_STATIC : 0), name, signature(), null, null);
        this.isStatic = isStatic;
        this.location = location;

        index = isStatic ? 0 : 1;

        for (PyParameter parameter : parameters) {
            Type type = parameter.type();
            VariableExpr variableExpr = defineVariable(parameter.name(), location);
            parameter.index = variableExpr.index;
        }
    }

    public static PyFunction withContent(FunctionDefiner owner, String name, PyParameter[] parameters, JvmClass returnType, boolean isStatic, Location location, Consumer<MethodNode> content) {
        return new PyFunction(owner, name, parameters, returnType, owner.isModule() || isStatic, location) {
            @Override
            public void writeContent(PythonCompiler compiler, MethodNode node) {
                content.accept(node);
            }
        };
    }

    public MethodNode node() {
        return node;
    }

    public FunctionDefiner owner() {
        return owner;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public PyBlock body() {
        return body;
    }

    @Override
    public void writeFunction(PythonCompiler compiler, JvmWriter writer) {
        FunctionContext functionContext = compiler.startFunction(owner, this, name(), parameters(), signature());
        compiler.swapMethod(node, () -> {
            writeContent(compiler, node);
        });
        compiler.endFunction(functionContext);
    }

    @Override
    public void writeContent(PythonCompiler compiler, MethodNode node) {
        body.writeStatement(compiler, compiler.writer);
        List<PyStatement> statements = body.getStatements();
        compiler.checkPop(!statements.isEmpty() ? statements.getLast().location() : body.location());
    }

    public VariableExpr defineVariable(String name, Location location) {
        if (variables.contains(name)) {
            return variables.get(name);
        }
        this.variables.add(index++, name, location);
        return this.variables.get(name);
    }

    @Override
    public Location location() {
        return location;
    }

    public boolean returnType(Type type) {
        JvmClass returnType = PythonCompiler.classCache.require(PythonCompiler.current(), type);
        if (returnType == null) {
            return false;
        }
        if (returnType.doesInherit(PythonCompiler.current(), returnClass())) {
            this.returnType = returnType;
            return true;
        }
        return false;
    }

    public VariableExpr getVariable(String name) {
        return variables.get(name);
    }

    public void body(PyBlock visit) {
        body = visit;
    }

    @Override
    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        owner.write(compiler, writer);
        writer.createArgs(args);
        writer.createKwargs(kwargs);
        writer.dynamicCall(name());
    }
}
