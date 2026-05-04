package sk.iway.iwcm.form;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;


/**
 *  RegExpActionBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff Miroslav Repaský $
 *@version      $Revision: 1.3 $
 *@created      Date: 22.6.2011 18:09:24
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class RegExpActionBean implements ActionBean
{
	/*@ValidateNestedProperties({
		@Validate(field = "title", required = true, maxlength=40, on={"save"}),
		@Validate(field = "type", required = true, maxlength=40, on={"save"}),
		@Validate(field = "regExp", required = true, maxlength=120, on={"save"}),
		})*/
		private String title, typeOld, type, regExp;
	private ActionBeanContext context;

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

	public String getTitle()
	{
      return title;
	}
	public void setTitle(String value)
	{
		this.title = value;
	}

	public String getTypeOld()
	{
      return typeOld;
	}
	public void setTypeOld(String value)
	{
		this.typeOld = value;
	}

	public String getType()
	{
      return type;
	}
	public void setType(String value)
	{
		this.type = value;
	}

	public String getRegExp()
	{
      return regExp;
	}
	public void setRegExp(String value)
	{
		this.regExp = value;
	}

	@DefaultHandler
	public Resolution save()
	{
		Identity user = UsersDB.getCurrentUser(getContext().getRequest());
		if (user == null || user.isAdmin()==false) return(new ForwardResolution("/components/maybeError.jsp"));
		Prop prop = Prop.getInstance(context.getRequest());
		Logger.debug(null, "Diakritika v title v Bean: "+title);
		typeOld = context.getRequest().getParameter("typeExp");
		/*title = DocTools.removeChars(title);
		Logger.println(null, titleOld + " " + title);*/
		if(Tools.isNotEmpty(typeOld))
		{
			if(typeOld.compareTo(type) != 0 && FormDB.getRegExpByType(type) != null)	//kontrola zaznamu s rovnakym type, okrem vybraneho
			{
				context.getRequest().setAttribute("error", prop.getText("components.form.admin_form.error_same_title"));
				return(new ForwardResolution("/components/maybeError.jsp"));	//ak uz zaznam s rovnakym title existuje vyhodim chybu
			}
			else
			{
				FormDB.updateRegularExpression(title, type, typeOld, regExp);
				FormDB.getInstance(true);
				return new ForwardResolution("/components/reloadParentClose.jsp");
			}
		}

		if(FormDB.getRegExpByType(type) == null)
			FormDB.saveRegularExpression(title, type, regExp);	//ulozim regularny vyraz
		else
		{
			context.getRequest().setAttribute("error", prop.getText("components.form.admin_form.error_same_title"));
			return(new ForwardResolution("/components/maybeError.jsp"));	//ak uz zaznam s rovnakym title existuje vyhodim chybu
		}
		FormDB.getInstance(true);
		//context.getRequest().setAttribute("result", ddb.getAll());
		return new ForwardResolution("/components/reloadParentClose.jsp");
	}
}
