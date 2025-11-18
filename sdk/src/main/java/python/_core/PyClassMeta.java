package python._core;

import python.types.ModuleType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PyClassMeta {
    String name();

    Class<?>[] bases();

    Class<? extends ModuleType> module();
}
