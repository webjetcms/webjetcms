<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

%>
<%

if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
int range = pageParams.getIntValue("range", 10);
DocDB docDB = DocDB.getInstance();

int docsLength = pageParams.getIntValue("docsLength", 10);
int period = pageParams.getIntValue("period", 7);	
List topDocIds = RatingService.getDocIdTopList(docsLength, period);
if (topDocIds.size() > 0)
{		
	request.setAttribute("docIdTopList", topDocIds);
}	

%>

<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.rating.RatingService"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<div class="rating">	
	<!--  TOP STRANKY -->
	
	<iwcm:present name="docIdTopList">
	
		<table style="width:400px;">
			 <thead>
			   <tr>
				 <th style="width:30px;">Rank</th>
				 <th style="width:270px;">Doc title</th>
				 <th style="width:50px;">Activity</th>
				 <th style="width:50px;">Rating</th>
			   </tr>
			 </thead>
			 <tbody>
				 <iwcm:iterate name="docIdTopList" id="d" type="sk.iway.iwcm.components.rating.jpa.RatingEntity" indexId="index" >
							<% DocDetails docDet = docDB.getDoc(d.getDocId(), -1, false);
								 if (docDet != null)
								 {%>
						<tr>
								<td><%=index.intValue()+1%>.&nbsp;</td>
								<td><%=docDet.getTitle()%></td>
								<td><bean:write name="d" property="ratingStat"/></td>
								<td style="display: contents;"> <%= "" + d.getRatingValueDouble() %>  / <%= "" + range%>  </td>
						</tr>
						<%}%>	
					</iwcm:iterate>  
			 </tbody>
		</table>
		
	</iwcm:present>
	
</div>
		
	<%		
		request.removeAttribute("docIdTopList");
	%>
