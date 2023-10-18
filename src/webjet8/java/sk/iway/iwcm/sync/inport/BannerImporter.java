package sk.iway.iwcm.sync.inport;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.banner.BannerDB;
import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.sync.export.Content;

/**
 * Import bannerov.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.6.2012 11:08:12
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BannerImporter
{

	public static List<ContentBannerBean> getBanners(Content content)
	{
		List<ContentBannerBean> bannerBeans = new ArrayList<ContentBannerBean>();
		if (null == content) return bannerBeans;

		for (Numbered<BannerBean> remoteBanner : Numbered.list(content.getBanners()))
		{
			BannerBean localBanner = getLocalBanner(remoteBanner.item);
			bannerBeans.add(new ContentBannerBean(remoteBanner.number, remoteBanner.item, localBanner));
		}
		return bannerBeans;
	}

	public static void importBanners(HttpServletRequest request, Content content)
	{
		if (null == content) return;
		for (Numbered<BannerBean> banner : Numbered.list(content.getBanners()))
		{
			if (null != request.getParameter("banner_" + banner.number) || request.getAttribute("syncAll")!=null)
			{
				createLocalContentBanner(banner.item);
			}
		}
	}

	/**
	 * Vrati banner zodpovedajuci importovanemu (rovnaka skupina a meno), alebo null.
	 * 
	 * @param remoteBanner
	 * @return
	 */
	private static BannerBean getLocalBanner(BannerBean remoteBanner)
	{
		List<BannerBean> localBanners = BannerDB.getBanners(remoteBanner.getBannerGroup(), null);  // rovnaka skupina
		for (BannerBean localBanner : localBanners)
		{
			if (Tools.areSame(remoteBanner.getName(), localBanner.getName()))  // rovnake meno
			{
				return localBanner;
			}
		}
		return null;
	}

	private static boolean createLocalContentBanner(BannerBean remoteBanner)
	{
		BannerBean localBanner = getLocalBanner(remoteBanner);
		if (null == localBanner)
		{
			remoteBanner.setBannerId(-1);
			return BannerDB.saveBanner(remoteBanner, -1);
		}
		int id = localBanner.getBannerId();
		remoteBanner.setBannerId(id);
		return BannerDB.saveBanner(remoteBanner, id);
	}

}
