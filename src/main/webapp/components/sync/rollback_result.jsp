<%@page import="sk.iway.iwcm.FileTools"%>
<%@page import="org.json.JSONArray"%>
<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<%@page import="java.nio.charset.Charset"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="java.io.StringWriter"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.ImportExport"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.Tools"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"%>

<%!
void deleteBackup(IwcmFile json,IwcmFile dir, JspWriter out)
{
	try
	{
		json.delete();
		FileTools.deleteDirTree(dir);
		out.print("<br>Rollback skončil, vymazanie zálohy prebehlo úspešne.");
	}
	catch(Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
		try{out.print("<br>Rollback skončil, vymazanie zálohy sa nepodarilo.");}
		catch(Exception ex){}
	}
}
%>

<%
String path = Tools.getParameter(request, "rollback");
if(Tools.isEmpty(path))
{
	out.print("Rollback verzia nebola zvolená.");
	return;
}

IwcmFile dir = IwcmFile.fromVirtualPath(path);
IwcmFile json = IwcmFile.fromVirtualPath(path + ".json");

List<IwcmFile> filesToRollback = new ArrayList<IwcmFile>();
List<IwcmFile> filesToDelete = new ArrayList<IwcmFile>();

if(dir.exists())
{
	filesToRollback = ImportExport.getFiles(dir, null, null, null);
}
if(json.exists())
{
	StringWriter writer = new StringWriter();
	IwcmInputStream in = new IwcmInputStream(json);
	IOUtils.copy(in, writer, Charset.forName("UTF-8"));
	if(in!=null)
		in.close();
	JSONArray snapshot = new JSONArray(writer.toString());

	for(int i=0; i<snapshot.length(); i++)
	{
		filesToDelete.add(IwcmFile.fromVirtualPath(snapshot.getJSONObject(i).get("path").toString()));
	}
}

if(filesToRollback.size()<1 && filesToDelete.size()<1)
{
	out.print("Žiadne súbory pre rollback.");
	deleteBackup(json, dir, out);
	return;
}

int failedToRollback = 0;
int failedToDelete = 0;

int scrollIndex = 0;
for(IwcmFile f : filesToRollback)
{
	IwcmFile dest = IwcmFile.fromVirtualPath(f.getVirtualPath().substring(path.length()));
	try
	{
		FileTools.copyFile(f, dest);
		out.print("Successfully rolled back: " + dest.getVirtualPath() + "<br>");
	}
	catch(Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
		out.print("<font color=\"red\">Failed to roll back: </font>" + dest.getVirtualPath() + "<br>");
		failedToRollback++;
	}

	if (scrollIndex >= 15)
	{
		out.print("<script language='javascript'>window.scrollBy(0,1000);</script>");
		scrollIndex = 0;
	}
	out.flush();
	scrollIndex++;
}

for(IwcmFile f : filesToDelete)
{
	try
	{
		IwcmFile fParent = IwcmFile.fromVirtualPath(f.getVirtualParent());
		f.delete();
		if(fParent.listFiles().length==0)
			fParent.delete();
		out.print("Successfully deleted: " + f.getVirtualPath() + "<br>");
	}
	catch(Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
		out.print("<font color=\"red\">Failed to delete file or parent dir: </font>" + f.getVirtualPath() + "<br>");
		failedToDelete++;
	}

	if (scrollIndex >= 15)
	{
		out.print("<script language='javascript'>window.scrollBy(0,1000);</script>");
		scrollIndex = 0;
	}
	out.flush();
	scrollIndex++;
}

if(filesToRollback.size()>0)
{
	if(failedToRollback==0)
		out.print("<br>Nahradenie súborov prebehlo bez problémov. Počet súborov: " + filesToRollback.size() + "<br>");
	else
		out.print("<br>Niektoré súbory sa nepodarilo nahradiť. Počet nenahradených súborov: "+ failedToRollback +"<br>");
}

if(filesToDelete.size()>0)
{
	if(failedToDelete==0)
		out.print("<br>Vymazanie súborov prebehlo bez problémov. Počet súborov: " + filesToDelete.size() + "<br>");
	else
		out.print("<br>Niektoré súbory sa nepodarilo vymazať. Počet nevymazanných súborov: "+ failedToDelete +"<br>");
}

deleteBackup(json, dir, out);

out.print("<script language='javascript'>window.scrollBy(0,1000);</script>");
%>