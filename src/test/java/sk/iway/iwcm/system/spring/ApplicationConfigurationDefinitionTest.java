package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;

import sk.iway.dynamicinstall.DynamicInstallController;
import sk.iway.dynamicinstall.SpringConfig;
import sk.iway.iwcm.setup.LicenseController;
import sk.iway.iwcm.setup.SetupController;
import sk.iway.iwcm.setup.SetupSpringConfig;
import sk.iway.webjet.v9.V9JpaDBConfig;
import sk.iway.webjet.v9.V9SpringConfig;

class ApplicationConfigurationDefinitionTest {

    @Test
    void setupModeRegistersOnlySetupDefinitions() {
        try (GenericApplicationContext applicationContext = parseDefinitions(WebjetBootstrapMode.SETUP)) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, SetupApplicationConfiguration.class, 1);
            assertDefinitionCount(beanFactory, SetupSpringConfig.class, 1);
            assertDefinitionCount(beanFactory, SetupController.class, 1);
            assertDefinitionCount(beanFactory, LicenseController.class, 1);
            assertBeanDefinitionCount(beanFactory, "setupCharacterEncodingFilterRegistration", 1);
            assertBeanDefinitionCount(beanFactory, "setupTomcatHttpConnectorCustomizer", 1);
            assertBeanDefinitionCount(beanFactory, "messageSource", 1);
            assertDefinitionCount(beanFactory, SpringAppInitializer.class, 1);
            assertBeanDefinitionCount(beanFactory, "webjetApplicationReadyListener", 0);

            assertDefinitionCount(beanFactory, ProductionApplicationConfiguration.class, 0);
            assertDefinitionCount(beanFactory, SpringBootStarter.ProductionServletConfiguration.class, 0);
            assertBeanDefinitionCount(beanFactory, "characterEncodingFilterRegistration", 0);
            assertDefinitionCount(beanFactory, BaseSpringConfig.class, 0);
            assertDefinitionCount(beanFactory, V9SpringConfig.class, 0);
            assertDefinitionCount(beanFactory, V9JpaDBConfig.class, 0);
            assertDefinitionCount(beanFactory, SpringSecurityConf.class, 0);
            assertDefinitionCount(beanFactory, SpringConfig.class, 0);
            assertDefinitionCount(beanFactory, DynamicInstallController.class, 0);
            assertDefinitionCount(beanFactory, com.example.webjetadditional.AdditionalController.class, 0);
            assertBeanDefinitionCount(beanFactory, "dynamicInstallBean", 0);
            assertBeanDefinitionCount(beanFactory, "additionalPackageBean", 0);
        }
    }

    @Test
    void productionModeRegistersOnlyProductionDefinitions() {
        try (GenericApplicationContext applicationContext = parseDefinitions(WebjetBootstrapMode.PRODUCTION)) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, ProductionApplicationConfiguration.class, 1);
            assertDefinitionCount(beanFactory, BaseSpringConfig.class, 1);
            assertDefinitionCount(beanFactory, V9SpringConfig.class, 1);
            assertDefinitionCount(beanFactory, V9JpaDBConfig.class, 1);
            assertDefinitionCount(beanFactory, SpringSecurityConf.class, 1);
            assertDefinitionCount(beanFactory, SpringAppInitializer.class, 1);
            assertBeanDefinitionCount(beanFactory, "webjetApplicationReadyListener", 1);
            assertDefinitionCount(beanFactory, SpringBootStarter.ProductionServletConfiguration.class, 1);
            assertBeanDefinitionCount(beanFactory, "characterEncodingFilterRegistration", 1);
            assertBeanDefinitionCount(beanFactory, "messageSource", 1);
            assertBeanDefinitionCount(beanFactory, "webjetMessageSource", 0);
            assertDefinitionCount(beanFactory, SpringConfig.class, 1);
            assertDefinitionCount(beanFactory, DynamicInstallController.class, 1);
            assertDefinitionCount(beanFactory, com.example.webjetadditional.AdditionalController.class, 1);
            assertBeanDefinitionCount(beanFactory, "dynamicInstallBean", 1);
            assertBeanDefinitionCount(beanFactory, "additionalPackageBean", 1);

            assertDefinitionCount(beanFactory, sk.iway.aceintegration.SpringConfig.class, 0);
            assertDefinitionCount(beanFactory, sk.iway.basecms.SpringConfig.class, 0);
            assertBeanDefinitionCount(beanFactory, "tomcatSessionPersistenceCustomizer", 0);

            assertDefinitionCount(beanFactory, SetupApplicationConfiguration.class, 0);
            assertDefinitionCount(beanFactory, SetupSpringConfig.class, 0);
            assertDefinitionCount(beanFactory, SetupController.class, 0);
            assertDefinitionCount(beanFactory, LicenseController.class, 0);
            assertBeanDefinitionCount(beanFactory, "setupCharacterEncodingFilterRegistration", 0);
            assertBeanDefinitionCount(beanFactory, "setupTomcatHttpConnectorCustomizer", 0);
        }
    }

    @Test
    void selectedCustomerOwnsItsPersistenceConfiguration() {
        try (GenericApplicationContext applicationContext = parseDefinitions(
                WebjetBootstrapMode.PRODUCTION, "basecms")) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, sk.iway.basecms.SpringConfig.class, 1);
            assertDefinitionCount(beanFactory, sk.iway.basecms.JpaDBConfig.class, 1);
            assertBeanDefinitionCount(beanFactory, "contactRepository", 1);
            assertBeanDefinitionCount(beanFactory, "basecmsEntityManager", 1);
            assertBeanDefinitionCount(beanFactory, "basecmsTransactionManager", 1);

            assertDefinitionCount(beanFactory, SpringConfig.class, 0);
            assertDefinitionCount(beanFactory, sk.iway.aceintegration.SpringConfig.class, 0);
            assertBeanDefinitionCount(beanFactory, "tomcatSessionPersistenceCustomizer", 0);
        }
    }

    private GenericApplicationContext parseDefinitions(WebjetBootstrapMode mode) {
        return parseDefinitions(mode, "dynamicinstall");
    }

    private GenericApplicationContext parseDefinitions(WebjetBootstrapMode mode, String installName) {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.getDefaultListableBeanFactory().setAllowBeanDefinitionOverriding(false);
        TestPropertyValues.of(
            WebjetBootstrapMode.PROPERTY_NAME + "=" + mode.getPropertyValue(),
            WebjetBootstrapSpringConfiguration.INSTALL_NAME_PROPERTY + "=" + installName,
            WebjetBootstrapSpringConfiguration.ADD_PACKAGES_PROPERTY + "=com.example.webjetadditional"
        )
            .applyTo(applicationContext);

        new AnnotatedBeanDefinitionReader(applicationContext).register(
            SetupApplicationConfiguration.class,
            ProductionApplicationConfiguration.class,
            SpringAppInitializer.class,
            SpringBootStarter.ProductionServletConfiguration.class
        );

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
}
