package cn.bluejoe.elfinder.controller.executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;

public class UnlockfileCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json) throws Exception
	{
		String result = "ok";
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);


		LockfileCommandExecutor.unlock(fsi); 

		json.put("result", result);
	}



}


