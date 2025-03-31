package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.statement.PyCompoundStatement
import dev.ultreon.pythonc.statement.PyStatement
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.tree.MethodNode

import static org.objectweb.asm.Opcodes.*

class PyClassDefinition extends PyCompoundStatement {
    private final List<PyStatement> statements = new ArrayList<>()
    private final List<PyCompoundStatement> compoundStatements = new ArrayList<>()
    private final String name
    private final ModulePath module
    public final PyClass type
    public final PyFunctions functions = new PyFunctions()
    public final PyFields fields = new PyFields()

    PyClassDefinition(ClassPath path, Location location, PyClass type) {
        super(location)
        this.name = path.name
        this.module = path.path
        this.type = type
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        compiler.classDefinition(type.type, classNode -> {
            FieldVisitor self = classNode.visitField(ACC_PUBLIC | ACC_FINAL, "__dict__", "Ljava/util/Map;", /*<String, Object>*/ "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null)
            self.visitEnd()

            // Generate method: public Map<String, Object> --dict--() { return storage; }
            MethodNode mv = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "\uffffdict\uffff", "()Ljava/util/Map;", "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null)
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitFieldInsn(GETFIELD, type.type.internalName, "__dict__", "Ljava/util/Map;")
            mv.visitInsn(ARETURN)
            mv.visitMaxs(1, 1)
            mv.visitEnd()

            classNode.methods.add(mv)

            // Generate method: public void --dict--(Map<String, Object> dict) { storage = dict; }
            mv = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "\uffffdict\uffff", "(Ljava/util/Map;)V", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", null)
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitFieldInsn(PUTFIELD, type.type.internalName, "__dict__", "Ljava/util/Map;")
            mv.visitInsn(RETURN)
            mv.visitMaxs(2, 2)
            mv.visitEnd()

            classNode.methods.add(mv)

            // Create default constructor
            mv = new MethodNode(ACC_PUBLIC, "__init__", "()V", null, null)
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitTypeInsn(NEW, "java/util/HashMap")
            mv.visitInsn(DUP)
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
            mv.visitFieldInsn(PUTFIELD, type.type.internalName, "__dict__", "Ljava/util/Map;")

            classNode.methods.add(mv)

            writerClassInit(compiler, writer)

            for (PyCompoundStatement statement : compoundStatements) {
                throw new TODO()
            }

            for (PyFunction function : functions) {
                function.writeFunction(compiler, writer)
            }
        })
    }

    private void writerClassInit(PythonCompiler compiler, JvmWriter writer) {
        compiler.classInit(methodNode -> {
            for (PyStatement statement : statements) {
                if (statement instanceof PyCompoundStatement) continue
                statement.writeStatement(compiler, writer)
            }
        })
    }
}
