<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>

<%
PageParams pageParams = new PageParams(request);
String groupId = pageParams.getValue("groupId", "1");
request.setAttribute("groupid", groupId);

DocTreeAction.doTree(request);
%>

<!-- zaciatok stromu stranok -->
<script type="text/javascript" src="/components/sitemap/jscripts/ua.js"></script>
<script type="text/javascript" src="/components/sitemap/jscripts/tree.js"></script>
<script type="text/javascript">
	<!--
   rootNodeName="<b><iway:request name="root_name"/></b>";
   var Tree = new Array;
   // nodeId | parentNodeId | nodeName | nodeUrl
   <iwcm:iterate id="group" name="tree_list" type="sk.iway.iwcm.doc.DocTreeDetails" indexId="index">
      Tree[<iwcm:strutsWrite name="index"/>] = "<jsp:getProperty name="group" property="id"/>|<jsp:getProperty name="group" property="parent"/>|<jsp:getProperty name="group" property="name"/>|<jsp:getProperty name="group" property="link"/>";
   </iwcm:iterate>

               createTree(Tree, 0, 4);
	-->
</script>
<!-- koniec stromu stranok -->