package cn.bluejoe.elfinder.controller.executors;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import org.json.JSONObject;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.users.UsersDB;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LsCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null) {
			throw new Exception("User not logged in");
		}

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
					String name = IwcmFsVolume.normalizeUnicode(IwcmFsVolume.removeSpecialChars(s, fsi, user));

					if (!intersectMap.containsKey(name)) {
						intersectMap.put(name, s);
					}
				}

				List<String> hits = new ArrayList<>();
				for (Map.Entry<String, FsItemEx> entry : files.entrySet()) {
					String name = IwcmFsVolume.normalizeUnicode(entry.getValue().getName());
					if(intersectMap.containsKey(name)) {
						hits.add(intersectMap.get(name));
					}
				}

				json.put("list", hits.toArray());
			}
			// najdenia suboru, ak exisuje
			else {
				Set<String> normalizedIntersect = intersect.stream().map(IwcmFsVolume::normalizeUnicode).collect(Collectors.toSet());
				json.put("list", files.entrySet().stream().map(e -> e.getValue().getName()).filter(name -> (normalizedIntersect.contains(IwcmFsVolume.normalizeUnicode(name)) || intersect.contains(name))).toArray());
			}

			return;
		}

		json.put("list", files2JsonArray(request, files.values()));
	}
}
