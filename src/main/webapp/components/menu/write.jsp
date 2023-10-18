<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*"%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%
//stranka pre vypisanie textu v requeste

PageParams pageParams = new PageParams(request);
String name = pageParams.getValue("name", "title");
%><iwcm:write name="<%=name%>"/>