<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
	request.setAttribute("cmpName", "send_link");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>
<!--
	function Ok()
	{
		for (var i = 0; i<document.textForm.sendType.length; i++)
		{
			if (document.textForm.sendType[i].checked)
			{
				sendType = document.textForm.sendType[i].value;
			}
		}
		oEditor.FCK.InsertHtml("!INCLUDE(/components/send_link/send_link.jsp, sendType=" + sendType + ")!");
		return true ;
	}

	if (!isFck)
		resizeDialog(400, 250);
//-->
</script>
<style>
<!--
	.types
	{
		float: left;
	  	padding-top: 2px;
	}
	label
	{
		display: block;
		padding: 2px;
		margin-left: 5px;
	}

//-->
	#tabMenu1 {
		margin-top: 20px;
	}
	.leftCol {
		text-align: right;
		margin-top: 10px;
	}
	.col-sm-10 {
		margin-top: 10px;
	}
</style>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">


		<form method="get" name="textForm">
			<div class="col-sm-10">
				<label for="pageId">
					<div class="col-sm-2 left leftCol">
						<input type="radio" name="sendType" value="page" id="pageId" <%if (ResponseUtils.filter(pageParams.getValue("sendType", "")).compareTo("page") == 0)
								out.print("checked='checked'");%>/>
					</div>
					<div class="col-sm-10 right">
						<span class="types"><iwcm:text key="components.send_link.send_page"/></span>
					</div>
				</label>
				<label for="linkId">
					<div class="col-sm-2 left leftCol">
						<input type="radio" name="sendType" value="link" id="linkId" <%if (ResponseUtils.filter(pageParams.getValue("sendType", "")).compareTo("page") != 0)
							out.print("checked='checked'");%>/>
					</div>
					<div class="col-sm-10 right">
						<span class="types"><iwcm:text key="components.send_link.send_link"/></span>
					</div>
				</label>
			</div>
		</form>

	</div>
</div>

<jsp:include page="/components/bottom.jsp" />
