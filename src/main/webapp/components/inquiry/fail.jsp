<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<jsp:useBean id="answerForm" type="sk.iway.iwcm.inquiry.AnswerForm" scope="request"/>
<HTML>
<HEAD>
<TITLE>TITLE</TITLE>
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=windows-1250">
<link rel="stylesheet" href="css/page.css" type="text/css">
</HEAD>
<BODY class="inquiryPopup">

<iwcm:present name="spam"><iwcm:text key="components.inquiry.spamDetected"/></iwcm:present>
<iwcm:notPresent name="spam"><bean:write name="answerForm" property="answerTextFail"/></iwcm:notPresent>    

</BODY>
</HTML>

<script type="text/javascript">
if (opener && opener!=null && opener.location)
{
   opener.location.reload();
   //setTimeout("window.close();", 1500);
}
</script>