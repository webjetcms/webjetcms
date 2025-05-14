<%@page import="sk.iway.iwcm.doc.DocDB"%><%@
page import="sk.iway.iwcm.doc.TemplateDetails"%><%@
page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%><%@
page import="sk.iway.iwcm.doc.GroupDetails"%><%@
page import="sk.iway.iwcm.doc.GroupsDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><iwcm:checkLogon admin="true" perms="cmp_basket"/><%

/*
Zmaze dokument alebo grupu, robi kontrolu podla domeny.
*/

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null || user.isAdmin()==false)
{
	out.print(prop.getText("error.userNotLogged"));
	return;
}

int docId = Tools.getIntValue(request.getParameter("docId"), -1);
int groupId = Tools.getIntValue(request.getParameter("groupId"), -1);
Logger.debug(null, "admin_delete.jsp docid: "+docId+" groupid: "+groupId);

if(docId > 0 && groupId == -1)
{
	//ak som v rovnakej domene
	String domain_nameById = DocDB.getInstance().getDomain(docId);
	String domain_nameByRequest = DocDB.getDomain(request);

	if(domain_nameById != null &&  domain_nameById.equals(domain_nameByRequest))
	{
		out.print( DocDB.deleteDoc(docId, request)+" reload");
		return;
	}
}

if(groupId > 0 && docId == -1)
{
	//ak som v rovnakej domene
	String domain_name = sk.iway.iwcm.common.CloudToolsForCore.getDomainName();
	String domain_nameById = GroupsDB.getInstance().getDomain(groupId);

	if(domain_name != null &&  domain_name.equals(domain_nameById))
	{
		out.print(GroupsDB.deleteGroup(groupId, request)+" reload");
		return;
	}
}


out.print(prop.getText("dmail.subscribe.db_error"));
%>