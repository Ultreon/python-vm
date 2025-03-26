package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;

public class CompileExpectations {
    private final List<ExpectedField> expectedFields = new ArrayList<>();
    private final List<ExpectedFunction> expectedFunctions = new ArrayList<>();
    private final List<ExpectedConstructor> expectedConstructors = new ArrayList<>();
    private final List<ExpectedClass> expectedClasses = new ArrayList<>();
    private final List<ExpectedModule> expectedModules = new ArrayList<>();

    public ExpectedFunction expectFunction(PythonCompiler pythonCompiler, JvmClass pyClass, String name, Type[] types, boolean isStatic, boolean isAbstract, Location location) {
        ExpectedFunction alreadyExists = this.expectedFunctions.stream().filter(expectedFunction -> expectedFunction.equals(new ExpectedFunction(pyClass, name, types, isStatic, isAbstract, location))).findFirst().orElse(null);
        if (alreadyExists != null) {
            return alreadyExists;
        }
        ExpectedFunction e = new ExpectedFunction(pyClass, name, types, isStatic, isAbstract, location);
        this.expectedFunctions.add(e);
        return e;
    }

    public void defineFunction(PythonCompiler pythonCompiler, PyClass pyClass, String name, Type[] types, boolean isStatic, boolean isAbstract, Location location) {
        this.expectedFunctions.removeIf(expectedFunction -> expectedFunction.equals(new ExpectedFunction(pyClass, name, types, isStatic, isAbstract, location)));
    }

    public ExpectedClass expectClass(PythonCompiler pythonCompiler, String module, String name) {
        ExpectedClass expectedClass = new ExpectedClass(module, name);
        expectedClasses.add(expectedClass);
        return expectedClass;
    }

    public List<ExpectedFunction> getExpectedFunctions() {
        return this.expectedFunctions;
    }

    public ExpectedField expectField(PythonCompiler compiler, JvmClass owner, String name, boolean isStatic) {
        ExpectedField field = new ExpectedField(owner, name, isStatic);
        this.expectedFields.add(field);
        return field;
    }

    public ExpectedModule expectModule(PythonCompiler compiler, String module, Type type) {
        ExpectedModule expectedModule = expectedModules.stream().filter(expectedModule1 -> expectedModule1.equals(new ExpectedModule(module, type))).findFirst().orElse(null);
        if (expectedModule != null) {
            return expectedModule;
        }
        ExpectedModule e = new ExpectedModule(module, type);
        this.expectedModules.add(e);
        return e;
    }

    public ExpectedConstructor expectConstructor(PythonCompiler compiler, JvmClass expectedClass, Type[] paramTypes) {
        ExpectedConstructor constructor = expectedConstructors.stream().filter(expectedConstructor -> expectedConstructor.equals(new ExpectedConstructor(expectedClass, paramTypes))).findFirst().orElse(null);
        if (constructor != null) {
            return constructor;
        }
        ExpectedConstructor e = new ExpectedConstructor(expectedClass, paramTypes);
        this.expectedConstructors.add(e);
        return e;
    }

    public static final class ExpectedFunction implements JvmFunction {
        private final JvmClass owner;
        private final String name;
        private final Type[] parameters;
        private final boolean isStatic;
        private final boolean isAbstract;
        private Type expectedReturnType;
        private final List<ExpectedReturnType> returnTypeExpectations = new ArrayList<>();
        private final Location location;

        public ExpectedFunction(JvmClass owner, String name, Type[] parameters, boolean isStatic, boolean isAbstract, Location location) {
            this.owner = owner;
            this.name = name;
            this.parameters = parameters;
            this.isStatic = isStatic;
            this.isAbstract = isAbstract;
            this.location = location;
        }

        @Override
        public void invoke(Object callArgs, Runnable paramInit) {
            throw new AssertionError("DEBUG");
        }

        @Override
        public Type returnType(PythonCompiler compiler) {
            return expectedReturnType;
        }

        @Override
        public JvmClass returnClass(PythonCompiler compiler) {
            return PythonCompiler.classCache.require(compiler, expectedReturnType);
        }

        @Override
        public Type[] parameterTypes(PythonCompiler compiler) {
            return parameters;
        }

        @Override
        public JvmClass[] parameterClasses(PythonCompiler compiler) {
            JvmClass[] classes = new JvmClass[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                classes[i] = PythonCompiler.classCache.require(compiler, parameters[i]);
            }
            return classes;
        }

        @Override
        public boolean isStatic() {
            return isStatic;
        }

        @Override
        public JvmClass ownerClass(PythonCompiler compiler) {
            return owner;
        }

        @Override
        public boolean isDynamicCall() {
            return false;
        }

        @Override
        public JvmClass owner(PythonCompiler compiler) {
            return owner;
        }

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
//            if (isStatic) {
//                compiler.writer.invokeStatic(owner.type(compiler).getInternalName(), name, Type.getMethodDescriptor(expectedReturnType, parameters), boxed);
//            } else {
//                compiler.writer.invokeVirtual(owner.type(compiler).getInternalName(), name, Type.getMethodDescriptor(expectedReturnType, parameters), boxed);
//            }

            compiler.writer.dynamicCall(name, Type.getMethodDescriptor(expectedReturnType, parameters));
        }

        @Override
        public Type type(PythonCompiler compiler) {
            return expectedReturnType;
        }

        @Override
        public Location location() {
            return location;
        }

        @Override
        public void expectReturnType(PythonCompiler compiler, JvmClass newExpectation, Location location) {
            JvmClass currentExpectation = PythonCompiler.classCache.require(compiler, this.expectedReturnType);
            if (newExpectation.equals(currentExpectation)) return;
            Stack<JvmClass> stack = new Stack<>();
            JvmClass testAgainst = newExpectation;
            boolean found = false;
            while (true) {
                if (testAgainst.doesInherit(compiler, currentExpectation)) {
                    found = true;
                    break;
                }
                JvmClass[] interfaces = testAgainst.interfaces(compiler);
                if (interfaces.length == 0) {
                    break;
                }
                stack.addAll(Arrays.asList(interfaces));
                testAgainst = stack.pop();
            }

            if (found) {
                this.expectedReturnType = testAgainst.type(compiler);
                this.returnTypeExpectations.add(new ExpectedReturnType(this, expectedReturnType, location));
                return;
            }
            testAgainst = newExpectation;
            while (true) {
                if (testAgainst.doesInherit(compiler, currentExpectation)) {
                    found = true;
                    break;
                }
                testAgainst = testAgainst.firstSuperClass(compiler);
                if (testAgainst == null) {
                    break;
                }
            }

            if (found) {
                this.expectedReturnType = testAgainst.type(compiler);
                this.returnTypeExpectations.add(new ExpectedReturnType(this, expectedReturnType, location));
                return;
            }

            this.expectedReturnType = Type.getObjectType("java/lang/Object");
            this.returnTypeExpectations.add(new ExpectedReturnType(this, expectedReturnType, location));
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
            throw new AssertionError("DEBUG");
        }

        public JvmClass owner() {
            return owner;
        }

        @Override
        public String name() {
            return name;
        }

        public Type[] parameters() {
            return parameters;
        }

        @Override
        public boolean isAbstract() {
            return isAbstract;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ExpectedFunction) obj;
            return Objects.equals(this.owner, that.owner) &&
                    Objects.equals(this.name, that.name) &&
                    Arrays.equals(this.parameters, that.parameters) &&
                    this.isStatic == that.isStatic &&
                    this.isAbstract == that.isAbstract;
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name, Arrays.hashCode(parameters), isStatic, isAbstract);
        }

        @Override
        public String toString() {
            return "ExpectedFunction[" +
                    "owner=" + owner + ", " +
                    "name=" + name + ", " +
                    "parameters=" + Arrays.toString(parameters) + ", " +
                    "isStatic=" + isStatic + ", " +
                    "isAbstract=" + isAbstract + ']';
        }

        public PyFunction create(PythonCompiler compiler, PyCompileClass cls, JvmClass returnType, PythonParser.Function_def_rawContext ctx) {
            JvmClass require = PythonCompiler.classCache.require(compiler, expectedReturnType);
            if (!returnType.equals(require)) {
                if (!require.doesInherit(compiler, returnType)) {
                    throw new CompilerException("");
                }
            }

            return new PyFunction(compiler, cls, name, parameters, returnType.type(compiler), compiler.getLocation(ctx), cls.isModule() || isStatic);
        }
    }
}
