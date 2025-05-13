<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
PageParams pageParams = new PageParams(request);
String groupId = pageParams.getValue("groupId", "1");
String rootNodeName = pageParams.getValue("rootNodeName", "WEB");

request.setAttribute("groupid", groupId);
SitemapAction.doTree(request);
DocDB docDB = DocDB.getInstance();
String link = "";
if(request.getAttribute("doc_id") != null)
{
	int id = Integer.valueOf(request.getAttribute("doc_id").toString()).intValue();
	if (docDB.getDocLink( id, request).indexOf("?") != -1)
	{
		link = docDB.getDocLink( id, request) +"&rootId=";
	}
	else
	{
		link = docDB.getDocLink( id, request) +"?rootId=";
	}
}
%>
<!-- zaciatok stromu stranok -->
<table border="0">
	<tr>
		<td valign="top">
			<script type="text/javascript" src="/components/sitemap/jscripts/ua.js"></script>
			<script type="text/javascript" src="/components/sitemap/jscripts/tree.js"></script>
			<script type="text/javascript">
				<!--
				rootNodeName="<b><%=rootNodeName%></b>";
				var Tree = new Array;
				<iwcm:iterate id="group" name="groups" type="sk.iway.iwcm.doc.GroupDetails" indexId="index">
					Tree[<iwcm:beanWrite name="index"/>] = "<jsp:getProperty name="group" property="groupId"/>|<jsp:getProperty name="group" property="parentGroupId"/>|<jsp:getProperty name="group" property="groupName"/>|<%=link%><jsp:getProperty name="group" property="groupId"/>";
				</iwcm:iterate>
				createTree(Tree, 0, <iway:request name="group_id"/>);
				-->
			</script>
		</td>
		<td valign="top">
			<img src="/components/sitemap/images/empty.gif" alt="Empty" width="20" height="10" />
		</td>
		<td valign="top">
			<iwcm:text key="components.sitemap.webPagesInDir"/>:
			<br /><br />
			<iwcm:iterate id="doc" name="docs" type="sk.iway.iwcm.doc.DocDetails">
				<a href="/showdoc.do?docid=<jsp:getProperty name="doc" property="docId"/>"><img src="/components/sitemap/images/page.gif" alt="Page" /><jsp:getProperty name="doc" property="title"/></a><br />
			</iwcm:iterate>
		</td>
	</tr>
</table>
<!-- koniec stromu stranok -->
