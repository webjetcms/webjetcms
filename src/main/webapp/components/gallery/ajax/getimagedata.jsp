<%@page
import="org.json.JSONObject"%><%@page
import="sk.iway.iwcm.i18n.Prop"%><%@page
import="sk.iway.iwcm.PageLng"%><%@page
import="sk.iway.iwcm.Tools"%><%@page
import="sk.iway.iwcm.io.IwcmFile"%><%@page
import="sk.iway.iwcm.gallery.GalleryBean"%><%@page
import="sk.iway.iwcm.gallery.GalleryDB"%><%@ page
import="java.util.Map" %><%@ page
import="java.io.UnsupportedEncodingException" %><%@ page
import="java.util.LinkedHashMap" %><%@ page
import="java.net.URLDecoder" %><%@ page
import="sk.iway.iwcm.SetCharacterEncodingFilter" %>
<%@ page import="sk.iway.iwcm.common.GalleryDBTools" %>
<%@ page import="sk.iway.iwcm.system.multidomain.MultiDomainFilter" %>
<%@ page import="sk.iway.iwcm.FileTools" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%!

	public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException
	{
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), SetCharacterEncodingFilter.getEncoding()), URLDecoder.decode(pair.substring(idx + 1), SetCharacterEncodingFilter.getEncoding()));
		}
		return query_pairs;
	}

%><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

Prop prop = Prop.getInstance(lng);

// vraci v JSON formatu data s rozmery a popiskou pro Photoswipe fotogalerii
String imagePath = Tools.getStringValue(Tools.getRequestParameter(request, "path"), "");
if (FileTools.isFile(imagePath)==false) imagePath = MultiDomainFilter.rewriteUrlToLocal(imagePath, request);

System.out.println(imagePath);

int imageWidth = 800;
int imageHeight = 600;
String imageTitle = "";

GalleryBean image = null;

try
{

	if (Tools.isNotEmpty(imagePath) && imagePath.startsWith("/thumb") == false)
	{
		IwcmFile imageFile = new IwcmFile(Tools.getRealPath(imagePath));
		int[] dim = GalleryDBTools.getImageSize(imageFile);
		if (imageFile != null)
		{
			imageWidth = dim[0];
			imageHeight = dim[1];
		}

		image = GalleryDB.getGalleryBean(imageFile.getVirtualPath(), request, lng);
	}
	else if (Tools.isNotEmpty(imagePath) && imagePath.startsWith("/thumb"))
	{

		//je to thumb obrazok, skusme ziskat rozmer z w & h parametra
		int question = imagePath.indexOf("?");
		if (question > 0)
		{
			Map<String, String> params = splitQuery(imagePath.substring(question + 1));

			int width = Tools.getIntValue(params.get("w"), 0);
			int height = Tools.getIntValue(params.get("h"), 0);
			int ip = Tools.getIntValue(params.get("ip"), 0);

			String noThumbPath = imagePath.substring(6, question);
			IwcmFile imageFile = new IwcmFile(Tools.getRealPath(noThumbPath));

			image = GalleryDB.getGalleryBean(imageFile.getVirtualPath(), request, lng);

			if (ip > 0)
			{
				int[] imageSize = GalleryDBTools.getImageSize(imageFile);

				int cwidth = imageSize[0];
				int cheight = imageSize[1];

				if (ip==1)
				{
					//mame zadany len parameter w, h dopocitame podla pomeru stran povodneho vyrezu
					double pomer = (double)cwidth / (double) cheight;
					height = (int)Math.round(width / pomer);
				}
				else if (ip==2)
				{
					//mame zadany len parameter h, w dopocitame podla pomeru stran povodneho vyrezu
					double pomer = (double) cwidth / (double) cheight;
					width = (int) Math.round(height * pomer);
				}
			}

			imageWidth = width;
			imageHeight = height;
		}
	}

	if (image != null)
	{
		String title = "";

		if (!image.getShortDescription().equals(""))
		{
			title = "<span class=\"photoswipeShortDesc\">" + image.getShortDescription() + "</span>";
		}

		if (!image.getLongDescription().equals(""))
		{
			if (!title.equals(""))
			{
				title += "<span class=\"photoswipeDelimiter\"> - </span>";
			}
			title += " <span class=\"photoswipeLongDesc\">";
			title += image.getLongDescription();
			title += "</span>";
		}

		if (!image.getAuthor().equals(""))
		{
			if (!title.equals(""))
			{
				title += "<br />";
			}
			title += "<small>" + prop.getText("components.gallery.photoswipe.author") + image.getAuthor() + "</small>";
		}

		imageTitle = title;
	}
}
catch (Exception ex)
{
   sk.iway.iwcm.Logger.error(ex);
}


JSONObject dataset = new JSONObject();
dataset.put("imageWidth", imageWidth);
dataset.put("imageHeight", imageHeight);
dataset.put("imageTitle", imageTitle);
out.println(dataset.toString());
%>
