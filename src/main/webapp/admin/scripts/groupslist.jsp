<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Tools,sk.iway.iwcm.doc.groups.GroupsController" %>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="menuWebpages|menuFbrowser|menuGallery"/>

var groupId = 0;

function xtreeItemClick(groupId, groupIdName)
{
	window.location.href="<%=GroupsController.BASE_URL%>"+groupId;
}

var globalCtxGroupId = -1;
var globalCtxDocId = -1;
var globalCtxGroupName = "";
var globalCtxDocName = "";

function nodeRightClick(nodeGroupId, nodeName)
{
	var el = document.getElementById("nodeContextMenu");
	if (nodeGroupId == 0)
		el = document.getElementById("rootNodeContextMenu");

	popupHide();
	//window.alert("nodeGroupId="+nodeGroupId+" nodeName="+nodeName);
	globalCtxGroupId = nodeGroupId;
	globalCtxGroupName = nodeName;

	if (isNaN(oy))
	{
		ox = 340;
		oy = 95;
	}

	if (el != null)
	{
		var jqDocument = $(document);
		var jqEl = $(el);
		if (jqDocument.height() < (jqEl.height() + oy + 25))
		{
			oy = oy - jqEl.height() - 25;
		}
		if (jqDocument.width() < (jqEl.width() + ox + 25))
		{
			ox = jqDocument.width() - jqEl.width() - 25;
		}

		el.style.visibility='visible';
		var pom = ox + 10;
		if (pom < 0)
			pom = 3;
		el.style.top = oy + 10 + "px";
		el.style.left = pom + "px";

		try
		{
			if ("System"==nodeName)
			{
				$("#nodeContextMenu td.systemHide").hide();
			}
			else
			{
				$("#nodeContextMenu td.systemHide").show();
			}
		} catch (e) {}
	}
}

function pageRightClick(nodeDocId, nodeName)
{
	popupHide();

	globalCtxDocId = nodeDocId;
	globalCtxDocName = nodeName;

	if (isNaN(oy))
	{
	   ox = 340;
	   oy = 95;
	}

   var el = $("#pageContextMenu");

	if (el != null)
	{
		var jqDocument = $(document);
		var jqEl = el;
		if (jqDocument.height() < (jqEl.height() + oy + 25))
		{
			oy = oy - jqEl.height() - 25;
		}
		if (jqDocument.width() < (jqEl.width() + ox + 25))
		{
			ox = jqDocument.width() - jqEl.width() - 25;
		}

		el.css('visibility','visible');
		var pom = ox + 10;
		if (pom < 0)
			pom = 3;


		el.css('top', oy + 10 + "px");
		el.css('left', pom + "px");

		try
		{
			if ("Header"==nodeName || "Footer"==nodeName)
			{
				$("#pageContextMenu td.systemHide").hide();
			}
			else
			{
				$("#pageContextMenu td.systemHide").show();
			}
		} catch (e) {}
	}
}

function contexClick(ctxAct)
{
	var ctxValue = null;

	if ("rename" == ctxAct)
	{
		globalCtxGroupName = globalCtxGroupName.replace("<span style=color:red>", "");
		globalCtxGroupName = globalCtxGroupName.replace("<span style=color:red!important>", "");
		var myValue= window.prompt("<iwcm:text key="groupslist.enter_dir_name"/>", globalCtxGroupName);
		if (myValue != "" && myValue != globalCtxGroupName)
		{
			ctxValue = myValue;
		}
	}
	else if ("newFolder" == ctxAct)
	{
		var myValue = window.prompt("<iwcm:text key="groupslist.enter_dir_name"/>", "<iwcm:text key="groupslist.new_dir_default_name"/>");
		if (myValue != "" && myValue != globalCtxGroupName)
		{
			ctxValue = myValue;
		}
	}
	else if ("newRootFolder" == ctxAct)
	{
		var myValue = window.prompt("<iwcm:text key="groupslist.enter_dir_name"/>", "<iwcm:text key="groupslist.new_dir_default_name"/>");
		if (myValue != "" && myValue != globalCtxGroupName)
		{
			ctxValue = myValue;
			globalCtxGroupId = 0;
			ctxAct = "newFolder";
		}
	}
	else if ("delete" == ctxAct)
	{
		if (window.confirm("<iwcm:text key="groupslist.delete_dir_confirm"/>"))
		{
		   ctxValue = "delete";
		}
	}
	else if ("renamePage" == ctxAct)
	{
		globalCtxDocName = globalCtxDocName.replace("<span style=color:red>", "");
		globalCtxDocName = globalCtxDocName.replace("<span style=color:red!important>", "");
		var myValue = window.prompt("<iwcm:text key="groupslist.enter_page_name"/>", globalCtxDocName);
		if (myValue != "" && myValue != globalCtxGroupName)
		{
			ctxValue = myValue;
		}
	}
	else if ("newPagePage" == ctxAct)
	{
		globalCtxGroupId = groupId;
		ctxAct = "newPage";
		ctxValue = "-1";
	}
	else if ("newBlankPage" == ctxAct)
	{
		var myValue = window.prompt("<iwcm:text key="groupslist.enter_page_name"/>", "<iwcm:text key="editor.newDocumentName"/>");
		if (myValue != "" && myValue != globalCtxGroupName)
		{
			ctxValue = myValue;
		}
	}
	else if ("deletePage" == ctxAct)
	{
		if (window.confirm("<iwcm:text key="groupslist.delete_page_confirm"/>"))
		{
		   ctxValue = "delete";
		}
	}
	else if ("movePageUp" == ctxAct || "movePageDown" == ctxAct)
	{
		globalCtxGroupId = groupId;
		ctxValue = "move";
	}
	else if ("setAsDefaultPage" == ctxAct)
	{
		globalCtxGroupId = groupId;
		ctxValue = "setAsDefaultPage";
	}
	else if ("pageStat" == ctxAct)
	{
		window.location.href="/apps/stat/admin/top-details/?docId="+globalCtxDocId+"&title="+globalCtxDocName;
	}
	else if ("pageHistory" == ctxAct)
	{
		window.setTimeout("popupDIV('dochistory.jsp?docid=" + globalCtxDocId + "')", 200);
	}
	else if ("properties" == ctxAct)
	{
		openPopupDialogFromLeftMenu("<%=GroupsController.BASE_URL%>"+globalCtxGroupId+"/?Edit=Edituj");
		return;
	}
	else if ("propertiesPopup" == ctxAct)
	{
		openPopupDialogFromLeftMenu("<%=GroupsController.BASE_URL%>"+globalCtxGroupId+"/?Edit=Edituj&singlePopup=true");
		return;
	}
	else if ("sortPriority" == ctxAct)
	{
		location.href='/admin/skins/webjet6/groupslist-tree.jsp?viewInfo=sortPriority&rnd=<%= Tools.getNow()%>';
	}
	else if ("docId" == ctxAct)
	{
		location.href='/admin/skins/webjet6/groupslist-tree.jsp?viewInfo=docId&rnd=<%= Tools.getNow()%>';
	}
	else if ("dateCreated" == ctxAct)
	{
		location.href='/admin/skins/webjet6/groupslist-tree.jsp?viewInfo=dateCreated&rnd=<%= Tools.getNow()%>';
	}
	else
	{
		ctxValue = ctxAct;
	}

	if (ctxValue != null)
	{
		document.ctxForm.ctxAct.value = ctxAct;
		document.ctxForm.ctxGroupId.value = globalCtxGroupId;
		document.ctxForm.ctxDocId.value = globalCtxDocId;
		document.ctxForm.ctxValue.value = ctxValue;
		document.ctxForm.submit();
	}
}

function contexHilite(elementObject)
{
	if (elementObject.style.color == "ButtonShadow" || elementObject.style.color == "buttonshadow")
	{
		return;
	}

	elementObject.style.backgroundColor='#a4dbed';
}

function contexDelite(elementObject)
{
	elementObject.style.backgroundColor='transparent';
}