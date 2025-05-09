<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.users.*,java.util.*"%>
<%@ page import="sk.iway.iwcm.tags.WriteTag" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
PageParams pageParams = new PageParams(request);
//TODO: joruz - pridat to do komponenty pre vkladanie
String emailLogon = pageParams.getValue("emailLogon", "false");

String smsForward = pageParams.getValue("smsForward", null);

String facebook = pageParams.getValue("facebook", "false");
String google = pageParams.getValue("google", "false");

String facebookKey = pageParams.getValue("facebook_key", "");
String facebookSecret = pageParams.getValue("facebook_secret", "");
String googleKey = pageParams.getValue("google_key", "");
String googleSecret = pageParams.getValue("google_secret", "");

if(Tools.isEmpty(facebookKey) || Tools.isEmpty(facebookSecret)) { facebook = "false"; }
if(Tools.isEmpty(googleKey) || Tools.isEmpty(googleSecret)) { google = "false"; }	//nezobrazim ikony socialnych sieti, pokial nebol zadany kluc a secure

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if(request.getParameter("loginName") != null)
{	
	//posli heslo emailom
	String loginName = org.apache.struts.util.ResponseUtils.filter(request.getParameter("loginName"));
	//nastavime URL na zaslanie hesla na aktualnu login stranku, kde potom includneme podla parametrov form na zmenu hesla
	if (pageParams.getBooleanValue("sendPasswordUrlSameAsLogin", true))
	{
		request.setAttribute("sendPasswordUrl", PathFilter.getOrigPath(request));
	}
	boolean ret = UsersDB.sendPassword(request,loginName);	
}

if(request.getParameter("login") != null && request.getParameter("auth")!=null)
{
	String customPath = WriteTag.getCustomPage("/components/user/change_password.jsp", request);
	pageContext.include(customPath);
	return;
}

	
DocDetails docDetailsOriginal = (DocDetails)request.getAttribute("docDetailsOriginal");
String submitURL = null;
String userGroupsIds = null;
int docId = -1;
if (docDetailsOriginal != null)
{
	submitURL = "/showdoc.do?docid="+docDetailsOriginal.getDocId();
	if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML)
	{
		submitURL = docDetailsOriginal.getVirtualPath();
	}
	userGroupsIds = pageParams.getValue("regToUserGroups", docDetailsOriginal.getPasswordProtected());	//nastavim skupiny z premennej, ak regToUserGroups neexistuje 
																																		//nastavim skupiny podla stranky
	session.setAttribute("groupsIds", userGroupsIds);	
	docId = docDetailsOriginal.getDocId();
}
else if (request.getAttribute("logonFormSubmitURL")!=null)
{
	//je to zaheslovany subor, submit musi ist sam na seba
	submitURL = (String)request.getAttribute("logonFormSubmitURL");
}
String socialError = Tools.getStringValue(request.getParameter("socialError"), "false");
String socialErrorRights = Tools.getStringValue(request.getParameter("socialErrorRights"), "false");
if("true".compareTo(socialError) == 0){	//ak chyba, vypisem ju
%>
	<span class='error'><iwcm:text key="logon.err.socialAuthError"/></span>
<%} 
if("true".compareTo(socialErrorRights) == 0){	//ak chyba, vypisem ju
%>
		<span class='error'><iwcm:text key="logon.err.socialAuthErrorRights"/></span>
	<%} 
%>

<section class="panel panel-default md-login">

	<div class="panel-heading"><iwcm:text key="logon.panelHeading"/></div>

	<div class="panel-body">

		<iwcm:present name="error.logon.user.unknown">
			<p class="alert alert-danger"><span class='error'><iwcm:text key="logon.err.userUnknown"/></span></p>
		</iwcm:present>

		<iwcm:present name="error.logon.user.blocked">
			<p class="alert alert-danger"><span class='error'><iwcm:text key="logon.error.blocked"/></span></p>
		</iwcm:present>

		<iwcm:present name="error.logon.wrong.pass">
			<p><span class='error'><iwcm:text key="logon.err.wrongPass"/></span></p>
			<form name="passwdSendForm" method="get" action="<%=PathFilter.getOrigPathDocId(request) %>">
				<iwcm:text key="components.user.forgot_password"/>.
				<a href="javascript:document.passwdSendForm.submit();"><iwcm:text key="components.user.send_login_info"/></a>.
				<input class="input" type="hidden" name="loginName" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("username"))%>" />
				<input type="hidden" name="docid" value="<%
				if (request.getParameter("origDocId")!=null) out.print(org.apache.struts.util.ResponseUtils.filter(request.getParameter("origDocId")));
				else out.print(org.apache.struts.util.ResponseUtils.filter(request.getParameter("docId")));
				%>" />
			</form>
			<br /><br />
		</iwcm:present>

		<iwcm:present name="error.logon.wrong.dateLoginDissabled">
			<p><span class='error'><iwcm:text key="logon.err.dateLoginDissabled" param1='<%=(String)request.getAttribute("error.logon.wrong.dateLoginDissabled-start")%>' param2='<%=(String)request.getAttribute("error.logon.wrong.dateLoginDissabled-end")%>'/></span></p>
		</iwcm:present>

		<iwcm:present name="passResultEmail">
		   <p class="alert alert-success"><iwcm:text key="logon.lost_password_send_success"/></p>
		</iwcm:present>

		<iwcm:present name="unauthorized">
			<p class="alert alert-danger"><span class='error'><iwcm:text key="components.user.logon.unauthorized"/></span></p>
		</iwcm:present>

		<iwcm:present name='<%=Constants.USER_KEY+"_changepassword"%>'>
			<%
			Identity user = (Identity)session.getAttribute(Constants.USER_KEY+"_changepassword");
			String constStr = "";
			if(user != null && user.isAdmin()) constStr = "Admin";
			%>
			<p>
				<iwcm:text key="logon.change_password.nesplna_nastavenia"/><br/>
				<%if(Constants.getInt("password"+constStr+"MinLength") > 0){%>
				   - <iwcm:text key="logon.change_password.min_length" param1='<%=String.valueOf(Constants.getInt("password"+constStr+"MinLength"))%>'/>.<br/>
			  <%}if(Constants.getInt("password"+constStr+"MinCountOfDigits") > 0){%>
				   - <iwcm:text key="logon.change_password.count_of_digits" param1='<%=String.valueOf(Constants.getInt("password"+constStr+"MinCountOfDigits"))%>'/>.<br/>
				<%}if(Constants.getInt("password"+constStr+"MinUpperCaseLetters") > 0){%>
				   - <iwcm:text key="logon.change_password.count_of_upper_case" param1='<%=String.valueOf(Constants.getInt("password"+constStr+"MinUpperCaseLetters"))%>'/>.<br/>
				<%}if(Constants.getInt("password"+constStr+"MinCountOfSpecialSigns") > 0){%>
				   - <iwcm:text key="logon.change_password.count_of_special_sign" param1='<%=String.valueOf(Constants.getInt("password"+constStr+"MinCountOfSpecialSigns"))%>'/>.<br/>
				<%}%>
			</p>
			<iwcm:present name="passwordsNotMatch">
				<p><span class='error'><iwcm:text key="logon.change_password.password_not_match"/></span></p>
			</iwcm:present>
			<form action="/usrlogon.do" method="post" name="logonForm">
				<fieldset>
				<p>
					<label for="oldpass"><b><iwcm:text key="logon.old_password"/>:</b></label><br/>
					<input type="password" name="password" size="16" maxlength="16" class="input" id="oldpass" />
				</p>
				<p>
					<label for="newpass"><b><iwcm:text key="logon.new_password"/>:</b></label><br/>
					<input type="password" name="newPassword" size="16" maxlength="16" class="input" id="newpass" />
				</p>
				<p>
					<label for="retypepass"><b><iwcm:text key="logon.retype_new_password"/>:</b></label><br/>
					<input type="password" name="retypeNewPassword" size="16" maxlength="16" class="input" id="retypepass" />
				</p>
				<p class="right">
					<label>
						<input type="submit" class="buttonLogon btn green" value="<iwcm:text key="button.submit"/>" />
						<input type="hidden" name="docId" value="<%=docId%>"/>
						<input type="hidden" name="doShowdocAction" value="/usrlogon.do" />
						<input type="hidden" name="emailLogon" value="<%=emailLogon%>" />
						<input type="hidden" name="username" value="<%=user.getLogin()%>" />
						<%
						if (Tools.isNotEmpty(smsForward)) {%><input type="hidden" name="smsForward" value="<%=smsForward%>" /><%}
						Enumeration parameters = request.getParameterNames();
						while (parameters.hasMoreElements())
						{
						   String name = (String)parameters.nextElement();
						   if ("docid".equals(name) || "docId".equals(name) || "username".equals(name) || "smsForward".equals(name) ||
							 "password".equals(name) || "doShowdocAction".equals(name) || "emailLogon".equals(name) ||
							 "newPassword".equals(name) || "retypeNewPassword".equals(name) || "origDocId".equals(name)) continue;

						   String values[] = request.getParameterValues(name);
						   for (int i=0; i<values.length; i++)
						   {
							   String value = org.apache.struts.util.ResponseUtils.filter(values[i]);
							   out.println("<input type='hidden' name='"+org.apache.struts.util.ResponseUtils.filter(name)+"' value=\""+value+"\" />");
						   }
						}
						%>
					</label>
				</p>
				</fieldset>
			</form>
		</iwcm:present>
		<iwcm:notPresent name='<%=Constants.USER_KEY+"_changepassword"%>'>

			<%
				//user je prihlaseny, ale zobrazil sa mu logon form - nema spravne prava (skupinu)
				Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
				if(user!=null)
				{%>
					<div class="row">
						<div class="col-sm-10 col-sm-offset-2">
						<div class="alert alert-danger" role="alert">
							<iwcm:text key="logon.err.unauthorized_group"/>
						</div>
						  </div>
					</div>
				<%
				}
			%>

			<form action="/usrlogon.do" method="post" name="logonForm" class="form-horizontal">
				<fieldset>
				  <div class="form-group">
					<label id="loginLabelName" for="name" class="col-md-2 control-label"><iwcm:text key="logon.name"/>:</label>
					<div class="col-md-12">
					  <input type="text" name="username" size="16" maxlength="64" class="form-control" id="name" value="<% if (request.getParameter("username")!=null) out.print(org.apache.struts.util.ResponseUtils.filter(request.getParameter("username"))); %>" />
					</div>
				  </div>
				  <div class="form-group">
					<label id="loginLabelPassword" for="pass" class="col-md-2 control-label"><iwcm:text key="logon.password"/>:</label>
					<div class="col-md-12">
					  <input type="password" name="password" size="16" maxlength="16" class="form-control" id="pass" />
					</div>
				  </div>
					<p>
					<% if(facebook.compareTo("true") == 0) { session.setAttribute("facebook_key", facebookKey); session.setAttribute("facebook_secret", facebookSecret);
					//ak je prihlasenie cez facebook aktivne, vlozim do session facebook key a secret %>
						<a href="/socialAuth.do?id=facebook&docId=<%=docId %>"><img src="/components/user/facebook_icon.png" alt="Facebook" title="Facebook" border="0"></img></a>
					<%} %></b>
					<% if(google.compareTo("true") == 0) { session.setAttribute("google_key", googleKey); session.setAttribute("google_secret", googleSecret);
					//ak je prihlasenie cez google aktivne, vlozim do session google key a secret %>
						<a href="/socialAuth.do?id=google&docId=<%=docId %>"><img src="/components/user/gmail-icon.jpg" alt="Gmail" title="Gmail" border="0"></img></a>
					<%} %>
					</p>
					<div class="form-group">
				<div class="col-sm-12 text-center">

							<input type="submit" class="btn btn-primary" value="<iwcm:text key="button.submit"/>" />
							<% if (submitURL != null)
							{
								int submitDocId = Tools.getIntValue(request.getParameter("docid"), -1);
								if (docDetailsOriginal!=null) submitDocId = docDetailsOriginal.getDocId();
								%>
								<input type="hidden" name="docId" value="<%=submitDocId %>"/>
							<% } else { %>
								<input type="hidden" name="docId" value="<%=docId%>"/>
							<% } %>
							<input type="hidden" name="doShowdocAction" value="/usrlogon.do" />
							<input type="hidden" name="emailLogon" value="<%=emailLogon%>" />
							<%
							if (Tools.isNotEmpty(smsForward)) {%><input type="hidden" name="smsForward" value="<%=smsForward%>" /><%}
							Enumeration parameters = request.getParameterNames();
							while (parameters.hasMoreElements())
							{
							   String name = (String)parameters.nextElement();
							   if ("docid".equals(name) || "docId".equals(name) || "username".equals(name) || "smsForward".equals(name) ||
								 "password".equals(name) || "doShowdocAction".equals(name) || "emailLogon".equals(name) ||
								 "newPassword".equals(name) || "retypeNewPassword".equals(name) || "loginName".equals(name)) continue;

							   String values[] = request.getParameterValues(name);
							   for (int i=0; i<values.length; i++)
							   {
								   String value = org.apache.struts.util.ResponseUtils.filter(values[i]);
								   out.println("<input type='hidden' name='"+org.apache.struts.util.ResponseUtils.filter(name)+"' value=\""+value+"\" />");
							   }
							}
							%>
						</div>
					</div>
				</fieldset>
			</form>



		</iwcm:notPresent>

	</div>

</section>

<script type="text/javascript">
<!--
<%
if (submitURL != null) 
{
	String httpsDomains = Constants.getString("formmailHttpsDomains");   	   	
  	if (Tools.isNotEmpty(httpsDomains))
  	{
  		String actualDomain = DocDB.getDomain(request);
  		
  		if ( (","+httpsDomains+",").indexOf(","+actualDomain+",")!=-1)
  		{
  			submitURL = "https://"+DocDB.getDomain(request)+submitURL; 
  		}
  	}
	
	out.println("document.logonForm.action=\""+submitURL+"\";");
	if (docDetailsOriginal!=null)
	{
		out.println("document.logonForm.docId.value=\""+docDetailsOriginal.getDocId()+"\";");
	}
	else if (request.getParameter("docid")!=null)
	{
		out.println("document.logonForm.docId.value=\""+Tools.getIntValue(request.getParameter("docid"), -1)+"\";");
	}
} %>
-->
</script>
