
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,org.apache.commons.codec.binary.Base64"%><%@ 
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@ 
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@ 
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@ 
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@ 
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@ 
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@ 
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);
	Prop prop = Prop.getInstance(lng);
	String encoded = pageParams.getValue("url", "");
	Base64 b64 = new Base64();
	String decoded = new String(b64.decode(encoded.getBytes()));
	System.out.println("decoded: " + decoded);
	String width = pageParams.getValue("width", "900");
	String height = pageParams.getValue("height", "700");
	
	if (decoded.indexOf("http")==-1) decoded = Tools.getBaseHref(request) + decoded;
%>

<iframe src="https://docs.google.com/viewer?url=<%=decoded%>&embedded=true" width="<%=width%>" height="<%=height%>" style="border: none;"></iframe>