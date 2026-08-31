package sk.iway.iwcm.system.elfinder;

import java.util.Locale;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivSupportMethodsService;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.users.UsersDB;

public class FsSecurityCheckForAll implements FsSecurityChecker
{
	boolean _locked = false;

	boolean _readable = true;

	boolean _writable = true;

	public boolean isLocked()
	{
		return _locked;
	}

	@Override
	public boolean isLocked(FsService fsService, FsItem fsi)
	{
		return _locked || isFileArchiveReadOnly(fsService, fsi);
	}

	public boolean isReadable()
	{
		return _readable;
	}

	@Override
	public boolean isReadable(FsService fsService, FsItem fsi)
	{
		return _readable;
	}

	public boolean isWritable()
	{
		return _writable;
	}

	@Override
	public boolean isWritable(FsService fsService, FsItem fsi)
	{
		if (isFileArchiveReadOnly(fsService, fsi)) return false;

		if (fsi.getVolume() instanceof IwcmActualPageFsVolume)
		{
			return ((IwcmActualPageFsVolume)fsi.getVolume()).isWritable(fsi);
		}
		//zistim ci ma user pravo na zapis do tohto adresara
		if (fsi instanceof IwcmFsItem)
		{
			if (SetCharacterEncodingFilter.getCurrentRequestBean()!=null&&SetCharacterEncodingFilter.getCurrentRequestBean().getUserId()>0)
			{
				Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
				if (user!=null)
				{
					String virtualPath = ((IwcmFsItem)fsi).getFile().getVirtualPath();
					if ("/".equals(virtualPath) && fsi.getVolume() instanceof IwcmLibraryFsVolume)
					{
						//pre library nie je mozne zapisovat do rootu
						return false;
					}
					
					return UsersDB.isFolderWritable(user.getWritableFolders(), virtualPath);
				}
			}
		}

		return _writable;
	}

	private boolean isFileArchiveReadOnly(FsService fsService, FsItem fsi)
	{
		if (!(fsService instanceof sk.iway.iwcm.system.elfinder.FsService iwcmFsService)) return false;

		int selectedType = iwcmFsService.getSelectedType();
		if (selectedType != sk.iway.iwcm.system.elfinder.FsService.TYPE_LINK &&
			selectedType != sk.iway.iwcm.system.elfinder.FsService.TYPE_IMAGES &&
			selectedType != sk.iway.iwcm.system.elfinder.FsService.TYPE_MULTIMEDIA &&
			selectedType != sk.iway.iwcm.system.elfinder.FsService.TYPE_VIDEOS)
		{
			return false;
		}

		if (fsi.getVolume() instanceof IwcmArchivFsVolume) return true;
		if (!(fsi instanceof IwcmFsItem iwcmFsItem)) return false;

		String archivePath = FileArchivSupportMethodsService.normalizePath(FileArchivatorKit.getArchivPath());
		String itemPath = FileArchivSupportMethodsService.normalizePath(iwcmFsItem.getFile().getVirtualPath());
		return Tools.isNotEmpty(archivePath) && Tools.isNotEmpty(itemPath) && normalizePathForComparison(itemPath).startsWith(normalizePathForComparison(archivePath));
	}

	private String normalizePathForComparison(String path)
	{
		return path.toLowerCase(Locale.ROOT).replaceAll("[. ]+/", "/");
	}

	public void setLocked(boolean locked)
	{
		_locked = locked;
	}

	public void setReadable(boolean readable)
	{
		_readable = readable;
	}

	public void setWritable(boolean writable)
	{
		_writable = writable;
	}

}
