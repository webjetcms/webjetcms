package cn.bluejoe.elfinder.controller.executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.GalleryDBTools;
import sk.iway.iwcm.io.IwcmFile;

/**
 * JEEFF / WEBJET command pre ziskanie velkosti obrazka
 *  DimCommandExecutor.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.3.2015 16:06:53
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DimCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json) throws Exception
	{
		String target = request.getParameter("target");
		FsItemEx fsi = super.findItem(fsService, target);
		
		String url = fsi.getPath();
		IwcmFile file = new IwcmFile(Tools.getRealPath(url));
		int dim[] = GalleryDBTools.getImageSize(file);
		
		json.put("dim", dim[0]+"x"+dim[1]);
	}
}