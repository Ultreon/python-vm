package dev.ultreon.pythonc


import static dev.ultreon.pythonc.Location.*

class CompilerException extends RuntimeException {
    String message
    def location

    CompilerException(String message) {
        super(message)
        this.message = message
    }

    CompilerException(String message, Location location) {
        super(message)
        this.message = message
        this.location = location
    }

    String toAdvancedString() throws IOException {
        StringBuilder builder = new StringBuilder()

        Location location = this.location
        builder.append(ANSI_RED).append("[ERROR]: ").append(ANSI_WHITE).append(message).append(location == null ? "" : location.formattedText).append(ANSI_RESET)
        return builder.toString()
    }
}
