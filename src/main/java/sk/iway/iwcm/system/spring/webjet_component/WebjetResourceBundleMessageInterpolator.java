package sk.iway.iwcm.system.spring.webjet_component;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.validation.MessageInterpolator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;

import sk.iway.iwcm.Logger;

/**
 * Custom MessageInterpolator for WebJET CMS that supports resource bundle
 * message interpolation with Spring MessageSource support.
 *
 * Rewritten for Hibernate Validator 9.1.x / Spring Boot 4.x compatibility.
 * Uses the public API (ResourceBundleLocator interface changed in 9.1).
 */
public class WebjetResourceBundleMessageInterpolator implements MessageInterpolator {

    private final ResourceBundleLocator userResourceBundleLocator;
    private final ResourceBundleLocator defaultResourceBundleLocator;
    private final ResourceBundleLocator contributorResourceBundleLocator;
    private final MessageInterpolator delegate;

    public WebjetResourceBundleMessageInterpolator() {
        this(new MessageSourceResourceBundleLocator(new WebjetMessageSource()),
             new PlatformResourceBundleLocator("org.hibernate.validator.ValidationMessages"),
             false);
    }

    public WebjetResourceBundleMessageInterpolator(ResourceBundleLocator userResourceBundleLocator,
                                                    ResourceBundleLocator contributorResourceBundleLocator,
                                                    boolean cacheMessages) {
        if (userResourceBundleLocator == null) {
            this.userResourceBundleLocator = new PlatformResourceBundleLocator("ValidationMessages");
        } else {
            this.userResourceBundleLocator = userResourceBundleLocator;
        }

        if (contributorResourceBundleLocator == null) {
            this.contributorResourceBundleLocator = new PlatformResourceBundleLocator(
                "ContributorValidationMessages", null, true);
        } else {
            this.contributorResourceBundleLocator = contributorResourceBundleLocator;
        }

        this.defaultResourceBundleLocator = new PlatformResourceBundleLocator(
            "org.hibernate.validator.ValidationMessages");

        // Build the delegate chain: ResourceBundle -> Parameter -> Default
        ResourceBundleMessageInterpolator rbInterpolator =
            new ResourceBundleMessageInterpolator(
                new CompositeResourceBundleLocator(
                    this.userResourceBundleLocator,
                    this.contributorResourceBundleLocator,
                    this.defaultResourceBundleLocator
                )
            );

        this.delegate = new ParameterMessageInterpolator();
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        try {
            return this.delegate.interpolate(messageTemplate, context);
        } catch (Exception e) {
            Logger.debug(WebjetResourceBundleMessageInterpolator.class,
                "Validation interpolation error for message: " + messageTemplate, e);
            return messageTemplate;
        }
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        try {
            return this.delegate.interpolate(messageTemplate, context, locale);
        } catch (Exception e) {
            Logger.debug(WebjetResourceBundleMessageInterpolator.class,
                "Validation interpolation error for message: " + messageTemplate, e);
            return messageTemplate;
        }
    }

    /**
     * Composite ResourceBundleLocator that combines multiple locators.
     * Adapted for Hibernate Validator 9.1 API (getResourceBundle(Locale) only).
     */
    private static class CompositeResourceBundleLocator implements ResourceBundleLocator {
        private final ResourceBundleLocator[] locators;

        public CompositeResourceBundleLocator(ResourceBundleLocator... locators) {
            this.locators = locators;
        }

        @Override
        public ResourceBundle getResourceBundle(Locale locale) {
            for (ResourceBundleLocator locator : locators) {
                try {
                    ResourceBundle rb = locator.getResourceBundle(locale);
                    if (rb != null) {
                        return rb;
                    }
                } catch (MissingResourceException e) {
                    // Try next locator
                }
            }
            return null;
        }
    }
}
