package pythonvm.builtins;

public class PyException extends Exception {
    public PyException(String message) {
        super(message);
    }
}
