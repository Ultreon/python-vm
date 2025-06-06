package dev.ultreon.pythonc.functions

import com.google.common.base.CaseFormat
import dev.ultreon.pythonc.FunctionContext
import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.classes.PyClass
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.SelfExpr
import dev.ultreon.pythonc.expr.VariableExpr
import dev.ultreon.pythonc.functions.param.PyParameter
import dev.ultreon.pythonc.statement.PyBlock
import dev.ultreon.pythonc.statement.PyStatement
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

import java.util.function.Consumer

class PyFunction extends PyBaseFunction {
    private final FunctionDefiner owner
    private PyBlock body
    private final MethodNode node
    private final boolean isStatic
    private final Location location
    private int index
    private final PyVariables variables = new PyVariables()
    SelfExpr selfSymbol

    PyFunction(FunctionDefiner owner, String name, PyParameter[] parameters, @Nullable JvmClass returnType, boolean isStatic, Location location) {
        super(name, parameters, returnType)
        this.owner = owner
        this.body = body

        node = new MethodNode(Opcodes.ACC_PUBLIC | (isStatic || owner.module ? Opcodes.ACC_STATIC : 0), name == "<init>" ? "<init>" : "-def-" + name, signature(), null, null)
        this.isStatic = isStatic
        this.location = location

        index = isStatic ? 0 : 1

        for (PyParameter parameter : parameters) {
            VariableExpr variableExpr = defineVariable(parameter.name, location)
            parameter.index = variableExpr.index
        }

        if (!isStatic) {
            selfSymbol = new SelfExpr(owner.type, location)
            defineVariable("self", location)
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

    MethodNode getNode() {
        return node
    }

    FunctionDefiner getOwner() {
        return owner
    }

    boolean isStatic() {
        return isStatic
    }

    PyBlock getBody() {
        return body
    }

    @Override
    void writeFunction(PythonCompiler compiler, JvmWriter writer) {
        FunctionContext functionContext = compiler.startFunction owner, this, name == "<init>" ? "<init>" : "-def-" + name, parameters()
        compiler.swapMethod node, {
            node.access |= (PythonCompiler.current.debug ? 0 : Opcodes.ACC_SYNTHETIC)
            if (name == "<init>") {
                def type = Type.getMethodType(signature())
                node.desc = Type.getMethodDescriptor(Type.VOID_TYPE, type.argumentTypes)
            } else {
                def type = Type.getMethodType(signature())
                node.desc = Type.getMethodDescriptor(Type.getType(Object), type.argumentTypes)
            }

            def head = new Label()
            writer.label(head)
            writer.lineNumber(location.lineStart, head)
            functionContext.head = head
            writeContent(compiler, node)
            if (name == "<init>") {
                if (writer.context.popNeeded) {
                    writer.pop()
                }
                writer.returnVoid()
            } else if (writer.context.popNeeded) {
                writer.cast Object
                writer.returnObject()
            } else {
                writer.pushNull()
                writer.returnObject()
            }
            compiler.endFunction functionContext
        }

        compiler.classOut.methods.add(node)

        if (name == "<init>") {
            return
        }
        functionContext = compiler.startFunction owner, this, CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name), parameters()
        MethodNode referenceNode = new MethodNode(Opcodes.ACC_PUBLIC | (isStatic || owner.module ? Opcodes.ACC_STATIC : 0), CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name), signature(), null, null)
        compiler.swapMethod(referenceNode, () -> {
            referenceNode.access |= Opcodes.ACC_PUBLIC
            writer.dynamicCall(isStatic ? (owner.owner as JvmClass) : new SelfExpr((owner.owner as JvmClass).type, location), name, parameters().collect {
                new VariableExpr(it.index(), it.name, location)
            })
            if (returnType == null || returnType == Type.VOID_TYPE) {
                writer.pop()
                compiler.checkPop(location)
                writer.returnVoid()
            } else {
                writer.cast returnType
                writer.returnValue(returnType, location)
            }
            referenceNode.visitMaxs(0, 0)
            referenceNode.visitEnd()
            referenceNode.signature = null
            referenceNode.exceptions = null

            compiler.endFunction(functionContext)
        })

        compiler.classOut.methods.add(referenceNode)
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        if (isStatic) {
            builder.append(Location.ANSI_RED).append("Static-").append(Location.ANSI_RESET)
        }

        if (body == null) {
            return builder.append(Location.ANSI_RED).append("Function ").append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET).append(Location.ANSI_YELLOW).append(signature()).append(Location.ANSI_RESET).toString()
        }
        builder.append(Location.ANSI_RED).append("Function ").append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET).append(Location.ANSI_YELLOW).append(signature()).append(Location.ANSI_RESET).append(" {\n")
        builder.append("  ").append(body.toString().replace("\n", "\n  ")).append("\n")
        builder.append(Location.ANSI_RESET).append("}")
    }

    @Override
    void writeContent(PythonCompiler compiler, MethodNode node) {
        if (body != null) {
            body.write(compiler, compiler.writer)
            List<PyStatement> statements = body.statements
            compiler.checkPop(!statements.empty ? statements.last.location : body.location)
        }
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

    VariableExpr variableByName(String name) {
        return variables.get(name)
    }

    void setBody(PyBlock visit) {
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
