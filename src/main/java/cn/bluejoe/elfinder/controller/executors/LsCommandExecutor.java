package cn.bluejoe.elfinder.controller.executors;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import org.json.JSONObject;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LsCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");

		Map<String, FsItemEx> files = new HashMap<String, FsItemEx>();
		FsItemEx fsi = super.findItem(fsService, target);
		super.addChildren(files, fsi);

		// zoznam suborov cakajucich na upload, pre kontrolu ci na server neexistuje subor s rovnakym nazovm
		final List<String> intersect = Tools.getStringListValue(request.getParameterValues("intersect[]"));

		if (!intersect.isEmpty()) {
			List<String> useFileNameFixPaths = Tools.getStringListValue(new String[]{"/files", "/images"});
			FsItemEx cwd = findCwd(fsService, target);
			String path = cwd.getPath();
			boolean useFileNameFix = useFileNameFixPaths.stream().anyMatch(path::startsWith);

			// najdenia suboru s fixnutym menom, ak exisuje
			if (useFileNameFix) {
				Map<String, String> intersectMap = new HashMap<>();
				for (String s : intersect) {
					String directory = DB.internationalToEnglish(s);
					directory = DocTools.removeCharsDir(directory, true).toLowerCase();

					if (!intersectMap.containsKey(directory)) {
						intersectMap.put(directory, s);
					}
				}

				List<String> hits = new ArrayList<>();
				for (Map.Entry<String, FsItemEx> entry : files.entrySet()) {
					String name = entry.getValue().getName();
					if(intersectMap.containsKey(name)) {
						hits.add(intersectMap.get(name));
					}
				}

				json.put("list", hits.toArray());
			}
			// najdenia suboru, ak exisuje
			else {
				json.put("list", files.entrySet().stream().map(e -> e.getValue().getName()).filter(intersect::contains).toArray());
			}

			return;
		}

		json.put("list", files2JsonArray(request, files.values()));
	}
}
