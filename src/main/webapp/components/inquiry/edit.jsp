<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="menuInquiry"/>

<% request.setAttribute("cmpName", "inquiry"); %>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>

var error
function doOK()
{
   window.location.href="editor_component.jsp";
} // End function

function Ok()
{
   window.location.href="<iwcm:cp/>/components/inquiry/editor_component.jsp";
} // End function

//var leftPos = (screen.availWidth-650) / 2;
//var topPos = (screen.availHeight-540) / 2;
//window.moveTo(leftPos, topPos);
//window.resizeTo(650, 560);

</script>

<iframe src="/components/inquiry/admin_inquiry_list.jsp" width="500" height="280" scroll="auto" name="componentIframe"></iframe>

<jsp:include page="/components/bottom.jsp"/>