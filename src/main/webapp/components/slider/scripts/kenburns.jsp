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

//custom id
Integer multipleSliderId = (Integer)request.getAttribute("multipleSliderId");
if (multipleSliderId == null) multipleSliderId = Integer.valueOf(1);

%>
<style type="text/css">

.amazingslider-img-elem-<%=multipleSliderId%> {
   min-width:100%;
  
    -moz-animation-duration: 275s;
    -webkit-animation-duration: 275s;
  
  -webkit-animation-name: slidein;
    -moz-animation-name: slidein;
    -ms-animation-name: slidein;
    -o-animation-name: slidein;
    animation-name: slidein;

    -webkit-animation-iteration-count: infinite;
    -moz-animation-iteration-count: infinite;
    -ms-animation-iteration-count: infinite;
    -o-animation-iteration-count: infinite;
    animation-iteration-count: infinite;
    -webkit-animation-direction: alternate;
}

@keyframes "slidein" {
 25% {
    -webkit-transform: scale(1.6) translate3d(-400px, 0px, 0px);
    -moz-transform: scale(1.6) translate3d(-400px, 0px, 0px);
    -o-transform: scale(1.6) translate3d(-400px, 0px, 0px);
    -ms-transform: scale(1.6) translate3d(-400px, 0px, 0px);
    transform: scale(1.6) translate3d(-400px, 0px, 0px);
 }
 50% {
    -webkit-transform: scale(1) translate3d(0px, 0px, 0px);
    -moz-transform: scale(1) translate3d(0px, 0px, 0px);
    -o-transform: scale(1) translate3d(0px, 0px, 0px);
    -ms-transform: scale(1) translate3d(0px, 0px, 0px);
    transform: scale(1) translate3d(0px, 0px, 0px);
 }
 75% {
    -webkit-transform: scale(1.6) translate3d(+400px, 0px, 0px);
    -moz-transform: scale(1.6) translate3d(+400px, 0px, 0px);
    -o-transform: scale(1.6) translate3d(+400px, 0px, 0px);
    -ms-transform: scale(1.6) translate3d(+400px, 0px, 0px);
    transform: scale(1.6) translate3d(+400px, 0px, 0px);
 }

}

@-moz-keyframes slidein {
 25% {
   -moz-transform: scale(1.6) translate3d(-400px, 0px, 0px);
   transform: scale(1.6) translate3d(-400px, 0px, 0px);
 }
 50% {
   -moz-transform: scale(1) translate3d(0px, 0px, 0px);
   transform: scale(1) translate3d(0px, 0px, 0px);
 }
 75% {
   -moz-transform: scale(1.6) translate3d(+400px, 0px, 0px);
   transform: scale(1.6) translate3d(+400px, 0px, 0px);
 }

}

@-webkit-keyframes "slidein" {
 25% {
   -webkit-transform: scale(1.6) translate3d(-400px, 0px, 0px);
   transform: scale(1.6) translate3d(-400px, 0px, 0px);
 }
 50% {
   -webkit-transform: scale(1) translate3d(0px, 0px, 0px);
   transform: scale(1) translate3d(0px, 0px, 0px);
 }
 75% {
   -webkit-transform: scale(1.6) translate3d(+400px, 0px, 0px);
   transform: scale(1.6) translate3d(+400px, 0px, 0px);
 }

}

@-ms-keyframes "slidein" {
 25% {
   -ms-transform: scale(1.6) translate3d(-400px, 0px, 0px);
   transform: scale(1.6) translate3d(-400px, 0px, 0px);
 }
 50% {
   -ms-transform: scale(1) translate3d(0px, 0px, 0px);
   transform: scale(1) translate3d(0px, 0px, 0px);
 }
 75% {
   -ms-transform: scale(1.6) translate3d(+400px, 0px, 0px);
   transform: scale(1.6) translate3d(+400px, 0px, 0px);
 }

}

@-o-keyframes "slidein" {
 25% {
   -o-transform: scale(1.6) translate3d(-400px, 0px, 0px);
   transform: scale(1.6) translate3d(-400px, 0px, 0px);
 }
 50% {
   -o-transform: scale(1) translate3d(0px, 0px, 0px);
   transform: scale(1) translate3d(0px, 0px, 0px);
 }
 75% {
   -o-transform: scale(1.6) translate3d(+400px, 0px, 0px);
   transform: scale(1.6) translate3d(+400px, 0px, 0px);
 }

}
/* keby nefungovalo:tak skus toto:https://www.kirupa.com/html5/ken_burns_effect_css.htm */
</style>