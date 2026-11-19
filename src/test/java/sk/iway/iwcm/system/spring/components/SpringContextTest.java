package sk.iway.iwcm.system.spring.components;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.GenericApplicationContext;

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

    private GenericApplicationContext createContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("springContext", SpringContext.class);
        context.refresh();
        return context;
    }
}
