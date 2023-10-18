<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

//skus najst nahradu za tuto stranku
//KED SPRAVITE KOPIU TEJTO KOMPONENTY NASLEDOVNY KOD ZMAZTE INAK SA TO ZACYKLI
String pageURL2 = "/components/bottom-public.jsp";
String nahrada2 = sk.iway.iwcm.tags.WriteTag.getCustomPage(pageURL2 , request);
if (pageURL2.equals(nahrada2)==false)
{
	pageContext.include(nahrada2);
	return;
}
%><%@ page pageEncoding="utf-8"%>
<%@page import="sk.iway.iwcm.stat.BrowserDetector"%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
	<% if (BrowserDetector.isSmartphoneOrTablet(request)==false) {%></div><% } %>
   </td>
</tr>
<tr id="dialogBottomButtonsRow">
   <td class="footer" colspan="2">
   	  <iwcm:write name="dialogBottomButtons"/> 
      <input id="btnOk" type="button" value="<iwcm:text key="button.ok"/>" onClick="doOK();"> 
	  <input id="btnCancel" type="button" value="<iwcm:text key="button.cancel"/>" onClick="window.close();"> 
   </td>
</tr>
</table>
</body>
<script type="text/javascript">
    onLoadHandler();
    try
    {
    	if (initHandler) initHandler();
    } catch (e) {}
    onResizeHandler();
   </script>
</html>