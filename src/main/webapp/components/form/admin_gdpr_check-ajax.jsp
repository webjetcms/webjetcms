<%@ page import="sk.iway.iwcm.form.GdprUserNotify" %><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %><%@
        page pageEncoding="utf-8" import="sk.iway.iwcm.form.*,sk.iway.iwcm.*,java.util.*" %><%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_form"/><%
    GdprUserNotify gdprUserNotify = new GdprUserNotify(request);
    if(Tools.getRequestParameter(request, "hasUserApproved") != null)
        out.print(gdprUserNotify.hasUserGdprApproved());

    if(Tools.getRequestParameter(request, "addUserApprove") != null)
        out.print(gdprUserNotify.addNotify());

%>
