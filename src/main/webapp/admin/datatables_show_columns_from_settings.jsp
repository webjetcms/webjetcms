<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminBean"%>
<%@page import="sk.iway.iwcm.Identity"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%><%@ page import="sk.iway.iwcm.i18n.Prop" %><%@ page import="sk.iway.iwcm.Tools" %><%@ page import="sk.iway.iwcm.PathFilter" %>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>

<%--//Potrebne styly pre korektne spravanie datatables--%>
<style>
    body { overflow: hidden; }
	@media (min-width: 992px) {
		.page-content-wrapper { min-width: auto; width: 100%; }
	}
</style>

<%--//Pomocne bloky pre vyratanie vysky obsahu, datatables prebera vysku nastavenu tomuto content-body, same sa meni po resize--%>
<div class="full-height-content full-height-content-scrollable full-height-content-for-datatables">
    <div class="full-height-content-body" data-always-visible="1" data-axis="both" data-scrollbarsize="5px"></div>
</div>

<%--//Globalne nastavenia pre datatables--%>
<%@ include file="/admin/datatables_global_config.jsp" %>