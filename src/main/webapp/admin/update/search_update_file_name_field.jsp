<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,java.sql.*,sk.iway.iwcm.doc.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate"/>

<%
//novy sposob vyhladavania pouziva povodne prazdny stlpec file_name pre urcenie adresara
//v ktorom sa ma hladat (namiesto group_id IN (sialene dlhy zoznam ideciek))
//toto JSP stlpec naplni hodnotami
%>
<h1>Pregenerovanie file_name_field stlpca v databaze, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<br/>

<%
if ("fix".equals(request.getParameter("act"))) {
    DocDB.updateFileNameField(-1);
}
%>

done.
<br/>


