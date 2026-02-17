package sk.iway.iwcm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

/**
 *  Pracovanie s pageParams (v dynamicky includnutych objektoch)
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Nedela, 2003, september 28
 *@modified     $Date: 2003/12/19 10:02:33 $
 */
public class PageParams implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 6079376479113116704L;
	private Map<String, String> params;
	private List<String> paramNames;
	private String pageParams; //NOSONAR

	/**
	 * Returns deep copy of params map
	 * @return
	 */
	public Map<String, String> getParams()
	{
		return Tools.deepCopy(params);
	}

	public List<String> getParamNames()
	{
		return paramNames;
	}

	/**
	 *  Constructor for the PageParams object
	 */
	public PageParams()
	{
		setPageParams("");
	}

	public PageParams(HttpServletRequest request)
	{
		String requestValue = (String)request.getAttribute("includePageParams");

		if (Tools.isEmpty(requestValue))
		{
			persistFromSession(request);
		}
		else
		{
			setPageParams(requestValue);

			int docId = Tools.getIntValue(request.getParameter("docid"), -1);
			if (docId > 0) params.put("originalDocId", Integer.toString(docId));
		}
	}

	public PageParams(String paramsString)
	{
		setPageParams(paramsString);
	}

	/**
	 * Vrati hidden pole pre formular s identifikatorom objektu v session
	 * @return
	 */
	public String persistToSession(HttpServletRequest request)
	{
		String key = PkeyGenerator.getUniqueKey();

		Object[] sessionData = new Object[2];
		sessionData[0] = pageParams;
		sessionData[1] = Integer.valueOf(Tools.getIntValue(request.getParameter("docid"), -1));

		request.getSession().setAttribute("pageParams."+key, sessionData);

		String hiddenField = "<input type='hidden' name='__ppState' value='"+key+"'/>";
		return hiddenField;
	}

	private void persistFromSession(HttpServletRequest request)
	{
		//skusme podla hidden pola
		String ppState = request.getParameter("__ppState");
		if (Tools.isNotEmpty(ppState))
		{
			Object[] sessionData = (Object[])request.getSession().getAttribute("pageParams."+ppState);
			if (sessionData!=null && sessionData.length==2)
			{
				String sessionValue = (String)sessionData[0];
				if (Tools.isNotEmpty(sessionValue)) setPageParams(sessionValue);

				int docId = ((Integer)sessionData[1]).intValue();
				if (docId > 0) params.put("originalDocId",Integer.toString(docId));

				return;
			}
		}
		//aby sa vsetko inicializovalo
		setPageParams(null);
	}

	/**
	 * Sparsovanie retazca, vytvorenie HashTabulky hodnot
	 *
	 */
	/*private void parseOld()
	{
		params = new Hashtable();
		if (pageParams==null) return;
		pageParams = pageParams.trim();
		StringTokenizer st = new StringTokenizer(pageParams, ",");
		String param, name, value;
		int index;
		while (st.hasMoreTokens())
		{
			try
			{
				param = st.nextToken().trim();
				index = param.indexOf('=');
				if (index > 0)
				{
					name = param.substring(0, index);
					if (param.length()>(index+1))
					{
						value = param.substring(index+1);
					}
					else
					{
						value="";
					}

					//Logger.println(this,"name="+name+" value="+value);
					params.put(name, value);
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}*/


	@SuppressWarnings("java:S1659")
	private void parse()
	{
		params = new Hashtable<>();
		paramNames = new ArrayList<>();
		if (pageParams==null) return;

		String paramStr = pageParams.trim();
		String param, name, value, temp;
		int index, ind, ind2, i;
		boolean iterate = true;
		int quoteCounter = 0;

		//Logger.println(this,"*** START: PageParams parser ***");
		temp = paramStr;

		//spocitam uvodzovky v parametroch; ak nemam parny pocet, vyhodim chybu
		int failsafe = 0;
		try
		{
			while(temp.indexOf('\"') != -1)
			{
				quoteCounter++;
				if(temp.length() > (temp.indexOf('\"')+1))
					temp = temp.substring(temp.indexOf('\"')+1);
				else
					temp = "";
			}

			//ak mam parny pocet uvodzoviek
			if((quoteCounter > 0 && (quoteCounter%2 == 0)) || quoteCounter == 0)
			{
				//Logger.println(this,"quoteCounter: "+quoteCounter);

				while(iterate && failsafe++ < 100)
				{
					ind = paramStr.indexOf('\"');
					if(ind != -1 && paramStr.length() > ind)
					{
						temp = paramStr.substring(0, ind);
						//Logger.println(this,"*****\ntest = "+pageParams+ "\ntemp = "+temp);

						//sparsujem to podla ,
						StringTokenizer st = new StringTokenizer(temp, ",");
						int size = st.countTokens();
						value = "";
					 	i = 1;

						while (st.hasMoreTokens())
						{
							param = reparseEmailAddress(st.nextToken().trim());
							//Logger.println(this,"token = "+param);

							//pred '=' je nazov parametra a za '=' je hodnota
							index = param.indexOf('=');
							ind2 = -1;

							if (index > 0)
							{
								name = param.substring(0, index);
								if (param.length()>(index+1))
								{
									value = param.substring(index+1);
								}
								else if(i == size) //ak som na poslednom tokene, najdem dalsiu " a cele to pridam ako hodnotu parametra
								{
									if(paramStr.length() > (ind+1))
									{
										ind2 = paramStr.indexOf("\"", ind+1);
										//ind2 = paramStr.substring(ind+1).indexOf("\"");
										//Logger.println(this,"test.substring(ind+1): "+test.substring(ind+1));
									}
									if(ind2 > -1)
									{
										value = paramStr.substring(ind+1, ind2);
										//sparsovanu cast retazca odrezem
										paramStr = paramStr.substring(ind2+1);
										//Logger.println(this,"test.substring(ind+ind2+2): "+test);
									}
									else
									{
										Logger.println(this,"PARSE ERROR - Pageparams: wrong number of quotes! pageParams="+pageParams);
										break;
									}
									//Logger.println(this,"ind: "+ind+ "   ind2: "+ind2+ "  length: "+test.length()+ "  test: "+test);

								}
								else
								{
									value = "";
								}

								//Logger.println(this,"name = "+name+"   value = "+value);
								//parameter hodim do tabulky
								params.put(name, value);
								paramNames.add(name);
							}
							i++;
						}
						//ak nemam tokeny, posuniem sa v retazci na najblizsie " - osetrenie pripadu ""
						if(size < 1)
							paramStr = paramStr.substring(ind+1);
						//Logger.println(this,"test: "+test);
					}
					else
					{
						//sparsujem to podla ,
						StringTokenizer st = new StringTokenizer(paramStr, ",");
						//size = st.countTokens();
						value = "";
						while (st.hasMoreTokens())
						{
							param = st.nextToken().trim();
							//Logger.println(this,"token = "+param);
							index = param.indexOf('=');

							if (index > 0)
							{
								name = param.substring(0, index);
								if (param.length()>(index+1))
								{
									value = param.substring(index+1);
								}
								else
								{
									value = "";
								}

								//Logger.println(this,"name = "+name+"   value = "+value);
								params.put(name, value);
								paramNames.add(name);
							}
						}
						iterate = false;
						break;
					}
				}
			}
			else
			{
				Logger.println(this,"PARSE ERROR - Pageparams: odd count of quotes! "+quoteCounter+" pageParams="+pageParams);
			}

			//Logger.println(this,"*** END: PageParams parser ***");
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

	}

	/**
	 * Ziskanie hodnoty ako String
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public String getValue(String name, String defaultValue)
	{
		String value = params.get(name);
		if (Tools.isEmpty(value))
		{
			return(defaultValue);
		}
        value = Tools.replace(value,"&amp;nbsp;", "&nbsp;");
        value = Tools.replace(value,"&amp;quot;", "\"");
		value = Tools.replace(value,"&quot;", "\"");
		value = Tools.replace(value,"&#39;", "'");
		return(value);
	}

	/**
	 * Preparsuje hodnotu parametra aby neobsahoval <a href="mailto:...">email</a> ale len email
	 * @param value
	 * @return
	 */
	private static String reparseEmailAddress(String value)
	{
		try
		{
			String valueLC = value.toLowerCase();
			if (valueLC.indexOf("mailto:")!=-1 && valueLC.indexOf("href")!=-1)
			{
				int start = valueLC.indexOf('>');
				int end = valueLC.indexOf("</a>");
				if (end > start)
				{
					value = value.substring(start+1, end);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(value);
	}

	/**
	 * Ziskanie hodnoty ako cislo
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public int getIntValue(String name, int defaultValue)
	{
		String value = params.get(name);
		if (value==null)
		{
			return(defaultValue);
		}
		try
		{
			return(Integer.parseInt(value));
		}
		catch (Exception ex)
		{

		}
		return(defaultValue);
	}

	/**
	 * Ziskanie hodnoty ako boolean
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public boolean getBooleanValue(String name, boolean defaultValue)
	{
		String value = params.get(name);
		if (value==null)
		{
			return(defaultValue);
		}
		return value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
	}

	/**
	 * Ziskanie celeho retazca
	 * @return
	 */
	public String getPageParams()
	{
		return pageParams;
	}
	/**
	 * Nastavenie retazca a jeho parsovanie
	 * @param pageParams
	 */
	public void setPageParams(String pageParams)
	{
		if (pageParams != null && pageParams.length()>6 && pageParams.endsWith(")!")) pageParams = pageParams.substring(0, pageParams.length()-2);

		pageParams = Tools.replace(pageParams, "&quot;", "\"");
		pageParams = Tools.replace(pageParams, "&nbsp;", " ");
		//aby sme v parametri mohli mat uvodzovky staci ich nahradit za \", tytmto to vratime nazad na entitu a bude to fungovat
        pageParams = Tools.replace(pageParams, "\\\"", "&quot;");

        pageParams = Tools.replace(pageParams, "&lt;","<");
        pageParams = Tools.replace(pageParams, "&gt;",">");

		this.pageParams = pageParams;
		parse();
	}

	/**
	 * Test, ci je dany parameter nastaveny (ma nejaku hodnotu)
	 * @param name
	 * @return
	 */
	public boolean hasParameter(String name)
	{
		String value = params.get(name);
		if (value!=null)
		{
			return(true);
		}
		return(false);
	}
}
