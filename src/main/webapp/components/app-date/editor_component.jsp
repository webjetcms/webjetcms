<%@page import="sk.iway.iwcm.PageParams"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<% request.setAttribute("cmpName", "app-date");
request.setAttribute("iconLink", "/components/app-date/editoricon.png");
%>
<jsp:include page="/components/top.jsp"/>

<%

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
//out.println("paramPageParams="+paramPageParams);
String jspFileName = ResponseUtils.filter(request.getParameter("jspFileName"));

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);


%>

<script type='text/javascript'>

function Ok()
{
	var htmlCode = "";
   if(document.textForm.field.value == "last_update")
   {
      datum="no";
      if(document.checkForm.datum.checked)
      {
       	 datum = "yes";
      }
      cas="no";
      if(document.checkForm.cas.checked)
      {
   		cas = "yes";
   	}

   	//window.alert("nastavil si last update.");
   	htmlCode = "!INCLUDE(/components/app-date/last_update.jsp, date="+datum+", time="+cas+")!";
   }
   else if(document.textForm.field.value == "meniny")
   {
	   var format = "long";
	   if (document.checkFormMeniny.datum.checked == false) format = "short";
	   htmlCode = "!INCLUDE(/components/app-date/meniny.jsp, format="+format+")!";
   }
   else
   {
   	htmlCode = document.textForm.field.value;
   }

  	if (htmlCode != "")
  	{
  		oEditor.FCK.InsertHtml(htmlCode);
  	}
	return true ;
} // End function



function showHelp(select)
{

	el = document.getElementById("options");
	if (el!=null)
	{
		if ("last_update" == document.textForm.field.value)
		{
			el.style.display = "block";
		}
		else
		{
			el.style.display = "none";
		}
	}
	el = document.getElementById("optionsMeniny");
	if (el!=null)
	{
		if ("meniny" == document.textForm.field.value)
		{
			el.style.display = "block";
		}
		else
		{
			el.style.display = "none";
		}
	}


}

</script>





<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 940px;">
		<form name="textForm" style="padding: 10px; margin: 0px;">
		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text key="components.cloud.apps.insertToYourSite"/>:</div>
			<div class="col-xs-8">
				<select name="field" onchange="showHelp(this);">
					<option value="meniny"><iwcm:text key="components.app-date.meniny"/></option>
					<option value="last_update"><iwcm:text key="components.date.last_update"/></option>
					<option value="!DATUM!"><iwcm:text key="components.date.datum"/> (<%=ShowDoc.updateCodes(null, "!DATUM!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
					<option value="!DATE!"><iwcm:text key="components.date.date"/> (<%=ShowDoc.updateCodes(null, "!DATE!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
					<option value="!DEN_DATUM!"><iwcm:text key="components.date.den_datum"/> (<%=ShowDoc.updateCodes(null, "!DEN_DATUM!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
					<option value="!DEN_DATUM_CZ!"><iwcm:text key="components.date.den_datum_cz"/> (<%=ShowDoc.updateCodes(null, "!DEN_DATUM_CZ!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
					<option value="!DAY_DATE!"><iwcm:text key="components.date.day_date"/> (<%=ShowDoc.updateCodes(null, "!DAY_DATE!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
					<option value="!TIME!"><iwcm:text key="components.date.time"/> (<%=ShowDoc.updateCodes(null, "!TIME!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
					<option value="!YEAR!"><iwcm:text key="components.date.year"/> (<%=ShowDoc.updateCodes(null, "!YEAR!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
				</select>
			</div>
		</div>
		</form>
		<div id="options" class="form-group clearfix" style="display:none">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
				<form name="checkForm">
						<input type="checkbox" name="datum" <% if (pageParams.getBooleanValue("datum", true)) out.print("checked"); %> value="yes">&nbsp;<iwcm:text key="components.date.display_date"/>&nbsp;&nbsp;
						<input type="checkbox" name="cas" <% if (pageParams.getBooleanValue("cas", true)) out.print("checked"); %> value="yes">&nbsp;<iwcm:text key="components.date.display_time"/>
				</form>
			</div>
		</div>
		<div id="optionsMeniny">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
				<form name="checkFormMeniny">
						<input type="checkbox" name="datum" <% if ("long".equals(pageParams.getValue("format", "long"))) out.print("checked"); %> value="yes">&nbsp;<iwcm:text key="components.date.display_date"/>&nbsp;&nbsp;
				</form>
			</div>
		</div>

		<%if (jspFileName!=null && jspFileName.indexOf("last_update.jsp")!=-1) { %>
		<script type="text/javascript">
			document.textForm.field.value="last_update";
			showHelp(document.textForm.field);
		</script>
		<% } %>
	</div>
</div>
<jsp:include page="/components/bottom.jsp"/>
