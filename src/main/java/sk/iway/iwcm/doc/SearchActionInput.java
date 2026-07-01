package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;

public class SearchActionInput {

	private final Map<String, List<String>> parameters = new LinkedHashMap<>();
	private final Map<String, Object> attributes = new LinkedHashMap<>();
	private Identity user;
	private String domainName;

	public static SearchActionInput fromRequest(HttpServletRequest request)
	{
		SearchActionInput input = new SearchActionInput();

		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements())
		{
			String name = parameterNames.nextElement();
			String[] values = request.getParameterValues(name);
			if (values != null)
			{
				input.setParameterValues(name, values);
			}
		}

		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements())
		{
			String name = attributeNames.nextElement();
			input.setAttribute(name, request.getAttribute(name));
		}

		return input;
	}

	public String getParameter(String name)
	{
		List<String> values = parameters.get(name);
		if (values == null || values.isEmpty()) return null;
		return values.get(0);
	}

	public String[] getParameterValues(String name)
	{
		List<String> values = parameters.get(name);
		if (values == null) return null;
		return values.toArray(new String[0]);
	}

	public Enumeration<String> getParameterNames()
	{
		return Collections.enumeration(new LinkedHashSet<>(parameters.keySet()));
	}

	public void setParameter(String name, String value)
	{
		if (value == null)
		{
			parameters.remove(name);
			return;
		}

		List<String> values = new ArrayList<>();
		values.add(value);
		parameters.put(name, values);
	}

	public void setParameterValues(String name, String[] values)
	{
		if (values == null)
		{
			parameters.remove(name);
			return;
		}

		List<String> mappedValues = new ArrayList<>(values.length);
		Collections.addAll(mappedValues, values);
		parameters.put(name, mappedValues);
	}

	public Object getAttribute(String name)
	{
		return attributes.get(name);
	}

	public void setAttribute(String name, Object value)
	{
		if (value == null)
		{
			attributes.remove(name);
			return;
		}

		attributes.put(name, value);
	}

	public void removeAttribute(String name)
	{
		attributes.remove(name);
	}

	public boolean getBooleanValue(String name, boolean defaultValue)
	{
		return Tools.getBooleanValue(getParameter(name), defaultValue);
	}

	public int getIntValue(String name, int defaultValue)
	{
		return Tools.getIntValue(getParameter(name), defaultValue);
	}

	public Identity getUser()
	{
		return user;
	}

	public void setUser(Identity user)
	{
		this.user = user;
	}

	public String getDomainName()
	{
		return domainName;
	}

	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}
}