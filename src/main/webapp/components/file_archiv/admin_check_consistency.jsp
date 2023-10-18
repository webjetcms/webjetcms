<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %>
<iwcm:checkLogon admin="true" perms="users.edit_admins"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
    if(request.getParameter("checkBtn") != null)
    {
    	out.println("START "+Tools.formatTime(Tools.getNow())); out.flush();
        String fileConsistant = FileArchivatorKit.checkFileConsistency();
        if(Tools.isNotEmpty(fileConsistant))
        {
        %>
            <p style="color: red;"><iwcm:text key="components.file_archiv.list.not_consistent"/></p>
            <div style="color: orange; font-weight: bold;"><p><%=fileConsistant %></p></div>
        <%
        }
        else
        {
        %>
            <div style="color: orange; font-weight: bold;"><p>Všetko OK</p></div>
        <%
        }
        out.println("END "+Tools.formatTime(Tools.getNow())); out.flush();
    }
    else
    {
        %>
        <p>Skontroluje konzisteciu suborov, ak niektory chyba, vrati naplneny string. Pozor ! Vypoctovo narocne, prechadza vsetky zaznamy v DB a fyzicky kontroluje ci subory existuju</p>
        <form action="<%=PathFilter.getOrigPath(request)%>" method="post">
            <input type="submit" name="checkBtn" value="Spustit" />
        </form>
        <%
    }
%>
<%@ include file="/admin/layout_bottom.jsp" %>