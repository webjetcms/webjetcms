package sk.iway.iwcm.system.spring.webjet_component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.JstlView;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;

class WebjetViewResolverTest extends BaseWebjetTest {

    @Test
    void explicitJspViewUsesExactlyOneSuffix() throws Exception {
        try (StaticWebApplicationContext context = createApplicationContext()) {
            WebjetViewResolver resolver = createResolver(context);

            assertEquals("/404.jsp", resolveUrl(resolver, "/404.jsp"));
            assertEquals("/403.jsp", resolveUrl(resolver, "forward:/403.jsp"));
        }
    }

    @Test
    void viewFolderIsScopedToSingleResolution() throws Exception {
        try (StaticWebApplicationContext context = createApplicationContext()) {
            WebjetViewResolver resolver = createResolver(context);
            String viewName = "/components/carousel_slider/Simplicity.jsp";

            assertEquals("/components/carousel_slider/css/Simplicity.jsp",
                    resolveUrl(resolver, viewName, "css"));
            assertNull(resolver.resolveViewName(viewName, Locale.ENGLISH));
            assertNull(resolver.resolveViewName(viewName, Locale.ENGLISH, ""));
            assertEquals("/components/carousel_slider/skins/Simplicity.jsp",
                    resolveUrl(resolver, viewName, "skins"));
        }
    }

    private StaticWebApplicationContext createApplicationContext() {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(Constants.getServletContext());
        context.refresh();
        return context;
    }

    private WebjetViewResolver createResolver(StaticWebApplicationContext context) {
        WebjetInternalResourceViewResolver jspResolver = new WebjetInternalResourceViewResolver();
        jspResolver.setViewClass(JstlView.class);
        jspResolver.setSuffix(".jsp");
        jspResolver.setCache(false);
        jspResolver.setOrder(1);
        jspResolver.setApplicationContext(context);

        WebjetViewResolver resolver = new WebjetViewResolver();
        resolver.setViewResolvers(new ArrayList<>(List.of(jspResolver)));
        resolver.setApplicationContext(context);
        return resolver;
    }

    private String resolveUrl(WebjetViewResolver resolver, String viewName) throws Exception {
        View view = resolver.resolveViewName(viewName, Locale.ENGLISH);
        return assertInstanceOf(AbstractUrlBasedView.class, view).getUrl();
    }

    private String resolveUrl(WebjetViewResolver resolver, String viewName, String viewFolder) throws Exception {
        View view = resolver.resolveViewName(viewName, Locale.ENGLISH, viewFolder);
        return assertInstanceOf(AbstractUrlBasedView.class, view).getUrl();
    }
}
