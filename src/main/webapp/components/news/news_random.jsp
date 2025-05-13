<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
//stranka pre nahodny vypis jednej novinky
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
int groupId = pageParams.getIntValue("groupId", -1);

//ziskaj DocDB
DocDB docDB = DocDB.getInstance();
GroupsDB groupsDB = GroupsDB.getInstance();
GroupDetails group = groupsDB.getGroup(groupId);

List docs = docDB.getDocByGroup(groupId);
int size = docs.size();

DocDetails rndDoc = null;
try
{
	int failSafe = 0;
	while (failSafe++ < 100)
	{
		int rndValue = (int) ( Math.random() * (double)size);
   	rndDoc = (DocDetails)docs.get(rndValue);

   	//vyradime default stranku adresara (tam je pravdepodobne zoznam noviniek)
   	if (group != null && rndDoc.getDocId()==group.getDefaultDocId()) continue;

   	//vyradime tie ktore nemaju platne datumy
   	long now = Tools.getNow();
   	if (rndDoc.getPublishStart()>now) continue;
   	if (rndDoc.getPublishEnd()>0 && rndDoc.getPublishEnd()<now) continue;

   	break;
   }
}
catch (Exception ex)
{

}

if (rndDoc != null)
{
	request.setAttribute("rndDoc", rndDoc);
}
%>

<iwcm:present name="rndDoc">
   <a href="/showdoc.do?docid=<iwcm:beanWrite name="rndDoc" property="docId"/>"><iwcm:beanWrite name="rndDoc" property="title"/></a>
   <br />
   <iwcm:beanWrite name="rndDoc" property="perex"/>
</iwcm:present>