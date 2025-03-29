package dev.ultreon.pythonc;

import java.util.ArrayList;
import java.util.List;

public class PyDecorators {
    private final List<PyDecorator> decorators = new ArrayList<>();

    public PyDecorators(List<PyDecorator> decorators) {
        this.decorators.addAll(decorators);
    }

    public void add(PyDecorator decorator) {
        decorators.add(decorator);
    }

    public List<PyDecorator> getDecorators() {
        return decorators;
    }
}
