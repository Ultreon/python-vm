package dev.ultreon.pythonc;

enum MemberContext {
    FIELD,
    VAR,
    FUNCTION
}

enum ErrorValue {
    Instance
}

enum NoneType {
    None

    @Override
    String toString() {
        return "NoneType";
    }
}

enum Unit {
    Instance
}
