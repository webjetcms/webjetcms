<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/json");%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.common.CloudToolsForCore" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
page import="sk.iway.iwcm.components.dictionary.DictionaryDB"%><%@
page import="java.util.ArrayList"%><%@
page import="sk.iway.iwcm.components.dictionary.model.DictionaryBean"%><%@
page import="java.util.List"%><%@
page import="org.json.JSONObject" %><%@
page import="org.json.JSONArray" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%
    List<DictionaryBean> dictList = new ArrayList<DictionaryBean>();

    dictList = DictionaryDB.getAll();
    String term = Tools.getStringValue(Tools.getRequestParameter(request, "term"),"");
    if (term != null) term = term.toLowerCase();
    JSONArray responseJson = new JSONArray();

    String domain = null;
    if(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
        domain = CloudToolsForCore.getDomainName();
    }

    for(DictionaryBean dict:dictList){
        if(!dict.getName().toLowerCase().contains(term)) continue;

        //filtruj domenu
        if (domain != null && domain.equals(dict.getDomain())==false) continue;

        JSONObject responseObject = new JSONObject();
        responseObject.put("id", dict.getId());
        responseObject.put("label", dict.getName());
        responseObject.put("value", dict.getName());
        responseJson.put(responseObject);
        }

    out.print(responseJson);

%>
