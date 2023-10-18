<%@page import="java.util.Map"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.form.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_form"/><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

int formId = Tools.getIntValue(Tools.getRequestParameter(request, "_editFormId"), -1);
if (formId < 0) return;

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null || user.isAdmin()==false) return;

//out.println(formId);

//ziskaj data
FormDetails formDetails = FormDB.getForm(formId);
if (formDetails == null) return;

Map<String,String> nameValueTable = formDetails.getNameValueTable();
%>
<%@page import="sk.iway.iwcm.tags.JSEscapeTag"%>
<script type="text/javascript">
var fillFormForm = null;
for (i=0; i<document.forms.length; i++)
{
	if (document.forms[i].action.indexOf("formmail.do")!=-1) fillFormForm = document.forms[i];
}

if (fillFormForm != null)
{
	//pridaj parameter s ID zaznamu v DB
	if (fillFormForm.action.indexOf("?")==-1) fillFormForm.action = fillFormForm.action+"?_editFormId=<%=formId%>";
	else fillFormForm.action = fillFormForm.action+"&_editFormId=<%=formId%>";
	//window.alert(fillFormForm.action);
}
function setFillFormValue(key, value)
{
	if (fillFormForm==null) return;

	var formElement = null;
	var formElementCounter = 0;
	for (i=0; i<fillFormForm.elements.length; i++)
	{
		if (fillFormForm.elements[i].name == key)
		{
			if (formElement==null) formElement = new Array();
			formElement[formElementCounter++] = fillFormForm.elements[i];
		}
	}
	if (formElement == null)
	{
		window.alert("Nezname pole: "+key);
		return;
	}
	//if (key=="2") window.alert(key+"="+ typeof(formElement)+" s="+formElement.length+" c="+formElementCounter);

	if (formElementCounter==1)
	{
		if (formElement[0].options!=undefined)
		{
			for (i=0; i<formElement[0].options.length; i++)
			{
				if (formElement[0].options[i].value==value)
				{
					formElement[0].options[i].selected = true;
				}
			}
		}
		else
		{
			formElement[0].value = value;
		}
	}
	else
	{
		for (i=0; i<formElement.length; i++)
		{
			if (formElement[i].value==value || (","+value+",").indexOf(","+formElement[i].value+",")!=-1)
			{
				formElement[i].checked = true;
				formElement[i].selected = true;
			}
		}
	}
}

<%

String key, value;
for (Map.Entry<String, String> entry : nameValueTable.entrySet())
{
	key = entry.getKey();
	value = entry.getValue();

	out.println("setFillFormValue(\""+key+"\", \""+JSEscapeTag.jsEscape(value)+"\");");

}
%>
</script>
