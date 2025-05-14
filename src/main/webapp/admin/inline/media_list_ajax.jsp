<%@page import="java.util.List"%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.spirit.*,sk.iway.spirit.model.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>


<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null || user.isAdmin()==false)
{
	out.print(prop.getText("error.userNotLogged"));
	return;
}

int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docId"),-1);

if(docId>0){
List<Media> mediaList = MediaDB.getMedia(session, "documents",docId, "eshop");

%>
<ul class="basket-media-images">
<%
for(Media media:mediaList){
%>
	<li onclick="javascript:deleteMedia(<%=media.getId() %>)" class="delete-media"><span class="glyphicon remove-icon glyphicon-remove" aria-hidden="true"></span><img src="/thumb<%=media.getMediaThumbLink() %>?w=55&h=55&ip=5"></li>
<%
}
%>

<li class="add-media-image" onclick="javascript:openImageDialogWindow('editorForm', 'newMedia', null)"><span class="glyphicon add-icon glyphicon-plus" aria-hidden="true"></span></li>
</ul>
<input type="text" val="" style="display:none" onchange="saveNewMedium(this)" name="newMedia" id="newMedia">

<% } %>
