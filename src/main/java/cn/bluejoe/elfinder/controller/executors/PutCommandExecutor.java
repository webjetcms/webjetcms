package cn.bluejoe.elfinder.controller.executors;

import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

public class PutCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);

		Prop prop = Prop.getInstance(request);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getParent().getPath()))
		{
			//skus odhadnut encoding
			String content = request.getParameter("content");
			String encoding = GetCommandExecutor.getEncoding(fsi, content);

			OutputStream os = fsi.openOutputStream();
			IOUtils.write(content, os, encoding);
			os.close();
			json.put("changed", new Object[] { super.getFsItemInfo(request, fsi) });
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.put.error", fsi.getParent().getPath()));
		}

	}
}
