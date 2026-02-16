package sk.iway.iwcm.system.stripes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.controller.LifecycleStage;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/**
 *  WebJETActionBean.java - supertrieda pre standardne Stripes Action Beany
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 3.3.2008 17:20:16
 *@modified     $Date: 2008/12/11 08:49:57 $
 */
public class WebJETActionBean implements ActionBean, IncludeRequestWrapperInterface
{

	public static final String RESOLUTION_CLOSE = "/components/reloadParentClose.jsp";
	public static final String RESOLUTION_CONTINUE = "/components/maybeError.jsp";
	public static final String RESOLUTION_NOT_LOGGED = "/components/maybeError.jsp";
	public static final String RESOLUTION_CSRF = "/components/csrfError.jsp";

	protected ActionBeanContext context;

	/**
	 * Vrati true, ak je prihlaseny administrator
	 * @return
	 */
	public boolean isAdminLogged()
	{
		Identity user = (Identity) getRequest().getSession().getAttribute(Constants.USER_KEY);
		if (user != null && user.isAdmin()) return true;
		getRequest().setAttribute("errorText", Prop.getInstance(getRequest()).getText("error.userNotLogged"));
		return false;
	}

	/**
	 * Vrati true, ak je prihlaseny administrator a ma povoleny dany modul
	 * @param module
	 * @return
	 */
	public boolean isAdminLoggedWithPerms(String module)
	{
		Identity user = (Identity) getRequest().getSession().getAttribute(Constants.USER_KEY);
		if (user != null && user.isAdmin() && user.isEnabledItem(module)) return true;
		getRequest().setAttribute("errorText", Prop.getInstance(getRequest()).getText("error.userNotLogged"));
		return false;
	}

	/**
	 * Vrati true ak je prihlaseny akykolvek user (navstevnik stranky)
	 * @return
	 */
	public boolean isAnyUserLogged()
	{
		Identity user = (Identity) getRequest().getSession().getAttribute(Constants.USER_KEY);
		if (user != null) return true;
		getRequest().setAttribute("errorText", Prop.getInstance(getRequest()).getText("error.userNotLogged"));
		return false;
	}

	public void setErrorText(String text)
	{
		getRequest().setAttribute("errorText", text);
	}

	public HttpServletRequest getRequest()
	{
		return context.getRequest();
	}

	public HttpSession getSession()
	{
		return context.getRequest().getSession();
	}

	public Identity getCurrentUser()
	{
		return UsersDB.getCurrentUser(context);
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

	private static Map<Class<? extends ActionBean>, List<String>> includeParamsOnly = new HashMap<>();

	@Before(stages={LifecycleStage.BindingAndValidation})
	public void prepareIncludeRequestWrapper()
	{
		prepareIncludeRequestWrapper(context, includeParamsOnly, getClass(), getClass());
	}

	@After(stages={LifecycleStage.BindingAndValidation})
	public void removeIncludeRequestWrapper()
	{
		removeIncludeRequestWrapper(context);
	}
}