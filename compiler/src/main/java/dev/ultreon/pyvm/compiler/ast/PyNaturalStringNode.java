package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.CompilerException;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyNaturalStringNode implements PyStringNode {
    private final String content;

    public PyNaturalStringNode(String content) {
        this.content = content;
    }

    public String getContent() {
        String content = this.content;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\') {
                i++;
                c = content.charAt(i);
                if (c == 'x' || c == 'X') {
                    i++;
                    StringBuilder hex = new StringBuilder();
                    int count = 0;
                    while (i < content.length() && content.charAt(i) >= '0' && content.charAt(i) <= '9') {
                        hex.append(content.charAt(i));
                        if (++count == 2) break;
                        i++;
                    }
                    if (count < 2) {
                        throw new CompilerException("Invalid hex escape sequence");
                    }
                    try {
                        int value = Integer.parseInt(hex.toString(), 16);
                        content = content.substring(0, i - 4) + (char) value + content.substring(i);
                    } catch (NumberFormatException e) {
                        throw new CompilerException("Invalid hex escape sequence");
                    }
                } else if (c == 'u' || c == 'U') {
                    i++;
                    StringBuilder hex = new StringBuilder();
                    int count = 0;
                    while (i < content.length() && content.charAt(i) >= '0' && content.charAt(i) <= '9') {
                        hex.append(content.charAt(i));
                        if (++count == 4) break;
                        i++;
                    }
                    if (count < 4) {
                        throw new CompilerException("Invalid unicode escape sequence");
                    }
                    try {
                        int value = Integer.parseInt(hex.toString(), 16);
                        content = content.substring(0, i - 6) + (char) value + content.substring(i);
                    } catch (NumberFormatException e) {
                        throw new CompilerException("Invalid unicode escape sequence");
                    }
                } else if (c == 'r') {
                    content = content.substring(0, i - 2) + "\r" + content.substring(i);
                } else if (c == 'n') {
                    content = content.substring(0, i - 2) + "\n" + content.substring(i);
                } else if (c == 't') {
                    content = content.substring(0, i - 2) + "\t" + content.substring(i);
                } else if (c == 'f') {
                    content = content.substring(0, i - 2) + "\f" + content.substring(i);
                } else if (c == 'b') {
                    content = content.substring(0, i - 2) + "\b" + content.substring(i);
                } else if (c == 'v') {
                    content = content.substring(0, i - 2) + "\u000B" + content.substring(i);
                }
            }
        }
        return content;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        method.instructions.add(new LdcInsnNode(this.getContent()));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createStr", "(Ljava/lang/String;)Lpython/builtins/Str;", false));
    }
}
