package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "RuntimeError", bases = {BaseException.class}, module = Builtins.class)
public class RuntimeError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public RuntimeError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
