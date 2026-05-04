package sk.iway.iwcm.stripes;

import java.util.Enumeration;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.ValidationErrorHandler;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.stripes.ActionContextJavaScriptResolution;

/**
 *  RegUserAjaxAction.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 6.3.2008 22:53:01
 *@modified     $Date: 2008/07/29 14:42:14 $
 */
public class RegUserAjaxAction extends RegUserAction implements ValidationErrorHandler
{
	@Override
	public Resolution handleValidationErrors(ValidationErrors errors) throws Exception
	{
		Logger.debug(RegUserAjaxAction.class, "handleValidationErrors: "+errors);
		return new ActionContextJavaScriptResolution(this);
	}
	@Override
	public Resolution bSubmit()
	{
		Logger.debug(RegUserAjaxAction.class, "bSubmit");

		super.bSubmit();

		return new ActionContextJavaScriptResolution(this);
	}

	@ValidationMethod(on="bSubmit")
	@Override
	public void validation(ValidationErrors errors)
	{
		Logger.debug(RegUserAjaxAction.class, "validation");

		Enumeration<String> parameters = getRequest().getParameterNames();
		while (parameters.hasMoreElements())
		{
			String name = parameters.nextElement();
			Logger.debug(RegUserAjaxAction.class, "params: "+name+"="+getRequest().getParameter(name));
		}

		//Prop prop = Prop.getInstance(getRequest());
		//errors.add("user.email", new SimpleError(prop.getText("userForm.err.email")));

		super.validation(errors);
	}
}
