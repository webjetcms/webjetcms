package sk.iway.iwcm.tags;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;


/**
 *  Tag pre vypis stringu z requestu v JSP stranke
 *
 *@Title        WebJET
 *@Company      Interway
 *@Copyright    Ľuboš Balát, Copyright (c) 2002
 *@author       jeeff
 *@version      1.0
 *@created      Piatok, 2002, február 22
 *@modified     $Date: 2010/01/20 11:15:08 $
 */
public class TextTag extends TagSupport
{
	private static final long serialVersionUID = 1955748610554165388L;

	public static final String PREFIX_KEY = "webjet.textTag.prefix";

	// Name of request attribute - required
	private String key = null;
	private String param1 = null;
	private String param2 = null;
	private String param3 = null;
	private String defaultText = null;
	private String lng = null;
	private boolean pluralize = false;
    private boolean jsEscape = false;

	@Override
	public void release()
	{
		super.release();
		key = null;
		param1 = null;
		param2 = null;
		param3 = null;
		defaultText = null;
		lng = null;
	}

	/**
	 *  Description of the Method
	 *
	 *@return                   Description of the Return Value
	 *@exception  JspException  Description of the Exception
	 */
	@Override
	public final int doEndTag() throws JspException
	{
		try
		{
			String text = key;
			HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			String lng = "";
			if (this.lng!=null)
			{
				lng = this.lng;
			}
			else if (pageContext.getAttribute("lng")!=null)
			{
				lng = (String)pageContext.getAttribute("lng");
			}
			else if (request.getAttribute("PageLng")!=null)
			{
				lng = (String)request.getAttribute("PageLng");
			}
			else
			{
				lng = ResponseUtils.filter(request.getParameter("language"));
				if (lng!=null && lng.length()>3) lng = null;
				if (lng != null && request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG)==null) request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);
				if (lng==null)
				{
					lng = (String)request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
				}
				if (lng == null)
				{
					lng = PageLng.getUserLng(request); // sk.iway.iwcm.Constants.getString("defaultLanguage");
				}
			}

			boolean needRefresh = false;
			if (request.getSession().getAttribute("userlngr")!=null)
			{
				//ak to mame v session, tak chceme pri kazdom zobrazeni stranky spravit refresh
				needRefresh = true;
			}
			if (request.getParameter("userlngr")!=null)
			{
				needRefresh = true;
			}
			if (pageContext.getAttribute("userlngr")!=null)
			{
				//pri zobrazeni tejto stranky sme uz jazyk refreshli, nema zmysel to robit znova
				needRefresh = false;
			}

			//Logger.println(this,"userlngr="+request.getParameter("userlngr"));

			Prop prop = null;
			if (needRefresh)
			{
				pageContext.setAttribute("userlngr", "refreshed");
				prop = Prop.getInstance(pageContext.getServletContext(), lng, true);
				if ("session".equals(request.getParameter("userlngr")))
				{
					//uloz si to do session, teraz sa bude refreshovat pri kazdom zobrazeni stranky
					request.getSession().setAttribute("userlngr", "session");
				}
			}
			else
			{
				prop = Prop.getInstance(pageContext.getServletContext(), lng, false);
			}

			if (pluralize)
			{
				IwayProperties iwayProperties = prop.getRes(lng);
				if (iwayProperties.containsKey(key+"."+param1))
				{
					key = key+"."+param1;
				} 
				else 
				{
					boolean keyFound = false;
					for (String lKey : iwayProperties.keySet())
					{
						if (lKey.startsWith(key) && (lKey.contains("."+param1+".") || lKey.endsWith("."+param1)))
						{
							key = lKey;
							keyFound = true;
							break;
						}
					}
					if (!keyFound)
					{
						key = key+".X";
					}
				}
				iwayProperties = null;
			}

			text = prop.getText(key, param1, param2, param3);

			String textTagPrefix = (String)pageContext.getRequest().getAttribute(PREFIX_KEY);
			if (Tools.isNotEmpty(textTagPrefix))
			{
				String prefixKey = textTagPrefix+"."+key;
				String textWithPrefix = prop.getText(prefixKey, param1, param2, param3);
				if (Tools.isNotEmpty(textWithPrefix) && prefixKey.equals(textWithPrefix)==false) text = textWithPrefix;
			}

			if (text.equals(key) && Tools.isNotEmpty(defaultText) && lng.equals("sk"))
			{
				text = defaultText;
			}

			if (jsEscape) text = JSEscapeTag.jsEscape(text);

			//text = prop.getText(key);
			if (text != null)
			{
				pageContext.getOut().write(text);
			}

		}
		catch (Exception e)
		{

		}
		return EVAL_PAGE;
	}

	public final void setKey(String key)
	{
		this.key = key;
	}

	public String getParam1()
	{
		return param1;
	}
	public void setParam1(String param1)
	{
		this.param1 = param1;
	}
	public String getParam2()
	{
		return param2;
	}
	public void setParam2(String param2)
	{
		this.param2 = param2;
	}
	public String getParam3()
	{
		return param3;
	}
	public void setParam3(String param3)
	{
		this.param3 = param3;
	}

	public String getDefaultText()
	{
		return defaultText;
	}

	public void setDefaultText(String defaultText)
	{
		this.defaultText = defaultText;
	}

	public String getLng() {
		return lng;
	}

	public void setLng(String lng) {
		this.lng = lng;
	}

	public void setPluralize(boolean pluralize) {
		this.pluralize = pluralize;
	}

    public boolean isJsEscape()
    {
        return jsEscape;
    }

    public void setJsEscape(boolean jsEscape)
    {
        this.jsEscape = jsEscape;
    }
}
