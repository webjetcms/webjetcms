<%@page import="sk.iway.iwcm.PathFilter"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.stat.*,java.util.*,sk.iway.iwcm.i18n.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><iwcm:checkLogon admin="true" perms="edit_text"/><%

<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.PageLng"%>
<%@ page import="sk.iway.iwcm.Tools" %>

<%@ include file="/admin/layout_top.jsp" %>

<script type="text/javascript">
<!--
	helpLink = "admin/texts.jsp&book=admin";
//-->
</script>

<script type="text/javascript" src="/admin/scripts/tablesort.js"></script>
<script type="text/javascript">
<!--
	function popup2( width, height, url)
	{
	      popup(url, width, height);
	}

//-->
</script>

<%
	String pathFilterPath = (String)request.getAttribute("path_filter_orig_path");
	String lng= Tools.getRequestParameter(request, "lng");
	boolean case_sensitive=false;
	if(lng == null)
		lng="";
	String languages[];
	languages = new String[9];
		languages[0] = "sk";
		languages[1] = "cz";
		languages[2] = "en";
		languages[3] = "de";
		languages[4] = "pl";
		languages[5] = "hu";
		languages[6] = "cho";
		languages[7] = "ru";
		languages[8] = "esp";

	//ziskame default(alebo vybrany) jazyk
	String language = PageLng.getUserLng(request);
	language = ( Tools.getRequestParameter(request, "lng") != null ? Tools.getRequestParameter(request, "lng") : language );

	if(Tools.getRequestParameter(request, "export")!= null || Tools.getRequestParameter(request, "importAct")!= null){
		pageContext.include("/sk/iway/iwcm/i18n/PropImportExport.action");
		if(request.getAttribute("pathToFile")!= null){
					%>
					<script type="text/javascript">
						<!--
						popup('<%=request.getAttribute("pathToFile")%>', 800, 600);
						//-->
					</script>

		<%

		}
	}

%>
<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.i18n.PropImportExportAction"/>

<div class="row title">
    <h1 class="page-title"><i class="icon-speech"></i><iwcm:text key="admin.conf_editor.edit_text"/><i class="fa fa-angle-right"></i><iwcm:text key="admin.conf_editor.export-import"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="admin.conf_editor.export" />
			</a>
		</li>
		<li>
			<a href="#tabMenu2" id="importTab" data-toggle="tab">
				<iwcm:text key="admin.conf_editor.import" />
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">

	    	<form method="get" action="/admin/prop-export.jsp" name="exportForm" style="margin: 0px;" >


               <div class="col-lg-2 col-sm-4">
                   <div class="form-group">
						<label for="lngSelect" class="control-label"><iwcm:text key="user.admin.language"/>:</label>
						<select id="lngSelect" name="lng" class="select form-control">
							<% for(int i=0;i<languages.length;i++) { %>
							<option <%if((languages[i]).equalsIgnoreCase(language)) out.println("selected");%>><%=languages[i]%></option>
							<%}%>
						</select>
					</div>
				</div>

				<div class="col-lg-2 col-sm-4">
                   <div class="form-group">
						<label for="formatSelect" class="control-label"><iwcm:text key="editor.link.type"/>:</label>
						<select id="formatSelect" name="format" class="select form-control">
							<option value="properties" selected>properties</option>
							<option value="json">json</option>
						</select>
					</div>
				</div>

				<div class="col-lg-2 col-sm-4">
                   <div class="form-group">
						<label for="prefix" class="control-label"><iwcm:text key="admin.textexport.prefix"/>:</label>
						<input id="prefix" name="prefix" type="text" class="form-control">
					</div>
				</div>

				<div class="col-lg-3 col-sm-8">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<label class="control-label"><iwcm:text key="admin.textexport.onlyChangedTexts"/></label>
					</div>
				</div>

				<div class="col-lg-2 col-sm-4">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<input type="submit" name="export" value="<iwcm:text key="admin.conf_editor.export"/>"  style="margin-left: 10px;" class="btn green" />
					</div>
				</div>


			</form>

		</div>
	    <div id="tabMenu2" class="tab-pane">

	    	<iwcm:stripForm method="POST" action="<%=PathFilter.getOrigPathUpload(request)%>" name="importForm" style="margin: 0px;" beanclass="sk.iway.iwcm.i18n.PropImportExportAction">
				<stripes:errors/>

					<div class="col-md-2 col-sm-4">
						<div class="form-group">
							<label for="lngSelectImport" class="control-label"><iwcm:text key="user.admin.language"/>:</label>
							<select id="lngSelectImport" name="lng" class="select form-control">
							<% for(int i=0;i<languages.length;i++) { %>
							<option <%if((languages[i]).equalsIgnoreCase(language)) out.println("selected");%>><%=languages[i]%></option>
							<%}%>
						</select>
						</div>
					</div>

					<div class="col-md-3 col-sm-4">
	                   <div class="form-group">
							<label for="prefixImport" class="control-label"><iwcm:text key="admin.textexport.addPrefix"/>:</label>
							<input id="prefixImport" name="prefix" type="text" class="form-control">
						</div>
					</div>

					<div class="col-md-3 col-sm-4">
	                   <div class="form-group">
							<label for="onlyPrefixImport" class="control-label"><iwcm:text key="admin.textexport.onlyPrefix"/>:</label>
							<input id="onlyPrefixImport" name="filterPrefix" type="text" class="form-control">
						</div>
					</div>

					<div class="col-md-4 col-sm-4">
						<div class="form-group">
							<label for="propFile"><iwcm:text key="user.admin.propfile"/>:</label>
							<stripes:file id="propFile" class="required" name="propFile"></stripes:file>
						</div>
					</div>

					<div class="col-md-8 col-sm-4 text-right">
	                   <div class="form-group">
	                   		<label for="onlyNewKeys" class="control-label">
								<input id="onlyNewKeys" name="onlyNewKeys" type="checkbox" class="form-control">
								<iwcm:text key="admin.textexport.onlyNewKeys"/>
							</label>
						</div>
					</div>


					<div class="col-md-4 col-sm-4">
						<div class="form-group">

							<input type="submit" name="importAct" value="<iwcm:text key="admin.conf_editor.import"/>" class="btn green" />
						</div>
					</div>

			</iwcm:stripForm>


		</div>
	</div>
</div>

<div id="results">
<%
boolean saveOK = (request.getAttribute("saveOk")!= null);
boolean saveFail = request.getAttribute("saveFail") != null;
%>
<%=request.getAttribute("saveOk")!=null?Prop.getInstance().getText("admin.conf_editor.importOk"):"" %>
<%=request.getAttribute("saveFail")!=null?Prop.getInstance().getText("admin.conf_editor.importFail"):"" %>
<%=request.getAttribute("message")!=null?(String)request.getAttribute("message"):"" %>
</div>
<%@ include file="/admin/layout_bottom.jsp" %>
<%-- refresh textov spat na custom hodnoty --%>
<% Prop.getInstance(Constants.getServletContext(),PageLng.getUserLng(request),true); %>
<logic:present parameter="importAct">
<script type="text/javascript">
$("#importTab").click();
</script>
</logic:present>