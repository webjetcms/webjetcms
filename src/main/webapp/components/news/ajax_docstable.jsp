<%@page import="org.json.JSONObject"%><%@page import="org.json.JSONArray"%><%@page import="java.util.*"%><%@page import="sk.iway.iwcm.doc.*"%><%@page import="sk.iway.iwcm.users.UsersDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"
%>
<%@ page import="sk.iway.iwcm.common.DocTools" %><%

	//System.out.println("preview.editorDomainName="+session.getAttribute("preview.editorDomainName"));

Identity user = UsersDB.getCurrentUser(request);
if (user == null) return;

String groupIds = Tools.getStringValue(Tools.getRequestParameter(request, "id"), "");

// expanding subgroups
boolean alsoSubgroups = false;
if(groupIds.contains("expand")){
    alsoSubgroups = true;
	groupIds = groupIds.replace("expand", "");
}

JSONObject json = new JSONObject();
JSONArray jsonArray = new JSONArray();

//remove non-numbers
groupIds = DocDB.getOnlyNumbersIn(groupIds, true);
String[] groupIdsArray = groupIds.split(",");

if(alsoSubgroups) {
// get subgroups of given groups - start
	StringBuilder searchGroups = null;

	for (String groupId : groupIdsArray) {
		if (searchGroups == null) {
			searchGroups = new StringBuilder(DocDB.getSubgroups(Integer.parseInt(groupId)));
		} else {
			searchGroups.append(',').append(DocDB.getSubgroups(Integer.parseInt(groupId)));
		}
	}
	// split new groups (subgroups included)
	if (Tools.isNotEmpty(searchGroups)) {
		groupIdsArray = searchGroups.toString().split(",");
	}
// end - get subgroups of given groups
}

for(String groupId: groupIdsArray) {
	    int id = Integer.parseInt(groupId);
		if (id > 0 && GroupsDB.isGroupEditable(user, id)) {

			GroupDetails group = GroupsDB.getInstance().getGroup(id);

			String parents = GroupsDB.getInstance().getParents(id);
			json.put("parents", parents);
			json.put("domainName", DocDB.getDomain(request));

			List<DocDetails> childDocs = DocTools.getEditableDocs(id, user, -1);
			for (DocDetails d : childDocs) {
				jsonArray.put(DocDB.getJsonObject(d, group, request));
			}
		}
}

json.put("data", jsonArray);

out.println(json);

	//System.out.println("preview.editorDomainName="+session.getAttribute("preview.editorDomainName"));
%>
