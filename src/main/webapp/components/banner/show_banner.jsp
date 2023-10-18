<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.components.banner.BannerDB,sk.iway.iwcm.components.banner.model.*"%>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
BannerBean bBean = new BannerBean();

if (Tools.getRequestParameter(request, "bannerId") != null)
{
	int bannerId = Integer.parseInt((String)Tools.getRequestParameter(request, "bannerId"));
	bBean = BannerDB.getBanner(bannerId);
	if (bBean != null)
	{
		if (bBean.getBannerType().intValue() == 1)
		{
			request.setAttribute("picture","");
		}
		else if (bBean.getBannerType().intValue() == 2)
		{
			request.setAttribute("flash","");
		}
		else if (bBean.getBannerType().intValue() == 3)
		{
			request.setAttribute("html","");
		}
	}
}

%>
<html>
<head>
<title><iwcm:text key="components.banner.banner_preview"/></title>
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=windows-1250">
</head>

<script type="text/javascript">
function resizeWindow()
{
   width = document.all.image.width+80;
   height = document.all.image.height+120;
   if (width > (screen.availWidth - 20)) width = screen.availWidth - 20;
   if (height > (screen.availHeight - 20)) height = screen.availHeight - 20;
   if (width < 10) width = 500;
   if (height < 10) height = 150;
   window.resizeTo(width, height);
}
</script>

<body>

<logic:present name="picture">
	<p align="center">
	   <a href='javascript:window.close()'><img onLoad="resizeWindow()" name="image" id='image' src='<%=bBean.getBannerLocation()%>' border='0' alt='<iwcm:text key="components.banner.banner_preview"/>'></a>
	</p>
</logic:present>

<logic:present name="flash">
	<script type="text/javascript">
	   window.resizeTo(<%=bBean.getWidth()%>+100, <%=bBean.getHeight()%>+100);
	</script>
	<p align="center">
		<OBJECT codebase='http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,0,0'
				WIDTH='<%=bBean.getWidth()%>' HEIGHT='<%=bBean.getHeight()%>'>
			  <PARAM NAME=movie VALUE='<%=bBean.getBannerLocation()%>'>
			  <PARAM NAME=quality VALUE=high> <PARAM NAME=bgcolor VALUE=#FFFFFF>
			  <EMBED
			 		src='<%=bBean.getBannerLocation()%>'
			 		quality=high bgcolor=#FFFFFF
			 		WIDTH='<%=bBean.getWidth()%>'
			 		HEIGHT='<%=bBean.getHeight()%>'
			 		TYPE='application/x-shockwave-flash' PLUGINSPAGE='http://www.macromedia.com/go/getflashplayer' >
			</EMBED>
		</OBJECT>
	</p>
</logic:present>

<logic:present name="html">
	<%=bBean.getHtmlCode()%>
</logic:present>

</body>
</html>
