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

<%--

	Viacmenej totozna stranka s povodnou google mapou s tym rozdielom, ze tato sluzi
	na preview, co u povodnej stranky nie je take jednoduche, kedze ide o komponentu

--%>

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
String key = Tools.getStringValue(Tools.getRequestParameter(request, "key"), Constants.getString("googleMapsApiKey"));

boolean showContentString = "true".equals(Tools.getRequestParameter(request, "showContentString"));
boolean sizeInPercent = "true".equals(Tools.getRequestParameter(request, "sizeInPercent"));
boolean showControls = "true".equals(Tools.getRequestParameter(request, "showControls"));

String markerIcon = Tools.getStringValue(Tools.getRequestParameter(request, "markerIcon"), "");

String viewMap;
switch(view){
	case 1:
		viewMap = "MapTypeId.SATELLITE";
		break;
	case 2:
		viewMap = "MapTypeId.HYBRID";
		break;
	case 3:
		viewMap = "MapTypeId.TERRAIN";
		break;
	default:
		viewMap = "MapTypeId.ROADMAP";
}
%>
<script src="/components/form/event_attacher.js" type="text/javascript" ></script>
<script src="https://maps.googleapis.com/maps/api/js?sensor=false<%=(Tools.isNotEmpty(key) ? "&key="+key : "")%>" type="text/javascript"></script>
<script type="text/javascript">
//<![CDATA[
var map = null;
var geocoder = null;

function load_map()
{
	try{
    var latlng;
    var myOptions;
    latlng = new google.maps.LatLng(<%=latitude%>, <%=longitude%>);	//nastavenie pozicie podla zemepisnej sirky a dlzky
    myOptions =
	 {
	    zoom: <%=zoom%>,
	    center: latlng,
	    mapTypeId: google.maps.<%=viewMap%>	//typ mapy
	 };

    map = new google.maps.Map(document.getElementById("map"), myOptions);

    if(<%=longitude%> == null || <%=latitude%> == null){	//ak nemame nastavenu zemepisnu sirku a vysku
    	geocoder = new google.maps.Geocoder();
  	    var address = "<%=place%>";
  	    geocoder.geocode( { 'address': address}, function(results, status) {
  	      if (status == google.maps.GeocoderStatus.OK) {
  	        map.setCenter(results[0].geometry.location);
  	        setMarker(address, results[0].geometry.location);	//nastavim marker
  	      } else {
  	        alert("Geocode was not successful for the following reason: " + status);
  	      }
  	    });
    }
    else{
    	setMarker("", latlng);	//inak nastavim iba marker
    }
	}
	catch(e){}
}

function setMarker(address, position){
	var marker = new google.maps.Marker({
	      position: position,
	      icon: "<%=markerIcon%>",
	      map: map,
	      animation: google.maps.Animation.DROP
	  });
	var contentString;	//text pre informacne okno
	if(address == ""){
		contentString = "<iwcm:text key="components.map.latitude"/>"+": <%=latitude%><br/>"+"<iwcm:text key="components.map.longitude"/>"+": <%=longitude%>"+"<br><%=label%>";
	}
	else {
		contentString = address + "<br>"+"<%=label%>";
	}
	<%if(showContentString){%>
		var infowindow = new google.maps.InfoWindow({
		    content: contentString
		});
	<%}%>

		google.maps.event.addListener(marker, 'click', function() {
				infowindow.open(map,marker);	//pridam otvorenie informacneho okna po kliknuti na marker
			});

		google.maps.event.addListener(map, "click", function(e) {

			latLng = e.latLng;

			// if marker exists and has a .setMap method, hide it
			if (marker && marker.setMap) {
				marker.setMap(null);
			}
			marker = new google.maps.Marker({
				position: latLng,
				map: map
			});

			var lat = marker.getPosition().lat();
			var lng = marker.getPosition().lng();

			document.getElementById("m-lat").value = lat;
			document.getElementById("m-lng").value = lng;

			var coords = { 'lat': lat, 'lng': lng};

				// Put the object into storage
			localStorage.setItem('coords', JSON.stringify(coords));
		});

}

//animacia - momentalne sa nepouziva
function toggleBounce() {

	  if (marker.getAnimation() != null) {
	    marker.setAnimation(null);
	  } else {
	    marker.setAnimation(google.maps.Animation.BOUNCE);
	  }

	}
//]]>
</script>
	<style type="text/css">
		body { margin: 0px; padding: 0px; }
		#map {
			width: 100%!important;
			height: 400px!important;
		}
	</style>
</head>
<body>
<input type="hidden" id="m-lat"><input type="hidden" id="m-lng">
<div id="map" style="width: <%=width%>px; height: <%=height%>px;" class="details">

<script type="text/javascript">
	addEvent(window, "load", load_map);
</script>
</div>

</body>
</html>
