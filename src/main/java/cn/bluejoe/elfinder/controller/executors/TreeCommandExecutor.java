package cn.bluejoe.elfinder.controller.executors;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.DB;

public class TreeCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");

		Map<String, FsItemEx> files = new HashMap<String, FsItemEx>();
		FsItemEx fsi = super.findItem(fsService, target);
		String query = request.getParameter("treeSearchValue");
		if (query != null && !query.isBlank()) {
			try {
				FolderSearchResult result = searchFolders(fsi, query, request.getParameter("treeSearchType"));
				json.put("tree", files2JsonArray(request, result.folders().values()));
				json.put("treeSearchMatches", result.matches());
				json.put("treeSearch", true);
			} catch (IOException ex) {
				json.put("error", "errOpen");
			}
			return;
		}
		super.addSubfolders(files, fsi);

		json.put("tree", files2JsonArray(request, files.values()));
	}

    record FolderSearchResult(Map<String, FsItemEx> folders, Set<String> matches) { }

    /** Returns matching readable folders with their ancestors for the filtered navigation tree. */
    static FolderSearchResult searchFolders(FsItemEx root, String query, String matchType) throws IOException {
        Map<String, FsItemEx> folders = new LinkedHashMap<>();
        Map<String, FsItemEx> parents = new HashMap<>();
        Set<String> matches = new HashSet<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<FsItemEx> pending = new ArrayDeque<>();
        pending.add(root);
        String wanted = DB.internationalToEnglish(query.trim()).toLowerCase(Locale.ROOT);
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!pending.isEmpty()) {
            if (visited.size() >= 10000 || System.nanoTime() > deadline) throw new IOException("errTimeout");
            FsItemEx folder = pending.removeFirst();
            if (!folder.isFolder() || !folder.isReadable(folder) || !visited.add(folder.getPath())) continue;
            String name = DB.internationalToEnglish(folder.getName()).toLowerCase(Locale.ROOT);
            boolean match = switch (matchType == null ? "contains" : matchType) {
                case "startwith" -> name.startsWith(wanted);
                case "endwith" -> name.endsWith(wanted);
                case "equals" -> name.equals(wanted);
                default -> name.contains(wanted);
            };
            if (match) {
                matches.add(folder.getHash());
                FsItemEx ancestor = folder;
                while (ancestor != null && folders.putIfAbsent(ancestor.getHash(), ancestor) == null) {
                    ancestor = parents.get(ancestor.getHash());
                }
            }
            for (FsItemEx child : folder.listChildren()) {
                if (child.isFolder() && !child.getHash().equals(root.getHash())) {
                    parents.putIfAbsent(child.getHash(), folder);
                    pending.addLast(child);
                }
            }
        }
        return new FolderSearchResult(folders, matches);
    }
}
