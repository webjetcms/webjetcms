<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<iwcm:menu notName="cmp_adminlog_logging">
    <%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
    %>
</iwcm:menu>

<%@ include file="/admin/layout_top.jsp" %>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-list"></i><iwcm:text key="components.adminlog.adminlog"/><i class="ti ti-chevron-right"></i><iwcm:text key="components.adminlog.logging"/></h1>
</div>

<form method="post" action="/admin/adminlog/logging">
	<p class="bg-warning" style="padding: 15px;"><iwcm:text key="components.adminlog.logging.ulozenie_sposobi_nastavenie_hodnot_do_premennych_prostredi_len_do_dalsieho_restartu"/></p>
	<div class="col-md-12">
		<div class="form-group">
			<div class="checkbox">
				<label><input type="checkbox" name="isDebug" value="true" <c:if test="${isDebug}">checked="checked"</c:if>><iwcm:text key="components.adminlog.logging.webjet__debug_rezim"/></label>
			</div>
		</div>

		<div class="form-group">
			<label for="package-debug"><iwcm:text key="components.adminlog.logging.package_level_debug"/>:</label>
			<textarea class="form-control" name="logLevels" rows="5" id="package-debug"><c:out value="${logLevels}"/></textarea>
		</div>

		<div class="form-group">
			<button type="submit" class="btn btn-primary"><iwcm:text key="components.adminlog.logging.submit"/></button>
		</div>
	</div>
</form>

<div class="col-md-12">
	<h4>${logDirPath}</h4>
	<ul class="list-group">
		<c:forEach items="${files}" var="file">
			<li class="list-group-item"><a style="display:block;" href="/admin/tail.jsp?file=${file.absolutePath}">${file.name} (${file.formatedFileSize}, ${file.formatedDateTime})</a></li>
		</c:forEach>
	</ul>
</div>


<%@ include file="/admin/layout_bottom.jsp" %>