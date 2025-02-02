package sk.iway.iwcm.system.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

import sk.iway.iwcm.Logger;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled=true)
public class SpringSecurityConf extends WebSecurityConfigurerAdapter
{
	private static boolean basicAuthEnabled = false;

	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception
	{
		Logger.debug(SpringSecurityConf.class, "SpringSecurityConf - configure auth");
		auth.authenticationProvider(new WebjetAuthentificationProvider());
    }

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

    @Override
	protected void configure(HttpSecurity http) throws Exception
	{
		//System.out.println("---------- CONFIGURE SECURITY -------------");

    	Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure http");

		//toto zapne Basic autorizaciu (401) pri neautorizovanom REST volani, inak by request vracal rovno 403 Forbidden
		String springSecurityAllowedAuths = SpringAppInitializer.getConstant("springSecurityAllowedAuths");
		if (springSecurityAllowedAuths != null && springSecurityAllowedAuths.contains("basic")) {
			Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure http - httpBasic");
			basicAuthEnabled = true;
			http.httpBasic();
		}

		//toto nastavuje WebJET - https://docs.spring.io/spring-security/site/docs/4.2.x/reference/html/headers.html
      http.headers().xssProtection().disable();
		http.headers().frameOptions().disable();
		http.headers().contentTypeOptions().disable();
		http.headers().httpStrictTransportSecurity().disable();
		http.csrf().disable();

		// configure security from BaseSpringConfig
		configureSecurity(http, "sk.iway.iwcm.system.spring.BaseSpringConfig");

		if (SpringAppInitializer.installName != null)
		{
			//WebJET 9
			configureSecurity(http, "sk.iway.webjet.v9.V9SpringConfig");
			//custom InstallName config
			configureSecurity(http, "sk.iway." + SpringAppInitializer.installName + ".SpringConfig");
		}

		if (SpringAppInitializer.logInstallName != null)
		{
			configureSecurity(http, "sk.iway." + SpringAppInitializer.logInstallName + ".SpringConfig");
		}

		/* JEEFF: toto sa zda, ze nic nerobi, breakpoint vo vnutri nikdy nenastal, takze zakomentovavam
		http.exceptionHandling().accessDeniedHandler(new AccessDeniedHandlerImpl() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException, ServletException {
                //super.handle(request, response, accessDeniedException);
            	response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

				//response.getWriter().println("Access denied");
                //
                // Your Code Here
                //

            }

            @Override
            public void setErrorPage(String errorPage) {
                super.setErrorPage(errorPage);

                //
                // Your Code Here
                //

            }
		  });
		  */

		// #39691 jeeff: tymto som chcel nastavit handlovanie chyby, ked neposlem heslo k REST sluzbe ktora ho chce, ale nefunguje to
		//riesi sa to hore cez http.httpBasic()
		// http.exceptionHandling().authenticationEntryPoint(new WebjetAuthentificationEntryPoint());
	}

	@Bean
	public HttpFirewall webjetHttpFirewall() {
		//StrictHttpFirewall firewall = new StrictHttpFirewall();
		//firewall.setAllowUrlEncodedSlash(true);

		//pouzivame defaultfirewall aby nam spring nezastavil URL vo formate /sk//, to spracuje az nasledne PathFilter, ktory to presmeruje na /sk/
		DefaultHttpFirewall firewall = new DefaultHttpFirewall();
		return firewall;
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		Logger.debug(SpringSecurityConf.class, "configure web security, setting default firewall");
		super.configure(web);

		web.httpFirewall(webjetHttpFirewall());
	}

	protected void configureSecurity(HttpSecurity http, String className)
	{
		Logger.info(SpringSecurityConf.class, "configure - SpringAppInitializer - start - " + className);

		try
		{
			Class<?> configClass = Class.forName(className);
			if (ConfigurableSecurity.class.isAssignableFrom(configClass))
			{
				ConfigurableSecurity cs = (ConfigurableSecurity) configClass.getDeclaredConstructor().newInstance();
				cs.configureSecurity(http);
			}
		} catch (Exception e)
		{
			// config class asi neexistuje.
		}

		Logger.info(SpringSecurityConf.class, "configure - SpringAppInitializer - end - " + className);
	}

	/**
	 * Returns true if Basic Auth is enabled, it is initilized on startup,
	 * so tests can't rely on springSecurityAllowedAuths conf value
	 * @return
	 */
	public static boolean isBasicAuthEnabled()
	{
		return basicAuthEnabled;
	}
}
