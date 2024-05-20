<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<iwcm:checkLogon admin="true" perms="menuEmail"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<%
	request.setAttribute("cmpName", "dmail");
%>

<jsp:include page="/components/top.jsp"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>
<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	String jspFileName = request.getParameter("jspFileName");
	if(Tools.isNotEmpty(jspFileName)){
		int slash = jspFileName.lastIndexOf("/");
		int dot = jspFileName.lastIndexOf(".");

		if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
	}
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type='text/javascript'>
<!--
	function Ok()
	{
		var form = document.menuSubscribeForm;
		oEditor.FCK.InsertHtml("!INCLUDE(/components/dmail/"+form.type.value+".jsp, senderEmail=\""+form.senderEmail.value+"\", senderName=\""+form.senderName.value+"\", emailBodyId=\""+form.emailBodyId.value+"\")!");
		return true;
	}

	if (isFck)
	{
	}
	else
		resizeDialog(570, 750);

	function loadSendIframe()
	{
		var url = "/components/dmail/admin_campaigns.jsp";
		 $("#componentIframeWindowTabSent").attr("src", url);
	}
	function loadUnsubscribedIframe()
	{
		var url = "/components/dmail/admin_unsubscribed.jsp";
		 $("#componentIframeWindowTabUnsubscribed").attr("src", url);
	}
	function loadLimitsIframe()
	{
		var url = "/components/dmail/admin-domainlimits-list.jsp";
		 $("#componentIframeWindowTabLimits").attr("src", url);
	}
	function setStandardDoc (docId) {
		$('#emailBodyIdId').val(docId);
	}
//-->
</script>

<iwcm:menu name="menuDmail">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li><a href="#" onclick="loadSendIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.dmail.camp.send_emails"/></a></li>
			<li><a href="#" onclick="loadUnsubscribedIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.admin_unsubscribed_email.unsubscribed_email"/></a></li>
			<li class="last"><a href="#" onclick="loadLimitsIframe();showHideTab('4');" id="tabLink4"><iwcm:text key="components.dmail.domainlimits.list"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<div class="tab-pane toggle_content tab-pane-fullheight" style="height:540px; o2verflow: auto; width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

	<form name="menuSubscribeForm" >
		<div class="col-sm-12 padding10">
			<div class="col-sm-12 form-group">
				<div class="col-sm-4 text-right"><label for="typeId"><iwcm:text key="calendar.type"/>:</label></div>
				<div class="col-sm-8">
					<select name="type" id="typeId">
						<option value="subscribe"><iwcm:text key="components.user.reg_form"/></option>
						<option value="subscribe-simple"><iwcm:text key="components.dmail.subscribeSimple"/></option>
						<option value="unsubscribe"><iwcm:text key="components.dmail.unsubscribeForm"/></option>
					</select>
				</div>
			</div>
			<div class="col-sm-12 form-group">
				<div class="col-sm-4 text-right"><label for="senderNameId"><iwcm:text key="components.dmail.camp.sender_name"/>:</label></div>
				<div class="col-sm-8"><input type="text" name="senderName" id="senderNameId" size="40" value="<%=ResponseUtils.filter(pageParams.getValue("senderName", iwcm_useriwcm.getFullName()))%>" /></div>
			</div>
			<div class="col-sm-12 form-group">
				<div class="col-sm-4 text-right"><label for="senderEmailId"><iwcm:text key="components.dmail.camp.sender_email"/>:</label></div>
				<div class="col-sm-8"><input type="text" name="senderEmail" id="senderEmailId" size="40" value="<%=ResponseUtils.filter(pageParams.getValue("senderEmail", iwcm_useriwcm.getEmail()))%>" /></div>
			</div>
			<div class="col-sm-12 form-group">
				<div class="col-sm-4 text-right"><label for="emailBodyIdId"><iwcm:text key="components.dmail.email_docid"/>:</label></div>
				<div class="col-sm-8">
					<div class="input-group">
						<input type="text" name="emailBodyId" id="emailBodyIdId" size="40" value="<%=pageParams.getIntValue("emailBodyId", -1)%>" class="form-control" style="max-width: 268px;" />
						<span class="input-group-btn">
							<span onclick="popup('/admin/user_adddoc.jsp', 450, 340);" class="input-group-addon btn green">
								<i class="ti ti-focus-2"></i>
							</span>
						</span>
					</div>
				</div>
			</div>
		</div>
	</form>

	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabSent" frameborder="0" name="componentIframeWindowTabSent" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu3">
		<iframe id="componentIframeWindowTabUnsubscribed" frameborder="0" name="componentIframeWindowTabUnsubscribed" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu4">
		<iframe id="componentIframeWindowTabLimits" frameborder="0" name="componentIframeWindowTabLimits" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>

<script type='text/javascript'>
<% if (Tools.isNotEmpty(jspFileName)) {%>
	document.menuSubscribeForm.type.value = "<%=jspFileName%>";
<%}%>
</script>
<jsp:include page="/components/bottom.jsp"/>