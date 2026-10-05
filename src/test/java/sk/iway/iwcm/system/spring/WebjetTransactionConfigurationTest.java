package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.mock.web.MockServletContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.support.GenericWebApplicationContext;

/** Exercises Boot transaction configuration together with real CMS and customer bean definitions. */
class WebjetTransactionConfigurationTest {

    /** The template selects the CMS manager without changing unqualified transaction manager lookup. */
    @Test
    void templateUsesCmsManagerWithoutMakingItPrimary() {
        try (GenericWebApplicationContext context = parseDefinitions(WebjetBootstrapMode.PRODUCTION)) {
            DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();
            TransactionTemplate template = factory.getBean(TransactionTemplate.class);

            assertSame(factory.getBean("webjet2022TransactionManager"), template.getTransactionManager());
            assertEquals(2, factory.getBeanNamesForType(PlatformTransactionManager.class, false, false).length);
            assertFalse(factory.containsBeanDefinition("ragTransactionManager"));
            assertThrows(NoUniqueBeanDefinitionException.class,
                () -> factory.getBean(PlatformTransactionManager.class));
        }
    }

    /** Customer templates take precedence regardless of their bean name. */
    @ParameterizedTest
    @ValueSource(strings = { "standard", "named" })
    void preservesCustomerTemplateFromAdditionalPackage(String templateKind) {
        try (GenericWebApplicationContext context = parseDefinitions(WebjetBootstrapMode.PRODUCTION,
                "test.customer.template=" + templateKind)) {
            DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();
            TransactionTemplate template = factory.getBean(TransactionTemplate.class);

            assertSame(factory.getBean("basecmsTransactionManager"), template.getTransactionManager());
            assertEquals(1, factory.getBeanNamesForType(TransactionOperations.class, false, false).length);
        }
    }

    /** A customer may supply TransactionOperations without exposing a TransactionTemplate bean. */
    @Test
    void preservesCustomerTransactionOperations() {
        try (GenericWebApplicationContext context = parseDefinitions(WebjetBootstrapMode.PRODUCTION,
                "test.customer.template=operations")) {
            DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();

            assertSame(factory.getBean("customerTransactionOperations"), factory.getBean(TransactionOperations.class));
            assertFalse(factory.containsBeanDefinition("transactionTemplate"));
        }
    }

    /** The explicit template does not compete with a customer's primary transaction manager. */
    @Test
    void preservesCustomerPrimaryManager() {
        try (GenericWebApplicationContext context = parseDefinitions(WebjetBootstrapMode.PRODUCTION,
                "test.customer.primary=true")) {
            DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();

            assertSame(factory.getBean("customerPrimaryTransactionManager"), factory.getBean(PlatformTransactionManager.class));
            assertSame(factory.getBean("webjet2022TransactionManager"),
                factory.getBean(TransactionTemplate.class).getTransactionManager());
        }
    }

    /** An empty additional package list must not fall back to scanning the infrastructure package. */
    @ParameterizedTest
    @ValueSource(strings = { "", "   ", " , " })
    void handlesEmptyAdditionalPackages(String packages) {
        try (GenericWebApplicationContext context = parseDefinitions(WebjetBootstrapMode.PRODUCTION,
                WebjetBootstrapSpringConfiguration.ADD_PACKAGES_PROPERTY + "=" + packages)) {
            DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();

            assertFalse(factory.containsBeanDefinition("basecmsTransactionManager"));
            assertSame(factory.getBean("webjet2022TransactionManager"),
                factory.getBean(TransactionTemplate.class).getTransactionManager());
        }
    }

    /** Setup and license recovery must not acquire a dependency on CMS persistence. */
    @ParameterizedTest
    @EnumSource(value = WebjetBootstrapMode.class, names = { "SETUP", "LICENSE_RECOVERY" })
    void doesNotRegisterTemplateOutsideProduction(WebjetBootstrapMode mode) {
        try (GenericWebApplicationContext context = parseDefinitions(mode)) {
            DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();

            assertEquals(0, factory.getBeanNamesForType(TransactionOperations.class, false, false).length);
            assertEquals(0, factory.getBeanNamesForType(PlatformTransactionManager.class, false, false).length);
        }
    }

    private GenericWebApplicationContext parseDefinitions(WebjetBootstrapMode mode, String... properties) {
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of(
            WebjetBootstrapMode.PROPERTY_NAME + "=" + mode.getPropertyValue(),
            WebjetBootstrapSpringConfiguration.INSTALL_NAME_PROPERTY + "=aceintegration",
            WebjetBootstrapSpringConfiguration.ADD_PACKAGES_PROPERTY
                + "=sk.iway.basecms,com.example.webjettransactions"
        ).and(properties).applyTo(context);

        DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();
        factory.setAllowBeanDefinitionOverriding(false);
        new AnnotatedBeanDefinitionReader(context).register(SpringBootStarter.class);
        ConfigurationClassPostProcessor processor = new ConfigurationClassPostProcessor();
        processor.setEnvironment(context.getEnvironment());
        processor.setResourceLoader(context);
        processor.postProcessBeanDefinitionRegistry(factory);

        // Substitute only manager instances so selection uses real metadata without accessing databases.
        for (String name : factory.getBeanNamesForType(PlatformTransactionManager.class, false, false)) {
            factory.registerSingleton(name, mock(PlatformTransactionManager.class));
        }
        return context;
    }
}
