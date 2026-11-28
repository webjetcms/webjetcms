<%@ page pageEncoding="utf-8" contentType="application/json; charset=UTF-8"
	import="org.json.JSONArray, sk.iway.iwcm.Constants, sk.iway.iwcm.Tools, sk.iway.iwcm.system.fulltext.FulltextSearch" %><%
String term = Tools.getRequestParameterUnsafe(request, "words");
String lang = Tools.getRequestParameter(request, "lang");

if (Tools.isEmpty(term) || Tools.isEmpty(lang) || Constants.getBoolean("luceneAsDefaultSearch") == false)
{
	out.print("[]");
	return;
}

String[] suggestions = FulltextSearch.suggestSimilar(term, lang);
out.print(suggestions == null ? new JSONArray() : new JSONArray(suggestions));
%>
