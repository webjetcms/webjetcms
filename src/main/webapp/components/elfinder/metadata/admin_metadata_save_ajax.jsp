<%@page import="org.json.JSONArray"%>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="org.json.JSONObject"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuFbrowser|menuWebpages"/><%
	JSONArray result = new JSONArray();

	Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
	if (user == null) {
		JSONObject fileResult = new JSONObject();
		fileResult.put("success", false);
		fileResult.put("error", "User not logged in");
		result.put(fileResult);
		out.print(result.toString());
		return;
	}

	Map<String, String[]> parameterMap = request.getParameterMap();
	String[] ids = Tools.getTokens(request.getParameter("ids"), ",");
	String[] fields = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
	
	for(String idString : ids) {
		JSONObject fileResult = new JSONObject();
		int id = Tools.getIntValue(idString, 0);
		fileResult.put("id", id);
		
		DocDB docDB = DocDB.getInstance();
		DocDetails fileDoc = docDB.getDoc(id);
		
		for (String field : fields) {
			String fieldValue = Tools.getStringValue(request.getParameter("field" + field + "[" + id + "]"), "");
			BeanUtils.setProperty(fileDoc, "field" + field, fieldValue);
		}
		
		// passwordProtected
		fileDoc.setPasswordProtected(parameterMap.containsKey("passwordProtected[" + id + "]") ? Tools.join(parameterMap.get("passwordProtected[" + id + "]"), ",") : "");
		
		if (!docDB.saveDoc(fileDoc)) {
			fileResult.put("success", false);
			fileResult.put("error", "Doc cannot be saved");
		}
		else {
			fileResult.put("success", true);
		}
		
		result.put(fileResult);
	}
	
	out.print(result.toString());
%>