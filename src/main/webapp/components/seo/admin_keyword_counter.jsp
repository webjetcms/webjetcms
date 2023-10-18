<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.seo.Density"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.seo.SeoTools"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_seo"/>

<script language="JavaScript">
var helpLink = "";
</script>
<%

	String data="";
	String[] keyWords=null;
	int middleSentencesCount = -1;
	int maxSentencesCount = -1;
	int middleWordsCount = -1;
	int maxWordsCount = -1;

	if(request.getParameter("data")!=null){
		data =  request.getParameter("data");
	}

	if(request.getParameter("keyWord")!=null){
		keyWords =  request.getParameter("keyWord").replace(',', ';').split(";");
	}
	if(request.getParameter("middleSentencesCount")!=null){
		middleSentencesCount =  Tools.getIntValue(request.getParameter("middleSentencesCount"), -1);
	}

	if(request.getParameter("maxSentencesCount")!=null){
		maxSentencesCount =  Tools.getIntValue(request.getParameter("maxSentencesCount"), -1);
	}

	if(request.getParameter("middleWordsCount")!=null){
		middleWordsCount =  Tools.getIntValue(request.getParameter("middleWordsCount"), -1);
	}

	if(request.getParameter("maxWordsCount")!=null){
		maxWordsCount =  Tools.getIntValue(request.getParameter("maxWordsCount"), -1);
	}

	List<Density> list = SeoTools.getKeywordDensityTable(data,keyWords);

	SeoTools.countSentences(data, middleSentencesCount, maxSentencesCount);

	SeoTools.countWords(data, middleWordsCount, maxWordsCount);

	List<String> middleWordsList = SeoTools.getMiddleWords();
	List<String> maxWordsList = SeoTools.getMaxWords();
	List<String> middleSentencesList = SeoTools.getMiddleSentences();
	List<String> maxSentencesList = SeoTools.getMaxSentences();
	double readibility = SeoTools.textReadibility();
%>
<strong><iwcm:text key="components.seo.keywords.index"/> <%=readibility %> =>
	<%if(readibility < 8){%>
	<iwcm:text key="components.seo.keywords.great_readibility"/>
	<%} else if(readibility >= 8 && readibility <= 10){ %>
	<iwcm:text key="components.seo.keywords.good_readibility"/>
	<%} else if(readibility > 10 && readibility <= 14){%>
	<iwcm:text key="components.seo.keywords.professional_readibility"/>
	<%} else if(readibility > 14 && readibility <= 20){%>
	<iwcm:text key="components.seo.keywords.scientific_readibility"/>
	<%} else {%>
	<iwcm:text key="components.seo.keywords.bad_readibility"/>
	<%} %>
	</strong>
<br/><br/>

<div>

	<display:table class="sort_table" cellspacing="0" cellpadding="0" style="sort_table"  name="<%=list %>" id="density" requestURI=""
		defaultsort="1">
		<display:column titleKey="density.keyWord" property="keyWord"
	     sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.h1Count" property="h1Count"
	     sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.h2Count" property="h2Count"
	   	sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.h3Count" property="h3Count"
	   	sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.strong" property="strong"
	   	sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.italics" property="italics"
	   	sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.link" property="link"
	   	sortable="false" escapeXml="true"/>
	   <display:column titleKey="density.allTogether" property="allTogether"
	   	sortable="false" escapeXml="true"/>
	</display:table>
	<br style="clear: both;"/>
		<strong><iwcm:text key="components.seo.keywords.middle"/>:</strong>
		<display:table cellspacing="0" cellpadding="0" name="<%=middleSentencesList %>"></display:table>
	<br style="clear: both;"/>
		<strong><iwcm:text key="components.seo.keywords.max"/>:</strong>
		<display:table cellspacing="0" cellpadding="0" name="<%=maxSentencesList %>"></display:table>
	<br style="clear: both;"/>
		<strong><iwcm:text key="components.seo.keywords.middle_word"/>:</strong>
		<display:table cellspacing="0" cellpadding="0" name="<%=middleWordsList %>" length="20" requestURI=""></display:table>
	<br style="clear: both;"/>
		<strong><iwcm:text key="components.seo.keywords.max_word"/>:</strong>
		<display:table cellspacing="0" cellpadding="0" name="<%=maxWordsList %>" length="20" requestURI=""></display:table>

</div>