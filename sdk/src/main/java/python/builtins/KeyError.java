package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "KeyError", bases = {Exception.class}, module = Builtins.class)
public class KeyError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public KeyError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
