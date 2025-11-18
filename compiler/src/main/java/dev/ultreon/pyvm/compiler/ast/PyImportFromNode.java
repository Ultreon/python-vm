package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.PyFromImport;
import dev.ultreon.pyvm.compiler.util.PyImport;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class PyImportFromNode implements PyImportNode, PyImport {
    private String fromName;
    private PyImportFromAsNameNode[] importName;

    public PyImportFromNode(String fromName, String importName) {
        this.fromName = fromName;
        this.importName = new PyImportFromAsNameNode[] { new PyImportFromAsNameNode(importName, null) };
    }

    public PyImportFromNode(PyDottedNameNode dottedName, PyImportFromTargetsNode importFromTargets) {
        this.fromName = dottedName.getName();
        this.importName = importFromTargets.getAsNameNodes();
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.addImport(this.fromName, this);
        List<? extends PyImport> imports = getImports();
        for(PyImport imp : imports) {
            imp.compile(method, context);
        }
    }

    @Override
    public List<? extends PyImport> getImports() {
        List<PyImport> imports = new java.util.ArrayList<>();
        for (PyImportFromAsNameNode importName : this.importName) {
            imports.add(new PyFromImport(this.fromName, importName.getName(), importName.getAsName()));
        }
        return imports;
    }
}
