<%@page import="java.util.StringTokenizer"%>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Fixing writable_folders, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>

<%
if ("fix".equals(request.getParameter("act"))) {
	//opravi hodnoty poli writable_folders po aktualizacii WebJETu

	List<UserDetails> allUsers = new ComplexQuery().setSql("SELECT user_id, login, writable_folders FROM users WHERE is_admin=1").
		list(new Mapper<UserDetails>(){
			public UserDetails map(ResultSet rs) throws SQLException{
				UserDetails user = new UserDetails();
				user.setUserId(rs.getInt("user_id"));
				user.setLogin(rs.getString("login"));
				user.setWritableFolders(rs.getString("writable_folders"));
				return user;
			}
		}
	);


	for(UserDetails user : allUsers)
	{
		StringTokenizer st = new StringTokenizer(user.getWritableFolders(), "\n");
		String dir;

		String writableFolders = "";
		while (st.hasMoreTokens())
		{
			dir = st.nextToken().trim();
			if (Tools.isEmpty(dir)) continue;

			if (dir.endsWith("/")==false) dir = dir+"/*";
			dir = Tools.replace(dir, "+", "*");

			if (Tools.isNotEmpty(writableFolders)) writableFolders += "\n";
			writableFolders += dir;
		}

		new SimpleQuery().execute("UPDATE "+prefix+"users SET writable_folders = ? WHERE user_id = ?", writableFolders, user.getUserId());
		out.println("Updated field for user: "+user.getUserId()+" "+user.getLogin()+"<br />");
		out.println(writableFolders);
		out.println("<br/><br/>");


		out.flush();
	}
}
%>