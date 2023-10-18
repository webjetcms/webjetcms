<%@page import="java.util.Set"%><%@page import="java.util.HashSet"%><%@page import="sk.iway.iwcm.doc.GroupDetails"%><%@page import="sk.iway.iwcm.doc.GroupsDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop" %><%@page import="sk.iway.iwcm.doc.DocDB"%><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_news"/><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null || user.isAdmin()==false)
{
	out.print(prop.getText("error.userNotLogged"));
	return;
}

int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -2);
if (docId == 0) docId = -1;

int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupid"), -2);
int createdGroupId = -1;
if (groupId < 1)
{
	GroupsDB groupsDB = GroupsDB.getInstance();
	//vytvor adresar v system foldri
	GroupDetails localSystemGroup = groupsDB.getLocalSystemGroup();
	if (localSystemGroup != null)
	{
		Set<String> usedNames = new HashSet<String>();
		for (GroupDetails sub : groupsDB.getGroups(localSystemGroup.getGroupId()))
		{
			usedNames.add(sub.getGroupName());
		}

		//najdi vhodne meno pre grupu co sa este nepouziva
		String newGroupName = "News Box";
		for (int index=1; index<99; index++)
		{
			String testName = newGroupName+" "+index;
			if (usedNames.contains(testName)==false)
			{
				newGroupName = testName;
				break;
			}
		}

		//vytvor adresar pre novinky
		GroupDetails boxGroup = new GroupDetails(localSystemGroup);
		boxGroup.setGroupId(-1);
		boxGroup.setDefaultDocId(-1);
		boxGroup.setParentGroupId(localSystemGroup.getGroupId());
		boxGroup.setGroupName(newGroupName);
		boxGroup.setNavbar(boxGroup.getGroupName());
		boxGroup.setInternal(true);
		boxGroup.setMenuType(GroupDetails.MENU_TYPE_HIDDEN);
		boxGroup.setLoggedMenuType(GroupDetails.MENU_TYPE_HIDDEN);

		groupsDB.save(boxGroup);

		groupId = boxGroup.getGroupId();
		createdGroupId = groupId;
	}
}

if ("blog".equals(Tools.getRequestParameter(request, "type")) && groupId>0)
{
	GroupsDB groupsDB = GroupsDB.getInstance();
	GroupDetails blogGroup = groupsDB.getGroup(groupId);
	if (blogGroup != null)
	{
		blogGroup.setMenuType(GroupDetails.MENU_TYPE_ONLYDEFAULT);
		groupsDB.save(blogGroup);
	}
}


System.out.println("docId="+docId+" groupId="+groupId);
if (docId > 0 || (docId == -1 && groupId > 0) )
{
	EditorForm ef = EditorDB.getEditorForm(request, docId, -1, groupId);
	System.out.println("mam rf, docid="+ef.getDocId()+" domain="+ef.getDomainName()+" editable="+EditorDB.isPageEditable(user, ef));
	if (ef != null && ef.getDocId()==docId && EditorDB.isPageEditable(user, ef))
	{
		if ("1".equals(Tools.getRequestParameter(request, "toDelete")))
		{
			DocDB.deleteDoc(ef.getDocId(), request);

			out.print("OK, reload");
			return;
		}
		else
		{
			ef.setAuthorId(user.getUserId());
			ef.setTitle(DB.prepareString(Tools.getRequestParameter(request, "title"), 255));
			ef.setNavbar(ef.getTitle());
			ef.setHtmlData(DB.prepareString(Tools.getRequestParameter(request, "subtitle"), 2000));
			ef.setPerexImage(DB.prepareString(Tools.getRequestParameter(request, "perexImage"), 255));
			ef.setExternalLink(DB.prepareString(Tools.getRequestParameter(request, "externalLink"), 255));

			if (Tools.getRequestParameter(request, "publishStart") != null && Tools.getRequestParameter(request, "publishStartTime") != null) {
				ef.setPublishStart(DB.prepareString(Tools.getRequestParameter(request, "publishStart"), 10));
				ef.setPublishStartTime(DB.prepareString(Tools.getRequestParameter(request, "publishStartTime"), 6));
			}

			ef.setPublish("1");

			int historyId = EditorDB.saveEditorForm(ef, request);
			EditorDB.cleanSessionData(request);
			if (historyId > 0)
			{
				out.print("OK");
				if (createdGroupId > 0) out.print(", groupid="+createdGroupId);
				else if (docId < 1) out.print(", reload");
				return;
			}
		}
	}
}

out.print(prop.getText("dmail.subscribe.db_error"));
%>
