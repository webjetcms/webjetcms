package sk.iway.iwcm.search;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * interface pre implementaciu vlastnych hladani vo WJ (webstranky, textove kluce, moduly, ...)
 * po pridani triedy s implementaciou do konstanty "searchClasses" sa prida moznost noveho hladania v komponente "admin/skins/webjet6/searchall.jsp"
 */
public interface Searchable
{
	List<SearchResult> search(String text, HttpServletRequest request);

	/**
	 * Umoznuje definovat prava na pouzitie vyhladavaca
	 * @param request
	 * @return
	 */
	boolean canUse(HttpServletRequest request);
}
