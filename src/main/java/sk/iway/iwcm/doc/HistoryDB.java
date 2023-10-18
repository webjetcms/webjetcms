package sk.iway.iwcm.doc;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Description of the Class
 *
 * @Title magma-web
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @author $Author: jeeff $
 * @version $Revision: 1.6 $
 * @created Piatok, 2002, jun 14
 * @modified $Date: 2004/03/07 20:58:27 $
 */
public class HistoryDB extends DB
{
	private String serverName = "iwcm";

	/**
	 * Constructor for the HistoryDB object
	 *
	 * @param serverName
	 *           Description of the Parameter
	 */
	public HistoryDB(String serverName)
	{
		this.serverName = serverName;
	}

	/**
	 * Constructor for the HistoryDB object
	 */
	@SuppressWarnings("unused")
	private HistoryDB() { }

	/**
	 * Gets the history attribute of the HistoryDB object
	 *
	 * @param doc_id
	 *           Description of the Parameter
	 * @return The history value
	 */
	public List<DocDetails> getHistory(int doc_id)
	{
		return (getHistory(doc_id, false, false));
	}

	/**
	 * Gets the history attribute of the HistoryDB object
	 *
	 * @param doc_id
	 *           Description of the Parameter
	 * @param data
	 *           Description of the Parameter
	 * @param onlyNew
	 *           Description of the Parameter
	 * @return The history value
	 */
	public List<DocDetails> getHistory(int doc_id, boolean data, boolean onlyNew)
	{
		List<DocDetails> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Prop prop = Prop.getInstance();

			db_conn = DBPool.getConnection(serverName);
			//toto je tu koli optimalizacii, cez 2 selecty to zbehne asi tak 20x
			// rychlejsie
			ps = db_conn.prepareStatement("SELECT history_id FROM documents_history WHERE doc_id=?");
			ps.setInt(1, doc_id);
			StringBuilder historyIds = null;
			rs = ps.executeQuery();
			while (rs.next())
			{
				if (historyIds == null)
				{
					historyIds = new StringBuilder(Integer.toString(rs.getInt("history_id")));
				}
				else
				{
					historyIds.append(",").append(rs.getInt("history_id"));
				}
			}
			rs.close();
			ps.close();

			if (historyIds != null)
			{
				String fields = DocDB.getDocumentFields(data);

				String selectStart = "SELECT d.history_id, d.save_date, d.approved_by, d.disapproved_by, d.actual, d.approve_date, d.publicable, d.awaiting_approve, d.publish_after_start, "+fields+", u.title as u_title, u.first_name, u.last_name ";

				String sql = selectStart + " FROM documents_history d LEFT JOIN  users u ON d.author_id=u.user_id WHERE history_id IN ("+historyIds.toString()+") AND d.publicable=1 ORDER BY history_id DESC ";
				if (Constants.DB_TYPE == Constants.DB_ORACLE)
				{
					sql = "SELECT d.*, u.title as u_title, u.first_name, u.last_name FROM documents_history d,  users u WHERE d.author_id=u.user_id(+) AND history_id IN ("+historyIds.toString()+") AND d.publicable=1 ORDER BY history_id DESC ";
				}
			   ps = db_conn.prepareStatement(sql);
			   rs = ps.executeQuery();
			   UserDetails userDetails;
			   //tu sa nacitava zoznam neschvalenych verzii
				while (rs.next())
				{
					DocDetails doc = new DocDetails();
					doc.setHistoryId(rs.getInt("history_id"));
					doc.setHistorySaveDate(getDbDateTime(rs, "save_date", serverName));
					doc.setHistoryApprovedBy(rs.getInt("approved_by"));
					doc.setHistoryActual(rs.getBoolean("actual"));
					doc.setHistoryApproveDate(getDbDateTime(rs, "approve_date", serverName));
					doc.setHistoryDisapprovedBy(rs.getInt("disapproved_by"));

					if (rs.getBoolean("publicable"))
					{
						//zistuje ci stranka bude niekedy publikovana alebo uz nie
						doc.setPublishStartStringExtra(getDbDateTime(rs, "publish_start", serverName));
					}
					else
					{
						doc.setPublishStartStringExtra("&nbsp;");
					}

					doc.setDocId(rs.getInt("doc_id"));
					doc.setTitle(getDbString(rs, "title"));
					if (data)
					{
						doc.setData(DB.getDbString(rs, "data"));
					}
					doc.setExternalLink(getDbString(rs, "external_link"));
					doc.setVirtualPath(getDbString(rs, "virtual_path"));
					doc.setNavbar(getDbString(rs, "navbar"));
					doc.setDateCreated(DB.getDbTimestamp(rs, "date_created"));
					doc.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
					doc.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));
					doc.setAuthorId(rs.getInt("author_id"));
					doc.setAuthorName(getFullName(rs));
					doc.setGroupId(rs.getInt("group_id"));
					doc.setTempId(rs.getInt("temp_id"));
					doc.setSearchable(rs.getBoolean("searchable"));
					doc.setAvailable(rs.getBoolean("available"));
					doc.setPasswordProtected(getDbString(rs, "password_protected"));
					doc.setCacheable(rs.getBoolean("cacheable"));
					doc.setFileName(getDbString(rs, "file_name"));
					doc.setSortPriority(rs.getInt("sort_priority"));
					doc.setHeaderDocId(rs.getInt("header_doc_id"));
					doc.setFooterDocId(rs.getInt("footer_doc_id"));
					doc.setMenuDocId(rs.getInt("menu_doc_id"));
					doc.setRightMenuDocId(rs.getInt("right_menu_doc_id"));

					doc.setFieldA(DB.getDbString(rs, "field_a"));
					doc.setFieldB(DB.getDbString(rs, "field_b"));
					doc.setFieldC(DB.getDbString(rs, "field_c"));
					doc.setFieldD(DB.getDbString(rs, "field_d"));
					doc.setFieldE(DB.getDbString(rs, "field_e"));
					doc.setFieldF(DB.getDbString(rs, "field_f"));

					doc.setFieldG(DB.getDbString(rs, "field_g"));
					doc.setFieldH(DB.getDbString(rs, "field_h"));
					doc.setFieldI(DB.getDbString(rs, "field_i"));
					doc.setFieldJ(DB.getDbString(rs, "field_j"));
					doc.setFieldK(DB.getDbString(rs, "field_k"));
					doc.setFieldL(DB.getDbString(rs, "field_l"));

					doc.setDisableAfterEnd(rs.getBoolean("disable_after_end"));
					doc.setPublishAfterStart(rs.getBoolean("publish_after_start"));

					doc.setFieldM(DB.getDbString(rs, "field_m"));
					doc.setFieldN(DB.getDbString(rs, "field_n"));
					doc.setFieldO(DB.getDbString(rs, "field_o"));
					doc.setFieldP(DB.getDbString(rs, "field_p"));
					doc.setFieldQ(DB.getDbString(rs, "field_q"));
					doc.setFieldR(DB.getDbString(rs, "field_r"));
					doc.setFieldS(DB.getDbString(rs, "field_s"));
					doc.setFieldT(DB.getDbString(rs, "field_t"));

					doc.setRequireSsl(rs.getBoolean("require_ssl"));


					if ((doc.getHistoryApprovedBy()>0 && doc.getAuthorId()!=doc.getHistoryApprovedBy()) || doc.getHistoryDisapprovedBy()>0) {
						if (doc.getHistoryApprovedBy()>0)
						{
							userDetails = UsersDB.getUser(doc.getHistoryApprovedBy());
							if (userDetails != null)
							{
								doc.setHistoryApprovedByName(userDetails.getFullName());
							} else {
								doc.setHistoryApprovedByName(prop.getText("editor.history.not_existing_user"));
							}
							doc.setHistoryDisapprovedByName("");
						}

						if (doc.getHistoryDisapprovedBy()>0)
						{
							userDetails = UsersDB.getUser(doc.getHistoryDisapprovedBy());
							if (userDetails != null)
							{
								doc.setHistoryDisapprovedByName(userDetails.getFullName());
							} else {
								doc.setHistoryApprovedByName(prop.getText("editor.history.not_existing_user"));
							}
							doc.setHistoryApprovedByName("");
						}
					} else {
						String awaitingApprove = getDbString(rs, "awaiting_approve");
						if (Tools.isEmpty(awaitingApprove)) {
							doc.setHistoryApprovedByName(prop.getText("editor.history.not_to_approve"));
							doc.setHistoryDisapprovedByName("");
						} else {
							doc.setHistoryApprovedByName(prop.getText("editor.history.not_approved"));
						}
					}

					ret.add(doc);

				}
				rs.close();
				ps.close();

				//tu sa nacitava zoznam este nepublikovanych alebo kompletny zoznam historie
				ps = db_conn.prepareStatement(selectStart + " FROM documents_history d LEFT JOIN  users u ON d.author_id=u.user_id WHERE history_id IN ("+historyIds.toString()+") AND d.publicable=0 ORDER BY history_id DESC ");
				rs = ps.executeQuery();
				while (rs.next())
				{

					DocDetails doc = new DocDetails();
					doc.setHistoryId(rs.getInt("history_id"));
					doc.setHistorySaveDate(getDbDateTime(rs, "save_date", serverName));
					doc.setHistoryApprovedBy(rs.getInt("approved_by"));
					doc.setHistoryDisapprovedBy(rs.getInt("disapproved_by"));
					doc.setHistoryActual(rs.getBoolean("actual"));
					doc.setHistoryApproveDate(getDbDateTime(rs, "approve_date", serverName));

					if (rs.getBoolean("publicable") || rs.getBoolean("publish_after_start")) //zistuje ci stranka bude
																// niekedy publikovana alebo uz
																// nie
					  doc.setPublishStartStringExtra(getDbDateTime(rs, "publish_start", serverName));
					else
					{
					  doc.setPublishStartStringExtra("&nbsp;");
					}

					if (onlyNew && doc.isHistoryActual())
					{
						rs.close();
						ps.close();
						db_conn.close();
						rs = null;
						ps = null;
						db_conn = null;

						return (ret);
					}

					String awaitingApprove = getDbString(rs, "awaiting_approve");
					if (Tools.isEmpty(awaitingApprove)) {
						doc.setHistoryApprovedByName(prop.getText("editor.history.not_to_approve"));
						doc.setHistoryDisapprovedByName("");
					} else {
						doc.setHistoryApprovedByName(prop.getText("editor.history.not_approved"));
					}

					doc.setDocId(rs.getInt("doc_id"));
					doc.setTitle(getDbString(rs, "title"));
					if (data)
					{
						doc.setData(DB.getDbString(rs, "data"));
					}
					doc.setExternalLink(getDbString(rs, "external_link"));
					doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
					doc.setNavbar(getDbString(rs, "navbar"));
					doc.setDateCreated(DB.getDbTimestamp(rs, "date_created"));
					doc.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
					doc.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));
					doc.setAuthorId(rs.getInt("author_id"));
					doc.setAuthorName(getFullName(rs));
					doc.setGroupId(rs.getInt("group_id"));
					doc.setTempId(rs.getInt("temp_id"));
					doc.setSearchable(rs.getBoolean("searchable"));
					doc.setAvailable(rs.getBoolean("available"));
					doc.setPasswordProtected(getDbString(rs, "password_protected"));
					doc.setCacheable(rs.getBoolean("cacheable"));
					doc.setFileName(getDbString(rs, "file_name"));
					doc.setSortPriority(rs.getInt("sort_priority"));
					doc.setHeaderDocId(rs.getInt("header_doc_id"));
					doc.setFooterDocId(rs.getInt("footer_doc_id"));
					doc.setMenuDocId(rs.getInt("menu_doc_id"));

					doc.setDisableAfterEnd(rs.getBoolean("disable_after_end"));

					ret.add(doc);

				}
				rs.close();
				ps.close();
			}

			for (DocDetails doc : ret)
			{
				/*
					Author is allso approver, but awaiting_approve is empty. Reason is that if author == approver, approverId is set but awaiting_approve
					is empty so this approver will not receive redundant email about approving page.
					This chnage in logic was made in ApproveService.loadApproveTables ifn, at the end
				*/
				if(doc.getHistoryApprovedBy() > 0 && doc.getAuthorId() == doc.getHistoryApprovedBy()) {
					UserDetails approver = UsersDB.getUser(doc.getHistoryApprovedBy());
					if (approver != null) doc.setHistoryApprovedByName(approver.getFullName());
				}

				if ((doc.getHistoryApprovedBy() > 0 && doc.getAuthorId()!=doc.getHistoryApprovedBy()))
				{
					UserDetails approver = UsersDB.getUserCached(doc.getHistoryApprovedBy());
					if (approver != null)
					{
						doc.setHistoryApprovedByName(approver.getFullName());
					} else {
						doc.setHistoryApprovedByName(prop.getText("editor.history.not_existing_user"));
					}
				}

				if (doc.getHistoryDisapprovedBy() > 0)
				{
					UserDetails approver = UsersDB.getUserCached(doc.getHistoryDisapprovedBy());
					if (approver != null)
					{
						doc.setHistoryDisapprovedByName(approver.getFullName());
					} else {
						doc.setHistoryDisapprovedByName(prop.getText("editor.history.not_existing_user"));
					}

					doc.setHistoryApprovedByName("");
				}
			}

			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		return (ret);
	}
	public boolean deleteHistory(int historyId) {

		Connection db_conn = null;
		PreparedStatement ps = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM documents_history WHERE history_id=?");
			ps.setInt(1, historyId);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			return true;
		} catch(Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally {
			try {
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch(SQLException e) {sk.iway.iwcm.Logger.error(e);}
		}
		return false;
	}
}
