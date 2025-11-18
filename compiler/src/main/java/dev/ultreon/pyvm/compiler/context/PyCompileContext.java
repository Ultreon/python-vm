package dev.ultreon.pyvm.compiler.context;

import dev.ultreon.pyvm.compiler.CompilerException;
import dev.ultreon.pyvm.compiler.scope.Scope;
import dev.ultreon.pyvm.compiler.util.Variable;
import dev.ultreon.pyvm.compiler.ast.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public abstract class PyCompileContext {
    private final Map<String, PyImportNode> imports = new HashMap<>();
    private final Stack<Scope> scopes = new Stack<>();

    public PyCompileContext() {
        this.imports.put("print", new PyImportFromNode("builtins", "print"));
        this.imports.put("input", new PyImportFromNode("builtins", "input"));
        this.imports.put("range", new PyImportFromNode("builtins", "range"));
        this.imports.put("len", new PyImportFromNode("builtins", "len"));
        this.imports.put("str", new PyImportFromNode("builtins", "str"));
        this.imports.put("int", new PyImportFromNode("builtins", "int"));
        this.imports.put("float", new PyImportFromNode("builtins", "float"));
        this.imports.put("bool", new PyImportFromNode("builtins", "bool"));
        this.imports.put("type", new PyImportFromNode("builtins", "type"));
        this.imports.put("abs", new PyImportFromNode("builtins", "abs"));
        this.imports.put("ord", new PyImportFromNode("builtins", "ord"));
        this.imports.put("chr", new PyImportFromNode("builtins", "chr"));
        this.imports.put("pow", new PyImportFromNode("builtins", "pow"));
        this.imports.put("sum", new PyImportFromNode("builtins", "sum"));
        this.imports.put("min", new PyImportFromNode("builtins", "min"));
        this.imports.put("max", new PyImportFromNode("builtins", "max"));
        this.imports.put("round", new PyImportFromNode("builtins", "round"));
        this.imports.put("None", new PyImportFromNode("builtins", "None"));
        this.imports.put("True", new PyImportFromNode("builtins", "True"));
        this.imports.put("False", new PyImportFromNode("builtins", "False"));
        this.imports.put("list", new PyImportFromNode("builtins", "list"));
        this.imports.put("tuple", new PyImportFromNode("builtins", "tuple"));
        this.imports.put("set", new PyImportFromNode("builtins", "set"));
        this.imports.put("dict", new PyImportFromNode("builtins", "dict"));
        this.imports.put("zip", new PyImportFromNode("builtins", "zip"));
        this.imports.put("enumerate", new PyImportFromNode("builtins", "enumerate"));
        this.imports.put("reversed", new PyImportFromNode("builtins", "reversed"));
        this.imports.put("next", new PyImportFromNode("builtins", "next"));
        this.imports.put("super", new PyImportFromNode("builtins", "super"));
        this.imports.put("object", new PyImportFromNode("builtins", "object"));
        this.imports.put("exit", new PyImportFromNode("builtins", "exit"));
        this.imports.put("compile", new PyImportFromNode("builtins", "compile"));
        this.imports.put("globals", new PyImportFromNode("builtins", "globals"));
        this.imports.put("locals", new PyImportFromNode("builtins", "locals"));
        this.imports.put("open", new PyImportFromNode("builtins", "open"));
        this.imports.put("AttributeError", new PyImportFromNode("builtins", "AttributeError"));
        this.imports.put("TypeError", new PyImportFromNode("builtins", "TypeError"));
        this.imports.put("NameError", new PyImportFromNode("builtins", "NameError"));
        this.imports.put("IndexError", new PyImportFromNode("builtins", "IndexError"));
        this.imports.put("StopIteration", new PyImportFromNode("builtins", "StopIteration"));
        this.imports.put("NotImplementedError", new PyImportFromNode("builtins", "NotImplementedError"));
        this.imports.put("ArithmeticError", new PyImportFromNode("builtins", "ArithmeticError"));
        this.imports.put("ValueError", new PyImportFromNode("builtins", "ValueError"));
        this.imports.put("UnicodeError", new PyImportFromNode("builtins", "UnicodeError"));
        this.imports.put("UnicodeEncodeError", new PyImportFromNode("builtins", "UnicodeEncodeError"));
        this.imports.put("UnicodeDecodeError", new PyImportFromNode("builtins", "UnicodeDecodeError"));
        this.imports.put("UnicodeTranslateError", new PyImportFromNode("builtins", "UnicodeTranslateError"));
        this.imports.put("SystemError", new PyImportFromNode("builtins", "SystemError"));
        this.imports.put("ReferenceError", new PyImportFromNode("builtins", "ReferenceError"));
        this.imports.put("MemoryError", new PyImportFromNode("builtins", "MemoryError"));
        this.imports.put("LookupError", new PyImportFromNode("builtins", "LookupError"));
        this.imports.put("ImportError", new PyImportFromNode("builtins", "ImportError"));
        this.imports.put("ModuleNotFoundError", new PyImportFromNode("builtins", "ModuleNotFoundError"));
        this.imports.put("OSError", new PyImportFromNode("builtins", "OSError"));
        this.imports.put("BlockingIOError", new PyImportFromNode("builtins", "BlockingIOError"));
        this.imports.put("ConnectionResetError", new PyImportFromNode("builtins", "ConnectionResetError"));
        this.imports.put("BrokenPipeError", new PyImportFromNode("builtins", "BrokenPipeError"));
        this.imports.put("PermissionError", new PyImportFromNode("builtins", "PermissionError"));
        this.imports.put("TimeoutError", new PyImportFromNode("builtins", "TimeoutError"));
        this.imports.put("EOFError", new PyImportFromNode("builtins", "EOFError"));
        this.imports.put("NotConnectedError", new PyImportFromNode("builtins", "NotConnectedError"));
        this.imports.put("ConnectionAbortedError", new PyImportFromNode("builtins", "ConnectionAbortedError"));
        this.imports.put("ResourceWarning", new PyImportFromNode("builtins", "ResourceWarning"));
        this.imports.put("UserWarning", new PyImportFromNode("builtins", "UserWarning"));
        this.imports.put("DeprecationWarning", new PyImportFromNode("builtins", "DeprecationWarning"));
        this.imports.put("PendingDeprecationWarning", new PyImportFromNode("builtins", "PendingDeprecationWarning"));
        this.imports.put("SyntaxWarning", new PyImportFromNode("builtins", "SyntaxWarning"));
        this.imports.put("RuntimeWarning", new PyImportFromNode("builtins", "RuntimeWarning"));
        this.imports.put("CompositeWarning", new PyImportFromNode("builtins", "CompositeWarning"));
        this.imports.put("Warning", new PyImportFromNode("builtins", "Warning"));
        this.imports.put("BytesWarning", new PyImportFromNode("builtins", "BytesWarning"));
        this.imports.put("UnicodeWarning", new PyImportFromNode("builtins", "UnicodeWarning"));
        this.imports.put("UserDictWarning", new PyImportFromNode("builtins", "UserDictWarning"));
        this.imports.put("UserListWarning", new PyImportFromNode("builtins", "UserListWarning"));
        this.imports.put("UserStringWarning", new PyImportFromNode("builtins", "UserStringWarning"));
        this.imports.put("CopyrightWarning", new PyImportFromNode("builtins", "CopyrightWarning"));
        this.imports.put("ImmediateReloadWarning", new PyImportFromNode("builtins", "ImmediateReloadWarning"));
        this.imports.put("NotModuleWarning", new PyImportFromNode("builtins", "NotModuleWarning"));
    }

    public void addImport(String name, PyImportNode importNode) {
        this.imports.put(name, importNode);
    }

    public PyImportNode getImport(String name) {
        return this.imports.get(name);
    }

    public Type resolveType(String name) {
        if (this.imports.containsKey(name)) {
            return this.imports.get(name).getResolvedType(this);
        }

        return Type.getType(Object.class);
    }

    public <T extends Scope> T getScope(Class<T> ifScopeClass) {
        if (this.scopes.isEmpty()) {
            return null;
        }

        for (int i = this.scopes.size() - 1; i >= 0; i--) {
            Scope scope = this.scopes.get(i);

            if (ifScopeClass.isInstance(scope)) {
                return ifScopeClass.cast(scope);
            }
        }

        throw new CompilerException("No scope of type " + ifScopeClass.getName() + " found");
    }

    public abstract PyFileCompileContext fileContext();

    public abstract ClassNode classNode();

    protected abstract MethodNode method();

    public abstract Variable createVar(String name);


    public void getAttribute(String attributeName) {
        if (attributeName == null) throw new NullPointerException("attributeName");
        method().instructions.add(new LdcInsnNode(attributeName));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "getAttribute", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false));
    }

    public void call(PyExprNode... args) {
        method().instructions.add(new IntInsnNode(Opcodes.BIPUSH, args.length));
        method().instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "python/builtins/Object"));
        int i = 0;
        for (PyExprNode arg : args) {
            method().instructions.add(new InsnNode(Opcodes.DUP));
            method().instructions.add(new IntInsnNode(Opcodes.BIPUSH, i));
            arg.compile(method(), this);
            method().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "toPyObject", "(Ljava/lang/Object;)Lpython/builtins/Object;", false));
            method().instructions.add(new InsnNode(Opcodes.AASTORE));
            i++;
        }

        method().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "call", "(Lpython/builtins/Object;[Lpython/builtins/Object;)Lpython/builtins/Object;", false));
    }

    public void call(PyExprNode[] args, PyKeywordArgNode[] kwargs) {
        method().instructions.add(new IntInsnNode(Opcodes.BIPUSH, args.length));
        method().instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "python/builtins/Object"));
        int i = 0;
        for (PyExprNode arg : args) {
            method().instructions.add(new InsnNode(Opcodes.DUP));
            method().instructions.add(new IntInsnNode(Opcodes.BIPUSH, i));
            arg.compile(method(), this);
            method().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "toPyObject", "(Ljava/lang/Object;)Lpython/builtins/Object;", false));
            method().instructions.add(new InsnNode(Opcodes.AASTORE));
            i++;
        }

        method().instructions.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashMap"));
        method().instructions.add(new InsnNode(Opcodes.DUP));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false));
        for (PyKeywordArgNode kwarg : kwargs) {
            method().instructions.add(new InsnNode(Opcodes.DUP));
            method().instructions.add(new LdcInsnNode(kwarg.getName()));
            kwarg.getValue().compile(method(), this);
            method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
        }

        method().instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/util/Map"));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "call", "(Lpython/builtins/Object;[Lpython/builtins/Object;Ljava/util/Map;)Lpython/builtins/Object;", false));
    }

    protected abstract int localsVar();

    public abstract void getLocal(String text);

    public abstract void setLocal(String text, PyExprNode expr);

    public abstract void setLocal(String text, Type asmType, PyExprNode expr);

    public int getCounter() {
        return fileContext().getCounter();
    }

    public void storeVar(Variable var) {
        VarInsnNode insnNode = new VarInsnNode(Opcodes.ASTORE, var.getSlot());
        method().instructions.add(insnNode);
        var.interact(insnNode);
    }

    public void loadVar(Variable var) {
        VarInsnNode insnNode = new VarInsnNode(Opcodes.ALOAD, var.getSlot());
        method().instructions.add(insnNode);
        var.interact(insnNode);
    }

    public Scope getScope() {
        if (this.scopes.isEmpty()) return null;
        return this.scopes.peek();
    }

    public void pushScope(Scope scope) {
        this.scopes.push(scope);
    }

    public void popScope() {
        this.scopes.pop();
    }

    public void addFunction(String name) {
        setLocal(name, Type.getObjectType("python/types/FunctionType"), new PyExprNode() {
            @Override
            public Type getResolvedType(PyCompileContext context) {
                return Type.getObjectType("python/types/FunctionType");
            }

            @Override
            public void compile(MethodNode method, PyCompileContext context) {
                if ((method().access & Opcodes.ACC_STATIC) == 0)
                    method().instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                else method().instructions.add(new LdcInsnNode(Type.getObjectType(classNode().name)));
                method().instructions.add(new LdcInsnNode("!" + name));
                method().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createFunction", "(Ljava/lang/Object;Ljava/lang/String;)Lpython/_core/VMObject;", false));
            }
        });
    }

    public abstract PyClassCompileContext classContext();

    public abstract PyMethodCompileContext getMethodContext(MethodNode method);
}
