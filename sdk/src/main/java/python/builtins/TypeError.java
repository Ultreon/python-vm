package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "TypeError", bases = {Exception.class}, module = Builtins.class)
public class TypeError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public TypeError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
