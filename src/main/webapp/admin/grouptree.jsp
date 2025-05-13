<%@page import="sk.iway.iwcm.Constants"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ page import="sk.iway.iwcm.Identity,sk.iway.iwcm.InitServlet,sk.iway.iwcm.common.CloudToolsForCore" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.GroupDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/>
<%
//otestuj ci existuje nahrada za tuto stranku
String forward = "/admin/spec/"+Constants.getInstallName()+"/grouptree.jsp";
java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
if (fForward.exists())
{
	pageContext.forward(forward);
	return;
}

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

boolean alsoPages = false;
if ("true".equals(Tools.getRequestParameter(request, "alsoPages"))) alsoPages = true;

int rootGroupId = 0;
String rootGroupIdName = "0              /";
if (InitServlet.isTypeCloud())
{
	GroupDetails rootGroupDetails = GroupsDB.getInstance().getGroup(CloudToolsForCore.getDomainId());
	if (rootGroupDetails != null)
	{
		rootGroupId = rootGroupDetails.getGroupId();
		rootGroupIdName = rootGroupDetails.getGroupIdName();
	}
}
%>
<html>
<head>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
<LINK rel="stylesheet" href="css/style.css">
<title><iwcm:text key="grouptree.title"/></title>
</head>
<body>

<%
   //System.out.println("grouptree sid: " + session.getId());

   Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

   if (user==null)
   {
   	System.out.println("SOM NULL");
      return;
   }

	GroupsDB groupsDB = GroupsDB.getInstance(false);
   //GroupsDB groupsDB = GroupsDB.getInstance(sk.iway.iwcm.Constants.getServletContext(), false, DBPool.getDBName(request));
   String userEditableGroups = user.getEditableGroups();
   if (user.isDisabledItem("users.edit_admins")==false) userEditableGroups = "";

   List<GroupDetails> groups = groupsDB.getGroupsTree(userEditableGroups);
   groups = sk.iway.iwcm.editor.service.WebpagesService.filterGroupsByCurrentDomain(groups);
   pageContext.setAttribute("groups", groups);

   //List groups = groupsDB.getGroups(0);
	//request.setAttribute("groups", groups);

   String fcnName = "setParentGroupId";
   String inputId = "";
   boolean addOtherGroup = false;
   boolean setInputValue =false;
   if(Tools.getRequestParameter(request, "fcnName") != null)
   		fcnName = Tools.getRequestParameter(request, "fcnName").trim();
   if("addOtherGroupId".equals(Tools.getRequestParameter(request, "fcnName")))
   		addOtherGroup = true;
   if(Tools.getRequestParameter(request, "inputId")!=null)
   {
   	  setInputValue = true;
   	  inputId = Tools.getRequestParameter(request, "inputId");
   }
String folderIcon;
int parentGroupId;
%>

<script type="text/javascript">
<!--
function xtreeItemClick(groupId, groupIdName)
{
   //window.alert("value="+groupIdName);
   window.returnValue=groupIdName;
   if(window.opener && <%=setInputValue%>)
   {
	   window.opener.document.getElementById('<%=inputId%>').value = groupId
   }
   else if(window.opener && window.opener.<%=fcnName%> && <%=addOtherGroup%>)
   {
	   	window.opener.<%=fcnName%>(groupId);
   }
   else if(window.opener && window.opener.<%=fcnName%>)
   {
		window.opener.<%=fcnName%>(groupIdName);
		//alert(value);
   }
   else if(window.parent && window.parent.<%=fcnName%>)
   {
		window.parent.<%=fcnName%>(groupIdName);
		window.parent.WJ.closeIframeModal();
		//alert(value);
   }
   self.close();
}

function xtreeItemPageClick(docid, name)
{
	//defined in _groupslist_drag_and_drop.jsp
	if(window.objectBeingDragged)
		return false
    var ret = new Array(2);
    ret[0] = docid;
    ret[1] = name;
    window.returnValue = ret;

    if(window.opener && window.opener.setPage)
	   {
	   	window.opener.setPage(ret);
	   	//alert(ret);
	   }
    window.close();
 }
//-->
</script>

<script type="text/javascript" src="/admin/scripts/xtree/xtree-dragdrop.js"></script>
<script type="text/javascript" src="/admin/scripts/xtree/xmlextras.js"></script>
<script type="text/javascript" src="/admin/scripts/xtree/xloadtree.js"></script>
<link type="text/css" rel="stylesheet" href="/admin/scripts/xtree/xtree.css" />
<script type="text/javascript">
webFXTreeConfig.loadingText = "<iwcm:text key="groupslist.xtree.loading"/>";
</script>

<%="<script type='text/javascript'>"%>

	/// XP Look
	webFXTreeConfig.rootIcon		= "/admin/images/dtree/base.gif";
	webFXTreeConfig.openRootIcon	= "/admin/images/dtree/base.gif";
	webFXTreeConfig.folderIcon		= "/admin/images/dtree/folder.gif";
	webFXTreeConfig.openFolderIcon	= "/admin/images/dtree/folderopen.gif";
	webFXTreeConfig.fileIcon		= "/admin/images/dtree/folder.gif";
	webFXTreeConfig.lMinusIcon		= "/admin/images/dtree/minusbottom.gif";
	webFXTreeConfig.lPlusIcon		= "/admin/images/dtree/plusbottom.gif";
	webFXTreeConfig.tMinusIcon		= "/admin/images/dtree/minus.gif";
	webFXTreeConfig.tPlusIcon		= "/admin/images/dtree/plus.gif";
	webFXTreeConfig.iIcon			= "/admin/images/dtree/line.gif";
	webFXTreeConfig.lIcon			= "/admin/images/dtree/joinbottom.gif";
	webFXTreeConfig.tIcon			= "/admin/images/dtree/join.gif";
	webFXTreeConfig.usePersistence = false;
	webFXTreeConfig.focus = null;

	//var tree = new WebFXLoadTree("WebFXLoadTree", "tree1.xml");
	//tree.setBehavior("classic");

	var rti;
	//var tree = new WebFXTree("Web");
	var tree = new WebFXTree("<b><iwcm:text key="groupslist.dirs"/></b>", "javascript:xtreeItemClick(<%=rootGroupId %>, '<%=rootGroupIdName %>')", null, null, "/admin/images/dtree/base.gif", "/admin/images/dtree/base.gif");
	<%
	List<String> allreadyHasGroupParentTable = new ArrayList<>();
	%>
	<iwcm:iterate id="group" name="groups" type="sk.iway.iwcm.doc.GroupDetails" indexId="index"><%
		boolean canAccess = true;
		if (Constants.getBoolean("adminCheckUserGroups"))
		{
			canAccess = DocDB.canAccess(group, user);
		}
		if (group.isHiddenInAdmin() && user.isDisabledItem("editor_show_hidden_folders")) canAccess = false;
		if (group.getParentGroupId() == 0 && group.getGroupName().length()>2 && group.getGroupName().startsWith("/"))
		{
			//kontrola na duplicity v strome, user moze mat pravo na nejaky folder a nasledne aj na subfoldre, co by spravilo duplicitu v strome
			boolean uzMamPrefix = false;
			for (String prefix : allreadyHasGroupParentTable)
			{
				if (group.getGroupName().startsWith(prefix))
				{
					//out.println("//uz mam: "+group.getGroupName()+" prefix:"+prefix);
					uzMamPrefix = true;
					break;
				}
			}
			//out.println("// prefix: "+group.getGroupName()+" uz mam: "+uzMamPrefix);
			if (uzMamPrefix)
			{
				//uz takyto strom mam, preskocime
				canAccess = false;
			}
			else
			{
				//este nemam, pridam si
				allreadyHasGroupParentTable.add(group.getGroupName());
			}
		}


	   if (canAccess && group.getParentGroupId()==0)
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
			}
			if (group.isInternal())
			{
				if ("system".equals(folderIcon))
				{

				}
				else if ("lock".equals(folderIcon))
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

	   %>tree.add(new WebFXLoadTreeItem("<jsp:getProperty name="group" property="sortPriority"/>. <jsp:getProperty name="group" property="groupNameShort"/>", "/admin/scripts/xtree/tree.jsp?<%if (alsoPages) out.print("alsoPages=true&"); %>parent=<iwcm:beanWrite name="group" property="groupId"/>", "javascript:xtreeItemClick(<iwcm:beanWrite name="group" property="groupId"/>, '<jsp:getProperty name="group" property="groupIdName"/>')", null, "images/dtree/folder<%=folderIcon%>.gif", "images/dtree/folderopen<%=folderIcon%>.gif"));<% }
	%></iwcm:iterate>

	document.write(tree);

<%="</script>"%>
<br><br>

</html>
</body>
