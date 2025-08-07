package sk.iway.webjet.v9;

import java.util.Locale;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.FileArchivFilter;
import sk.iway.iwcm.system.spring.ApiTokenAuthFilter;
import sk.iway.iwcm.system.spring.ConfigurableSecurity;

/**
 * SpringConfig - konfiguracia, aby sme vedeli co treba potom pridat do standardnej WJ konfiguracie
 */
@EnableSpringDataWebSupport
@Configuration
@EnableWebMvc
@MultipartConfig
@ComponentScan({
    "sk.iway.iwcm.admin",
    "sk.iway.iwcm.calendar",
    "sk.iway.iwcm.doc",
    "sk.iway.iwcm.dmail.rest",
    "sk.iway.iwcm.editor",
    "sk.iway.iwcm.filebrowser",
    "sk.iway.iwcm.findexer",
    "sk.iway.iwcm.grideditor.controller",
    "sk.iway.iwcm.localconf",
    "sk.iway.iwcm.logon",
    "sk.iway.iwcm.rest",
    "sk.iway.iwcm.stat.rest",
    "sk.iway.iwcm.system.audit.rest",
    "sk.iway.iwcm.system.datatable.editorlocking",
    "sk.iway.iwcm.system.datatables",
    "sk.iway.iwcm.system.elfinder",
    "sk.iway.iwcm.system.logging",
    "sk.iway.iwcm.system.adminlog",
    "sk.iway.iwcm.system.monitoring.rest",
    "sk.iway.iwcm.system.ntlm",
    "sk.iway.iwcm.update",
    "sk.iway.iwcm.users",
    "sk.iway.iwcm.xls",
    "sk.iway.iwcm.components.abtesting.rest",
    "sk.iway.iwcm.components.appcookiebar",
    "sk.iway.iwcm.components.appdate",
    "sk.iway.iwcm.components.appdisqus",
    "sk.iway.iwcm.components.appdocsembed",
    "sk.iway.iwcm.components.appfacebookcomments",
    "sk.iway.iwcm.components.appfacebooklike",
    "sk.iway.iwcm.components.appfacebooklikebox",
    "sk.iway.iwcm.components.apphtmlembed",
    "sk.iway.iwcm.components.appsmartsupp",
    "sk.iway.iwcm.components.appvyhladavanie",
    "sk.iway.iwcm.components.banner",
    "sk.iway.iwcm.components.basket.rest",
    "sk.iway.iwcm.components.basket.mvc",
    "sk.iway.iwcm.components.basket.payment_methods.rest",
    "sk.iway.iwcm.components.blog.rest",
    "sk.iway.iwcm.components.calendar",
    "sk.iway.iwcm.components.calendarnews",
    "sk.iway.iwcm.components.configuration",
    "sk.iway.iwcm.components.contentblock",
    "sk.iway.iwcm.components.crypto",
    "sk.iway.iwcm.components.cronjob",
    "sk.iway.iwcm.components.date",
    "sk.iway.iwcm.components.dmail",
    "sk.iway.iwcm.components.domain_redirects",
    "sk.iway.iwcm.components.enumerations.rest",
    "sk.iway.iwcm.components.export",
    "sk.iway.iwcm.components.file_archiv",
    "sk.iway.iwcm.components.form",
    "sk.iway.iwcm.components.forms",
    "sk.iway.iwcm.components.forum.rest",
    "sk.iway.iwcm.components.gallery",
    "sk.iway.iwcm.components.gdpr",
    "sk.iway.iwcm.components.inquiry",
    "sk.iway.iwcm.components.inquirySimple",
    "sk.iway.iwcm.components.insertScript.rest",
    "sk.iway.iwcm.components.media",
    "sk.iway.iwcm.components.menu",
    "sk.iway.iwcm.components.memory_cleanup.cache_objects",
    "sk.iway.iwcm.components.memory_cleanup.database",
    "sk.iway.iwcm.components.memory_cleanup.persistent_cache_objects",
    "sk.iway.iwcm.components.monitoring.rest",
    "sk.iway.iwcm.components.news",
    "sk.iway.iwcm.components.perex_groups",
    "sk.iway.iwcm.components.proxy.rest",
    "sk.iway.iwcm.components.qa.rest",
    "sk.iway.iwcm.components.quiz",
    "sk.iway.iwcm.components.redirects",
    "sk.iway.iwcm.components.reservation",
    "sk.iway.iwcm.components.response_header.rest",
    "sk.iway.iwcm.components.restaurant_menu.rest",
    "sk.iway.iwcm.components.search",
    "sk.iway.iwcm.components.seo.rest",
    "sk.iway.iwcm.components.sendlink",
    "sk.iway.iwcm.components.sitebrowser",
    "sk.iway.iwcm.components.stat",
    "sk.iway.iwcm.components.structuremirroring",
    "sk.iway.iwcm.components.templates",
    "sk.iway.iwcm.components.template_groups",
    "sk.iway.iwcm.components.todo",
    "sk.iway.iwcm.components.tooltip",
    "sk.iway.iwcm.components.translation_keys",
    "sk.iway.iwcm.components.user",
    "sk.iway.iwcm.components.users.groups_approve",
    "sk.iway.iwcm.components.users.permgroups",
    "sk.iway.iwcm.components.users.userdetail",
    "sk.iway.iwcm.components.users.usergroups",
    "sk.iway.iwcm.components.welcome",
    "sk.iway.webjet.v9",
    "sk.iway.iwcm.components.forum",
    "sk.iway.iwcm.components.emoticon",
    "sk.iway.iwcm.components.appuser",
    "sk.iway.iwcm.components.video",
    "sk.iway.iwcm.components.appslitslider",
    "sk.iway.iwcm.components.appslider",
    "sk.iway.iwcm.components.sitemap",
    "sk.iway.iwcm.components.appsocialicon",
    "sk.iway.iwcm.components.qa",
    "sk.iway.iwcm.components.rating",
    "sk.iway.iwcm.components.relatedpages",
    "sk.iway.iwcm.components.restaurant_menu",
    "sk.iway.iwcm.search",
    "sk.iway.iwcm.components.appweather",
    "sk.iway.iwcm.components.appimpressslideshow",
    "sk.iway.iwcm.components.carouselslider"
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
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        //TODO: JAKARTA multipartResolver.setMaxUploadSize(-1);
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
        http.addFilterAfter(new SetCharacterEncodingFilter(), ApiTokenAuthFilter.class);

        // filter for setting non filter header for files "X-Robots-Tag","noindex, nofollow"
        http.addFilterAfter(new FileArchivFilter(), SetCharacterEncodingFilter.class);
    }
}