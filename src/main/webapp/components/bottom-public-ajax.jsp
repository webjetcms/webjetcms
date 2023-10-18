<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

//skus najst nahradu za tuto stranku
//KED SPRAVITE KOPIU TEJTO KOMPONENTY NASLEDOVNY KOD ZMAZTE INAK SA TO ZACYKLI
String pageURL = "/components/bottom-public.jsp";
String nahrada = sk.iway.iwcm.tags.WriteTag.getCustomPage(pageURL , request);
if (pageURL.equals(nahrada)==false)
{
	pageContext.include(nahrada);
	return;
}
%><%@ page pageEncoding="utf-8"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
	</div>
   </td>
</tr>
<tr id="dialogBottomButtonsRow">
   <td class="footer" colspan="2">
   	  <iwcm:write name="dialogBottomButtons"/>
   </td>
</tr>
</table>
