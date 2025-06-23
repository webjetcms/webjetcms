<%@page import="java.util.List"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.SQLException"%>
<%@page import="sk.iway.iwcm.database.Mapper"%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.database.ComplexQuery"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.Password"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.PasswordSecurity"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.lang.String"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.database.SimpleQuery" %>
<%@ page import="sk.iway.iwcm.users.UserChangePasswordService" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!
	public void error(JspWriter out, String message) throws Exception
	{
		out.println("<span style='font-weight: bold; color: red'>");
		out.println(message);
		out.println("</span>");
	}
%>

<%
boolean includedIntoPage = false;
if (request.getAttribute("docDetails")!=null) includedIntoPage = true;

String lng = PageLng.getUserLng(request);

if (includedIntoPage==false)
{

   request.setAttribute("titleKey", "welcome.change_password");
   request.setAttribute("descKey", "logon.password.enter_new_password");
	%>
	<jsp:include page="/components/top-public.jsp"/>
	<style type="text/css">
		#dialogBottomButtonsRow { display: none; }
		div.panel-heading { display: none; }
		section.md-change-password { max-width: 350px; margin: 40px; }
		.text-center { text-align: center; }
	</style>
<% } %>

<section class="panel panel-default md-login md-change-password">

	<div class="panel-heading"><iwcm:text key="welcome.change_password"/></div>

	<div class="panel-body">

				<c:catch var="exc">
				<%
					/**
						Called from sendPassword() - allows user to reset his/her password

					**/
					Prop prop = Prop.getInstance(request);
					String auth = request.getParameter("auth");
					String login = request.getParameter("login");
					login = new Password().decrypt(login);
					auth = new Password().decrypt(auth);

					//Login handle
					login = sk.iway.iwcm.tags.support.ResponseUtils.filter(login);

					//Login CAN BE combination of more logins separated by UserChangePasswordService.LOGINS_SEPARATOR
					String[] logins = Tools.getTokens(login, UserChangePasswordService.LOGINS_SEPARATOR);
					request.setAttribute("logins", logins);
					//Its important use the newest login (position 0), it match created AdminlogBean
					AdminlogBean log = UserChangePasswordService.getChangePasswordAdminlogBean(logins[0], auth);

					long timeAskedFor = log.getCreateDate().getTime();
					long timeNow = System.currentTimeMillis();
					long validity = Constants.getInt("passwordResetValidityInMinutes")*60L*1000L;

					if (timeNow - timeAskedFor > validity)
					{
						error(out, prop.getText("logon.password.reset_no_longer_valid"));
						return;
					}

					UserDetails user = UsersDB.getUser(logins[0]);

					//In case user use 2nd link to end action
					if("cancelChangePasswordAction".equals(request.getParameter("act")) == true)
					{
						UserChangePasswordService.deleteChangePasswordAdminlogBean(user, auth);
					}

					String selectedLogin = request.getParameter("selectedLogin");
					if (Tools.isNotEmpty(request.getParameter("newPassword")) && Tools.isNotEmpty(selectedLogin) && UserChangePasswordService.verifyLoginValue(login, selectedLogin, auth, request))
					{
						//Do a verification that this login can be used
						//Then retrieve userToUpdate by selected login
						UserDetails userToUpdate = UsersDB.getUser(selectedLogin);

						String newPassword = request.getParameter("newPassword");
						String retypePassword = request.getParameter("retypePassword");
						java.util.List<String> errors = new java.util.ArrayList<>();

						if (!newPassword.equals(retypePassword))
						{
							error(out, prop.getText("logon.password.passwords_not_the_same"));
						}
						else if(!Password.checkPassword(true, newPassword, userToUpdate.isAdmin(), userToUpdate.getUserId(), session, errors))
						{
							request.setAttribute("errors", errors);
						}
						else
						{
							if (Constants.getBoolean("passwordUseHash") && Tools.isEmpty(user.getSalt()))
							{
								userToUpdate.setSalt(PasswordSecurity.generateSalt());
							}
							userToUpdate.setPassword(newPassword);
							UsersDB.saveUser(userToUpdate);
							request.setAttribute("success", true);

							//Here is not userToUpdate BUT user what is user on [0] position -> this one must be returned to remove AdminlogBean
							UserChangePasswordService.deleteChangePasswordAdminlogBean(user, auth);
						}
					}
				%>
				<c:if test='${"cancelChangePasswordAction" eq param.act}'>
					<p><strong><iwcm:text key="logon.change_password.action_canceled" /></strong></p>
				</c:if>
				<c:if test='${"cancelChangePasswordAction" ne param.act}'>
					<c:if test="${success}">
						<p><strong><iwcm:text key="logon.password.change_successful" /></strong></p>
					</c:if>
					<iwcm:present name="errors">
					<%-- --------DUPLICATED IN logon.jsp--------%>
						<%
							String constStr = "";
							if(user != null && user.isAdmin()) constStr = "Admin";
						%>
						<p>
							<iwcm:text key="useredit.change_password.nesplna_nastavenia"/>
						</p>
						<ul>
							<%if(Constants.getInt("password"+constStr+"MinLength") > 0){%>
								<li><iwcm:text key="logon.change_password.min_length" param1='<%=""+Constants.getInt("password"+constStr+"MinLength")%>'/>.</li>
							<%}if(Constants.getInt("password"+constStr+"MinCountOfDigits") > 0){%>
								<li><iwcm:text key="logon.change_password.count_of_digits" param1='<%=""+Constants.getInt("password"+constStr+"MinCountOfDigits")%>'/>.</li>
							<%}if(Constants.getInt("password"+constStr+"MinUpperCaseLetters") > 0){%>
								<li><iwcm:text key="logon.change_password.count_of_upper_case" param1='<%=""+Constants.getInt("password"+constStr+"MinUpperCaseLetters")%>'/>.</li>
							<%}if(Constants.getInt("password"+constStr+"MinCountOfSpecialSigns") > 0){%>
								<li><iwcm:text key="logon.change_password.count_of_special_sign" param1='<%=""+Constants.getInt("password"+constStr+"MinCountOfSpecialSigns")%>'/>.</li>
							<%}%>
								<li><iwcm:text key="logon.change_password.used_in_history2"/></li>
						</ul>
					</iwcm:present>
					<c:if test="${!success}">
						<form action="<%=PathFilter.getOrigPath(request)%>" method="post">
							<table border="0" cellpadding="2" style="width: 100%">
								<tr>
									<td><div class="form-group"><label class="control-label"><iwcm:text key="user.login"/>:</label></div></td>
									<td>
										<select class="form-group" id="selectedLogin" name="selectedLogin">
											<c:forEach items="${logins}" var="loginOption">
												<option value="${loginOption}">${loginOption}</option>
											</c:forEach>
										</select>
									</td>
								</tr>
								<tr>
									<td><div class="form-group"><label class="control-label"><iwcm:text key="logon.password.new_password"/>:</label></div></td>
									<td>
										<div class="form-group">
											<input type="password" name="newPassword" maxlengt="64"/>
											<br>
											<span id="strength" ></span>
										</div>
									</td>
								</tr>
								<tr>
									<td><div class="form-group"><label class="control-label"><iwcm:text key="logon.password.retype_password"/>:</label></div></td>
									<td><div class="form-group"><input type="password" name="retypePassword" maxlength="64"/></div></td>
								</tr>
								<tr>
									<td colspan="2">
										<div class="form-group text-center">
											<input type="submit" class="button btn btn-info" value="<iwcm:text key="button.submit"/>"/>
											<input type="hidden" name="login" value="<%=sk.iway.iwcm.tags.support.ResponseUtils.filter(request.getParameter("login")) %>" />
											<input type="hidden" name="auth" value="<%=sk.iway.iwcm.tags.support.ResponseUtils.filter(request.getParameter("auth")) %>" />
											<input type="hidden" name="language" value="<%=sk.iway.iwcm.tags.support.ResponseUtils.filter(request.getParameter("language")) %>" />
										</div>
									</td>
								</tr>
							</table>
						</form>
						<script src="/components/_common/javascript/password_strenght.js.jsp?language=<%=lng%>" type="text/javascript"></script>

						<script>
							$(document).ready(function(){
								$('input[name=newPassword]').keyup(function () {
									chceckPasswordStrenght($(this),0);
								});

							});
						</script>
					</c:if>
				</c:if>
			</c:catch>
			<c:if test='<%=pageContext.getAttribute("exc") != null %>'>
				<%error(out, Prop.getInstance(request).getText("logon.password.invalid_parameters")); %>
			</c:if>

	</div>
</section>

<% if (includedIntoPage==false) { %>
	<jsp:include page="/components/bottom-public.jsp"/>
<% } %>
