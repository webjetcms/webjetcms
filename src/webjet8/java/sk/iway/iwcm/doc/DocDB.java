package sk.iway.iwcm.doc;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import org.apache.struts.util.ResponseUtils;
import org.json.JSONObject;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.forum.rest.ForumGroupService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.editor.*;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.spring.events.DocumentPublishEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.text.Collator;
import java.util.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Drzi cacheable udaje z tabulky documents a nacita pozadovany necacheable
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.55 $
 *@created      $Date: 2004/03/23 20:41:10 $
 *@modified     $Date: 2004/03/23 20:41:10 $
 */
public class DocDB extends DB
{
	/**
	 *  Description of the Field
	 */
	public static final int ORDER_TITLE = 1;
	/**
	 *  Description of the Field
	 */
	public static final int ORDER_ID = 2;
	/**
	 *  Description of the Field
	 */
	public static final int ORDER_PRIORITY = 3;
	/**
	 *  Description of the Field
	 */
	public static final int ORDER_DATE = 4;
	/**
	 *  Description of the Field
	 */
	public static final int ORDER_PLACE = 5;

	public static final int ORDER_EVENT_DATE = 6;

	public static final int ORDER_SAVE_DATE = 7;

	public static final int ORDER_RATING = 8;


	/**
	 * getDocPerex vrati iba dokumenty, co maju platny zaciatok a koniec
	 */
	public static final int PUBLISH_NEW = 1;
	/**
	 * getDocPerex vrati iba dokumenty, co su stare
	 */
	public static final int PUBLISH_OLD = 2;
	/**
	 * getDocPerex vrati vsetky dokumenty, bez ohladu na datum publikovania
	 */
	public static final int PUBLISH_ALL = 3;
	/**
	 * getDocPerex vrati vsetky dokumenty v buducnosti (_NEW vrati iba tie, ktore maju uz platny
	 * datum zaciatku)
	 */
	public static final int PUBLISH_NEXT = 4;


	/**
	 * getDocPerex vrati iba dokumenty, co maju platny zaciatok a koniec
	 * neberie ohlad na to, ci je zadany text perexu
	 */
	public static final int PUBLISH_NO_PEREX_CHECK_NEW = 101;
	/**
	 * getDocPerex vrati iba dokumenty, co su stare
	 * neberie ohlad na to, ci je zadany text perexu
	 */
	public static final int PUBLISH_NO_PEREX_CHECK_OLD = 102;
	/**
	 * getDocPerex vrati vsetky dokumenty, bez ohladu na datum publikovania
	 * neberie ohlad na to, ci je zadany text perexu
	 */
	public static final int PUBLISH_NO_PEREX_CHECK_ALL = 103;

	/**
	 * getDocPerex vrati vsetky dokumenty v buducnosti (_NEW vrati iba tie, ktore maju uz platny
	 * datum zaciatku)
	 */
	public static final int PUBLISH_NO_PEREX_CHECK_NEXT = 104;

	/**
	 *  pole dokumentov cachovanych v pamati
	 */
	private Map<Integer, DocDetails> cachedDocs;
	private List<PublicableForm> publicableDocs;

	//tabulka pre skupiny perexov (zrychleny pristup)
	private List<PerexGroupBean> perexGroups = null;

	private final String serverName;

	//toto drzi hashtabulku url pre danu domenu, vzdy k tomu pristupujte cez getUrlsByUrlDomains(domena)
	private Map<String, TObjectIntHashMap<String>> urlsByUrlDomains = null;
	/**
	 * tato hashtabulka ma zakladne info o kazdej stranke bez nutnosti pristupu do DB (docid, title, groupid, navbar)
	 * Kvoli pamatovej narocnosti je riesena ako {@link List}, nie ako {@link Map}.
	 *
	 * Index v Liste je doc_id a pouzity ako kluc na vyhladavanie
	 *
	 * Nemente na typ {@link List}! Potrebujeme niektore vlastnosti Listu, ako napr. ensureCapacity()
	 */
	private TIntObjectHashMap<DocDetails> basicAllDocsTable = null;
	private TIntObjectHashMap<List<DocDetails>> basicAllDocsInGroupTable = null;

	/**
	 * mapa master-slave stranok
	 */
	private Map<Integer, Integer> slavesMasterMappings = null;
	/**
	 * mapa master-all_master_slaves stranok
	 */
	private Map<Integer, Integer[]> masterMappings = null;

	/**
	 * Zakladna instancia objektu
	 * @return
	 */
	public static DocDB getInstance()
	{
		return(getInstance(false));
	}

	/**
	 * Zakladna instancia objektu
	 * @param forceRefresh - true = refresh instancie
	 * @return
	 */
	public static DocDB getInstance(boolean forceRefresh)
	{
		return(getInstance(Constants.getServletContext(), forceRefresh, "iwcm")); //NOSONAR
	}

	/**
	 *  Gets the instance attribute of the DocDB class
	 *
	 *@param  servletContext2  Description of the Parameter
	 *@param  force_refresh   Description of the Parameter
	 *@param  serverName      Description of the Parameter
	 *@return                 The instance value
	 *@deprecated
	 */
	@Deprecated
	public static DocDB getInstance(javax.servlet.ServletContext servletContext2, boolean force_refresh, String serverName)
	{
		javax.servlet.ServletContext servletContext = Constants.getServletContext();
		if (!force_refresh)
		{
			DocDB myDocDB = (DocDB) servletContext.getAttribute(Constants.A_DOC_DB);
			if (myDocDB != null && myDocDB.urlsByUrlDomains!=null)
			{
				myDocDB.checkPublicable();
				return myDocDB;
			}
		}
		synchronized (DocDB.class)
		{
			if (force_refresh)
			{
				DocDB myDocDB = new DocDB(servletContext, serverName);
				//	remove
				//servletContext.removeAttribute(Constants.A_DOC_DB);
				//save us to server space
				servletContext.setAttribute(Constants.A_DOC_DB, myDocDB);

				//zrus MilonicMenu v GroupsDB
				//GroupsDB groupsDB = GroupsDB.getInstance();
				//groupsDB.clearMilonicMenu();

				return myDocDB;
			}
			else
			{
				DocDB myDocDB = (DocDB) servletContext.getAttribute(Constants.A_DOC_DB);
				if (myDocDB == null)
				{
					myDocDB = new DocDB(servletContext, serverName);
					//	remove
					//servletContext.removeAttribute(Constants.A_DOC_DB);
					//save us to server space
					servletContext.setAttribute(Constants.A_DOC_DB, myDocDB);

					//zrus MilonicMenu v GroupsDB
					//GroupsDB groupsDB = GroupsDB.getInstance();
					//groupsDB.clearMilonicMenu();
				}
				return myDocDB;
			}
		}
	}

	/**
	 * Constructor for the DocDB object
	 *
	 * @param servletContext
	 *           Description of the Parameter
	 * @param serverName
	 *           Description of the Parameter
	 */
	private DocDB(javax.servlet.ServletContext servletContext, String serverName)
	{
		Logger.println(this,"DocDB: constructor ["+Constants.getInstallName()+"]");
		Logger.debugMemInfo();

		this.serverName = serverName;

		//Logger.println(this,"DocDB: constructor");

		cachedDocs = new Hashtable<>();
		publicableDocs = new ArrayList<>();

		forceRefreshMasterSlaveMappings();

		try
		{
			//cache documents
			getDoc(-1, -1);
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
		}

		/** @todo nacitanie stranok na publikovanie */

		readPagesToPublic();
		//checkPublicable();

		loadUrls();
		//readVirtualPaths();

		ClusterDB.addRefresh(DocDB.class);

		Logger.debug(this,"DocDB: constructor ["+Constants.getInstallName()+"] done");
		Logger.debugMemInfo();
	}

	/**
	 * Vrati ci moze byt dokument zobrazeny
	 * @param docId
	 * @return true/false podla toho ci moze byt dokument zobrazeny podla datumov publikovania
	 */
	public boolean canBeShown(int docId)
	{
		if(docId < 0)
		{
			return false;
		}

		DocDetails doc = getDoc(docId);
		return canBeShown(doc);
	}

	public boolean canBeShown(DocDetails doc)
	{
		if(doc == null || doc.getDocId() < 1)
		{
			return false;
		}

		long start = doc.getPublishStart();
		long end = doc.getPublishEnd();
		long rightNow = Tools.getNow();

		return (start <= 0 || start <= rightNow) && (end <= 0 || end >= rightNow);
	}

	private void fillPublicableForm(PublicableForm pForm, ResultSet rs, boolean publicable) throws Exception
	{
		pForm.setDateCreated(DB.getDbTimestamp(rs, "date_created"));
		pForm.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
		pForm.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));
		pForm.setAuthorId(rs.getInt("author_id"));
		pForm.setSearchable(rs.getBoolean("searchable"));
		pForm.setGroupId(rs.getInt("group_id"));
		pForm.setAvailable(rs.getBoolean("available"));
		pForm.setShowInMenu(rs.getBoolean("show_in_menu"));
		pForm.setPasswordProtected(getDbString(rs, "password_protected"));

		pForm.setCacheable(rs.getBoolean("cacheable"));
		pForm.setExternalLink(getDbString(rs, "external_link"));
		pForm.setVirtualPath(getDbString(rs, "virtual_path"));
		pForm.setTempId(rs.getInt("temp_id"));
		pForm.setTitle(getDbString(rs, "title"));
		pForm.setNavbar(getDbString(rs, "navbar"));
		pForm.setFileName(getDbString(rs, "file_name"));
		pForm.setSortPriority(rs.getInt("sort_priority"));
		pForm.setHeaderDocId(rs.getInt("header_doc_id"));
		pForm.setFooterDocId(rs.getInt("footer_doc_id"));
		pForm.setMenuDocId(rs.getInt("menu_doc_id"));
		pForm.setRightMenuDocId(rs.getInt("right_menu_doc_id"));

		pForm.setHtmlHead(getDbString(rs, "html_head"));
		pForm.setHtmlData(getDbString(rs, "html_data"));
		pForm.setPerexPlace(getDbString(rs, "perex_place"));
		pForm.setPerexImage(getDbString(rs, "perex_image"));
		pForm.setPerexGroupString(getDbString(rs, "perex_group"));

		pForm.setDocId(rs.getInt("doc_id"));
		pForm.setData(DB.getDbString(rs, "data"));
		pForm.setData_asc(DB.getDbString(rs, "data_asc"));
		pForm.setViews_total(rs.getInt("views_total"));
		pForm.setViews_month(rs.getInt("views_month"));
		pForm.setFile_change(rs.getTimestamp("file_change"));
		pForm.setPublicable(publicable);
		if (publicable) pForm.setHistoryId(rs.getInt("history_id"));

		pForm.setEventDate(DB.getDbTimestamp(rs, "event_date"));

		pForm.setFieldA(DB.getDbString(rs, "field_a"));
		pForm.setFieldB(DB.getDbString(rs, "field_b"));
		pForm.setFieldC(DB.getDbString(rs, "field_c"));
		pForm.setFieldD(DB.getDbString(rs, "field_d"));
		pForm.setFieldE(DB.getDbString(rs, "field_e"));
		pForm.setFieldF(DB.getDbString(rs, "field_f"));

		pForm.setFieldG(DB.getDbString(rs, "field_g"));
		pForm.setFieldH(DB.getDbString(rs, "field_h"));
		pForm.setFieldI(DB.getDbString(rs, "field_i"));
		pForm.setFieldJ(DB.getDbString(rs, "field_j"));
		pForm.setFieldK(DB.getDbString(rs, "field_k"));
		pForm.setFieldL(DB.getDbString(rs, "field_l"));

		pForm.setDisableAfterEnd(rs.getBoolean("disable_after_end"));

		pForm.setFieldM(DB.getDbString(rs, "field_m"));
		pForm.setFieldN(DB.getDbString(rs, "field_n"));
		pForm.setFieldO(DB.getDbString(rs, "field_o"));
		pForm.setFieldP(DB.getDbString(rs, "field_p"));
		pForm.setFieldQ(DB.getDbString(rs, "field_q"));
		pForm.setFieldR(DB.getDbString(rs, "field_r"));
		pForm.setFieldS(DB.getDbString(rs, "field_s"));
		pForm.setFieldT(DB.getDbString(rs, "field_t"));

		pForm.setRequireSsl(rs.getBoolean("require_ssl"));
	}

	/**
	 * @todo nacitanie stranok na publikovanie
	 */
	public void readPagesToPublic()
	{
		DebugTimer dt = new DebugTimer("readPagesToPublic");

		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		PublicableForm pForm; // bean for saving data from documents_history especially publicable property

		List<PublicableForm> newPublicableDocs = new ArrayList<>();

		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT * FROM documents_history WHERE publicable=?";
			ps = db_conn.prepareStatement(sql);
			ps.setBoolean(1, true);

			dt.diff("executing sql="+sql);

			rs = ps.executeQuery();

			while (rs.next())
			{
				if (Tools.isNotEmpty(DB.getDbString(rs, "awaiting_approve")))
				{
					//tieto cakaju na schvalenie, nemozem vypublikovat
					continue;
				}

				pForm = new PublicableForm();
				fillPublicableForm(pForm, rs, true);

				newPublicableDocs.add(pForm);
			}
			rs.close();
			ps.close();

			sql = "SELECT * FROM documents WHERE disable_after_end=?";
			ps = db_conn.prepareStatement(sql);
			ps.setBoolean(1, true);

			dt.diff("executing sql="+sql);

			rs = ps.executeQuery();
			while (rs.next())
			{
				pForm = new PublicableForm();
				fillPublicableForm(pForm, rs, false);

				newPublicableDocs.add(pForm);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;

			dt.diff("done");

			publicableDocs = newPublicableDocs;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
	}

	/**
	 * @todo skontrolovat co treba a pripadne zmenit v DB
	 */

	private long lastPublicableCheck = 0;
	public void checkPublicable()
	{
		//budeme kontrolovat len raz za 5 sekund
		if (lastPublicableCheck + 5000 > System.currentTimeMillis())
		{
			return;
		}
		lastPublicableCheck = System.currentTimeMillis();

		//Logger.println(this,"DocDB - checkPublicable");

		/** @todo skontrolovat co treba a pripadne zmenit v DB */

		try
		{
			List<PublicableForm> copyDHtoD = new ArrayList<>(); // specify which pForm-s to copy from documents_history to documents
			List<PublicableForm> removeAfterEndList = new ArrayList<>();
			int index = -1;
			long now = Tools.getNow();
			if (publicableDocs!=null && publicableDocs.size()>0)
			{
				index = -1;
				for (PublicableForm pForm : publicableDocs)
				{
					index++;
					if (pForm.isDone()) continue;

					//Logger.println(this,now+" vs "+pForm.getPublishStart() + " = " + (now >= pForm.getPublishStart()));

					if(pForm.isPublicable() && (pForm.getPublishStart()>0) && (now >= pForm.getPublishStart()))
					{
						copyDHtoD.add(pForm);
						//aby sa nam to nasledne nevykonalo znova
						pForm.setDone(true);
						publicableDocs.set(index,pForm);
					}
					else if (pForm.isPublicable()==false && (pForm.getPublishEnd()>0) && (now >= pForm.getPublishEnd()))
					{
						removeAfterEndList.add(pForm);
						//aby sa nam to nasledne nevykonalo znova
						pForm.setDone(true);
						publicableDocs.set(index,pForm);
					}
				}
				if (copyDHtoD.size()>0)
				{
					copyDHistory(copyDHtoD);
				}
				if (removeAfterEndList.size()>0)
				{
					disableAfterEnd(removeAfterEndList);
				}
				/*
				 * BHR: zbytocne aby sa zakazdym refreshovali sablony, aj ked to nie je potrebne
				if (copyDHtoD.size()>0 || removeAfterEndList.size()>0)
				{
					TemplatesDB.getInstance(true);
					//musime refreshnut aj docdb kvoli cache objektom
					DocDB.getInstance(true);
				}
				*/
			}
		}
		catch (RuntimeException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

	}

	/**
	 * copy data from table documents_history to table documents
	 */
	private synchronized void copyDHistory(List<PublicableForm> copyDHtoD)
	{
		int h_id;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;

		String sql ="UPDATE documents SET title=?, data=?, data_asc=?, external_link=?, navbar=?, date_created=?, " +
				"publish_start=?, publish_end=?, author_id=?, group_id=?, temp_id=?, searchable=?, available=?, " +
				"cacheable=?, sort_priority=?, header_doc_id=?, footer_doc_id=?, menu_doc_id=?, password_protected=?, "+
				"html_head=?, html_data=?, perex_place=?, perex_image=?, perex_group=?, show_in_menu=?, event_date=?, "+
				"right_menu_doc_id=?, field_a=?, field_b=?, field_c=?, field_d=?, field_e=?, field_f=?, " +
				"field_g=?, field_h=?, field_i=?, field_j=?, field_k=?, field_l=?, disable_after_end=?, sync_status=1, " +
				"field_m=?, field_n=?, field_o=?, field_p=?, field_q=?, field_r=?, field_s=?, field_t=?, require_ssl=?, file_name=?, " +
				"root_group_l1=?, root_group_l2=?, root_group_l3=?, virtual_path=? " +
				"WHERE doc_id=?";

		if (!copyDHtoD.isEmpty())
		{
			try
			{
				db_conn = DBPool.getConnection(serverName);

				Map<Integer, GroupDetails> allGroups = getAllGroups();
				for (PublicableForm pForm : copyDHtoD) // bean for saving data from documents_history especially publicable property
				{

					Logger.debug(this,"Publishing from historyId: " + pForm.getHistoryId() + " docId: " + pForm.getDocId());

					h_id = pForm.getHistoryId();

					//zrus atribut actual=true
					//je to haluz, ale takto na 2x je to nad mySQL rychlejsie
					//skusal som aj indexy a nepomohlo...
					ps = db_conn.prepareStatement("SELECT history_id, publicable FROM documents_history WHERE doc_id=?");
					ps.setInt(1, pForm.getDocId());
					StringBuilder historyIds = null;
					rs = ps.executeQuery();
					boolean publicable = false;
					while (rs.next())
					{
						if (rs.getBoolean("publicable")) publicable = true;
						if (historyIds == null)
						{
							historyIds = new StringBuilder(String.valueOf(rs.getInt("history_id")));
						}
						else
						{
							historyIds.append(',').append(rs.getInt("history_id"));
						}
					}
					rs.close();
					ps.close();

					if (historyIds==null || publicable==false)
					{
						//asi sme cluster a uz to niekto aktualizoval
						continue;
					}

					ps = db_conn.prepareStatement("UPDATE documents_history SET actual=?, sync_status=1 WHERE history_id IN ("+ getOnlyNumbersIn(historyIds.toString())+")");
					ps.setBoolean(1, false);
					ps.execute();
					ps.close();

					//updatni zaznam v history, zrus publicable a nastav actual
					ps = db_conn.prepareStatement("UPDATE documents_history SET publicable=?, actual=?, approved_by=?, sync_status=1, available=? WHERE history_id=?");
					ps.setBoolean(1, false);
					ps.setBoolean(2, true);
					ps.setInt(3, pForm.getAuthorId());
					ps.setBoolean(4, true);
					ps.setInt(5,h_id);
					ps.execute();
					ps.close();

					DocDetails doc = getBasicDocDetails(pForm.getDocId(), false);
					boolean isTrash = true;
					if(doc != null)
					{

						// zisti ci sme v adresari /System/Trash (kos), ak ano, nepublikuj
						String navbarNoHref = DB.internationalToEnglish(GroupsDB.getURLPathDomainGroup(allGroups, doc.getGroupId())[0]).toLowerCase();
						//tu sa vytvara adresar podla default jazyka, nie podla prihlaseneho pouzivatela!
						Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
						String trashDirName = propSystem.getText("config.trash_dir");
						if (navbarNoHref.startsWith(DB.internationalToEnglish(trashDirName).toLowerCase())==false)
						{
							isTrash = false;
						}
					}

					if(isTrash == false)
					{
						//updatni tabulku documents
						ps = db_conn.prepareStatement(sql);

						ps.setString(1, pForm.getTitle());
						DB.setClob(ps, 2, pForm.getData());
						DB.setClob(ps, 3, pForm.getData_asc());
						ps.setString(4, pForm.getExternalLink());
						ps.setString(5, pForm.getNavbar());
						Logger.println(this,"pForm.getPublishStart()="+pForm.getPublishStart());
						ps.setTimestamp(6, DB.getDbTimestamp(pForm.getDateCreated()));
						ps.setTimestamp(7, DB.getDbTimestamp(pForm.getPublishStart()));
						ps.setTimestamp(8, DB.getDbTimestamp(pForm.getPublishEnd()));
						ps.setInt(9, pForm.getAuthorId());
						ps.setInt(10, pForm.getGroupId());
						ps.setInt(11, pForm.getTempId());
						ps.setBoolean(12, pForm.isSearchable());
						//ps.setBoolean(13,  pForm.isAvailable());
						ps.setBoolean(13,  true);
						ps.setBoolean(14, pForm.isCacheable());
						ps.setInt(15, pForm.getSortPriority());
						ps.setInt(16, pForm.getHeaderDocId());
						ps.setInt(17, pForm.getFooterDocId());
						ps.setInt(18, pForm.getMenuDocId());
						ps.setString(19, pForm.getPasswordProtected());
						DB.setClob(ps, 20, pForm.getHtmlHead());
						DB.setClob(ps, 21, pForm.getHtmlData());
						ps.setString(22, pForm.getPerexPlace());
						ps.setString(23, pForm.getPerexImage());
						ps.setString(24, pForm.getPerexGroupIdsString(true));
						ps.setBoolean(25, pForm.isShowInMenu());
						ps.setTimestamp(26, DB.getDbTimestamp(pForm.getEventDate()));

						ps.setInt(27, pForm.getRightMenuDocId());

						ps.setString(28, pForm.getFieldA());
						ps.setString(29, pForm.getFieldB());
						ps.setString(30, pForm.getFieldC());
						ps.setString(31, pForm.getFieldD());
						ps.setString(32, pForm.getFieldE());
						ps.setString(33, pForm.getFieldF());
						ps.setString(34, pForm.getFieldG());
						ps.setString(35, pForm.getFieldH());
						ps.setString(36, pForm.getFieldI());
						ps.setString(37, pForm.getFieldJ());
						ps.setString(38, pForm.getFieldK());
						ps.setString(39, pForm.getFieldL());

						ps.setBoolean(40, pForm.isDisableAfterEnd());

						ps.setString(41, pForm.getFieldM());
						ps.setString(42, pForm.getFieldN());
						ps.setString(43, pForm.getFieldO());
						ps.setString(44, pForm.getFieldP());
						ps.setString(45, pForm.getFieldQ());
						ps.setString(46, pForm.getFieldR());
						ps.setString(47, pForm.getFieldS());
						ps.setString(48, pForm.getFieldT());

						ps.setBoolean(49, pForm.isRequireSsl());

						GroupsDB groupsDB = GroupsDB.getInstance();
						GroupDetails group = groupsDB.getGroup(pForm.getGroupId());
						String fileName = null;
						if (group != null && group.isInternal()==false)
						{
							fileName = groupsDB.getGroupNamePath(pForm.getGroupId());
						}
						ps.setString(50, fileName);

						DocDB.getRootGroupL(pForm.getGroupId(), ps, 51);

						ps.setString(54, pForm.getVirtualPath());

						ps.setInt(55, pForm.getDocId());

						ps.execute();
						ps.close();

						// vypublikovanie slave clankov z historie (multikategorie)
						DocDetails masterDocDetails = null;
						for(Integer docId : MultigroupMappingDB.getSlaveDocIds(pForm.getDocId()))
						{
							if (masterDocDetails == null) masterDocDetails = getDoc(pForm.getDocId(), -1, false);
							DocDetails slaveDoc = getBasicDocDetails(docId, false);
							if (slaveDoc != null)
							{
								//teraz zmenme hodnoty pre master doc a ulozme do DB
								masterDocDetails.setVirtualPath(slaveDoc.getVirtualPath());
								masterDocDetails.setExternalLink(slaveDoc.getExternalLink());
								masterDocDetails.setDocId(docId);
								masterDocDetails.setGroupId(slaveDoc.getGroupId());
								DocDB.saveDoc(masterDocDetails);
							}

						}
						//DocDB.updateDataClob(db_conn, pForm.getDocId(), -1, pForm.getData(), pForm.getData_asc());;

						//prekopirovanie poznamky pre redaktorov k stranke
						DocNoteBean historyNote = DocNoteDB.getInstance().getDocNote(-1, pForm.getHistoryId());
						if(historyNote!=null)
						{
							DocNoteBean publishedNote = DocNoteDB.getInstance().getDocNote(pForm.getDocId(), -1);
							if(publishedNote==null)
							{
								publishedNote = new DocNoteBean();
							}
							publishedNote.setDocId(pForm.getDocId());
							publishedNote.setHistoryId(-1);
							publishedNote.setNote(historyNote.getNote());
							publishedNote.setUserId(historyNote.getUserId());
							publishedNote.setCreated(historyNote.getCreated());
							publishedNote.save();
						}
					}

					DocDB.getInstance().updateInternalCaches(pForm.getDocId());

					//refresh sablony sa vykona iba ak je stranka v System adresari
					if(pForm.getGroupId() == Constants.getInt("tempGroupId") || pForm.getGroupId() == Constants.getInt("menuGroupId") || pForm.getGroupId() == Constants.getInt("headerFooterGroupId"))
					{
						TemplatesDB.getInstance(true);
					}
				}
				db_conn.close();
				db_conn = null;
				ps = null;
				rs = null;
			}
			catch (Exception ex){Logger.error(DocDB.class, ex);}
			finally{
				try{
					if (rs != null) rs.close();
					if (ps != null) ps.close();
					if (db_conn != null) db_conn.close();
				}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
			}
		}
	}

	/**
	 * Zneplatni dokumenty ktore maju nastavene atribut disable_after_end a ich cas vyprsal
	 * @param disableAfterEndList
	 */
	private synchronized void disableAfterEnd(List<PublicableForm> disableAfterEndList)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE documents SET available=?, disable_after_end=? WHERE doc_id=?");

			for (PublicableForm pForm : disableAfterEndList)
			{
				ps.setBoolean(1, false);
				ps.setBoolean(2, false);
				ps.setInt(3, pForm.getDocId());
				ps.execute();
			}

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
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
				//
			}
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public int getVirtualPathDocId(String virtualPath)
	{
		//nevieme domenu
		return getVirtualPathDocId(virtualPath, "");
	}

	/**
	 * Ziska docId stranky so zadanym URL a zadanou domenou
	 * @param virtualPath
	 * @param domain
	 * @return
	 */
	public int getVirtualPathDocId(String virtualPath, String domain)
	{
		//disallow webpages URL starts with /admin
		if (virtualPath.startsWith("/admin")) return -1;

		if (virtualPath.startsWith("/css")||
			virtualPath.startsWith("/images")||
			virtualPath.startsWith("/files") ||
			virtualPath.startsWith("/jscripts"))
		{
			if (virtualPath.endsWith(".html") || virtualPath.endsWith("/")) {
				//allow to create webpage in this folder with .html or / extension
			} else {
				return -1;
			}
		}

		if (Tools.isEmpty(domain) || Constants.getBoolean("multiDomainEnabled")==false) domain = "default";
		TObjectIntHashMap<String> urlsByUrl = getUrlsByUrlDomains(domain, false);
		//Logger.debug(DocDB.class, "getVirtualPathDocId: domain="+domain+" hashSize="+urlsByUrl.size());
		return getVirtualPathDocId(virtualPath, urlsByUrl);
	}

	/**
	 * Vrati docId pre zadanu virtualnu cestu, alebo -1, ak zadana cesta nie je zadana
	 * @param virtualPath
	 * @return
	 */
	public int getVirtualPathDocId(String virtualPath, TObjectIntHashMap<String> urlsByUrl)
	{
		if (urlsByUrl!=null)
		{
			virtualPath = normalizeVirtualPath(virtualPath);

			int docId = urlsByUrl.get(virtualPath);
			if (docId!=0)
			{
				return docId;
			}

			if (virtualPath.endsWith(".gif") || virtualPath.endsWith(".jpg") ||
					virtualPath.endsWith(".css") || virtualPath.endsWith(".js"))
			{
				return(-1);
			}

			//skus najst /adr1/adr2/* cestu k adresaru
			try
			{
				//Logger.println(this,"testing: "+virtualPath+"*");
				int i = urlsByUrl.get(virtualPath+"*");
				if (i!=0)
				{
					return i;
				}

				int index = 1;
				int failsafe = 1;
				while (failsafe < 200 && index > 0)
				{
					index = virtualPath.lastIndexOf('/');
					if (index > 0)
					{
						virtualPath = virtualPath.substring(0, index);
						//Logger.println(this,"testing: "+virtualPath+"*");
						i = urlsByUrl.get(virtualPath+"*/");
						if (i!=0)
						{
							return i;
						}
						i = urlsByUrl.get(virtualPath+"/*/");
						if (i!=0)
						{
							return i;
						}
						i = urlsByUrl.get(virtualPath+"*");
						if (i!=0)
						{
							return i;
						}
						i = urlsByUrl.get(virtualPath+"/*");
						if (i!=0)
						{
							return i;
						}

					}
					failsafe++;
				}

			}
			catch (Exception e)
			{
				Logger.error(DocDB.class, e);
			}
		}
		return(-1);
	}

	/**
	 * Znormalizuje cestu, napr. odstrani koncove lomitko (napr /produkty/ -> /produkty),
	 * prida zaciatocne lomitko (ak nie je)
	 * @param virtualPath
	 * @return
	 */
	@SuppressWarnings("java:S1075")
	public static String normalizeVirtualPath(String virtualPath)
	{
		if (Tools.isEmpty(virtualPath)) return "";

		/*if (virtualPath.endsWith("/"))
		{
			virtualPath = virtualPath.substring(0, virtualPath.length()-1);
		}*/
		if (virtualPath.endsWith("/")==false && virtualPath.lastIndexOf('/')!=-1 && virtualPath.lastIndexOf('/')>=virtualPath.lastIndexOf('.'))
		{
			virtualPath = virtualPath + "/";
		}
		if (virtualPath.startsWith("/")==false)
		{
			virtualPath = "/" + virtualPath;
		}
		if (virtualPath.contains("//")) virtualPath = Tools.replace(virtualPath, "//", "/");
		//Logger.println(this,"vp="+virtualPath);
		return(virtualPath);
	}


	/**
	 *  Vrati web stranku
	 *
	 *@param  docId  ID stranky
	 *@return         The doc value
	 */
	public DocDetails getDoc(int docId)
	{
		if (docId < 1) return null;

		return (getDoc(docId, -1));
	}

	/**
	 *  vrati web stranku
	 *
	 *@param  doc_id      id stranky
	 *@param  history_id  id historie (doc_id musi byt -1)
	 *@return             The doc value
	 */
	public DocDetails getDoc(int doc_id, int history_id)
	{
		return getDoc(doc_id, history_id, true);
	}

	/**
	 * Hodnotu useCache=false pouzivame v pripade ked vysledny docDetails modifikujeme
	 * ak je totiz nastavena premenna cacheDocDetailsNewerThanDays na true docDetails sa cachuje
	 * podobne ak ma nastaveny atribut cache
	 * @param doc_id
	 * @param history_id
	 * @param useCache
	 * @return
	 */
	public DocDetails getDoc(int doc_id, int history_id, boolean useCache)
	{
		boolean dbConnectionOK = false;

		String sql = "";

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			DocDetails doc = null;

			if (history_id < 0)
			{
				if (doc_id == -1)
				{
					//oracle: sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, d.* FROM documents d, users u WHERE (d.author_id(+)=u.user_id  AND cacheable="+DB.getBooleanSql(true)+")";
					sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, "+DocDB.getDocumentFields()+" FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE cacheable="+DB.getBooleanSql(true);
					if (Constants.DB_TYPE == Constants.DB_ORACLE)
					{
						sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, "+DocDB.getDocumentFields()+" FROM documents d,  users u WHERE d.author_id=u.user_id(+) AND cacheable="+DB.getBooleanSql(true);
					}
					cachedDocs = new Hashtable<>();
				}
				else
				{
					//try to find document in cache
					if (useCache && cachedDocs!=null)
					{
						doc = cachedDocs.get(Integer.valueOf(doc_id));
						if (doc!=null)
						{
							return (doc);
						}
					}
					sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, "+DocDB.getDocumentFields()+" FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE doc_id= ?";
					if (Constants.DB_TYPE == Constants.DB_ORACLE)
					{
						sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, "+DocDB.getDocumentFields()+" FROM documents d,  users u WHERE d.author_id=u.user_id(+) AND doc_id= ?";
					}
				}
			}
			else
			{
				sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, h.* FROM documents_history h LEFT JOIN  users u ON h.author_id=u.user_id WHERE history_id= ?";
				if (Constants.DB_TYPE == Constants.DB_ORACLE)
				{
					sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, h.* FROM documents_history h,  users u WHERE h.author_id=u.user_id(+) AND history_id= ?";
				}
				doc_id = 0;
			}

			if (Constants.getBoolean("docAuthorLazyLoad") && history_id < 0 && doc_id != -1)
				sql = "SELECT "+DocDB.getDocumentFields()+" FROM documents d WHERE doc_id= ?";
			if (Constants.getBoolean("docAuthorLazyLoad") && history_id >= 0)
				sql = "SELECT * FROM documents_history h WHERE history_id = ?";
			if (Constants.getBoolean("docAuthorLazyLoad") && history_id < 0 && doc_id == -1)
				sql = "SELECT "+DocDB.getDocumentFields()+" FROM documents d WHERE cacheable="+DB.getBooleanSql(true);

			db_conn = DBPool.getConnectionReadUncommited();
			ps = db_conn.prepareStatement(sql);
			if (history_id >= 0)
				ps.setInt(1, history_id);
			if (doc_id != -1 && history_id < 0)
				ps.setInt(1, doc_id);
			//ps.setInt(1, user.getUserId());
			//Logger.println(this,"sql="+sql);
			rs = ps.executeQuery();

			dbConnectionOK = true;

			while (rs.next())
			{
				if (Constants.getBoolean("docAuthorLazyLoad"))
					doc = getDocDetails(rs, false, true);
				else
					doc = getDocDetails(rs, false);

				if (history_id > 0)
				{
					doc.setHistoryApprovedBy(rs.getInt("approved_by"));
					doc.setHistoryDisapprovedBy(rs.getInt("disapproved_by"));
					doc.setHistoryActual(rs.getBoolean("actual"));
				}

				if (history_id>-1)
					doc.setPublicable(rs.getBoolean("publicable"));//lubbis


				if (doc_id == -1 && history_id == -1)
				{
					cachedDocs.put(Integer.valueOf(doc_id), doc);
				}
				else
				{
					int cacheNewerDays = Constants.getInt("cacheDocDetailsNewerThanDays");
					if (useCache && cacheNewerDays>0)
					{
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DATE, -cacheNewerDays);
						if (doc.getDateCreated()>cal.getTimeInMillis())
						{
							Logger.debug(DocDB.class, "Caching web page: "+doc.getDocId()+" date="+doc.getDateCreatedString());
							cachedDocs.put(Integer.valueOf(doc_id), doc);
						}
					}
					rs.close();
					ps.close();
					db_conn.close();

					rs = null;
					ps = null;
					db_conn = null;

					return (doc);
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
			Logger.error(DocDB.class, ex);
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

		//toto reportujeme aby sme v showdocaction mohli vypisat chybu
		if (dbConnectionOK==false) throw new NullPointerException();

		return (null);
	}

	/**
	 * Vrati web stranku na zaklade nazvu, ak je zadane groupId tak v danom adresari
	 * @param title
	 * @param groupId
	 * @return
	 */
	public DocDetails getDocByTitle(String title, int groupId)
	{
		DocDetails doc = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			StringBuilder sql = new StringBuilder("SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN users u ON d.author_id=u.user_id WHERE d.title=?");
			if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				sql = new StringBuilder("SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d, users u WHERE d.author_id=u.user_id(+) AND d.title=?");
			}
			if (groupId > 0) sql.append(" AND d.group_id=?");

			db_conn = DBPool.getConnectionReadUncommited();
			ps = db_conn.prepareStatement(sql.toString());
			ps.setString(1, title);
			if (groupId > 0) ps.setInt(2, groupId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				doc = new DocDetails();
				getDocDetails(rs, doc, false, false);
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
			Logger.error(DocDB.class, ex);
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

		return doc;
	}

	/**
	 *  Vrati stranky v zadanom adresari
	 *
	 *@param  group_id  Description of the Parameter
	 *@return           The docByGroup value
	 */
	public List<DocDetails> getDocByGroup(int group_id)
	{
		return (getDocByGroup(group_id, ORDER_TITLE, true, -1, -1, false));
	}

	/**
	 *  vrati stranky v zadanom adresari
	 *
	 *@param  groupId    id skupiny
	 *@param  orderType  sposob usporiadania
	 *@param  asc        smer usporiadania, ak true vzostupne
	 *@param  start      poradove cislo zaciatku (strankovanie)
	 *@param  end        poradove cislo konca (strankovanie)
	 *@param  no_data    ak je true nevracia sa obsah stlpca data
	 *@return            List s dokumentami v skupine groupId
	 */
	public List<DocDetails> getDocByGroup(int groupId, int orderType, boolean asc, int start, int end, boolean no_data)
	{
		return getDocByGroup(groupId, orderType, asc, start, end, no_data, true);
	}

	/**
	 *  vrati stranky v zadanom adresari
	 *
	 *@param  groupId    id skupiny
	 *@param  orderType  sposob usporiadania
	 *@param  asc        smer usporiadania, ak true vzostupne
	 *@param  start      poradove cislo zaciatku (strankovanie)
	 *@param  end        poradove cislo konca (strankovanie)
	 *@param  no_data    ak je true nevracia sa obsah stlpca data
	 *@param  onlyAvailable ak je true vrati len stranky s availabe=1
	 *@return            List s dokumentami v skupine groupId
	 */
	public List<DocDetails> getDocByGroup(int groupId, int orderType, boolean asc, int start, int end, boolean no_data, boolean onlyAvailable)
	{
		List<DocDetails> ret = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();

			//read user permissions for every org struct

			StringBuilder order = new StringBuilder("d.title");
			if (orderType == ORDER_DATE)
			{
				order = new StringBuilder("publish_start");
			}
			else if (orderType == ORDER_ID)
			{
				order = new StringBuilder("doc_id");
			}
			else if (orderType == ORDER_PRIORITY)
			{
				order = new StringBuilder("sort_priority");
			}
			else if (orderType == ORDER_PLACE)
			{
				order = new StringBuilder("perex_place");
			}
			else if (orderType == ORDER_EVENT_DATE)
			{
				order = new StringBuilder("event_date");
			}
			else if (orderType == ORDER_SAVE_DATE)
			{
				order = new StringBuilder("date_created");
			}
			else if (orderType == ORDER_RATING)
			{
				order = new StringBuilder("forum_count");
			}

			if (asc)
			{
				order.append(" ASC");
			}
			else
			{
				order.append(" DESC");
			}

			if (orderType == ORDER_PLACE || orderType == ORDER_PRIORITY)
			{
				//ak sme podla mesta, ostatne sortni podla title
				order.append(", d.title ASC");
			}

			String sql;
			DocDetails doc;

			String onlyAvailableAppend = "";
			if (onlyAvailable) {
				onlyAvailableAppend = "available="+DB.getBooleanSql(true)+" AND";
				if (Constants.DB_TYPE == Constants.DB_ORACLE) onlyAvailableAppend = "AND available="+DB.getBooleanSql(true)+" AND";
			}

			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				String top = "";
				if (end > 0)
				{
					top = "TOP " + end;
				}
				sql = "SELECT " + top + " u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE "+onlyAvailableAppend+" group_id=" + groupId + " ORDER BY " + order.toString();
			}
			else if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				//POZOR: ORA tu chyba limit!
				sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d,  users u WHERE d.author_id=u.user_id(+) "+onlyAvailableAppend+" group_id=" + groupId + " ORDER BY " + order.toString();
			}
			else
			{
				String limit = "";
				if (start >= 0 && end > 0)
				{
					int rows = end - start;
					limit = " LIMIT " + start + ", " + rows;
					if (Constants.DB_TYPE==Constants.DB_PGSQL) limit = " OFFSET " + start + " LIMIT " + rows;
					start = 0;
					end = rows + 1;
					//?? je treba +1 ??
				}
				sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE "+onlyAvailableAppend+" group_id=" + groupId + " ORDER BY " + order + limit;
			}
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			int counter = 0;
			if (start > 0)
			{
				int i;
				for (i=0; i<start; i++)
				{
					//jTDS nevie rs.absolute...
					//rs.absolute(index);
					rs.next();
				}
				counter = start;
			}
			while (rs.next())
			{
				if (end > 0 && counter > end)
				{
					counter++;
					break;
				}

				counter++;

				doc = getDocDetails(rs, no_data);

				ret.add(doc);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (ret);
	}

    /**
     *  Vrati stranky v zadanom adresari
     *
     *@param  domainId  Description of the Parameter
     *@return           The docByDomainId value
     */
    public List<DocDetails> getDocByDomainId(int domainId)
    {
        return (getDocByDomainId(domainId, ORDER_TITLE, true, -1, -1, false));
    }

    /**
     *  vrati stranky v zadanom adresari
     *
     *@param  domainId    id skupiny
     *@param  orderType  sposob usporiadania
     *@param  asc        smer usporiadania, ak true vzostupne
     *@param  start      poradove cislo zaciatku (strankovanie)
     *@param  end        poradove cislo konca (strankovanie)
     *@param  no_data    ak je true nevracia sa obsah stlpca data
     *@return            List s dokumentami v skupine groupId
     */
    public List<DocDetails> getDocByDomainId(int domainId, int orderType, boolean asc, int start, int end, boolean no_data)
    {
        List<DocDetails> ret = new ArrayList<>();
        java.sql.Connection db_conn = null;
        java.sql.PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            db_conn = DBPool.getConnectionReadUncommited();

            //read user permissions for every org struct

            StringBuilder order = new StringBuilder("d.title");
            if (orderType == ORDER_DATE)
            {
                order = new StringBuilder("publish_start");
            }
            else if (orderType == ORDER_ID)
            {
                order = new StringBuilder("doc_id");
            }
            else if (orderType == ORDER_PRIORITY)
            {
                order = new StringBuilder("sort_priority");
            }
            else if (orderType == ORDER_PLACE)
            {
                order = new StringBuilder("perex_place");
            }
            else if (orderType == ORDER_EVENT_DATE)
            {
                order = new StringBuilder("event_date");
            }
            else if (orderType == ORDER_SAVE_DATE)
            {
                order = new StringBuilder("date_created");
            }
            else if (orderType == ORDER_RATING)
            {
                order = new StringBuilder("forum_count");
            }

            if (asc)
            {
                order.append(" ASC");
            }
            else
            {
                order.append(" DESC");
            }

            if (orderType == ORDER_PLACE || orderType == ORDER_PRIORITY)
            {
                //ak sme podla mesta, ostatne sortni podla title
                order.append(", d.title ASC");
            }

            String sql;
            DocDetails doc;

            if (Constants.DB_TYPE == Constants.DB_MSSQL)
            {
                String top = "";
                if (end > 0)
                {
                    top = "TOP " + end;
                }
                sql = "SELECT " + top + " u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE available="+DB.getBooleanSql(true)+" AND root_group_l1=" + domainId + " ORDER BY " + order.toString();
            }
            else if (Constants.DB_TYPE == Constants.DB_ORACLE)
            {
                //POZOR: ORA tu chyba limit!
                sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d,  users u WHERE d.author_id=u.user_id(+) AND available="+DB.getBooleanSql(true)+" AND root_group_l1=" + domainId + " ORDER BY " + order.toString();
            }
            else
            {
                String limit = "";
                if (start >= 0 && end > 0)
                {
                    int rows = end - start;
                    limit = " LIMIT " + start + ", " + rows;
					if (Constants.DB_TYPE==Constants.DB_PGSQL) limit = " OFFSET " + start + " LIMIT " + rows;
                    start = 0;
                    end = rows + 1;
                    //?? je treba +1 ??
                }
                sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE available="+DB.getBooleanSql(true)+" AND root_group_l1=" + domainId + " ORDER BY " + order + limit;
            }
            ps = db_conn.prepareStatement(sql);
            rs = ps.executeQuery();

            int counter = 0;
            if (start > 0)
            {
                int i;
                for (i=0; i<start; i++)
                {
                    //jTDS nevie rs.absolute...
                    //rs.absolute(index);
                    rs.next();
                }
                counter = start;
            }
            while (rs.next())
            {
                if (end > 0 && counter > end)
                {
                    counter++;
                    break;
                }

                counter++;

                doc = getDocDetails(rs, no_data);

                ret.add(doc);
            }
            rs.close();
            ps.close();
            db_conn.close();
            db_conn = null;
            ps = null;
            rs = null;
        }
        catch (Exception ex){Logger.error(DocDB.class, ex);}
        finally{
            try{
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (db_conn != null) db_conn.close();
            }catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
        }
        return (ret);
    }

	/**
	 * Vrati stranky pre zadanu userGroupId, pouziva sa pre zistenie dostupnych emailov
	 * pre danu emailovu skupinu
	 * @param userGroupId
	 * @return
	 */
	public List<DocDetails> getDocByUserGroup(int userGroupId)
	{
		List<DocDetails> ret = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();

			//read user permissions for every org struct

			String sql;
			DocDetails doc;


			sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, "+getDocumentFieldsNodata()+" FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE available="+DB.getBooleanSql(true)+" AND password_protected LIKE '%" + userGroupId + "%' ORDER BY date_created DESC";
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next())
			{
				doc = getDocDetails(rs, true);

				//ak to tam skutocne je, pridaj
				if ((","+doc.getPasswordProtected()+",").indexOf(","+userGroupId+",")!=-1)
				{
					ret.add(doc);
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (ret);
	}

	/**
	 * Pripravi zoznam stranok, co maju perex v adresaroch groupIds a potrebne id sablony, vratane
	 * strankovania a handlovania parametrov
	 *
	 * @param groupIds
	 * @param orderType
	 * @param asc
	 * @param publishType
	 * @param pageSize
	 * @param arrayListDocName
	 * @param arrayListPagerName
	 * @param request
	 * @param tempId
	 * @return
	 */
	public int getDocPerex(String groupIds, int orderType, boolean asc, int publishType, int pageSize, String arrayListDocName, String arrayListPagerName, HttpServletRequest request, final int tempId)
	{
		SelectionFilter<DocDetails> filter;
		//ak nemame zadane tempId alebo je zaporne, tak vracaj vsetky dokumenty
		if (tempId <= 0)
			filter = new SelectionFilter<DocDetails>(){
			@Override
			public boolean fullfilsConditions(DocDetails candidate){return true;}};
			//inak musi byt id sablony dokumentu zhodne s parametrom
			else
				filter = new SelectionFilter<DocDetails>(){
				@Override
				public boolean fullfilsConditions(DocDetails candidate){
					return candidate.getTempId() == tempId;
				}};

				return getDocPerex(groupIds, orderType, asc, publishType, pageSize, arrayListDocName, arrayListPagerName, request, filter);
	}

	public int getDocumentsByIds(List<Integer> docIds, String arrayListDocName, String arrayListPagerName, HttpServletRequest request)
	{
		String groupIds = "";
		int orderType = DocDB.ORDER_ID;
		boolean asc = false;
		int publishType = DocDB.PUBLISH_NO_PEREX_CHECK_NEW;
		int pageSize = docIds.size();

		String docIdsStr = docIds.size() > 0 ? Tools.join(docIds) : "null";
		String whereSql = " AND doc_id IN ( " + docIdsStr + ") ";
		String oldWhereSql = (String) request.getAttribute("whereSql");
		request.setAttribute("whereSql", (Tools.isEmpty(oldWhereSql) ? "" : oldWhereSql) + whereSql);

		SelectionFilter<DocDetails> filter;
		filter = new SelectionFilter<DocDetails>(){
		@Override
		public boolean fullfilsConditions(DocDetails candidate){return true;}};

		return getDocPerex(groupIds, orderType, asc, publishType, pageSize, arrayListDocName, arrayListPagerName, request, filter);
	}

	/**
	 * Pripravi zoznam stranok pre news komponentu
	 * @param groupIds - id adresara
	 * @param orderType
	 * @param asc
	 * @param publishType - typ publikovania (konstanty PUBLISH_)
	 * @param pageSize - velkost stranky
	 * @param arrayListDocName	- pod tymto nazvom nastavi do requestu zoznam stranok
	 * @param arrayListPagerName	- pod tymto nazvom nastavi do requestu strankovanie
	 * @param request
	 * @param filter
	 * @return
	 */
	public int getDocPerex(String groupIds, int orderType, boolean asc, int publishType, int pageSize, String arrayListDocName, String arrayListPagerName, HttpServletRequest request, SelectionFilter<DocDetails> filter)
	{

		request.removeAttribute(arrayListPagerName);
		request.removeAttribute(arrayListDocName);

		//strankovanie
		int page = 1;

		PageParams pageParams = new PageParams(request);

		if (request.getAttribute("DocDB.pageParams") != null)
			pageParams = (PageParams)request.getAttribute("DocDB.pageParams");

		String pageParamName = pageParams.getValue("pageParamName", "page");
		try
		{
			if (request.getParameter(pageParamName) != null)
			{
				page = Integer.parseInt(request.getParameter(pageParamName));
			}
		}
		catch (Exception ex)
		{
			//
		}

		//ak sa nema strankovat, ignoruj parameter
		boolean paging = pageParams.getBooleanValue("paging", false);
		try
		{
			if (request.getParameter("paging") != null)
			{
				paging = Boolean.parseBoolean(request.getParameter("paging"));
			}
		}
		catch (Exception ex)
		{
			//
		}
		if (paging == false)
		{
			page = 1;
		}

		String p_order = request.getParameter("order");
		if (p_order != null)
		{
			if (p_order.compareTo("date") == 0)
			{
				orderType = DocDB.ORDER_DATE;
			}
			else if (p_order.compareTo("id") == 0)
			{
				orderType = DocDB.ORDER_ID;
			}
			else if (p_order.compareTo("priority") == 0)
			{
				orderType = DocDB.ORDER_PRIORITY;
			}
			else if (p_order.compareTo("title") == 0)
			{
				orderType = DocDB.ORDER_TITLE;
			}
			else if (p_order.compareTo("place") == 0)
			{
				orderType = DocDB.ORDER_PLACE;
			}
			else if (p_order.compareTo("save_date") == 0)
			{
				orderType = DocDB.ORDER_SAVE_DATE;
			}
		}
		if (request.getParameter("desc") != null && request.getParameter("desc").compareToIgnoreCase("yes")==0)
		{
			asc = false;
		}
		if (request.getParameter("asc") != null && request.getParameter("asc").compareToIgnoreCase("yes")==0)
		{
			asc = true;
		}
		String p_publish = request.getParameter("publish");
		if (p_publish!=null)
		{
			if (p_publish.compareToIgnoreCase("new")==0)
			{
				publishType = PUBLISH_NEW;
			}
			else if (p_publish.compareToIgnoreCase("old")==0)
			{
				publishType = PUBLISH_OLD;
			}
			else if (p_publish.compareToIgnoreCase("all")==0)
			{
				publishType = PUBLISH_ALL;
			}
			else if (p_publish.compareToIgnoreCase("next")==0)
			{
				publishType = PUBLISH_NEXT;
			}
			else if (p_publish.compareToIgnoreCase("npcnew")==0)
			{
				publishType = PUBLISH_NO_PEREX_CHECK_NEW;
			}
			else if (p_publish.compareToIgnoreCase("npcold")==0)
			{
				publishType = PUBLISH_NO_PEREX_CHECK_OLD;
			}
			else if (p_publish.compareToIgnoreCase("npcall")==0)
			{
				publishType = PUBLISH_NO_PEREX_CHECK_ALL;
			}
			else if (p_publish.compareToIgnoreCase("npcnext")==0)
			{
				publishType = PUBLISH_NO_PEREX_CHECK_NEXT;
			}

		}
		try
		{
			if (request.getParameter("psize") != null)
			{
				pageSize = Integer.parseInt(request.getParameter("psize"));
			}
		}
		catch (Exception ex)
		{
			//
		}

		//ziskaj zoznam dokumentov
		int start = (page - 1) * pageSize;
		int end = start + pageSize;

		//Logger.println(this,"start="+start+" end="+end+" p_size="+p_size);

		long now = -1;
		String perexNow = request.getParameter("perexNow");
		if (perexNow==null)
		{
			perexNow = (String)request.getAttribute("perexNow");
		}

		if (perexNow!=null)
		{
			try
			{
				now = Long.parseLong(perexNow);
			}
			catch (Exception ex)
			{
				//
			}
		}

		StringBuilder whereSql = request.getAttribute("whereSql") == null ? new StringBuilder() : new StringBuilder(Tools.replace(request.getAttribute("whereSql").toString(), " doc_id", " d.doc_id"));
		String orderSql = (String)request.getAttribute("orderSql");
		request.removeAttribute("orderSql");

		String perexGroup = null;
		if (request.getAttribute("perexGroup")!=null)
		{
			//najskor to skus ziskat z requestu
			perexGroup = (String)request.getAttribute("perexGroup");
		}
		else
		{
			try
			{
				//skus ziskat perexGroup podla sablony
				//je to tam vo formate <perexGroup name="meno" label="nazov skupiny">
				String afterBodyData = (String)request.getAttribute("after_body");
				if (afterBodyData!=null)
				{
					String perexGroupLabel = null;
					//format: <perexGroup name="ajurvedska" label="Ajurvedska strava">
					int startIndex = afterBodyData.indexOf("<perexGroup name=\"");
					if (startIndex!=-1)
					{
						//najdi prve uvodzovky
						startIndex = afterBodyData.indexOf('\"', startIndex);
						//najdi konciace uvodzovky
						int endIndex = afterBodyData.indexOf("\"", startIndex+1);
						if (endIndex>startIndex)
						{
							perexGroup = afterBodyData.substring(startIndex+1, endIndex);

							perexGroupLabel = perexGroup;
							//najdi uvodzovky pre label="
							startIndex = afterBodyData.indexOf("\"", endIndex+1);
							endIndex = afterBodyData.indexOf("\"", startIndex+1);
							if (endIndex>startIndex)
							{
								perexGroupLabel = afterBodyData.substring(startIndex+1, endIndex);
							}
							request.setAttribute("perex_group_label", perexGroupLabel);
						}
					}
				}
			}
			catch (Exception ex)
			{
				Logger.error(DocDB.class, ex);
			}
		}

		if (perexGroup!=null)
		{
			if(Constants.getBoolean("perexGroupUseJoin"))
			{
				StringBuilder groupNamesIn = new StringBuilder();
				String[] groupNames = Tools.getTokens(perexGroup, ",", true);
				String gn;
				int pid = -1;
				PerexGroupBean pgb = null;
				for (int j=0; j<groupNames.length; j++)
				{
					pid = -1;
					gn = groupNames[j];
					if(Tools.getIntValue(gn,0) > 0) pid = Tools.getIntValue(gn,0);
					else
					{
						pgb = getPerexGroup(-1, gn);
						if (pgb != null) pid = pgb.getPerexGroupId();
					}

					if(pid > 0) groupNamesIn.append(gn).append(",");
				}

				if(Tools.isNotEmpty(groupNamesIn)) whereSql.append(" AND p.perex_group_id IN ("+(groupNamesIn.toString().endsWith(",") ? groupNamesIn.substring(0, groupNamesIn.length()-1) : groupNamesIn)+") ");
			}
			else
			{
				whereSql.append(" AND ( ");
				StringTokenizer st = new StringTokenizer(perexGroup, ",");
				int counter = 0;

				String perexGroupMode = " OR ";
				if ("and".equals(request.getAttribute("perexGroupMode"))) perexGroupMode = " AND ";
				request.removeAttribute("perexGroupMode");

				while (st.hasMoreTokens())
				{
					String token = st.nextToken().trim();
					int pid = Tools.getIntValue(token, -1);
					if (pid < 1)
					{
						PerexGroupBean pgb = getPerexGroup(-1, token);
						if (pgb != null) pid = pgb.getPerexGroupId();
					}

					if (pid < 1) continue;

					if (counter > 0)
					{
						whereSql.append(perexGroupMode);
					}
					whereSql.append(" perex_group LIKE '%,").append(pid).append(",%' ");
					counter ++;
				}
				if (whereSql.toString().endsWith("AND ( "))
				{
					whereSql = new StringBuilder(whereSql.substring(0,whereSql.length()-6).trim()+" ");
				}
				else
				{
					whereSql.append(" ) ");
				}
			}
		}

		Identity user = UsersDB.getCurrentUser(request);
		boolean searchAlsoProtectedPages = pageParams.getBooleanValue("searchAlsoProtectedPages", false);
		try{
			if(request.getParameter("searchAlsoProtectedPages")!=null)
			{
				searchAlsoProtectedPages = Tools.getBooleanValue(request.getParameter("searchAlsoProtectedPages"), false);
			}
		}
		catch(Exception e)
		{
			//
		}
		if (user == null && searchAlsoProtectedPages==false)
		{
			whereSql.append(" AND (password_protected IS null OR password_protected='') ");
		}
		else if (user != null && searchAlsoProtectedPages==false)
		{
			//skupiny pre web stranky
			StringBuilder sqlProtected = new StringBuilder(" AND (password_protected IS null OR password_protected=''");
			StringTokenizer stu = new StringTokenizer(user.getUserGroupsIds(), ",");
			while (stu.hasMoreTokens())
			{
				int groupId = Tools.getIntValue(stu.nextToken(), -1);
				if (groupId > 0)
				{
					sqlProtected.append(" OR password_protected='").append(groupId).append("' OR password_protected LIKE '")
					.append(groupId).append(",%' OR password_protected LIKE '%,").append(groupId).append(",%' OR password_protected LIKE '%,").append(groupId).append('\'');
				}
			}
			sqlProtected.append(") ");
			whereSql.append(sqlProtected.toString());
		}

		boolean expandGroupIds = pageParams.getBooleanValue("expandGroupIds", true);
		if(!expandGroupIds)
			expandGroupIds = "true".equals(request.getAttribute("expandGroupIds"));
		String expandedGroupIds = null;
		if (expandGroupIds)
		{
			StringBuilder searchGroups = null;
			StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
			GroupsDB groupsDB = GroupsDB.getInstance();
			List<GroupDetails> searchGroupsArray;
			int searchRootGroupId;
			while (st.hasMoreTokens())
			{
				try
				{
					String sid = st.nextToken();
					sid = sid.replace('(', ' ');
					sid = sid.replace(')', ')');
					sid = sid.trim();
					searchRootGroupId = Integer.parseInt(sid);
				}
				catch (Exception ex)
				{
					Logger.error(DocDB.class, "groupIds="+groupIds);
					Logger.error(DocDB.class, ex);
					continue;
				}
				//najdi child grupy
				searchGroupsArray = groupsDB.getGroupsTree(searchRootGroupId, true, false);
				for (GroupDetails group : searchGroupsArray)
				{
					if (group != null)
					{
						if (searchGroups == null)
						{
							searchGroups = new StringBuilder(String.valueOf(group.getGroupId()));
						}
						else
						{
							searchGroups.append(',').append(group.getGroupId());
						}
					}
				}
			}
			if (searchGroups!=null) expandedGroupIds = searchGroups.toString();
		}

		//ak je TRUE, tak nenacitam data stlpec
		boolean noData = pageParams.getBooleanValue("noData", false);
		//ak je TRUE, tak chcem kontrolovat duplicitu pre multikategorie
		boolean checkDuplicity = false;
		if(request.getAttribute("checkDuplicity") != null) checkDuplicity = "true".equals(request.getAttribute("checkDuplicity"));
		else checkDuplicity = pageParams.getBooleanValue("checkDuplicity", false);
		StringBuilder docIdsNotIn = new StringBuilder("");
		if(checkDuplicity)
		{
			List<Integer> slavesMasterForDoc = null;

			if(getSlavesMasterMappings() != null)
			{
				String[] groupIdsTokens = Tools.getTokens(getOnlyNumbersIn((expandedGroupIds != null ? expandedGroupIds : groupIds).replace('+', ','),true), ",");
				List<DocDetails> docsInGroups = new ArrayList<>(); //vsetky stranky v zadanych adresaroch
				for(String g : groupIdsTokens)
					docsInGroups.addAll(getBasicDocDetailsByGroup(Tools.getIntValue(g,0), 0));

				if(docsInGroups.size() > 0)
				{
					HashMap<Integer, Boolean> docsInGroupsHm = new HashMap<>();
					for(DocDetails dd : docsInGroups)
						docsInGroupsHm.put(Integer.valueOf(dd.getDocId()), Boolean.TRUE);

					for (Entry<Integer, Boolean> entry : docsInGroupsHm.entrySet())
					{
						if(entry.getValue() != Boolean.FALSE)
						{
							Integer docId = entry.getKey();
							//ziskam zoznam docId, ktore su rovnake k danej stranke
							slavesMasterForDoc = null;

							if(slavesMasterForDoc == null)
								slavesMasterForDoc = new ArrayList<>();

							Integer masterId = getSlavesMasterMappings().get(docId);
							if(masterId != null)
							{
								//ak sa master nachadza v zozname stranok, tak ponecham master miesto slave a vymazem vsetky slave stranky danej master stranky
								//v opacnom pripade vymazem vsetky ostatne slaves
								boolean containsMaster = docsInGroupsHm.get(masterId) != null && docsInGroupsHm.get(masterId) == Boolean.TRUE;
								Integer[] slaves = getMasterMappings().get(masterId);
								if(slaves != null)
								{
									if(containsMaster) //aby preskocilo pri iteracii master
										docsInGroupsHm.put(masterId, Boolean.FALSE);
									for(Integer slave : slaves)
									{
										if(containsMaster || (!containsMaster && slave.intValue() != docId.intValue()))
											slavesMasterForDoc.add(slave);
									}
								}
							}
							else
							{
								//ak docId je master, tak pridam do duplicitnych vsetky jeho slaves
								Integer[] slaves = getMasterMappings().get(docId);
								if(slaves != null)
								{
									slavesMasterForDoc.addAll(Arrays.asList(slaves));
								}
							}
							//odstranim ich zo zonamu stranok
							if(slavesMasterForDoc != null && slavesMasterForDoc.size() > 0)
							{
								for(Integer sm: slavesMasterForDoc)
								{
									if(docsInGroupsHm.get(sm) != null)
									{
										docsInGroupsHm.put(sm, Boolean.FALSE);
										docIdsNotIn.append(sm.intValue()).append(",");
									}
								}
							}
						}
					}
				}
			}

			if(Tools.isNotEmpty(docIdsNotIn))
				docIdsNotIn = new StringBuilder(docIdsNotIn.toString().substring(0, docIdsNotIn.length()-1));
		}

		//kvoli blogom aby prihlaseny blogger videl na stranke aj nepublikovane clanky
		boolean onlyAvailable = true;
		if ("false".equals(request.getAttribute("perexOnlyAvailable")))
		{
			onlyAvailable = false;
			request.removeAttribute("perexOnlyAvailable");
		}

		//pokial je zadany datum pre ktory sa maju novinky vypisat
		String datum = request.getParameter("datum") != null ? request.getParameter("datum") : null;
		if (request.getAttribute("newsDatum")!=null)
		{
			//aby bolo mozne mat v stranke viacero komponent ale len jedna bude filtrovana podla datumu
			//ostatnym nastavime atribut datum na znak -
			datum = (String)request.getAttribute("newsDatum");
			if ("-".equals(datum)) datum = null;
			request.removeAttribute("newsDatum");
		}

		if(expandedGroupIds != null && expandGroupIds)
		{
			int rootGroupLevel = 1000;
			//ak groupId je na vacsej urovni ako 3, pouzijeme expandedGroupIds
			int rootGroupId = Tools.isNotEmpty(groupIds) ? Tools.getIntValue(groupIds, 0) : 0;
			if(rootGroupId > 0) rootGroupLevel =  GroupsDB.getInstance().getPathList(Tools.getIntValue(groupIds,0)).size();
			if(rootGroupLevel<=0 || rootGroupLevel > 3) groupIds = expandedGroupIds;
		}

		//toto sa zda to iste ako predchadzajuce, nie je to ale tak kedze predchadzajuca vetva sa vykona len ked sa expandovalo
		int rootGroupLevel = 1000;
		if(groupIds != null && expandGroupIds && pageParams.getBooleanValue("useRootGroupLevel", true))
		{
			//ak groupId je na vacsej urovni ako 3, pouzijeme expandedGroupIds
			int rootGroupId = Tools.isNotEmpty(groupIds) ? Tools.getIntValue(groupIds, 0) : 0;
			if(rootGroupId > 0) rootGroupLevel =  GroupsDB.getInstance().getPathList(Tools.getIntValue(groupIds,0)).size();
		}

		List<DocDetails> tempDocs = getDocPerex(groupIds, orderType, asc, start, end, publishType, now, whereSql.toString(), orderSql, noData, docIdsNotIn.toString(), onlyAvailable, datum, rootGroupLevel);
		List<Object> docs = new ArrayList<>();
		//zistime, ci dokument vybrany z databazy splna podmienky dane filtrom
		for (DocDetails object : tempDocs) {
			if ( filter == null || filter.fullfilsConditions(object) ) {
				docs.add(object);
			}
		}


		int total_docs = docs.size();
		//vytvor pages
		List<LabelValueDetails> pages = new ArrayList<>();
		request.removeAttribute(arrayListPagerName);
		if (paging)
		{
			//ziskaj celkovy pocet dokumentov
			total_docs = getDocPerexCountInGroups(groupIds, publishType, whereSql.toString(), docIdsNotIn.toString(), onlyAvailable, datum, rootGroupLevel);

			LabelValueDetails lv_page;
			int p = 1;
			start = 0;
			int docId = Tools.getIntValue(request.getParameter("docid"), -1);
			StringBuilder url = new StringBuilder(getDocLink(docId));
			Enumeration<String> e = request.getParameterNames();
			String name;
			String value;
			while (e.hasMoreElements())
			{
				name = ResponseUtils.filter(e.nextElement());
				value = Tools.URLEncode(request.getParameter(name));
				//znak _ je tam kvoli system parametrom ako _writePerfStat, aby sa nahodou nestalo, ze sa ulozi do cache
				if ("docid".equals(name) || pageParamName.equals(name) || name.startsWith("_")) continue;

				if (url.indexOf("?")==-1)
				{
					url.append('?').append(name).append('=').append(value);
				}
				else
				{
					url.append("&amp;").append(name).append('=').append(value);
				}
			}
			boolean pagingNavigation = pageParams.getBooleanValue("pagingNavigation", false);
			if (pageSize > 0)
			{
				String mark = "&amp;";
				if (url.indexOf("?")==-1)
				{
					mark = "?";
				}
				while (start < total_docs)
				{
					if (p == page)
					{
						if(pagingNavigation)
						{
							//prev
							if(p != 1)
							{
								request.setAttribute("prev", new LabelValueDetails("&lt;&lt;&lt;", "<a href='" + url + mark + pageParamName  + "=" + (p-1) + "'>"));
							}
							//next
							if(start+pageSize < total_docs)
							{
								request.setAttribute("next",new LabelValueDetails("&gt;&gt;&gt;", "<a href='" + url + mark + pageParamName  + "=" + (p+1) + "'>"));
							}
						}
						lv_page = new LabelValueDetails(Integer.toString(p), "");
					}
					else
					{

						lv_page = new LabelValueDetails(Integer.toString(p), "<a href='" + url + mark + pageParamName  + "=" + p + "'>");
					}
					pages.add(lv_page);
					p++;
					start = start + pageSize;
				}
			}
			if (pages.size()>1) request.setAttribute(arrayListPagerName, pages);
		}

		request.setAttribute(arrayListDocName, docs);
		return(total_docs);
	}



	/**
	 * Pripravi zoznam stranok, co maju perex v adresaroch groupIds, vratane
	 * strankovania a handlovania parametrov
	 *
	 * odkazuje na /showdoc.do?docid=?
	 */
	public int getDocPerex(String groupIds, int orderType, boolean asc, int publishType, int pageSize, String arrayListDocName, String arrayListPagerName, HttpServletRequest request)
	{
		return getDocPerex(groupIds, orderType, asc, publishType, pageSize, arrayListDocName, arrayListPagerName, request, -1);
	}

	/**
	 * Vrati stranky, ktore maju zadany perex v adresaroch groupIds
	 */
	public List<DocDetails> getDocPerex(String groupIds, int orderType, boolean asc, int publishType)
	{
		return(getDocPerex(groupIds, orderType, asc, 0, 0, publishType, Tools.getNow(), "", null, false, null, true, null, -1));
	}

	/**
	 * Vrati stranky, ktore maju zadany perex v adresaroch groupIds
	 * @param groupIds - zoznam id adresarov, napr. 1,2,4,8
	 * @param orderType - sposob usporiadania
	 * @param asc - ak true, vzostupne usporiadane
	 * @param startIndex - zaciatok (pre stramkovanie), alebo 0 ak vsetky
	 * @param endIndex - koniec (pre strankovanie), alebo 0 ak vsetky
	 * @param publishType - ake sa vyberu dokumenty (nove, stare, vsetky)
	 * @param now - timestamp casu pre ktory sa to generuje, ak je <0, pouzije sa aktualny cas
	 * @param whereSql - SQL prikaz, ktory sa prida do WHERE
	 * @param noData - TRUE ak chceme nacitat vysledky BEZ stlpca data a data_asc
	 * @param docIdsNotIn - obsahuje duplicitne docId oddelene ciarkou, ktore sa maju z hladania vypustit
	 * @param datum - ak chcem zobrazit news pre konkretny den v tvare dd.MM.yyyy
	 * @return
	 */
	private List<DocDetails> getDocPerex(String groupIds, int orderType, boolean asc, int startIndex, int endIndex, int publishType, long now, String whereSql, String orderSql, boolean noData, String docIdsNotIn, boolean onlyAvailable, String datum, int rootGroupLevel)
	{
		//Logger.println(this,"DocDB.getDocPerex now="+now+" "+(new java.util.Date(now)));
		if (whereSql==null) whereSql = "";
		//len pre istotu...
		whereSql = whereSql.replace(';', ' ');

		if(Tools.isNotEmpty(docIdsNotIn))
			whereSql += " AND d.doc_id NOT IN ("+docIdsNotIn+")";

		List<DocDetails> ret = new ArrayList<>();

		if (now < 0)
		{
			now = (new java.util.Date()).getTime();
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();

			boolean checkPerex = true;
			if (publishType>100)
			{
				//nebudeme kontrolovat ci doc_html je null
				checkPerex = false;
				publishType = publishType - 100;
			}

			StringBuilder order = new StringBuilder("d.title");
			if (orderType == ORDER_DATE)
			{
				order = new StringBuilder("publish_start");
			}
			else if (orderType == ORDER_ID)
			{
				order = new StringBuilder("doc_id");
			}
			else if (orderType == ORDER_PRIORITY)
			{
				order = new StringBuilder("sort_priority");
			}
			else if (orderType == ORDER_PLACE)
			{
				order = new StringBuilder("perex_place");
			}
			else if (orderType == ORDER_EVENT_DATE)
			{
				order = new StringBuilder("event_date");
			}
			else if (orderType == ORDER_SAVE_DATE)
			{
				order = new StringBuilder("date_created");
			}
			else if (orderType == ORDER_RATING)
			{
				order = new StringBuilder("forum_count");
			}

			if (asc)
			{
				order.append(" ASC");
				if (orderType != ORDER_PRIORITY && orderType != ORDER_PLACE) order.append(", sort_priority ASC");
			}
			else
			{
				order.append(" DESC");
				if (orderType != ORDER_PRIORITY && orderType != ORDER_PLACE) order.append(", sort_priority ASC");
			}

			if (orderType == ORDER_PLACE)
			{
				//ak sme podla mesta, ostatne sortni podla title
				order.append(", d.title ASC");
			}

			if (orderSql != null)
			{
				order = new StringBuilder(orderSql);
			}

			String dateSql = " ";
			//v pripade ze existuje parameter datum napr. datum=25.10.2011
			if(datum != null)
			{
				//v pripade ze existuje parameter datum napr. datum=25.10.2011
				dateSql = " AND ( (publish_start IS NOT null AND publish_start >= ? AND publish_start <= ?) ) ";
			}
			else if (publishType == PUBLISH_NEW)
			{
				dateSql = " AND ( (publish_start IS null OR publish_start <= ?) AND (publish_end IS null OR publish_end >= ?) ) ";
				//dateSql = " AND ( (publish_start IS NOT null AND publish_start >= ? ) OR (publish_start IS null AND publish_end >= ?) ) ";
			}
			else if (publishType == PUBLISH_OLD)
			{
				dateSql = " AND ( (publish_end IS NOT null AND publish_end < ?) ) ";
			}
			else if (publishType == PUBLISH_NEXT)
			{
				//zobrazme aj veci za poslednych 24 hodin, aby sme vedeli, co sme zmeskali
				now = now - TimeUnit.DAYS.toMillis(1);
				dateSql = " AND ( (publish_end IS NOT null AND publish_end >= ?) OR (publish_end IS null AND publish_start IS NOT NULL AND publish_start >= ?) ) ";
			}

			//Logger.println(this,"dateSql="+dateSql);

			StringBuilder sql;

			String docFields = getDocumentFields(noData==false);

			DocDetails doc;
			int maxRows = -1;
			if (endIndex > startIndex) maxRows = endIndex - startIndex;

			String checkPerexSql = "";
			if (checkPerex)
			{
				checkPerexSql = " AND html_data IS NOT NULL ";
			}

			int rootGroupId = Tools.isNotEmpty(groupIds) ? Tools.getIntValue(groupIds, 0) : 0;

			boolean perexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin") && Tools.isNotEmpty(whereSql) && whereSql.contains("p.perex_group_id");

			if (groupIds != null) groupIds = getOnlyNumbersIn(groupIds,true);
			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				String top = "";
				if (endIndex > 0)
				{
					if (checkPerex)
					{
						//zvys start index, lebo niekde noze byt v adresari stranka, ktora nema perex a tu skipneme programovo
						StringTokenizer st = new StringTokenizer(groupIds, ",");
						//toto je len taky odhad, ze kazdy adresar ma jednu (hlavnu) stranku, ktora nema perex
						endIndex += st.countTokens() + 5;
					}
					top = "TOP " + endIndex;
				}
				sql = new StringBuilder("SELECT " ).append( top ).append( " u.title as u_title, u.first_name, u.last_name, u.email, u.photo, ").append(docFields).append(" FROM documents d LEFT JOIN users u ON d.author_id=u.user_id"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");

				if (Constants.getBoolean("docAuthorLazyLoad"))
					sql = new StringBuilder("SELECT " ).append( top ).append(' ').append(docFields).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");

				if (onlyAvailable) sql.append(" available="+DB.getBooleanSql(true)+" ");
				else sql.append(" d.doc_id>0 ");

				if (groupIds!=null && groupIds.length()>0)
				{
					if(rootGroupLevel <= 3 && rootGroupLevel > 0) sql.append(" AND root_group_l"+rootGroupLevel+" = "+rootGroupId);
					else sql.append(" AND group_id IN (" ).append( getOnlyNumbersIn(groupIds) ).append( ") ");
				}
				sql.append(" " ).append(dateSql).append(checkPerexSql).append(whereSql).append(" ORDER BY " ).append( order);
			}
			else if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				sql = new StringBuilder("SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, ").append(docFields).append(" FROM documents d LEFT JOIN users u ON d.author_id=u.user_id"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");
				if (Constants.getBoolean("docAuthorLazyLoad"))
					sql = new StringBuilder("SELECT ").append(docFields).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");

				if (onlyAvailable) sql.append(" available="+DB.getBooleanSql(true)+" ");
				else sql.append(" d.doc_id>0 ");

				if (groupIds!=null && groupIds.length()>0)
				{
					if(rootGroupLevel <= 3 && rootGroupLevel > 0) sql.append(" AND root_group_l"+rootGroupLevel+" = "+rootGroupId);
					else sql.append(" AND group_id IN (" ).append( getOnlyNumbersIn(groupIds) ).append( ") ");
				}
				sql.append(' ').append(dateSql).append(checkPerexSql).append(whereSql).append(" ORDER BY " ).append( order);
			}
			else
			{
				String limit = "";
				if (startIndex >= 0 && endIndex > 0)
				{
					int rows = endIndex - startIndex;
					limit = " LIMIT " + startIndex + ", " + rows;
					if (Constants.DB_TYPE == Constants.DB_PGSQL) limit = " OFFSET " + startIndex + " LIMIT " + rows;
					startIndex = 0;
				}
				sql = new StringBuilder("SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, ").append(docFields).append(" FROM documents d LEFT JOIN users u ON d.author_id=u.user_id"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");
				if (Constants.getBoolean("docAuthorLazyLoad"))
					sql = new StringBuilder("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+docFields).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");

				if (onlyAvailable) sql.append(" available="+DB.getBooleanSql(true)+" ");
				else sql.append(" d.doc_id>0 ");

				if (groupIds!=null && groupIds.length()>0)
				{
					if(rootGroupLevel <= 3 && rootGroupLevel > 0) sql.append(" AND root_group_l"+rootGroupLevel+" = "+rootGroupId);
					else sql.append(" AND group_id IN (" ).append( getOnlyNumbersIn(groupIds) ).append( ") ");
				}
				sql.append(' ').append(dateSql).append(checkPerexSql).append(whereSql).append(" ORDER BY " ).append( order ).append( limit);
			}
			//Logger.println(this,"getDocPerex() sql="+sql);
			if(datum != null)
			{
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, new Timestamp(DB.getTimestamp(datum, "00:00:00")));
				ps.setTimestamp(2, new Timestamp(DB.getTimestamp(datum, "23:59:59")));
			}
			else if (publishType == PUBLISH_NEW || publishType == PUBLISH_NEXT)
			{
				Timestamp tsNow = new Timestamp(now);
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, tsNow);
				ps.setTimestamp(2, tsNow);
			}
			else if (publishType == PUBLISH_OLD)
			{
				Timestamp tsNow = new Timestamp(now);
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, tsNow);
			}
			else
			{
				ps = db_conn.prepareStatement(sql.toString());
			}
			Logger.debug(this,"sql: "+sql);

			rs = ps.executeQuery();

			int counter = 0;
			if (startIndex > 0)
			{
				int i;
				for (i=0; i<startIndex; i++)
				{
					//jTDS nevie rs.absolute...
					//rs.absolute(startIndex);
					rs.next();
				}
			}
			counter = 0;
			while (rs.next())
			{
				//Logger.println(this,"counter="+counter+" rows="+maxRows);
				if (maxRows > 0 && counter >= maxRows)
				{
					break;
				}

				if (Constants.getBoolean("docAuthorLazyLoad"))
					doc = getDocDetails(rs, noData, true);
				else
					doc = getDocDetails(rs, noData);

				if (checkPerex)
				{
					//skontroluj, ci je tam nejaky perex
					if (doc.getHtmlData()!=null && doc.getHtmlData().length()>1)
					{
						ret.add(doc);
						counter++;
					}
				}
				else
				{
					ret.add(doc);
					counter++;
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
			Logger.error(DocDB.class, ex);
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

		return (ret);

	}

	/**
	 * vrati pocet stranok v adresaroch groupIds
	 * @param docIdsNotIn - obsahuje duplicitne docId oddelene ciarkou, ktore sa maju z hladania vypustit
	 * @return
	 */
	private int getDocPerexCountInGroups(String groupIds, int publishType, String whereSql, String docIdsNotIn, boolean onlyAvailable, String datum, int rootGroupLevel)
	{
		if (whereSql==null) whereSql = "";
		//len pre istotu...
		whereSql = whereSql.replace(';', ' ');

		if(Tools.isNotEmpty(docIdsNotIn))
			whereSql += " AND d.doc_id NOT IN ("+docIdsNotIn+")";

		int ret = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(serverName);

			boolean checkPerex = true;
			if (publishType>100)
			{
				//nebudeme kontrolovat ci doc_html je null
				checkPerex = false;
				publishType = publishType - 100;
			}

			boolean perexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin") && Tools.isNotEmpty(whereSql) && whereSql.contains("p.perex_group_id");

			if (groupIds != null) groupIds = getOnlyNumbersIn(groupIds,true);
			if (InitServlet.isTypeCloud() && ( Tools.isEmpty(groupIds) || "-1".equals(groupIds) ))
			{
				groupIds = String.valueOf(CloudToolsForCore.getDomainId());
			}

			int rootGroupId = Tools.isNotEmpty(groupIds) ? Tools.getIntValue(groupIds, 0) : 0;

			if (groupIds != null) groupIds = getOnlyNumbersIn(groupIds,true);

			StringBuilder sql = new StringBuilder("");
			if(Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL) sql.append("SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) as total FROM documents d");
			else sql.append("SELECT count(d.doc_id) as total FROM documents d");

			sql.append((perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");

			if (onlyAvailable) sql.append(" available="+DB.getBooleanSql(true)+" ");
			else sql.append(" d.doc_id>0 ");

			if (groupIds!=null && groupIds.length()>0)
			{
				if(rootGroupLevel <= 3 && rootGroupLevel > 0) sql.append(" AND root_group_l"+rootGroupLevel+" = "+rootGroupId);
				else sql.append(" AND group_id IN (" ).append( groupIds ).append( ") ");
			}

			if (checkPerex) sql.append(" AND html_data IS NOT null ");
			sql.append(whereSql);
			if(datum != null)
			{
				//v pripade ze existuje parameter datum napr. datum=25.10.2011
				sql.append(" AND ( (publish_start IS NOT null AND publish_start >= ? AND publish_start <= ?) ) ");
				Logger.debug(this, "getDocPerexCountInGroups sql="+sql);

				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, new Timestamp(DB.getTimestamp(datum, "00:00:00")));
				ps.setTimestamp(2, new Timestamp(DB.getTimestamp(datum, "23:59:59")));
			}
			else if (publishType == PUBLISH_NEW)
			{
				sql.append(" AND ( (publish_start IS null OR publish_start < ?) AND (publish_end IS null OR publish_end > ?) )");
				Logger.debug(this, "getDocPerexCountInGroups sql="+sql);

				Timestamp now = new Timestamp((new java.util.Date()).getTime());
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, now);
				ps.setTimestamp(2, now);
			}
			else if (publishType == PUBLISH_OLD)
			{
				sql.append(" AND ( (publish_end IS NOT null AND publish_end < ?) )");
				Logger.debug(this, "getDocPerexCountInGroups sql="+sql.toString());

				Timestamp now = new Timestamp((new java.util.Date()).getTime());
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, now);
			}
			else if (publishType == PUBLISH_NEXT)
			{
				sql.append(" AND ( (publish_end IS NOT null AND publish_end >= ?) OR (publish_end IS null AND publish_start IS NOT NULL AND publish_start >= ?) ) ");
				Timestamp now = new Timestamp((new java.util.Date()).getTime() - TimeUnit.DAYS.toMillis(1));
				ps = db_conn.prepareStatement(sql.toString());
				ps.setTimestamp(1, now);
				ps.setTimestamp(2, now);
			}
			else
			{
				Logger.debug(this, "getDocPerexCountInGroups sql="+sql.toString());
				ps = db_conn.prepareStatement(sql.toString());
			}
			rs = ps.executeQuery();

			if (rs.next())
			{
				ret = rs.getInt("total");
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
			Logger.error(DocDB.class, ex);
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

		return (ret);
	}


	/**
	 *  Vrati pocet dostupnych (available=1) stranok v zadanom adresari
	 *
	 *@param  group_id  Description of the Parameter
	 *@return           The docCountInGroup value
	 */
	public int getDocCountInGroup(int group_id)
	{
		int ret = 0;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(serverName);

			//read user permissions for every org struct

			String sql;
			sql = "SELECT count(doc_id) as total FROM documents d WHERE available="+DB.getBooleanSql(true)+" AND group_id=" + group_id;
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			if (rs.next())
			{
				ret = rs.getInt("total");
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (ret);
	}


	/**
	 *  Interne nacitanie zoznamu URL adries do cache
	 */
	private void loadUrls()
	{
		Logger.debug(DocDB.class, "Load URLS start");
		Logger.debugMemInfo();

		urlsByUrlDomains = new Hashtable<>();

		DebugTimer timer = new DebugTimer("loadUrls");

		//nemozeme pouzit GroupsDB, pretoze to potrebuje inicializovane DocDB (kvoli virtualpath)
		//GroupsDB groupsDB = GroupsDB.getInstance(servletContext, false, dbName);

		//iteruj po grupach a ziskavaj dokumenty
		Map<Integer, GroupDetails> allGroups = getAllGroups();

		Logger.debug(DocDB.class, "after getAllGroups");
		Logger.debugMemInfo();

		String fileName;
		int counter=0;
		timer.diff("loading doc table");
		TIntObjectHashMap<List<DocDetails>> basicAllDocsInGroupTableLocal = null;
		int failsafe = 0;
		while (basicAllDocsInGroupTableLocal==null && failsafe++<5)
		{
			timer.diff("loading doc table, try="+failsafe);
			basicAllDocsInGroupTableLocal = getDocForUrls(true);
		}
		if (basicAllDocsInGroupTableLocal == null) basicAllDocsInGroupTableLocal = getDocForUrls(false);

		basicAllDocsInGroupTable = basicAllDocsInGroupTableLocal;
		timer.diff("loaded");

		//System.gc();
		timer.diff("after getDocForUrls");
		//Logger.debugMemInfo();

		TObjectIntHashMap<String> urlsByUrl = null;
		String lastDomain = null;

		for (GroupDetails group : allGroups.values())
		{
			if (group != null)
			{
				//ziskaj zoznam dokumentov v danej grupe
				List<DocDetails> docs = basicAllDocsInGroupTable.get(group.getGroupId());

				if (docs==null)
				{
					continue;
				}

				//docDB.getDocForUrls(group.getGroupId());

				String[] groupDiskPathDomain = null;
				String domain = "";

				if (Constants.getBoolean("multiDomainEnabled"))
				{
					groupDiskPathDomain = GroupsDB.getURLPathDomainGroup(allGroups, group.getGroupId());
					domain = groupDiskPathDomain[1];
				}

				//Logger.debug(DocDB.class, "lastDomain="+lastDomain+" domain="+domain+" dif="+timer.getDiff());
				if (urlsByUrl==null || lastDomain==null || lastDomain.equals(domain)==false)
				{
					lastDomain = domain;
					urlsByUrl = getUrlsByUrlDomains(lastDomain, true);
					//System.out.println("loadUrls: domain="+lastDomain+" size="+urlsByUrl.size());
				}

				for (DocDetails doc : docs)
				{

					fileName = doc.getVirtualPath();
					if (Tools.isEmpty(fileName))
					{
						if (groupDiskPathDomain == null) groupDiskPathDomain = GroupsDB.getURLPathDomainGroup(allGroups, group.getGroupId());
						String groupDiskPath = DocTools.removeCharsDir(DB.internationalToEnglish(groupDiskPathDomain[0])).toLowerCase() + "/"; //NOSONAR
						//aby to nevytvorilo root stranky pre taketo typy URL
						int lastDefaulDocId = group.getDefaultDocId();
						if (group.getDefaultDocId()!=doc.getDocId())
						{
							//fakt uz netusim preco je tu toto, hlavne ta 4...
							group.setDefaultDocIdNoNavbar(4);
						}
						fileName = getURL(doc, group, groupDiskPath);
						group.setDefaultDocIdNoNavbar(lastDefaulDocId);
						//Logger.debug(DocDB.class, "Empty VP: docId="+doc.getDocId()+" vp="+doc.getVirtualPath()+" grp="+group.getGroupId()+" gdp="+groupDiskPath+" new url="+fileName);

						doc.setVirtualPath(fileName);
					}
					//Logger.println(this,doc.getDocId() + ": " + fileName);
					urlsByUrl.put(fileName, doc.getDocId());

					//POZOR: TUTO MAME ULOZENU DOMENU
					doc.setFieldT(domain);

					counter++;
				}

				//timer.diff("mam: " + group.getGroupId());
			}
		}

		timer.diff("done, size="+counter);

		//System.gc();
		Logger.debug(DocDB.class, "Load URLS done");
		//Logger.debugMemInfo();
	}

	/**
	 * This method is essential during startup phase and therefore
	 * can't be replaced by {@link GroupsDB}.getGroups(), since correct initialization
	 * of {@link GroupsDB} requires already initialized {@link DocDB}. Such a mutual
	 * dependency would render this code undeterministic.
	 */
	private static Map<Integer, GroupDetails> getAllGroups()
	{
		Map<Integer, GroupDetails> groups = new Hashtable<>();
		GroupDetails group;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT * FROM groups ORDER BY sort_priority, group_id, group_name";
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next())
			{
				group = new GroupDetails();
				group.setGroupId(rs.getInt("group_id"));
				group.setGroupName(getDbString(rs, "group_name"));
				group.setInternal(rs.getBoolean("internal"));
				group.setParentGroupId(rs.getInt("parent_group_id"));
				group.setNavbar(getDbString(rs, "navbar"));
				group.setDefaultDocIdNoNavbar(rs.getInt("default_doc_id"));
				group.setTempId(rs.getInt("temp_id"));
				group.setSortPriority(rs.getInt("sort_priority"));
				group.setPasswordProtected(getDbString(rs, "password_protected"));
				group.setMenuType(rs.getInt("menu_type"));
				group.setUrlDirName(getDbString(rs, "url_dir_name"));
				group.setSyncId(rs.getInt("sync_id"));
				group.setSyncStatus(rs.getInt("sync_status"));
				group.setHtmlHead(DB.getDbString(rs, "html_head"));
				group.setLogonPageDocId(rs.getInt("logon_page_doc_id"));

				group.setDomainName(DB.getDbString(rs, "domain_name"));

				//tu nepotrebujeme ostatne atributy

				groups.put(group.getGroupId(), group);
			}

			rs.close();

			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		return(groups);
	}

	private TIntObjectHashMap<List<DocDetails>> getDocForUrls(boolean returnNull)
	{
		TIntObjectHashMap<List<DocDetails>> table = new TIntObjectHashMap<>(10, 0.9f);
		if (basicAllDocsTable==null) basicAllDocsTable = allocateEmptyBasicDocDetails();

		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT doc_id, title, navbar, external_link, group_id, virtual_path, available, searchable, show_in_menu, sort_priority, password_protected, temp_id, date_created, field_a, field_b, field_c FROM documents";

			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			DocDetails doc;
			List<DocDetails> list;
			while (rs.next())
			{
				doc = new DocDetails();
				doc.setDocId(rs.getInt("doc_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				String navbar = DB.getDbString(rs, "navbar");
				if (doc.getTitle().equals(navbar)==false) doc.setNavbar(navbar);
				doc.setExternalLink(DB.getDbString(rs, "external_link"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setVirtualPath(normalizeVirtualPath(DB.getDbString(rs, "virtual_path")));
				doc.setAvailable(rs.getBoolean("available"));
				doc.setSearchable(rs.getBoolean("searchable"));
				doc.setShowInMenu(rs.getBoolean("show_in_menu"));
				doc.setSortPriority(rs.getInt("sort_priority"));
				doc.setPasswordProtected(DB.getDbString(rs, "password_protected"));
				doc.setTempId(rs.getInt("temp_id"));
				doc.setDateCreated(rs.getTimestamp("date_created").getTime());
				doc.setFieldA(DB.getDbString(rs, "field_a"));
				doc.setFieldB(DB.getDbString(rs, "field_b"));
				doc.setFieldC(DB.getDbString(rs, "field_c"));

				//POZOR: do fieldT si neskor ulozime DOMENU

				putToBasicDocDetails(doc);

				if (Constants.getInt("linkType")!=Constants.LINK_TYPE_HTML && Constants.getBoolean("forwardVirtualPath")==true && rs.getString("virtual_path")==null)
				{
					continue;
				}
				list = table.get(doc.getGroupId());
				if (list==null)
				{
					list = new ArrayList<>();
					table.put(doc.getGroupId(), list);
				}
				list.add(doc);
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
			Logger.error(DocDB.class, ex);
			if (returnNull) return null;
		}
		finally
		{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		basicAllDocsTable.compact();

		return (table);
	}

	private TIntObjectHashMap<DocDetails> allocateEmptyBasicDocDetails()
	{
		int maxDocId = new SimpleQuery().forInt("SELECT MAX(doc_id) FROM documents");
		sk.iway.iwcm.Logger.printf(DocDB.class, "Max doc id: %d", maxDocId);
		//allow +10% more for resize
		int allocationSize = (int) (maxDocId * 1.1);
		TIntObjectHashMap<DocDetails> newBasicDocDetails = new TIntObjectHashMap<>(allocationSize, 0.9f);
		return newBasicDocDetails;
	}

	void putToBasicDocDetails(DocDetails doc)
	{
		basicAllDocsTable.put(doc.getDocId(), doc);
	}

	private void putToBasicDocDetailsAndResize(DocDetails doc)
	{
		putToBasicDocDetails(doc);
		basicAllDocsTable.compact();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<DocDetails> getBasicDocDetailsAll()
	{
		//je to tu takto aby sa nehadzala ClassCastException v Spotlighte
		List basicDocDetails = new ArrayList();
		basicDocDetails.addAll(Arrays.asList(basicAllDocsTable.getValues()));

		return basicDocDetails;
	}

	/**
	 * Vrati docDetails s cache, su tam len zakladne info - docId, title, navbar, externalLink, groupId, virtualPath, available, showInMenu
	 * @param docId - id stranky
	 * @param doNotReturnNull - ak je nastavene na true, tak to vzdy vrati aspon prazdny objekt
	 * @return
	 */
	public DocDetails getBasicDocDetails(int docId, boolean doNotReturnNull)
	{
		DocDetails doc = null;

		if (basicAllDocsTable!=null && basicAllDocsTable.size()>0 && docId > 0)
		{
			doc = basicAllDocsTable.get(docId);
		}

		if (doNotReturnNull && doc == null)
		{
			doc = new DocDetails();
			doc.setDocId(docId);
			doc.setTitle("??? docId="+docId);
		}

		return(doc);
	}

	/**
	 * Vrati zoznam docDetails z cache (su tam len zakladne info) v zadanom adresari a so zadanym usporiadanim
	 * @param groupId
	 * @param orderType
	 * @return
	 */
	public List<DocDetails> getBasicDocDetailsByGroup(int groupId, int orderType)
	{
		List<DocDetails> docs = new ArrayList<>();

		List<DocDetails> docsInGroup = basicAllDocsInGroupTable.get(groupId);
		if (docsInGroup == null) return(docs);
		docs.addAll(docsInGroup);

		//usortuj to
		if (orderType == DocDB.ORDER_PRIORITY)
		{
			Collections.sort(docs, new Comparator<DocDetails>() {
				@Override
				public int compare(DocDetails d1, DocDetails d2)
				{
					if (d1.getSortPriority() < d2.getSortPriority())
						return -1;
					else if (d1.getSortPriority() > d2.getSortPriority())
						return 1;
					else
						return 0;
				}

			});
		}
		else if (orderType == DocDB.ORDER_TITLE)
		{
			Collections.sort(docs, new Comparator<DocDetails>() {
				@Override
				public int compare(DocDetails d1, DocDetails d2)
				{
					return(d1.getTitle().compareTo(d2.getTitle()));
				}

			});
		}
		else if (orderType == DocDB.ORDER_DATE)
		{
			Collections.sort(docs, new Comparator<DocDetails>() {
				@Override
				public int compare(DocDetails d1, DocDetails d2)
				{
					long date1 = d1.getPublishStart();
					if (date1 < 10000) date1 = d1.getDateCreated();

					long date2 = d2.getPublishStart();
					if (date2 < 10000) date2 = d2.getDateCreated();

					if (date1 > date2) return 1;
					else if (date1 < date2) return -1;
					return 0;
				}
			});
		}

		return(docs);
	}

	/**
	 * Ziska docId z URL a domeny
	 * @param url
	 * @param domain
	 * @return
	 */
	public static int getDocIdFromURL(String url, String domain)
	{
		DocDB docDB = DocDB.getInstance();
		return(docDB.getDocIdFromURLImpl(url, domain));
	}

	/**
	 * Vrati domenu podla request
	 * @param request
	 * @return
	 */
	public static String getDomain(HttpServletRequest request)
	{
		String serverName = Tools.getServerName(request);

		return getDomain(serverName, request);
	}

	/**
	 * Vrati domenu podla zadaneho nazvu
	 * @param serverName
	 * @param request
	 * @return
	 */
	public static String getDomain(String serverName, HttpServletRequest request)
	{
		if (request == null) return serverName;

		String sessionDomain = (String)request.getSession().getAttribute("preview.editorDomainName");

		if (Tools.isNotEmpty(sessionDomain))
		{
			serverName = sessionDomain;
		}

		String adminHost = Constants.getString("multiDomainAdminHost"); //admin host moze obsahovat viac hostov
		if (Tools.isNotEmpty(adminHost) &&
				( (","+adminHost+",").indexOf(","+serverName+",")!=-1 || "iwcm.interway.sk".equals(serverName) || "localhost".equals(serverName) ) &&
				Tools.isEmpty(sessionDomain) &&
				( request.getParameter("docid")!=null || request.getParameter("historyid")!=null))
		{
			DocDetails myDoc = null;
			int docId = Tools.getIntValue(request.getParameter("docid"), -1);
			if (docId > 0)
			{
				myDoc = DocDB.getInstance().getBasicDocDetails(docId, false);

			}
			else
			{
				int historyId = Tools.getIntValue(request.getParameter("historyid"), -1);
				if (historyId > 0)
				{
					myDoc = DocDB.getInstance().getDoc(-1, historyId);
				}
			}
			if (myDoc != null)
			{
				GroupDetails group = GroupsDB.getInstance().getGroup(myDoc.getGroupId());
				if (group != null)
				{
					String domain = group.getDomainName();
					if (Tools.isNotEmpty(domain))
					{
						Logger.debug(DocDB.class, "Setting preview domain: "+domain);
						request.getSession().setAttribute("preview.editorDomainName", domain);
						serverName = domain;
					}
				}
			}
		}

		return serverName;
	}

	/**
	 * ziska docId z URL
	 * @param url
	 * @return
	 */
	public int getDocIdFromURLImpl(String url, String domain)
	{
		Logger.debug(DocDB.class, "getDocIdFromURLImpl, url="+url+" domain="+domain);
		try
		{
			TObjectIntHashMap<String> mUrls = getUrlsByUrlDomains(domain, false);

			if (mUrls != null)
			{
				Integer iDocId = mUrls.get(url);
				if (iDocId == null || iDocId.intValue()==0) iDocId = mUrls.get(normalizeVirtualPath(url));

				if (iDocId != null)
				{
					return(iDocId.intValue());
				}
			}
		}
		catch (Exception e)
		{
			Logger.error(DocDB.class, e);
		}
		return(-1);
	}

	/**
	 * ziska URL z docId
	 * POZOR: pred url je nutne dat contextPath!!
	 * @param docId
	 * @return
	 */
	private static String getURLFromDocId(int docId)
	{
		DocDB docDB = DocDB.getInstance();
		return(docDB.getURLFromDocIdImpl(docId));
	}

	/**
	 * ziska URL z docId
	 * POZOR: pred url je nutne dat contextPath!!
	 * @param docId
	 * @return
	 */
	private String getURLFromDocIdImpl(int docId)
	{
		DocDetails doc = basicAllDocsTable.get(docId);

		if (doc==null) return "/showdoc.do?docid="+docId;

		String url = doc.getVirtualPath();
		url = Tools.replace(url, "*", "");
		return(url);
	}

	/**
	 * ziska URL z docId
	 * @param docId
	 * @param request
	 * @return
	 */
	public static String getURLFromDocId(int docId, HttpServletRequest request)
	{
		return(getURLFromDocId(docId));
	}


	/**
	 * Vrati cestu k adresaru s odstranenymi nepovolenymi znakmi
	 * @param allGroups
	 * @param groupId
	 * @return
	 */
	public static String getGroupDiskPath(List<GroupDetails> allGroups, int groupId)
	{
		String groupDiskPath = DB.internationalToEnglish(GroupsDB.getURLPathGroup(allGroups, groupId)).toLowerCase() + "/"; //NOSONAR
		groupDiskPath = DocTools.removeCharsDir(groupDiskPath).toLowerCase();
		return(groupDiskPath);
	}

	/**
	 * Ziskanie URL adresy stranky
	 * @param doc
	 * @param groupsDB
	 * @return
	 */
	public static String getURL(DocDetails doc, GroupsDB groupsDB)
	{
		return(getURL(doc, getGroupDiskPath(groupsDB.getGroupsAll(), doc.getGroupId())));
	}

	/**
	 * Ziskanie URL adresy web stranky
	 * @param doc
	 * @param groupDiskPath
	 * @return
	 */
	public static String getURL(DocDetails doc, String groupDiskPath)
	{
		return(getURL(doc, null, groupDiskPath));
	}

	/**
	 * Ziska URL adresu web stranky
	 * @param doc
	 * @param group
	 * @param groupDiskPath
	 * @return
	 */
	public static String getURL(DocDetails doc, GroupDetails group, String groupDiskPath)
	{
		if (Tools.isEmpty(doc.getVirtualPath()) || doc.getVirtualPath().indexOf('/')==-1)
		{
			String htmlFileName = doc.getVirtualPath();
			if (Tools.isEmpty(htmlFileName))
			{
				htmlFileName = doc.getNavbar();
			}
			StringBuilder fileName = new StringBuilder(groupDiskPath);
			if (group == null)
			{
				GroupsDB groupsDB = GroupsDB.getInstance();
				group = groupsDB.getGroup(doc.getGroupId());
			}
			if (group == null)
			{
				return "/page-"+doc.getDocId()+".html";
			}
			if (Tools.isEmpty(doc.getVirtualPath()) && ((group.getDefaultDocId()<=0 && !"-".equals(group.getUrlDirName())) || group.getDefaultDocId()==doc.getDocId()))
			{
				//ak adresar nema default stranku, alebo je to prave nasa, nemusi to koncit na .html
				if (fileName.toString().endsWith("/")==false) fileName.append('/');
			}
			else
			{
				fileName.append(DocTools.removeChars(DB.internationalToEnglish(htmlFileName).toLowerCase())).append(Constants.getString("editorPageExtension"));
			}
			return (normalizeVirtualPath(fixURL(fileName.toString())));
		}
		else
		{
			return(doc.getVirtualPath());
		}
	}

	private static String fixURL(String url)
	{
		if (url==null) return("");

		url = DocTools.removeCharsDir(url).toLowerCase();

		url = Tools.replace(url, "//", "/");
		url = Tools.replace(url, "_-_", "_");
		url = Tools.replace(url, "__", "_");
		url = Tools.replace(url, "___", "_");
		url = Tools.replace(url, "._", "_");
		url = Tools.replace(url, "_.html", ".html");
		url = Tools.replace(url, ".html.html", ".html");
		//ak mame nahradu spojok a bodiek v URL toto treba
		url = Tools.replace(url, "-html.html", ".html");
		//htm je tu kvoli importu web stranok zo zip archivu
		url = Tools.replace(url, ".htm.html", ".htm");
		url = Tools.replace(url, "-htm.html", ".htm");
		return(url);
	}

	/**
	 * Vrati linku na dokument
	 * @param docId
	 * @return
	 */
	public String getDocLink(int docId)
	{
		return(getDocLink(docId, null, null));
	}

	public String getDocLink(int docId, HttpServletRequest request)
	{
		return(getDocLink(docId, null, request));
	}

	/**
	 * Vrati linku na zadane docId
	 * @param docId - id stranky
	 * @param externalLink - externa linka (ak ma dokument nastavene a vieme urcit)
	 * @param request
	 * @return
	 */
	public String getDocLink(int docId, String externalLink, HttpServletRequest request)
	{
		return getDocLink(docId, externalLink, false, request);
	}

	/**
	 * Vrati linku na zadane docId
	 * @param docId - id stranky
	 * @param externalLink - externa linka (ak ma dokument nastavene a vieme urcit)
	 * @param alwaysIncludeHttpPrefix - ak je nastavene na true vracia sa vzdy aj s HTTP prefixom (napr. pre email...), inak sa http prefix nastavi len pre multidomain ine ako aktualna domena
	 * @param request
	 * @return
	 */
	public String getDocLink(int docId, String externalLink, boolean alwaysIncludeHttpPrefix, HttpServletRequest request)
	{
		if (docId<1)
		{
			return("javascript:void(0)");
		}
		if (Tools.isNotEmpty(externalLink))
		{
			if (alwaysIncludeHttpPrefix && externalLink.startsWith("http")==false) externalLink = Tools.getBaseHref(request)+externalLink;
			return(externalLink);
		}
		if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML || Constants.getBoolean("forwardVirtualPath")==true)
		{
			//ziskaj linku
			//Logger.println(this,"ziskavam linku: "+getURLFromDocId(docId));
			DocDetails doc = basicAllDocsTable.get(docId);

			String url;
			if (doc==null)
			{
				//url = "/showdoc.do?docid="+docId;
				//jeeff: uprava pre cloud, ale asi nema smysel robit linku na doc ktory neexistuje
				url = "#nopage";
			}
			else url = doc.getVirtualPath();
			url = Tools.replace(url, "*", "");
			url = Tools.replace(url, "//", "/");

			if (request!=null && doc!=null)
			{
				//vo fieldT mame domenu stranky (kvoli optimalizacii kodu)
				String fieldT = doc.getFieldT();

				//porovnaj domeny
				String domain = getDomain(request);
				if (alwaysIncludeHttpPrefix || (domain.equals(fieldT)==false && Tools.isNotEmpty(fieldT) && "default".equals(fieldT)==false))
				{
					if (Tools.isNotEmpty(fieldT))
					{
						if (Tools.isSecure(request)) url = "https://"+fieldT+url;
						else url = "http://"+fieldT+url;
					}
				}
			}
			if (alwaysIncludeHttpPrefix && url.startsWith("http")==false) url = Tools.getBaseHref(request)+url;

			if (url.startsWith("/javascript:")) url = url.substring(1);

			return url;
		}

		//nastava v pripade, ze adresar ma nastavene defaultDocId ale stranka bola vymazana, nechcem v cloude taku linku
		if ("cloud".equals(Constants.getInstallName())) return "";

		return("/showdoc.do?docid="+docId);
	}


	/**
	 * Vrati prazdny string ak aktualna domena v requeste je zhodna s domenou stranky docId,
	 * inak vrati odkaz http[s]://domenaDanehoDocId.sk
	 * @param docId - ID stranky
	 * @param alwaysIncludeHttpPrefix - ak je nastavene na true http[s] prefix vrati vzdy
	 * @param request
	 * @return
	 */
	public String getDocDomainIfDifferent(int docId, boolean alwaysIncludeHttpPrefix, HttpServletRequest request)
	{
		if (docId<1)
		{
			return("");
		}
		if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML || Constants.getBoolean("forwardVirtualPath")==true)
		{
			//ziskaj linku
			//Logger.println(this,"ziskavam linku: "+getURLFromDocId(docId));
			DocDetails doc = basicAllDocsTable.get(docId);

			String retDomain = "";

			if (request!=null && doc!=null)
			{
				//vo fieldT mame domenu stranky (kvoli optimalizacii kodu)
				String fieldT = doc.getFieldT();

				//porovnaj domeny
				String actualDomain = getDomain(request);
				if (alwaysIncludeHttpPrefix || (actualDomain.equals(fieldT)==false && Tools.isNotEmpty(fieldT) && "default".equals(fieldT)==false))
				{
					if (Tools.isNotEmpty(fieldT))
					{
						if (Tools.isSecure(request)) retDomain = "https://"+fieldT;
						else retDomain = "http://"+fieldT;
					}
				}
			}

			return retDomain;
		}

		return("");
	}



	/**
	 * Replace oldText na newText vo vsetkych strankach
	 * @param oldText
	 * @param newText
	 * @return
	 */
	public List<DocDetails> replaceTextAll(String oldText, String newText)
	{
		List<DocDetails> docs = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection(serverName);
			ps = db_conn.prepareStatement("SELECT * FROM documents WHERE data LIKE ?");
			ps.setString(1, "%"+oldText+"%");
			rs = ps.executeQuery();
			while (rs.next())
			{
				DocDetails doc = new DocDetails();
				doc.setDocId(rs.getInt("doc_id"));
				doc.setTitle(getDbString(rs, "title"));
				doc.setData(DB.getDbString(rs, "data"));

				//Logger.println(this,"mam: " + doc.getTitle());

				docs.add(doc);
			}
			rs.close();
			ps.close();

			//ok, mame zoznam, updatni to
			for (DocDetails doc : docs)
			{
				Logger.println(this,"updating link in: "+doc.getTitle());

				doc.setData(Tools.replace(doc.getData(), oldText, newText));

				ps = db_conn.prepareStatement("UPDATE documents SET data=?, data_asc=?, sync_status=1 WHERE doc_id=?");
				//ps.setString(1, doc.getData());
				//ps.setString(2, DB.internationalToEnglish(doc.getData()).toLowerCase());
				DB.setClob(ps, 1, doc.getData());
				DB.setClob(ps, 2, DB.internationalToEnglish(doc.getData()).toLowerCase());

				ps.setInt(3, doc.getDocId());
				ps.executeUpdate();
				ps.close();

				//DocDB.updateDataClob(db_conn, doc.getDocId(), -1, doc.getData(), DB.internationalToEnglish(doc.getData()).toLowerCase());
			}

			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		return(docs);
	}

	/**
	 * Vyhladavanie vo vsetkych strankach (/admin/searchall.jsp)
	 * @param text
	 * @return
	 */
	public List<DocDetails> searchTextAll(String text)
	{
		return searchTextAll(text, -1);
	}

	/**
	 * Vyhladavanie vo vsetkych strankach (/admin/searchall.jsp) v danom adresari
	 *
	 * @param 	text		text, ktory sa vyhladava v nazvoch a telach stranok
	 * @param	groupId	identifikacne cislo adresara, v ktorom sa vyhladava
	 *
	 * @return	zoznam stranok, ktore obsahuju vo svojom nazve alebo tele dany text a su umiestnene v danom adresari
	 *
	 */
	public List<DocDetails> searchTextAll(String text, int groupId)
	{
		return searchTextAll(text, groupId, null);
	}

	/**
	 * Search for text in documents
	 * @param text - text to search
	 * @param groupId - root groupId
	 * @param whereSql - additional SQL query
	 * @return
	 */
	public List<DocDetails> searchTextAll(String text, int groupId, String whereSql) {
		return searchTextAll(text, groupId, whereSql, true);
	}

	/**
	 * Search for text in documents
	 * @param text - text to search
	 * @param groupId - root groupId
	 * @param whereSql - additional SQL query
	 * @param allowFulltext - allow to use fulltext for faster results
	 * @return
	 */
	public List<DocDetails> searchTextAll(String text, int groupId, String whereSql, boolean allowFulltext)
	{
		if ("tatrabanka".equals(Constants.getInstallName()))
		{
			//fix na TB, ale inak rozumne som to nevedel spravit
			//vo fulltexte to mame ako TatraPay TB cize s medzerou kvoli sup
			text = Tools.replace(text, "TB", " TB");
		}
		//odstran dvojite medzery
		text = Tools.replace(text, "  ", " ");

		List<DocDetails> docs = new ArrayList<>();
		String groupIdsQuery = null;
		if (groupId > 0) groupIdsQuery = StatDB.getRootGroupWhere("group_id", groupId);

		//obmedzujeme na max 2000 zaznamov, viac realne nema zmysel (zbytocne iba zere pamat)
		int limit = Constants.getInt("searchTextAllLimit");
		String limit1 = "";
		String limit2 = "";
		if (Constants.DB_TYPE==Constants.DB_MYSQL || Constants.DB_TYPE==Constants.DB_PGSQL) limit2 = " LIMIT "+limit;
		if (Constants.DB_TYPE==Constants.DB_MSSQL) limit1 = " TOP "+limit;

		StringBuilder sql = new StringBuilder("SELECT").append(limit1).append(' ').append(getDocumentFields()).append(", d.data_asc FROM documents d WHERE ");

		boolean useFullText = false;

		int searchTextDocsLimit = Constants.getInt("searchTextDocsLimit");
		if(searchTextDocsLimit < 0) searchTextDocsLimit = 10000;

		if (basicAllDocsTable.size()> searchTextDocsLimit && allowFulltext)
		{
			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				sql.append("CONTAINS(data_asc, ?)");
			}
			else if (Constants.DB_TYPE == Constants.DB_ORACLE && Constants.getBoolean("searchUseOracleText"))
			{
				sql.append("CONTAINS(data_asc, ?)>1");
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				sql.append("to_tsvector(data_asc) @@ to_tsquery(?)");
			}
			else
			{
				sql.append("MATCH(title, data_asc) AGAINST (? IN BOOLEAN MODE) ");
			}
			useFullText =true;
		}
		else
		{
			sql.append("(data LIKE ? OR data_asc LIKE ? OR d.title LIKE ?) ");
			useFullText =false;
		}

		if (Tools.isNotEmpty(groupIdsQuery)) sql.append(' ').append(groupIdsQuery);

		if (Tools.isNotEmpty(whereSql)) sql.append(' ').append(whereSql).append(' ');

		sql.append(limit2);

		Logger.debug(DocDB.class, "SearcTextAll, text="+text+", groupid="+groupId+", sql="+sql);

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			ps = StatNewDB.prepareStatement(db_conn, sql.toString());

			if (useFullText)
			{
				if ((Constants.DB_TYPE == Constants.DB_MSSQL || (Constants.DB_TYPE == Constants.DB_ORACLE && Constants
						.getBoolean("searchUseOracleText"))) && text.split(" ").length > 1)
				{
					String[] words = text.split(" ");
					//String containsPhraze = Lambda.join(words, " AND ");
					String containsPhraze = String.join(" AND ", words);

					Logger.debug(getClass(), "contains phraze: " + containsPhraze);
					ps.setString(1, DB.internationalToEnglish(containsPhraze.toLowerCase()));
				}
				else if (Constants.DB_TYPE == Constants.DB_PGSQL) {
					String containsPhraze = text;
					String[] words = text.split(" ");
					if (words.length>1) containsPhraze = String.join(" & ", words);

					Logger.debug(getClass(), "contains phraze pgsql: " + containsPhraze);
					ps.setString(1, DB.internationalToEnglish(containsPhraze.toLowerCase()));
				}
				else
				{
					ps.setString(1, "%" + DB.internationalToEnglish(text.toLowerCase()) + "%");
				}
			}
			else
			{
				String searchText = Tools.replace(text, " ", "%");
				//data
				ps.setString(1, "%"+searchText+"%");
				//data_asc
				ps.setString(2, "%"+DB.internationalToEnglish(searchText.toLowerCase())+"%");
				//title
				ps.setString(3, "%"+searchText+"%");
			}

			rs = ps.executeQuery();
			DocDetails doc;
			int counter = 0;
			while (rs.next() && counter++ < limit)
			{
				doc = getDocDetails(rs, false, true);
				String dataAsc = DB.getDbString(rs, "data_asc");
				if (dataAsc != null && "null".equals(dataAsc)==false)
				{
					doc.setData(dataAsc);
				}
				docs.add(doc);
			}
			rs.close();
			ps.close();

			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		return(docs);
	}

	/**
	 * Vyhlada stranky obsahujuce zadane URL
	 * @param url
	 * @return
	 */
	public List<DocDetails> searchTextUrl(String url)
	{
		return searchTextUrl(url, -1);
	}

	/**
	 * Vyhlada stranky obsahujuce zadane URL v zadanom adresari
	 * @param url
	 * @param groupId
	 * @return
	 */
	public List<DocDetails> searchTextUrl(String url, int groupId)
	{
		List<DocDetails> docs = new ArrayList<>();

		String groupIdsQuery = null;
		if (groupId > 0) groupIdsQuery = StatDB.getRootGroupWhere("group_id", groupId);

		//obmedzujeme na max 2000 zaznamov, viac realne nema zmysel (zbytocne iba zere pamat)
		int limit = Constants.getInt("searchTextAllLimit");
		String limit1 = "";
		String limit2 = "";
		if (Constants.DB_TYPE==Constants.DB_MYSQL || Constants.DB_TYPE==Constants.DB_PGSQL) limit2 = " LIMIT "+limit;
		if (Constants.DB_TYPE==Constants.DB_MSSQL) limit1 = " TOP "+limit;

		StringBuilder sql = new StringBuilder("SELECT").append(limit1).append(' ').append(getDocumentFieldsNodata()).append(" FROM documents d WHERE virtual_path LIKE ?");

		if (Tools.isNotEmpty(groupIdsQuery)) sql.append(' ').append(groupIdsQuery);

		sql.append(limit2);

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			URI uri = new URI(url);
			String path = uri.getPath();
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			ps = StatNewDB.prepareStatement(db_conn, sql.toString());
			ps.setString(1, "%"+path.toLowerCase()+"%");
			rs = ps.executeQuery();
			DocDetails doc;
			int counter = 0;
			while (rs.next() && counter++<limit)
			{
				doc = getDocDetails(rs, true, true);
				docs.add(doc);
			}
			rs.close();
			ps.close();

			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		return(docs);
	}

	/**
	 * vrati zoznam dokumentov na schvalenie pre daneho pouzivatela
	 * @param userId
	 * @return
	 */
	public List<DocDetails> getDocsForApprove(int userId)
	{
		List<DocDetails> docs = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(serverName);

			ps = db_conn.prepareStatement("SELECT dh.history_id, dh.doc_id, dh.save_date, dh.title, dh.author_id, dh.group_id, dh.available, u.title as u_title, u.first_name, u.last_name, u.email, u.photo FROM documents_history dh,  users u WHERE dh.awaiting_approve IS NOT NULL AND dh.awaiting_approve LIKE '%,"+userId+",%' AND dh.author_id = u.user_id ORDER BY save_date DESC");
			rs = ps.executeQuery();
			DocDetails doc;
			GroupsDB groupsDB = GroupsDB.getInstance();
			while (rs.next())
			{
				doc = new DocDetails();
				doc.setHistoryId(rs.getInt("history_id"));
				doc.setDocId(rs.getInt("doc_id"));
				doc.setAuthorId(rs.getInt("author_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setNavbar(groupsDB.getNavbarNoHref(doc.getGroupId()));
				doc.setDateCreated(DB.getDbTimestamp(rs, "save_date"));
				doc.setAvailable(rs.getBoolean("available"));

				doc.setAuthorName(getFullName(rs));
				doc.setAuthorEmail(getDbString(rs, "email"));

				docs.add(doc);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return(docs);
	}

	/**
	 * Vymazanie stranky z databazy, nemaze to historiu (aby sa dalo dostat aspon k niecomu, ked sa to zmaze omylom)
	 * PRE KOREKTNE PRESUNUTIE DO KOSA POUZITE DeleteServlet.deleteDoc
	 * @param docId
	 * @param request - moze byt aj null
	 * @return
	 */
	public static boolean deleteDoc(int docId, HttpServletRequest request) {
		return deleteDoc(docId, request, true);
	}

	/**
	 * Vymazanie stranky z databazy, nemaze to historiu (aby sa dalo dostat aspon k niecomu, ked sa to zmaze omylom)
	 * PRE KOREKTNE PRESUNUTIE DO KOSA POUZITE DeleteServlet.deleteDoc
	 * @param docId
	 * @param request
	 * @param publishEvents - ak je true publikuju sa aj udalosti o zmene
	 * @return
	 */
	public static boolean deleteDoc(int docId, HttpServletRequest request, boolean publishEvents)
	{
		// ziskanie namapovanych stranok
		List<Integer> slaves = MultigroupMappingDB.getSlaveDocIds(docId);

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
			if (doc!=null && publishEvents) (new WebjetEvent<DocDetails>(doc, WebjetEventType.ON_DELETE)).publishEvent();

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM documents WHERE doc_id=?");
			ps.setInt(1, docId);
			ps.execute();
			ps.close();

			// zmazanie namapovanych stranok
			for (Integer slave : slaves) {
				 ps.setInt(1, slave);
				 ps.execute();
				 DocDB.getInstance().updateInternalCaches(slave);
			}

			//zmazem aj pripadne zaznamy z perex_group_doc
			ps = db_conn.prepareStatement("DELETE FROM perex_group_doc WHERE doc_id=?");
			ps.setInt(1, docId);
			ps.execute();
			ps.close();

			if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
			{
				db_conn.commit();
			}
			db_conn.close();

			//zmaz zapisane hodnoty mapovania z tabulky 'multigroup_mapping'
			MultigroupMappingDB.deleteSlaves(docId);

			DocDB.getInstance().updateInternalCaches(docId);

			if (doc!=null && publishEvents) (new WebjetEvent<DocDetails>(doc, WebjetEventType.AFTER_DELETE)).publishEvent();

			db_conn = null;
			ps = null;
			return(true);
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return(false);
	}

	/**
	 * Naplni DocDetails z ResultSetu, je treba spravit join medzi documents a users
	 * @param rs
	 * @param noData
	 * @return
	 */
	public static DocDetails getDocDetails(ResultSet rs, boolean noData)
	{
		DocDetails doc = new DocDetails();
		getDocDetails(rs, doc, noData, false);
		return(doc);

	}

	/**
	 * Naplni DocDetails z ResultSetu
	 * @param rs
	 * @param noData - ak je true nevracia sa data hodnota
	 * @param noAuthor - ak je true nevracia sa autor (SQL nemusi robit JOIN na tabulku users)
	 * @return
	 */
	public static DocDetails getDocDetails(ResultSet rs, boolean noData, boolean noAuthor)
	{
		DocDetails doc = new DocDetails();
		getDocDetails(rs, doc, noData, noAuthor);
		return(doc);
	}

	/**
	 * Naplni DocDetails z ResultSetu
	 * @param rs
	 * @param doc
	 * @param noData - ak je true nevracia sa data hodnota
	 * @param noAuthor - ak je true nevracia sa autor (SQL nemusi robit JOIN na tabulku users)
	 */
	public static void getDocDetails(ResultSet rs, DocDetails doc, boolean noData, boolean noAuthor)
	{
		try
		{
			doc.setDocId(rs.getInt("doc_id"));
			if (noData)
			{
				doc.setData(WebpagesService.DATA_NOT_LOADED);
			}
			else
			{
				doc.setData(DB.getDbString(rs, "data"));
			}

			doc.setDateCreated(DB.getDbTimestamp(rs, "date_created"));
			doc.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
			doc.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));
			doc.setAuthorId(rs.getInt("author_id"));
			try
			{
				if (noAuthor == false)
				{
					doc.setAuthorName(getFullName(rs));
					doc.setAuthorEmail(getDbString(rs, "email"));
                    doc.setAuthorPhoto(getDbString(rs, "photo"));
				}
			}
			catch (Exception ex)
			{
				// asi to nebolo JOINute na users...
			}

			doc.setSearchable(rs.getBoolean("searchable"));
			doc.setGroupId(rs.getInt("group_id"));
			doc.setAvailable(rs.getBoolean("available"));
			doc.setShowInMenu(rs.getBoolean("show_in_menu"));
			doc.setPasswordProtected(getDbString(rs, "password_protected"));

			doc.setCacheable(rs.getBoolean("cacheable"));
			doc.setExternalLink(getDbString(rs, "external_link"));
			doc.setVirtualPath(getDbString(rs, "virtual_path"));
			doc.setTempId(rs.getInt("temp_id"));
			doc.setTitle(getDbString(rs, "title"));
			doc.setNavbar(getDbString(rs, "navbar"));
			doc.setFileName(getDbString(rs, "file_name"));
			doc.setSortPriority(rs.getInt("sort_priority"));
			doc.setHeaderDocId(rs.getInt("header_doc_id"));
			doc.setFooterDocId(rs.getInt("footer_doc_id"));
			doc.setMenuDocId(rs.getInt("menu_doc_id"));
			doc.setRightMenuDocId(rs.getInt("right_menu_doc_id"));

			doc.setHtmlHead(getDbString(rs, "html_head"));
			doc.setHtmlData(getDbString(rs, "html_data"));
			doc.setPerexPlace(getDbString(rs, "perex_place"));
			doc.setPerexImage(getDbString(rs, "perex_image"));
			doc.setPerexGroupString(getDbString(rs, "perex_group"));

			doc.setEventDate(DB.getDbTimestamp(rs, "event_date"));

			doc.setSyncId(rs.getInt("sync_id"));
			doc.setSyncStatus(rs.getInt("sync_status"));

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

			doc.setForumCount(rs.getInt("forum_count"));

			doc.setViewsTotal(rs.getInt("views_total"));

			doc.setFieldM(DB.getDbString(rs, "field_m"));
			doc.setFieldN(DB.getDbString(rs, "field_n"));
			doc.setFieldO(DB.getDbString(rs, "field_o"));
			doc.setFieldP(DB.getDbString(rs, "field_p"));
			doc.setFieldQ(DB.getDbString(rs, "field_q"));
			doc.setFieldR(DB.getDbString(rs, "field_r"));
			doc.setFieldS(DB.getDbString(rs, "field_s"));
			doc.setFieldT(DB.getDbString(rs, "field_t"));

			doc.setRequireSsl(rs.getBoolean("require_ssl"));

			DataAccessHelper.docLoadData(rs, doc);
		}
		catch (Exception re)
		{
			//
		}
	}

	/**
	 * Vrati DocDetails podla adresara a nazvu, alebo ak nenajde/nie je zadane podla syncId
	 * @param syncId - hodnota docId na remote serveri
	 * @return
	 */
	public DocDetails getDocBySync(int syncId, int groupId, String title, String remoteFullPath, String syncDefaultForGroupId)
	{
		if (Tools.isNotEmpty(title) && groupId > 0)
		{
			//skusme najst podla mena
			GroupsDB groupsDB = GroupsDB.getInstance();
			GroupDetails parentGroup = groupsDB.getGroupBySync(remoteFullPath, groupId);
			if (parentGroup != null)
			{
				List<DocDetails> docsByGroup = getDocByGroup(parentGroup.getGroupId());
				DocDetails localDefaultDoc = null;
				for (DocDetails docByName : docsByGroup)
				{
					if (parentGroup.getDefaultDocId()==docByName.getDocId())
					{
						localDefaultDoc = docByName;
					}
					if (docByName.getTitle().equals(title))
					{
						return(docByName);
					}
				}
				if (localDefaultDoc != null && Tools.isNotEmpty(syncDefaultForGroupId))
				{
					//nenasli sme, ale ak stranka co ju hladame je tiez default doc adresara, tak to skusme pouzit
					StringTokenizer st = new StringTokenizer(syncDefaultForGroupId, ",");
					String sTmp;
					int remoteGroupId;
					GroupDetails syncGroup;
					while (st.hasMoreTokens())
					{
						sTmp = st.nextToken();
						try
						{
							remoteGroupId = Tools.getIntValue(sTmp, -1);
							if (remoteGroupId > 0)
							{
								syncGroup = groupsDB.getGroupBySync(null, remoteGroupId);
								if (syncGroup != null && parentGroup.getGroupId() == syncGroup.getGroupId())
								{
									return(localDefaultDoc);
								}
							}
						}
						catch (Exception e)
						{
							Logger.error(DocDB.class, e);
						}
					}
				}
			}
		}

		if (syncId < 1) return(null);

		DocDetails doc = null;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();

			String sql;


			sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE sync_id=? ORDER BY doc_id ASC";

			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, syncId);
			rs = ps.executeQuery();

			if (rs.next())
			{
				doc = getDocDetails(rs, false);
			}

			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (doc);
	}


	/**
	 * Ulozi perex skupinu do DB, ak je vsetko OK, vrati TRUE
	 * @param groupName - nazov perex skupiny
	 * @return
	 */
	public boolean savePerexGroup(String groupName)
	{
		return (savePerexGroup(groupName, -1, "", null));
	}


	/**
	 * Ulozi perex skupinu do DB alebo vykona update, ak je vsetko OK, vrati TRUE
	 * @param groupName - nazov perex skupiny
	 * @param groupId - id perex skupiny
	 * @return
	 */
	public boolean savePerexGroup(String groupName, int groupId)
	{
		return (savePerexGroup(groupName, groupId, "", null));
	}

	/**
	 * Ulozi perex skupinu do DB alebo vykona update, ak je vsetko OK, vrati TRUE
	 * @param groupName - nazov perex skupiny
	 * @param groupId - id perex skupiny
	 * @param availableGroups - skupina adresarov, pre ktore je perex skupina platna
	 * @return - "1" - ulozilo sa v poriadku, "-1" - nastal problem pri ulozeni do DB, "-2" - zadana group name uz existuje
	 */
	public boolean savePerexGroup(String groupName, int groupId, String availableGroups, HttpServletRequest request)
	{
		if (InitServlet.isTypeCloud() && Tools.isEmpty(availableGroups))
		{
			availableGroups = String.valueOf(CloudToolsForCore.getDomainId());
		}

		String lng = "";
		if(request != null)
			PageLng.getUserLng(request);
		Prop prop = Prop.getInstance(lng);
		boolean result = false;
		boolean found = false;
		int resultsDb = 0;
		StringBuilder ulozeneAdresare = new StringBuilder();
		try
		{
			if (Tools.isNotEmpty(groupName))
			{
				if( InitServlet.isTypeCloud() ) {
					if(Tools.isEmpty(availableGroups))
					{
						availableGroups = CloudToolsForCore.getDomainId() + "";
					}
					else
					{
						Logger.debug(this," Removing availableGroups [ "+availableGroups+" ] from other domains");
						GroupDetails gd = null;
						int[] newAvailableGroupsCd = Tools.getTokensInt(availableGroups, ",");
						availableGroups = ","+availableGroups+",";
						boolean removeGroup = false;
						for(int avgGrup : newAvailableGroupsCd)
						{
							removeGroup = false;
							if((gd = GroupDetails.getById(avgGrup)) != null )
							{
								if (!gd.getDomainName().equalsIgnoreCase(CloudToolsForCore.getDomainName()))
									removeGroup = true;  //vymazeme cislo zo zoznamu, pretoze je z inej domeny
							}
							else
								removeGroup = true;	//je null (nepatri do ziadnej domeny) nema tu co hladat

							if(removeGroup)
								availableGroups = Tools.replace(availableGroups, ","+avgGrup+",", ",");
						}
						//odstranim pridane ciarky
						availableGroups = availableGroups.substring(1,availableGroups.length());
						//ak bolo v availableGroups iba jedno cislo a bolo zmazane, neostala tam uz ziadna ciarka
						if(availableGroups.length() > 0) availableGroups = availableGroups.substring(0,availableGroups.length()-1);
					}
				}
				PerexGroupBean pg = getPerexGroup(groupId, null);

				for(PerexGroupBean pgBean : getPerexGroups())
				{
					//duplicitu kontrolujem pri novej perex skupine, alebo editacii perex skupiny s ostatnymi skupinami
					if((pg == null || pg.getPerexGroupId() != pgBean.getPerexGroupId()) && pgBean.getPerexGroupName().equalsIgnoreCase(groupName.trim()))
					{
						//ak naslo rovnaky nazov perex skupiny, skontrolujem este aj zhodnost skupin
						int[] pgBeanAvailableGroupsInt = pgBean.getAvailableGroupsInt();
						int[] newAvailableGroups = Tools.getTokensInt(availableGroups, ",");
						GroupsDB groupsDB = GroupsDB.getInstance();

						//ak je zadane pre vsetky adresare, tak je zhoda
						if(pgBeanAvailableGroupsInt.length == 0 || newAvailableGroups.length == 0)
						{
							found = true;
							if(pgBeanAvailableGroupsInt.length == 0)
							{
								ulozeneAdresare.delete(0, ulozeneAdresare.length());
								ulozeneAdresare.append(prop.getText("editor.perex_group.vsetky"));
							}
							else
							{
								for(int groupIdTmp : pgBeanAvailableGroupsInt)
									ulozeneAdresare.append(groupsDB.getPath(groupIdTmp)).append(", ");
								if(Tools.isNotEmpty(ulozeneAdresare)){
									ulozeneAdresare = new StringBuilder(ulozeneAdresare.substring(0, ulozeneAdresare.length()-2));
								}
							}
							break;
						}
						else
						{
							for(int groupIdTmp : pgBeanAvailableGroupsInt)
							{
								if(isGroupAvailable(newAvailableGroups, groupsDB.getParentGroups(groupIdTmp)))
								{
									found = true;
									break;
								}
							}
							if(found)
							{
								for(int groupIdTmp : pgBeanAvailableGroupsInt)
									ulozeneAdresare.append(groupsDB.getPath(groupIdTmp)).append(", ");
								if(Tools.isNotEmpty(ulozeneAdresare))
									ulozeneAdresare = new StringBuilder(ulozeneAdresare.substring(0, ulozeneAdresare.length()-2));
								break;
							}
						}

					}
				}

				//Logger.println(this,"UPDATE: " +groupName+ "  " +groupId);

				if (!found)
				{
					java.sql.Connection db_conn = null;
					java.sql.PreparedStatement ps = null;
					try{
						db_conn = DBPool.getConnection();
						String sql;

						if (groupId > 0)
						{
							sql = "UPDATE perex_groups SET perex_group_name=?, available_groups=? WHERE perex_group_id=?";
							if (pg!=null) Adminlog.add(Adminlog.TYPE_PEREX_GROUP_UPDATE, String.format("Zmena nazvu perex skupiny: %s => %s ", pg.getPerexGroupName(), groupName), groupId, -1);
							else Adminlog.add(Adminlog.TYPE_PEREX_GROUP_UPDATE, String.format("Zmena nazvu perex skupiny: %s => %s ", groupId, groupName), groupId, -1);
						}
						else
						{
							sql = "INSERT INTO perex_groups (perex_group_name, available_groups) VALUES (?,?)";
							Adminlog.add(Adminlog.TYPE_PEREX_GROUP_CREATE, "Vytvorena perex skupina: "+groupName, groupId, -1);
						}

						ps = db_conn.prepareStatement(sql);
						int ind = 1;
						ps.setString(ind++, groupName);
						ps.setString(ind++, availableGroups);
						if (groupId > 0)
							ps.setInt(ind++, groupId);

						resultsDb = ps.executeUpdate();
						ps.close();
						db_conn.close();

						if(resultsDb > 0)
							result = true;
						else
							result = false;

						db_conn = null;
						ps = null;
					}
					catch (Exception ex){Logger.error(DocDB.class, ex);}
					finally{
						try{
							if (ps != null) ps.close();
							if (db_conn != null) db_conn.close();
						}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
					}
				}
				else
				{
					if(request != null)
						request.setAttribute("groupExist", prop.getText("editor.perex_group.skupina_je_uz_definovana", ulozeneAdresare.toString()));
					result = false;
				}
			}
		}
		catch (Exception ex)
		{
			result = false;
			Logger.error(DocDB.class, ex);
		}

		if(resultsDb > 0)
			getPerexGroups(true);

		if (result) {
			ClusterDB.addRefresh(DocDB.class);
		}

		return(result);
	}

	/**
	 * Vrati List s nazvami perex skupin
	 * @return
	 */
	public List<PerexGroupBean> getPerexGroups()
	{
		return(getPerexGroups(false));
	}


	/**
	 *  Vrati List s nazvami perex skupin
	 * @param forceRefresh
	 * @return
	 */
	public List<PerexGroupBean> getPerexGroups(boolean forceRefresh)
	{
		if (perexGroups!=null && forceRefresh==false)
		{
			return(perexGroups);
		}

		List<PerexGroupBean> perexGroupsHolder = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			PerexGroupBean pgBean;
			String sql;

			sql = "SELECT * FROM perex_groups ORDER BY perex_group_name ASC";
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next())
			{
				pgBean = new PerexGroupBean();
				pgBean.setPerexGroupId(rs.getInt("perex_group_id"));
				pgBean.setPerexGroupName(DB.getDbString(rs, "perex_group_name"));
				pgBean.setRelatedPages(DB.getDbString(rs, "related_pages"));
				pgBean.setAvailableGroups(DB.getDbString(rs, "available_groups"));
				perexGroupsHolder.add(pgBean);
			}

			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		perexGroups = perexGroupsHolder;

		return (perexGroups);
	}

	public PerexGroupBean getPerexGroupByName(String name) {
	    List<PerexGroupBean> groups = getPerexGroups();
        try {
            return groups.stream().filter(i -> name.equalsIgnoreCase(i.getPerexGroupName())).findFirst().get(); //NOSONAR
        } catch(Exception e) {
				//
        }

	    return null;
    }

	/**
	 * Zmaze zadanu perex skupinu
	 * @param groups
	 * @return
	 */
	public boolean deletePerexGroup(String groups)
	{
		String[] groupsArray;
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			if (Tools.isNotEmpty(groups))
			{
				db_conn = DBPool.getConnection();

				groupsArray = Tools.getTokens(groups, ",");
				for (int i=0; i<groupsArray.length; i++)
				{
					int groupId = Tools.getIntValue(groupsArray[i], -1);
					PerexGroupBean perexGroup = getPerexGroup(groupId, groupsArray[i]);

					if (perexGroup.getPerexGroupId() < 0) continue;
					ps = db_conn.prepareStatement("DELETE FROM perex_groups WHERE perex_group_id=?");
					ps.setInt(1, perexGroup.getPerexGroupId()) ;
					ps.execute();
					ps.close();

					//zmazem aj pripadne zaznamy z perex_group_doc
					ps = db_conn.prepareStatement("DELETE FROM perex_group_doc WHERE perex_group_id=?");
					ps.setInt(1, perexGroup.getPerexGroupId());
					ps.execute();
					ps.close();

					Adminlog.add(Adminlog.TYPE_PEREX_GROUP_DELETE, "Zmazana perex skupina: "+perexGroup.getPerexGroupName()+" id="+perexGroup.getPerexGroupId(), perexGroup.getPerexGroupId(), -1);
					//Logger.println(this,"Deleting "+groupsArray[i]+", result: "+result);
				}

				db_conn.close();

				getPerexGroups(true);

				return(true);
			}
			db_conn = null;
			ps = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		getPerexGroups(true);
		return (false);
	}


	/**
	 * Vrati perex skupinu, bud sa zada id alebo name, ak su obe, tak sa zoberie id
	 * @param groupId - ak je -1 zoberiem name
	 * @param groupName
	 * @return
	 */
	public PerexGroupBean getPerexGroup(int groupId, String groupName)
	{
		for(PerexGroupBean pgBean : getPerexGroups())
		{
			if (groupId>0)
			{
				if (pgBean.getPerexGroupId()==groupId)
				{
					return(pgBean);
				}
			}
			else if (Tools.isNotEmpty(groupName) && pgBean.getPerexGroupName().equalsIgnoreCase(groupName))
			{
				return(pgBean);
			}
		}
		return(new PerexGroupBean());
	}

	/**
	 * Skonvertuje ID skupiny na meno
	 * @param perexGroupId
	 * @return
	 */
	public String convertPerexGroupIdToName(int perexGroupId)
	{
		PerexGroupBean pgBean = getPerexGroup(perexGroupId, null);
		if (pgBean == null) return null;
		return(pgBean.getPerexGroupName());
	}

	/**
	 * Skonvertuje z pola IDecok perex skupin na retazec nazvov
	 * @param perexGroupIds
	 * @return
	 */
	public String convertPerexGroupIdsToNames(String[] perexGroupIds)
	{
		if (perexGroupIds == null || perexGroupIds.length<1) return "";
		int size = perexGroupIds.length;
		StringBuilder perexGroupNames = new StringBuilder();
		int i;
		for (i=0; i<size; i++)
		{
			String name = convertPerexGroupIdToName(perexGroupIds[i]);
			if (perexGroupNames.length()>0) perexGroupNames.append(", ");
			if (Tools.isNotEmpty(name)) perexGroupNames.append(name);
		}
		return perexGroupNames.toString();
	}

	/**
	 * Skonvertuje z pola IDecok perex skupin na retazec nazvov
	 * @param perexGroupIds
	 * @return
	 */
	public String convertPerexGroupIdsToNames(int[] perexGroupIds)
	{
		if (perexGroupIds == null || perexGroupIds.length<1) return "";
		int size = perexGroupIds.length;
		StringBuilder perexGroupNames = new StringBuilder();
		int i;
		for (i=0; i<size; i++)
		{
			String name = convertPerexGroupIdToName(perexGroupIds[i]);
			if (perexGroupNames.length()>0) perexGroupNames.append(", ");
			if (Tools.isNotEmpty(name)) perexGroupNames.append(name);
		}
		return perexGroupNames.toString();
	}

	/**
	 * Skonvertuje ID skupiny na meno (id je ako String)
	 * @param perexGroupId
	 * @return
	 */
	public String convertPerexGroupIdToName(String perexGroupId)
	{
		int i = Tools.getIntValue(perexGroupId, -1);
		if (i>0)
		{
			return(convertPerexGroupIdToName(i));
		}
		//ak by nahodou to ID bolo nejake zmyslu plne, vratme to
		return(perexGroupId);
	}

	/**
	 * Skonvertuje nazov skupiny na id
	 *
	 * @author kmarton
	 * @param perexGroupName meno skupiny, ktorej chceme ziskat identifikator
	 *
	 * @return -1 ak sa take meno nenaslo, inak vrati identifikator skupiny
	 */
	public int convertPerexGroupNameToId(String perexGroupName)
	{
		PerexGroupBean pgBean = getPerexGroup(-1, perexGroupName);
		int perexGroupId = pgBean.getPerexGroupId();

		if (perexGroupId > 0)
			return(perexGroupId);
		else
			return -1;
	}

	/**
	 *
	 * @param rootGroupId
	 * @param time
	 * @return
	 */
	public static String getDateTimeCreatedString(int rootGroupId, boolean time)
	{
		StringBuilder ret = new StringBuilder();
		List<GroupDetails> searchGroupsArray;
		GroupsDB groupsDB = GroupsDB.getInstance();
		StringBuilder searchGroups = null;
		long dateTime = 0;
		java.text.SimpleDateFormat formatter;
		java.text.SimpleDateFormat timeFormatter;

		formatter = new java.text.SimpleDateFormat(sk.iway.iwcm.Constants.getString("dateFormat"));
		timeFormatter = new java.text.SimpleDateFormat("H:mm");

		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (rootGroupId < 1)
				rootGroupId = 1;

			db_conn = DBPool.getConnection();
			StringBuilder sql;

			searchGroupsArray = groupsDB.getGroupsTree(rootGroupId, true, false);
			for (GroupDetails group : searchGroupsArray)
			{
				if (group != null)
				{
					if (searchGroups == null)
					{
						searchGroups = new StringBuilder(String.valueOf(group.getGroupId()));
					}
					else
					{
						searchGroups.append(',').append(group.getGroupId());
					}
				}
			}

			sql = new StringBuilder("SELECT max(date_created) as last_update FROM documents");

			if (searchGroups != null)
			{
				sql.append(" WHERE group_id IN (").append(getOnlyNumbersIn(searchGroups.toString(),true)).append(')');
			}


			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			if(rs.next())
			{

				dateTime = DB.getDbTimestamp(rs, "last_update");

			}

			ret = new StringBuilder(formatter.format(new java.util.Date(dateTime)));
			if(time)
			{
				ret.append("  ").append(timeFormatter.format(new java.util.Date(dateTime)));
			}

			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (ret.toString());
	}

	/**
	 * Vrati zoznam roznych hodnot v danom fielde, pouziva sa na rendering selectu pre hodnoty vo field_X
	 * @param field
	 * @return
	 */
	public static List<String> getFieldDistinctValues(String field)
	{
		List<String> ret = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT DISTINCT "+field+" as field_value FROM documents ORDER BY " + field);
			rs = ps.executeQuery();
			String value;
			while (rs.next())
			{
				value = DB.getDbString(rs, "field_value");
				if (Tools.isNotEmpty(value))
				{
					ret.add(value);
				}
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
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
	 * Zistenie ci stranku mozeme povazovat za novu, zmenenu alebo nemodifikovanu
	 *
	 * nova je taka, ktora nema historiu starsiu ako zadany pocet dni
	 * zmenena je taka, kde doslo za zadany pocet dni k zmene
	 *
	 * @param docId - id stranky
	 * @param minDaysNotChanged - pocet dni, pocas ktorych nesmelo dojst k zmene
	 * @param maxDaysTestChanged - maximalny pocet dni, ktore sa testuju na zmenu (ak v tomto rozsahu nie je ziadna zmena, je dokument bezo zmeny)
	 * @return 0=bez zmeny, 1=nova, 2=zmenena
	 */
	public static int getPageNewChangedStatus(int docId, int minDaysNotChanged, int maxDaysTestChanged)
	{
		int status = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -minDaysNotChanged);
			long dateNewStart = cal.getTime().getTime();

			//Logger.println(this,"dateNewStart"+Tools.formatDateTime(dateNewStart));

			//cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -maxDaysTestChanged);
			long dateChangedStart = cal.getTime().getTime();


			//Logger.println(this,"dateChangedStart"+Tools.formatDateTime(dateChangedStart));

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT save_date, actual FROM documents_history WHERE doc_id=? AND awaiting_approve IS NULL ORDER BY save_date DESC");
			ps.setInt(1, docId);
			//ps.setTimestamp(2, new Timestamp(dateChangedStart));
			rs = ps.executeQuery();
			boolean actual = false;
			long saveDate;
			while (rs.next())
			{
				if (!actual)
				{
					actual = rs.getBoolean("actual");
				}
				//tu nemoze byt else, inak by to nezbehlo ak je len jedna zmena v DB
				if (actual)
				{
					saveDate = rs.getTimestamp("save_date").getTime();
					//Logger.println(this,"saveDate"+Tools.formatDateTime(saveDate));
					if (saveDate > dateNewStart)
					{
						//predpokladame, ze je nova
						status = 1;
					}
					else if (saveDate < dateChangedStart)
					{
						//ak sme ju mali ako novu a narazili sme na datum po rozsahu maxDaysTestChanged, nie je nova, ale zmenena
						if (status == 1) status = 2;
						break;
					}
					else
					{
						status = 2;
						break;
					}
					//Logger.println(this,"nastavujem["+docId+"]: " + status);
				}

			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
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

		return(status);
	}

	/** vrati najvacsiu prioritu so vsetkych dokumentov v skupine
	 * @param groupId
	 * @return
	 */
	public static int getMaxSortPriorityInGroup(int groupId) {
		int maxP = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{


			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("select max(sort_priority) as max,group_id from documents where group_id = ? group by group_id");
			ps.setInt(1, groupId);

			rs = ps.executeQuery();

			while (rs.next())
			{
				maxP = rs.getInt("max");
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
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

		return(maxP);
	}

	/**
	 * Otestuje, ci stranka je pristupna aktualne prihlasenemu clovekovi
	 * @param doc
	 * @param user
	 * @return
	 */
	public static boolean canAccess(DocDetails doc, Identity user)
	{
		return canAccess(doc, user, false);
	}

	/**
	 * Otestuje, ci stranka je pristupna aktualne prihlasenemu clovekovi
	 * @param doc
	 * @param user
	 * @param checkGroup - ak je nastavene na true, skontroluju sa aj prava nastavene pre adresar
	 * @return
	 */
	public static boolean canAccess(DocDetails doc, Identity user, boolean checkGroup)
	{
		//aby sme nemenili hodnoty pre BasicDocDetails
		String passwordProtected = doc.getPasswordProtected();

		if (checkGroup && Tools.isEmpty(passwordProtected))
		{
			GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
			if (group != null)
			{
				passwordProtected = group.getPasswordProtected();
			}
		}

		if (Tools.isEmpty(passwordProtected)) return(true);

		if (user == null)
		{
			//tu nemozeme vratit false, pretoze doc moze mat nastavene len email skupiny
			user = new Identity();
		}

		Logger.debug(DocDB.class, "doc("+doc.getDocId()+").getPasswordProtected()="+passwordProtected);
		//zisti ci dany user ma pravo na skupinu pre web stranku
		boolean canAccess = false;
		if (Tools.isNotEmpty(user.getUserGroupsIds()))
		{
			Logger.debug(DocDB.class,"doc perms="+passwordProtected+" user="+user.getUserGroupsIds()+" userid="+user.getUserId());
			StringTokenizer st = new StringTokenizer(user.getUserGroupsIds(), ",");
			String passwordProtected2 = "," + passwordProtected + ",";
			while (st.hasMoreTokens())
			{
				if (passwordProtected2.indexOf("," + st.nextToken() + ",") != -1)
				{
					Logger.debug(DocDB.class, "canAccess=true");
					canAccess = true;
					break;
				}
			}
		}

		if (!canAccess)
		{
			//otestuj, ci stranka nema nastavene len email skupiny
			StringTokenizer st = new StringTokenizer(passwordProtected, ",");
			boolean onlyEmailGroups = true;
			UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
			UserGroupDetails ugd;
			int groupId;
			while (st.hasMoreTokens())
			{
				groupId = Tools.getIntValue(st.nextToken().trim(), -1);
				if (groupId != -1)
				{
					ugd = userGroupsDB.getUserGroup(groupId);
					if (ugd != null && ugd.getUserGroupType() == UserGroupDetails.TYPE_PERMS)
					{
						onlyEmailGroups = false;
					}
				}
				else
				{
					//nezbehlo parsovanie, co s tym? Zakazem to?
					onlyEmailGroups = false;
				}
			}

			if (onlyEmailGroups == true)
			{
				canAccess = true;
			}
		}

		if (user.isAdmin())
		{
			if (Constants.getBoolean("adminCheckUserGroups")==false)
			{
				Logger.debug(DocDB.class, "je to admin, pristup povolujem");
				//ak je to admin a pre admina sa nekontroluju prava (default), zobraz
				canAccess = true;
			}
		}
		return(canAccess);
	}

	/**
	 * Skontroluje, ci je dostupny adresar aktualne prihlasenemu pouzivatelovi
	 * @param group
	 * @param user
	 * @return
	 */
	public static boolean canAccess(GroupDetails group, Identity user)
	{
		DocDetails doc = new DocDetails();
		doc.setDocId(-group.getGroupId());
		doc.setPasswordProtected(group.getPasswordProtected());

		return canAccess(doc, user);
	}

	/**
	 * Ziska urlsByUrl hashtabulku pre zadanu domenu
	 * @param domain
	 * @return
	 */
	protected TObjectIntHashMap<String> getUrlsByUrlDomains(String domain, boolean createIfNull)
	{
		if (createIfNull==false && urlsByUrlDomains.size()==1)
		{
			return urlsByUrlDomains.entrySet().iterator().next().getValue();
		}

		if (Tools.isEmpty(domain)) domain = "default";
		try
		{
			//normalize domain - odstran http
			int i = domain.indexOf("://");
			if (i>0) domain = domain.substring(i+3);

			i = domain.indexOf('/');
			if (i>0) domain = domain.substring(0, i);
		}
		catch (Exception e)
		{
			Logger.error(DocDB.class, e);
		}

		TObjectIntHashMap<String> urlsByUrl = urlsByUrlDomains.get(domain);
		if (urlsByUrl==null)
		{
			if (createIfNull)
			{
				urlsByUrl = new TObjectIntHashMap<>(10, 0.9f);
				urlsByUrlDomains.put(domain, urlsByUrl);
			}
			else
			{
				//v cloude nechceme dohladavat "default" redirecty
				if (InitServlet.isTypeCloud()==false)
				{
					urlsByUrl = urlsByUrlDomains.get("default");
					if (urlsByUrl==null)
					{
						if (Constants.getBoolean("multiDomainEnabled")==true)
						{
							urlsByUrl = new TObjectIntHashMap<>(10, 0.9f);
							urlsByUrlDomains.put(domain, urlsByUrl);
						}
						else if (urlsByUrlDomains.size() > 0)
						{
							//ak nie je z nejakeho dovodu ani default, vrat prvu najdenu
							urlsByUrl = urlsByUrlDomains.entrySet().iterator().next().getValue();
						}
					}
				}
			}
		}

		//System.out.println("getUrlsByUrlDomains("+domain+").size="+urlsByUrl.size());

		return urlsByUrl;
	}

	/**
	 * Vrati zoznam stranok so zadanou sablonou
	 * @param tid
	 * @return
	 */
	public static List<DocDetails> getDocsByTempId(int tid)
	{
		List<DocDetails> docs = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM documents WHERE temp_id=?");
			ps.setInt(1, tid);
			rs = ps.executeQuery();

			DocDetails doc;
			GroupsDB groupsDB = GroupsDB.getInstance();
			boolean linkTypeHtml = false;
			if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML)
			{
				linkTypeHtml = true;
			}
			while (rs.next())
			{
				doc = new DocDetails();
				getDocDetails(rs, doc, true, true);

				if (linkTypeHtml)
				{
					doc.setDocLink(DocDB.getURL(doc, groupsDB));
				}
				doc.setNavbar(groupsDB.getNavbarNoHref(doc.getGroupId()));

				docs.add(doc);
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
			Logger.error(DocDB.class, ex);
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
		return (docs);
	}

	/**
	 * vrati vsetky stranky medzi zadanymi datumami a so zadanym authorId
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static List<DocDetails> getAllDocsFromTo(String dateFrom, String dateTo, Integer authorId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<DocDetails> docs = new ArrayList<>();

		try
		{
			db_conn = DBPool.getConnection();

			if(authorId == null || authorId.intValue() == -1)
			{
				ps = db_conn.prepareStatement("SELECT * FROM documents WHERE date_created >= ? AND date_created <= ? ORDER BY date_created DESC");
				ps.setTimestamp(1,new Timestamp(DB.getTimestamp(dateFrom,"0:00:00")));
				ps.setTimestamp(2,new Timestamp(DB.getTimestamp(dateTo,"23:59:59")));
			}else{
				ps = db_conn.prepareStatement("SELECT * FROM documents WHERE date_created >= ? AND date_created <= ? AND author_id = ? ORDER BY date_created DESC");
				ps.setTimestamp(1,new Timestamp(DB.getTimestamp(dateFrom,"0:00:00")));
				ps.setTimestamp(2,new Timestamp(DB.getTimestamp(dateTo,"23:59:59")));
				ps.setInt(3, authorId);
			}

			rs = ps.executeQuery();

			DocDetails doc;
			GroupsDB groupsDB = GroupsDB.getInstance();
			boolean linkTypeHtml = false;
			if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML)
			{
				linkTypeHtml = true;
			}

			while (rs.next())
			{
				doc = new DocDetails();
				DocDB.getDocDetails(rs, doc, true, true);

				if (linkTypeHtml)
				{
					doc.setDocLink(DocDB.getURL(doc, groupsDB));
				}
				doc.setNavbar(groupsDB.getNavbarNoHref(doc.getGroupId()));

				docs.add(doc);
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
			Logger.error(DocDB.class, ex);
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
				Logger.error(DocDB.class, ex2);
			}
		}

		return docs;
	}

	/**
	 * Vrati domenu pre zadane docId
	 * @param docId
	 * @return
	 */
	public String getDomain(int docId)
	{
		DocDetails doc = getBasicDocDetails(docId, false);
		if (doc != null)
		{
			GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
			if (group != null)
			{
				return group.getDomainName();
			}
		}

		return "";
	}

	/**
	 * Vrati dokumenty pouzivatela, ktore este neboli schvalene
	 * @return
	 */
	public List<DocDetails> getNotApprovedDocs(int userId)
	{
		List<DocDetails> docs = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(serverName);
			ps = db_conn.prepareStatement("SELECT history_id, dh.doc_id, dh.save_date, dh.title, dh.author_id, dh.group_id, dh.available FROM documents_history dh" +
					" JOIN documents ON (documents.doc_id = dh.doc_id)"+
					" WHERE dh.awaiting_approve IS NOT NULL AND NOT dh.awaiting_approve='' AND dh.author_id = ? ORDER BY save_date DESC");
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			DocDetails doc;
			GroupsDB groupsDB = GroupsDB.getInstance();
			while (rs.next())
			{
				doc = new DocDetails();
				doc.setHistoryId(rs.getInt("history_id"));
				doc.setDocId(rs.getInt("doc_id"));
				doc.setAuthorId(rs.getInt("author_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setNavbar(groupsDB.getNavbarNoHref(doc.getGroupId()));
				doc.setDateCreated(DB.getDbTimestamp(rs, "save_date"));
				doc.setAvailable(rs.getBoolean("available"));
				docs.add(doc);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){Logger.error(DocDB.class, ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return(docs);
	}

	/**
	 * Nastavi title tak, ze obsahuje aj spatnu cestu k adresarom
	 * @param doc
	 * @return
	 */
	public static String getTitleWithPath(DocDetails doc)
	{
		if (doc == null) return "";
		StringBuilder title = new StringBuilder(doc.getTitle());

		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group = groupsDB.getGroup(doc.getGroupId());
		if (group == null) return title.toString();
		if (group.getNavbarNameNoAparam().equals(doc.getTitle())==false && group.getDefaultDocId()!=doc.getDocId() && "&nbsp;".equals(group.getNavbarNameNoAparam())==false && Tools.isNotEmpty(group.getNavbarNameNoAparam()))
			title.append(" - ").append(group.getNavbarNameNoAparam());
		int failsafe = 0;

		while (group!=null && group.getParentGroupId()>0 && failsafe++ < 50)
		{
			group = groupsDB.getGroup(group.getParentGroupId());
			if (group != null && "&nbsp;".equals(group.getNavbarNameNoAparam())==false && Tools.isNotEmpty(group.getNavbarNameNoAparam()))
			{
				title.append(" - ").append(group.getNavbarNameNoAparam());
			}
		}

		return title.toString();
	}

	/**
	 * Ulozi DocDetails do databazy, POZOR: NEVYKONA DocDB.getInstance(true)
	 * @param docDetails
	 */
	public static boolean saveDoc(DocDetails docDetails) {
		return saveDoc(docDetails, true);
	}

	/**
	 * Ulozi DocDetails do databazy, POZOR: NEVYKONA DocDB.getInstance(true)
	 * @param docDetails
	 * @param publishEvents - ak je true publikuju sa aj udalosti o zmene
	 * @return
	 */
	public static boolean saveDoc(DocDetails docDetails, boolean publishEvents)
	{
		if (docDetails==null) return false;

		boolean saveOK = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			if (docDetails!=null && publishEvents) (new WebjetEvent<DocDetails>(docDetails, WebjetEventType.ON_START)).publishEvent();

			Prop properties = Prop.getInstance();

			String sql = "INSERT INTO documents (title, data, data_asc, external_link, navbar, date_created, " +
					"publish_start, publish_end, author_id, group_id, temp_id, searchable, available, " +
					"cacheable, sort_priority, header_doc_id, footer_doc_id, menu_doc_id, password_protected, html_head, "+
					"html_data, perex_place, perex_image, perex_group, show_in_menu, event_date, right_menu_doc_id," +
					"field_a, field_b, field_c, field_d, field_e, field_f, field_g, field_h, field_i, field_j, field_k, field_l, disable_after_end, virtual_path, require_ssl, file_name, field_m, field_n, field_o, field_p, field_q, field_r, field_s, field_t, root_group_l1, root_group_l2, root_group_l3) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			if (docDetails.getDocId() > 0)
			{
				sql = "UPDATE documents SET title=?, data=?, data_asc=?, external_link=?, navbar=?, date_created=?, " +
						"publish_start=?, publish_end=?, author_id=?, group_id=?, temp_id=?, searchable=?, available=?, " +
						"cacheable=?, sort_priority=?, header_doc_id=?, footer_doc_id=?, menu_doc_id=?, password_protected=?, html_head=?, "+
						"html_data=?, perex_place=?, perex_image=?, perex_group=?, show_in_menu=?, event_date=?, right_menu_doc_id=?," +
						"field_a=?, field_b=?, field_c=?, field_d=?, field_e=?, field_f=?, field_g=?, field_h=?, field_i=?, field_j=?, field_k=?, field_l=?, disable_after_end=?, virtual_path=?, require_ssl=?, file_name=?, field_m=?, field_n=?, field_o=?, field_p=?, field_q=?, field_r=?, field_s=?, field_t=?, root_group_l1=?, root_group_l2=?, root_group_l3=?, sync_status=1 " +
						"WHERE doc_id=?";
			}

			String publishDocData = null;
			java.util.Date now = new java.util.Date();
			long time = docDetails.getPublishStart();
			if (time > 0)
			{
				//ak sa to ma vypublikovat az po aktualnom case, zrus available
				if (time > now.getTime())
				{
					if (docDetails.isPublicable())
					{
						docDetails.setAvailable(false);
						publishDocData = properties.getText("editor.publish.note") + " " + docDetails.getPublishStartString() + " " + docDetails.getPublishStartTimeString();
					}
				}
			}

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, docDetails.getTitle());
			if (publishDocData != null)
			{
				DB.setClob(ps, 2, publishDocData);
				DB.setClob(ps, 3, DB.internationalToEnglish(publishDocData).toLowerCase());
			}
			else
			{
				DB.setClob(ps, 2, docDetails.getData());
				EditorForm ef = new EditorForm(docDetails);
				DB.setClob(ps, 3, EditorDB.getDataAsc(docDetails.getData(), ef));
			}

			ps.setString(4, docDetails.getExternalLink());
			ps.setString(5, docDetails.getNavbar());

			ps.setTimestamp(6, new Timestamp(now.getTime()));

			if (Tools.isNotEmpty(docDetails.getPublishStartString()))
			{
				if (time > 0)
				{
					ps.setTimestamp(7, new Timestamp(time));
				}
				else
				{
					ps.setNull(7, java.sql.Types.TIMESTAMP);
				}
			}
			else
			{
				ps.setNull(7, java.sql.Types.DATE);
			}

			if (Tools.isNotEmpty(docDetails.getPublishEndString()))
			{
				long time2 = docDetails.getPublishEnd();
				if (time2 > 0)
				{
					ps.setTimestamp(8, new Timestamp(time2));
				}
				else
				{
					ps.setNull(8, java.sql.Types.TIMESTAMP);
				}
			}
			else
			{
				ps.setNull(8, java.sql.Types.DATE);
			}

			ps.setInt(9, docDetails.getAuthorId());
			ps.setInt(10, docDetails.getGroupId());
			ps.setInt(11, docDetails.getTempId());
			ps.setBoolean(12, docDetails.isSearchable());
			ps.setBoolean(13, docDetails.isAvailable());
			ps.setBoolean(14, docDetails.isCacheable());
			ps.setInt(15, docDetails.getSortPriority());
			ps.setInt(16, docDetails.getHeaderDocId());
			ps.setInt(17, docDetails.getFooterDocId());
			ps.setInt(18, docDetails.getMenuDocId());
			if (docDetails.getPasswordProtected() == null)
			{
				ps.setNull(19, java.sql.Types.VARCHAR);
			}
			else
			{
				ps.setString(19, docDetails.getPasswordProtected());
			}
			DB.setClob(ps, 20, docDetails.getHtmlHead());
			if (docDetails.getHtmlData() == null || docDetails.getHtmlData().length() < 1)
			{
				ps.setNull(21, Types.VARCHAR);
			}
			else
			{
				DB.setClob(ps, 21, docDetails.getHtmlData());
			}
			ps.setString(22, docDetails.getPerexPlace());
			ps.setString(23, docDetails.getPerexImage());

			ps.setString(24, ","+docDetails.getPerexGroupIdsString()+",");
			ps.setBoolean(25, docDetails.isShowInMenu());

			if (docDetails.getEventDateString()!=null )
			{
				if (docDetails.getEventTimeString().trim().length() < 3)
				{
					docDetails.setEventTimeString("6:00");
				}
				long time2 = docDetails.getEventDate();
				if (time2 > 0)
				{
					ps.setTimestamp(26, new Timestamp(time2));
				}
				else
				{
					ps.setNull(26, java.sql.Types.TIMESTAMP);
				}
			}
			else
			{
				ps.setNull(26, java.sql.Types.DATE);
			}
			ps.setInt(27, docDetails.getRightMenuDocId());

			ps.setString(28, docDetails.getFieldA());
			ps.setString(29, docDetails.getFieldB());
			ps.setString(30, docDetails.getFieldC());
			ps.setString(31, docDetails.getFieldD());
			ps.setString(32, docDetails.getFieldE());
			ps.setString(33, docDetails.getFieldF());
			ps.setString(34, docDetails.getFieldG());
			ps.setString(35, docDetails.getFieldH());
			ps.setString(36, docDetails.getFieldI());
			ps.setString(37, docDetails.getFieldJ());
			ps.setString(38, docDetails.getFieldK());
			ps.setString(39, docDetails.getFieldL());

			ps.setBoolean(40, docDetails.isDisableAfterEnd());
			if (Tools.isEmpty(docDetails.getVirtualPath()))
			{
				EditorForm ef = new EditorForm(docDetails);
				EditorDB.setVirtualPath(ef);
				docDetails.setVirtualPath(ef.getVirtualPath());
			}
			ps.setString(41, docDetails.getVirtualPath());

			ps.setBoolean(42, docDetails.isRequireSsl());

			GroupsDB groupsDB = GroupsDB.getInstance();
			GroupDetails group = groupsDB.getGroup(docDetails.getGroupId());
			String fileName = null;
			if (group != null && group.isInternal()==false)
			{
				fileName = groupsDB.getGroupNamePath(docDetails.getGroupId());
				//musim obmedzit na max 255 znakov
				if(fileName.length() > 255)
				{
					fileName = DB.prepareString(fileName, 255);
					int lastSlash = fileName.lastIndexOf("/");
					if(lastSlash != -1) fileName = fileName.substring(0, lastSlash);
				}
			}
			ps.setString(43, fileName);

			ps.setString(44, docDetails.getFieldM());
			ps.setString(45, docDetails.getFieldN());
			ps.setString(46, docDetails.getFieldO());
			ps.setString(47, docDetails.getFieldP());
			ps.setString(48, docDetails.getFieldQ());
			ps.setString(49, docDetails.getFieldR());
			ps.setString(50, docDetails.getFieldS());
			ps.setString(51, docDetails.getFieldT());

			DocDB.getRootGroupL(docDetails.getGroupId(), ps, 52);

			if (docDetails.getDocId() > 0)
			{
				ps.setInt(55, docDetails.getDocId());
			}

			ps.execute();
			ps.close();

			if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
			{
				db_conn.commit();
			}

			//get new doc_id
			//get document id
			// JRASKA: Toto predsa treba robit len pri novom clanku, inak zmenime docID pre predtym existujuci clanok
			int docId = -1;
			if (docDetails.getDocId() < 1)
			{
				ps = db_conn.prepareStatement("SELECT max(doc_id) FROM documents WHERE title=?");
				ps.setString(1, docDetails.getTitle());
				rs = ps.executeQuery();
				if (rs.next())
				{
					docId = rs.getInt(1);
				}
				rs.close();
				ps.close();
				docDetails.setDocId(docId);
			}

			//aktualizuj pripadne aj tab. perex_group_doc
			DocDB.udpdatePerexGroupDoc(docDetails.getDocId(), docDetails.getPerexGroupIdsString());

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			//ak su nastavene syncId tak zapis aj to, potrebne pre synchronizaciu struktury
			if (docDetails.getSyncId()>0) {
				new SimpleQuery().execute("UPDATE documents SET sync_id=? WHERE doc_id=?", docDetails.getSyncId(), docDetails.getDocId());
			}

			//premenovanie Groupy ak je stranka defaultna pre Grupu.
			if(GroupsService.canSyncTitle(docDetails.getDocId(), docDetails.getGroupId()))
			{
				changeGroupTitle(docDetails.getGroupId(), docDetails.getDocId(), docDetails.getTitle());
			}

			DocDB.getInstance().updateInternalCaches(docDetails.getDocId());
			saveOK = true;

			if (docDetails!=null && publishEvents) (new WebjetEvent<DocDetails>(docDetails, WebjetEventType.AFTER_SAVE)).publishEvent();
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
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

		return saveOK;
	}

	public static boolean isGroupAvailable(int[] availableGroups, List<GroupDetails> groups)
	{
		return isGroupAvailable(availableGroups, groups, false, null);
	}

	/**
	 * availableGroups - array of int Ids represent in which groups is perex available
	 * groups - list of parent groups of actual selected group in jsTree
	 * recursive - True (check if perexGroup is available not only for parent groups but also for child group of selected group in jsTree)
	 * actualGropId - if "recursive" if true this MUST by set (its id of selected group in jsTree)
	 */
	public static boolean isGroupAvailable(int[] availableGroups, List<GroupDetails> groups, boolean recursive, Integer actualGropId)
	{
		//check availability for parent groups
		for (int groupId : availableGroups)
		{
			for (GroupDetails group : groups)
			{
				if (group.getGroupId() == groupId) return true;
			}
		}

		//check availability for child groups
		if(recursive && actualGropId != null) {

			GroupsDB groupsDB = GroupsDB.getInstance();

			for (int availableGroupId : availableGroups) {

				int[] tmpParentGroupIds = Tools.getTokensInt(groupsDB.getParents(availableGroupId), ",");

				for(int tmpParentGroupId : tmpParentGroupIds) {
					if(tmpParentGroupId == actualGropId) return true;
				}
			}
		}

		return false;
	}

	/**
	 * Vrati perex groupy dostupne pre zadane groupId, pokial nieje zadane groupId, vrati zoznam skupin, ktore niesu priradene ziadnemu adresaru
	 * @param groupId
	 * @return
	 */
	public List<PerexGroupBean> getPerexGroups(int groupId)
	{
		return getPerexGroups(groupId, false);
	}

	/**
	 * Vrati perex groupy dostupne pre zadane groupId, pokial nieje zadane groupId, vrati zoznam skupin, ktore niesu priradene ziadnemu adresaru
	 * @param groupId
	 * @return
	 */
	public List<PerexGroupBean> getPerexGroups(int groupId, boolean recursive)
	{
		int[] groupIds = new int[1];
		groupIds[0] = groupId;
		return getPerexGroups(groupIds, recursive);
	}

	/**
	 * Returns PerexGroups available in groupIds folder (if not empty)
	 * @param groupIds
	 * @param recursive - true to recursivelly check subfolders
	 * @return
	 */
	public List<PerexGroupBean> getPerexGroups(int[] groupIds, boolean recursive)
	{
		List<PerexGroupBean> ret = new ArrayList<>();
		if(groupIds.length > 0 && groupIds[0]>0)
		{
			//najskor potrebujeme zoznam parent skupin
			GroupsDB groupsDB = GroupsDB.getInstance();
			Set<Integer> duplicityCheck = new HashSet<>();

			for (int groupId : groupIds) {
				List<GroupDetails> parentGroups = groupsDB.getParentGroups(groupId);

				//get all perex groups
				List<PerexGroupBean> allPerexGroups = getPerexGroups();

				if(allPerexGroups != null && allPerexGroups.size() > 0)
				{
					//Loop all perex groups and for each one call isGroupAvailable method (method will return true if this perexGroup is availaible)
					for(PerexGroupBean perexGroup : allPerexGroups)
					{
						if (perexGroup.getPerexGroupId() < 1) continue;

						int[] perexAvailableGroups = perexGroup.getAvailableGroupsInt();

						//isGroupAvailable param "recursive" and "groupId" are set for getting perexGroups for child (all subfolders - if recursive is true)
						if (perexAvailableGroups.length == 0 || isGroupAvailable(perexAvailableGroups, parentGroups, recursive, groupId))
						{
							if (duplicityCheck.contains(perexGroup.getPerexGroupId())==false) ret.add(perexGroup);
							duplicityCheck.add(perexGroup.getPerexGroupId());
						}
					}
				}
			}
		}
		else
		{
			//pokial nemame zadane groupId, tak vratime zoznam skupin, ktore su dostupne pre vsetky adresare
			List<PerexGroupBean> allPerexGroups = getPerexGroups();
			if(allPerexGroups != null && allPerexGroups.size() > 0)
			{
				for(PerexGroupBean perexGroup : allPerexGroups)
				{
					if(Tools.isEmpty(perexGroup.getAvailableGroups()))
						ret.add(perexGroup);
				}
			}
		}

		Collator collator = Collator.getInstance();
		ret.sort((o1, o2) -> collator.compare(o1.getPerexGroupName().toLowerCase(), o2.getPerexGroupName().toLowerCase()));

		return (ret);
	}

	/**
	 * Vrati vsetky clanky t.z. webstranky, ktore vytvoril dany pouzivatel a vyfiltruje ich podla zadanych kriterii
	 *
	 * @param authorId			autor webstranok, identifikator pouzivatela
	 * @param filterTopicId		identifikator rubriky - groupId
	 * @param filterBlogName	nazov clanku, pouziva sa LIKE, cize staci len cast nazvu
	 * @param filterDateFrom	datum ulozenia od
	 * @param filterDateTo		datum ulozenia do
	 *
	 * @return
	 */
	public static List<DocDetails> getBlogs(int authorId, int filterTopicId, String filterBlogName, Date filterDateFrom, Date filterDateTo)
	{
		List<DocDetails> docs = new ArrayList<>();

		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();

			//--------------VYBER AUTOROVYCH CLANKOV--------------------------
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT " + DocDB.getDocumentFieldsNodata() + " FROM documents d WHERE d.author_id = ? ");

			if(Tools.isNotEmpty(filterBlogName))
				sql.append(" AND d.title LIKE ? ");
			if (filterDateFrom != null)
				sql.append(" AND d.date_created > ? ");
			if (filterDateTo != null)
				sql.append(" AND d.date_created < ? ");

			sql.append(" ORDER BY d.date_created DESC, doc_id DESC");

			ps = db_conn.prepareStatement(sql.toString());
			int psCounter = 1;
			ps.setInt(psCounter++, authorId);
			if(Tools.isNotEmpty(filterBlogName))
				ps.setString(psCounter++, "%" + filterBlogName + "%");
			if (filterDateFrom != null)
				ps.setTimestamp(psCounter++, new Timestamp(filterDateFrom.getTime()));
			if (filterDateTo != null)
				ps.setTimestamp(psCounter++, new Timestamp(filterDateTo.getTime()));

			rs = ps.executeQuery();

			int failsafe = Constants.getInt("searchTextAllLimit");
			int counter = 0;
			while (rs.next() && counter++ < failsafe)
			{
				DocDetails doc = DocDB.getDocDetails(rs, true, true);

				//mozny filter, ak bol zadany iba vybrany topic
				if (filterTopicId > -1 && doc.getGroupId() != filterTopicId)
					continue;

				//ak sme nasli defaultnu stranku adresara, tak tu nezobrazuj, pretoze pravdepodobne ide o news komponentu
				if (group == null || group.getGroupId()!= doc.getGroupId())
					group = groupsDB.getGroup( doc.getGroupId() );
				if (group != null && group.getDefaultDocId() == doc.getDocId())
					continue;

				docs.add( doc );
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
			Logger.error(DocDB.class, ex);
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
				Logger.error(DocDB.class, ex2);
			}
		}

		return docs;
	}

	/**
	 * Vrati identifikator skupiny Blog
	 *
	 * @return
	 */
	public static int getBlogGroupId()
	{
		return new SimpleQuery().forInt("SELECT user_group_id FROM user_groups WHERE user_group_name LIKE 'Blog'");
	}

	public synchronized void updateInternalCaches(int docId)
	{
		//najskor vyber z DB rovnako ako pre loadUrls
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT doc_id, title, navbar, external_link, group_id, virtual_path, available, searchable, show_in_menu, sort_priority, password_protected, temp_id, date_created, field_a, field_b, field_c FROM documents WHERE doc_id=?";

			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, docId);
			rs = ps.executeQuery();

			DocDetails doc = null;
			boolean docIsNew = false;
			int oldGroupId = -1;
			String oldVirtualPath = null;

			if (rs.next())
			{
				doc = getBasicDocDetails(docId, false);
				if (doc == null)
				{
					doc = new DocDetails();
					docIsNew = true;
				}
				else
				{
					oldGroupId = doc.getGroupId();
					oldVirtualPath = doc.getVirtualPath();
				}

				doc.setDocId(rs.getInt("doc_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				String navbar = DB.getDbString(rs, "navbar");
				if (doc.getTitle().equals(navbar)==false) doc.setNavbar(navbar);
				doc.setExternalLink(DB.getDbString(rs, "external_link"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setVirtualPath(normalizeVirtualPath(DB.getDbString(rs, "virtual_path")));
				doc.setAvailable(rs.getBoolean("available"));
				doc.setSearchable(rs.getBoolean("searchable"));
				doc.setShowInMenu(rs.getBoolean("show_in_menu"));
				doc.setSortPriority(rs.getInt("sort_priority"));
				doc.setPasswordProtected(DB.getDbString(rs, "password_protected"));
				doc.setTempId(rs.getInt("temp_id"));
				doc.setDateCreated(rs.getTimestamp("date_created").getTime());

				doc.setFieldA(DB.getDbString(rs, "field_a"));
				doc.setFieldB(DB.getDbString(rs, "field_b"));
				doc.setFieldC(DB.getDbString(rs, "field_c"));

				//POZOR: do fieldT si neskor ulozime DOMENU
				GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
				if (group != null && Tools.isNotEmpty(group.getDomainName()))
				{
					doc.setFieldT(group.getDomainName());
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			if (doc != null)
			{
				if (docIsNew)
					putToBasicDocDetailsAndResize(doc);

				if (docIsNew == false && oldGroupId != doc.getGroupId())
				{
					//najskor vymazeme povodny zaznam
					List<DocDetails> list = basicAllDocsInGroupTable.get(oldGroupId);
					if (list != null)
					{
						Iterator<DocDetails> iter = list.iterator();
						while (iter.hasNext())
						{
							if (iter.next().getDocId()==doc.getDocId())
							{
								Logger.debug(DocDB.class, "updateInternalCaches: mazem stary zaznam v basilAllDocsInGroupTable "+oldGroupId);
								iter.remove();
								break;
							}
						}
					}
				}
				if (docIsNew || oldGroupId != doc.getGroupId())
				{
					//vlozime na spravne miesto
					List<DocDetails> list = basicAllDocsInGroupTable.get(doc.getGroupId());
					if (list==null)
					{
						list = new ArrayList<>();
						Logger.debug(DocDB.class, "updateInternalCaches: vytvaram novy list");
						basicAllDocsInGroupTable.put(doc.getGroupId(), list);
					}
					Logger.debug(DocDB.class, "updateInternalCaches: vkladam do listu basicAllDocsInGroupTable");
					list.add(doc);
				}

            	DocumentPublishEvent documentPublishEvent = new DocumentPublishEvent(doc);

				//aktualizovat URL schemu
				//toto drzi hashtabulku url pre danu domenu, vzdy k tomu pristupujte cez getUrlsByUrlDomains(domena)
				//private Map<String, Map<String, Integer>> urlsByUrlDomains = null;
				if (doc.getVirtualPath().equals(oldVirtualPath)==false || oldGroupId != doc.getGroupId())
				{
					GroupsDB groupsDB = GroupsDB.getInstance();

					String oldDomain = null;
					GroupDetails oldGroup = groupsDB.getGroup(oldGroupId);
					if (oldGroup != null) oldDomain = oldGroup.getDomainName();

					String newDomain = null;
					if (Constants.getBoolean("multiDomainEnabled"))
					{
						GroupDetails group = groupsDB.getGroup(doc.getGroupId());
						if (group != null) newDomain = group.getDomainName();
					}
					else
					{
						newDomain = oldDomain;
					}

					//vymazeme staru hodnotu z hash tabulky
					TObjectIntHashMap<String> oldTable = getUrlsByUrlDomains(oldDomain, false);
					if (oldTable != null && oldVirtualPath != null)
					{
						Logger.debug(DocDB.class, "updateInternalCaches: mazem stare URL ("+oldVirtualPath+") pre domenu "+oldDomain);
						oldTable.remove(oldVirtualPath);
					}

					//zapiseme novu
					TObjectIntHashMap<String> newTable = getUrlsByUrlDomains(newDomain, false);
					if (newTable != null)
					{
						Logger.debug(DocDB.class, "updateInternalCaches: vkladam URL ("+doc.getVirtualPath()+") pre domenu "+newDomain);
						newTable.put(doc.getVirtualPath(), doc.getDocId());
					}

					if (documentPublishEvent != null) {
						documentPublishEvent.setOldVirtualPath(oldVirtualPath);
					}
				}

				if (documentPublishEvent != null) {
					(new WebjetEvent<DocumentPublishEvent>(documentPublishEvent, WebjetEventType.ON_PUBLISH)).publishEvent();
				}

				//aktualizovat v clustri
				ClusterDB.addRefresh("sk.iway.iwcm.doc.DocDB-"+doc.getDocId());

				//aktualizuj si publicable cache
				readPagesToPublic();

				//aktualizuj cache dokumentov
				if (cachedDocs.containsKey(Integer.valueOf(doc.getDocId())))
				{
					DocDetails docRefreshCache = getDoc(doc.getDocId(), -1, false);
					if (docRefreshCache != null)
					{
						Logger.debug(DocDB.class, "refreshing cached doc: "+docRefreshCache.getDocId()+" "+docRefreshCache.getTitle());
						cachedDocs.put(Integer.valueOf(doc.getDocId()), docRefreshCache);
					}
				}

				//aktualizuj Cache
				Cache.getInstance().onDocChange(doc);
				/*spravime update dokumentu v indexe */
				Documents.updateSingleDocument(docId);
			}
			/**
			 * ak sme nic nevytiahli z DB tak to znamena ze dokument bol zmazany a treba ho
			 * zmazat aj z internych tabuliek a cache a indexu
			 */
			else
			{
				cleanupDocReferences(docId);
			}
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
		}
		finally
		{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
	}

	/**
	 * Tato metoda zmaze informacie o doc s docId z internych tabuliek
	 * v pamati, basicAllDocsTable, basicAllDocsInGroupTable, urlsByUrlDomains
	 * basicAllDocsTable, z cache ak sa tam nachadza, zmaze takisto data z lucene indexu
	 * informacie o dokumente vyberie z basicAllDocsTable. ak sa tam doc uz nenachadza tak
	 * metode nic nespravi
	 * @param docId id dokumentu
	 * @author mhalas
	 */
	private void cleanupDocReferences(int docId)
	{
		DocDetails doc;
		doc = basicAllDocsTable.get(docId);
		/* uz je to pravdepodobne vymazane aj z pamati */
		if(doc == null) return;
		removeDocFromBasilAllDocsInGroup(doc);
		TObjectIntHashMap<String> oldTable = DocDB.getInstance().getUrlsByUrlDomains(doc.getGroup().getDomainName(), false);
		if (oldTable != null && doc.getVirtualPath() != null)
		{
			Logger.debug(DocDB.class, "updateInternalCaches: mazem stare URL ("+doc.getVirtualPath()+") pre domenu "+doc.getGroup().getDomainName());
			oldTable.remove(doc.getVirtualPath());
		}
		basicAllDocsTable.remove(docId);
		readPagesToPublic();
		if (cachedDocs.containsKey(Integer.valueOf(docId))) cachedDocs.remove(Integer.valueOf(docId));
		Cache.getInstance().clearAll();
		Cache.getInstance(true);
		ClusterDB.addRefresh(DocDB.class.getName()+"-"+docId);
		Documents.deleteSingleDocument(docId);
	}

	/**
	 * Skontroluje ci dany DocDetails nie je v cache, ak je vrati, ak nie je ziska z DB a prida do cache (pouziva sa primarne z TemplatesDB)
	 * @param docId
	 * @return
	 */
	public DocDetails getDocAndAddToCacheIfNot(int docId)
	{
		DocDetails doc = null;
		if (cachedDocs!=null)
		{
			doc = cachedDocs.get(Integer.valueOf(docId));
			if (doc!=null)
			{
				Logger.debug(DocDB.class, "getDocAndAddToCacheIfNot - returning from cache "+docId);
				return (doc);
			}
		}
		doc = getDoc(docId);
		if (cachedDocs!=null && doc != null)
		{
			Logger.debug(DocDB.class, "getDocAndAddToCacheIfNot - caching "+docId);
			cachedDocs.put(Integer.valueOf(docId), doc);
		}
		return doc;
	}

	/**
	 * novy sposob vyhladavania pouziva povodne prazdny stlpec file_name pre urcenie adresara
	 * v ktorom sa ma hladat (namiesto group_id IN (sialene dlhy zoznam ideciek))
	 * tato metoda stlpec naplni hodnotami
	 * treba volat po zmene adresara (ak sa presunie, alebo nieco podobne)
	 * @param rootGroupId - id adresara, alebo -1 pre aktualizaciu vsetkeho
	 */
	public static void updateFileNameField(int rootGroupId)
	{
		//nacitaj si hodnoty doc_id, group_id do listu

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			List<LabelValueDetails> list = new ArrayList<>();
			int counter = 0;

			db_conn = DBPool.getConnection();

			StringBuilder sql = new StringBuilder("SELECT doc_id, group_id FROM documents WHERE doc_id>0");
			if (rootGroupId > 0) sql.append(StatDB.getRootGroupWhere("group_id", rootGroupId));

			Logger.debug(DocDB.class, "updateFileNameField: sql="+sql);

			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			while (rs.next())
			{
				LabelValueDetails lvd = new LabelValueDetails();
				lvd.setInt1(rs.getInt("doc_id"));
				lvd.setInt2(rs.getInt("group_id"));
				list.add(lvd);

				counter++;
				if (counter % 1000 == 0)
				{
					Logger.debug(DocDB.class, "updateFileNameField: Reading data "+counter);
				}
			}
			rs.close();
			ps.close();


			//poukladaj hodnoty
			GroupsDB groupsDB = GroupsDB.getInstance();
			ps = db_conn.prepareStatement("UPDATE documents SET file_name=? WHERE doc_id=?");

			int total = counter;
			counter = 0;
			for (LabelValueDetails lvd : list)
			{
				GroupDetails group = groupsDB.getGroup(lvd.getInt2());
				String fileName = null;

				if (group != null && group.isInternal()==false)
				{
					fileName = groupsDB.getGroupNamePath(lvd.getInt2());
				}

				ps.setString(1, DB.prepareString(fileName, 255));
				ps.setInt(2, lvd.getInt1());
				ps.execute();

				counter++;
				if (counter % 1000 == 0)
				{
					Logger.debug(DocDB.class, "Updating data "+counter+"/"+total);
				}
			}
			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
		}
		finally
		{
			try
			{
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
				if (db_conn!=null) db_conn.close();
			}
			catch (Exception ex2)
			{
				//
			}
		}

		//update-nem este root_group_l1 - l3
		updateRootGroupLevelValues(rootGroupId);
	}

	public List<PublicableForm> getPublicableDocs(){
		return publicableDocs;
	}

	public void forceRefreshMasterSlaveMappings()
	{
		slavesMasterMappings = null;
		masterMappings = null;
	}

	/**
	 * inicializuje mapu master-slave stranok
	 * @return
	 */
	public Map<Integer, Integer> getSlavesMasterMappings()
	{
		if(slavesMasterMappings == null)
			slavesMasterMappings = MultigroupMappingDB.getAllMappings();

		return slavesMasterMappings;
	}

	/**
	 * inicializuje mapu master-all_master_slaves stranok
	 * @return
	 */
	public Map<Integer, Integer[]> getMasterMappings()
	{
		if(getSlavesMasterMappings() != null && masterMappings == null)
		{
			masterMappings = new HashMap<>();
			for(Entry<Integer, Integer> ms : getSlavesMasterMappings().entrySet())
			{
				Integer[] slavesOfMaster = masterMappings.get(ms.getValue());
				if(slavesOfMaster == null) {
					masterMappings.put(ms.getValue(), new Integer[]{ms.getKey()});
				} else {
					Integer[] slavesOfMasterTmp = Arrays.copyOf(slavesOfMaster, slavesOfMaster.length+1);
					slavesOfMasterTmp[slavesOfMaster.length] = ms.getKey();
					masterMappings.put(ms.getValue(), slavesOfMasterTmp);
				}
			}
			/*
			for(Iterator iterator = masterMappings.entrySet().iterator(); iterator.hasNext();)
			{
				Entry<Integer, Integer[]> mm = (Entry<Integer, Integer[]>)iterator.next();
				System.out.println(">>>> "+mm.getKey()+": "+Arrays.toString(mm.getValue()));
			}
			 */
		}

		return masterMappings;
	}

	/**
	 * Parses docid from url string, if any error or string does not contain docid
	 * if url does not contain docid, uses getVirtualPathDocId to get docId
	 * returns -1
	 * @param url
	 * @return docid or -1
	 */
	public static int parseDocIdFromDmailUrl(String url,HttpServletRequest request){

		int ret = -1;
		String path = url;

		if (!Tools.isEmpty(url))
		{
			url = url.toLowerCase();
			if (url.contains("docid"))
			{
				url = url.split("docid=")[1];
				url = url.split("&")[0];
				Pattern p = Pattern.compile("\\d+");
				Matcher m = p.matcher(url);
				if (m.find())
				{
					ret = Integer.parseInt(Tools.getStringValue(m.group(), "-1"));
				}
			}
			else if (ret == -1)
			{
				String domain = DocDB.getDomain(request);
				URL durl;
				try
				{
					durl = new URL(path);
					path = durl.getPath();
				}
				catch (MalformedURLException e)
				{
					//ak to nie je url tak to je path a rovno to dame metode
					//getVirtualPathDocId
				}
				ret = DocDB.getInstance().getVirtualPathDocId(path, domain);
			}
		}

		return ret;
	}

	/**
	 * novy sposob vyhladavania pouziva povodne prazdne stlpce root_group_l1, root_group_l2, root_group_l3
	 * co obsahuju pre kazdy adresar (hodnota group_id) ID adresarov na prvej, druhej a tretej urovni,
	 * co urychli vyhladavanie news v "getDocPerex" pre adresare na prvych troch urovnich (namiesto group_id IN (sialene dlhy zoznam ideciek))
	 *
	 * tato metoda stlpce root_group_l1, root_group_l2, root_group_l3 naplni hodnotami
	 * treba volat po zmene adresara (ak sa presunie, alebo nieco podobne)
	 * @param rootGroupId - id adresara, alebo -1 pre aktualizaciu vsetkeho
	 */
	public static void updateRootGroupLevelValues(int rootGroupId)
	{
		if (rootGroupId < 1 && InitServlet.isTypeCloud())
		{
            rootGroupId = CloudToolsForCore.getDomainId();
		}
		if (InitServlet.isTypeCloud())
		{
			GroupDetails grp = GroupsDB.getInstance().getGroup(rootGroupId);
			if (grp == null || grp.getDomainName().equals(CloudToolsForCore.getDomainName())==false) return;
		}

		//nacitaj si hodnoty doc_id, group_id do listu
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			List<LabelValueDetails> list = new ArrayList<>();
			int counter = 0;

			db_conn = DBPool.getConnection();

			StringBuilder sql = new StringBuilder("SELECT doc_id, group_id FROM documents WHERE doc_id>0");
			if (rootGroupId > 0) sql.append(StatDB.getRootGroupWhere("group_id", rootGroupId));

			Logger.debug(DocDB.class, "updateRootGroupLevelValues: sql="+sql);

			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			while (rs.next())
			{
				LabelValueDetails lvd = new LabelValueDetails();
				lvd.setInt1(rs.getInt("doc_id"));
				lvd.setInt2(rs.getInt("group_id"));
				list.add(lvd);

				counter++;
				if (counter % 1000 == 0)
				{
					Logger.debug(DocDB.class, "updateRootGroupLevelValues: Reading data "+counter);
				}
			}
			rs.close();
			ps.close();


			//poukladaj hodnoty
			ps = db_conn.prepareStatement("UPDATE documents SET root_group_l1=?, root_group_l2=?, root_group_l3=? WHERE doc_id=?");

			int total = counter;
			counter = 0;
			for (LabelValueDetails lvd : list)
			{
				DocDB.getRootGroupL(lvd.getInt2(), ps, 1);

				ps.setInt(4, lvd.getInt1());
				ps.execute();

				counter++;
				if (counter % 1000 == 0)
				{
					Logger.debug(DocDB.class, "Updating data "+counter+"/"+total);
				}
			}
			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			Logger.error(DocDB.class, ex);
		}
		finally
		{
			try
			{
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
				if (db_conn!=null) db_conn.close();
			}
			catch (Exception ex2)
			{
				//
			}
		}
	}

	/**
	 * vrati hodnoty pre root_group_l1 - root_group_l3 a pripadne aj nastavi do PreparedStatement
	 * @param groupId
	 * @param ps
	 * @param psInd
	 * @return
	 */
	public static int[] getRootGroupL(int groupId, PreparedStatement ps, int psInd)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> parentGroups = groupsDB.getParentGroups(groupId);
		int[] root_group_l = new int[3];
		Arrays.fill(root_group_l, 0);
		int ind = 0;
		for(int i=parentGroups.size()-1; i >= 0; i--)
		{
			root_group_l[ind++] = parentGroups.get(i).getGroupId();
			if(ind == 3) break;
		}

		if(ps != null)
		{
			try
			{
				if(root_group_l[0] > 0) ps.setInt(psInd++, root_group_l[0]);
				else ps.setObject(psInd++, null);
				if(root_group_l[1] > 0) ps.setInt(psInd++, root_group_l[1]);
				else ps.setObject(psInd++, null);
				if(root_group_l[2] > 0) ps.setInt(psInd++, root_group_l[2]);
				else ps.setObject(psInd++, null);
			}
			catch (SQLException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		return root_group_l;
	}

	/**
	 * aktualizuj hodnoty v perex_group_doc
	 * @param docId
	 * @param perexGroups
	 */
	public static void udpdatePerexGroupDoc(int docId, String perexGroups)
	{
		boolean perexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin");
		if(perexGroupUseJoin)
		{
			//skontrolujem aj MultigroupMapping
			int masterId = MultigroupMappingDB.getMasterDocId(docId);
			List<MultigroupMapping> slavesId = MultigroupMappingDB.getSlaveMappings(masterId > 0 ? masterId : docId);
			List<Integer> docIdList = new ArrayList<>();
			if(masterId > 0)
				docIdList.add(Integer.valueOf(masterId));
			if(slavesId != null && slavesId.size() > 0)
				for(MultigroupMapping mm : slavesId)
					docIdList.add(Integer.valueOf(mm.getDocId()));
			if(masterId < 1 || docIdList.size() < 1)
				docIdList.add(Integer.valueOf(docId));

			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();
				String[] pgArray = Tools.getTokens(perexGroups, ",");

				for(Integer id : docIdList)
				{
					Logger.println(DocDB.class, "udpdatePerexGroupDoc - doc_id = "+id+"; perexGroups = "+perexGroups);

					ps = db_conn.prepareStatement("DELETE FROM perex_group_doc WHERE doc_id = ?");
					ps.setInt(1, id);
					ps.execute();
					ps.close();
					ps = null;

					for(String pg : pgArray)
					{
						if(Tools.getIntValue(pg, -1) > 0)
						{
							ps = db_conn.prepareStatement("INSERT INTO perex_group_doc (doc_id, perex_group_id) VALUES (?,?)");
							ps.setInt(1, id);
							ps.setInt(2, Tools.getIntValue(pg, 0));
							ps.execute();
							ps.close();
							ps = null;
						}
					}
				}

				db_conn.close();
				db_conn = null;
			}
			catch (Exception ex)
			{
				Logger.error(DocDB.class, ex);
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
					//
				}
			}
		}
	}

	/**
	 * Prenesene z GroupsListAction koli kontrole na cloud WJ
	 * @param user
	 * @return
	 */
	public static List<DocDetails> getMyPages(Identity user)
	{
		//ziskaj Moje Dokumenty
		List<DocDetails> myPages = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(user.getEditablePages(), ",");
		int id;
		DocDetails page;
		DocDB docDB = DocDB.getInstance();
		GroupsDB groupsDB = GroupsDB.getInstance();
		while (st.hasMoreTokens())
		{
			id = Integer.parseInt(st.nextToken());
			page = docDB.getDoc(id);
			if (page != null)
			{
				//skontroluj, ci uz nemam pristup k adresaru (aby nebola duplicita)
				if ((","+user.getEditableGroups()+",").indexOf(","+page.getGroupId()+",")==-1)
				{
					page.setNavbar(groupsDB.getNavbarNoHref(page.getGroupId()));
					myPages.add(page);
				}
			}
		}
		return myPages;
	}

	/**
	 * Vrati zoznam naposledy editovanych stranok podla tabulky documents_history. Vynecha stranky ktorych virtual_path zacina na /files/ co su zaindexovane subory
	 * Prenesene z GroupsListAction koli kontrole na cloud WJ
	 * @deprecated - pouzite AdminTools.get...
	 * @param userId
	 * @param maxSize
	 * @return
	 */
	@Deprecated
	public static List<DocDetails> getMyRecentPages(int userId, int maxSize)
	{
		return(AdminTools.getMyRecentPages(userId,maxSize));
	}

	/**
	 * Zmeni nazov grupy. Grupa musi mat defaultne DocID > 0 a nesmie to byt v System adresari / internom / s vypnutym zobrazenim
	 * @param groupId - id grupy ktorej nazov ideme zmenit
	 * @param newTitle - novy nazov grupy
	 * @return - true ak vsetko prebehlo v poriadku
	 */
	public static boolean changeGroupTitle(int groupId, int docId, String newTitle)
	{
		return changeGroupTitle(groupId, docId, newTitle, false);
	}

	/**
	 * Zmeni nazov grupy. Grupa musi mat defaultne DocID > 0 a nesmie to byt v System adresari
	 * @param groupId - id grupy ktorej nazov ideme zmenit
	 * @param docId - id web stranky ktorej zmena nastala
	 * @param newTitle - novy nazov grupy
	 * @param forceUpdate - ak je true, zmeni sa aj nazov interneho/nezobrazovaneho priecinka
	 * @return
	 */
	public static boolean changeGroupTitle(int groupId, int docId, String newTitle, boolean forceUpdate)
	{
		GroupDetails grDetails =  GroupsDB.getInstance().getGroup(groupId);
		if(grDetails != null && grDetails.getDefaultDocId() == docId && newTitle != null && !newTitle.equals(grDetails.getGroupName()) &&
				(forceUpdate || (grDetails.isInternal()==false && grDetails.getMenuType()!=GroupDetails.MENU_TYPE_HIDDEN &&  grDetails.getFullPath()!=null))
				&& grDetails.getFullPath().indexOf("/System")==-1 && grDetails.getParentGroupId()>0)
		{
			Logger.debug(DocDB.class, "Renaming group: "+groupId+" to name :"+newTitle);
			grDetails.setGroupName(newTitle);
			grDetails.setNavbar(newTitle);
			if (grDetails.getUrlDirName().startsWith("/")==false)
			{
				String urlDirName = sk.iway.iwcm.DB.internationalToEnglish(newTitle).toLowerCase();
				urlDirName = DocTools.removeCharsDir(urlDirName, true).toLowerCase();
				grDetails.setUrlDirName(urlDirName);
			}
			GroupsDB.getInstance().setGroup(grDetails);
			return true;
		}
		return false;
	}


	protected void changeUrlInUrlmap(String oldUrl, String newUrl)
	{
		if (urlsByUrlDomains.containsKey(oldUrl))
		{
			TObjectIntHashMap<String> urlsForDomain = urlsByUrlDomains.get(oldUrl);
			urlsByUrlDomains.remove(oldUrl);
			urlsByUrlDomains.put(newUrl, urlsForDomain);
		}

	}

	/**
	 * Zmaze zaznam o dokumente z prislusnej grupy
	 * v basicAllDocsInGroupTable
	 * @param doc
	 * @author mhalas
	 */
	private void removeDocFromBasilAllDocsInGroup(DocDetails doc)
	{
		List<DocDetails> list = DocDB.getInstance().basicAllDocsInGroupTable.get(doc.getGroupId());
		if (list != null)
		{
			Iterator<DocDetails> iter = list.iterator();
			while (iter.hasNext())
			{
				if (iter.next().getDocId()==doc.getDocId())
				{
					Logger.debug(DocDB.class, "updateInternalCaches: mazem stary zaznam v basilAllDocsInGroupTable "+doc.getGroupId());
					iter.remove();
					break;
				}
			}
		}
	}

	/**
	 * @deprecated - pouzite AdminTools.get...
	 */
	@Deprecated
	public static List<DocDetails> getRecentPages(int size){
		return AdminTools.getRecentPages(size);
	}

	/**
	 * @deprecated - pouzite AdminTools.get...
	 */
	@Deprecated
	public static List<DocDetails> getRecentPages(int size, Identity user)
	{
		return AdminTools.getRecentPages(size,user);
	}

	public DocDetails getDocByField(String fieldName, String fieldValue)
	{
		return getDocByField(fieldName, fieldValue, false);
	}

	public DocDetails getDocByField(String fieldName, String fieldValue, boolean noData)
	{
		String sql = "";

		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			DocDetails doc = null;

			String select = DocDB.getDocumentFields(noData==false);
			if (Constants.getBoolean("docAuthorLazyLoad")) {
				sql = "SELECT " + select + " FROM documents d WHERE field_" + fieldName + "= ?";
			} else {
				sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, " + select + " FROM documents d LEFT JOIN users u ON d.author_id=u.user_id WHERE d.field_" + fieldName + "= ?";
			}

			try {
				connection = DBPool.getConnectionReadUncommited();

				try {
					ps = connection.prepareStatement(sql);
					ps.setString(1, fieldValue);

					try {
						rs = ps.executeQuery();

						while (rs.next())
						{
							if (Constants.getBoolean("docAuthorLazyLoad")) {
								doc = getDocDetails(rs, noData, true);
							}
							else {
								doc = getDocDetails(rs, noData);
							}
						}
					}
					finally {
						ps.close();
					}
				}
				finally {
					if (rs!=null) rs.close();
				}
			}
			finally {
				if (connection!=null) connection.close();
			}

			return doc;
		}
		catch (Exception e)
		{
			Logger.error(DocDB.class, e);
		}

		return (null);
	}

	/**
	 * Vrati zoznam podadresarov oddelenych ciarkou
	 * @param groupId - id korenoveho adresara
	 * @return String - zoznam podadresarov oddelenych ciarkou
	 */

	public static String getSubgroups(int groupId){
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> searchGroupsArray;
		StringBuilder searchGroups = null;

			searchGroupsArray = groupsDB.getGroupsTree(groupId, true, true);
			for (GroupDetails group : searchGroupsArray)
			{
				if (group != null)
				{
					if (searchGroups == null)
					{
						searchGroups = new StringBuilder(String.valueOf(group.getGroupId()));
					}
					else
					{
						searchGroups.append(',').append(group.getGroupId());
					}
				}
			}

		if(searchGroups!=null){
			return  searchGroups.toString();
		}
		else{
			return "";
		}

	}

	/**
	 * Vrati zoznam produktov ktore maju zadanu cenu, pouziva sa v eshope v admin_pricelist.jsp
	 * @param showOnlyAvailable
	 * @return
	 */
	public List<DocDetails> getItemsWithPrice(boolean showOnlyAvailable)
	{
		List<DocDetails> products = new ArrayList<>();

		Connection db_conn=null;

		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			String priceColumnName = Constants.getString("basketPriceField");
			if (priceColumnName == null)
				ps = db_conn.prepareStatement("SELECT doc_id FROM documents");
			else
			{
				//premena z fieldJ na field_j, ak je nutna
				priceColumnName = priceColumnName.toLowerCase().startsWith("field") ?
							"field_"+priceColumnName.toLowerCase().substring(priceColumnName.length() - 1) : priceColumnName;

				StringBuilder sql = new StringBuilder("SELECT doc_id FROM documents WHERE");
				if (Constants.DB_TYPE == Constants.DB_MSSQL) sql.append(" LEN(");
				else sql.append(" LENGTH(");
				sql.append(priceColumnName).append(") > 0");
				ps = db_conn.prepareStatement(sql.toString());
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				DocDetails document = getDoc(rs.getInt(1));
				if (document == null)
					continue;
				double price = document.getPrice();
				//ak nema cenu, tak nas nezaujima, nejde o produkt
				if (Math.abs(price) < 1e-7 || (showOnlyAvailable && !document.isAvailable() ) )
					continue;
				products.add(document);
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
			Logger.error(DocDB.class, ex);
		}
		finally
		{
			try
			{
				if (rs!=null)
					rs.close();
				if (ps!=null)
					ps.close();
				if (db_conn!=null)
					db_conn.close();
			}
			catch (Exception ex2){
				//
			}
		}

		return products;
	}


	private static String notNull(String text)
	{
		if (text == null) return "";
		return text;
	}

	/**
	 * Otestuje stav fora pre stranku
	 * return: 0=nema forum, 1=forum aktivne, -1=forum neaktivne
	 */
	private static int getForumStatus(DocDetails docDet, TemplateDetails temp)
	{
		if (docDet == null || docDet.getData() == null)
			return(0);

		if (temp == null)
			return(0);

		if(ForumGroupService.isMessageBoard(docDet, temp)) {
			if(ForumGroupService.isActive(docDet.getDocId()))
				return(1);
			else
				return(-1);
		}

		return(0);
	}

	/**
	 * Vrati DocDetails ako json object, pouziva sa v ajax_docstable.jsp pre webstranky a novinky
	 * @param d
	 * @param group
	 * @param request
	 * @return
	 */
	public static JSONObject getJsonObject(DocDetails d, GroupDetails group, HttpServletRequest request) {
		JSONObject j = new JSONObject();
		try {
			String tempName = "";

			TemplateDetails temp = TemplatesDB.getInstance().getTemplate(d.getTempId());
			if (temp != null) tempName = temp.getTempName();

			boolean showOnHOmepage = false;
			String perexGroups = d.getPerexGroupString();
			String showOnHomepagePerex = Constants.getString("showOnHomepagePerexGroup");

			String category = "";
			String subCategory = "";

			String newsListConstantName = Tools.getStringValue(request.getParameter("newsListConstantName"),"");
			if(Tools.isNotEmpty(newsListConstantName)){

				String constGroups = Tools.replace(Constants.getString(newsListConstantName), "\\*", "");
				List<String> groupIdsList = Arrays.asList(constGroups.split(","));

				if(groupIdsList.contains(d.getGroupId()+"")){
					// ake je parent v liste kategorii => nieje v podkategorii
					category = group.getGroupName() ;
				}else{
					subCategory = group.getGroupName();
					category = GroupDetails.getById(group.getParentGroupId()).getGroupName();

				}
			}

			if( Tools.isNotEmpty("showOnHomepagePerex") && perexGroups!=null && perexGroups.indexOf(showOnHomepagePerex)>-1){
				showOnHOmepage = true;
			}

			j.put("docId", String.valueOf(d.getDocId()));
            j.put("historyId", String.valueOf(d.getHistoryId()));

			//stranky na schvalenie
			if (group.getGroupId()==Constants.getInt("systemPagesDocsToApprove")) j.put("groupId", Constants.getInt("systemPagesDocsToApprove"));
			else j.put("groupId", String.valueOf(d.getGroupId()));

			j.put("sortPriority", d.getSortPriority());
			j.put("title", ResponseUtils.filter(d.getTitle()));
			j.put("authorName", ResponseUtils.filter(notNull(d.getAuthorName())));
			j.put("templateName", ResponseUtils.filter(tempName));
			j.put("templateId", d.getTempId());
			j.put("fieldA", ResponseUtils.filter(d.getFieldA()));
			j.put("fieldB", ResponseUtils.filter(d.getFieldB()));
			j.put("fieldC", ResponseUtils.filter(d.getFieldC()));
			j.put("fieldD", ResponseUtils.filter(d.getFieldD()));
			j.put("fieldE", ResponseUtils.filter(d.getFieldE()));
			j.put("fieldF", ResponseUtils.filter(d.getFieldF()));
			j.put("fieldG", ResponseUtils.filter(d.getFieldG()));
			j.put("fieldH", ResponseUtils.filter(d.getFieldH()));
			j.put("fieldI", ResponseUtils.filter(d.getFieldI()));
			j.put("fieldJ", ResponseUtils.filter(d.getFieldJ()));
			j.put("fieldK", ResponseUtils.filter(d.getFieldK()));
			j.put("fieldL", ResponseUtils.filter(d.getFieldL()));
			j.put("fieldM", ResponseUtils.filter(d.getFieldM()));
			j.put("fieldN", ResponseUtils.filter(d.getFieldN()));
			j.put("fieldO", ResponseUtils.filter(d.getFieldO()));
			j.put("fieldP", ResponseUtils.filter(d.getFieldP()));
			j.put("fieldQ", ResponseUtils.filter(d.getFieldQ()));
			j.put("fieldR", ResponseUtils.filter(d.getFieldR()));
			j.put("fieldS", ResponseUtils.filter(d.getFieldS()));
			j.put("fieldT", ResponseUtils.filter(d.getFieldT()));

			if(group.getLng()!=null){
				j.put("lang", group.getLng());
			}else if (temp!=null) {
				j.put("lang", temp.getLng());
			}
			else
			{
				j.put("lang", Constants.getString("defaultLanguage"));
			}

			j.put("publicStart", d.getPublishStartString());
			j.put("publicStartTime", d.getPublishStartTimeString());
			j.put("publicStartWithTime", d.getPublishStartString()+" "+d.getPublishStartTimeString() );
			j.put("publicStartTimestamp", String.valueOf(d.getPublishStart()));

			j.put("publicEnd", d.getPublishEndString());
			j.put("publicEndTime", d.getPublishEndTimeString());
			j.put("publicEndWithTime", d.getPublishEndString() +" "+d.getPublishEndTimeString() );
			j.put("publicEndTimestamp", String.valueOf(d.getPublishEnd()));

			j.put("publicEvent", d.getEventDateString());
			j.put("publicEventTime", d.getEventTimeString());
			j.put("publicEventWithTime", d.getEventDateString() +" "+d.getEventTimeString() );
			j.put("publicEventTimestamp", String.valueOf(d.getEventDate()));

			j.put("dateCreated", Tools.formatDateTime(d.getDateCreated()));
			j.put("dateCreatedTimestamp", String.valueOf(d.getDateCreated()));
			j.put("empty", "");
			j.put("domain", group.getDomainName());

			JSONObject properties = new JSONObject();
			properties.put("isAvailable", d.isAvailable());
			properties.put("isCacheable", d.isCacheable());
			properties.put("isSearchable", d.isSearchable());
			properties.put("isShowInMenu", d.isShowInMenu());
			properties.put("isRequireSsl", d.isRequireSsl());
			properties.put("isForum", String.valueOf(getForumStatus(d, temp)));
			properties.put("isShowOnHomepage", showOnHOmepage);

			j.put("properties", properties);

			j.put("link", d.getDocLink());
			j.put("groupName", ResponseUtils.filter(group.getGroupName()));
			j.put("category", ResponseUtils.filter(category));
			j.put("subcategory", ResponseUtils.filter(subCategory));

			try {
				j.put("externalLink", notNull(d.getExternalLink()));
			}
			catch (Exception e) {
				j.put("externalLink", "");
			}

			j.put("perex", ResponseUtils.filter(d.getPerex()));
			j.put("perexPlace", ResponseUtils.filter(d.getPerexPlace()));
			j.put("perexImage", ResponseUtils.filter(d.getPerexImage()));

			try
			{
				String[] perexGroupNames = d.getPerexGroupNames();
				if (perexGroupNames != null && perexGroupNames.length>1)
				{
					j.put("perexGroup", perexGroupNames);
				}
				else
				{
					j.put("perexGroup", "");
				}
			}
			catch (Exception e)
			{
				Logger.error(DocDB.class, e);
				j.put("perexGroup", "");
			}

			List<String> passwordProtected = new ArrayList<>();
			if (d.getPasswordProtected() != null) {
				for (String idString : Tools.getTokens(d.getPasswordProtected(), ",")) {
					int id = Integer.parseInt(idString);
					UserGroupDetails userGroup = UserGroupsDB.getInstance().getUserGroup(id);

					if (userGroup != null) {
						passwordProtected.add(userGroup.getUserGroupName());
					}
				}
			}

			j.put("perexPasswordProtected", passwordProtected);

			boolean isDefaultDoc = false;
			if (group != null && group.getDefaultDocId()==d.getDocId()) isDefaultDoc = true;

			j.put("defaultDoc", isDefaultDoc);
		}
		catch (Exception e) {
			Logger.error(DocDB.class, e);
		}

		return j;
	}

	 /**
	  * vsetkym strankam z adresara na vstupe opatovne vygeneruje Url (virtualPath)
	  * zaroven rekurzivne prechadza aj jeho podadresare
	  *
	  * @param rootGroupId
	  * @param user
	  * @param request
	  */
	 public void regenerateUrl(int rootGroupId, Identity user, HttpServletRequest request)
	 {
		  //ziskaj zoznam stranok v adresari
		  List<DocDetails> docs = DocDB.getInstance().getBasicDocDetailsByGroup(rootGroupId, -1);
		  Iterator<DocDetails> iter = docs.iterator();
		  DocDetails doc;
		  EditorForm ef;
		  while (iter.hasNext())
		  {
				doc = iter.next();
				ef = EditorDB.getEditorForm(request, doc.getDocId(), -1, rootGroupId);

				Logger.println(this, ef.getTitle() + " [docid: " + ef.getDocId() + "] - virtualPath: " + ef.getVirtualPath());

				if (ef.getVirtualPath().contains("*"))
				{
					 Logger.println(this," skipping (contains *)");
					 continue;
				}

				ef.setVirtualPath("");
				//nastav aktualneho usera
				ef.setAuthorId(user.getUserId());
				ef.setPublish("1");

				EditorDB.saveEditorForm(ef, request);

				Logger.println(this," -> " + ef.getVirtualPath());
		  }

		  // rekurzivne sa zavolaj na podadresare
		  List<GroupDetails> subGroups = GroupsDB.getInstance().getGroups(rootGroupId);
		  Iterator<GroupDetails> iterGroup = subGroups.iterator();
		  GroupDetails group;
		  while (iterGroup.hasNext())
		  {
				group = iterGroup.next();
				regenerateUrl(group.getGroupId(), user, request);
		  }
	}


	/**
	 * Upravi poradie stranok v adresari po zmene stranky cez drag&drop, vsetky za touto strankou precisluje
	 * @param editorForm
	 * @param position
	 */
	public void fixWebpageSortOrder(EditorForm editorForm, int position) {
		// ziskaj pocet adresarov v tomto adresari, tie musime odpocitat
		List<GroupDetails> folders = GroupsDB.getInstance().getGroups(editorForm.getGroupId());
		position = position - folders.size();
		if (position < 0)
			position = 0;

		// iterujme stranky a upravime poradie
		DocDB docDB = DocDB.getInstance();
		List<DocDetails> docs = docDB.getBasicDocDetailsByGroup(editorForm.getGroupId(), DocDB.ORDER_PRIORITY);

		// v adresari nic nie je, nemusime menit sort priority
		if (docs.size() == 0)
			return;

		StringBuilder updateSortOrderList = null;
		int maxSortPriority = 0;
		for (DocDetails doc : docs) {
			maxSortPriority = doc.getSortPriority();

			if (doc.getDocId() == editorForm.getDocId()) {
				// netreba nic menit
				Logger.debug(DocDB.class, "fixWebpageSortOrder, ZHODA DOCID, position=" + position);
				if (position == 0)
					return;
				continue;
			}

			position--;

			Logger.debug(DocDB.class, "fixWebpageSortOrder, docid=" + doc.getTitle() + " " + doc.getDocId() + " position=" + position);

			if (position == -1) {
				Logger.debug(DocDB.class, "fixWebpageSortOrder, position==-1, setting priority: " + doc.getSortPriority());
				editorForm.setSortPriority(doc.getSortPriority());
			}
			if (position < 0) {
				if (updateSortOrderList == null)
					updateSortOrderList = new StringBuilder(String.valueOf(doc.getDocId()));
				else
					updateSortOrderList.append(",").append(String.valueOf(doc.getDocId()));
			}

			Logger.debug(DocDB.class, "fixWebpageSortOrder, toUpdate=" + updateSortOrderList);
		}

		if (updateSortOrderList == null) {
			Logger.debug(DocDB.class, "updateSortOrderList, updatujem self, maxSortPriority=" + maxSortPriority);
			editorForm.setSortPriority(maxSortPriority + 10);
		} else {

			// update cache hodnoty
			int[] docids = Tools.getTokensInt(updateSortOrderList.toString(), ",");
			for (int docid : docids) {
				DocDetails doc = docDB.getDoc(docid);
				doc.setSortPriority(doc.getSortPriority()+10);
				DocDB.saveDoc(doc, true);
			}
		}
	}

	/**
	 * Vrati/vytvori stranku podla zadanej cesty, pouziva sa napr. pri importe
	 * @param path - cesta vo formate /adresar1/adresar2/meno-stranky
	 * @return
	 */
	public DocDetails getCreateDoc(String path) {

		//check if path is number, then return doc with this id
		try {
			int id = Integer.parseInt(path);
			return DocDB.getInstance().getDoc(id);
		} catch (Exception e) {
			//not a number, continue
		}

		int domainSeparatorIndex = path.indexOf(":");
		if (domainSeparatorIndex>0) {
			String domain = path.substring(0, domainSeparatorIndex);
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (rb != null) {
				rb.setDomain(domain);
			}
			path = path.substring(domainSeparatorIndex+1);
		}

		int lomka = path.lastIndexOf("/");
		if (lomka == -1 || path.endsWith("/")) return null;

		String groupPath = path.substring(0, lomka);
		String pageName = path.substring(lomka+1);

		GroupDetails group = GroupsDB.getInstance().getCreateGroup(groupPath);
		if (group == null) return null;

		DocDetails doc = getDocByTitle(pageName, group.getGroupId());
		if (doc != null) return doc;

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null) return null;

		doc = new DocDetails();
		doc.setGroupId(group.getGroupId());
		doc.setTempId(group.getTempId());
		doc.setAuthorId(rb.getUserId());
		doc.setSortPriority(getMaxSortPriorityInGroup(group.getGroupId())+10);

		doc.setTitle(pageName);
		doc.setNavbar(pageName);
		doc.setData("");

		saveDoc(doc);
		updateInternalCaches(doc.getDocId());

		return doc;
	}

	/**
	 * Vrati zoznam SQL stlpcov pre vyber z databazy
	 * @return
	 */
	public static String getDocumentFields() {
		return getDocumentFields(true);
	}

	/**
	 * Vrati zoznam SQL stlpcov pre vyber z databazy bez data stlpca (mensi objem dat)
	 * @return
	 */
	public static String getDocumentFieldsNodata() {
		return getDocumentFields(false);
	}

	/**
	 * Vrati zoznam SQL stlpcov pre vyber z databazy
	 * @param includeDataField - ak je true, bude sa vracat aj stlpec data (je narocnejsie na pamat pri citani dat)
	 * @return
	 */
	public static String getDocumentFields(boolean includeDataField) {
		StringBuilder sqlFields = new StringBuilder();

		sqlFields.append("d.doc_id, ");
		if (includeDataField) sqlFields.append("d.data, ");
		sqlFields.append("d.date_created, d.publish_start, d.publish_end, d.author_id, d.searchable, d.group_id, d.available, d.show_in_menu, d.password_protected, d.cacheable, d.external_link, d.virtual_path, d. temp_id, d.title, d.navbar, d.file_name, d.sort_priority, d.header_doc_id, d.footer_doc_id, d.menu_doc_id, d.right_menu_doc_id, d.html_head, d.html_data, d.perex_place, d.perex_image, d.perex_group, d.event_date, d.sync_id, d.sync_status, d.field_a, d.field_b, d.field_c, d.field_d, d.field_e, d.field_f, d.field_g, d.field_h, d.field_i, d.field_j, d.field_k, d.field_l, d.disable_after_end, d.forum_count, d.views_total, d.field_m, d.field_n, d.field_o, d.field_p, d.field_q, d.field_r, d.field_s, d.field_t, d.require_ssl");

		String[] additionalFields = DataAccessHelper.getDocFields();
		if (additionalFields!=null && additionalFields.length>0) {
			for (String field : additionalFields) {
				sqlFields.append(", d.").append(field);
			}
		}

		return sqlFields.toString();
	}

}
