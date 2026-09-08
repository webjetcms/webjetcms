package sk.iway.iwcm.system.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

class WebjetAdditionalSpringPackagesRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {
        String[] packages = WebjetBootstrapSpringConfiguration
            .fromEnvironment(environment)
            .getAdditionalPackages();
        if (packages.length == 0) {
            return;
        }

        Logger.info(WebjetAdditionalSpringPackagesRegistrar.class,
            "Spring scan packages: " + Tools.join(packages, ", "));
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
            registry, true, environment, resourceLoader
        );
        scanner.scan(packages);
    }
}
