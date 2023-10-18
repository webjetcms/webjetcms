package sk.iway.iwcm.helpers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;

/**
 *  RequestDump.java
 *
 *		Serves ar {@link HttpServletRequest}.toString()
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 7.4.2011 16:00:54
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class RequestDump
{
	HttpServletRequest request;
	private String lineSeparator;
	
	public RequestDump(HttpServletRequest request)
	{
		this.request = request;
	}
	
	
	public String printParameters()
	{
		StringBuilder message = new StringBuilder();
		message.append("<ul>");
		for (Object paramAsObject : request.getParameterMap().keySet())
			message.
				append("<li>").
				append(paramAsObject).
				append('=').
				append(request.getParameter(paramAsObject.toString())).
				append("</li>");
		
		message.append("</ul>");
		return message.toString();
	}
	
	public String printAttributes()
	{
		StringBuilder message = new StringBuilder();
		message.append("<ul>");
		Enumeration<String> attributes = request.getAttributeNames();
		while(attributes.hasMoreElements())
		{
			String attribute = attributes.nextElement();
			message.
				append("<li>").
				append(attribute).
				append('=').
				append(request.getAttribute(attribute)).
				append("</li>");
		}
		message.append("</ul>");
		return message.toString();
	}
	
	public String completeRequestReport()
	{
		lineSeparator = "\n";
		return createReport();
	}
	
	public String completeRequestReportAsHtml()
	{
		lineSeparator = "<br />";
		return createReport();
	}

	private String createReport()
	{
		StringBuilder report = new StringBuilder();
		appendPathInfoTo(report);
		appendUserInfoTo(report);
		appendHeadersTo(report);
		appendParametersTo(report);
		appendAttributesTo(report);
		appendSessionAttributesTo(report);
		return report.toString();
	}

	private void appendPathInfoTo(StringBuilder report)
	{
		report.append(Tools.getRequestedLink(request)).append(lineSeparator);
	}

	private void appendUserInfoTo(StringBuilder report)
	{
		Identity user = new RequestHelper(request).getUser();
		if (user == null)
			report.append(lineSeparator).append("User not logged in").append(lineSeparator);
		else
			report.append(lineSeparator).append("User: ").append(user).append(lineSeparator);
	}

	private void appendHeadersTo(StringBuilder report)
	{
		List<String> headerNames = Collections.list(request.getHeaderNames());
		report.append(lineSeparator).append("Request headers: ").append(lineSeparator);
		
		for (String header : headerNames)
			report.append(header).append(" = ").append(request.getHeader(header)).append(lineSeparator);
	}

	private void appendParametersTo(StringBuilder report)
	{
		List<String> parameterNames = Collections.list(request.getParameterNames());
		report.append(lineSeparator).append("Request parameters: ").append(lineSeparator);
		
		for (String parameterName : parameterNames)
			report.append(parameterName).append(" = ").append(request.getParameter(parameterName)).append(lineSeparator);
	}

	private void appendAttributesTo(StringBuilder report)
	{
		List<String> attributeNames = Collections.list(request.getAttributeNames());
		report.append(lineSeparator).append("Request attributes: ").append(lineSeparator);
		
		for (String attributeName : attributeNames)
			report.append(attributeName).append(" = ").append(request.getAttribute(attributeName)).append(lineSeparator);
	}

	private void appendSessionAttributesTo(StringBuilder report)
	{
		if(request.getSession() == null) return;
		List<String> attributeNames = Collections.list(request.getSession().getAttributeNames());
		report.append(lineSeparator).append("Session attributes: ").append(lineSeparator);
		
		for (String attributeName : attributeNames)
			report.append(attributeName).append(" = ").append(request.getSession().getAttribute(attributeName)).append(lineSeparator);
	}
}