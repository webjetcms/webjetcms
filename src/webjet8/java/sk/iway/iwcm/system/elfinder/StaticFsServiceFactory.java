package sk.iway.iwcm.system.elfinder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import cn.bluejoe.elfinder.service.FsServiceFactory;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;

public class StaticFsServiceFactory implements FsServiceFactory
{
	@Override
	public FsService getFileService(HttpServletRequest request, ServletContext servletContext)
	{
		Logger.debug(StaticFsServiceFactory.class, "getFileService, req="+request+" context="+servletContext);

		int type = FsService.TYPE_ALL;
		if ("images".equals(request.getParameter("volumes"))) type = FsService.TYPE_IMAGES;
		else if ("link".equals(request.getParameter("volumes"))) type = FsService.TYPE_LINK;
		else if ("files".equals(request.getParameter("volumes"))) type = FsService.TYPE_FILES;
		else if("pages".equals(request.getParameter("volumes"))) type = FsService.TYPE_PAGES;
		else if("videos".equals(request.getParameter("volumes"))) type = FsService.TYPE_VIDEOS;

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null)
		{
			rb.setRequest(request);
		}

		FsService _fsService = new FsService(type);

		return _fsService;
	}
}
