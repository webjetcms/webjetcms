package sk.iway.iwcm.system.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled=true)
public class SpringSecurityConf {

	private static boolean basicAuthEnabled = false;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure filterChain");

        Logger.debug(SpringSecurityConf.class, "SpringSecurityConf - configure auth provider");
        http.authenticationProvider(new WebjetAuthentificationProvider());

		//toto zapne Basic autorizaciu (401) pri neautorizovanom REST volani, inak by request vracal rovno 403 Forbidden
		String springSecurityAllowedAuths = Constants.getString("springSecurityAllowedAuths");
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

		if (Tools.isNotEmpty(Constants.getInstallName()))
		{
			//WebJET 9
			configureSecurity(http, "sk.iway.webjet.v9.V9SpringConfig");
			//custom InstallName config
			configureSecurity(http, "sk.iway." + Constants.getInstallName() + ".SpringConfig");
		}

		if (Tools.isNotEmpty(Constants.getLogInstallName()))
		{
			configureSecurity(http, "sk.iway." + Constants.getLogInstallName() + ".SpringConfig");
		}

        return http.build();
    }

    @Bean
	public HttpFirewall webjetHttpFirewall() {
		//StrictHttpFirewall firewall = new StrictHttpFirewall();
		//firewall.setAllowUrlEncodedSlash(true);

		Logger.debug(SpringSecurityConf.class, "configure web security, setting default firewall");

		//pouzivame defaultfirewall aby nam spring nezastavil URL vo formate /sk//, to spracuje az nasledne PathFilter, ktory to presmeruje na /sk/
		DefaultHttpFirewall firewall = new DefaultHttpFirewall();
		return firewall;
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
