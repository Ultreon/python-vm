package org.python._internal;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface PyProperty {
    @Retention(RetentionPolicy.RUNTIME)
    @interface Getter {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Setter {
        String value();
    }
}
