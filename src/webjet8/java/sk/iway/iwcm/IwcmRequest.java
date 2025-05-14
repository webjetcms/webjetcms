package sk.iway.iwcm;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import sk.iway.iwcm.tags.support_logic.CustomResponseUtils;
import sk.iway.iwcm.utils.MapUtils;

/**
 *  IwcmRequest.java
 *
 *  Allows user to wrap an incoming {@link HttpServletRequest} and alter its parameters.
 *  The class acts transparently to a caller, acting as if supplied parameters
 *  were received via standard Tomcat request creation.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.11.2010 13:14:49
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwcmRequest extends HttpServletRequestWrapper
{
	//private final HttpServletRequest original;
	private Map<String, String[]> changedParameterValues = new HashMap<>();
	private Locale forcedLocale = null;

	public IwcmRequest(HttpServletRequest original)
	{
		//this.original = original;
		super(original);

		String lng = PageLng.getUserLng(original);
		String[] isoLocale = Tools.getTokens(PageLng.getUserLngIso(lng), "-");
		if (isoLocale.length == 2) {
			setLocale(new java.util.Locale(isoLocale[0], isoLocale[1]));
		}
	}

	@Override
	public String getParameter(String parameter)
	{
		if (changedParameterValues.containsKey(parameter))
			return changedParameterValues.get(parameter)[0];

		//[#32245 - Penetracni testy novy web] - osetrenie formlarovych parametrov aby nemohli obsahovat HTML kod
		//testovaci payload zadany do inputu: <input type="text" oncopy="prompt(1)" value="Try to copy this text">
		return CustomResponseUtils.filter(super.getParameter(parameter));
	}
	@Override
	public Map<String, String[]> getParameterMap()
	{
		return MapUtils.merge(changedParameterValues, super.getParameterMap());
	}
	@Override
	public Enumeration<String> getParameterNames()
	{
		return Collections.enumeration(getParameterMap().keySet());
	}
	@Override
	public String[] getParameterValues(String parameter)
	{
		if (changedParameterValues.containsKey(parameter))
			return changedParameterValues.get(parameter);

		//[#32245 - Penetracni testy novy web] - osetrenie formlarovych parametrov aby nemohli obsahovat HTML kod
		String[] values = super.getParameterValues(parameter);
		if (values==null || values.length<1) return values;

		//musime to prekopirovat do noveho pola, inak sa nam pri kazdom volani getParameterValues zduplikuje CustomResponseUtils.filter
		String[] valuesFiltered = new String[values.length];
		if (values!=null && values.length>0)
		{
			for (int i=0; i<values.length; i++)
			{
				valuesFiltered[i] = CustomResponseUtils.filter(values[i]);
			}
		}

		return valuesFiltered;
	}

	public String getOriginalParameter(String key)
	{
		return super.getParameter(key);
	}

	public void setParameter(String key, String value)
	{
		String[] values = getParameterValues(key);
		if (values == null || values.length!=1)
		{
			values = new String[1];
		}
		//jeeff: povodne som uvazoval, ze sa len zvacsi to pole, to by sa ale potom nedala prepisat hodnota, pre pole treba pouzit setParameterValues
		values[0] = value;

		changedParameterValues.put(key, values);
	}

	public void setParameterValues(String key, String[] values)
	{
		changedParameterValues.put(key, values);
	}

	public String param(String key)
	{
		return getParameter(key);
	}

	public boolean hasParameter(String key)
	{
		return param(key) != null;
	}

	public Object attr(String key)
	{
		return getAttribute(key);
	}

	public boolean hasAttribute(String key)
	{
		return attr(key) != null;
	}

	public void setLocale(Locale locale) {
		this.forcedLocale = locale;
	}

	@Override
	public Locale getLocale() {
		if (forcedLocale != null) {
			return forcedLocale;
		}
		return super.getLocale();
	}

}