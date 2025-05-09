<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

//ak spravite kopiu tejto stranky, toto musite vymazat, inak sa zacyklite
String customPage = WriteTag.getCustomPageNull("/components/forum/saveok.jsp", request);
if (customPage!=null)
{
	pageContext.include(customPage);
	return;
}

request.setAttribute("cmpName", "forum");
request.setAttribute("titleKey", "forum.new.title");
request.setAttribute("descKey", "components.forum.new.insert_new_post");

Tools.insertJQuery(request);

boolean isAdmin = false;
String referer = request.getHeader("referer");
if (referer != null && referer.indexOf("new_admin.jsp")!=-1) isAdmin = true;

if (isAdmin)
{
	%>
	<script type="text/javascript">
		function doOK()
		{
			window.close();
		}
	</script>

	<jsp:include page="/components/top-public.jsp"/>
	<%
}
%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>

<iwcm:present name="permissionDenied">

	<strong>
	   <span class="forumVulgar">
			<logic:equal name="permissionDenied" value="true"><iwcm:text key="components.forum.new.upload_not_allowed"/></logic:equal>
			<logic:equal name="permissionDenied" value="fileSize"><iwcm:text key="components.forum.new.upload_not_allowed_filesize"/></logic:equal>
			<logic:equal name="permissionDenied" value="fileType"><iwcm:text key="components.forum.new.upload_not_allowed_filetype"/></logic:equal>
			<logic:equal name="permissionDenied" value="sameText"><iwcm:text key="components.forum.new.sameText"/></logic:equal>
			<logic:equal name="permissionDenied" value="postLimit"><iwcm:text key="spam_protection.post_forbidden"
				param1='<%=Constants.getInt(SpamProtection.HOURLY_LIMIT_KEY)+""%>'
				param2='<%=Constants.getInt(SpamProtection.TIMEOUT_KEY)+""%>' />
			</logic:equal>
	   </span>
   </strong>
</iwcm:present>

<iwcm:notPresent name="permissionDenied">
	<iwcm:notPresent name="isVulgar">
		<iwcm:notPresent name="errorKey">

		   <iwcm:text key="forum.new.saveok"/>
		   <script type="text/javascript">
		  	/*
		      if (opener!=null) opener.location.href=opener.location.href;
		      window.close()
		   */
		   <%
		   String type = request.getParameter("type");
		   int docId = Tools.getIntValue(request.getParameter("docid"),-1);
		   if(Tools.isNotEmpty(type))
		   {
		   	int parentId = Tools.getIntValue(request.getParameter("parent2"), Tools.getIntValue(request.getParameter("parentId"), -1));
		   	int pageNum = Tools.getIntValue(request.getParameter("pageNum"), 1);
		   	String pageParams = org.apache.struts.util.ResponseUtils.filter(Tools.getRequestParameterUnsafe(request, "pageParams"));

		   	String component = "forum-open.jsp?docid="+docId+"&pageParams="+pageParams;
		   	if("normal".equals(type))
		   		component = "forum.jsp?docid="+docId+"&page="+pageNum;
		   	else if("mb".equals(type))
		   		component = "forum_mb.jsp?docid="+docId+"&rootForumId="+request.getParameter("rootForumId");
		   	else if("mb_open".equals(type))
		   		component = "forum_mb_open.jsp?docid="+docId+"&rootForumId="+request.getParameter("rootForumId")+"&pId="+parentId+"&pageNum="+pageNum+"&pageParams="+pageParams;
		   %>
		    function reloadForumContent()
		    {
		  	 	$('#forumContentDiv').load('/components/forum/<%=component%>');
		    }
		    //jeeff: 4000 je tu preto, aby sa stihol zavret dialog, lebo po refreshe to robi problem
		    window.setTimeout(reloadForumContent, 4000);
		   <%
		   }
		   %>
		   </script>
		</iwcm:notPresent>
	   <iwcm:present name="errorKey">

		   <iwcm:text key='<%=(String)request.getAttribute("errorKey")%>'/>
	   </iwcm:present>
	</iwcm:notPresent>

	<iwcm:present name="isVulgar">
	   <br><br>
	   <span class="forumVulgar">
	   <iwcm:text key="forum.new.invalid_question"/>
	   </span>

	</iwcm:present>
</iwcm:notPresent>

<%
if (isAdmin)
{
	%>
	<jsp:include page="/components/bottom-public.jsp"/>
	<script type="text/javascript">
		try
		{
			if (window.opener.loadForumTableAjax) window.opener.loadForumTableAjax();
			else window.opener.location.reload();
		}
		catch (ex)
		{
		}
	</script>
	<%
}
%>
