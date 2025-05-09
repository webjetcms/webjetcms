<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.doc.*"%>

<%@ include file="/admin/skins/webjet8/layout_top_iframe.jsp" %>
<%
String key = Tools.getRequestParameter(request, "textKey");
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"), -1);
boolean actual = "true".equals(Tools.getRequestParameter(request, "actual"));
boolean onlyBody = "true".equals(Tools.getRequestParameter(request, "forwarddoccompare"));
%>
<style>
	body { overflow: hidden; background-image: url('/admin/skins/webjet8/assets/global/img/wj/popup_bg_header.png') !important; }
	ul.tab_menu { min-width: 600px; }
</style>
<script type="text/javascript">
function onlyBodyRedirect(docId, historyId)
{
	var onlyBody = document.getElementById("onlyBody");
	if(onlyBody.checked)
		window.top.location.href = window.top.location.pathname+"?historyid="+historyId+"&docid="+docId+"&onlyBody=true";
	else
		window.top.location.href = window.top.location.pathname+"?historyid="+historyId+"&docid="+docId;
}

function showChanges(checkbox)
{
	if (checkbox.checked)
	{
		window.parent.frames['left'].location.href='/admin/doc_diff.jsp?docid=<%=docId %>&historyid=<%=historyId + (onlyBody ? "&onlyBody=true" : "")%>';
	}
	else
	{
		window.parent.frames['left'].location.href='/showdoc.do?docid=<%=docId %>&historyid=<%=historyId + (onlyBody ? "&forwarddoccompare=true" : "")%>&NO_WJTOOLBAR=true';
	}
}
</script>
<div class="box_tab box_tab_thin" style="background-position: -10px;">

	<%
	if (Tools.isNotEmpty(key)) {%>
	<ul class="tab_menu">
		<li class="first openFirst">
			<a href="/showdoc.do?docid=<%=docId%>&NO_WJTOOLBAR=true<%=onlyBody ? "&forwarddoccompare=true" : ""%>" target="left" id="tabLink1" class="activeTab" onclick="showHideTab('1');">
				<iwcm:text key="<%=key %>" />
			</a>
		</li>
		<% if (actual) { %>
		<li style="color: white; padding-top: 13px;">
			<input type="checkbox" onclick="showChanges(this)" id="showChangesCb">&nbsp;<label for="showChangesCb"><iwcm:text key="editor.compare.showDiferences"/></label>

			<input type="checkbox" name="onlyBody" id="onlyBody" onclick="onlyBodyRedirect('<%=docId %>', '<%=historyId %>');" <%=onlyBody ? "checked=\"checked\"" : ""%>/>&nbsp;<label for="onlyBody"><iwcm:text key="admin.doc_compare_top.zobrazit_len_text_stranky"/></label>
		</li>
		<% } %>
	</ul>
	<%} %>

</div>
<div class="box_toggle">
	<div class="toggle_content" style="padding-top: 5px;">
		<div id="tabMenu1" style="height: 60px;">
			<%
			DocDB docDB = DocDB.getInstance();
			DocDetails doc;
			if (actual) doc = docDB.getDoc(docId);
			else doc = docDB.getDoc(docId, historyId);

			if (doc != null)
			{
				request.setAttribute("doc", doc);
				%>
				 <iwcm:text key="history.changedBy"/>:
			    <a href="mailto:<iwcm:strutsWrite name="doc" property="authorEmail"/>"><iwcm:strutsWrite name="doc" property="authorName"/></a>
			    <iwcm:strutsWrite name="doc" property="dateCreatedString"/> <iwcm:strutsWrite name="doc" property="timeCreatedString"/>&nbsp;<br />
				<%
				if (Tools.isNotEmpty(doc.getPublishStartString()))
				{
					if (doc.isPublicable())
					{
						%><br/><img src="/admin/images/warning.gif" align="absmiddle"/> <strong><iwcm:text key="document.start.publication"/> <iwcm:strutsWrite name="doc" property="publishStartString"/> <iwcm:strutsWrite name="doc" property="publishStartTimeString"/></strong><%
					}
					else if (historyId > 0)
					{
						%><br/><iwcm:text key="components.reservation.addReservation.date_from"/> <iwcm:strutsWrite name="doc" property="publishStartString"/> <iwcm:strutsWrite name="doc" property="publishStartTimeString"/><%
					}
				}
				if (Tools.isNotEmpty(doc.getPublishEndString()))
				{
					if (doc.isDisableAfterEnd())
					{
						%><br/><img src="/admin/images/warning.gif" align="absmiddle"/> <strong><iwcm:text key="document.end.publication"/> <iwcm:strutsWrite name="doc" property="publishEndString"/> <iwcm:strutsWrite name="doc" property="publishEndTimeString"/></strong><%
					}
					else if (historyId > 0)
					{
						%><br/><iwcm:text key="components.reservation.addReservation.date_to"/> <iwcm:strutsWrite name="doc" property="publishEndString"/> <iwcm:strutsWrite name="doc" property="publishEndTimeString"/><%
					}
				}
			}
			%>
		</div>
	</div>
</div>

<%@ include file="/admin/skins/webjet8/layout_bottom_iframe.jsp" %>
