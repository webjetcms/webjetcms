package sk.iway.iwcm.system.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
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
		return _locked;
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