<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>
<%@ page import="sk.iway.iwcm.i18n.Prop"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String perpage = pageParams.getValue("perpage", "10");
String rootGroup = pageParams.getValue("rootGroup", "1");
String orderType = pageParams.getValue("orderType", "sort_priority");
String order = pageParams.getValue("order", "asc");
String searchType = pageParams.getValue("sForm", "complete");
String buttonText = pageParams.getValue("buttonText", Prop.getInstance(request).getText("components.search.search") );
String inputText = "";
String placeholder = pageParams.getValue("inputText", Prop.getInstance(request).getText("components.search.title") );
String engine = pageParams.getValue("engine", "db"); // can be "db" or "lucene"

//vycistenie requestu, inak ak by stranka pre vyhladavanie mala nastavene hodnoty, pouzili by sa (naprp. perex)
for (String name : SearchTools.getCheckInputParams())
{
	String value = (String)request.getAttribute(name);
	if (Tools.isNotEmpty(value))
	{
		request.setAttribute("preserve_"+name, value);
		request.removeAttribute(name);
	}
}


if (Tools.getRequestParameter(request, "words") != null)
{
	inputText = Tools.getRequestParameter(request, "words");
	if (Tools.isNotEmpty(inputText))
	{
		//nebezpecne znaky odpaz
		if (Tools.isNotEmpty(Constants.getString("searchActionOmitCharacters")))
		{
			inputText = inputText.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", " ");
		}
		//inputText = Tools.replace(inputText, "\"", "");
		//inputText = Tools.replace(inputText, "'", "");
		inputText = ResponseUtils.filter(inputText);
	}
}

int resultsDocId = pageParams.getIntValue("resultsDocId", 0);

request.setAttribute("perpage", perpage);
request.setAttribute("rootGroup", rootGroup);
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

if (resultsDocId == 0 && session.getAttribute("last_doc_id")!=null)
{
	resultsDocId = ((Integer)session.getAttribute("last_doc_id")).intValue();
	//out.println(session.getAttribute("last_doc_id"));
}

DocDB docDB = DocDB.getInstance();
%><%
if (searchType != null)
{
	if ("form".equals(searchType) || "complete".equals(searchType))
	{
		String url = "/showdoc.do";
		if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML) url = DocDB.getURLFromDocId(resultsDocId, request);
%>


<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@ page import="sk.iway.iwcm.system.stripes.CSRF" %>
<%@ page import="sk.iway.iwcm.common.SearchTools" %>
<form class="smallSearchForm" action="<%=url%>" method="get">
		<p>
			<% if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID) { %><input type="hidden" name="docid" value="<%=resultsDocId%>" /><% } %>
			<input class="smallSearchInput" type="text" name="words" size="25" value="<%=inputText %>" placeholder="<%=ResponseUtils.filter(placeholder) %>" id="searchWords" maxlength="512" />
			<input class="smallSearchSubmit" type="submit" value="<%=buttonText %>" />
			<%=CSRF.getCsrfTokenInputFiled(request.getSession(), false)%>
		</p>
	</form>

	<%=Tools.insertJQuery(request) %>

	<iwcm:script type="text/javascript">
	$(document).ready(function(){
    	$("#searchWords").focus(function () {
			<%
				//nechceme token v URL vysledkov vyhladavania, je tu kvoli hlaseniu pentestov a false positive
			%>
			var form = this.form;
			setTimeout( function() {	$(form.elements["__token"]).remove(); }, 1000 );
    	});
	});
	</iwcm:script>

<%
}


	if ("results".equals(searchType) || "complete".equals(searchType))
	{

		if ( Tools.getRequestParameter(request, "words") != null || SearchAction.hasInputParams(request) || Tools.getRequestParameter(request, "text")!=null)
		{
			String ret = "";

			if ("db".equals(engine)){
				ret = SearchAction.search(request, response);
			}
			else
			{
				ret = LuceneSearchAction.search(request);
			}

			String newNextHref = "";
			String newPrevHref = "";

			if(String.valueOf(request.getAttribute("nextHref")).startsWith("search.do"))
			{
				newNextHref	= String.valueOf(request.getAttribute("nextHref"));
				newNextHref = PathFilter.getOrigPath(request)+newNextHref.substring(newNextHref.indexOf('?'));
				if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID) newNextHref += "&amp;docid=" +session.getAttribute("last_doc_id");
			}

			if(String.valueOf(request.getAttribute("prevHref")).startsWith("search.do"))
			{
				newPrevHref	= String.valueOf(request.getAttribute("prevHref"));
				newPrevHref = PathFilter.getOrigPath(request) + newPrevHref.substring(newPrevHref.indexOf('?'));
				if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID) newPrevHref += "&amp;docid=" +session.getAttribute("last_doc_id");
			}

			if(String.valueOf(request.getAttribute("nextHref")).startsWith("lucene_search.do"))
			{
				newNextHref	= String.valueOf(request.getAttribute("nextHref"));
				newNextHref = PathFilter.getOrigPath(request)+newNextHref.substring(newNextHref.indexOf('?'));
				if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID) newNextHref += "&amp;docid=" +session.getAttribute("last_doc_id");
			}

			if(String.valueOf(request.getAttribute("prevHref")).startsWith("lucene_search.do"))
			{
				newPrevHref	= String.valueOf(request.getAttribute("prevHref"));
				newPrevHref = PathFilter.getOrigPath(request) + newPrevHref.substring(newPrevHref.indexOf('?'));
				if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID) newPrevHref += "&amp;docid=" +session.getAttribute("last_doc_id");
			}
%>

 <h1 class="searchResultsH1"><iwcm:text key="components.search.search_results"/></h1>

 <div class="search">
	<iwcm:present name="totalResults">
		<p class="totalResults">
			<iwcm:text key="components.search.number_of_found_results"/>: <iway:request name="totalResults"/>
		</p>
	</iwcm:present>

  	<!-- VYSLEDKY VYHLADAVANIA -->

	<iwcm:present name="aList">
		<logic:iterate id="search" name="aList" type="sk.iway.iwcm.doc.SearchDetails">
				<p>
					<a href="<%=docDB.getDocLink(search.getDocId(), search.getExternalLink(), request)%>">
						<strong><jsp:getProperty name="search" property="title"/></strong>
					</a>
				</p>
			<dl>
				<dt>
					<jsp:getProperty name="search" property="data"/>
				</dt>
				<dd>
					<jsp:getProperty name="search" property="link"/>
				</dd>
			</dl>
		</logic:iterate>
	</iwcm:present>

	<iwcm:present name="prevHref">
		<div class="navigation">
			<a href="<%=newPrevHref%>">
			&lt;&lt;&lt; <iwcm:text key="components.search.back"/>
			</a>
		</div>
	</iwcm:present>

	<iwcm:present name="nextHref">
		<div class="navigation">
			<a href="<%=newNextHref%>">
				<iwcm:text key="components.search.next"/> &gt;&gt;&gt;
			</a>
		</div>
	</iwcm:present>

	<iwcm:present name="notfound">
		<p>
			<b>
				<iwcm:text key="components.search.no_matches_found"/>.
			</b>
		</p>
	</iwcm:present>

	<iwcm:present name="emptyrequest">
		<p>
			<b>
				<iwcm:text key="components.search.enter_search_string"/> (<iwcm:text key="components.search.min_3"/>)
			</b>
		</p>
	</iwcm:present>

	<iwcm:present name="crossHourlyLimit">
		<p>
			<strong>
				<iwcm:text key="components.search.cross_hourly_limit" param1='<%=String.valueOf(request.getAttribute("wait")) %>'/>
			</strong>
		</p>
	</iwcm:present>

	<iwcm:present name="crossTimeout">
		<p>
			<strong>
				<iwcm:text key="components.search.cross_timeout" param1='<%=String.valueOf(Constants.getInt("spamProtectionTimeout-search")) %>'/>
			</strong>
		</p>
	</iwcm:present>

	</div>
 <!-- KONIEC VYSLEDKOV VYHLADAVANIA -->

 <%
		}
	}
}

for (String name : SearchTools.getCheckInputParams())
{
	String value = (String)request.getAttribute("preserve_"+name);
	if (Tools.isNotEmpty(value))
	{
		request.setAttribute(name, value);
		request.removeAttribute("preserve_"+name);
	}
}
 %>
