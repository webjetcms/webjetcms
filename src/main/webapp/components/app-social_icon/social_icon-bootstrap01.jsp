<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,org.apache.commons.codec.binary.Base64"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
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

<style>
/*Social icons*/
.icon > .hover{opacity: 0; height: 0px; width: 100%; position: absolute; left: 0px; bottom: 0px;background-repeat: no-repeat;background-repeat: no-repeat;
    background-position: center;
}
.icon{
    height: 100%;
    min-height: 40px;
    background-repeat: no-repeat;
    background-position: center;
    border-right: 1px solid #e0e0e0;
    border-left: 1px solid #e0e0e0;
    cursor: pointer;
}
.icon.iconFacebook{
    background-color: transparent;
    background-image: url(/components/app-social_icon/temp51/facebook.png);
    border-right: 0px;
}

.icon.iconFacebook > .hover{
    background-color: #3a579d;
    background-image: url(/components/app-social_icon/temp51/facebook-hover.png);
}

.icon.iconTwitter{
    background-color: transparent;
    background-image: url(/components/app-social_icon/temp51/twitter.png);
     border-right: 0px;

}

.icon.iconTwitter > .hover{
    background-color: #00ccff;
    background-image: url(/components/app-social_icon/temp51/twitter-hover.png);
}

.icon.iconInstagram {
    background-color: transparent;
    background-image: url(/components/app-social_icon/temp51/instagram.png);
}

.icon.iconInstagram > .hover{
    background-color: #ff0600;
    background-image: url(/components/app-social_icon/temp51/instagram-hover.png);
}

#nextMenu > .inlineComponentEdit{ height:100%;}
</style>
<div class="row">

	<%
		int pocet = 0;
		if (!"".equals(instagram_decoded)) pocet++;
		if (!"".equals(twitter_decoded)) pocet++;
		if (!"".equals(facebook_decoded)) pocet++;


		if (!"".equals(facebook_decoded))
		{

			String facebook = prop.getText("components.app-social_icon.editor_components.facebook");
	%>
	<script>
	$(document).ready(function(){
    $('.iconFacebook').bind("mouseenter", function(){
        $('.iconFacebook > .hover').stop().animate({height:"100%",opacity:"1"},200);
    });
    $('.iconFacebook').bind("mouseleave", function(){
         $('.iconFacebook > .hover').stop().animate({height:"0%",opacity:"0"},200);
        });
	});
	</script>
<div class="col-sm-<%=(12/pocet)%> icon iconFacebook">
	<a class="hover" href="<%=facebook_decoded%>"></a>
</div>
	<%
		}

	%>

	<%

		if (!"".equals(instagram_decoded))
		{
			String instagramText = prop.getText("components.app-social_icon.editor_components.instagram");
	%>
	<script>
	$(document).ready(function(){
   $('.iconInstagram').bind("mouseenter", function(){
        $('.iconInstagram > .hover').stop().animate({height:"100%",opacity:"1"},200);
    });
    $('.iconInstagram').bind("mouseleave", function(){
         $('.iconInstagram > .hover').stop().animate({height:"0%",opacity:"0"},200);
        });
	});
	</script>
<div class="col-sm-<%=(12/pocet)%>  icon iconInstagram">
	<a class="hover" href="<%=instagram_decoded%>"></a>
</div>
	<%
		}

	%>


	<%

		if (!"".equals(twitter_decoded))
		{
			String twitterText = prop.getText("components.app-social_icon.editor_components.twitter");
	%>
	<script>
	$(document).ready(function(){
  $('.iconTwitter').bind("mouseenter", function(){
        $('.iconTwitter > .hover').stop().animate({height:"100%",opacity:"1"},200);
    });
    $('.iconTwitter').bind("mouseleave", function(){
         $('.iconTwitter > .hover').stop().animate({height:"0%",opacity:"0"},200);
        });
	});
	</script>
<div class="col-sm-<%=(12/pocet)%>  icon iconTwitter">
	<a class="hover" href="<%=twitter_decoded%>"></a>
</div>
	<%
		}

	%>

</div>