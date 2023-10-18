<%@page import="java.io.InputStream"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.nio.charset.Charset"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="java.io.StringWriter"%>
<%@page import="java.io.BufferedInputStream"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.Date"%>
<%@page import="sk.iway.iwcm.components.ImportExport"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.*" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"%>

<%
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

String path = Tools.getParameter(request, "path");
String filter = Tools.getParameter(request, "filter");
String format = Tools.getParameter(request, "format");
String nodeType = Tools.getParameter(request, "nodeType");

String[] selectedDirs = Tools.getRequestParameterValues(request, "selectedDir");
String manualPath = Tools.getParameter(request, "manualPath");
String fromDateString = Tools.getParameter(request, "fromDate");

String emailAddress = null;
boolean sendEmail = false;
if(Tools.getRequestParameter(request, "sendEmail")!=null)
{
	sendEmail = true;
	emailAddress = Tools.getParameter(request, "emailAddress");
}

if(path==null || filter==null || format==null || nodeType==null)
	return;

List<String> filePaths = new ArrayList<String>();
if(path.equals("standard"))
	filePaths = Arrays.asList(Tools.replace(Constants.getString("exportStandardDirs"), "INSTALL_NAME", Constants.getInstallName()).split("\\?"));
else if(path.equals("selected") && selectedDirs!=null && selectedDirs.length>0)
	filePaths = Arrays.asList(selectedDirs);
else if(path.equals("all"))
	filePaths.add("/");
else if(path.equals("manual") && Tools.isNotEmpty(manualPath))
	filePaths = Arrays.asList(manualPath.split("\\s*,\\s*"));

if(filePaths.size()<1)
	return;

Date fromDate = null;
if(filter.equals("fromDate"))
{
	if(fromDateString != null)
		fromDate = Tools.getDateFromString(fromDateString.replace('.','-').replace('/','-'), "dd-MM-yyyy");
	if(fromDate == null)
		return;
}

Map<String, JSONObject> snapshotMap = null;
if(filter.equals("snapshot"))
{
	Map<String,FileItem> snapshotFiles = (Map<String,FileItem>)request.getAttribute("MultipartWrapper.files");
	if(snapshotFiles!=null)
	{
		FileItem snapshotFile = snapshotFiles.get("snapshot");
		if (snapshotFile != null && snapshotFile.getSize()>0)
		{
			StringWriter writer = new StringWriter();
			InputStream in = snapshotFile.getInputStream();
			IOUtils.copy(in, writer, Charset.forName("UTF-8"));
			if(in!=null)
				in.close();
			String jsonContent = writer.toString();
			JSONArray snapshot = new JSONArray(jsonContent);

			snapshotMap = new HashMap<String, JSONObject>();
			for(int i=0; i<snapshot.length(); i++)
			{
				snapshotMap.put(snapshot.getJSONObject(i).get("path").toString(), snapshot.getJSONObject(i));
			}
		}
	}
}

List<IwcmFile> files = new ArrayList<IwcmFile>();
List<String> doNotExport = null;

if(path.equals("all"))
	doNotExport = new ArrayList<String>(Arrays.asList(Constants.getString("doNotExport").split("\\?")));

for(String s : filePaths)
{
	IwcmFile file = IwcmFile.fromVirtualPath(s);
	//kontrola prav na adresare
	if(file.exists() && UsersDB.isFolderWritable(user.getWritableFolders(), file.getVirtualPath()))
	{
		if(file.isDirectory())
			files.addAll(ImportExport.getFiles(file, fromDate, snapshotMap, doNotExport));
		else if(file.isFile())
			files.add(file);
	}
}

String archivePath = Constants.getString("exportArchivePath");
String archiveName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());

if(files.size()>0)
{
	if(format.equals("zip"))
	{
		archiveName = archiveName+".zip";
		ImportExport.createZip(archivePath, archiveName, files, out);
	}
	else if(format.equals("tgz"))
	{
		archiveName = archiveName+".tgz";
		ImportExport.createTarGz(archivePath, archiveName, files, out);
	}
	else if(format.equals("snapshot"))
	{
		archiveName = archiveName+".json";
		ImportExport.createJson(archivePath, archiveName, files);
	}
}
else
	out.print("Ziadne subory<br>");

if(IwcmFile.fromVirtualPath(archivePath, archiveName).exists())
{
	out.print("Vytvoreny archív: <a href=\""+archivePath+"/"+archiveName+"\">"+archivePath+"/"+archiveName+"</a><br>");
	out.print("Veľkosť archívu: "+Math.round((double)(IwcmFile.fromVirtualPath(archivePath, archiveName).length())/1024.0/1024.0 * 100d) / 100d+" MB<br>");
	out.print("<script language='javascript'>window.scrollBy(0,1000);</script>");

	if(sendEmail)
	{
		if(Tools.isEmail(emailAddress))
		{
			if(SendMail.send(Tools.getBaseHref(request), emailAddress, emailAddress, null, null, "Vygenerovany nový archív", "Archív "+ archiveName +" v prílohe.", archivePath+"/"+archiveName+";"+archiveName))
				out.print("<br>Email úspešne odoslaný.");
			else
				out.print("<br>Email sa nepodarilo odoslať. Maximálna dovolená veľkosť príloh je: "+Constants.getString("maxSizeOfAttachments"));
		}
		else
			out.print("Neplatná emailová adresa.");
	}
}
else
	out.print("Archiv sa nevytvoril");
%>
