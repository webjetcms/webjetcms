package sk.iway.iwcm.components.ai.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Describes inputs read inside a REST handler, including handlers inherited by the annotated controller. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AiToolEndpoint.Endpoints.class)
public @interface AiToolEndpoint {
    /** Java name of the Spring-mapped method. */
    String method();
    Query[] query() default {};
    /** Dot-separated JSON paths required on the complete incoming entity. */
    String[] requiredBody() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Query {
        String name();
        Class<?> type() default int.class;
        String description();
        boolean required() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Endpoints {
        AiToolEndpoint[] value();
    }
}
