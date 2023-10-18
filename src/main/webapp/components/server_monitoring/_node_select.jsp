<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page import="sk.iway.iwcm.system.cluster.ClusterDB"%>
<c:if test="<%=ClusterDB.isServerRunningInClusterMode()%>">
	<select name="node">
		<optgroup label="<iwcm:text key="components.monitoring.this_node"/>">
			<option value="<%=Constants.getString("clusterMyNodeName") %>"><%=Constants.getString("clusterMyNodeName") %></option>
		</optgroup>
		<optgroup label="<iwcm:text key="components.monitoring.other_nodes"/>">
			<c:forEach items="<%=ClusterDB.getClusterNodeNamesExpandedAuto()%>" var="nodeName">
				<option value="${nodeName}"	<c:if test="${nodeName == param.node}">selected=""</c:if>>
				${nodeName}</option>
			</c:forEach>
		</optgroup>
	</select>
</c:if>