package sk.iway.iwcm.system.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import sk.iway.iwcm.Tools;

/**
 *  ContextRequest.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.8.2012 9:12:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
//inherited from HttpServletRequest
@SuppressWarnings("deprecation")
public class ContextRequest implements HttpServletRequest
{
	private final HttpServletRequest original;
	private final String contextPath;

	public ContextRequest(HttpServletRequest original)
	{
		this.contextPath = original.getContextPath();
		this.original = original;
	}

	@Override
	public String getParameter(String parameter)
	{
		return ContextFilter.removeContextPath(contextPath, original.getParameter(parameter));
	}
	@Override
	public Map<String, String[]> getParameterMap()
	{
		Map<String, String[]> newMap = new LinkedHashMap<String, String[]>();
		Set<Map.Entry<String, String[]>> set = original.getParameterMap().entrySet();
		for(Map.Entry<String, String[]> me : set)
		{
			newMap.put(me.getKey(), removeCp(me.getValue()));
		}

		return newMap;
	}
	@Override
	public Enumeration<String> getParameterNames()
	{
		return original.getParameterNames();
	}
	@Override
	public String[] getParameterValues(String parameter)
	{
		return removeCp(original.getParameterValues(parameter));
	}

	/**
	 * Odstrani context path z celej sady parametrov
	 * @param values
	 * @return
	 */
	private String[] removeCp(String values[])
	{
		String newValues[] = new String[values.length];
		for (int i=0; i<values.length; i++)
		{
			newValues[i] = ContextFilter.removeContextPath(contextPath, values[i]);
		}
		return newValues;
	}

	//-----------------DELEGATE METHODS--------------------------
	@Override
	public Object getAttribute(String attribute)
	{
		return original.getAttribute(attribute);
	}
	@Override
	public Enumeration<String> getAttributeNames()
	{
		return original.getAttributeNames();
	}
	@Override
	public String getAuthType()
	{
		return original.getAuthType();
	}
	@Override
	public int getContentLength()
	{
		return original.getContentLength();
	}
	@Override
	public String getContentType()
	{
		return original.getContentType();
	}
	@Override
	public String getContextPath()
	{
		return original.getContextPath();
	}
	@Override
	public Cookie[] getCookies()
	{
		return original.getCookies();
	}
	@Override
	public long getDateHeader(String headerName)
	{
		return original.getDateHeader(headerName);
	}
	@Override
	public String getHeader(String headerName)
	{
		return original.getHeader(headerName);
	}
	@Override
	public Enumeration<String> getHeaderNames()
	{
		return original.getHeaderNames();
	}
	@Override
	public Enumeration<String> getHeaders(String headerName)
	{
		return original.getHeaders(headerName);
	}
	@Override
	public String getCharacterEncoding()
	{
		return original.getCharacterEncoding();
	}
	@Override
	public ServletInputStream getInputStream() throws IOException
	{
		return original.getInputStream();
	}
	@Override
	public int getIntHeader(String headerName)
	{
		return original.getIntHeader(headerName);
	}
	@Override
	public String getLocalAddr()
	{
		return original.getLocalAddr();
	}
	@Override
	public Locale getLocale()
	{
		return original.getLocale();
	}
	@Override
	public Enumeration<Locale> getLocales()
	{
		return original.getLocales();
	}
	@Override
	public String getLocalName()
	{
		return original.getLocalName();
	}
	@Override
	public int getLocalPort()
	{
		return original.getLocalPort();
	}
	@Override
	public String getMethod()
	{
		return original.getMethod();
	}
	@Override
	public String getPathInfo()
	{
		return original.getPathInfo();
	}
	@Override
	public String getPathTranslated()
	{
		return original.getPathTranslated();
	}
	@Override
	public String getProtocol()
	{
		return original.getProtocol();
	}
	@Override
	public String getQueryString()
	{
		return original.getQueryString();
	}
	@Override
	public BufferedReader getReader() throws IOException
	{
		return original.getReader();
	}
	@Override
	public String getRemoteAddr()
	{
		return original.getRemoteAddr();
	}
	@Override
	public String getRemoteHost()
	{
		return original.getRemoteHost();
	}
	@Override
	public int getRemotePort()
	{
		return original.getRemotePort();
	}
	@Override
	public String getRemoteUser()
	{
		return original.getRemoteUser();
	}
	@Override
	public RequestDispatcher getRequestDispatcher(String target)
	{
		return original.getRequestDispatcher(target);
	}
	@Override
	public String getRequestedSessionId()
	{
		return original.getRequestedSessionId();
	}
	@Override
	public String getRequestURI()
	{
		return original.getRequestURI();
	}
	@Override
	public StringBuffer getRequestURL()
	{
		return original.getRequestURL();
	}
	@Override
	public String getServerName()
	{
		return Tools.getServerName(original);
	}
	@Override
	public int getServerPort()
	{
		return original.getServerPort();
	}
	@Override
	public String getServletPath()
	{
		return original.getServletPath();
	}
	@Override
	public HttpSession getSession()
	{
		return original.getSession();
	}
	@Override
	public HttpSession getSession(boolean createNew)
	{
		return original.getSession(createNew);
	}
	@Override
	public String getScheme()
	{
		return original.getScheme();
	}
	@Override
	public Principal getUserPrincipal()
	{
		return original.getUserPrincipal();
	}
	@Override
	public boolean isRequestedSessionIdFromCookie()
	{
		return original.isRequestedSessionIdFromCookie();
	}
	@Override
	public boolean isRequestedSessionIdFromURL()
	{
		return original.isRequestedSessionIdFromURL();
	}
	@Override
	public boolean isRequestedSessionIdValid()
	{
		return original.isRequestedSessionIdValid();
	}
	@Override
	public boolean isSecure()
	{
		return original.isSecure();
	}
	@Override
	public boolean isUserInRole(String role)
	{
		return original.isUserInRole(role);
	}
	@Override
	public void removeAttribute(String attributeName)
	{
		original.removeAttribute(attributeName);
	}
	@Override
	public void setAttribute(String attributeName, Object value)
	{
		original.setAttribute(attributeName, value);
	}
	@Override
	public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException
	{
		original.setCharacterEncoding(encoding);
	}

	@Override
	public boolean authenticate(HttpServletResponse res) throws IOException,
			ServletException {
		return original.authenticate(res);
	}

	@Override
	public AsyncContext getAsyncContext() {
		return original.getAsyncContext();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return original.getDispatcherType();
	}

	@Override
	public ServletContext getServletContext() {
		return original.getServletContext();
	}

	@Override
	public boolean isAsyncStarted() {
		return original.isAsyncStarted();
	}

	@Override
	public boolean isAsyncSupported() {
		return original.isAsyncSupported();
	}

	@Override
	public AsyncContext startAsync() {
		return original.startAsync();
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		return original.startAsync(arg0, arg1);
	}

	@Override
	public Part getPart(String arg0) throws IOException, IllegalStateException,
			ServletException {
		return original.getPart(arg0);
	}

	@Override
	public Collection<Part> getParts() throws IOException,
			IllegalStateException, ServletException {
		return original.getParts();
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		original.login(arg0, arg1);
	}

	@Override
	public void logout() throws ServletException {
		original.logout();
	}

	@Override
	public long getContentLengthLong()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String changeSessionId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocolRequestId() {
		return original.getProtocolRequestId();
	}

	@Override
	public String getRequestId() {
		return original.getRequestId();
	}

	@Override
	public ServletConnection getServletConnection() {
		return original.getServletConnection();
	}

}
