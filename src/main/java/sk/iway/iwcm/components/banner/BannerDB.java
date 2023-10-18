package sk.iway.iwcm.components.banner;

import static sk.iway.iwcm.components.banner.model.BannerStatViews.FIELD_INSERT_DATE;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.components.banner.model.BannerGroupBean;
import sk.iway.iwcm.components.banner.model.BannerWebDocBean;
import sk.iway.iwcm.components.banner.model.BannerWebGroupBean;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.StatGraphNewDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.StatWriteBuffer;
import sk.iway.iwcm.system.jpa.JpaComparator;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.SettingsAdminDB;


/**
 *  BannerDB.java - zobrazovanie bannerov, praca s tabulkou banner_banners
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.49 $
 *@created      $Date: 2010/01/20 10:12:27 $
 *@modified     $Date: 2010/01/20 10:12:27 $
 */
public class BannerDB
{
	private static Random random = new Random();

	private BannerDB() {
		//
	}

	/**
	 * Ziskanie zoznamu vsetky banerov (aj neaktivnych) z databazy
	 * @return
	 */
	public static List<BannerBean> getAllBanners()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ReadAllQuery dbQuery = new ReadAllQuery(BannerBean.class);

		ExpressionBuilder builder = new ExpressionBuilder();
		Expression expr = builder.get("domainId").equal(CloudToolsForCore.getDomainId());
		dbQuery.setSelectionCriteria(expr);

		Query query = em.createQuery(dbQuery);
		return JpaDB.getResultList(query);
	}

	/**
	 * Ziskanie banerov podla podmienok
	 * @param groups - zoznam skupin oddelenych ciarkou, alebo null
	 * @param orderBy - nazov JAVA PROPERTY (nie stlpca v DB), podla ktoreho sa robi order
	 * @return
	 */
	public static List<BannerBean> getBanners(String groups, String orderBy)
	{
		//ak mozeme, pouzi cache
		String cacheKey = "BannerDB.ban."+groups;
		Cache c = Cache.getInstance();
		int cacheTime = Constants.getInt("bannerCacheTime");
		if (cacheTime>0 && Tools.isNotEmpty(groups))
		{
			@SuppressWarnings("unchecked")
			List<BannerBean> banners = (List<BannerBean>)c.getObject(cacheKey);

			if (banners != null)
			{
				if (orderBy != null)
				{
					JpaComparator<BannerBean> comparator = new JpaComparator<>(BannerBean.class, orderBy, true);
					comparator.orderList(banners);
				}
				//Filter banners
				return BannerDB.filterByDocAndGroupId(banners);
			}
		}

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(BannerBean.class, builder);

		Expression expr = null;
		if (Tools.isNotEmpty(groups))
		{
			groups = DB.removeSlashes(groups);
			StringTokenizer st = new StringTokenizer(groups, ",+");
			List<String> groupList = new ArrayList<>();
			while (st.hasMoreTokens())
			{
				groupList.add(st.nextToken());
			}
			expr = builder.get("bannerGroup").in(groups.split(","));

		}

		if(expr == null)
			expr = builder.get("domainId").equal(CloudToolsForCore.getDomainId());
		else
			expr = expr.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));

		dbQuery.setSelectionCriteria(expr);


		Query query = em.createQuery(dbQuery);
		List<BannerBean> banners = JpaDB.getResultList(query);

		if (cacheTime>0)
		{
			c.setObject(cacheKey, banners, cacheTime);
		}

		if (orderBy != null)
		{
			JpaComparator<BannerBean> comparator = new JpaComparator<>(BannerBean.class, orderBy, true);
			comparator.orderList(banners);
		}

		//Filter banners by docId/groupId
		return BannerDB.filterByDocAndGroupId(banners);
	}

	/**
	 * vyfiltrujem vsetky bannery na zaklade campaignBanner. Ak nenajde, tak vratim vsetky
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 */
	public static List<BannerBean> getBanners(String groups, String orderBy, String campaignBanner)
	{
		List<BannerBean> result = new ArrayList<>();
		List<BannerBean> allBanners = getBanners(groups, orderBy);
		if(allBanners != null && allBanners.isEmpty()==false)
		{
			for(BannerBean banner : allBanners)
			{
				if (Tools.isNotEmpty(campaignBanner)) {
					//adding only matching campaign banners
					if (campaignBanner.equals(banner.getCampaignTitle())) result.add(banner);
				} else {
					result.add(banner);
				}
			}

			if (result.size()==0) {
				//didn't find any matching campaign banner, add at least non campaign ones (maybe there are more banners on page and we need to show other banners)
				for(BannerBean banner : allBanners)
				{
					if (Tools.isEmpty(banner.getCampaignTitle())) {
						result.add(banner);
					}
				}
			}
		}

		return (result.size() > 0 ? result : allBanners);
	}

	/**
	 * Ziskanie skupin bannerov
	 * @return
	 */
	public static List<BannerGroupBean> getBannerGroups()
	{
		List<BannerGroupBean> banners =new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT distinct banner_group FROM banner_banners ORDER BY banner_group");
			rs = ps.executeQuery();
			while (rs.next())
			{
				BannerGroupBean bean=new BannerGroupBean();
				bean.setBannerGroup(rs.getString("banner_group"));
				banners.add(bean);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				//
			}
		}

		return(banners);
	}

	/**
	 * Ziska skupiny bannerov povolenych pre usera
	 * @param userid id usera
	 * @return
	 */
	public static List<BannerGroupBean> getBannerGroupsByUserAllowedCategories(int userid){
		List<BannerGroupBean> bannerGroupNames = BannerDB.getBannerGroups();
		List<String> allowedCategories = SettingsAdminDB.getAllowedCategories(SettingsAdminDB.getSettings(userid),"menuBanner");
		if(allowedCategories != null){
			List<BannerGroupBean> bannerGroupsFiltered = new ArrayList<>(bannerGroupNames.size());
			for(BannerGroupBean bgb : bannerGroupNames){
				if(allowedCategories.contains(bgb.getBannerGroup().trim()))
					bannerGroupsFiltered.add(bgb);
			}

			bannerGroupNames = bannerGroupsFiltered;
		}
		return bannerGroupNames;
	}


	/**
	 * Ziskanie banera
	 * @param bannerId - id bannera
	 * @return
	 */
	public static BannerBean getBanner(int bannerId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(BannerBean.class, builder);
		Expression expr = builder.get("domainId").equal(CloudToolsForCore.getDomainId());
		expr = expr.and(builder.get("id").equal(bannerId));
		dbQuery.setSelectionCriteria(expr);


		Query query = em.createQuery(dbQuery);
		List<BannerBean> banners = JpaDB.getResultList(query);
		if(!Tools.isEmpty(banners))
			return banners.get(0);
		return new BannerBean();
	}

	/**
	 * Ulozenie banera do DB
	 * @param banBean
	 * @param banId
	 * @return
	 */
	public static boolean saveBanner(BannerBean banBean, int banId)
	{
		boolean saveOK = false;
		try
		{
			banBean.setDomainId(CloudToolsForCore.getDomainId());
			//		uloz banner do DB
			JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
			em.getTransaction().begin();

			if (banId<1)
			{
				em.persist(banBean);
				Adminlog.add(Adminlog.TYPE_BANNER_CREATE, "Vytvoreny banner: "+banBean.getName(), -1, -1);
			}
			else
			{
				//BannerBean original = em.find(BannerBean.class, banId);
//				SQLTemplate template = new SQLTemplate(BannerBean.class, "SELECT * FROM banner", true);
				BeanDiff diff = new BeanDiff().setNewLoadJpaOriginal(banBean, banBean.getBannerId());
				Adminlog.add(Adminlog.TYPE_BANNER_UPDATE, "Upraveny banner: "+banBean.getName()+new BeanDiffPrinter(diff), banId, -1);
				banBean = em.merge(banBean);
			}
			//banBean.setObjectId(new ObjectId(BannerBean.class, "bannerId", 222));

			if (banBean.getStatClicks() == null)
			{
				banBean.setStatClicks(Integer.valueOf(0));
			}
			if (banBean.getStatViews() == null)
			{
				banBean.setStatViews(Integer.valueOf(0));
			}

			em.getTransaction().commit();

			saveOK = true;
		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}

		return saveOK;
	}

	/**
	 * Vymaze banner z DB
	 * @param bannerId - ID bannera v DB
	 * @return
	 */
	public static boolean deleteBanner(int bannerId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean ret = false;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT name FROM banner_banners WHERE banner_id = ?");
			ps.setInt(1, bannerId);
			String name = null;
			rs = ps.executeQuery();

			if (rs.next())
				name = rs.getString(1);

			rs.close();
			rs = null;
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM banner_banners WHERE banner_id=? AND domain_id=? ");
			Adminlog.add(Adminlog.TYPE_BANNER_DELETE, "Zmazany banner: "+name, bannerId, -1);
			ps.setInt(1, bannerId);
			ps.setInt(2, CloudToolsForCore.getDomainId());

			int updateBanner = ps.executeUpdate();

			if(updateBanner != 0) ret = true;
			ps.close();
			ps = null;
		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				//
			}
		}
		return (ret);
	}

	public static BannerBean getRandomBanner(String groups)
	{
		return getRandomBanner(groups, "");
	}

	/**
	 * Vrati nahodny banner
	 * @param groups - zoznam skupin oddelenych ciarkou
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 * @return
	 */
	public static BannerBean getRandomBanner(String groups, String campaignBanner)
	{
		List<BannerBean> bannersOK = getOnlyAvailable(getBanners(groups, null, campaignBanner), campaignBanner);

		if (bannersOK != null && bannersOK.size() > 0)
		{
			//vygeneruj nahodne cislo od 0 do size
			int index = random.nextInt(bannersOK.size());
			if (index >= bannersOK.size())
				index = bannersOK.size() - 1;
			BannerBean banner = bannersOK.get(index);
			return(banner);
		}
		return(null);
	}

	/**
	 * Vrati nahodny banner - doplnene o cookie filtrovanie na zaklade cookieGroup
	 * @param groups - zoznam skupin oddelenych ciarkou
	 * @return
	 */
	public static BannerBean getRandomBanner(String groups, Cookie[] cookie)
	{
		List<BannerBean> banners = getBanners(groups, null);
		List<BannerBean> bannersOK = new ArrayList<>();
		BannerBean banBean;
		long dateFrom = 0;
		long dateTo = 0;
		long now = Tools.getNow();
		int statClicks;
		int statViews;

		for (int i=0; i<banners.size(); i++)
		{
			banBean = banners.get(i);

			dateFrom = 0;
			dateTo = now + 1;
			if (banBean.getDateFrom()!=null)	dateFrom = banBean.getDateFrom().getTime();
			if (banBean.getDateTo()!=null) dateTo = banBean.getDateTo().getTime();

			//ak je nastavene 0, ignoruj statistiku
			statClicks = Tools.getIntValue(banBean.getStatClicks());
			statViews = Tools.getIntValue(banBean.getStatViews());
			if (Tools.getIntValue(banBean.getMaxClicks())==0) statClicks = 0;
			if (Tools.getIntValue(banBean.getMaxViews())==0) statViews = 0;

			//testujem, ci este nebolo dosiahnute obmedzenie zobrazovania banneru, ak nie, pridam banner do zoznamu bannersOK
			if (  Tools.getIntValue(banBean.getMaxClicks()) >= statClicks &&		//MAX CLICKS >= AKTUALNY POCET KLIKNUTI
					Tools.getIntValue(banBean.getMaxViews()) >= statViews &&		//MAX VIEWS >= AKTUALNY POCET VIDENI
					dateFrom <= now &&								//CAS OD <= AKTUALNY CAS
					dateTo >= now 	&&								//CAS DO >= AKTUALNY CAS
					banBean.getActive().booleanValue()==true)
			{
				bannersOK.add(banBean);
			}
		}

		//custom pre Ing kontrola podla cookie usera, vyhodi tie ktore nevyhovuju fitru
		if(cookie != null)
			bannersOK = getVisitorCookieGroup(bannersOK, cookie);

		if (bannersOK.size() > 0)
		{
			//vygeneruj nahodne cislo od 0 do size
			int index = random.nextInt(bannersOK.size());
			if (index >= bannersOK.size())
			{
				index = bannersOK.size() - 1;
			}
			BannerBean banner = bannersOK.get(index);
			return(banner);
		}
		return(null);
	}

    /**
     * Vrati nasledujuci banner v zozname
     * @param groups - zoznam skupin oddelenych ciarkou
     * @param session - session
     * @param bannerList - zoznam bannerov, ktore som uz zobrazil
     * @param bannerIndex - index bannera v session
     * @return
     */
	public static BannerBean getNextBanner(String groups, HttpSession session, List<String> bannerList, String bannerIndex)
	{
		return getNextBanner( groups,  session,  bannerList, bannerIndex, null );
	}

    /**
     * @deprecated pouzite verziu so stringom*
     */
	@Deprecated
    public static BannerBean getNextBanner(String groups, HttpSession session, List<String> bannerList, int bannerIndex)
    {
        return getNextBanner( groups,  session,  bannerList, String.valueOf(bannerIndex), null );
    }

    /**
     * @deprecated pozuite verziu so Stringom
     * @return
     */
	@Deprecated
    public static BannerBean getNextBanner(String groups, HttpSession session, List<String> bannerList, int bannerIndex, Cookie[] cookie)
    {
        return getNextBanner( groups,  session,  bannerList, String.valueOf(bannerIndex), cookie );
    }

	 public static BannerBean getNextBanner(String groups, HttpSession session, List<String> bannerList, String bannerIndex, Cookie[] cookie)
	 {
	 	  return getNextBanner(groups, session, bannerList, String.valueOf(bannerIndex), cookie, null);
	 }

    /**
     * Doplnenie o fitrovanie cez cookie custom pre Ing
     * @param groups
     * @param session
     * @param bannerList
     * @param bannerIndex
	  * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
     * @return
     *@author		$Author: prau $(prau)
     */
	public static BannerBean getNextBanner(String groups, HttpSession session, List<String> bannerList, String bannerIndex, Cookie[] cookie, String campaignBanner)
	{
		//ziskam si zoznam bannerov z DB
		List<BannerBean> banners = getOnlyAvailable(getBanners(groups, null, campaignBanner), campaignBanner);
		List<BannerBean> bannersOK = new ArrayList<>();
		BannerBean banBean;
		BannerBean banner;
		String bannerId;
		boolean found = false;
		//int index = -1;
		long dateFrom = 0;
		long dateTo = 0;
		long now = Tools.getNow();
		int statClicks;
		int statViews;

		for (int i=0; i<banners.size(); i++)
		{
			banBean = banners.get(i);

			dateFrom = 0;
			dateTo = now + 1;
			if (banBean.getDateFrom()!=null)	dateFrom = banBean.getDateFrom().getTime();
			if (banBean.getDateTo()!=null) dateTo = banBean.getDateTo().getTime();

			//ak je nastavene 0, ignoruj statistiku
			statClicks = Tools.getIntValue(banBean.getStatClicks());
			statViews = Tools.getIntValue(banBean.getStatViews());
			if (Tools.getIntValue(banBean.getMaxClicks())==0) statClicks = 0;
			if (Tools.getIntValue(banBean.getMaxViews())==0) statViews = 0;

			//testujem, ci este nebolo dosiahnute obmedzenie zobrazovania banneru, ak nie, pridam banner do zoznamu bannersOK
			if ( Tools.getIntValue(banBean.getMaxClicks()) >= statClicks &&		//MAX CLICKS >= AKTUALNY POCET KLIKNUTI
					Tools.getIntValue(banBean.getMaxViews()) >= statViews &&		//MAX VIEWS >= AKTUALNY POCET VIDENI
					dateFrom <= now &&								//CAS OD <= AKTUALNY CAS
					dateTo >= now 	&&								//CAS DO >= AKTUALNY CAS
					banBean.getActive().booleanValue()==true)
			{
				bannersOK.add(banBean);
			}
		}

		//custom pre Ing kontrola podla cookie usera, vyhodi tie ktore nevyhovuju fitru
		if(cookie != null)
			bannersOK = getVisitorCookieGroup(bannersOK, cookie);

		//System.out.println("bannersOK.size() : "+bannersOK.size());

		//ak v session nie je bannerIndex, t.j. prvy krat volam fukciu a zoznam bannerList je prazdny
		if (session.getAttribute("bannerIndex"+bannerIndex) == null)
		{
			if (bannersOK.size() > 0)
			{
				banner = bannersOK.get(0);
				bannerList.add(String.valueOf(banner.getBannerId()));
				session.setAttribute("bannerList"+bannerIndex, bannerList);

				return(banner);
			}
		}
		else
		{
			if (bannersOK.size() > 0)
			{
				//prelezem zoznam bannersOK a zoznam bannerList, ak su ID rovnake => banner uz bol zobrazeny a preskocim ho
				//V pripade, ze v skupine je iba jeden baner, nekontrolujem ID ci uz bol baner zobrazeny
				for (int i=0; i<bannersOK.size(); i++)
				{
					banner = bannersOK.get(i);
					for (int j=0; j<bannerList.size(); j++)
					{
						bannerId = bannerList.get(j);
						if ( bannerId.equals( String.valueOf(banner.getBannerId()) ) && bannersOK.size() > 1)
						{
							found = true;
							//index = banner.getBannerId();
						}
					}
					if ( !found )
					{
						bannerList.add(String.valueOf(banner.getBannerId()));
						session.setAttribute("bannerList"+bannerIndex, bannerList);

					/*	Logger.println(BannerDB.class,"-----------------\nBannerList: ");
						for (int j=0; j<bannerList.size(); j++)
						{
							Logger.println(BannerDB.class,"   "+ bannerList.get(j));
						}
						Logger.println(BannerDB.class,"-----------------");
					*/

						if (bannerList.size() >= bannersOK.size())
						{
							session.removeAttribute("bannerIndex"+bannerIndex);
							//Logger.println(BannerDB.class,"----- session.removeAttribute");
						}
						return(banner);
					}
					//index = -1;
					found = false;
				}
			}

		}

		return(null);
	}

	public static BannerBean getPriorityBanner(String groups)
	{
		return getPriorityBanner(groups, "");
	}


	/**
	 * Vrati banner s najvyssou prioritou
	 * @param groups - zoznam skupin oddelenych ciarkou
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 * @return
	 */
	public static BannerBean getPriorityBanner(String groups, String campaignBanner)
	{
		List<BannerBean> bannersOK = getOnlyAvailable(getBanners(groups, "id", campaignBanner), campaignBanner);
		BannerBean banBean;
		int index = 0;
		int prioritySum = 0;
		int prioritySumTemp = 0;

		//Logger.println(BannerDB.class,"banners: "+banners.size());

		if (bannersOK != null && bannersOK.size() > 0)
		{
			//urobim sumu vah vsetkych bannerov
			for (int i=0; i<bannersOK.size(); i++)
			{
				banBean = bannersOK.get(i);
				prioritySum += Tools.getIntValue(banBean.getPriority());
			}

			//vygeneruj nahodne cislo od 0 do prioritySum
			index = random.nextInt(prioritySum);	//((int) (Math.random() * (prioritySum)));

			if (index > prioritySum)
			{
				index = prioritySum;
			}

			//vyberam podla nahodneho indexu banner zo zoznamu bannersOK
			for (int i=0; i<bannersOK.size(); i++)
			{
				banBean = bannersOK.get(i);
				prioritySumTemp += Tools.getIntValue(banBean.getPriority());

				if (prioritySumTemp >= index)
				{
					return(banBean);
				}
			}
		}
		return(null);
	}

	public static BannerBean getPriorityBanner(String groups, Cookie[] cookie)
	{
		List<BannerBean> banners = getBanners(groups, "id");
		List<BannerBean> bannersOK = new ArrayList<>();
		BannerBean banBean;
		int index = 0;
		int prioritySum = 0;
		int prioritySumTemp = 0;
		long dateFrom = 0;
		long dateTo = 0;
		long now = Tools.getNow();
		int statClicks;
		int statViews;

		//Logger.println(BannerDB.class,"banners: "+banners.size());

		for (int i=0; i<banners.size(); i++)
		{
			banBean = banners.get(i);
			//Logger.println(BannerDB.class,banBean.getBannerId()+"  "+banBean.getPriority());

			dateFrom = 0;
			dateTo = now + 1;
			if (banBean.getDateFrom()!=null)	dateFrom = banBean.getDateFrom().getTime();
			if (banBean.getDateTo()!=null) dateTo = banBean.getDateTo().getTime();

			//ak je nastavene 0, ignoruj statistiku
			statClicks = Tools.getIntValue(banBean.getStatClicks());
			statViews = Tools.getIntValue(banBean.getStatViews());
			if (Tools.getIntValue(banBean.getMaxClicks())==0) statClicks = 0;
			if (Tools.getIntValue(banBean.getMaxViews())==0) statViews = 0;

			//Logger.println(BannerDB.class,banBean.getBannerId() +  " dateFrom: " + banBean.getDateFrom() + " " + banBean.getDateFromTime() + " = " + dateFrom + " now="+Tools.getNow());
			//Logger.println(BannerDB.class,banBean.getBannerId() +  "   dateTo: " + banBean.getDateTo() + " " + banBean.getDateToTime() + " = " + dateTo + " now="+Tools.getNow());

			//testujem, ci este nebolo dosiahnute obmedzenie zobrazovania banneru, ak nie, pridam banner do zoznamu bannersOK
			if ( Tools.getIntValue(banBean.getMaxClicks()) >= statClicks &&		//MAX CLICKS >= AKTUALNY POCET KLIKNUTI
					Tools.getIntValue(banBean.getMaxViews()) >= statViews &&		//MAX VIEWS >= AKTUALNY POCET VIDENI
					dateFrom <= now &&								//CAS OD <= AKTUALNY CAS
					dateTo >= now 	&&								//CAS DO >= AKTUALNY CAS
					banBean.getActive().booleanValue()==true)
			{
				bannersOK.add(banBean);
			}
		}

		bannersOK = filterByDocAndGroupId(bannersOK);

		//custom pre Ing kontrola podla cookie usera, vyhodi tie ktore nevyhovuju fitru
		if(cookie != null)
			bannersOK = getVisitorCookieGroup(bannersOK, cookie);

		if (bannersOK.size() > 0)
		{
			//urobim sumu vah vsetkych bannerov
			for (int i=0; i<bannersOK.size(); i++)
			{
				banBean = bannersOK.get(i);
				prioritySum += Tools.getIntValue(banBean.getPriority());
			}

			//vygeneruj nahodne cislo od 0 do prioritySum
			index = random.nextInt(prioritySum);

			if (index > prioritySum)
			{
				index = prioritySum;
			}

			//vyberam podla nahodneho indexu banner zo zoznamu bannersOK
			for (int i=0; i<bannersOK.size(); i++)
			{
				banBean = bannersOK.get(i);
				prioritySumTemp += Tools.getIntValue(banBean.getPriority());

				if (prioritySumTemp >= index)
				{
					return(banBean);
				}
			}
		}

		return(null);
	}

	/**
	 * vrati vsetky bannery pre stranku s danou url
	 * @param url - url stranky, pre ktoru chceme vratit zoznam bannerov
	 *	@param groups - nazov skup
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 */
	private static List<BannerBean> getBannersForUrl(String url, String groups, String campaignBanner)
	{
		List<BannerBean> bannerList = getOnlyAvailable(getBanners(groups, null, campaignBanner), campaignBanner)
												.stream()
												.filter(b -> isBannerForUrl(b,url) && BannerDB.isBannerActive(b))
												.collect(Collectors.toList());

		return bannerList;
	}

	/**
	 * Banner pre url - nahodny
	 * @param url - url stranky, pre ktoru chceme vratit zoznam bannerov
	 *	@param groups - nazov skup
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 */
	public static BannerBean getFirstBannerForUrlByPriority(String url, String groups, String campaignBanner)
	{
		List<BannerBean> bannersForUrl = getBannersForUrl(url,groups,campaignBanner);
		if(bannersForUrl != null && bannersForUrl.size()  > 0)
		{
			bannersForUrl = bannersForUrl.stream().sorted((b1,b2)->Integer.compare(b2.getPriority(),b1.getPriority())).collect(Collectors.toList());
			return bannersForUrl.get(0);
		}

		return null;
	}

	/**
	 * Banner pre url - prvy podla najvyssej priority
	 * @param url - url stranky, pre ktoru chceme vratit zoznam bannerov
	 *	@param groups - nazov skup
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 */
	public static BannerBean getBannerForUrlRandom(String url, String groups, String campaignBanner)
	{
		List<BannerBean> bannersForUrl = getBannersForUrl(url,groups,campaignBanner);
		if(bannersForUrl != null && bannersForUrl.size()  > 0)
		{
			return bannersForUrl.get(random.nextInt(bannersForUrl.size()));
		}

		return null;
	}

	/**
	 * vrati priznak, ci dany banner je pre danu url
	 * @param banner
	 * @param url
	 */
	public static boolean isBannerForUrl(BannerBean banner, String url)
	{
		if(Tools.isEmpty(url) || banner == null)
			return false;

		String bannerLocation = banner.getBannerLocation();
		if(Tools.isEmpty(bannerLocation))
			return false;

		String[] bannerUrls = Tools.getTokens(bannerLocation, ",");
		boolean isBannerForUrl = false;
		for (String bannerUrl : bannerUrls)
		{
			//moznost zadat znak * alebo /zaciatok/url-adresy/* do pola Adresa umiestnenia bannera. Pri zadani znaku * sa banner bude zobrazovat na lubovolnej stranke, pri zadani /zaciatok/url-adresy/* na strankach s URL adresou zacinajucou na zadany vyraz.
			if (url.equals(bannerUrl) || "*".equals(bannerUrl) || (bannerUrl.endsWith("/*") && url.startsWith(bannerUrl.substring(0, bannerUrl.length()-1))))
				isBannerForUrl = true;

			if(isBannerForUrl) break;
		}
		return isBannerForUrl;
	}

	/**
	 * Update statistiky kliknuti na banner v DB
	 * @param bannerId - ID bannera v DB
	 * @return
	 */
	public static boolean statAddClick(int bannerId, HttpServletRequest request)
	{
		StatWriteBuffer.add(
			"UPDATE banner_banners SET stat_clicks = stat_clicks + 1, stat_date = ? WHERE banner_id=?",
			"banner_banners",
			new Timestamp(Tools.getNow()),
			bannerId
		);

		StatWriteBuffer.add(
			"INSERT INTO banner_stat_clicks (banner_id, insert_date, ip, host, domain_id) VALUES (?, ?, ?, ?, ?)",
			"banner_stat_clicks",
			bannerId,
			new Timestamp(Tools.getNow()),
			DB.prepareString(Tools.getRemoteIP(request), 16),
			DB.prepareString(Tools.getRemoteHost(request), 128),
			CloudToolsForCore.getDomainId()
		);

		return true;
	}

	/**
	 * Update statistiky videni bannera v DB
	 * @param bannerId - ID bannera v DB
	 * @return
	 */
	public static boolean statAddView(int bannerId)
	{
		StatWriteBuffer.add(
			"UPDATE banner_banners SET stat_views = stat_views + 1, stat_date = ? WHERE banner_id=?",
			"banner_banners",
			new Timestamp(Tools.getNow()),
			bannerId
		);

		StatWriteBuffer.addUpdateInsertPair(
		"UPDATE banner_stat_views_day SET views=views+1 WHERE banner_id = ? AND insert_date = ? AND domain_id=?",
		"INSERT INTO banner_stat_views_day (banner_id, insert_date, views, domain_id) VALUES (?, ?, 1, ?)",
		"banner_stat_views_day",
			bannerId,
			new java.sql.Date(Tools.getNow()),
			CloudToolsForCore.getDomainId()
		);

		return true;
	}


	/**
	 *  vrati time serie VIDENI bannera pre graf
	 *
	 *@param  from      Description of the Parameter
	 *@param  to        Description of the Parameter
	 *@return           The topPagesTimeData value
	 */
	public static Map<String,  Map<java.util.Date, Number>> getBannerStatViewsTimeData(java.util.Date from, java.util.Date to, int bannerId)
	{
		Map<String,  Map<java.util.Date, Number>> collection = new Hashtable<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			Map<java.util.Date, Number> bts;
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder("SELECT ").append(StatNewDB.getDMYSelect(FIELD_INSERT_DATE)).append(", SUM(views) AS views FROM banner_stat_views_day WHERE banner_id=? ");
			if (from != null)
			{
				//Logger.println(BannerDB.class,"++++++++++from: "+Tools.formatDate(from.getTime()));
				sql.append(" AND insert_date>=? ");
			}
			if (to != null)
			{
				//Logger.println(BannerDB.class,"++++++++++from: "+Tools.formatDate(to.getTime()));
				sql.append(" AND insert_date<=? ");
			}
			sql.append(" GROUP BY "+StatNewDB.getDMYGroupBy(FIELD_INSERT_DATE));
			//sql += " ORDER BY insert_date ASC";
			Logger.debug(BannerDB.class,sql.toString());

			ps = db_conn.prepareStatement(sql.toString());
			int index = 1;
			ps.setInt(index++, bannerId);

			if (from != null)
			{
				ps.setDate(index++, new Date(from.getTime()));
			}
			if (to != null)
			{
				ps.setDate(index++, new Date(to.getTime()));
			}

			DebugTimer timer = new DebugTimer("getBannerStatViewsTimeData");
			rs = ps.executeQuery();
			timer.diff("mam RS");

			Calendar cal = Calendar.getInstance();
			//Logger.println(BannerDB.class,"---------- Views");

			//bts = new TimeSeries("banner views");
			bts = new HashMap<>();
			while (rs.next())
			{
				try
				{
					cal.clear();
					cal.setFirstDayOfWeek(Calendar.MONDAY);
					cal.set(Calendar.YEAR, rs.getInt("vt_year"));
					cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
					cal.set(Calendar.DAY_OF_MONTH, rs.getInt("vt_day"));

					Logger.debug(BannerDB.class, "views: "+Tools.formatDate(cal.getTimeInMillis())+" views="+rs.getInt("views"));

					try
					{
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(cal.getTime());
						calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
						calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
						calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
						calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
						java.util.Date day = calendar.getTime();

						bts.put(day, Integer.valueOf(rs.getInt("views")));
					}
					catch (Exception ex)
					{
						Logger.error(StatGraphNewDB.class,"getTimeData: period allready exist: "+cal.getTime().toString());
						Logger.error(ex);
					}
				}
				catch (Exception ex)
				{
					Logger.error(ex);
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			timer.diff("mam data");
			collection.put("banner views" ,bts);
		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				Logger.error(ex2);
			}
		}

		return (collection);
	}


	/**
	 *  vrati time serie KLIKNUTI bannera pre graf
	 *
	 *@param  from      Description of the Parameter
	 *@param  to        Description of the Parameter
	 *@param  bannerId  ID bannera
	 *@return           The topPagesTimeData value
	 */
	public static Map<String,  Map<java.util.Date, Number>> getBannerStatClicksTimeData(java.util.Date from, java.util.Date to, int bannerId)
	{

		Map<String,  Map<java.util.Date, Number>> collection = new Hashtable<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder("SELECT ").append(StatNewDB.getDMYSelect(FIELD_INSERT_DATE)).append(", COUNT(id) AS clicks FROM banner_stat_clicks WHERE banner_id=? ");
			if (from != null)
			{
				//Logger.println(BannerDB.class,"++++++++++from: "+Tools.formatDate(from.getTime()));
				sql.append(" AND insert_date>=? ");
			}
			if (to != null)
			{
				//Logger.println(BannerDB.class,"++++++++++from: "+Tools.formatDate(to.getTime()));
				sql.append(" AND insert_date<=? ");
			}
			sql.append(" GROUP BY "+StatNewDB.getDMYGroupBy(FIELD_INSERT_DATE));
			//sql += " ORDER BY insert_date";
			//Logger.println(BannerDB.class,sql);
			Logger.debug(BannerDB.class, "getBannerStatClicksTimeData sql: "+sql);

			ps = db_conn.prepareStatement(sql.toString());
			int index = 1;
			ps.setInt(index++, bannerId);
			if (from != null)
			{
				ps.setTimestamp(index++, new Timestamp(from.getTime()));
			}
			if (to != null)
			{
				ps.setTimestamp(index++, new Timestamp(to.getTime()));
			}

			rs = ps.executeQuery();

			Calendar cal = Calendar.getInstance();
			//Logger.println(BannerDB.class,"---------- Clicks");

			//TimeSeries bts = new TimeSeries("banner clicks");
			Map<java.util.Date, Number> bts = new HashMap<>();
			while (rs.next())
			{
				try
				{
					cal.clear();
					cal.setFirstDayOfWeek(Calendar.MONDAY);
					cal.set(Calendar.YEAR, rs.getInt("vt_year"));
					cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
					cal.set(Calendar.DAY_OF_MONTH, rs.getInt("vt_day"));
					try
					{
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(cal.getTime());
						calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
						calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
						calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
						calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
						java.util.Date day = calendar.getTime();

						bts.put(day, Integer.valueOf(rs.getInt("clicks")));
					}
					catch (Exception ex)
					{
						Logger.error(StatGraphNewDB.class,"getTimeData: period allready exist: "+cal.getTime().toString());
						Logger.error(ex);
					}
				}
				catch (Exception ex)
				{
					Logger.error(ex);
				}
			}

			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			collection.put("banner clicks" ,bts);

		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				Logger.error(ex2);
			}
		}
		return (collection);
	}


	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	@SuppressWarnings("unused")
	public static List<Column> getTop10Banners(java.util.Date from, java.util.Date to, List<BannerGroupBean> bannerGroups)
	{
		int max_size = 10;
		List<Column> ret = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			StringBuilder sql;
			db_conn = DBPool.getConnection();

			//fix na zle nastavene hodnoty
			ps = db_conn.prepareStatement("UPDATE banner_stat_clicks SET clicks=1 WHERE clicks IS NULL");
			ps.execute();
			ps.close();
			ps = null;

			sql = new StringBuilder("SELECT banner_id, name, banner_location, stat_views, stat_clicks, width, height, active, client_id, banner_group, banner_type")
			.append(" FROM banner_banners")
			.append(" WHERE stat_date >= ? AND stat_date <= ? AND domain_id = ? ");
			//filter by banner groups
			if(bannerGroups != null)
			{
				if(bannerGroups.size() > 0)
				{
					boolean first = true;
					sql.append(" AND banner_group IN (");
					for(BannerGroupBean bgb : bannerGroups)
					{
						if(first)
						{
							sql.append(" ? ");
							first = false;
						}
						else
							sql.append(", ? ");
					}
					sql.append(" ) ");
				}
			}
			sql.append(" ORDER BY stat_views DESC, stat_clicks DESC");

			//Logger.println(BannerDB.class,"GetTopPages: "+sql);

			int counter = 1;
			ps = db_conn.prepareStatement(sql.toString());
			ps.setTimestamp(counter++, new Timestamp(from.getTime()));
			ps.setTimestamp(counter++, new Timestamp(to.getTime()));
			ps.setInt(counter++, CloudToolsForCore.getDomainId());

			if(bannerGroups != null)
			{
				if(bannerGroups.size() > 0)
				{
					for(BannerGroupBean bgb : bannerGroups)
					{
						ps.setString(counter++, bgb.getBannerGroup());
					}
				}
			}

			rs = ps.executeQuery();
			//iteruj cez riadky
			Column col;
			int count = 0;
			while (rs.next() && count < max_size)
			{
				col = new Column();
				col.setIntColumn1(rs.getInt("banner_id"));
				col.setColumn2(DB.getDbString(rs, "banner_location"));
				col.setIntColumn3(rs.getInt("stat_views"));
				col.setIntColumn4(rs.getInt("stat_clicks"));
				col.setIntColumn5(rs.getInt("width"));
				col.setIntColumn6(rs.getInt("height"));
				col.setIntColumn7(rs.getInt("client_id"));
				col.setColumn8(rs.getString("banner_group"));
				col.setColumn9(rs.getString("banner_type"));
				col.setBooleanColumn1(rs.getBoolean("active"));
				col.setColumn3(BannerDB.getBannerNameFromLocation(DB.getDbString(rs, "name"), col.getColumn2()));
				ret.add(col);
				count++;
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				Logger.error(ex2);
			}
		}
		return (ret);
	}

	private static Map<Integer, String> getBannerNamesTable()
	{
		Map<Integer, String> bannerNames = new Hashtable<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			//ziskaj si zoznam bannerov
			String sql = "SELECT banner_id, name, banner_location FROM banner_banners WHERE domain_id = ? ";

			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, CloudToolsForCore.getDomainId());
			rs = ps.executeQuery();
			while(rs.next())
			{
				bannerNames.put(Integer.valueOf(rs.getInt("banner_id")), BannerDB.getBannerNameFromLocation(DB.getDbString(rs, "name"), DB.getDbString(rs, "banner_location")));
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				//
			}
		}
		return bannerNames;
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @param bannerGroups
	 * @return
	 */
	@SuppressWarnings("unused")
	public static Map<String, Map<java.util.Date, Number>> getTop10BannersViewsTimeData(java.util.Date from, java.util.Date to, List<BannerGroupBean> bannerGroups)
	{
		int max_size = 10;
		int count = 0;
		Map<String, List<Integer[]>> dates = new Hashtable<>();
		int[] topBanners = new int[max_size];
		Map<Integer, String> bannerNames = BannerDB.getBannerNamesTable();

		Map<String,  Map<java.util.Date, Number>> collection = new Hashtable<>();
		Map<java.util.Date, Number> bts;
		StringBuilder sql = new StringBuilder();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (from != null && to != null)
			{
				db_conn = DBPool.getConnection();
				//ziskam si top10 - robime to z clicks tabulky (je to rychlejsie) a vieme porovnat videnia VS kliknutia
				sql.append("SELECT c.banner_id, COUNT(c.banner_id) AS clicks ").append("FROM banner_stat_clicks c JOIN banner_banners b ON (b.banner_id = c.banner_id) ").append("WHERE insert_date >= ? AND insert_date <= ? AND b.domain_id = ? ");

				if (bannerGroups != null)
				{
					if (bannerGroups.size() > 0)
					{
						boolean first = true;
						sql.append(" AND b.banner_group IN (");
						for (BannerGroupBean bgb : bannerGroups)
						{
							if (first)
							{
								sql.append(" ? ");
								first = false;
							}
							else
								sql.append(", ? ");
						}
						sql.append(" ) ");
					}
				}


				sql.append("GROUP BY c.banner_id ").append("ORDER BY clicks DESC");

				int counter = 1;

				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(counter++, new Timestamp(from.getTime()));
				ps.setTimestamp(counter++, new Timestamp(to.getTime()));
				ps.setInt(counter++, CloudToolsForCore.getDomainId());

				if(bannerGroups != null)
				{
					if(bannerGroups.size() > 0)
					{
						for(BannerGroupBean bgb : bannerGroups)
						{
							ps.setString(counter++, bgb.getBannerGroup());
						}
					}
				}

				rs = ps.executeQuery();
				while(rs.next() && count < max_size)
				{
					topBanners[count] = rs.getInt("banner_id");
					count++;
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;

				//podla top10 zistim pocet videni na den
				sql.delete(0, sql.length());
				sql.append("SELECT banner_id, ").append(StatNewDB.getDMYSelect(FIELD_INSERT_DATE)).append(", SUM(views) AS views ")
				.append("FROM banner_stat_views_day ").append("WHERE insert_date >= ? AND insert_date <= ? AND banner_id IN ( ");

				if (Constants.DB_TYPE == Constants.DB_ORACLE)
				{
					sql.delete(0, sql.length());
					sql.append("SELECT banner_id, ").append(StatNewDB.getDMYSelect(FIELD_INSERT_DATE)).append(", SUM(views) AS views ")
					.append("FROM banner_stat_views_day ").append("WHERE insert_date >= ? AND insert_date <= ? AND banner_id IN ( ");
				}

				for(int i=0; i<topBanners.length; i++)
				{
					if (i > 0)
					{
						sql.append(", '").append(topBanners[i]).append('\'');
					}
					else
					{
						sql.append('\'').append(topBanners[i]).append('\'');
					}
				}
				sql.append(") AND domain_id = ? ");

				if (Constants.DB_TYPE == Constants.DB_MSSQL)
				{
					sql.append("GROUP BY banner_id, ").append(StatNewDB.getDMYGroupBy(FIELD_INSERT_DATE));
				}
				else
				{
					if (Constants.DB_TYPE == Constants.DB_ORACLE)
					{
						String fieldName = FIELD_INSERT_DATE;
						sql.append("GROUP BY banner_id,TO_CHAR(").append(fieldName).append(", 'DD') , TO_CHAR(").append(fieldName)
						.append(", 'MM') , TO_CHAR(").append(fieldName).append(", 'YYYY')");
					}
					else
					{
					sql.append("GROUP BY banner_id, vt_day, vt_month, vt_year ");
					}

				}
				//-netreba sql += "ORDER BY vt_year, vt_month, vt_day";

				Logger.debug(BannerDB.class, "sql:"+sql);
				DebugTimer timer = new DebugTimer("getTop10BannersViewsTimeData");
				ps = db_conn.prepareStatement(sql.toString());
				ps.setDate(1, new Date(from.getTime()));
				ps.setDate(2, new Date(to.getTime()));
				ps.setInt(3, CloudToolsForCore.getDomainId());

				rs = ps.executeQuery();
				//iteruj cez riadky
				Calendar cal = Calendar.getInstance();
				timer.diff("mam RS");
				while (rs.next())
				{
					try {
						cal.clear();
						cal.setFirstDayOfWeek(Calendar.MONDAY);
						cal.set(Calendar.YEAR, rs.getInt("vt_year"));
						cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
						cal.set(Calendar.DAY_OF_MONTH, rs.getInt("vt_day"));

						Integer[] data = new Integer[2];
						data[0] = Integer.valueOf(rs.getInt("banner_id"));
						data[1] = Integer.valueOf(rs.getInt("views"));

						if (dates.get(Tools.formatDate(cal.getTime()))!=null)
						{
							List<Integer[]> dayBanners = dates.get(Tools.formatDate(cal.getTime()));
							if (dayBanners != null)
								dayBanners.add(data);
						}
						else
						{
							List<Integer[]> dayBanners = new ArrayList<>();
							dayBanners.add(data);
							dates.put(Tools.formatDate(cal.getTime()), dayBanners);
						}
					} catch (Exception ex) {}
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
				timer.diff("RS spracovane");

				Calendar calTo = Calendar.getInstance();
				calTo.setFirstDayOfWeek(Calendar.MONDAY);
				calTo.setTime(to);

				cal.clear();
				cal.setTimeInMillis(from.getTime());
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				//cal.set(from.getYear()+1900, from.getMonth(), from.getDate(), 0, 0);

				//Logger.println(BannerDB.class,"Date: "+cal.getTime());

				for(int i=0; i<topBanners.length; i++)
				{
					if(topBanners[i] > 0)
					{
						String name = bannerNames.get(Integer.valueOf(topBanners[i]));
						//bts = new TimeSeries(name != null ? name:"");
						bts = new HashMap<>();

						cal.clear();
						cal.setTimeInMillis(from.getTime());
						cal.setFirstDayOfWeek(Calendar.MONDAY);
						//cal.set(from.getYear()+1900, from.getMonth(), from.getDate(), 0, 0);
						while (cal.getTimeInMillis() <= calTo.getTimeInMillis())
						{
							if (dates.get(Tools.formatDate(cal.getTime()))!=null)
							{
								List<Integer[]> dayBanners = dates.get(Tools.formatDate(cal.getTime()));
								if (dayBanners != null)
								{
									for (Integer[] data : dayBanners)
									{

										if (topBanners[i] == data[0].intValue())
										{
											try
											{
												Calendar calendar = Calendar.getInstance();
												calendar.setTime(cal.getTime());
												calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
												calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
												calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
												calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
												java.util.Date day = calendar.getTime();

												if(bts.get(day)==null)
													bts.put(day, data[1]);
												else
													bts.put(day, bts.get(day).intValue() + data[1].intValue());
											}
											catch (Exception ex)
											{
												Logger.error(BannerDB.class,"getTimeData: period allready exist: "+cal.getTime().toString());
												//Logger.error(ex);
											}
										}
									}
								}
							}
							cal.add(Calendar.DAY_OF_YEAR, 1);
						}
						collection.put(name != null ? name:"" ,bts);
					}
				}
			}

		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				Logger.error(ex2);
			}
		}

		return (collection);
	}


	/**
	 *
	 * @param from
	 * @param to
	 * @param bannerGroups
	 * @return
	 */
	@SuppressWarnings("unused")
	public static Map<String, Map<java.util.Date, Number>> getTop10BannersClicksTimeData(java.util.Date from, java.util.Date to, List<BannerGroupBean> bannerGroups)
	{
		int max_size = 10;
		int count = 0;
		Map<String, List<Integer[]>> dates = new Hashtable<>();
		int[] topBanners = new int[max_size];
		Map<Integer, String> bannerNames = BannerDB.getBannerNamesTable();

		Map<String,  Map<java.util.Date, Number>> collection = new Hashtable<>();
		Map<java.util.Date, Number> bts;
		StringBuilder sql = new StringBuilder();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (from != null && to != null)
			{
				db_conn = DBPool.getConnection();
				//ziskam si top10
				sql.append("SELECT c.banner_id, COUNT(c.banner_id) AS clicks ")
				.append("FROM banner_stat_clicks c JOIN banner_banners b ON (b.banner_id = c.banner_id)").append("WHERE c.insert_date >= ? AND c.insert_date <= ? AND b.domain_id = ? ");

				if (bannerGroups != null)
				{
					if (bannerGroups.size() > 0)
					{
						boolean first = true;
						sql.append(" AND b.banner_group IN (");
						for (BannerGroupBean bgb : bannerGroups)
						{
							if (first)
							{
								sql.append(" ? ");
								first = false;
							}
							else
								sql.append(", ? ");
						}
						sql.append(" ) ");
					}
				}


				sql.append("GROUP BY c.banner_id ").append("ORDER BY clicks DESC");

				int counter = 1;
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(counter++, new Timestamp(from.getTime()));
				ps.setTimestamp(counter++, new Timestamp(to.getTime()));
				ps.setInt(counter++, CloudToolsForCore.getDomainId());

				if(bannerGroups != null)
				{
					if(bannerGroups.size() > 0)
					{
						for(BannerGroupBean bgb : bannerGroups)
						{
							ps.setString(counter++, bgb.getBannerGroup());
						}
					}
				}

				rs = ps.executeQuery();
				while(rs.next() && count < max_size)
				{
					topBanners[count] = rs.getInt("banner_id");
					count++;
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;

				//podla top10 zistim pocet videni na den
				sql.delete(0, sql.length());
				sql.append("SELECT banner_id, COUNT(banner_id) AS clicks, ").append(StatNewDB.getDMYSelect(FIELD_INSERT_DATE)).append(' ')
				.append("FROM banner_stat_clicks ").append("WHERE banner_id IN ( ");

				for(int i=0; i<topBanners.length; i++)
				{
					if (i > 0)
					{
						sql.append(", '").append(topBanners[i]).append('\'');
					}
					else
					{
						sql.append('\'').append(topBanners[i]).append('\'');
					}
				}
				sql.append(") AND domain_id = ? ");
				sql.append("GROUP BY banner_id, ").append(StatNewDB.getDMYGroupBy(FIELD_INSERT_DATE)).append(' ')
				.append("ORDER BY vt_year, vt_month, vt_day");

				ps = db_conn.prepareStatement(sql.toString());
				ps.setInt(1, CloudToolsForCore.getDomainId());
				rs = ps.executeQuery();
				//iteruj cez riadky
				Calendar cal = Calendar.getInstance();

				while (rs.next())
				{
					cal.clear();
					cal.setFirstDayOfWeek(Calendar.MONDAY);
					cal.set(Calendar.YEAR, rs.getInt("vt_year"));
					cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
					cal.set(Calendar.DAY_OF_MONTH, rs.getInt("vt_day"));

					Integer[] data = new Integer[2];
					data[0] = Integer.valueOf(rs.getInt("banner_id"));
					data[1] = Integer.valueOf(rs.getInt("clicks"));

					if (dates.containsKey(Tools.formatDate(cal.getTime())))
					{
						List<Integer[]> dayBanners = dates.get(Tools.formatDate(cal.getTime()));
						if (dayBanners != null)
							dayBanners.add(data);
					}
					else
					{
						List<Integer[]> dayBanners = new ArrayList<>();
						dayBanners.add(data);
						dates.put(Tools.formatDate(cal.getTime()), dayBanners);
					}
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;

				Calendar calTo = Calendar.getInstance();
				calTo.setFirstDayOfWeek(Calendar.MONDAY);
				calTo.setTime(to);

				cal.clear();
				cal.setTimeInMillis(from.getTime());
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				//cal.set(from.getYear()+1900, from.getMonth(), from.getDate(), 0, 0);

				//Logger.println(BannerDB.class,"Date: "+cal.getTime());

				for(int i=0; i<topBanners.length; i++)
				{
					if(topBanners[i] > 0)
					{
						String value = bannerNames.get(Integer.valueOf(topBanners[i]));
						//bts = new TimeSeries(value != null ? value : "");
						bts = new HashMap<>();

						cal.clear();
						cal.setTimeInMillis(from.getTime());
						cal.setFirstDayOfWeek(Calendar.MONDAY);
						//cal.set(from.getYear()+1900, from.getMonth(), from.getDate(), 0, 0);
						while (cal.getTimeInMillis() <= calTo.getTimeInMillis())
						{
							if (dates.containsKey(Tools.formatDate(cal.getTime())))
							{
								List<Integer[]> dayBanners = dates.get(Tools.formatDate(cal.getTime()));
								if (dayBanners != null)
								{
									for (Integer[] data : dayBanners)
									{

										if (topBanners[i] == data[0].intValue())
										{
											try
											{
												Calendar calendar = Calendar.getInstance();
												calendar.setTime(cal.getTime());
												calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
												calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
												calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
												calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
												java.util.Date day = calendar.getTime();

												bts.put(day, data[1]);
											}
											catch (Exception ex)
											{
												Logger.error(BannerDB.class,"getTimeData: period allready exist: "+cal.getTime().toString());
												Logger.error(ex);
											}
										}
									}
								}
							}
							cal.add(Calendar.DAY_OF_YEAR, 1);
						}
						collection.put(value != null ? value : "" ,bts);
					}
				}
			}

		}
		catch (Exception ex)
		{
			Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				Logger.error(ex2);
			}
		}
		return (collection);
	}

	/**
	 * Update statistiky videni bannera v DB
	 * @param bannerId - ID bannera v DB
	 * @return
	 */
	public static boolean statAddViewExt(int bannerId,int inc,java.util.Date insertDate)
	{
		StatWriteBuffer.addUpdateInsertPair(
			"UPDATE banner_stat_views_day SET views=views+? WHERE banner_id = ? AND insert_date = ?",
			"INSERT INTO banner_stat_views_day (views, banner_id, insert_date) VALUES (?, ?, ?)",
			"banner_stat_views_day",
			inc,
			bannerId,
			new java.sql.Date(insertDate.getTime())
		);
		return true;
	}

	public static String getBannerNameFromLocation(String name, String location)
	{
		try
		{
			if (Tools.isEmpty(name))
			{
				name = location;
				if (name != null)
				{
					int i = name.lastIndexOf('/');
					if (i > 0)
					{
						name = name.substring(i+1);
					}
				}
			}
		}
		catch (RuntimeException ex)
		{
			Logger.error(ex);
		}

		return(name);
	}

	/**
	 * Ziskanie uzivatelskych bannerov
	 * @param userId
	 * @return
	 */
	public static List<BannerBean> getBannersByUserId(String groups, String orderBy, int userId)
	{
		List<BannerBean> bannerList = getBanners(groups, orderBy);
		List<BannerBean> userBannerList = new ArrayList<>();
		for (BannerBean bannerBean : bannerList)
		{
			if(bannerBean.getClientId() == userId)
			{
				userBannerList.add(bannerBean);
			}
		}
		return userBannerList;
	}

	/**
	 * Ziskanie uzivatelskych bannerov pre statistiku
	 * @param from
	 * @param to
	 * @param userId
	 * @return
	 */
	public static List<Column> getTop10BannersByUserId(java.util.Date from, java.util.Date to, int userId,List<BannerGroupBean> bannerGroups)
	{
		List<Column> topBanners = BannerDB.getTop10Banners(from, to,bannerGroups);
		List<Column> userTopBanners = new ArrayList<>();
		for (Column col : topBanners)
		{
			if(col.getIntColumn7() == userId)
			{
				userTopBanners.add(col);
			}
		}
		return userTopBanners;
	}

	/**
	 * Metoda vrati true ak uzivatel ma pravo vidiet banner so zadanym bannerId, v opacnom pripade vrati false
	 * @param bannerId
	 * @param userId
	 * @return
	 */
	public static boolean getBannerAccess(int bannerId, int userId)
	{
		BannerBean bb = getBanner(bannerId);
		if(bb != null)
		{
			List<BannerBean> userBannerList = getBannersByUserId(null, "id", userId);
			if(userBannerList.size() > 0)
			{
				if(bb.getClientId() == userId)
					return true;
			}
		}

		return false;
	}

	/**
	 * Zistuje platnost baneru, ci je aktivny, banerovu expiraciu, ci nieje prekoreceny pocet zobrazeni
	 * @param banner
	 * @return
	 */
	public static boolean isBannerActive(BannerBean banner)
	{
		if(banner != null)
		{
			long dateFrom = 0;
			long dateTo = 0;
			long now = Tools.getNow();
			int statClicks;
			int statViews;

			dateFrom = 0;
			dateTo = now + 1;
			if (banner.getDateFrom()!=null)	dateFrom = banner.getDateFrom().getTime();
			if (banner.getDateTo()!=null) dateTo = banner.getDateTo().getTime();

			//ak je nastavene 0, ignoruj statistiku
			statClicks = Tools.getIntValue(banner.getStatClicks());
			statViews = Tools.getIntValue(banner.getStatViews());
			if (Tools.getIntValue(banner.getMaxClicks())==0) statClicks = 0;
			if (Tools.getIntValue(banner.getMaxViews())==0) statViews = 0;

			//testujem, ci este nebolo dosiahnute obmedzenie zobrazovania banneru, ak nie, pridam banner do zoznamu bannersOK
			if (  Tools.getIntValue(banner.getMaxClicks()) >= statClicks &&		//MAX CLICKS >= AKTUALNY POCET KLIKNUTI
					Tools.getIntValue(banner.getMaxViews()) >= statViews &&		//MAX VIEWS >= AKTUALNY POCET VIDENI
				  dateFrom <= now &&								//CAS OD <= AKTUALNY CAS
				  dateTo >= now 	&&								//CAS DO >= AKTUALNY CAS
				  banner.getActive().booleanValue()==true)
			{
				return true;
			}
		}

		return false;
	}

	public static List<BannerBean> getOnlyAvailable(List<BannerBean> banners)
	{
		return getOnlyAvailable(banners, null);
	}

	/**
	 * prefiltruje zoznam banerov a vrati dostupne banery a v pripade obsahovych len take, ktore sa nezobrazuju len pri kampani
	 * @param banners
	 * @param campaignBanner - hodnota kampanoveho bannera ziskaneho z parametra
	 */
	public static List<BannerBean> getOnlyAvailable(List<BannerBean> banners, String campaignBanner)
	{
		List<BannerBean> result = new ArrayList<>();
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		String url = "/";
		if (rb != null) url = rb.getUrl();

		if(banners != null && banners.isEmpty()==false)
		{
			for(BannerBean banner : banners)
			{
				if(banner.isAvailable())
				{
					//obsahovy banner sa zobrazuje len na nasich URL adresach
					if (banner.getBannerType().intValue() == 4)
					{
						//ak mam priznak zobrazovania len pri kampani
						if(isBannerCampaignOnly(banner) &&
							(
								Tools.isEmpty(campaignBanner) || //hodnota v kampanovom parametri nesmie byt prazdna
								Tools.isEmpty(banner.getCampaignTitle()) || //hodnota campaignTitle (Kampanovy banner) nesmie byt prazdna
								campaignBanner.equals(banner.getCampaignTitle()) == false //hodnoty parametra a campaignTitle musi byt zhodna
							)
						) {
							continue;
						}

						if(isBannerForUrl(banner, url))
							result.add(banner);
					}
					else
					{
						result.add(banner);
					}
				}
			}
		}

		//Filte banners
		return BannerDB.filterByDocAndGroupId(result);
	}

	/**
	 * Vrati iba tie banery, ktore vyhovuju skupine v cookies. Ak je cookie prazdna vrati null alebo jeden banner.
	 * @param banners
	 * @param cookie
	 * @return
	 *@author		$Author: prau $(prau)
	 */
	public static List<BannerBean> getVisitorCookieGroup(List<BannerBean> banners, Cookie[] cookie)
	{
		List<BannerBean> returnBanners = new ArrayList<>();
		List<BannerBean> returnDefaultBanner = new ArrayList<>();
		String[] tokens;
		String cookieValue = null;
		String defaultGroupValue = null;

		Logger.debug(BannerDB.class,"banners size() : "+banners.size());
		if(cookie != null && banners != null && banners.size() > 0)
		{
			defaultGroupValue = Constants.getString("ingBankVisitorCookieGroupDefaultValue");
			//System.out.println("Cookies: ");
			for(Cookie c: cookie)
			{
				//"meno cookie premennej ktoru ziskam"
				if(c.getName().equals(Constants.getString("ingBankVisitorCookieGroupName")))
				{
					cookieValue = c.getValue();
					Logger.debug(BannerDB.class,"Ziskal som cookie hodnotu: "+cookieValue);
				}
			}

			boolean banneradded = false;
			for(BannerBean banner : banners)
			{
				tokens = Tools.getTokens(banner.getVisitorCookieGroup(),",");
				for(String bGrop:tokens)
				{
					if(!banneradded && cookieValue != null && cookieValue.length() > 0 && bGrop.indexOf(cookieValue) != -1 )
					{
						returnBanners.add(banner);
						banneradded = true;
					}

					if(bGrop.indexOf(defaultGroupValue) != -1 )
					{
						returnDefaultBanner.add(banner);
					}
				}
				banneradded = false;
			}
		}
		else
			Logger.debug(BannerDB.class,"cookie je null alebo banners je null alebo prazdny"+" cookie: "+cookie+" banners: "+banners);

		return  (returnBanners != null && returnBanners.size() > 0) ? returnBanners : returnDefaultBanner;
	}

	/**
	 * Metoda zisti ci je banner kampanovy, tzn. zobrazuje sa iba pri kampani
	 * @param banner
	 * @return true alebo false podla toho ci je banner kampanovy
	 */
	public static boolean isBannerCampaignOnly(BannerBean banner)
	{
		if (banner == null)
			return false;

		// Kampanovy banner moze byt iba banner typu 4 = Obsahovy banner
		if (banner.getBannerType().intValue() == 4 && (banner.getOnlyWithCampaign() == null || !banner.getOnlyWithCampaign()))
			return false;

		return true;
	}

	private static List<BannerBean> filterByDocAndGroupId(List<BannerBean> bannerList)
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null) return bannerList;

		int docId = rb.getDocId();
		int groupId = rb.getGroupId();

		if(docId == -1) return bannerList;

		if(groupId == -1)
		{
			DocDetails dd = DocDB.getInstance().getBasicDocDetails(docId, false);
			if(dd != null) groupId = dd.getGroupId();
		}

		Set<BannerBean> hs = new HashSet<>();
		List<GroupDetails> parentGroups = null;
		if (groupId > 0) parentGroups = GroupsDB.getParentGroupsCached(groupId);

		for(BannerBean bb : bannerList)
		{
			//ak je prazdne groups aj docs pridaj (zobrazuje sa vsade)
			if ((bb.getDocIds()==null || bb.getDocIds().isEmpty()) && (bb.getGroupIds()==null || bb.getGroupIds().isEmpty()))
			{
				hs.add(bb);
			}
			else
			{
				if (bb.getGroupIds()!=null && bb.getGroupIds().isEmpty()==false && parentGroups!=null)
				{
					// pre adresare
					for(GroupDetails gd:parentGroups)
					{
						for(BannerWebGroupBean isgb:bb.getGroupIds())
						{
							if(isgb.getGroupId() == gd.getGroupId()) hs.add(bb);
						}
					}
				}
				if (bb.getDocIds()!=null && bb.getDocIds().isEmpty()==false)
				{
					for(BannerWebDocBean bwdb:bb.getDocIds())
					{
						if(bwdb.getDocId() == docId || docId == -1) hs.add(bb);
					}
				}
			}
		}

		bannerList.clear();
		bannerList.addAll(hs);
		return bannerList;
	}

}
