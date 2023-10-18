<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
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
<iwcm:checkLogon admin="true" perms="cmp_sync"/>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.system.Modules"%>
<%
//synchronizacia adresara a web stranok zo vzdialeneho servera

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
	document.getElementById("btnSync1").click();
	<% } else { %>
	document.getElementById("btnLoadData1").click();
	<% } %>
}
</script>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.stripes.SyncDirAction"/>

<iwcm:stripForm id="syncForm" action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.stripes.SyncDirAction" method="post">
	<input type="hidden" name="syncDir" value="${param.syncDir}" />
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
	<stripes:checkbox name="createMissingTemplates" value="true"/><iwcm:text key="components.sync.vytvorit_chybajuce_sablony"/>
	<stripes:checkbox name="createMissingUserGroups" value="true"/><iwcm:text key="components.sync.vytvorit_skupiny_pouzivatelov"/>
	<input type="submit" id="btnSync1" name="btnSync" value="Synchronizovať" style="display: none;"/>
	<table class="sort_table" border="0">
		<thead>
			<tr>
				<th><iwcm:text key="components.sync.vzdialena_adresa"/></th>
				<th><iwcm:text key="components.sync.stav"/></th>
				<th><iwcm:text key="components.sync.synchronizovat"/></th>
				<th><iwcm:text key="components.sync.lokalna_adresa"/></th>
			</tr>
		</thead>
		<tbody>

		<tr><td colspan="4" style="background-color: #ddd;"><iwcm:text key="menu.web_sites" /></td></tr>
		<c:forEach items="${actionBean.remoteGroups}" var="remoteGroup">
			<tr>
				<td><span class="iconFolder">&nbsp;</span> <strong>${remoteGroup.fullPath}</strong></td>
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
				<td style="text-align: center;"><input type="checkbox" name="group_${remoteGroup.groupId}"<%=checked %>/></td>
				<td><span class="iconFolder">&nbsp;</span> <strong><bean:write name="localPath"/></strong></td>
			</tr>
			<c:if test="${!empty actionBean.remoteDocs}">
				<c:forEach items="${actionBean.remoteDocs}" var="remoteDoc">
					<c:if test="${remoteGroup.groupId==remoteDoc.groupId}">
						<tr>
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
							<td style="text-align: center;"><input type="checkbox" name="doc_${remoteDoc.docId}"<%=checked %>/></td>
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
					<input type="checkbox" name="file_${file.number}" <c:if test="${file.selected}">checked="checked"</c:if> />
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
					<input type="checkbox" name="banner_${banner.number}" <c:if test="${banner.selected}"> checked="checked" </c:if> />
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
					<input type="checkbox" name="inquiry_${i.number}" <c:if test="${i.selected}"> checked="checked" </c:if> />
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
					<input type="checkbox" name="galleryInfo_${info.number}" <c:if test="${info.selected}"> checked="checked" </c:if> />
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
						<input type="checkbox" name="galleryImage_${image.number}" <c:if test="${image.selected}"> checked="checked" </c:if> />
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
