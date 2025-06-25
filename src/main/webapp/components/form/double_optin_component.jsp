<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.database.SimpleQuery" %>
<%@ page import="java.util.List" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.util.Date" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%!
    public enum Status {
        SUCCESS,
        ALREADY_CONFIRMED,
        FAIL
    }
%>
<%
//alternativa za sk.iway.iwcm.components.form.DoubleOptInComponent, ak na WebJETe sa nemoze pouzit WebjetComponent
String lng = PageLng.getUserLng(request);
Prop prop = Prop.getInstance(lng);
int formId = Tools.getIntValue(Tools.getRequestParameter(request, "formId"),-1);
String hash = Tools.getParameter(request, "hash");
Status status = Status.FAIL;
if (formId > 0 && Tools.isNotEmpty(hash)) {
    SimpleQuery simpleQuery = new SimpleQuery();
    List<String> list = simpleQuery.forList("SELECT * FROM forms WHERE id = ? AND double_optin_hash = ?", formId, hash);

    if (list.size() == 1) {
        list = simpleQuery.forList("SELECT * FROM forms WHERE id = ? AND double_optin_hash = ? AND double_optin_confirmation_date IS NOT NULL", formId, hash);
        if (list.size() > 0) {
            status = Status.ALREADY_CONFIRMED;
        }
        else {
            simpleQuery.execute("UPDATE forms SET double_optin_confirmation_date = ? WHERE id = ? AND double_optin_hash = ?", new Timestamp(new Date().getTime()), formId, hash);
            status = Status.SUCCESS;
            Adminlog.add(Adminlog.TYPE_FORMMAIL, Tools.getUserId(request), String.format("Double opt in success, formId: %d, hash: %s", formId, hash), formId, 0, new Timestamp(new Date().getTime()));
        }
    }
}
request.setAttribute("text", prop.getText("doubleoptin.confirm_text." + status.name().toLowerCase()));
request.setAttribute("title", prop.getText("doubleoptin.confirm_title." + status.name().toLowerCase()));
%>
<h1>${title}</h1>
<p>${text}</p>
