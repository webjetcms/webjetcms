<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.system.fulltext.indexed.*"%>
<%@page import="sk.iway.iwcm.system.fulltext.FulltextSearch"%>
<%@page import="org.apache.lucene.document.Document"%>
<%@page import="sk.iway.iwcm.system.fulltext.LuceneQuery"%>
<%!
String selected(String value,HttpServletRequest request){
	return value.equals(Tools.getRequestParameter(request, "indexed")) ? "selected = 'selected' " : "";
}
%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%@ include file="/admin/layout_top.jsp" %>
<%=Tools.insertJQuery(request) %>
<%
StopWatch watch = new StopWatch();
watch.start();
String indexedNane = Tools.getRequestParameter(request, "indexed");
Indexed indexed = null;
boolean index = Tools.getRequestParameter(request, "index") != null;

if ("documents".equals(indexedNane))
{
	String lng = Tools.getRequestParameter(request, "lng");
	if (Tools.isEmpty(lng))
	{
		lng = "sk";
	}

	if (index)
	{
		try
		{
			Documents docs = new sk.iway.iwcm.system.fulltext.indexed.Documents(lng);
			FulltextSearch.index(docs, out);
			IndexSearcherBuilder.refresh();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			out.println("CHYBA: "+ex.getMessage());
		}
	}
	else
	{
		indexed = new Documents(lng);
	}

}
else if ("forums".equals(indexedNane))
{
	String lng = Tools.getRequestParameter(request, "lng");
	if (Tools.isEmpty(lng)){
		lng = "sk";
	}

	if (index)
	{
		try
		{
			Forums forums = new Forums();
			FulltextSearch.index(forums, out);
			IndexSearcherBuilder.refresh();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			out.println("CHYBA: "+ex.getMessage());
		}
	}
	else
	{
	  indexed = new Forums();
	}
}
else
{
	if (index)
	{
		FulltextSearch.index(null, out);
		IndexSearcherBuilder.refresh();
	}
}
%>
<%@page import="org.apache.commons.lang.time.StopWatch"%>
<form action="<%=PathFilter.getOrigPath(request)%>" method="post">
	<p>
	<label>Miesto</label>
	<select name="indexed" onchange="changeIndexed();" id="indexed">
		<option value="all" <%=selected("all",request)%>>Všetko</option>
		<option value="documents" <%=selected("documents",request)%>>Stránky</option>
		<option value="forums" <%=selected("forums",request)%>>Fóra</option>
	</select>
	</p>
	<p>
	<div id="lang_div" style="display:none;">
		<label for="lng">Jazyk</label>
		<select name="lng" id="lng">
			<option value="sk">sk</option>
			<option value="en">en</option>
			<option value="cz">cz</option>
		</select>
	</div>
	</p>

	<p>
	<label for="query">Výraz</label>
	<textarea name="query" id="query" cols="90" rows="5">${param.query}</textarea>
	</p>

	<p>
	<input type="submit" name="search" value="Hľadať" class="button50">
	<input type="submit" name="index" value="Indexovať" class="button50">
	</p>


</form>
<%
if (Tools.isNotEmpty(Tools.getRequestParameterUnsafe(request, "query")) && Tools.isNotEmpty(Tools.getRequestParameter(request, "search")) && indexed!=null)
{
	LuceneQuery query = new LuceneQuery(indexed);

	String queryString = Tools.getRequestParameterUnsafe(request, "query");
	//queryString = "+group_id:"+NumericUtils.intToPrefixCoded(250);

	out.println("Testing query: "+queryString+ " i="+indexed.numberOfDocuments()+"<br/>");

	List<Document> docs = query.documents(queryString);
	out.println("Size: "+docs.size()+"<br/>");

	out.println("<ul>");
	DocDB docDB = DocDB.getInstance();
	for(Document document:docs)
	{
		DocDetails doc = null;
		int docId = Tools.getIntValue(document.getFieldable("doc_id").stringValue(), -1);
		if (docId > 0)
		{
			doc = docDB.getBasicDocDetails(docId, false);
		}
		if (doc == null) doc = new DocDetails();

		out.println("<li>" + document.getFieldable("doc_id").stringValue() + " author_id=" + document.getFieldable("author_id").stringValue() + " title=" + doc.getTitle() + " ["+document.getFieldable("title").stringValue()+"]" + " groupid=" + doc.getGroupId() + " groups="+document.getFieldable("password_protected").stringValue() +" " +DB.prepareString(document.getFieldable(indexed.defaultField()).stringValue(), 100)+"</li>");
	}
	out.println("</ul>");

	//IndexSearcher searcher = new IndexSearcher(indexed);
}
%>
<script type="text/javascript">
<!--
$(document).ready(function(){
	changeIndexed();
});

function changeIndexed(){
	if ($('#indexed').val() == "documents" || $('#indexed').val() == "forums"  ){
		$('#lang_div').show();
	}
	else
	{
		$('#lang_div').hide();
	}
}
//-->
</script>
Elapsed time:<%=watch.getTime()%> ms
<%@ include file="/admin/layout_bottom.jsp" %>
