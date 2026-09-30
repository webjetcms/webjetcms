package sk.iway.iwcm.system.spring;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

class WebjetCustomerSpringConfigurationImportSelector
        implements ImportSelector, EnvironmentAware, BeanClassLoaderAware {

    private Environment environment;
    private ClassLoader beanClassLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        WebjetBootstrapSpringConfiguration springConfiguration =
            WebjetBootstrapSpringConfiguration.fromEnvironment(environment);
        Set<String> configurationClasses = new LinkedHashSet<>();

        addIfPresent(configurationClasses,
            getConfigurationClassName(springConfiguration.installName(), "SpringConfig"));

        if (Tools.isNotEmpty(springConfiguration.logInstallName())) {
            String logConfiguration = getConfigurationClassName(
                springConfiguration.logInstallName(), "LogSpringConfig"
            );
            if (isPresent(logConfiguration)) {
                configurationClasses.add(logConfiguration);
            } else {
                addIfPresent(configurationClasses, getConfigurationClassName(
                    springConfiguration.logInstallName(), "SpringConfig"
                ));
            }
        }

        return configurationClasses.toArray(String[]::new);
    }

    private void addIfPresent(Set<String> configurationClasses, String className) {
        if (isPresent(className)) {
            configurationClasses.add(className);
            Logger.info(WebjetCustomerSpringConfigurationImportSelector.class,
                "SPRING: found custom config " + className);
        } else if (Tools.isNotEmpty(className)) {
            Logger.info(WebjetCustomerSpringConfigurationImportSelector.class,
                "SPRING: custom config not found " + className);
        }
    }

    private boolean isPresent(String className) {
        return Tools.isNotEmpty(className) && ClassUtils.isPresent(className, beanClassLoader);
    }

    private String getConfigurationClassName(String packageName, String simpleClassName) {
        if (Tools.isEmpty(packageName)) {
            return "";
        }
        return "sk.iway." + packageName + "." + simpleClassName;
    }
}
