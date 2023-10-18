<%@ page contentType="text/html; charset=windows-1250"
import="sk.iway.iwcm.*"%><%
//stranka pre includnutie inej stranky (aj inej domeny)

PageParams pageParams = new PageParams(request);
String url = pageParams.getValue("url", null);

if (url.startsWith("http")==false)
{
	if (url.startsWith("/")==false)
	{
		url = "/" + url;
	}
	String protocol = "http";
	int port = Constants.getInt("httpServerPort");
	if (Tools.isSecure(request)) {
		protocol += "s";
		port = Constants.getInt("httpsServerPort");
	}
	url = protocol+"://" + Tools.getServerName(request) + ":" + port + url;
}

int cacheMinutes = pageParams.getIntValue("cacheMinutes", -1);

try
{
	Cache cache = Cache.getInstance();
	String data = cache.downloadUrl(url, cacheMinutes);

	if (data == null)
	{
		return;
	}
	out.println(data);
}
catch (Exception ex)
{
	Logger.error(ex);
}
%>