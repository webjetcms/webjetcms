<%@page import="sk.iway.iwcm.users.UsersDB"%><%@
		page import="org.apache.struts.util.ResponseUtils"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
		page import="org.json.JSONArray" %><%@
		page import="org.json.JSONObject" %><%@
		page import="net.sourceforge.stripes.util.ReflectUtil" %><%@
		page import="sk.iway.iwcm.io.IwcmFile" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.stream.Collectors" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><iwcm:checkLogon admin="true" perms="cmp_form"/><%
	String term = Tools.getRequestParameter(request, "term");

	String maybeClassPartname = "";
	if (term.indexOf(".")>-1)
	{
		maybeClassPartname = term.substring(term.lastIndexOf(".")+1);
		if (Tools.isNotEmpty(maybeClassPartname) && Character.isUpperCase(maybeClassPartname.charAt(0)))
		{
			term = Tools.replace(term, maybeClassPartname, "");
		}
		else
		{
			maybeClassPartname="";
		}
	}

	JSONArray array = new JSONArray();

	for (Package p : Arrays.stream(Package.getPackages()).sorted((p1,p2) -> p1.getName().compareTo(p2.getName())).collect(Collectors.toList()) )
	{
	    if (p.getName().startsWith(term))
		{
		    JSONObject obj = new JSONObject();
		    //obj.put("id", p.getName());
			obj.put("value", p.getName());
		    array.put(obj);
		}

		if ((term.endsWith(".") && p.getName().equals(term.substring(0,term.length()-1))))
		{
		    String path = Tools.getRealPath("/WEB-INF/classes/"+Tools.replace(term, ".", "/"));
			IwcmFile dir = new IwcmFile(path);
			for (IwcmFile file : dir.listFiles())
			{
			    if (Tools.isNotEmpty(maybeClassPartname) && !file.getName().startsWith(maybeClassPartname)) continue;
				JSONObject obj = new JSONObject();
				//obj.put("id", term + Tools.replace(file.getName(), ".class",""));
				obj.put("value", term + Tools.replace(Tools.escapeHtml(file.getName()), ".class",""));
				array.put(obj);
			}

			//ReflectUtil.
		}
	}
	out.print(array.toString());
%>
