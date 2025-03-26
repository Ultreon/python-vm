package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.io.IOException;

import static dev.ultreon.pythonc.Location.*;

public record ExpectedReturnType(CompileExpectations.ExpectedFunction expectedFunction, Type expectedReturnType,
                                 Location location) {

    @Override
    public String toString() {
        return "Expected return type: " + expectedReturnType + " (" + location + ")";
    }

    public String toAdvancedString() throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append(ANSI_RED).append("[ERROR]: ").append(ANSI_RESET).append("Expected return type: ").append(expectedReturnType).append(ANSI_RESET);
        builder.append(ANSI_RESET).append(" (").append(location).append(ANSI_RESET).append(")");
        return location.toAdvancedString();
    }
}
