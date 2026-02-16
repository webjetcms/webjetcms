package sk.iway.iwcm.system.ntlm;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.StringTokenizer;

/**
 *
 */
public class AuthenticationFilter implements Filter {

	private static FilterConfig config;

 	@Override
    public void init( FilterConfig filterConfig ) throws ServletException {
		AuthenticationFilter.config = filterConfig;
    }

    @Override
    public void destroy() {
    }

    /**
     * This method simply calls <tt>negotiate( req, resp, false )</tt>
     * and then <tt>chain.doFilter</tt>. You can override and call
     * negotiate manually to achive a variety of different behavior.
     */
    @Override
   	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
 	{
   	 	Logger.debug(AuthenticationFilter.class, "doFilter");

        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;

        Enumeration<String> e = req.getHeaderNames();
        String name, value;
        while (e.hasMoreElements())
        {
           name = (String)e.nextElement();
           value = req.getHeader(name);

           Logger.debug(AuthenticationFilter.class, "header " + name + "=" + value);
        }

        Principal user = negotiateIIS(req, resp, false);

        chain.doFilter( new NtlmHttpServletRequest( req, user ), response );
    }

	/**
	 * Negotiate user from request.getUserPrincipal(), check for correct domainName from NTLMiisTrustedDomains
	 * @param req
	 * @param resp
	 * @param skipAuthentication
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
   	public static Principal negotiateIIS(HttpServletRequest req, HttpServletResponse resp, boolean skipAuthentication)
				throws IOException, ServletException
	{
		Logger.debug(AuthenticationFilter.class, "negotiate IIS");
		// IIS autorizacia aktivovana
		Principal iisPrincipal = Tools.getUserPrincipal(req);
		Logger.debug(AuthenticationFilter.class, "IIS Principal: " + iisPrincipal);
		if (iisPrincipal != null)
		{
			String userDomain[] = iisPrincipal.getName().split("\\\\");
			if (userDomain.length == 2)
			{
				Logger.debug(AuthenticationFilter.class, "domain: " + userDomain[0] + " login:" + userDomain[1]);
				boolean domainOK = false;
				String logonDomains = Constants.getString("NTLMiisTrustedDomains");
				if (Tools.isNotEmpty(logonDomains))
				{
					StringTokenizer st = new StringTokenizer(logonDomains, ",;");
					while (st.hasMoreTokens())
					{
						String domain = st.nextToken();
						Logger.debug(AuthenticationFilter.class, "Testing domain: "+domain+" vs "+userDomain[0]);
						if (domain.equalsIgnoreCase(userDomain[0]))
						{
							domainOK = true;
							break;
						}
					}
				}
				else
				{
					domainOK = true;
				}
				if (domainOK)
				{
					NtlmPrincipal principal = new NtlmPrincipal(userDomain[0], userDomain[1]);
					return principal;
				}
				else
				{
					Logger.debug(AuthenticationFilter.class, "Nespravna domena: "+userDomain[0]);
				}
			}
			else if ("*".equals(Constants.getString("NTLMiisTrustedDomains")))
			{
				NtlmPrincipal principal = new NtlmPrincipal("", userDomain[0]);
				return principal;
			}
		}
		else
		{
			Logger.debug(AuthenticationFilter.class, "nemam principal, user nie je prihlaseny!");
		}
		return null;
	}

    /**
 	 * @return Returns the ldapPassword.
 	 */
 	public static String getLdapPassword()
 	{
 		return Constants.getString("ldapPassword", getInitParameter("jcifs.ldap.password"));
 	}
 	/**
 	 * @return Returns the ldapProvider.
 	 */
 	public static String getLdapProvider()
 	{
		return Constants.getString("ldapProviderUrl", getInitParameter("jcifs.ldap.provider"));
 	}
 	/**
 	 * @return Returns the ldapUsername.
 	 */
 	public static String getLdapUsername()
 	{
		return Constants.getString("ldapUsername", getInitParameter("jcifs.ldap.username"));
 	}
 	/**
 	 * @return Returns the forbiddenURL.
 	 */
 	public static String getForbiddenURL()
 	{
 		return Constants.getString("NTLMForbiddenURL", weTrustIIS() ? "/500.jsp" : null);
 	}

	/**
	 * ak je vo web.xml zadany config parameter iis.trustIIS, beriem primarne
	 * inak beriem co je zadane vo WJ konf. premennej authenticationTrustIIS
	 * @return
	 */
	public static boolean weTrustIIS()
	{
		 boolean trustIIS = Constants.getBoolean("authenticationTrustIIS");
		 String configTrustIIS = getInitParameter("iis.trustIIS");
		 if(Tools.isNotEmpty(configTrustIIS)) trustIIS = "true".equalsIgnoreCase(configTrustIIS);

		 //Logger.debug(AuthenticationFilter.class, "weTrustIIS()="+trustIIS+", konf (authenticationTrustIIS)="+Constants.getString("authenticationTrustIIS")+", initParam (iis.trustIIS)="+Config.getProperty("iis.trustIIS"));
		 return trustIIS;
	}

	public static String getDomainController()
	{
		String result = Constants.getString("NTLMDomainController", getInitParameter( "jcifs.http.domainController"));
		if(Tools.isEmpty(result))
			result = getInitParameter("jcifs.smb.client.domain");

		return result;
	}

	private static String getInitParameter(String name) {
		if (config != null) return config.getInitParameter(name);
		return "";
	}

}

