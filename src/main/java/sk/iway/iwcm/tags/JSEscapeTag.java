package sk.iway.iwcm.tags;

import java.io.IOException;

import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import sk.iway.iwcm.Tools;


/**
 *  Escapuje body tak, aby mohlo byt vlozene do JS
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.6 $
 *@created      $Date: 2010/01/20 11:15:08 $
 */
public class JSEscapeTag extends BodyTagSupport
{
	private static final long serialVersionUID = -7334014241009801243L;

	/**
	 *  Description of the Method
	 *
	 *@return                      Description of the Return Value
	 *@exception  JspTagException  Description of the Exception
	 */
	 @Override
	public int doAfterBody() throws JspTagException
	{
		BodyContent bc = getBodyContent();
		String body = bc.getString();
		bc.clearBody();
		try
		{			
			getPreviousOut().print(jsEscape(body));
		}
		catch (IOException e)
		{
			throw new JspTagException("JSEscapeTag: " +	e.getMessage());
		}
		return SKIP_BODY;
	}
	
	public static String jsEscape(String body)
	{
     char content[] = new char[body.length()];
     body.getChars(0, body.length(), content, 0);
     StringBuilder result = new StringBuilder(content.length + 50);
     for (int i = 0; i < content.length; i++) {
         switch (content[i]) {
         case '\n':
               result.append("\\n");
               break;
         case '\r':
               result.append(' ');
               break;
         case '&':
             result.append("&amp;");
             break;
         case '"':
             result.append("&quot;");
             break;
         case '\'':
             result.append("\\'");
             break;
         default:
             result.append(content[i]);
         }
     }
		
		return( Tools.replace(result.toString(), "&amp;mp;", "&amp;"));
	}
}

