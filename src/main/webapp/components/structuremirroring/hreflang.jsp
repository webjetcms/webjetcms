<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.structuremirroring.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

DocDetails doc;
if (request.getAttribute("docDetailsOriginal")!=null) doc = (DocDetails)request.getAttribute("docDetailsOriginal");
else doc = (DocDetails)request.getAttribute("docDetails");

List<LabelValueDetails> otherLanguages = DocMirroringServiceV9.getHrefLang(doc, request);
%>
<c:forEach var="link" items="<%=otherLanguages%>">
   <link rel="alternate" hreflang="${link.label}" href="${link.value}" /></c:forEach>