package cn.bluejoe.elfinder.controller.executors;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import org.json.JSONObject;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

public class RenameCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");
		String name = request.getParameter("name");
		FsItemEx fsi = super.findItem(fsService, target);
		Prop prop = Prop.getInstance(request);

		if (FileBrowserTools.hasForbiddenSymbol(name)) {
			json.put("error", prop.getText("components.elfinder.commands.rename.error.banned_character"));
			return;
		}

		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getParent().getPath()))
		{
			// remove diacritics
			name = IwcmFsVolume.removeSpecialChars(name, fsi);

			FsItemEx dst = new FsItemEx(fsi.getParent(), name);

			//#20481 - po vystrihnuti/premenovani vytvori redirect
			if(PasteCommandExecutor.isAllowedFolder(fsi.getPath(), Constants.getString("elfinderRedirectFolders")))
				PasteCommandExecutor.createRedirect(fsi, fsi.getPath(), dst.getPath(), request);

			fsi.renameTo(dst);

			json.put("added", new Object[] { getFsItemInfo(request, dst) });
			json.put("removed", new String[] { target });
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.rename.error", fsi.getParent().getPath()));
		}
	}
}
