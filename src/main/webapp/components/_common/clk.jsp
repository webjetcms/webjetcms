<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.components.banner.BannerDB,sk.iway.iwcm.components.banner.model.*"
%><%

//toto je povodne /components/banner/click.jsp kedze ale adblock to mozu detekovat na zaklade
//nazvu banner, je to presunute do sekcie bez nazvu banner v URL

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if (Tools.getRequestParameter(request, "bid") != null)
{
		int bannerId = Tools.getIntValue(Tools.getRequestParameter(request, "bid"), -1);
		if (bannerId == -1)
		{
			out.println("error: missing parameter bannerId");
			return;
		}
		if ("1".equals(Tools.getRequestParameter(request, "se"))==false)
		{
			//se ma v URL vyhladavac, pren neevidujem click
			BannerDB.statAddClick(bannerId, request);
		}
		BannerBean bannerBean = BannerDB.getBanner(bannerId);
		if (bannerBean == null)
		{
			out.println("error: banner doesn't exists.");
			return;
		}
		System.out.println("bannerBean.getBannerRedirect(): "+bannerBean.getBannerRedirect()+" ua="+request.getHeader("User-Agent")+" se="+Tools.getRequestParameter(request, "se")+" ip="+Tools.getRemoteIP(request)+" host="+Tools.getRemoteHost(request));
		response.sendRedirect(bannerBean.getBannerRedirect());
}
%>
