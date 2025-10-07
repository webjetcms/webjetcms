package sk.iway.iwcm.system.stripes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.controller.StripesRequestWrapper;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;

public interface IncludeRequestWrapperInterface {

	/**
	 * odstrani IncludePageParamsRequestWrapper z stripesrequestu aby v dalsom vykonavani uz nezavadzal
	 */
	default void removeIncludeRequestWrapper(ActionBeanContext context)
	{
		HttpServletRequest request = context.getRequest();
		StripesRequestWrapper stripesWrapper = StripesRequestWrapper.findStripesWrapper(request);
		if (stripesWrapper!=null)
		{
			ServletRequest maybeIncludeRQW = stripesWrapper.getRequest();
			// zistim ci je to instancia includeReqWrappera, ak ano odstranim ju
			if (maybeIncludeRQW instanceof IncludePageParamsRequestWrapper includeRQW)
			{
				ServletRequest originalRequest = includeRQW.getRequest();
				stripesWrapper.setRequest(originalRequest);
			}
		}
	}

	/**
	 * vykona sa pri kazdom requeste pred BindingAndValidation stage a v pripade ze je actionBean anotovana InjectPageParams,
	 * zabezpeci zabalenie povodneho requestu do specialneho-vyčuraného requestu, ktory podsunie pageparams ako parametre requestu
	 * zaroven zabezpeci ze nieje mozne poslat ako parameter actionbeane taky ktory ma rovnaky nazov ako atribut actionbeany, anotovany
	 * //
	 */
    default void prepareIncludeRequestWrapper(ActionBeanContext context, Map<Class<? extends ActionBean>, List<String>> includeParamsOnly, Class<?> clazz, Class<? extends ActionBean> actionBeanClass)
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

				// ziskam si zoznam poli anotovanych PageParamOnly, bud uz ziskane z mapy, alebo prebehnem actionbean classu
				List<String> paramNames = null;
				if (includeParamsOnly.containsKey(actionBeanClass))
				{
					paramNames = includeParamsOnly.get(actionBeanClass);
				}
				else
				{
					paramNames = new ArrayList<>();
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
}
