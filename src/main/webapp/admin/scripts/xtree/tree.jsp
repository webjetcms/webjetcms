<%@page import="java.util.List"%><%
response.setContentType("text/xml; charset=UTF-8");

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

%><%@ page pageEncoding="utf-8" import="org.json.JSONException,org.json.JSONObject,sk.iway.iwcm.Constants, sk.iway.iwcm.Identity, sk.iway.iwcm.Tools" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@page import="sk.iway.iwcm.doc.DocDB"%><%@ page import="sk.iway.iwcm.doc.GroupsDB" %><%@
        page import="sk.iway.iwcm.tags.JSEscapeTag" %><%@
        page import="sk.iway.iwcm.users.SettingsAdminBean" %><%@
        page import="sk.iway.iwcm.users.UserDetails" %><%@
        page import="sk.iway.iwcm.users.UsersDB" %><%@
        page import="java.util.ArrayList" %><%@
        page import="java.util.Iterator" %><%@
        page import="java.util.Map" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%!

    //POZOR nesmu tu byt ziadne prazdne entre pred generovanim XML hlavicky, davajte na to pozor!!!

void convertToRealBoolean(JSONObject obj) throws org.json.JSONException {
	Iterator<?> keys = obj.keys();

	while( keys.hasNext() ) {
		String key = (String)keys.next();
		try {
			String value = obj.getString(key);

			if (value.equalsIgnoreCase("yes")) {
				obj.put(key, "true");
			}
			else if (value.equalsIgnoreCase("no")) {
				obj.put(key, "false");
			}
		} catch (JSONException e) {
			//sk.iway.iwcm.Logger.error(e);
		}
	}
}
%><%
int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "parent"), -1);
GroupsDB groupsDB = GroupsDB.getInstance();
DocDB docDB = DocDB.getInstance();
List groups = groupsDB.getGroups(groupId);
request.setAttribute("groups", groups);

boolean alsoPages = false;
if ("true".equals(Tools.getRequestParameter(request, "alsoPages"))) alsoPages = true;

List pages = new ArrayList();
if (alsoPages)
{
	pages = docDB.getBasicDocDetailsByGroup(groupId, DocDB.ORDER_PRIORITY);
	request.setAttribute("pages", pages);
}

int docsCount = Tools.getIntValue(Tools.getRequestParameter(request, "docs_count"),-1);
if(docsCount > -1)
{
	session.setAttribute("docs_count",new Integer(docsCount));
}
else if(session.getAttribute("docs_count")==null)
{
	session.setAttribute("docs_count",new Integer(40));
}
else
{
	docsCount = ((Integer)session.getAttribute("docs_count")).intValue();
}
if (docsCount<1) docsCount = 40;
docsCount = Math.round((float)docsCount / 2F);
boolean pagesWasSkip = false;

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null)
	return;
//System.out.println("tree.jsp groupId="+groupId);

	UserDetails userDetails = UsersDB.getUser(user.getUserId());

	Map<String, SettingsAdminBean> userAdminSettings = userDetails.getAdminSettings();

	JSONObject viewInfoJSON = new JSONObject();

	if (userAdminSettings.get("groups_tree_view_info") != null)
	{
		 try
		 {
			 //System.out.println("hodnota: "+userAdminSettings.get("groups_tree_view_info").getValue());
			 viewInfoJSON =	 new JSONObject(userAdminSettings.get("groups_tree_view_info").getValue());
			 convertToRealBoolean(viewInfoJSON);
		 }
		 catch(Exception e)
		 {
		 	sk.iway.iwcm.Logger.error(e);
		 }

	}
	else
	{
		viewInfoJSON.put("sortPriority", false);
		viewInfoJSON.put("docId", false);
		viewInfoJSON.put("dateCreated", false);
	}

	boolean canAccess;
%><?xml version="1.0"?>

<tree>

	<%int parentGroupId; String folderIcon; boolean showChilds = false; int systemGroupId=-2; String src; List childs;%>
	<logic:iterate id="group" name="groups" type="sk.iway.iwcm.doc.GroupDetails" indexId="index">
		<%
		canAccess = true;
		if (Constants.getBoolean("adminCheckUserGroups"))
		{
			canAccess = DocDB.canAccess(group, user);
		}
		if (group.isHiddenInAdmin() && user.isDisabledItem("editor_show_hidden_folders")) canAccess = false;
		if (canAccess)
		{

			parentGroupId = group.getParentGroupId();
			folderIcon = null;
			if (group.getPasswordProtected()!=null && group.getPasswordProtected().length()>0)
			{
			   folderIcon = "lock";
			}
			else if ("system".equalsIgnoreCase(group.getGroupName()) && parentGroupId < 1)
			{
				folderIcon = "system";
				systemGroupId = group.getGroupId();
			}
			if (group.isInternal())
			{
				if ("lock".equals(folderIcon))
				{
					folderIcon = "lock-internal";
				}
				else
				{
					folderIcon = "internal";
				}
			}

			if (folderIcon != null)
			{
			   //folderIcon = ",'','','images/dtree/folder-"+folderIcon+".gif','images/dtree/folderopen-"+folderIcon+".gif'";
			   folderIcon = "-" + folderIcon;
			}
			else
			{
			   folderIcon = "";
			}

			if (alsoPages) src = "src=\"/admin/scripts/xtree/tree.jsp?alsoPages=true&amp;parent="+group.getGroupId()+"\"";
			else src = "src=\"/admin/scripts/xtree/tree.jsp?parent="+group.getGroupId()+"\"";
			childs = groupsDB.getGroups(group.getGroupId());
			List childPages = null;
			if (alsoPages) childPages = docDB.getBasicDocDetailsByGroup(group.getGroupId(), DocDB.ORDER_ID);
			if ((childs == null || childs.size()==0) && (childPages==null || childPages.size()==0)) src = "";

			String groupIdName = JSEscapeTag.jsEscape(group.getGroupIdName());
			groupIdName = Tools.replace(groupIdName, "<", "&lt;");
			groupIdName = Tools.replace(groupIdName, ">", "&gt;");

			String prefix = viewInfoJSON != null && viewInfoJSON.getBoolean("sortPriority") ? group.getSortPriority() + ". " : "";
			prefix += viewInfoJSON != null && viewInfoJSON.getBoolean("docId") ? "[#" + group.getGroupId() + "] " : "";
		%>
		<tree text="&lt;em&gt;<%= prefix %>&lt;/em&gt;<bean:write name="group" property="groupNameShortNoJS"/>" action="javascript:xtreeItemClick(<bean:write name="group" property="groupId"/>, '<%=groupIdName %>')" icon="/admin/images/dtree/folder<%=folderIcon%>.gif" openIcon="/admin/images/dtree/folderopen<%=folderIcon%>.gif" <%=src%> /><%
		}%>
	</logic:iterate>

	<iwcm:present name="pages">
	<logic:iterate id="doc" name="pages" type="sk.iway.iwcm.doc.DocDetails" indexId="index">
		<%
	   folderIcon = "-page";
		src = "";
		//xml escape
		String title = doc.getTitle();
		title = Tools.replace(title, "&", "&amp;");
		title = Tools.replace(title, "<", "&lt;");
		title = Tools.replace(title, ">", "&gt;");
		title = Tools.replace(title, "\"", "&quot;");

		//if (title.length()>23) title = doc.getTitle().subSequence(0, 20)+"...";
		if (Tools.isNotEmpty(doc.getExternalLink())) folderIcon = "-page-link";

		if (doc.isAvailable()==false) title="&lt;span style=color:red&gt;"+title+"&lt;/span&gt;";

		canAccess = true;
		if (Constants.getBoolean("adminCheckUserGroups") && DocDB.canAccess(doc, user, true)==false)
		{
			canAccess = false;
		}
		int totalPages = pages.size();
		int counter = index.intValue();
		if (session.getAttribute("webpages.treeShowAll."+doc.getGroupId())==null)
		{
			if (counter>docsCount && counter < (totalPages-docsCount))
			{
				canAccess = false;
			}
		}
		if (!canAccess && pagesWasSkip==false)
		{
			folderIcon = "-notloaded";
			%>
	   		<tree text="... <iwcm:text key="formslist.showAll"/> ..." action="javascript:xtreeItemPageClick(0, <%=groupId %>)" icon="/admin/images/dtree/folder<%=folderIcon%>.gif" openIcon="/admin/images/dtree/folderopen<%=folderIcon%>.gif" <%=src%> />
	   	    <%
	   	    pagesWasSkip = true;
		}
		if (canAccess)
		{
			String titleEscaped = JSEscapeTag.jsEscape(Tools.replace(Tools.replace(title, "'", " "), "\"", " "));
		%>
		<tree text="&lt;em&gt;<%if (viewInfoJSON != null && viewInfoJSON.getBoolean("sortPriority")) out.print(doc.getSortPriority() + ". ");%><%
			if (viewInfoJSON != null && viewInfoJSON.getBoolean("docId"))
				out.print("[#" + doc.getDocId() + "] ");%>&lt;/em&gt;<%=title%><%
			if (viewInfoJSON != null && viewInfoJSON.getBoolean("dateCreated"))
				out.print(" &lt;em&gt;(" + doc.getDateCreatedString() + ")&lt;/em&gt;");%>"
			action="javascript:xtreeItemPageClick(<bean:write name="doc" property="docId"/>, '<bean:write name="doc" property="sortPriority"/>. <%=titleEscaped %>')" icon="/admin/images/dtree/folder<%=folderIcon%>.gif" openIcon="/admin/images/dtree/folderopen<%=folderIcon%>.gif" <%=src%> /><%
		} %>
	</logic:iterate>
	</iwcm:present>

</tree>
