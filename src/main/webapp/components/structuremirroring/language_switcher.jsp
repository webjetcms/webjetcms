<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.structuremirroring.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

DocDetails doc;
if (request.getAttribute("docDetailsOriginal")!=null) doc = (DocDetails)request.getAttribute("docDetailsOriginal");
else doc = (DocDetails)request.getAttribute("docDetails");

List<LabelValueDetails> otherLanguages = DocMirroringServiceV9.getOtherLanguages(doc);
if (otherLanguages==null || otherLanguages.size()==0) {
   if (request.getAttribute("inPreviewMode")!=null) {
      String defaultLanguage = Constants.getString("defaultLanguage");
      if (Tools.isEmpty(defaultLanguage)) defaultLanguage = "sk";
      LabelValueDetails lv = new LabelValueDetails(defaultLanguage.toUpperCase(), "");
      lv.setValue2(defaultLanguage);
      otherLanguages.add(lv);
      lv = new LabelValueDetails("EN", "");
      lv.setValue2("en");
      otherLanguages.add(lv);
   } else {
      return;
   }
}

String flagsPath = pageParams.getValue("flagsPath", null);
if (Tools.isNotEmpty(flagsPath)) {
   //change link.label to flagsPath + value2 + .png
   for (LabelValueDetails link : otherLanguages) {
      link.setLabel("<img src=\"" + flagsPath + link.getValue2() + ".png\" alt=\"" + link.getLabel() + "\" />");
   }
}

%>
<ul class="navbar-nav other-languages">
<c:forEach var="link" items="<%=otherLanguages%>">
   <li class="nav-item">
      <a class="nav-link" href="${link.value}">${link.label}</a>
   </li>
</c:forEach>
</ul>