package sk.iway.iwcm.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

import sk.iway.iwcm.Constants;

class SetupSpringConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
        .withUserConfiguration(SetupSpringConfig.class);

    @Test
    void registersWebjetLocaleResolverForDispatcherServlet() {
        contextRunner.run(applicationContext -> {
            assertThat(applicationContext).hasSingleBean(LocaleResolver.class);

            LocaleResolver localeResolver = applicationContext.getBean(
                DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class
            );
            ServletContext previousServletContext = Constants.getServletContext();
            try {
                Constants.setServletContext(applicationContext.getServletContext());
                MockHttpServletRequest request = new MockHttpServletRequest(applicationContext.getServletContext());
                request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "sk-SK");
                request.setParameter("language", "en");

                assertEquals("en", localeResolver.resolveLocale(request).getLanguage());
            } finally {
                Constants.setServletContext(previousServletContext);
            }
        });
    }
}
