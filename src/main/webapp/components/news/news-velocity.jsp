<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

//removneme triedu aby sa nepouzila znova pri opakovanom vlozeni newsky
pageContext.removeAttribute("/sk/iway/iwcm/components/news/News.action");
pageContext.removeAttribute("newsActionBean");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><stripes:useActionBean var="newsActionBean" beanclass="sk.iway.iwcm.components.news.NewsActionBean" /><%

//ochrana pred vnorenym volanim news_velocity (zacyklenie noviniek)
try
{
    String RECURSION_PROTECTION_KEY = "/components/news/news-velocity.jsp-RECURSION_PROTECTION";

    Integer includeCounter = (Integer)request.getAttribute(RECURSION_PROTECTION_KEY);
    if (includeCounter==null) includeCounter = Integer.valueOf(1);

    if (includeCounter>25) return;

    includeCounter = new Integer(includeCounter.intValue()+1);

    request.setAttribute(RECURSION_PROTECTION_KEY, includeCounter);

}
catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }

pageContext.include("/sk/iway/iwcm/components/news/News.action");

//stranka pre includnutie noviniek
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
%>
<%request.setAttribute("newsHtmlOutput", newsActionBean.getHtmlOut()); %>
<iwcm:write name="newsHtmlOutput" />