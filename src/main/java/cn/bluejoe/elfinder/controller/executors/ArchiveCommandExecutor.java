package cn.bluejoe.elfinder.controller.executors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.system.elfinder.IwcmDocGroupFsVolume;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipOutputStream;
import sk.iway.iwcm.users.UsersDB;

/**
 *  ArchiveCommandExecutor.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff suchy $
 *@version      $Revision: 1.3 $
 *@created      Date: Sep 22, 2015 12:32:03 PM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ArchiveCommandExecutor extends AbstractJsonCommandExecutor
{
	public static List<String> getAllowedTypes()
	{
		return Arrays.asList(
					"application/zip"
					);
	}

	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		executeZip(fsService, request, servletContext, json);
	}

	/**
	 * Vykona zozipovanie zadanych suborov a vrati odkaz na dany ZIP, aby sa dalo pouzit aj pre ZildlCommandExecutor
	 * @param fsService
	 * @param request
	 * @param servletContext
	 * @param json
	 * @return
	 * @throws Exception
	 */
	public FsItemEx executeZip(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json) throws Exception {
		String[] targets = request.getParameterValues("targets[]");
		String name = request.getParameter("name");
		FsItemEx zipFilePath = null;

		if (Tools.isEmpty(name)) name = "archive-"+Tools.getNow();
		if (name.endsWith(".zip")) name = name.substring(0, name.lastIndexOf(".zip"));

		//String type = request.getParameter("type");
		List<FsItemEx> added = new ArrayList<FsItemEx>();

		if(targets.length > 0)
		{
			FsItemEx firstItem = super.findItem(fsService, targets[0]);

			Prop prop = Prop.getInstance(request);
			Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
			if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), firstItem.getParent().getPath()))
			{
				// zipovanie jedneho adresaru
				if(targets.length == 1)
				{
					FsItemEx fsi = firstItem;
					if(fsi.isFolder())
					{
						try {
							zipDirectory(fsi.getPath(), fsi.getParent().getPath() + "/" + name + ".zip", false);
							zipFilePath = new FsItemEx(fsi.getParent(), name + ".zip");
							added.add(zipFilePath);
						} catch (ZipException e) {
							if (e.getMessage() != null && e.getMessage().contains("ZIP file must have at least one entry"))
							{
								json.put("error", prop.getText("components.elfinder.commands.archive.error.empty"));
							}
							else
							{
								json.put("error", prop.getText("components.elfinder.commands.archive.error.exception", e.getLocalizedMessage()));
							}
						} catch (IOException e) {
							json.put("error", prop.getText("components.elfinder.commands.archive.error.exception", e.getLocalizedMessage()));
						}
					}
				}
				// zipovanie viacerych suborov/adresarov
				else
				{
					FsItemEx newTempDirName = createNewTempDir(fsService, firstItem.getParent());
					for(String filePathHash : targets)
					{
						FsItemEx fsi = super.findItem(fsService, filePathHash);
						copyFileToDir(fsService, fsi, newTempDirName);
					}
					zipDirectory(newTempDirName.getPath(), firstItem.getParent().getPath() + "/" + name + ".zip", true);
					removeTempDir(newTempDirName);
					zipFilePath = new FsItemEx(firstItem.getParent(), name + ".zip");
					added.add(zipFilePath);
				}
			}
			else
			{
				json.put("error", prop.getText("components.elfinder.commands.archive.error", firstItem.getParent().getPath()));

			}

		}

		// pridany novy zip
		json.put("added", files2JsonArray(request, added));

		return zipFilePath;
	}

	public FsItemEx newZipFileForResponse(FsItemEx file) throws IOException
	{
		FsItemEx newFile = new FsItemEx(file.getParent(), file.getName() + ".zip");
		newFile.createFile();

		return newFile;
	}

	protected FsItemEx createNewTempDir(FsService fsService, FsItemEx rootDir) throws IOException
	{
		String name = rootDir.getName() + "-tempfolder-" + String.valueOf(new Date().getTime());
		FsItemEx dir = new FsItemEx(rootDir, name);
		dir.createFolder();
		return dir;
	}

	protected void copyFileToDir(FsService fsService, FsItemEx file, FsItemEx dir) throws IOException
	{
		FsItemEx ftgt = file;

		//FsItemEx fsrc = ftgt.getParent();
		FsItemEx fdst = dir;


		String name = ftgt.getName();
		FsItemEx newFile = new FsItemEx(fdst, name);

		//JEEFF: upravene pre podporu nasho DocGroup
		if (ftgt.getVolumeId().equals(IwcmDocGroupFsVolume.VOLUME_ID))
		{
			((IwcmDocGroupFsVolume)ftgt.getVolume()).createAndCopy(ftgt, fdst, false);
		}
		else
		{
			super.createAndCopy(ftgt, newFile);
		}
	}

	protected void removeTempDir(FsItemEx dir) throws IOException
	{
		dir.delete();
	}

	protected void getFilesFromDirToZip(String rootDir, String rootDirPrefix, ZipOutputStream zipOut, boolean saveFullPath) throws IOException
	{
	   IwcmFile file;
	   if (rootDir.indexOf(":")==-1)
	   {
	      file = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(rootDir));
	   }
	   else
	   {
	      file = new IwcmFile(rootDir);
	   }
		int i;
		byte[] buf = new byte[64000];
		IwcmInputStream in;
		int len;

		System.out.println("Testing file: " + file.getAbsolutePath()+" isDir=" + file.isDirectory());

		String rootDir2 = rootDir.replace('/', File.separatorChar);
	   if (file.isDirectory())
	   {
	   	IwcmFile[] files = file.listFiles();
	   	int size = files.length;
			for (i=0; i<size; i++)
			{
				if (files[i].isDirectory())
				{
					System.out.println("Adding DIRECTORY: " + file.getName() + "<br>");
					String newRootDir = rootDir + "/" + files[i].getName();
				   getFilesFromDirToZip(newRootDir, rootDirPrefix, zipOut, saveFullPath);
				}
				else
				{
					try
					{
						System.out.println("Adding file: " + files[i].getName() + "<br>");

					   in = new IwcmInputStream(files[i].getAbsolutePath());
						//Add ZIP entry to output stream.
						ZipEntry entry;

						if (saveFullPath)
						{
							String entryPath = rootDir2 + java.io.File.separatorChar + files[i].getName();
							entryPath = entryPath.replace(rootDirPrefix, "");
							if (entryPath.indexOf(File.separatorChar) == 0)
							{
								entryPath = entryPath.substring(1);
							}
							entry = new ZipEntry(entryPath);
						}
						else
						{
							String entryPath = rootDir2.substring(
											rootDir2.lastIndexOf(File.separator) + 1,
											rootDir2.length()
										) +
										java.io.File.separatorChar +
										files[i].getName();
							entry = new ZipEntry(entryPath);
						}

						entry.setTime(files[i].lastModified());
						entry.setSize(files[i].length());
						zipOut.putNextEntry(entry);

		            // Transfer bytes from the file to the ZIP file
		            while ((len = in.read(buf)) > 0)
		            {
		            	zipOut.write(buf, 0, len);
		            }
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}
			}
	   }
	   else
	   {
		   in = new IwcmInputStream(file.getAbsolutePath());
			//Add ZIP entry to output stream.
			String entryPath = rootDir2 + java.io.File.separatorChar + file.getName();
			if (entryPath.indexOf(File.separatorChar)==0)
			{
				entryPath = entryPath.substring(1);
			}

			ZipEntry entry = new ZipEntry(entryPath);
			entry.setTime(file.lastModified());
			zipOut.putNextEntry(entry);

	      // Transfer bytes from the file to the ZIP file
	      while ((len = in.read(buf)) > 0)
	      {
	      	zipOut.write(buf, 0, len);
	      }
	   }
	}

	protected void zipDirectory(String dirName, String fileName, boolean onlyContentInDir) throws IOException
	{
		if (dirName != null && fileName != null)
		{
			String dir = dirName;
			if (dir.endsWith("/"))
			{
				dir =  dir.substring(0, dir.length()-1);
			}
			String[] path = dir.split("/");

			if (onlyContentInDir) path = Arrays.copyOf(path, path.length);
			else path = Arrays.copyOf(path, path.length-1);

			String pathString = Tools.join(path, "/").replace('/', File.separatorChar);
			ZipOutputStream zout = new ZipOutputStream(
						new IwcmOutputStream(
									sk.iway.iwcm.Tools.getRealPath(fileName)
									)
						);
		   getFilesFromDirToZip(dir, pathString, zout, true);
		   zout.close();
		}
	}
}
