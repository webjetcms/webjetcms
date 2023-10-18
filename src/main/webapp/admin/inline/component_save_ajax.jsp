<%@page import="sk.iway.iwcm.system.context.ContextFilter"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.editor.EditorDB"%>
<%@page import="sk.iway.iwcm.editor.EditorForm"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

Identity user = UsersDB.getCurrentUser(request);
if (user == null || user.isAdmin()==false)
{
	out.println("NOT LOGGED");
	return;
}

int docId = Tools.getDocId(request);
String oldComponent = Tools.getRequestParameterUnsafe(request, "oldText");
String newComponent = Tools.getRequestParameterUnsafe(request, "newText");

//System.out.println("docid="+docId+" old="+oldComponent+" new="+newComponent);

EditorForm ef = EditorDB.getEditorForm(request, docId, -1, -1);

if (ef == null || ef.getDocId()!=docId || Tools.isEmpty(oldComponent) || Tools.isEmpty(newComponent) || docId < 1)
{
	out.println("NOT EXISTS");
	return;
}

boolean canAccess = EditorDB.isPageEditable(user, ef);
if (canAccess == false)
{
	out.println("PERMS DENIED");
	return;
}

oldComponent += ")!";

String data = ef.getData();

if (ContextFilter.isRunning(request))
{
	oldComponent = ContextFilter.removeContextPath(request.getContextPath(), oldComponent);
	newComponent = ContextFilter.removeContextPath(request.getContextPath(), newComponent);
	data = ContextFilter.removeContextPath(request.getContextPath(), data);
}

System.out.println("COMPONENT SAVE AJAX, docId="+docId+" old="+oldComponent+" new="+newComponent);
System.out.println("OLD DATA:"+data);

if ("DELETE".equals(newComponent)) newComponent = "";

data = Tools.replace(data, oldComponent, newComponent);

//fix pre Google Chrome, ktory zle prenasa &nbsp na spojkach (identifikovane v hlavicke s textom Vitajte na&nbsp;mojej stranke)
String oldComponentNbsp = Tools.replace(oldComponent, Constants.NON_BREAKING_SPACE, "&nbsp;");
System.out.println("OLD NBSP:"+oldComponentNbsp);
data = Tools.replace(data, oldComponentNbsp, newComponent);

System.out.println("NEW DATA:"+data);

ef.setData(data);
ef.setAuthorId(user.getUserId());
ef.setPublish("1");

EditorDB.cleanSessionData(request);

int historyId = EditorDB.saveEditorForm(ef, request);
if (historyId < 1)
{
	out.println("ERROR SAVING PAGE");
	EditorDB.cleanSessionData(request);
	return;
}
else
{
	if (session.getAttribute("pageSavedToPublic")!=null)
	{
		out.println("SAVEOK");
		EditorDB.cleanSessionData(request);
		return;
	}
}

//vyhodenie veci co nechceme zobrazovat
session.removeAttribute("cssError");
session.removeAttribute("docHistory");
session.removeAttribute("updatedDocs");
session.removeAttribute("editorUsers");

%>
<logic:present name="approveByUsers">
	<iwcm:text key="editor.approveRequestGet"/>:
	<iway:request name="approveByUsers" />
</logic:present>

<logic:notPresent name="approveByUsers">
	<logic:present name="pageSavedToPublic">
		<% if (ef.isAvailable()==false) { %>
			<iwcm:text key="admin.page_toolbar.pozor_stranka_sa_verejne_nezobrazuje"/>
		<% } else { %>
			<iwcm:text key="editor.pageSavedToPublic"/>
		<% } %>
	</logic:present>

	<logic:present name="pageSaved">
		<iwcm:text key="editor.pageSaved"/>
	</logic:present>

	<logic:present name="pagePublishDate">
				<iwcm:text key="editor.publish.pagesaved" />
				<iway:request name="pagePublishDate"/>
	</logic:present>
</logic:notPresent>
<%
EditorDB.cleanSessionData(request);
%>
