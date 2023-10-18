<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB" %>
<%@ page import="sk.iway.iwcm.DB" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%
String type = Tools.getParameter(request, "type");
String value = Tools.getParameter(request, "product");
if(Tools.isEmpty(value))
    value = Tools.getParameter(request, "productCode");

JSONArray result = new JSONArray();
if(("product".equals(type) || "product_code".equals(type)) && Tools.isAnyEmpty(type, value) == false)
{
    value = DB.internationalToEnglish(value.trim()).toLowerCase();
    List<String> all = FileArchivatorDB.getDistinctListByProperty(type);
    if(all != null && all.size() > 0)
    {
        for(String one : all)
        {
            if(DB.internationalToEnglish(one).toLowerCase().startsWith(value))
            {
                JSONObject entry = new JSONObject();
                entry.put("value", one);
                entry.put("label", one);
                result.put(entry);
            }
        }
    }
}
out.print(result);
%>