package dev.ultreon.pyvm.compiler.util;

import dev.ultreon.pyvm.compiler.ast.PyExprNode;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyTypeImport implements PyImport {
    private final String module;
    private final String alias;

    public PyTypeImport(String module, String alias) {
        this.module = module;
        this.alias = alias;
    }

    public String getModule() {
        return this.module;
    }

    public String getAlias() {
        return this.alias;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.setLocal(this.alias == null ? this.module.substring(this.module.lastIndexOf('.') + 1) : this.alias, new PyExprNode() {
            @Override
            public Type getResolvedType(PyCompileContext context) {
                return Type.getType(Object.class);
            }

            @Override
            public void compile(MethodNode method, PyCompileContext context) {
                method.instructions.add(new LdcInsnNode(module));
                if (alias == null) {
                    method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                } else {
                    method.instructions.add(new LdcInsnNode(alias));
                }
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "typeImport", "(Ljava/lang/String;Ljava/lang/String;)Lpython/_core/VMObject;", false));
            }
        });
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }
}
