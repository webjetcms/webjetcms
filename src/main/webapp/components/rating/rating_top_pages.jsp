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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

//Zobrazi zoznam clankov zoradenych podla vysky RATING-u.
//Ak existuju clanky s rovnakym ratingom, zoradi ich podla poctu hlasujucich citatelov.

static DecimalFormat nf;
static
{
	nf = new DecimalFormat("0.00");	
}

%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

//pocet zobrazenych clankov
int docsLength = pageParams.getIntValue("docsLength", 10);
//pocet dni dozadu za ktore sa statistika berie
int period = pageParams.getIntValue("period", 21);
//minimalny pocet citatelov, ktory hodnotili clanok
int minUsers = pageParams.getIntValue("minUsers", 3);
//id adresara v ktorom sa sledovane clanky nachadzaju (je mozne zadat aj viac adresarov, napr 1+15)
String groupIds = pageParams.getValue("groupIds", null);
//ak je true, beru sa aj podadresare
boolean includeSubGroups = pageParams.getBooleanValue("includeSubGroups", true);
//ak je true sortuje sa presne, ak je false, tak zaokruhlene na celu hodnotu
boolean doubleSort = pageParams.getBooleanValue("doubleSort", true); 

List topPages = RatingDB.getTopPages(docsLength, period, minUsers, groupIds, includeSubGroups, doubleSort);
if (topPages.size() > 0)
{		
	request.setAttribute("topPages", topPages);
}
else
{
	return;
}
DocDB docDB = DocDB.getInstance();
%>
<%@page import="sk.iway.iwcm.components.rating.RatingDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="java.text.DecimalFormat"%>
<logic:present name="topPages">
	<table>
		 <thead>
		   <tr>
			 <th>Rank</th>
			 <th>Doc title</th>
			 <th>Rating</th>
			 <th>Users</th>
		   </tr>
		 </thead>
		 <tbody>
			 <logic:iterate name="topPages" id="r" type="sk.iway.iwcm.components.rating.RatingBean" indexId="index" >
					<tr>
							<td><%=index.intValue()+1%>.&nbsp;</td>
							<td><a href="<%=docDB.getDocLink(r.getDocId(), request)%>"><bean:write name="r" property="docTitle"/></a></td>
							<td><%
							if (doubleSort) out.print(nf.format(r.getRatingValueDouble()));
							else out.print(r.getRatingValue());
							%></td>
							<td><bean:write name="r" property="ratingStat"/></td>							
					</tr>
				</logic:iterate>  
		 </tbody>
	</table>
	
</logic:present>