package sk.iway.iwcm.system.spring;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/** Processes additional customer configurations before Boot evaluates its bean conditions. */
class WebjetAdditionalSpringPackagesImportSelector implements ImportSelector, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        String[] packages = WebjetBootstrapSpringConfiguration
            .fromEnvironment(environment)
            .getAdditionalPackages();
        if (packages.length == 0) {
            return new String[0];
        }

        Logger.info(WebjetAdditionalSpringPackagesImportSelector.class,
            "Spring scan packages: " + Tools.join(packages, ", "));
        return new String[] { AdditionalPackagesConfiguration.class.getName() };
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("${" + WebjetBootstrapSpringConfiguration.ADD_PACKAGES_PROPERTY + "}")
    static class AdditionalPackagesConfiguration {
    }
}
