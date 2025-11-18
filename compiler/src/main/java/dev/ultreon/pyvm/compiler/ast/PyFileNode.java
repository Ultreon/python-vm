package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.Main;
import dev.ultreon.pyvm.compiler.context.PyClassCompileContext;
import dev.ultreon.pyvm.compiler.context.PyFileCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Path;

public class PyFileNode implements PyNode {
    private final PyStatementsNode statementsNode;

    public PyFileNode(PyStatementsNode statementsNode) {
        this.statementsNode = statementsNode;
    }

    public void writeClass(Path outputDir, PyFileCompileContext context) {
        ClassNode classNode = context.getClassNode();
        PyClassCompileContext classContext = context.createClassContext();

        MethodNode classInit = context.getClassInit();
        PyMethodCompileContext context1 = new PyMethodCompileContext(classInit, classNode, context, classContext);

        for (PyStatementNode statement : this.statementsNode.getStatements()) {
            if (statement == null) {
                continue;
            }
            if (statement instanceof PyClassDefNode) throw new UnsupportedOperationException("Not yet implemented");
            if (statement instanceof PyFunctionDefNode) {
                PyFunctionDefNode functionDefNode = (PyFunctionDefNode) statement;
                functionDefNode.compile(classNode, classContext);
                continue;
            }
            statement.compile(classInit, context);
        }

        if (classInit.instructions.size() > 0) {
            classInit.instructions.add(new InsnNode(Opcodes.RETURN));
        }

        context1.finish();
        classContext.finish();

        Main.writeClassToFile(outputDir.resolve(classNode.name + ".class"), classNode);
    }
}
