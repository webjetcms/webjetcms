<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.common.DocTools" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@page import="sk.iway.iwcm.Tools"%><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.components.form_settings.rest.FormSettingsService"%><%@page import="java.util.Map"%>
<iwcm:checkLogon admin="true" perms="cmp_form"/><%

if (Tools.getRequestParameter(request, "form_name") != null)
{
   String formName = Tools.getRequestParameter(request, "form_name");
   if ("true".equals(Tools.getRequestParameter(request, "fixFormName")))
   {
       formName = DocTools.removeChars(formName, true);
   }

	Map<String, String> parameters = FormSettingsService.filterAttributes(request.getParameterMap());
	FormSettingsService.save(formName, parameters);
}
%>
