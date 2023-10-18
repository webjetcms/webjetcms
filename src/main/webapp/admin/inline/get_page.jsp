<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

//pouziva sa pre true inline editaciu pre ziskanie HTML kodu povodnej stranky (kvoli INCLUDE objektom a obaleniu do article elementu)

int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docId"), -1);
String wjAppField = Tools.getRequestParameter(request, "wjAppField");

DocDetails doc = DocDB.getInstance().getDoc(docId);
if (doc != null)
{
	String data = doc.getData();
	if ("perexPre".equals(wjAppField)) data = doc.getPerex();
	else if ("title".equals(wjAppField)) data = doc.getTitle();

	/*
	if (data.indexOf("!INCLUDE(")!=-1)
	{
		data = Tools.replace(data, "!INCLUDE(", "<article>!INCLUDE(");
		data = Tools.replace(data, ")!", ")!</article>");
	}
	*/

	out.println(data);
}
%>
