package sk.iway.iwcm.stripes;

import jakarta.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrorHandler;
import net.sourceforge.stripes.validation.ValidationErrors;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.ActionContextJavaScriptResolution;
import sk.iway.iwcm.tags.JSEscapeTag;

/**
 *  FormMailAjax.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 3.2.2008 16:02:43
 *@modified     $Date: 2008/03/14 09:27:53 $
 */
public class FormMailAjax implements ActionBean, ValidationErrorHandler
{
	private ActionBeanContext context;

	@Override
	public Resolution handleValidationErrors(ValidationErrors errors) throws Exception
	{
		Logger.debug(FormMailAjax.class, "handleValidationErrors: "+errors);
		return new ActionContextJavaScriptResolution(this);
	}

	@DefaultHandler
	public Resolution defaultResolution()
	{
		Logger.debug(FormMailAjax.class, "DefaultResolution");

		String formName = DB.internationalToEnglish(context.getRequest().getParameter("savedb"));

		//POZOR vsetky request pemenne treba ziskavat z tohto, nie z context.getRequest() - inak by neslo nastavenie formu cez databazove hodnoty
		HttpServletRequest request = FormMailAction.fillRequestWithDatabaseOptions(formName, context.getRequest(), null);
		String forward = FormMailAction.saveForm(request, null, null, getContext());

		if (context.getValidationErrors().size()>0)
		{
			return new ActionContextJavaScriptResolution(this);
		}

		//tato metoda berie hodnotu podla session, takze sa pouzije spravny jazyk
		Prop prop = Prop.getInstance(PageLng.getUserLng(getContext().getRequest()));
		if (forward.indexOf("formsend=true")!=-1)
		{
			String key = "checkform.sent";
			if (forward.indexOf("DoubleOptIn")!=-1) key = "checkform.sentDoubleOptIn";

			context.getMessages().add(new SimpleMessage(prop.getTextWithSuffix(key, formName)));
			String forwardOk = request.getParameter("forward");
			if (Tools.isNotEmpty(forwardOk))
			{
				context.getMessages().add(new SimpleMessage("<script type='text/javascript'>window.location.href=\""+ Tools.replace(JSEscapeTag.jsEscape(forwardOk), "&amp;", "&")+"\";</script>"));
			}
		}
		else if (forward.indexOf("formfail=formIsAllreadySubmitted")!=-1)
		{
			context.getValidationErrors().add("notSendMessage", new SimpleError(prop.getText("checkform.formIsAllreadySubmitted")));
		}
		else if (forward.indexOf("formfail=javascript")!=-1)
		{
			context.getValidationErrors().add("notSendMessage", new SimpleError(prop.getText("checkform.fail_javascript")));
		}
		else if (forward.indexOf("formfail=probablySpamBot")!=-1)
		{
			context.getValidationErrors().add("notSendMessage", new SimpleError(prop.getText("checkform.fail_probablySpamBot")));
		}
		else if (forward.indexOf("formfail=requiredFields")!=-1)
		{
			context.getValidationErrors().add("recipients", new SimpleError(prop.getText("checkform.fail_requiredFields")));
		}
		else if (forward.indexOf("formfail=bad_file")!=-1)
		{
			context.getValidationErrors().add("recipients", new SimpleError(prop.getText("checkform.bad_file")));
		}
		else if (forward.indexOf("formfail=captcha")!=-1)
		{
			context.getValidationErrors().add("recipients", new SimpleError(prop.getText("captcha.textNotCorrect")));
		}
		else
		{
			context.getValidationErrors().add("notSendMessage", new SimpleError(prop.getTextWithSuffix("checkform.sendfail", formName)));
		}

		//query was created
		return new ActionContextJavaScriptResolution(this);
	}

	@Override
	public ActionBeanContext getContext()
	{
		return context;
	}

	@Override
	public void setContext(ActionBeanContext context)
	{
		this.context = context;
	}
}
