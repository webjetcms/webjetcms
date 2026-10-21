package sk.iway.iwcm.system.spring.components;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;

class SpringContextTest {

    @Test
    void closingContextClearsPublishedContext() {
        GenericApplicationContext context = createContext();
        try {
            assertSame(context, SpringContext.getApplicationContext());
        } finally {
            context.close();
        }

        assertNull(SpringContext.getApplicationContext());
    }

    @Test
    void closingOlderContextDoesNotClearNewerContext() {
        GenericApplicationContext olderContext = createContext();
        GenericApplicationContext newerContext = createContext();
        try {
            assertSame(newerContext, SpringContext.getApplicationContext());

            olderContext.close();

            assertSame(newerContext, SpringContext.getApplicationContext());
        } finally {
            newerContext.close();
            olderContext.close();
        }

        assertNull(SpringContext.getApplicationContext());
    }

    @Test
    void failedRefreshClearsPublishedContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        AtomicBoolean contextWasPublished = new AtomicBoolean();
        context.registerBean("springContext", SpringContext.class);
        context.registerBean("failingBean", Object.class, () -> {
            contextWasPublished.set(SpringContext.getApplicationContext() == context);
            throw new IllegalStateException("Intentional refresh failure");
        }, beanDefinition -> beanDefinition.setDependsOn("springContext"));

        try {
            assertThrows(BeanCreationException.class, context::refresh);
            assertTrue(contextWasPublished.get());
            assertNull(SpringContext.getApplicationContext());
        } finally {
            context.close();
        }
    }

    @Test
    void servletContextTakesPrecedenceOverStaticFallback() {
        ServletContext originalServletContext = Constants.getServletContext();
        RequestBean originalRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        GenericApplicationContext staticContext = createContext();
        GenericApplicationContext servletApplicationContext = new GenericApplicationContext();
        servletApplicationContext.refresh();
        MockServletContext servletContext = new MockServletContext();
        servletContext.setAttribute("springContext", servletApplicationContext);
        Constants.setServletContext(servletContext);
        SetCharacterEncodingFilter.setCurrentRequestBean(new RequestBean());

        try {
            assertSame(servletApplicationContext, Tools.getSpringContext());
        } finally {
            SetCharacterEncodingFilter.setCurrentRequestBean(originalRequestBean);
            Constants.setServletContext(originalServletContext);
            servletApplicationContext.close();
            staticContext.close();
        }
    }

    private GenericApplicationContext createContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("springContext", SpringContext.class);
        context.refresh();
        return context;
    }
}
