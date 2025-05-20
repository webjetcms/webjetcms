<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.PkeyGenerator"%>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.io.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Spustenie vsetkych PkeyGeneratorov, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>

<p>
Po skonceni overte log servera a opravte mena tabuliek a stlpcov. Pre PkeyGenerator ktory nema tabulku nastavte ako meno tabulky a stlpca null.
</p>

<%
if ("fix".equals(request.getParameter("act"))) {
    List<String> names = (new SimpleQuery()).forList("SELECT name FROM pkey_generator");
    for (String name : names) {
        out.println("NAME="+name);
        PkeyGenerator.getNextValue(name);
    }
}
%>


<%@ include file="/admin/layout_bottom.jsp" %>
