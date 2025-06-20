<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.doc.*, java.util.*, sk.iway.iwcm.i18n.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%@page import="sk.iway.iwcm.tags.AutoCompleteHelper"%><%@page import="org.json.JSONArray"%><%
	int templateId = Tools.getIntValue(Tools.getRequestParameter(request, "template"), 0);
	if (templateId > 0) {
		TemplateDetails temp = TemplatesDB.getInstance().getTemplate(templateId);
		if (temp != null && temp.getTemplatesGroupId()!=null && temp.getTemplatesGroupId().longValue() > 0) {
			TemplatesGroupBean tgb = TemplatesGroupDB.getInstance().getById(temp.getTemplatesGroupId());
			if (tgb != null && Tools.isNotEmpty(tgb.getKeyPrefix())) {
				RequestBean.addTextKeyPrefix(tgb.getKeyPrefix(), false);
			}
		}
		RequestBean.addTextKeyPrefix("temp-"+templateId, false);
	}

	String keyPrefix = Tools.getRequestParameter(request, "keyPrefix");
	if (Tools.isEmpty(keyPrefix)) keyPrefix = "editor";

	String requestField = Tools.getRequestParameter(request, "field");
	if(Tools.isEmpty(requestField)) {
		requestField = "";
	}

	String field = keyPrefix + ".field_" + requestField + ".type";
	final String search = DB.internationalToEnglish(Tools.getRequestParameter(request, "field" + requestField).toLowerCase());
	String value = null;
	String[] values = null;
	Map<String, String> valueTable = new Hashtable();
	List<String> valueList = null;
	Prop prop = Prop.getInstance(Constants.getString("defaultLanguage")); //Def language or nothing will be found
	value = prop.getText(field.toLowerCase());

	if (value.equals(field)) {
		out.println("[]");
		return;
	}

	if (value != null && value.length()>13) {
		value = value.substring(13);	//odstranim znacku autocomplete:
		values = Tools.getTokens(value, "|");	//rozparsujem
		for(int i = 0; i < values.length; i++)
		{
			if((DB.internationalToEnglish(values[i].toLowerCase()).contains(search) || "%".equals(search)) && !valueTable.containsKey(values[i]))
			{
				valueTable.put(values[i], values[i]);	//vytvorim hash tabulku, kvoli neopakovaniu a sortovaniu
			}
		}
		valueList = new ArrayList(valueTable.values());
		AutoCompleteHelper.sortByLeadingFirst(valueList, search);

		out.println(new JSONArray(valueList));
	}
%>
