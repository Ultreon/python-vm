package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.io.IOException;

import static dev.ultreon.pythonc.Location.ANSI_RED;
import static dev.ultreon.pythonc.Location.ANSI_RESET;

public record ExpectedFieldType(ExpectedField expectedField, Type expectedType,
                                Location location) {

    @Override
    public String toString() {
        return "Expected return type: " + expectedType + " (" + location + ")";
    }

    public String toAdvancedString() throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append(ANSI_RED).append("[ERROR]: ").append(ANSI_RESET).append("Expected return type: ").append(expectedType).append(ANSI_RESET);
        builder.append(ANSI_RESET).append(" (").append(location).append(ANSI_RESET).append(")");
        return location.toAdvancedString();
    }
}
