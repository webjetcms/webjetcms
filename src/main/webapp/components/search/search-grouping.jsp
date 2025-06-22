<%@ page import="org.apache.commons.beanutils.BeanUtils" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%

//vysledky vyhladavania zgrupene podla nejakeho fieldu

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String perpage = pageParams.getValue("perpage", "10");
String rootGroup = pageParams.getValue("rootGroup", "1");
String orderType = pageParams.getValue("orderType", "sort_priority");
String order = pageParams.getValue("order", "asc");
String searchType = pageParams.getValue("sForm", "complete");
int resultsDocId = pageParams.getIntValue("resultsDocId", 0);

request.setAttribute("perpage", perpage);
//request.setAttribute("rootGroup", rootGroup);
request.setAttribute("orderType", orderType);
request.setAttribute("order", order);

int i;
for (i=2; i<=5; i++)
{
   orderType = pageParams.getValue("orderType"+i, null);
   if (Tools.isNotEmpty(orderType))
   {
      order = pageParams.getValue("order"+i, "asc");
      request.setAttribute("orderType"+i, orderType);
		request.setAttribute("order"+i, order);
   }
}

if (resultsDocId == 0)
	resultsDocId = ((Integer)session.getAttribute("last_doc_id")).intValue();
	//out.println(session.getAttribute("last_doc_id"));


String groupingField = pageParams.getValue("groupingField", null);
String actualGroupingValue = null;
String lastGroupingValue = null;

String words = Tools.getRequestParameter(request, "words");
if (Tools.isEmpty(words))
{
   words = pageParams.getValue("words", null);
   if (words != null)
   {
   	request.setAttribute("words", words);
   }
}

if (searchType != null)
{
	if ("form".equals(searchType) || "complete".equals(searchType))
	{
%>

<form name="textForm" method="get">

<table width="100%">
	<tr>
		<td valign="center">
				<input type="hidden" name="docid" value="<%=resultsDocId%>">
				<input type="text" name="words" size="25">
				<input type="submit" value="<iwcm:text key="components.search.search"/>">
		</td>
	</tr>
</table>

</form>

<%
}


	if ("results".equals(searchType) || "complete".equals(searchType))
	{

		if ( words != null )
		{
			String ret = sk.iway.iwcm.doc.SearchAction.search(request, response);

			String newNextHref = "";
			String newPrevHref = "";

			if(String.valueOf(request.getAttribute("nextHref")).startsWith("search.do"))
			{
				newNextHref	= String.valueOf(request.getAttribute("nextHref"));
				newNextHref = "/showdoc.do" + newNextHref.substring(newNextHref.indexOf('?'));
				newNextHref= newNextHref + "&docid=" +session.getAttribute("last_doc_id");
			}

			if(String.valueOf(request.getAttribute("prevHref")).startsWith("search.do"))
			{
				newPrevHref	= String.valueOf(request.getAttribute("prevHref"));
				newPrevHref = newPrevHref.substring(newPrevHref.indexOf('?'));
				newPrevHref = "/showdoc.do" + newPrevHref + "&docid=" +session.getAttribute("last_doc_id");
			}

%>

 <br>
 <h2><iwcm:text key="components.search.search_results"/></h2>

	<iwcm:present name="totalResults">
		<div align=right>
			<iwcm:text key="components.search.number_of_found_results"/>: <iway:request name="totalResults"/>
			<br><br>
		</div>
	</iwcm:present>

  <!-- VYSLEDKY VYHLADAVANIA -->
  <iwcm:present name="aList">
			<iwcm:iterate id="search" name="aList" type="sk.iway.iwcm.doc.SearchDetails">
			   <%
			   if (Tools.isNotEmpty(groupingField))
			   {
			      actualGroupingValue = BeanUtils.getProperty(search, groupingField);
			      System.out.println("act="+actualGroupingValue+" old="+lastGroupingValue);
			      if (actualGroupingValue != null && actualGroupingValue.equals(lastGroupingValue)==false)
			      {
			         out.println("<h2>++++++"+actualGroupingValue+"+++++</h2");
			         lastGroupingValue = actualGroupingValue;
			      }
			   }
			   %>
				<a href="showdoc.do?docid=<jsp:getProperty name="search" property="doc_id"/>">
				<b><jsp:getProperty name="search" property="title"/></b><br>
				</a>
				<jsp:getProperty name="search" property="data"/>
				<br>
				<jsp:getProperty name="search" property="link"/>
				<hr>
			</iwcm:iterate>
   </iwcm:present>

		<p align=right>
		  <iwcm:present name="prevHref">
			 <b><a href="<%=newPrevHref%>">&lt;&lt;&lt; <iwcm:text key="components.search.back"/></a></b>
		  </iwcm:present>
		  &nbsp;&nbsp;&nbsp;
		  <iwcm:present name="nextHref">
			 <b><a href="<%=newNextHref%>"><iwcm:text key="components.search.next"/> &gt;&gt;&gt;</a></b>
		  </iwcm:present>
		</p>
		<iwcm:present name="notfound">
		  <p align="center"><b><iwcm:text key="components.search.no_matches_found"/><br>(<iwcm:text key="components.search.too_short_string"/>).</b></p>
		</iwcm:present>
		<iwcm:present name="emptyrequest">
		  <p align="center"><b><iwcm:text key="components.search.enter_search_string"/> (<iwcm:text key="components.search.min_3"/>)</b></p>
		</iwcm:present>
		<br>

 <!-- KONIEC VYSLEDKOV VYHLADAVANIA -->

 <%
		}
	}
}
 %>
