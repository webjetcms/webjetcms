package sk.iway.iwcm.inquiry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.inquiry.jpa.InquiryUsersVoteEntity;
import sk.iway.iwcm.components.inquiry.rest.InquiryStatService;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.users.SettingsAdminDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * InquiryDB.java - praca s anketami
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: bistak $
 *@version      $Revision: 1.2 $
 *@created      Date: 9.4.2003 10:35:10
 *@modified     $Date: 2004/08/09 08:42:03 $
 */
public class InquiryDB
{
	public static final String ORDER_BY_ANSWER_TEXT = "ia.answer_text";
	public static final String ORDER_BY_ANSWER_CLICKS = "ia.answer_clicks";
	public static final String ORDER_BY_ANSWER_ID = "ia.answer_id";
	private static Random random = new Random();

	protected InquiryDB() {
		//utility class
	}

	public static InquiryBean getLastInquiry(int imagesLength, String percentageFormat, HttpServletRequest request)
	{
		return (getInquiry(-1, imagesLength, percentageFormat, ORDER_BY_ANSWER_TEXT, true, request));
	}

	public static InquiryBean getLastInquiry(HttpServletRequest request)
	{
		return (getInquiry(-1, 10, null, ORDER_BY_ANSWER_ID, true, request));
	}

	/**
	 * Vrati zoznam ID ankiet zodpovedajucich danym skupinam.
	 *
	 * @param groupNames
	 * @param request
	 * @param all         true = vsetky ankety, false = iba najnovsia
	 * @return
	 */
	public static List<Integer> getInquiryIds(String groupNames, HttpServletRequest request, boolean all)
	{
		List<String> groups = Arrays.asList(DB.removeSlashes(groupNames).split(",\\|\\+"));
		List<String> groupsSQL = new ArrayList<>();
		for (String group : groups)
		{
			groupsSQL.add("'" + group + "'");
		}

		String sql = "SELECT " + (all ? "question_id" : "max(question_id)") + " AS id FROM inquiry " +
			"WHERE question_group IN (" + StringUtils.join(groupsSQL.toArray(), ", ") + ") " +
			"AND (date_from < ? OR date_from IS NULL) AND (date_to > ? OR date_to IS NULL) AND question_active = "+DB.getBooleanSql(true)+" "+CloudToolsForCore.getDomainIdSqlWhere(true);
		List<Integer> ids = new ArrayList<>();

		Connection db_conn = DBPool.getConnection(DBPool.getDBName(request));
		if (null == db_conn) return ids;
		try
		{
			try
			{
				PreparedStatement ps = db_conn.prepareStatement(sql);
				try
				{
					ps.setTimestamp(1, new Timestamp(Tools.getNow()));
					ps.setTimestamp(2, new Timestamp(Tools.getNow()));
					ResultSet rs = ps.executeQuery();
					try
					{
						while (rs.next())
						{
							ids.add(Integer.valueOf(rs.getInt("id")));
						}
						return ids;
					} finally { rs.close(); }
				} finally { ps.close(); }
			} finally { db_conn.close(); }
		}
		catch (SQLException ex)
		{
			return ids;
		}
	}

	/**
	 * vrati poslednu anketu zo zadanej skupiny
	 *
	 * @param groupName -
	 *           nazov skupiny ankiet
	 * @param imagesLength -
	 *           pocet generovanych obrazkov v stlpiku
	 * @param percentageFormat -
	 *           format vypisu percent
	 * @param orderBy -
	 *           SQL sposob usporiadania odpovedi
	 * @param ascending -
	 *           true ak je vzostupne usporiadanie
	 * @param request
	 * @param random - boolean hodnota, ci sa ma vybrat nahodna anketa alebo najnovsia
	 * @return
	 */
	public static InquiryBean getInquiry(String groupNames, int imagesLength, String percentageFormat, String orderBy,
				boolean ascending, HttpServletRequest request, boolean random)
	{
		List<Integer> questionIds = getInquiryIds(groupNames, request, random);
		if (questionIds.isEmpty()) return null;
		int questionId = questionIds.get(InquiryDB.random.nextInt(questionIds.size())).intValue();
		return (getInquiry(questionId, imagesLength, percentageFormat, orderBy, ascending, request));
	}

	/**
	 * Vrati anketu so zadanym id, urcene do JSP stranok s designom
	 *
	 * @param questionId -
	 *           id anketu
	 * @param imagesLength -
	 *           pocet generovanych obrazkov v stlpiku
	 * @param percentageFormat -
	 *           format vypisu percent
	 * @param orderBy -
	 *           SQL sposob usporiadania odpovedi
	 * @param ascending -
	 *           true ak je vzostupne usporiadanie
	 * @param request
	 * @return
	 */
	public static InquiryBean getInquiry(int questionId, int imagesLength, String percentageFormat, String orderBy,
				boolean ascending, HttpServletRequest request)
	{
		InquiryBean inquiryBean = new InquiryBean();
		String cookieName = "inquiry-" + questionId;
		Cookie[] cookies = request.getCookies();
		int len = 0;
		if (cookies != null) {
			len = cookies.length;
			int i;
			Cookie cookie;
			for (i = 0; i < len; i++)
			{
				cookie = cookies[i];
				if (cookie.getName().compareTo(cookieName) == 0)
				{
					inquiryBean.setCanAnswer("no");
				}
			}
		}
		List<AnswerForm> answers = new ArrayList<>();
		AnswerForm answerForm;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String orderType = "DESC";
			if (ascending)	orderType = "ASC";
			//keby nieco...
			orderBy = orderBy.replace('\'', ' ');
			if (orderBy.indexOf('.') == -1)
			{
				orderBy = "ia." + orderBy;
			}
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			if (questionId == -1)
			{
				ps = db_conn.prepareStatement("SELECT i.*, ia.* FROM inquiry i, inquiry_answers ia WHERE i.question_id=ia.question_id  "+CloudToolsForCore.getDomainIdSqlWhere(true, "ia")+" ORDER BY i.question_id DESC, "
										+ orderBy + " " + orderType);
			}
			else
			{
				ps = db_conn.prepareStatement("SELECT i.*, ia.* FROM inquiry i, inquiry_answers ia WHERE i.question_id=ia.question_id AND i.question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true, "ia")+" ORDER BY "
										+ orderBy + " " + orderType);
				ps.setInt(1, questionId);
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				answerForm = new AnswerForm();
				answerForm.setQuestionID(rs.getInt("question_id"));
				if (questionId == -1)
				{
					questionId = answerForm.getQuestionID();
				}
				if (questionId != answerForm.getQuestionID())
				{
					//to je koli tomu zisteniu najnovsej ankety, selectnu sa vsetky
					//a ak sa zmenilo questionId voci poslednemu, je to uz ina
					//anketa
					break;
				}
				answerForm.setQuestionString(DB.getDbString(rs, "question_text"));
				answerForm.setAnswerString(DB.getDbString(rs, "answer_text"));
				answerForm.setAnswerID(rs.getInt("answer_id"));
				answerForm.setAnswerClicks(rs.getInt("answer_clicks"));
				answerForm.setHours(rs.getInt("hours"));
				answerForm.setGroup(DB.getDbString(rs, "question_group"));
				answerForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
				answerForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
				answerForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
				answerForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
				answerForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
				answerForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
				answerForm.setActive(rs.getBoolean("question_active"));
				answerForm.setTotalClicks(rs.getInt("total_clicks"));
				answerForm.setImagePath(rs.getString("image_path"));
				answerForm.setUrl(rs.getString("url"));
				answers.add(answerForm);
				inquiryBean.setMultiple(rs.getBoolean("multiple"));
				inquiryBean.setQuestion(answerForm.getQuestionString());
				inquiryBean.setTotalClicksMultiple(rs.getInt("total_clicks"));
			}
			rs.close();
			ps.close();
			db_conn.close();
			countPercentage(answers, percentageFormat);
			countImages(answers, imagesLength);
			inquiryBean.setAnswers(answers);

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

		return (inquiryBean);
	}

	public static List<AnswerForm> getAnswers(int questionID, HttpServletRequest request)
	{
		AnswerForm aForm = null;
		List<AnswerForm> aList = null;
		if (questionID > -1)
		{
			aList = new ArrayList<>();

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection(DBPool.getDBName(request));
				ps = db_conn
							.prepareStatement("SELECT i.*, ia.* FROM inquiry i, inquiry_answers ia WHERE i.question_id=ia.question_id AND i.question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true, "ia")+" ORDER BY "
										+ ORDER_BY_ANSWER_ID + " ASC");
				ps.setInt(1, questionID);
				rs = ps.executeQuery();
				while (rs.next())
				{
					aForm = new AnswerForm();
					aForm.setAnswerID(rs.getInt("answer_id"));
					aForm.setQuestionID(rs.getInt("question_id"));
					aForm.setAnswerString(DB.getDbString(rs, "answer_text"));
					aForm.setQuestionString(DB.getDbString(rs, "question_text"));
					aForm.setAnswerClicks(rs.getInt("answer_clicks"));
					aForm.setHours(rs.getInt("hours"));
					aForm.setGroup(DB.getDbString(rs, "question_group"));
					aForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
					aForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
					aForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
					aForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
					aForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
					aForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
					aForm.setActive(rs.getBoolean("question_active"));
					aForm.setMultiple(rs.getBoolean("multiple"));
					aForm.setTotalClicks(rs.getInt("total_clicks"));
					aForm.setImagePath(rs.getString("image_path"));
					aForm.setUrl(rs.getString("url"));
					aList.add(aForm);
				}
				rs.close();
				ps.close();
				db_conn.close();
				countPercentage(aList, null);

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
		}
		return aList;
	}

	/**
	 * nacita otazky a odpovede do List-u
	 * @param answerID
	 * @param request
	 * @return
	 */
	public static AnswerForm getAnswer(int answerID, HttpServletRequest request)
	{
		AnswerForm aForm = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn
						.prepareStatement("SELECT i.*, ia.* FROM inquiry i, inquiry_answers ia WHERE i.question_id=ia.question_id AND ia.answer_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true, "ia"));
			ps.setInt(1, answerID);
			rs = ps.executeQuery();
			if (rs.next())
			{
				aForm = new AnswerForm();
				aForm.setAnswerID(rs.getInt("answer_id"));
				aForm.setQuestionID(rs.getInt("question_id"));
				aForm.setAnswerString(DB.getDbString(rs, "answer_text"));
				aForm.setQuestionString(DB.getDbString(rs, "question_text"));
				aForm.setAnswerClicks(rs.getInt("answer_clicks"));
				aForm.setHours(rs.getInt("hours"));
				aForm.setGroup(DB.getDbString(rs, "question_group"));
				aForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
				aForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
				aForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
				aForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
				aForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
				aForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
				aForm.setActive(rs.getBoolean("question_active"));
				aForm.setMultiple(rs.getBoolean("multiple"));
				aForm.setTotalClicks(rs.getInt("total_clicks"));
				aForm.setImagePath(rs.getString("image_path"));
				aForm.setUrl(rs.getString("url"));

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

		return (aForm);
	}

	/**
	 * nacita skupiny ankiet do listu
	 *
	 * @return
	 */
	public static List<LabelValueDetails> getQuestionGroups(HttpServletRequest request)
	{
		List<LabelValueDetails> aList = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement("SELECT DISTINCT question_group FROM inquiry WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false)+" ORDER BY question_group ASC");
			rs = ps.executeQuery();
			LabelValueDetails lvd;
			while (rs.next())
			{
				lvd = new LabelValueDetails();
				lvd.setLabel(DB.getDbString(rs, "question_group"));
				lvd.setValue(DB.getDbString(rs, "question_group"));
				aList.add(lvd);
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

		return aList;
	}

	/**
	 * nacita skupiny ankiet do listu a vyfiltruje podla povolenych kategorii pre usera
	 *
	 * @return
	 */
	public static List<LabelValueDetails> getQuestionGroupsByUser(HttpServletRequest request)
	{
		Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
		List<LabelValueDetails> distinctGroups = SettingsAdminDB.filterBeansByUserAllowedCategories(getQuestionGroups(request), "label", user, "menuInquiry") ;
		return distinctGroups;
	}


	/**
	 * vypocita percentualne hodnotu odpovede
	 * @param answers
	 * @param format
	 */
	private static void countPercentage(List<AnswerForm> answers, String format)
	{
		double suma = 0;
		//vyrataj sumu
		for (AnswerForm answer : answers)
		{
			suma += answer.getAnswerClicks();
		}
		if (format == null)
			format = "0.##";
		DecimalFormat formatter = new DecimalFormat(format);
		for (AnswerForm answer : answers)
		{
			if (answer.getAnswerClicks() > 0 && suma > 0)
			{
				answer.setPercentage((100D * answer.getAnswerClicks()) / suma);
				answer.setPercentageString(formatter.format(answer.getPercentage()));
			}
			else
			{
				answer.setPercentage(0);
				answer.setPercentageString(formatter.format(answer.getPercentage()));
			}
		}
	}

	/**
	 * nastavi pocty obrazkov pre vygenerovanie stlpceka MUSI sa volat az po
	 * countPercentage!
	 *
	 * @param answers
	 * @param imagesLength -
	 *           maximalny pocet obrazkov
	 */
	private static void countImages(List<AnswerForm> answers, int imagesLength)
	{
		//vyrataj sumu
		for (AnswerForm answer : answers)
		{
			answer.setImages((int) ((imagesLength / 100D) * answer.getPercentage()));
		}
	}

	/**
	 * incrementuje hodnotu(t.j. result) v odpovedi urcenej pomocou qID
	 * (ktora otazka) a aID(ktora odpoved)
	 * @param qID
	 * @param aID
	 * @param request
	 */
	public static void updateAnswer(int qID, int aID, HttpServletRequest request)
	{
		String sql = "UPDATE inquiry_answers SET answer_clicks=answer_clicks+1 WHERE question_id=? and answer_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);

		if ((qID > -1) && (aID > -1))
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection(DBPool.getDBName(request));
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, qID);
				ps.setInt(2, aID);
				ps.execute();

				ps.close();
				db_conn.close();
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
		}
	}

	/**
	 * inkrementuje premennu totalClicks na zaklade qID, co definuje anketu
	 * @param qID - identifikator ankety
	 * @param request
	 */
	public static void updateTotalClicks(int qID, HttpServletRequest request)
	{
		String sqlTotalClicks = "UPDATE inquiry SET total_clicks = total_clicks + 1 WHERE question_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		if ((qID > -1))
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection(DBPool.getDBName(request));

				ps = db_conn.prepareStatement(sqlTotalClicks);
				ps.setInt(1, qID);
				ps.execute();

				ps.close();
				db_conn.close();
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
					sk.iway.iwcm.Logger.error(ex2);
				}
			}
		}
	}

	public static List<AnswerForm> getAllInquiry(HttpServletRequest request)
	{
		List<AnswerForm> al = new ArrayList<>();
		AnswerForm icForm;
		String sql = "SELECT * FROM inquiry WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false)+" ORDER BY question_id DESC";

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next())
			{
				icForm = new AnswerForm();
				icForm.setQuestionString(DB.getDbString(rs, "question_text"));
				icForm.setQuestionID(rs.getInt("question_id"));
				icForm.setHours(rs.getInt("hours"));
				icForm.setGroup(DB.getDbString(rs, "question_group"));
				icForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
				icForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
				icForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
				icForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
				icForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
				icForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
				icForm.setActive(rs.getBoolean("question_active"));
				icForm.setMultiple(rs.getBoolean("multiple"));
				icForm.setTotalClicks(rs.getInt("total_clicks"));
				al.add(icForm);
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

		return al;
	}

	/**
	 * Funkcia vrati zoznam objektov AnswerForm, ktore zodpovedaju vstupnym parametrom - otazka obsahuje text alebo/a patri do skupiny
	 *
	 * @param groups			nazvy skupin, do ktorych musi patrit anketa
	 * @param questionText	text, ktory sa vyhladava v otazke
	 *
	 * @return Vrat zoznam objektov AnswerForm, ak ziadna z ankiet nevyhovuje podmienke, vrati prazdny zoznam (nie NULL)
	 */
	public static List<AnswerForm> getInquiries(List<String> groups, String questionText)
   {
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM inquiry WHERE question_id > 0 ");

		String groupsInSql = "(";
		StringBuilder buf = new StringBuilder(groupsInSql);
		if (groups != null && groups.size() > 0)
		{
			for (int i = 0; i < groups.size(); i++) buf.append("?, ");

			groupsInSql = buf.toString();
			groupsInSql = groupsInSql.substring(0, (groupsInSql.length()-2));
			groupsInSql += ")";

			//skupina ankety
			sql.append(" AND question_group IN " + groupsInSql);

			for (int i = 0; i < groups.size(); i++) params.add(groups.get(i));
		}

		//text otazky
		if (Tools.isNotEmpty(questionText))
		{
			sql.append(" AND question_text LIKE ?");
			params.add("%" + questionText + "%");
		}

		sql.append(CloudToolsForCore.getDomainIdSqlWhere(true));

		sql.append(" ORDER BY question_id DESC");

      List<AnswerForm> inquiries = new ComplexQuery().setSql(sql.toString()).setParams(params.toArray()).list(new Mapper<AnswerForm>()
      {
      	@Override
         public AnswerForm map(ResultSet rs) throws SQLException
         {
         	AnswerForm icForm = new AnswerForm();

				icForm.setQuestionString(DB.getDbString(rs, "question_text"));
				icForm.setQuestionID(rs.getInt("question_id"));
				icForm.setHours(rs.getInt("hours"));
				icForm.setGroup(DB.getDbString(rs, "question_group"));
				icForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
				icForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
				icForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
				icForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
				icForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
				icForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
				icForm.setActive(rs.getBoolean("question_active"));
				icForm.setMultiple(rs.getBoolean("multiple"));
				icForm.setTotalClicks(rs.getInt("total_clicks"));

         	return icForm;
         }
      });

		return inquiries;
	}

	public static int getNewQuestionID(HttpServletRequest request)
	{
		int newqID = -1;
		String sql = "SELECT max(question_id) AS max FROM inquiry WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false);

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next())
			{
				newqID = rs.getInt("max");
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

		newqID++;
		return newqID;
	}

	public static void addNewAnswer(int qID, AnswerForm answer, HttpServletRequest request)

	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			String sql_3_1 = "INSERT INTO inquiry_answers(question_id, answer_text, image_path, url, answer_clicks, domain_id) VALUES(?,?,?,?,?,?)";
			if ((qID > -1) && (answer != null))
			{
				db_conn = DBPool.getConnection(DBPool.getDBName(request));
				ps = db_conn.prepareStatement(sql_3_1);
				ps.setInt(1, qID);
				ps.setString(2, answer.getAnswerString());
				ps.setString(3, answer.getImagePath());
				ps.setString(4, answer.getUrl());
				ps.setInt(5, answer.getAnswerClicks());
				ps.setInt(6, CloudToolsForCore.getDomainId());
				ps.execute();
				ps.close();
				db_conn.close();
			}

			ps = null;
			AnswerForm question = getQuestion(qID, request);
			Adminlog.add(Adminlog.TYPE_INQUIRY_CREATE, String.format("Pridana odpoved %s na otazku %s", answer, question.getQuestionString()), qID, -1);
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
	}

	public static void createNewQuestion(AnswerForm form, HttpServletRequest request)
	{
		String sql = "INSERT INTO inquiry (question_text, hours, question_group, answer_text_ok, answer_text_fail, "+
					 "date_from, date_to, question_active, multiple, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		int max = -1;


		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, form.getQuestionString());
			ps.setInt(2, form.getHours());
			ps.setString(3, form.getGroup());
			ps.setString(4, form.getAnswerTextOk());
			ps.setString(5, form.getAnswerTextFail());
			if (Tools.isNotEmpty(form.getDateFrom()))
			{
				ps.setTimestamp(6, new Timestamp(DB.getTimestamp(form.getDateFrom(), form.getDateFromTime() )));
			}
			else
			{
				ps.setNull(6, Types.TIMESTAMP);
			}
			if (Tools.isNotEmpty(form.getDateTo()))
			{
				ps.setTimestamp(7, new Timestamp(DB.getTimestamp(form.getDateTo(), form.getDateToTime() )));
			}
			else
			{
				ps.setNull(7, Types.TIMESTAMP);
			}
			ps.setBoolean(8, form.isActive());
			ps.setBoolean(9, form.isMultiple());
			ps.setInt(10, CloudToolsForCore.getDomainId());
			ps.execute();
			ps.close();
			ps = db_conn.prepareStatement("SELECT max(question_id) AS max FROM inquiry WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false));
			rs = ps.executeQuery();
			rs.next();
			max = rs.getInt("max");
			rs.close();
			ps.close();
			form.setQuestionID(max);
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
		Adminlog.add(Adminlog.TYPE_INQUIRY_CREATE, "Vytvorena nova anketa: "+form.getQuestionString(), form.getQuestionID(), -1);
	}

	public static void alterQuestion(AnswerForm form, HttpServletRequest request)
	{
		//Logger.println(this,"Alter Question: " + form.getDateFrom() + " to=" + form.getDateTo());
		AnswerForm question = getQuestion(form.getQuestionID(), request);
		BeanDiffPrinter diff = new BeanDiffPrinter(new BeanDiff().setOriginal(question).setNew(form));
		Adminlog.add(Adminlog.TYPE_INQUIRY_UPDATE, "Zmenena anketa "+form.getQuestionString()+" "+diff, form.getQuestionID(), -1);


		String sql = "UPDATE inquiry SET question_text=?, hours=?, question_group=?, answer_text_ok=?, answer_text_fail=?, "+
					"date_from=?, date_to=?, question_active=?, multiple= ?, total_clicks= ? WHERE question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{

			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, form.getQuestionString());
			ps.setInt(2, form.getHours());
			ps.setString(3, form.getGroup());
			ps.setString(4, form.getAnswerTextOk());
			ps.setString(5, form.getAnswerTextFail());
			if (Tools.isNotEmpty(form.getDateFrom()))
			{
				ps.setTimestamp(6, new Timestamp(DB.getTimestamp(form.getDateFrom(), form.getDateFromTime() )));
			}
			else
			{
				ps.setNull(6, Types.TIMESTAMP);
			}
			if (Tools.isNotEmpty(form.getDateTo()))
			{
				ps.setTimestamp(7, new Timestamp(DB.getTimestamp(form.getDateTo(), form.getDateToTime() )));
			}
			else
			{
				ps.setNull(7, Types.TIMESTAMP);
			}
			ps.setBoolean(8, form.isActive());
			ps.setBoolean(9, form.isMultiple());
			ps.setInt(10, form.getTotalClicks());
			ps.setInt(11, form.getQuestionID());
			ps.execute();
			ps.close();
			db_conn.close();
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

	}

	public static void alterAnswerString(AnswerForm answer, HttpServletRequest request)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		AnswerForm answerOld = getAnswer(answer.getAnswerID(), request);

		Adminlog.add(Adminlog.TYPE_INQUIRY_UPDATE,
				String.format("Zmenena odpoved na anketu %s: %s => %s", answerOld.getQuestionString(), answerOld.getAnswerString(), answer.getAnswerString()),
				answer.getAnswerID(), -1);

		String sql = "UPDATE  inquiry_answers SET answer_text=?, answer_clicks=?, image_path = ?, url= ? WHERE answer_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, answer.getAnswerString());
			ps.setInt(2, answer.getAnswerClicks());
			ps.setString(3, answer.getImagePath());
			ps.setString(4, answer.getUrl());
			ps.setInt(5, answer.getAnswerID());
			ps.execute();
			ps.close();
			db_conn.close();
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
	}

	public static void deleteAnswer(int aID, HttpServletRequest request)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		AnswerForm answer = getAnswer(aID, request);
		Adminlog.add(Adminlog.TYPE_INQUIRY_DELETE,
				String.format("Zmazana odpoved %s na anketu %s", answer.getAnswerString(), answer.getQuestionString()),
				aID, -1);
		String sql = "DELETE FROM inquiry_answers WHERE answer_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, aID);
			ps.execute();
			ps.close();
			db_conn.close();
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
	}

	/*
	 * Vymaze z DB anketu a odpovede pre danu anketu
	 */
	public static String deleteInquiry(int questionId, HttpServletRequest request)
	{
		Adminlog.add(Adminlog.TYPE_INQUIRY_DELETE, "Zmazana anketa s ID: "+questionId, -1, -1);
		Connection db_conn = null;
		PreparedStatement ps = null;
		String sql_question = "DELETE FROM inquiry WHERE question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		String sql_answer = "DELETE FROM inquiry_answers WHERE question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql_question);
			ps.setInt(1, questionId);
			ps.execute();
			ps.close();
			db_conn.close();
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

		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql_answer);
			ps.setInt(1, questionId);
			ps.execute();
			ps.close();
			db_conn.close();
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
		return ("success");
	}

	public static AnswerForm getQuestion(int qID, HttpServletRequest request)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		AnswerForm aForm = new AnswerForm();
		String sql = "SELECT * FROM inquiry WHERE question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		try
		{
			if (qID > -1)
			{
				db_conn = DBPool.getConnection(DBPool.getDBName(request));
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, qID);
				rs = ps.executeQuery();
				if (rs.next())
				{
					aForm.setQuestionID(qID);
					aForm.setQuestionString(DB.getDbString(rs, "question_text"));
					aForm.setHours(rs.getInt("hours"));
					aForm.setGroup(DB.getDbString(rs, "question_group"));
					aForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
					aForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
					aForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
					aForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
					aForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
					aForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
					aForm.setActive(rs.getBoolean("question_active"));
					aForm.setMultiple(rs.getBoolean("multiple"));
					aForm.setTotalClicks(rs.getInt("total_clicks"));
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;

				Logger.println(InquiryDB.class,"DateToTime: " + aForm.getDateToTime());
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
		return aForm;
	}

	public static int getHoursCount(int qID, HttpServletRequest request)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "SELECT hours FROM inquiry WHERE question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		int result = 0;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, qID);
			rs = ps.executeQuery();
			if (rs.next())
			{
				result = rs.getInt("hours");
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
		return result;
	}

	/**
	 * Vrati zoznam starych ankiet usporiadanych podla datumu platnosti
	 * @param groupNames - nazvy skupin oddelene ciarkou
	 * @param orderAscending - ak je true je usporiadanie od najstarsich po najnovsie
	 * @return
	 */
	public static List<AnswerForm> getOldInquiry(String groupNames, boolean orderAscending)
	{
		List<AnswerForm> inquirys = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			String sOrder = "DESC";
			if (orderAscending) sOrder = "ASC";

			StringBuilder sql = new StringBuilder("SELECT * FROM inquiry");

			if (Tools.isNotEmpty(groupNames))
			{
				groupNames = DB.removeSlashes(groupNames);
				StringTokenizer st = new StringTokenizer(groupNames, ",+");
				sql.append(" WHERE question_group IN ( '").append(DB.removeSlashes(st.nextToken())).append('\'');

				while (st.hasMoreTokens())
				{
					sql.append(", '").append(st.nextToken()).append('\'');
				}
				sql.append(") ");

				sql.append(CloudToolsForCore.getDomainIdSqlWhere(true));
			}
			else
			{
				sql.append(" WHERE ").append(CloudToolsForCore.getDomainIdSqlWhere(false));
			}

			sql.append(" ORDER BY date_from ").append(sOrder).append(", question_id ").append(sOrder);

			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			AnswerForm icForm;
			while (rs.next())
			{
				icForm = new AnswerForm();
				icForm.setQuestionString(DB.getDbString(rs, "question_text"));
				icForm.setQuestionID(rs.getInt("question_id"));
				icForm.setHours(rs.getInt("hours"));
				icForm.setGroup(DB.getDbString(rs, "question_group"));
				icForm.setAnswerTextOk(DB.getDbString(rs, "answer_text_ok"));
				icForm.setAnswerTextFail(DB.getDbString(rs, "answer_text_fail"));
				icForm.setDateFrom(Tools.formatDate(rs.getTimestamp("date_from")));
				icForm.setDateFromTime(Tools.formatTime(rs.getTimestamp("date_from")));
				icForm.setDateTo(Tools.formatDate(rs.getTimestamp("date_to")));
				icForm.setDateToTime(Tools.formatTime(rs.getTimestamp("date_to")));
				icForm.setActive(rs.getBoolean("question_active"));
				icForm.setMultiple(rs.getBoolean("multiple"));
				icForm.setTotalClicks(rs.getInt("total_clicks"));
				//zadisablovane tam nedame, iba take co maju neplatne datumy
				if (icForm.isActive()==false) continue;

				//dame tam len take kde nie je platny datum, alebo datum nie je zadany
				if (icForm.isDateValid()==false || (Tools.isEmpty(icForm.getDateFrom()) && Tools.isEmpty(icForm.getDateTo())))
				{
					inquirys.add(icForm);
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
		return(inquirys);
	}

	/**
	 * Vrati najnovsi datum zahlasovania daneho pouzivatela pre danu anketu
	 * @param userId	ID pouzivatela
	 * @param questionId	ID ankety
	 * @return najnovsi datum zahlasovania pre daneho pouzivatela
	 */
	public static java.util.Date getLastVoteDate(int userId, int questionId){
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		java.util.Date createdDate = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT MAX(create_date) as max_value FROM inquiry_users WHERE user_id=? AND question_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, userId);
			ps.setInt(2, questionId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				createdDate = rs.getTimestamp("max_value");
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
		return createdDate;
	}

	/**
	 * Vrati zoznam ankiet ako List {@link AnswerForm} vyfiltrovanych podla povolenych kategorii pre usera
	 * @param request
	 * @return
	 */
	public static Object getAllInquiryByUser(HttpServletRequest request)
	{
		List<LabelValueDetails> groupsByUser = getQuestionGroupsByUser(request);
		List<String> stringGroups = new ArrayList<>(groupsByUser.size());
		for(LabelValueDetails lvd : groupsByUser)
			stringGroups.add(lvd.getLabel());
		return getInquiries(stringGroups, "");
	}

	/**
	 * Save answer in selected inquiry. Hadle logic and return path to some jsp taht represent "fail" or "ok" (may contain params).
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public static String saveAnswer(HttpServletRequest request, HttpServletResponse response) {

		final String fail = "/components/inquiry/fail";
		final String ok = "/components/inquiry/ok";

		int[] aID;
		int questionID = Tools.getIntValue(request.getParameter("qID"), -1);
		String resultUrl = Tools.sanitizeHttpHeaderParam(request.getParameter("resultUrl"));	//Vysledok ankety na inej stranke
		Identity user = UsersDB.getCurrentUser(request);	//kvoli ukladaniu statistik hlasovania pre jednotliveho pouzivatela

		String la = request.getParameter("la");
		if ("multipleAnswer".equals(la)) { // Zahlasovanie za viac moznosti
			String[] mAnswersString = request.getParameterValues("selectedAnswers");
			aID = new int[mAnswersString.length];

			for (int i = 0; i < mAnswersString.length; i++) { //prekonvertovanie
				aID[i] = Tools.getIntValue(mAnswersString[i], -1);
				if (aID[i] < 0) {
					if(Tools.isNotEmpty(resultUrl))
						return SpringUrlMapping.redirect(resultUrl + "?fail=1&qID=" + questionID);
					else
						return fail;
				}
			}
		} else { // ak je null
			aID = new int[]{Tools.getIntValue(request.getParameter("aID"), -1)};
			if (aID[0] < 0) {
				if(Tools.isNotEmpty(resultUrl))
						return SpringUrlMapping.redirect(resultUrl + "?fail=1&qID=" + questionID);
				else
					return fail;
			}
		}

		if (questionID < 0) {
			if(Tools.isNotEmpty(resultUrl))
				return SpringUrlMapping.redirect(resultUrl + "?fail=1&qID=" + questionID); //chyba
			else
				return fail;
		}

		String docId = request.getParameter("docId");
		String errorDocId = request.getParameter("errorDocId");
		String cookieName = "inquiry-" + questionID;

		//zisti ci nema cookie, ktore by nedovolilo znova kliknut
		Cookie[] cookies = request.getCookies();
		int len = 0;
		if (cookies != null) {
			len = cookies.length;
			Cookie cookie;
			for (int i = 0; i < len; i++) {
				cookie = cookies[i];
				if (cookie.getName().compareTo(cookieName) == 0) {
					if (errorDocId!=null)
						return SpringUrlMapping.redirect(Tools.sanitizeHttpHeaderParam("/showdoc.do?docid=" + errorDocId + "&qID=" + questionID + "&aID=" + aID[0]));
					else {
						request.setAttribute("answerForm", InquiryDB.getQuestion(questionID, request));
						if(Tools.isNotEmpty(resultUrl))
							return SpringUrlMapping.redirect(resultUrl + "?fail=1&qID=" + questionID);
						else return fail;
					}
				}
			}
		}

		Cookie cookie = new Cookie(cookieName, "true");
		cookie.setMaxAge(3600 * InquiryDB.getHoursCount(questionID, request));
		cookie.setPath("/");

		if (docId!=null) {
			Tools.addCookie(cookie, response, request);
			return SpringUrlMapping.redirect(Tools.sanitizeHttpHeaderParam("/showdoc.do?docid=" + docId + "&qID=" + questionID + "&aID=" + aID[0]));
		} else { //kontrola aj podla IP adresy
			if(user != null && user.getUserId() > -1) {
				//ak je user prihlaseny
				if(!canVote(user.getUserId(), questionID, request)) {
					Tools.addCookie(cookie, response,request);
					//overi, ci moze prihlaseny pouzivatel opat hlasovat
					request.setAttribute("answerForm", InquiryDB.getQuestion(questionID, request));
					if(Tools.isNotEmpty(resultUrl))
						return SpringUrlMapping.redirect(resultUrl + "?fail=1&qID=" + questionID); //nemoze zahlasovat
					else return fail;
				}
			}

			if (!SpamProtection.canPost("inquiry", "", request)) {
				request.setAttribute("spam", "true");
				request.setAttribute("answerForm", InquiryDB.getQuestion(questionID, request));
				if(Tools.isNotEmpty(resultUrl))
					return SpringUrlMapping.redirect(resultUrl + "?fail=1&spam=1&qID=" + questionID); //nemoze zahlasovat
				else return fail;
			} else
				Tools.addCookie(cookie, response,request);

			java.util.Date date = new java.util.Date();
			Logger.debug(null, "Pred pridanim");
			for (int k = 0; k < aID.length; k++) { //Hlasuje
				InquiryDB.updateAnswer(questionID, aID[k], request);

				Logger.debug(null, "Pridavam InquiryUsersVoteEntity");
				InquiryUsersVoteEntity iuve = new InquiryUsersVoteEntity();

				if(user == null)  iuve.setUserId(-1);
				else iuve.setUserId(user.getUserId());

				iuve.setQuestionId(questionID);
				iuve.setAnswerId(aID[k]);
				iuve.setDayDate(date);
				iuve.setIpAddress(request.getRemoteAddr());
				iuve.setDomainId(CloudToolsForCore.getDomainId());

				InquiryStatService.saveInquiryUserVote(iuve);
			}
			InquiryDB.updateTotalClicks(questionID, request);	//aktualizujem pocet kliknuti

			//START - po zahlasovani vymazem z cache
			AnswerForm inquiry = InquiryDB.getQuestion(questionID, request);
			String group = inquiry.getGroup();
			if(Tools.isNotEmpty(group)) {
				Cache c = Cache.getInstance();
				String cacheKey = "components.inquiry.group."+group;
				if(c.getObject(cacheKey) != null)
					c.removeObject(cacheKey);
			}
			//END

			request.setAttribute("answerForm", inquiry);
			if(Tools.isNotEmpty(resultUrl))
				return SpringUrlMapping.redirect(resultUrl + "?qID=" + questionID); //ok
			else return ok;
		}
	}

	private static boolean canVote(int userId, int questionId, HttpServletRequest request) {
		long wait = 3600L * 1000L * InquiryDB.getHoursCount(questionId, request); // limit v milisekundach - naastavuje
																					// sa v ankete
		Date createDate = InquiryDB.getLastVoteDate(userId, questionId);
		if (createDate == null)
			return true; // pouzivatel este vobec nehlasoval v ankete

		Date nowDate = new Date();
		long delta = (nowDate.getTime() - createDate.getTime());
		if (delta < wait)
			return false;
		return true; // ak je rozdiel vacsi ako nastavena doba cakania
	}

}