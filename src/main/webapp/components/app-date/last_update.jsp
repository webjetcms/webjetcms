<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>

<iwcm:text key="components.date.last_update"/>:

<%

PageParams pageParams = new PageParams(request);
DocDetails doc = (DocDetails)request.getAttribute("docDetails");
if (doc == null)
{
	doc = new DocDetails();
	doc.setDateCreated(Tools.getNow());
}

boolean date = pageParams.getBooleanValue("date", true);
boolean time = pageParams.getBooleanValue("time", true);


if (date)
{
	out.print(doc.getDateCreatedString());
}
if (time)
{
	if (date)
	{
		out.print(" ");
	}
	out.print(doc.getTimeCreatedString());
}

%>