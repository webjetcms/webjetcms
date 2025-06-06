
<%@page import="sk.iway.cloud.basket.UploadExcelFileAction"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
Prop prop = Prop.getInstance(lng);
if (request.getParameter("saveExcel") != null)
{
	pageContext.include("/sk/iway/basket/UploadExcelFile.action");
}


%>
<iwcm:stripForm name="saveExcelForm" id="saveExcelForm_ID" action="<%=PathFilter.getOrigPathUpload(request)%>" method="post" beanclass="sk.iway.cloud.basket.UploadExcelFileAction">
	<table>
		<tr>
			<td>
				<div class="fileFormHidden">
					<%
// 					String titlePhotoImageUrl = (SaveImageAction.getTitleImageURL(Tools.getIntValue(request.getParameter("projectId"), -1)));
// 					String photo1ImageUrl = (SaveImageAction.getPhoto1ImageURL(Tools.getIntValue(request.getParameter("projectId"), -1)));
// 					String photo2ImageUrl = (SaveImageAction.getPhoto2ImageURL(Tools.getIntValue(request.getParameter("projectId"), -1)));
					%>
					<stripes:file name="excelFile" style="margin: 0 0 10px 0;" /> <br>
<%-- 					<stripes:file name="photo1" style="margin: 0 0 10px 0;" /> <%=Tools.isNotEmpty(photo1ImageUrl) ? "Soubor je úspěšně nahrán." : "" %><br> --%>
<%-- 					<stripes:file name="photo2" style="margin: 0 0 10px 0;" /> <%=photo2ImageUrl != null ? "Soubor je úspěšně nahrán." : "" %><br> --%>

					<input type="hidden" name="saveExcel" value="true" />

<%-- 					<span class="imageLoaded" style="display:none;"><%=titlePhotoImageUrl != null ? titlePhotoImageUrl : "" %></span> --%>

					<input type="submit" class="button" id="saveExcelForm" name="saveExcel" value="<%=prop.getText("gallery.upload") %>" />
				</div>
			</td>
		</tr>
		<tr>
			<td>
				<input type="submit" style="position: absolute; left: -9999px; top: -9999px;" />
			</td>
		</tr>
	</table>

</iwcm:stripForm>