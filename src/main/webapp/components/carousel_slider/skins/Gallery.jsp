<%@page import="sk.iway.iwcm.tags.WriteTag"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String style = pageParams.getValue("style", "01");

String skin = pageParams.getValue("skin","Classic");

//custom id
Integer multipleCarouselSliderIdId = (Integer)request.getAttribute("multipleCarouselSliderIdId");
if (multipleCarouselSliderIdId == null) multipleCarouselSliderIdId = Integer.valueOf(1);

%>
<iwcm:script type="text/javascript"> // GALLERY skin

jQuery(document).ready(function(){
    var scripts = document.getElementsByTagName("script");
    var jsFolder = "/components/carousel_slider/carouselengine/";
    for (var i= 0; i< scripts.length; i++)
    {
        if( scripts[i].src && scripts[i].src.match(/initcarousel-1\.js/i))
            jsFolder = scripts[i].src.substr(0, scripts[i].src.lastIndexOf("/") + 1);
    }
    if ( typeof html5Lightbox === "undefined" )
    {
        html5Lightbox = jQuery(".html5lightbox").html5lightbox({
            skinsfoldername:"",
            jsfolder:jsFolder,
            barheight:64,
            showtitle:true,
            showdescription:false,
            shownavigation:false,
            thumbwidth:80,
            thumbheight:60,
            thumbtopmargin:12,
            thumbbottommargin:8,
            titlebottomcss:'{color:#333; font-size:14px; font-family:Armata,sans-serif,Arial; overflow:hidden; text-align:left;}',
            descriptionbottomcss:'{color:#333; font-size:12px; font-family:Arial,Helvetica,sans-serif; overflow:hidden; text-align:left; margin:4px 0px 0px; padding: 0px;}'
        });
    }
    jQuery("#amazingcarousel-<%=multipleCarouselSliderIdId%>").amazingcarousel({
        jsfolder:jsFolder,
        width:<%=pageParams.getIntValue("carouselWidth",240)%>,
        height:<%=pageParams.getIntValue("carouselHeight",180)%>,
        skinsfoldername:"",
        itembottomshadowimagetop:99,
        random:<%=pageParams.getBooleanValue("random_play",false)%>,
        rownumber:<%=pageParams.getIntValue("rowNumber",1)%>,
        skin:"Gallery",
        responsive:true,
        itembottomshadowimage:"itembottomshadow-100-98-3.png",
        watermarklinkcss:"text-decoration:none;font:12px Arial,Tahoma,Helvetica,sans-serif;color:#333;",
        showhoveroverlayalways:false,
        lightboxdescriptionbottomcss:"{color:#333; font-size:12px; font-family:Arial,Helvetica,sans-serif; overflow:hidden; text-align:left; margin:4px 0px 0px; padding: 0px;}",
        supportiframe:false,
        lightboxthumbwidth:80,
        imagefillcolor:"FFFFFF",
        showwatermark:false,
        arrowwidth:48,
        transparent:false,
        continuousduration:2500,
        watermarkpositioncss:"display:block;position:absolute;bottom:8px;right:8px;",
        lightboxthumbheight:60,
        navspacing:8,
        playvideoimage:"playvideo-64-64-0.png",
        usescreenquery:true,
        screenquery:{
			tablet: {
				screenwidth: 900,
				visibleitems: 2
			},
			mobile: {
				screenwidth: 600,
				visibleitems: 1
			}
		},
        navwidth:16,
        navheight:16,
        watermarkimage:"",
        watermarkstyle:"text",
        imageheight:180,
        lightboxthumbbottommargin:8,
        lightboxtitlebottomcss:"{color:#333; font-size:14px; font-family:Armata,sans-serif,Arial; overflow:hidden; text-align:left;}",
        enabletouchswipe:<%=pageParams.getBooleanValue("touch_swipe",true)%>,
        navstyle:"<%=pageParams.getValue("nav_style","bullets")%>",
        arrowstyle:"<%=pageParams.getValue("arrow_style","always")%>",
        navswitchonmouseover:false,
        showitembackgroundimage:false,
        watermarklink:"http://amazingcarousel.com?source=watermark",
        arrowimage:"arrows-48-48-2.png",
        showbackgroundimage:false,
        spacing:4,
        scrollitems:1,
        navdirection:"<%=pageParams.getValue("direction","horizontal")%>",
        watermarktarget:"_blank",
        bottomshadowimagewidth:110,
        donotcrop:false,
        showhoveroverlay:true,
        height:<%=pageParams.getIntValue("carouselHeight",180)%>,
        itembackgroundimagewidth:100,
        backgroundimagetop:-40,
        width:<%=pageParams.getIntValue("carouselWidth",240)%>,
        hoveroverlayimage:"hoveroverlay-64-64-5.png",
        hidehoveroverlayontouch:false,
        transitioneasing:"easeOutExpo",
        lightboxshownavigation:false,
        itembackgroundimage:"",
        direction:"<%=pageParams.getValue("direction","horizontal")%>",
        watermarktext:"amazingcarousel.com",
        lightboxbarheight:64,
        continuous:false,
        pauseonmouseover:<%=pageParams.getBooleanValue("pause_on_mousover",true)%>,
        navimage:"bullet-16-16-0.png",
        lightboxthumbtopmargin:12,
        arrowhideonmouseleave:1000,
        backgroundimagewidth:110,
        loop:0,
        arrowheight:48,
        bottomshadowimage:"bottomshadow-110-95-0.png",
        lightboxshowdescription:false,
        bottomshadowimagetop:95,
        showitembottomshadow:<%=pageParams.getBooleanValue("show_shadow_bottom",false)%>,
        playvideoimagepos:"center",
        circular:<%=pageParams.getBooleanValue("autoplay",true)%>,
        showbottomshadow:false,
        showplayvideo:true,
        itembackgroundimagetop:0,
        lightboxshowtitle:true,
        scrollmode:"page",
        backgroundimage:"",
        lightboxnogroup:false,
        navmode:"page",
        interval:3000,
        watermarktextcss:"font:12px Arial,Tahoma,Helvetica,sans-serif;color:#666;padding:2px 4px;-webkit-border-radius:2px;-moz-border-radius:2px;border-radius:2px;background-color:#fff;opacity:0.9;filter:alpha(opacity=90);",
        itembottomshadowimagewidth:100,
        visibleitems:<%=pageParams.getIntValue("imgPerSlide",3)%>,
        imagewidth:240,
        transitionduration:1000,
        autoplay:<%=pageParams.getBooleanValue("autoplay",true)%>
    });
});

</iwcm:script>