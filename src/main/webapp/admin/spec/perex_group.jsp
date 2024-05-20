<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/>
<%@ page import="sk.iway.iwcm.Tools"%>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Constants" %>
<%@ page import="java.util.List" %>
<%
int groupId = -1;
DocDetails doc = DocDB.getInstance().getDoc(Tools.getIntValue((String)request.getAttribute("docid"), -1));
if(doc != null && doc.getDocId() > 0)
{
	groupId = doc.getGroupId();
	session.setAttribute("groupId", groupId);
}
else
{
	if(request.getAttribute("groupId") != null)
		groupId = (Integer)request.getAttribute("groupId");
	else if(session.getAttribute("groupId") != null)
		groupId = (Integer)session.getAttribute("groupId");
}

PageParams pageParams = new PageParams(request);
int[] groupIds = null;
if (groupId < 1) {
    groupIds = Tools.getTokensInt(pageParams.getValue("groupIds", "-1"), ",+");
}
if (groupId < 1)
{
	groupId = Tools.getIntValue((String)session.getAttribute(Constants.SESSION_GROUP_ID), -1);
}

System.out.println("--------- groupId="+groupId+" groupIds="+pageParams.getValue("groupIds", "-1"));
List<PerexGroupBean> perexGroups;
if (groupIds != null) perexGroups = DocDB.getInstance().getPerexGroups(groupIds, false);
else if (groupId>0) perexGroups = DocDB.getInstance().getPerexGroups(groupId);
else perexGroups = DocDB.getInstance().getPerexGroups();
request.setAttribute("perexGroups", perexGroups);
%>

<table style="width:100%;">
	<!-- <tr>
		<td><iwcm:text key="editor.search"/>:</td>
		<td>&nbsp;</td>
	</tr> -->
	<tr>
		<td valign="top">
			<div class="input-icon input-icon-lg right">
				<i class="ti ti-search"></i>
		   	<input type="text" name="perexGroupSearch" onkeyup="perexGroupSearchChange(this, 'perexGroup', 'disabledItems')" class="form-control" />
		   </div>
		</td>
		<td></td>
		<td><label for="disabledItemsLeft1"><iwcm:text key="editor.perex_group.selected_items"/>:</label></td>
	</tr>
	<tr>
		<td style="width: 47%; vertical-align: top;">
			<select name="disabledItemsLeft" id="disabledItemsLeft1" multiple="true" size="7" class="disItems form-control"></select>
		</td>
			<td style=" text-align: center; vertical-align: middle; width: 6%; padding-left: 8px; padding-right: 8px;">
				<button type="button" onclick="moveLeft(this.form, 'perexGroup', 'disabledItems');" title="<iwcm:text key="editor.perex_group.unselect"/>" class="btn green">
					<i class="ti ti-circle-arrow-left"></i>
				</button>

				<button type="button" onclick="moveRight(this.form, 'perexGroup', 'disabledItems');" title="<iwcm:text key="editor.perex_group.select"/>" class="btn green" style="margin-top: 8px;">
					<i class="ti ti-circle-arrow-right"></i>
				</button>
			</td>
		<td style="width: 47%; vertical-align: top;">
			<select name="disabledItemsRight" multiple="true" size="7" class="disItems form-control"></select>
		</td>
	</tr>
</table>

<select name="perexGroup" id="real" multiple="true" size="6" style="display: none;">
	<%
		String perexGroupsString = (String)request.getAttribute("perexGroup");
		//System.out.println("perexGroupsString: "+perexGroupsString);
		for (PerexGroupBean perexGroup: perexGroups)
		{
			out.println("<option value=\"" + perexGroup.getPerexGroupId() + "\"");
			if (perexGroupsString != null && perexGroupsString.length() > 0)
				if (perexGroupsString.indexOf("," + perexGroup.getPerexGroupId()+",")!=-1)
					out.println("selected = \"selected\"");
			out.println(">" + perexGroup.getPerexGroupNameId() + "</option>");
		}
	%>
</select>


<script type="text/javascript">
jQuery(document).ready(function()
{
	try{
		if (typeof document.editorForm != "undefined") initializeDisabledItems(document.editorForm, 'perexGroup', 'disabledItems');
		if (typeof document.textForm != "undefined") initializeDisabledItems(document.textForm, 'perexGroup', 'disabledItems');
	} catch (e) {}
});
</script>
