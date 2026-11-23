package sk.iway.iwcm.components.ai.providers.local;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

/** Enables local AI components only when the optional webjet-ai-local library is installed. */
public final class LocalAiAvailableCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return ClassUtils.isPresent(
            "com.webjetcms.ai.provider.local.LocalEmbeddingModelProvider", context.getClassLoader()
        );
    }
}
