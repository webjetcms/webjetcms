
<%@page import="sk.iway.iwcm.system.fulltext.FulltextSearch"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
page import="java.util.List"%><%@
page import="java.util.Comparator"%><%@
page import="java.util.Collections"%><%@
page import="java.util.Collection"%><%@
page import="org.json.JSONArray"%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*"
%><%
if (Constants.getBoolean("luceneAsDefaultSearch")==false)
{
	// Lucene search is not enabled
	return;
}

String term = Tools.getRequestParameterUnsafe(request, "words");
String lang = Tools.getRequestParameter(request, "lang");

if (Tools.isEmpty(term))
{
	out.println("Missing parameter term.");
}

if (Tools.isEmpty(lang))
{
	out.println("Missing parameter lang.");
}


Object suggestions = FulltextSearch.suggestSimilar(term,lang);
JSONArray keys = suggestions == null ? new JSONArray() : new JSONArray(suggestions);
%><%=keys.toString() %>
