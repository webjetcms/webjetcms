package sk.iway.iwcm.system.spring.webjet_component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Payload;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.junit.jupiter.api.Test;

class WebjetResourceBundleMessageInterpolatorTest {

    private final Map<String, String> messages = new ConcurrentHashMap<>(Map.of(
        "legacy.size", "Length must be between {min} and {max}",
        "legacy.alias", "legacy.alias.target",
        "legacy.alias.target", "Resolved alias",
        "legacy.cycle.one", "legacy.cycle.two",
        "legacy.cycle.two", "legacy.cycle.one",
        "jakarta.validation.constraints.NotBlank.message", "Required by WebJET",
        "dynamic.message", "First message"
    ));

    private final ResourceBundleLocator webjetLocator = locale -> new CodeAsDefaultResourceBundle(messages, locale);

    @Test
    void resolvesLegacyRawKeyBeforeInterpolatingConstraintParameters() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new LegacySizeBean("x")));

            assertEquals("Length must be between 2 and 4", message);
        }
    }

    @Test
    void resolvesLegacyRawKeyAliases() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new LegacyAliasBean(null)));

            assertEquals("Resolved alias", message);
        }
    }

    @Test
    void stopsResolvingCyclicLegacyRawKeyAliases() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new LegacyCycleBean(null)));

            assertEquals("legacy.cycle.one", message);
        }
    }

    @Test
    void resolvesWebjetOverrideForStandardConstraintMessage() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new NotBlankBean("")));

            assertEquals("Required by WebJET", message);
        }
    }

    @Test
    void resolvesConstraintContributorMessage() {
        ResourceBundleLocator contributorLocator = locale -> new TestResourceBundle(
            Map.of("contributor.message", "Contributed message"), locale, false
        );
        MessageInterpolator interpolator = new WebjetResourceBundleMessageInterpolator(
            webjetLocator, contributorLocator, false
        );

        try (ValidatorFactory factory = validatorFactory(interpolator)) {
            String message = onlyMessage(factory.getValidator().validate(new ContributorBean(null)));

            assertEquals("Contributed message", message);
        }
    }

    @Test
    void fallsBackToHibernateBundleWhenWebjetKeyIsMissing() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new MinBean(1)));

            assertTrue(message.contains("5"));
            assertFalse(message.contains("jakarta.validation.constraints.Min.message"));
        }
    }

    @Test
    void leavesUnknownRawMessageUnchanged() {
        WebjetResourceBundleMessageInterpolator interpolator = new WebjetResourceBundleMessageInterpolator(
            webjetLocator, null, false
        );

        String message = interpolator.interpolate("Literal message", new UnusedContext(), Locale.ENGLISH);

        assertEquals("Literal message", message);
    }

    @Test
    void treatsShowTextKeysOutputForMissingKeyAsMissing() {
        ResourceBundleLocator showTextKeysLocator = locale -> new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return "[" + key + "] " + key;
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        };
        WebjetResourceBundleMessageInterpolator interpolator = new WebjetResourceBundleMessageInterpolator(
            showTextKeysLocator, null, false
        );

        String message = interpolator.interpolate("missing.message", new UnusedContext(), Locale.ENGLISH);

        assertEquals("missing.message", message);
    }

    @Test
    void leavesUserSuppliedElLiteralForCustomViolationWithElDisabled() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new UnsafeTemplateBean("${validatedValue}")));

            assertEquals("Invalid email: ${validatedValue}", message);
        }
    }

    @Test
    void evaluatesExplicitlyEnabledExpressionVariables() {
        try (ValidatorFactory factory = validatorFactory()) {
            String message = onlyMessage(factory.getValidator().validate(new SafeExpressionBean("shown safely")));

            assertEquals("Invalid email: shown safely", message);
        }
    }

    @Test
    void doesNotCacheRequestDependentWebjetMessages() {
        try (ValidatorFactory factory = validatorFactory()) {
            Validator validator = factory.getValidator();

            assertEquals("First message", onlyMessage(validator.validate(new DynamicMessageBean(null))));

            messages.put("dynamic.message", "Second message");

            assertEquals("Second message", onlyMessage(validator.validate(new DynamicMessageBean(null))));
        }
    }

    @Test
    void usesLocalePassedToLocaleOverload() {
        ResourceBundleLocator localeLocator = locale -> new CodeAsDefaultResourceBundle(
            Map.of("locale.message", locale.getLanguage()), locale
        );
        WebjetResourceBundleMessageInterpolator interpolator = new WebjetResourceBundleMessageInterpolator(
            localeLocator, null, false
        );

        String message = interpolator.interpolate("locale.message", new UnusedContext(), Locale.forLanguageTag("sk"));

        assertEquals("sk", message);
    }

    private ValidatorFactory validatorFactory() {
        return validatorFactory(new WebjetResourceBundleMessageInterpolator(webjetLocator, null, false));
    }

    private ValidatorFactory validatorFactory(MessageInterpolator interpolator) {
        return Validation.byProvider(HibernateValidator.class)
            .configure()
            .messageInterpolator(interpolator)
            .customViolationExpressionLanguageFeatureLevel(ExpressionLanguageFeatureLevel.NONE)
            .buildValidatorFactory();
    }

    private static String onlyMessage(Set<? extends ConstraintViolation<?>> violations) {
        assertEquals(1, violations.size());
        return violations.iterator().next().getMessage();
    }

    private record LegacySizeBean(@Size(min = 2, max = 4, message = "legacy.size") String value) {
    }

    private record LegacyAliasBean(@NotNull(message = "legacy.alias") String value) {
    }

    private record LegacyCycleBean(@NotNull(message = "legacy.cycle.one") String value) {
    }

    private record NotBlankBean(@NotBlank String value) {
    }

    private record ContributorBean(@NotNull(message = "{contributor.message}") String value) {
    }

    private record MinBean(@Min(5) int value) {
    }

    private record UnsafeTemplateBean(@UnsafeTemplate String value) {
    }

    private record SafeExpressionBean(@SafeExpression String value) {
    }

    private record DynamicMessageBean(@NotNull(message = "{dynamic.message}") String value) {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = UnsafeTemplateValidator.class)
    private @interface UnsafeTemplate {

        String message() default "Invalid email";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class UnsafeTemplateValidator implements ConstraintValidator<UnsafeTemplate, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid email: " + value).addConstraintViolation();
            return false;
        }
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = SafeExpressionValidator.class)
    private @interface SafeExpression {

        String message() default "Invalid email";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class SafeExpressionValidator implements ConstraintValidator<SafeExpression, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.disableDefaultConstraintViolation();
            hibernateContext.addExpressionVariable("shownValue", value);
            hibernateContext.buildConstraintViolationWithTemplate("Invalid email: ${shownValue}")
                .enableExpressionLanguage(ExpressionLanguageFeatureLevel.VARIABLES)
                .addConstraintViolation();
            return false;
        }
    }

    private static class CodeAsDefaultResourceBundle extends TestResourceBundle {

        private CodeAsDefaultResourceBundle(Map<String, String> messages, Locale locale) {
            super(messages, locale, true);
        }
    }

    private static class TestResourceBundle extends ResourceBundle {

        private final Map<String, String> messages;
        private final Locale locale;
        private final boolean codeAsDefault;

        private TestResourceBundle(Map<String, String> messages, Locale locale, boolean codeAsDefault) {
            this.messages = messages;
            this.locale = locale;
            this.codeAsDefault = codeAsDefault;
        }

        @Override
        protected Object handleGetObject(String key) {
            return messages.getOrDefault(key, codeAsDefault ? key : null);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(messages.keySet());
        }

        @Override
        public Locale getLocale() {
            return locale;
        }
    }

    private static class UnusedContext implements MessageInterpolator.Context {

        @Override
        public jakarta.validation.metadata.ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
        }

        @Override
        public Object getValidatedValue() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }
}
