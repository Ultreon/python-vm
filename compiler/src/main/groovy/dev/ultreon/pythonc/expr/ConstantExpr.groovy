package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.NoneType
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass

import static java.util.Objects.requireNonNull

class ConstantExpr extends PyExpression {
    private final Object value

    ConstantExpr(String value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(NoneType value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(int value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(long value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(float value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(double value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(char value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(byte value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(short value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(boolean value, Location location) {
        super(location)
        this.value = value
    }

    ConstantExpr(JvmClass value, Location location) {
        super(location)
        this.value = value
    }

    Object value() {
        return value
    }

    String stringValue() {
        return (String) value
    }

    int intValue() {
        return (int) value
    }

    long longValue() {
        return (long) value
    }

    float floatValue() {
        return (float) value
    }

    double doubleValue() {
        return (double) value
    }

    char charValue() {
        return (char) value
    }

    byte byteValue() {
        return (byte) value
    }

    short shortValue() {
        return (short) value
    }

    boolean booleanValue() {
        return (boolean) value
    }

    JvmClass classValue() {
        return value as JvmClass
    }

    def <T> T value(Class<T> clazz) {
        return clazz.cast(value)
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (requireNonNull(value) instanceof String) {
            String string = requireNonNull(value) as String
            writer.loadConstant string
        } else if (value instanceof Integer) {
            Integer integer = value as Integer
            writer.loadConstant integer
        } else if (value instanceof Boolean) {
            Boolean booleanValue = value as Boolean
            writer.loadConstant booleanValue
        } else if (value instanceof Long) {
            Long longValue = value as Long
            writer.loadConstant longValue
        } else if (value instanceof Float) {
            Float floatValue = value as Float
            writer.loadConstant floatValue
        } else if (value instanceof Double) {
            Double doubleValue = (Double) value
            writer.loadConstant doubleValue
        } else if (value instanceof Character) {
            Character charValue = (Character) value
            writer.loadConstant charValue
        } else if (value instanceof Byte) {
            Byte byteValue = (Byte) value
            writer.loadConstant byteValue
        } else if (value instanceof Short) {
            Short shortValue = (Short) value
            writer.loadConstant shortValue
        } else if (value instanceof JvmClass) {
            JvmClass jvmClass = (JvmClass) value
            writer.loadClass jvmClass
        } else if (value instanceof NoneType) {
            writer.pushNull()
        } else {
            throw new RuntimeException("Unknown constant type: " + value.class.name)
        }
    }

    @Override
    String toString() {
        if (value instanceof String) {
            return Location.ANSI_BLUE +  "\"" + value.replace("\\", Location.ANSI_RED + "\\\\" + Location.ANSI_BLUE).replace("\"", Location.ANSI_RED + "\\\"" + Location.ANSI_BLUE).replace("\n", Location.ANSI_RED + "\\n" + Location.ANSI_BLUE).replace("\r", Location.ANSI_RED + "\\r" + Location.ANSI_BLUE).replace("\t",  Location.ANSI_RED + "\\t" + Location.ANSI_BLUE).replace("\b", Location.ANSI_RED + "\\b" + Location.ANSI_BLUE).replace("\f", Location.ANSI_RED + "\\f" + Location.ANSI_BLUE) + "\"" + Location.ANSI_RESET
        }
        if (value instanceof JvmClass) {
            return Location.ANSI_YELLOW + value.name + Location.ANSI_RESET
        }
        if (value instanceof NoneType) {
            return Location.ANSI_RED + "None" + Location.ANSI_RESET
        }
        if (value instanceof Boolean) {
            return Location.ANSI_RED + (value ? "True" : "False") + Location.ANSI_RESET
        }
        if (value instanceof Number) {
            return Location.ANSI_GREEN + value + Location.ANSI_RESET
        }
        return value.toString()
    }
}
