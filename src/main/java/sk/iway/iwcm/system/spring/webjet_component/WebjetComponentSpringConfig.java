package sk.iway.iwcm.system.spring.webjet_component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.system.spring.webjet_component.dialect.IwcmDialect;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebjetComponentSpringConfig {

    final
    ApplicationContext context;

    public WebjetComponentSpringConfig(ApplicationContext context) {
        this.context = context;
    }

    // nastavenie message source pre validator
    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        validatorFactoryBean.setMessageInterpolator(new WebjetResourceBundleMessageInterpolator());

        return validatorFactoryBean;
    }

    @Bean
    public WebjetViewResolver viewResolver() {

        List<ViewResolver> resolvers = new ArrayList<>();

        WebjetInternalResourceViewResolver view = new WebjetInternalResourceViewResolver();
        view.setViewClass(JstlView.class);
        view.setSuffix(".jsp");
        view.setOrder(1);
        view.setApplicationContext(context);

        resolvers.add(view);

        WebjetFreeMarkerViewResolver view2 = new WebjetFreeMarkerViewResolver();
        view2.setViewClass(FreeMarkerView.class);
        view2.setCache(true);
        view2.setOrder(2);
        view2.setSuffix(".ftl");
        view2.setContentType("text/html;charset=windows-1250");
        view2.setApplicationContext(context);

        resolvers.add(view2);
        resolvers.add(webjetThymeleafViewResolver());

        WebjetViewResolver resolver = new WebjetViewResolver();
        resolver.setViewResolvers(resolvers);
        resolver.setApplicationContext(context);
        return resolver;
    }

    /* THYMELEAF KONFIGURACIA, namapovana na / s vyhladavanim .html suborov */
    @Bean
    public MessageSource webjetMessageSource() {
        Logger.debug(WebjetComponentParser.class, "Spring: messageSource");
        WebjetMessageSource source = new WebjetMessageSource();
        return source;
    }

    @Bean
    public SpringTemplateEngine webjetTemplateEngine() {
        Logger.debug(WebjetComponentParser.class, "thymeleaf: templateEngine");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setEnableSpringELCompiler(true);
        templateEngine.addDialect(new IwcmDialect());
        templateEngine.setTemplateResolver(webjetThymeleafTemplateResolver());
        return templateEngine;
    }

    @Bean
    public SpringResourceTemplateResolver webjetThymeleafTemplateResolver() {
        Logger.debug(WebjetComponentParser.class, "thymeleaf: thymeleafTemplateResolver");

        SpringResourceTemplateResolver templateResolver
          = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setOrder(0);
        templateResolver.setCacheable(false);
        Logger.debug(WebjetComponentSpringConfig.class, "thymeleafTemplateResolver SETTING ENCODING: "+Constants.getString("defaultEncoding"));
        templateResolver.setCharacterEncoding(SetCharacterEncodingFilter.getEncoding());
        return templateResolver;
    }

    @Bean
    public ThymeleafViewResolver webjetThymeleafViewResolver() {

        Logger.debug(WebjetComponentParser.class, "thymeleaf: thymeleafViewResolver");

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(webjetTemplateEngine());
        viewResolver.setExcludedViewNames(new String[] {"*.jsp", "*.ftl"});
        Logger.debug(WebjetComponentSpringConfig.class, "thymeleafViewResolver SETTING ENCODING: "+Constants.getString("defaultEncoding"));
        viewResolver.setCharacterEncoding(SetCharacterEncodingFilter.getEncoding());
        return viewResolver;
    }
}
