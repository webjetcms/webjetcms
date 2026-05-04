package sk.iway.iwcm.system.spring;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface ConfigurableSecurity
{
	void configureSecurity(HttpSecurity http) throws Exception;
}
