package cn.bluejoe.elfinder.controller.executor;

import java.io.PrintWriter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.system.stripes.MultipartWrapper;

public abstract class AbstractJsonCommandExecutor extends AbstractCommandExecutor
{
	@Override
	final public void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext) throws Exception
	{
		JSONObject json = new JSONObject();
		try
		{
			execute(fsService, request, servletContext, json);
			//response.setContentType("application/json; charset=UTF-8");
			response.setContentType("text/html; charset=UTF-8");

			//WebJET: pridaj reload stromu
			if (sk.iway.iwcm.RequestBean.getAttribute("forceReloadTree")!=null) {
				json.put("forceReloadTree", true);
				//aby stihli dobehnut procesy
				MultipartWrapper.slowdownUpload();
			}

			PrintWriter writer = response.getWriter();
			json.write(writer);
			writer.flush();
			writer.close();
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			json.put("error", e.getMessage());
		}
	}

	protected abstract void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext,
			JSONObject json) throws Exception;

}