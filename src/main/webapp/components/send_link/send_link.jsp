
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@page import="org.apache.commons.codec.binary.Base64"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%
PageParams pageParams = new PageParams(request);
String sendType = pageParams.getValue("sendType", "link");
boolean showLink = false;

if ("link".equals(sendType)) showLink = true;
else if ("page".equals(sendType)) showLink = true; 

request.setAttribute("showLink", ""+showLink);

String urlParams = "";

String qs = (String)request.getAttribute("path_filter_query_string");
if (Tools.isNotEmpty(qs))
{
	Base64 b64 = new Base64();
	String base64encoded = new String(b64.encode(qs.getBytes()));
	urlParams = Tools.replace(base64encoded, "=", "|");
}
%>
<%=Tools.insertJQuery(request) %>

<!-- // dialogove okno s funkciami -->
<jsp:include page="/components/dialog.jsp" /> 

<iwcm:equal name="showLink" value="true">
	<a class="sendLink" href="javascript:void(0);" onclick="openWJDialog('sendLink', '/components/send_link/send_link_form.jsp?docid=<%=Tools.getDocId(request)%>&sendType=<%=sendType%>&qs=<%=urlParams %>');" title="<iwcm:text key="components.send_link.title" />"><iwcm:text key="components.send_link.title" /></a>
</iwcm:equal>