<%@page import="sk.iway.iwcm.io.IwcmOutputStream"%>
<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="java.util.regex.Matcher"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.io.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Kontrola STRICT_QUOTES_ESCAPING suborov, call with URL parameter ?act=fix, write=true to save files</h1>
<p><a href="?act=fix">Spustiť</a></p>

<p>
vid. http://stackoverflow.com/questions/14743805/dorg-apache-jasper-compiler-parser-strict-quote-escaping-in-websphere-as
</p>

<%!

public static void checkFile(String fileName, JspWriter out, boolean write) throws Exception
{
	String percent = "%";

   Pattern pattern = Pattern.compile("([A-Za-z]+\\w?\\s*=\\s*(\")<%=\\s*[^"+percent+">]*\"+[^>]*\\s*"+percent+">(\")\\s*)");

   // Pass the input JSP in the first argument
   IwcmInputStream is = new IwcmInputStream(Tools.getRealPath(fileName));
   InputStreamReader isr = new InputStreamReader(is, "windows-1250");
   LineNumberReader lnr = new LineNumberReader(isr);

   String line = null;
   int lineCounter = 0;
   boolean found = false;

   StringBuilder sb = new StringBuilder();

   while ((line = lnr.readLine()) != null)
   {
   	 lineCounter++;
   	 if (lineCounter>1) sb.append("\r\n");

       Matcher matcher = pattern.matcher(line);
       while (matcher.find())
       {
      	 if (found == false) out.println("<br/>");
      	 found = true;

      	 int n = matcher.groupCount();
           for (int i = 2; i <= n; i++)
           {
               line = line.substring(0, matcher.start(i)) + "'" + line.substring(matcher.end(i));
           }
           out.println("<br/>"+Tools.escapeHtml(fileName)+": "+lineCounter);
           out.println("<br/>"+ResponseUtils.filter(line));
       }

       sb.append(line);
   }
   lnr.close();
   isr.close();
   is.close();

   if (found && write)
   {
   	File outFile = new File(Tools.getRealPath("/admin/aaa-strict-quotes"+fileName));
   	if (outFile.getParentFile().exists()==false) outFile.getParentFile().mkdirs();

   	IwcmOutputStream ios = new IwcmOutputStream(outFile);
		OutputStreamWriter osw = new OutputStreamWriter( ios, "windows-1250");
		osw.write(sb.toString());
		osw.close();
		ios.close();
   }
}

public void checkFolder(String url, JspWriter out, boolean write) throws Exception
{
	File dir = new File(Tools.getRealPath(url));
	File files[] = dir.listFiles();

	for (File f : files)
	{
		if (f.isDirectory())
		{
			checkFolder(url+f.getName()+"/", out, write);
		}
		else
		{
			checkFile(url+f.getName(), out, write);
		}
	}
}

%>

<%
if ("fix".equals(request.getParameter("act"))) {
    boolean write = "true".equals(Tools.getRequestParameter(request, "write"));

    checkFolder("/admin/", out, write);
    checkFolder("/components/", out, write);
}
%>


<%@ include file="/admin/layout_bottom.jsp" %>
