<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<iwcm:menu notName="cmp_adminlog">
    <%
        response.sendRedirect("/admin/403.jsp");
			if (1==1) return;
    %>
</iwcm:menu>


<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>

<link rel="stylesheet" type="text/css" href="/admin/css/tablesort.css" />

<div class="row title">
    <h1 class="page-title"><i class="fa icon-list"></i><iwcm:text key="components.adminlog.adminlog"/><i class="fa fa-angle-right"></i><iwcm:text key="components.adminlog.changedWebPages"/></h1>
</div>

<iwcm:menu name="menuWebpages">

<%
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

	List admins = UsersDB.getAdmins();
	request.setAttribute("admins",admins);

	Calendar endDate = Calendar.getInstance();
	Calendar startDate = Calendar.getInstance();
	startDate.set(Calendar.MONTH, endDate.get(Calendar.MONTH)-1);

	Integer adminId = -1;
	if(Tools.isNotEmpty(request.getParameter("adminId")))
		adminId = Tools.getIntValue(request.getParameter("adminId"), -1);

	if(Tools.isNotEmpty(request.getParameter("startDate")))
		startDate.setTimeInMillis(DB.getTimestamp(request.getParameter("startDate")));
	if(Tools.isNotEmpty(request.getParameter("endDate")))
		endDate.setTimeInMillis(DB.getTimestamp(request.getParameter("endDate")));

	String startDateString = Tools.formatDate(startDate.getTimeInMillis());
	String endDateString = Tools.formatDate(endDate.getTimeInMillis());

	List recentPages = DocDB.getAllDocsFromTo(startDateString, endDateString, adminId);
	request.setAttribute("recentPages", recentPages);
%>

<script type="text/javascript" charset="windows-1250" src="/admin/scripts/dateTime.jsp"></script>
<script type="text/javascript" language="javascript" src="/admin/scripts/divpopup.js"></script>

<script type="text/javascript" language="javascript">
<!--
	helpLink = "admin/audit.jsp&book=admin";
	//toto treba zadefinovat v stranke po includnuti divpopup.js
	//je to offset o ktory sa posuva okno vlavo
	leftOffset=-445;
	//a toto ofset o ktory sa posuva nadol
	topOffset=10;

	//popup sa potom vola:
	//popupDIV(url);

	function select(value)
	{
		//none
	}

	function deleteOK(text,obj,url)
	{
		if(confirm(text))
			obj.href=url;
	}

	function popupMessage(id)
	{
		var options = "status=no,toolbar=no,scrollbars=no,resizable=yes,width=500,height=400,titlebar=no;"
		window.open("/components/messages/message_popup.jsp?equal=true&messageId="+id,"msgpop"+id,options);
	}

	function confirmDelete(id)
 	{
 		if (window.confirm("<iwcm:text key='admin.conf_editor.do_you_really_want_to_delete'/>"))
 		{
			document.actionForm.act.value="delete";
			document.actionForm.id.value=id;
			document.actionForm.submit();
 		}
	 }
//-->
</script>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.filter"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">
			<form name="settings" action="web_pages.jsp">

				<div class="col-lg-3 col-md-5 col-sm-6">
					<div class="form-group">
						<input type="hidden" name="search" value="true" />
						<label for="startDateId" class="form-label"><iwcm:text key="components.forum.od" />:</label>
						<input type="text" size="10" maxlength="10" name="startDate" id="startDateId" value="<%=startDateString %>" onblur="checkDate(document.settings.startDate);return false;" class="form-control datepicker" />
					</div>
				</div>
				<div class="col-lg-3 col-md-5 col-sm-6">
					<div class="form-group">
						<label for="endDateId" class="form-label"><iwcm:text key="components.forum.do" />:</label>
						<input type="text" size="10" maxlength="10" name="endDate" id="endDateId" value="<%=endDateString %>" onblur="checkDate(document.settings.endDate);return false;" class="form-control datepicker" />
					</div>
				</div>
				<div class="col-lg-3 col-md-5 col-sm-6">
					<div class="form-group">
						<label for="admins" class="form-label"><iwcm:text key="groupslist.approve.authorName"/>:</label>

						<select class="form-control" name="adminId" id="admins">
							<option value="-1" <%if(adminId == -1) out.println("selected=\"selected\"");%>><iwcm:text key="users.authType.all"/></option>
							<%
							for (Iterator iter = admins.iterator(); iter.hasNext();)
							{
								UserDetails userDet = (UserDetails)iter.next();
							%>
								<option value="<%=userDet.getUserId()%>" <%if(adminId == userDet.getUserId()) out.println("selected=\"selected\"");%> ><%=userDet.getFullName()%></option>
							<%
							}
							%>
						</select>
					</div>
				</div>
				<div class="col-lg-2 col-md-2 col-sm-6">
					<div class="form-group">
						<label class="form-label col-xs-12 hidden-xs" for="">&nbsp;</label>
						<input type="submit" value="<iwcm:text key="components.forum.zobrazit" />" class="btn green">
					</div>
				</div>

			</form>
		</div>
	</div>
</div>

	<logic:present name="recentPages">
	<display:table uid="doc" name="recentPages" class="sort_table" cellspacing="0" cellpadding="1" export="true" pagesize="20" requestURI="<%=PathFilter.getOrigPath(request)%>">

		<display:setProperty name="export.excel.filename" value="log.xls" />
		<display:setProperty name="export.csv.filename" value="log.csv" />
		<display:setProperty name="export.xml.filename" value="log.xml" />
		<display:setProperty name="export.pdf.filename" value="log.pdf" />

		<display:column titleKey="groupslist.approve.pageTitle">
		<%DocDetails _doc = (DocDetails)doc; %>
			<logic:notEqual name="doc" property="passwordProtected" value=""><img src='/admin/images/lock.gif'></logic:notEqual><a
			 class="groups<jsp:getProperty name="doc" property="available"/>"
			 href="/admin/editor.do?docid=<bean:write name="doc" property="docId"/>"><bean:write name="doc" property="title"/></a>
			 <logic:notEmpty name="doc" property="publishStartString">
				 <bean:write name="doc" property="publishStartString"/> <bean:write name="doc" property="publishStartTimeString"/>
			 </logic:notEmpty>

			 <logic:notEmpty name="doc" property="publishEndString">
				 <logic:notEmpty name="doc" property="publishStartString">
					 -
				 </logic:notEmpty>
				 <bean:write name="doc" property="publishEndString"/> <bean:write name="doc" property="publishEndTimeString"/>
			 </logic:notEmpty>

			 <iwcm:menu notName="editorMiniEdit">
				 (<bean:write name="doc" property="sortPriority"/>)
			 </iwcm:menu>
			 <br />
			 <%=GroupsDB.getInstance().getPathLinkForward(_doc.getGroupId(),null)%>
		</display:column>

		<display:column titleKey="groupslist.approve.date" sortable="true">
	 		&nbsp;<bean:write name="doc" property="dateCreatedString"/>
			<bean:write name="doc" property="timeCreatedString"/>&nbsp;
		</display:column>

		<display:column titleKey="groupslist.approve.authorName" sortable="true">
			 <%
			 Integer usertId = ((DocDetails)doc).getAuthorId();
			 if(usertId != null && usertId.intValue() > 0)
			 {
				UserDetails tmpUser = UsersDB.getUser(usertId);
				if(tmpUser != null)
					out.print(tmpUser.getFullName());
			 }
			 %>
		</display:column>

		<display:column titleKey="groupslist.approve.tools">
			<iwcm:menu name="cmp_stat">
				<a  href="/apps/stat/admin/top-details/?docId=<jsp:getProperty name="doc" property="docId"/>&title=<%=Tools.URLEncode(((DocDetails)doc).getTitle())%>" title='<iwcm:text key="menu.stat"/>'><span class="glyphicon glyphicon-stats" aria-hidden="true"></span></a>
			</iwcm:menu>
			 &nbsp;<a href="<%=DocDB.getInstance().getDocLink(((DocDetails)doc).getDocId())%>" target="_blank" title="<iwcm:text key="editor.preview"/>"><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span></a>
			 &nbsp;<a href='javascript:popupDIV("/admin/dochistory.jsp?docid=<jsp:getProperty name="doc" property="docId"/>")' title="<iwcm:text key="groupslist.show_history"/>"><span class="glyphicon glyphicon-time" aria-hidden="true"></span></a>
			 <span><a href="javascript:void(0)" onClick="deleteOK('<iwcm:text key="groupslist.do_you_really_want_to_delete"/>',this,'docdel.do?docid=<jsp:getProperty name="doc" property="docId"/>')" title='<iwcm:text key="button.delete"/>'><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a></span>
		</display:column>
	</display:table>
	</logic:present>

	<div id="divPopUp" style="position:absolute; width:450px; height:100px; z-index:130; left: 71px; top: 146px; visibility: hidden">
		<table width="450" bgcolor="white" cellspacing="0" cellpadding="0">
		<tr>
			<td align="left" bgcolor="#CCCCFF"><small><iwcm:text key="groupslist.web_page_history"/></small></td>
			<td align="right" bgcolor="#CCCCFF"><a href="javascript:popupHide();"><small><b>[X]</b></small></a></td>
		</tr>
		<tr>
			<td valign="top" colspan="2">
				<iframe src="/admin/divpopup-blank.jsp" name="popupIframe" style="border:solid #000000 1px" width="448" height="130" align="left" marginwidth="0" marginheight="0" frameborder="0" scrolling="auto"></iframe>
			</td>
		</tr>
		</table>
	</div>

	<br />
	<br />
</iwcm:menu>
<%@ include file="/admin/layout_bottom.jsp" %>