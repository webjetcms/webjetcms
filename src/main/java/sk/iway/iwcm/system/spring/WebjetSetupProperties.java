package sk.iway.iwcm.system.spring;

import org.springframework.core.env.Environment;

final class WebjetSetupProperties {

    static final String ENABLED_PROPERTY = "webjet.setup.enabled";
    static final String TOKEN_PROPERTY = "webjet.setup.token";
    static final int MINIMUM_TOKEN_LENGTH = 16;

    private WebjetSetupProperties() {
    }

    static boolean isEnabled(Environment environment) {
        return environment.getProperty(ENABLED_PROPERTY, Boolean.class, false);
    }

    static String requireToken(Environment environment) {
        String token = environment.getProperty(TOKEN_PROPERTY);
        if (token == null || token.isBlank() || token.length() < MINIMUM_TOKEN_LENGTH) {
            throw new IllegalStateException("WebJET setup mode requires webjet.setup.token with at least "
                + MINIMUM_TOKEN_LENGTH + " characters");
        }
        return token;
    }
}
