package sk.iway.iwcm.setup;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.webjet_component.WebjetMessageSource;

@Configuration
public class SetupSpringConfig implements WebMvcConfigurer {

    @Bean
    public AcceptHeaderLocaleResolver webjetLocaleResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String lng = Prop.getLng(request, false);
                if (Tools.isEmpty(lng)) lng = "sk";
                if ("cz".equals(lng)) lng = "cs";
                return org.springframework.util.StringUtils.parseLocaleString(PageLng.getUserLngIso(lng).replace("-", "_"));
            }
        };

        // During embedded server startup, ServletContext may not be available yet.
        // Use a fallback default locale when Cache/ServletContext is not available.
        try {
            localeResolver.setDefaultLocale(new Locale(PageLng.getUserLngIso(Constants.getString("defaultLanguage")).replace("-", "_")));
        } catch (Exception e) {
            localeResolver.setDefaultLocale(new Locale("sk")); // fallback
        }
        return localeResolver;
    }

    @Bean
    public SpringResourceTemplateResolver thymeleafTemplateResolver() {
        Logger.debug(SetupSpringConfig.class, "thymeleaf: thymeleafTemplateResolver");

        SpringResourceTemplateResolver templateResolver
          = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setOrder(0);
        templateResolver.setCacheable(false);
        Logger.debug(SetupSpringConfig.class, "thymeleafTemplateResolver SETTING ENCODING: "+Constants.getString("defaultEncoding"));
        templateResolver.setCharacterEncoding(SetCharacterEncodingFilter.getEncoding());
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        Logger.debug(SetupSpringConfig.class, "thymeleaf: templateEngine");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setEnableSpringELCompiler(true);
        templateEngine.setTemplateResolver(thymeleafTemplateResolver());
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver thymeleafViewResolver() {

        Logger.debug(SetupSpringConfig.class, "thymeleaf: thymeleafViewResolver");

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        Logger.debug(SetupSpringConfig.class, "thymeleafViewResolver SETTING ENCODING: "+Constants.getString("defaultEncoding"));
        viewResolver.setCharacterEncoding(SetCharacterEncodingFilter.getEncoding());
        return viewResolver;
    }

    @Bean
    public MessageSource messageSource() {
        Logger.debug(SetupSpringConfig.class, "Spring: messageSource");
        WebjetMessageSource source = new WebjetMessageSource();
        return source;
    }

}
