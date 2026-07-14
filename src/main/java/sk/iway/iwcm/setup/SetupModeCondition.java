package sk.iway.iwcm.setup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;

/**
 * Spring Condition to detect if WebJET CMS is in Setup mode.
 *
 * SetupSpringConfig and Setup-related beans should only be loaded when
 * WebJET is NOT configured (Setup mode). Once configured (Production mode),
 * setup beans should not be available.
 *
 * Usage:
 *   @Conditional(SetupModeCondition.class)
 *   public class SetupSpringConfig { ... }
 */
public class SetupModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // WebJET is in Setup mode when it's NOT configured
        boolean isSetupMode = !InitServlet.isWebjetConfigured();
        Logger.debug(SetupModeCondition.class, "Setup mode detected: " + isSetupMode);
        return isSetupMode;
    }
}
