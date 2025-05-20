<%@page import="sk.iway.iwcm.system.captcha.Captcha"%>
<%@page import="java.io.InputStreamReader"%><%@
page import="java.io.BufferedReader"%><%@
page import="java.io.DataOutputStream"%><%@
page import="javax.net.ssl.HttpsURLConnection"%><%@
page import="java.net.URL"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
if("reCaptcha".equals(Constants.getString("captchaType")) )
{
	session.setAttribute("sessionId", Tools.getParameter(request, "capchaId"));
}
if( Captcha.validateResponse(request, "", null))
	out.print("OK");
else
	out.print("ERR");
/*try
{
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);

	String url = "https://www.google.com/recaptcha/api/siteverify";
	URL obj = new URL(url);
	HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

	con.setRequestMethod("POST");
	con.setRequestProperty("User-Agent", null);
	con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

	String urlParameters = "secret="+Constants.getString("reCaptchaSecret")+"&response="+Tools.getParameter(request, "capchaId");

	con.setDoOutput(true);
	DataOutputStream wr = new DataOutputStream(con.getOutputStream());
	wr.writeBytes(urlParameters);
	wr.flush();
	wr.close();

	int responseCode = con.getResponseCode();
	if(responseCode != 200)
	{
		Logger.debug(null, "Sending 'POST' request to URL : " + url);
		Logger.debug(null, "Post parameters : " + urlParameters);
		Logger.debug(null, "Response Code : " + responseCode);
	}

	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	String inputLine;
	StringBuffer postResponse = new StringBuffer();

	while ((inputLine = in.readLine()) != null) {
		postResponse.append(inputLine);
	}
	in.close();

	if(postResponse.toString().indexOf("\"success\": true") != -1)
		out.print("OK");
	else if(postResponse.toString().indexOf("\"success\": false") != -1)
		out.print("ERR");
	else
	{
		out.print("Response Error");
		Logger.debug(null, postResponse.toString());
	}

}
catch(Exception ex)
{
	out.print("ReCaptcha Verify Error");
	sk.iway.iwcm.Logger.error(ex);
}*/
%>