<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<%@page import="sk.iway.Password"%>
<%@page import="sk.iway.iwcm.users.PasswordSecurity"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.database.Mapper"%>
<%@page import="sk.iway.iwcm.database.ComplexQuery"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate|users.edit_admins"/>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Zahashovanie hesiel pouzivatelov</h1>
<%
/*
	IRREVERSIBLE CHANGE!!!

	for all users
		decrypt current password
		generate a new salt
		hash password with salt
		save changes
*/
if ("fix".equals(request.getParameter("act"))==false) {
	%><p><a href="?act=fix">Spustit aktualizaciu</a></p><%
} else {
	if (Constants.getBoolean("passwordUseHash")==true)
	{
		out.println("Allready converted, skipping");
		return;
	}

	List<UserDetails> allUsers = new ComplexQuery().setSql("SELECT user_id, password FROM users").
		list(new Mapper<UserDetails>(){
			public UserDetails map(ResultSet rs) throws SQLException{
				UserDetails user = new UserDetails();
				user.setUserId(rs.getInt("user_id"));
				user.setPassword(rs.getString("password"));
				return user;
			}
		}
	);


	Password decryptor = new Password();
	for(UserDetails user : allUsers)
	{
		String salt = PasswordSecurity.generateSalt();
		String password = decryptor.decrypt(user.getPassword());
		String hash = PasswordSecurity.calculateHash(password, salt);
		new SimpleQuery().execute("UPDATE users SET password = ?, password_salt = ? WHERE user_id = ?", hash, salt, user.getUserId());
		out.println("Secured password for user_id: "+user.getUserId()+"<br />");
		out.flush();
	}

	ConfDB.setName("passwordUseHash", "true");
}
%>
DONE