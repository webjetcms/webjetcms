<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.stat.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>



 <%

 Identity user = UsersDB.getCurrentUser(request);
 Prop prop = Prop.getInstance(request);

 if (user == null || user.isAdmin()==false)
 {
 	out.print(prop.getText("error.userNotLogged"));
 	return;
 }

 int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);

//mesiac
Calendar c = Calendar.getInstance();
c.set(Calendar.HOUR, 0);
c.set(Calendar.MINUTE, 0);
c.set(Calendar.SECOND, 0);
c.set(Calendar.MILLISECOND, 0);
c.set(Calendar.DAY_OF_MONTH, 1);
Date start = c.getTime();

c.add(Calendar.MONTH, 1);
c.add(Calendar.MILLISECOND, -1);
Date end = c.getTime();


List<Column> statMonth = StatNewDB.getViewsForDoc(start, end, docId);
request.setAttribute("statMonth", statMonth);

//den

c = Calendar.getInstance();
c.set(Calendar.HOUR, 0);
c.set(Calendar.MINUTE, 0);
c.set(Calendar.SECOND, 0);
c.set(Calendar.MILLISECOND, 0);
start = c.getTime();

c.add(Calendar.DAY_OF_MONTH, 1);
c.add(Calendar.MILLISECOND, -1);
end = c.getTime();

List<Column> statDay = StatNewDB.getViewsForDoc(start, end, docId);
request.setAttribute("statDay", statDay);

int totalInMonth = 0;
int totalInDay = 0;





%>

<iwcm:iterate id="column" name="statMonth" type="sk.iway.iwcm.stat.Column" indexId="index">
<%
    try
    {
  	  totalInMonth += column.getIntColumn4();
    }
    catch (Exception ex)
    {
       sk.iway.iwcm.Logger.error(ex);
    }
 %>
</iwcm:iterate>
<iwcm:iterate id="column" name="statDay" type="sk.iway.iwcm.stat.Column" indexId="index">
<%
    try
    {
  	  totalInDay += column.getIntColumn4();
    }
    catch (Exception ex)
    {
       sk.iway.iwcm.Logger.error(ex);
    }
 %>
</iwcm:iterate>

<%
out.print("{ \"month\": \""+totalInMonth+"\",\"day\": \""+totalInDay+"\"}");


%>
