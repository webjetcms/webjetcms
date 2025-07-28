<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.doc.SearchAction"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ 
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%
String data = (String) request.getAttribute("doc_data");
String separator = "!CONTENT-BLOCK-SEPARATOR!";

Pattern p = Pattern.compile("!INCLUDE(.+content-block.jsp.+)!");
Matcher m = p.matcher(data);

List<String> groups = new ArrayList<String>();
String newData = data;
while (m.find()) {
	String g = m.group();
	groups.add(g.toString());
	newData = newData.replace(g, separator);
}

if(groups.size() > 0) 
{
	//out.println("matches");
	int startIndex = 0;
	String plainData = SearchAction.htmlToPlain(newData).trim();
	boolean isIncludeFirst = (plainData.indexOf(separator) == 0);
	String[] newDataArray = newData.split(separator);
	
	String finalContent = "";
	
	for(int i = 0; i < groups.size(); i++) 
	{
		groups.set(i, groups.get(i).replace("content-block.jsp", "content-block.jsp, attrName=contentBlock" + i));
		request.setAttribute("contentBlock" + i, newDataArray[i + 1]);
	}
	finalContent = Tools.join(groups.toArray(new String[groups.size()]), " ");
	
	if(isIncludeFirst) {
		//out.println("include is first");
		request.setAttribute("article", finalContent);
		%>
		<iwcm:write name="article"/>
		<%
	} 
	else 
	{
		//out.println("include is not first and show menu");
		%>
		<div class="container content">
			<div class="bg row">
				<div class="col-lg-3 col-sm-3 leftNav">
						<iwcm:write name="doc_right_menu"/>
				</div>
				<div class="col-lg-9 col-sm-9">
					<article>
						<% request.setAttribute("article", newDataArray[0]); %>
        				<iwcm:write name="article"/>
					</article>						
				</div>
			</div>
		</div>
		<% request.setAttribute("article", finalContent); %>
		<iwcm:write name="article"/>
		<%
	}
	
}
else 
{
	//out.println("not matches");
	%>
	<div class="container content">
		<div class="bg row">
			<div class="col-lg-3 col-sm-3 leftNav">
					<iwcm:write name="doc_right_menu"/>
			</div>
			<div class="col-lg-9 col-sm-9">
			
				<article>
						<iwcm:write name="doc_data"/>
				</article>
				
			</div>
		</div>
	</div>
	<%
}
%>