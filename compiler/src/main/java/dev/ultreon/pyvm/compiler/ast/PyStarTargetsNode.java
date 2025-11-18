package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.Variable;
import org.objectweb.asm.tree.MethodNode;

public class PyStarTargetsNode implements PyTargetNode {
    private final PyTargetNode[] array;

    public PyStarTargetsNode(PyTargetNode[] array) {
        this.array = array;
    }

    public PyTargetNode[] getArray() {
        return this.array;
    }

    @Override
    public void compileSet(MethodNode method, PyCompileContext context, PyExprNode value) {
        Variable var = context.createVar("$starTarget_" + context.getCounter());
        PyVarAccessNode value1 = new PyVarAccessNode(var, value.getResolvedType(context));
        value1.compileSet(method, context, value);

        for (PyTargetNode target : this.array) {
            target.compileSet(method, context, value1);
        }
    }
}
