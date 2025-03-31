package dev.ultreon.pythonc

import dev.ultreon.pythonc.statement.PyDecorator

class PyDecorators {
    private final List<PyDecorator> decorators = new ArrayList<>()

    PyDecorators(List<PyDecorator> decorators) {
        this.decorators.addAll decorators
    }

    void add(PyDecorator decorator) {
        decorators.add(decorator)
    }

    List<PyDecorator> getDecorators() {
        return decorators
    }
}
