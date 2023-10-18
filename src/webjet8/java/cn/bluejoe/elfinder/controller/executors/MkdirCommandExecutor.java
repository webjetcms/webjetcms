package cn.bluejoe.elfinder.controller.executors;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

public class MkdirCommandExecutor extends AbstractJsonCommandExecutor
{
	FsService fsService;
	Prop prop;
	HttpServletRequest request;
	JSONObject json;

	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		this.fsService = fsService;
		this.prop = Prop.getInstance(request);
		this.request = request;
		this.json = json;

		String target = Tools.getStringValue(request.getParameter("target"), "");
		String name = Tools.getStringValue(request.getParameter("name"), "");
		String[] dirs = request.getParameterValues("dirs[]");

		if (FileBrowserTools.hasForbiddenSymbol(name)) {
			json.put("error", prop.getText("components.elfinder.commands.error.banned_character"));
			return;
		}

		if (Tools.isNotEmpty(target)) {
			if (dirs != null && dirs.length > 0) {
				for (String dir : dirs) {
					mkDir(target, dir);
				}
			}
			else if (Tools.isNotEmpty(name)) {
				mkDir(target, name);
			}
		}
		else {
			json.put("error", prop.getText("target cannot be null"));
		}
	}

	private void mkDir(String target, String name) throws IOException, JSONException
	{
		FsItemEx fsi = super.findItem(fsService, target);
		Prop prop = Prop.getInstance(request);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getPath()))
		{
			if (fsi.getPath().startsWith("/files") || fsi.getPath().startsWith("/images"))
			{
				name = DB.internationalToEnglish(name);
				name = DocTools.removeCharsDir(name, true).toLowerCase();
			}
			FsItemEx dir = new FsItemEx(fsi, name);
			dir.createFolder();

			json.put("added", new Object[] { getFsItemInfo(request, dir) });
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.mkdir.error", fsi.getPath()));
			json.put("added", new Object[] {});
		}
	}
}
