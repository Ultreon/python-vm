package org.python._internal;

public class NoUnsafeError extends Throwable {
    public NoUnsafeError() {
        super("Unsafe is not available on this platform");
    }
}
