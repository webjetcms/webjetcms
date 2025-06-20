
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop,java.util.Set,java.util.HashSet,sk.iway.iwcm.doc.GroupDetails,sk.iway.iwcm.doc.GroupsDB"%><%@page
	import="sk.iway.iwcm.doc.DocDB"%><%@
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><iwcm:checkLogon admin="true" perms="cmp_app-slit_slider"/>

<%
	Identity user = UsersDB.getCurrentUser(request);
	Prop prop = Prop.getInstance(request);
	if (user == null || user.isAdmin() == false)
	{
		out.print(prop.getText("error.userNotLogged"));
		return;
	}
	int docId = Tools.getIntValue(request.getParameter("docid"), -2);
	if (docId == 0)
		docId = -1;
	int groupId = Tools.getIntValue(request.getParameter("groupid"), -2);
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
			for (int index = 1; index < 99; index++)
			{
				String testName = newGroupName + " " + index;
				if (usedNames.contains(testName) == false)
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
	System.out.println("docId=" + docId + " groupId=" + groupId);
	if (docId > 0 || (docId == -1 && groupId > 0))
	{
		EditorForm ef = EditorDB.getEditorForm(request, docId, -1, groupId);
		System.out.println("mam rf, docid=" + ef.getDocId() + " domain=" + ef.getDomainName() + " editable="
					+ EditorDB.isPageEditable(user, ef));
		if (ef != null && ef.getDocId() == docId && ef.getDomainName().equals(sk.iway.iwcm.common.CloudToolsForCore.getDomainName())
					&& EditorDB.isPageEditable(user, ef) && sk.iway.iwcm.common.CloudToolsForCore.isPossibleToChangeDoc(ef.getDocId()) == null)
		{
			if ("1".equals(request.getParameter("toDelete")))
			{
				DocDB.deleteDoc(ef.getDocId(), request);
				out.print("OK, reload");
				return;
			}
			else
			{
				ef.setAuthorId(user.getUserId());
				ef.setTitle(DB.prepareString(request.getParameter("title"), 255));
				ef.setNavbar(ef.getTitle());
				ef.setHtmlData(DB.prepareString(request.getParameter("subtitle"), 2000));
				ef.setPerexImage(DB.prepareString(request.getParameter("perexImage"), 255));
				ef.setExternalLink(DB.prepareString(request.getParameter("externalLink"), 255));
				ef.setPublish("1");
				ef.setFieldA(DB.prepareString(request.getParameter("colors"), 255));
				ef.setFieldB(DB.prepareString(request.getParameter("fontColor"), 255));
				ef.setFieldC(DB.prepareString(request.getParameter("headingColor"), 255));


				int historyId = EditorDB.saveEditorForm(ef, request);
				EditorDB.cleanSessionData(request);
				if (historyId > 0)
				{
					out.print("OK");
					if (createdGroupId > 0)
						out.print(", groupid=" + createdGroupId);
					else if (docId < 1)
						out.print(", reload");
					return;
				}
			}
		}
	}
	out.print(prop.getText("dmail.subscribe.db_error"));
%>