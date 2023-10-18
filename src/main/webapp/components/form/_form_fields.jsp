<%@page import="sk.iway.iwcm.form.FormDetails"%><%@
page import="sk.iway.iwcm.form.FormDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
page import="org.json.JSONArray"%><%

	int definingFormId = FormDB.searchOldNameForm(Tools.getRequestParameter(request, "form_name"));
	if (definingFormId < 1)
		throw new IllegalArgumentException("Form with name: "+Tools.getRequestParameter(request, "form_name")+" does NOT exist");

	FormDetails definingForm = new FormDetails();
	definingForm.setData(FormDB.getData(definingFormId));
	JSONArray values = new JSONArray();
	for (Object o : definingForm.getColNamesArray())
		values.put(o.toString());
%><%=values.toString()%>
