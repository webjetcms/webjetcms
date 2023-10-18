<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop,java.util.Set,java.util.HashSet,sk.iway.iwcm.doc.GroupDetails,sk.iway.iwcm.doc.GroupsDB,sk.iway.iwcm.SendMail"%><%@page
	import="sk.iway.iwcm.doc.DocDB"%><%@
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null)
{
	out.print(prop.getText("error.userNotLogged"));
	return;
}

request.setAttribute("cmpName", "report_popup");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<%
	String actualPage = Tools.getStringValue(Tools.getRequestParameter(request, "actualPage"), "");

	if(Tools.getBooleanValue(Tools.getRequestParameter(request, "send"), false))
	{
	 	String question = Tools.getStringValue(Tools.getRequestParameter(request, "question"),"");
	 	String info = Tools.getStringValue(Tools.getRequestParameter(request, "info"),"");
		if(SendMail.send(user.getFullName(), user.getEmail(), Constants.getString("problemReportEmail"), prop.getText("inlineToolbar.report.subject"),"<h2>Info</h2> IP: "+Tools.getRemoteIP(request)+"<br>"+info+"<h2>Text</h2>"+question)){
			%>
			<script>
			window.close();
			</script>
			<%
		}else{
			%>
			<script>
			alert('<%=prop.getText("inlineToolbar.report.error")%>');
			</script>
			<%
		}

	}


%>


<script type="text/javascript">


function Ok(){

$("input[type='submit']").trigger( "click" );
}



</script>
	<div class="padding10">
	<br>

		<script type='text/javascript' src='/components/form/check_form.js'></script>
		<form method="post" action="/admin/inline/form_report_problem.jsp?send=true" name="formMailForm">
		        <input  type="hidden" name="info" value="<%=Tools.escapeHtml(request.getHeader("User-Agent"))+" - "+actualPage %>">

		    <table>
		        <tbody>

		            <tr>
		                <td style="vertical-align: top;"><label for="question"><iwcm:text key="inlineToolbar.report.problem"/>:</label></td>
		                <td><textarea class="required" id="question" name="question" rows="10" cols="55"></textarea>

		                </td>
		            </tr>
		            <tr>
		                <td></td>
		                <td>


		                <input   type="hidden" name="subject" value="Inline toolbar report">

		                <input style="display:none;" type="submit" value="Odoslať" class="btn green" /></td>
		            </tr>
		        </tbody>
		    </table>
		</form>

	</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
