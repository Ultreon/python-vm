package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.NoneType;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.classes.JvmClass;

public class ConstantExpr extends PyExpression {
    private final Object value;

    public ConstantExpr(String value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(NoneType value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(int value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(long value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(float value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(double value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(char value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(byte value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(short value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(boolean value, Location location) {
        super(location);
        this.value = value;
    }

    public ConstantExpr(JvmClass value, Location location) {
        super(location);
        this.value = value;
    }

    public Object value() {
        return value;
    }

    public String stringValue() {
        return (String) value;
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public char charValue() {
        return (char) value;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public boolean booleanValue() {
        return (boolean) value;
    }

    public JvmClass classValue() {
        return (JvmClass) value;
    }

    public <T> T value(Class<T> clazz) {
        return clazz.cast(value);
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        switch (value) {
            case String string -> writer.loadConstant(string);
            case Integer integer -> writer.loadConstant(integer);
            case Boolean booleanValue -> writer.loadConstant(booleanValue);
            case Long longValue -> writer.loadConstant(longValue);
            case Float floatValue -> writer.loadConstant(floatValue);
            case Double doubleValue -> writer.loadConstant(doubleValue);
            case Character charValue -> writer.loadConstant(charValue);
            case Byte byteValue -> writer.loadConstant(byteValue);
            case Short shortValue -> writer.loadConstant(shortValue);
            case JvmClass jvmClass -> writer.loadClass(jvmClass);
            case NoneType noneType -> writer.pushNull();
            default -> throw new RuntimeException("Unknown constant type: " + value.getClass().getName());
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
