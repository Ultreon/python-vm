package python;

import python._core.Py;
import python._core.PyFunction;
import python._core.PyModuleMeta;
import python._core.VMObject;
import python.builtins.Dict;
import python.builtins.Str;
import python.types.ModuleType;

import java.util.Scanner;

@PyModuleMeta(name = "builtins", source = "builtins.pyd")
public class Builtins extends ModuleType {
    public static final Builtins INSTANCE = new Builtins();

    static {
        Py.registerModule(INSTANCE);
    }

    @PyFunction(name = "exec", args = {"code", "globals", "locals"})
    public VMObject exec(VMObject code, VMObject globals, VMObject locals) {
        throw Py.createNotImplementedError("exec");
    }

    @PyFunction(name = "input")
    public VMObject input() {
        return Sys.INSTANCE.getAttr("stdin").$().call(new Object[0], new Dict());
    }
}
