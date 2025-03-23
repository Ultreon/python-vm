package pythonvm.builtins;

import _pythonvm.PythonException;

public class TypeException extends PythonException {
    public TypeException(String message) {
        super(message);
    }
}
