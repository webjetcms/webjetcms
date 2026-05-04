package cn.bluejoe.elfinder.controller.executors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
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

		List<Map<String, Object>> added = new ArrayList<>();

		if (Tools.isNotEmpty(target)) {
			if (dirs != null && dirs.length > 0) {
				for (String dir : dirs) {
					Map<String, Object> fsItemEx = mkDir(target, dir);
					if (fsItemEx != null) {
						added.add(fsItemEx);
					}
				}
			}
			else if (Tools.isNotEmpty(name)) {
				Map<String, Object> fsItemEx = mkDir(target, name);
				if (fsItemEx != null) {
					added.add(fsItemEx);
				}
			}
		}
		else {
			json.put("error", prop.getText("target cannot be null"));
		}
		json.put("added", added.toArray());
	}

	private Map<String, Object> mkDir(String target, String name) throws IOException, JSONException
	{
		FsItemEx fsi = super.findItem(fsService, target);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getPath()))
		{
			// remove diacritics
			name = IwcmFsVolume.removeSpecialChars(name, fsi);

			FsItemEx dir = new FsItemEx(fsi, name);
			dir.createFolder();

			return getFsItemInfo(request, dir);
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.mkdir.error", fsi.getPath()));
			//json.put("added", new Object[] {});
			return null;
		}
	}
}
