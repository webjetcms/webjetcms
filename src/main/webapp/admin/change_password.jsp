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
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
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
%><c:catch var="exc">
<%
	/**
		Called from sendPassword() - allows user to reset his/her password

	**/
	Prop prop = Prop.getInstance(request);
	String auth = Tools.getRequestParameterUnsafe(request, "auth");
	String login = Tools.getRequestParameterUnsafe(request, "login");
	login = new Password().decrypt(login);
	auth = new Password().decrypt(auth);

	UserDetails user = UsersDB.getUser(login);

	AdminlogBean log = new ComplexQuery().
		setSql("SELECT * FROM "+ConfDB.ADMINLOG_TABLE_NAME+" WHERE user_id = ? AND sub_id1 = ?").
		setParams(user.getUserId(), Integer.valueOf(auth)).
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

	if (Tools.isNotEmpty(Tools.getRequestParameterUnsafe(request, "newPassword")))
	{
		String newPassword = Tools.getRequestParameterUnsafe(request, "newPassword");
		String retypePassword = Tools.getRequestParameterUnsafe(request, "retypePassword");
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
			user.setPassword(PasswordSecurity.calculateHash(newPassword, user.getSalt()));
			UsersDB.saveUser(user);
			request.setAttribute("success", true);
		}
	}
%>
<c:if test="${success}">
	<iwcm:text key="logon.password.change_successful" />
</c:if>
<logic:present name="errors">
<%-- --------DUPLICATED IN logon.jsp--------%>
	<%
		String constStr = "User";
		if(user != null && user.isAdmin()) constStr = "Admin";
	%>
	<p>
		<iwcm:text key="logon.change_password.nesplna_nastavenia"/><br/>
		<%if(Constants.getInt("password"+constStr+"MinLength") > 0){%>
		   - <iwcm:text key="logon.change_password.min_length" param1='<%=""+Constants.getInt("password"+constStr+"MinLength")%>'/>.<br/>
	    <%}if(Constants.getInt("password"+constStr+"MinCountOfDigits") > 0){%>
		   - <iwcm:text key="logon.change_password.count_of_digits" param1='<%=""+Constants.getInt("password"+constStr+"MinCountOfDigits")%>'/>.<br/>
		<%}if(Constants.getInt("password"+constStr+"MinUpperCaseLetters") > 0){%>
		   - <iwcm:text key="logon.change_password.count_of_upper_case" param1='<%=""+Constants.getInt("password"+constStr+"MinUpperCaseLetters")%>'/>.<br/>
		<%}if(Constants.getInt("password"+constStr+"MinCountOfSpecialSigns") > 0){%>
		   - <iwcm:text key="logon.change_password.count_of_special_sign" param1='<%=""+Constants.getInt("password"+constStr+"MinCountOfSpecialSigns")%>'/>.<br/>
		<%}%>
	</p>
</logic:present>
<form action="/admin/change_password.jsp" method="post">
	<label><span><iwcm:text key="logon.password.new_password"/></span>: <input type="password" name="newPassword"/></label>
	<label><span><iwcm:text key="logon.password.retype_password"/></span>: <input type="password" name="retypePassword"/></label>
	<input type="submit"/>
	<input type="hidden" name="login" value="<%=Tools.getRequestParameter(request, "login") %>" />
	<input type="hidden" name="auth" value="<%=Tools.getRequestParameter(request, "auth") %>" />
</form>
</c:catch>
<c:if test='<%=pageContext.getAttribute("exc") != null %>'>
	<%error(out, Prop.getInstance(request).getText("logon.password.invalid_parameters")); %>
</c:if>
