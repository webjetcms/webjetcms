package sk.iway.iwcm.system.spring;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;

import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import sk.iway.iwcm.Logger;

class WebjetBootstrapApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "webjetBootstrap";
    private static final String AUTO_CONFIGURATION_EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";
    private static final String[] SETUP_SECURITY_AUTO_CONFIGURATION_EXCLUSIONS = {
        ServletWebSecurityAutoConfiguration.class.getName(),
        SecurityFilterAutoConfiguration.class.getName(),
        UserDetailsServiceAutoConfiguration.class.getName()
    };

    private final WebjetBootstrapMode forcedMode;
    private final WebjetBootstrapModeDetector modeDetector;
    private final WebjetInitializationActions initializationActions;

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

        ServletContext servletContext = getServletContext(applicationContext);
        WebjetBootstrapState state;
        if (servletContext != null) {
            boolean initialized = initializationActions.initialize(servletContext);
            WebjetBootstrapMode mode = initialized ? WebjetBootstrapMode.PRODUCTION : WebjetBootstrapMode.SETUP;
            state = WebjetBootstrapState.initialized(mode, initialized);
        } else {
            WebjetBootstrapMode mode = forcedMode != null
                ? forcedMode
                : modeDetector.detect(applicationContext.getEnvironment());
            state = WebjetBootstrapState.pending(mode);
        }

        applicationContext.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource(PROPERTY_SOURCE_NAME, getBootstrapProperties(applicationContext, state))
        );
        applicationContext.getBeanFactory().registerSingleton(WebjetBootstrapState.BEAN_NAME, state);

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

    private Map<String, Object> getBootstrapProperties(ConfigurableApplicationContext applicationContext,
            WebjetBootstrapState state) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(WebjetBootstrapMode.PROPERTY_NAME, state.getMode().getPropertyValue());

        if (state.getMode() == WebjetBootstrapMode.SETUP) {
            Set<String> exclusions = new LinkedHashSet<>();
            String[] configuredExclusions = applicationContext.getEnvironment()
                .getProperty(AUTO_CONFIGURATION_EXCLUDE_PROPERTY, String[].class);
            if (configuredExclusions != null) {
                Collections.addAll(exclusions, configuredExclusions);
            }
            Collections.addAll(exclusions, SETUP_SECURITY_AUTO_CONFIGURATION_EXCLUSIONS);
            properties.put(AUTO_CONFIGURATION_EXCLUDE_PROPERTY, String.join(",", exclusions));
        }
        return properties;
    }
}
