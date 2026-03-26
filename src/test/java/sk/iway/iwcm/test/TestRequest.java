package sk.iway.iwcm.test;

import jakarta.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.mock.MockHttpServletRequest;

/**
 *  TestRequest.java
 *  
 *  Implementation of {@link HttpServletRequest}
 *  without any external dependencies. Allows you to programatically
 *  set request's parameters.
 *  
 *  <strong>
 *  	getSession() returns NULL unless setSession() is called.
 *  	In order to obtain a request WITH a populated session, use {@link BaseWebjetTest}.newRequest() instead
 *  </strong>
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.9.2010 14:07:11
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class TestRequest extends MockHttpServletRequest
{

	public TestRequest(String contextPath, String servletPath)
	{
		super(contextPath, servletPath);
	}
	
	public TestRequest()
	{
		this("", "");
	}
	
	public void setParameter(String name, String value)
	{
		getParameterMap().put(name, new String[]{value});
	}

	/**
	 * Shortcut for adding multiple parameters.
	 * @param namesAndValues - usage: setParameters(name1, value1, name2, value2)
	 * @throws IllegalArgumentException in case 
	 */
	public void setParameters(String... namesAndValues)
	{
		if (namesAndValues.length % 2 == 1)
			throw new IllegalArgumentException("Can't form name => value pairs. One of the parameters is not mapped on its value");
		
		for (int i = 0; i < namesAndValues.length; i+=2)
			getParameterMap().put(namesAndValues[i], new String[]{namesAndValues[i+1]});
	}
}