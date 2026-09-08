package sk.iway.iwcm.setup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.spring.WebjetBootstrapMode;

/**
 * Compatibility condition for setup-only configuration.
 *
 * @deprecated Prefer {@code @ConditionalOnProperty} with
 * {@link WebjetBootstrapMode#PROPERTY_NAME}. The bootstrap property is available
 * before Spring parses bean definitions.
 */
@Deprecated(forRemoval = false)
public class SetupModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean setupMode = WebjetBootstrapMode.SETUP_VALUE.equals(
            context.getEnvironment().getProperty(WebjetBootstrapMode.PROPERTY_NAME)
        );
        Logger.debug(SetupModeCondition.class, "Setup mode detected: " + setupMode);
        return setupMode;
    }
}
