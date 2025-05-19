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

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmOutputStream;
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
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fsi.getPath()))
		{
			String zipFile = fsi.getPath();

			FsItemEx makedir = null;
			if ("1".equals(request.getParameter("makedir")))
			{
				// outputFolder += "/unzip-"+Tools.getNow();
				fsi = new FsItemEx(fsi.getParent(), "unzip-"+Tools.getNow()+"/"+fsi.getName());
				fsi.getParent().createFolder();
				makedir = fsi.getParent();
			}

			String outputFolder = fsi.getParent().getPath().replace(".zip", "");

			List<FsItemEx> added = unZipFile(zipFile, outputFolder, fsi);
			if (makedir != null) added.add(0, makedir);

			json.put("added", files2JsonArray(request, added));
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.extract.error", fsi.getPath()));
			json.put("added", new Object[] {});
		}


	}

	public static List<String> getAllowedTypes()
	{
		return Arrays.asList(
					"application/zip"
					);
	}

	protected List<FsItemEx> unZipFile(String zipFile, String outputFolder, FsItemEx fsi)
	{
		Logger.debug(this.getClass(), "unzipFile, outputFolder="+outputFolder);

		List<FsItemEx> added = new ArrayList<FsItemEx>();

		byte[] buffer = new byte[64000];
		try
		{
			IwcmFile folder = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(outputFolder));
			if(!folder.exists()){
				folder.mkdir();
			}
			ZipInputStream zis = new ZipInputStream(new FileInputStream(sk.iway.iwcm.Tools.getRealPath(zipFile)));
			ZipEntry ze = zis.getNextEntry();

			Set<String> allreadyAddedFolders = new HashSet<String>();

			while(ze != null)
			{
				String fileName = ze.getName();
				Logger.debug(this.getClass(), "ZE fileName="+fileName);
				IwcmFile newFile = new IwcmFile(folder.getPath() + File.separator + fileName);

				if (newFile.getParentFile().exists()==false)
				{
					new IwcmFile(newFile.getParent()).mkdirs();
					if (ze.getName().indexOf("/")>1)
					{
						//je tam indexOf namiesto lastIndexOf lebo chceme tam pridat len root priecinky a nie tie posledne
						String folderName = ze.getName().substring(0, ze.getName().indexOf("/"));
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
					FsItemEx addedFile = new FsItemEx(fsi.getParent(), ze.getName());
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

				FsItemEx addedFile = new FsItemEx(fsi.getParent(), ze.getName());
				added.add(addedFile);

				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
		}
		catch(IOException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return added;
	}
}
