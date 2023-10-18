package sk.iway.iwcm.sync.export;

import java.util.List;

import sk.iway.iwcm.components.banner.BannerDB;
import sk.iway.iwcm.components.banner.model.BannerBean;

/**
 * Export udajov pre komponent "banner".
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.6.2012 21:58:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BannerExporter extends ComponentExporter
{

	public BannerExporter(String params)
	{
		super(params);
	}

	@Override
	public void export(ContentBuilder callback)
	{
		String bannerGroup = pageParams.getValue("group", null);
		List<BannerBean> banners = BannerDB.getBanners(bannerGroup, null);
		for (BannerBean banner : banners)
		{
			callback.addLink(banner.getBannerLocation());  // banner: obrazok alebo flash
			callback.addLink(banner.getBannerRedirect());  // na co smeruje (moze byt staticky kontent)
			callback.addHtml(banner.getHtmlCode());  // HTML bannery
			callback.addBanner(banner);
		}
	}

}
