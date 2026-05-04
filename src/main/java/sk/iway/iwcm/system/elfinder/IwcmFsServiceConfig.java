package sk.iway.iwcm.system.elfinder;

import cn.bluejoe.elfinder.service.FsServiceConfig;
import sk.iway.iwcm.Constants;

public class IwcmFsServiceConfig implements FsServiceConfig
{
	private int _tmbWidth = Constants.getInt("imageThumbsWidth");

	public void setTmbWidth(int tmbWidth)
	{
		_tmbWidth = tmbWidth;
	}

	@Override
	public int getTmbWidth()
	{
		return _tmbWidth;
	}
}
