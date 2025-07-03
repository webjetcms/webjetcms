<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

//komponenta na automaticke najdenie CTA modulu
//ten sa hlada rekurzivne pod nazvom /cesta/aktualne-url.html/cta.html a nasledne ako /cesta/cta.html a /cta.html

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String suffix = pageParams.getValue("suffix", "cta");

int ctaDocId = -1;
int maxDirectoryDepth = pageParams.getIntValue("maxDirectoryDepth", 5);

String testPath = PathFilter.getOrigPath(request);
if (testPath.startsWith("/showdoc.do"))
{
//musime vygenerovat
testPath = DocDB.getInstance().getDocLink(Tools.getIntValue(request.getParameter("docid"), -1), request);
}

if (testPath.endsWith("/")) testPath = testPath.substring(0, testPath.length()-1);

int failsafe = 0;
DocDB docDB = DocDB.getInstance();
DocDetails ctaDocDetails = null;
while (failsafe++ < maxDirectoryDepth && ctaDocId<1)
{

	String urlCta = testPath + "/"+suffix+".html";
	urlCta = Tools.replace(urlCta, ".html/"+suffix+".html", "-"+suffix+".html");
	int docId = DocDB.getDocIdFromURL(urlCta, DocDB.getDomain(request));
	//out.println("Testing: "+urlCta+" docid="+docId);
	if (docId > 0)
	{
		System.out.println("cta.jsp: forwarding "+urlCta+" = "+docId+" ip="+Tools.getRemoteIP(request));
		ctaDocDetails = docDB.getBasicDocDetails(docId, true);
		
		//if(ctaDocDetails.isAvailable()){ 	//ak je aktivna
			GroupDetails grd = GroupDetails.getById(ctaDocDetails.getGroupId());
			//zistime ci je cta stranka v kosi, ak je nezobrazime ju
			if(grd != null && !DB.internationalToEnglish(grd.getFullPath().toLowerCase()).startsWith(DB.internationalToEnglish("/system")))
			{
				ctaDocId = docId;
				break;
			}
		//}
	}
	
	int start = testPath.lastIndexOf("/");
	if (start != -1) testPath = testPath.substring(0, start);
	else break;
}

request.removeAttribute("ctaData");
if (ctaDocId < 1) return;

//najdi telo dokumentu

ctaDocDetails = docDB.getDoc(ctaDocId);
if (ctaDocDetails == null) return;


String data = ctaDocDetails.getData();

//ak su data prazdne, alebo je tam menej ako 20 znakov skonci
if (Tools.isEmpty(data) || data.length()<10) return;

//osetri, aby sa data zacinali na <h6
//int index = data.indexOf("<h6");
//if (index > 0) data = data.substring(index);

//obal H6 tagy nejakym div elementom
//data = Tools.replace(data, "<h6", "</div>\n<div class=\"tabCase\">\n<h6");
//odstran vsetko pred prvym <div
//data = data.substring(data.indexOf("\n<div"));
//pridaj koncovy div
//data = data + "</div>";
String cssClass = Tools.getCookieValue(request.getCookies(), "cs-segment", "-1");
int firstIndexTdOpen = -1;
int firstIndexTdClose = -1;
int lastIndexTd = -1;
//prvy blok - ak nenajdeme iny, pouzijeme tento
if(data.indexOf("class=\"cs") != -1)
{
	firstIndexTdOpen = data.indexOf("class=\"cs");
	if(data.indexOf("</td>",firstIndexTdOpen) != -1)
	{
		firstIndexTdClose = data.indexOf("</td>",firstIndexTdOpen);
// 		if(firstIndexTdClose != -1 )
// 			firstIndexTdClose += 5;
	}
}
lastIndexTd = data.lastIndexOf("</td>");
// if(lastIndexTd != -1 )
// 	lastIndexTd += 5;
//ak to nezapasovalo na prve <td>
int findTdStartIndex = data.indexOf("class=\"cs"+cssClass+"\"",firstIndexTdOpen);
int findTdEndIndex = data.indexOf("</td>",findTdStartIndex);
if(findTdStartIndex != -1 && findTdEndIndex != -1)
{
	findTdStartIndex += 11+cssClass.length();
	data = data.substring(findTdStartIndex,findTdEndIndex);
}
else if(firstIndexTdOpen != -1 && firstIndexTdClose != -1 )
{
	firstIndexTdOpen += 11+cssClass.length();
	data = data.substring(firstIndexTdOpen,firstIndexTdClose);
}

request.setAttribute("ctaData", data);

%>
<iwcm:write name="ctaData"/>
