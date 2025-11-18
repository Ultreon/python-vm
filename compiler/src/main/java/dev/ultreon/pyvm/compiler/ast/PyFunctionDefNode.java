package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class PyFunctionDefNode implements PyCompoundStatementNode {
    private final String name;
    private final PyParamsNode params;
    private final PyBlockNode body;

    public PyFunctionDefNode(String name, PyParamsNode params, PyBlockNode body) {
        this.name = name;
        this.params = params;
        this.body = body;
    }

    public PyFunctionDefNode(String text, PyParamsNode pyParamsNode) {
        this.name = text;
        this.params = pyParamsNode;
        this.body = new PyBlockNode(new PyStatementNode[0]);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.addFunction(name);
        compile(context.classNode(), context.classContext());
    }

    public PyBlockNode getBody() {
        return body;
    }

    public PyParamsNode getParams() {
        return params;
    }

    public String getName() {
        return name;
    }

    public void compile(ClassNode classNode, PyCompileContext classContext) {
        MethodNode method = new MethodNode(
                0,
                "!" + this.name,
                this.params.getDescriptor() + "Lpython/_core/VMObject;",
                null,
                null
        );
        method.access = Opcodes.ACC_PUBLIC;
        method.visibleAnnotations = new java.util.ArrayList<>();
        AnnotationNode e = new AnnotationNode("Lpython/_core/PyFunctionMeta;");
        e.values = new java.util.ArrayList<>();
        e.values.add("name");
        e.values.add(this.name);
        method.visibleAnnotations.add(e);
        PyMethodCompileContext methodContext = classContext.getMethodContext(method);
        this.body.compile(method, methodContext);
        if (method.instructions.size() > 0) {
            AbstractInsnNode last = method.instructions.getLast();
            if (last.getOpcode() != Opcodes.ARETURN) {
                methodContext.finish();
                method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
            } else {
                methodContext.finish();
            }
        } else {
            methodContext.finish();
        }

        classNode.methods.add(method);
    }
}
