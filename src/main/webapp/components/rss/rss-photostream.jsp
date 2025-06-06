<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/xml");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*,java.text.*"%><%@page import="sk.iway.iwcm.io.IwcmFile"%><%@ page import="java.text.SimpleDateFormat" %><%@ page import="java.util.Locale" %><%@page import="sk.iway.iwcm.gallery.*"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%!

/**
* RSS export fotografii podla vzoru flickr
* Komponenta sa vklada ako bezna fotogaleria, nasledne staci upravit cestu k JSP na /components/rss/rss-photostream.jsp
* mala by byt vlozena v beznej stranke, ktora ma pouzitu RSS sablonu (bez contentu, vystup nastaveny ako text/xml)
*/

private static SimpleDateFormat sdf;

static
{
   sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
   sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
}

public synchronized static String formatDate(Date date)
{
	return sdf.format(date);
}

%><%

String title = "RSS Photo Stream";
String description = "";
String logo = "/images/logo.gif";
String url = "/";

DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
if (docDetails != null)
{
	title = docDetails.getTitle();
	description = docDetails.getPerex();
	if (Tools.isNotEmpty(docDetails.getPerexImage())) logo = docDetails.getPerexImage();
}

//v tomto parametre je adresar s galeriou, ak nie je zadany, pouzije sa default
   	PageParams pageParams = new PageParams(request);
	String lng = PageLng.getUserLng(request);
   	pageContext.setAttribute("lng", lng);

   	String perexGroup = pageParams.getValue("perexGroup", "");

   	if (perexGroup != null && perexGroup.length() > 0)
   		request.setAttribute("perexGroup", perexGroup);
   	else
   		request.removeAttribute("perexGroup");

   //String pageParams = (String)request.getAttribute("includePageParams");
	String dir = pageParams.getValue("dir", "/images/gallery");

   //pocet stlpcov galerie
   boolean alsoSubfolders = pageParams.getBooleanValue("alsoSubfolders", false);

   if (alsoSubfolders)
   		request.setAttribute("recursive", "");

   request.setAttribute("gpsize", String.valueOf(pageParams.getIntValue("maxLength", 50)));

	List<GalleryBean> photoList = GalleryDB.getImages(dir, alsoSubfolders, lng, (String)request.getAttribute("perexGroup"), pageParams.getValue("orderBy", "title"), pageParams.getValue("orderDirection", "asc"), request);

  request.setAttribute("photos", photoList);

  request.setAttribute("NO WJTOOLBAR", "1");
%><?xml version="1.0" encoding="<%=sk.iway.iwcm.SetCharacterEncodingFilter.getEncoding() %>" ?>
<rss version="2.0"
	    xmlns:media="http://search.yahoo.com/mrss/"
	    xmlns:dc="http://purl.org/dc/elements/1.1/"
	    xmlns:creativeCommons="http://cyber.law.harvard.edu/rss/creativeCommonsRssModule.html"
  	    xmlns:flickr="urn:flickr:" >

	<channel>
		<title><%=title %></title>
		<% out.print("<link>http://"+Tools.getServerName(request)+"/</link>"); //takto je to kvoli validatoru %>
		<description><%=description %></description>
		<language><%=PageLng.getUserLng(request) %></language>
		<pubDate><%
		try
	   {
			out.print(formatDate(new Date(Tools.getNow())));
	   } catch (Exception ex) {}
		%></pubDate>
		<generator>http://www.flickr.com/</generator>

		<image>
			<title><%=title %></title>
			<url>http://<%=Tools.getServerName(request)%><%=logo %></url>
			<%
			IwcmFile imageFile = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(url));
			if (imageFile.exists())
			{
				out.print("<link>http://"+Tools.getServerName(request)+url+"</link>");
			} %>
		</image>

		<iwcm:present name="photos">
			<iwcm:iterate id="photo" name="photos" type="sk.iway.iwcm.gallery.GalleryBean">
				<item>
					<title><iwcm:empty name="photo" property="shortDescription"><iwcm:beanWrite name="photo" property="imageName"/></iwcm:empty><iwcm:notEmpty name="photo" property="shortDescription"><![CDATA[<iwcm:beanWrite name="photo" property="shortDescription"/>]]></iwcm:notEmpty></title>
					<% out.print("<link>"+Tools.getBaseHref(request) + photo.getImagePath() + "/" + photo.getImageName() +"</link>"); %>
					<description>			&lt;p&gt;&lt;a href=&quot;<%=Tools.getBaseHref(request)%>&quot;&gt;<iwcm:notEmpty name="photo" property="author"><iwcm:beanWrite name="photo" property="author"/></iwcm:notEmpty>&lt;/a&gt; posted a photo:&lt;/p&gt;


&lt;p&gt;&lt;a href=&quot;<%=Tools.getBaseHref(request)%><iwcm:beanWrite name="photo" property="imagePath"/>/<iwcm:beanWrite name="photo" property="imageName"/>&quot; title=&quot;<iwcm:beanWrite name="photo" property="imageName"/>&quot;&gt;&lt;img src=&quot;<%=Tools.getBaseHref(request)%><iwcm:beanWrite name="photo" property="imagePath"/>/<iwcm:beanWrite name="photo" property="imageName"/>&quot; alt=&quot;<iwcm:beanWrite name="photo" property="imageName"/>&quot; /&gt;&lt;/a&gt;&lt;/p&gt;

					</description>
					<pubDate><%
					   try
					   {
					   	  out.print(formatDate(photo.getUploadDate()));
					   } catch (Exception ex) {}
					%></pubDate>
					<author><iwcm:notEmpty name="photo" property="author"><iwcm:beanWrite name="photo" property="author"/></iwcm:notEmpty></author>
					<guid><%=Tools.getBaseHref(request)%><iwcm:beanWrite name="photo" property="imagePath"/>/<iwcm:beanWrite name="photo" property="imageName"/></guid>

					<media:content url="<%=Tools.getBaseHref(request)%><iwcm:beanWrite name="photo" property="imagePath"/>/<iwcm:beanWrite name="photo" property="imageName"/>"
				                   type="image/jpeg"
				                   height="612"
				                   width="612"/>
				    <media:title><iwcm:empty name="photo" property="shortDescription"><iwcm:beanWrite name="photo" property="imageName"/></iwcm:empty><iwcm:notEmpty name="photo" property="shortDescription"><![CDATA[<iwcm:beanWrite name="photo" property="shortDescription"/>]]></iwcm:notEmpty></media:title>

				    <media:thumbnail url="<%=Tools.getBaseHref(request)%><iwcm:beanWrite name="photo" property="imagePath"/>/s_<iwcm:beanWrite name="photo" property="imageName"/>" height="75" width="75" />
				    <media:credit role="photographer"><iwcm:notEmpty name="photo" property="author"><iwcm:beanWrite name="photo" property="author"/></iwcm:notEmpty></media:credit>
				    <media:category scheme="urn:flickr:tags"></media:category>

				</item>
			</iwcm:iterate>
		</iwcm:present>
	</channel>
</rss>