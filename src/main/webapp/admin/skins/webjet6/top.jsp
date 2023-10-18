<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,java.io.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
page import="sk.iway.iwcm.system.Modules"%><%@
page import="sk.iway.iwcm.system.ModuleInfo"%>
<iwcm:checkLogon admin="true"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>
<%!
	List<ModuleInfo> getModulesWithoutOwnTab(Identity iwcm_useriwcm)
	{
		List<ModuleInfo> modules = Modules.getInstance().getUserMenuItems(iwcm_useriwcm);
		List<String> notModules = Arrays.asList(new String[]{
					"/admin/listgroups.do","/admin/listtemps.do","/admin/listusers.do",
					"/admin/fbrowser.browse.do","/admin/conf_editor.jsp"});
		List<ModuleInfo> storage = new ArrayList<ModuleInfo>();
		for (ModuleInfo module : modules)			
			if ( !notModules.contains(module.getLeftMenuLink()) )
				storage.add(module);
		return storage;
	}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title>:: Web JET admin ::</title>
	<%
	   response.setHeader("Pragma","No-Cache");
	   response.setDateHeader("Expires",0);
	   response.setHeader("Cache-Control","no-Cache");
	%>
	
	<!-- META -->
	
	<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" >
	
	<!-- STYLE -->

	<link type="text/css" rel="stylesheet" media="screen" href="<iwcm:cp/>/admin/skins/webjet6/css/webjet6.css" />
	<style type="text/css">
		body { margin: 0px; min-width: 930px;}
	</style>
	
	<% if ("D".equals(Constants.getString("wjVersion"))) { %>
	<style type="text/css">
		div.topContainer
		{
			background-image: url(<iwcm:cp/>/admin/skins/webjet6/images/bg-top-menu-intellimail.png);
			padding-left: 120px;
		}
		div.topHead .version
		{
			margin-left: -110px;
		}
	</style>
	<% } %>

	<!-- SCRIPT -->
	
	<script type="text/javascript" src="<iwcm:cp/>/admin/scripts/common.jsp"></script>
</head>
<body onLoad="Cas();" class="frameTop">

<div class="topContainer">
	<div class="topContainerRight">
		<div class="topHead clearfix">
			<span class="version"><iwcm:text key="admin.top.webjet_version"/> <%=InitServlet.getActualVersionLong()%></span>
			<span id="tdTime"></span>
		</div>
		<div class="topCenter">
			<div class="left">		
				<ul class="tab_menu_white">
					<%if ("D".equals(Constants.getString("wjVersion"))==false) {  %>
						<li class="first openFirst"><a id="firstLink" href='<iwcm:cp/>/admin/skins/webjet6/left_welcome.jsp' target="leftFrame"><iwcm:text key="welcome.left_menu"/></a></li>
						<iwcm:menu name="menuWebpages"><li><a href='<iwcm:cp/>/admin/listgroups.do' id="topTabWebpages" target="mainFrame"><iwcm:text key="menu.web_sites"/></a></li></iwcm:menu>
					<% } else {%>
						<iwcm:menu name="menuWebpages"><li class="first openFirst"><a id="firstLink" href='<iwcm:cp/>/admin/listgroups.do' id="topTabWebpages" target="mainFrame"><iwcm:text key="menu.web_sites"/></a></li></iwcm:menu>
					<% } %>
					<%
					int modulesCount = getModulesWithoutOwnTab(iwcm_useriwcm).size();
					if (modulesCount > 0)
					{
					%>
					<li>
						<a href="<iwcm:cp/>/admin/skins/webjet6/left_modules.jsp" id="topTabModules" target="leftFrame">					
						<%-- AK MAME IBA JEDEN MODUL, TAK ZALOZKA BUDE NAZVANA PODLA TOHOTO MODULU--%>
						<% if ( modulesCount != 1 ){ %>
							<iwcm:text key="components.modules.title"/>
						<% }else{ %>
							<iwcm:text key="<%=getModulesWithoutOwnTab(iwcm_useriwcm).get(0).getLeftMenuNameKey()%>"/>
						<% } %>
						</a>
					</li>
					<% } %>
					<iwcm:menu name="menuTemplates"><li><a href="<iwcm:cp/>/admin/skins/webjet6/left_templates.jsp" target="leftFrame" ><iwcm:text key="menu.templates"/></a></li></iwcm:menu>
					<iwcm:menu name="menuUsers"><li><a id="topTabUsers" href="<iwcm:cp/>/admin/skins/webjet6/left_users.jsp" target="leftFrame"><iwcm:text key="menu.users"/></a></li></iwcm:menu>
					<iwcm:menu name="menuFbrowser"><li><a id="topTabFiles" href="<iwcm:cp/>/admin/skins/webjet6/left_files.jsp" target="leftFrame"><iwcm:text key="menu.fbrowser"/></a></li></iwcm:menu>
					<iwcm:menu name="modUpdate | cmp_attributes | cmp_adminlog | edit_text | export_offline | cmp_clone_structure | menuConfig | cmp_data_deleting | cmp_server_monitoring | cmp_redirects | modRestart | cmp_crontab | make_zip_archive">
						<li><a href="<iwcm:cp/>/admin/skins/webjet6/left_conf.jsp" target="leftFrame"><iwcm:text key="menu.config"/></a></li>
					</iwcm:menu>
				</ul>
				<!-- 
				<div class="spotlight">
					<a href="javascript:spotlightShow()" class="spotlightTab" id="spotlightTab">&nbsp;</a>
				</div>
			 	-->
			</div>
			<div class="right">
				<a href="<%=Constants.getString("adminLogoffLink") %>" target="_top" class="logoff">&nbsp;<iframe src="<iwcm:cp/>/admin/refresher.jsp" name="refresher" width="1" height="1" marginwidth="0" marginheight="0" frameborder="0" scrolling="no"></iframe></a>
				
				<span>
					<bean:write name="iwcm_useriwcm" property="fullName"/><br/>
				<% if (Tools.isEmpty(Constants.getString("NTLMDomainController"))) { %><a href="javascript:openPopupDialogFromTopFrame('<iwcm:cp/>/admin/edituser.do');" target="mainFrame"><iwcm:text key="admin.top.change_user_details"/></a><% } %>
				</span>
				
				<form target="mainFrame" action="searchall.jsp" method="post" id="spotlightTopForm">
					<div class="boxSearch">
						<input type="text" size="4" value="" name="text" id="search" autocomplete="off" />
						<input type="submit" name="submitSearch" id="submitSearch" value="" />
					</div>
				</form>
					
				<a href="javascript:m_click_help()" class="tabHelp"><iwcm:text key="menu.top.help"/></a>	
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">

	$(document).ready(function(){
		$('#search').keyup(function(event){
			spotlightShow();
			parent.mainFrame.$('#spotlightInput').val($(this).val());
			//up or down
			if(event.keyCode == 40 || event.keyCode == 38){
				window.parent.mainFrame.$('#spotlightInput').focus();
			}else{
				window.parent.mainFrame.$('#spotlightInput').keydown();
				$(this).focus()
				$(this).val($(this).val())
			}
		});
	});

	<%
	File refresherF = new File(sk.iway.iwcm.Tools.getRealPath("/components/messages/refresher-ac.jsp"));
	if (refresherF.exists())
	{
	%>
		function popupMessage(id)
		{
			var options = "status=no,toolbar=no,scrollbars=no,resizable=yes,width=510,height=400,titlebar=no;"
			window.open("/components/messages/message_popup.jsp?equal=true&messageId="+id,"msgpop"+id,options);
		}
		function checkNewMessages()
		{
			$.get("/components/messages/refresher-ac.jsp?rnd="+(new Date()), null, function(data) {
				  $('#messagesRefresherDiv').html(data);
				});
		}
		
		var updater = setInterval(checkNewMessages, 1000*10);
	<% } %>

	function detectPropperTab()
	{
		try
		{
			var url = ""+window.parent.leftFrame.location;
			if (url.indexOf("users")!=-1) activMainMenu(document.getElementById("topTabUsers"));
		}
		catch (e) {}
	}
	
	$(document).ready(function()
	{
		$('.tab_menu_white li:last').addClass('last');
		setTimeout(detectPropperTab, 50);
		setTimeout(detectPropperTab, 500);
	});
</script>

<div id="messagesRefresherDiv" style="display: none;"></div>
<script type="text/javascript">
	<%
	   java.util.Date d_server = new java.util.Date();
	   int i_d_server = (int) (d_server.getTime() / 1000);
	   out.println("t_server = " + i_d_server);
	%>
	lokalny_start = new Date().getTime() // pocet ms od 1.1. 1970 (lokal)
	var timeServerOffset = (new Date().getTime()) - <%=Tools.getNow()%>;
	//window.status = "time offset: " + timeServerOffset;
</script>

<script type="text/vbscript" src="<iwcm:cp/>/admin/FCKeditor/editor/plugins/WJSpellCheck/word_script.jsp"></script>

</body>
</html>
