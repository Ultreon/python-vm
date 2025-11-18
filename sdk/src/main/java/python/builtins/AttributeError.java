package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "AttributeError", bases = {Exception.class}, module = Builtins.class)
public class AttributeError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public AttributeError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
