package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.rag.search.SemanticSearchResult;
import sk.iway.iwcm.rag.search.SemanticSearchService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.users.UsersDB;

public class SemanticSearchAction {

    private SemanticSearchAction() {
        // private constructor to prevent instantiation
    }

	/**
	 * Standalone semantic search using pgvector embeddings.
	 * Same pagination and request attributes as DB search, but results come purely from semantic search.
	 */
	protected static String search(HttpServletRequest request, HttpServletResponse response) {
		Identity user = UsersDB.getCurrentUser(request);

		String forward = "success";
		String error = "error";

		int perPage = 10;
		try {
			if (SearchAction.getParamAttribute("perpage", request, "10") != null)
				perPage = Integer.parseInt(SearchAction.getParamAttribute("perpage", request, "10"));
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}

		String rq = request.getParameter("index");
		int index = 0;
		if (rq != null) {
			try {
				index = Integer.parseInt(rq);
			} catch (Exception e) {
				index = 0;
			}
		}

		String words = request.getParameter("words");
		if (Tools.isEmpty(words)) words = (String) request.getAttribute("words");
		if (words == null) words = "";
		String text = request.getParameter("text");
		if (Tools.isNotEmpty(text)) words = text;
		if (Tools.isNotEmpty((String) request.getAttribute("forceWords"))) {
			words = (String) request.getAttribute("forceWords");
		}

		words = words.replace('\'', ' ').replace(',', ' ').replace('.', ' ').replace(';', ' ');
		if (Tools.isNotEmpty(Constants.getString("searchActionOmitCharacters"))) {
			words = words.replaceAll("[" + Constants.getString("searchActionOmitCharacters") + "]", "");
		}
		words = words.trim();

		if (words.isEmpty()) {
			request.setAttribute("aList", new ArrayList<SearchDetails>());
			request.setAttribute("wrong", "true");
			request.setAttribute("emptyrequest", "true");
			return forward;
		}

		// spam protection (same as DB search)
		long wait;
		if (request.getAttribute("disableSearchSpamProtection") == null
				&& request.getAttribute("forceWords") == null && rq == null) {
			if ((wait = SpamProtection.getWaitTimeout("search", request)) != 0) {
				request.setAttribute("wrong", "true");
				request.setAttribute("crossHourlyLimit", "true");
				request.setAttribute("wait", (wait / 60 / 1000));
				return forward;
			}
			if (!SpamProtection.canPost("search", "", request)) {
				request.setAttribute("wrong", "true");
				request.setAttribute("crossTimeout", "true");
				return forward;
			}
		}

		request.removeAttribute("forceWords");

		try {
			SemanticSearchService semanticSearchService = Tools.getSpringBean("semanticSearchService", SemanticSearchService.class);
			if (semanticSearchService == null || semanticSearchService.isAvailable() == false) {
				Logger.error(SearchAction.class, "Semantic search service not available");
				request.setAttribute("aList", new ArrayList<SearchDetails>());
				request.setAttribute("wrong", "true");
				request.setAttribute("notfound", "true");
				return forward;
			}

			Integer domainId = CloudToolsForCore.getDomainId();
			String language = PageLng.getUserLng(request);

			// Fetch max 3 pages of results from semantic search, we will do pagination in Java after filtering non-searchable documents
			int maxFetch = perPage * 3;
			List<SemanticSearchResult> semanticResults = semanticSearchService.search(words, domainId, language, maxFetch, RagEntityType.DOCUMENT, request);

			// Apply pagination - skip results before index
			int totalResults = semanticResults.size();
			List<SearchDetails> aList = new ArrayList<>();
			GroupsDB groupsDB = GroupsDB.getInstance();
			String ttf = words;

			int resultIndex = 0;
			for (SemanticSearchResult sr : semanticResults) {
				if (resultIndex < index) {
					resultIndex++;
					continue;
				}
				if (aList.size() >= perPage) {
					break;
				}

				DocDetails loaded = DocDB.getInstance().getDoc(sr.getDocId().intValue());
				if (loaded == null) continue;

				// Skip non-searchable / non-available documents
				if (loaded.isSearchable() == false || loaded.isAvailable() == false) continue;

				// Skip internal groups
				GroupDetails group = groupsDB.getGroup(loaded.getGroupId());
				if (group != null && group.isInternal()) continue;

				// Skip password protected pages for anonymous users
				if (user == null && Tools.isNotEmpty(loaded.getPasswordProtected())) continue;

				SearchDetails sd = new SearchDetails();
				sd.setDocId(loaded.getDocId());
				sd.setTitle(loaded.getTitle());
				sd.setVirtualPath(loaded.getVirtualPath());
				sd.setSimilarity(sr.getSimilarity());

				if (group != null) {
					sd.setLink(groupsDB.getNavbar(group.getGroupId()));
				} else {
					sd.setLink("");
				}

				// Generate snippet
				sd.setDataOriginal(loaded.getData());
				sd.setData("");
				SearchSnippet searchSnippet = new SearchSnippet(sd, ttf, request);
				String snippet = searchSnippet.getSnippet();
				if (Tools.isEmpty(snippet)) snippet = "&nbsp;";
				sd.setData(snippet);

				aList.add(sd);
				resultIndex++;
			}

			request.setAttribute("words", words);
			request.setAttribute("aList", aList);

			if (aList.isEmpty()) {
				request.setAttribute("wrong", "true");
				request.setAttribute("notfound", "true");
				return forward;
			}

			// Build pagination params (same logic as DB search)
			String paramsLink = "";
			StringBuilder paramsLinkBuilder = new StringBuilder();
			Enumeration<String> e = request.getParameterNames();
			while (e.hasMoreElements()) {
				String name = e.nextElement();
				if ("index".equals(name) || "docid".equals(name)) continue;
				String[] values = request.getParameterValues(name);
				for (int v = 0; v < values.length; v++) {
					paramsLinkBuilder.append("&amp;").append(Tools.URLEncode(name)).append('=').append(Tools.URLEncode(values[v]));
				}
			}
			paramsLink = paramsLinkBuilder.toString();
			request.setAttribute("paramsLink", paramsLink);

			request.setAttribute("fromIndex", Integer.toString(index + 1));
			int toIndex = index + aList.size();
			request.setAttribute("toIndex", Integer.toString(toIndex));

			Logger.debug(SearchAction.class, "semantic totalResults=" + totalResults);
			request.setAttribute("totalResults", Integer.toString(totalResults));

			SearchAction.preparePages(request, perPage, index, totalResults, paramsLink, "search.do");

			if (totalResults > toIndex) {
				request.setAttribute("nextHref", "search.do?index=" + (index + perPage) + paramsLink);
			}
			if (index != 0) {
				request.setAttribute("prevHref", "search.do?index=" + (index - perPage) + paramsLink);
			}

			String searchTerm = request.getParameter("words");
			if (Tools.isEmpty(searchTerm)) searchTerm = request.getParameter("text");
			if (Tools.isNotEmpty(searchTerm)) {
				StatDB.addStatSearchLocal(searchTerm, Tools.getIntValue(request.getParameter("docid"), -1), request);
			}

			return forward;
		} catch (Exception e) {
			Logger.error(SearchAction.class, "Error in semantic search: " + e.getMessage(), e);
			request.setAttribute("wrong", "true");
			request.setAttribute("notfound", "true");
			request.setAttribute("err_msg", "Chyba pri sémantickom vyhľadávaní...");
			return error;
		}
	}

}