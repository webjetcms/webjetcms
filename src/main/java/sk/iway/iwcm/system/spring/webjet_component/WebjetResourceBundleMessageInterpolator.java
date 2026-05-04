package sk.iway.iwcm.system.spring.webjet_component;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.el.ELManager;
import jakarta.el.ExpressionFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ValidationException;

import org.hibernate.validator.internal.engine.messageinterpolation.InterpolationTerm;
import org.hibernate.validator.internal.engine.messageinterpolation.InterpolationTermType;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.MessageDescriptorFormatException;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.Token;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenCollector;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenIterator;
import org.hibernate.validator.internal.util.ConcurrentReferenceHashMap;
import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;
import org.hibernate.validator.internal.util.actions.GetClassLoader;
import org.hibernate.validator.internal.util.actions.SetContextClassLoader;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;

import sk.iway.iwcm.Logger;

public class WebjetResourceBundleMessageInterpolator implements MessageInterpolator {
    private boolean cachingEnabled = false;
    private ExpressionFactory expressionFactory;

    private static final Log LOG = LoggerFactory.make(MethodHandles.lookup());
    private final Locale defaultLocale;
    private final ResourceBundleLocator userResourceBundleLocator;
    private final ResourceBundleLocator defaultResourceBundleLocator;
    private final ResourceBundleLocator contributorResourceBundleLocator;
    private final ConcurrentReferenceHashMap<String, List<Token>> tokenizedParameterMessages;
    private final ConcurrentReferenceHashMap<String, List<Token>> tokenizedELMessages;
    private static final Pattern LEFT_BRACE = Pattern.compile("\\{", 16);
    private static final Pattern RIGHT_BRACE = Pattern.compile("\\}", 16);
    private static final Pattern SLASH = Pattern.compile("\\\\", 16);
    private static final Pattern DOLLAR = Pattern.compile("\\$", 16);

    public WebjetResourceBundleMessageInterpolator() {
        this(new MessageSourceResourceBundleLocator( new WebjetMessageSource()), new PlatformResourceBundleLocator("org.hibernate.validator.ValidationMessages"), false);
        this.expressionFactory = buildExpressionFactory();
    }

    public WebjetResourceBundleMessageInterpolator(ResourceBundleLocator userResourceBundleLocator, ResourceBundleLocator contributorResourceBundleLocator, boolean cacheMessages) {
        this.defaultLocale = Locale.getDefault();
        if (userResourceBundleLocator == null) {
            this.userResourceBundleLocator = new PlatformResourceBundleLocator("ValidationMessages");
        } else {
            this.userResourceBundleLocator = userResourceBundleLocator;
        }

        if (contributorResourceBundleLocator == null) {
            this.contributorResourceBundleLocator = new PlatformResourceBundleLocator("ContributorValidationMessages", (ClassLoader)null, true);
        } else {
            this.contributorResourceBundleLocator = contributorResourceBundleLocator;
        }

        this.defaultResourceBundleLocator = new PlatformResourceBundleLocator("org.hibernate.validator.ValidationMessages");
        this.cachingEnabled = cacheMessages;
        if (this.cachingEnabled) {
            this.tokenizedParameterMessages = new ConcurrentReferenceHashMap<>(100, 0.75F, 16, ConcurrentReferenceHashMap.ReferenceType.SOFT, ConcurrentReferenceHashMap.ReferenceType.SOFT, EnumSet.noneOf(ConcurrentReferenceHashMap.Option.class));
            this.tokenizedELMessages = new ConcurrentReferenceHashMap<>(100, 0.75F, 16, ConcurrentReferenceHashMap.ReferenceType.SOFT, ConcurrentReferenceHashMap.ReferenceType.SOFT, EnumSet.noneOf(ConcurrentReferenceHashMap.Option.class));
        } else {
            this.tokenizedParameterMessages = null;
            this.tokenizedELMessages = null;
        }
    }

    public String interpolate(String message, Context context) {
        String interpolatedMessage = message;

        try {
            interpolatedMessage = this.interpolateMessage(message, context, this.defaultLocale);
        } catch (MessageDescriptorFormatException var5) {
            LOG.warn(var5.getMessage());
        }

        return interpolatedMessage;
    }

    public String interpolate(String message, Context context, Locale locale) {
        String interpolatedMessage = message;

        try {
            interpolatedMessage = this.interpolateMessage(message, context, locale);
        } catch (ValidationException var6) {
            LOG.warn(var6.getMessage());
        }

        return interpolatedMessage;
    }

    private String interpolateMessage(String message, Context context, Locale locale) throws MessageDescriptorFormatException {
        String resolvedMessage = this.resolveMessage(message, locale);

        if (resolvedMessage.indexOf(123) > -1) {
            resolvedMessage = this.interpolateExpression(new TokenIterator(this.getParameterTokens(resolvedMessage, this.tokenizedParameterMessages, InterpolationTermType.PARAMETER)), context, locale);
            resolvedMessage = this.interpolateExpression(new TokenIterator(this.getParameterTokens(resolvedMessage, this.tokenizedELMessages, InterpolationTermType.EL)), context, locale);
        }

        resolvedMessage = this.replaceEscapedLiterals(resolvedMessage);
        return resolvedMessage;
    }

    private List<Token> getParameterTokens(String resolvedMessage, ConcurrentReferenceHashMap<String, List<Token>> cache, InterpolationTermType termType) {
        return this.cachingEnabled
                ?
                cache.computeIfAbsent(resolvedMessage, rm -> (new TokenCollector(resolvedMessage, termType)).getTokenList())
                :
                (new TokenCollector(resolvedMessage, termType)).getTokenList();
    }

    private String resolveMessage(String message, Locale locale) {
        String resolvedMessage = message;
        ResourceBundle userResourceBundle = this.userResourceBundleLocator.getResourceBundle(locale);
        ResourceBundle constraintContributorResourceBundle = this.contributorResourceBundleLocator.getResourceBundle(locale);
        ResourceBundle defaultResourceBundle = this.defaultResourceBundleLocator.getResourceBundle(locale);
        boolean evaluatedDefaultBundleOnce = false;

        while(true) {
            String userBundleResolvedMessage = userResourceBundle.getString(resolvedMessage);

            if (this.hasReplacementNotTakenPlace(userBundleResolvedMessage, resolvedMessage)) {
                userBundleResolvedMessage = this.interpolateBundleMessage(resolvedMessage, constraintContributorResourceBundle, locale, true);
            }

            if (evaluatedDefaultBundleOnce && this.hasReplacementNotTakenPlace(userBundleResolvedMessage, resolvedMessage)) {
                return resolvedMessage;
            }

            resolvedMessage = this.interpolateBundleMessage(userBundleResolvedMessage, defaultResourceBundle, locale, false);
            evaluatedDefaultBundleOnce = true;
        }
    }

    private String replaceEscapedLiterals(String resolvedMessage) {
        if (resolvedMessage.indexOf(92) > -1) {
            resolvedMessage = LEFT_BRACE.matcher(resolvedMessage).replaceAll("{");
            resolvedMessage = RIGHT_BRACE.matcher(resolvedMessage).replaceAll("}");
            resolvedMessage = SLASH.matcher(resolvedMessage).replaceAll(Matcher.quoteReplacement("\\"));
            resolvedMessage = DOLLAR.matcher(resolvedMessage).replaceAll(Matcher.quoteReplacement("$"));
        }

        return resolvedMessage;
    }

    private boolean hasReplacementNotTakenPlace(String origMessage, String newMessage) {
        return origMessage.equals(newMessage);
    }

    private String interpolateBundleMessage(String message, ResourceBundle bundle, Locale locale, boolean recursive) throws MessageDescriptorFormatException {
        TokenCollector tokenCollector = new TokenCollector(message, InterpolationTermType.PARAMETER);
        TokenIterator tokenIterator = new TokenIterator(tokenCollector.getTokenList());

        while(tokenIterator.hasMoreInterpolationTerms()) {
            String term = tokenIterator.nextInterpolationTerm();
            String resolvedParameterValue = this.resolveParameter(term, bundle, locale, recursive);
            tokenIterator.replaceCurrentInterpolationTerm(resolvedParameterValue);
        }

        return tokenIterator.getInterpolatedMessage();
    }

    private String interpolateExpression(TokenIterator tokenIterator, Context context, Locale locale) throws MessageDescriptorFormatException {
        while(tokenIterator.hasMoreInterpolationTerms()) {
            String term = tokenIterator.nextInterpolationTerm();
            String resolvedExpression = this.interpolate(context, locale, term);
            tokenIterator.replaceCurrentInterpolationTerm(resolvedExpression);
        }

        return tokenIterator.getInterpolatedMessage();
    }

    private String resolveParameter(String parameterName, ResourceBundle bundle, Locale locale, boolean recursive) throws MessageDescriptorFormatException {
        String parameterValue;
        try {
            if (bundle != null) {
                parameterValue = bundle.getString(this.removeCurlyBraces(parameterName));
                if (recursive) {
                    parameterValue = this.interpolateBundleMessage(parameterValue, bundle, locale, recursive);
                }
            } else {
                parameterValue = parameterName;
            }
        } catch (MissingResourceException var7) {
            parameterValue = parameterName;
        }

        return parameterValue;
    }

    private String removeCurlyBraces(String parameter) {
        return parameter.substring(1, parameter.length() - 1);
    }

    public String interpolate(Context context, Locale locale, String message) {
        InterpolationTerm expression = new InterpolationTerm(message, locale, expressionFactory);
        return expression.interpolate(context);
    }

    private static ExpressionFactory buildExpressionFactory() {
        if (canLoadExpressionFactory()) {
            ExpressionFactory expressionFactory = ELManager.getExpressionFactory();
            Logger.debug(WebjetResourceBundleMessageInterpolator.class, "Loaded expression factory via original TCCL");
            return expressionFactory;
        } else {
            ClassLoader originalContextClassLoader = GetClassLoader.fromContext();

            try {
                SetContextClassLoader.action(ResourceBundleMessageInterpolator.class.getClassLoader());
                ExpressionFactory expressionFactory;
                ExpressionFactory var2;
                if (canLoadExpressionFactory()) {
                    expressionFactory = ELManager.getExpressionFactory();
                    Logger.debug(WebjetResourceBundleMessageInterpolator.class, "Loaded expression factory via HV classloader");
                    var2 = expressionFactory;
                    return var2;
                }

                SetContextClassLoader.action(ELManager.class.getClassLoader());
                if (canLoadExpressionFactory()) {
                    expressionFactory = ELManager.getExpressionFactory();
                    Logger.debug(WebjetResourceBundleMessageInterpolator.class, "Loaded expression factory via EL classloader");
                    var2 = expressionFactory;
                    return var2;
                }
            } catch (Exception var6) {
                throw LOG.getUnableToInitializeELExpressionFactoryException(var6);
            } finally {
                SetContextClassLoader.action(originalContextClassLoader);
            }

            throw LOG.getUnableToInitializeELExpressionFactoryException((Throwable)null);
        }
    }

    private static boolean canLoadExpressionFactory() {
        try {
            ExpressionFactory.newInstance();
            return true;
        } catch (Exception var1) {
            return false;
        }
    }
}