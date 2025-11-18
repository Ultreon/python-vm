package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "IOError", bases = {Exception.class}, module = Builtins.class)
public class IOError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public IOError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
