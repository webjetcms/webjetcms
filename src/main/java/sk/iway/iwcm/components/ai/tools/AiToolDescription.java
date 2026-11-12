package sk.iway.iwcm.components.ai.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Describes a mapped operation on an {@link AiTool} controller. The catalog appends the controller's shared context. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiToolDescription {
    /** Translation key or English text describing what the endpoint does. */
    String value();
}
