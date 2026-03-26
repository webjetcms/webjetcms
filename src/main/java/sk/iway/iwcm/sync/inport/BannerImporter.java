package sk.iway.iwcm.sync.inport;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.banner.BannerDB;
import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stripes.SyncDirWriterService;
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

	private static final String BANNER_PREFIX = "banner_";

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

	public static void importBanners(HttpServletRequest request, Content content, PrintWriter writer) {
		Prop prop = Prop.getInstance(request);
		//
		SyncDirWriterService.prepareProgress(prop.getText("components.syncDirAction.progress.syncingBanners"), "bammersImportCount", prop.getText("components.syncDirAction.progress.syncingBanner") + ": - / -", writer);

		if (null == content) return;

		Map<String, String> selectedBannersMap = SyncDirWriterService.getOptionsMap(BANNER_PREFIX, request);
		if(selectedBannersMap.size() < 1) return;

		int importedBannersCount = 1;
		Iterable<Numbered<BannerBean>> bannersToImport = Numbered.list(content.getBanners());
		int bannersToImportCount = SyncDirWriterService.getCountToHandle(selectedBannersMap, bannersToImport, BANNER_PREFIX);

		for (Numbered<BannerBean> banner : bannersToImport)
		{
			if (selectedBannersMap.get(BANNER_PREFIX + banner.number) != null)
			{
				SyncDirWriterService.updateProgress("bammersImportCount", prop.getText("components.syncDirAction.progress.syncingBanner") + ": " + importedBannersCount + " / " + bannersToImportCount, writer);
				importedBannersCount++;

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
