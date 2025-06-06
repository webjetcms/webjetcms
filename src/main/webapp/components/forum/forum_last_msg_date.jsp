<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,sk.iway.iwcm.*" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String type = pageParams.getValue("type", "iframe");

int docId = pageParams.getIntValue("docid", -1);
if (docId < 0)
{
	try
	{
		docId = Integer.parseInt(request.getParameter("docid"));
	}
	catch (Exception ex)
	{
		return;
	}
}

DocForumEntity fb = ForumDB.getLastMessage(docId);

if (fb != null)
{
	//vo fore existuje nejaky prispevok, vypis datum
	out.print(fb.getQuestionDateDisplayDate() + " " + fb.getQuestionDateDisplayTime());
}
else
{
	//vo fore este neexistuje ziadny prispevok
	%><iwcm:text key="components.forum.no_messages"/><%
}
%>