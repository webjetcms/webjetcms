<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_sync"/>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.system.Modules"%>
<%
//synchronizacia adresara a web stranok zo vzdialeneho servera

request.setAttribute("closeTable", "true");
request.setAttribute("cmpName", "syncDir");
request.setAttribute("dialogTitleKey", "components.import_web_pages.xml.dialogTitle");
request.setAttribute("dialogDescKey", "components.import_web_pages.xml.dialogDesc");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>


<div class="padding10">
<%
boolean showForm = true;
if (Tools.getRequestParameter(request, "btnLoadData")!=null || Tools.getRequestParameter(request, "btnSync")!=null)
{
	System.out.println("---- sync dir start ----");
	pageContext.include("/SyncDir.action");
	System.out.println("----  sync dir end  ----");
}
if (request.getAttribute("showConnectionInfo")==null && (Tools.getRequestParameter(request, "btnLoadData")!=null || Tools.getRequestParameter(request, "btnSync")!=null))
{
	showForm = false;
}
//import z ineho WeBJETu priamo uz nie je podporovany
showForm = false;
%>

<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Hashtable"%>

<%if (showForm) {	%>
	<script type="text/javascript" src="/components/form/check_form.js"></script>
<% } %>
<script type="text/javascript">
function Ok()
{
	<%if (showForm==false) {%>

	setSelectedOptionsRequestParams();
	<% } else { %>
	document.getElementById("btnLoadData1").click();
	<% } %>
}

//sivan - #57441

let prefixes = ["group", "doc", "file", "banner", "inquiry", "galleryInfo", "galleryImage"];

function setSelectedOptionsRequestParams() {
	for(let i = 0; i < prefixes.length; i++) {
		setRequestParam(prefixes[i]);
	}

	document.getElementById("btnSync1").click();
}

function setRequestParam(prefix) {
	//Get all selected elemnt with given class (prefix + "-checkbox")
	let selectedDatas = $("." + prefix + "-checkbox:checked");

	//Loop through all selected elements, get data-id attribute value and concate string
	let seletedIdsString = "";
	if(selectedDatas != undefined && selectedDatas != null && selectedDatas.length > 0) {
		for (let i = 0; i < selectedDatas.length; i++) {
			let dataId = selectedDatas[i].getAttribute("data-id");
			seletedIdsString += dataId + ",";
		}
	}

	//Find hidden input field with id = prefix + "_options" and set value to it
	let input = $("#" + prefix + "_options");
	if(input != undefined && input != null && input.length > 0) {
		input.val(seletedIdsString);
	}
}
</script>

<style>
	.statHeader {
		position: sticky;
		top: 0;
		padding: 10px;
		padding-bottom: 0;
	}
	.statHeader label {
		min-width: 150px;
	}

	.border-bottom {
		border-bottom: 1px solid;
		margin-bottom: 10px;
		border-color: lightgray;
		padding-bottom: 10px;
	}

	.actionBtn {
		background-color: white;
		color: #13151b;
		border-color: #868ea5;
		border-width: 1px;
		border-style: solid;
		display: flex;
	}
</style>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.stripes.SyncDirAction"/>

<iwcm:stripForm id="syncForm" action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.stripes.SyncDirAction" method="post" style="display:none;">
	<input type="hidden" name="syncDir" value="<%=Tools.getParameterNotNull(request, "syncDir")%>" />
	<input type="hidden" name="compareBy" value="<%=Tools.getParameterNotNull(request, "compareBy")%>" />
	<table border="0" style="<%if (showForm==false) out.print("display: none;");	%>">
		<tr>
			<td><iwcm:text key="components.syncDir.localGroupId"/></td>
			<td><stripes:text name="localGroupId" size="5" maxlength="5" class="required numbers"/></td>
		</tr>
		<tr>
			<td><iwcm:text key="components.syncDir.remoteGroupId"/></td>
			<td><stripes:text name="remoteGroupId" size="5" maxlength="5" class="required numbers"/></td>
		</tr>
		<tr>
			<td></td>
			<td><input type="submit" id="btnLoadData1" name="btnLoadData" value="Porovnať" style="display: none;"/></td>
		</tr>
	</table>

<c:if test="${actionBean.remoteGroupId>0 && !empty actionBean.remoteGroups}">
	<%
	GroupDetails localGroup = null;

	DocDB docDB = DocDB.getInstance();
	DocDetails localDoc = null;

	Map<Integer, Integer> existingLocalDocIds = null;
	String checked = "";
	%>

	<script type="text/javascript">
		function selectAll() {
			$("#deselectAllBtn").prop("checked", false);
			$("#syncForm > table.sort_table > tbody > tr > td > input[type=checkbox]").prop("checked", true);
			for(let i = 0; i < prefixes.length; i++) {
				setSelectedInfo(prefixes[i]);
			}
		}

		function deselectAll() {
			$("#selectAllBtn").prop("checked", false);
			$("#syncForm > table.sort_table > tbody > tr > td > input[type=checkbox]").prop("checked", false);
			for(let i = 0; i < prefixes.length; i++) {
				setSelectedInfo(prefixes[i]);
			}
		}

		function setSelectedInfo(prefix) {
			if(prefix == undefined || prefix == null || prefix == "") return;
			$("#" + prefix + "_options_info").val( $("input." + prefix + "-checkbox:checked").length + " / " + $("input." + prefix + "-checkbox").length );
		}

		function setOnClickEvent(prefix) {
			if(prefix == undefined || prefix == null || prefix == "") return;
			$("input." + prefix + "-checkbox").click(function() {
				setSelectedInfo(prefix);
			});
		}

		function collpaseFolder(collapseId) {
			let elements = $("tr[data-collapse-id='" + collapseId + "']");
			if(elements != undefined || elements != null || elements.length > 0) {
				let isCollapsed = elements[0].style.display == "none";
				let btn = $("input[data-collapse-btn-id='" + collapseId + "']");

				if(isCollapsed == true) {
					btn.removeClass("btnPlus");
					btn.addClass("btnMinus");

					for(let i = 0; i < elements.length; i++) {
						elements[i].style.display = "";
					}
				} else {
					btn.removeClass("btnMinus");
					btn.addClass("btnPlus");

					for(let i = 0; i < elements.length; i++) {
						elements[i].style.display = "none";
					}
				}
			}
		}

		function closeAllFolders() {
			//Set DOC tr elements to hide
			$("#syncForm").find("tr[data-collapse-id]").hide();
			//Set button's to plus
			$("#syncForm").find("input[data-collapse-btn-id]").removeClass("btnMinus");
			$("#syncForm").find("input[data-collapse-btn-id]").addClass("btnPlus");
		}

		function openAllFolders() {
			//Set DOC tr elements to show
			$("#syncForm").find("tr[data-collapse-id]").css( "display", "");
			//Set button's to minus
			$("#syncForm").find("input[data-collapse-btn-id]").removeClass("btnPlus");
			$("#syncForm").find("input[data-collapse-btn-id]").addClass("btnMinus");
		}

		document.addEventListener("DOMContentLoaded", function() {
			// HIde status messages
			var statusMsgs = document.getElementsByClassName("statusMsg");
			if(statusMsgs != undefined || statusMsgs != null) {
				for (var i = 0; i < statusMsgs.length; i++) {
					statusMsgs[i].style.display = "none";
				}
			}

			// Load stat info
			for(let i = 0; i < prefixes.length; i++) {
				setSelectedInfo(prefixes[i]);
				setOnClickEvent(prefixes[i]);
			}

			// Show stat header
			$(".statHeader").attr("hidden",false);

			//Show table
			$("#syncForm").css( "display", "");
		});
	</script>

	<div class="statHeader bg-light">
		<div class="row border-bottom">
			<div class="col-sm">
				<label for="group_options_info" class="form-label"> <iwcm:text key="components.syncDirAction.selected_groups"/>:</label>
				<input id="group_options_info" class="form-control-sm" disabled></br></br>

				<label for="doc_options_info" class="form-label"> <iwcm:text key="components.syncDirAction.selected_docs"/>:</label>
				<input id="doc_options_info" class="form-control-sm" disabled></br></br>

				<label for="file_options_info" class="form-label"> <iwcm:text key="components.syncDirAction.selected_files"/>:</label>
				<input id="file_options_info" class="form-control-sm" disabled>
			</div>

			<div class="col-sm">
				<label for="banner_options_info" class="form-label"> <iwcm:text key="components.syncDirAction.selected_banners"/>:</label>
				<input id="banner_options_info" class="form-control-sm" disabled></br></br>

				<label for="inquiry_option_infos" class="form-label"> <iwcm:text key="components.syncDirAction.selected_inquiries"/>:</label>
				<input id="inquiry_options_info" class="form-control-sm" disabled></br></br>

				<button id="selectAllBtn" class="btn btn-outline-secondary" type="button" onClick="selectAll();" style="width: 130px;"><iwcm:text key="usersedit.select_all"/></button>
				<button id="deselectAllBtn" class="btn btn-outline-secondary" type="button" onClick="deselectAll();" style="width: 130px;"><iwcm:text key="usersedit.deselect_all"/></button>
			</div>

			<div class="col-sm">
				<label for="galleryInfo_options_info" class="form-label"> <iwcm:text key="components.syncDirAction.selected_galleryInfos"/>:</label>
				<input id="galleryInfo_options_info" class="form-control-sm" disabled></br></br>

				<label for="galleryImage_options_info" class="form-label"> <iwcm:text key="components.syncDirAction.selected_galleryImages"/>:</label>
				<input id="galleryImage_options_info" class="form-control-sm" disabled></br></br>

				<button id="closeAllFoldersBtn" class="btn btn-outline-secondary" type="button" onClick="closeAllFolders();"><iwcm:text key="components.syncDirAction.close_all_folders"/></button>
				<button id="openAllFoldersBtn" class="btn btn-outline-secondary" type="button" onClick="openAllFolders();"><iwcm:text key="components.syncDirAction.open_all_folders"/></button>
			</div>
		</div>
	</div>

	<div class="col align-self-end">
		<div class="form-check form-switch form-check-inline">
			<stripes:checkbox id="createMissingTemplates" name="createMissingTemplates" value="true" class="form-check-input"/>
			<label for="createMissingTemplates" class="form-check-label"><iwcm:text key="components.sync.vytvorit_chybajuce_sablony"/></label>
		</div>
		<div class="form-check form-switch form-check-inline">
			<stripes:checkbox id="createMissingUserGroups" name="createMissingUserGroups" value="true" class="form-check-input"/>
			<label for="createMissingUserGroups" class="form-check-label"><iwcm:text key="components.sync.vytvorit_skupiny_pouzivatelov"/></label>
		</div>
	</div>
	<input type="submit" id="btnSync1" name="btnSync" value="Synchronizovať" style="display: none;"/>
	<table class="sort_table" border="0" style="min-width: 99%;">
		<thead>
			<tr>
				<th><iwcm:text key="components.sync.vzdialena_adresa"/></th>
				<th><iwcm:text key="components.sync.stav"/></th>
				<th><iwcm:text key="components.sync.synchronizovat"/></th>
				<th><iwcm:text key="components.sync.lokalna_adresa"/></th>
			</tr>
		</thead>
		<tbody>

		<tr style="display:none !important;">
			<td> <input id="group_options" name="group_options"/> </td>
			<td> <input id="doc_options" name="doc_options"/> </td>
			<td> <input id="file_options" name="file_options"/> </td>
			<td> <input id="banner_options" name="banner_options"/> </td>
			<td> <input id="inquiry_options" name="inquiry_options"/> </td>
			<td> <input id="galleryInfo_options" name="galleryInfo_options"/> </td>
			<td> <input id="galleryImage_options" name="galleryImage_options"/> </td>
		</tr>

		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="menu.web_sites" /></td></tr>
		<c:forEach items="${actionBean.remoteGroups}" var="remoteGroup">
			<tr>
				<td> <input type="button" class="btnMinus" data-collapse-btn-id="${remoteGroup.groupId}" style="display: inline-block;" onClick="collpaseFolder(${remoteGroup.groupId})"/> <span class="iconFolder">&nbsp;</span> <strong>${remoteGroup.fullPath}</strong></td>
				<td>
					<%
					checked = "";
					GroupDetails remoteGroup = (GroupDetails)pageContext.getAttribute("remoteGroup");
					//urci local cestu

					String localPath = actionBean.getLocalPath(remoteGroup);
					//out.print(localPath);
					pageContext.setAttribute("localPath", localPath);
					//otestuj, ci local path existuje
					localGroup = actionBean.getLocalGroup(localPath);
					if (localGroup!=null)
					{
						out.print(" <span style='color:green;'>"+prop2.getText("components.sync.lokalne_existuje")+"</span>");
					}
					else
					{
						out.print(" <span style='color:red;'>"+prop2.getText("components.sync.lokalne_neexistuje")+"</span>");
						checked = " checked=\"checked\"";
					}
					existingLocalDocIds = new Hashtable<Integer, Integer>();
					%>
				</td>
				<td style="text-align: center;"><input type="checkbox" class="group-checkbox" data-id="group_${remoteGroup.groupId}"<%=checked %>/></td>
				<td><span class="iconFolder">&nbsp;</span> <strong><iwcm:beanWrite name="localPath"/></strong></td>
			</tr>
			<c:if test="${!empty actionBean.remoteDocs}">
				<c:forEach items="${actionBean.remoteDocs}" var="remoteDoc">
					<c:if test="${remoteGroup.groupId==remoteDoc.groupId}">
						<tr data-collapse-id="${remoteGroup.groupId}">
							<td style="padding-left: 30px;"><span class="iconPreview">&nbsp;</span>${remoteDoc.title}</td>
							<td>
							<%
							checked = "";
							DocDetails remoteDoc = (DocDetails)pageContext.getAttribute("remoteDoc");
							request.removeAttribute("localDoc");
							if (localGroup != null)
							{
								localDoc = actionBean.getLocalDoc(remoteDoc);
								if (localDoc != null)
								{
									request.setAttribute("localDoc", localDoc);
									existingLocalDocIds.put(localDoc.getDocId(), remoteDoc.getDocId());

									//porovnaj datumy poslednej zmeny
									if (remoteDoc.getDateCreated() > localDoc.getDateCreated())
									{
										out.print(" <span style='color:orange;'>"+prop2.getText("components.sync.vzdialena_stranka_je_novsia")+"</span>");
										checked = " checked=\"checked\"";
									}
									else
									{
										out.print(" <span style='color:green;'>"+prop2.getText("components.sync.lokalne_existuje")+"</span>");
									}
								}
								else
								{
									out.print(" <span style='color:red;'>"+prop2.getText("components.sync.lokalne_neexistuje")+"</span>");
									checked = " checked=\"checked\"";
								}
							}
							else
							{
								out.print(" <span style='color:red;'>"+prop2.getText("components.sync.lokalne_neexistuje")+"</span>");
								checked = " checked=\"checked\"";
							}
							%>
							</td>
							<td style="text-align: center;"><input type="checkbox" class="doc-checkbox"  data-id="doc_${remoteDoc.docId}"<%=checked %>/></td>
							<td style="padding-left: 30px;">
								<c:if test="${!empty localDoc}"><span class="iconPreview">&nbsp;</span> <a href="${localDoc.virtualPath}" target="_blank">${localDoc.title}</a></c:if>
							</td>
						</tr>
					</c:if>
				</c:forEach>
				<%
				//vypiseme lokalne existujuce stranky, ktore sa nenachadzaju na remote serveri
				if (localGroup!=null)
				{
					List<DocDetails> localDocs = docDB.getBasicDocDetailsByGroup(localGroup.getGroupId(), DocDB.ORDER_PRIORITY);
					Iterator<DocDetails> iter = localDocs.iterator();
					while (iter.hasNext())
					{
						localDoc = iter.next();
						if (existingLocalDocIds.get(localDoc.getDocId())==null)
						{
							request.setAttribute("localDoc", localDoc);
							%>
							<tr>
								<td style="padding-left: 30px;"></td>
								<td style='color: orange;'><iwcm:text key="components.sync.vzdialene_neexistuje"/></td>
								<td></td>
								<td style="padding-left: 30px;"><span class="iconPreview">&nbsp;</span> <a href="${localDoc.virtualPath}" target="_blank">${localDoc.title}</a></td>
							</tr>
							<%
						}
					}
				}
				%>
			</c:if>
		</c:forEach>
		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="menu.fbrowser" /></td></tr>
		<c:forEach items="${actionBean.remoteContentFiles}" var="file">
			<tr>
				<td><span class="iconPreview">&nbsp;</span> <c:out value="${file.path}"/></td>
				<td>
					<c:if test="${file.missing}"><span style='color:red;'><iwcm:text key="components.sync.content.file.missing"/></span></c:if>
					<c:if test="${file.newer}"><span style='color:orange;'><iwcm:text key="components.sync.content.file.newer"/></span></c:if>
					<c:if test="${file.older}"><span style='color:green;'><iwcm:text key="components.sync.content.file.older"/></span></c:if>
					<c:if test="${file.currentDifferent}"><span style='color:orange;'><iwcm:text key="components.sync.content.file.different"/></span></c:if>
					<c:if test="${file.currentSame}"><span style='color:green;'><iwcm:text key="components.sync.content.file.same"/></span></c:if>
				</td>
				<td style="text-align: center;">
					<input type="checkbox" class="file-checkbox" data-id="file_${file.number}" <c:if test="${file.selected}">checked="checked"</c:if> />
				</td>
				<td><c:if test="${!empty(file.localPath)}"><span class="iconPreview">&nbsp;</span> <c:out value="${file.localPath}"/></c:if></td>
			</tr>
		</c:forEach>

		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="components.banner.title" /></td></tr>
		<c:forEach items="${actionBean.remoteContentBanners}" var="banner">
			<tr>
				<td><span class="iconPreview">&nbsp;</span> <c:out value="${banner.group}"/>: <c:out value="${banner.name}"/></td>
				<td>
					<c:if test="${banner.local}"><span style='color:green;'><iwcm:text key="components.sync.lokalne_existuje"/></span></c:if>
					<c:if test="${!banner.local}"><span style='color:red;'><iwcm:text key="components.sync.lokalne_neexistuje"/></span></c:if>
				</td>
				<td style="text-align: center;">
					<input type="checkbox" class="banner-checkbox" data-id="banner_${banner.number}" <c:if test="${banner.selected}"> checked="checked" </c:if> />
				</td>
				<td><c:if test="${banner.local}"><span class="iconPreview">&nbsp;</span> <c:out value="${banner.group}"/>: <c:out value="${banner.name}"/></c:if></td>
			</tr>
		</c:forEach>

		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="menu.inquiry" /></td></tr>
		<c:forEach items="${actionBean.remoteContentInquiries}" var="i">
			<tr>
				<td><span class="iconPreview">&nbsp;</span> <c:out value="${i.group}"/>: <span title='<c:out value="${i.question}"/>'><c:out value="${i.questionShort}"/></span></td>
				<td>
					<c:if test="${i.local}"><span style='color:green;'><iwcm:text key="components.sync.lokalne_existuje"/></span></c:if>
					<c:if test="${!i.local}"><span style='color:red;'><iwcm:text key="components.sync.lokalne_neexistuje"/></span></c:if>
				</td>
				<td style="text-align: center;">
					<input type="checkbox" class="inquiry-checkbox" data-id="inquiry_${i.number}" <c:if test="${i.selected}"> checked="checked" </c:if> />
				</td>
				<td><c:if test="${i.local}"><span class="iconPreview">&nbsp;</span> <c:out value="${i.group}"/>: <span title='<c:out value="${i.question}"/>'><c:out value="${i.questionShort}"/></span></c:if></td>
			</tr>
		</c:forEach>

		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="gallery.menu" /></td></tr>
		<c:forEach items="${actionBean.remoteContentGalleryInfos}" var="info">
			<tr>
				<td><span class="iconFolder">&nbsp;</span> <c:out value="${info.path}"/></td>
				<td>
					<c:if test="${info.local}"><span style='color:green;'><iwcm:text key="components.sync.lokalne_existuje"/></span></c:if>
					<c:if test="${!info.local}"><span style='color:red;'><iwcm:text key="components.sync.lokalne_neexistuje"/></span></c:if>
				</td>
				<td style="text-align: center;">
					<input type="checkbox" class="galleryInfo-checkbox" data-id="galleryInfo_${info.number}" <c:if test="${info.selected}"> checked="checked" </c:if> />
				</td>
				<td><c:if test="${info.local}"><span class="iconFolder">&nbsp;</span> <c:out value="${info.path}"/></c:if></td>
			</tr>
			<c:forEach items="${actionBean.remoteContentGalleryImages}" var="image"><c:if test="${image.path == info.path}">
				<tr>
					<td style="padding-left: 30px;"><span class="iconPreview">&nbsp;</span> <c:out value="${image.name}"/></td>
					<td>
						<c:if test="${image.local}"><span style='color:green;'><iwcm:text key="components.sync.lokalne_existuje"/></span></c:if>
						<c:if test="${!image.local}"><span style='color:red;'><iwcm:text key="components.sync.lokalne_neexistuje"/></span></c:if>
					</td>
					<td style="text-align: center;">
						<input type="checkbox" class="galleryImage-checkbox" data-id="galleryImage_${image.number}" <c:if test="${image.selected}"> checked="checked" </c:if> />
					</td>
					<td style="padding-left: 30px;"><c:if test="${image.local}"><span class="iconPreview">&nbsp;</span> <c:out value="${image.name}"/></c:if></td>
				</tr>
			</c:if></c:forEach>
		</c:forEach>
		<%
		IwcmFile pobockyComponents = new IwcmFile(sk.iway.iwcm.Tools.getRealPath("/components/raiffeisen/pobocky/admin_pobocky_list.jsp"));
		if(Modules.getInstance().isAvailable("cmp_pobocky") && pobockyComponents.exists())	//importujem iba ak je modul pristupny a existuju komponenty pre pobocky
		{ %>
		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="raiffeisen.pobocky.import" /></td></tr>
		<c:forEach items="${actionBean.remoteContentPobocky}" var="pobocky">
		  <tr>
		    <td><span class="iconPreview">&nbsp;</span> <c:out value="${pobocky.adresa}"/></td>
		    <td>
		      <c:if test="${pobocky.local}"><span style='color:green;'><iwcm:text key="components.sync.lokalne_existuje"/></span></c:if>
		      <c:if test="${!pobocky.local}"><span style='color:red;'><iwcm:text key="components.sync.lokalne_neexistuje"/></span></c:if>
		    </td>
		    <td style="text-align: center;">
		      <input type="checkbox" name="pobocky_${pobocky.number}" <c:if test="${pobocky.selected}"> checked="checked" </c:if> />
		    </td>
		    <td><c:if test="${pobocky.local}"><span class="iconPreview">&nbsp;</span> <c:out value="${pobocky.adresa}"/></c:if></td>
		  </tr>
		</c:forEach>
	<% } %>
		</tbody>
	</table>
</c:if>

</iwcm:stripForm>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>

<script type="text/javascript">
   <%if (showForm==false) {%>
	document.getElementById("btnOk").value = '<%=prop2.getText("components.sync.synchronizovat")%>';
    document.getElementById("btnOk").style.width = "130px";
	try {
		window.opener?.webpagesDatatable?.ajax?.reload();
		window.opener?.$('#SomStromcek').jstree(true).refresh();
	} catch (e) {}
	<% } else { %>
		resizeDialog(980, 630);
		document.getElementById("btnOk").value = '<%=prop2.getText("components.sync.porovnat")%>';
	<% } %>
</script>
