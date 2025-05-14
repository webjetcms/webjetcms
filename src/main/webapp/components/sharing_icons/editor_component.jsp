<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="java.io.*, sk.iway.iwcm.*, org.apache.commons.lang.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%@page import="sk.iway.iwcm.system.Modules"%>

<%
	request.setAttribute("cmpName", "sharing_icons");
	Modules modulesInfo = Modules.getInstance(); //kvoli zisteniu, ci je modul send_link pristupny (nie je pristupny iba v Basic a DirectMail verzii)
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

<link rel="stylesheet" type="text/css" href="/components/sharing_icons/sharing_icons.css" />

<script type='text/javascript'>
<!--
	$(document).ready(function() {
		//zabezpecuje, ze ked sa odklikne nejaky checkbox, tak sa odklikne aj hlavny selectAll checkbox
		//a zaroven, ze ked sa zakliknu vsetky checkboxy, tak sa zaklikne aj hlavny selectAll checkbox
		$("INPUT[@name=sites][type='checkbox']").click(function(){
			$("INPUT[@name=sites][type='checkbox']").each(function() {
				$("#selectAllId").attr("checked", true);
				if ($(this).attr("checked") == false)
				{
					$("#selectAllId").attr("checked", false);
					return false;
				}
			})
		});
	});

	/**
	 *	Funkcia, ktora oznaci vsetky checkboxy s danym menom name. Id sa udava identifikator selectAll checkboxu
	 */
	function checkAll(id, name)
	{
	   $("INPUT[@name=" + name + "][type='checkbox']").attr('checked', $('#' + id).is(':checked'));
	}

	function Ok()
	{
		var htmlCode = "!INCLUDE(/components/sharing_icons/sharing_icons.jsp";

		var checkedSites = $("input:checkbox[name=sites]:checked");
		if (checkedSites.length > 0)//pre checknute stranky vygeneruje sites=stranka+stranka+stranka
		{
			htmlCode += ", sites=";
			$.each(checkedSites, function() {
				  htmlCode += ($(this).val() + "+");
				});
			htmlCode = htmlCode.slice(0, -1);	//odreze posledne plus
		}
		else //ak nebola vybrana ani jedna stranka
		{
			alert('<iwcm:text key="components.sharing_icons.alert"/>');
			return false;
		}

		htmlCode += ")!";

	  	if (htmlCode != "")
	  		oEditor.FCK.InsertHtml(htmlCode);

	  	return true ;
	}

	if (!isFck)
		resizeDialog(370, 200);
//-->
</script>

<style>
<!--
	.site, .selectAll
	{
		float: left;
	  	width: 150px;
	  	padding-top: 2px;
	}
	label
	{
		display: block;
		padding: 2px;
		margin-left: 5px;
	}
	.choiceText
	{
		margin-bottom: 5px;
		display: block;
		font-weight: bold;
	}

	fieldset
	{
		border: 1px solid #C9C9C9;
			-moz-border-radius: 10px;
			-webkit-border-radius: 10px;
		margin: 10px 0 15px 0;
		width: 220px;
		padding: 5px;
	}

	.icon
	{
		margin: 0px;
		line-height: 18px;
	}

	.selectAll
	{
	  	width: 176px;
	  	font-style: italic;
	}
//-->
</style>

<div>
	<span class="choiceText"><iwcm:text key="components.sharing_icons.choice"/>:</span>

	<label title="<iwcm:text key="components.sharing_icon.facebook.icon" />" for="selectAllId">
		<span class="selectAll"><iwcm:text key="components.sharing_icon.select.all" /></span>
		<input type="checkbox" name="selectAll" id="selectAllId" onclick="checkAll('selectAllId', 'sites');" />
	</label>

	<fieldset>
		<legend><iwcm:text key="components.sharing_icons.portals"/></legend>

		<label title="<iwcm:text key="components.sharing_icon.facebook.icon" />" for="fbId">
			<span class="btnFacebook icon">&nbsp;</span>
			<span class="site">Facebook</span>
			<input type="checkbox" name="sites" value="facebook" id="fbId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("facebook") != -1)
				out.print("checked='checked'");%>/>
		</label>

		<label title="<iwcm:text key="components.sharing_icon.twitter.icon" />" for="twitterId">
			<span class="btnTwitter icon">&nbsp;</span>
			<span class="site">Twitter</span>
			<input type="checkbox" name="sites" value="twitter" id="twitterId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("twitter") != -1)
				out.print("checked='checked'");%>/>
		</label>

		<label title="<iwcm:text key="components.sharing_icon.google.icon" />" for="googleId">
			<span class="btnGoogle icon">&nbsp;</span>
			<span class="site">Google</span>
			<input type="checkbox" name="sites" value="google" id="googleId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("google") != -1)
				out.print("checked='checked'");%>/>
		</label>

		<label title="<iwcm:text key="components.sharing_icon.vybrali.icon" />" for="vybraliId">
			<span class="btnVybraliSme icon">&nbsp;</span>
			<span class="site">Vybrali.sme</span>
			<input type="checkbox" name="sites" value="vybrali" id="vybraliId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("vybrali") != -1)
				out.print("checked='checked'");%>/>
		</label>
	</fieldset>

	<fieldset style="margin-top: 10px;">
		<legend><iwcm:text key="stat.graph.browserPie.other"/></legend>

		<label title="<iwcm:text key="components.sharing_icon.print.icon" />" for="printId">
			<span class="btnPrint icon">&nbsp;</span>
			<span class="site"><iwcm:text key="components.basket.payment.print" /></span>
			<input type="checkbox" name="sites" value="print" id="printId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("print") != -1)
				out.print("checked='checked'");%>/>
		</label>

		<label title="<iwcm:text key="components.sharing_icon.bookmarks.icon" />" for="bookmarksId">
			<span class="btnBookmarks icon">&nbsp;</span>
			<span class="site"><iwcm:text key="components.sharing_icon.bookmarks.icon.short" /></span>
			<input type="checkbox" name="sites" value="bookmarks" id="bookmarksId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("bookmarks") != -1)
				out.print("checked='checked'");%>/>
		</label>

		<%
			if (modulesInfo.isAvailable("cmp_send_link"))
			{
		%>
				<label title="<iwcm:text key="components.sharing_icon.email.icon" />" for="emailId">
					<span class="btnEmail icon">&nbsp;</span>
					<span class="site"><iwcm:text key="components.sharing_icon.email.icon" /></span>
					<input type="checkbox" name="sites" value="email" id="emailId" <%if (ResponseUtils.filter(pageParams.getValue("sites", "")).indexOf("email") != -1)
				out.print("checked='checked'");%>/>
				</label>
		<%
			}
		%>
	</fieldset>
</div>
<script type="text/javascript">
<% if(StringUtils.countMatches(ResponseUtils.filter(pageParams.getValue("sites", "")), "+") == 6)
	{ %>
		$("#selectAllId").attr("checked", "checked");
<%	} %>

</script>

<jsp:include page="/components/bottom.jsp"/>
