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
<iwcm:script type="text/javascript"> // CUBE skin

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

        skin: "<%=pageParams.getValue("skin","Classic")%>",

        addgooglefonts:true,

        navshowplaypause:true,

        navshowplayvideo:true,

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

        navcolor:"#999999",

        lightboxdescriptionbottomcss:"{color:#333; font-size:12px; font-family:Arial,Helvetica,sans-serif; overflow:hidden; text-align:left; margin:4px 0px 0px; padding: 0px;}",

        lightboxthumbwidth:80,

        navthumbnavigationarrowimagewidth:32,

        navthumbtitlehovercss:"text-decoration:underline;",

        navthumbmediumheight:64,

        texteffectresponsivesize:600,

        arrowwidth:36,

        texteffecteasing:"easeOutCubic",

        texteffect:"slide",

        lightboxthumbheight:60,

        navspacing:8,

        navarrowimage:"<%=support_images_path%>"+"navarrows-28-28-0.png",

        bordercolor:"#ffffff",

        ribbonimage:"<%=support_images_path%>"+"ribbon_topleft-0.png",

        navwidth:24,

        navheight:24,

        arrowimage:"<%=support_images_path%>"+"arrows-36-80-0.png",

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

        arrowstyle: "<%=pageParams.getValue("arrow_style","always")%>",

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

        navstyle: "<%=pageParams.getValue("nav_style","bullets")%>",

        textpositionmarginleft:24,

        descriptioncss:"display:block; position:relative; font:12px \"Lucida Sans Unicode\",\"Lucida Grande\",sans-serif,Arial; color:#fff;  background-color:#e04000; margin-top:10px; padding:10px; ",

        navplaypauseimage:"<%=support_images_path%>"+"navplaypause-28-28-0.png",

        backgroundimagetop:-10,

        timercolor: "<%=pageParams.getValue("autoplay_countdown_color","#ffffff")%>" ,

        numberingformat:"%NUM/%TOTAL ",

        navthumbmediumwidth:64,

        navfontsize:12,

        navhighlightcolor:"#333333",

        texteffectdelay1:1000,

        navimage:"<%=support_images_path%>"+"bullet-24-24-1.png",

        texteffectdelay2:1500,

        texteffectduration1:600,

        navshowplaypausestandaloneautohide:false,

        texteffectduration2:600,

        navbuttoncolor:"#999999",

        navshowarrow:true,

        texteffectslidedirection:"left",

        navshowfeaturedarrow:false,

        lightboxbarheight:64,

        titlecss:"display:table; position:relative; font:bold 14px \"Lucida Sans Unicode\",\"Lucida Grande\",sans-serif,Arial; color:#fff; white-space:nowrap; background-color:#f7a020; padding:10px;",

        ribbonimagey:0,

        ribbonimagex:0,

        navthumbsmallheight:48,

        texteffectslidedistance1:120,

        texteffectslidedistance2:120,

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

        navpreviewposition:"top",

        texteffectseparate:true,

        arrowheight:80,

        shadowcolor:"#aaaaaa",

        arrowmargin:-18,

        texteffectduration:600,

        bottomshadowimage:"<%=support_images_path%>"+"bottomshadow-110-95-0.png",

        border:0,

        lightboxshowdescription:false,

        timerposition:"bottom",

        navfontcolor:"#333333",

        navthumbnavigationstyle:"arrow",

        borderradius:0,

        navbuttonhighlightcolor:"#333333",

        textpositionstatic:"bottom",

        texteffecteasing2:"easeOutCubic",

        navthumbstyle:"imageonly",

        texteffecteasing1:"easeOutCubic",

        textcss:"display:block; padding:8px 16px; text-align:left;",

        navthumbsmallwidth:48,

        navbordercolor:"#ffffff",

        navpreviewarrowimage:"<%=support_images_path%>"+"previewarrow-16-8-0.png",

        navthumbtitlecss:"display:block;position:relative;padding:2px 4px;text-align:left;font:bold 14px Arial,Helvetica,sans-serif;color:#333;",

        showbottomshadow: <%=pageParams.getBooleanValue("show_shadow_bottom",true)%>,

        texteffectslidedistance:30,

        texteffectdelay:500,

        textpositionmarginstatic:0,

        backgroundimage:"",

        navposition:"bottom",

        texteffectslidedirection1:"right",

        navpreviewarrowwidth:16,

        textformat:"Color box",

        texteffectslidedirection2:"right",

        bottomshadowimagetop:95,

        texteffectresponsive:true,

        navshowbuttons:false,

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