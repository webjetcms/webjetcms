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
    String longitude = Tools.isEmpty(Tools.getRequestParameter(request, "longitude")) ? null :Tools.getRequestParameter(request, "longitude");
    String latitude = Tools.isEmpty(Tools.getRequestParameter(request, "latitude")) ? null :Tools.getRequestParameter(request, "latitude");

    boolean sizeInPercent = "true".equals(Tools.getRequestParameter(request, "sizeInPercent"));
    String width = Tools.isEmpty(Tools.getRequestParameter(request, "width")) ? "400" :Tools.getRequestParameter(request, "width");
    String height = Tools.isEmpty(Tools.getRequestParameter(request, "height")) ? "400" :Tools.getRequestParameter(request, "height");

    int zoom = Tools.getIntValue(Tools.getRequestParameter(request, "zoom"), 13);
	// Show controls by default true, so it wont turn off the controll buttons
	String showControlsStr = Tools.getRequestParameter(request, "showControls");
    boolean showControls = Tools.isEmpty(showControlsStr) ? true : "true".equals(showControlsStr);
    boolean scrollwheel = "true".equals(Tools.getRequestParameter(request, "scrollwheel"));

    String label = Tools.isEmpty(Tools.getRequestParameter(request, "label")) ? "" :Tools.getRequestParameter(request, "label");
    int view = Tools.getIntValue(Tools.getRequestParameter(request, "view"), 0);
    boolean showContentString = "true".equals(Tools.getRequestParameter(request, "showContentString"));
    boolean closeLabel = "true".equals(Tools.getRequestParameter(request, "closeLabel"));

    int offsetX = Tools.getIntValue(Tools.getRequestParameter(request, "offsetX"), 0);
    int offsetY = Tools.getIntValue(Tools.getRequestParameter(request, "offsetY"), 0);
    %>

    <%=Tools.insertJQuery(request) %>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.4.0/dist/leaflet.css" integrity="sha512-puBpdR0798OZvTTbP4A8Ix/l+A4dHDD0DGqYW6RQ+9jxkRFclaxxQb/SJAWZfWAkuyeQUytO7+7N4QKrDh+drA==" crossorigin=""/>
    <script src="/components/_common/javascript/leaflet.js"></script>

    <style type="text/css">
        body {
            margin: 0px;
            padding: 0px;
        }
    </style>
    </head>

    <body>
        <input type="hidden" id="m-lat"><input type="hidden" id="m-lng">
        <% if(sizeInPercent) { %>
            <div id="map" style="width: <%=width%>%; height: <%=height%>%;" class="details"></div>
        <% } else { %>
            <div id="map" style="width: <%=width%>px; height: <%=height%>px;" class="details"></div>
        <% } %>
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
        var targetPoint = map.project([data[0].lat, data[0].lon], <%= zoom %>).subtract([<%=offsetX%>, <%=offsetY%>]);
        var targetLatLng = map.unproject(targetPoint, <%= zoom %>);
        map.setView(targetLatLng, <%= zoom %>);
        marker = L.marker([data[0].lat, data[0].lon]).addTo(map);
        <% if(showContentString) { %>
            var labelText = "<%=label%>";
            var contentString = (labelText != "" ? "<b>" + labelText + "</b><br/>" : "") + "<iwcm:text key='components.map.latitude'/>: " + data[0].lat + "<br/><iwcm:text key='components.map.longitude'/>: " + data[0].lon;
            marker.bindPopup(contentString, {closeButton: <%=closeLabel%>}).openPopup();
        <% } else if(Tools.isNotEmpty(label)) { %>
            var contentString = "<b><%=label%></b>";
            marker.bindPopup(contentString, {closeButton: <%=closeLabel%>}).openPopup();
        <% } %>
    });
} else {
    var targetPoint = map.project([<%=latitude%>, <%=longitude%>], <%= zoom %>).subtract([<%=offsetX%>, <%=offsetY%>]);
    var targetLatLng = map.unproject(targetPoint, <%= zoom %>);
    map.setView(targetLatLng, <%= zoom %>);
    marker = L.marker([<%=latitude%>, <%=longitude%>]).addTo(map);

    <% if(showContentString) { %>
        var labelText = "<%=label%>";
        var contentString = (labelText != "" ? "<b>" + labelText + "</b><br/>" : "") + "<iwcm:text key='components.map.latitude'/>: <%=latitude%><br/><iwcm:text key='components.map.longitude'/>: <%=longitude%>";
        marker.bindPopup(contentString, {closeButton: <%=closeLabel%>}).openPopup();
    <% } else if(Tools.isNotEmpty(label)) { %>
        var contentString = "<b><%=label%></b>";
        marker.bindPopup(contentString, {closeButton: <%=closeLabel%>}).openPopup();
    <% } %>
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
    <% if(showContentString) { %>
        var labelText = "<%=label%>";
        var newContentString = (labelText != "" ? "<b>" + labelText + "</b><br/>" : "") + "<iwcm:text key='components.map.latitude'/>: " + e.latlng.lat + "<br/><iwcm:text key='components.map.longitude'/>: " + e.latlng.lng;
        marker.bindPopup(newContentString, {closeButton: <%=closeLabel%>}).openPopup();
    <% } else if(Tools.isNotEmpty(label)) { %>
        var newContentString = "<b><%=label%></b>";
        marker.bindPopup(newContentString, {closeButton: <%=closeLabel%>}).openPopup();
    <% } %>
}
</script>
