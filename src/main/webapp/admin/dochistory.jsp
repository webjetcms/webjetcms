<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/>
<%
int doc_id = -1;
   try
   {
      doc_id = Integer.parseInt(Tools.getRequestParameter(request, "docid"));
      HistoryDB historyDB = new HistoryDB(DBPool.getDBName(request));
      int masterDocId = MultigroupMappingDB.getMasterDocId(doc_id);
      doc_id = masterDocId > 0 ? masterDocId : doc_id; //ak slave stranka, tak ukazem historiu master stranky
      List list = historyDB.getHistory(doc_id, false, false);
      if (list != null && list.size()>0)
      {
         pageContext.setAttribute("docHistory", list);
      }
   }
   catch (Exception ex)
   {

   }

   int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
   String domain = DocDB.getInstance().getDomain(docId);
   if (Tools.isNotEmpty(domain))
   {
   	session.setAttribute("preview.editorDomainName", domain);
   }
%>
<html>
<head>
  <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
  <link type="text/css" rel="stylesheet" href="css/style.css">
  <link type="text/css" rel="stylesheet" href="/admin/skins/webjet6/css/webjet6.css" />
 	<%=sk.iway.iwcm.Tools.insertJQuery(request) %>
  <script type="text/javascript">
      function retrieve(id)
      {
        if (confirm("Naozaj chcete tento dokument vybrať z archívu a použiť namiesto aktuálneho dokumentu?"))
        {
            parent.window.location="docdir.jsp?histId="+id;
        }
      }
      function editPage(docId, historyId)
      {
    	  window.parent.open("/admin/v9/webpages/web-pages-list/?docid="+docId+"&historyid="+historyId);
      }
      $(document).ready(function(){
			$('table.history tr').hover(function(){
				$(this).addClass('hover');
			},function(){
				$(this).removeClass('hover');
			});
      });
   </script>
   <style type="text/css">
	   .activetrue { font-weight: bold; }
	   .activefalse { font-weight: normal;}
	   body { background: none !important;}
   </style>
</head>
<body>
<iwcm:notPresent name="docHistory">
&nbsp;&nbsp;<iwcm:text key="history.empty"/>
</iwcm:notPresent>
<iwcm:present name="docHistory">
<table class="history">
	<tr>
		<th><iwcm:text key="history.date"/></th>
		<th><iwcm:text key="history.changedBy"/></th>
		<th><iwcm:text key="history.approvedBy"/></th>
		<th>&nbsp;</th>
	</tr>
<logic:iterate name="docHistory" id="docHist" type="sk.iway.iwcm.doc.DocDetails" indexId="index">
	<tr <% if(index % 2 == 0) out.println("class=\"even\""); else out.println("class=\"odd\""); %>>
		<td class="active<jsp:getProperty name="docHist" property="historyActual"/>"><jsp:getProperty name="docHist" property="historySaveDate"/></td>
		<td class="active<jsp:getProperty name="docHist" property="historyActual"/>"><jsp:getProperty name="docHist" property="authorName"/></td>
		<td class="active<jsp:getProperty name="docHist" property="historyActual"/>"><jsp:getProperty name="docHist" property="historyApprovedByName"/><iwcm:notEmpty name="docHist" property="historyApproveDate"><br/><jsp:getProperty name="docHist" property="historyApproveDate"/></iwcm:notEmpty></td>
		<td class="active<jsp:getProperty name="docHist" property="historyActual"/>">
			<a href="/showdoc.do?docid=<%=doc_id%>&historyid=<jsp:getProperty name="docHist" property="historyId"/>" target="_blank" class="iconPreview" title="<iwcm:text key="history.showPage"/>">&nbsp;</a>
			<a href="javascript:editPage(<jsp:getProperty name="docHist" property="docId"/>, <jsp:getProperty name="docHist" property="historyId"/>)" title="<iwcm:text key="history.editPage"/>" class="iconEdit">&nbsp;</a>
			<a href="/admin/doc_compare.jsp?historyid=<jsp:getProperty name="docHist" property="historyId"/>&docid=<%=doc_id%>" target="_blank" class="iconCompare" title="<iwcm:text key="groupslist.compare" />">&nbsp;</a>
		</td>
	</tr>
</logic:iterate>
</table>
</iwcm:present>
