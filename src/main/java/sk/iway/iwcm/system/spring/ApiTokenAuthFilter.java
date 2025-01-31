package sk.iway.iwcm.system.spring;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.web.filter.GenericFilterBean;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/**
 * SpringSecurity filter na prihlasovanie sa pomocou API tokenu
 * Pridany do chain je v V9SpringConfig.configureSecurity
 */
public class ApiTokenAuthFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        boolean logged = logUserViaApiKey((HttpServletRequest)request, (HttpServletResponse)response);
        chain.doFilter(request, response);
		if (logged) {
			((HttpServletRequest)request).getSession().invalidate();
		}
    }

    /**
	 * Prihlasenie pouzivatela pomocou tokenu v HTTP hlavicke
	 * Token je vo formate base64(login:token) a v DB je ulozeny v api_key ako salt|hash
	 * @param request
	 * @param response
	 * @return
	 */
	private static boolean logUserViaApiKey(HttpServletRequest request, HttpServletResponse response) {
		try {
			String springSecurityAllowedAuths = Constants.getString("springSecurityAllowedAuths");
			if (springSecurityAllowedAuths == null || springSecurityAllowedAuths.contains("api-token")==false) return false;

			String loginToken = request.getHeader(Constants.getString("logonTokenHeaderName"));
			if (Tools.isNotEmpty(loginToken)) {
				try{
					if (Base64.isBase64(loginToken)) {
						Base64 b64 = new Base64();
						loginToken = new String(b64.decode(loginToken.getBytes()));
					}
				} catch (Exception ex) {
					//asi nebolo zakodovane cez base64
				}

				int pos = loginToken.indexOf(":");
				if (pos==-1) pos = loginToken.indexOf("|");

				if (pos < 0) return false;

				String login = loginToken.substring(0, pos);
				String token = loginToken.substring(pos+1);

				Identity user = new Identity(UsersDB.getUser(login));

				//because SetCharacterEncoding filter was not called yet, we need to register data context to correctly call Adminlog (set URl, userId...)
				SetCharacterEncodingFilter.registerDataContext(request);

				Map<String, String> errors = new Hashtable<>();
				LogonTools.logon(login, token, user, errors, request, Prop.getInstance(request));

				Logger.debug(ApiTokenAuthFilter.class, "errors.ERROR_KEY="+errors.get("ERROR_KEY"));

				if (errors.get("ERROR_KEY")==null)
				{
					// Save our logged-in user in the session
					user.setValid(true);
					LogonTools.setUserToSession(request.getSession(), user);
					request.setAttribute("csrfDisabled", "1");

					return true;
				}
			}
		} catch (Exception ex) {
			Logger.error(ApiTokenAuthFilter.class, ex);
		}
		return false;
	}
 }
