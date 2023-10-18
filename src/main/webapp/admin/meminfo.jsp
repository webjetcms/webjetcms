<%@ page import="java.lang.management.*" %>
<%@ page import="java.util.*" %>
<html>
    <head><title>Memory</title></head>
<body>

<h1>Memory</h1>
<table border="1">
<tr><th width="100"><b>Name</b></th><th width="150">Type</th><th colspan="6">Usage</th></tr>
<tr>
        <td colspan="2"><b>Heap Memory summary</b></td>
        <td><b>Usage</b></td><td><%= String.valueOf( ManagementFactory.getMemoryMXBean().getHeapMemoryUsage() ).replace(")",")</td><td>")%></td>
</tr>
<tr>
        <td colspan="2"><b>Non-Heap Memory summary</b></td>
        <td><b>Usage</b></td><td><%= String.valueOf( ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage() ).replace(")",")</td><td>")%></td>
</tr>
<tr><th colspan="8"></th></tr>
<%
Iterator iter = ManagementFactory.getMemoryPoolMXBeans().iterator();
while (iter.hasNext()) {
        MemoryPoolMXBean item = (MemoryPoolMXBean) iter.next(); %>
<tr>
        <td rowspan="2"><b><%= item.getName() %></b></td>
        <td rowspan="2"><%= item.getType() %></td>
        <td><b>Usage</b></td><td><%= String.valueOf( item.getUsage() ).replace(")",")</td><td>") %></td>
</tr>
<tr><td><b>Peak</b></td><td><%= String.valueOf( item.getPeakUsage() ).replace(")",")</td><td>") %></td></tr>
<%}%>
</table>

</body></html>
