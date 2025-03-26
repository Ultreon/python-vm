package org.python._internal;

import java.io.PrintStream;
import java.io.PrintWriter;

public class PythonException extends RuntimeException {
    public PythonException(String message) {
        super(message);
    }

    public PythonException(String message, Throwable cause) {
        super(message, cause);
    }

    public PythonException(Throwable cause) {
        super(cause);
    }

    public PythonException() {
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        String pyBackTrace = getPyTraceback();
        s.println(pyBackTrace);
    }

    @Override
    public void printStackTrace() {
        String pyBackTrace = getPyTraceback();
        System.err.println(pyBackTrace);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        String pyBackTrace = getPyTraceback();
        s.println(pyBackTrace);
    }

    private String getPyTraceback() {
        StackTraceElement[] stackTrace = getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append(pythonifyStackTrace(stackTrace, getClass(), getLocalizedMessage()));
        if (getCause() != null) {
            sb.append("\nWas caused by:\n");
            sb.append(pythonifyStackTrace(getCause().getStackTrace(), getCause().getClass(), getCause().getLocalizedMessage()).replace("\n", "\n  "));
        }
        for (Throwable t : getSuppressed()) {
            sb.append("\nSuppressed:\n");
            sb.append(pythonifyStackTrace(t.getStackTrace(), t.getClass(), t.getLocalizedMessage()).replace("\n", "\n  "));
        }
        return sb.toString();
    }

    private static String pythonifyStackTrace(StackTraceElement[] stackTrace, Class<?> clazz, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Traceback (most recent call last):\n");
        for (StackTraceElement ste : stackTrace) {
            int lineNumber = ste.getLineNumber();
            if (ste.getFileName().endsWith(".py")) {
                sb.append("  File \"classpath://").append(ste.getFileName()).append("\", line ").append(lineNumber < 0 ? "???" : lineNumber).append(", in ").append(ste.getClassName()).append("\n");
                sb.append("    <compiled method '").append(ste.getMethodName()).append("'").append(ste.isNativeMethod() ? " (Native Method)" : "").append(">\n");
                continue;
            }
            String replace = ste.getClassName().replace('.', '/');
            sb.append("  File \"classpath://").append(replace.substring(0, replace.lastIndexOf('/') + 1) + ste.getFileName()).append("\", line ").append(lineNumber < 0 ? "???" : lineNumber).append(", in ").append(ste.getClassName()).append("\n");
            sb.append("    <compiled method '").append(ste.getMethodName()).append("'").append(ste.isNativeMethod() ? " (Native Method)" : "").append(">\n");
        }
        String name = clazz.getName();
        if (clazz.getPackage().getName().equals("org.python.builtins")) {
            sb.append(clazz.getSimpleName()).append(": ").append(message);
        } else {
            sb.append(name).append(": ").append(message);
        }
        return sb.toString();
    }
}
