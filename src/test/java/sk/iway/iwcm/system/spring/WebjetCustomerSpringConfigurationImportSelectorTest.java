package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;

class WebjetCustomerSpringConfigurationImportSelectorTest {

    @Test
    void prefersLogSpringConfig() {
        try (GenericApplicationContext applicationContext = parseDefinitions(
                WebjetBootstrapSpringConfiguration.LOG_INSTALL_NAME_PROPERTY + "=dynamiclog")) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, sk.iway.dynamiclog.LogSpringConfig.class, 1);
            assertDefinitionCount(beanFactory, sk.iway.dynamiclog.SpringConfig.class, 0);
            assertBeanDefinitionCount(beanFactory, "preferredLogConfigurationBean", 1);
            assertBeanDefinitionCount(beanFactory, "legacyLogConfigurationBean", 0);
        }
    }

    @Test
    void fallsBackToLegacyLogSpringConfig() {
        try (GenericApplicationContext applicationContext = parseDefinitions(
                WebjetBootstrapSpringConfiguration.LOG_INSTALL_NAME_PROPERTY + "=legacylog")) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, sk.iway.legacylog.SpringConfig.class, 1);
            assertBeanDefinitionCount(beanFactory, "legacyLogFallbackBean", 1);
        }
    }

    @Test
    void ignoresMissingOptionalConfiguration() {
        try (GenericApplicationContext applicationContext = parseDefinitions(
                WebjetBootstrapSpringConfiguration.INSTALL_NAME_PROPERTY + "=missinginstall")) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, sk.iway.dynamicinstall.SpringConfig.class, 0);
            assertDefinitionCount(beanFactory, sk.iway.dynamiclog.LogSpringConfig.class, 0);
            assertBeanDefinitionCount(beanFactory, "dynamicInstallBean", 0);
        }
    }

    private GenericApplicationContext parseDefinitions(String... properties) {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.getDefaultListableBeanFactory().setAllowBeanDefinitionOverriding(false);
        TestPropertyValues.of(properties).applyTo(applicationContext);
        new AnnotatedBeanDefinitionReader(applicationContext).register(DynamicImports.class);

        ConfigurationClassPostProcessor configurationProcessor = new ConfigurationClassPostProcessor();
        configurationProcessor.setEnvironment(applicationContext.getEnvironment());
        configurationProcessor.setResourceLoader(applicationContext);
        configurationProcessor.postProcessBeanDefinitionRegistry(applicationContext);
        return applicationContext;
    }

    private void assertDefinitionCount(DefaultListableBeanFactory beanFactory, Class<?> beanType,
            int expectedCount) {
        assertEquals(expectedCount, beanFactory.getBeanNamesForType(beanType, false, false).length,
            () -> "Unexpected bean definition count for " + beanType.getName());
    }

    private void assertBeanDefinitionCount(DefaultListableBeanFactory beanFactory, String beanName,
            int expectedCount) {
        assertEquals(expectedCount, beanFactory.containsBeanDefinition(beanName) ? 1 : 0,
            () -> "Unexpected bean definition count for " + beanName);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(WebjetCustomerSpringConfigurationImportSelector.class)
    static class DynamicImports {
    }
}
