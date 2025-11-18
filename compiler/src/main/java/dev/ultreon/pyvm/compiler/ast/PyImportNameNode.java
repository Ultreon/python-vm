package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.PyImport;
import dev.ultreon.pyvm.compiler.util.PyTypeImport;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class PyImportNameNode implements PyImportNode {
    private final PyDottedAsNamesNode moduleNames;

    public PyImportNameNode(PyDottedAsNamesNode moduleNames) {
        this.moduleNames = moduleNames;
    }

    @Override
    public List<? extends PyImport> getImports() {
        List<PyTypeImport> imports = new ArrayList<>();
        for (PyDottedAsNameNode dottedAsNameNode : this.moduleNames.getDottedAsNameNodes()) {
            imports.add(new PyTypeImport(dottedAsNameNode.getDottedName(), dottedAsNameNode.getAsName()));
        }
        return imports;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        for (PyImport import_ : this.getImports()) {
            import_.compile(method, context);
        }
    }
}
