<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<html>
<head>
	<title>Forum</title>
	<style>
	body, p, td, th
	{
		font-family: Verdana, Arial, Helvetica, sans-serif;
	   font-size: 11px;
	}
	div.forumQuote {
		font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 11px; color: #444444; line-height: 125%;
		background-color: #FAFAFA; border: #D1D7DC; border-style: solid;
		border-left-width: 1px; border-top-width: 1px; border-right-width: 1px; border-bottom-width: 1px;
		padding-left: 3px;
	   padding-right: 3px;
	   margin-bottom: 10px;
	}
	p.forumQuoteUser { padding: 0px; margin: 0px; margin-top: 10px; font-weight: bold; }
	</style>
	<link rel="stylesheet" type="text/css" href="/css/page.css">
	<style>
	body { padding: 3px; }	
    body p { text-align: left;}
	</style>
</head>

<body id="bodyWysiwyg"><iwcm:text key="editor.loading_please_wait"/></body>

</html>
