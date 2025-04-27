package org.python._internal;

import java.io.File;
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
        try {
            StackTraceElement[] stackTrace = getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append(pythonifyStackTrace(stackTrace, getClass(), getLocalizedMessage()));
            Throwable cause = getCause();
            while (cause != null) {
                sb.append("\nWas caused by:\n");
                sb.append(pythonifyStackTrace(cause.getStackTrace(), cause.getClass(), cause.getLocalizedMessage()).replace("\n", "\n  "));

                cause = cause.getCause();
                for (Throwable t : getSuppressed()) {
                    sb.append("\nSuppressed:\n");
                    sb.append(pythonifyStackTrace(t.getStackTrace(), cause.getClass(), t.getLocalizedMessage()).replace("\n", "\n  "));
                }
            }
            for (Throwable t : getSuppressed()) {
                sb.append("\nSuppressed:\n");
                sb.append(pythonifyStackTrace(t.getStackTrace(), t.getClass(), t.getLocalizedMessage()).replace("\n", "\n  "));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR: " + e;
        }
    }

    private static String pythonifyStackTrace(StackTraceElement[] stackTrace, Class<?> clazz, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Traceback (most recent call last):\n");
        int i = 0;
        int max = stackTrace.length - 1;
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().startsWith("org.python._internal") && !System.getProperty("dev.ultreon.pythonvm.verbose", "0").equals("1")) {
                max--;
                continue;
            }
            int lineNumber = ste.getLineNumber();
            String methodName = ste.getMethodName();
            String[] classNameElements = ste.getClassName().split("\\.");
            String simpleName = classNameElements[classNameElements.length - 1];
            String s = File.separator;
            if (getFileName(ste).endsWith(".py")) {
                if (methodName.startsWith("-def-")) {
                    methodName = methodName.substring(6);
                } else {
                    if (i == max - 1) {
                        methodName = "<ref>" + methodName;
                    } else {
                        max--;
                        continue;
                    }
                }
                sb.append("  File \"").append(s).append(getFileName(ste)).append("\", line ").append(lineNumber <= 0 ? "???" : lineNumber).append(", in ").append(methodName.equals("<clinit>") ? simpleName : methodName).append("\n");
                max--;
                continue;
            }
            if (methodName.startsWith("-def-")) {
                methodName = methodName.substring(6);
            }
            String replace = ste.getClassName().replace('.', s.charAt(0));
            sb.append("  File \"").append(s).append(replace, 0, replace.lastIndexOf(s) + 1).append(getFileName(ste)).append("\", line ").append(lineNumber < 0 ? "???" : lineNumber).append(", in ").append(methodName).append("\n");
            i++;
        }
        String name = clazz.getName();
        if (clazz.getPackage().getName().equals("org.python.builtins")) {
            sb.append(clazz.getSimpleName()).append(": ").append(message);
        } else {
            sb.append(name).append(": ").append(message);
        }
        return sb.toString();
    }

    private static String getFileName(StackTraceElement ste) {
        String fileName = ste.getFileName();
        String[] className = ste.getClassName().split("\\.");
        return fileName == null ? className[className.length - 1] : fileName;
    }
}
