package dev.ultreon.pyvm.compiler.util;

import dev.ultreon.pyvm.compiler.ast.PyExprNode;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyFromImport implements PyImport {
    private final String fromName;
    private final String importName;
    private final String asName;

    public PyFromImport(String fromName, String importName, String asName) {
        this.fromName = fromName;
        this.importName = importName;
        this.asName = asName;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.setLocal(this.asName == null ? this.importName : this.asName, new PyExprNode() {

            @Override
            public void compile(MethodNode method, PyCompileContext context) {
                method.visitLdcInsn(PyFromImport.this.fromName);
                method.visitLdcInsn(PyFromImport.this.importName);
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "importFrom", "(Ljava/lang/String;Ljava/lang/String;)Lpython/types/ModuleType;", false));
            }

            @Override
            public Type getResolvedType(PyCompileContext context) {
                return Type.getType(Object.class);
            }
        });
    }
}
