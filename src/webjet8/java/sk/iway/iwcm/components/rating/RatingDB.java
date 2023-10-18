package sk.iway.iwcm.components.rating;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.forum.ForumDB;

/**
 *  RatingDB - hodnotenie stranok
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.20 $
 *@created      $Date: 2010/01/20 08:41:45 $
 *@modified     $Date: 2010/01/20 08:41:45 $
 */
public class RatingDB
{

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
						RatingBean rDB = getDocIdRating(dc.getDocId());
						int docIdRating = rDB.getRatingValue();
						ForumDB.setForumCountForDocIds(dc.getDocId(), docIdRating );
						//Logger.debug(RefreshRating.class,"Refreshed : DocID: "+dc.getDocId()+" rating: "+docIdRating);
						count++;
					}
				}
			}
		Logger.debug(RatingDB.class, "Rating of page was Refreshed for "+count+" pages.");
		}
	}

	/**
	 * Vrati zoznam ratingov
	 * @return
	 */
	public static List<RatingBean> getRatings()
	{
		List<RatingBean> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM rating ORDER BY rating_id ASC";

			//Logger.println(this,"sql:"+sql);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			RatingBean rBean;
			while (rs.next())
			{
				rBean = new RatingBean();
				rBean.setRatingId(rs.getInt("rating_id"));
				rBean.setDocId(rs.getInt("doc_id"));
				rBean.setUserId(rs.getInt("user_id"));
				rBean.setRatingValueDouble(rs.getInt("rating_value"));
				rBean.setInsertDate(Tools.formatDate(rs.getTimestamp("insert_date")));
				rBean.setInsertTime(Tools.formatTime(rs.getTimestamp("insert_date")));
				ret.add(rBean);
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
			sk.iway.iwcm.Logger.error(ex);
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
			}
		}

		return(ret);
	}


	/**
	 * Ulozi rating do databazy a vrati TRUE, ak je vsetko OK
	 * @param rBean - rating, ktory sa ulozi do DB
	 * @return
	 */
	public static boolean saveRating(RatingBean rBean)
	{
		boolean ret = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			String sql = "INSERT INTO rating (doc_id, user_id, rating_value, insert_date) VALUES ( ?, ?, ?, ?)";

			if (rBean.getRatingId()>0)
			{
				sql = "UPDATE rating SET " +
				"doc_id=?, user_id=?, rating_value=?, insert_date=? WHERE rating_id=?";
			}

			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, rBean.getDocId());
			ps.setInt(2, rBean.getUserId());
			ps.setInt(3, rBean.getRatingValue());
			ps.setTimestamp(4, new Timestamp(Tools.getNow()));

			if (rBean.getRatingId()>0)
			{
				ps.setInt(5, rBean.getRatingId());
			}
			ps.execute();
			ps.close();

			if (rBean.getRatingId()<1)
			{
				ps = db_conn.prepareStatement("SELECT max(rating_id) AS rating_id FROM rating");
				rs = ps.executeQuery();
				if (rs.next())
				{
					rBean.setRatingId(rs.getInt("rating_id"));
				}
				rs.close();
				ps.close();
			}
			rs = null;
			ps = null;

			ret = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ret);
	}


	/**
	 * Vymaze rating z DB
	 * @return
	 */
	public static boolean deleteRating(int ratingId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		boolean ret = false;
		try
		{
			Logger.println(RatingDB.class,"DELETE: ratingId= " +ratingId);
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM rating WHERE rating_id=?");
			ps.setInt(1, ratingId);

			int update = ps.executeUpdate();


			Logger.println(RatingDB.class,"Number of DELETED rows= " +update+ " from rating");


			if(update != 0) ret = true;
			ps.close();
			ps = null;

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
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
			}
		}
		return (ret);
	}


	/**
	 * Vrati rating podla ratingID
	 * @param ratingId - ID ratingu v DB
	 * @return
	 */
	public static RatingBean getRatingById(int ratingId)
	{
		RatingBean rBean = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM rating WHERE rating_id=?";

			//Logger.println(this,"sql:"+sql);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, ratingId);
			rs = ps.executeQuery();

			if (rs.next())
			{
				rBean = new RatingBean();
				rBean.setRatingId(rs.getInt("rating_id"));
				rBean.setDocId(rs.getInt("doc_id"));
				rBean.setUserId(rs.getInt("user_id"));
				rBean.setRatingValueDouble(rs.getInt("rating_value"));
				rBean.setInsertDate(Tools.formatDate(rs.getTimestamp("insert_date")));
				rBean.setInsertTime(Tools.formatTime(rs.getTimestamp("insert_date")));
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
			sk.iway.iwcm.Logger.error(ex);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return(rBean);
	}


	/**
	 * Vrati vsetky ratingy usera
	 * @param userId - ID uzivatela v DB
	 * @return
	 */
	public static List<RatingBean> getRatingsByUser(int userId)
	{
		List<RatingBean> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM rating WHERE user_id=? ORDER BY rating_id ASC";

			//Logger.println(this,"sql:"+sql);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			RatingBean rBean;
			while (rs.next())
			{
				rBean = new RatingBean();
				rBean.setRatingId(rs.getInt("rating_id"));
				rBean.setDocId(rs.getInt("doc_id"));
				rBean.setUserId(rs.getInt("user_id"));
				rBean.setRatingValueDouble(rs.getInt("rating_value"));
				rBean.setInsertDate(Tools.formatDate(rs.getTimestamp("insert_date")));
				rBean.setInsertTime(Tools.formatTime(rs.getTimestamp("insert_date")));
				ret.add(rBean);
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
			sk.iway.iwcm.Logger.error(ex);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ret);
	}

	public static List<RatingBean> getRatingsByDoc(int docId)
	{
		List<RatingBean> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM rating WHERE doc_id=?"; //spomalovalo to vykon na 7plus ORDER BY rating_id ASC";

			//Logger.println(this,"sql:"+sql);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, docId);
			rs = ps.executeQuery();
			RatingBean rBean;
			while (rs.next())
			{
				try
				{
					rBean = new RatingBean();
					rBean.setRatingId(rs.getInt("rating_id"));
					rBean.setDocId(rs.getInt("doc_id"));
					rBean.setUserId(rs.getInt("user_id"));
					rBean.setRatingValueDouble(rs.getInt("rating_value"));
					rBean.setInsertDate(Tools.formatDate(rs.getTimestamp("insert_date")));
					rBean.setInsertTime(Tools.formatTime(rs.getTimestamp("insert_date")));
					ret.add(rBean);
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
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
			sk.iway.iwcm.Logger.error(ex);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ret);
	}


	/**
	 * Vrati RatingBean, ak user este nehlasoval za docId, vrati NULL
	 * @param userId - ID uzivatela v DB
	 * @param docId - ID stranky, za ktoru sa hlasuje
	 * @return
	 */
	public static RatingBean getRatingByUserByDoc(int userId, int docId)
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
	public static RatingBean getRatingByUserByDoc(int userId, int docId, int rateAgainCycleInHours)
	{
		RatingBean rBean = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "";
			Calendar checkTime = null;
			if(rateAgainCycleInHours > 0)
			{
				checkTime = Calendar.getInstance();
				checkTime.add(Calendar.HOUR, -rateAgainCycleInHours);

				sql = "SELECT * FROM rating WHERE user_id=? AND doc_id=? AND insert_date >= ? ORDER BY insert_date DESC";

			}
			else
				sql = "SELECT * FROM rating WHERE user_id=? AND doc_id=? ORDER BY insert_date DESC";

			//Logger.println(this,"sql:"+sql);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, userId);
			ps.setInt(2, docId);
			if(rateAgainCycleInHours > 0 && checkTime!=null)
				ps.setTimestamp(3, new Timestamp(checkTime.getTimeInMillis()));
			rs = ps.executeQuery();

			if (rs.next())
			{
				rBean = new RatingBean();
				rBean.setRatingId(rs.getInt("rating_id"));
				rBean.setDocId(rs.getInt("doc_id"));
				rBean.setUserId(rs.getInt("user_id"));
				rBean.setRatingValueDouble(rs.getInt("rating_value"));
				rBean.setInsertDate(Tools.formatDate(rs.getTimestamp("insert_date")));
				rBean.setInsertTime(Tools.formatTime(rs.getTimestamp("insert_date")));
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
			sk.iway.iwcm.Logger.error(ex);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(rBean);
	}




	/**
	 * Vrati pocet zaznamov pre zvolene kriterium, ak za krit. nezada, vrati pocet vsetkych
	 * @param colName - nazov stlpca, nad ktorym sa vykona COUNT
	 * @param whereCol - nazov stlpca, ktory je kriteriom pre WHERE (typ int), ak je NULL, WHERE sa nevykona
	 * @param whereValue - int hodnota pre WHERE
	 * @return
	 */
	public static int countCol(String countCol, String whereCol, int whereValue)
	{
		int ret = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (countCol != null)
			{
				String sql = "SELECT COUNT("+countCol+") AS countCols FROM rating";

				if  (whereCol != null)
				{
					sql += " WHERE "+whereCol+"=?";

				}
				//Logger.println(this,"sql:"+sql);

				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);

				if  (whereCol != null)
				{
					ps.setInt(1, whereValue);
				}

				rs = ps.executeQuery();
				if (rs.next())
				{
					ret = rs.getInt("countCols");
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ret);
	}


	/**
	 * Pre zadane docId vrati ratingBean s vypocitanym ratingom a poctom userov
	 * @param docId - doc ID v DB
	 * @return
	 */
	public static RatingBean getDocIdRating(int docId)
	{
		RatingBean rBean = new RatingBean();
		int totalUsers;
		int sumRating;
		List<RatingBean> ratings;

		try
		{
			sumRating = 0;
			ratings = getRatingsByDoc(docId);

			for (RatingBean rB : ratings)
			{
				sumRating += rB.getRatingValue();
			}

			totalUsers = ratings.size();
			if (totalUsers != 0)
			{
				rBean.setTotalUsers(totalUsers);
				rBean.setRatingValueDouble((double)sumRating/(double)totalUsers);

			}
			//Logger.println(this,"docId="+docId+"\tsumRating="+sumRating+"\ttotalUsers="+totalUsers+"\tratingValue="+rBean.getRatingValue());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(rBean);
	}


	/**
	 * Vrati zoznam najaktivnejsich userov, v bean-e je iba userId a ratingStat
	 * @param users - pocet userov v TOP-liste
	 * @return
	 */
	public static List<RatingBean> getUsersTopList(int users)
	{
		return(getUsersTopList(users, -1));
	}

	/**
	 * Vrati zoznam najaktivnejsich userov, v bean-e je iba userId a ratingStat
	 * @param users - pocet userov v TOP-liste
	 * @param period - pocet dni, od aktualneho datumu spat
	 * @return
	 */
	public static List<RatingBean> getUsersTopList(int users, int period)
	{
		List<RatingBean> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		RatingBean rBean;

		long fromDate;
		long dayInMiliSec;

		try
		{
			if (period < 1)
			{
				//zobereme cely rok
				period = 365;
			}

			dayInMiliSec = 24L * 60L * 60L * 1000L;
			fromDate = Tools.getNow() - (period * dayInMiliSec);

			String sql = "SELECT user_id, COUNT(user_id) AS clicks FROM rating WHERE insert_date >= ? GROUP BY user_id ORDER BY clicks DESC";

			//Logger.println(this,"sql:"+sql);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setTimestamp(1, new Timestamp(fromDate));
			rs = ps.executeQuery();
			StringBuilder allreadyHasUserId = new StringBuilder("-1");
			while (rs.next() && ret.size() < users)
			{
				rBean = new RatingBean();
				rBean.setUserId(rs.getInt("user_id"));
				rBean.setRatingStat(rs.getInt("clicks"));
				ret.add(rBean);
				allreadyHasUserId.append(',').append(rBean.getUserId());
			}
			rs.close();
			ps.close();

			if (ret.size() < users && users > 0)
			{
				sql = "SELECT user_id, COUNT(user_id) AS clicks FROM rating WHERE user_id NOT IN ("+allreadyHasUserId.toString()+") GROUP BY user_id ORDER BY clicks DESC";
				ps = db_conn.prepareStatement(sql);
				rs = ps.executeQuery();

				while (rs.next() && ret.size() < users)
				{
					rBean = new RatingBean();
					rBean.setUserId(rs.getInt("user_id"));
					rBean.setRatingStat(rs.getInt("clicks"));
					ret.add(rBean);
				}
				rs.close();
				ps.close();
			}

			rs = null;
			ps = null;

			db_conn.close();
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ret);
	}


	/**
	 * Vrati zoznam TOP doc ID, v bean-e je iba docId a ratingStat
	 * @param length - pocet docId v TOP-liste
	 * @param period - pocet dni, od aktualneho datumu spat
	 * @return
	 */
	public static List<RatingBean> getDocIdTopList(int length, int period)
	{
		List<RatingBean> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		RatingBean rBean;
		long fromDate;
		long dayInMiliSec;

		try
		{
			dayInMiliSec = 24L * 60L * 60L * 1000L;
			fromDate = Tools.getNow() - (period * dayInMiliSec);

			String sql = "SELECT doc_id, COUNT(doc_id) AS clicks FROM rating WHERE insert_date >= ? GROUP BY doc_id ORDER BY clicks DESC";

			//Logger.println(this,"sql:"+sql);
			//Logger.println(this,"fromDate:"+fromDate+"\tNOW: "+Tools.getNow());

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setTimestamp(1, new Timestamp(fromDate));
			rs = ps.executeQuery();

			StringBuilder allreadyHasDocId = new StringBuilder("-1");
			DocDetails testDoc;
			DocDB docDB = DocDB.getInstance();
			while (rs.next() && ret.size() < length)
			{
				rBean = new RatingBean();
				rBean.setDocId(rs.getInt("doc_id"));

				testDoc = docDB.getBasicDocDetails(rBean.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable()==false) continue;

				rBean.setRatingStat(rs.getInt("clicks"));
				rBean.setRatingValueDouble(getDocIdRating(rs.getInt("doc_id")).getRatingValueDouble());
				ret.add(rBean);

				allreadyHasDocId.append(',').append(rBean.getDocId());
			}
			rs.close();
			ps.close();

			if (ret.size() < length && length > 0)
			{
				//doplnime za cele obdobie
				sql = "SELECT doc_id, COUNT(doc_id) AS clicks FROM rating WHERE doc_id NOT IN ("+allreadyHasDocId.toString()+") GROUP BY doc_id ORDER BY clicks DESC";
				ps = db_conn.prepareStatement(sql);
				rs = ps.executeQuery();

				while (rs.next() && ret.size() < length)
				{
					rBean = new RatingBean();
					rBean.setDocId(rs.getInt("doc_id"));

					testDoc = docDB.getBasicDocDetails(rBean.getDocId(), false);
					if (testDoc == null || testDoc.isAvailable()==false) continue;

					rBean.setRatingStat(rs.getInt("clicks"));
					rBean.setRatingValueDouble(getDocIdRating(rs.getInt("doc_id")).getRatingValueDouble());
					ret.add(rBean);
				}

				rs.close();
				ps.close();
			}

			rs = null;
			ps = null;


		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ret);
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
	public static List<RatingBean> getTopPages(int docsLength, int period, int minUsers, String groupIds, boolean includeSubGroups, boolean doubleSort)
	{
		List<RatingBean> topPages = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		RatingBean rBean;

		try
		{
			DebugTimer dt = new DebugTimer("RatingDB.getTopPages");


			//priprav si hashtabulku povolenych adresarov
			Map<Integer, Integer> availableGroups = new Hashtable<>();
			boolean doGroupsCheck = false;
			GroupsDB groupsDB = GroupsDB.getInstance();
			if (Tools.isNotEmpty(groupIds))
			{
				StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
				while (st.hasMoreTokens())
				{
					int groupId = Tools.getIntValue(st.nextToken(), 0);
					if (groupId > 0)
					{
						doGroupsCheck = true;

						availableGroups.put(groupId, 1);
						if (includeSubGroups)
						{
							//najdi child grupy
							for (GroupDetails group : groupsDB.getGroupsTree(groupId, false, false))
							{
								if (group != null && group.isInternal()==false)
								{
									availableGroups.put(group.getGroupId(), 1);
								}
							}
						}
					}
				}
			}

			dt.diff("availableGroups="+availableGroups.size());

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, -period);

			String sql = "SELECT doc_id, COUNT(doc_id) AS clicks, SUM(rating_value) AS sucet FROM rating WHERE insert_date >= ? GROUP BY doc_id";

			Logger.debug(RatingDB.class, "sql:"+sql);
			Logger.debug(RatingDB.class, "fromDate:"+Tools.formatDate(cal.getTime()));

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setTimestamp(1, new Timestamp(cal.getTimeInMillis()));
			rs = ps.executeQuery();

			DocDetails testDoc;
			DocDB docDB = DocDB.getInstance();
			List<RatingBean> list = new ArrayList<>();
			while (rs.next())
			{
				rBean = new RatingBean();
				rBean.setDocId(rs.getInt("doc_id"));

				testDoc = docDB.getBasicDocDetails(rBean.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable()==false) continue;

				//kontrola na groupids
				if (doGroupsCheck)
				{
					if (availableGroups.get(testDoc.getGroupId())==null) continue;
				}

				rBean.setDocTitle(testDoc.getTitle());
				rBean.setRatingStat(rs.getInt("clicks"));
				rBean.setTotalUsers(rBean.getRatingStat());

				if (minUsers>0 && rBean.getTotalUsers()<minUsers) continue;

				rBean.setTotalSum(rs.getInt("sucet"));
				rBean.setRatingValueDouble((double)rBean.getTotalSum()/(double)rBean.getTotalUsers());
				//rBean.setRatingValue((int)Math.round(rBean.getRatingValueDouble()));
				list.add(rBean);
			}
			rs.close();
			ps.close();

			rs = null;
			ps = null;

			dt.diff("sorting");

			//usortuj to podla rating value a nazvu
			if (doubleSort)
			{
				Collections.sort(list, new Comparator<RatingBean>() {
					@Override
					public int compare(RatingBean r1, RatingBean r2)
					{
						if (r1.getRatingValueDouble()==r2.getRatingValueDouble())
						{
							return r1.getDocTitle().compareTo(r2.getDocTitle());
						}
						return Double.compare(r2.getRatingValueDouble(), r1.getRatingValueDouble());

					}

				});
			}
			else
			{
				Collections.sort(list, new Comparator<RatingBean>() {
					@Override
					public int compare(RatingBean r1, RatingBean r2)
					{
						if (r1.getRatingValue()==r2.getRatingValue())
						{
							return r1.getDocTitle().compareTo(r2.getDocTitle());
						}

						return r2.getRatingValue() - r1.getRatingValue();
					}

				});
			}

			//sprav z toho skrateny zoznam
			if (docsLength<1) topPages = list;
			else
			{
				for (int i=0; (i<docsLength && i<list.size()); i++)
				{
					topPages.add(list.get(i));
				}
			}

			dt.diff("done, size: "+topPages.size());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(topPages);
	}

	/**
	 * Vrati zoznam hodnoteni, ktore vykonal user v tabulke title, datum_hodnotenia, tvoje_hodnotenie, priemerne_hodnotenie
	 * @param userId
	 * @param
	 * @return
	 */
	public static List<RatingBean> getUserRatings(int userId)
	{
		List<RatingBean> ratings = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ten insert_date ASC je tam kvoli tomu, ze ak by nahodou bolo viacnasobne hodnotenie hry (zobrazime len posledny)
			String sql = "SELECT r.*, d.title FROM rating r, documents d " +
							"WHERE r.doc_id=d.doc_id AND r.user_id=? " +
							"ORDER BY title, insert_date DESC ";

			StringBuilder docIds = null;

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			int docId;
			boolean allreadyHas = false;
			while (rs.next())
			{
				allreadyHas = false;
				docId = rs.getInt("doc_id");
				if (docIds == null)
				{
					docIds = new StringBuilder(Integer.toString(docId));
				}
				else
				{
					if ((","+docIds.toString()+",").indexOf(","+docId+",")!=-1)
					{
						allreadyHas = true;
						continue;
					}
					docIds.append(",").append(docId);
				}

				if (!allreadyHas)
				{
					RatingBean r = new RatingBean();
					r.setRatingId(rs.getInt("rating_id"));
					r.setDocId(docId);
					r.setUserId(userId);
					r.setRatingValueDouble(rs.getInt("rating_value"));
					r.setInsertDate(DB.getDbDate(rs, "insert_date"));
					r.setInsertTime(DB.getDbTime(rs, "insert_date"));
					r.setDocTitle(DB.getDbString(rs, "title"));

					ratings.add(r);
				}
			}
			rs.close();
			ps.close();

			if (docIds == null) return ratings;

			sql = "SELECT doc_id, sum(r.rating_value) as suma, count(r.rating_value) as pocet " +
					"FROM rating r " +
					"WHERE r.doc_id IN ("+docIds.toString()+") " +
					"GROUP BY doc_id ";

			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next())
			{
				docId = rs.getInt("doc_id");

				//najdi rating
				for (RatingBean r : ratings)
				{
					if (r.getDocId() == docId)
					{
						int hodnotenie = rs.getInt("suma") / rs.getInt("pocet");
						r.setRatingStat(hodnotenie);

						break;
					}
				}
			}
			rs.close();
			ps.close();

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(ratings);
	}
}
