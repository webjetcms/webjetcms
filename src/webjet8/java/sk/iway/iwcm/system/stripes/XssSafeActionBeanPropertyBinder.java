package sk.iway.iwcm.system.stripes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.controller.DefaultActionBeanPropertyBinder;
import net.sourceforge.stripes.controller.ParameterName;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.tags.support_logic.CustomResponseUtils;

/**
 *  XssSafeActionBeanPropertyBinder.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: thaber $
 *@version      $Revision: 1.1 $
 *@created      Date: Jul 24, 2009 1:52:05 PM
 *@modified     $Date: 2009/07/30 08:56:44 $
 */
public class XssSafeActionBeanPropertyBinder extends DefaultActionBeanPropertyBinder
{
	@Override
	protected SortedMap<ParameterName, String[]> getParameters(ActionBean actionBean)
	{
		Map<String, String[]> requestParameters = actionBean.getContext().getRequest().getParameterMap();
      SortedMap<ParameterName, String[]> parameters = new TreeMap<ParameterName,String[]>();
      String url = PathFilter.getOrigPath(actionBean.getContext().getRequest());

      boolean xssFiltering = true;

      String[] noFilteringPaths = Constants.getString("stripesXssNoFilteringPaths").split(";");

      for (String path:noFilteringPaths)
      {
      	if (url.startsWith(path))
      	{
      		xssFiltering = false;
      		break;
      	}
      }



      for (Map.Entry<String,String[]> entry : requestParameters.entrySet())
      {

      	 if (xssFiltering)
			{
				List<String> values = new ArrayList<String>();
				for (String val : entry.getValue())
				{
					values.add(CustomResponseUtils.filter(val));
				}
				parameters.put(new ParameterName(entry.getKey().trim()), values.toArray(new String[values.size()]));
			}
      	 else
      	 {
      		 parameters.put(new ParameterName(entry.getKey().trim()),entry.getValue());
      	 }

      }

      return parameters;
	}
}
