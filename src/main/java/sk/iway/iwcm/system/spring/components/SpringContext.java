package sk.iway.iwcm.system.spring.components;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
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
public class SpringContext implements ApplicationContextAware, DisposableBean {
    private static final AtomicReference<ApplicationContext> CONTEXT = new AtomicReference<>();

    private ApplicationContext ownedContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        ownedContext = context;
        CONTEXT.set(context);
    }

    @Override
    public void destroy() {
        CONTEXT.compareAndSet(ownedContext, null);
    }

    public static ApplicationContext getApplicationContext() {
        return CONTEXT.get();
    }
}
