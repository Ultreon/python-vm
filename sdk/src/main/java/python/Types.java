package python;

import python._core.PyModuleMeta;
import python.types.ModuleType;

@PyModuleMeta(name = "types", source = "types.pyd")
public class Types extends ModuleType {
    public Types() {
        super();
    }
}
