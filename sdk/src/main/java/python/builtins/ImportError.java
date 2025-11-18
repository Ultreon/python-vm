package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "ImportError", bases = {Exception.class}, module = Builtins.class)
public class ImportError extends RuntimeException {
    @PyConstructor(args = {"*value"})
    public ImportError(VMObject... message) {
        super(BaseException.translate(message));
    }
}
