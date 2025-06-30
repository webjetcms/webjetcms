<%@page import="sk.iway.iwcm.system.cache.PersistentCacheDB"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%

//[#18339 - app livechat] vloženie aplikacie do stránky. Ak nieje zadaný kód, aplikacia sa nevloží

sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,
javax.xml.parsers.*,
org.w3c.dom.*,
java.io.*,
java.text.*,
java.util.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
PageParams pageParams = new PageParams(request);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if ("cz".equals(lng)) lng = "cs";

if (!pageParams.getValue("kluc", "").isEmpty()){
%>
	<!-- SmartSupp Live Chat script -->
	<script type="text/javascript">
	var _smartsupp = _smartsupp || {};
	_smartsupp.key = '<%=pageParams.getValue("kluc", "")%>';
	_smartsupp.language = '<%=lng%>';
	window.smartsupp||(function(d) {
		var s,c,o=smartsupp=function(){ o._.push(arguments)};o._=[];
		s=d.getElementsByTagName('script')[0];c=d.createElement('script');
		c.type='text/javascript';c.charset='utf-8';c.async=true;
		c.src='//www.smartsuppchat.com/loader.js?';
		s.parentNode.insertBefore(c,s);
	})(document);
	</script>
	<script>
	smartsupp('language','<%=lng%>');
	</script>
<%
}

%>