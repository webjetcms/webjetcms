<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.tags.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
%>

function htmlSelectTagAddOption(select)
{
	if (select.value == "<%=SelectTag.NEW_OPTION_VALUE%>")
	{
		var myValue = window.prompt("<iwcm:text key="html_tags_support.enter_new_value"/>", "");
		myValue = ""+myValue;
	   if (myValue!="" && myValue!="null" && myValue!="undefined")
	   {
	   	var optionName = new Option(myValue, myValue, true, true)
	      select.options[select.length] = optionName;	      
	   }
	   else
	   {
	      select.selectedIndex = 0;
	   }
	}
}