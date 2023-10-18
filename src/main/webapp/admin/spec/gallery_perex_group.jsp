<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.gallery.GalleryDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*" %>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="menuWebpages|menuGallery"/>

<%@page import="java.util.ArrayList"%>

<script type="text/javascript">
<!--


//-->
</script>

<%
	List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups();
%>

<jsp:useBean id="imageBeanSk" type="sk.iway.iwcm.gallery.GalleryBean" scope="request"/>

<div>
	<table border="0" cellpadding="1" cellspacing="0" style="width:240px;">
		<tr>
			<td><label for="perexGroupSearchId"><iwcm:text key="editor.search"/>:</label></td>
			<td>&nbsp;</td>
			<td><label for="disabledItemsRightId"><iwcm:text key="editor.perex_group.selected_items"/>:</label></td>
		</tr>
		<tr>
			<td valign="top">
			 	<input type="text" name="perexGroupSearch" id="perexGroupSearchId" onkeyup="perexGroupSearchChange(this, 'perexGroup', 'disabledItems')" style="width: 200px;" />

				<select name="disabledItemsLeft" multiple="true" size="6" class='disItems' style="width: 200px;">

				</select>
			</td>
			<td width="10">
				<input type="button" name="bMoveLeft" onclick="moveLeft(this.form, 'perexGroup', 'disabledItems');" title="<iwcm:text key="editor.perex_group.unselect"/>" value="<-" style="width: 20px;" class="btnLeft" />
				<input type="button" name="bMoveRight" onclick="moveRight(this.form, 'perexGroup', 'disabledItems');" title="<iwcm:text key="editor.perex_group.select"/>" value="->" style="width: 20px;" class="btnRight" />
			</td>
			<td valign="top">
				<select name="disabledItemsRight" id="disabledItemsRightId" multiple="true" size="8" class='disItems' style="width: 200px;">

				</select>
			</td>
		</tr>
	</table>

	<select name="perexGroup" multiple="multiple" size="6" style="display: none;" id="real">
		<%
			String perexGroupsString = GalleryDB.convertPerexGroupString(imageBeanSk.getPerexGroup());
			System.out.println("perexGroupsString: "+perexGroupsString);
			for (PerexGroupBean perexGroup: perexGroups)
			{
				out.println("<option value=\"" + perexGroup.getPerexGroupId() + "\"");
				if (perexGroupsString.indexOf("," + perexGroup.getPerexGroupId()+",")!=-1)	out.println("selected = \"selected\"");
				out.println(">" + perexGroup.getPerexGroupName() + "</option>");
			}
		%>
	</select>
</div>

<script type="text/javascript">
<!--
jQuery(document).ready(function() {
	initializeDisabledItems(document.imageForm, 'perexGroup', 'disabledItems');
});
//-->
</script>