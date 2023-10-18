<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page language="java" contentType="text/html; charset=UTF-8"    pageEncoding="UTF-8"%>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %><%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><iwcm:checkLogon admin="true" perms="cmp_blog"/><%
    int docid = Tools.getDocId(request);
    request.setAttribute("docDetails", DocDB.getInstance().getDoc(docid));
    int groupid = Tools.getIntValue(request.getParameter("groupid"),-1);
    request.setAttribute("pageGroupDetails", GroupsDB.getInstance().getGroup(groupid));
    
%><stripes:useActionBean alwaysExecuteEvent="true" beanclass="sk.iway.iwcm.components.blog.BlogAction" event="btnSave" var="actionBean" /><%=request.getAttribute("errorText") %>



