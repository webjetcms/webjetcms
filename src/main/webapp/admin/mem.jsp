<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<%@ page contentType="text/html" import="java.util.*,java.io.*,sk.iway.iwcm.*" %>
<%@page import="sk.iway.iwcm.stat.SessionHolder"%>
<%@page import="sk.iway.iwcm.system.dbpool.ConfigurableDataSource"%><%!



%><%

if (sk.iway.iwcm.components.monitoring.rest.MonitoringManager.isIpAllowed(Tools.getRemoteIP(request))==false)
{
	Logger.error(sk.iway.iwcm.components.monitoring.rest.MonitoringManager.class, "mem.jsp, forbidden for IP "+Tools.getRemoteIP(request));

	response.setStatus(403);
	pageContext.include("/403.jsp");
	return;
}

%>
<html>
<head>
<%if (Tools.getRequestParameter(request, "printTrace")==null && Tools.getRequestParameter(request, "gc")==null) { %>
<meta http-equiv='refresh' content='5; url=mem.jsp'>
<% } %>

<style>
body
{
   margin:2px;
   font-family: arial;
   font-size: 11px;
}
</style>
</head>
<body>
    <b><%= java.net.InetAddress.getLocalHost().getHostAddress() %></b>
    (secure: <%=Tools.isSecure(request) %>)
    <%= new java.util.Date() %><br>
    <%
        ConfigurableDataSource ds = null;
        try {
            ds = (ConfigurableDataSource) DBPool.getInstance().getDataSource("iwcm");
            out.println("DBPool total: " + ds.getNumTotal() + " active: <b>" + ds.getNumActive() + "</b> idle:" + ds.getNumIdle() + " waiting: " + ds.getNumWaiting() + " <br>");
        }
        catch (Exception ex) {}

       Runtime rt = Runtime.getRuntime();
       long free = rt.freeMemory();
       long total = rt.totalMemory();
       long used = total - free;
       long max = rt.maxMemory();
       int proces = rt.availableProcessors();
       int cacheSize = Cache.getInstance().getSize();
       int openSessions = SessionHolder.getTotalSessions();
       out.println("<br>");
       out.println("Free  = <b>"+(free/1024/1024) +"</b> MB ("+free +" bytes)<br>");
       out.println("Total = <b>"+(total/1024/1024)+"</b> MB ("+total+" bytes)<br>");
       out.println("Used = <b>"+(used/1024/1024)+"</b> MB ("+used+" bytes)<br>");
       out.println("Max  = <b>"+(max/1024/1024)+"</b> MB ("+max+" bytes)<br>");
       out.println("<br>");
       out.println("Processors = <b>"+proces+"</b><br>");
       out.println("Cache = <b>"+cacheSize+"</b><br>");
       out.println("Sessions = <b>"+openSessions+"</b><br>");
       out.println("Remote IP = <b>"+Tools.getRemoteIP(request)+"</b><br>");
       out.println("X-forwarded-for = <b>"+ sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getHeader("X-forwarded-for")) +"</b><br>");
       out.println("Node = <b>"+Constants.getString("clusterMyNodeName")+"</b><br>");

       Locale l = Locale.getDefault();
       out.println("country="+l.getCountry()+" lang="+l.getLanguage());

       if ("true".equals(Tools.getRequestParameter(request, "gc")))
       {
        System.gc();
        out.println("<br>AFTER GC:<br>");
        free = rt.freeMemory();
          total = rt.totalMemory();
          used = total - free;
          max = rt.maxMemory();
        out.println("Free  = <b>"+(free/1024/1024) +"</b> MB ("+free +" bytes)<br>");
          out.println("Total = <b>"+(total/1024/1024)+"</b> MB ("+total+" bytes)<br>");
          out.println("Used = <b>"+(used/1024/1024)+"</b> MB ("+used+" bytes)<br>");
          out.println("Max  = <b>"+(max/1024/1024)+"</b> MB ("+max+" bytes)<br>");
          out.println("<br>");
       }
       if ("true".equals(Tools.getRequestParameter(request, "finalize")) )
       {
        System.runFinalization();
        out.println("<br>AFTER Finalize:<br>");
        free = rt.freeMemory();
          total = rt.totalMemory();
          used = total - free;
          max = rt.maxMemory();
        out.println("Free  = <b>"+(free/1024/1024) +"</b> MB ("+free +" bytes)<br>");
          out.println("Total = <b>"+(total/1024/1024)+"</b> MB ("+total+" bytes)<br>");
          out.println("Used = <b>"+(used/1024/1024)+"</b> MB ("+used+" bytes)<br>");
          out.println("Max  = <b>"+(max/1024/1024)+"</b> MB ("+max+" bytes)<br>");
          out.println("<br>");
       }

    String contextFile = "";
    try
    {
       File f = new File("/proc/loadavg");
        if (f.exists() && f.canRead())
        {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f));
            char buff[] = new char[8000];
            int len;
            String line;
            while ((len = isr.read(buff))!=-1)
            {
                line = new String(buff, 0, len);
                contextFile += line;
            }
        }
        out.println("Loadavg = <b>"+Tools.escapeHtml(contextFile)+"</b><br>");

    }
    catch (Exception ex)
    {

    }

    if (ds != null) {
        System.out.println((new java.util.Date()) + " DBPool A=" + ds.getNumActive() + " I=" + ds.getNumIdle() + " Sessions=" + openSessions + " Cache=" + cacheSize + " MEM: F=" + (free / 1024 / 1024) + " T=" + (total / 1024 / 1024) + " U=" + (used / 1024 / 1024) + " M=" + (max / 1024 / 1024) + " l=" + contextFile);
    }

       Properties props = System.getProperties();
       out.println("<br>");
       out.println("java.runtime.name: "+props.getProperty("java.runtime.name")+"<br>");
       out.println("java.vm.version: "+props.getProperty("java.vm.version")+"<br>");
       out.println("java.vm.name: "+props.getProperty("java.vm.name")+"<br>");
       out.println("java.version: "+props.getProperty("java.version")+"<br>");
       out.println("java.vendor: "+props.getProperty("java.vendor")+"<br>");
       out.println("serverInfo: "+Constants.getServletContext().getServerInfo()+"<br>");
       out.println("<br>");
       out.println("os.name: "+props.getProperty("os.name")+"<br>");
       out.println("os.version: "+props.getProperty("os.version")+"<br>");
     %>
    <%@page import="java.lang.management.ManagementFactory"%>
    <%@page import="java.lang.management.MemoryPoolMXBean"%>
     <%


        List<MemoryPoolMXBean> memory = ManagementFactory.getMemoryPoolMXBeans();
        out.println("<br />-------------------------------MEMORY TYPES------------------------------------<br />");
        for (MemoryPoolMXBean memoryType : memory)
        {
            out.println("<p>");
            out.println(memoryType.getName());
            out.println("<br />");
            out.println(memoryType.getType());
            out.println("<br />");
            out.print("Used: ");
            out.println(memoryType.getUsage().getUsed() / 1000000L);
            out.println("M<br />");
            out.print("Maximum: ");
            out.println(memoryType.getUsage().getMax() / 1000000L);
            out.println("M<br />");

            out.println("Free: ");
            out.println(memoryType.getUsage().getMax() / 1000000L);
            out.println("M<br />");
            out.println("</p>");
       }
    //File f = new File(sk.iway.iwcm.Tools.getRealPath("/admin/mem.jsp"));
    //f.setLastModified(sk.iway.iwcm.Tools.getNow()+50000);
    %>
</body>
</html>
