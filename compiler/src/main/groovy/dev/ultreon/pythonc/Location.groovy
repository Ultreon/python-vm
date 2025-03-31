package dev.ultreon.pythonc


import java.nio.file.Files
import java.nio.file.Path

final class Location {
    public static final String ANSI_BLACK = "\u001B[30m"
    public static final String ANSI_RED = "\u001B[31m"
    public static final String ANSI_GREEN = "\u001B[32m"
    public static final String ANSI_YELLOW = "\u001B[33m"
    public static final String ANSI_BLUE = "\u001B[34m"
    public static final String ANSI_PURPLE = "\u001B[35m"
    public static final String ANSI_CYAN = "\u001B[36m"
    public static final String ANSI_WHITE = "\u001B[37m"
    public static final String ANSI_GRAY = "\u001B[37;1m"
    public static final String ANSI_BRIGHT_GRAY = "\u001B[90m"
    public static final String ANSI_BRIGHT_RED = "\u001B[91m"
    public static final String ANSI_BRIGHT_GREEN = "\u001B[92m"
    public static final String ANSI_BRIGHT_YELLOW = "\u001B[93m"
    public static final String ANSI_BRIGHT_BLUE = "\u001B[94m"
    public static final String ANSI_BRIGHT_PURPLE = "\u001B[95m"
    public static final String ANSI_BRIGHT_CYAN = "\u001B[96m"
    public static final String ANSI_BRIGHT_WHITE = "\u001B[97m"
    public static final String ANSI_DEFAULT = "\u001B[39m"

    public static final String ANSI_BOLD = "\u001B[1m"
    public static final String ANSI_DIM = "\u001B[2m"
    public static final String ANSI_ITALIC = "\u001B[3m"
    public static final String ANSI_UNDERLINE = "\u001B[4m"
    public static final String ANSI_BLINK = "\u001B[5m"
    public static final String ANSI_RAPID_BLINK = "\u001B[6m"
    public static final String ANSI_REVERSE = "\u001B[7m"
    public static final String ANSI_CONCEAL = "\u001B[8m"
    public static final String ANSI_STRIKETHROUGH = "\u001B[9m"
    public static final String ANSI_NORMAL = "\u001B[22m"

    public static final String ANSI_RESET = "\u001B[0m"
    public static final Location BUILTIN = new Location("<builtin>", 0, 0, 0, 0)
    public static final Location JAVA = new Location("<java>", 0, 0, 0, 0)
    private final String file
    private final int lineStart
    private final int columnStart
    private final int lineEnd
    private final int columnEnd

    Location(String file, int lineStart, int columnStart, int lineEnd, int columnEnd) {
        this.file = file
        this.lineStart = lineStart
        this.columnStart = columnStart
        this.lineEnd = lineEnd
        this.columnEnd = columnEnd
    }

    Location() {
        this("<unknown>", 0, 0, 0, 0)
    }

    @Override
    String toString() {
        return file + ":" + lineStart + ":" + columnStart
    }

    String getFormattedText() {
        Path resolve = Path.of(file)

        List<String> lines
        StringBuilder builder = new StringBuilder()

        builder.append("\n").append(ANSI_PURPLE).append("File: ").append(ANSI_WHITE).append(resolve).append(":").append(ANSI_BLUE).append(lineStart).append(":").append(columnStart + 1).append(ANSI_RESET).append("\n")
        try {
            lines = Files.readAllLines(resolve)
        } catch (IOException e) {
            return builder.append(ANSI_RED).append("Cannot read file: ").append(file).append(" (").append(e.message).append(")").append(ANSI_RESET).append("\n").toString()
        }

        for (int i = Math.max(0, lineStart - 3); i <= Math.min(lineEnd + 1, lines.size() - 1); i++) {
            if (i < lineStart - 1) {
                builder.append(ANSI_GRAY)
                builder.append(lines.get(i)).append("\n")
            } else if (i == lineStart - 1 && i == lineEnd - 1) {
                builder.append(ANSI_GRAY)
                builder.append(lines.get(i), 0, columnStart)
                builder.append(ANSI_RED)
                builder.append(ANSI_UNDERLINE)
                builder.append(lines.get(i), columnStart, Math.min(columnEnd + 1, lines.get(i).length()))
                builder.append(ANSI_RESET)
                builder.append(ANSI_WHITE)
                builder.append(lines.get(i), Math.min(columnEnd + 1, lines.get(i).length()), lines.get(i).length())
                builder.append("\n")
            } else if (i == lineStart - 1) {
                builder.append(ANSI_GRAY)
                builder.append(lines.get(i), 0, columnStart)
                builder.append(ANSI_RED)
                builder.append(ANSI_UNDERLINE)
                builder.append(lines.get(i), columnStart, lines.get(i).length())
                builder.append(ANSI_RESET)
                builder.append(ANSI_GRAY)
                builder.append("\n")
            } else if (i < lineEnd - 1) {
                builder.append(lines.get(i).replaceAll(/(^ *)/, "\$1" + ANSI_RED + ANSI_UNDERLINE)).append("\n")
                builder.append(ANSI_RESET)
                builder.append(ANSI_GRAY)
            } else if (i == lineEnd - 1) {
                builder.append(lines.get(i).replaceAll(/(^ *)/, "\$1" + ANSI_RED + ANSI_UNDERLINE), 0, Math.min(columnEnd, lines.get(i).length()))
                builder.append(ANSI_RESET)
                builder.append(ANSI_GRAY)
                builder.append(lines.get(i), Math.min(columnEnd, lines.get(i).length()), lines.get(i).length())
            } else {
                builder.append(ANSI_GRAY)
                builder.append(lines.get(i)).append("\n")
            }
        }

        builder.append(ANSI_RESET)

        return builder.toString()
    }

    Location firstLine() {
        return new Location(file, lineStart, 0, lineStart, Integer.MAX_VALUE)
    }

    boolean isUnknown() {
        return !file.startsWith("/") && !file.startsWith("\\")
    }

    String file() {
        return file
    }

    int getLineStart() {
        return lineStart
    }

    int getColumnStart() {
        return columnStart
    }

    int getLineEnd() {
        return lineEnd
    }

    int getColumnEnd() {
        return columnEnd
    }

    @Override
    boolean equals(Object obj) {
        if (obj == null || obj.class != this.class) return false
        var that = (Location) obj
        return Objects.equals(this.file, that.file) &&
                this.lineStart == that.lineStart &&
                this.columnStart == that.columnStart &&
                this.lineEnd == that.lineEnd &&
                this.columnEnd == that.columnEnd
    }

    @Override
    int hashCode() {
        return Objects.hash(file, lineStart, columnStart, lineEnd, columnEnd)
    }

}
