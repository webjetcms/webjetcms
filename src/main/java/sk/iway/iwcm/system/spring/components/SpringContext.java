package sk.iway.iwcm.system.spring.components;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

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