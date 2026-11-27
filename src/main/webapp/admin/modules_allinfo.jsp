<%@page import="java.util.Map"%><%@page import="java.util.List"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.system.*"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%><script language="JavaScript">
var helpLink = "";
</script>
<%
// List the modules included in the CMS edition comparison.

List<ModuleInfo> allModules = Modules.getInstance().getItems();
List<ModuleInfo> filtered = new ArrayList<ModuleInfo>();
for (ModuleInfo m : allModules)
{
	if (m.getNameKey().equals("webjet_cmp_catalog")) continue;
	if (m.getNameKey().equals("webjet_cmp__common")) continue;
	if (m.getNameKey().equals("webjet_cmp_auction")) continue;
	if (m.getNameKey().equals("webjet_cmp_cestovka")) continue;
	if (m.getNameKey().equals("webjet_cmp_kurzy")) continue;
	if (m.getNameKey().equals("webjet_cmp_bazar")) continue;
	if (m.getNameKey().equals("webjet_cmp_page_update_info")) continue;
	if (m.getNameKey().equals("webjet_cmp_tips")) continue;
	if (m.getNameKey().equals("webjet_cmp_chat")) continue;
	if (m.getNameKey().equals("webjet_cmp_counter")) continue;
	if (m.getNameKey().equals("webjet_cmp_pocasie")) continue;
	if (m.getNameKey().equals("webjet_cmp_sms")) continue;
	if (m.getNameKey().equals("webjet_cmp_json_editor")) continue;
	if (m.getNameKey().equals("webjet_cmp_popup")) continue;
	if (m.getNameKey().equals("webjet_cmp_profesia")) continue;
	if (m.getNameKey().equals("webjet_cmp_graph")) continue;
	if (m.getNameKey().equals("webjet_cmp_imagefont")) continue;
	if (m.getNameKey().equals("intranet")) continue;
	if (m.getNameKey().equals("webjet_cmp_intranet")) continue;
	if (m.getNameKey().equals("webjet_cmp_projects_microsite")) continue;
	if (m.getNameKey().equals("webjet_cmp_elfinder")) continue;
	if (m.getNameKey().equals("webjet_cmp_content-block")) continue;
	if (m.getNameKey().equals("webjet_editor_unlimited_upload")) continue;
	if (m.getNameKey().equals("webjet_make_zip_archive")) continue;
	if (m.getNameKey().equals("webjet_modRestart")) continue;
	if (m.getNameKey().equals("webjet_replaceAll")) continue;

	//NET/DSK/LMS appky
	if (m.getNameKey().equals("webjet_cmp_lms")) continue;
	if (m.getNameKey().equals("webjet_cmp_adresar")) continue;
	if (m.getNameKey().equals("webjet_cmp_sharepoint")) continue;
	if (m.getNameKey().equals("webjet_cmp_inzercia")) continue;
	if (m.getNameKey().equals("webjet_cmp_uschovna")) continue;
	if (m.getNameKey().equals("webjet_cmp_wiki")) continue;
	if (m.getNameKey().equals("webjet_cmp_docman")) continue;
	if (m.getNameKey().equals("webjet_cmp_magzilla")) continue;
	if (m.getNameKey().equals("webjet_cmp_inventory")) continue;
	if (m.getNameKey().equals("webjet_cmp_mail")) continue;
	if (m.getNameKey().equals("webjet_cmp_organization")) continue;
	if (m.getNameKey().equals("webjet_cmp_kanban")) continue;
	if (m.getNameKey().equals("webjet_cmp_page_reactions")) continue;
	if (m.getNameKey().equals("webjet_cmp_mcalendar")) continue;
	if (m.getNameKey().equals("webjet_cmp_helpdesk")) continue;


	//systemove appky
	if (m.getNameKey().equals("webjet_cmp_webjet9")) continue;
	if (m.getNameKey().equals("webjet_cmp_grideditor")) continue;
	if (m.getNameKey().equals("webjet_cmp_sync")) continue;
	if (m.getNameKey().equals("webjet_menuSync")) continue;
	if (m.getNameKey().equals("webjet_cmp_xml")) continue;

	//TODO:
	if (m.getNameKey().equals("webjet_cmp_fileatr")) continue;

	//DEPRECATED
	if (m.getNameKey().equals("webjet_cmp_db_browser")) continue;

	filtered.add(m);
}
request.setAttribute("modules", filtered);

Prop prop = Prop.getInstance(request);

int basicCount = 0;
int proCount = 0;
int entCount = 0;
int multiCount = 0;
int osCount = 0;
int msgCount = 0;

%>
<%!
// Most catalogue entries use the component title and description. Some modules use administration menu keys.
private static final Map<String, String> TITLE_KEYS = Map.ofEntries(
	Map.entry("abtesting", "components.abtesting.dialog_title"),
	Map.entry("adminlog", "components.adminlog.adminlog"),
	Map.entry("ai_tools", "components.ai_assistants.title"),
	Map.entry("app-language-redirect", "apps.app-language-redirect.title"),
	Map.entry("attributes", "editor.atributes"),
	Map.entry("calendar", "menu.calendar"),
	Map.entry("captcha", "editor.form.captcha"),
	Map.entry("clone_structure", "admin.clone.clone_site"),
	Map.entry("crontab", "admin.crontab.view"),
	Map.entry("enumerations", "components.enumerations"),
	Map.entry("export", "components.export.dialog_title"),
	Map.entry("fileIndexer", "fbrowse.fileIndexer"),
	Map.entry("form", "menu.forms"),
	Map.entry("imageEditor", "image_editor.title"),
	Map.entry("insert_script", "components.insert_script.insert"),
	Map.entry("menu.fbrowser", "menu.root.files"),
	Map.entry("menu.templates", "menu.templates"),
	Map.entry("menu.users", "menu.users"),
	Map.entry("messages", "components.messages.menu"),
	Map.entry("proxy", "components.proxy.dialog_title"),
	Map.entry("redirects", "components.redirect.admin_list.tabulka_presmerovani"),
	Map.entry("response-header", "apps.response-header.table_name"),
	Map.entry("server_monitoring", "components.monitor.monitoring"),
	Map.entry("stat", "menu.stat"),
	Map.entry("tooltip", "components.tooltip.dialog_title")
);

private static final Map<String, String> DESCRIPTION_KEYS = Map.ofEntries(
	Map.entry("app-language-redirect", "apps.app-language-redirect.desc"),
	Map.entry("proxy", "components.proxy.dialog_desc"),
	Map.entry("response-header", "apps.response-header.desc"),
	Map.entry("stat", "menu.stat.desc"),
	Map.entry("tooltip", "components.tooltip.dialog_desc")
);
%>
<style>
.center { text-align: center; }
</style>

<display:table class="sort_table" name="modules" uid="m" export="true" defaultsort="1" pagesize="999"><%
    ModuleInfo mi = (ModuleInfo)pageContext.getAttribute("m");
    String moduleId = mi.getNameKey().replaceFirst("^webjet_(cmp_)?", "");
    String componentKey = "components." + moduleId;
    String titleKey = TITLE_KEYS.getOrDefault(moduleId, componentKey + ".title");
    String descriptionKey = DESCRIPTION_KEYS.getOrDefault(moduleId, componentKey + ".desc");
%>

	<display:setProperty name="export.excel.filename" value="modules.xls" />
	<display:setProperty name="export.csv.filename" value="modules.csv" />
	<display:setProperty name="export.xml.filename" value="modules.xml" />
	<display:setProperty name="export.pdf.filename" value="modules.pdf" />

	<display:column titleKey="components.cookies.cookie_manager.application" sortable="true"><%=prop.getText(titleKey) %></display:column>
	<display:column titleKey="user.admin.key" property="nameKey" sortable="true"/>
	<display:column titleKey="admin.conf_editor.description" sortable="true"><%=prop.getText(descriptionKey) %></display:column>
	<display:column property="group" sortable="true"/>
	<display:column class="center" title="Basic"><% if (mi.getWjVersions().indexOf("B")!=-1) { out.print("X"); basicCount++; } %></display:column>
	<display:column class="center" title="Professional"><% if (mi.getWjVersions().indexOf("P")!=-1) { out.print("X"); proCount++; } %></display:column>
	<display:column class="center" title="Enterprise"><% if (mi.getWjVersions().indexOf("E")!=-1) { out.print("X"); entCount++; } %></display:column>
	<display:column class="center" title="Community"><% if (mi.getWjVersions().indexOf("O")!=-1) { out.print("X"); osCount++; } %></display:column>
	<display:column class="center" title="MultiWeb"><% if (mi.getWjVersions().indexOf("M")!=-1) { out.print("X"); multiCount++; } %></display:column>
	<display:column class="center" title="MSG"><% if (mi.getWjVersions().indexOf("D")!=-1) { out.print("X"); msgCount++; } %></display:column>
</display:table>

Basic: <%=basicCount %><br/>
Professional: <%=proCount %><br/>
Enterprise: <%=entCount %><br/>
Community/Open Source: <%=osCount %><br/>
MultiWeb: <%=multiCount %><br/>
MSG: <%=msgCount %><br/>


<%@ include file="/admin/layout_bottom.jsp" %>
