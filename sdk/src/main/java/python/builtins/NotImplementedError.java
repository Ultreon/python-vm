package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "NotImplementedError", bases = {Exception.class}, module = Builtins.class)
public class NotImplementedError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public NotImplementedError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
