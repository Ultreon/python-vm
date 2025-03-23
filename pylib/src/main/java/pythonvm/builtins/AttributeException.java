package pythonvm.builtins;

public class AttributeException extends Exception {
    public AttributeException(String name) {
        super(name);
    }
}
