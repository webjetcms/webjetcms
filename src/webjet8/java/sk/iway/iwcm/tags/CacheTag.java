package sk.iway.iwcm.tags;

import java.io.IOException;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  CacheTag.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 14.4.2005 21:19:01
 *@modified     $Date: 2010/01/20 11:15:08 $
 */
public class CacheTag extends BodyTagSupport
{
	private static final long serialVersionUID = -644225644526769652L;

	private String name;

	private int minutes=10;

	private boolean storeInSession = false;

	private boolean disableCache = false;

	public static final String IS_CACHE_TAG = "sk.iway.iwcm.tags.CacheTag.IS";

	@Override
	public int doStartTag() throws JspTagException
	{
		pageContext.getRequest().setAttribute(IS_CACHE_TAG, "true");
		if (!disableCache && !Constants.getBoolean("CacheTagDisable"))
		{
			if (storeInSession)
			{
				if (pageContext.getSession().getAttribute(name) != null)
				{
					if (pageContext.getSession().getAttribute(name) != null && Tools.getNow() + 1000 * 60 * minutes > (long) pageContext.getSession().getAttribute(name + "Timestamp"))
					{
						try
						{
							Logger.debug(getClass(), "Vraciam cachovany content pre session kluc '" + name + "', cachovanu " + minutes + " minut");
							pageContext.getOut().print((String) pageContext.getSession().getAttribute(name));

						} catch (IOException e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
						return (SKIP_BODY);
					}
				}
			} else
			{
				Cache c = Cache.getInstance();
				String cachedOutput = c.getObject(name, String.class);
				if (cachedOutput != null)
				{
					try
					{
						Logger.debug(getClass(), "Vraciam cachovany content pre kluc '" + name + "', cachovanu " + minutes + " minut");
						pageContext.getOut().print(cachedOutput);
					} catch (IOException e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
					return (SKIP_BODY);
				}
			}
		}
		return(EVAL_BODY_BUFFERED);
	}


	@Override
	public int doAfterBody() throws JspTagException
	{

		BodyContent bc = getBodyContent();
		String body = bc.getString();

		try
		{
			getPreviousOut().print(body);

			//pageContext.setAttribute("cacheTagOutput", body);
		} catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		if (!disableCache)
		{
			if (storeInSession)
			{
				Logger.debug(getClass(), "Ukladam content do session - kluc '" + name + "', cachovanu " + minutes + " minut");
				pageContext.getSession().setAttribute(name, body);
				pageContext.getSession().setAttribute(name + "Timestamp", Tools.getNow());
			} else
			{
				Logger.debug(getClass(), "Ukladam content do cache - kluc '" + name + "', cachovanu " + minutes + " minut");
				Cache c = Cache.getInstance();
				c.setObject(name, body, minutes);
			}
		}
		return EVAL_PAGE;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getMinutes()
	{
		return minutes;
	}

	public void setMinutes(int minutes)
	{
		this.minutes = minutes;
	}

	public boolean isStoreInSession()
	{
		return storeInSession;
	}

	public void setStoreInSession(boolean storeInSession)
	{
		this.storeInSession = storeInSession;
	}

	public boolean isDisableCache()
	{
		return disableCache;
	}

	public void setDisableCache(boolean disableCache)
	{
		this.disableCache = disableCache;
	}
}
