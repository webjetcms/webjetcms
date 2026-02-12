<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants,sk.iway.iwcm.Identity"%>
<%@ page import="sk.iway.iwcm.InitServlet" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupDetails" %>
<%@ page import="sk.iway.iwcm.doc.TemplateDetails" %>
<%@ page import="sk.iway.iwcm.doc.TemplatesDB" %>
<%@ page import="sk.iway.iwcm.doc.groups.GroupsController" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/>
<script type="text/javascript">
	function documentReady(f){
	    if(document.readyState !== "complete"){
	        setTimeout('documentReady('+f+')', 9);
		} else{
	        f();
		}
	}

function closeWebJETToolbar()
{
   if (document.getElementById)
   {
	   var el = document.getElementById("webjetToolbar");
	   if (el!=null)
	   {
	      el.style.display="none";
	   }
   }
}

oldDocumentOnReady = document.onready;
var scriptsMovedToHead = false;

document.onready = function(){
	if (scriptsMovedToHead)
		return;
	if (oldDocumentOnReady)
		oldDocumentOnReady();

	var body = document.getElementsByTagName('BODY')[0];
	var head = document.getElementsByTagName('HEAD')[0];

	var link = document.createElement("link");
	// var script = document.createElement("script");

	link.href = "<%=request.getContextPath()%>/admin/skins/webjet8/css/page_toolbar.css";
	link.type = "text/css";
	link.rel = "stylesheet";

	//script.src = "<%=request.getContextPath()%>/admin/scripts/blackbird.js";
	//script.type = "text/javascript";

	//head.insertBefore(script, head.firstChild);

	head.appendChild(link);
	body.appendChild(document.getElementById("webjetToolbar"));
	scriptsMovedToHead = true;
};

documentReady(function(){
    document.onready();
});

//document.cookie = 'blackbird={pos:0,size:0,load:true};';
//-->
</script>

<%
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null || !user.isAdmin())
{
   return;
}
DocDetails doc = (DocDetails)request.getAttribute("docDetails");
if (doc == null)
{
   return;
}
pageContext.setAttribute("doc", doc);
%>
<div class="webjetToolbar noprint" id="webjetToolbar">
	<div class="webjetToolbarContent">
		<table class="webjetToolbarTable">
		   <tr>
			   <td class="header">
				   <img src="/admin/v9/dist/images/logo-<%=InitServlet.getBrandSuffix()%>.svg" />
			   		<a href="javascript:closeWebJETToolbar()" class="webjetToolbarClose">&nbsp;</a>
			   </td>
			</tr>
			<tr>
			   <td><strong>DocID:</strong>
			      <a href='/admin/v9/webpages/web-pages-list/?docid=<iwcm:beanWrite name="doc" property="docId"/>' target="_blank"><iwcm:beanWrite name="doc" property="docId"/></a>
				</td>
			</tr>
			<%if(!doc.isAvailable() || "false".equals((String)request.getAttribute("is_available"))){%>
			<tr>
				<td colspan="2" class="warning">
			      <img src="/admin/images/warning.gif" align="absmiddle" alt="" /> <strong><iwcm:text key="admin.page_toolbar.pozor_stranka_sa_verejne_nezobrazuje"/></strong>
				</td>
			</tr>
			<%}
			GroupDetails group = (GroupDetails)request.getAttribute("pageGroupDetails");
			if (group != null)
			{%>
			<tr>
				<td><strong><iwcm:text key="admin.temp_group_list.directory"/>:</strong>
					<a href='<%=GroupsController.BASE_URL%><%=group.getGroupId()%>' target="_blank"><%=group.getGroupName()%></a></td>
			</tr>
			<%
			}

			TemplatesDB tempDB = TemplatesDB.getInstance();
			TemplateDetails temp = tempDB.getTemplate(doc.getTempId());
			if(temp != null){%>
			<tr>
				<td><strong><iwcm:text key="editor.template"/>:</strong>
					<a href='/admin/v9/templates/temps-list/?tempId=<%=temp.getTempId()%>' target="_blank"><%=temp.getTempName()%></a></td>
			</tr>
			<%}%>

			<tr>
				<td><strong><iwcm:text key="history.changedBy"/>:</strong>
					<a href="mailto:<iwcm:beanWrite name="doc" property="authorEmail"/>"><iwcm:beanWrite name="doc" property="authorName"/></a></td>
			</tr>
			<tr>
				<td><strong><iwcm:text key="editor.date"/>:</strong>
			      <iwcm:beanWrite name="doc" property="dateCreatedString"/>
					<iwcm:beanWrite name="doc" property="timeCreatedString"/>
			   </td>
			</tr>
		</table>
	</div>
	<div class="webjetToolbarFooter">&nbsp;</div>
</div>
