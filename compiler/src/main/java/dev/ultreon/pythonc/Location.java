package dev.ultreon.pythonc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record Location(String file, int lineStart, int columnStart, int lineEnd, int columnEnd) {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_GRAY = "\u001B[37;1m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";

    public static final String ANSI_RESET = "\u001B[0m";

    public Location() {
        this("<unknown>", 0, 0, 0, 0);
    }

    @Override
    public String toString() {
        return file + ":" + lineStart + ":" + columnStart;
    }

    public String toAdvancedString() {
        Path resolve = Path.of(file);

        List<String> lines;
        StringBuilder builder = new StringBuilder();

        builder.append("\n").append(ANSI_PURPLE).append("File: ").append(ANSI_WHITE).append(resolve).append(":").append(ANSI_BLUE).append(lineStart).append(":").append(columnStart + 1).append(ANSI_RESET).append("\n");
        try {
            lines = Files.readAllLines(resolve);
        } catch (IOException e) {
            return builder.append(ANSI_RED).append("Cannot read file: ").append(file).append(" (").append(e.getMessage()).append(")").append(ANSI_RESET).append("\n").toString();
        }

        for (int i = Math.max(0, lineStart - 3); i <= Math.min(lineEnd + 1, lines.size() - 1); i++) {
            if (i < lineStart - 1) {
                builder.append(ANSI_GRAY);
                builder.append(lines.get(i)).append("\n");
            } else if (i == lineStart - 1 && i == lineEnd - 1) {
                builder.append(ANSI_GRAY);
                builder.append(lines.get(i), 0, columnStart);
                builder.append(ANSI_RED);
                builder.append(ANSI_UNDERLINE);
                builder.append(lines.get(i), columnStart, Math.min(columnEnd + 1, lines.get(i).length()));
                builder.append(ANSI_RESET);
                builder.append(ANSI_WHITE);
                builder.append(lines.get(i), Math.min(columnEnd + 1, lines.get(i).length()), lines.get(i).length());
                builder.append("\n");
            } else if (i == lineStart - 1) {
                builder.append(ANSI_GRAY);
                builder.append(lines.get(i), 0, columnStart);
                builder.append(ANSI_RED);
                builder.append(ANSI_UNDERLINE);
                builder.append(lines.get(i), columnStart, lines.get(i).length());
                builder.append(ANSI_RESET);
                builder.append(ANSI_GRAY);
                builder.append("\n");
            } else if (i < lineEnd - 1) {
                builder.append(ANSI_RED);
                builder.append(lines.get(i)).append("\n");
                builder.append(ANSI_GRAY);
            } else if (i == lineEnd - 1) {
                builder.append(lines.get(i), 0, Math.min(columnEnd, lines.get(i).length()));
                builder.append(ANSI_RED);
                builder.append(lines.get(i), Math.min(columnEnd + 1, lines.get(i).length()), lines.get(i).length());
                builder.append(ANSI_GRAY);
            } else {
                builder.append(ANSI_GRAY);
                builder.append(lines.get(i)).append("\n");
            }
        }

        builder.append(ANSI_RESET);

        return builder.toString();
    }
}
