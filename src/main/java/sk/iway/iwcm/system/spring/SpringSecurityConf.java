package sk.iway.iwcm.system.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.spring.oauth2.OAuth2DynamicErrorHandler;
import sk.iway.iwcm.system.spring.oauth2.OAuth2DynamicSuccessHandler;
import sk.iway.iwcm.system.spring.passkey.PasskeyAuthSuccessHandler;

import java.util.List;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled=true)
public class SpringSecurityConf {

	private static boolean basicAuthEnabled = false;

	/**
	 * BeanPostProcessor that sets a custom AuthenticationSuccessHandler on the
	 * WebAuthnAuthenticationFilter. Replaces the removed withObjectPostProcessor API
	 * from Spring Security 6.x. The WebAuthnConfigurer calls postProcess() on the filter,
	 * which triggers initializeBean(), invoking this BeanPostProcessor.
	 */
	@Bean
	static BeanPostProcessor webAuthnFilterCustomizer() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter filter
						&& bean.getClass().getName().contains("WebAuthnAuthenticationFilter")) {
					filter.setAuthenticationSuccessHandler(new PasskeyAuthSuccessHandler());
					Logger.info(SpringSecurityConf.class, "PassKey success handler set on WebAuthnAuthenticationFilter");
				}
				return bean;
			}
		};
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			@org.springframework.beans.factory.annotation.Autowired(required = false) UserCredentialRepository passkeyUserCredentialRepository,
			@org.springframework.beans.factory.annotation.Autowired(required = false) PublicKeyCredentialUserEntityRepository passkeyUserEntityRepository,
			@org.springframework.beans.factory.annotation.Autowired(required = false) @org.springframework.beans.factory.annotation.Qualifier("webauthnUserDetailsService") org.springframework.security.core.userdetails.UserDetailsService webauthnUserDetailsService) {
		Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure filterChain");
		SpringAppInitializer.dtDiff("configureSecurity START");

		Logger.debug(SpringSecurityConf.class, "SpringSecurityConf - configure auth provider");
		http.authenticationProvider(new WebjetAuthentificationProvider());

		//toto zapne Basic autorizaciu (401) pri neautorizovanom REST volani, inak by request vracal rovno 403 Forbidden
		String springSecurityAllowedAuths = Constants.getString("springSecurityAllowedAuths");
		if (springSecurityAllowedAuths != null && springSecurityAllowedAuths.contains("basic")) {
			Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure http - httpBasic");
			basicAuthEnabled = true; //NOSONAR
			http.httpBasic(customizer -> {});
		}

		// OAuth2 login support
		if (Tools.isNotEmpty(Constants.getString("oauth2_clients"))) {
			Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure http - oauth2Login");
			http.oauth2Login(oauth2 -> {
				oauth2.clientRegistrationRepository(clientRegistrationRepository());
				oauth2.authorizedClientService(authorizedClientService(clientRegistrationRepository()));
				oauth2.successHandler(new OAuth2DynamicSuccessHandler());
				oauth2.failureHandler(new OAuth2DynamicErrorHandler());
			});
		}

		try {
			// WebAuthn/PassKey support
			if (Constants.getBoolean("password_passKeyEnabled")) {
				Logger.info(SpringSecurityConf.class, "SpringSecurityConf - configure http - webAuthn (PassKey)");
				String rpId = Constants.getString("password_passKeyRpId");
				String rpName = Constants.getString("password_passKeyRpName");
				String allowedOriginsStr = Constants.getString("password_passKeyAllowedOrigins");

				// Explicitly set repositories as shared objects so WebAuthnConfigurer uses
				// our JDBC implementations instead of falling back to in-memory Map-based ones
				if (passkeyUserCredentialRepository != null) {
					http.setSharedObject(UserCredentialRepository.class, passkeyUserCredentialRepository);
					Logger.info(SpringSecurityConf.class, "PassKey: using JDBC UserCredentialRepository: " + passkeyUserCredentialRepository.getClass().getName());
				} else {
					Logger.error(SpringSecurityConf.class, "PassKey: UserCredentialRepository bean not found! Credentials will NOT be persisted to database.");
				}
				if (passkeyUserEntityRepository != null) {
					http.setSharedObject(PublicKeyCredentialUserEntityRepository.class, passkeyUserEntityRepository);
					Logger.info(SpringSecurityConf.class, "PassKey: using JDBC PublicKeyCredentialUserEntityRepository: " + passkeyUserEntityRepository.getClass().getName());
				} else {
					Logger.error(SpringSecurityConf.class, "PassKey: PublicKeyCredentialUserEntityRepository bean not found!");
				}
				if (webauthnUserDetailsService != null) {
					http.setSharedObject(org.springframework.security.core.userdetails.UserDetailsService.class, webauthnUserDetailsService);
					Logger.info(SpringSecurityConf.class, "PassKey: using WebjetWebAuthnUserDetailsService");
				} else {
					Logger.error(SpringSecurityConf.class, "PassKey: UserDetailsService bean not found!");
				}

				http.webAuthn(webAuthn -> {
					webAuthn.rpId(rpId);
					webAuthn.rpName(rpName);
					webAuthn.allowedOrigins(Tools.getTokens(allowedOriginsStr, ","));
					webAuthn.disableDefaultRegistrationPage(true);
				});

				// Note: WebAuthn filter success handler is customized via webAuthnFilterCustomizer BeanPostProcessor
			}
		} catch (Exception e) {
			Logger.error(SpringSecurityConf.class, "Error configuring WebAuthn support", e);
		}

		// Enable session fixation protection by migrating the session on authentication
		http.sessionManagement(session ->
			session.sessionFixation(sessionFixation -> sessionFixation.migrateSession())
		);
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

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		List<String> clients = List.of(Tools.getTokens(Constants.getString("oauth2_clients"), ","));
		List<ClientRegistration> registrations = clients.stream()
				.map(this::buildClientRegistration)
				.filter(registration -> registration != null)
				.toList();
		// Ak je zoznam prázdny, vráť anonymnú implementáciu ClientRegistrationRepository namiesto InMemoryClientRegistrationRepository
		if (registrations.isEmpty()) {
			return new ClientRegistrationRepository() {
				@Override
				public ClientRegistration findByRegistrationId(String registrationId) {
					return null;
				}
			};
		}
		return new InMemoryClientRegistrationRepository(registrations);
	}

	private ClientRegistration buildClientRegistration(String providerId) {
		String clientId = Constants.getString("oauth2_" + providerId + "ClientId");
		String clientSecret = Constants.getString("oauth2_" + providerId + "ClientSecret");
		if (Tools.isAnyEmpty(clientId, clientSecret)) return null;
		// known providers
		if ("google".equalsIgnoreCase(providerId)) {
			return CommonOAuth2Provider.GOOGLE.getBuilder(providerId)
					.clientId(clientId)
					.clientSecret(clientSecret)
					.build();
		}
		if ("facebook".equalsIgnoreCase(providerId)) {
			return CommonOAuth2Provider.FACEBOOK.getBuilder(providerId)
					.clientId(clientId)
					.clientSecret(clientSecret)
					.build();
		}
		if ("github".equalsIgnoreCase(providerId)) {
			return CommonOAuth2Provider.GITHUB.getBuilder(providerId)
					.clientId(clientId)
					.clientSecret(clientSecret)
					.build();
		}
		if ("okta".equalsIgnoreCase(providerId)) {
			return CommonOAuth2Provider.OKTA.getBuilder(providerId)
					.clientId(clientId)
					.clientSecret(clientSecret)
					.build();
		}
		// other providers - load all necessary parameters
		String authorizationUri = Constants.getString("oauth2_" + providerId + "AuthorizationUri");
		String tokenUri = Constants.getString("oauth2_" + providerId + "TokenUri");
		String userInfoUri = Constants.getString("oauth2_" + providerId + "UserInfoUri");
		String jwkSetUri = Constants.getString("oauth2_" + providerId + "JwkSetUri");
		String issuerUri = Constants.getString("oauth2_" + providerId + "IssuerUri");
		String userNameAttributeName = Constants.getString("oauth2_" + providerId + "UserNameAttributeName", "email");
		String scopesStr = Constants.getString("oauth2_" + providerId + "Scopes", "openid,profile,email");
		String clientName = Constants.getString("oauth2_" + providerId + "ClientName", providerId);
		if (Tools.isAnyEmpty(authorizationUri, tokenUri, userInfoUri, jwkSetUri, issuerUri)) return null;
		return ClientRegistration.withRegistrationId(providerId)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.scope(scopesStr.split(","))
				.authorizationUri(authorizationUri)
				.tokenUri(tokenUri)
				.userInfoUri(userInfoUri)
				.userNameAttributeName(userNameAttributeName)
				.jwkSetUri(jwkSetUri)
				.issuerUri(issuerUri)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.clientName(clientName)
				.authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
				.build();
	}

	@Bean
	public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}
}
