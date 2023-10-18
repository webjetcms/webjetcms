<%@page import="sk.iway.iwcm.system.cluster.ClusterDB"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.components.abtesting.ABTesting"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_abtesting"/>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

if(Tools.isNotEmpty(request.getParameter("bSave")))
{
	boolean isCluster = ClusterDB.isServerRunningInClusterMode();
	ConfDB.setName("ABTesting", String.valueOf("true".equals(request.getParameter("abTestingOn"))));
	if(isCluster) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-ABTesting");

	if(Tools.isNotEmpty(request.getParameter("pomer")))
	{
		ConfDB.setName("ABTestingRatio", request.getParameter("pomer"));
		if(isCluster) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-ABTestingRatio");
	}

	if(Tools.isNotEmpty(request.getParameter("varName")))
	{
		ConfDB.setName("ABTestingName", request.getParameter("varName"));
		if(isCluster) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-ABTestingName");
	}

	if(Tools.isNotEmpty(request.getParameter("cookieName")))
	{
		ConfDB.setName("ABTestingCookieName", request.getParameter("cookieName"));
		if(isCluster) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-ABTestingCookieName");
	}

	if(Tools.isNotEmpty(request.getParameter("cookieDays")))
	{
		ConfDB.setName("ABTestingCookieDays", request.getParameter("cookieDays"));
		if(isCluster) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-ABTestingCookieDays");
	}
}
%>
<%@ include file="/admin/layout_top.jsp" %>

<iwcm:menu notName="cmp_abtesting">
<%
	response.sendRedirect("/admin/");
%>
</iwcm:menu>

<script type="text/javascript">
	function showDoc(id)
	{
		window.open("/showdoc.do?docid="+id);
	}
</script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-notebook"></i> <iwcm:text key="components.abtesting.dialog_title"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="stat_menu.options"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">
	    	<form class="globalFilterForm" role="form" action="/components/abtesting/admin_abtesting.jsp" name="settingsForm" method="post" id="settingsFormId">

	    		<div class="col-md-1 col-sm-6">
	    			<div class="form-group">
	    				<label class="form-label" for="pomerId"><iwcm:text key="components.abtesting.ratio"/>: </label>
	    				<input class="form-control input-xsmall" type="text" name="pomer" id="pomerId" value="<%=Constants.getString("ABTestingRatio")%>">
	    			</div>
	    		</div>
	    		<div class="col-md-2 col-sm-6">
	    			<div class="form-group">
	    				<label class="form-label" for="varNameId"><iwcm:text key="components.abtesting.variantName"/>: </label>
	    				<input class="form-control" type="text" name="varName" id="varNameId" value="<%=Constants.getString("ABTestingName")%>">
	    			</div>
	    		</div>
	    		<div class="col-md-2 col-sm-6">
	    			<div class="form-group">
	    				<label class="form-label" for="cookieNameId"><iwcm:text key="components.abtesting.cookieName"/>: </label>
	    				<input class="form-control" type="text" name="cookieName" id="cookieNameId" value="<%=Constants.getString("ABTestingCookieName")%>">
	    			</div>
	    		</div>
	    		<div class="col-md-2 col-sm-6">
	    			<div class="form-group">
	    				<label class="form-label" for="cookieDaysId"><iwcm:text key="components.abtesting.cookieDays"/>: </label>
	    				<input class="form-control input-xsmall" type="text" name="cookieDays" id="cookieDaysId" value="<%=Constants.getString("ABTestingCookieDays")%>">
	    			</div>
	    		</div>
	    		<div class="col-md-2 col-sm-6">
	    			<div class="form-group">
	    				<label class="control-label display-block">&nbsp;</label>
	    				<label class="form-label" for="abTestingOnId">
    						<input class="form-control" type="checkbox" name="abTestingOn" id="abTestingOnId" value="true" <%if(Constants.getBoolean("ABTesting")){out.print("checked=\"checked\"");}%>><iwcm:text key="components.abtesting.allowed"/>
    					</label>
	    			</div>
	    		</div>
	    		<div class="col-md-2 col-sm-6">
	    			<div class="form-group">
	    				<label class="control-label display-block">&nbsp;</label>
	    				<input type="submit" name="bSave" value="<iwcm:text key="button.setup"/>" class="btn green"/>
	    			</div>
	    		</div>

			</form>
		</div>
	</div>
</div>

<%
List<DocDetails> vsetkyStranky = DocDB.getInstance().getBasicDocDetailsAll();
Set<DocDetails> hlavneStranky = new HashSet<DocDetails>();

if(vsetkyStranky!=null)
{
	List<String> allDomains = GroupsDB.getInstance().getAllDomainsList();
	DocDB docDB = DocDB.getInstance();
	for(DocDetails dd : vsetkyStranky)
	{
		if(ABTesting.getAllVariantsDocIds(dd, allDomains, docDB).size()>0)
			hlavneStranky.add(dd);
	}
}

request.setAttribute("hlavneStranky", hlavneStranky);
%>

<display:table class="sort_table" cellspacing="0" cellpadding="0" name="hlavneStranky" uid="row" defaultsort="1" >
	<display:column sortable="true" titleKey="admin.temps_page_uses.docid_stranky" property="docId" />

	<display:column titleKey="components.table.column.tools" style="text-align:center;">
		<a href="javascript:showDoc(${row.docId});" title='<iwcm:text key="editor.preview"/>' class="iconPreview">&nbsp;</a>
 		<a href="/apps/stat/admin/top-details/?docId=${row.docId}" title='<iwcm:text key="menu.stat"/>' class="iconStat">&nbsp;</a>
 	</display:column>

	<display:column sortable="true" titleKey="editor.title" property="title" />
 	<display:column sortable="true" titleKey="editor.virtual_path" property="virtualPath" />
</display:table>

<%@ include file="/admin/layout_bottom.jsp" %>