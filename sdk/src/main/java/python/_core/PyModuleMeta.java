package python._core;

import python.types.ModuleType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PyModuleMeta {
    String name();
    String source();
    Class<? extends ModuleType> parent() default ModuleType.class;
}
