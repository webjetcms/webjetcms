<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.doc.*, sk.iway.iwcm.editor.*, java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<iwcm:menu notName="cmp_adminlog">
    <%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
    %>
</iwcm:menu>

<%@ include file="/admin/layout_top.jsp" %>
<script type="text/javascript" charset="windows-1250" src="/admin/scripts/dateTime.jsp"></script>
<script type="text/javascript" language="javascript" src="/admin/scripts/divpopup.js"></script>
<script language="JavaScript">
var helpLink = "";

function deleteHistoryOK(historyId, param)
{
	if(window.confirm('<iwcm:text key="components.adminlog.adminlog_wait_publishing.do_you_really_want_to_delete_history"/>') == true)
	{
		if(param == 1) document.actionForm.historyDeleteId.value = historyId;
		else document.actionForm.historyDirDeleteId.value = historyId;
		document.actionForm.submit();
	}
}
</script>
<%
int historyId = Tools.getIntValue(request.getParameter("historyDeleteId"),-1);	//id pre vymazanie web stranky z historie
int historyDirId = Tools.getIntValue(request.getParameter("historyDirDeleteId"),-1);	//id pre vymazanie adresara z historie
if(historyId>0) {
	HistoryDB historyDB = new HistoryDB(DBPool.getDBName(request));
	historyDB.deleteHistory(historyId);	//odstranenie publikacie v pripade stranky
}
if(historyDirId>0) {
	GroupPublisher.deleteRecord(historyDirId); //odstranenie publikacie v pripade adresara
}
DocDB ddb = DocDB.getInstance(true);
List<PublicableForm> publicableDocs = ddb.getPublicableDocs();		//nacitanie stranok na publikovani

GroupDetails gd = null;
List<DocDetails> publicableDocsWithoutNull = new ArrayList<DocDetails>();
List historyList = null;
for(DocDetails dd: publicableDocs){
	gd = (GroupsDB.getInstance()).getGroup(dd.getGroupId());
	if(gd != null) {	//ak stranka ma adresar, pridam ju do zoznamu
		HistoryDB historyDB = new HistoryDB(DBPool.getDBName(request));
		List list = historyDB.getHistory(dd.getDocId(), false, false);
		dd.setHistoryId(((DocDetails)list.get(0)).getHistoryId());	//nastavujem historiId -> pri strankach priamo z publicableDocs je historyId = 0
		dd.setExternalLink(gd.getFullPath());
		publicableDocsWithoutNull.add(dd);
	}
}

request.setAttribute( "publicableDocs", publicableDocs );
List<GroupDetails> publicableGroups = GroupPublisher.getPublicableDirs();  //nacitanie adresarov na publikovanie
request.setAttribute( "publicableDirs", publicableGroups );
%>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-list"></i><iwcm:text key="components.adminlog.adminlog"/><i class="fa fa-angle-right"></i><iwcm:text key="components.adminlog.wait_publishing"/></h1>
</div>

<!-- Tabuľka stránok pripravených na publikovanie -->
<h3><iwcm:text key="components.adminlog.adminlog_wait_publishing.table_doc_title"/></h3>

<display:table class="sort_table" uid="doc" name="publicableDocs" export="true" pagesize="20" defaultsort="1" defaultorder="ascending"  id="PublishDocTable" >
   <%DocDetails _doc = (DocDetails)PublishDocTable; %>
   <display:setProperty name="export.excel.filename" value="docs_waiting_for_publishing.xls" />
	<display:setProperty name="export.csv.filename" value="docs_waiting_for_publishing.csv" />
	<display:setProperty name="export.xml.filename" value="docs_waiting_for_publishing.xml" />
	<display:setProperty name="export.pdf.filename" value="docs_waiting_for_publishing.pdf" />

  <display:column property="docId" title="ID" sortable="true" headerClass="sortable" />
  <display:column property="title" titleKey="components.adminlog.adminlog_wait_publishing.title" sortable="true" headerClass="sortable" escapeXml="true"/>
  <display:column property="publishStartString" titleKey="components.adminlog.adminlog_wait_publishing.publishing_from" sortable="true" headerClass="sortable" escapeXml="true"/>
  <display:column property="publishEndString" titleKey="components.adminlog.adminlog_wait_publishing.publishing_to" sortable="true" headerClass="sortable" escapeXml="true"/>
  <display:column titleKey="components.adminlog.adminlog_wait_publishing.directory" sortable="true" headerClass="sortable">
  	<%
  		String link = GroupsDB.getInstance().getPathLinkForward(_doc.getGroupId(),null);
  		out.println(link);
  	%>
  </display:column>
  <display:column >
  <!-- Zobrazenie stranky -->
  <a href="/showdoc.do?docid=${PublishDocTable.docId}&historyid=${PublishDocTable.historyId}" target="_blank"  title="<iwcm:text key="groupslist.show_web_page"/>" ><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span></a>
  </display:column>
  <display:column>
  <!-- Zobrazenie historie -->
  <a href="javascript:popupDIV('/admin/dochistory.jsp?docid=${PublishDocTable.docId}')" title="<iwcm:text key="groupslist.show_history" />" ><span class="glyphicon glyphicon-time" aria-hidden="true"></span></a>
  </display:column>
  <display:column media="html" class="delete">
  <!-- Mazanie stranky z historie -->
    <a href="javascript:deleteHistoryOK('${PublishDocTable.historyId}', '1');" title="<iwcm:text key="button.delete"/>" /><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>
  </display:column>
</display:table>
<br/>

<div class="clearfix"></div>
<!-- Tabuľka adresarov pripravených na publikovanie -->
<h3><iwcm:text key="components.adminlog.adminlog_wait_publishing.table_dir_title"/></h3>

<display:table class="sort_table" name="publicableDirs" export="true" pagesize="20" defaultsort="1" defaultorder="ascending"  id="PublishDirTable" >
   <display:setProperty name="export.excel.filename" value="dirs_waiting_for_publishing.xls" />
	<display:setProperty name="export.csv.filename" value="dirs_waiting_for_publishing.csv" />
	<display:setProperty name="export.xml.filename" value="dirs_waiting_for_publishing.xml" />
	<display:setProperty name="export.pdf.filename" value="dirs_waiting_for_publishing.pdf" />

  <display:column property="groupId" title="ID" sortable="true" headerClass="sortable" />
  <display:column property="groupName" titleKey="components.adminlog.adminlog_wait_publishing.title" sortable="true" headerClass="sortable" escapeXml="true"/>
  <display:column property="fieldA" titleKey="components.adminlog.adminlog_wait_publishing.publishing_from" sortable="true" headerClass="sortable" escapeXml="true"/>
  <display:column property="fullPath" titleKey="components.adminlog.adminlog_wait_publishing.directory" sortable="true" headerClass="sortable" escapeXml="true" />

  <display:column media="html" class="delete">
    <a href="javascript:deleteHistoryOK('${PublishDirTable.tempId}', '2');" title="<iwcm:text key="button.delete"/>" /><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>
    <!-- temp_id -> schedule_id
     Mazanie adresara z historie -->
   </display:column>
</display:table>
<br/>

<form method="get" action="/components/adminlog/adminlog_wait_publishing.jsp" name="actionForm">
	<input type="hidden" name="historyDeleteId" value="" />
	<input type="hidden" name="historyDirDeleteId" value="" />
</form>

<!-- IFrame pre popup historie -->
<div id="divPopUp" style="position:absolute; width:450px; height:100px; z-index:130; left: 71px; top: 146px; visibility: hidden">
		<table width="450" bgcolor="white" cellspacing="0" cellpadding="0">
		<tr>
			<td align="left" bgcolor="#CCCCFF"><small><iwcm:text key="groupslist.web_page_history"/></small></td>
			<td align="right" bgcolor="#CCCCFF"><a href="javascript:popupHide();"><small><b>[X]</b></small></a></td>
		</tr>
		<tr>
			<td valign="top" colspan="2">
				<iframe src="/admin/divpopup-blank.jsp" name="popupIframe" style="border:solid #000000 1px" width="448" height="130" align="left" marginwidth="0" marginheight="0" frameborder="0" scrolling="auto"></iframe>
			</td>
		</tr>
		</table>
	</div>

<%@ include file="/admin/layout_bottom.jsp" %>