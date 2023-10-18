package cn.bluejoe.elfinder.controller.executors;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.UrlRedirectDB;
import sk.iway.iwcm.system.elfinder.IwcmDocGroupFsVolume;
import sk.iway.iwcm.users.UsersDB;

public class PasteCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String[] targets = request.getParameterValues("targets[]");
		//String src = request.getParameter("src");
		String dst = request.getParameter("dst");
		boolean cut = "1".equals(request.getParameter("cut"));

		List<FsItemEx> added = new ArrayList<FsItemEx>();
		List<String> removed = new ArrayList<String>();

		//FsItemEx fsrc = super.findItem(fsService, src);
		FsItemEx fdst = super.findItem(fsService, dst);

		//skontrolujem prava na zapis do cieloveho adresara
		Prop prop = Prop.getInstance(request);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && UsersDB.isFolderWritable(user.getWritableFolders(), fdst.getPath()))
		{
			for (String target : targets)
			{
				FsItemEx ftgt = super.findItem(fsService, target);
				String name = ftgt.getName();
				FsItemEx newFile = new FsItemEx(fdst, name);



				//JEEFF: upravene pre podporu nasho DocGroup
				if (ftgt.getVolumeId().equals(IwcmDocGroupFsVolume.VOLUME_ID))
				{
					((IwcmDocGroupFsVolume)ftgt.getVolume()).createAndCopy(ftgt, fdst, cut);
				}
				else
				{
					super.createAndCopy(ftgt, newFile);
					if (cut)
					{
						if (UsersDB.isFolderWritable(user.getWritableFolders(), ftgt.getParent().getPath()))
						{
							//#20481 - po vystrihnuti/premenovani vytvori redirect
							if(isAllowedFolder(ftgt.getPath(), Constants.getString("elfinderRedirectFolders")))
								createRedirect(ftgt, ftgt.getPath(), newFile.getPath(), request);

							//ak chcem povodny vymazat, musim mat pravo na zapis aj do zdrojoveho
							ftgt.delete();
						}
						else
						{
							json.put("error", prop.getText("components.elfinder.commands.paste.cut.error", ftgt.getParent().getPath()));
						}
					}
				}
				added.add(newFile);

				if (cut)
				{
					//JEEFF: upravene, ma sa vratit zoznam hashov a nie objektov
					removed.add(ftgt.getHash());
				}
			}
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.paste.error", fdst.getPath()));

		}



		json.put("added", files2JsonArray(request, added));
		//JEEFF: upravene, ma sa vratit zoznam hashov a nie objektov
		json.put("removed", removed.toArray());
	}

	/**
	 * vrati true, ak path zacina na niektory allowed adresar(adresare oddelene ciarkou)
	 *
	 * @param path
	 * @param allowed
	 * @return
	 */
	static boolean isAllowedFolder(String path, String allowed)
	{
		if(Tools.isNotEmpty(allowed) && Tools.isNotEmpty(path))
		{
			//zero or more whitespace, a literal comma, zero or more whitespace
			String[] allowedFolders = allowed.split("\\s*,\\s*");
			for(String folder : allowedFolders)
			{
				if(path.startsWith(folder))
					return true;
			}
		}
		return false;
	}

	/**
	 * rekurzivne prejde vsetky subory zadaneho adresara a vytvori presmerovania na ich novu lokaciu
	 *
	 * @param file
	 * @param originalLink
	 * @param newLink
	 * @param request
	 */
	static void createRedirect(FsItemEx file, String originalLink, String newLink, HttpServletRequest request)
	{
		if(Tools.isNotEmpty(originalLink) && newLink!=null)
		{
			if(file.isFolder())
			{
				for(FsItemEx child : file.listChildren())
				{
					createRedirect(child, originalLink, newLink, request);
				}
			}
			else
			{
				try
				{
					//nahradime staru(original) cestu za novu
					UrlRedirectDB.addRedirect(file.getPath(), newLink+file.getPath().substring(originalLink.length()), DocDB.getDomain(request), 301);
				}
				catch(Exception e){sk.iway.iwcm.Logger.error(e);}
			}
		}
	}
}
