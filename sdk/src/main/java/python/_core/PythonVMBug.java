package python._core;

public class PythonVMBug extends Error {
    public PythonVMBug(String error, Exception cause) {
        super(error, cause);
    }

    public PythonVMBug(String error) {
        super(error);
    }
}
