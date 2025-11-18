package python._core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface PyProperty {
    @Target({java.lang.annotation.ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Getter {
        String name();
    }

    @Target({java.lang.annotation.ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Setter {
        String name();
    }

    @Target({java.lang.annotation.ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Deleter {
        String name();
    }
}
