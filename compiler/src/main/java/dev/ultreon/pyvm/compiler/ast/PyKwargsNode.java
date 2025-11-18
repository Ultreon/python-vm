package dev.ultreon.pyvm.compiler.ast;

import java.util.List;

public class PyKwargsNode implements PyNode {
    private final List<PyKeywordArgNode> kwargs;

    public PyKwargsNode(List<PyKeywordArgNode> kwargs) {
        this.kwargs = kwargs;
    }

    public List<PyKeywordArgNode> getKwargs() {
        return this.kwargs;
    }
}
