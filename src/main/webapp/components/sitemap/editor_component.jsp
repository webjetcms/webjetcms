<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<% request.setAttribute("cmpName", "sitemap"); %>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.Constants"%>
<jsp:include page="/components/top.jsp"/>
<%
int actualRootGroupId = 1;
if (session.getAttribute(Constants.SESSION_GROUP_ID) != null)
{
	int  actualGroupId = Integer.parseInt((String) session.getAttribute(Constants.SESSION_GROUP_ID));
	GroupsDB groupsDB = GroupsDB.getInstance();
	actualRootGroupId = groupsDB.getRoot(actualGroupId);
}

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
String jspFileName = Tools.getRequestParameter(request, "jspFileName");
if(Tools.isNotEmpty(jspFileName)){
	int slash = jspFileName.lastIndexOf("/");
	int dot = jspFileName.lastIndexOf(".");

	if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
}
if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

int groupId = pageParams.getIntValue("groupId", -1);	//neviem preco ma jeden parameter dva rozdielne nazvy!!!
if(groupId == -1) groupId = pageParams.getIntValue("rootGroupId", actualRootGroupId);

%>

<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<script type='text/javascript'>
if (isFck)
{

}
else
{
	resizeDialog(550, 200);
}

function setParentGroupId(returnValue)
{
	if(returnValue.length > 15)
	{
		var groupid = returnValue.substr(0,15);
		var groupname = returnValue.substr(15);
		groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
		document.textForm.groupId.value = groupid;
	}
}

function Ok()
{
			groupId = document.textForm.groupId.value;
			mapType = document.textForm.mapType.value;

			if (mapType == "sitemap")
			{
				htmlCode = "!INCLUDE(/components/sitemap/sitemap.jsp, groupId="+groupId+")!";
			}
			if (mapType == "treelist")
			{
				htmlCode = "!INCLUDE(/components/sitemap/treelist.jsp, groupId="+groupId+")!";
			}
			if (mapType == "ul_li")
			{
				maxDepth = document.ulForm.maxDepth.value;
				colsNum = document.ulForm.colsNum.value;
				htmlCode = "!INCLUDE(/components/sitemap/ul_li.jsp, groupId="+groupId+", maxDepth="+maxDepth+", colsNum="+colsNum+")!";
			}
			if (mapType == "ul_li_jquery")
			{
				maxLevel = document.ulJQueryForm.maxDepth.value;
				htmlCode = "!INCLUDE(/components/sitemap/ul_li_jquery.jsp, rootGroupId="+groupId+", maxLevel="+maxLevel+")!";
			}
	oEditor.FCK.InsertHtml(htmlCode);
	return true ;
} // End function

function showHelp(select)
{
	size = select.options.length;
	for (i=0; i<size; i++)
	{
		el = document.getElementById("help_"+i);
		if (el!=null)
		{
			if (i==select.selectedIndex)
			{
				el.style.display = "block";
			}
			else
			{
				el.style.display = "none";
			}
		}
	}
}

</script>

<div class="tab-pane toggle_content" style="height:540px !important; o2verflow: auto; width:990px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

		<table border="0" cellspacing="0" cellpadding="1">
			<form method=get name=textForm>
			<tr>
				<td><iwcm:text key="components.sitemap.root_group"/>:</td>
				<td>
					<input type=text name=groupId size=5 maxlength="10" value="<%=groupId%>">
					<input type="button" name="groupSelect" class="btn green" value='<iwcm:text key="components.sitemap.change"/>' onClick='popupFromDialog("/admin/grouptree.jsp", 300, 450);'>
				</td>
			</tr>
			<tr>
				<td>
					<iwcm:text key="components.sitemap.map_type"/>
				</td>
				<td>
					<select name="mapType"  onChange="showHelp(this)">
						<option value="ul_li"><iwcm:text key="components.sitemap.ul_li"/></option>
						<option value="sitemap"><iwcm:text key="components.sitemap.sitemap"/></option>
						<option value="treelist"><iwcm:text key="components.sitemap.treelist"/></option>
						<option value="ul_li_jquery"><iwcm:text key="components.sitemap.ul_li_jquery"/></option>
					</select>
				</td>
			</tr>
			</form>
		</table>

		<div id="help_0">
		<form name="ulForm" >
		  <table>
				<tr>
					<td nowrap><iwcm:text key="components.sitemap.max_depth"/>:</td>
					<td><input type="text" name="maxDepth" size=5 maxlength="5" value="<%=pageParams.getIntValue("maxLevel", 5)%>"></td>
				</tr>
				<tr>
					<td nowrap><iwcm:text key="components.sitemap.cols_num"/>:</td>
					<td><input type="text" name="colsNum" size=5 maxlength="5" value="<%=pageParams.getIntValue("colsNum", 1)%>"></td>
				</tr>
			</table>
		</form>
		</div>

		<div id="help_3" style="display: none;">
		<form name="ulJQueryForm" >
		  <table>
				<tr>
					<td nowrap><iwcm:text key="components.sitemap.max_depth"/>:</td>
					<td><input type="text" name="maxDepth" size=5 maxlength="5" value="<%=pageParams.getIntValue("maxLevel", 5)%>"></td>
				</tr>
			</table>
		</form>
		</div>
	</div>
</div>

<script type="text/javascript">
<% if (Tools.isNotEmpty(jspFileName)) {%>
	document.textForm.mapType.value = "<%=jspFileName%>";
	showHelp(document.textForm.mapType);
<%}%>
</script>

<jsp:include page="/components/bottom.jsp"/>
