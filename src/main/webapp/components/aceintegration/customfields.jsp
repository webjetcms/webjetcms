<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="windows-1250"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%
DocDetails doc = (DocDetails)request.getAttribute("docDetails");

if(doc == null) return;

out.print("<p>" + doc.getTempId() + "</p>");
%>
<p class="noprefix-a"><iwcm:text key="editor.field_a"/></p>
<p class="noprefix-test"><iwcm:text key="editor.field_tempgroup"/></p>
<p><iwcm:text key="temp-3.editor.field_a"/></p>
<p><iwcm:text key="demojet.editor.field_tempgroup"/></p>
<p><iwcm:text key='<%="temp-"+doc.getTempId()+".editor.field_a"%>'/></p>