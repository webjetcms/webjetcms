<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.*, sk.iway.iwcm.doc.*, sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "related-pages");

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

List groups = DocDB.getInstance().getPerexGroups();

if (groups.size() > 0)
{
	request.setAttribute("perexGroups", groups);
}

DocDB docDB = DocDB.getInstance(true);
DocDetails docDetails;
String perGroups[] = null;
String groupStr = "";
PerexGroupBean pgBean = null;
if (request.getParameter("openerDocId") != null)
{
	int openerDocId = Integer.parseInt(request.getParameter("openerDocId"));
	docDetails = docDB.getDoc(openerDocId);
	if (docDetails != null)
	{
		perGroups = docDetails.getPerexGroup();
		if (perGroups != null)
		{
			for (int i=0; i<perGroups.length; i++)
			{
				pgBean = DocDB.getInstance().getPerexGroup(-1, perGroups[i]);
				if (groupStr.length() > 0)
					groupStr += "+"+pgBean.getPerexGroupId();
				else
					groupStr = ""+pgBean.getPerexGroupId();
			}
		}
	}
}

%>
<jsp:include page="/components/top.jsp"/>
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type='text/javascript'>

function Ok()
{
	var useParentName = "false";
	pagesInGroup = document.textForm.pagesInGroup.value;
	groups = document.textForm.groups.value;
	var rGroupsRecursive = false;
	titleName = document.textForm.titleName.value;
	if (document.textForm.rGroupsRecursive.checked == true)
		rGroupsRecursive = true;
	for (var i = 0; i<document.textForm.titleName.length; i++)
	{
		if (document.textForm.titleName[i].checked)
		{
			if (document.textForm.titleName[i].value == "customName")
				titleName = document.textForm.customName.value;
			else
				titleName = document.textForm.titleName[i].value;
		}
	}
	rootGroup = document.textForm.rootGroup.value;
	oEditor.FCK.InsertHtml("!INCLUDE(/components/related-pages/related_pages.jsp, pagesInGroup="+pagesInGroup+", groups="+groups+", rootGroups="+rootGroup+", rGroupsRecursive="+rGroupsRecursive+", titleName="+titleName+")!");
	return true ;
} // End function

if (isFck)
{

}
else
{
	resizeDialog(500, 400);
}


function setParentGroupId(returnValue)
{
	if (returnValue.length > 15)
	{
		var groupid = returnValue.substr(0,15);
		var groupname = returnValue.substr(15);
		groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
		if (document.textForm.rootGroup.value == "")
		{
			document.textForm.rootGroup.value = groupid;
		}
		else
		{
			document.textForm.rootGroup.value = document.textForm.rootGroup.value + "+" + groupid;
		}

	}
}

function addGroup()
{
	var value = document.textForm.perexGroupId.value;
	if (document.textForm.perexGroupId.value != -1)
	{
		if (document.textForm.groups.value == "")
		{
			document.textForm.groups.value = document.textForm.perexGroupId.value;
		}
		else
		{
			document.textForm.groups.value = document.textForm.groups.value + "+" + value;
		}
	}


}

function setSelectedPerexGroups()
{
	if (isFck)
	{
		var oEditor = window.parent.InnerDialogLoaded();
		var FCK		= oEditor.FCK ;
		document.actionForm.openerDocId.value = FCK.LinkedField.form.docId.value;
	}
	else
	{
		document.actionForm.openerDocId.value = window.opener.editorForm.docId.value;
	}

	document.actionForm.reloadAction.value = "reload";
	document.actionForm.submit();
}

function checkRootGroup()
{
	var rgValue = document.textForm.rootGroup.value;
	if (rgValue.length > 2 && rgValue.indexOf("+") > 0)
	{
		if (document.textForm.titleName[0].checked)
		{
			document.textForm.customName.focus();
			document.textForm.titleName[2].checked = true;

		}
		document.textForm.titleName[0].disabled = true;
	}
	else
	{
		document.textForm.titleName[0].disabled = false;
	}
}

</script>
<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	.formMainGroup {
		margin-top: 20px;
	}
	.formGroup {
		margin-bottom: 10px;
	}
	label {
		margin-bottom: 5px;
	}
</style>
<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">

		<form action="/components/related-pages/editor_component.jsp" method="get" name="actionForm" >
			<input type="hidden" name="openerDocId">
			<input type="hidden" name="reloadAction">
			<input type="hidden" name="pageParams" value="<%=Tools.getRequestParameterUnsafe(request, "pageParams") %>">
		</form>
		<form method=get name=textForm>
			<div class="container">
				<div class="formMainGroup">
					<div class="formGroup row">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="components.news.groupids"/>
						</div>
						<div class="col-sm-8">
							<input type=text name=rootGroup size=26 maxlength="255" value="<%=ResponseUtils.filter(pageParams.getValue("rootGroups", ""))%>" onchange="JavaScript: checkRootGroup();" />
							<input type="button" name="groupSelect" class="btn green" value="<iwcm:text key="editor.spell.add"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500),checkRootGroup();' />
						</div>
					</div>
					<div class="formGroup row">
						<div class="col-sm-4"></div>
						<div class="col-sm-8">
							<input type="checkbox" name="rGroupsRecursive" <%if (pageParams.getBooleanValue("rGroupsRecursive", false)) out.print("checked='checked'");%> /><iwcm:text key="components.user.root_group_recursive"/>
						</div>
					</div>
				</div>
				<div class="formMainGroup">
					<div class="formGroup row">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="components.user.title_name"/>
						</div>
						<div class="col-sm-8">
							<label for="radio1">
								<input type="radio" name="titleName" id="radio1" value="groupName" onclick="JavaScript: checkRootGroup();"/>
								<iwcm:text key="components.user.root_group"/>
							</label>
							<br>
							<label for="radio2">
								<input type="radio" name="titleName" id="radio2" value="rootGroupName" onclick="JavaScript: checkRootGroup();"/>
								<iwcm:text key="components.user.root_group_parent"/>
							</label>
							<br>
							<label for="radio3">
								<input type="radio" name="titleName" id="radio3" value="customName" onclick="JavaScript: checkRootGroup(), document.textForm.customName.focus();" />
								<iwcm:text key="components.user.custom_title"/>
								<input type="text" name="customName" size="20" maxlength="30" onfocus="javascript: document.textForm.titleName[2].checked = true;" />
							</label>
						</div>
					</div>
				</div>
				<div class="formMainGroup">
					<div class="formGroup row">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="components.user.perex_groups"/>
						</div>
						<div class="col-sm-8">
							<logic:present name="perexGroups">
								<select name='perexGroupId'>
									<option value='-1'><iwcm:text key="components.related-pages.select_default"/></option>
								  <logic:iterate id="pg" name="perexGroups" type="sk.iway.iwcm.doc.PerexGroupBean">
									<option value='<%=pg.getPerexGroupId()%>'><%=pg.getPerexGroupName()%></option>
								  </logic:iterate>
								</select>
							</logic:present>
							<input type="button" name="groupSelect"class="btn green" value="<iwcm:text key="editor.spell.add"/>" onClick="addGroup();" />
						</div>
					</div>
					<div class="formGroup row">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="components.user.perex_group_id"/>
						</div>
						<div class="col-sm-8">
							<input type=text name=groups size=26 maxlength="255" value="<%=ResponseUtils.filter(pageParams.getValue("groups", ""))%>" />
						</div>
					</div>
				</div>
				<div class="formMainGroup">
					<div class="formGroup row">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="components.related-pages.results_per_group"/>
						</div>
						<div class="col-sm-8">
							<input type=text name=pagesInGroup size=2 value="<%=ResponseUtils.filter(pageParams.getValue("pagesInGroup", "10"))%>" />
						</div>
					</div>
				</div>
			</div>
		</form>
		<script type="text/javascript">
			//inicializacia poloziek

			<%if (request.getParameter("reloadAction") == null)
			  {%>
				setSelectedPerexGroups();
			<%}%>

			<% if(Tools.isEmpty(ResponseUtils.filter(pageParams.getValue("titleName", ""))) || ResponseUtils.filter(pageParams.getValue("titleName", "")).compareTo("groupName") == 0) 	//default alebo nastavene
			{ %>
				$("#radio1").attr("checked", "checked");
				checkRootGroup();
		<%	}
			else if(ResponseUtils.filter(pageParams.getValue("titleName", "")).compareTo("rootGroupName") == 0)
			{ %>
				$("#radio2").attr("checked", "checked");
				checkRootGroup();
		<%	}
			else
			{ %>
				$("#radio3").attr("checked", "checked");
				document.textForm.customName.value="<%=ResponseUtils.filter(pageParams.getValue("titleName", ""))%>"
				checkRootGroup();
				document.textForm.customName.focus();
		<%	} %>

		</script>
	</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
