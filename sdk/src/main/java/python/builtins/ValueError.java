package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "ValueError", bases = {Exception.class}, module = Builtins.class)
public class ValueError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public ValueError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
