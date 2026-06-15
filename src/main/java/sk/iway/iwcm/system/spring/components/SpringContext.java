package sk.iway.iwcm.system.spring.components;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring application context holder for legacy code access.
 * Provides static access to the ApplicationContext.
 * 
 * Note: This class does NOT depend on Spring Boot.
 * It works with pure Spring Framework 7.x.
 */
@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext context;

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SpringContext.context = context;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
