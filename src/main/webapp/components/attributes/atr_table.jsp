<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.doc.*"%>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
DocDetails actualDoc = (DocDetails)request.getAttribute("docDetails");
if (actualDoc == null) return;

PageParams pageParams = new PageParams(request);
int dirId = pageParams.getIntValue("dirId", actualDoc.getGroupId());
boolean includeSub = pageParams.getBooleanValue("includeSub", true);
String group = pageParams.getValue("group", "default");
boolean includeCurrentPage = pageParams.getBooleanValue("includeCurrentPage", false);

if (includeCurrentPage==false) {
   request.setAttribute("getAtributesTableSqlWhere", "AND d.doc_id!=" + actualDoc.getDocId());
}

List atrTable = AtrDB.search(dirId, includeSub, group, request);

//usortuj podla nazvu
Collections.sort(atrTable, new Comparator() {

	public int compare(Object o1, Object o2)
	{
		AtrDocBean b1 = (AtrDocBean)o1;
		AtrDocBean b2 = (AtrDocBean)o2;

		return(b1.getTitle().compareTo(b2.getTitle()));
	}

});

String sortBy = request.getParameter("sortBy");
if (sortBy == null) sortBy = pageParams.getValue("sortBy", null);
if (sortBy != null && sortBy.equals("title")==false)
{
	Collections.sort(atrTable, new AtrComparator(sortBy));
}

request.setAttribute("atrTable", atrTable);

int counter = 0;

DocDB docDB = DocDB.getInstance();
%>
<div class="table-responsive">
   <table class="table table-sm tabulkaStandard atrTable">
   <iwcm:iterate id="atrDocBean" name="atrTable" type="sk.iway.iwcm.doc.AtrDocBean" indexId="index">
      <% if (index.intValue()==0) { %>
         <tr class="atrTableHeader">
            <td>&nbsp;</td>
            <iwcm:iterate id="atrBean" name="atrDocBean" property="atrList" type="sk.iway.iwcm.doc.AtrBean">
               <td><bean:write name="atrBean" property="atrName"/></td>
            </iwcm:iterate>
         </tr>
      <% }
      counter = 1 - counter;
      %>
      <tr class='atrTableRow<%=counter%>'>
         <td class='atrTableLink'><a href="<%=docDB.getDocLink(atrDocBean.getDocId(), request)%>"><bean:write name="atrDocBean" property="title"/></a></td>
         <iwcm:iterate id="atrBean" name="atrDocBean" property="atrList" type="sk.iway.iwcm.doc.AtrBean">
            <td align="center"><bean:write name="atrBean" property="valueHtml" filter="false"/></td>
         </iwcm:iterate>
      </tr>
   </iwcm:iterate>
   </table>
   <% if (atrTable.size()<1) { %>
      <iwcm:text key="components.search.no_matches_found"/>
   <% } %>
</div>