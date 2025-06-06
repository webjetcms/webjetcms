<%@page import="java.util.Map"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
    <head>
    <%
    String lng = PageLng.getUserLng(request);
    pageContext.setAttribute("lng", lng);

    String place = Tools.isEmpty(Tools.getRequestParameter(request, "object")) ? "Stara vajnorska 21, Bratislava" :Tools.getRequestParameter(request, "object");
    String width = Tools.isEmpty(Tools.getRequestParameter(request, "width")) ? "400" :Tools.getRequestParameter(request, "width");
    String height = Tools.isEmpty(Tools.getRequestParameter(request, "height")) ? "400" :Tools.getRequestParameter(request, "height");
    String widthPercent = Tools.isEmpty(Tools.getRequestParameter(request, "widthPercent")) ? "100" :Tools.getRequestParameter(request, "widthPercent");
    String heightPercent = Tools.isEmpty(Tools.getRequestParameter(request, "heightPercent")) ? "100" :Tools.getRequestParameter(request, "heightPercent");
    int zoom = Tools.getIntValue(Tools.getRequestParameter(request, "zoom"), 13);
    String longitude = Tools.isEmpty(Tools.getRequestParameter(request, "longitude")) ? null :Tools.getRequestParameter(request, "longitude");
    String latitude = Tools.isEmpty(Tools.getRequestParameter(request, "latitude")) ? null :Tools.getRequestParameter(request, "latitude");
    String label = Tools.isEmpty(Tools.getRequestParameter(request, "label")) ? "" :Tools.getRequestParameter(request, "label");
    int view = Tools.getIntValue(Tools.getRequestParameter(request, "view"), 0);

    boolean showContentString = "true".equals(Tools.getRequestParameter(request, "showContentString"));
    boolean sizeInPercent = "true".equals(Tools.getRequestParameter(request, "sizeInPercent"));
    boolean showControls = "true".equals(Tools.getRequestParameter(request, "showControls"));
    boolean scrollwheel = "true".equals(Tools.getRequestParameter(request, "scrollwheel"));
    %>

    <%=Tools.insertJQuery(request) %>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.4.0/dist/leaflet.css" integrity="sha512-puBpdR0798OZvTTbP4A8Ix/l+A4dHDD0DGqYW6RQ+9jxkRFclaxxQb/SJAWZfWAkuyeQUytO7+7N4QKrDh+drA==" crossorigin=""/>
    <script src="/components/_common/javascript/leaflet.js"></script>

    <style type="text/css">
        body {
            margin: 0px;
            padding: 0px;
        }
        #map {
            width: 100%!important;
            height: 400px!important;
        }
    </style>
    </head>

    <body>
        <input type="hidden" id="m-lat"><input type="hidden" id="m-lng">
        <div id="map" style="width: <%=width%>px; height: <%=height%>px;" class="details"></div>
    </body>
</html>

<script type="text/javascript">
var map = L.map('map', {
    scrollWheelZoom: <%= scrollwheel %>,
    zoomControl: <%= showControls %>
});
var marker;

L.tileLayer('https://{s}.tile.osm.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://osm.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

if (<%=latitude%> == null || <%=longitude%> == null) {
    $.get('https://nominatim.openstreetmap.org/search?format=json&q=<%= place %>', function(data){
        map.setView([data[0].lat, data[0].lon], <%= zoom %>);
        L.marker([data[0].lat, data[0].lon]).addTo(map);
    });
} else {
    map.setView([<%=latitude%>, <%=longitude%>], <%= zoom %>);
    marker = L.marker([<%=latitude%>, <%=longitude%>]).addTo(map);
}

map.on('click', addMarker);
map.on('click', function(e){

    var coord = e.latlng;
    var lat = coord.lat;
    var lng = coord.lng;

    document.getElementById("m-lat").value = lat;
    document.getElementById("m-lng").value = lng;

    var coords = { 'lat': lat, 'lng': lng};

    localStorage.setItem('coords', JSON.stringify(coords));

});

function addMarker(e){
    if (marker) {
        map.removeLayer(marker);
    }
    marker = L.marker(e.latlng).addTo(map);
}
</script>
