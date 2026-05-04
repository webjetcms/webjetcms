package cn.bluejoe.elfinder.controller.executors;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.elfinder.FolderPropertiesService;
import sk.iway.iwcm.users.UsersDB;

public class RmCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String[] targets = request.getParameterValues("targets[]");
		Prop prop = Prop.getInstance(request);
		//String current = request.getParameter("current");
		List<String> removed = new ArrayList<String>();
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();

		StringBuilder deniedFilenames = new StringBuilder();
		for (String target : targets)
		{

			FsItemEx ftgt = super.findItem(fsService, target);
			boolean deleted = false;
			boolean isFolder = ftgt.isFolder();
			if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), ftgt.getParent().getPath()))
			{
				deleted = ftgt.delete();
				if (deleted)
				{
					removed.add(ftgt.getHash());

					if(isFolder == true) {
						FolderPropertiesService.deleteFolderProperties(ftgt.getPath(), request);
					}
				}
			}

			if (deleted == false)
			{
				if (!Tools.isEmpty(deniedFilenames)) deniedFilenames.append(", ");
				deniedFilenames.append(ftgt.getName());
			}
		}

		if (!Tools.isEmpty(deniedFilenames))
			json.put("error", prop.getText("components.elfinder.commands.rm.error", deniedFilenames.toString()));

		json.put("removed", removed.toArray());
	}
}
