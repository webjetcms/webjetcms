package sk.iway.iwcm.system.stripes;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.ajax.JavaScriptBuilder;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stripes.RegUserAction;

/**
 *  ActionContextJavaScriptResolution.java
 *  <p>Resolution that will convert a Java object web to a web of JavaScript objects and arrays, and
 * stream the JavaScript back to the client.  The output of this resolution can be evaluated in
 * JavaScript using the eval() function, and will return a reference to the top level JavaScript
 * object.  For more information see {@link JavaScriptBuilder}</p>
 *
 * <p>Resolution builds up result from the object of current ActionBean context returning simple
 * containing involved validation errors if they occur.</p>
 *
 * source: http://blog.novoj.net/2008/01/25/running-ajax-with-jquery-in-stripes-framework/
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.2.2008 15:21:35
 *@modified     $Date: 2008/07/29 14:42:14 $
 */
public class ActionContextJavaScriptResolution implements Resolution
{
	private JavaScriptBuilder builder;

	/**
	 * Constructs a new JavaScriptResolution that will convert the supplied object to JavaScript.
	 *
	 * @param actionBean
	 */
	public ActionContextJavaScriptResolution(ActionBean actionBean)
	{
		ActionBeanContext ctx = actionBean.getContext();
		Object result;
		HttpServletRequest request = ctx.getRequest();
		ValidationErrors validationErrors = ctx.getValidationErrors();
		int userId = -1;

		if(actionBean instanceof RegUserAction && ((RegUserAction)actionBean).getUsr() != null)
			userId = ((RegUserAction)actionBean).getUsr().getUserId();

		if (!validationErrors.isEmpty())
		{
			result = new AjaxPostFormResult(validationErrors, composeErrorsHtml(validationErrors, request), userId);

			((AjaxPostFormResult)result).updateCsrfToken(request);
		}
		else
		{
			result = new AjaxPostFormResult(composeMessagesHtml(ctx.getMessages(), request), composeErrorsHtml(ctx.getMessages(),
						request), userId);

			((AjaxPostFormResult)result).updateCsrfToken(request);
		}
		this.builder = new JavaScriptBuilder(result);
	}

	/**
	 * Converts the object passed in to JavaScript and streams it back to the client.
	 */
	@Override
	public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		//response.setContentType("text/javascript; charset="+response.getCharacterEncoding());

		sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");

		String ajaxRedirectCode = (String)request.getAttribute("ajaxRedirectCode");
		if (Tools.isNotEmpty(ajaxRedirectCode))
		{
			response.getWriter().println(ajaxRedirectCode);
		}

		this.builder.build(response.getWriter());

		response.flushBuffer();
	}

	/**
	 * Compose HTML snippet with messages based on standard properties enveloping used by Stripes
	 * <html:messages/> tag.
	 * @param messages
	 * @param request
	 * @return
	 */
	private String composeMessagesHtml(List<Message> messages, ServletRequest request)
	{
		String[] properties = getStandardMessageProperties(request);
		return composeResult(request, messages, properties, new MessageFilter()
		{
			@Override
			public boolean includeMessage(Message message)
			{
				return !(message instanceof ValidationError);
			}
		});
	}

	/**
	 * Compose HTML snippet with messages based on standard properties enveloping used by Stripes
	 * <html:errors/> tag.
	 * @param messages
	 * @param request
	 * @return
	 */
	private String composeErrorsHtml(List<Message> messages, ServletRequest request)
	{
		String[] properties = getStandardErrorProperties(request);
		return composeResult(request, messages, properties, new MessageFilter()
		{
			@Override
			public boolean includeMessage(Message message)
			{
				return (message instanceof ValidationError);
			}
		});
	}

	/**
	 * Compose HTML snippet with messages based on standard properties enveloping used by Stripes
	 * <html:messages/> tag.
	 * @param errors
	 * @param request
	 * @return
	 */
	public String composeErrorsHtml(ValidationErrors errors, ServletRequest request)
	{
		String[] properties = getStandardErrorProperties(request);

		if (errors != null && errors.size()==1 && errors.get("notSendMessage")!=null)
		{
			properties = getNotSendErrorProperties(request);
		}

		List<Message> messages = new ArrayList<>();
		if (errors != null) {
			for (List<ValidationError> validationErrors : errors.values())
			{
				messages.addAll(validationErrors);
			}
		}
		return composeResult(request, messages, properties, new MessageFilter()
		{
			@Override
			public boolean includeMessage(Message message)
			{
				return true;
			}
		});
	}

	/**
	 * Returns standard message Stripes properties in array.
	 * @param request
	 * @return
	 */
	private String[] getStandardMessageProperties(ServletRequest request)
	{
		Prop prop = Prop.getInstance(PageLng.getUserLng((HttpServletRequest) request));
		//ResourceBundle bundle = StripesFilterIway.getConfiguration().getLocalizationBundleFactory().getErrorMessageBundle(locale);
		return new String[]{prop.getText("stripes.messages.header"), prop.getText("stripes.messages.footer"),
					prop.getText("stripes.messages.beforeMessage"), prop.getText("stripes.messages.afterMessage")};
	}

	/**
	 * Returns standard message Stripes properties in array.
	 * @param request
	 * @return
	 */
	private String[] getStandardErrorProperties(ServletRequest request)
	{
		//Locale locale = request.getLocale();
		//ResourceBundle bundle = StripesFilterIway.getConfiguration().getLocalizationBundleFactory().getErrorMessageBundle(locale);

		Prop prop = Prop.getInstance(PageLng.getUserLng((HttpServletRequest) request));

		return new String[]{prop.getText("stripes.ajax.errors.header"), prop.getText("stripes.ajax.errors.footer"),
					prop.getText("stripes.ajax.errors.beforeError"), prop.getText("stripes.ajax.errors.afterError")};
	}

	private String[] getNotSendErrorProperties(ServletRequest request)
	{
		//Locale locale = request.getLocale();
		//ResourceBundle bundle = StripesFilterIway.getConfiguration().getLocalizationBundleFactory().getErrorMessageBundle(locale);

		Prop prop = Prop.getInstance(PageLng.getUserLng((HttpServletRequest) request));

		return new String[]{prop.getText("stripes.ajax.errors.header_not_send"), prop.getText("stripes.ajax.errors.footer_not_send"),
					prop.getText("stripes.ajax.errors.beforeError_not_send"), prop.getText("stripes.ajax.errors.afterError_not_send")};
	}

	/**
	 * Renders HTML snippet result containing all messages conforming to MessageFilter implementation
	 * enveloped by properties strings.
	 * @param request
	 * @param messages
	 * @param properties
	 * @param messageFilter
	 * @return
	 */
	private String composeResult(ServletRequest request, List<Message> messages, String[] properties, MessageFilter messageFilter)
	{
		boolean hasMessages = false;
		StringBuilder result = new StringBuilder(properties[0]);
		for (Message message : messages)
		{
			if (messageFilter.includeMessage(message))
			{
				hasMessages = true;
				result.append(properties[2]).append(message.getMessage(request.getLocale())).append(properties[3]);
			}
		}
		if (hasMessages)
		{
			result.append(properties[1]);
			Logger.debug(ActionContextJavaScriptResolution.class, "composeResult:"+result.toString());
			return result.toString();
		}
		else
		{
			Logger.debug(ActionContextJavaScriptResolution.class, "composeResult: null");
			return null;
		}
	}

	/**
	 * Interface used to filter messages going to output.
	 */
	private interface MessageFilter
	{
		/**
		 * Returns true or false if message should render in output.
		 * @param message
		 * @return
		 */
		boolean includeMessage(Message message);
	}
}
