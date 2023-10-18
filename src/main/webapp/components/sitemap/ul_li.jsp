<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*,sk.iway.iwcm.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
PageParams pageParams = new PageParams(request);
int groupId = pageParams.getIntValue("groupId", 1);
int maxDepth = pageParams.getIntValue("maxDepth", 5);
int colsNum = pageParams.getIntValue("colsNum", 1);

	int currentDepth = 1;
	int rowsNum = 0;
	List<GroupDetails> mainGroups;
	String outStr = "";
	GroupsDB groupsDB = GroupsDB.getInstance();
	List<GroupDetails> treeList = groupsDB.getGroupsAll();

	if (maxDepth > 0 && colsNum > 0)
	{
		mainGroups = SitemapULAction.getMainGroups(groupId, treeList, request);
		rowsNum = mainGroups.size()/colsNum;
		if (mainGroups.size()%colsNum > 0)
			rowsNum ++;

		DocDB docDB = DocDB.getInstance();
		String sitemapId = null;
		if (request.getAttribute("lastSitemapId") == null)
			sitemapId = "sitemap";
		else
			sitemapId = request.getAttribute("lastSitemapId").toString()+"_1";
		request.setAttribute("lastSitemapId",sitemapId);

		out.println("<table border='0' id='"+sitemapId+"' class='sitemap' cellspacing='0' cellpadding='0' >\n");
		Iterator<GroupDetails> iter = mainGroups.iterator();
		GroupDetails group;
		DocDetails dd = null;
		boolean isExternalLink = false;
		for (int iR=0; iR<rowsNum; iR++)
		{
			out.println("<tr>\n");
			for (int iC=0; iC<colsNum; iC++)
	      {
				out.println("<td align='left' valign='top'>\n");
            if (iter.hasNext())
            {
            	group = iter.next();
            	dd = docDB.getBasicDocDetails(group.getDefaultDocId(), false);
            	isExternalLink = (dd != null && Tools.isNotEmpty(dd.getExternalLink()) && dd.getExternalLink().trim().toLowerCase().startsWith("http"));
            	out.println("<ul>\n");
            	String navbarParam[] = SitemapULAction.fixAparam(group.getNavbarName());
            	out.println("<li"+(isExternalLink ? " class=\"externalLink\"" : "")+"><a href='"+docDB.getDocLink(group.getDefaultDocId())+"'"+navbarParam[1]+(isExternalLink ? " target=\"_blank\"" : "")+">" + navbarParam[0]+"</a>\n");
            	out.println(SitemapULAction.doTreeRecursive(group.getGroupId(), treeList, maxDepth, currentDepth, request));
            	out.println("</li>\n");
         		out.println("</ul>\n");
            }
            else
            {
            	out.println("&nbsp;\n");
            }
				out.println("</td>\n");

	      }
			out.println("</tr>\n");
		}
		out.println("</table>\n");
	}
%>