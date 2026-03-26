<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="java.util.Map" %>
<%@ page import="sk.iway.iwcm.components.form_settings.rest.FormSettingsService" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

PageParams pp = new PageParams(request);
String formId = pp.getValue("formId", "formMailForm");
String formElementName = pp.getValue("formElementName", "uploadedFiles");
String formSaveDb = pp.getValue("savedb", null);
String allowedExtensions = pp.getValue("allowedExtensions", null);

//ak mame savedb mozeme skusit nacitat allowedExtensions
if (Tools.isNotEmpty(formSaveDb))
{
    Map<String, String> formAttrs = FormSettingsService.load(formSaveDb);
    if (formAttrs != null)
    {
       if (Tools.isNotEmpty(formAttrs.get("allowedExtensions"))) allowedExtensions = formAttrs.get("allowedExtensions");
    }
}

//out.println("formElementName="+formElementName);
%>
<input type="hidden" name="Multiupload.formElementName" value="<%=formElementName %>" />

<div id="<%=formElementName%>-dropzone" class="drop-zone-box dropzone wjdropzone"
     data-dzmaxfiles="10"
     <% if (Tools.isNotEmpty(allowedExtensions)) { %>data-dzacceptedfiles="<%=allowedExtensions%>"<% } %>
><input type="hidden" name="<%=formElementName %>"/><input type="text" name="<%=formElementName %>-fileNames" class="uploadedObjectsNames"/><input type="hidden" class="uploadedObjectsInfo"></div>

<jsp:include page="/components/_common/upload/upload.jsp"/>