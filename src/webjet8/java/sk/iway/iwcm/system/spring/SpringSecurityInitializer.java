package sk.iway.iwcm.system.spring;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;


public class SpringSecurityInitializer extends AbstractSecurityWebApplicationInitializer
{
	public SpringSecurityInitializer() {
        super(SpringSecurityConf.class);
    }
}
