package sk.iway.iwcm.system.spring;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.spring.components.SpringContext;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class WebjetAuthentificationProvider implements AuthenticationProvider
{

	@Override
	public Authentication authenticate(Authentication auth) throws AuthenticationException {
		Logger.debug(WebjetAuthentificationProvider.class, "WebjetAuthentificationProvider - authenticate - " + auth.getPrincipal());

        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attr.getRequest();
        Prop prop = Prop.getInstance(request);

		boolean isLogonBlocked = LogonTools.isLoginBlocked(request);
        if (isLogonBlocked)
        {
           Logger.debug(WebjetAuthentificationProvider.class, "Bad credentials - logon blocked timeout - " + auth.getPrincipal());
           throw new BadCredentialsException("Bad credentials - logon blocked timeout");
        }

		UserDetails user = null;
		if (Constants.getBoolean("emailAuthentification"))
		{
			user = UsersDB.getUserByEmail(String.valueOf(auth.getPrincipal()), 1);

            //pre admina ziskajme udaje aj ked je nastavene email prihlasovanie (ak sa user nenajde), niekedy moze byt email neznamy a nedalo by sa vobec prihlasit
            if (user == null && "admin".equalsIgnoreCase(String.valueOf(auth.getPrincipal()))) {
                user = UsersDB.getUser(String.valueOf(auth.getPrincipal()));
            }

		} else {
			user = UsersDB.getUser(String.valueOf(auth.getPrincipal()));
		}

        Identity adminIdentity = null;

		String url = request.getRequestURI();
		if (url.startsWith("/admin"))
        {
            adminIdentity = new Identity();
            Map<String, String> errors = new Hashtable<>();
            String forward = LogonTools.logon((String)auth.getPrincipal(), (String) auth.getCredentials(), adminIdentity, errors, request, prop);
            if (errors.get("ERROR_KEY")==null && "logon_ok_admin".equals(forward))
            {
                adminIdentity.setValid(true);
                return LogonTools.setUserToSession(request.getSession(), adminIdentity);
            }
        }
        else
        {
            List<String> errorKeys = LogonTools.logonUser(request, (String) auth.getPrincipal(), (String) auth.getCredentials());
		    if (errorKeys==null || errorKeys.isEmpty())
            {
                //get user from session, it cab be set by logonUser method (eg. in BasicLdapLogon)
                Identity identity = UsersDB.getCurrentUser(request);
                if (identity == null)
                {
                    //was not set, use user from DB
                    identity = new Identity(user);
                }

                return LogonTools.setUserToSession(request.getSession(), identity);
            }
            else
            {
                StringBuilder message = new StringBuilder();
                for (String key : errorKeys)
                {
                    message.append(prop.getText(key));
                }

                throw new BadCredentialsException(message.toString());
            }
        }

        user = UsersDB.getCurrentUser(request);// getUser(userId);
        if (user != null)
        {
            return LogonTools.setUserToSession(request.getSession(), new Identity(user));
        }

		throw new BadCredentialsException("Bad credentials");
	}

	public static Authentication authenticate(Identity user)
    {
        if (user != null && user.isAuthorized())
        {
            List<GrantedAuthority> grantedAuths = new ArrayList<>();
            for (String groupName : Tools.getTokens(user.getUserGroupsNames(), ","))
            {
                groupName = WebjetSecurityService.normalizeUserGroupName(groupName);
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_Group_" + groupName));
            }

            Identity i = new Identity(user);
            //i.isEnabledItem(name)

            if (user.isAdmin())
            {
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_Group_admin"));
            }

            for (ModuleInfo mod : Modules.getInstance().getAvailableModules())
            {
                if (i.isDisabledItem(mod.getItemKey())==false && user.isDisabledItem(mod.getItemKey())==false) {
                    String itemKey = WebjetSecurityService.normalizeUserGroupName(mod.getItemKey());
                    grantedAuths.add(new SimpleGrantedAuthority("ROLE_Permission_" + itemKey));
                }

                //ries aj submenu
                if (mod.getSubmenus()!=null)
                {
                    for (ModuleInfo submod : mod.getSubmenus())
                    {
                        if (Tools.isEmpty(submod.getItemKey())) continue;
                        if (i.isDisabledItem(submod.getItemKey())) continue;
                        if (user.isDisabledItem(submod.getItemKey())) continue;

                        String itemKey = WebjetSecurityService.normalizeUserGroupName(submod.getItemKey());
                        grantedAuths.add(new SimpleGrantedAuthority("ROLE_Permission_" + itemKey));
                    }
                }
            }

            /*
             Pridanie custom pravidiel per project.
             priklad hodnoty customSpringAuthenticate=SiovSecurityService
             @See pre priklad sk/iway/siov_kolaboracny/cooperation/service/SiovSecurityService.java
             */
            try {
                String customSpringAuthenticate = Constants.getString("customSpringAuthenticate");
                if (Tools.isNotEmpty(customSpringAuthenticate)) {
                    WebjetAuthentificationInterface webjetAuthentificationInterface = SpringContext.getApplicationContext().getBean(customSpringAuthenticate, WebjetAuthentificationInterface.class);
                    grantedAuths.addAll(webjetAuthentificationInterface.create(user));
                }
            } catch (Exception e) {
                sk.iway.iwcm.Logger.error(e);
            }

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    user.getLogin(),
                    "password",
                    grantedAuths);
            return auth;
        }
        return null;
    }

	 @Override
	public boolean supports(Class<?> authentication)
	{
		 Logger.debug(WebjetAuthentificationProvider.class, "WebjetAuthentificationProvider - supports - " + authentication.getName());
		 return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

}