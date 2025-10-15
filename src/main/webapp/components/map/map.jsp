<%@page import="com.fasterxml.jackson.databind.ObjectMapper"%>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants" %>
<%@ page import="sk.iway.iwcm.PageLng" %>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.tags.WriteTag" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
    //Video prehravac nepodporuje packager ControlJS
    request.setAttribute("packagerEnableControljs", Boolean.FALSE);

    int mapCounter = request.getAttribute("mapCounter") != null ? (Integer) request.getAttribute("mapCounter") : 0;
    mapCounter++;
    request.setAttribute("mapCounter", mapCounter);

    String lng = PageLng.getUserLng(request);
    pageContext.setAttribute("lng", lng);

    String mapType = Constants.getString("mapProvider", "OpenStreetMap");
    request.setAttribute("mapType", mapType);

    PageParams pageParams = new PageParams(request);
    Map<String, Object> options = new HashMap<String, Object>();

    int view = pageParams.getIntValue("view", 0);
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

    options.put("zoom", pageParams.getIntValue("zoom", 13));
    options.put("view", view);
    options.put("viewMap", viewMap);
    options.put("showContentString", pageParams.getBooleanValue("showContentString", true));
    options.put("labelAddress", pageParams.getBooleanValue("labelAddress", true));
    options.put("closeLabel", pageParams.getBooleanValue("closeLabel", true));
    options.put("markerIcon", pageParams.getValue("markerIcon", ""));
    options.put("label", pageParams.getValue("label", ""));

    String place = (String)request.getAttribute("object");
    if(Tools.isEmpty(place)) {
        place = pageParams.getValue("object", "");
    }

    options.put("place", place);
    options.put("longitude", pageParams.getValue("longitude", null));
    options.put("latitude", pageParams.getValue("latitude", null));
    options.put("key", pageParams.getValue("key", Constants.getString("googleMapsApiKey")));

    int offsetX = pageParams.getIntValue("offsetX", 0);
    int offsetY = pageParams.getIntValue("offsetY", 0);

    // vynasobim -1 aby to odsadenie davalo zmysel
    offsetX *= -1;
    offsetY *= -1;

    options.put("offsetX", offsetX);
    options.put("offsetY", offsetY);
    options.put("scrollwheel", pageParams.getBooleanValue("scrollwheel", false));
    options.put("showControls", pageParams.getBooleanValue("showControls", false));

    boolean mapAllreadyIncluded = (request.getAttribute("googleMapAllreadyIncluded")!=null);
    request.setAttribute("googleMapAllreadyIncluded", "true");

    ObjectMapper objectMapper = new ObjectMapper();
    request.setAttribute("optionsHtml", objectMapper.writeValueAsString(options));

    boolean sizeInPercent = pageParams.getBooleanValue("sizeInPercent", false);
    boolean closeLabel = pageParams.getBooleanValue("closeLabel", false);
    String width = (String)request.getAttribute("width");
    String widthPercent = (String)request.getAttribute("widthPercent");
    if(Tools.isEmpty(width))
        width = pageParams.getValue("width","400");
    
    if(Tools.isEmpty(widthPercent))
        widthPercent = pageParams.getValue("widthPercent","100");
    
    String height = (String)request.getAttribute("height");
    String heightPercent = (String)request.getAttribute("heightPercent");
    if(Tools.isEmpty(height))
        height = pageParams.getValue("height","400");
    
    if(Tools.isEmpty(heightPercent))
        heightPercent = pageParams.getValue("heightPercent","100");

    if(!height.endsWith("px") && !height.endsWith("%")) height+="px";
    if(!width.endsWith("px") && !width.endsWith("%")) width+="px";
    if(!heightPercent.endsWith("px") && !heightPercent.endsWith("%")) heightPercent+="%";
    if(!widthPercent.endsWith("px") && !widthPercent.endsWith("%")) widthPercent+="%";

    if (sizeInPercent) {
        width = widthPercent;
        height = heightPercent;
    }

    if (!mapAllreadyIncluded)
    {%>
<script type="text/javascript">
    var MapFactory = (function(){
        return {
            getMap: function(options) {
                var mapType;

                if (typeof ${mapType} != 'function') {
                    mapType = OpenStreetMap;
                }
                else {
                    mapType = ${mapType};
                }

                return new mapType(options);
            }
        }
    })();

    var GoogleMap = (function(options) {

        var scripts = [
                {
                    src: "https://maps.googleapis.com/maps/api/js?sensor=false"
                }
            ],
            map;


        function addKey() {
            if (MapTools.isNotEmpty(options.key)) {
                $.each(scripts, function(i, v){
                   v.src = v.src + "&key=" + options.key;
                });
            }
        }

        function addAssets() {
            addKey();
            MapTools.addTo($('body'), '<script type="text/javascript" {attrs} /><\/script>', scripts);
        }

        function createMap() {
            var latlng = new google.maps.LatLng(options.latitude, options.longitude);	//nastavenie pozicie podla zemepisnej sirky a dlzky
            var mapOptions = {
                zoom: options.zoom,
                center: latlng,
                scrollwheel: options.scrollwheel,
                mapTypeId: google.maps[options.viewMap]	//typ mapy
                // };
            }
            map = new google.maps.Map(options.mapElement.get(0), mapOptions);
        }

        function geocode(query, callback) {
            var geocoder = new google.maps.Geocoder();
            geocoder.geocode( { 'address': query}, function(results, status) {
                if (results.length == 0 || status != google.maps.GeocoderStatus.OK) {
                    console.log("Geocode query: " + query + ", was not successful for the following reason: " + status);
                    return;
                }
                else {
                    options.latitude = results[0].geometry.location.lat();
                    options.longitude = results[0].geometry.location.lng();
                }

                callback();
            });
        }

        function setView() {
            if (options.latitude == null || options.longitude == null) {
                geocode(options.place, setView);
                return;
            }

            var latlng = new google.maps.LatLng(options.latitude, options.longitude);	//nastavenie pozicie podla zemepisnej sirky a dlzky
            var ov = new google.maps.OverlayView();
            ov.onAdd = function() {
                var proj = this.getProjection();
                var aPoint = proj.fromLatLngToContainerPixel(latlng);
                aPoint.x = aPoint.x + options.offsetX;
                aPoint.y = aPoint.y + options.offsetY;
                map.setCenter(proj.fromContainerPixelToLatLng(aPoint));
            };
            ov.draw = function() {};
            ov.setMap(map);

            addMarker();
        }

        function addMarker() {
            var latlng = new google.maps.LatLng(options.latitude, options.longitude);	//nastavenie pozicie podla zemepisnej sirky a dlzky
            var marker = new google.maps.Marker({
                position: latlng,
                map: map,
                icon: options.markerIcon,
                animation: google.maps.Animation.DROP
            });

            if (options.showContentString) {
                var text = MapTools.getMarkerText(options);
                var infowindow = new google.maps.InfoWindow({
                    content: text
                });
                infowindow.open(map, marker);
            }

            if (!options.closeLabel) {
                setTimeout(() => {
                    $(".gm-style-iw button").remove();
                }, 500);
            }
        }

        return {
            render: function(){
                addAssets();
                var self = this;

                // cakame na nacitanie JS
                if (typeof google == 'undefined') {
                    setTimeout(function(){
                        self.render();
                    });
                    return;
                }

                createMap();
                setView();
            }
        };
    });

    var OpenStreetMap = (function(options) {

        var scripts = [
                {
                    src: "https://unpkg.com/leaflet@1.4.0/dist/leaflet.js",
                    integrity: "sha512-QVftwZFqvtRNi0ZyCtsznlKSWOStnDORoefr1enyq5mVL4tmKB3S/EnC3rRJcxCPavG10IcrVGSmPh6Qw5lwrg==",
                    crossorigin: ""
                }
            ],
            links = [
                {
                    href: "https://unpkg.com/leaflet@1.4.0/dist/leaflet.css",
                    integrity: "sha512-puBpdR0798OZvTTbP4A8Ix/l+A4dHDD0DGqYW6RQ+9jxkRFclaxxQb/SJAWZfWAkuyeQUytO7+7N4QKrDh+drA==",
                    crossorigin: ""
                }
            ],
            map;

        function addAssets() {
            MapTools.addTo($('head'), '<link rel="stylesheet" {attrs} />', links);
            MapTools.addTo($('body'), '<script type="text/javascript" {attrs} /><\/script>', scripts);
        }

        function createMap() {
            map = L.map(options.mapElement.attr('id'), {
                scrollWheelZoom: options.scrollwheel
            });

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);
        }

        function setView() {
            if (options.latitude == null || options.longitude == null) {
                geocode(options.place, setView);
                return;
            }

            var targetPoint = map.project([options.latitude, options.longitude], options.zoom).subtract([-options.offsetX, -options.offsetY]),
                targetLatLng = map.unproject(targetPoint, options.zoom);

            map.setView(targetLatLng, options.zoom);
            addMarker();
        }

        function addMarker() {
            var markerOptions = {};

            if (MapTools.isNotEmpty(options.markerIcon)) {
                var icon = L.icon({
                    iconUrl: options.markerIcon,
                    //shadowUrl: 'leaf-shadow.png',

                    iconSize:     [32, 32], // size of the icon
                    iconAnchor:   [0, 0], // point of the icon which will correspond to marker's location

                    //shadowSize:   [50, 64], // size of the shadow
                    //shadowAnchor: [0, 0],  // the same for the shadow

                    popupAnchor:  [16, 0] // point from which the popup should open relative to the iconAnchor
                });

                markerOptions.icon = icon;
            }

            var marker = L.marker([options.latitude, options.longitude], markerOptions).addTo(map);

            if (options.showContentString) {
                var text = MapTools.getMarkerText(options);
                marker.bindPopup(text).openPopup();
            }

            if (!options.closeLabel) {
                setTimeout(() => {
                    $(".leaflet-popup-close-button").remove();
                }, 500);
            }
        }

        function geocode(query, callback) {
            var url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + query;

            $.ajax({
                url: url,
                success: function(response) {
                    if (response.length > 0) {
                        options.latitude = response[0].lat;
                        options.longitude = response[0].lon;

                        callback(response);
                    }
                    else {
                        console.log("Geocode query " + query + " failed");
                    }
                }
            })
        }

        function repairOnModalOpen() {
            $('.modal').on('shown.bs.modal', function () {
                var modal = $(this),
                    mapComponent = modal.find('.map-component');

                if (map == null || mapComponent.length === 0) {
                    return;
                }

                if (typeof map.invalidateSize === 'function') {
                    map.invalidateSize();
                }
                else {
                    console.warn("Leaflet prestal podporovat metodu invalidateSize, ktora bola pouzivana pre reload mapy v modali");
                }
            })
        }

        return {
            render: function(){
                addAssets();

                var self = this;

                // cakame na nacitanie JS
                if (typeof L == 'undefined') {
                    setTimeout(function(){
                        self.render();
                    });
                    return;
                }

                createMap();
                setView();
                repairOnModalOpen();
            }
        };
    });

    var MapTools = (function() {
        return {
            isNotEmpty: function(str) {
                return str != null && str != '';
            },
            isEmpty: function(str) {
                return !this.isNotEmpty(str);
            },
            addTo: function(el, link, urls) {
                if (el.data('map-assets-loaded') != null) {
                    return;
                }

                var append = "";
                $.each(urls, function (i, v) {
                    var result = link,
                        attrs = [];

                    $.each(v, function (key, value) {
                        attrs.push(key + "=" + value);
                    });

                    append += result.replace("{attrs}", attrs.join(" "));
                });
                el.data('map-assets-loaded', true).append(append);
            },
            getMarkerText: function(options) {
                var result = "";
                if(MapTools.isEmpty(options.place)) {
                    if (MapTools.isNotEmpty(options.label)) {
                        result += "<strong>" + options.label + "</strong><br/>";
                    }
                    if (options.labelAddress) {
                        result += "<iwcm:text key="components.map.latitude"/> " + options.latitude + "<br/><iwcm:text key="components.map.longitude"/> " + options.longitude;   
                    }
                }
                else {
                    if (options.label != "") {
                        result += "<strong>" + options.label + "</strong><br>";
                    }
                    if (MapTools.isNotEmpty(options.place) && options.labelAddress) {
                        result += options.place;
                    }
                }

                return result;
            }
        }
    })();
</script>
<%} %>
<style type="text/css">
   #map${mapCounter} {
        width: <%=width%>;
        height: <%= (sizeInPercent) ? "auto" : height%>;
        padding-bottom: <%= (sizeInPercent) ? height : "auto"%>;
        color: black;
    }
</style>
<div id="map${mapCounter}" class="details map-component"></div>
<% WriteTag.setInlineComponentEditTextKey("components.map.editMap", request); %>
<script type="text/javascript">
    $(function(){
        var options = ${optionsHtml};
        options.mapElement = $('#map${mapCounter}');
        var map = MapFactory.getMap(options);
        map.render();
    });
</script>