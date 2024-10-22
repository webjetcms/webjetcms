package sk.iway.iwcm.system.spring;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

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
import sk.iway.iwcm.FreemarkerHelpers;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
//@PropertySource("classpath:swagger.properties")
@EnableSwagger2
@EnableWebMvc
@EnableAsync
//@EnableJpaRepositories
//@EnableTransactionManagement
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
    }

    @Bean
    public Docket api() {
        Logger.println(BaseSpringConfig.class, "-------> Docket api()");
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build().apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("WebJet API")
                .description("WebJET services")
                .version("")
                .license("")
                .licenseUrl("https://www.interway.sk/kontakt/")
                .contact(new Contact("Interway","http://www.interway.sk","web@interway.sk"))
                .build();
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

    /*
    JEEFF: nemozeme to mat async, lebo nam to potom robi problem na WebjetEvent - ON_START a AFTER_SAVE co sa vykonava asynchronne, co nechceme
    async je potrebne zabezpecit v listeneri anotaciou @Async
    dokumentacia: http://docs.webjetcms.sk/v2021/#/developer/backend/events

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster
                = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }*/

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
    }

    /* TOTO JE POTREBNE PRE TOMCAT 7 pretoze tam nie je WebjetComponentSpringConfig
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix("/");
        viewResolver.setSuffix(".jsp");

        return viewResolver;
    }
    */

    /*
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();

        em.setPersistenceXmlLocation("classpath:META-INF/persistence-webjet.xml");
        em.setDataSource(dataSource());

        String scanPackages = Constants.getString("jpaAddPackages");
        em.setPackagesToScan(scanPackages);

        JpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        Properties properties = new Properties();
        properties.setProperty("eclipselink.weaving", "false");
        em.setJpaProperties(properties);

        return em;
    }

    @Bean
    public DataSource dataSource(){
        DataSource dataSource = DBPool.getInstance().getDataSource("iwcm");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);

        return transactionManager;
    }
    */
}
