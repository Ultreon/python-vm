package dev.ultreon.pythonc;

import java.io.IOException;

import static dev.ultreon.pythonc.Location.*;

public class CompilerException extends RuntimeException {
    private String message;
    private Location location;

    public CompilerException(String message) {
        super(message);
        this.message = message;
    }

    public CompilerException(String message, Location location) {
        super(message);
        this.message = message;
        this.location = location;
    }

    public String toAdvancedString() throws IOException {
        StringBuilder builder = new StringBuilder();

        Location location = this.location;
        builder.append(ANSI_RED).append("[ERROR]: ").append(ANSI_WHITE).append(message).append(location == null ? "" : location.toAdvancedString()).append(ANSI_RESET);
        return builder.toString();
    }
}
