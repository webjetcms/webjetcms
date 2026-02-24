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

	String key = Tools.getStringValue(Tools.getRequestParameter(request, "key"), Constants.getString("googleMapsApiKey"));
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
	    mapTypeId: google.maps.<%=viewMap%>,	//typ mapy
	    scrollwheel: <%=scrollwheel%>
	    <%if(!showControls){%>
			,mapTypeControl: false,
			fullscreenControl: false,
			zoomControl: false,
			streetViewControl: false,
			cameraControl: false
	    <%}%>
	 };

    map = new google.maps.Map(document.getElementById("map"), myOptions);

    // Apply offset using OverlayView
    var ov = new google.maps.OverlayView();
    ov.onAdd = function() {
        var proj = this.getProjection();
        var aPoint = proj.fromLatLngToContainerPixel(latlng);
        aPoint.x = aPoint.x + <%=offsetX%>;
        aPoint.y = aPoint.y + <%=offsetY%>;
        map.setCenter(proj.fromContainerPixelToLatLng(aPoint));
    };
    ov.draw = function() {};
    ov.setMap(map);

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
	var labelText = "<%=label%>";
	<%if(showContentString){%>
		// showContentString is true - show coordinates and label
		if(address == ""){
			contentString = (labelText != "" ? "<b>"+labelText+"</b><br/>" : "")+"<iwcm:text key='components.map.latitude'/>"+": <%=latitude%><br/>"+"<iwcm:text key='components.map.longitude'/>"+": <%=longitude%>";
		}
		else {
			contentString = (labelText != "" ? "<b>"+labelText+"</b><br/>" : "") + address;
		}
		var infowindow = new google.maps.InfoWindow({
		    content: contentString
		});

		<%if(!closeLabel){%>
			console.log("Removing close button from InfoWindow");
			google.maps.event.addListener(infowindow, "domready", function() {
				document.querySelectorAll("button.gm-ui-hover-effect").forEach(function(el) { el.remove(); });
			});
		<%}%>

		// Automatically open the InfoWindow
		infowindow.open(map, marker);

		google.maps.event.addListener(marker, 'click', function() {
				console.log("Marker clicked");
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

			// Update InfoWindow content with new coordinates and show it
			var newContentString = (labelText != "" ? "<b>"+labelText+"</b><br/>" : "")+"<iwcm:text key='components.map.latitude'/>"+": "+lat+"<br/>"+"<iwcm:text key='components.map.longitude'/>"+": "+lng;
			infowindow.setContent(newContentString);
			infowindow.open(map, marker);

			// Add click listener to new marker
			google.maps.event.addListener(marker, 'click', function() {
				infowindow.open(map, marker);
			});
		});
	<%} else if(!label.isEmpty()) {%>
		// showContentString is false but label is set - show only label
		contentString = "<b>"+labelText+"</b>";
		var infowindow = new google.maps.InfoWindow({
		    content: contentString
		});

		<%if(!closeLabel){%>
			console.log("Removing close button from InfoWindow");
			google.maps.event.addListener(infowindow, "domready", function() {
				document.querySelectorAll("button.gm-ui-hover-effect").forEach(function(el) { el.remove(); });
			});
		<%}%>

		// Automatically open the InfoWindow
		infowindow.open(map, marker);

		google.maps.event.addListener(marker, 'click', function() {
				infowindow.open(map,marker);
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

			// Update InfoWindow content - show only label
			infowindow.setContent("<b>"+labelText+"</b>");
			infowindow.open(map, marker);

			// Add click listener to new marker
			google.maps.event.addListener(marker, 'click', function() {
				infowindow.open(map, marker);
			});
		});
	<%} else {%>
		// No label shown - just allow moving the pin
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
	<%}%>
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
</style>
</head>
<body>
<input type="hidden" id="m-lat"><input type="hidden" id="m-lng">
<%if(sizeInPercent){%>
	<div id="map" style="width: <%=width%>%; height: <%=height%>%;" class="details">
<%} else {%>
	<div id="map" style="width: <%=width%>px; height: <%=height%>px;" class="details">
<%}%>

<script type="text/javascript">
	window.addEventListener("load", load_map);
</script>
</div>

</body>
</html>
