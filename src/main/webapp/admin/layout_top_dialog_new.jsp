<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
page import="sk.iway.iwcm.*,java.io.*,sk.iway.iwcm.i18n.Prop" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/><%	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");

	Prop prop2 = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);

	if (request.getAttribute("dialogTitleKey")!=null)
	   request.setAttribute("dialogTitle", prop2.getText((String)request.getAttribute("dialogTitleKey")));

	if (request.getAttribute("dialogDescKey")!=null)
	   request.setAttribute("dialogDesc", prop2.getText((String)request.getAttribute("dialogDescKey")));

	String cmpName = (String)request.getAttribute("cmpName");
	String title = (String)request.getAttribute("dialogTitle");
	String desc = (String)request.getAttribute("dialogDesc");
	String iconLink = "/components/"+cmpName+"/editoricon.gif";

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
%><%@page
import="sk.iway.iwcm.stat.BrowserDetector"%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
<html>
<head>
	<title><%=title%></title>

	<iwcm:present name="mobile">
		<meta name="viewport" content="width=100%, minimum-scale=1.0, maximum-scale=1.0" />
		<meta name="viewport" content="width=device-width, user-scalable=no">
	</iwcm:present>

	<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
	<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />

	<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" />
	<link type="text/css" rel="stylesheet" href="/components/cmp.css" />
	<!-- mho edit - výmena za webjet8/css/fck_dialog.css
	<link type="text/css" rel="stylesheet" href="/admin/FCKeditor/editor/skins/webjet/fck_dialog.css" />
	 -->
	<link rel="stylesheet" href="/admin/skins/webjet8/css/fck_dialog.css" />

	<style>
		<jsp:include page="/admin/css/perms-css.jsp"/>

		<iwcm:notPresent name="closeTable">
			body
			{
				cursor: wait;
				overflow: hidden;
			}
		</iwcm:notPresent>
	</style>

	<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>

	<script type="text/javascript">
		if (window.name && window.name=="componentIframe")
		{
			document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/components/iframe.css'>");
		}
		else
		{
			//document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/style.css'>");
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
	</script>
</head>

<iwcm:present name="closeTable">
	<body onload="onLoadHandler();" bgcolor="#FFFFFF" leftmargin="2" topmargin="2" marginwidth="2" marginheight="2">

		<table border="0" cellspacing="0" cellpadding="0" width="100%" height="65">
			<tr height="65">
				<td class="header" >
					<h1><%=title%></h1>
					<%=desc%>
				</td>

				<td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
					<div style="background-image: url(<%=iconLink%>);">&nbsp;</div>
				</td>
			</tr>
		</table>
</iwcm:present>

<iwcm:notPresent name="closeTable">
	<body onload="onLoadHandler();" bgcolor="#FFFFFF" leftmargin="2" topmargin="2" marginwidth="2" marginheight="2">

	<table border="0" cellspacing="0" cellpadding="0" width="100%" height="<%=height%>" style="height: 100%;">
		<tr id="headerTopRow" height="65" >
			<td class="header" >
				<h1><%=title%></h1>
				<%=desc%>
			</td>

			<td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
				<div style="background-image: url(<%=iconLink%>);">&nbsp;</div>
			</td>
		</tr>

		<tr height="*">
			<td class="main mainTab" valign="top" colspan="2">
	   		<div id="dialogCentralRow" class="calendarPopupStop" style="height: 100%; width: auto; overflow: auto; padding: 0px; margin:0px; position: relative;">
				<script type='text/javascript' src='/components/calendar/popcalendar.jsp'></script>
				<%request.setAttribute("sk.iway.iwcm.tags.CalendarTag.isJsIncluded", "true");%>
</iwcm:notPresent>
