<%@ page import="sk.iway.iwcm.Tools" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true"/>

<html>
<head>
<title><iwcm:text key="approve.page.title"/></title>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
</head>

<frameset rows="75,*,100" cols="*" BORDERCOLOR="#edeff1" border="1" frameborder="1" framespacing="0">
	<frame name="leftTop" src="/admin/doc_compare_top.jsp?textKey=editor.delete.deletePage&docid=<%=Tools.getRequestParameter(request, "docid") %>&historyid=<%=Tools.getRequestParameter(request, "lasthid") %>" scrolling="no" BORDERCOLOR="#edeff1">
  <frame name="bodyFrameId"  id="bodyFrameId" src="/showdoc.do?<%
     if (Tools.getRequestParameter(request, "lasthid")!=null)
     {
        out.print("historyid="+Tools.getRequestParameter(request, "lasthid"));
        out.print("&docid="+Tools.getRequestParameter(request, "docid"));
     }
     else
     {
        out.print("docid="+Tools.getRequestParameter(request, "docid"));
     }
  %>&NO_WJTOOLBAR=true">
  <frame name="approveDelFormId"  id="approveDelFormId" src="approve_form_delete.jsp?historyid=<%=Tools.getRequestParameter(request, "historyid")%>&docid=<%=Tools.getRequestParameter(request, "docid")%>" scrolling="no" noresize>
</frameset>
<noframes><body bgcolor="#FFFFFF" text="#000000">

</body></noframes>
</html>
