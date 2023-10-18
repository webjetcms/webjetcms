<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="java.io.File"%>
<%@page import="sk.iway.iwcm.components.blog.BloggerMaker"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.common.BlogTools" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

Identity user = UsersDB.getCurrentUser(request);
GroupDetails actualGroup = (GroupDetails)request.getAttribute("pageGroupDetails");
boolean canEdit = false;

if (user != null && actualGroup != null) canEdit = BlogTools.isEditable(actualGroup.getGroupId(), user);

if (canEdit==false) return;

if ("1".equals(request.getParameter("blogEdit")))
{
	pageContext.include("blog_editor_inline.jsp");
}
else
{
	%>
	<link rel='stylesheet' media='screen' type='text/css' href='/components/blog/blog.css'/>
	<div class="blogEditorToolbar">
		<table cellspacing="0" cellpadding="0">
			<tr>
				<td class="toolbarTitle"><iwcm:text key="components.blog.toolbarTitle"/></td>
				<td class="wjTlacitka" style="width: auto; margin: 0px;">					
					<% if (user.isEnabledItem("addPage") && request.getParameter("blogEdit")==null){ %>
							<a href="<%=Tools.addParametersToUrlNoAmp(PathFilter.getOrigPath(request), "blogEdit=1&new=1") %>" title="<iwcm:text key="components.blog.newblog"/>" class="TB_Button_Off"><img src="/admin/FCKeditor/editor/skins/webjet/toolbar/newpage.gif" class="TB_Button_Image"></a>
					<%} %>	
				</td>				
			</tr>
		</table>
	</div>
	<%
}
%>
