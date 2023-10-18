<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"%>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%
    //otestuj ci existuje nahrada za tuto stranku
    String originalJsp = "/components/file_archiv/manager_category.jsp";
    String forward = sk.iway.iwcm.common.WriteTagToolsForCore.getCustomPage(originalJsp, request);
    if (forward!=null && forward.equals(originalJsp)==false && sk.iway.iwcm.FileTools.isFile(forward))
    {
        pageContext.include(forward);
    }
    else
    {
    	%>
        <%@ include file="/admin/layout_top.jsp" %>
        <%
    	out.print("<h4>"+sk.iway.iwcm.i18n.Prop.getTxt("components.file_archiv.manager_category_not_found")+"</h4>");
    	%><%@ include file="/admin/layout_bottom.jsp" %><%
    }
%>
