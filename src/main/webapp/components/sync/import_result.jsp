<%@page import="sk.iway.iwcm.io.IwcmOutputStream"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.io.BufferedOutputStream"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.ImportExport"%>
<%@page import="java.nio.charset.Charset"%>
<%@page import="java.io.StringWriter"%>
<%@page import="org.apache.commons.fileupload2.core.FileItem"%>
<%@page import="java.util.Map"%>
<%@ page import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"%>

<%!
void createJson(String archivePath, String archiveName, List<String> files)
{
	BufferedOutputStream output = null;
	try
	{
		JSONArray result = new JSONArray();

		for(String s : files)
		{
			JSONObject item = new JSONObject();
			item.put("path",  s);
		    result.put(item);
		}

		IwcmFile dir = IwcmFile.fromVirtualPath(archivePath);
		if(!dir.exists())
			dir.mkdirs();

		output = new BufferedOutputStream(new IwcmOutputStream(new IwcmFile(PathFilter.getRealPath(archivePath), archiveName)));
		output.write(result.toString(3).getBytes(Charset.forName("UTF-8")));
		output.close();
		output = null;
	}
	catch(Exception e)		{sk.iway.iwcm.Logger.error(e);}
	finally
	{
		if(output!=null)
		{
			try						{output.close();}
			catch (Exception ex)	{sk.iway.iwcm.Logger.error(ex);}
		}
	}
}
%>

<%
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

Map<String,FileItem> archive = (Map<String,FileItem>)request.getAttribute("MultipartWrapper.files");
if(archive!=null)
{
	FileItem archiveFile = archive.get("archive");
	if (archiveFile != null && archiveFile.getSize()>0 && (archiveFile.getName().endsWith(".zip") || archiveFile.getName().endsWith(".tgz")))
	{
		List<IwcmFile> importedFiles = null;
		String tmpDir = Constants.getString("importArchivePath") + "/" + archiveFile.getName().replace('.', '_');
		FileTools.deleteDirTree(IwcmFile.fromVirtualPath(tmpDir));

		if(archiveFile.getName().endsWith(".zip"))
		{
			importedFiles = ImportExport.importFromZip(archiveFile, tmpDir, out);
		}
		else if(archiveFile.getName().endsWith(".tgz"))
		{
			importedFiles = ImportExport.importFromTgz(archiveFile, tmpDir, out);
		}

		IwcmFile importedTmp = IwcmFile.fromVirtualPath(tmpDir);
		if(importedFiles!=null && importedTmp!=null && importedTmp.exists())
		{
		%>

			<form action="<%=PathFilter.getOrigPath(request) %>" name="conflictFilesForm" method="post">
				<br>
				<table border="0" class="sort_table">
					<thead>
						<tr>
							<th>Súbor</th>
							<th>Existujúci</th>
							<th>Nový</th>
							<th></th>
							<th>Nahradiť</th>
						</tr>
					</thead>
					<tbody>
					<%for(IwcmFile file : importedFiles)
					{
						String existingFilePath = file.getVirtualPath().substring(tmpDir.length());

						//kontrola prav na adresare
						if(!UsersDB.isFolderWritable(user.getWritableFolders(), existingFilePath))
							continue;

						IwcmFile existingFile = IwcmFile.fromVirtualPath(existingFilePath);
						boolean exists = existingFile.exists();
					%>
						<tr>
							<td><%=existingFilePath%></td>
							<td><%if(exists){%><a target="_blank" href="<%=Tools.escapeHtml(existingFilePath)%>"><%=Tools.escapeHtml(Tools.formatDateTimeSeconds(existingFile.lastModified()))%></a><%}%></td>
							<td><a target="_blank" href="<%=Tools.escapeHtml(file.getVirtualPath())%>"><%=Tools.escapeHtml(Tools.formatDateTimeSeconds(file.lastModified()))%></a></td>
							<td><%if(exists){%><a target="_blank" href="/components/sync/file_compare.jsp?firstFile=<%=Tools.escapeHtml(existingFile.getVirtualPath())%>&secondFile=<%=Tools.escapeHtml(file.getVirtualPath())%>">Porovnaj</a><%}%></td>
							<td>
								<input type="checkbox" name="selectedFiles" value="<%=Tools.escapeHtml(existingFilePath)%>" <%if(!exists || existingFile.lastModified()<file.lastModified()){out.print("checked=\"checked\"");} %>>
							</td>
						</tr>
					<%} %>
					</tbody>
				</table>
				<br>
				<%if(Tools.getParameter(request, "backup")!=null)
				{%>
					<input type="hidden" name="backup" value="y">
				<%}%>
				<input type="hidden" name="tmpDir" value="<%=tmpDir%>">
				<input type="hidden" name="import" value="y">
				<input type="submit" class="button50" value="Importuj" />
			</form>

		<%
		}
	}
	else
		out.print("Zly archiv.");
}
else if(Tools.getParameter(request, "import")!=null)
{
	boolean makeBackup = false;
	if(!Tools.isEmpty(Tools.getParameter(request, "backup")))
		makeBackup = true;
	try
	{
		String rollbackDir = Constants.getString("rollbackArchivePath");
		String rollbackDate = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
		String rollbackPath = rollbackDir + "/" + rollbackDate;

		String[] selectedFiles = Tools.getRequestParameterValues(request, "selectedFiles");
		String tmpDir = Tools.getParameter(request, "tmpDir");
		if(selectedFiles!=null && selectedFiles.length>0 && tmpDir!=null)
		{
			List<String> nonExisting = new ArrayList<String>();
			int failed = 0;
			int failedRollback = 0;
			for(int i=0; i<selectedFiles.length;i++)
			{
				IwcmFile existing = IwcmFile.fromVirtualPath(selectedFiles[i]);

				//kontrola prav na adresare
				if(!UsersDB.isFolderWritable(user.getWritableFolders(), existing.getVirtualPath()))
				{
					out.print("<font color=\"red\">Failed to import: </font>"+ selectedFiles[i] + "<br>");
					continue;
				}

				IwcmFile imported = IwcmFile.fromVirtualPath(tmpDir+selectedFiles[i]);

				//zalohovanie starych suborov a vytvorenie listu suborov, ktore pred importom neexistovali
				if(makeBackup)
				{
					IwcmFile rollback = IwcmFile.fromVirtualPath(rollbackPath+selectedFiles[i]);
					if(existing.exists())
					{
						try 				{FileTools.copyFile(existing, rollback);}
						catch(Exception e)	{sk.iway.iwcm.Logger.error(e); failedRollback++;}
					}
					else
						nonExisting.add(selectedFiles[i]);
				}

				//samotny import - vytvaranie/prepisovanie suborov
				try
				{
					FileTools.copyFile(imported, existing);
					out.print("Successfully imported: "+ selectedFiles[i] +"<br>");
				}
				catch(Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
					failed ++;
					out.print("<font color=\"red\">Failed to import: </font>"+ selectedFiles[i] + "<br>");
				}

				if(i%15==0)
					out.print("<script language='javascript'>window.scrollBy(0,1000);</script>");
				out.flush();
			}

			//vytvorenie jsonu so subormi ktore pred importom neexistovali - pri rollbacku ich bude treba zmazat
			//podla adresarov sa zobrazuje ponuka moznych rollback-ov (takze ho vytvorime, aj ked ostane prazdny)
			if(makeBackup)
			{
				IwcmFile dir = IwcmFile.fromVirtualPath(rollbackPath);
				if(!dir.exists())
					dir.mkdirs();
				createJson(rollbackDir, rollbackDate+".json", nonExisting);
				if(failedRollback == 0)
					out.print("<br>Všetky súbory úspešne zálohované.<br>");
				else
					out.print("<br>Niektoré súbory sa nepodarilo zálohovať. Počet nezálohovaných súborov: "+ failedRollback +"<br>");
			}

			if(failed==0)
				out.print("<br>Všetky súbory úspešne importované. Počet súborov: " + selectedFiles.length + "<br>");
			else
				out.print("<br>Niektoré súbory sa nepodarilo importovť. Počet neiportovaných súborov: "+ failed +"<br>");
		}
		else
			out.print("Žiadne súbory na import.<br>");

		if(FileTools.deleteDirTree(IwcmFile.fromVirtualPath(tmpDir)))
			out.print("Dočasné súbory vymazané.");
		else
			out.print("Dočasné súbory sa nepodarilo vymazať.");
		out.print("<script language='javascript'>window.scrollBy(0,1000);</script>");
	}
	catch(Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
	}
}%>

