<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

//skus najst nahradu za tuto stranku
//KED SPRAVITE KOPIU TEJTO KOMPONENTY NASLEDOVNY KOD ZMAZTE INAK SA TO ZACYKLI
String pageURL = "/components/top-public.jsp";
String nahrada = sk.iway.iwcm.tags.WriteTag.getCustomPage(pageURL , request);
if (pageURL.equals(nahrada)==false)
{
	pageContext.include(nahrada);
	return;
}

%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %><%@page import="sk.iway.iwcm.stat.BrowserDetector"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

<%
response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

String cmpName = (String)request.getAttribute("cmpName");
String titleKey = (String)request.getAttribute("titleKey");
String descKey = (String)request.getAttribute("descKey");
String iconLink = "/components/"+cmpName+"/editoricon.gif";
if (request.getAttribute("iconLink")!=null)
{
	iconLink = (String)request.getAttribute("iconLink");
}
if (titleKey == null) titleKey = "components."+cmpName+".title"; 
if (descKey == null) descKey = "components."+cmpName+".desc";

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

%>


<html>
<head>

   <title><iwcm:text key="<%=titleKey%>"/></title>
   <meta http-equiv="Content-Type" content="text/html; charset=<%=SetCharacterEncodingFilter.getEncoding() %>">
	<link rel="stylesheet" href="/components/cmp.css" />

   <%=Tools.insertJQuery(request) %>

   <script type='text/javascript' src='/components/calendar/popcalendar.jsp'></script></html><%request.setAttribute("sk.iway.iwcm.tags.CalendarTag.isJsIncluded", "true");%>
   <script type="text/javascript">
		 //resize na inner velkost okna
         //resize na inner velkost okna
         function getInnerSize () {
             var x,y;
             if (self.innerHeight) // all except Explorer
             {
                 x = self.innerWidth;
                 y = self.innerHeight;
             }
             else if (document.documentElement && document.documentElement.clientHeight)
             // Explorer 6 Strict Mode
             {
                 x = document.documentElement.clientWidth;
                 y = document.documentElement.clientHeight;
             }
             else if (document.body) // other Explorers
             {
                 x = document.body.clientWidth;
                 y = document.body.clientHeight;
             }

             return [x,y];
         }

         function resizeToInner (w, h, x, y)
         {
             try
             {
                 var innerWidth = window.innerWidth;
                 var innerHeight = window.innerHeight;

                 var outerWidth = window.outerWidth;
                 var outerHeight = window.outerHeight;

                 var diffWidth = outerWidth - innerWidth;
                 var diffHeight = outerHeight - innerHeight;

                 var newWidth = w + diffWidth;
                 var newHeight = h + diffHeight;

                 //window.alert("required 2: "+w+"x"+h+" inner: "+innerWidth+"x"+innerHeight+" outer:"+outerWidth+"x"+outerHeight+" new:"+newWidth+"x"+newHeight);
                 //window.alert("avail 3="+screen.availWidth+":"+screen.availHeight);

                 if (screen.availWidth>200 && newWidth > screen.availWidth)
                 {
                     newWidth = screen.availWidth;
                     w = newWidth;
                 }
                 if (screen.availHeight>200 && newHeight > screen.availHeight)
                 {
                     newHeight = screen.availHeight;
                     h = newHeight;
                 }

                 newWidth = Math.floor(newWidth);
                 newHeight = Math.floor(newHeight);

                 if (innerWidth > 90 && outerWidth > 100 && innerHeight > 90 && outerHeight > 100 && newWidth < 1200 && newHeight < 1100 && newWidth >= w && newHeight >= h)
                 {
                     //window.alert("resizing, newWidth="+newWidth+" newHeight="+newHeight);
                     window.resizeTo(newWidth, newHeight);
                     return;
                 }


             } catch (e) { console.log(e); window.alert(e); }

             //window.alert("avail 1="+screen.availWidth+":"+screen.availHeight);
             // make sure we have a final x/y value
             // pick one or the other windows value, not both
             if (x==undefined) x = window.screenLeft || window.screenX;
             if (y==undefined) y = window.screenTop || window.screenY;
             // for now, move the window to the top left
             // then resize to the maximum viewable dimension possible
             window.moveTo(0,0);
             window.resizeTo(screen.availWidth,screen.availHeight);
             // now that we have set the browser to it's biggest possible size
             // get the inner dimensions.  the offset is the difference.

             //window.alert(navigator.userAgent);
             if (navigator.userAgent.indexOf("WebKit")!=-1)
             {
                 setTimeout(resizeToInnerAsync, 800, w, h, x, y);
                 //setTimeout(resizeToInnerAsync, 10000, w, h, x, y);
             }
             else resizeToInnerAsync(w, h, x, y);
         }

         function resizeToInnerAsync (w, h, x, y)
         {

             var inner = getInnerSize();

             //window.alert(inner[0]+"x"+inner[1]+" "+screen.availWidth+"x"+screen.availHeight);

             if (inner[0]==undefined)
             {
                 window.resizeTo(w, h);
             }
             else
             {
                 var ox = screen.availWidth-inner[0];
                 var oy = screen.availHeight-inner[1];
                 // now that we have an offset value, size the browser
                 // and position it
                 var newWidth = w+ox+2;
                 var newHeight = h+oy+2;
                 window.resizeTo(newWidth, newHeight);
                 window.moveTo(x,y);

                 //window.alert("ox="+ox+" oy="+oy+" i0="+inner[0]+" i1="+inner[1]+" nw="+newWidth+" nh="+newHeight+" w="+w+" h="+h+" sw="+screen.availWidth+" sh="+screen.availHeight);
             }
         }
		
		   function resizeDialog(width, height)
		   {
		   	resizeToInner(width, height);
		   	
		   	//nastav vysku centralneho riadku (len pre FF)
		   	var centralRow = document.getElementById("dialogCentralRow");
		   	if (centralRow != null && navigator.userAgent.indexOf("Gecko")!=-1)
		   	{
		   		centralRow.style.height = (height - 110) + "px";
		   	}
		   }

		   function onResizeHandler()
			{
				//resize window
		    	try
		    	{
		    		var dims = getInnerSize();
		 			var centerTd = document.getElementById("centerDivTd");
		 			if (centerTd != null) centerTd.style.height = (dims[1] - 110)+"px";
		    	}
		    	catch (e) {}
				
			}
		   function onLoadHandler()
			{
				this.focus;
				
				try
				{
					var addBottomButtonsEl = document.getElementById("addBottomButtons");
					var spanAddBottomButtonsEl = window.parent.document.getElementById("spanAddBottomButtons");
					if (addBottomButtonsEl != null && spanAddBottomButtonsEl != null)
					{
						spanAddBottomButtonsEl.innerHTML = addBottomButtonsEl.innerHTML;
					}
				}
				catch (e) {}
			}
   </script>
   <% if (BrowserDetector.isSmartphoneOrTablet(request)) {%>
      <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=0.5, maximum-scale=2.0" />      
   <% } else { %>   
	   <style>
	   body
	   {
	   	overflow: hidden;
	   }
	   </style>
   <% } %>
</head>

<body onResize="onResizeHandler();">
<table class="wjDialogHeaderTable no-table-responsive" border=0 cellspacing=0 cellpadding=0 width="100%" height="100%">
<tr id="headerTopRow">
   <td class="header">
		<h1><iwcm:text key="<%=titleKey%>"/></h1>
		<iwcm:text key="<%=descKey%>"/>
   </td>
   <td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
		<img src="<%=iconLink%>" alt="" align="absmiddle" />
   </td>
</tr>
<tr>
   <td class="main <% if (request.getAttribute("dialogHasTabs")==null){%>mainNoTabs<%} %>" valign="top" colspan="2">
   	<% if (BrowserDetector.isSmartphoneOrTablet(request)==false) {%><div style="height:400px;overflow:auto;" id="centerDivTd"><% } %>

