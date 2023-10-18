<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="java.util.List"%>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:menu notName="cmp_sync">
	<%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
	%>
</iwcm:menu>

<%
List<IwcmFile> dirsToSelect = new ArrayList<IwcmFile>();

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

IwcmFile root = IwcmFile.fromVirtualPath("/");
if (root.exists())
{
	IwcmFile[] fileList = root.listFiles();
	for (IwcmFile file : fileList)
	{
		if(file.isDirectory() && UsersDB.isFolderWritable(user.getWritableFolders(), file.getVirtualPath()))
			dirsToSelect.add(file);
	}
}

request.setAttribute("dialogTitle", "Export / Import súborov");
request.setAttribute("dialogDesc", "Umožní export, import a rollback súborov.");
%>

<%@ include file="/admin/layout_top_dialog.jsp" %>

<link rel="stylesheet" type="text/css" href="/css/style.css"/>
<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />

<style type="text/css" media="screen">

.checkboxes_zvoleneAdresare,
.input_manualPath,
.input_snapshot,
.input_emailAddress,
.fromDateBox { display:none;}

.inputFile { margin:5px 5px 20px 20px !important; }

.radio_Dirs { padding:20px 0; }
label { display:block; padding:2px 0; }

.float-block { display:inline-block; float:left; width:50%;  box-sizing: border-box; padding:15px; }
.clearfix { clear:both; padding-bottom:20px; }

.button50.export,
.button50.import  { font-size:20px; padding:15px 30px; margin-left:20px; margin-bottom:20px; }
.form-horizontal div.radio {padding-top: 0px;}
</style>

<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type="text/javascript" src="/admin/scripts/modalDialog.js"></script>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1">Export</a></li>
		<li class="first"><a href="#" onclick="showHideTab('2');" id="tabLink2">Import</a></li>
		<%//rollback, iba ak ma povolene vsetky adresare
			if(Tools.isEmpty(user.getWritableFolders())){%>
			<li class="first"><a href="#" onclick="showHideTab('3');" id="tabLink3">Rollback</a></li>
		<%}%>
		<li class="first"><a href="#" onclick="showHideTab('4');" id="tabLink4">Log</a></li>
	</ul>
</div>

<c:set var="head" value="col-xs-2" />
<c:set var="content" value="col-xs-10" />
<c:set var="contentFull" value="col-xs-12" />

<div class="tab-pane toggle_content">

	<div class="tab-page" id="tabMenu1" style="display:block;">
		<form class="form-horizontal" action="/components/sync/export_result.jsp?__setf=1&__sfu=1" name="exportFilesForm" method="post" enctype="multipart/form-data" id="exportForm" target="log_iframe">

			<div class="col-sm-6">
				<h2>Adresáre, ktoré chcete exportovať</h2>

				<div class="form-group">
					<div class="${contentFull}">

						<label class="${contentFull} radio">
							<input class="radio_Dirs" type="radio" name="path" id="path" value="standard" checked="checked">
							Štandardne definované
						</label>

						<label class="${contentFull} radio">
							<input class="radio_Dirs zvoleneAdresare" type="radio" name="path" value="selected">
							Zvolené adresáre
						</label>


						<div class="checkboxes_zvoleneAdresare">
							<%for(IwcmFile f : dirsToSelect) {%>
								<label><input type="checkbox" name="selectedDir" value="<%=Tools.escapeHtml(f.getVirtualPath())%>"> <%=Tools.escapeHtml(f.getName())%></label>
							<%}%>
					 	</div>


						<label class="${contentFull} radio">
							<input class="radio_Dirs" type="radio" name="path" value="all">
							Všetky adresáre
						</label>

						<label class="${contentFull} radio">
							<input class="radio_Dirs" type="radio" name="path" value="manual">
							Zadané adresáre
						</label>
						<input class="form-control input_manualPath" type="text" name="manualPath" placeholder="/adresar,/druhy_adresar">

					</div>
				</div>

				<br>
				<h2>Filter exportu</h2>

				<div class="form-group">
					<div class="${contentFull}">

						<label class="${contentFull} radio">
							<input type="radio" name="filter" value="none" checked="checked">
							Bez filtra
						</label>

						<label class="${contentFull} radio">
							<input type="radio" name="filter" value="fromDate">
							Od dátumu
						</label>

						<div class="fromDateBox">
							<div class="col-xs-6">
								<div data-date-format="dd.mm.yyyy" class="input-group date date-picker">
									<input class="input_fromDate form-control datepicker" type="text" name="fromDate">
									<span class="input-group-btn">
										<button type="button" class="btn default"><i class="fa fa-calendar"></i></button>
									</span>
								</div>
							</div>
						</div>

						<label class="${contentFull} radio">
							<input type="radio" name="filter" value="snapshot">
							Podľa výpisu
						</label>

						<input class="input_snapshot" type="file" name="snapshot">

					</div>
				</div>



			</div>
			<div class="col-sm-6">

				<h2>Typ exportovaného súboru</h2>

				<div class="form-group">
					<div class="${contentFull}">

						<label class="${contentFull} radio">
							<input type="radio" name="format" value="zip" checked="checked">
							.ZIP
						</label>

						<label class="${contentFull} radio">
							<input type="radio" name="format" value="tgz">
							.TGZ
						</label>

						<label class="${contentFull} radio">
							<input type="radio" name="format" value="snapshot">
							Výpis
						</label>
					</div>
				</div>

				<br>
				<h2>Typ nodu</h2>

				<div class="form-group">
					<div class="${contentFull}">

						<label class="${contentFull} radio">
							<input type="radio" name="nodeType" value="admin" checked="checked">
							Admin
						</label>

						<label class="${contentFull} radio">
							<input type="radio" name="nodeType" value="public">
							Public
						</label>
					</div>
				</div>

				<br>
				<h2>Kópia na e-mail</h2>

			 	<div class="form-group">
			 		<div class="${contentFull}">
				 		<label class="${contentFull} radio">
							<input class="checkbox_sendEmail" type="checkbox" name="sendEmail" value="yes">
							Poslať kópiu na e-mail
						</label>
						<div class="${contentFull}">
							<input class="input_emailAddress form-control"  type="text" name="emailAddress" placeholder="email@domena.sk"><br>
						</div>
					</div>
				</div>

			</div>
			<div class="clearfix"></div>

			<input name="exportSubmit" style="display: none;" type="submit" class="button50 export" value="Export" />
		</form>
	</div>

	<div class="tab-page" id="tabMenu2">
		<form action="/components/sync/import_result.jsp?__setf=1&__sfu=1" name="importFilesForm" method="post" enctype="multipart/form-data" target="log_iframe">
			<input type="file" name="archive"><br>
			<input type="checkbox" name="backup" value="y">Vytvoriť zálohu (umožní vykonať rollback)<br>
			<input name="importSubmit" style="display: none;" type="submit" class="button50 import" value="Import" />
			<%sk.iway.iwcm.system.stripes.CSRF.writeCsrfTokenInputFiled(session, out);%>
		</form>
	</div>

	<div class="tab-page" id="tabMenu3">
		<%
		IwcmFile rollbackDir = IwcmFile.fromVirtualPath(Constants.getString("rollbackArchivePath"));
		IwcmFile[] rollbackVersionsArray = null;
		//ziskani vsetkych suborov v rollback adresari
		if(rollbackDir.exists())
			rollbackVersionsArray = rollbackDir.listFiles();
		if(rollbackVersionsArray != null && rollbackVersionsArray.length>0)
		{
			//vyfiltrovanie iba adresarov a uloznie do listu
			List<IwcmFile> rollbackVersions = new ArrayList<IwcmFile>();
			for(int i=0; i<rollbackVersionsArray.length; i++)
			{
				if(rollbackVersionsArray[i].isDirectory())
					rollbackVersions.add(rollbackVersionsArray[i]);
			}

			//zosortovanie listu podla datumu
			Collections.sort(rollbackVersions, new Comparator<IwcmFile>()
			{
				DateFormat f = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
				@Override
				public int compare(IwcmFile o1, IwcmFile o2)
				{
				    try
				    {
				    	return f.parse(o1.getName()).compareTo(f.parse(o2.getName()));
				    }
				    catch (Exception e)
				    {
				   		sk.iway.iwcm.Logger.error(e);
				   		return -1;
				    }
				}
		  	});%>

			<form action="/components/sync/rollback_result.jsp" name="rollbackFilesForm" target="log_iframe">
				<%for(int i=0; i<rollbackVersions.size(); i++)
				{
				//rollback-nut sa da iba najnovsi import (po rollbacku sa zaloha vymaze a bude sa dat zvolit predchadzajuci)
				%>
						<input type="radio" name="rollback" <%if(i!=rollbackVersions.size()-1) out.print("disabled=\"disabled\"");%> value="<%=Tools.escapeHtml(rollbackVersions.get(i).getVirtualPath())%>"><%=Tools.escapeHtml(rollbackVersions.get(i).getName())%><br>
				<%}%>
				<input name="rollbackSubmit" style="display: none;" type="submit" class="button50" value="Rollback" />
			</form>
		<%}
		else
			out.print("Nenašli sa žadne zálohy v adresári "+ Constants.getString("rollbackArchivePath"));
		%>
	</div>

	<div class="tab-page" id="tabMenu4">
		<iframe id="logId" name="log_iframe" width="600px" height="500px"></iframe>
	</div>
</div>

<script>

$(document).ready(function(){
	$(".input_snapshot").hide().val("");
	$('input:radio').change(function(){

		var radioPathValue = $("input[name='path']:checked").val();
		var radioFilterValue = $("input[name='filter']:checked").val();

		if( radioPathValue == "selected" ) {
			$(".checkboxes_zvoleneAdresare").show();
		} else {
			$(".checkboxes_zvoleneAdresare").hide();
			$(".checkboxes_zvoleneAdresare input[type='checkbox']").attr('checked', false);
		}

		if( radioPathValue == "manual" ) {
			$(".input_manualPath").show();
		} else {
			$(".input_manualPath").hide().val("");
		}

		if( radioFilterValue == "fromDate" ) {
			$(".fromDateBox").show();
		} else {
			$(".fromDateBox").hide();
			$(".input_fromDate").val("");
		}

		if( radioFilterValue == "snapshot" ) {
			$(".input_snapshot").show();
		} else {
			$(".input_snapshot").hide().val("");
		}
	});

	$('input.checkbox_sendEmail').change(function(){

		if ( $(this).prop('checked') ) {
			$(".input_emailAddress").show();
		} else {
			$(".input_emailAddress").hide().val("");
		}
	});

});

function Ok()
{
	//document.getElementById("logId").src = "about:blank";
	$('.tab-page:visible input:submit').click();
    checkForm.allreadySending = false;
	showHideTab('4');

	//document.exportFilesForm.exportSubmit.click();
	//document.importFilesForm.importSubmit.click();
	//document.rollbackFilesForm.rollbackSubmit.click();
	//cancelWindow();
}
</script>


<%@ include file="/admin/layout_bottom_dialog.jsp" %>
