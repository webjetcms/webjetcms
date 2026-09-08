package sk.iway.iwcm.system.spring;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import sk.iway.iwcm.Logger;

class WebjetBootstrapApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "webjetBootstrap";
    private static final String AUTO_CONFIGURATION_EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";

    private final WebjetBootstrapMode forcedMode;
    private final WebjetBootstrapModeDetector modeDetector;
    private final WebjetInitializationActions initializationActions;

    WebjetBootstrapApplicationContextInitializer() {
        this(null, new WebjetBootstrapModeDetector(), new WebjetInitializationActions());
    }

    WebjetBootstrapApplicationContextInitializer(WebjetBootstrapModeDetector modeDetector,
            WebjetInitializationActions initializationActions) {
        this(null, modeDetector, initializationActions);
    }

    WebjetBootstrapApplicationContextInitializer(WebjetBootstrapMode forcedMode) {
        this(forcedMode, new WebjetBootstrapModeDetector(), new WebjetInitializationActions());
    }

    WebjetBootstrapApplicationContextInitializer(WebjetBootstrapMode forcedMode,
            WebjetBootstrapModeDetector modeDetector, WebjetInitializationActions initializationActions) {
        this.forcedMode = forcedMode;
        this.modeDetector = modeDetector;
        this.initializationActions = initializationActions;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        SpringAppInitializer.startDebugTimer();

        Environment environment = applicationContext.getEnvironment();
        ServletContext servletContext = getServletContext(applicationContext);
        boolean setupEnabled = WebjetSetupProperties.isEnabled(environment);
        if (setupEnabled) {
            WebjetSetupProperties.requireToken(environment);
        }

        WebjetBootstrapState state;
        WebjetBootstrapSpringConfiguration springConfiguration;
        if (servletContext != null) {
            boolean initialized = initializationActions.initialize(servletContext);
            WebjetBootstrapMode mode = resolveMode(setupEnabled, initialized);
            validateMode(mode, initialized);
            state = WebjetBootstrapState.initialized(mode, initialized);
            springConfiguration = initialized
                ? WebjetBootstrapSpringConfiguration.fromConstants(environment)
                : WebjetBootstrapSpringConfiguration.empty(environment);
        } else {
            WebjetBootstrapModeDetector.Detection detection;
            if (forcedMode != null) {
                detection = new WebjetBootstrapModeDetector.Detection(
                    forcedMode, WebjetBootstrapSpringConfiguration.empty(environment)
                );
            } else {
                detection = setupEnabled
                    ? WebjetBootstrapModeDetector.Detection.setup(environment)
                    : modeDetector.detect(environment);
            }
            state = WebjetBootstrapState.pending(detection.mode());
            springConfiguration = detection.springConfiguration();
        }

        applicationContext.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource(PROPERTY_SOURCE_NAME,
                getBootstrapProperties(environment, state, springConfiguration))
        );
        applicationContext.getBeanFactory().registerSingleton(WebjetBootstrapState.BEAN_NAME, state);
        applicationContext.getBeanFactory().registerSingleton(
            WebjetBootstrapSpringConfiguration.BEAN_NAME, springConfiguration
        );

        Logger.info(WebjetBootstrapApplicationContextInitializer.class,
            "WebJET bootstrap mode: " + state.getMode().getPropertyValue());
    }

    @Override
    public int getOrder() {
        // WAR deployment attaches ServletContext at HIGHEST_PRECEDENCE.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private ServletContext getServletContext(ConfigurableApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableWebApplicationContext webApplicationContext) {
            return webApplicationContext.getServletContext();
        }
        return null;
    }

    private WebjetBootstrapMode resolveMode(boolean setupEnabled, boolean initialized) {
        if (setupEnabled) {
            return WebjetBootstrapMode.SETUP;
        }
        if (initialized) {
            return WebjetBootstrapMode.PRODUCTION;
        }
        if (initializationActions.isLicenseRecoveryRequired()) {
            return WebjetBootstrapMode.LICENSE_RECOVERY;
        }
        return WebjetBootstrapMode.PRODUCTION;
    }

    private void validateMode(WebjetBootstrapMode mode, boolean initialized) {
        boolean productionMode = mode == WebjetBootstrapMode.PRODUCTION;
        if (productionMode != initialized) {
            initializationActions.cleanupAfterRejectedCoreInitialization(initialized);
            if (productionMode) {
                throw new WebjetBootstrapUnavailableException("core initialization did not complete");
            }
            throw new WebjetBootstrapModeMismatchException(mode, initialized);
        }
    }

    private Map<String, Object> getBootstrapProperties(Environment environment, WebjetBootstrapState state,
            WebjetBootstrapSpringConfiguration springConfiguration) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(WebjetBootstrapMode.PROPERTY_NAME, state.getMode().getPropertyValue());
        springConfiguration.addProperties(properties);
        if (state.getMode() != WebjetBootstrapMode.PRODUCTION) {
            Set<String> exclusions = new LinkedHashSet<>();
            List<String> configuredExclusions = Binder.get(environment)
                .bind(AUTO_CONFIGURATION_EXCLUDE_PROPERTY, Bindable.listOf(String.class))
                .orElseGet(List::of);
            exclusions.addAll(configuredExclusions);
            exclusions.add(SecurityFilterAutoConfiguration.class.getName());
            properties.put(AUTO_CONFIGURATION_EXCLUDE_PROPERTY, String.join(",", exclusions));
        }
        return properties;
    }
}
