<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><iwcm:checkLogon admin="true" perms="modUpdate"/><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
if ("has765h".equals(Tools.getRequestParameter(request, "hasty"))==false) return;
%>

<%@page import="sk.iway.iwcm.doc.DebugTimer"%>
<%@page import="sk.iway.iwcm.io.*"%>
<%@page import="java.io.*"%>
<%@page import="sk.iway.iwcm.doc.*"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.sql.*"%>
<%@page import="java.util.Collections"%>
<h1>Database speed test</h1>

<%!

public void prepareFileList(String baseDir, List<String> allUrls)
{
	IwcmFile dir = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(baseDir));
	if (dir.exists() == false || dir.isDirectory() == false) return;

	IwcmFile files[] = dir.listFiles();
	for (IwcmFile f : files)
	{
		if (f.isDirectory())
		{
			prepareFileList(baseDir+f.getName()+"/", allUrls);
		}
		else
		{
			allUrls.add(baseDir+f.getName());
		}
	}
}

public List<Integer> prepareDocIds()
{
	List<Integer> allDocIds = new ArrayList<Integer>();
	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
	   db_conn = DBPool.getConnection();
	   ps = db_conn.prepareStatement("SELECT doc_id FROM documents");
	   rs = ps.executeQuery();
	   while (rs.next())
	   {
			allDocIds.add(new Integer(rs.getInt("doc_id")));
	   }
	   rs.close();
	   ps.close();
		db_conn.close();
	   rs = null;
	   ps = null;
		db_conn = null;
	}
	catch (Exception ex)
	{
	   sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
	   try
	   {
	      if (rs!=null) rs.close();
	      if (ps!=null) ps.close();
	      if (db_conn!=null) db_conn.close();
	   }
	   catch (Exception ex2)
	   {

	   }
	}

	return allDocIds;
}

public String getDocData(int docId)
{
	String data = "";
	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
	   db_conn = DBPool.getConnection();
	   ps = db_conn.prepareStatement("SELECT data FROM documents WHERE doc_id=?");
	   ps.setInt(1, docId);
	   rs = ps.executeQuery();
	   while (rs.next())
	   {
			data = rs.getString("data");
	   }
	   rs.close();
	   ps.close();
		db_conn.close();
	   rs = null;
	   ps = null;
		db_conn = null;
	}
	catch (Exception ex)
	{
	   sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
	   try
	   {
	      if (rs!=null) rs.close();
	      if (ps!=null) ps.close();
	      if (db_conn!=null) db_conn.close();
	   }
	   catch (Exception ex2)
	   {

	   }
	}

	return data;
}

%><%
List<String> allUrls = new ArrayList<String>();
prepareFileList("/images/", allUrls);
%>


<h2>Image read, count=<%=allUrls.size() %></h2>
<%
//test rychlosti databazy
DebugTimer dt = new DebugTimer("dbspeedtest");
long start = System.currentTimeMillis();

double totalBytesRead = 0;
int i = 0;
if (false)
{
	for (String fileUrl : allUrls)
	{
		IwcmFile f = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(fileUrl));
		if (f.exists() && f.canRead())
		{
			try
			{
				InputStream isr = new IwcmInputStream(f);
				byte buff[] = new byte[64000];
				int len;
				int total = 0;
				while ((len = isr.read(buff))!=-1)
				{
					total += len;
					totalBytesRead += len;
					out.println("i="+i+" reading: "+len+"b total: "+total+"b<br/>");
				}
				isr.close();
			} catch (Exception ex) {}
		}

		out.println("i="+i+" url="+fileUrl+" diff="+dt.getLastDiff()+"<br/>");
		i++;
	}
}

long end = System.currentTimeMillis();

long totalTime = end - start;
double perItemTime = (double)totalTime / (double)allUrls.size();
double bytesPerSecond = totalBytesRead / (double)totalTime * 1000;
out.println("<strong>Total time: "+(end-start)+" ms, per item: "+perItemTime+"</strong><br/>");
out.println("<strong>Total bytes: "+totalBytesRead+", per second: "+ bytesPerSecond +"</strong>");


List<Integer> allDocIds = prepareDocIds();
Collections.shuffle(allDocIds);
Collections.shuffle(allDocIds);
Collections.shuffle(allDocIds);
%>

<h2>Random web page read, count=<%=allDocIds.size() %></h2>
<%
totalBytesRead = 0;
DocDB docDB = DocDB.getInstance();
start = System.currentTimeMillis();
i = 0;
if (false)
{
	for (Integer docId : allDocIds)
	{
		DocDetails doc = docDB.getDoc(docId.intValue());
		out.println("i="+i+" data:"+doc.getData().length()+"b diff="+dt.getLastDiff()+"<br/>");
		totalBytesRead += doc.getData().length() + doc.getData().length();
		i++;
	}
}
end = System.currentTimeMillis();

totalTime = end - start;
perItemTime = (double)totalTime / (double)allDocIds.size();
bytesPerSecond = totalBytesRead / (double)totalTime * 1000;
out.println("<strong>Total time: "+totalTime+" ms, per item: "+perItemTime+"</strong><br/>");
out.println("<strong>Total bytes: "+totalBytesRead+", per second: "+ bytesPerSecond +"</strong>");
%>
<h2>Only documents.data web page read count=<%=allDocIds.size() %></h2>
<%

if (false)
{



	totalBytesRead = 0;
	start = System.currentTimeMillis();
	i = 0;
	for (Integer docId : allDocIds)
	{
		String data = getDocData(docId);
		out.println("i3="+i+" data:"+data.length()+"b diff="+dt.getLastDiff()+"<br/>");
		totalBytesRead += data.length();
		i++;
	}
	end = System.currentTimeMillis();

	totalTime = end - start;
	perItemTime = (double)totalTime / (double)allDocIds.size();
	bytesPerSecond = totalBytesRead / (double)totalTime * 1000;
	out.println("<strong>Total time: "+totalTime+" ms, per item: "+perItemTime+"</strong><br/>");
	out.println("<strong>Total bytes: "+totalBytesRead+", per second: "+ bytesPerSecond +"</strong>");
}

if (true)
{
	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
	   db_conn = DBPool.getConnection();
	   ps = db_conn.prepareStatement("SELECT "+DocDB.D_DOCUMENT_FIELDS+" FROM documents d WHERE doc_id=?");
	   totalBytesRead = 0;
		start = System.currentTimeMillis();
		i = 0;
		for (Integer docId : allDocIds)
		{
		   ps.setInt(1, docId);
		   rs = ps.executeQuery();
		   if (rs.next())
		   {
				//String data = rs.getString("data");

				DocDetails doc = DocDB.getDocDetails(rs, false, true);

				out.println("i4="+i+" docid="+docId+" data:"+doc.getData().length()+"b diff="+dt.getLastDiff()+"<br/>");
				totalBytesRead += doc.getData().length();
				i++;
		   }
		   rs.close();
		}
	   ps.close();
		db_conn.close();
	   rs = null;
	   ps = null;
		db_conn = null;
	}
	catch (Exception ex)
	{
	   sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
	   try
	   {
	      if (rs!=null) rs.close();
	      if (ps!=null) ps.close();
	      if (db_conn!=null) db_conn.close();
	   }
	   catch (Exception ex2)
	   {

	   }
	}
	end = System.currentTimeMillis();

	totalTime = end - start;
	perItemTime = (double)totalTime / (double)allDocIds.size();
	bytesPerSecond = totalBytesRead / (double)totalTime * 1000;
	out.println("<strong>Total time: "+totalTime+" ms, per item: "+perItemTime+"</strong><br/>");
	out.println("<strong>Total bytes: "+totalBytesRead+", per second: "+ bytesPerSecond +"</strong>");
}



%>
