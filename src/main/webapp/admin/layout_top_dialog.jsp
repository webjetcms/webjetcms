<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.Prop,sk.iway.iwcm.stat.BrowserDetector" %>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<iwcm:checkLogon admin="true"/>

<%
if (Tools.isNotEmpty(Tools.getRequestParameter(request, "inIframe"))) request.setAttribute("inIframe", "1");
if ("true".equals(Tools.getRequestParameter(request, "hideHeaderFooter"))) request.setAttribute("hideHeaderFooter", "1");
%>

<%
if (Tools.isNotEmpty(Tools.getRequestParameter(request, "widgetData"))) request.setAttribute("widgetData", "1");

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

Prop prop2 = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
%>


<iwcm:empty name="inIframe">
	<%
		if (request.getAttribute("dialogTitleKey")!=null)
		   request.setAttribute("dialogTitle", prop2.getText((String)request.getAttribute("dialogTitleKey")));

		if (request.getAttribute("dialogDescKey")!=null)
		   request.setAttribute("dialogDesc", prop2.getText((String)request.getAttribute("dialogDescKey")));

		String cmpName = (String)request.getAttribute("cmpName");
		String title = (String)request.getAttribute("dialogTitle");
		String desc = (String)request.getAttribute("dialogDesc");
		String iconLink = "/components/"+cmpName+"/editoricon.png";

		if (request.getAttribute("cmpName") != null && request.getAttribute("dialogTitle") == null)
			title = prop2.getText("components."+cmpName+".dialog_title");

		if (request.getAttribute("cmpName") != null && request.getAttribute("dialogDesc") == null)
			desc = prop2.getText("components."+cmpName+".dialog_desc");

		if (request.getAttribute("iconLink")!=null)
			iconLink = (String)request.getAttribute("iconLink");

		if (Tools.isEmpty(title))
			title = "&nbsp;";
		if (Tools.isEmpty(desc))
			desc = "&nbsp;";

		//otestuj ci existuje...
		File fileLTD = new File(sk.iway.iwcm.Tools.getRealPath(iconLink));
		if (fileLTD.isFile()==false)
		{
			iconLink = "/components/"+cmpName+"/editoricon.png";
			fileLTD = new File(sk.iway.iwcm.Tools.getRealPath(iconLink));
			if (fileLTD.isFile()==false)
			{
				//kukni podla install name
				iconLink = "/components/"+Constants.getInstallName()+"/editoricon.png";
				fileLTD = new File(sk.iway.iwcm.Tools.getRealPath(iconLink));
				if (fileLTD.isFile()==false)
				{
					iconLink = "/components/"+Constants.getInstallName()+"/editoricon.gif";
					fileLTD = new File(sk.iway.iwcm.Tools.getRealPath(iconLink));
					if (fileLTD.isFile()==false)
					{
				   	iconLink = "/components/editoricon.png";
					}
				}
			}
		}

		String height = "100%";
		if (Tools.isNotEmpty(height))
		{
			//toto je kvoli kompileru
		}

		// koli posunutiu spodnej listy s buttonmi nahor pri editacii textov
		if( Tools.getRequestParameter(request, "rnd") != null ) request.setAttribute("closeTable", "true");

		//mobilna verzia
		String referer = request.getHeader("Referer");
		if (referer != null && referer.indexOf("/admin/m/")!=-1)
		{
			request.setAttribute("mobile", "true");
			request.setAttribute("closeTable", "true");
		}

		if (request.getAttribute("closeTable")!=null)
			height="";

		String submenu = request.getAttribute("submenu") != null ? (String)request.getAttribute("submenu") : "";
	%>

	<%@page import="java.io.*"%>

	<html class="wjDialogWindow">
	<head>
		<title><%=title%></title>

		<iwcm:present name="mobile">
			<meta name="viewport" content="width=100%, minimum-scale=1.0, maximum-scale=1.0" />
			<meta name="viewport" content="width=device-width, user-scalable=no">
		</iwcm:present>
		<%
		String uaCompatible = (String)request.getAttribute("X-UA-Compatible");
		if (Tools.isEmpty(uaCompatible)) uaCompatible = Constants.getString("xUaCompatibleAdminValue");
		%>
		<meta http-equiv="X-UA-Compatible" content="<%=uaCompatible %>" />
		<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />

		<iwcm:combine type="css" set="adminStandardCssWj9" />
		<link href="/admin/skins/webjet8/assets/global/css/webjet2021.css" rel="stylesheet" type="text/css"/>
		<%
			//nafejkujeme ze mame jquery aj ui, to sa vklada v adminJqueryJs
			Tools.insertJQueryUI(pageContext, "all");
			//fajkujeme ajax_support.js pre autocomplete tag
			//request.setAttribute("ajaxSupportInserted", true);
		%>
		<iwcm:combine type="js" set="adminJqueryJs" />

		<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" media="all"/>
		<link type="text/css" rel="stylesheet" href="/components/cmp.css" media="all"/>


		<style type="text/css" media="print">
			.noprint { display: none; }
			.main { background-color: white; }
			#buttonsBottomRow { display: none; }
			.button50, .button100 { display: none; }
		</style>
		<style type="text/css" media="all">
			.padding10 { padding-right: 0px; }
			#dialogCentralRow .padding10 { min-height: 250px; }

			<jsp:include page="/admin/css/perms-css.jsp"/>

			<% if (BrowserDetector.isSmartphoneOrTablet(request) == false) {%>
			<iwcm:notPresent name="closeTable">
				body
				{
					overflow: hidden;
				}
			</iwcm:notPresent>
			<% } %>


			.fa { font-size:15px; }

			.table.table-wj, table.sort_table { border: 0px none; }
			.table.table-wj tr td, table.sort_table tr td { font-size: 14px; border: 2px solid #ffffff; border-top: 0px none;  padding: 10px;  }

			.table.table-wj tr.heading > th,
			table.sort_table th { color: #000000; white-space: nowrap; border: 0px solid #ffffff !important; border-bottom: 6px solid #bad7fb !important; background:transparent !important; padding: 25px 10px !important;  }

			.table.table-wj tr.heading > th a, table.sort_table th a { color: #000000; }

			.table.table-wj tr.odd td, table.sort_table tr td { background: #ffffff url(/admin/skins/webjet8/assets/global/img/wj/bg-table-td-odd.png) left bottom repeat-x !important; }
			.table.table-wj tr.even td, table.sort_table tr:nth-child(2n) td { background: #f6f9fd !important; }

            div.padding10:before, div.padding10:after { display: table; content: " "; box-sizing: border-box; }
            div.padding10:after { clear: both; }
			<iwcm:present name="hideHeaderFooter">
				#headerTopRow, #buttonsBottomRow { display: none !important; }
				div.padding10 {background-color: white;}
			</iwcm:present>


			.padding10 {
				padding: 10px 10px 0px 10px;
				width: 100%;
			}

			input[type="text"], input[type="password"], select, textarea {
				max-width:530px;
				font-size: 16px;
				padding: 6px 12px;
			}

			.ti {
				font-size: 16px;
			}
			.btn .ti {
				line-height: 24px;
			}

			input, select, textarea, #iframePreview { border: 1px solid #bdbcbc;}
			label{ cursor: pointer; font-size:16px; line-height:16px; }
			.PopupBody { margin: 0px; padding: 0px; background-color: #ECEEF0;}
			hr{ border: 0px; height: 0px; font-size: 0px; line-height: 0px; display: none;}
			iframe#docLink{ border: 1px solid #bdbcbc; background-color: #ffffff;}

			.PopupButtons {
				height: 50px;
				width: 100%;
				padding: 0;
			}
			.PopupButtons { height:50px; width:100%; padding: 0; }
			.PopupButtons table { border-top:1px solid #d7d7d7; background-color: #fff; width: 100%; height:50px; position:absolute; bottom:0; }
			.PopupButtons table td{ padding: 10px;}

			#btnOk, #btnCancel, #btnSaveAs, body .button70,
			.button220x20,
			.button150x20,
			.button50, .button100, .button150, a.button50, a.button100, a.button150, a.button100:link, a.button100:visited,
			.button100, a.button100, a.button100:link, a.button100:visited,
			.button150, a.button150, #btnHelp, .button {

				border-width: 0px;
				padding: 7px 14px;
				font-size: 16px;
				outline: medium none !important;
				background-image: none !important;
				filter: none;
				box-shadow: none;
				text-shadow: none;
				color: #FFF;
				background-color: #0063fb;
				cursor:pointer;
				border-radius: 6px !important;
			}

			#btnOk:hover, #btnCancel:hover, #btnSaveAs:hover, body .button70:hover,
			.button220x20:hover,
			.button150x20:hover,
			.button50:hover, .button100:hover, .button150:hover, a.button50:hover, a.button100:hover, a.button150:hover,
			.button100:hover, a.button100:hover,
			.button150:hover, a.button150:hover,#btnHelp:hover, .button:hover {
			background-color:#004fc9;
			color:white;
			text-decoration:none;
			}

			#btnCancel { background-color: white; color:#13151b; border-color: #868ea5; border-width: 1px; }
			#btnCancel:hover { background-color: #13151b; border-color: #13151b; color: white; }

			#btnHelp { background-color: #fabd00; color:#13151b; }
			#btnHelp:hover { background-color: #fbc41a; color:#13151b; }

			.btn {
				font-size: 16px;
			}

			.form-control, .btn-outline-secondary {
				border-color: #868EA5 !important;
				color: #23262E;
			}

			#headerTopRow {
				border-bottom: 1px solid #DDDFE6;
			}
			.box_tab {
				background-color: #F3F3F6;
			}
			.box_tab ul.tab_menu li {
				margin-bottom: 0px;
				padding-left: 10px;
			}

			.panel .panel-body {
				font-size: 16px;
			}
			ul.tab_menu {
				margin-left: 0px;
			}
			#dialogCentralRow form tr td {
				padding-top: 16px !important;
				font-size: 14px;
			}
		</style>

		<% if (BrowserDetector.isSmartphoneOrTablet(request)) {%>
	      <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=0.3, maximum-scale=3.0" />
	   <% } %>

		<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>

		<script type="text/javascript">
			if (window.name && window.name=="componentIframe")
			{
				document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/components/iframe.css' media=\"all\">");
			}
			else
			{
				//document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/style.css' media=\"all\">");
			}

			var helpLink = "";

			function onLoadHandler()
			{
				this.focus;
			}

			function cancelWindow()
			{
				window.close();
			}
			$.ajaxSetup({
				headers: {
					'X-CSRF-Token': "<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(request.getSession(), true)%>"
				}
			});
		</script>
	</head>

	<iwcm:present name="closeTable">
		<body class="closeTableBody" onload="onLoadHandler();" bgcolor="#FFFFFF" leftmargin="2" topmargin="2" marginwidth="2" marginheight="2">


			<div class="popupSizeFinder"></div>
			<style type="text/css">
				body.closeTableBody {padding-bottom: 50px !important;}
				body.closeTableBody table.closeTable {position: fixed; bottom: 0;}
				.PopupButtons table { position: initial !important; }
			</style>

			<table class="wjDialogHeaderTable" border="0" cellspacing="0" cellpadding="0" width="100%" height="65">
				<tr id="headerTopRow" height="65">
					<td class="header" >
						<h1><%=title%></h1>
						<%=desc%>
						<%=submenu%>
					</td>

					<td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
						<div>&nbsp;</div>
					</td>
				</tr>
			</table>
	</iwcm:present>

	<iwcm:notPresent name="closeTable">
		<body onload="onLoadHandler();" bgcolor="#FFFFFF" leftmargin="2" topmargin="2" marginwidth="2" marginheight="2">

		<div class="popupSizeFinder"></div>

		<table class="wjDialogHeaderTable" border="0" cellspacing="0" cellpadding="0" width="100%" height="<%=height%>" style="height: 100%;">
			<tr id="headerTopRow" height="65" >
				<td class="header" >
					<h1><%=title%></h1>
					<%=desc%>
					<%=submenu%>
				</td>

				<td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
					<div>&nbsp;</div>
				</td>
			</tr>

			<tr height="*">
				<td class="main mainTab" valign="top" colspan="2">
		   		<% if (BrowserDetector.isSmartphoneOrTablet(request)==false) {%><div id="dialogCentralRow" class="calendarPopupStop" style="height: 100vh; width: auto; overflow: auto; padding: 0px; margin:0px; position: relative;"><% } %>

	</iwcm:notPresent>
</iwcm:empty>

<iwcm:notEmpty name="inIframe">

	<html class="wjDialogWindow">
	<head>
		<title></title>

		<iwcm:present name="mobile">
		<meta name="viewport" content="width=100%, minimum-scale=1.0, maximum-scale=1.0" />
		<meta name="viewport" content="width=device-width, user-scalable=no">
	</iwcm:present>
	<%
	String uaCompatible = (String)request.getAttribute("X-UA-Compatible");
	if (Tools.isEmpty(uaCompatible)) uaCompatible = Constants.getString("xUaCompatibleAdminValue");
	%>
	<meta http-equiv="X-UA-Compatible" content="<%=uaCompatible %>" />
	<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />

	<iwcm:combine type="css" set="adminStandardCss" />
	<iwcm:combine type="js" set="adminJqueryJs" />
	<script type="text/javascript">
		$.ajaxSetup({
			headers: {
				'X-CSRF-Token': "<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(request.getSession(), true)%>"
			}
		});
	</script>

	<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" media="all"/>
	<link type="text/css" rel="stylesheet" href="/components/cmp.css" media="all"/>

	<iwcm:notEmpty name="widgetData">
		<link rel="stylesheet" href="/css/page.css" />
	</iwcm:notEmpty>

	<style type="text/css" media="print">
		.noprint { display: none; }
		.main { background-color: white; }
		#buttonsBottomRow { display: none; }
		.button50, .button100 { display: none; }
	</style>
	<style type="text/css" media="all">
		body,
		.padding10 { background-color:transparent !important; background-image: none !important; }

		.gridster { width:100%; margin-top: 5px; }
		.gridster > ul { width: 296px !important; height: 296px; margin:0 auto; }
		.gridster > ul li.gs-w { width:100% !important; position:relative; }

		.fa { font-size:15px; }

		.table.table-wj, table.sort_table { border: 0px none; }
		.table.table-wj tr td, table.sort_table tr td { font-size: 14px; border: 2px solid #ffffff; border-top: 0px none;  padding: 10px;  }

		.table.table-wj tr.heading > th,
		table.sort_table th { color: #000000; white-space: nowrap; border: 0px solid #ffffff !important; border-bottom: 6px solid #bad7fb !important; background:transparent !important; padding: 25px 10px !important;  }

		.table.table-wj tr.heading > th a, table.sort_table th a { color: #000000; }

		.table.table-wj tr.odd td, table.sort_table tr td { background: #ffffff url(/admin/skins/webjet8/assets/global/img/wj/bg-table-td-odd.png) left bottom repeat-x !important; }
		.table.table-wj tr.even td, table.sort_table tr:nth-child(2n) td { background: #f6f9fd !important; }

	</style>
	<% if (BrowserDetector.isSmartphoneOrTablet(request)) {%>
      <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=0.3, maximum-scale=3.0" />
   <% } %>

	<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>

</head>

<body>

	<iwcm:notEmpty name="widgetData">
		<div class="gridster gridInModal">
		<ul>
		<li data-sizex="3" data-sizey="3" class="gs-w">
	</iwcm:notEmpty>

</iwcm:notEmpty>
