package sk.iway.iwcm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;

/**
 *  RequestBean.java - drzi zakladne info z requestu, uklada sa do hash tabulky podla thread ID
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Date: 16.12.2008 10:28:07
 *@modified     $Date: 2010/02/09 08:31:11 $
 */
public class RequestBean
{
	private int userId;
	private String remoteIP;
	private String remoteHost;
	private String baseHref;
	private String lng;
	private String url;
	private String queryString;
	private String userAgent;
	private String domain;
	private String httpProtocol;
	private String serverName;
	private int httpPort;
	private String sessionId;
	private String cryptoPrivateKey;
	private boolean isUserAdmin = false;
	private Map<String, String[]> parameters = new HashMap<>();
	private String headerOrigin;
	private List<String> allowedParameters = new LinkedList<>();
	private Map<String, String[]> auditValues = new HashMap<>();

	private List<String> errors;

	private HttpServletRequest request;
	private String referrer;

	private List<String> textKeyPrefixes = null;

	private ApplicationContext springContext;

	//umoznuje prenasat atributy podobne ako request.set/getAttribute
	private Map<String, Object> attributes;

	private int docId;
	private int groupId;

	public int getUserId()
	{
		return userId;
	}
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
	public String getRemoteIP()
	{
		return remoteIP;
	}
	public void setRemoteIP(String remoteIP)
	{
		this.remoteIP = remoteIP;
	}
	public String getRemoteHost()
	{
		return remoteHost;
	}
	public void setRemoteHost(String remoteHost)
	{
		this.remoteHost = remoteHost;
	}
	public String getBaseHref()
	{
		return baseHref;
	}
	public void setBaseHref(String baseHref)
	{
		this.baseHref = baseHref;
	}
	public String getLng()
	{
		return lng;
	}
	public void setLng(String lng)
	{
		this.lng = lng;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getQueryString()
	{
		return queryString;
	}
	public void setQueryString(String queryString)
	{
		this.queryString = queryString;
	}
	public String getUserAgent()
	{
		return userAgent;
	}
	public void setUserAgent(String userAgent)
	{
		this.userAgent = userAgent;
	}
	public String getDomain()
	{
		return domain;
	}
	public void setDomain(String domain)
	{
		this.domain = domain;
	}
	public String getSessionId()
	{
		return sessionId;
	}
	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}
	public String getCryptoPrivateKey() { return cryptoPrivateKey; }
	public void setCryptoPrivateKey(String cryptoPrivateKey) { this.cryptoPrivateKey = cryptoPrivateKey; }

    /**
	 * Ziska HTTP request, nenastavi sa tam ale automaticky ale len pre urcite pripady, vid SetCharacterEncodingFilter
	 * @return
	 */
	public HttpServletRequest getRequest()
	{
		return request;
	}
	public void setRequest(HttpServletRequest request)
	{
		this.request = request;
	}

	public boolean isUserAdmin()
	{
		return isUserAdmin;
	}
	public void setUserAdmin(boolean isUserAdmin)
	{
		this.isUserAdmin = isUserAdmin;
	}

	public Map<String, String[]> getParameters() {
		if (allowedParameters != null && allowedParameters.size() > 0) {
			parameters = parameters.entrySet().stream().filter(x -> allowedParameters.contains(x.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		return parameters;
	}

	protected Map<String, String[]> getAllParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String[]> parameters) {
		LinkedHashMap<String, String[]> map = new LinkedHashMap<>();
		map.putAll(parameters);
		this.parameters = map;
	}

	public Enumeration<String> getParameterNames()
	{
		return Collections.enumeration(getParameters().keySet());
	}

	public String getParameter(String parameter)
	{
		if (getAllParameters().containsKey(parameter)) {
			return getAllParameters().get(parameter)[0];
		}

		return null;
	}

	public String[] getParameterValues(String parameter)
	{
		if (getAllParameters().containsKey(parameter)) {
			return getAllParameters().get(parameter);
		}

		return null;
	}

	public boolean hasParameter(String key)
	{
		return getAllParameters().containsKey(key);

	}

	public String getReferrer() {
		return referrer;
	}

	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}

	public List<String> getAllowedParameters() {
		return allowedParameters;
	}

	public void setAllowedParameters(List<String> allowedParameters) {
		this.allowedParameters = allowedParameters;
	}

	public static void addAllowedParameter(String allowedParameter) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			List<String> parameters = requestBean.getAllowedParameters();
			parameters.add(allowedParameter);
			requestBean.setAllowedParameters(parameters);
		}
	}

	public static boolean hasAllowedParameters() {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			return requestBean.getAllowedParameters() != null && !requestBean.getAllowedParameters().isEmpty();
		}

		return false;
	}

	public static void addParameter(String key, String value) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			Map<String, String[]> parameters = requestBean.getAllParameters();
			String[] val = parameters.get(key);
			if (val != null) {
				List<String> strings = new ArrayList<>(Arrays.asList(val));
				strings.add(value);
				val = strings.toArray(new String[0]);
			}
			else {
				val = new String[]{value};
			}

			parameters.put(key, val);
			requestBean.setParameters(parameters);
		}
	}

	public static void addParameter(String key, String[] value) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			Map<String, String[]> parameters = requestBean.getAllParameters();
			parameters.put(key, value);
			requestBean.setParameters(parameters);
		}
	}

	public Map<String, String[]> getAuditValues(boolean createIfNull) {
		if (createIfNull && auditValues == null) {
			auditValues = new LinkedHashMap<>();
		}
		return auditValues;
	}

	/**
	 * Add audit value for use with AuditEntityListener
	 * @param key
	 * @param value
	 */
	public static void addAuditValue(String key, String value) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			Map<String, String[]> auditValues = requestBean.getAuditValues(true);
			String[] val = auditValues.get(key);
			if (val != null) {
				List<String> strings = new ArrayList<>(Arrays.asList(val));
				strings.add(value);
				val = strings.toArray(new String[0]);
			}
			else {
				val = new String[]{value};
			}

			auditValues.put(key, val);
		}
	}

	public static void removeAuditValue(String key) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			Map<String, String[]> auditValues = requestBean.getAuditValues(false);
			if (auditValues != null) {
				auditValues.remove(key);
			}
		}
	}

	public static void addError(String error) {
		addError(error, true);
	}

	public static void addError(String error, boolean addToEnd) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			List<String> errors = requestBean.getErrors();

			if (errors == null) {
				errors = new ArrayList<>();
			}
			if (addToEnd) errors.add(error);
			else errors.add(0, error);

			requestBean.setErrors(errors);
		}
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public String getErrorsString()
	{
		return Tools.join(getErrors(), ",");
	}

	/**
	 * Umozni nastavit atribut vramci requestu podobne ako request.setAttribute
	 * @param key
	 * @param value
	 */
	public static void setAttribute(String key, Object value) {
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null) return;

		if (rb.attributes==null) rb.attributes = new HashMap<>();
		rb.attributes.put(key, value);
	}

	/**
	 * Umozni ziskat atribut vramci requestu podobne ako request.setAttribute
	 * @param key
	 * @return
	 */
	public static Object getAttribute(String key) {
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null) return null;

		if (rb.attributes == null) return null;
		return rb.attributes.get(key);
	}

    /**
     * Prida prefix pre textove kluce, nasledne sa hlada preklad podla prefixu (posledny v zozname ma najvacsiu prioritu)
     * @param prefix
     * @param addToLastPosition
     */
    public static void addTextKeyPrefix(String prefix, boolean addToLastPosition) {
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean != null) {
            if (requestBean.textKeyPrefixes==null) requestBean.textKeyPrefixes = new ArrayList<>();
            if (addToLastPosition) requestBean.textKeyPrefixes.add(prefix);
            else requestBean.textKeyPrefixes.add(0, prefix);
        }

    }

    /**
     * Vrati zoznam prefixov pre textove kluce
     * @return
     */
    public static List<String> getTextKeyPrefixes() {
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean != null) return requestBean.textKeyPrefixes;

        return null;
    }

	public ApplicationContext getSpringContext() {
		return springContext;
	}

	public void setSpringContext(ApplicationContext springContext) {
		this.springContext = springContext;
	}

	public <T> T getSpringBean(String name, Class<T> clazz) {
        if (springContext == null || !springContext.containsBean(name)) {
            return null;
        }

        return springContext.getBean(name, clazz);
    }

	/**
	 * Staticka metoda pre zistenie, ci je prihlaseny admin
	 * @return
	 */
	public static boolean isAdminLogged()
	 {
		 RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		 if (requestBean != null) {
			 return requestBean.isUserAdmin;
		 }
		 return false;
	 }
	/**
	 * Vrati protokol/schemu pouziteho HTTP requestu
	 * @return
	 */
	public String getHttpProtocol() {
		return httpProtocol;
	}
	public void setHttpProtocol(String httpProtocol) {
		this.httpProtocol = httpProtocol;
	}
	/**
	 * Vrati port HTTP pripojenia
	 */
	public int getHttpPort() {
		return httpPort;
	}
	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}
	/**
	 * Vrati request.getServerName(), moze byt odlisne od getDomain,
	 * ktora berie do uvahy aj nastavenu domenu stranky
	 * @return
	 */
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	/**
	 * Vrati hodnotu HTTP hlavicky origin
	 * @return
	 */
	public String getHeaderOrigin() {
		return headerOrigin;
	}
	public void setHeaderOrigin(String headerOrigin) {
		this.headerOrigin = headerOrigin;
	}
	public int getDocId() {
		return docId;
	}
	public void setDocId(int docId) {
		this.docId = docId;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
}