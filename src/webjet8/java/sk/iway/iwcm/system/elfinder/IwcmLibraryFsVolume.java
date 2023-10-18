package sk.iway.iwcm.system.elfinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.bluejoe.elfinder.service.FsItem;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;

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
public class IwcmLibraryFsVolume extends IwcmFsVolume
{
	public IwcmLibraryFsVolume(String name, String rootUrl)
	{
		super(name, rootUrl);
	}

	public IwcmLibraryFsVolume(String name, IwcmFile rootDir)
	{
		super(name, rootDir);
	}

	@Override
	public String getName(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);

		String virtualPath = fsiFile.getVirtualPath();
		String domainAlias = AdminTools.getDomainNameFileAliasAppend();

		if ( ("/images"+domainAlias+"/gallery").equals(virtualPath)) return Prop.getTxt("elfinder.images.gallery");
		if ( ("/images"+domainAlias).equals(virtualPath)) return Prop.getTxt("elfinder.images");
		if ( ("/files"+domainAlias).equals(virtualPath)) return Prop.getTxt("elfinder.files");
		if ( ("/images"+domainAlias+"/video").equals(virtualPath)) return Prop.getTxt("elfinder.images.video");
		if ( ("/shared"+domainAlias).equals(virtualPath)) return Prop.getTxt("elfinder.shared");

		return super.getName(fsi);
	}

	@Override
	public FsItem getParent(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);

		String domainAlias = AdminTools.getDomainNameFileAliasAppend();

		for (String path : getLibraryFolders())
		{
			//ak sa jedna o cestu s 2 lomitkami nafejkuj parent path, napr. /images/gallery nafejkuj ako /gallery
			if (path.lastIndexOf("/")>1 && fsiFile.getVirtualPath().equals(path))
			{
				//pre kniznice nafejkujeme ze sa jedna o iny parent
				if (Tools.isNotEmpty(domainAlias))
				{
					//posuvame sa az o 2 adresare lebo mame alias
					fsiFile = fsiFile.getParentFile().getParentFile();
				}
				else
				{
					//nemame domenovy alias, posuvame sa o jeden adresar
					fsiFile = fsiFile.getParentFile();
				}
				return fromFile(fsiFile.getParentFile());
			}
		}
		/*
		if (fsiFile.getVirtualPath().equals("/images/gallery") || fsiFile.getVirtualPath().equals("/images/video"))
		{
			//pre kniznice nafejkujeme ze sa jedna o iny parent
			fsiFile = fsiFile.getParentFile();
			return fromFile(fsiFile.getParentFile());
		}
		*/

		return super.getParent(fsi);
	}

	@Override
	public FsItem[] listChildren(FsItem fsi)
	{
		List<FsItem> list = new ArrayList<FsItem>();

		IwcmFile fsiFile = asFile(fsi);

		Logger.debug(IwcmLibraryFsVolume.class, "listChildrens, virtualPath="+fsiFile.getVirtualPath()+" realPath="+fsiFile.getAbsolutePath());

		if (fsiFile.getVirtualPath().equals("/"))
		{
			for (String path : getLibraryFolders())
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

	private String[] getLibraryFolders()
	{
		String folders = Constants.getString("elfinderLibraryFolders");
		//test existencie /shared adresara priamo v strukture WJ
		if (Constants.getBoolean("enableStaticFilesExternalDir")) {
			File shared = new File(Tools.getRealPath("/"), "shared");
			if (shared.exists()) folders = folders+",/shared";
		}

		String domainAlias = AdminTools.getDomainNameFileAliasAppend();
		if (Tools.isNotEmpty(domainAlias))
		{
			folders = Tools.replace(folders, "/images", "/images"+domainAlias);
			folders = Tools.replace(folders, "/files", "/files"+domainAlias);
		}

		Logger.debug(IwcmLibraryFsVolume.class, "getLibraryFolders, folders="+folders);

		String[] elfinderLibraryFolders = Tools.getTokens(folders, ",", true);

		return elfinderLibraryFolders;
	}
}
