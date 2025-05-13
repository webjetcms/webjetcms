<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.system.ConfDetails"%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@
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
    String moduleName = Tools.getStringValue((String) request.getParameter("moduleName"), null);

    if(moduleName != null) {
        List<ConfDetails> confValuse = ConfDB.getConfForJsp(moduleName);
        if (confValuse.size()>0) request.setAttribute("confValues", confValuse);

    } else {
        %>
            <form>
                <label for="moduleName"><iwcm:text key="admin.show_constants.module_name"/>:</label>
                <input id="moduleName" name="moduleName" type="text" required>
                <button type="submit"><iwcm:text key="admin.show_constants.find_constants"/></button>
            </form>
        <%
    }
%>

<iwcm:present name="confValues">
    <h3><iwcm:text key="admin.help.possibleConfigValues"/></h3>
    <iwcm:iterate id="conf" name="confValues" type="sk.iway.iwcm.system.ConfDetails">
        <span> - ```<iwcm:beanWrite name="conf" property="name"/>``` - <iwcm:beanWrite name="conf" property="description"/> ( <iwcm:beanWrite name="conf" property="value"/>) </span><br>
    </iwcm:iterate>
</iwcm:present>