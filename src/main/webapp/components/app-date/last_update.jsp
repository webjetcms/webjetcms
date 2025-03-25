<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.Prop"%><%

Prop prop = Prop.getInstance();
PageParams pageParams = new PageParams(request);
DocDetails doc = (DocDetails)request.getAttribute("docDetails");
if (doc == null)
{
	doc = new DocDetails();
	doc.setDateCreated(Tools.getNow());
}

boolean aktualizovane = pageParams.getBooleanValue("aktualizovane", true);
boolean date = pageParams.getBooleanValue("datum", pageParams.getBooleanValue("date", true));
boolean time = pageParams.getBooleanValue("cas", pageParams.getBooleanValue("time", true));
boolean globalDateCreated = pageParams.getBooleanValue("globalDateCreated", false);
int rootGroupId = pageParams.getIntValue("rootGroupId", 1);
int cacheInMinutes = pageParams.getIntValue("cacheInMinutes", 1);

if (globalDateCreated)
{
	Cache cache = Cache.getInstance();
	Object obj = cache.getObject("web_last_update");
	if (obj == null)
	{
		cache.setObject("web_last_update", DocDB.getDateTimeCreatedString(rootGroupId, time), cacheInMinutes);
		obj = cache.getObject("web_last_update");
	}
	out.print(obj);
}
else
{
	if (aktualizovane)
	{
		out.print(prop.getText("components.date.aktualizovane").concat(" "));
	}
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

}

%>