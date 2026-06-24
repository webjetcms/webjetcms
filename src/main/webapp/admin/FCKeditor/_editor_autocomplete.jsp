<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.doc.*, java.util.*, sk.iway.iwcm.i18n.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%@page import="sk.iway.iwcm.tags.AutoCompleteHelper"%><%@page import="org.json.JSONArray"%><%@page import="sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity"%><%@page import="sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto"%><%@page import="sk.iway.iwcm.components.customfields.rest.CustomFieldsService"%><%
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

	// CustomFieldsEntity has higher priority - check it first
	String className = Tools.getRequestParameter(request, "className");
	Long objectId = Tools.getLongValue(Tools.getRequestParameter(request, "objectId"), -1L);
	if (Tools.isNotEmpty(className) && Tools.isNotEmpty(requestField)) {
		char fieldAlphabet = Character.toUpperCase(requestField.charAt(0));

		// We need to pass the templateId as a bonus parameter for DocDetails
		CustomFieldsSearchDto searchDto = new CustomFieldsSearchDto(className, objectId);
		searchDto.setBonusParam(templateId);

		Map<Character, CustomFieldsEntity> customFields = CustomFieldsService.getCustomFieldsMap(searchDto);
		CustomFieldsEntity cfe = customFields.get(fieldAlphabet);
		if (cfe != null && Tools.isNotEmpty(cfe.getValue())) {
			value = cfe.getValue();
		}
	}

	// Fallback to properties lookup
	if (value == null) {
		Prop prop = Prop.getInstance(Constants.getString("defaultLanguage")); //Def language or nothing will be found
		value = prop.getText(field.toLowerCase());
		if (value.equals(field)) {
			value = null;
		}
	}

	//System.out.println("Autocomplete for field: " + field + ", search: " + search + ", value: " + value);

	if (value == null) {
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
