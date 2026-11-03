package sk.iway.iwcm.system.spring.webjet_component;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import jakarta.validation.MessageInterpolator;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;

public class WebjetResourceBundleMessageInterpolator implements MessageInterpolator {

    private static final int MAX_RAW_KEY_RESOLUTION_DEPTH = 32;

    private final Locale defaultLocale;
    private final ResourceBundleLocator userResourceBundleLocator;
    private final MessageInterpolator delegate;

    public WebjetResourceBundleMessageInterpolator() {
        this(
            new MessageSourceResourceBundleLocator(new WebjetMessageSource()),
            null,
            false
        );
    }

    public WebjetResourceBundleMessageInterpolator(
        ResourceBundleLocator userResourceBundleLocator,
        ResourceBundleLocator contributorResourceBundleLocator,
        boolean cacheMessages
    ) {
        this.defaultLocale = Locale.getDefault();

        ResourceBundleLocator resolvedUserResourceBundleLocator = userResourceBundleLocator;
        if (resolvedUserResourceBundleLocator == null) {
            resolvedUserResourceBundleLocator = new PlatformResourceBundleLocator("ValidationMessages");
        }
        // WebjetMessageSource returns the key itself when it is missing, while Hibernate Validator
        // requires a missing lookup so it can continue with contributor and default bundles.
        this.userResourceBundleLocator = new MissingAwareResourceBundleLocator(resolvedUserResourceBundleLocator);

        ResourceBundleLocator resolvedContributorResourceBundleLocator = contributorResourceBundleLocator;
        if (resolvedContributorResourceBundleLocator == null) {
            resolvedContributorResourceBundleLocator = new PlatformResourceBundleLocator(
                "ContributorValidationMessages", null, true
            );
        }

        this.delegate = new ResourceBundleMessageInterpolator(
            this.userResourceBundleLocator,
            resolvedContributorResourceBundleLocator,
            cacheMessages
        );
    }

    @Override
    public String interpolate(String message, Context context) {
        return delegate.interpolate(resolveRawWebjetMessage(message, defaultLocale), context);
    }

    @Override
    public String interpolate(String message, Context context, Locale locale) {
        return delegate.interpolate(resolveRawWebjetMessage(message, locale), context, locale);
    }

    public String interpolate(Context context, Locale locale, String message) {
        return delegate.interpolate(message, context, locale);
    }

    private String resolveRawWebjetMessage(String message, Locale locale) {
        ResourceBundle resourceBundle = userResourceBundleLocator.getResourceBundle(locale);
        if (resourceBundle == null) {
            return message;
        }

        String resolvedMessage = message;
        Set<String> resolvedKeys = new HashSet<>();

        for (int i = 0; i < MAX_RAW_KEY_RESOLUTION_DEPTH && resolvedKeys.add(resolvedMessage); i++) {
            try {
                resolvedMessage = resourceBundle.getString(resolvedMessage);
            } catch (MissingResourceException ex) {
                break;
            }
        }

        return resolvedMessage;
    }

    private static final class MissingAwareResourceBundleLocator implements ResourceBundleLocator {

        private final ResourceBundleLocator delegate;

        private MissingAwareResourceBundleLocator(ResourceBundleLocator delegate) {
            this.delegate = delegate;
        }

        @Override
        public ResourceBundle getResourceBundle(Locale locale) {
            ResourceBundle resourceBundle = delegate.getResourceBundle(locale);
            if (resourceBundle == null) {
                return null;
            }
            return new MissingAwareResourceBundle(resourceBundle);
        }
    }

    private static final class MissingAwareResourceBundle extends ResourceBundle {

        private final ResourceBundle delegate;

        private MissingAwareResourceBundle(ResourceBundle delegate) {
            this.delegate = delegate;
        }

        @Override
        protected Object handleGetObject(String key) {
            try {
                Object value = delegate.getObject(key);
                return isMissingValue(key, value) ? null : value;
            } catch (MissingResourceException ex) {
                return null;
            }
        }

        @Override
        public boolean containsKey(String key) {
            return handleGetObject(key) != null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }

        @Override
        public Locale getLocale() {
            return delegate.getLocale();
        }

        private boolean isMissingValue(String key, Object value) {
            if (key.equals(value)) {
                return true;
            }
            return value instanceof String stringValue && stringValue.equals("[" + key + "] " + key);
        }
    }
}
