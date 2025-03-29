package dev.ultreon.pythonc.classes;

import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.functions.PyFunction;
import dev.ultreon.pythonc.statement.PyStatement;

import java.util.ArrayList;
import java.util.List;

public class PyModuleDefinition extends PyCompoundStatement {
    private final List<PyStatement> statements = new ArrayList<>();
    private final List<PyCompoundStatement> compoundStatements = new ArrayList<>();
    private final String name;
    private final ModulePath parent;
    private final Module type;
    public final PyClasses classes = new PyClasses();
    public final PyFunctions functions = new PyFunctions();

    public PyModuleDefinition(ModulePath path, Location location, Module type) {
        super(location);
        this.parent = path.getParent();
        this.name = path.getName();
        this.type = type;
    }

    public void addStatement(PyStatement statement) {
        if (statement instanceof PyClassDefinition classDefinition) {
            addClass(classDefinition.type);
            return;
        }
        if (statement instanceof PyFunction function) {
            addFunction(function);
            return;
        }

        statements.add(statement);
    }

    public void addCompoundStatement(PyCompoundStatement statement) {
        compoundStatements.add(statement);
    }

    public void addClass(LangClass clazz) {
        classes.add(clazz);
    }

    public void addFunction(PyFunction function) {
        functions.add(function);
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        compiler.moduleDefinition(type.type(), classNode -> {
            writeClassInit(compiler, writer);

            for (PyFunction function : functions) {
                function.writeFunction(compiler, writer);
                classNode.methods.add(function.node());
            }
        });

        for (LangClass clazz : classes) {
            clazz.writeClass(compiler, writer);
        }
    }

    private void writeClassInit(PythonCompiler compiler, JvmWriter writer) {
        compiler.classInit(methodNode -> {
            for (PyStatement statement : statements) {
                if (statement instanceof PyCompoundStatement) continue;
                statement.writeStatement(compiler, writer);
                compiler.checkPop(statement.location());
            }
        });
    }
}
