<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.components.banner.BannerDB,sk.iway.iwcm.components.banner.model.*"
%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if (Tools.getRequestParameter(request, "bannerId") != null)
{
		int bannerId = Tools.getIntValue(Tools.getRequestParameter(request, "bannerId"), -1);
		if (bannerId == -1)
		{
			out.println("error: missing parameter bannerId");
			return;
		}

		BannerDB.statAddClick(bannerId, request);
		BannerBean bannerBean = BannerDB.getBanner(bannerId);
		if (bannerBean == null)
		{
			out.println("error: banner doesn't exists.");
			return;
		}
		System.out.println("bannerBean.getBannerRedirect(): "+bannerBean.getBannerRedirect());
		response.sendRedirect(bannerBean.getBannerRedirect());
}
%>
