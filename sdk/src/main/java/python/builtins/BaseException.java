package python.builtins;

import python.Builtins;
import python._core.PyClassMeta;
import python._core.PyConstructor;
import python._core.VMObject;

@PyClassMeta(name = "BaseException", bases = {Object.class}, module = Builtins.class)
public class BaseException extends RuntimeException {
    @PyConstructor(args = {"*values"})
    public BaseException(VMObject... values) {
        super(translate(values));
    }

    public static String translate(VMObject[] values) {
        StringBuilder sb = new StringBuilder();
        for (VMObject value : values) {
            sb.append(value.__str__().$java());
        }
        return sb.toString();
    }
}
