<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="menuWebpages|menuGallery"/>
<%@ include file="/admin/layout_top.jsp" %>

<H3>Import z Excelu</H3>

<br><br>

<html:form action="/admin/importxls.do" name="xlsImportForm" type="sk.iway.iwcm.xls.ImportXLSForm" enctype="multipart/form-data"> 
 <table>
  <tr>
	<td>Vyberte súbor:</td>
	<td>
		<html:file property="file" styleClass="input"/>
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
</html:form>


<%@ include file="/admin/layout_bottom.jsp" %>