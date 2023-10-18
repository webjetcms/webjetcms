<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

  <td class="editorMiniEdit"><div class="tbSeparator"></div></td>

  <td style="position: relative;" class="tbButton" ID="DECMD_HTMLBox" TITLE="<iwcm:text key="editor.insert_htmlbox"/>" TBTYPE="toggle" onclick="OpenHTMLBox(ObjEditoriwcm)">
	 <img class="tbIcon" src="images/editor/htmlbox.gif" WIDTH="23" HEIGHT="22" border="0" align="absmiddle">
  </td>