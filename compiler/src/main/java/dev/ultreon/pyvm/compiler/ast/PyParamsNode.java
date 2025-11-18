package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;

public class PyParamsNode implements PyNode {
    private final PyParamNode[] params;

    public PyParamsNode(PyParamNode[] params) {
        this.params = params;
    }

    public PyParamNode[] getParams() {
        return this.params;
    }

    public String getDescriptor() {
        return "(" + "Lpython/_core/VMObject;".repeat(this.params.length) + ")";
    }

    public String getEstimatedDescriptor(PyCompileContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (PyParamNode param : this.params) {
            sb.append(param.getResolvedType(context).getDescriptor());
        }
        sb.append(")");
        return sb.toString();
    }
}
