package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.util.PyImport;

import java.util.List;

public interface PyImportNode extends PyStatementNode {

    List<? extends PyImport> getImports();
}
