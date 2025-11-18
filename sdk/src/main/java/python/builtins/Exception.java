package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;

@PyClassMeta(name = "Exception", bases = {BaseException.class}, module = Builtins.class)
public class Exception extends RuntimeException {
    public Exception(String message) {
        super(message);
    }
}
