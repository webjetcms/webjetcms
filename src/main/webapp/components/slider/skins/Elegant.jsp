<%@page import="sk.iway.iwcm.tags.WriteTag"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String style = pageParams.getValue("style", "01");

String skin = pageParams.getValue("skin","Classic");
String support_images_path = "/components/slider/support_images/";

//custom id
Integer multipleSliderId = (Integer)request.getAttribute("multipleSliderId");
if (multipleSliderId == null) multipleSliderId = Integer.valueOf(1);

%>
<iwcm:script type="text/javascript"> // ELEGANT skin

jQuery(document).ready(function(){

    var scripts = document.getElementsByTagName("script");

    var jsFolder = "";

    for (var i= 0; i< scripts.length; i++)

    {

        if( scripts[i].src && scripts[i].src.match(/initslider-1\.js/i))

            jsFolder = scripts[i].src.substr(0, scripts[i].src.lastIndexOf("/") + 1);

    }

    jQuery("#amazingslider-<%=multipleSliderId%>").amazingslider({

        sliderid:<%=multipleSliderId%>,

        jsfolder:jsFolder,

        width:<%=pageParams.getIntValue("sliderWidth",900)%>,

        height:<%=pageParams.getIntValue("sliderHeight",360)%>,

        skinsfoldername:"",

        loadimageondemand:false,

        videohidecontrols:false,

        donotresize:false,

        enabletouchswipe: <%=pageParams.getBooleanValue("touch_swipe",true)%>,

        fullscreen:false,

        autoplayvideo:false,

        addmargin:true,

        transitiononfirstslide: <%=pageParams.getBooleanValue("transition_on_first_slide",true)%>,

        forceflash:false,

        isresponsive:true,

        forceflashonie11:true,

        forceflashonie10:true,

        pauseonmouseover:<%=pageParams.getBooleanValue("pause_on_mousover",false)%>,

        playvideoonclickthumb:true,

        slideinterval: <%=pageParams.getIntValue("autoplay_interval",5000)%>,

        fullwidth:<%=pageParams.getBooleanValue("fullWidthSlider",false)%>,

        randomplay: <%=pageParams.getBooleanValue("random_play",false)%>,

        scalemode:"fill",

        loop: <%=pageParams.getIntValue("loop_number",0)%>,

        autoplay: <%=pageParams.getBooleanValue("autoplay",true)%>,

        navplayvideoimage:"<%=support_images_path%>"+"play-32-32-0.png",

        navpreviewheight:60,

        timerheight:2,

        descriptioncssresponsive:"font-size:12px;",

        shownumbering: <%=pageParams.getBooleanValue("show_numbering",false)%>,

        skin: "<%=pageParams.getValue("skin","Elegant")%>",

        addgooglefonts:true,

        navshowplaypause:true,

        navshowplayvideo:false,

        navshowplaypausestandalonemarginx:8,

        navshowplaypausestandalonemarginy:8,

        navbuttonradius:0,

        navthumbnavigationarrowimageheight:32,

        navmarginy:16,

        lightboxshownavigation:false,

        showshadow:false,

        navfeaturedarrowimagewidth:16,

        navpreviewwidth:120,

        googlefonts:"Inder",

        navborderhighlightcolor:"",

        navcolor:"",

        lightboxdescriptionbottomcss:"{color:#333; font-size:12px; font-family:Arial,Helvetica,sans-serif; overflow:hidden; text-align:left; margin:4px 0px 0px; padding: 0px;}",

        lightboxthumbwidth:80,

        navthumbnavigationarrowimagewidth:32,

        navthumbtitlehovercss:"text-decoration:underline;",

        navthumbmediumheight:64,

        texteffectresponsivesize:600,

        arrowwidth:32,

        texteffecteasing:"easeOutCubic",

        texteffect:"slide",

        lightboxthumbheight:60,

        navspacing:4,

        navarrowimage:"<%=support_images_path%>"+"navarrows-28-28-0.png",

        bordercolor:"#ffffff",

        ribbonimage:"<%=support_images_path%>"+"ribbon_topleft-0.png",

        navwidth:28,

        navheight:28,

        arrowimage:"<%=support_images_path%>"+"arrows-32-32-0.png",

        timeropacity:0.6,

        arrowhideonmouseleave:1000,

        navthumbnavigationarrowimage:"<%=support_images_path%>"+"carouselarrows-32-32-0.png",

        navshowplaypausestandalone:false,

        texteffect1:"slide",

        navpreviewbordercolor:"#ffffff",

        texteffect2:"slide",

        customcss:"",

        ribbonposition:"topleft",

        navthumbdescriptioncss:"display:block;position:relative;padding:2px 4px;text-align:left;font:normal 12px Arial,Helvetica,sans-serif;color:#333;",

        lightboxtitlebottomcss:"{color:#333; font-size:14px; font-family:Armata,sans-serif,Arial; overflow:hidden; text-align:left;}",

        arrowstyle: "<%=pageParams.getValue("arrow_style","none")%>",

        navthumbmediumsize:800,

        navthumbtitleheight:20,

        textpositionmargintop:24,

        buttoncssresponsive:"",

        navswitchonmouseover:false,

        playvideoimage:"<%=support_images_path%>"+"playvideo-64-64-0.png",

        arrowtop:50,

        textstyle:"dynamic",

        playvideoimageheight:64,

        navfonthighlightcolor:"#666666",

        showbackgroundimage:false,

        navpreviewborder:4,

        navshowplaypausestandaloneheight:28,

        navdirection:"horizontal",

        navbuttonshowbgimage:true,

        navbuttonbgimage:"<%=support_images_path%>"+"navbuttonbgimage-28-28-0.png",

        textbgcss:"display:none;",

        textpositiondynamic:"bottomleft",

        playvideoimagewidth:64,

        buttoncss:"display:block; position:relative; margin-top:10px;",

        navborder:4,

        navshowpreviewontouch:false,

        bottomshadowimagewidth:110,

        showtimer: <%=pageParams.getBooleanValue("show_countdown",true)%>,

        navradius:0,

        navmultirows:false,

        navshowpreview: <%= pageParams.getBooleanValue("show_thumbnails",true)%>,

        navpreviewarrowheight:8,

        navmarginx:16,

        navfeaturedarrowimage:"<%=support_images_path%>"+"featuredarrow-16-8-0.png",

        navthumbsmallsize:480,

        showribbon:false,

        navstyle: "<%=pageParams.getValue("nav_style","numbering")%>",

        textpositionmarginleft:24,

        descriptioncss:"display:block; position:relative; font:14px \"Lucida Sans Unicode\",\"Lucida Grande\",sans-serif,Arial; color:#00ccff;  background-color:#fff; margin-top:10px; padding:10px; ",

        navplaypauseimage:"<%=support_images_path%>"+"navplaypause-28-28-0.png",

        backgroundimagetop:-10,

        timercolor: "<%=pageParams.getValue("autoplay_countdown_color","#ffffff")%>" ,

        numberingformat:"%NUM/%TOTAL ",

        navthumbmediumwidth:64,

        navfontsize:12,

        navhighlightcolor:"",

        texteffectdelay1:1000,

        navimage:"<%=support_images_path%>"+"bullet-24-24-0.png",

        texteffectdelay2:1500,

        texteffectduration1:800,

        navshowplaypausestandaloneautohide:false,

        texteffectduration2:800,

        navbuttoncolor:"",

        navshowarrow:true,

        texteffectslidedirection:"bottom",

        navshowfeaturedarrow:false,

        lightboxbarheight:64,

        titlecss:"display:table; position:relative; font:16px \"Lucida Sans Unicode\",\"Lucida Grande\",sans-serif,Arial; color:#fff; white-space:nowrap; background-color:#00ccff; padding:10px;",

        ribbonimagey:0,

        ribbonimagex:0,

        navthumbsmallheight:48,

        texteffectslidedistance1:10,

        texteffectslidedistance2:10,

        navrowspacing:8,

        navshowplaypausestandaloneposition:"bottomright",

        shadowsize:5,

        lightboxthumbtopmargin:12,

        titlecssresponsive:"font-size:12px;",

        navshowplaypausestandalonewidth:28,

        navthumbresponsive:false,

        navfeaturedarrowimageheight:8,

        navopacity:0.8,

        textpositionmarginright:24,

        backgroundimagewidth:120,

        textautohide:true,

        navthumbtitlewidth:120,

        navpreviewposition:"bottom",

        texteffectseparate:true,

        arrowheight:32,

        shadowcolor:"#aaaaaa",

        arrowmargin:8,

        texteffectduration:600,

        bottomshadowimage:"<%=support_images_path%>"+"bottomshadow-110-95-3.png",

        border:0,

        lightboxshowdescription:false,

        timerposition:"bottom",

        navfontcolor:"#333333",

        navthumbnavigationstyle:"arrow",

        borderradius:0,

        navbuttonhighlightcolor:"",

        textpositionstatic:"bottom",

        texteffecteasing2:"easeOutCubic",

        navthumbstyle:"imageonly",

        texteffecteasing1:"easeOutCubic",

        textcss:"display:block; padding:8px 16px; text-align:left;",

        navthumbsmallwidth:48,

        navbordercolor:"#ffffff",

        navpreviewarrowimage:"<%=support_images_path%>"+"previewarrow-16-8-1.png",

        navthumbtitlecss:"display:block;position:relative;padding:2px 4px;text-align:left;font:bold 14px Arial,Helvetica,sans-serif;color:#333;",

        showbottomshadow: <%=pageParams.getBooleanValue("show_shadow_bottom",true)%>,

        texteffectslidedistance:10,

        texteffectdelay:800,

        textpositionmarginstatic:0,

        backgroundimage:"",

        navposition:"topright",

        texteffectslidedirection1:"bottom",

        navpreviewarrowwidth:16,

        textformat:"Blue box",

        texteffectslidedirection2:"bottom",

        bottomshadowimagetop:95,

        texteffectresponsive:true,

        navshowbuttons:true,

        lightboxthumbbottommargin:8,

        textpositionmarginbottom:24,

        lightboxshowtitle:true,

        tiles: {

            duration:2000,

            checked:true

        },

        slice: {

            checked:true,

            effectdirection:0,

            effects:"up,down,updown",

            slicecount:10,

            duration:1500,

            easing:"easeOutCubic"

        },

        blocks: {

            columncount:5,

            checked:true,

            rowcount:5,

            effects:"topleft,bottomright,top,bottom,random",

            duration:1500,

            easing:"easeOutCubic"

        },

        elastic: {

            duration:1000,

            easing:"easeOutElastic",

            checked:true,

            effectdirection:0

        },

        threedflipwithzoom: {

            duration:2000,

            fallback:"flipwithzoom",

            checked:true

        },

        threedwithzoom: {

            duration:2500,

            fallback:"flip",

            checked:true

        },

        flip: {

            duration:1500,

            checked:true

        },

        threedflip: {

            duration:1500,

            fallback:"flip",

            checked:true

        },

        threedtiles: {

            duration:2000,

            fallback:"tiles",

            checked:true

        },

        slide: {

            duration:1000,

            easing:"easeOutCubic",

            checked:true,

            effectdirection:0

        },

        crossfade: {

            duration:1000,

            easing:"easeOutCubic",

            checked:true

        },

        kenburns: {

            duration:5000,

            checked:true

        },

        threedhorizontalwithzoom: {

            duration:2200,

            fallback:"flipwithzoom",

            checked:true

        },

        fade: {

            duration:1000,

            easing:"easeOutCubic",

            checked:true

        },

        shuffle: {

            duration:1500,

            easing:"easeOutCubic",

            columncount:5,

            checked:true,

            rowcount:5

        },

        flipwithzoom: {

            duration:2000,

            checked:true

        },

        threedhorizontal: {

            checked:true,

            effectdirection:0,

            bgcolor:"#222222",

            perspective:1000,

            slicecount:1,

            duration:1500,

            easing:"easeOutCubic",

            fallback:"slice",

            scatter:5,

            perspectiveorigin:"bottom"

        },

        blinds: {

            duration:2000,

            easing:"easeOutCubic",

            checked:true,

            effectdirection:0,

            slicecount:3

        },

        threed: {

            checked:true,

            effectdirection:0,

            bgcolor:"#222222",

            perspective:1000,

            slicecount:5,

            duration:1500,

            easing:"easeOutCubic",

            fallback:"slice",

            scatter:5,

            perspectiveorigin:"right"

        },

        transition: "<%=pageParams.getValue("transitions_all","fade")%>",

        scalemode:"fill",

        isfullscreen:false,

        textformat: {



        }

    });

});

</iwcm:script>