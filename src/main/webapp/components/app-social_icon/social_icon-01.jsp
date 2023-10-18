
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,org.apache.commons.codec.binary.Base64"%><%@ 
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@ 
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@ 
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@ 
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@ 
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@ 
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@ 
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);
	Prop prop = Prop.getInstance(lng);
	Base64 b64 = new Base64();
	//
	String blog_encoded = pageParams.getValue("blog_url", "");
	String blog_decoded = new String(b64.decode(blog_encoded.getBytes()));
	//
	String facebook_encoded = pageParams.getValue("facebook_url", "");
	String facebook_decoded = new String(b64.decode(facebook_encoded.getBytes()));
	//
	String flickr_encoded = pageParams.getValue("flickr_url", "");
	String flickr_decoded = new String(b64.decode(flickr_encoded.getBytes()));
	//
	String google_plus_encoded = pageParams.getValue("google_plus_url", "");
	String google_plus_decoded = new String(b64.decode(google_plus_encoded.getBytes()));
	//
	String instagram_encoded = pageParams.getValue("instagram_url", "");
	String instagram_decoded = new String(b64.decode(instagram_encoded.getBytes()));
	//
	String linkedin_encoded = pageParams.getValue("linkedin_url", "");
	String linkedin_decoded = new String(b64.decode(linkedin_encoded.getBytes()));
	//
	String mail_encoded = pageParams.getValue("mail_url", "");
	String mail_decoded = new String(b64.decode(mail_encoded.getBytes()));
	//
	String rss_encoded = pageParams.getValue("rss_url", "");
	String rss_decoded = new String(b64.decode(rss_encoded.getBytes()));
	//
	String twitter_encoded = pageParams.getValue("twitter_url", "");
	String twitter_decoded = new String(b64.decode(twitter_encoded.getBytes()));
	//
	String youtube_encoded = pageParams.getValue("youtube_url", "");
	String youtube_decoded = new String(b64.decode(youtube_encoded.getBytes()));
%>

<p <%if(!pageParams.getValue("socialIconAlign", "").equals("")){%>style="text-align: <%=pageParams.getValue("socialIconAlign", "left")%>;"<%}%>>
	<%
		if (!"".equals(blog_decoded))
		{
			String blog = prop.getText("components.app-social_icon.editor_components.blog");
	%>
	<a style="display: inline-block;" href="<%=blog_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/blog.png" title="<%=blog%>"
		alt="<%=blog%>"></a>
	<%
		}
		if (!"".equals(facebook_decoded))
		{
			String facebook = prop.getText("components.app-social_icon.editor_components.facebook");
	%>
	<a style="display: inline-block;" href="<%=facebook_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/facebook.png"
		title="<%=facebook%>" alt="<%=facebook%>"></a>
	<%
		}
		if (!"".equals(flickr_decoded))
		{
			String flickrText = prop.getText("components.app-social_icon.editor_components.flickr");
	%>
	<a style="display: inline-block;" href="<%=flickr_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/flickr.png" title="<%=flickrText%>"
		alt="<%=flickrText%>"></a>
	<%
		}
		if (!"".equals(google_plus_decoded))
		{
			String google_plusText = prop.getText("components.app-social_icon.editor_components.google_plus");
	%>
	<a style="display: inline-block;" href="<%=google_plus_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/google_plus.png"
		title="<%=google_plusText%>" alt="<%=google_plusText%>"></a>
	<%
		}
		if (!"".equals(instagram_decoded))
		{
			String instagramText = prop.getText("components.app-social_icon.editor_components.instagram");
	%>
	<a style="display: inline-block;" href="<%=instagram_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/instagram.png"
		title="<%=instagramText%>" alt="<%=instagramText%>"></a>
	<%
		}
		if (!"".equals(linkedin_decoded))
		{
			String linkedinText = prop.getText("components.app-social_icon.editor_components.linkedin");
	%>
	<a style="display: inline-block;" href="<%=linkedin_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/linkedin.png"
		title="<%=linkedinText%>" alt="<%=linkedinText%>"></a>
	<%
		}
		if (!"".equals(mail_decoded))
		{
			String mailText = prop.getText("components.app-social_icon.editor_components.mail");
	%>
	<a style="display: inline-block;" href="<%=mail_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/mail.png" title="<%=mailText%>"
		alt="<%=mailText%>"></a>
	<%
		}
		if (!"".equals(rss_decoded))
		{
			String rssText = prop.getText("components.app-social_icon.editor_components.rss");
	%>
	<a style="display: inline-block;" href="<%=rss_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/rss.png" title="<%=rssText%>"
		alt="<%=rssText%>"></a>
	<%
		}
		if (!"".equals(twitter_decoded))
		{
			String twitterText = prop.getText("components.app-social_icon.editor_components.twitter");
	%>
	<a style="display: inline-block;" href="<%=twitter_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/twitter.png"
		title="<%=twitterText%>" alt="<%=twitterText%>"></a>
	<%
		}
		if (!"".equals(youtube_decoded))
		{
			String youtubeText = prop.getText("components.app-social_icon.editor_components.youtube");
	%>
	<a style="display: inline-block;" href="<%=youtube_decoded%>"
		target="_blank"><img
		src="/components/app-social_icon/youtube.png"
		title="<%=youtubeText%>" alt="<%=youtubeText%>"></a>
	<%
		}
	%>
</p>