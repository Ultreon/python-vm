package python._core;

public @interface PyFunction {
    String name();
    String[] args() default {};
}
