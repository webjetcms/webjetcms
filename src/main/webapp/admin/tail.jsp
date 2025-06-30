<%@page import="java.util.List"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ page import="java.io.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@
		taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@page import="org.apache.commons.lang.time.DateUtils"%>
<%@page import="sk.iway.iwcm.PathFilter"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.tags.support.RequestUtils"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%><iwcm:checkLogon admin="true" perms="cmp_adminlog_logging"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>
<%@ include file="layout_top.jsp" %>

<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.FileTools"%>
<%@ page import="java.net.URI" %>
<%
	System.out.println(System.getProperty("catalina.base"));

	//String tomcatHomePath = new URI(Tools.replace(System.getProperty("catalina.base"), " ", "%20")).normalize().getPath();
	File tomcatHome = new File(System.getProperty("catalina.base")); //new File(tomcatHomePath);
	File logDir = new File(System.getProperty("catalina.base"),"logs");

	String filePath = "";
	if(Tools.isNotEmpty(Tools.getRequestParameter(request, "file"))) {
		filePath = new File(Tools.getRequestParameter(request, "file")).getAbsolutePath();
		if(!filePath.startsWith(logDir.getAbsolutePath())) {
			out.println("path error, logDir="+logDir.getAbsolutePath()+" filePath="+filePath);
			return;
		}
	}
	pageContext.setAttribute("filePath", filePath);
	pageContext.setAttribute("iframed", Tools.getBooleanValue(Tools.getRequestParameter(request, "iframed"), false));
%>

<form action="<%=PathFilter.getOrigPath(request)%>">
	<c:if test="${ empty filePath}">
		<p>
			<label>Clear ./work directory</label>
			<input type="submit"  name="clear_work" class="button100" value="Do">
		</p>
	</c:if>
	<c:if test="${not empty filePath}">

		<p>
			<input type="hidden" type="text" name="iframed" value="${iframed}"/>
			<label> <iwcm:text key="admin.tail.exp"/> <input type="text" name="exp" value="${param.exp}"/></label>
			<iwcm:text key="admin.tail.lines_before"/> <input type="text" name="linesBefore"  value="${param.linesBefore}"/>
			<iwcm:text key="admin.tail.lines_after"/> <input type="text" name="linesAfter" value="${param.linesAfter}" />
			<input type="submit"  name="grep" class="button100" value='<iwcm:text key="admin.tail.grep"/>'/>
			<input type="hidden" name="file" value="${filePath}" />
		</p>
	</c:if>
</form>
<c:if test="${not empty param.clear_work}">
	<%
		File work = new File(tomcatHome,"work");
		if (work.listFiles()!=null)
		{
			for (File f :work.listFiles())
			{
				boolean ok = f.delete();
				if (ok)
				{
					out.println("Failed to remove : "+Tools.escapeHtml(f.getAbsolutePath()));
				}
				else
				{
					out.println("Removed : "+Tools.escapeHtml(f.getAbsolutePath()));
				}
			}
		}
	%>
</c:if>
<c:if test="${empty filePath}">
	<h4><%=logDir.getAbsolutePath()%></h4>
	<ul>
		<%
			Date now = new Date();
			File[] files = logDir.listFiles();

			if (files!=null)
			{
				try
				{
					//usortuj to podla abecedy
					Arrays.sort(files,
							new Comparator<File>()
							{
								public int compare(File f1, File f2)
								{
									return (Long.valueOf(f1.lastModified()-f2.lastModified()).intValue());
								}
							});
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				for (File f:files)
				{
					//if (f.length()>0 && DateUtils.isSameDay(new Date(f.lastModified()),now))
					{
		%>
		<li> <a href="/admin/tail.jsp?file=<%=Tools.escapeHtml(f.getAbsolutePath())%>"><%=Tools.escapeHtml(f.getName() + " (" + FileTools.getFormatFileSize(f.length(),false ) + ", " + Tools.formatDateTimeSeconds(f.lastModified())) %>) </a></li>
		<%
					}
				}
			}
		%>
	</ul>
</c:if>
<%
	String exp = Tools.getRequestParameterUnsafe(request, "exp");
	if (Tools.getRequestParameter(request, "file")!=null)
	{
		out.println("<pre>");

		String file = Tools.getRequestParameter(request, "file");
		if (file.contains("/")==false && file.contains("\\")==false) file = logDir.getAbsolutePath()+File.separator+file;

		File f = new File(file);
		int linesBefore = Tools.getIntValue(Tools.getRequestParameter(request, "linesBefore"), 0);
		int linesAfter = Tools.getIntValue(Tools.getRequestParameter(request, "linesAfter"), 0);
		int freeTokens = 0;


		if (f.canRead())
		{
			Date d = new Date();
			f.setLastModified(d.getTime());
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			long totalSize = f.length();
			long size=500000;
			try
			{
				size = Long.parseLong(Tools.getRequestParameter(request, "size"));
			}
			catch (Exception ex)
			{

			}

			if (Tools.isEmpty(exp))
			{
				long skip = totalSize - size;
				if (skip>0)
				{
					br.skip(skip);
					br.readLine();
				}
			}

			String line;
			List<String> linesBeforeThis = new ArrayList<String>();


			while ((line=br.readLine())!=null)
			{
				if (Tools.isNotEmpty(exp))
				{
					if (Pattern.matches(".*"+exp.toLowerCase()+".*",line.toLowerCase()))
					{
						while(!linesBeforeThis.isEmpty())
						{
							out.println(Tools.escapeHtml(linesBeforeThis.get(0)));
							linesBeforeThis.remove(0);
						}
						out.println(Tools.escapeHtml(line).replaceAll("(?i)"+exp,"<b>"+exp+"</b>"));
						freeTokens = linesAfter;
					}
					else if (freeTokens > 0)
					{
						freeTokens--;
						out.println(Tools.escapeHtml(line));
					}
					if (freeTokens == 0){
						linesBeforeThis.add(line);
						if (linesBeforeThis.size() > linesBefore)
							linesBeforeThis.remove(0);
					}
				}
				else
				{
					out.println(Tools.escapeHtml(line));
				}
			}
			br.close();
			fr.close();
		}
		out.println("</pre>");
	}
%>

<%@ include file="layout_bottom.jsp" %>
