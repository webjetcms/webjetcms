<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="java.util.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>

<%@ page import="sk.iway.iwcm.InitServlet" %>
<%@ page import="sk.iway.iwcm.Identity" %>
<%@ page import="sk.iway.iwcm.Constants" %>

<iwcm:menu notName="modUpdate">
	<%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
	%>
</iwcm:menu>

<%@ include file="../layout_top.jsp" %>

<%
	List errors = (ArrayList)session.getAttribute("updateErrors");
	if (errors != null)
	{
		request.setAttribute("updateErrors", errors);
		session.removeAttribute("updateErrors");
	}
%>

<script type="text/javascript" src="../scripts/tablesort.js"></script>
<link rel="stylesheet" type="text/css" href="../css/tablesort.css" />

<script type="text/javascript">
<!--
	helpLink = "admin/update.jsp&book=admin";

	function bSubmitVersionsClick(btn)
	{
		el = document.getElementById("noteDiv");
		if (el != null)
		{
			el.innerHTML = '<iwcm:text key="update.showFilesNote"/>';
			el.style.display = "block";
		}
		btn.disabled = true;
		btn.form.submit();
	}

	function bSubmitFilesClick(btn)
	{
		el = document.getElementById("noteDiv");
		if (el != null)
		{
			el.innerHTML = '<iwcm:text key="update.downloadFilesNote"/>';
			el.style.display = "block";
		}
		btn.disabled = true;
		btn.form.submit();
	}

	function openPopupNew()
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=600,height=500";
		popupwindow = window.open('/admin/conf_editor_popup.jsp', "ConfDBeditor", options);
		if (window.focus)
			popupwindow.focus();
	}

	function confirmRestart()
	{
		if (window.confirm('<iwcm:text key="admin.conf_editor.do_you_really_want_to_restart"/>'))
		{
			document.actionForm.act.value="restart";
			document.actionForm.submit();
		}
	}
//-->
</script>

<form method="get" action="/admin/conf_editor.jsp" name="actionForm">
	<input type="hidden" name="act" />
	<input type="hidden" name="name" />
</form>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-refresh"></i><iwcm:text key="admin.conf_editor.update_system"/></h1>
</div>

<div class="content-wrapper">

<%
	if (Constants.getBoolean("statEnableTablePartitioning")==false)
	{
%>
		<p><iwcm:text key="admin.update.webjet6notify"/></p>
<%
	}
%>

<div class="note note-warning">
	<button type="button" class="close" data-dismiss="alert" aria-hidden="true"></button>
	<p><iwcm:text key="admin.update.webjet7notify"/></p>
</div>

<div class="note note-warning">
	<button type="button" class="close" data-dismiss="alert" aria-hidden="true"></button>
	<p><iwcm:text key="admin.update.notify"/></p>
</div>

<logic:present name="updateErrors">
	<span class="error"><iwcm:text key="update.errors"/>:</span><br />
	<ul>
		<logic:iterate id="lvb" name="updateErrors" type="sk.iway.iwcm.LabelValueDetails">
			<li>
				<bean:write name="lvb" property="label"/>:
				<bean:write name="lvb" property="value" filter="false"/>
			</li>
		</logic:iterate>
	</ul>
	<br />
</logic:present>

<!-- Pravy panel: verzia a kontakty, Predaj, Podpora, Info -->
<div class="panel panel-default">
	<div class="panel-heading">
		 Info
	</div>
	<div class="panel-body">


		<div class="row">

			<div class="col-sm-6">
				<strong><iwcm:text key="welcome.version_and_contants"/></strong>
				<br />WebJET Content Management<br />
				<iwcm:text key="welcome.version"/>:<br />
				<%=InitServlet.getActualVersionLong()%>
			</div>

			<div class="col-sm-6">
				<strong><iwcm:text key="welcome.sales"/>:</strong>
				<a href="mailto:<iwcm:text key="welcome.sales_email"/>">
					<iwcm:text key="welcome.sales_email"/>
				</a>
				<br />
				<small><iwcm:text key="welcome.sales_info"/></small>
				<br /><br />

				<strong><iwcm:text key="welcome.support"/>:</strong>
				<a href="mailto:<iwcm:text key="welcome.support_email"/>">
					<iwcm:text key="welcome.support_email"/>
				</a>
				<br /><small><iwcm:text key="welcome.support_info"/></small>
				<br /><br />

				<strong><iwcm:text key="welcome.info"/>:</strong>
				<a href="http://<iwcm:text key="welcome.info_url"/>" target="_blank">
					<iwcm:text key="welcome.info_url"/>
				</a>

				<logic:present name="sessionList">
					<hr /><strong><iwcm:text key="welcome.logged_administrators"/>:</strong><br />
					<logic:iterate id="ses" name="sessionList" type="sk.iway.iwcm.stat.SessionDetails" indexId="index">
					<%
						try
			         	{
							if (ses.getLoggedUserId()>0)
								out.print(ses.getLoggedUserName()+"<br>");
						}
						catch (Exception ex2) { }
					%>
					</logic:iterate>
				</logic:present>

			<!--
			<p class="d">
				<a href="javascript:hidden_block(1,1);">
					<img id="i1hidden" src="/admin/skins/webjet6/images/menu-display-content.gif" alt="" />
				</a>
			</p>
			 -->

			</div>

		</div>


	</div>
</div>

<h4 class="block"><iwcm:text key="update.select_version"/></h4>

<logic:present name="updateArrayListVersions">
	<form action="update.do" method="post" onsubmit="return false;"><%=org.apache.struts.taglib.html.FormTag.renderToken(session)%>
		<table border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td align="right" style="padding-bottom: 4px;">
					<input type="hidden" name="action" value="GetArchive" />
					<input type="button" onclick="bSubmitVersionsClick(this);" name="bSubmitGroups" value="<iwcm:text key="update.doUpdate"/>" class="button100" />
				</td>
			</tr>
			<tr>
				<td>
					<div id="noteDiv" style="display:none; font-weight: bold;"></div>

					<display:table id="version" name="updateArrayListVersions" class="sort_table" pagesize="30" cellspacing="0" cellpadding="1" export="false" length="6">
		 				<display:column style="vertical-align: top;">
		 					<input type="radio" name="version" value="<jsp:getProperty name="version" property="version"/>" />
		 				</display:column>
		 				<display:column style="vertical-align: top;" titleKey="update.version" property="version"></display:column>
		 				<display:column titleKey="update.note" property="noteHTML"></display:column>
		 			</display:table>
				</td>
			</tr>
		</table>
	</form>
</logic:present>

<logic:present name="versionsEmpty">
	<iwcm:text key="update.versions_empty"/>
</logic:present>

<%
	if (Constants.getBoolean("updateAllowFileUpload")==true)
	{
%>
		<hr />
		<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
		<strong><iwcm:text key="update.update_from_file"/>:</strong>

		<html:form action="/admin/update/update.do" name="xlsImportForm" type="sk.iway.iwcm.xls.ImportXLSForm" enctype="multipart/form-data" onsubmit="showUploadProgressBar(this.file)">
			<table>
				<tr>
					<td><iwcm:text key="components.tips.select_file"/>:</td>
					<td><html:file property="file" styleClass="input"/></td>
				</tr>
				<tr>
					<td></td>
					<td align="right">
						<input type="submit" class="button100" value="<iwcm:text key="update.doUpdate"/>" />
					</td>
				</tr>
			</table>
			<input type="hidden" name="action" value="GetArchive" />
			<input type="hidden" name="version" value="upload" />
		</html:form>
<%
	}
%>

</div>


<%@ include file="../layout_bottom.jsp" %>