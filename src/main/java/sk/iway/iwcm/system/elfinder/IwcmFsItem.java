package sk.iway.iwcm.system.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  IwcmFsItem.java - file objekt pre elFinder (cn.bluejoe.elfinder)
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.2.2015 19:11:16
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwcmFsItem implements FsItem
{
	IwcmFile _file;

	FsVolume _volumn;

	public IwcmFsItem(IwcmFsVolume volumn, IwcmFile file)
	{
		super();
		_volumn = volumn;
		_file = file;
	}

	public IwcmFile getFile()
	{
		return _file;
	}

	public FsVolume getVolume()
	{
		return _volumn;
	}

	public void setFile(IwcmFile file)
	{
		_file = file;
	}

	public void setVolumn(FsVolume volumn)
	{
		_volumn = volumn;
	}

	@Override
	public String toString() {
		if (_file != null) {
			try {
				return "IwcmFsItem [_file=" + _file.getVirtualPath() + "]";
			} catch (Exception ex) {}
		}
		return "IwcmFsItem [_file=" + _file + "]";
	}
}