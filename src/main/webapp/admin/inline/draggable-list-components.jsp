<%
  sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.stat.*,java.util.*" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
        taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
        taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
        taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
  Prop prop = Prop.getInstance(request);

%>
{
  "items": [
    {
      "name": "<iwcm:text key="components.map.title"/>",
      "img": "/components/map/editoricon.png",
      "value": "!INCLUDE(/components/map/google_map.jsp, width=400, height=400, zoom=13, view=0, label=, markerIcon=, showContentString=false, scrollwheel=false object=)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.video.title"/>",
      "img": "/components/video/editoricon.png",
      "value": "!INCLUDE(/components/vide/video.jsp)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.inquiry.title"/>",
      "img": "/components/inquiry/editoricon.png",
      "value": "!INCLUDE(/components/inquiry/inquiry.jsp)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.slider.title"/>",
      "img": "/components/slider/editoricon.png",
      "value": "!INCLUDE(/components/slider/slider.jsp)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.app-testimonials.editMenu"/>",
      "img": "/components/app-testimonials/editoricon.png",
      "value": "!INCLUDE(/components/app-testimonials/news-01.jsp)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.gallery.title"/>",
      "img": "/components/gallery/editoricon.png",
      "value": "!INCLUDE(/components/gallery/gallery.jsp)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.news.title"/>",
      "img": "/components/news/editoricon.png",
      "value": "!INCLUDE(/components/news/news.jsp)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.inline.tabs.components.news"/>",
      "img": "/components/app-mesta_aktuality/editoricon.png",
      "value": "!INCLUDE(/components/app-mesta_aktuality/news-01.jsp)!",
      "class": "component"
    }
  ,
    {
      "name": "<iwcm:text key="components.blog.title"/>",
      "img": "/components/app-blog/editoricon.png",
      "value": "!INCLUDE(/components/app-blog/blog-01.jsp)!",
      "class": "component"
    }
  ]
}