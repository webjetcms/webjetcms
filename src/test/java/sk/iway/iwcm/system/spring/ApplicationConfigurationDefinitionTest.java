package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

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
            assertDefinitionCount(beanFactory,
                SpringBootStarter.ProductionServletInfrastructureConfiguration.class, 0);
            assertDefinitionCount(beanFactory, SpringBootStarter.ProductionServletConfiguration.class, 0);
            assertBeanDefinitionCount(beanFactory, "externalWarMultipartServletInitializer", 0);
            assertBeanDefinitionCount(beanFactory, "characterEncodingFilterRegistration", 0);
            assertProductionServletDefinitions(beanFactory, 0);
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
            assertDefinitionCount(beanFactory,
                SpringBootStarter.ProductionServletInfrastructureConfiguration.class, 1);
            assertDefinitionCount(beanFactory, SpringBootStarter.ProductionServletConfiguration.class, 1);
            assertBeanDefinitionCount(beanFactory, "externalWarMultipartServletInitializer", 0);
            assertBeanDefinitionCount(beanFactory, "characterEncodingFilterRegistration", 1);
            assertProductionServletDefinitions(beanFactory, 1);
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
    void externalWarDoesNotRegisterDescriptorOwnedServletComponents() {
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
        applicationContext.setServletContext(new MockServletContext());
        parseDefinitions(applicationContext, WebjetBootstrapMode.PRODUCTION, "dynamicinstall");
        try (applicationContext) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory,
                SpringBootStarter.ProductionServletInfrastructureConfiguration.class, 1);
            assertDefinitionCount(beanFactory, SpringBootStarter.ProductionServletConfiguration.class, 0);
            assertBeanDefinitionCount(beanFactory, "externalWarMultipartServletInitializer", 1);
            assertProductionServletDefinitions(beanFactory, 0);
        }
    }

    @Test
    void externalWarSetupSkipsEmbeddedTomcatCustomization() {
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
        applicationContext.setServletContext(new MockServletContext());
        parseDefinitions(applicationContext, WebjetBootstrapMode.SETUP, "dynamicinstall");
        try (applicationContext) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, SetupApplicationConfiguration.class, 1);
            assertBeanDefinitionCount(beanFactory, "setupCharacterEncodingFilterRegistration", 1);
            assertDefinitionCount(beanFactory,
                SetupApplicationConfiguration.EmbeddedServletContainerConfiguration.class, 0);
            assertBeanDefinitionCount(beanFactory, "setupTomcatHttpConnectorCustomizer", 0);
        }
    }

    @Test
    void externalWarKeepsCustomerConfigurationButSkipsEmbeddedTomcatCustomization() {
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
        applicationContext.setServletContext(new MockServletContext());
        parseDefinitions(applicationContext, WebjetBootstrapMode.PRODUCTION, "aceintegration");
        try (applicationContext) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, sk.iway.aceintegration.SpringConfig.class, 1);
            assertBeanDefinitionCount(beanFactory, "tomcatHttpConnectorCustomizer", 0);
            assertBeanDefinitionCount(beanFactory, "webjetTomcatMimeMappingsCustomizer", 0);
            assertBeanDefinitionCount(beanFactory, "tomcatSessionPersistenceCustomizer", 0);
        }
    }

    @Test
    void embeddedCustomerConfigurationRegistersItsTomcatCustomizers() {
        try (GenericApplicationContext applicationContext = parseDefinitions(
                WebjetBootstrapMode.PRODUCTION, "aceintegration")) {
            DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

            assertDefinitionCount(beanFactory, sk.iway.aceintegration.SpringConfig.class, 1);
            assertBeanDefinitionCount(beanFactory, "tomcatHttpConnectorCustomizer", 1);
            assertBeanDefinitionCount(beanFactory, "webjetTomcatMimeMappingsCustomizer", 1);
            assertBeanDefinitionCount(beanFactory, "tomcatSessionPersistenceCustomizer", 1);
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
        parseDefinitions(applicationContext, mode, installName);
        return applicationContext;
    }

    private void parseDefinitions(GenericApplicationContext applicationContext,
            WebjetBootstrapMode mode, String installName) {
        applicationContext.getDefaultListableBeanFactory().setAllowBeanDefinitionOverriding(false);
        TestPropertyValues.of(
            WebjetBootstrapMode.PROPERTY_NAME + "=" + mode.getPropertyValue(),
            WebjetBootstrapSpringConfiguration.INSTALL_NAME_PROPERTY + "=" + installName,
            WebjetBootstrapSpringConfiguration.ADD_PACKAGES_PROPERTY + "=com.example.webjetadditional"
        )
            .applyTo(applicationContext);

        Import applicationImports = SpringBootStarter.class.getAnnotation(Import.class);
        assertNotNull(applicationImports, "SpringBootStarter must declare its application configurations");
        new AnnotatedBeanDefinitionReader(applicationContext).register(applicationImports.value());

        ConfigurationClassPostProcessor configurationProcessor = new ConfigurationClassPostProcessor();
        configurationProcessor.setEnvironment(applicationContext.getEnvironment());
        configurationProcessor.setResourceLoader(applicationContext);
        configurationProcessor.postProcessBeanDefinitionRegistry(applicationContext);
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

    private void assertProductionServletDefinitions(DefaultListableBeanFactory beanFactory,
            int expectedCount) {
        assertBeanDefinitionCount(beanFactory, "contextFilterRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "stripesFilterRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "virtualPathFilterRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "requestContextListenerRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "sessionListenerRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "iwcmInitServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "showDocServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "previewServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "formMailServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "offlineServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "deleteServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "logoffServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "multipleFileUploadServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "thumbServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "captchaServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "elfinderServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "pdfServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "xhrFileUploadServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "adminUploadServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "exportSyncServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "getProtectedFileServletRegistration", expectedCount);
        assertBeanDefinitionCount(beanFactory, "stripesDispatcherRegistration", expectedCount);
    }
}
