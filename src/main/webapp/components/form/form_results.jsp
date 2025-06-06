<%@page import="java.util.Map"%><%@page import="java.util.List"%><%@page import="sk.iway.iwcm.form.FormResultsParser"%>
<%@page import="java.util.*"%>
<%@page import="sk.iway.iwcm.form.FormResults"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

FormResults results = new FormResults(pageParams.getValue("form_name", ""));
FormResultsParser parser = new FormResultsParser(pageParams.getValue("config", ""));
int pageSize = pageParams.getIntValue("page_size", 30);
List<Map<String, String>> sortedResults = results.results();
String sort = pageParams.getValue("sort", "");
//sort by a given criteria
if (Tools.isNotEmpty(sort) && sort.contains(";"))
{
	final int coefficient = "asc".equals(sort.split(";")[1].trim()) ? 1 : -1;
	final String sortBy = sort.split(";")[0].trim();
	Collections.sort(sortedResults, new Comparator<Map<String, String>>(){
		public int compare(Map<String, String> row1, Map<String, String> row2){
			String value1 = Tools.getStringValue(row1.get(sortBy), "");
			String value2 = Tools.getStringValue(row2.get(sortBy), "");
			return coefficient* Tools.slovakCollator.compare(value1, value2);
		}
	});
}

request.setAttribute("results", sortedResults);
%>
<c:if test="<%=results.size() == 0 %>">
	Nenašli sa žiadne záznamy.
</c:if>
<c:if test="<%=results.size() != 0 %>">
<display:table name="results" class="tabulkaStandard" pagesize="<%=pageSize%>" requestURI="<%=PathFilter.getOrigPath(request)%>" excludedParams="docid" uid="row" sort="list">
	<% for (String column : parser.getColumnNames()){ %>
		<display:column title="<%=parser.getLabelFor(column) %>" style='<%="width: "+parser.getCssWidthFor(column) %>' sortable="true">
			<%=Tools.getStringValue(((Map<String, String>)row).get(column), "") %>
		</display:column>
	 <% } %>
</display:table>
</c:if>
