<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

//Zobrazi zoznam clankov zoradenych podla poctu prispevkov v diskusii.
//Ak existuju clanky s rovnakym poctom prispevkov, zoradi ich podla poctu hlasujucich citatelov.

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
//minimalny pocet prispevkov v diskusii
int minMessages = pageParams.getIntValue("minMessages", 3);
//id adresara v ktorom sa sledovane clanky nachadzaju (je mozne zadat aj viac adresarov, napr 1+15)
String groupIds = pageParams.getValue("groupIds", null);
//ak je true, beru sa aj podadresare
boolean includeSubGroups = pageParams.getBooleanValue("includeSubGroups", true);

List topPages = ForumDB.getTopForums(docsLength, period, minMessages, groupIds, includeSubGroups);
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
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="java.text.DecimalFormat"%>
<%@page import="sk.iway.iwcm.forum.ForumDB"%>
<iwcm:present name="topPages">
	<table>
		 <thead>
		   <tr>
			 <th>Rank</th>
			 <th>Doc title</th>
			 <th>Messages</th>
		   </tr>
		 </thead>
		 <tbody>
			 <iwcm:iterate name="topPages" id="r" type="sk.iway.iwcm.components.rating.jpa.RatingEntity" indexId="index" >
					<tr>
							<td><%=index.intValue()+1%>.&nbsp;</td>
							<td><a href="<%=docDB.getDocLink(r.getDocId(), request)%>"><iwcm:beanWrite name="r" property="docTitle"/></a></td>
							<td><iwcm:beanWrite name="r" property="ratingStat"/></td>
					</tr>
				</iwcm:iterate>
		 </tbody>
	</table>

</iwcm:present>