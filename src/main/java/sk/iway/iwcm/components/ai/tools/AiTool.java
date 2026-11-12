package sk.iway.iwcm.components.ai.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Publishes a concrete DataTable controller's mapped REST handlers in the AI tool catalog. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiTool {
    /** Translation key or English text describing the project area and records shared by this controller's tools. */
    String description() default "";

    /** Restrict discovery and invocation to GET requests. */
    boolean readOnly() default false;

    /** Java handler names to omit, including inherited handlers and all their mappings. */
    String[] exclude() default {};
}
