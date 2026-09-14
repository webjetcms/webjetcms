package cn.bluejoe.elfinder.controller.executors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipInputStream;
import sk.iway.iwcm.users.UsersDB;

/**
 *  ExtractCommandExecutor.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff suchy $
 *@version      $Revision: 1.3 $
 *@created      Date: Sep 22, 2015 12:34:02 PM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ExtractCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);
		Prop prop = Prop.getInstance(request);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && fsi.isWritable(fsi) && fsi.getParent().isWritable(fsi.getParent()) && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getPath()))
		{
			String zipFile = fsi.getPath();

			FsItemEx makedir = null;
			if ("1".equals(request.getParameter("makedir")))
			{
				// outputFolder += "/unzip-"+Tools.getNow();
				fsi = new FsItemEx(fsi.getParent(), "unzip-"+Tools.getNow()+"/"+fsi.getName());
				makedir = fsi.getParent();
			}

			String outputFolder = fsi.getParent().getPath();
			if (areAllExtractEntriesWritable(zipFile, fsi.getParent()) == false)
			{
				json.put("error", prop.getText("components.elfinder.commands.extract.error", zipFile));
				json.put("added", new Object[] {});
				return;
			}

			if (makedir != null) makedir.createFolder();

			List<String> skippedFiles = new ArrayList<>();
			List<FsItemEx> added = unZipFile(zipFile, outputFolder, fsi, skippedFiles);
			if (added == null)
			{
				json.put("error", prop.getText("components.elfinder.commands.extract.error", zipFile));
				json.put("added", new Object[] {});
				return;
			}
			if (makedir != null) added.add(0, makedir);

			json.put("added", files2JsonArray(request, added));
			if (skippedFiles.isEmpty() == false)
			{
				JSONArray warning = new JSONArray().put(prop.getText("components.elfinder.commands.extract.skipped"));
				for (String skippedFile : skippedFiles)
				{
					// Pass paths as values so elFinder does not interpret "$1" in file names as a placeholder.
					warning.put("$1").put(skippedFile);
				}
				json.put("warning", warning);
			}
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.extract.error", fsi.getPath()));
			json.put("added", new Object[] {});
		}


	}

	protected boolean areAllExtractEntriesWritable(String zipFile, FsItemEx outputFolder)
	{
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sk.iway.iwcm.Tools.getRealPath(zipFile))))
		{
			ZipEntry ze = zis.getNextEntry();
			while (ze != null)
			{
				String fileName = IwcmFsVolume.normalizeUnicode(ze.getName());
				FsItemEx destination = new FsItemEx(outputFolder, fileName);
				// ZIP names retain the trailing slash needed to distinguish directories from .class files.
				if (FileBrowserTools.hasForbiddenSymbol(fileName) == false && destination.getPath() != null)
				{
					if (isExtractDestinationWritable(destination) == false) return false;
				}
				ze = zis.getNextEntry();
			}
		}
		catch (IOException ex)
		{
			Logger.error(ex);
			return false;
		}

		return true;
	}

	private boolean isExtractDestinationWritable(FsItemEx destination) throws IOException
	{
		boolean writable = destination.isWritable(destination);
		if (writable == false)
		{
			Logger.debug(this.getClass(), "isExtractDestinationWritable, destination="+destination.getPath()+", writable="+writable);
		}
		return writable;
	}

	public static List<String> getAllowedTypes()
	{
		return Arrays.asList(
					"application/zip"
					);
	}

	protected List<FsItemEx> unZipFile(String zipFile, String outputFolder, FsItemEx fsi)
	{
		return unZipFile(zipFile, outputFolder, fsi, new ArrayList<>());
	}

	/**
	 * Extracts permitted entries and collects virtual destination paths of skipped entries.
	 *
	 * @param zipFile virtual path of the archive
	 * @param outputFolder virtual extraction directory
	 * @param fsi archive item whose parent is the extraction directory
	 * @param skippedFiles destination list for entries skipped because of forbidden paths
	 * @return added items, or {@code null} if a destination is not writable
	 */
	protected List<FsItemEx> unZipFile(String zipFile, String outputFolder, FsItemEx fsi, List<String> skippedFiles)
	{
		Logger.debug(this.getClass(), "unzipFile, outputFolder="+outputFolder);

		List<FsItemEx> added = new ArrayList<FsItemEx>();

		byte[] buffer = new byte[64000];
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sk.iway.iwcm.Tools.getRealPath(zipFile))))
		{
			IwcmFile folder = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(outputFolder));
			if(!folder.exists()){
				folder.mkdir();
			}
			ZipEntry ze = zis.getNextEntry();

			Set<String> allreadyAddedFolders = new HashSet<String>();

			while(ze != null)
			{
				String fileName = IwcmFsVolume.normalizeUnicode(ze.getName());
				Logger.debug(this.getClass(), "ZE fileName="+fileName);
				FsItemEx destination = new FsItemEx(fsi.getParent(), fileName);
				if (FileBrowserTools.hasForbiddenSymbol(fileName) || destination.getPath() == null)
				{
					skippedFiles.add(outputFolder + (outputFolder.endsWith("/") ? "" : "/") + fileName);
					Logger.debug(this.getClass(), "Skipping ZIP entry with forbidden path, zipFile="+zipFile+", fileName="+fileName);
					ze = zis.getNextEntry();
					continue;
				}
				if (isExtractDestinationWritable(destination) == false) return null;
				IwcmFile newFile = new IwcmFile(folder.getPath() + File.separator + fileName);

				if (newFile.getParentFile().exists()==false)
				{
					new IwcmFile(newFile.getParent()).mkdirs();
					if (fileName.indexOf("/")>1)
					{
						//je tam indexOf namiesto lastIndexOf lebo chceme tam pridat len root priecinky a nie tie posledne
						String folderName = fileName.substring(0, fileName.indexOf("/"));
						if (allreadyAddedFolders.contains(folderName)==false)
						{
							allreadyAddedFolders.add(folderName);
							FsItemEx addedFile = new FsItemEx(fsi.getParent(), folderName);
							added.add(addedFile);
						}
					}
				}

				if (ze.isDirectory())
				{
					newFile.mkdirs();
					FsItemEx addedFile = new FsItemEx(fsi.getParent(), fileName);
					added.add(addedFile);
				}
				else
				{
					IwcmOutputStream fos = new IwcmOutputStream(newFile);

					int len;
					while ((len = zis.read(buffer)) > 0)
					{
						fos.write(buffer, 0, len);
					}
					fos.close();
				}

				FsItemEx addedFile = new FsItemEx(fsi.getParent(), fileName);
				added.add(addedFile);

				ze = zis.getNextEntry();
			}
		}
		catch(IOException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return added;
	}
}
