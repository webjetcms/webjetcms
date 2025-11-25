package sk.iway.webjet.v9;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.ApiTokenAuthFilter;
import sk.iway.iwcm.system.spring.ConfigurableSecurity;

/**
 * SpringConfig - konfiguracia, aby sme vedeli co treba potom pridat do standardnej WJ konfiguracie
 */
@EnableSpringDataWebSupport
@Configuration
@EnableWebMvc
@ComponentScan({
    "sk.iway.iwcm.admin.upload",
    "sk.iway.iwcm.doc",
    "sk.iway.iwcm.components.gallery",
    "sk.iway.iwcm.components.redirects",
    "sk.iway.iwcm.components.insertScript.rest",
    "sk.iway.iwcm.components.domain_redirects",
    "sk.iway.iwcm.components.forms",
    "sk.iway.iwcm.components.forms.archive",
    "sk.iway.iwcm.components.memory_cleanup.cache_objects",
    "sk.iway.iwcm.editor.rest",
    "sk.iway.iwcm.components.translation_keys.rest",
    "sk.iway.webjet.v9",
    "sk.iway.iwcm.system.audit.rest",
    "sk.iway.iwcm.system.monitoring.rest",
    "sk.iway.iwcm.system.elfinder",
    "sk.iway.iwcm.system.logging",
    "sk.iway.iwcm.system.ntlm",
    "sk.iway.iwcm.components.media",
    "sk.iway.iwcm.components.welcome",
    "sk.iway.iwcm.components.perex_groups",
    "sk.iway.iwcm.components.user",
    "sk.iway.iwcm.components.users.userdetail",
    "sk.iway.iwcm.components.users.groups_approve",
    "sk.iway.iwcm.components.users.usergroups",
    "sk.iway.iwcm.components.users.permgroups",
    "sk.iway.iwcm.components.gdpr.rest",
    "sk.iway.iwcm.components.qa.rest",
    "sk.iway.iwcm.components.tooltip",
    "sk.iway.iwcm.components.export",
    "sk.iway.iwcm.dmail.rest",
    "sk.iway.iwcm.system.datatable.editorlocking",
    "sk.iway.iwcm.components.banner",
    "sk.iway.iwcm.dmail.jpa",
    "sk.iway.iwcm.stat.rest",
    "sk.iway.iwcm.components.calendar.rest",
    "sk.iway.iwcm.components.reservation.rest",
    "sk.iway.iwcm.components.inquiry.rest",
    "sk.iway.iwcm.doc.attributes.jpa",
    "sk.iway.iwcm.components.proxy.rest",
    "sk.iway.iwcm.components.enumerations.rest",
    "sk.iway.iwcm.components.response_header.rest",
    "sk.iway.iwcm.findexer",
    "sk.iway.iwcm.filebrowser",
    "sk.iway.iwcm.components.forum.rest",
    "sk.iway.iwcm.components.seo.rest",
    "sk.iway.iwcm.doc.clone_structure",
    "sk.iway.iwcm.update",
    "sk.iway.iwcm.xls",
    "sk.iway.iwcm.components.restaurant_menu.rest",
    "sk.iway.iwcm.components.quiz.rest",
    "sk.iway.iwcm.components.blog.rest",
    "sk.iway.iwcm.components.appcookiebar",
    "sk.iway.iwcm.components.appdocsembed",
    "sk.iway.iwcm.components.appfacebookcomments",
    "sk.iway.iwcm.components.appfacebooklike",
    "sk.iway.iwcm.components.appsmartsupp",
    "sk.iway.iwcm.components.apphtmlembed",
    "sk.iway.iwcm.components.appvyhladavanie",
    "sk.iway.iwcm.components.contentblock",
    "sk.iway.iwcm.components.sendlink",
    "sk.iway.iwcm.components.gdpr",
    "sk.iway.iwcm.components.sitebrowser",
    "sk.iway.iwcm.components.basket.rest",
    "sk.iway.iwcm.components.upload"
})
public class V9SpringConfig implements WebMvcConfigurer, ConfigurableSecurity {

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String lng = Prop.getLng(request, false);
                if (Tools.isEmpty(lng)) lng = "sk";
                if ("cz".equals(lng)) lng = "cs";
                return org.springframework.util.StringUtils.parseLocaleString(PageLng.getUserLngIso(lng).replaceAll("-", "_"));
            }
        };

        localeResolver.setDefaultLocale(new Locale(PageLng.getUserLngIso(Constants.getString("defaultLanguage")).replaceAll("-", "_")));
        return localeResolver;
    }

    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(-1);
        return multipartResolver;
    }

    /**
     * Servis sluzi na konvertovanie typov premennych, napr. String -> Double
     * @return ConversionService
     */
    @Bean(name = "conversionService")
    public ConversionService conversionService() {
        ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
        conversionServiceFactoryBean.afterPropertiesSet();
        return conversionServiceFactoryBean.getObject();
    }

    @Override
    public void configureSecurity(HttpSecurity http) throws Exception {
        //add logon filter using API token
        http.addFilterAfter(new ApiTokenAuthFilter(), BasicAuthenticationFilter.class);

        //add SetCharacterEncodingFilter for setting RequestBean and other system stuff, must be after ApiTokenAuthFilter because it needs logged user
        http.addFilterAfter(new sk.iway.iwcm.SetCharacterEncodingFilter(), ApiTokenAuthFilter.class);
    }
}