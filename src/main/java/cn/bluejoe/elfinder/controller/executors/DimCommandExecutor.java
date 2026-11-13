package cn.bluejoe.elfinder.controller.executors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

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
		java.nio.file.Path selected=sk.iway.iwcm.branch.BranchContexts.current().isBranch()?sk.iway.iwcm.branch.BranchFiles.read(url):null;
        if(sk.iway.iwcm.branch.BranchContexts.current().isBranch()&&selected==null) throw new java.io.IOException("The working image does not exist");
        IwcmFile file = new IwcmFile(selected==null?Tools.getRealPath(url):selected.toString());
		int dim[] = GalleryDBTools.getImageSize(file);
		
		json.put("dim", dim[0]+"x"+dim[1]);
	}
}