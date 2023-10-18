package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;
import java.util.List;

import cn.bluejoe.elfinder.service.FsItem;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/**
 *  IwcmLibraryFsVolume.java - volume objekt pre elFinder (cn.bluejoe.elfinder) pre vypis kniznic (obrazky, videa, galerie, subory)
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.2.2015 19:09:46
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwcmActualPageFsVolume extends IwcmFsVolume
{
	private String libraryFolders[];

	public IwcmActualPageFsVolume(String name, String rootUrl, String libraryFolders)
	{
		super(name, rootUrl);

		//fixni 2 lomitka v ceste co na linuxe zblbne
		libraryFolders = Tools.replace(libraryFolders, "//", "/");
		//Logger.debug(IwcmActualPageFsVolume.class, "rootUrl="+rootUrl+", libraryFolders="+libraryFolders);
		this.libraryFolders = Tools.getTokens(libraryFolders, ",", true);
	}

	@Override
	public String getName(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);

		String virtualPath = fsiFile.getVirtualPath();
		if (virtualPath.endsWith("/")==false) virtualPath += "/";
		for (String path : libraryFolders)
		{
			if (path.endsWith("/")==false) path += "/";
			if (path.equals(virtualPath))
			{
				if (virtualPath.startsWith("/images/gallery")) return Prop.getTxt("elfinder.images.gallery");
				if (virtualPath.startsWith("/images")) return Prop.getTxt("elfinder.images");
				if (virtualPath.startsWith("/files")) return Prop.getTxt("elfinder.files");
				if (virtualPath.startsWith("/images/video")) return Prop.getTxt("elfinder.images.video");
			}
		}

		return super.getName(fsi);
	}

	@Override
	public FsItem getParent(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);

		for (String path : libraryFolders)
		{
			//fixni 2 lomitka v ceste co na linuxe zblbne
			path = Tools.replace(path, "//", "/");
			//ak sa jedna o cestu s 2 lomitkami nafejkuj parent path, napr. /images/gallery nafejkuj ako /gallery
			if (path.lastIndexOf("/")>1 && fsiFile.getVirtualPath().equals(path))
			{
				//pre kniznice nafejkujeme ze sa jedna o iny parent
				//fsiFile = fsiFile.getParentFile();
				//return fromFile(fsiFile.getParentFile());

				return fromFile(_rootDir);
			}
		}

		return super.getParent(fsi);
	}

	@Override
	public FsItem[] listChildren(FsItem fsi)
	{
		List<FsItem> list = new ArrayList<FsItem>();

		IwcmFile fsiFile = asFile(fsi);

		Logger.debug(IwcmActualPageFsVolume.class, "listChildrens, virtualPath="+fsiFile.getVirtualPath()+" realPath="+fsiFile.getAbsolutePath());

		if (fsiFile.getVirtualPath().equals("/"))
		{
			for (String path : libraryFolders)
			{
				IwcmFile f = new IwcmFile(Tools.getRealPath(path));
				if (f.exists())
				{
					list.add(fromFile(f));
				}
			}

			return list.toArray(new FsItem[0]);
		}

		return super.listChildren(fsi);
	}

	public boolean isWritable(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);
		if (fsiFile.getVirtualPath().equals("/")) return false;

		if (SetCharacterEncodingFilter.getCurrentRequestBean()!=null&&SetCharacterEncodingFilter.getCurrentRequestBean().getUserId()>0)
		{
			Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
			if (user!=null)
			{
				return UsersDB.isFolderWritable(user.getWritableFolders(), ((IwcmFsItem)fsi).getFile().getVirtualPath());
			}
		}

		return true;
	}
}
