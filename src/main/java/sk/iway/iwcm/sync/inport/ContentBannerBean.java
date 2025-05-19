package sk.iway.iwcm.sync.inport;

import sk.iway.iwcm.components.banner.model.BannerBean;

/**
 * Zobrazenie bannera pri importe.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.6.2012 9:55:57
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContentBannerBean extends SimpleContentBean<BannerBean>
{

	public ContentBannerBean(int number, BannerBean remoteBanner, BannerBean localBanner)
	{
		super(number, remoteBanner, localBanner);
	}

	public String getGroup()
	{
		return remoteItem.getBannerGroup();
	}

	public String getName()
	{
		return remoteItem.getName();
	}

}
