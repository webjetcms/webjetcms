<%@ page pageEncoding="utf-8"
%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

%>

<html>
	<head>
		<title>403</title>
		<style type="text/css">
			body {
				font-family: "Open Sans", sans-serif;
			}
		</style>
	</head>
	<body>
	<!--
	-- Unnnfortunately, Microsoft has added a clever new
	-- "feature" to Internet Explorer. If the text in
	-- an error's message is "too small", specifically
	-- less than 512 bytes, Internet Explorer returns
	-- its own error message. Yes, you can turn that
	-- off, but *surprise* it's pretty tricky to find
	-- buried as a switch called "smart error
	-- messages" That means, of course, that many of
	-- error messages are censored by default.
	-- And, of course, you'll be shocked to learn that
	-- IIS always returns error messages that are long
	-- enough to make Internet Explorer happy. The
	-- workaround is pretty simple: pad the error
	-- message with a big comment to push it over the
	-- five hundred and twelve byte minimum. Of course,
	-- that's exactly what you're reading right now....
	-->

	<center>
	   <br><br>
		 <iwcm:text key="components.permsDenied"/>
	</center>
	</body>
</html>