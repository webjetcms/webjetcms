package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  Zobrazi dokumenty v stromovej strukture
 *	  - pomocou tagov <ul><li>
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.13 $
 *@created      $Date: 2009/05/04 09:07:32 $
 *@modified     $Date: 2009/05/04 09:07:32 $
 */

public class SitemapULAction
{
	private SitemapULAction() {

	}

	public static String doTree(int rootGroup, int maxDepth, int colsNum)
	{
		return doTree(rootGroup, maxDepth, colsNum, null);
	}

	public static String doTree(int rootGroup, int maxDepth, int colsNum, HttpServletRequest request) {
		int currentDepth = 0;
		int rowsNum = 0;
		List<GroupDetails> mainGroups;
		StringBuilder outStr = new StringBuilder();
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> treeList = groupsDB.getGroupsAll();
		// Logger.println(this,"size="+treeList.size()+" root="+rootGroup);
		// return(doTreeRecursive(rootGroup, treeList, maxDepth, currentDepth));

		if (maxDepth > 0 && colsNum > 0) {
			mainGroups = getMainGroups(rootGroup, treeList, request);
			rowsNum = mainGroups.size() / colsNum;
			if (mainGroups.size() % colsNum > 0)
				rowsNum++;

			DocDB docDB = DocDB.getInstance();
			outStr.append("<table border=1>\n");
			Iterator<GroupDetails> iter = mainGroups.iterator();
			GroupDetails group;
			for (int iR = 0; iR < rowsNum; iR++) {
				outStr.append("\t<tr>\n");
				for (int iC = 0; iC < colsNum; iC++) {
					outStr.append("\t\t<td align='left' valign='top'>\n");
					if (iter.hasNext()) {
						group = iter.next();

						if (group.getShowInSitemap(request) == GroupDetails.MENU_TYPE_HIDDEN) {
							continue;
						}

						outStr.append("<ul>\n");
						outStr.append("<li><a href='").append(docDB.getDocLink(group.getDefaultDocId())).append("'>")
								.append(group.getNavbarName()).append("</a>\n");
						if (group.getShowInSitemap(request) == GroupDetails.MENU_TYPE_NORMAL) {
							outStr.append("\t\t\t\t").append(
									doTreeRecursive(group.getGroupId(), treeList, maxDepth, currentDepth, request));
						}
						outStr.append("</li>\n");

						/*
						 * if (group.getShowInSitemap(request) != GroupDetails.MENU_TYPE_ONLYDEFAULT)
						 * {
						 * List docs = docDB.getDocByGroup(group.getGroupId());
						 * Iterator iter2 = docs.iterator();
						 * while (iter2.hasNext())
						 * {
						 * DocDetails doc = (DocDetails)iter2.next();
						 * if (doc.isAvailable() && doc.isShowInSitemap(request) && doc.getDocId() !=
						 * group.getDefaultDocId())
						 * {
						 * outStr +=
						 * "<li><a href='"+docDB.getDocLink(doc.getDocId())+"'>"+doc.getNavbar()+
						 * "</a></li>\n";
						 * }
						 * }
						 * }
						 */
						outStr.append("</ul>\n");

						// outStr += "\t\t\t\t" +group.getGroupId();
						// outStr += "\n";
					} else {
						outStr.append("&nbsp;\n");
					}
					outStr.append("\t\t</td>\n");

				}
				outStr.append("\t</tr>\n");
			}
			outStr.append("</table>\n");
		}
		return outStr.toString();

	}

/*	private static String doTreeRecursive(int rootGroup, List treeList, int maxDepth, int currentDepth)
	{
		String out = "";

		try
		{
			Iterator iter = treeList.iterator();
			GroupDetails group;

			out += "<ul>\n";
			DocDB docDB = DocDB.getInstance();

			while (iter.hasNext())
			{
				group = (GroupDetails)iter.next();

				if (group.isInternal() || group.getShowInSitemap(request)==GroupDetails.MENU_TYPE_HIDDEN)
				{
					continue;
				}

				if (group.getParentGroupId() == rootGroup)
				{
					Logger.println(this,"currentDepth: "+currentDepth+ "maxDepth: "+maxDepth);

					if (currentDepth < maxDepth)
					{
						out += "<li><a href='"+docDB.getDocLink(group.getDefaultDocId())+"'>" + group.getNavbarName()+"</a>\n";
						out += doTreeRecursive(group.getGroupId(), treeList, maxDepth, currentDepth+1);
						out += "</li>\n";
					}
					else
					{
						return("");
					}
				}
				//currentDepth = 0;
			}

			GroupsDB groupsDB = GroupsDB.getInstance();
			group = groupsDB.getGroup(rootGroup);
			if (group.getShowInSitemap(request) != GroupDetails.MENU_TYPE_ONLYDEFAULT)
			{

				List docs = docDB.getDocByGroup(rootGroup);
				Iterator iter2 = docs.iterator();
				group = groupsDB.getGroup(rootGroup);
				while (iter2.hasNext())
				{
				   DocDetails doc = (DocDetails)iter2.next();

				   if (doc.isAvailable() && doc.isShowInSitemap(request) && doc.getDocId() != group.getDefaultDocId())
				   {
				      out += "<li><a href='"+docDB.getDocLink(doc.getDocId())+"'>"+doc.getNavbar()+"</a></li>\n";
				   }
				}
			}
			out += "</ul>\n";

			if ("<ul>\n</ul>\n".equals(out))
			{
			   out = "";
			}
		}
		catch (Exception ex)
		{
			Logger.error(SitemapULAction.class, ex);
		}

		return out;
	}
*/
	public static List<GroupDetails> getMainGroups(int rootGroup, List<GroupDetails> treeList) {
		return getMainGroups(rootGroup, treeList, null);
	}

	public static List<GroupDetails> getMainGroups(int rootGroup, List<GroupDetails> treeList, HttpServletRequest request)
	{
		List<GroupDetails> mainGroups = new ArrayList<>();
		for (GroupDetails group : treeList)
		{
			if (group.isInternal() || group.getShowInSitemap(request)==GroupDetails.MENU_TYPE_HIDDEN)
			{
				continue;
			}
			if (group.getParentGroupId() == rootGroup)
			{
				mainGroups.add(group);
			}
		}
		return mainGroups;
	}

	public static String doTreeRecursive(int rootGroup, List<GroupDetails> treeList, int maxDepth, int currentDepth)
	{
		return doTreeRecursive(rootGroup, treeList, maxDepth, currentDepth, null);
	}

	public static String doTreeRecursive(int rootGroup, List<GroupDetails> treeList, int maxDepth, int currentDepth, HttpServletRequest request)
	{
		StringBuilder out = new StringBuilder();

		try
		{
			GroupsDB groupsDB = GroupsDB.getInstance();
			GroupDetails rootGroupDetails = groupsDB.getGroup(rootGroup);
			if (rootGroupDetails.getShowInSitemap(request)==GroupDetails.MENU_TYPE_NOSUB) return "";

			out.append("<ul>\n");
			DocDB docDB = DocDB.getInstance();
			DocDetails dd = null;
			boolean isExternalLink = false;

			for (GroupDetails group : treeList)
			{

				if (group.isInternal() || group.getShowInSitemap(request)==GroupDetails.MENU_TYPE_HIDDEN)
				{
					continue;
				}

				if (group.getParentGroupId() == rootGroup)
				{
					//Logger.println(this,"currentDepth: "+currentDepth+ "maxDepth: "+maxDepth);

					if (currentDepth < maxDepth)
					{
						String[] navbarParam = SitemapULAction.fixAparam(group.getNavbarName());
						dd = docDB.getBasicDocDetails(group.getDefaultDocId(), false);
						isExternalLink = (dd != null && Tools.isNotEmpty(dd.getExternalLink()) && dd.getExternalLink().trim().toLowerCase().startsWith("http"));
						out.append("<li").append(isExternalLink ? " class=\"externalLink\">" : ">").append("<a href='").append(docDB.getDocLink(group.getDefaultDocId())).append('\'').append(navbarParam[1]).
						append(isExternalLink ? " target=\"_blank\"" : "").
						append('>').append(navbarParam[0]).append("</a>\n");
						if (group.getShowInSitemap(request) == GroupDetails.MENU_TYPE_NORMAL || group.getShowInSitemap(request) == GroupDetails.MENU_TYPE_ONLYDEFAULT)
						{
							out.append(doTreeRecursive(group.getGroupId(), treeList, maxDepth, currentDepth+1, request));
						}
						out.append("</li>\n");
					}
					else
					{
						break;
					}
				}
				//currentDepth = 0;
			}

			GroupDetails group = rootGroupDetails;
			if (group.getShowInSitemap(request) != GroupDetails.MENU_TYPE_ONLYDEFAULT && currentDepth < maxDepth)
			{

				List<DocDetails> docs = docDB.getDocByGroup(rootGroup, DocDB.ORDER_PRIORITY, true, -1, -1, false); // docDB.getDocByGroup(rootGroup);

				group = groupsDB.getGroup(rootGroup);
				for (DocDetails doc : docs)
				{
				   if (doc.isAvailable() && doc.isShowInSitemap(request) && doc.getDocId() != group.getDefaultDocId())
				   {
				   	String[] navbarParam = SitemapULAction.fixAparam(doc.getNavbar());
				   	isExternalLink = (Tools.isNotEmpty(doc.getExternalLink()) && doc.getExternalLink().trim().toLowerCase().startsWith("http"));
				   	out.append("<li").append(isExternalLink ? " class=\"externalLink\">" : ">").append("<a href='").append(docDB.getDocLink(doc.getDocId())).append('\'').append(navbarParam[1]).
				   	append(isExternalLink ? " target=\"_blank\"" : "").
				   	append('>').append(navbarParam[0]).append("</a></li>\n");
				   }
				}
			}
			out.append("</ul>\n");

			if ("<ul>\n</ul>\n".equals(out.toString()))
			{
			   out.delete(0, out.length());
			}
		}
		catch (Exception ex)
		{
			Logger.error(SitemapULAction.class, ex);
		}

		return out.toString();
	}

	/**
	 * Z povodneho nazvu vrati nazov bez <APARAM ako [0] a hodnotu APARAM ako [1] (ak je zadana)
	 * @param navbarName
	 * @return
	 */
	public static String[] fixAparam(String navbarName)
	{
		String aParam = "";
		String navnarNameUCase = navbarName.toUpperCase();
		int aParamStart = navnarNameUCase.indexOf("<APARAM");
		if (aParamStart != -1)
		{
			try
			{
				int aParamEnd = navnarNameUCase.indexOf('>', aParamStart);
				if (aParamEnd > aParamStart)
				{
					aParam = " " + navbarName.substring(aParamStart + 7, aParamEnd);
					//odstran aParam z textu
					navbarName = Tools.replace(navbarName, navbarName.substring(aParamStart, aParamEnd + 1), "");
				}
			}
			catch (Exception ex)
			{
				//
			}
		}
		String[] ret = new String[2];
		ret[0] = navbarName;
		ret[1] = aParam;
		return ret;
	}

	/**
	 * Generovanie sitemapy podla priznaku "Vyhladavatelne" vo web strankach na ziadost TB - tiket 23535
	 * @param rootGroupId - ID root adresara
	 * @param maxDepth - maximalna hlbka do ktorej sa prehladava
	 * @param actualDepth
	 * @param request
	 * @return
	 */
	public static StringBuilder getTreeSearchable(int rootGroupId, int maxDepth, int actualDepth, HttpServletRequest request)
	{
		String abTestingName = Constants.getString("ABTestingName", "");
		StringBuilder htmlCode = new StringBuilder();

		if (actualDepth > maxDepth) return htmlCode;

		String spacer = "                                                                                                      ";
		try
		{
			spacer = spacer.substring(0, actualDepth*3);
		}
		catch (Exception e)
		{
			spacer = "";
		}

		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();
		List<GroupDetails> subGroups = groupsDB.getGroups(rootGroupId);

		for (GroupDetails group : subGroups)
		{
			if (group.isInternal()) continue;

			StringBuilder subGroupsHtmlCode = getTreeSearchable(group.getGroupId(), maxDepth, actualDepth+1, request);

			//ziskaj zoznam stranok v tomto adresari
			List<DocDetails> docs = docDB.getBasicDocDetailsByGroup(group.getGroupId(), DocDB.ORDER_PRIORITY);
			StringBuilder docsHtmlCode = new StringBuilder();

			DocDetails defaultDoc = null;

			for (DocDetails doc : docs)
			{
				String virtualPath = doc.getVirtualPath();
				if (Tools.isNotEmpty(abTestingName) && virtualPath.contains(abTestingName)) {
					continue;
				}

				if (doc.getDocId()==group.getDefaultDocId())
				{
					defaultDoc = doc;
					continue;
				}

				if (doc.isAvailable()==false) continue;
				if (doc.isSearchable()==false) continue;

				String title = doc.getTitle();

				docsHtmlCode.append(spacer).append("<li").append("><a href=\"").append(docDB.getDocLink(doc.getDocId(), request)).append('\"').append('>');
				docsHtmlCode.append(ResponseUtils.filter(title) + "</a></li>\n");
			}

			//ak je default stranka adresara oznacena ako NOT searchable nedaj cely adresar do mapy stranok
			if (defaultDoc!=null && defaultDoc.isSearchable()==false) continue;


			String title = "";
			if (defaultDoc != null) title = defaultDoc.getTitle();
			if (Tools.isNotEmpty(group.getGroupName())) title = group.getGroupName();
			htmlCode.append(spacer).append("<li").append("><a href=\"").append(docDB.getDocLink(group.getDefaultDocId(), request)).append('\"').append('>');
			htmlCode.append(ResponseUtils.filter(title));
			htmlCode.append("</a>\n");

			if (subGroupsHtmlCode.length()>0 || docsHtmlCode.length()>0)
			{

				htmlCode.append(spacer).append("<ul>\n");

				if (subGroupsHtmlCode.length() > 0) htmlCode.append(subGroupsHtmlCode);

				if (docsHtmlCode.length() > 0) htmlCode.append(docsHtmlCode);

				htmlCode.append(spacer).append("</ul>\n");
			}

			htmlCode.append(spacer).append("</li>\n");


		}

		return htmlCode;
	}
}