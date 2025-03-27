<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ include file="/admin/layout_top.jsp" %>
<iwcm:menu notName="importAegonFunds"><%response.sendRedirect("/admin/welcome.jsp");%></iwcm:menu>
<div class="row title">
	<h1 class="page-title"><i class=""></i>Import Fondov</h1>
</div>
<div class="box_toggle">
	<div class="toggle_content">
		<div id="tabMenu1">
			<form action="/admin/importxls.do" name="xlsImportForm" enctype="multipart/form-data">
				<table>
					<tr>
						<td>Vyberte súbor:</td>
						<td>
							<input type="file" styleClass="input"/>
						</td>
					</tr>
					<tr>
						<td></td>
						<td>
							<input type="hidden" name="type" value="sk.iway.IZPPriceImportCsv">
							<input type="hidden" name="lang" value="CZ">
						</td>
					</tr>
					<tr>
						<td></td>
						<td align="right">
							<input type="submit" class="button50" value="Potvrdiť">
						</td>
					</tr>
				</table>
			</form>
		</div>
	</div>
</div>
<%@ include file="/admin/layout_bottom.jsp" %>