package sk.iway.iwcm.system.spring;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import sk.iway.iwcm.setup.SetupFormBean;

class SetupTemplateSecurityContractTest {

    private static final String SETUP_TOKEN = "01234567890123456789012345678901";
    private static final String TEMPLATE_URL = "/wjerrorpages/setup/template-contract";
    private static final String LEAKED_PASSWORD = "must-not-appear-in-response";

    @Test
    void setupTemplateContainsCsrfTokenAndDoesNotExposePasswordsAsText() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext()) {
            var mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();

            mockMvc.perform(get(TEMPLATE_URL).with(user("setup").roles("SETUP")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("type=\"password\"")))
                .andExpect(content().string(not(containsString(LEAKED_PASSWORD))));
        }
    }

    private AnnotationConfigWebApplicationContext createContext() {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setServletContext(new MockServletContext("src/main/webapp", new FileSystemResourceLoader()));
        TestPropertyValues.of(WebjetSetupProperties.TOKEN_PROPERTY + "=" + SETUP_TOKEN)
            .applyTo(applicationContext);
        applicationContext.register(TemplateTestConfiguration.class);
        applicationContext.refresh();
        return applicationContext;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({
        SetupApplicationConfiguration.SetupSecurityConfiguration.class,
        SetupTemplateController.class
    })
    static class TemplateTestConfiguration {

        @Bean
        SpringResourceTemplateResolver setupTemplateResolver() {
            SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
            resolver.setPrefix("/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode("HTML");
            resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resolver.setCacheable(false);
            return resolver;
        }

        @Bean
        SpringTemplateEngine setupTemplateEngine(SpringResourceTemplateResolver setupTemplateResolver) {
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(setupTemplateResolver);
            return engine;
        }

        @Bean
        ThymeleafViewResolver setupViewResolver(SpringTemplateEngine setupTemplateEngine) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(setupTemplateEngine);
            resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
            return resolver;
        }

        @Bean
        MessageSource messageSource() {
            StaticMessageSource messageSource = new StaticMessageSource();
            messageSource.setUseCodeAsDefaultMessage(true);
            return messageSource;
        }
    }

    @Controller
    static class SetupTemplateController {

        @GetMapping(TEMPLATE_URL)
        String renderSetupTemplate(Model model) {
            SetupFormBean setupForm = new SetupFormBean();
            setupForm.setDbPassword(LEAKED_PASSWORD);
            setupForm.setDbSuperuserPassword(LEAKED_PASSWORD);
            model.addAttribute("setupForm", setupForm);
            model.addAttribute("disableLng", false);
            model.addAttribute("dbConnFail", false);
            model.addAttribute("dbCreateErrMsg", null);
            model.addAttribute("isSetup", true);
            model.addAttribute("isSetupSave", false);
            model.addAttribute("isLicense", false);
            model.addAttribute("isLicenseSave", false);
            model.addAttribute("cmpCss", "");
            return "/admin/setup/setup";
        }
    }
}
