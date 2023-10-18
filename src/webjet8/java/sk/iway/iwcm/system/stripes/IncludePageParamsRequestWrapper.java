package sk.iway.iwcm.system.stripes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import sk.iway.iwcm.Tools;

/**
 * upravuje parametre ktore vracia na zaklade zoznamu PPOnly nazov a mapy PP
 * @author mbocko
 *
 */
public class IncludePageParamsRequestWrapper extends HttpServletRequestWrapper
{
	private Map<String, String> includeParams;
	private List<String> includeParamsOnly;
	private Map<String, String[]> paramMap=null;

	public IncludePageParamsRequestWrapper(HttpServletRequest req, Map<String, String> includeParams, List<String> includeParamsOnly)
	{
		super(req);
		this.includeParams = includeParams;
		this.includeParamsOnly = includeParamsOnly;
	}

	@Override
	public String getParameter(String name)
	{
		if (getParameterMap().containsKey(name))
		{
			String[] values = getParameterMap().get(name);
			return values[0];
		}
		return null;
	}

	/**
	 * vrati mapu parametrov, parametre ktore su anotovane ako includeParamsOnly vyhodi uplne, potom tam hodi parametre z original rq,
	 * a potom prepise tie ktore su v PP
	 */
	@Override
	public Map<String, String[]> getParameterMap()
	{
		if (paramMap!=null)
		{
			return paramMap;
		}
		else
		{
			Map<String, String[]> original = new HashMap<String, String[]>();
			for (Object originalParamName : super.getParameterMap().keySet())
			{
				String lName = (String)originalParamName;

				//ak by posielal property nejakeho objekty user.name
				if (lName.contains("."))
					lName = lName.substring(0, lName.indexOf("."));

				//ak by posielal indexovany parameter nazovParametra[1] alebo user[1].name
				if (lName.contains("["))
					lName = lName.substring(0, lName.indexOf("["));

				if (!includeParamsOnly.contains(lName))
				{
					original.put((String)originalParamName, super.getParameterValues((String)originalParamName));
				}
			}
			for (String name : includeParams.keySet())
			{
				String maybeArrayValue = includeParams.get(name);
				//ak je to pluskami deleny string, daj to ako pole hodnot, v actionbeane sa potom ziskava ako pole, list alebo set
				// toto je mozno diskutabilne, zvazil by som zna pipe namiesto plus ako delimiter
				if (maybeArrayValue.contains("+") && !maybeArrayValue.startsWith("+") && !maybeArrayValue.endsWith("+"))
				{
					String values[] = Tools.getTokens(maybeArrayValue, "+", true);
					original.put(name, values);
				}
				else
				{
					String values[] = new String[1];
					values[0] = includeParams.get(name);
					original.put(name, values);
				}
			}
			paramMap = original;
			return original;
		}
	}

	@Override
	public String[] getParameterValues(String name)
	{
		return getParameterMap().get(name);
	}

}