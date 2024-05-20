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
<%@page import="org.apache.struts.action.ActionMessages"%>
<%@page import="java.util.Iterator"%>
<%@page import="org.apache.struts.action.ActionMessage"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.database.SimpleQuery" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
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

					UserDetails user = UsersDB.getUser(login);

					AdminlogBean log = new ComplexQuery().
						setSql("SELECT * FROM "+ConfDB.ADMINLOG_TABLE_NAME+" WHERE log_type=? AND user_id = ? AND sub_id1 = ?").
						setParams(Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), Integer.valueOf(auth)).
					 	singleResult(new Mapper<AdminlogBean>(){;
							public AdminlogBean map(ResultSet rs) throws SQLException{
								return new AdminlogBean(rs);
							}
					});


					long timeAskedFor = log.getCreateDate().getTime();
					long timeNow = System.currentTimeMillis();
					long validity = Constants.getInt("passwordResetValidityInMinutes")*60L*1000L;

					if (timeNow - timeAskedFor > validity)
					{
						error(out, prop.getText("logon.password.reset_no_longer_valid"));
						return;
					}

					if (Tools.isNotEmpty(request.getParameter("newPassword")))
					{
						String newPassword = request.getParameter("newPassword");
						String retypePassword = request.getParameter("retypePassword");
						ActionMessages errors = new ActionMessages();

						if (!newPassword.equals(retypePassword))
						{
							error(out, prop.getText("logon.password.passwords_not_the_same"));
						}
						else if(!Password.checkPassword(true, newPassword, user.isAdmin(), user.getUserId(), session, errors))
						{
							request.setAttribute("errors", errors);
						}
						else
						{
							if (Constants.getBoolean("passwordUseHash") && Tools.isEmpty(user.getSalt()))
							{
								user.setSalt(PasswordSecurity.generateSalt());
							}
							user.setPassword(newPassword);
							UsersDB.saveUser(user);
							request.setAttribute("success", true);
							//zmaz zaznam z audit tabulky (aby druhy krat linka nefungovala)
							new SimpleQuery().execute("DELETE FROM "+ConfDB.ADMINLOG_TABLE_NAME+" WHERE log_type=? AND user_id=? AND sub_id1=?", Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), Integer.valueOf(auth).intValue());
						}
					}
				%>
				<c:if test="${success}">
					<p><strong><iwcm:text key="logon.password.change_successful" /></strong></p>
				</c:if>
				<logic:present name="errors">
				<%-- --------DUPLICATED IN logon.jsp--------%>
					<%
						String constStr = "";
						if(user != null && user.isAdmin()) constStr = "Admin";
					%>
					<p>
						<iwcm:text key="logon.change_password.nesplna_nastavenia"/><br/>
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
					</p>
				</logic:present>
				<c:if test="${!success}">

					<form action="<%=PathFilter.getOrigPath(request)%>" method="post">
						<table border="0" cellpadding="2" style="width: 100%">
							<tr>
								<td><div class="form-group"><label class="control-label"><iwcm:text key="user.login"/>:</label></div></td>
								<td><div class="form-group"><%=org.apache.struts.util.ResponseUtils.filter(login)%></div></td>
							</tr>
							<tr>
								<td><div class="form-group"><label class="control-label"><iwcm:text key="logon.password.new_password"/>:</label></div></td>
								<td>
									<div class="form-group">
										<input type="password" name="newPassword"/>
										<br>
										<span id="strength" ></span>
									</div>
								</td>
							</tr>
							<tr>
								<td><div class="form-group"><label class="control-label"><iwcm:text key="logon.password.retype_password"/>:</label></div></td>
								<td><div class="form-group"><input type="password" name="retypePassword"/></div></td>
							</tr>
							<tr>
								<td colspan="2">
									<div class="form-group text-center">
										<input type="submit" class="button btn btn-info" value="<iwcm:text key="button.submit"/>"/>
										<input type="hidden" name="login" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("login")) %>" />
										<input type="hidden" name="auth" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("auth")) %>" />
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
				</c:catch>
				<c:if test='<%=pageContext.getAttribute("exc") != null %>'>
					<%error(out, Prop.getInstance(request).getText("logon.password.invalid_parameters")); %>
				</c:if>

	</div>
</section>



<% if (includedIntoPage==false) { %>
	<jsp:include page="/components/bottom-public.jsp"/>
<% } %>
