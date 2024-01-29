package sk.iway.iwcm.components.rating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.rating.jpa.RatingEntity;
import sk.iway.iwcm.components.rating.jpa.RatingRepository;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.forum.ForumDB;
import sk.iway.iwcm.stat.StatDB;

public class RatingService {

	/** Do tabulky documents do stlpcu forum_count vypocita priemerny rating vsetkych stranok (aj v podpriecinkoch).
	 *  POZOR na danej stranke nemoze byt pozita diskusia (tiez pouziva stlpec forum_count)
	 *
	 * @param args - pole groupID
	 */
	public static void main(String[] args)
	{
		int count = 0;
		if(args != null && args.length > 0)
		{
			//prejdeme vsetky grupy zadane ako parameter
			for(String s : args)
			{
				// prejdeme vsetky pod-grupy
				List<GroupDetails> ret = GroupsDB.getInstance().getGroupsTree(Tools.getIntValue(s, -1),true,true,false);
				for(GroupDetails gd:ret)
				{
					// z kazdej pod-grupy prejdeme vsetky stranky
					List<DocDetails> det = DocDB.getInstance().getBasicDocDetailsByGroup(gd.getGroupId(), DocDB.ORDER_PRIORITY);
					for(DocDetails dc : det)
					{
						RatingEntity rDB = getDocIdRating(dc.getDocId());
						int docIdRating = rDB.getRatingValue();
						ForumDB.setForumCountForDocIds(dc.getDocId(), docIdRating );
						//Logger.debug(RefreshRating.class,"Refreshed : DocID: "+dc.getDocId()+" rating: "+docIdRating);
						count++;
					}
				}
			}
		Logger.debug(RatingService.class, "Rating of page was Refreshed for "+count+" pages.");
		}
	}


	/**
	 * Metoda vrati aktualne browserId, prip. priradi nove na zaklade typu pouzivatela (stroj, registrovany, neregistrovany).
	 *
	 * @param request
	 * @param response
	 *
	 * @return
	 */
	public static long getBrowserId(HttpServletRequest request, HttpServletResponse response) {
		return (StatDB.getBrowserId(request, response));
	}

	/**
	 * Ulozi rating do databazy a vrati TRUE, ak je vsetko OK
	 * @param rBean - rating, ktory sa ulozi do DB
	 * @return
	 */
	public static boolean saveRating(RatingEntity entity) {
		return saveRating(entity, null);
	}

	/**
	 * Ulozi rating do databazy a vrati TRUE, ak je vsetko OK
	 * @param rBean - rating, ktory sa ulozi do DB
	 * @return
	 */
	public static boolean saveRating(RatingEntity entity, HttpServletRequest request) {
		if(entity == null) return false;

		RatingRepository ratingRepository = Tools.getSpringBean("ratingRepository", RatingRepository.class);
		//set actual date time
		entity.setInsertDate(new Date());

		//If non logged user doing rating
		if(entity.getUserId() == null || entity.getUserId() < 1)
			if(request != null)
				entity.setUserId( (int)getBrowserId(request, null) + 10000 );

		//save
		ratingRepository.save(entity);

		return true;
	}

	/**
	 * Vrati zoznam ratingov
	 * @return
	 */
	public static List<RatingEntity> getRatings() {
		RatingRepository ratingRepository = Tools.getSpringBean("ratingRepository", RatingRepository.class);
		return ratingRepository.findAllByOrderByIdAsc();
	}

	/**
	 * Vymaze rating z DB
	 * @return
	 */
	public static boolean deleteRating(long ratingId) {
		Logger.println(RatingService.class,"DELETE: ratingId= " + ratingId);
		int numberOfUpdated = (new SimpleQuery()).executeWithUpdateCount("DELETE FROM rating WHERE rating_id=?", ratingId);
		Logger.println(RatingService.class,"Number of DELETED rows= " + numberOfUpdated + " from rating");
		if(numberOfUpdated > 0) return true;
		return false;
	}

	/**
	 * Vrati rating podla ratingID
	 * @param ratingId - ID ratingu v DB
	 * @return
	 */
	public static RatingEntity getRaitingById(long raitingId) {
		RatingRepository ratingRepository = Tools.getSpringBean("ratingRepository", RatingRepository.class);
		Optional<RatingEntity> entityOpt = ratingRepository.findById(raitingId);
		if(entityOpt.isPresent()) return entityOpt.get();
		return null;
	}

	/**
	 * Vrati vsetky ratingy usera
	 * @param userId - ID uzivatela v DB
	 * @return
	 */
	public static List<RatingEntity> getRatingsByUserId(int userId) {
		RatingRepository ratingRepository = Tools.getSpringBean("ratingRepository", RatingRepository.class);
		return ratingRepository.findAllByUserIdOrderById(userId);
	}

	/**
	 * Vrati vsetky ratingy pre docId
	 * @param docId
	 * @return
	 */
	public static List<RatingEntity> getRatingsByDocId(int docId) {
		RatingRepository ratingRepository = Tools.getSpringBean("ratingRepository", RatingRepository.class);
		return ratingRepository.findAllByDocId(docId);
	}

	/**
	 * Vrati RatingBean, ak user este nehlasoval za docId, vrati NULL
	 * @param userId - ID uzivatela v DB
	 * @param docId - ID stranky, za ktoru sa hlasuje
	 * @return
	 */
	public static RatingEntity getRatingByUserByDoc(int userId, int docId)
	{
		return getRatingByUserByDoc(userId, docId, 0);
	}

	/**
	 * Vrati RatingBean, ak user este nehlasoval za docId, vrati NULL
	 * @param userId - ID uzivatela v DB
	 * @param docId - ID stranky, za ktoru sa hlasuje
	 * @param rateAgainCycleInHours - pokial je > 0, tak umozni uzivatelovi hlasovat viackrat s min odstupom rateAgainCycle hodin. Inak umozni zahlasovat len raz
	 * @return
	 */
	public static RatingEntity getRatingByUserByDoc(int userId, int docId, int rateAgainCycleInHours) {
		Optional<RatingEntity> entityOpt;
		RatingRepository ratingRepository = Tools.getSpringBean("ratingRepository", RatingRepository.class);

		if(rateAgainCycleInHours > 0) {
			Calendar checkTime = Calendar.getInstance();
			checkTime.add(Calendar.HOUR, -rateAgainCycleInHours);
			entityOpt = ratingRepository.findByUserIdAndDocIdAndInsertDateGreaterThanEqualOrderByInsertDateDesc(userId, docId, checkTime.getTime());
		} else {
			entityOpt = ratingRepository.findByUserIdAndDocIdOrderByInsertDateDesc(userId, docId);
		}

		if(entityOpt.isPresent()) return entityOpt.get();
		return null;
	}

	/**
	 * Vrati pocet zaznamov pre zvolene kriterium, ak za krit. nezada, vrati pocet vsetkych
	 * @param colName - nazov stlpca, nad ktorym sa vykona COUNT
	 * @param whereCol - nazov stlpca, ktory je kriteriom pre WHERE (typ int), ak je NULL, WHERE sa nevykona
	 * @param whereValue - int hodnota pre WHERE
	 * @return
	 */
	public static int countCol(String countCol, String whereCol, int whereValue) {
		if(Tools.isNotEmpty(countCol)) {
			if(Tools.isNotEmpty(whereCol))
				return (new SimpleQuery()).forInt("SELECT COUNT(" + countCol + ") FROM rating WHERE " + whereCol + "=?", whereValue);

			return (new SimpleQuery()).forInt("SELECT COUNT(" + countCol + ") FROM rating");
		}
		return 0;
	}

	/**
	 * Pre zadane docId vrati ratingBean s vypocitanym ratingom a poctom userov
	 * @param docId - doc ID v DB
	 * @return
	 */
	public static RatingEntity getDocIdRating(int docId) {
		int sumRating = 0;
		RatingEntity entity = new RatingEntity();
		List<RatingEntity> ratings = getRatingsByDocId(docId);

		for(RatingEntity rating : ratings)
			sumRating += rating.getRatingValue();

		int totalUsers = ratings.size();
		if(totalUsers != 0) {
			entity.setTotalUsers(totalUsers);
			entity.setRatingValueDouble((double)sumRating / (double)totalUsers);
		}
		return entity;
	}

	/**
	 * Vrati zoznam najaktivnejsich userov, v bean-e je iba userId a ratingStat
	 * @param users - pocet userov v TOP-liste
	 * @return
	 */
	public static List<RatingEntity> getUsersTopList(int users)
	{
		return(getUsersTopList(users, -1));
	}

	/**
	 * Vrati zoznam najaktivnejsich userov, v bean-e je iba userId a ratingStat
	 * @param users - pocet userov v TOP-liste
	 * @param period - pocet dni, od aktualneho datumu spat
	 * @return
	 */
	public static List<RatingEntity> getUsersTopList(int users, int period) {
		List<RatingEntity> retRatings = new ArrayList<>();
		long fromDate;
		long dayInMiliSec;

		if (period < 1) //zobereme cely rok
			period = 365;

		dayInMiliSec = 24L * 60L * 60L * 1000L;
		fromDate = Tools.getNow() - (period * dayInMiliSec);

		StringBuilder allreadyHasUserId = new StringBuilder("-1");
		String sql = "SELECT user_id, COUNT(user_id) AS clicks FROM rating WHERE insert_date >= ? GROUP BY user_id ORDER BY clicks DESC";
		new ComplexQuery().setSql(sql).setParams( new Timestamp(fromDate) ).list(new Mapper<RatingEntity>() {
			@Override
			public RatingEntity map(ResultSet rs) throws SQLException {
				RatingEntity rating = new RatingEntity();
				rating.setUserId( rs.getInt("user_id") );
				rating.setRatingStat( rs.getInt("clicks") );
				retRatings.add(rating);
				allreadyHasUserId.append(',').append( rating.getUserId() );

				return null;
			}
		});

		if (retRatings.size() < users && users > 0) {
			sql = "SELECT user_id, COUNT(user_id) AS clicks FROM rating WHERE user_id NOT IN (" + allreadyHasUserId.toString() + ") GROUP BY user_id ORDER BY clicks DESC";
			new ComplexQuery().setSql(sql).list(new Mapper<RatingEntity>() {
				@Override
				public RatingEntity map(ResultSet rs) throws SQLException {
					RatingEntity rating = new RatingEntity();
					rating.setUserId( rs.getInt("user_id") );
					rating.setRatingStat( rs.getInt("clicks") );
					retRatings.add(rating);

					return null;
				}
			});
		}

		return retRatings;
	}

	/**
	 * Vrati zoznam TOP doc ID, v bean-e je iba docId a ratingStat
	 * @param length - pocet docId v TOP-liste
	 * @param period - pocet dni, od aktualneho datumu spat
	 * @return
	 */
	public static List<RatingEntity> getDocIdTopList(int length, int period) {
		List<RatingEntity> retRatings = new ArrayList<>();
		DocDB docDB = DocDB.getInstance();
		long dayInMiliSec = 24L * 60L * 60L * 1000L;
		long fromDate = Tools.getNow() - (period * dayInMiliSec);

		StringBuilder allreadyHasDocId = new StringBuilder("-1");
		String sql = "SELECT doc_id, COUNT(doc_id) AS clicks FROM rating WHERE insert_date >= ? GROUP BY doc_id ORDER BY clicks DESC";
		new ComplexQuery().setSql(sql).setParams( new Timestamp(fromDate) ).list(new Mapper<RatingEntity>() {
			@Override
			public RatingEntity map(ResultSet rs) throws SQLException {
				RatingEntity rating = new RatingEntity();
				rating.setDocId( rs.getInt("doc_id") );

				DocDetails testDoc = docDB.getBasicDocDetails(rating.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable() == false) return null;

				rating.setRatingStat( rs.getInt("clicks") );
				rating.setRatingValueDouble( getDocIdRating( rs.getInt("doc_id") ).getRatingValueDouble() );
				retRatings.add(rating);

				allreadyHasDocId.append(',').append(rating.getDocId());

				return null;
			}
		});

		if (retRatings.size() < length && length > 0) {
			//doplnime za cele obdobie
			sql = "SELECT doc_id, COUNT(doc_id) AS clicks FROM rating WHERE doc_id NOT IN (" + allreadyHasDocId.toString() + ") GROUP BY doc_id ORDER BY clicks DESC";
			new ComplexQuery().setSql(sql).list(new Mapper<RatingEntity>() {
				@Override
				public RatingEntity map(ResultSet rs) throws SQLException {
					RatingEntity rating = new RatingEntity();
					rating.setDocId( rs.getInt("doc_id") );

					DocDetails testDoc = docDB.getBasicDocDetails(rating.getDocId(), false);
					if (testDoc == null || testDoc.isAvailable() == false) return null;

					rating.setRatingStat( rs.getInt("clicks") );
					rating.setRatingValueDouble( getDocIdRating( rs.getInt("doc_id") ).getRatingValueDouble() );
					retRatings.add(rating);

					allreadyHasDocId.append(',').append(rating.getDocId());

					return null;
				}
			});
		}

		return retRatings;
	}

	/**
	 * Zobrazi zoznam clankov zoradenych podla vysky RATING-u.
	 * Ak existuju clanky s rovnakym ratingom, zoradi ich podla poctu hlasujucich citatelov.
	 * @param docsLength - pocet zobrazenych clankov
	 * @param period - pocet dni dozadu za ktore sa statistika berie
	 * @param minUsers - minimalny pocet citatelov, ktory hodnotili clanok
	 * @param groupIds - id adresara v ktorom sa sledovane clanky nachadzaju
	 * @param includeSubGroups - ak je true, beru sa aj podadresare
	 * @param doubleSort - ak je true sortuje sa presne, ak je false, tak zaokruhlene na celu hodnotu
	 * @return
	 */
	public static List<RatingEntity> getTopPages(int docsLength, int period, int minUsers, String groupIds, boolean includeSubGroups, boolean doubleSort) {
		List<RatingEntity> topPages = new ArrayList<>();
		DocDB docDB = DocDB.getInstance();
		List<RatingEntity> ratings = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -period);

		DebugTimer dt = new DebugTimer("RatingService.getTopPages");

		//priprav si hashtabulku povolenych adresarov
		Map<Integer, Integer> availableGroups = new Hashtable<>();
		boolean doGroupsCheck = false;
		GroupsDB groupsDB = GroupsDB.getInstance();

		if (Tools.isNotEmpty(groupIds)) {
			StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
			while (st.hasMoreTokens()) {
				int groupId = Tools.getIntValue(st.nextToken(), 0);
				if (groupId > 0) {
					doGroupsCheck = true;
					availableGroups.put(groupId, 1);
					if (includeSubGroups) {
						//najdi child grupy
						for (GroupDetails group : groupsDB.getGroupsTree(groupId, false, false)) {
							if (group != null && group.isInternal() == false)
								availableGroups.put(group.getGroupId(), 1);
						}
					}
				}
			}
		}

		dt.diff("availableGroups=" + availableGroups.size());

		String sql = "SELECT doc_id, COUNT(doc_id) AS clicks, SUM(rating_value) AS ratingSum FROM rating WHERE insert_date >= ? GROUP BY doc_id";

		Logger.debug(RatingService.class, "sql:" + sql);
		Logger.debug(RatingService.class, "fromDate:" + Tools.formatDate(cal.getTime()));

		final boolean doGroupsCheckFinal = doGroupsCheck;
		new ComplexQuery().setSql(sql).setParams( new Timestamp(cal.getTimeInMillis()) ).list(new Mapper<RatingEntity>() {
			@Override
			public RatingEntity map(ResultSet rs) throws SQLException {
				RatingEntity rating = new RatingEntity();
				rating.setDocId( rs.getInt("doc_id") );

				DocDetails testDoc = docDB.getBasicDocDetails(rating.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable() == false) return null;

				//kontrola na groupids
				if (doGroupsCheckFinal)
					if (availableGroups.get(testDoc.getGroupId()) == null) return null;

				rating.setDocTitle( testDoc.getTitle() );
				rating.setRatingStat( rs.getInt("clicks") );
				rating.setTotalUsers( rating.getRatingStat() );

				if (minUsers > 0 && rating.getTotalUsers() < minUsers) return null;

				rating.setTotalSum( rs.getInt("ratingSum") );
				rating.setRatingValueDouble((double)rating.getTotalSum() / (double)rating.getTotalUsers());
				ratings.add(rating);

				return null;
			}
		});

		dt.diff("sorting");

		//usortuj to podla rating value a nazvu
		if (doubleSort) {
			Collections.sort(ratings, new Comparator<RatingEntity>() {
				@Override
				public int compare(RatingEntity r1, RatingEntity r2) {
					if (r1.getRatingValueDouble() == r2.getRatingValueDouble())
						return r1.getDocTitle().compareTo(r2.getDocTitle());

					return Double.compare(r2.getRatingValueDouble(), r1.getRatingValueDouble());

				}
			});
		} else {
			Collections.sort(ratings, new Comparator<RatingEntity>() {
				@Override
				public int compare(RatingEntity r1, RatingEntity r2) {
					if (r1.getRatingValue() == r2.getRatingValue())
						return r1.getDocTitle().compareTo(r2.getDocTitle());

					return r2.getRatingValue() - r1.getRatingValue();
				}
			});
		}

		//sprav z toho skrateny zoznam
		if (docsLength < 1) topPages = ratings;
		else{
			for (int i=0; (i < docsLength && i < ratings.size()); i++)
				topPages.add( ratings.get(i) );
		}

		dt.diff("done, size: " + topPages.size());

		return topPages;
	}

}
