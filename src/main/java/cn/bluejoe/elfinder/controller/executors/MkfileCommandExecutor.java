package cn.bluejoe.elfinder.controller.executors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

public class MkfileCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");
		String name = request.getParameter("name");
		Prop prop = Prop.getInstance(request);

		if (FileBrowserTools.hasForbiddenSymbol(name)) {
			json.put("error", prop.getText("components.elfinder.commands.error.banned_character"));
			return;
		}

		FsItemEx fsi = super.findItem(fsService, target);

		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getPath()))
		{
			FsItemEx dir = new FsItemEx(fsi, name);
			dir.createFile();
			json.put("added", new Object[] { getFsItemInfo(request, dir) });
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.mkfile.error", fsi.getPath()));
			json.put("added", new Object[] {});
		}

	}
}
