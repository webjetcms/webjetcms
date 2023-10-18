<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants,sk.iway.iwcm.DBPool,sk.iway.iwcm.Identity, sk.iway.iwcm.Tools, sk.iway.iwcm.doc.DocDB, sk.iway.iwcm.doc.DocDetails, sk.iway.iwcm.users.UserDetails, sk.iway.iwcm.users.UsersDB" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true"/>

<html>
<head>
<title><iwcm:text key="approve.page.title"/></title>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>"><%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");

	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user!=null)
	{
		request.setAttribute("user", "user");
	}

	int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"), -1);
	if (historyId>0)
	{
		DocDB docDB = DocDB.getInstance();
		DocDetails doc = docDB.getDoc(-1, historyId);
		if (doc != null)
		{
			// stranka je zamietnuta
			if (doc.getHistoryDisapprovedBy() > 0) {
				UserDetails approver = UsersDB.getUser(doc.getHistoryDisapprovedBy());
				if (approver != null) {
					doc.setHistoryDisapprovedByName(approver.getFullName());
					request.setAttribute("disapprovedBy", doc.getHistoryDisapprovedByName());
				}
			}

			// stranka je schvalena
			if (doc.getHistoryApprovedBy() > 0) {
				UserDetails approver = UsersDB.getUser(doc.getHistoryApprovedBy());
				if (approver != null) {
					doc.setHistoryApprovedByName(approver.getFullName());
					request.setAttribute("approvedBy", doc.getHistoryApprovedByName());
				}
			}
		}
		else
		{
			request.setAttribute("wrongApproveId", "true");
		}
	}

	String notes = Tools.getRequestParameter(request, "notes");
	if (notes == null)
	{
		notes = "";
	}
%>

	<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
	<iwcm:combine type="css" set="adminStandardCss" />
	<!--[if lt IE 7]>
	    <LINK rel='stylesheet' href='<%=request.getContextPath()%>/admin/skins/webjet6/css/webjet6_ie6.css'>
	<![endif]-->
</head>
<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">

<div class="box_toggle">
	<div class="toggle_content" style="padding-top: 5px;">
		<div id="tabMenu1" style="height: 100px;">
			<c:choose>
				<c:when test="${not empty disapprovedBy}">
					<span class="error">
						<img src="/admin/images/warning.gif" align="absmiddle"/>
						<iwcm:text key="approve.allready_disapproved"/>:
						${disapprovedBy}
					</span>
				</c:when>
				<c:when test="${not empty approvedBy}">
					<span class="error">
						<img src="/admin/images/warning.gif" align="absmiddle"/>
						<iwcm:text key="approve.allready_approved_by"/>:
						${approvedBy}
					</span>
				</c:when>
				<c:otherwise>
					<logic:present name="wrongApproveId">
						<span class="error"><img src="/admin/images/warning.gif" align="absmiddle"/> <iwcm:text key="approve.allready_approved"/>.</span>
					</logic:present>

					<logic:notPresent name="wrongApproveId">
						<form action="approve.do" method="post">
							<table border="0" align="left">
								<tr>
									<td rowspan="2" valign="top"><font color="red"><iway:request name="message"/></font></td>
									<logic:notPresent name="user">
										<td><iwcm:text key="user.Name"/></td>
										<td><iwcm:text key="user.password"/></td>
									</logic:notPresent>
									<td rowspan="2" valign="bottom">
										<input type="hidden" name="historyid" value="<%=Tools.getRequestParameter(request, "historyid")%>">
									</td>
									<td><img src="images/dot.gif" width=20 height=10></td>
									<td rowspan="2" valign="top"><iwcm:text key="approve.reject.notes"/>:</td>
									<td rowspan="2"><textarea name="notes" rows=3 cols=70 class="input" style="background-color: white;"><%=ResponseUtils.filter(notes)%></textarea></td>
									<td>&nbsp;</td>
								</tr>
								<tr>
									<logic:notPresent name="user">
										<td valign="bottom"><input type="text" name="username" size=10 class="input"></td>
										<td valign="bottom"><input type="password" name="password" size=10 class="input"></td>
									</logic:notPresent>
									<td>&nbsp;</td>
									<td>
										<input type="submit" name="schval" value="<iwcm:text key="approve.page.approve"/>" class="btn green">
										<input type="submit" name="zamietni" value="<iwcm:text key="approve.page.reject"/>" class="btn red">
									</td>
								</tr>
							</table>
							<%=org.apache.struts.taglib.html.FormTag.renderToken(session)%>
						</form>
					</logic:notPresent>
				</c:otherwise>
			</c:choose>

		</div>
	</div>
</div>

</body>
</html>