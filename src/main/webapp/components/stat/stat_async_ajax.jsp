<%@page import="sk.iway.iwcm.stat.StatDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

//zapisanie statistiky volanej z stat_async.jsp

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

//ochrana pred zahltenim statistiky
String userIP = Tools.getRemoteIP(request);
Cache c = Cache.getInstance();
String KEY = "stat-viewCount-"+userIP;
Integer count = (Integer)c.getObject(KEY);
if (count == null) count = Integer.valueOf(0);

count = Integer.valueOf(count.intValue()+1);
c.setObjectSeconds(KEY, count, 5*60, false);

int countLimit = Constants.getInt("spamProtectionHourlyLimit") * 2;
if (countLimit < 300) countLimit = 300;

long now = 0;

if (count.intValue()<countLimit)
{
	int docId = Tools.getIntValue(Tools.getRequestParameter(request, "d"), -1);
	int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "g"), -1);

	request.setAttribute("group_id", Integer.toString(groupId));

	StatDB.add(session, request, response, docId);

	now = Tools.getNow();
}
%>
//var statSaved = "<%=now%>";
