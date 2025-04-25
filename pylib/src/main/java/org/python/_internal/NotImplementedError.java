package org.python._internal;

import org.python.builtins.PyException;

public class NotImplementedError extends PyException {
    public NotImplementedError() {
        super("NotImplementedError");
    }
}
