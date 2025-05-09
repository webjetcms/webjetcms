<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/>

<html>
<head>
<title><iwcm:text key="approve.page.title"/></title>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
<%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");

	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user!=null)
	{
		request.setAttribute("user", "user");
	}

	int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"), -1);
	int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
	if (historyId>0)
	{
		DocDB docDB = DocDB.getInstance();
		DocDetails doc = docDB.getDoc(docId);
		if (doc != null)
		{
		   doc = docDB.getDoc(-1, historyId);
			if (doc==null)
			{
				request.setAttribute("allreadyRejected", "true");
			}

		}
		else
		{
			request.setAttribute("allreadyDeleted", "true");
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
</head>
<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">

<div class="box_toggle">
	<div class="toggle_content" style="padding-top: 5px;">
		<div id="tabMenu1" style="height: 100px;">

			<iwcm:present name="allreadyRejected">
				<span class="error">
					<iwcm:text key="approve.del.allready_rejected"/>:
				</span>
			</iwcm:present>


			<iwcm:notPresent name="allreadyRejected">

				<iwcm:present name="allreadyDeleted">
					<span class="error"><iwcm:text key="approve.del.allready_deleted"/>.</span>
					<br><br><br><br><br><br>
				</iwcm:present>

				<iwcm:notPresent name="wrongApproveId">
					<form action="approvedel.do" method="post">
					<table border=0 align="left">
						<tr>
							<td rowspan=2 valign="top"><font color="red"><iway:request name="message"/></font></td>
							<iwcm:notPresent name="user">
							<td><iwcm:text key="user.Name"/></td>
							<td><iwcm:text key="user.password"/></td>
							</iwcm:notPresent>
							<td>&nbsp</td>
							<td><img src="images/dot.gif" width=20 height=10></td>
							<td rowspan=2 valign="top"><iwcm:text key="approve.reject.notes"/>:</td>
							<td rowspan=2><textarea name="notes" rows=3 cols=70 class="input" style="background-color: white;"><%=notes%></textarea></td>
							<td>&nbsp;</td>
						</tr>
						<tr>
							<iwcm:notPresent name="user">
							<td><input type="text" name="username" size=10 class="input"></td>
							<td><input type="password" name="password" size=10 class="input"></td>
							</iwcm:notPresent>
							<td><input type="hidden" name="historyid" value="<%=Tools.getRequestParameter(request, "historyid")%>">
							    <input type="hidden" name="docid" value="<%=Tools.getRequestParameter(request, "docid")%>">
							</td>
							<td>&nbsp;</td>
							<td>
								<input type="submit" name="schval" value="<iwcm:text key="approve.page.delete"/>" class="btn green">
								<input type="submit" name="zamietni" value="<iwcm:text key="approve.page.reject"/>" class="btn red">
							</td>
						</tr>
					</table>
					<%=org.apache.struts.taglib.html.FormTag.renderToken(session)%>
					</form>
				</iwcm:notPresent>

			</iwcm:notPresent>
		</div>
	</div>
</div>

</body>
</html>
