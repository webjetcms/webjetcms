<%@page import="sk.iway.iwcm.users.*"%><%@page import="sk.iway.iwcm.io.IwcmInputStream"%><%@page
 import="org.apache.xmlbeans.impl.common.IOUtil"%><%@page
 import="sk.iway.iwcm.stat.heat_map.NoRecordException"%><%@page
 import="sk.iway.iwcm.stat.heat_map.HeatMapDB"%><%@page
 import="sk.iway.iwcm.io.IwcmFile"%><%@page
 import="java.util.Date"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
response.setContentType("image/png");
Identity user = UsersDB.getCurrentUser(request);
if (Tools.getRequestParameter(request, "document_id") != null && user != null && user.isAdmin())
{
	IwcmFile image = HeatMapDB.generateHeatMap(request);
	IOUtil.copyCompletely(new IwcmInputStream(image), response.getOutputStream());
}
%>
