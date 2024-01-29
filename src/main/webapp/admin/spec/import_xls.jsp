<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="menuWebpages|menuGallery"/>
<%@ include file="/admin/layout_top.jsp" %>

<H3>Import z Excelu</H3>

<br><br>

<%
request.setAttribute("xlsImportForm", new sk.iway.iwcm.xls.ImportXLSForm());
%>

<form:form method="post" modelAttribute="xlsImportForm" action="/admin/import/excel/" name="xlsImportForm" enctype="multipart/form-data">
 <table>
  <tr>
	<td>Vyberte súbor:</td>
	<td>
		<input type="file" name="file" size="30">
	</td>
  </tr>
  <tr>
	<td>Typ importu:</td>
	<td>
		<select name="type">
			<option value="sk.iway.XXX">vyberte si</option>
		   <option value="sk.iway.iwcm.doc.AtrExcelImport">AtrExcelImport</option>
		</select>
	</td>
  </tr>
  <tr>
	<td></td>
	<td align="right">
		<input type="submit" class="button50" value="Potvrdiť">
	</td>
  </tr>
 </table>
</form:form>


<%@ include file="/admin/layout_bottom.jsp" %>