<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

//Komponenta proxy dokaze ziskat data zo vzdialeneho proxy servera
//a tie vlozit do stranky
//Tu ich mozete lubovolne upravovat

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String data = (String)request.getAttribute("proxyOutputData");

//ukazka nahrady HTML kodu
//data = Tools.replace(data, "src=\"/obrazky/", "src=\"/images/");

request.setAttribute("proxyData", data);
%><iwcm:write name="proxyData"/>