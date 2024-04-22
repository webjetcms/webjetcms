<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.form.FormFileRestriction"%>
<%@page import="sk.iway.iwcm.form.FormDB"%>
<iwcm:checkLogon admin="true" perms="cmp_form"/>
<%
	String formName = Tools.getRequestParameter(request, "formname");

	FormFileRestriction restriction = null;

	if (FormDB.isThereFileRestrictionFor(formName))
		restriction = FormDB.getFileRestrictionFor(formName);
	else
	{
		restriction = new FormFileRestriction().
			setFormName(formName).setMaxSizeInKilobytes(5000).setAllowedExtensions("").
			setPictureHeight(3000).setPictureWidth(2000);
	}

	request.setAttribute("restriction", restriction);


%>
<style type="text/css">
	#restrictionsContent div.col-xs-12 { width: 100%; clear: both; padding-bottom: 2px; min-height: 28px; }
	#restrictionsContent div.col-xs-6 { width: 50%; float: left; }
	#restrictionsContent input, #restrictionsContent formAttributes select
	{
	    background-color: #fff;
	    border: 1px solid #c9cccf;
	    border-top-color: #aeb3b9;
	    padding: 4px 6px;
	    outline: none;
	    box-sizing: border-box;
	    border-radius: 3px;
	    box-shadow: 0 1px 2px rgba(0,0,0,.15) inset;
	}
</style>

<div class="formFileAttributes">
	<div class="col-xs-12 form-group">
		<div class="col-xs-6">
			<iwcm:text key="components.forms.file_restrictions.file_size_in_kilobytes"/>:
		</div>
		<div class="col-xs-6">
			<input type="text" size="6" value="${restriction.maxSizeInKilobytes}" name="attribute_maxSizeInKilobytes" /> kB
		</div>
	</div>
	<div class="col-xs-12 form-group">
		<div class="col-xs-6">
			<iwcm:text key="components.forms.file_restrictions.allowed_extensions"/>:
		</div>
		<div class="col-xs-6">
			<input type="text" size="20" value="${restriction.allowedExtensions}" name="attribute_allowedExtensions" placeholder="<iwcm:text key="editor.form.extensions_example"/>"/>
		</div>
	</div>
	<div class="col-xs-12 form-group">
		<div class="col-xs-6">
			<iwcm:text key="components.forms.file_restrictions.image_height"/>:
		</div>
		<div class="col-xs-6">
			<input type="text" size="6" value="${restriction.pictureHeight}" name="attribute_pictureHeight" /> <iwcm:text key="components.forms.file_restrictions.points"/>
		</div>
	</div>
	<div class="col-xs-12 form-group">
		<div class="col-xs-6">
			<iwcm:text key="components.forms.file_restrictions.image_width"/>:
		</div>
		<div class="col-xs-6">
			<input type="text" size="6" value="${restriction.pictureWidth}" name="attribute_pictureWidth" /> <iwcm:text key="components.forms.file_restrictions.points"/>
		</div>
	</div>
</div>
<script>
$("div.formFileAttributes input").on("keyup keydown", function(e) {
	//console.log("keyup, e=", e.keyCode);
	if (e.keyCode>=37 && e.keyCode<=40) e.stopPropagation();
	else if (e.keyCode == 9 || e.keyCode == 13) e.stopPropagation();
});
</script>
