<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.form.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("cmpName", "forms.file_restrictions");
%>
<iwcm:checkLogon perms="cmp_form"/>

<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<%

	String formName = Tools.getRequestParameter(request, "formname");

	if (Tools.getRequestParameter(request, "submitted") != null && Tools.isNotEmpty(formName))
	{
		int maxSizeInKilobytes = Tools.getIntValue(Tools.getRequestParameter(request, "attribute_maxSizeInKilobytes"), 0);
		int pictureHeight = Tools.getIntValue(Tools.getRequestParameter(request, "attribute_pictureHeight"), 0);
		int pictureWidth = Tools.getIntValue(Tools.getRequestParameter(request, "attribute_pictureWidth"), 0);
		String allowedExtensions = Tools.getRequestParameter(request, "attribute_allowedExtensions");

		FormAttributeDB fadb = new FormAttributeDB();
		Map<String, String> parameters = fadb.load(formName);
		parameters.put("maxSizeInKilobytes", String.valueOf(maxSizeInKilobytes));
		parameters.put("pictureHeight", String.valueOf(pictureHeight));
		parameters.put("pictureWidth", String.valueOf(pictureWidth));
		parameters.put("allowedExtensions", allowedExtensions);
		fadb.save(formName, parameters);
	}

	FormFileRestriction restriction = null;

	if (FormDB.isThereFileRestrictionFor(formName))
		restriction = FormDB.getFileRestrictionFor(formName);
	else
	{
		restriction = new FormFileRestriction().
			setFormName(formName).setMaxSizeInKilobytes(0).setAllowedExtensions("").
			setPictureHeight(0).setPictureWidth(0);
	}

	request.setAttribute("restriction", restriction);

%>
<script type="text/javascript">
	resizeDialog(550, 600);
	document.body.style.cursor = "default";
	function Ok()
	{
		//musi to ist takto, inak by sa nezavolal check form
		document.fileRestrictionsForm.submitted.click();
	}
</script>
<form action="/components/form/admin_form_file_restrictions.jsp" method="post" name="fileRestrictionsForm">
	<jsp:include page="/components/form/admin_file_restriction_form.jsp" />
	<input type="submit" style="display: none;" name="submitted" value="Submit" />
	<input type="hidden" name="formname" value="${restriction.formName}" />
</form>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
