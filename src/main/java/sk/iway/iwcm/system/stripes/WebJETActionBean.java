package sk.iway.iwcm.system.stripes;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.StripesRequestWrapper;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
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
public class WebJETActionBean implements ActionBean
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
	
	
	private static Map<Class<? extends ActionBean>, List<String>> includeParamsOnly = new HashMap<Class<? extends ActionBean>, List<String>>();

	/**
	 * vykona sa pri kazdom requeste pred BindingAndValidation stage a v pripade ze je actionBean anotovana InjectPageParams,
	 * zabezpeci zabalenie povodneho requestu do specialneho-vyčuraného requestu, ktory podsunie pageparams ako parametre requestu
	 * zaroven zabezpeci ze nieje mozne poslat ako parameter actionbeane taky ktory ma rovnaky nazov ako atribut actionbeany, anotovany
	 * // 
	 */
	@Before(stages={LifecycleStage.BindingAndValidation})
	public void prepareIncludeRequestWrapper()
	{
		//,sk.iway.iwcm.stripes.include.IncludePageParamsInterceptor
		HttpServletRequest request = context.getRequest();
		// ak mam nejake pageParams
		if (request.getAttribute("includePageParams")!= null || Tools.isNotEmpty(request.getParameter("__ppState")))
		{
			//ziskam si mapu params
			PageParams pp = new PageParams(request);
			Map<String, String> includeParams = pp.getParams();
			
			// najdem StripesRequestWrapper (ten moze byt niekde v request wrapperoch)
			StripesRequestWrapper stripesWrapper = StripesRequestWrapper.findStripesWrapper(request);
			//nemal by byt null, kedze ide o stripes RQ, ale istota je gulomet a zaroven actionBeana je anotovana @InjectPageParams
			if (stripesWrapper!=null && getClass().isAnnotationPresent(BindPageParams.class))
			{
				HttpServletRequest originalRequest = (HttpServletRequest) stripesWrapper.getRequest();
				
				Class<? extends ActionBean> actionBeanClass = getClass();
				
				// ziskam si zoznam poli anotovanych PageParamOnly, bud uz ziskane z mapy, alebo prebehnem actionbean classu
				List<String> paramNames = null;
				if (includeParamsOnly.containsKey(actionBeanClass))
				{
					paramNames = includeParamsOnly.get(actionBeanClass);
				}
				else
				{
					paramNames = new ArrayList<String>();
					Class<?> clazz = getClass();
					do
					{
						for (Field field : clazz.getDeclaredFields())
						{
							if (field.isAnnotationPresent(PageParamOnly.class))
							{
								paramNames.add(field.getName());
							}
						}
						clazz = clazz.getSuperclass();
					} while (clazz != null);
					includeParamsOnly.put(actionBeanClass, paramNames);
				}
				// vytvorim si novy rq wrapper (na tento ucel pripraveny)
				IncludePageParamsRequestWrapper includePPRequestWrapper = new IncludePageParamsRequestWrapper(originalRequest, includeParams, paramNames);
				// a ten pridam az do striper rq wrappera, aby binding zohladnoval pageParams
				stripesWrapper.setRequest(includePPRequestWrapper);
			}
		}
	}
	
	/**
	 * odstrani IncludePageParamsRequestWrapper z stripesrequestu aby v dalsom vykonavani uz nezavadzal
	 */
	@After(stages={LifecycleStage.BindingAndValidation})
	public void removeIncludeRequestWrapper()
	{
		HttpServletRequest request = context.getRequest();
		StripesRequestWrapper stripesWrapper = StripesRequestWrapper.findStripesWrapper(request);
		if (stripesWrapper!=null)
		{
			ServletRequest maybeIncludeRQW = stripesWrapper.getRequest();
			// zistim ci je to instancia includeReqWrappera, ak ano odstranim ju
			if (maybeIncludeRQW instanceof IncludePageParamsRequestWrapper)
			{
				IncludePageParamsRequestWrapper includeRQW = (IncludePageParamsRequestWrapper)maybeIncludeRQW;
				ServletRequest originalRequest = includeRQW.getRequest();
				stripesWrapper.setRequest(originalRequest);
			}
		}
	}
}
