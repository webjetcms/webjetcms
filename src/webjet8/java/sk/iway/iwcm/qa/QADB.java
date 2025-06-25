package sk.iway.iwcm.qa;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.captcha.Captcha;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;

/**
 *  praca z databazou otazok a odpovedi
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Piatok, 2003, januďż˝r 17
 *@modified     $Date: 2004/02/25 22:09:55 $
 */
public class QADB
{
	public static final int ORDER_DATE = 1;
	public static final int ORDER_PRIORITY = 2;
	private static final Random rand = new Random();

	protected QADB() {
		//utility class
	}

	public static List<QABean> getQAList(String groupName, boolean onlyForWeb, boolean ascending, HttpServletRequest request)
	{
		return(getQAList(groupName, onlyForWeb, ORDER_DATE, ascending, -1, -1, request));
	}


	public static List<QABean> getQAList(String groupName, boolean onlyForWeb, int orderType, boolean ascending, int startPage, int pageSize, HttpServletRequest request)
	{
		return(getQAList(groupName, onlyForWeb, orderType, ascending, startPage, pageSize, request, null, null));
	}

	public static List<QABean> getQAList(String groupName, boolean onlyForWeb, int orderType, boolean ascending, int startPage, int pageSize, HttpServletRequest request, String categoryName, String fullTextSearchString)
	{
		List<QABean> dataAll = getQAList(groupName, onlyForWeb, orderType, ascending, request, categoryName, fullTextSearchString);
		List<QABean> data = new ArrayList<>();

		//vyparsuj iba pozadovanu stranku
		if (pageSize > -1 && pageSize < 2)
		{
			pageSize = 2;
		}
		int start = (startPage - 1) * pageSize;

		Iterator<QABean> iter = dataAll.iterator();

		if (start > 0)
		{
			int i;
			for (i=0; i<start; i++)
			{
				if (iter.hasNext()) iter.next();
			}
		}

		int counter = 0;
		while (iter.hasNext())
		{
			QABean o = iter.next();
			counter++;
			if (pageSize > 1 && counter > pageSize)
			{
				break;
			}
			data.add(o);
		}

		return data;
	}

	public static List<QABean> getQAList(String groupName, boolean onlyForWeb, int orderType, boolean ascending, HttpServletRequest request)
	{
		return(getQAList(groupName, onlyForWeb, orderType, ascending, request, null, null));
	}


	/**
	 * Vyfiltruje QA beany podla danych skupin, vrati tie tkore do danych skupin patria
	 * @param original
	 * @param groups
	 * @return
	 */
	public static List<QABean> filterQaByGroups(List<QABean> original, Collection<LabelValueDetails> groups){

		Set<String> groupsSet = new HashSet<>(groups.size());
		for(LabelValueDetails lvd : groups){
			groupsSet.add(lvd.getLabel());
		}
		List<QABean> filtered = new ArrayList<>(original.size());
		for(QABean qabean : original){
			if(groupsSet.contains(qabean.getGroupName())){
				filtered.add(qabean);
			}
		}
		return filtered;
	}

	/**
	 * Načíta skupiny otázok do listu.
	 *
	 * @return List<LabelValueDetails> so zoznamom unikátnych skupín otázok
	 */
	public static List<LabelValueDetails> getQAGroups(HttpServletRequest request) {
		List<LabelValueDetails> aList = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement("SELECT DISTINCT group_name FROM questions_answers WHERE " + CloudToolsForCore.getDomainIdSqlWhere(false) + " ORDER BY group_name ASC");
			rs = ps.executeQuery();

			while (rs.next()) {
				LabelValueDetails lvd = new LabelValueDetails();
				lvd.setLabel(DB.getDbString(rs, "group_name"));
				lvd.setValue(DB.getDbString(rs, "group_name"));
				aList.add(lvd);
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			} catch (Exception ex2) {
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return aList;
	}

	/**
	 * Vrati zoznam otazok a odpovedi v danej skupine
	 * @param groupName - nazov skupiny
	 * @param onlyForWeb - ak true, iba tie co su urcene na web
	 * @param orderType - sposob usporiadania
	 * @param ascending - ak true, tak usporiada ASC
	 * @param request
	 * @param categoryName - kategoria
	 * @param fullTextSearchString - tento text hlada v question a answer
	 * @return - zoznam otazok a odpovedi
	 */
	public static List<QABean> getQAList(final String groupName, boolean onlyForWeb, int orderType, boolean ascending, HttpServletRequest request, String categoryName, String fullTextSearchString)
	{
	    String groupNameFixed = Tools.replace(groupName, "&ndash;", "\u2013");

		List<QABean> ret = new ArrayList<>();
		String order = "question_date";

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);

			switch(orderType){
				case 1 : {order = "question_date";break;}
				case 2 : {order = "sort_priority";break;}
				default : order = "question_date";
			}

			if (ascending)
			{
				order += " ASC";
			}
			else
			{
				order += " DESC";
			}

			StringBuilder sql = new StringBuilder("SELECT * FROM questions_answers WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false));
			if (Tools.isNotEmpty(groupNameFixed))
			{
				sql.append("AND group_name=?");
			}
			if (onlyForWeb)
			{
				sql.append(" AND publish_on_web=? AND allow_publish_on_web=?");
			}
			if(categoryName != null)
			{
				sql.append(" AND category_name LIKE ?");
			}
			if(fullTextSearchString != null)
			{
				sql.append(" AND (question LIKE ? OR answer LIKE ?)");
			}
			sql.append(" ORDER BY ").append(order);

			/*if (pageSize > -1 && pageSize < 2)
			{
				pageSize = 2;
			}
			int start = (startPage - 1) * pageSize;*/

			ps = db_conn.prepareStatement(sql.toString());
			int j = 1;
			if (Tools.isNotEmpty(groupNameFixed))
			{
				ps.setString(j++, groupNameFixed);
			}
			if (onlyForWeb)
			{
				ps.setBoolean(j++, true);
				ps.setBoolean(j++, true);
			}
			if(categoryName != null)
			{
				ps.setString(j++, "%"+categoryName+"%");
			}
			if(fullTextSearchString != null)
			{
				ps.setString(j++, "%"+fullTextSearchString+"%");
				ps.setString(j++, "%"+fullTextSearchString+"%");
			}
			rs = ps.executeQuery();
			/*if (start > 0)
			{
				int i;
				for (i=0; i<start; i++)
				{
					//jTDS nevie rs.absolute...
				   //rs.absolute(start);
					rs.next();
				}
			}*/
			QABean qa;
			while (rs.next())
			{
				/*if (pageSize > 1 && counter > pageSize)
				{
					break;
				}*/

				qa = fillFromResultSet(rs, false);

				qa.setAnswerWeb(Tools.replace(qa.getAnswerWeb(), "\n", "<br>"));

				qa.setSortPriority(Tools.getIntValue(rs.getInt("sort_priority"), 0));

				ret.add(qa);
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
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return (ret);
	}

	/**
	 * Vrati pocet otazok a odpovedi v danej skupine
	 * @param groupName - nazov skupiny
	 * @param onlyForWeb - ak true spocita len tie, ktore su urcene na web
	 * @param request
	 * @return
	 */
	public static int getQAListSize(String groupName, boolean onlyForWeb, HttpServletRequest request)
	{
		return(getQAListSize(groupName, onlyForWeb, request, null, null));
	}

	/**
	 * Vrati pocet otazok a odpovedi v danej skupine
	 * @param groupName - nazov skupiny
	 * @param onlyForWeb - ak true spocita len tie, ktore su urcene na web
	 * @param request
	 * @param categoryName - kategoria
	 * @param fullTextSearchString - tento text hlada v question a answer
	 * @return
	 */
	public static int getQAListSize(String groupName, boolean onlyForWeb, HttpServletRequest request, String categoryName, String fullTextSearchString)
	{
		int size = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			StringBuilder sql = new StringBuilder("SELECT count(qa_id) as total_size FROM questions_answers WHERE group_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			if (onlyForWeb)
			{
				sql.append(" AND publish_on_web=?");
			}
			if(categoryName != null)
			{
				sql.append(" AND category_name LIKE ?");
			}
			if(fullTextSearchString != null)
			{
				sql.append(" AND (question LIKE ? OR answer LIKE ?)");
			}
			ps = db_conn.prepareStatement(sql.toString());

			int j = 1;
			ps.setString(j++, groupName);
			if (onlyForWeb)
			{
				ps.setBoolean(j++, true);
			}
			if(categoryName != null)
			{
				ps.setString(j++, "%"+categoryName+"%");
			}
			if(fullTextSearchString != null)
			{
				ps.setString(j++, "%"+fullTextSearchString+"%");
				ps.setString(j++, "%"+fullTextSearchString+"%");
			}

			rs = ps.executeQuery();
			if (rs.next())
			{
				size = rs.getInt("total_size");
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

		return (size);
	}

	public static List<LabelValueDetails> getQARoots(HttpServletRequest request)
	{
		List<LabelValueDetails> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("SELECT count(qa_id) as qa_count, group_name FROM questions_answers WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false)+" GROUP BY group_name");
			rs = ps.executeQuery();
			LabelValueDetails lvd;
			while (rs.next())
			{
				lvd = new LabelValueDetails();
				lvd.setLabel(ResponseUtils.filter(DB.getDbString(rs, "group_name")));
				lvd.setValue(Integer.toString(rs.getInt("qa_count")));
				ret.add(lvd);
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

		return (ret);
	}

	public static QABean getQAById(int questionId)
	{
		QABean qa = null;

		Connection db_conn = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM questions_answers WHERE qa_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY qa_id ASC");
			ps.setInt(1, questionId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				qa = fillFromResultSet(rs, true);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (db_conn != null) db_conn.close();
			}catch(SQLException e){}
		}

		return (qa);
	}

	/**
	 * Ziska QABean pre admin cast (zadanie odpovede)
	 * @param request
	 * @return
	 */
	public static QABean getQA(HttpServletRequest request)
	{
		QABean qa = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			int qaId = Integer.parseInt(request.getParameter("id"));
			String hash = request.getParameter("hash");

			if (qaId < 0 || hash==null || hash.length()<5)
			{
				return(null);
			}

			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("SELECT * FROM questions_answers WHERE qa_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY qa_id ASC");
			ps.setInt(1, qaId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				qa = fillFromResultSet(rs, false);

				if (!qa.getHash().equals(hash))
				{
					qa = null;
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
			}
		}

		return (qa);
	}


	private static QABean fillFromResultSet(ResultSet rs, boolean filter) throws SQLException {
		QABean qa;
		qa = new QABean();
		qa.setRecode(false);
		qa.setQaId(rs.getInt("qa_id"));
		qa.setGroupName(DB.getDbString(rs, "group_name", filter));
		qa.setCategoryName(DB.getDbString(rs, "category_name", filter));
		Timestamp t = rs.getTimestamp("question_date");
		if (t!=null) qa.setQuestionDateLong(t.getTime());
		t = rs.getTimestamp("answer_date");
		if (t!=null) qa.setAnswerDateLong(t.getTime());
		qa.setQuestion(DB.getDbString(rs, "question", filter));
		qa.setAnswer(DB.getDbString(rs, "answer", filter));
		qa.setAnswerWeb(qa.getAnswer());
		qa.setFromName(DB.getDbString(rs, "from_name", filter));
		qa.setFromEmail(DB.getDbString(rs, "from_email", filter));
		qa.setToName(DB.getDbString(rs, "to_name", filter));
		qa.setToEmail(DB.getDbString(rs, "to_email", filter));
		qa.setPublishOnWeb(rs.getBoolean("publish_on_web"));
		qa.setAllowPublishOnWeb(rs.getBoolean("allow_publish_on_web"));
		qa.setHash(DB.getDbString(rs, "hash", filter));
		qa.setSortPriority(rs.getInt("sort_priority"));

		qa.setFromPhone(DB.getDbString(rs, "from_phone"));
		qa.setFromCompany(DB.getDbString(rs, "from_company"));
		qa.setFieldA(DB.getDbString(rs, "field_a"));
		qa.setFieldB(DB.getDbString(rs, "field_b"));
		qa.setFieldC(DB.getDbString(rs, "field_c"));
		qa.setFieldD(DB.getDbString(rs, "field_d"));

		qa.setRecode(true);




		return qa;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  qa       Description of the Parameter
	 *@param  request  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	public static boolean save(QABean qa, HttpServletRequest request)
	{
		Identity user = UsersDB.getCurrentUser(request);
		boolean isAdmin = false;
		if (user != null && user.isAdmin()) isAdmin = true;

		if (ShowDoc.getXssRedirectUrl(request)!=null) return false;
		if (isAdmin==false && SpamProtection.canPost("qa", qa.getQuestion(), request)==false) return false;
		if	(isAdmin==false && !Captcha.validateResponse(request, null, "qa")) return false;

		if (Tools.isAnyEmpty(qa.getGroupName(), qa.getFromName(), qa.getFromEmail()))
		{
			Logger.error(QADB.class, "CHYBA: Jedno z povinnych poli groupName, fromName, fromEmail je prazdne");
			return false; //pravdepodobne vyplnil bot a nezadal povinne udaje
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//vygeneruj 4 miestny kod
			StringBuilder hash = new StringBuilder();
			int i;
			for (i=0; i<16; i++)
			{
				hash.append(rand.nextInt(9));
			}
			qa.setHash(hash.toString());


			boolean onWeb = false;
			if(qa.isPublishOnWeb()==true)
				onWeb = true;

			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("INSERT INTO questions_answers (group_name, category_name, question_date, question, from_name, from_email, to_name, to_email, publish_on_web, hash, allow_publish_on_web, sort_priority, from_phone, from_company, field_a, field_b, field_c, field_d, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			int index = 1;
			ps.setString(index++, qa.getGroupName());
			ps.setString(index++, qa.getCategoryName());
			long now = (new java.util.Date()).getTime();
			Timestamp ts = new Timestamp(now);
			ps.setTimestamp(index++, ts);
			DB.setClob(ps, index++, qa.getQuestion());
			ps.setString(index++, qa.getFromName());
			ps.setString(index++, qa.getFromEmail());
			ps.setString(index++, qa.getToName());
			ps.setString(index++, qa.getToEmail());
			ps.setBoolean(index++, onWeb);
			ps.setString(index++, hash.toString());
			ps.setBoolean(index++, qa.isAllowPublishOnWeb());
			if (qa.getSortPriority()<1) qa.setSortPriority(getNewPriority(qa.getGroupName()));
			ps.setInt(index++, qa.getSortPriority());

			ps.setString(index++, qa.getFromPhone());
			ps.setString(index++, qa.getFromCompany());
			ps.setString(index++, qa.getFieldA());
			ps.setString(index++, qa.getFieldB());
			ps.setString(index++, qa.getFieldC());
			ps.setString(index++, qa.getFieldD());
			ps.setInt(index++, CloudToolsForCore.getDomainId());

			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("SELECT max(qa_id) as qa_id FROM questions_answers WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false));
			rs = ps.executeQuery();
			if (rs.next())
			{
				qa.setQaId(rs.getInt("qa_id"));
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
			return (false);
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

		Adminlog.add(Adminlog.TYPE_QA_CREATE, "Vytvorena otazka: "+qa.getQuestion(), qa.getQaId(), -1);

		return (true);
	}

	public static boolean answer(QABean qa, HttpServletRequest request)
	{
		QABean original = getQAById(qa.getQaId());
		BeanDiffPrinter diff = new BeanDiffPrinter(new BeanDiff().setNew(qa).setOriginal(original));
		Adminlog.add(Adminlog.TYPE_QA_UPDATE, "Zmenena otazka: "+diff, qa.getQaId(), -1);

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("UPDATE questions_answers SET category_name=?, answer_date=?, question=?, from_name=?, from_email=?, to_name=?, to_email=?, publish_on_web=?, answer=?, group_name=?, sort_priority=?, from_phone=?, from_company=?, field_a=?, field_b=?, field_c=?, field_d=? WHERE qa_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			int index = 1;
			ps.setString(index++, qa.getCategoryName());
			long now = (new java.util.Date()).getTime();
			ps.setTimestamp(index++, new Timestamp(now));
			DB.setClob(ps, index++, qa.getQuestion());
			ps.setString(index++, qa.getFromName());
			ps.setString(index++, qa.getFromEmail());
			ps.setString(index++, qa.getToName());
			ps.setString(index++, qa.getToEmail());
			ps.setBoolean(index++, qa.isPublishOnWeb());
			DB.setClob(ps, index++, qa.getAnswerWeb());
			ps.setString(index++, qa.getGroupName());
			if (qa.getSortPriority()<1) qa.setSortPriority(getNewPriority(qa.getGroupName()));
			ps.setInt(index++, qa.getSortPriority());

			ps.setString(index++, qa.getFromPhone());
			ps.setString(index++, qa.getFromCompany());
			ps.setString(index++, qa.getFieldA());
			ps.setString(index++, qa.getFieldB());
			ps.setString(index++, qa.getFieldC());
			ps.setString(index++, qa.getFieldD());

			ps.setInt(index++, qa.getQaId());
			ps.executeUpdate();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return (false);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return (true);
	}

	public static boolean delete(QABean qa, HttpServletRequest request)
	{
		SimpleQuery query = new SimpleQuery(DBPool.getDBName(request));
		String question = query.forString("SELECT question FROM questions_answers WHERE qa_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), qa.getQaId());
		try
		{
			query.execute("DELETE FROM questions_answers WHERE qa_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true), qa.getQaId());
		}
		catch (Exception ex)
		{
			return (false);
		}
		Adminlog.add(Adminlog.TYPE_QA_DELETE, "Zmazana otazka: "+question, qa.getQaId(), -1);
		return (true);
	}

	/**
	 * Vrati novu prioritu pre qa pre danu grupu, najde doteraz najvyssiu + 10
	 * @param groupName groupName
	 * @return
	 */
	protected static int getNewPriority(String groupName){
		return new SimpleQuery().forInt("select max(sort_priority) from questions_answers where group_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true), groupName)+10;
	}

	public static String addQuestion(QABean qa, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		//ak ju chce vymazat, tak sa pozrieme, ci je to admin, a ak hej, tak ju vymazeme
		Identity user = UsersDB.getCurrentUser(request);
		boolean isAdmin = (user != null && user.isAdmin());
		final String delete = "/components/qa/admin_list";
		final String successAnswer = "/components/qa/admin_answer_result";

		if (isAdmin){
			try{
				String qaId = request.getParameter("qaId");
				String action = request.getParameter("action");
				boolean isQuestionIdValid = ( qaId != null && qaId.matches("^[0-9]+$") );
				boolean isActionValid = "delete".equalsIgnoreCase(action);

				if (isActionValid && isQuestionIdValid){
					QABean question = new QABean();
					question.setQaId( Integer.parseInt(qaId) );
					QADB.delete(question, request);
					return SpringUrlMapping.redirect(delete);
				}
			}
			catch (Exception e) {
				return SpringUrlMapping.redirect(delete);
			}
		}
		//ochrana pred XSS utokom
		String forward = request.getParameter("forward");
		String xssRedirectUrl = ShowDoc.getXssRedirectUrl(request);
		if (xssRedirectUrl != null) {
			if (forward.indexOf('?') == -1)
				forward += "?qasend=xss";
			else
				forward += "&qasend=xss";

			SpringUrlMapping.redirect(Tools.sanitizeHttpHeaderParam(forward));
		}

		boolean ret;
		boolean performAnswer = false;
		if (qa.getQaId() > 0 && qa.getHash() != null && qa.getHash().length() > 5)
			return(executeAnswer(qa, request, response));

		if(qa.getQaId() == -2 && user != null && user.isAdmin()) {
			performAnswer = true;
			ret = QADB.save(qa, request);
			if(ret) return(executeAnswer(qa, request, response));
	   } else
			ret = QADB.save(qa, request);

		if (ret == true) {
			//posli mu mail
			ret = sendAdminMail(qa, request);
			if (user != null && user.isAdmin() && performAnswer)
				SpringUrlMapping.redirect("/components/qa/admin_roots");
		}

		if (ret == true)
			request.setAttribute("ok", "true");

		if (forward != null && forward.trim().length() > 2) {
			if (ret == true) {
				if (forward.indexOf('?') == -1)
					forward += "?qasend=ok";
				else
					forward += "&qasend=ok";
			} else {
				if (forward.indexOf('?') == -1)
					forward += "?qasend=fail";
				else
					forward += "&qasend=fail";
			}
			return SpringUrlMapping.redirect(Tools.sanitizeHttpHeaderParam(forward));
		}

		//Pôvodne tu bola "success" hodnota ale v struct-config.xml neexistuje takéto mapovanie
		//Preto sme to zmenili na existujúce mapovanie "success_answer"
		return SpringUrlMapping.redirect(successAnswer);
	}

	public static String executeAnswer(QABean qa, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		final String reloadParentClose = "/components/reloadParentClose";
		final String successAnswer = "/components/qa/admin_answer_result";

		if (Tools.isEmpty(qa.getQuestion())) {
			//zmaz to z db
			QADB.delete(qa, request);
			return SpringUrlMapping.redirect(reloadParentClose);
		}

		boolean ret = QADB.answer(qa, request);
		String lng =  (String)request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);

		if (lng == null)
			lng = sk.iway.iwcm.Constants.getString("defaultLanguage");

		if (request.getParameter("lng") != null)
			lng = request.getParameter("lng");

		Prop prop = Prop.getInstance(Constants.getServletContext(), lng, false);

		if (ret == true && qa.getAnswer() != null && qa.getAnswer().length() > 0) {
			//posli mu mail
			try {
				String message = "<p>"+prop.getText("components.qa.add_action.answer_to_your_question") + ":<br/></p>";

				String question = qa.getQuestion().trim();
				String answer = qa.getAnswer().trim();
				String sender = qa.getToName().trim();
				String recipient = qa.getFromName().trim();

				message += "<p>" + prop.getText("components.qa.add_action.question") + ":<br/></p><p>"+question+"<br/><br/></p>";
				message += "<p>" + prop.getText("components.qa.add_action.answer") + ":<br/></p><p>"+answer+"<br/><br/></p>";

				message += "<p><br/><br/>" + prop.getText("components.qa.add_action.footer") +  " " + sender + "</p>";

				//bacha, toto je odpoved, takze toName je vlastne sender
				//a fromName je recipient

				String toName = qa.getToName();
				if (toName == null || toName.length() < 2)
					toName = qa.getToEmail();

				String subject = prop.getText("components.qa.add_action.answer_to_your_question");

				SendMail.send(toName, qa.getToEmail(), qa.getFromEmail(), subject, message);

				//daj do requestu answer
				String email = "<table border=0><tr><td>"+prop.getText("components.qa.add_action.sender")+": </td><td>"+sender+" &lt;"+qa.getToEmail()+"&gt;</td></tr>";
				email += "<tr><td>"+prop.getText("components.qa.add_action.recipient")+": </td><td>"+recipient+" &lt;"+qa.getFromEmail()+"&gt;</td></tr>";
				email += "<tr><td>"+prop.getText("components.qa.add_action.subject")+": </td><td>"+subject+"</td></tr>";
				email += "<tr><td>"+prop.getText("components.qa.add_action.email_body")+": </td><td>&nbsp;</td></tr>";
				email += "<tr><td colspan=2>"+Tools.replace(message, "\n", "<br>")+"</td></tr>";
				email += "</table>";

				request.setAttribute("answer", email);
			} catch (Exception ex) {
				ret = false;
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (qa.getAnswer() == null || qa.getAnswer().length() <= 2) {
			String email = prop.getText("components.qa.add_action.email_text_not_entered_saved_to_db");
			request.setAttribute("answer", email);
		}

		if (ret == false)
			request.removeAttribute("answer");

		if ("true".equals(request.getParameter("isFromEmail")))
			return SpringUrlMapping.redirect(successAnswer);

		return SpringUrlMapping.redirect(reloadParentClose);
	}

	public static boolean sendAdminMail(QABean qa, HttpServletRequest request) {
		boolean ret = true;

		try {
			String lng = PageLng.getUserLng(request);
			Prop prop = Prop.getInstance(Constants.getServletContext(), lng, false);

			String url = Tools.getBaseHref(request) + "/components/qa/admin_answer.jsp";
			if(Tools.isNotEmpty(Constants.getString("sk.iway.iwcm.qa.AddAction.sendAdminMail.url")))
				url = Constants.getString("sk.iway.iwcm.qa.AddAction.sendAdminMail.url");

			StringBuilder message = new StringBuilder(prop.getText("components.qa.add_action.new_question")).append(": <br>\n");

			message.append("<a href='");
			message.append(url+"?id=").append(qa.getQaId()).append("&hash=").append(qa.getHash());
			if (request.getParameter("lng")!=null)
				message.append("&lng="+ResponseUtils.filter(request.getParameter("lng")));

			message.append("'>");
			message.append(prop.getText("components.qa.add_action.for_answer_click_here"));
			message.append("</a>");

			String subject = prop.getText("components.qa.add_action.question");

			SendMail.send(qa.getFromName(), qa.getFromEmail(), qa.getToEmail(), subject, message.toString());
		} catch (Exception ex) {
			ret = false;
			sk.iway.iwcm.Logger.error(ex);
		}

		return ret;
	}
}