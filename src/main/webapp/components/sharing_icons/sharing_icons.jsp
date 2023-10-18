<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.system.Modules"%>

<%
	PageParams pageParams = new PageParams(request);
	String sitesParams = pageParams.getValue("sites", "");
	if (Tools.isEmpty(sitesParams))
	{
		Prop prop = Prop.getInstance(request);
		out.println(prop.getText("components.sharing_icons.alert"));
		return;
	}
	
	String actualLink = Tools.getBaseHref(request) + PathFilter.getOrigPath(request);
	String queryString = (String)request.getAttribute("path_filter_query_string");
	if (Tools.isNotEmpty(queryString))
		actualLink += ("?" + queryString);
	
	actualLink = actualLink.replace('\'', ' ');
	actualLink = actualLink.replace('"', ' ');
	
	Modules modulesInfo = Modules.getInstance(); //kvoli zisteniu, ci je modul send_link pristupny (nie je pristupny iba v Basic a DirectMail verzii)
%>

<script type="text/javascript">
<!--
	document.write('<link type="text/css" rel="stylesheet" href="/components/sharing_icons/sharing_icons.css" />');
	
	<% if (sitesParams.indexOf("facebook") != -1) { %>
	function fbsClick() 
	{
		var url = location.href,
		title = document.title;
		window.open('http://www.facebook.com/sharer.php?u=' + encodeURIComponent(url) + '&amp;t=' + encodeURIComponent(title), 'sharer', 'toolbar=0, status=0, width=626, height=436');
		return false;
	}
	<% } %>
	<% if (sitesParams.indexOf("bookmarks") != -1) { %>
	function addToBookmarks ()
	{
		var ua = navigator.userAgent.toLowerCase(), 
		isMac = (ua.indexOf('mac')!=-1),
		isWebkit = (ua.indexOf('webkit')!=-1), 
		str = (isMac ? 'Command/Cmd' : 'CTRL'),
		title = document.title,
		url = '<%=actualLink%>';
		
		if (window.sidebar) // Mozilla Firefox Bookmark
			window.sidebar.addPanel(title, url, "");
		else if(window.external && !isWebkit) // IE Favorite
			window.external.AddFavorite(url, title);
		else
		{
			if(window.opera && (!opera.version || (opera.version() < 9))) // Opera versions pred 9
				str += ' + T';
			else if(ua.indexOf('konqueror')!=-1) // Konqueror
				str += ' + B';
			else if(window.opera || window.home || isWebkit || isMSIE || isMac) // IE, Firefox, Netscape, Safari, Google Chrome, Opera 9+, iCab, IE5/Mac
				str += ' + D';
			
			alert ('<iwcm:text key="components.sharing_icon.bookmarks.alert" param1="' + str + '" />');
		}
	}
	<% } %>
//-->
</script>

<%
	//poslat emailom
	if (sitesParams.indexOf("email") != -1 && modulesInfo.isAvailable("cmp_send_link"))
	{
%>
		<jsp:include page="/components/dialog.jsp" /> 
<%
	}
%>
<div id="sharingIcons">
	<ul id="sharingIconsList">
		<%
			//poslat facebook
			if (sitesParams.indexOf("facebook") != -1)
			{
		%>
				<li>
					<a href="http://www.facebook.com/share.php?u=<%=actualLink%>" onclick="return fbsClick(); window.open(this.href,'_blank'); return false;" class="btnFacebook" title="<iwcm:text key="components.sharing_icon.facebook.icon" />">
						&nbsp;
					</a>
				</li>
		<%
			}
		
			//poslat twitter
			if (sitesParams.indexOf("twitter") != -1)
			{
		%>
				<li>
					<a href="http://twitter.com/home?status=<%=actualLink %>" onclick="window.open(this.href,'_blank'); return false;" class="btnTwitter" title="<iwcm:text key="components.sharing_icon.twitter.icon" />">
						&nbsp;
					</a>
				</li>
		<%
			}
			
			//poslat google bookmarks
			if (sitesParams.indexOf("google") != -1)
			{
		%>
				<li>
					<a href="http://www.google.com/bookmarks/mark?op=add&amp;title=<iwcm:write name='doc_title'/>&amp;bkmk=<%=actualLink %>" onclick="window.open(this.href,'_blank'); return false;" title="<iwcm:text key="components.sharing_icon.google.icon" />" class="btnGoogle">
						&nbsp;
					</a>
				</li>
		<%
			}
			
			//poslat vybrali.sme
			if (sitesParams.indexOf("vybrali") != -1)
			{
		%>
				<li>
					<a href="http://vybrali.sme.sk/submit.php?url=<%=actualLink %>" title="<iwcm:text key="components.sharing_icon.vybrali.icon" />" target="_blank" class="btnVybraliSme">
						&nbsp;
					</a>
				</li>
		<%
			}
			
			//vytlacit stranku
			if (sitesParams.indexOf("print") != -1)
			{
		%>
				<li>
					<a href="#" class="btnPrint" onclick="window.print(); return false;" title="<iwcm:text key="components.sharing_icon.print.icon" />">
						&nbsp;
					</a>
				</li>
		<%
			}
			
			//pridat stranku do oblubenych poloziek/zaloziek - bookmarks
			if (sitesParams.indexOf("bookmarks") != -1)
			{
		%>
				<li>
					<a href="#" class="btnBookmarks" onclick="addToBookmarks(); return false;" title="<iwcm:text key="components.sharing_icon.bookmarks.icon" />">
						&nbsp;
					</a>
				</li>
		<%
			}
			
			//poslat emailom
			if (sitesParams.indexOf("email") != -1 && modulesInfo.isAvailable("cmp_send_link"))
			{
		%>
				<li>
					<a onclick="openWJDialog('sendLink', '/components/send_link/send_link_form.jsp?docid=<%=Tools.getDocId(request)%>&sendType=link');" class="btnEmail" title="<iwcm:text key="components.sharing_icon.email.icon" />">
						&nbsp;
					</a>
				</li>
		<%
			}
		%>
	</ul>
</div>