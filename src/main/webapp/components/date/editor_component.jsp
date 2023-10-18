<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<% request.setAttribute("cmpName", "date"); %>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>

function Ok()
{		
	var htmlCode = "";
   if(document.textForm.field.value == "last_update")
   {
	   aktualizovane="no";
      if(document.checkForm.aktualizovane.checked)
      {
    	  aktualizovane = "yes";
      }
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
   	htmlCode = "!INCLUDE(/components/date/last_update.jsp, aktualizovane="+aktualizovane+", date="+datum+", time="+cas+")!";
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

}
if (isFck)
{
	
}
else
{
	resizeDialog(470, 300);
}

</script>
<style type="text/css">
	.col-sm-4 {
		text-align: right;
		padding-top: 5px;
	}
	.col-sm-10 {
		margin-top: 15px;
	}
</style>
<%
Prop prop = Prop.getInstance();
%>
<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<table border="0" cellspacing="0" cellpadding="5">
		<form name=textForm>
			<div class="col-sm-10">
				<div class="col-sm-4">
					<iwcm:text key="components.date.field"/>
				</div>
				<div class="col-sm-8">
					<select name="field" onchange="showHelp(this);">
			            <option value="!DATUM!"><iwcm:text key="components.date.datum"/> (<%=ShowDoc.updateCodes(null, "!DATUM!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
			            <option value="!DATE!"><iwcm:text key="components.date.date"/> (<%=ShowDoc.updateCodes(null, "!DATE!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
			            <option value="!DEN_DATUM!"><iwcm:text key="components.date.den_datum"/> (<%=ShowDoc.updateCodes(null, "!DEN_DATUM!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
			            <option value="!DEN_DATUM_CZ!"><iwcm:text key="components.date.den_datum_cz"/> (<%=ShowDoc.updateCodes(null, "!DEN_DATUM_CZ!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
			            <option value="!DAY_DATE!"><iwcm:text key="components.date.day_date"/> (<%=ShowDoc.updateCodes(null, "!DAY_DATE!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
			            <option value="!TIME!"><iwcm:text key="components.date.time"/> (<%=ShowDoc.updateCodes(null, "!TIME!", -1, request, sk.iway.iwcm.Constants.getServletContext())%>)</option>
						<option value="last_update"><iwcm:text key="components.date.last_update"/></option>
		        	 </select>
				</div>
			</div>
		</form>
		</table>
		<div id="options" style="display:none">
		   <form name="checkForm">
		   	<input type="checkbox" name="aktualizovane" checked value="yes">&nbsp;<iwcm:text key="components.date.display_updated" param1='<%=prop.getText("components.date.aktualizovane")%>'/>&nbsp;&nbsp;
		  		<input type="checkbox" name="datum" checked value="yes">&nbsp;<iwcm:text key="components.date.display_date"/>&nbsp;&nbsp;
		  		<input type="checkbox" name="cas" checked value="yes">&nbsp;<iwcm:text key="components.date.display_time"/>
		   </form>
		</div>
	</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
