<%@ page import="java.io.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<iwcm:checkLogon admin="true"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>

<%

/*Runtime runtime = Runtime.getRuntime();
Process process = runtime.exec("net restart \"IBMWAS5Service - Express51\"");
int exitValue = process.waitFor();
System.out.println("exit === "+ exitValue);*/

out.println("restarting");

/*
String command = "net restart \"IBMWAS5Service - Express51\"";
Process p = Runtime.getRuntime().exec(command);
BufferedReader ls_in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
String ls_str;
try
{
	while ( (ls_str = ls_in.readLine()) != null)
	{
		out.println(ls_str);
	}
}
catch (IOException e) {
   out.println(e.getMessage());
	sk.iway.iwcm.Logger.error(e);
}
int rt = p.waitFor();
*/

%>
<br>done...