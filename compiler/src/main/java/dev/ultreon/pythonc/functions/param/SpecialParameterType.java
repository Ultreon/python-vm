package dev.ultreon.pythonc.functions.param;

public enum SpecialParameterType {
    NONE,
    SELF,
    ARGS,
    KWARGS;

    public static SpecialParameterType fromString(String s) {
        for (SpecialParameterType type : SpecialParameterType.values()) {
            if (type.name().equalsIgnoreCase(s)) {
                return type;
            }
        }
        return NONE;
    }
}
