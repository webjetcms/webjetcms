package sk.iway.iwcm.components;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.servlet.jsp.JspWriter;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.fileupload2.core.FileItem;
import org.json.JSONArray;
import org.json.JSONObject;

import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;

/**
 *  Export.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 21.12.2015 11:50:22
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ImportExport
{
	static int bufferSize = 4 * 1024;

	protected ImportExport() {
		//utility class
	}

	public static void createJson(String archivePath, String archiveName, List<IwcmFile> files)
	{
		BufferedOutputStream output = null;
		try
		{
			JSONArray result = new JSONArray();

			for(IwcmFile f : files)
			{
				JSONObject item = new JSONObject();
				item.put("path",  f.getVirtualPath());
				item.put("modified",  f.lastModified());
			   item.put("size",   f.length());
			   result.put(item);
			}

			makeDirs(archivePath);

			output = new BufferedOutputStream(new IwcmOutputStream(new IwcmFile(PathFilter.getRealPath(archivePath), archiveName)));
			output.write(result.toString(3).getBytes(StandardCharsets.UTF_8));
			output.close();
			output = null;
		}
		catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
		finally					{closeOutputStream(output);}
	}

	public static void createZip(String archivePath, String archiveName, List<IwcmFile> files, JspWriter printWriter)
	{
		ZipArchiveOutputStream zipOut = null;
		int scrollIndex = 0;
		try
		{
			if(files!=null && files.size()>0)
			{
				makeDirs(archivePath);

				IwcmOutputStream output = new IwcmOutputStream(new IwcmFile(PathFilter.getRealPath(archivePath), archiveName));
				zipOut = new ZipArchiveOutputStream(output);

				for(IwcmFile file : files)
				{
					addToZipArchive(zipOut, file);
					if (printWriter != null)
					{
						printWriter.println("Adding file: " + file.getVirtualPath() + "<br>");
						if (scrollIndex >= 15)
						{
							printWriter.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
							scrollIndex = 0;
						}
						printWriter.flush();
						scrollIndex++;
					}
				}
				if (printWriter != null) {
					printWriter.println("<br>Počet súborov: "+files.size()+"<br>");
					printWriter.flush();
				}

				zipOut.close();
				zipOut = null;
			}
		}
		catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
		finally					{closeOutputStream(zipOut);}
	}

	public static void createTarGz(String archivePath, String archiveName, List<IwcmFile> files, JspWriter printWriter)
	{
		TarArchiveOutputStream tOut = null;
		int scrollIndex = 0;
		try
		{
			if(files!=null && files.size()>0)
			{
				makeDirs(archivePath);

				IwcmOutputStream output = new IwcmOutputStream(new IwcmFile(PathFilter.getRealPath(archivePath), archiveName));
				GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(output);
				tOut = new TarArchiveOutputStream(gzOut);

				for(IwcmFile file : files)
				{
					addToTarGzArchive(tOut, file);
					if (printWriter != null)
					{
						printWriter.println("Adding file: " + file.getVirtualPath() + "<br>");
						if (scrollIndex >= 15)
						{
							printWriter.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
							scrollIndex = 0;
						}
						printWriter.flush();
						scrollIndex++;
					}
				}
				if (printWriter != null) {
					printWriter.println("<br>Počet súborov: "+files.size()+"<br>");
					printWriter.flush();
				}

				tOut.close();
				tOut = null;
			}
		}
		catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
		finally 					{closeOutputStream(tOut);}
	}

	private static void addToZipArchive(ZipArchiveOutputStream zipOut, IwcmFile inFile)
	{
		IwcmInputStream in = null;
		try
		{
			ZipArchiveEntry entry = new ZipArchiveEntry(removeSlash(inFile.getVirtualPath()));
			entry.setSize(inFile.length());
			entry.setTime(inFile.lastModified());

			zipOut.putArchiveEntry(entry);
			in = new IwcmInputStream(inFile);
			int bytesRead = -1;
			byte[] buffer = new byte[bufferSize];
			while ((bytesRead = in.read(buffer)) != -1)
			{
				zipOut.write(buffer, 0, bytesRead);
			}
			in.close();
			in = null;

			zipOut.closeArchiveEntry();
		}
		catch (Exception ex)	{sk.iway.iwcm.Logger.error(ex);}
		finally					{closeInputStream(in);}
	}

	private static void addToTarGzArchive(TarArchiveOutputStream tOut, IwcmFile inFile)
	{
		IwcmInputStream in = null;
		try
		{
			TarArchiveEntry tarEntry = new TarArchiveEntry(removeSlash(inFile.getVirtualPath()));
			tarEntry.setSize(inFile.length());
			tarEntry.setModTime(inFile.lastModified());

		   tOut.putArchiveEntry(tarEntry);
			in = new IwcmInputStream(inFile);
			int bytesRead = -1;
			byte[] buffer = new byte[bufferSize];
			while ((bytesRead = in.read(buffer)) != -1)
			{
				tOut.write(buffer, 0, bytesRead);
			}
			in.close();

			tOut.closeArchiveEntry();
		}
		catch (Exception ex)	{sk.iway.iwcm.Logger.error(ex);}
		finally					{closeInputStream(in);}
	}

	//s lomitkom sa vytvoril adresar bez nazvu
	private static String removeSlash(String s)
	{
		if(s.startsWith("/"))
			s = s.substring(1);

		return s;
	}

	public static List<IwcmFile> getFiles(IwcmFile directory, Date fromDate, Map<String, JSONObject> snapshot, List<String> doNotExport)
	{
		List<IwcmFile> result = new ArrayList<>();

		if (directory.exists())
		{
			IwcmFile[] fileList = directory.listFiles();
			for (IwcmFile file : fileList)
			{
				boolean add = true;

				if(doNotExport!=null)
				{
					for(String s : doNotExport)
					{
						if(file.getVirtualPath().startsWith(s))
						{
							add = false;
							break;
						}
					}
				}

				if(fromDate!=null && file.lastModified()<fromDate.getTime())
					add = false;

				if(snapshot!=null && snapshot.get(file.getVirtualPath())!=null)
				{
					try
					{
						long modified = Long.parseLong(snapshot.get(file.getVirtualPath()).get("modified").toString());
						if(file.lastModified()<=modified)
							add = false;
					}
					catch(Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
				}

				if(add == false)
					continue;

				if(file.isDirectory())
					result.addAll(getFiles(file, fromDate, snapshot, doNotExport));
				else
					result.add(file);
			}
		}

		return result;
	}

	public static List<IwcmFile> importFromZip(FileItem archive, String tmpDir, JspWriter printWriter)
	{
		List<IwcmFile> importedFiles = new ArrayList<>();
		ZipArchiveInputStream zipIn = null;
		try
		{
			BufferedInputStream in = new BufferedInputStream(archive.getInputStream());
			zipIn = new ZipArchiveInputStream(in);

			ZipArchiveEntry entry = null;
			int scrollIndex = 0;
			int fileCount = 0;

			while ((entry = (ZipArchiveEntry) zipIn.getNextEntry()) != null)
			{
				BufferedOutputStream dest = null;
				try
				{
					String entryPath = tmpDir + "/" + entry.getName(); //NOSONAR
					makeDirs(entryPath.substring(0, entryPath.lastIndexOf('/')));

					IwcmOutputStream fos = new IwcmOutputStream(entryPath, true);
					dest = new BufferedOutputStream(fos);

					int bytesRead = -1;
					byte[] buffer = new byte[bufferSize];
					while ((bytesRead = zipIn.read(buffer)) != -1)
					{
						dest.write(buffer, 0, bytesRead);
					}
					dest.close();
					dest = null;

					if (printWriter != null)
					{
						printWriter.println("Extracting file: " + entry.getName() + "<br>");
						if (scrollIndex >= 15)
						{
							printWriter.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
							scrollIndex = 0;
						}
						printWriter.flush();
						scrollIndex++;
					}
					fileCount++;

					IwcmFile importedFile = IwcmFile.fromVirtualPath(entryPath);
					importedFile.setLastModified(entry.getLastModifiedDate().getTime());
					importedFiles.add(importedFile);
				}
				catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
				finally					{closeOutputStream(dest);}
			}
			if (printWriter != null) {
				printWriter.println("<br>Počet súborov: "+fileCount+"<br>");
				printWriter.flush();
			}

			zipIn.close();
			zipIn = null;
		}
		catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
		finally					{closeInputStream(zipIn);}
		return importedFiles;
	}

	public static List<IwcmFile> importFromTgz(FileItem archive, String tmpDir, JspWriter printWriter)
	{
		List<IwcmFile> importedFiles = new ArrayList<>();
		TarArchiveInputStream tarIn = null;
		try
		{
			BufferedInputStream in = new BufferedInputStream(archive.getInputStream());
			GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
			tarIn = new TarArchiveInputStream(gzIn);

			TarArchiveEntry entry = null;
			int scrollIndex = 0;
			int fileCount = 0;

			while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null)
			{
				BufferedOutputStream dest = null;
				try
				{
					String entryPath = tmpDir + "/" + entry.getName(); //NOSONAR
					makeDirs(entryPath.substring(0, entryPath.lastIndexOf('/')));

					IwcmOutputStream fos = new IwcmOutputStream(entryPath, true);
					dest = new BufferedOutputStream(fos);

					int bytesRead = -1;
					byte[] buffer = new byte[bufferSize];
					while ((bytesRead = tarIn.read(buffer)) != -1)
					{
						dest.write(buffer, 0, bytesRead);
					}
					dest.close();
					dest = null;

					if (printWriter != null)
					{
						printWriter.println("Extracting file: " + entry.getName() + "<br>");
						if (scrollIndex >= 15)
						{
							printWriter.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
							scrollIndex = 0;
						}
						printWriter.flush();
						scrollIndex++;
					}
					fileCount++;

					IwcmFile importedFile = IwcmFile.fromVirtualPath(entryPath);
					importedFile.setLastModified(entry.getLastModifiedDate().getTime());
					importedFiles.add(importedFile);
				}
				catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
				finally					{closeOutputStream(dest);}
			}
			if (printWriter != null) {
				printWriter.println("<br>Počet súborov: "+fileCount+"<br>");
				printWriter.flush();
			}

			tarIn.close();
			tarIn = null;
		}
		catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
		finally					{closeInputStream(tarIn);}
		return importedFiles;
	}

	private static void makeDirs(String path)
	{
		IwcmFile dir = IwcmFile.fromVirtualPath(path);
		if(!dir.exists())
			dir.mkdirs();
	}

	private static void closeInputStream(InputStream s)
	{
		if(s!=null)
		{
			try						{s.close();}
			catch (Exception ex)	{sk.iway.iwcm.Logger.error(ex);}
		}
	}

	private static void closeOutputStream(OutputStream s)
	{
		if(s!=null)
		{
			try						{s.close();}
			catch (Exception ex)	{sk.iway.iwcm.Logger.error(ex);}
		}
	}
}
