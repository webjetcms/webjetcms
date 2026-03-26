package cn.bluejoe.elfinder.controller.executors;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.service.FsService;

/**
 * JEEFF / WEBJET command pre ziskanie places pre aktualnu stranku
 *  InfoCommandExecutor.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.3.2015 16:06:53
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class InfoCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json) throws Exception
	{		
		List<String> files = new ArrayList<String>();
		files.add("/images/gallery");
		files.add("/images/demo7/email");
		
		json.put("files", files);
	}
}