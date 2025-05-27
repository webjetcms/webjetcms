package sk.iway.iwcm.system.spring;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.core.Configurable;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FreemarkerHelpers;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

@Configuration
@EnableWebMvc
@EnableAsync
@ComponentScan({
    "sk.iway.iwcm.system.spring.components"
})
public class BaseSpringConfig implements WebMvcConfigurer, ConfigurableSecurity
{
    @Autowired
    ApplicationContext applicationContext;

    private static final Charset UTF8 = StandardCharsets.UTF_8;

//    @Value("${springfox.documentation.swagger.v2.path}")
//    private String swagger2Endpoint;
    @Override
    public void configureSecurity(HttpSecurity http) throws Exception
    {
        Logger.println(BaseSpringConfig.class, "-------> Configure security, http="+http);
        SpringAppInitializer.dtDiff("Configure security START");

        http
                .authorizeHttpRequests()
                    .requestMatchers("/private/rest/**","/webjars/**").authenticated()
                    .requestMatchers("/swagger-ui**", "/admin/rest/**").hasRole("Group_admin")
                    //toto nemoze byt, pokazi to custom SpringConfig kde sa nastavuje security .anyRequest().permitAll()
                /*.and()
                    .formLogin()
                    .loginPage("/admin/logon/")
                    .loginProcessingUrl("/admin/logon/")*/
                ;

        SpringAppInitializer.dtDiff("Configure security DONE");
    }

    @Bean
    public GroupedOpenApi api() {

        Predicate<String> paths;
        //TODO: JAKARTA if (Constants.getBoolean("swaggerEnabled")) paths = PathSelectors.any();
        //else paths = PathSelectors.none();

        Logger.println(BaseSpringConfig.class, "-------> Docket api()");
        SpringAppInitializer.dtDiff("Docket api() START");

        GroupedOpenApi docket = GroupedOpenApi.builder()
                .group("webjet-api")
                .addOpenApiMethodFilter(method -> method.isAnnotationPresent(RestController.class))
                .build();

        SpringAppInitializer.dtDiff("Docket api() END");
        return docket;
    }

    @Bean
    public OpenAPI apiInfo() {
        OpenAPI apiInfo = new OpenAPI()
                .info(new Info().title("WebJET API")
                .description("For more info visit https://docs.webjetcms.sk or http://github.com/webjetcms/webjetcms/")
                .version(InitServlet.getActualVersion()));
        return apiInfo;
    }

    /**
     * Nastavenie MAX velkosti stranky pre Spring data (bola to dost fuska...)
     * vid: https://stackoverflow.com/questions/35397912/how-to-configure-pageablehandlermethodargumentresolver-in-spring-boot-application
     * @return
     */
    @Bean
    PageableHandlerMethodArgumentResolverCustomizer sortCustomizer() {
        Logger.println(BaseSpringConfig.class, "-------> sortCustomizer()");
        return p -> p.setMaxPageSize(Integer.MAX_VALUE);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        Logger.println(BaseSpringConfig.class, "-------> configureMessageConverters(), size="+converters.size());

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(new MediaType("text", "plain", UTF8));
        mediaTypes.add(new MediaType("text", "html", UTF8));
        mediaTypes.add(new MediaType("application", "json", UTF8));
        stringConverter.setSupportedMediaTypes(mediaTypes);
        converters.add(stringConverter);

        //aby isla tlac do PDF (application/octet-stream)
        converters.add(new ResourceHttpMessageConverter(true));
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        Logger.println(BaseSpringConfig.class, "-------> addFormatters()");
        //super.addFormatters(registry);
        registry.addConverter(new DateConverter());
        registry.addConverter(new LongDateTimeConverter());
    }

    @Bean
    public FreeMarkerConfigurer freemarkerConfig(HttpServletRequest request) {
        Logger.println(BaseSpringConfig.class, "-------> 1 freemarkerConfig()");

        FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
        Map<String, Object> map = new HashMap<>();
        map.put("request", request);
        // z nejakeho dovodu robi problem pri inicializacii webjetu
//        map.put("currentUser", UsersDB.getCurrentUser(session));
        map.put("Tools", new Tools());
        map.put("WebjetHelpers", new FreemarkerHelpers(request));
        freeMarkerConfigurer.setFreemarkerVariables(map);
        freeMarkerConfigurer.setDefaultEncoding("windows-1250");


        Properties settings = new Properties();
        settings.put("auto_import", "spring.ftl as spring");
        settings.put(Configurable.NUMBER_FORMAT_KEY, "computer"); // vypisanie cisiel bez medzier
//        settings.put("template_exception_handler", getFreemarkerExceptionHandler());
//        settings.put("url_escaping_charset", WebConstants.CHAR_SET_UTF_8);
        freeMarkerConfigurer.setFreemarkerSettings(settings);

        // treba prazdny string, inac neresolvuje freemarker views
        freeMarkerConfigurer.setTemplateLoaderPath("");

        SpringAppInitializer.dtDiff("freemarkerConfig DONE");
        return freeMarkerConfigurer;
    }

    /**
     * jeeff: toto potrebujeme ako default handler na staticke subory, v SpringAppInitializer sa bindne Spring dispatcher na / a on nehandluje css, html atd
     * https://stackoverflow.com/questions/29394493/spring-mvc-configuration-enable
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        Logger.println(BaseSpringConfig.class, "-------> configureDefaultServletHandling()");
        configurer.enable();
        SpringAppInitializer.dtDiff("configureDefaultServletHandling DONE");
    }
}
