package dev.ultreon.pythonc.functions.param

enum SpecialParameterType {
    NONE,
    SELF,
    ARGS,
    KWARGS

    static SpecialParameterType fromString(String s) {
        for (SpecialParameterType type : values()) {
            if (type.name().equalsIgnoreCase(s)) {
                return type
            }
        }
        return NONE
    }
}
