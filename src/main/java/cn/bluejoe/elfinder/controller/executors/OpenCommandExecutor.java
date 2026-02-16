package cn.bluejoe.elfinder.controller.executors;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.service.FsVolume;
import sk.iway.iwcm.Tools;

public class OpenCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		boolean init = request.getParameter("init") != null;
		boolean tree = request.getParameter("tree") != null;
		String target = request.getParameter("target");
		String startPath = request.getParameter("startPath");

		Map<String, FsItemEx> files = new LinkedHashMap<String, FsItemEx>();
		if (init)
		{
			json.put("api", 2.1);
			json.put("netDrivers", new Object[0]);
		}

		if (tree)
		{
			for (FsVolume v : fsService.getVolumes())
			{
				FsItemEx root = new FsItemEx(v.getRoot(), fsService);
				files.put(root.getHash(), root);
				addSubfolders(files, root);
			}
		}

		FsItemEx cwd = findCwd(fsService, target);

		if (Tools.isNotEmpty(startPath) && files.size() > 0) {

			if (startPath.endsWith("/")) {
				startPath = startPath.substring(0, startPath.length() - 1);
			}

			for (Map.Entry<String, FsItemEx> file : files.entrySet())
			{
				FsItemEx item = file.getValue();
				String path = item.getPath();
				if (path.equals(startPath)) {
					cwd = findCwd(fsService, file.getKey());
					break;
				}
			}
		}

		// LPA zakomentovane pretoze elfinder uz nepotrebuje cwd - #20705 - wj8 - elfinder update
		//files.put(cwd.getHash(), cwd);
		addChildren(files, cwd);

		json.put("files", files2JsonArray(request, files.values()));
		json.put("cwd", getFsItemInfo(request, cwd));
		json.put("options", getOptions(request, cwd));
	}
}