package sk.iway.iwcm.tags;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 *  BuffTag.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 14.4.2005 21:19:01
 *@modified     $Date: 2010/01/20 11:15:08 $
 */
public class BuffTag extends BodyTagSupport
{
	private static final long serialVersionUID = -644225644526769652L;

	public String hideOut = "false";

	public String attName = "buffTagOutput";

	public boolean storeInRequest = false;

	public static final String IS_BUFF_TAG = "sk.iway.iwcm.tags.BuffTag.IS";

	@Override
	public int doStartTag() throws JspTagException
	{
		//Logger.println(this,"++++++++++++++++++++++++++++++++++++++++++++++++++ DO START TAG");
		pageContext.getRequest().setAttribute(IS_BUFF_TAG, "true");
		return(EVAL_BODY_BUFFERED);
	}

	@Override
	public int doAfterBody() throws JspTagException
	{
		BodyContent bc = getBodyContent();
		String body = bc.getString();

		//Logger.println(this,"BC: buff size=" + bc.getBufferSize()+" "+pageContext.getResponse().isCommitted());
		//Logger.println(this,"||| MAM BODY, size="+body.length());

		try
		{
			//resp.sendRedirect("http://magma.jeeff.sk");

			if ("true".equals(hideOut))
			{
				bc.clearBody();
				pageContext.getRequest().removeAttribute(IS_BUFF_TAG);
			}
			else
			{
				getPreviousOut().print(body);
			}
			if (storeInRequest)
				pageContext.getRequest().setAttribute(attName, body);
			else
				pageContext.setAttribute(attName, body);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return EVAL_PAGE;
	}

	public String getHideOut()
	{
		return hideOut;
	}

	public void setHideOut(String hideOut)
	{
		this.hideOut = hideOut;
	}

	public String getAttName()
	{
		return attName;
	}

	public void setAttName(String attName)
	{
		this.attName = attName;
	}

	public boolean isStoreInRequest()
	{
		return storeInRequest;
	}

	public void setStoreInRequest(boolean storeInRequest)
	{
		this.storeInRequest = storeInRequest;
	}
}
