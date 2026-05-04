package sk.iway.iwcm.tags;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.DynamicAttributes;
import jakarta.servlet.jsp.tagext.TagSupport;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

/**
 *  tag pre vypis pozadovaneho dokument (parameter docId) POZOR: nezapisuje
 *  statistiku
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2010/01/20 11:15:08 $
 *@modified     $Date: 2010/01/20 11:15:08 $
 */
public class ShowDoc extends TagSupport implements DynamicAttributes
{
	private static final long serialVersionUID = 2652530779542928047L;

	private int docId;
	private int menuDocId = -1;

	private Map<String,Object> dynamicAttrs = new HashMap<String,Object>();

	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspException
	{
		dynamicAttrs.put(localName.toLowerCase(), value);
	}

	@Override
	public void release()
	{
		super.release();
		docId = -1;
		menuDocId = -1;
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
			//get requested document
			DocDB docDB = DocDB.getInstance();
			DocDetails doc = docDB.getDoc(docId);
			if (doc != null)
			{
				HttpSession session = pageContext.getSession();
				Identity user = null;
				try
				{
					//ziskaj meno lognuteho usera
					if (session.getAttribute(Constants.USER_KEY) != null)
					{
						user = (Identity) session.getAttribute(Constants.USER_KEY);
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				// ------------ HEADER
				String text = doc.getData();
				if (text != null)
				{
					if (menuDocId < 1)
					{
						menuDocId = docId;
					}
					try
					{
						text = sk.iway.iwcm.doc.ShowDoc.updateCodes(user, text, menuDocId, (HttpServletRequest)pageContext.getRequest(), pageContext.getServletContext());
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}

				// nahradim zastupne symboly
				if (dynamicAttrs!=null)
				{
					for (String key : dynamicAttrs.keySet())
					{
						if (key.startsWith("replace-"))
						{
							String replaceKey = key.substring(key.indexOf("replace-")+"replace-".length());
							text = Tools.replace(text, "{"+replaceKey+"}", String.valueOf(dynamicAttrs.get(key)));
							text = Tools.replace(text, "{"+replaceKey.toUpperCase()+"}", String.valueOf(dynamicAttrs.get(key)));
						}
					}
				}

				//pageContext.getOut().write((String) pageContext.getRequest().getAttribute("doc_header"));
				WriteTag.writeText(text ,pageContext, "unknown");
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return EVAL_PAGE;
	}

	/**
	 *  Sets the docId attribute of the ShowDoc object
	 *
	 *@param  newDocId  The new docId value
	 */
	public void setDocId(int newDocId)
	{
		docId = newDocId;
		menuDocId = newDocId;
	}

	/**
	 *  Gets the docId attribute of the ShowDoc object
	 *
	 *@return    The docId value
	 */
	public int getDocId()
	{
		return docId;
	}
	public int getMenuDocId()
	{
		return menuDocId;
	}
	public void setMenuDocId(int menuDocId)
	{
		this.menuDocId = menuDocId;
	}

}
