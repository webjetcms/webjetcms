package cn.bluejoe.elfinder.controller.executors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  ArchiveCommandExecutor.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff suchy $
 *@version      $Revision: 1.3 $
 *@created      Date: Sep 22, 2015 12:32:03 PM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ResizeCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		//[19.11 17:10:19 {iway} {SetCharacterEncodingFilter}] GET /admin/elfinder-connector/?volumes=images&docId=4667&groupId=1267&cmd=resize&target=iwcm_fs_ap_volume_L2ltYWdlcy9jei93ZWItcmVzZW5pL2ludGVyd2F5L3Byb2R1a3R5L3JlZGFrY25pLXN5c3RlbS13ZWJqZXQva29ua3VyZW5jbmUtdnlob2R5L3NuaW1rYS1vYnJhem92a3ktMjAxNS0xMS0xMS1vLTE1LjQwLjAyLnBuZw_E_E&width=159&height=143&mode=resize&_=1447949419114
		
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);
		String virtualPath = sk.iway.iwcm.system.elfinder.FsService.getVirtualPath(fsi);
		if (virtualPath.startsWith("/")==false) virtualPath = "/"+virtualPath;
		
		String mode = request.getParameter("mode");
		int width = Tools.getIntValue(request.getParameter("width"), -1);
		int height = Tools.getIntValue(request.getParameter("height"), -1);
		int x = Tools.getIntValue(request.getParameter("x"), -1);
		int y = Tools.getIntValue(request.getParameter("y"), -1);
		double degree = Tools.getDoubleValue(request.getParameter("degree"), -1);
		IwcmFile tempFile = new IwcmFile(Tools.getRealPath(virtualPath));
		
		Logger.debug(ResizeCommandExecutor.class, "mode"+mode+" path="+virtualPath+" w="+width+" h="+height);
		
		if (tempFile.exists()==false || tempFile.canRead()==false) return;
		
		if ("resize".equals(mode))
		{
			int ret = ImageTools.resizeImage(tempFile, width, height);
			Logger.debug(ResizeCommandExecutor.class, " ret="+ret);
		}
		else if ("crop".equals(mode))
		{
			int ret = ImageTools.cropImage(tempFile, width, height, x, y);
			Logger.debug(ResizeCommandExecutor.class, " ret="+ret);
		}
		else if ("rotate".equals(mode))
		{
			int ret = ImageTools.rotateImage(tempFile, degree);
			Logger.debug(ResizeCommandExecutor.class, " ret="+ret);
		}
				
		// posli info o zmene
		json.put("changed", new Object[] { getFsItemInfo(request, fsi) });
	}
}
