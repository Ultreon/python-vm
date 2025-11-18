package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "StopIteration", bases = {Exception.class}, module = Builtins.class)
public class StopIteration extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public StopIteration(VMObject... message) {
        super(BaseException.translate(message));
    }
}
