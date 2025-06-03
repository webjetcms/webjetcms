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
		SpringAppInitializer.dtDiff("configureSecurity START");

		Logger.debug(SpringSecurityConf.class, "SpringSecurityConf - configure auth provider");
		http.authenticationProvider(new WebjetAuthentificationProvider());

		// Enable Basic Auth if configured
		String springSecurityAllowedAuths = Constants.getString("springSecurityAllowedAuths");
		if (springSecurityAllowedAuths != null && springSecurityAllowedAuths.contains("basic")) {
			Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure http - httpBasic");
			basicAuthEnabled = true;
			http.httpBasic(customizer -> {});
		}

		// Disable headers and CSRF as per original config
		http.headers(headers -> {
			headers.xssProtection(xss -> xss.disable());
			headers.frameOptions(frame -> frame.disable());
			headers.contentTypeOptions(contentType -> contentType.disable());
			headers.httpStrictTransportSecurity(hsts -> hsts.disable());
		});
		http.csrf(csrf -> csrf.disable());

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

		SecurityFilterChain chain = http.build();
		SpringAppInitializer.dtDiff("configureSecurity END");
		return chain;
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
		Logger.info(SpringSecurityConf.class, "configure - SpringSecurityConf - start - " + className);

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

		Logger.info(SpringSecurityConf.class, "configure - SpringSecurityConf - end - " + className);
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
