package sk.iway.iwcm.editor;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.AtrBean;
import sk.iway.iwcm.doc.AtrDB;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.HistoryDB;
import sk.iway.iwcm.doc.MultigroupMapping;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.XmlExport;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.UrlRedirectDB;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/**
 *  EditorDB.java - praca s EditorForm
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.40 $
 *@created      Date: 7.8.2006 14:21:53
 *@modified     $Date: 2010/02/09 08:44:18 $
 */
public class EditorDB
{
	public static String RENDER_DATA_SEPARATOR = "\n\n-----------------------------------------------------------------\n\n"; //NOSONAR

	protected EditorDB() {
		//utility class
	}

	/**
	 * Ziska editor form pre zadany adresar a zadany nazov stranky (ak uz existuje predvyplni sa)
	 * @param request
	 * @param title
	 * @param group_id
	 * @return
	 */
	public static EditorForm getEditorForm(HttpServletRequest request, String title, int group_id)
	{
		List<DocDetails> docs = DocDB.getInstance().getBasicDocDetailsByGroup(group_id, DocDB.ORDER_ID);
		int docId = -1;
		for (DocDetails doc : docs)
		{
			if (doc.getTitle().equalsIgnoreCase(title))
			{
				docId = doc.getDocId();
				break;
			}
		}

		EditorForm ef = getEditorForm(request, docId, -1, group_id);
		if (docId < 1)
		{
			ef.setTitle(title);
			ef.setNavbar(title);
		}
		return ef;
	}

	/**
	 * Ziska editor form pre zadany adresar a so zadanym docId / historyId
	 * @param request
	 * @param doc_id docId alebo -1
	 * @param historyId historyId alebo -1
	 * @param group_id - id adresara
	 * @return
	 */
	public static EditorForm getEditorForm(HttpServletRequest request, int doc_id, int historyId, int group_id)
	{
		Prop prop = Prop.getInstance(Constants.getServletContext(), request);

		DocDB docDB = DocDB.getInstance();
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails parentGroup = groupsDB.getGroup(group_id);

		int docIdOriginal = doc_id;
		DocDetails doc;
		//moznost nacitat slave data
		int masterId = (request.getAttribute("keepSlave") != null && "true".equals(request.getAttribute("keepSlave"))) ? 0 : MultigroupMappingDB.getMasterDocId(doc_id);
		int lastDoc_id = doc_id;
		if(masterId>0)
		{
			doc_id = masterId;
		}

		GroupDetails group = groupsDB.getGroup(group_id);
		if (group!=null && doc_id == -1 && group.getNewPageDocIdTemplate()>0)
		{
			//je potrebne pouzit sablonu namiesto -1
			doc_id = -group.getNewPageDocIdTemplate();
		}

		//sablona nemoze byt -1 (to je blank page)
		if (doc_id < 0 && historyId < 0)
		{
			//ak je to admin
			doc = null;

			try
			{
				//-1 je cisty dokument
				if (doc_id < -1)
				{
					doc = docDB.getDoc(-doc_id, -1, false);
					doc.setDocId(-1);
				}
				doc_id = -1;
			}
			catch (Exception ex)
			{

			}

			if (doc == null)
			{
				doc = new DocDetails();
				doc.setDocId(-1);
				doc.setGroupId(group_id);
				doc.setData("<p>&nbsp;</p>");

				doc.setTitle(prop.getText("editor.newDocumentName"));

				doc.setSearchable(true);
				doc.setAvailable(Constants.getBoolean("editorNewDocDefaultAvailableChecked"));
				doc.setShowInMenu(true);

				doc.setSortPriority(10);

				doc.setNavbar("");
			}
			else
			{
				//nastav grupu na aktualne vybratu
				doc.setDocId(-1);
				doc.setGroupId(group_id);
				doc.setTitle(prop.getText("editor.newDocumentName"));
				doc.setNavbar("");
				doc.setVirtualPath("");
				doc.setExternalLink("");

				doc.setEventDateString("");
				doc.setEventTimeString("");

				if (Constants.getBoolean("editorNewDocDefaultAvailableChecked")==false) doc.setAvailable(false);
			}

			/*List temps = tempDB.getTemplates();
			Iterator iter = temps.iterator();
			if (iter.hasNext())
			{
				temp = (TemplateDetails) iter.next();
				if (temp != null)
				{
					if (
					doc.setTempId(temp.getTempId());
					doc.setTempName(temp.getTempName());
				}
			}*/
			if (group!=null)
			{
				doc.setTempId(group.getTempId());
			}

			//zisti maximalnu prioritu a zvys o 10
			int sortPriorityIncrementDoc = Constants.getInt("sortPriorityIncrementDoc");

			doc.setSortPriority(0);
			boolean dirIsEmpty = true;
			int maxP = DocDB.getMaxSortPriorityInGroup(group_id);
			if(maxP>0) {
				doc.setSortPriority(maxP);
				dirIsEmpty = false;
			}

			if (Constants.getBoolean("sortPriorityIncremental") && parentGroup!=null)
			{
				if (maxP < parentGroup.getSortPriority())
				{
					//-10 lebo sa nam to o par riadkov nizsie navysi o 10
					maxP = parentGroup.getSortPriority() - sortPriorityIncrementDoc;
					doc.setSortPriority(maxP);
				}
			}

			if (dirIsEmpty && group!=null)
			{
				doc.setTitle(group.getGroupName());
				doc.setNavbar(group.getNavbarName());
			}

			doc.setSortPriority(doc.getSortPriority()+sortPriorityIncrementDoc);
		}
		else
		{
			if (historyId > 0)
			{
				doc = docDB.getDoc(-1, historyId, false);
			}
			else
			{
				doc = docDB.getDoc(doc_id, -1, false);
				//ziskaj zoznam dokumentov v history
				HistoryDB historyDB = new HistoryDB(DBPool.getDBName(request));
				List<DocDetails> history = historyDB.getHistory(doc_id, false, true);
				if (history != null && history.size() > 0)
				{
					request.getSession().setAttribute("docHistory", history);
				}
				if(history == null || history.isEmpty()) {
					request.getSession().removeAttribute("docHistory");
				}
			}
			if (doc == null)
			{
				request.setAttribute("err_msg", "Pozadovana stranka neexistuje 2");
				return(null);
			}
			else
			{
				//ak nacitavam slave clanok a chcem zachovat sort priority, tak NEnacitavam sort priority mastra
				boolean multiGroupkeepSortPriority = Constants.getBoolean("multiGroupKeepSortPriority");
				if(multiGroupkeepSortPriority && masterId > 0)
				{
					DocDetails slave = docDB.getBasicDocDetails(docIdOriginal, false);
					if(slave != null) doc.setSortPriority(slave.getSortPriority());
				}
			}
		}

		EditorForm editorForm;
		editorForm = (EditorForm)request.getAttribute("editorForm");
		if (editorForm == null)
		{
			editorForm = new EditorForm();
		}

		if (request != null) {
			editorForm.setHistoryId(historyId);

			String domainName = DocDB.getDomain(request);
			GroupDetails editorGroup = groupsDB.getGroup(doc.getGroupId());
			if (editorGroup != null && Tools.isNotEmpty(editorGroup.getDomainName())) domainName = editorGroup.getDomainName();
			editorForm.setDomainName(domainName);
			request.getSession().setAttribute("preview.editorDomainName", domainName);

			editorForm.setRefreshCss(request.getParameter("refreshCss") != null);
			editorForm.setDocAtrs(remapDocAttrs(doc_id, request));
			editorForm.setMedia(getMedia(doc_id, request));
			editorForm.setContextPath(request.getContextPath());
		}

		//set editorForm
		if (doc_id == 0)
		{
			editorForm.setRecode(false);
			try
			{
				int temp_id = Integer.parseInt(request.getParameter("tempid"));
				editorForm.setTempId(temp_id);
			}
			catch (Exception ex)
			{
			}
			editorForm.setRecode(true);
		}
		else
		{


			String pubToZero = request.getParameter("pubToZero");
			if ((pubToZero!=null) && (!pubToZero.equals("")) && ((Integer.parseInt(pubToZero))==1) )
			{
				String historyIdFromParameter = request.getParameter("historyid");
				editorForm.setPublicableToZero(Integer.parseInt(historyIdFromParameter));
			}
			else
			{
				editorForm.setPublicableToZero(-1);
			}

			//data is allready recoded to windows1250 code page
			editorForm.setRecode(false);

			editorForm.setDocId(doc.getDocId());
			editorForm.setTitle(doc.getTitle());
			editorForm.setData(doc.getData());
			editorForm.setExternalLink(doc.getExternalLink());
			editorForm.setVirtualPath(doc.getVirtualPath());
			editorForm.setNavbar(doc.getNavbar());
			editorForm.setDateCreated(doc.getDateCreatedString());
			editorForm.setPublishStart(doc.getPublishStartString());
			editorForm.setPublishStartTime(doc.getPublishStartTimeString());
			editorForm.setPublishEnd(doc.getPublishEndString());
			editorForm.setPublishEndTime(doc.getPublishEndTimeString());
			editorForm.setGroupId(doc.getGroupId());
			editorForm.setGroupName(groupsDB.getPath(editorForm.getGroupId()));
			editorForm.setTempId(doc.getTempId());
			editorForm.setSearchable(doc.isSearchable());
			editorForm.setAvailable(doc.isAvailable());
			editorForm.setShowInMenu(doc.isShowInMenu());
			editorForm.setPasswordProtectedString(doc.getPasswordProtected());
			editorForm.setCacheable(doc.isCacheable());
			editorForm.setFileName(doc.getFileName());
			editorForm.setSortPriority(doc.getSortPriority());
			editorForm.setHeaderDocId(doc.getHeaderDocId());
			editorForm.setFooterDocId(doc.getFooterDocId());
			editorForm.setMenuDocId(doc.getMenuDocId());
			editorForm.setRightMenuDocId(doc.getRightMenuDocId());
			editorForm.setHtmlHead(doc.getHtmlHead());
			editorForm.setHtmlData(doc.getHtmlData());
			editorForm.setPerexPlace(doc.getPerexPlace());
			editorForm.setPerexImage(doc.getPerexImage());
			editorForm.setPerexGroupString(doc.getPerexGroupIdsString());
			editorForm.setEventDate(doc.getEventDateString());
			editorForm.setEventTime(doc.getEventTimeString());

			editorForm.setFieldA(doc.getFieldA());
			editorForm.setFieldB(doc.getFieldB());
			editorForm.setFieldC(doc.getFieldC());
			editorForm.setFieldD(doc.getFieldD());
			editorForm.setFieldE(doc.getFieldE());
			editorForm.setFieldF(doc.getFieldF());
			editorForm.setFieldG(doc.getFieldG());
			editorForm.setFieldH(doc.getFieldH());
			editorForm.setFieldI(doc.getFieldI());
			editorForm.setFieldJ(doc.getFieldJ());
			editorForm.setFieldK(doc.getFieldK());
			editorForm.setFieldL(doc.getFieldL());

			editorForm.setDisableAfterEnd(doc.isDisableAfterEnd());

			editorForm.setFieldM(doc.getFieldM());
			editorForm.setFieldN(doc.getFieldN());
			editorForm.setFieldO(doc.getFieldO());
			editorForm.setFieldP(doc.getFieldP());
			editorForm.setFieldQ(doc.getFieldQ());
			editorForm.setFieldR(doc.getFieldR());
			editorForm.setFieldS(doc.getFieldS());
			editorForm.setFieldT(doc.getFieldT());

			// nastavime lastDoc_id a Dalsie adresare
			editorForm.setLastDocId(historyId > 0 ? doc.getDocId() : lastDoc_id);


			editorForm.setRequireSsl(doc.isRequireSsl());

			editorForm.setRecode(true);

			editorForm.setPublicable(doc.isPublicable());

			editorForm.setNote(DocNoteDB.getInstance().getNoteText(doc_id, historyId));
		}

		if (ContextFilter.isRunning(request))
		{
			//do editoru nahrame texty s pridanymi linkami
			editorForm.setData(ContextFilter.addContextPath(request.getContextPath(), editorForm.getData()));
		}

		return(editorForm);
	}

	private static List<Map<String, Object>> remapDocAttrs(int docId, HttpServletRequest request) {
		List<List<AtrBean>> atrs = AtrDB.getAtributes(docId, request);
		List<Map<String, Object>> docAtrs = new ArrayList<>();
		for (List<AtrBean> group : atrs) {
			for (AtrBean atr : group) {
				Map<String, Object> obj = new HashMap<>();

				obj.put("atrId", atr.getAtrId());
				obj.put("type", atr.getAtrType());
				obj.put("value", atr.getValue());

				docAtrs.add(obj);
			}
		}

		return docAtrs;
	}

	private static List<Map<String, Object>> getMedia(int mediaFkId, HttpServletRequest request) {
		List<Media> media;
		String mediaFkTableName = "documents";
		Identity currUser = UsersDB.getCurrentUser(request);
		if (mediaFkId == -1) {
			media = MediaDB.getMedia(request.getSession(), "documents_temp", currUser.getUserId(), null, 0, false);
		} else {
			media = MediaDB.getMedia(request.getSession(), mediaFkTableName, mediaFkId, null, 0, false);
		}

		List<Map<String, Object>> result = new ArrayList<>();

		for (Media media1 : media) {
			Map<String, Object> row = new HashMap<>();

			row.put("id", media1.getMediaId());
			row.put("title", media1.getMediaTitleSk());
			row.put("group", media1.getMediaGroup());
			row.put("link_url", media1.getMediaLink());
			row.put("thumbLink", media1.getMediaThumbLink());
			row.put("order", media1.getMediaSortOrder());

			result.add(row);
		}


		return result;
	}

	/**
	 * nahradim nepovolane znaky medzerami
	 * @param text
	 * @return
	 */
	public static String escapeInvalidCharacters(String text)
	{
		return escapeInvalidCharacters(text, null);
	}

	public static String escapeInvalidCharacters(String text, HttpSession session)
	{
		if(Tools.isEmpty(text)) return text;

		String editorInvalidCharactersInDecFormat = Constants.getString("editorInvalidCharactersInDecFormat");
		int[] charsForEscape = null;
		if(Tools.isNotEmpty(editorInvalidCharactersInDecFormat)) charsForEscape = Tools.getTokensInt(editorInvalidCharactersInDecFormat, ",");

		StringBuilder sb = new StringBuilder("");
		char[] textCharArry = text.toCharArray();
		if(textCharArry != null)
		{
			boolean wasAppend = false;
			for(char znak : textCharArry)
			{
				//Logger.debug(EditorDB.class, znak+" is:"+Character.isBmpCodePoint(znak)+" high="+Character.isHighSurrogate(znak));
				//if(Character.isISOControl(znak) == false && Character.isBmpCodePoint(znak)) sb.append(znak);
				wasAppend = false;
				if(charsForEscape != null)
				{
					for(int charForEscape : charsForEscape)
					{
						if(charForEscape == (znak))
						{
							sb.append(" ");
							wasAppend = true;
							break;
						}
					}
				}

				if (session != null && Character.isHighSurrogate(znak))
				{
					session.setAttribute("cssError", "Ukladany text obsahuje specialne znaky, ktore nie je mozne ulozit do databazy, boli nahradene prazdnym znakom. Skontrolujte ulozeny text znova nacitanim stranky do editora.");
				}

				if(!wasAppend)
				{
					if(Character.isISOControl(znak) == false && Character.isHighSurrogate(znak)==false) sb.append(znak);
					else sb.append(" ");
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Ulozi EditorForm do databazy vratane vsetkych akcii spojenych s ulozenim web stranky (schvalovanie, nastavenie adresara...)
	 * @param my_form
	 * @param request
	 * @return historyId alebo hodnotu &lt; 1 ak nastala chyba
	 */
	public static int saveEditorForm(EditorForm my_form, HttpServletRequest request)
	{
		HttpSession session = request.getSession();

		DebugTimer dt = new DebugTimer("EditorDB.save");
		dt.setEnabled(false);

		UserDetails user;

		//publishEvent(EditorBeforeSaveEvent.class, my_form);
		(new WebjetEvent<>(my_form, WebjetEventType.ON_START)).publishEvent();

		if (my_form.getApproveHistoryId()<1)
		{
			//user je autor dokumentu
			user = UsersDB.getUser(my_form.getAuthorId());
		}
		else
		{
			//user je schvalovatel
			user = UsersDB.getCurrentUser(request);
		}
		if (user == null)
		{
			Logger.error(EditorDB.class, "Nemozem ulozit stranku - nema zadaneho autora zmeny");
			//hmm, to je nejaky problem
			return(-2);
		}

		if(Constants.getBoolean("editorEscapeInvalidCharacters"))
		{
			my_form.setTitle(escapeInvalidCharacters(my_form.getTitle(), session));
			my_form.setData(escapeInvalidCharacters(my_form.getData(), session));
			my_form.setHtmlData(escapeInvalidCharacters(my_form.getHtmlData(), session));
		}

		//Logger.println(this,"DB_ENCODING="+Constants.DB_ENCODING);
		//Logger.println(this,"DB_ENCODING_RECODE="+Constants.DB_ENCODING_RECODE);

		String iwcm_DOC_PATH = Tools.getRealPath("/WEB-INF/docs_html");
		if (iwcm_DOC_PATH!=null)
		{
			iwcm_DOC_PATH = iwcm_DOC_PATH.replace('/', File.separatorChar);
		}

		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();

		//cloud - aby nebolo mozne premenovat Header / Footer

		DocDetails existing = CloudToolsForCore.isPossibleToChangeDoc(my_form.getDocId());
		if (existing != null)
		{
			//nemozeme menit title ani groupid
			my_form.setTitle(existing.getTitle());
			my_form.setGroupId(existing.getGroupId());
		}

		//kvoli Oracle, on nedokaze mat v DB prazdny string a potom to padalo
		if (Tools.isEmpty(my_form.getTitle())) my_form.setTitle("new web page");
		if (Tools.isEmpty(my_form.getNavbar())) my_form.setNavbar(my_form.getTitle());

		String domain = groupsDB.getDomain(my_form.getGroupId());

		my_form.setTitle(DB.prepareString(my_form.getTitle(), 255));

		if (Tools.isEmpty(my_form.getData()))
		{
			//aby v Oracle nepadalo na NULL v data stlpci
			my_form.setData("<p>&nbsp;</p>");
		}

		//nastav virtual path
		setVirtualPath(my_form);

		dt.diff("after virtual path");

		Prop properties = Prop.getInstance(Constants.getServletContext(), request);

		//koli approve
		/*if (my_form.getAuthorId() > 0)
		{
			author_id = my_form.getAuthorId();
		}
		*/

		nonBreakingSpaceReplacement(my_form);

		if (ContextFilter.isRunning(request))
		{
			//data nechceme mat ulozene s context linkami (tie nam prida filter, ak treba)
			my_form.setData(ContextFilter.removeContextPath(request.getContextPath(), my_form.getData()));
		}

		String data = my_form.getData().trim();

		int history_id = -1;
		data = getCleanBody(data);

		dt.diff("after getCleanBody");

		String data_asc = getDataAsc(data, my_form, false, request);

		dt.diff("after getDataAsc");

		long l_now = (new java.util.Date()).getTime();
		java.sql.Date now = new java.sql.Date(l_now);

		boolean requestPublish = false;
		if (my_form.getPublish().equals("1"))
		{
			requestPublish = true;
			//my_form.setAvailable(true);
		}

		if (my_form.getDocId() < 1 && requestPublish == false)
		{
			//nove stranky, ktore neziadaju o schvalenie rovno zadisabluj
			my_form.setAvailable(false);
		}

		/*String approveByUsers = null;
		String approveByUsersId = null;
		String approveByUsersEmail = null;*/

		boolean isNewPage = false;

		//zoznam schvalovatelov
		Map<Integer, UserDetails> approveByTable = new Hashtable<>();
		Map<Integer, UserDetails> approveByTableLevel2 = new Hashtable<>();
		//zoznam, komu ide info o zmene stranky
		Map<Integer, UserDetails> notifyTable = new Hashtable<>();

		java.sql.Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql;
			String sql_check;
			// zisti, ci v historii na ten isty datum a cas nema byt nieco vypublikovane

			if (requestPublish)
			{
				boolean finished = false;
				int currentGroupId = my_form.getGroupId();
				int depth = 0;
				int max_depth = 30;
				GroupDetails group;

				UserDetails u;
				int mode;
				boolean isSelfApprove = false;
				//ak bude true znamena to, ze ma user self approve na root folder
				boolean isSelfApproveRoot = false;
				boolean publish = true;

				while (finished == false)
				{
					group = groupsDB.getGroup(currentGroupId);
					if (group == null)
					{
						//group doesn't exist
						finished = true;
						break;
					}
					depth++;
					if (depth > max_depth)
					{
						finished = true;
						break;
					}
					//we are on the root group
					if (currentGroupId == 0)
					{
						finished = true;
						break;
					}
					currentGroupId = group.getParentGroupId();


					ps = db_conn.prepareStatement("SELECT a.*, u.title as u_title, u.first_name, u.last_name, u.email, u.user_id FROM groups_approve a,  users u WHERE a.user_id=u.user_id AND a.group_id=? AND u.is_admin="+DB.getBooleanSql(true)+" AND u.authorized="+DB.getBooleanSql(true));
					ps.setInt(1, group.getGroupId());
					rs = ps.executeQuery();
					//zoznam schvalovatelov
					Map<Integer, UserDetails> approveByTableGroup = new Hashtable<>();
					Map<Integer, UserDetails> approveByTableLevel2Group = new Hashtable<>();
					//zoznam, komu ide info o zmene stranky
					Map<Integer, UserDetails> notifyTableGroup = new Hashtable<>();
					while (rs.next())
					{
						//ak som ja ten co moze schvalovat, tak to netreba ani robit
						u = new UserDetails();
						u.setUserId(rs.getInt("user_id"));
						u.setTitle(DB.getDbString(rs, "u_title"));
						u.setFirstName(DB.getDbString(rs, "first_name"));
						u.setLastName(DB.getDbString(rs, "last_name"));
						u.setEmail(DB.getDbString(rs, "email"));

						mode = rs.getInt("approve_mode");

						if (u.getUserId() == user.getUserId())
						{
							//vynimka pre tych ktory maju self approve na root (aka interway user)
							if (group.getParentGroupId()<1) isSelfApproveRoot = true;
							//ak ma user leve2 prava, tiez dole neriesme
							if (mode == UsersDB.APPROVE_LEVEL2) isSelfApproveRoot = true;
							isSelfApprove = true;
						}

						if (mode == UsersDB.APPROVE_LEVEL2)
						{
							//schvalovanie
							approveByTableLevel2Group.put(Integer.valueOf(u.getUserId()), u);
						}
						else if (mode == UsersDB.APPROVE_APPROVE)
						{
							//schvalovanie
							approveByTableGroup.put(Integer.valueOf(u.getUserId()), u);
							publish = false;
						}
						else if (mode == UsersDB.APPROVE_NOTIFY && u.getUserId()!=user.getUserId())
						{
							//notifikacia
							notifyTableGroup.put(Integer.valueOf(u.getUserId()), u);
						}
						else
						{
							//nejde mu ziadne info, je to self approve alebo LEVEL2
						}
					}
					rs.close();
					ps.close();

					//pouzivame system best match
					if (approveByTable.size()==0)
					{
						approveByTable.putAll(approveByTableGroup);
					}
					//pouzivame system best match
					if (approveByTableLevel2.size()==0)
					{
						approveByTableLevel2.putAll(approveByTableLevel2Group);
					}
					//notify davame vsetkym
					notifyTable.putAll(notifyTableGroup);
				}

				if (isSelfApproveRoot==false && isSelfApprove && approveByTableLevel2.size()>0)
				{
					//na adresar ma user self approve, existuje ale level2 approver, takze to musi ist na neho
					isSelfApprove = false;
					publish = false;
					approveByTable = approveByTableLevel2;
				}

				if (isSelfApprove)
				{
					approveByTable = new Hashtable<>();
					publish = true;
				}

				if (!publish && requestPublish)
				{
					//ak nemame povolene schvalenie a je ziadost o publikaciu, tak to zrus
					requestPublish = false;
				}
			}

			dt.diff("after requestPublish");

			if (requestPublish && my_form.isPublicable())
			{
				//je zaskrtnuty checkbox publicable a kliknute na Publikuj, takze sa to nemoze publikovat
				requestPublish = false;

			}

			//over si virtual path
			if (my_form.getVirtualPath()!=null && my_form.getVirtualPath().trim().length()>0)
			{
				//Logger.println(this,"my_form.getVirtualPath="+my_form.getVirtualPath());

				int docIdFromVP = docDB.getVirtualPathDocId(my_form.getVirtualPath(), domain);
				if (docIdFromVP > 0 && docIdFromVP != my_form.getDocId())
				{
					//Logger.println(this,"docIdFromVP="+docIdFromVP+" my_form.getDocId()="+my_form.getDocId());
					//zadany VirtualPath je uz pouzity!!!
					DocDetails vpDocDetails = docDB.getDoc(docIdFromVP, -1, false);
					if (vpDocDetails!=null && vpDocDetails.getVirtualPath().equals(my_form.getVirtualPath()))
					{
						session.setAttribute("allreadyUsedVirtualPathDocId", Integer.toString(docIdFromVP));
						if (my_form.getDocId() > 0)
						{
							DocDetails doc = docDB.getDoc(my_form.getDocId(), -1, false);
							//nastav povodnu hodnotu
							my_form.setVirtualPath(doc.getVirtualPath());
						}
						else
						{
							my_form.setVirtualPath("");
						}
					}
				}
			}

			dt.diff("after virtualPath check");

			if (my_form.getDocId() < 1)
			{
				isNewPage = true;

				request.setAttribute("editorForm.saveAsNew", "true");

				sql = "INSERT INTO documents (title, data, data_asc, external_link, navbar, date_created, " +
						"publish_start, publish_end, author_id, group_id, temp_id, searchable, available, " +
						"cacheable, sort_priority, header_doc_id, footer_doc_id, menu_doc_id, password_protected, html_head, "+
						"html_data, perex_place, perex_image, perex_group, show_in_menu, event_date, virtual_path, right_menu_doc_id," +
						"field_a, field_b, field_c, field_d, field_e, field_f, field_g, field_h, field_i, field_j, field_k, field_l, disable_after_end, " +
						"field_m, field_n, field_o, field_p, field_q, field_r, field_s, field_t, require_ssl, file_name, root_group_l1, root_group_l2, root_group_l3)" +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

				if (requestPublish == false)
				{
					my_form.setAvailable(false);
				}

				String publishDocData = null;
				long time = DB.getTimestamp(my_form.getPublishStart(), my_form.getPublishStartTime());
				if (time != 0)
				{
					//ak sa to ma vypublikovat az po aktualnom case, zrus available
					if (time > now.getTime())
					{
						if (my_form.isPublicable())
						{
							my_form.setAvailable(false);
							publishDocData = properties.getText("editor.publish.note") + " " + my_form.getPublishStart() + " " + my_form.getPublishStartTime();
						}
					}
				}

				//Logger.println(this,"INSERTING sql=" + sql);
				ps = db_conn.prepareStatement(sql);
				ps.setString(1, my_form.getTitle());
				if (publishDocData != null)
				{
					DB.setClob(ps, 2, publishDocData);
					DB.setClob(ps, 3, getDataAsc(publishDocData, my_form));
				}
				else
				{
					//ps.setString(2, data);
					//ps.setString(3, data_asc);
					DB.setClob(ps, 2, data);
					DB.setClob(ps, 3, data_asc);
				}
				ps.setString(4, my_form.getExternalLink());
				ps.setString(5, my_form.getNavbar());

				ps.setTimestamp(6, new Timestamp(now.getTime()));
				if (my_form.getPublishStart() != null && my_form.getPublishStart().trim().length() > 0)
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

				if (my_form.getPublishEnd() != null && my_form.getPublishEnd().trim().length() > 0)
				{
					long time2 = DB.getTimestamp(my_form.getPublishEnd(), my_form.getPublishEndTime());
					if (time2 != 0)
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

				ps.setInt(9, user.getUserId());
				ps.setInt(10, my_form.getGroupId());
				ps.setInt(11, my_form.getTempId());
				ps.setBoolean(12, my_form.isSearchable());
				ps.setBoolean(13, my_form.isAvailable());
				ps.setBoolean(14, my_form.isCacheable());
				ps.setInt(15, my_form.getSortPriority());
				ps.setInt(16, my_form.getHeaderDocId());
				ps.setInt(17, my_form.getFooterDocId());
				ps.setInt(18, my_form.getMenuDocId());
				if (my_form.getPasswordProtectedString() == null)
				{
					ps.setNull(19, java.sql.Types.VARCHAR);
				}
				else
				{
					ps.setString(19, my_form.getPasswordProtectedString());
				}
				DB.setClob(ps, 20, my_form.getHtmlHead());
				if (my_form.getHtmlData() == null || my_form.getHtmlData().length() < 1)
				{
					ps.setNull(21, Types.VARCHAR);
				}
				else
				{
					DB.setClob(ps, 21, my_form.getHtmlData());
				}
				ps.setString(22, my_form.getPerexPlace());
				ps.setString(23, my_form.getPerexImage());

				ps.setString(24, my_form.getPerexGroupString());
				ps.setBoolean(25, my_form.isShowInMenu());

				if (Tools.isEmpty(my_form.getEventDate())==false)
				{
					if (Tools.isEmpty(my_form.getEventTime()))
					{
						my_form.setEventTime("6:00");
					}
					long time2 = DB.getTimestamp(my_form.getEventDate(), my_form.getEventTime());
					if (time2 != 0)
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

				if (my_form.getVirtualPath()==null || my_form.getVirtualPath().length()<1)
				{
					ps.setNull(27, Types.VARCHAR);
				}
				else
				{
					ps.setString(27, my_form.getVirtualPath());
				}
				ps.setInt(28, my_form.getRightMenuDocId());

				ps.setString(29, my_form.getFieldA());
				ps.setString(30, my_form.getFieldB());
				ps.setString(31, my_form.getFieldC());
				ps.setString(32, my_form.getFieldD());
				ps.setString(33, my_form.getFieldE());
				ps.setString(34, my_form.getFieldF());
				ps.setString(35, my_form.getFieldG());
				ps.setString(36, my_form.getFieldH());
				ps.setString(37, my_form.getFieldI());
				ps.setString(38, my_form.getFieldJ());
				ps.setString(39, my_form.getFieldK());
				ps.setString(40, my_form.getFieldL());

				ps.setBoolean(41, my_form.isDisableAfterEnd());

				ps.setString(42, my_form.getFieldM());
				ps.setString(43, my_form.getFieldN());
				ps.setString(44, my_form.getFieldO());
				ps.setString(45, my_form.getFieldP());
				ps.setString(46, my_form.getFieldQ());
				ps.setString(47, my_form.getFieldR());
				ps.setString(48, my_form.getFieldS());
				ps.setString(49, my_form.getFieldT());

				ps.setBoolean(50, my_form.isRequireSsl());

				GroupDetails group = groupsDB.getGroup(my_form.getGroupId());
				String fileName = null;
		   	if (group != null && group.isInternal()==false)
		   	{
		   		fileName = groupsDB.getGroupNamePath(my_form.getGroupId());
		   	}
				ps.setString(51, DB.prepareString(fileName,255));

				DocDB.getRootGroupL(my_form.getGroupId(), ps, 52);

				ps.execute();
				ps.close();

				if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
				{
					db_conn.commit();
				}

				dt.diff("after insert");
				//Logger.println(this,"EditorDB: INSERTED");

				//get new doc_id
				//get document id
				ps = db_conn.prepareStatement("SELECT max(doc_id) FROM documents WHERE group_id=? AND title=?");
				// AND date_created=?");
				ps.setInt(1, my_form.getGroupId()); //aby sa pouzil index!!!
				ps.setString(2, my_form.getTitle());
				//ps.setTimestamp(2, new Timestamp(now.getTime()));
				rs = ps.executeQuery();
				if (rs.next())
				{
					my_form.setDocId(rs.getInt(1));
				}
				rs.close();
				ps.close();

				dt.diff("after getDocId");

				//aktualizuj pripadne aj tab. perex_group_doc
				DocDB.udpdatePerexGroupDoc(my_form.getDocId(), my_form.getPerexGroupString());

				//DocDB.updateDataClob(db_conn, my_form.getDocId(), -1, data, data_asc);
			}
			else
			{
				//premenovanie Groupy ak je stranka defaultna pre Grupu.
				if(GroupsService.canSyncTitle(my_form.getDocId(), my_form.getGroupId()))
				{
					DocDB.changeGroupTitle(my_form.getGroupId(), my_form.getDocId(), my_form.getTitle());
				}

				//Logger.println(this,"UKLADAM: requestPublish="+requestPublish+" publicable="+my_form.isPublicable());

				if (my_form.isPublicable())
				{
					if (requestPublish)
					{
						long time = DB.getTimestamp(my_form.getPublishStart(), my_form.getPublishStartTime());
						if (time != 0)
						{
							//Logger.println(this,"time="+time);
							//ak sa to ma vypublikovat az po aktualnom case, zrus available
							if (time > now.getTime())
							{
								//Logger.println(this,"Rusim publish");
								requestPublish = false;
							}
						}
					}
				}

				//Logger.println(this,"UKLADAM 2: requestPublish="+requestPublish);

				if (requestPublish)
				{
					dt.diff("preparing update data");

					sql = "UPDATE documents SET title=?, data=?, data_asc=?, external_link=?, navbar=?, date_created=?, " +
							"publish_start=?, publish_end=?, author_id=?, group_id=?, temp_id=?, searchable=?, available=?, " +
							"cacheable=?, sort_priority=? , header_doc_id=?, footer_doc_id=?, menu_doc_id=?, password_protected=?, "+
							"html_head=?, html_data=?, perex_place=?, perex_image=?, perex_group=?, show_in_menu=?, event_date=?, " +
							"virtual_path=?, right_menu_doc_id=?, " +
							"field_a=?, field_b=?, field_c=?, field_d=?, field_e=?, field_f=?, field_g=?, field_h=?, field_i=?, field_j=?, field_k=?, field_l=?, disable_after_end=?, " +
							"field_m=?, field_n=?, field_o=?, field_p=?, field_q=?, field_r=?, field_s=?, field_t=?, require_ssl=?, file_name=?, root_group_l1=?, root_group_l2=?, root_group_l3=?, "+
							"sync_status=1 "+
							"WHERE doc_id=?";
					//Logger.println(this,"UPDATING id=" + doc_id + " sql=" + sql);
					ps = db_conn.prepareStatement(sql);
					ps.setString(1, my_form.getTitle());
					//ps.setString(2, data);
					//ps.setString(3, data_asc);

					DB.setClob(ps, 2, data);
					DB.setClob(ps, 3, data_asc);

					ps.setString(4, my_form.getExternalLink());
					ps.setString(5, my_form.getNavbar());

					ps.setTimestamp(6, new Timestamp(now.getTime()));
					if (my_form.getPublishStart() != null && my_form.getPublishStart().trim().length() > 0)
					{
						long time = DB.getTimestamp(my_form.getPublishStart(), my_form.getPublishStartTime());
						if (time != 0)
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
					if (my_form.getPublishEnd() != null && my_form.getPublishEnd().trim().length() > 0)
					{
						long time2 = DB.getTimestamp(my_form.getPublishEnd(), my_form.getPublishEndTime());
						if (time2 != 0)
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

					ps.setInt(9, user.getUserId());
					ps.setInt(10, my_form.getGroupId());
					ps.setInt(11, my_form.getTempId());
					ps.setBoolean(12, my_form.isSearchable());
					ps.setBoolean(13, my_form.isAvailable());
					ps.setBoolean(14, my_form.isCacheable());
					ps.setInt(15, my_form.getSortPriority());

					ps.setInt(16, my_form.getHeaderDocId());
					ps.setInt(17, my_form.getFooterDocId());
					ps.setInt(18, my_form.getMenuDocId());

					if (my_form.getPasswordProtectedString() == null)
					{
						ps.setNull(19, java.sql.Types.VARCHAR);
					}
					else
					{
						ps.setString(19, my_form.getPasswordProtectedString());
					}
					DB.setClob(ps, 20, my_form.getHtmlHead());
					if (my_form.getHtmlData() == null || my_form.getHtmlData().length() < 1)
					{
						ps.setNull(21, Types.VARCHAR);
					}
					else
					{
						DB.setClob(ps, 21, my_form.getHtmlData());
					}
					ps.setString(22, my_form.getPerexPlace());
					ps.setString(23, my_form.getPerexImage());
					ps.setString(24, my_form.getPerexGroupString());
					ps.setBoolean(25, my_form.isShowInMenu());

					if (Tools.isNotEmpty(my_form.getEventDate()))
					{
						if (Tools.isEmpty(my_form.getEventTime()))
						{
							my_form.setEventTime("6:00");
						}
						long time2 = DB.getTimestamp(my_form.getEventDate(), my_form.getEventTime());
						if (time2 != 0)
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
					if (my_form.getVirtualPath()==null || my_form.getVirtualPath().length()<1)
					{
						ps.setNull(27, Types.VARCHAR);
					}
					else
					{
						ps.setString(27, my_form.getVirtualPath());
					}
					ps.setInt(28, my_form.getRightMenuDocId());

					ps.setString(29, my_form.getFieldA());
					ps.setString(30, my_form.getFieldB());
					ps.setString(31, my_form.getFieldC());
					ps.setString(32, my_form.getFieldD());
					ps.setString(33, my_form.getFieldE());
					ps.setString(34, my_form.getFieldF());
					ps.setString(35, my_form.getFieldG());
					ps.setString(36, my_form.getFieldH());
					ps.setString(37, my_form.getFieldI());
					ps.setString(38, my_form.getFieldJ());
					ps.setString(39, my_form.getFieldK());
					ps.setString(40, my_form.getFieldL());

					ps.setBoolean(41, my_form.isDisableAfterEnd());

					ps.setString(42, my_form.getFieldM());
					ps.setString(43, my_form.getFieldN());
					ps.setString(44, my_form.getFieldO());
					ps.setString(45, my_form.getFieldP());
					ps.setString(46, my_form.getFieldQ());
					ps.setString(47, my_form.getFieldR());
					ps.setString(48, my_form.getFieldS());
					ps.setString(49, my_form.getFieldT());

					ps.setBoolean(50, my_form.isRequireSsl());

					GroupDetails group = groupsDB.getGroup(my_form.getGroupId());
					String fileName = null;
			   	if (group != null && group.isInternal()==false)
			   	{
			   		fileName = groupsDB.getGroupNamePath(my_form.getGroupId());
			   	}
					ps.setString(51, DB.prepareString(fileName, 255));

					DocDB.getRootGroupL(my_form.getGroupId(), ps, 52);

					ps.setInt(55, my_form.getDocId());

					dt.diff("before execute call");

					ps.execute();
					ps.close();

					//aktualizuj pripadne aj tab. perex_group_doc
					DocDB.udpdatePerexGroupDoc(my_form.getDocId(), my_form.getPerexGroupString());

					if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
					{
						db_conn.commit();
					}

					dt.diff("after update");

					//DocDB.updateDataClob(db_conn, my_form.getDocId(), -1, data, data_asc);

					//delete old file
					if (iwcm_DOC_PATH!=null && my_form.getFileName() != null && my_form.getFileName().length() > 2)
					{
						try
						{
							String filePath = iwcm_DOC_PATH + File.separatorChar + my_form.getFileName();
							File outputFile = new File(filePath);
							if (outputFile.exists())
							{
								if(outputFile.delete() == false)
									throw new SecurityException("Unable to delete file: "+filePath);
							}
						}
						catch (Exception ex)
						{
							sk.iway.iwcm.Logger.error(ex);
						}
					}
				}
			}

			dt.diff("after savedata");

			//savnutie do history
			if (requestPublish)
			{
				session.setAttribute("pageSavedToPublic", "true");
			}
			else
			{
				if (my_form.isPublicable())
				{
					session.setAttribute("pagePublishDate", my_form.getPublishStart() + " " + my_form.getPublishStartTime());
					request.setAttribute("pagePublishDate", my_form.getPublishStart() + " " + my_form.getPublishStartTime());
				}
				else
				{
					session.setAttribute("pageSaved", "true");
				}
			}

			dt.diff("after session set");

			saveDocAttrs(my_form.getDocId(), db_conn, request);

			dt.diff("after attributes save");

			//vypne zapisovanie zaznamov do documents_history tabulky. true - nezapise zaznam do documents_history
			boolean disableHistory =  Constants.getBoolean("editorDisableHistory");
			if(disableHistory)
			{
				Logger.debug(EditorDB.class, "Write into documents_history is disabled");
				history_id = 1;
			}

			boolean was_approved = false;
			//ak to nie je volane z ApproveAction
			if (my_form.getApproveHistoryId() < 0 && !disableHistory)
			{
				if ((my_form.getPublicableToZero() > -1) && (my_form.isPublicable()))
				{
					ps = db_conn.prepareStatement("UPDATE documents_history SET publicable="+DB.getBooleanSql(false)+" WHERE history_id=?");
					ps.setInt(1, my_form.getPublicableToZero());
					ps.execute();
					ps.close();
				}
				else
				{
					//Logger.println(this,"my_form.getPublicableToZero()="+my_form.getPublicableToZero());
				}

				dt.diff("after update doc history publicable");

				// zisti, ci v hystorii na ten isty datum a cas nema byt nieco vypublikovane //lubbis
				sql_check = "UPDATE documents_history SET publicable=?, sync_status=1 WHERE doc_id=? and publish_start=?";
				if (my_form.isPublicable())
				{
					ps = db_conn.prepareStatement(sql_check);
					ps.setBoolean(1, false);
					ps.setInt(2, my_form.getDocId());

					if (my_form.getPublishStart() != null && my_form.getPublishStart().trim().length() > 0)
					{
						long time = DB.getTimestamp(my_form.getPublishStart(), my_form.getPublishStartTime());

						if (time != 0)
						{
							ps.setTimestamp(3, new Timestamp(time));
						}
						else
						{
							ps.setNull(3, java.sql.Types.DATE);
						}
					}
					else
					{
						ps.setNull(3, java.sql.Types.DATE);
					}
					ps.execute();
					ps.close();

				}

				dt.diff("after dochist is publicable");

				sql = "INSERT INTO documents_history (title, data, data_asc, external_link, navbar, date_created, " +
						"publish_start, publish_end, author_id, group_id, temp_id, searchable, available, " +
						"cacheable, sort_priority, header_doc_id, footer_doc_id, menu_doc_id, doc_id, save_date, actual, " +
						"approved_by, awaiting_approve, password_protected, html_head, html_data, publicable, perex_place, " +
						"perex_image, perex_group, show_in_menu, event_date, virtual_path, right_menu_doc_id," +
						"field_a, field_b, field_c, field_d, field_e, field_f, field_g, field_h, field_i, field_j, field_k, field_l, disable_after_end, " +
						"field_m, field_n, field_o, field_p, field_q, field_r, field_s, field_t, require_ssl) "+
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				//Logger.println(this,"INSERTING sql=" + sql);
				ps = db_conn.prepareStatement(sql);
				ps.setString(1, my_form.getTitle());
				//ps.setString(2, data);
				//ps.setString(3, data_asc);
				DB.setClob(ps, 2, data);
				DB.setClob(ps, 3, data_asc);

				ps.setString(4, my_form.getExternalLink());
				ps.setString(5, my_form.getNavbar());

				ps.setTimestamp(6, new Timestamp(now.getTime()));
				if (my_form.getPublishStart() != null && my_form.getPublishStart().trim().length() > 0)
				{
					long time = DB.getTimestamp(my_form.getPublishStart(), my_form.getPublishStartTime());

					if (time != 0)
					{
						ps.setTimestamp(7, new Timestamp(time));
					}
					else
					{
						ps.setNull(7, java.sql.Types.DATE);
					}
				}
				else
				{
					ps.setNull(7, java.sql.Types.DATE);
				}

				if (my_form.getPublishEnd() != null && my_form.getPublishEnd().trim().length() > 0)
				{
					long time2 = DB.getTimestamp(my_form.getPublishEnd(), my_form.getPublishEndTime());
					if (time2 != 0)
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

				ps.setInt(9, user.getUserId());
				ps.setInt(10, my_form.getGroupId());
				ps.setInt(11, my_form.getTempId());
				ps.setBoolean(12, my_form.isSearchable());
				ps.setBoolean(13, my_form.isAvailable());
				ps.setBoolean(14, my_form.isCacheable());
				ps.setInt(15, my_form.getSortPriority());
				ps.setInt(16, my_form.getHeaderDocId());
				ps.setInt(17, my_form.getFooterDocId());
				ps.setInt(18, my_form.getMenuDocId());
				ps.setInt(19, my_form.getDocId());
				ps.setTimestamp(20, new Timestamp(l_now));
				if (requestPublish)
				{
					ps.setBoolean(21, true);
				}
				else
				{
					ps.setBoolean(21, false);
				}

				if (approveByTable.size()>0)
				{
					ps.setInt(22, -1);
				}
				else
				{
					if (requestPublish)
					{
						ps.setInt(22, user.getUserId());
						was_approved = true;
					}
					else
					{
						ps.setInt(22, -1);
					}
				}

				if (approveByTable.size()>0 && "1".equals(my_form.getPublish()))
				{
					StringBuilder approveByUsersId = new StringBuilder(",");
					for (Map.Entry<Integer, UserDetails> entry : approveByTable.entrySet())
					{
						approveByUsersId.append(entry.getValue().getUserId()).append(',');
					}
					//Logger.println(this,"approveByUsersId="+approveByUsersId+" publish="+requestPublish);
					ps.setString(23, ","+approveByUsersId.toString()+",");
				}
				else
				{
					ps.setNull(23, Types.VARCHAR);
				}

				if (my_form.getPasswordProtectedString() == null)
				{
					ps.setNull(24, java.sql.Types.VARCHAR);
				}
				else
				{
					ps.setString(24, my_form.getPasswordProtectedString());
				}

				DB.setClob(ps, 25, my_form.getHtmlHead());
				if (my_form.getHtmlData() == null || my_form.getHtmlData().length() < 1)
				{
					ps.setNull(26, Types.VARCHAR);
				}
				else
				{
					DB.setClob(ps, 26, my_form.getHtmlData());
				}
				ps.setBoolean(27, my_form.isPublicable());
				ps.setString(28, my_form.getPerexPlace());
				ps.setString(29, my_form.getPerexImage());
				ps.setString(30, my_form.getPerexGroupString());
				ps.setBoolean(31, my_form.isShowInMenu());

				if (my_form.getEventDate() != null && my_form.getEventTime().trim().length() > 0)
				{
					long time2 = DB.getTimestamp(my_form.getEventDate(), my_form.getEventTime());
					if (time2 != 0)
					{
						ps.setTimestamp(32, new Timestamp(time2));
					}
					else
					{
						ps.setNull(32, java.sql.Types.TIMESTAMP);
					}
				}
				else
				{
					ps.setNull(32, java.sql.Types.DATE);
				}

				if (my_form.getVirtualPath()==null || my_form.getVirtualPath().length()<1)
				{
					ps.setNull(33, Types.VARCHAR);
				}
				else
				{
					ps.setString(33, my_form.getVirtualPath());
				}
				ps.setInt(34, my_form.getRightMenuDocId());

				ps.setString(35, my_form.getFieldA());
				ps.setString(36, my_form.getFieldB());
				ps.setString(37, my_form.getFieldC());
				ps.setString(38, my_form.getFieldD());
				ps.setString(39, my_form.getFieldE());
				ps.setString(40, my_form.getFieldF());
				ps.setString(41, my_form.getFieldG());
				ps.setString(42, my_form.getFieldH());
				ps.setString(43, my_form.getFieldI());
				ps.setString(44, my_form.getFieldJ());
				ps.setString(45, my_form.getFieldK());
				ps.setString(46, my_form.getFieldL());

				ps.setBoolean(47, my_form.isDisableAfterEnd());

				ps.setString(48, my_form.getFieldM());
				ps.setString(49, my_form.getFieldN());
				ps.setString(50, my_form.getFieldO());
				ps.setString(51, my_form.getFieldP());
				ps.setString(52, my_form.getFieldQ());
				ps.setString(53, my_form.getFieldR());
				ps.setString(54, my_form.getFieldS());
				ps.setString(55, my_form.getFieldT());

				ps.setBoolean(56, my_form.isRequireSsl());

				ps.execute();
				ps.close();

				if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
				{
					db_conn.commit();
				}

				dt.diff("after dochistory insert");

				//ziskaj history_id
				ps = db_conn.prepareStatement("SELECT max(history_id) AS max FROM documents_history WHERE doc_id=?");
				ps.setInt(1, my_form.getDocId());
				rs = ps.executeQuery();
				if (rs.next())
				{
					history_id = rs.getInt("max");
				}
				rs.close();
				ps.close();

				if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
				{
					db_conn.commit();
				}

				dt.diff("after dochistory get id");

				//DocDB.updateDataClob(db_conn, -1, history_id, data, data_asc);

			}
			else if (my_form.getApproveHistoryId()>0)
			{
				//je to volane z ApproveAction
				history_id = my_form.getApproveHistoryId();
				if (requestPublish)
				{
					was_approved = true;
				}
				try
				{
					ps = db_conn.prepareStatement("UPDATE documents_history SET approved_by=?, actual=?, awaiting_approve=?, approve_date=?, sync_status=1, available=? WHERE history_id=?");
					ps.setInt(1, user.getUserId());
					ps.setBoolean(2, true);
					ps.setNull(3, Types.VARCHAR);
					ps.setTimestamp(4, new Timestamp(Tools.getNow()));
					ps.setBoolean(5, true);
					ps.setInt(6, history_id);
					ps.execute();
					ps.close();
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				dt.diff("after dochist update");
			}

			if (was_approved)
			{
				// zmaz stare dokumenty, ktore nie su schvalene
				ps = db_conn.prepareStatement("SELECT history_id FROM documents_history WHERE doc_id=? AND history_id<? AND publicable=? AND author_id=? ");
				ps.setInt(1, my_form.getDocId());
				if (my_form.getApproveHistoryId() > 0)
				{
					ps.setInt(2, my_form.getApproveHistoryId());
				}
				else
				{
					ps.setInt(2, history_id);
				}
				ps.setBoolean(3, false);
				ps.setInt(4, user.getUserId());

				StringBuilder historyIds = new StringBuilder();
				rs = ps.executeQuery();
				while (rs.next())
				{
					if (historyIds.length() == 0)
					{
						historyIds.append(Integer.toString(rs.getInt("history_id")));
					}
					else
					{
						historyIds.append(',').append(rs.getInt("history_id"));
					}
				}
				rs.close();
				ps.close();

				dt.diff("after was_approved history_id list");

				if (historyIds.length() > 0)
				{
					if (request.getAttribute("doNotDeleteHistoryOnPublish")!=null)
					{
						//aby sa pri zmene poradia stranky cez kontextove menu nezmazali rozpracovane verzie stranky
						//http://intra.iway.sk/helpdesk/?bugID=7713

						//musime im ale aktualizovat (mozne) zmenene hodnoty poradia usporiadania
						String sql2 = "UPDATE documents_history SET sort_priority=?, temp_id=?, group_id=? WHERE history_id IN (" + historyIds + ") AND approved_by=-1";

						Logger.debug(EditorDB.class, "sql2="+sql2);

						ps = db_conn.prepareStatement(sql2);
						ps.setInt(1, my_form.getSortPriority());
						ps.setInt(2, my_form.getTempId());
						ps.setInt(3, my_form.getGroupId());
						ps.execute();
						ps.close();
					}
					else
					{
						String sql2 = "DELETE FROM documents_history WHERE history_id IN (" + historyIds + ") AND approved_by=-1 AND author_id=?";
						//Logger.println(this,sql2+author_id);
						ps = db_conn.prepareStatement(sql2);
						ps.setInt(1, user.getUserId());
						ps.execute();
						ps.close();
					}
				}

				dt.diff("after was_approved delete");

				//nastav aktualne na actual=false
				ps = db_conn.prepareStatement("SELECT history_id FROM documents_history WHERE doc_id=? AND history_id<?");
				ps.setInt(1, my_form.getDocId());
				if (my_form.getApproveHistoryId() > 0)
				{
					ps.setInt(2, my_form.getApproveHistoryId());
				}
				else
				{
					ps.setInt(2, history_id);
				}
				historyIds = new StringBuilder();
				rs = ps.executeQuery();
				while (rs.next())
				{
					if (historyIds.length() == 0)
					{
						historyIds.append(Integer.toString(rs.getInt("history_id")));
					}
					else
					{
						historyIds.append(',').append(rs.getInt("history_id"));
					}
				}
				rs.close();
				ps.close();

				dt.diff("after was_approved select");

				if (historyIds.length() > 0 )
				{
					ps = db_conn.prepareStatement("UPDATE documents_history SET actual=?, awaiting_approve=?, sync_status=1 WHERE history_id IN (" + historyIds + ")");
					ps.setBoolean(1, false);
					ps.setNull(2, Types.VARCHAR);
					//ps.setInt(3, user.getUserId());
					ps.executeUpdate();
					ps.close();
				}

				if (Constants.DB_TYPE == Constants.DB_MSSQL && Constants.getBoolean("jtdsCommit"))
				{
					db_conn.commit();
				}

				dt.diff("after was_approved");


			}

			//posli maili na schvalenie
			if (approveByTable.size()>0)
			{
				sendApproveRequestEmail(history_id, approveByTable, user.getUserId(), null, request);

				//start- navrh na zmenu logovania v CMS (#19820)
				UserDetails u;
				String approveByUsersEmail = null;
				for (Map.Entry<Integer, UserDetails> entry : approveByTable.entrySet())
				{
					u=entry.getValue();
					if (Tools.isEmail(u.getEmail()))
					{
						if (approveByUsersEmail==null)
						{
							approveByUsersEmail = u.getEmail();
						}
						else
						{
							approveByUsersEmail += "," + u.getEmail(); //NOSONAR
						}
					}
				}
				Adminlog.add(Adminlog.TYPE_SAVEDOC, "Poziadane o schvalenie stranky "+my_form.getTitle()+" / "+my_form.getDocId()+" / "+history_id+", ziadost zaslana na emaili: "+approveByUsersEmail, my_form.getDocId(), history_id);
				//koniec- navrh na zmenu logovania v CMS (#19820)

				dt.diff("after sendApproveRequestEmail");
			}
			else if (notifyTable.size()>0)
			{
				//ak nie je nic na schvalenie, posielame info emailom
				//else if je preto, ze info sa posiela az po schvaleni

				String title = my_form.getTitle(); //.toLowerCase();
				//title = DB.internationalToEnglish(title).toLowerCase();

				String notifyEmails = null;
				UserDetails u;
				for (Map.Entry<Integer, UserDetails> entry : notifyTable.entrySet())
				{
					u = entry.getValue();

					if (notifyEmails==null)
					{
						notifyEmails = u.getEmail();
					}
					else
					{
						notifyEmails += "," + u.getEmail(); //NOSONAR
					}
				}

				String url = DocDB.getInstance().getDocLink(my_form.getDocId(), null, true, request);  // Tools.getBaseHref(request) + "/showdoc.do?docid=" + my_form.getDocId();

				StringBuilder message = new StringBuilder("<strong>").append(properties.getText("doc.notify.intro")).append(":</strong><br>\n");
				message.append("<strong>").append(title).append("</strong><br>\n");
				message.append(properties.getText("doc.notify.dir")).append(":<br>\n");
				message.append(groupsDB.getPath(my_form.getGroupId())).append("<br><br>\n\n");
				message.append(properties.getText("doc.notify.url")).append(":<br>\n");
				message.append("<a href='"+url+"'>").append(url).append("</a><br><br>\n\n");

				message.append(user.getFullName()).append(" &lt;").append(user.getEmail()).append("&gt;");

				String subject = properties.getText("doc.notify.subject") + ": " + title;

				SendMail.send(user.getFullName(), user.getEmail(), notifyEmails, subject, message.toString(), request);

				dt.diff("after sendNotifyEmail");
			}

			setDefaultDocId(my_form.getGroupId(), my_form.getDocId());

			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;

			if (requestPublish)
			{
				if (Constants.getBoolean("exportFlash")) XmlExport.flashExport("/flash_xml/"+my_form.getDocId()+".xml", my_form.getDocId());

				//	toto dava info pre ApproveAction ze to zbehlo OK
				request.setAttribute("saveOK", "true");
			}

			//Logger.println(this,my_form.getData());
		}
		catch (Exception sqle)
		{
			Logger.error(EditorDB.class,"DB spadlo spojenie");
			sk.iway.iwcm.Logger.error(sqle);
			session.setAttribute("cssError", Prop.getTxt("editor.ajax.save.error"));
		}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		dt.diff("after database close");

		if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML)
		{
			List<DocDetails> updated = fixRenamedVirtualPath(request, my_form.getDocId(), my_form.getVirtualPath());
			if (updated.size()>0)
			{
				request.getSession().setAttribute("updatedDocs", updated);
			}
			dt.diff("after fixRenamedVirtualPath");
		}

		//restore cache - v approve history nastavujem 0 ked nechcem aktualizaciu ani zapis do historie
		if ("true".equals(request.getParameter("dontRefreshDocDB"))==false && "true".equals(request.getAttribute("dontRefreshDocDB"))==false && my_form.getApproveHistoryId()!=0)
		{
			if (my_form.getGroupId() == Constants.getInt("tempGroupId") || my_form.getGroupId() == Constants.getInt("menuGroupId") || my_form.getGroupId() == Constants.getInt("headerFooterGroupId"))
			{
				//v system adresari pre istotu spravim klasicky refresh
				docDB = DocDB.getInstance(true);
				dt.diff("after DocDB.getInstance");
				/*spravime update dokumentu v indexe */
				docDB.updateInternalCaches(my_form.getDocId());
			}
			else
			{
				docDB.updateInternalCaches(my_form.getDocId());
				dt.diff("after DocDB.update internal caches");
			}
		}

		if (my_form.getGroupId() == Constants.getInt("tempGroupId") || my_form.getGroupId() == Constants.getInt("menuGroupId") || my_form.getGroupId() == Constants.getInt("headerFooterGroupId"))
		{
			//refreshnut aj sablony
			//GroupsDB grDB = GroupsDB.getInstance(servlet.getServletContext(), true, DBPool.getDBName(request));
			TemplatesDB.getInstance(true);
			dt.diff("after templates DB getInstance");
		}

		//ulozenie poznamky pre redaktorov k webstranke
		DocNoteBean note = DocNoteDB.getInstance().getDocNote(my_form.getDocId(), -1);
		if(Tools.isNotEmpty(my_form.getNote()))
		{
			//ak je my_form.isPublicable()==true, tak sa stranka publikuje v buducnosti. Poznamka sa nastavy v DocDB.copyDHistory(List<PublicableForm> copyDHtoD) a nie tu
			if("1".equals(my_form.getPublish()) && !my_form.isPublicable())
			{
				if(note == null)
					note = new DocNoteBean();
				note.setDocId(my_form.getDocId());
				note.setHistoryId(-1);
				note.setNote(my_form.getNote());
				note.setUserId(my_form.getAuthorId());
				note.setCreated(new Date());
				note.save();
			}

			//historia sa ulozi stale
			DocNoteBean noteHistory = DocNoteDB.getInstance().getDocNote(-1, history_id);
			if(noteHistory == null)
				noteHistory = new DocNoteBean();
			noteHistory.setDocId(-1);
			noteHistory.setHistoryId(history_id);
			noteHistory.setNote(my_form.getNote());
			noteHistory.setUserId(my_form.getAuthorId());
			noteHistory.setCreated(new Date());
			noteHistory.save();
		}
		else if(note!=null)
		{
			//ak je zadana poznamka z formulara prazdna, tak existujucu note k stranke vymazeme
			note.delete();
		}

		dt.diff("after instances");

		String newPageAppend = "";
		if (isNewPage) newPageAppend = " (newpage)";

		Adminlog.add(Adminlog.TYPE_SAVEDOC, "EditorDB"+newPageAppend+" doc_id:" + my_form.getDocId()+" history_id:"+history_id+" title: " + my_form.getTitle()+" path: "+my_form.getVirtualPath(), my_form.getDocId(), history_id);

		dt.diff("done");

		//ak su nastavene syncId tak zapis aj to, potrebne pre synchronizaciu struktury
		if (my_form.getSyncId()>0) {
			new SimpleQuery().execute("UPDATE documents SET sync_id=? WHERE doc_id=?", my_form.getSyncId(), my_form.getDocId());
		}

		//publishEvent(EditorAfterSaveEvent.class, my_form);
		(new WebjetEvent<>(my_form, WebjetEventType.AFTER_SAVE)).publishEvent();

		return(history_id);
	}

	/**
	 * @param myForm
	 */
	public static void nonBreakingSpaceReplacement(EditorForm myForm)
	{
		String conjunctions = Constants.getString("editorSingleCharNbsp");
		if (Tools.isNotEmpty(conjunctions))
		{
			myForm.setData(myForm.getData().replaceAll("(?i)(\\s|&nbsp;)("+conjunctions.replace(',','|')+")\\s","$1$2&nbsp;"));
			//volame dva krat, pretoze sa nenahradzali 2 pismena za sebou, napr. Test a v case
			myForm.setData(myForm.getData().replaceAll("(?i)(\\s|&nbsp;)("+conjunctions.replace(',','|')+")\\s","$1$2&nbsp;"));
		}

	}

	/**
	 * Ak sa zmeni virtual path stranky aktualizuje vsetky doterajsie linky
	 * @param request
	 * @param docId
	 * @param newVirtualPath
	 * @return
	 */
	private static List<DocDetails> fixRenamedVirtualPath(HttpServletRequest request, int docId, String newVirtualPath)
	{
		List<DocDetails> updated = new ArrayList<>();

		if (Tools.isEmpty(newVirtualPath) || Constants.getBoolean("editorDisableAutomaticRedirect"))
		{
			return(updated);
		}

		newVirtualPath = DocDB.normalizeVirtualPath(newVirtualPath);

		//	najdi web stranky, co treba premenovat...
		String oldLinkURL = DocDB.getURLFromDocId(docId, request);
		Logger.println(EditorDB.class, "OldLinkURL: " + oldLinkURL);
		String domain = null;
		if (Tools.isNotEmpty(oldLinkURL) && !oldLinkURL.equals("/showdoc.do?docid="+docId))
		{
			String newLinkURL = newVirtualPath;
			//Logger.println(EditorDB.class, "newLinkURL="+newLinkURL);

			if (oldLinkURL.compareTo(newLinkURL) != 0)
			{
				Connection db_conn = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try
				{
					GroupsDB groupsDB = GroupsDB.getInstance();
					DocDB docDB = DocDB.getInstance();

					String inSql = "";
					//zisti domenu (ak treba) a sprav to iba na substrankach domeny
					if (Constants.getBoolean("multiDomainEnabled")==true)
					{
						DocDetails doc = docDB.getBasicDocDetails(docId, false);
						if (doc != null)
						{
							GroupDetails group = groupsDB.getGroup(doc.getGroupId());
							domain = group.getDomainName();
							inSql = " AND group_id IN ("+groupsDB.getSubgroupsIds(domain)+")";
						}
					}

					Logger.debug(EditorDB.class, "fixRenamedVirtualPath: old=" + oldLinkURL + " new=" + newLinkURL+" domain="+domain);

					//premenuj stranky kde sa nachadza text
					updated = replaceUrl(oldLinkURL, newLinkURL, domain);

					//aktualizuj presmerovania
					db_conn = DBPool.getConnection();
					String sql = "UPDATE documents SET external_link=? WHERE external_link=?"+inSql;
					Logger.debug(EditorDB.class, "fixRenamedVirtualPath sql1: "+sql+" new="+newVirtualPath+" old="+oldLinkURL);
					ps = db_conn.prepareStatement(sql);
					ps.setString(1, newLinkURL);
					ps.setString(2, oldLinkURL);
					ps.execute();
					ps.close();

					inSql = "";
					if (domain != null)
					{
						inSql = "-1";
						//ziskaj najskor cely zoznam a priprav si ID medii na aktualizaciu
						ps = db_conn.prepareStatement("SELECT distinct(media_fk_id) FROM media WHERE media_link=? AND media_fk_table_name=?");
						ps.setString(1, oldLinkURL);
						ps.setString(2, "documents");
						rs = ps.executeQuery();
						StringBuilder inSqlBuffer = new StringBuilder();
						while (rs.next())
						{
							int mediaDocId = rs.getInt(1);
							String docDomain = docDB.getDomain(mediaDocId);
							if (docDomain.equals(domain))
							{
								inSqlBuffer.append(',').append(mediaDocId);

							}
						}
						inSql = inSqlBuffer.toString();
						if (Tools.isEmpty(inSql)) inSql = "-1";
						inSql = " AND media_fk_id IN ("+inSql+") AND media_fk_table_name='documents'";
						rs.close();
						ps.close();
						rs = null;
					}
					//aktualizuj media
					sql = "UPDATE media SET media_link=? WHERE media_link=? "+inSql;
					Logger.debug(EditorDB.class, "fixRenamedVirtualPath sql2: "+sql);
					ps = db_conn.prepareStatement(sql);
					ps.setString(1, newLinkURL);
					ps.setString(2, oldLinkURL);
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

				//	zapis presmerovanie
				UrlRedirectDB.addRedirect(oldLinkURL, newVirtualPath, domain, 301);
			}
		}

		return(updated);
	}

	/**
	 * Nahradi odkazy z povodneho na nove URL vo vsetkych strankach
	 * @param oldLinkURL
	 * @param newLinkURL
	 * @return
	 */
	public static List<DocDetails> replaceUrl(String oldLinkURL, String newLinkURL, String domain)
	{
		List<DocDetails> docsUpdated = new ArrayList<>();

		if (Constants.getBoolean("editorQuickUrlFix")==true) return docsUpdated;

		DebugTimer dt = new DebugTimer("replaceUrl");
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			dt.diff("starting");

			String oldLinkURL2 = oldLinkURL;
			if (oldLinkURL.length()>2 && oldLinkURL.endsWith("/"))
			{
				oldLinkURL2 = oldLinkURL.substring(0, oldLinkURL.length()-1);
			}
			String inSql = "";
			//zisti domenu (ak treba) a sprav to iba na substrankach domeny
			if (Constants.getBoolean("multiDomainEnabled")==true && Tools.isNotEmpty(domain))
			{
				GroupsDB groupsDB = GroupsDB.getInstance();
				inSql = " AND group_id IN ("+groupsDB.getSubgroupsIds(domain)+")";
			}

			String sql = "SELECT doc_id, title, data FROM documents WHERE data LIKE ? "+inSql;

			dt.diff("getting data, sql="+sql);

			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, "%"+oldLinkURL2+"%");
			rs = ps.executeQuery();
			List<DocDetails> docs = new ArrayList<>();
			while (rs.next())
			{
				DocDetails doc = new DocDetails();
				doc.setDocId(rs.getInt("doc_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setData(DB.getDbString(rs, "data"));

				dt.diff("mam: " + doc.getTitle());

				docs.add(doc);
			}
			rs.close();
			ps.close();

			dt.diff("mam, pocet:"+docs.size());

			//ok, mame zoznam, updatni to
			for (DocDetails doc : docs)
			{
				Logger.println(EditorDB.class,"updating link in: "+doc.getTitle());

				String oldData = doc.getData();
				doc.setData(Tools.replace(doc.getData(), "'"+oldLinkURL+"'", "'"+newLinkURL+"'"));
				doc.setData(Tools.replace(doc.getData(), "\""+oldLinkURL+"\"", "\""+newLinkURL+"\""));
				//toto robilo problem ked sa URL / menilo na /nieco-ine, menilo to aj 0903 / 100 100
				//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL+" ", " "+newLinkURL+" "));
				doc.setData(Tools.replace(doc.getData(), "'"+oldLinkURL+"#", "'"+newLinkURL+"#"));
				doc.setData(Tools.replace(doc.getData(), "\""+oldLinkURL+"#", "\""+newLinkURL+"#"));
				//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL+"#", " "+newLinkURL+"#"));

				if (oldLinkURL.length()>2 && oldLinkURL.endsWith("/"))
				{
					//fix na chybajuce koncove lomitko
					doc.setData(Tools.replace(doc.getData(), "'"+oldLinkURL2+"'", "'"+newLinkURL+"'"));
					doc.setData(Tools.replace(doc.getData(), "\""+oldLinkURL2+"\"", "\""+newLinkURL+"\""));
					//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL2+" ", " "+newLinkURL+" "));
					doc.setData(Tools.replace(doc.getData(), "'"+oldLinkURL2+"#", "'"+newLinkURL+"#"));
					doc.setData(Tools.replace(doc.getData(), "\""+oldLinkURL2+"#", "\""+newLinkURL+"#"));
					//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL2+"#", " "+newLinkURL+"#"));
				}
				else if (oldLinkURL.length()>2 && oldLinkURL.endsWith(".html")==false)
				{
					//ak je linka bez koncoveho /, toto to vyriesi
					oldLinkURL2 = oldLinkURL+"/";

					doc.setData(Tools.replace(doc.getData(), "'"+oldLinkURL2+"'", "'"+newLinkURL+"'"));
					doc.setData(Tools.replace(doc.getData(), "\""+oldLinkURL2+"\"", "\""+newLinkURL+"\""));
					//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL2+" ", " "+newLinkURL+" "));
					doc.setData(Tools.replace(doc.getData(), "'"+oldLinkURL2+"#", "'"+newLinkURL+"#"));
					doc.setData(Tools.replace(doc.getData(), "\""+oldLinkURL2+"#", "\""+newLinkURL+"#"));
					//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL2+"#", " "+newLinkURL+"#"));
				}

				if (oldData.equals(doc.getData()) == false)
				{
					dt.diff("updating doc:"+doc.getDocId());

					ps = db_conn.prepareStatement("UPDATE documents SET data=?, data_asc=?, sync_status=1 WHERE doc_id=?");
					//ps.setString(1, doc.getData());
					//ps.setString(2, DB.internationalToEnglish(doc.getData()).toLowerCase());
					DB.setClob(ps, 1, doc.getData());
					DB.setClob(ps, 2, DB.internationalToEnglish(doc.getData()).toLowerCase());

					ps.setInt(3, doc.getDocId());
					ps.executeUpdate();
					ps.close();

					docsUpdated.add(doc);
				}

				//DocDB.updateDataClob(db_conn, doc.getDocId(), -1, doc.getData(), DB.internationalToEnglish(doc.getData()).toLowerCase());
			}

			dt.diff("done");

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

		return(docsUpdated);
	}

	/**
	 *  Gets the cleanBody
	 *
	 *@param  data     Description of the Parameter
	 *@return          The cleanBody value
	 */
	public static String getCleanBody(String data)
	{
		//odstran vsetko pred <body> a po </body>
		String data_lcase = data.toLowerCase();

		if (data_lcase.contains("<!-- zaciatok textu iwcm editor -->"))
		{
			int index = data_lcase.indexOf("<!-- zaciatok textu iwcm editor -->");
			int index2;
			if (index > 0)
			{
				index2 = data_lcase.indexOf(">", index + 1);
				if (index2 > index && index2 < data_lcase.length())
				{
					data = data.substring(index2 + 1);
				}
			}

			index = data.toLowerCase().indexOf("<!-- koniec textu iwcm editor -->");
			if (index > 0)
			{
				data = data.substring(0, index);
			}
		}

		data_lcase = data.toLowerCase();

		//orezanie HTML
		if (Tools.isEmpty(Constants.getString("htmlImportStart")) == false && Tools.isEmpty(Constants.getString("htmlImportEnd")) == false && data_lcase.contains(Constants.getString("htmlImportStart").toLowerCase()))
		{
			int index = data_lcase.indexOf(Constants.getString("htmlImportStart").toLowerCase());
			int index2;
			if (index > 0)
			{
				index2 = data_lcase.indexOf(">", index + 1);
				if (index2 > index && index2 < data_lcase.length())
				{
					//odrezeme zaciatok
					data = data.substring(index2 + 1);
				}
			}

			index = data.toLowerCase().indexOf(Constants.getString("htmlImportEnd").toLowerCase());
			if (index > 0)
			{
				//odrezeme koniec
				data = data.substring(0, index);
			}
		}
		else
		{
			int index = data_lcase.indexOf("<body");
			int index2;
			if (index > 0)
			{
				index2 = data_lcase.indexOf(">", index + 1);
				if (index2 > index && index2 < data_lcase.length())
				{
					data = data.substring(index2 + 1);
				}
			}

			index = data.toLowerCase().indexOf("</body>");
			if (index > 0)
			{
				data = data.substring(0, index);
			}
		}
		return (data);
	}

	/**
	 * skopiruje niektore udaje z Identity do EditorUserAccessBean
	 * @param user
	 * @return
	 */
	public static EditorUserAccessBean setEditorUserAccesBean(UserDetails user)
	{
		EditorUserAccessBean userDetail = new EditorUserAccessBean();
		userDetail.setUser(user);
		userDetail.setDatumPoslednejAktivity(Tools.getNow());

		return userDetail;
	}

	/**
	 * aktualizuje zoznam uzivatelov ktory edituju stranku s docId
	 * @param docId
	 * @param user
	 */
	public static void updateUserAccessList(int docId, UserDetails user)
	{
		@SuppressWarnings("unchecked")
		Map<Integer, List<EditorUserAccessBean>> userList = (Map<Integer, List<EditorUserAccessBean>>)Constants.getServletContext().getAttribute("userAccessList");
		try
		{
			//zistim aj totozne stranky z multikategorii
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

			for(Integer id : docIdList)
			{
				List<EditorUserAccessBean> userListAl = null;
				if(userList != null && userList.size() > 0)
					userListAl = userList.get(id);
				else
					userList = new Hashtable<>();

				if(userListAl == null)
					userListAl = new ArrayList<>();

				List<EditorUserAccessBean> toRemove = new ArrayList<>();
				EditorUserAccessBean updateUser = null;
				for (int i = 0; i < userListAl.size(); i++)
				{

					EditorUserAccessBean editorUserAccessBean = userListAl.get(i);
					if (editorUserAccessBean==null || editorUserAccessBean.getUser()==null) continue;

					if(editorUserAccessBean.getUser().getUserId() == user.getUserId())
					{
						updateUser = editorUserAccessBean;
						toRemove.add(editorUserAccessBean);
					}
					else
					{
						int editorNotifyTime = Tools.getIntValue(Constants.getString("editorNotifyTime"), 10);
						long now = Tools.getNow();
						long userAccessTime = editorUserAccessBean.getDatumPoslednejAktivity();
						long rozdiel = (now-userAccessTime)/1000;
						if(rozdiel > (2*editorNotifyTime))
							toRemove.add(editorUserAccessBean);
					}
				}

				// vymaz uzivatelov, ktory uz needituju stranku
				if(toRemove.size() > 0)
					userListAl.removeAll(toRemove);

				if(updateUser == null)
				{
				   // uzivatel nieje v zozname - pridaj ho
					userListAl.add(EditorDB.setEditorUserAccesBean(UsersDB.getUser(user.getUserId())));
				}
				else
				{
				   //uzivatel edituje stranku - aktualizuj cas
					updateUser.setDatumPoslednejAktivity(Tools.getNow());
					userListAl.add(updateUser);
				}

				userList.put(id, userListAl);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		Constants.getServletContext().setAttribute("userAccessList", userList);
	}

	/**
	 * vrati zoznam uzivatelov, ktory prave edituju stranku
	 * @param userId
	 * @return
	 */
	public static List<EditorUserAccessBean> getAllEditorUsers(int docId, int userId, Map<Integer, List<EditorUserAccessBean>> userList)
	{
		List<EditorUserAccessBean> result = new ArrayList<>();
		List<EditorUserAccessBean> editUsersList = null;

		if(userList != null && userList.size() > 0)
			editUsersList = userList.get(Integer.valueOf(docId));
		else
			return null;
		if(editUsersList != null)
		{
			for (EditorUserAccessBean editorUserAccessBean : editUsersList)
			{
				if(editorUserAccessBean!=null && editorUserAccessBean.getUser()!=null && editorUserAccessBean.getUser().getUserId() != userId)
				{
					result.add(editorUserAccessBean);
				}
			}
		}
		else
		{
			return null;
		}

		return result;
	}

	public static void sendApproveRequestEmail(int historyId, Map<Integer, UserDetails> approveByTable, int senderUserId, String comment, HttpServletRequest request)
	{
		String title = null;
		int docId = -1;
		int groupId = -1;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT title, doc_id, group_id FROM documents_history WHERE history_id=?");
			ps.setInt(1, historyId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				title = DB.getDbString(rs, "title");
				docId = rs.getInt("doc_id");
				groupId = rs.getInt("group_id");
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

		if (title == null || docId < 0 || groupId < 0) return;

		//title = title.toLowerCase();
		//title = DB.internationalToEnglish(title).toLowerCase();

		String approveByUsers = null;
		String approveByUsersEmail = null;
		UserDetails u;
		for (Map.Entry<Integer, UserDetails> entry : approveByTable.entrySet())
		{
			u = entry.getValue();
			if (approveByUsers==null)
			{
				approveByUsers = u.getFullName();
			}
			else
			{
				approveByUsers += ", " + u.getFullName(); //NOSONAR
			}

			if (Tools.isEmail(u.getEmail()))
			{
				if (approveByUsersEmail==null)
				{
					approveByUsersEmail = u.getEmail();
				}
				else
				{
					approveByUsersEmail += "," + u.getEmail(); //NOSONAR
				}
			}
		}

		UserDetails user = UsersDB.getUser(senderUserId);
		if (user == null) return;

		Prop properties = Prop.getInstance(request);
		GroupsDB groupsDB = GroupsDB.getInstance();

		request.getSession().setAttribute("approveByUsers", approveByUsers);
		request.getSession().removeAttribute("pageSavedToPublic");
		String url = Tools.getBaseHref(request) + "/admin/approve.jsp?docid="+docId+"&historyid=" + historyId;

		StringBuilder message = new StringBuilder("<strong>").append(properties.getText("approve.ask")).append(":</strong><br>\n");
		message.append("<strong>").append(title).append("</strong><br>\n");
		message.append(properties.getText("approve.dir")).append(":<br>\n");
		message.append(groupsDB.getPath(groupId)).append("<br><br>\n\n");
		message.append(properties.getText("approve.url")).append(":<br>\n");
		message.append("<a href='").append(url).append("'>").append(url).append("</a><br><br>\n\n");

		if (Tools.isNotEmpty(comment))
		{
			message.append(comment).append("<br><br>\n\n");
		}

		message.append(user.getFullName()).append(" &lt;").append(user.getEmail()).append("&gt;");

		String subject = properties.getText("approve.subject") + ": " + title;

		SendMail.send(user.getFullName(), user.getEmail(), approveByUsersEmail, subject, message.toString(), request);
	}

	/**
	 * Pripravi data_asc pre full text hladanie (ak vkladate do DB priamo - mimo saveEditorForm)
	 * @param data
	 * @param ef
	 * @return
	 */
	public static String getDataAsc(String data, EditorForm ef)
	{
		return getDataAsc(data, ef, false);
	}

	public static String getDataAsc(String data, EditorForm ef, boolean isLucene)
	{
		return getDataAsc(data, ef, isLucene, null);
	}

    /**
     * @Deprecated - use EditorToolsForCore.getDataAsc
     * @param data
     * @param ef
     * @param isLucene
     * @param request
     * @return
     */
	public static String getDataAsc(String data, EditorForm ef, boolean isLucene, HttpServletRequest request)
	{
		return EditorToolsForCore.getDataAsc(data, ef, isLucene, request);
	}

	public static String renderIncludes(DocDetails doc, boolean addInternationalToEnglishSection, HttpServletRequest request)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
		GroupDetails group = groupsDB.getGroup(doc.getGroupId());
		if (temp != null && group != null)
		{
			String lng = Tools.isNotEmpty(group.getLng()) ? group.getLng() : Tools.isNotEmpty(temp.getLng()) ? temp.getLng() : "";
			PageLng.setUserLng(request, null, lng);

			ShowDoc.setRequestData(doc, group, DocDB.getInstance(), groupsDB, request);
			ShowDoc.setRequestData(group, groupsDB, request);
			ShowDoc.setRequestData(temp, request);
		}

		String data = doc.getData();

		return renderIncludes(data, addInternationalToEnglishSection, request);
	}

	public static String renderIncludes(String data, boolean addInternationalToEnglishSection, HttpServletRequest request)
	{
		return EditorToolsForCore.renderIncludes(data, addInternationalToEnglishSection, request);
	}

	/**
	 * Test ci stranku so zadanym docId moze zadany pouzivatel editovat
	 * @param user
	 * @param docId
	 * @return
	 */
	public static boolean isPageEditable(Identity user, int docId)
	{
		if (docId < 1) return true;

		DocDB docDB = DocDB.getInstance();

		DocDetails editForm = docDB.getDoc(docId, -1, false);
		if (editForm == null) return true;

		return isPageEditable(user, new EditorForm(editForm));
	}

	/**
	 * Test ci zadany editForm moze zadany pouzivatel editovat
	 * @param user
	 * @param editForm
	 * @return
	 */
	public static boolean isPageEditable(Identity user, EditorForm editForm)
	{
		if (user.isEnabledItem("menuWebpages")==false) return false;

		if (editForm == null) return true;

		//otestuj, ci mame na tento dokument pristupove prava
		boolean canAccess = GroupsDB.isGroupEditable(user, editForm.getGroupId());

		if (!canAccess)
		{
			//zisti, ci to nie je moja stranka
			StringTokenizer st = new StringTokenizer(user.getEditablePages(), ",");
			String id;
			int i_id;
			while (st.hasMoreTokens())
			{
				id = st.nextToken().trim();
				try
				{
					i_id = Integer.parseInt(id);
					//Logger.println(this,"i_id="+i_id+" doc_id="+doc_id);
					if (i_id == editForm.getDocId())
					{
						canAccess = true;
					}
				}
				catch (Exception ex)
				{

				}
			}
		}

		//kontrola pre slave adresare danej stranky
		if(!canAccess)
		{
			int groupId;
			for (MultigroupMapping mapping : MultigroupMappingDB.getSlaveMappings(editForm.getDocId()))
			{
				int dId = mapping.getDocId();
				groupId = DocDB.getInstance().getBasicDocDetails(dId, true).getGroupId();
				if (groupId > 0) canAccess = GroupsDB.isGroupEditable(user, groupId);
				Logger.debug(EditorDB.class, "testujem pristup pre multigroup stranky [groupId="+groupId+"] canAccess="+canAccess);
				if(canAccess) break;
			}
		}

		if (canAccess && Constants.getBoolean("adminCheckUserGroups"))
		{
			DocDetails doc = new DocDetails();
			doc.setDocId(editForm.getDocId());
			doc.setPasswordProtected(editForm.getPasswordProtectedString());
			 if (DocDB.canAccess(doc, user, true)==false)
			 {
				 canAccess = false;
			 }
		}

		return canAccess;
	}

	/**
	 * Vymaze zo session nepotrebne data po ulozeni stranky
	 * @param request
	 */
	public static void cleanSessionData(HttpServletRequest request)
	{
		cleanSessionData(request.getSession());
	}

	/**
	 * Vymaze zo session nepotrebne data po ulozeni stranky
	 * @param session
	 */
	public static void cleanSessionData(HttpSession session)
	{
		session.removeAttribute("pageSavedToPublic");
		session.removeAttribute("pagePublishDate");
		session.removeAttribute("pageSaved");
		session.removeAttribute("approveByUsers");
		session.removeAttribute("docHistory");
		session.removeAttribute("cssError");
	}

	/**
	 * Nastavi formu virtualPath
	 * @param my_form
	 */
	public static void setVirtualPath(EditorForm my_form)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		String domain = groupsDB.getDomain(my_form.getGroupId());
		if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML && my_form.getVirtualPath().startsWith("javascript:")==false)
		{
			boolean mustGenerateVirtualPath = false;
			if (Tools.isNotEmpty(my_form.getVirtualPath()))
			{
				int actualDocId = DocDB.getDocIdFromURL(my_form.getVirtualPath(), domain);
				if (actualDocId > 0 && actualDocId != my_form.getDocId())
				{
					mustGenerateVirtualPath = true;
					my_form.setVirtualPath("");
				}
			}

			if (mustGenerateVirtualPath || Tools.isEmpty(my_form.getVirtualPath()) || my_form.getVirtualPath().indexOf('/')==-1)
			{
				//nastavime ako treba
				String groupDiskPath = DocDB.getGroupDiskPath(groupsDB.getGroupsAll(), my_form.getGroupId());
				DocDetails doc = new DocDetails();
				doc.setDocId(my_form.getDocId());
				doc.setTitle(my_form.getTitle());
				doc.setNavbar(DB.prepareString(my_form.getNavbar(), 128));
				doc.setVirtualPath(my_form.getVirtualPath());
				doc.setGroupId(my_form.getGroupId());
				String virtualPath = DocDB.getURL(doc, groupDiskPath);
				String koncovka = virtualPath.endsWith("/") ? "/" : ".html";
				String editorPageExtension = Constants.getString("editorPageExtension");

				for (int i=1; i<1000; i++)
				{
					if(virtualPath != null && virtualPath.length() > 255)
					{
						String vpTmp = virtualPath.substring(0, virtualPath.length()-koncovka.length());
						vpTmp = DB.prepareString(vpTmp, 255-koncovka.length())+koncovka;
						virtualPath = vpTmp;
					}

					int allreadyDocId = DocDB.getDocIdFromURL(virtualPath, domain);
					Logger.debug(EditorDB.class, "setVirtualPath: allreadyDocId for virtualPath: "+virtualPath + " ,docid: "+allreadyDocId);
					if (allreadyDocId <= 0 || allreadyDocId==my_form.getDocId())
					{
						break;
					}
					doc.setTitle(my_form.getTitle()+" "+i);
					doc.setNavbar(DB.prepareString(my_form.getNavbar(), 128)+" "+i);

					if ("/".equals(editorPageExtension))
					{
						//nastav cistu, handluje sa to nastavenim title s cislom vyssie
						doc.setVirtualPath("");
					}
					else
					{
						if (my_form.getVirtualPath().endsWith(".html"))
						{
							doc.setVirtualPath(Tools.replace(my_form.getVirtualPath(), ".html", "-" + i + ".html"));
							koncovka = "-" + i + ".html";
						} else if (my_form.getVirtualPath().endsWith("/"))
						{
							doc.setVirtualPath(my_form.getVirtualPath() + i + ".html");
							koncovka = i + ".html";
						} else if (Tools.isEmpty(my_form.getVirtualPath()))
						{
							doc.setVirtualPath(Tools.replace(my_form.getTitle() + ".html", "/", "-"));
							my_form.setVirtualPath(doc.getVirtualPath());
							koncovka = ".html";
						}
					}

					virtualPath = DocDB.getURL(doc, groupDiskPath);
				}

				my_form.setVirtualPath(DocDB.normalizeVirtualPath(virtualPath));

				Logger.println(EditorDB.class, "nastaveny virtual path na:"+virtualPath+";");
			}
			else if ("cloud".equals(Constants.getInstallName()))
			{
				//tiket 15910 - kontrola specialnych znakov v URL
				String cleaned = DocTools.removeCharsDir(DB.internationalToEnglish(my_form.getVirtualPath())).toLowerCase();
				if(!cleaned.equals(my_form.getVirtualPath()))
				{
					my_form.setVirtualPath(DocDB.normalizeVirtualPath(cleaned));
					Logger.println(EditorDB.class, "virtual path upraveny na:"+my_form.getVirtualPath()+";");
				}
			}
		}
	}

	/**
     * @Deprecated - use EditorToolsForCore.removeHtmlTagsKeepLength
	 * Remove from string html tags and keep the length of string
	 */
	public static String removeHtmlTagsKeepLength(String html_text)
	{
		return EditorToolsForCore.removeHtmlTagsKeepLength(html_text);
	}

	/**
     * @Deprecated - use EditorToolsForCore.removeCommandKeepLength
	 * Odstrani z HTML kodu riadiace bloky typu !INCLUDE(...)!, !PARAM(...)!, pricom zachova dlzku retazca
	 */
	public static String removeCommandKeepLength(String html_text, String commandStart, String commandEnd)
	{
		return EditorToolsForCore.removeCommandKeepLength(html_text, commandStart, commandEnd);
	}

	/**
	 * Skontroluje a nastavi default docid adresara (ak je neplatne alebo nenastavene)
	 * @param groupId
	 * @param docId
	 */
	public static void setDefaultDocId(int groupId, int docId)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();

		//ak je to prva stranka v adresari a adresar nema defaultDoc, nastav
		GroupDetails group = groupsDB.getGroup(groupId);
		if (group != null)
		{
			DocDetails doc = docDB.getBasicDocDetails(group.getDefaultDocId(), false);

			if ((group.getDefaultDocId() < 1 && group.getNavbar().indexOf("<a") == -1) || doc == null)
			{
				group.setDefaultDocId(docId);
				groupsDB.setGroup(group);
			}
		}
	}

	public static void saveDocAttrs(int docId, Connection db_conn, HttpServletRequest request)
	{
		//ulozenie atributov stranky
		if ("true".equals(request.getParameter("saveDocAtrs")))
		{
			try
			{
				//najskor vymazeme
				PreparedStatement ps = db_conn.prepareStatement("DELETE FROM doc_atr WHERE doc_id=?");
				ps.setInt(1, docId);
				ps.execute();
				ps.close();

				//nainsertujeme
				Enumeration<String> params = request.getParameterNames();
				String name;
				String value;
				int atrId;
				AtrBean atr;

				//najskor si najdeme zoznam skupin atributov, kde je nieco vyplnene
				//je to kvoli tomu, ze pre skupinu musim vyplnit vsetky atributy skupiny
				Map<String, Byte> atrUsedGroups = new Hashtable<>();
				Byte bValue = (byte)1;
				while (params.hasMoreElements())
				{
					try
					{
						name = params.nextElement();
						if (name!=null && name.startsWith("atr_"))
						{
							atrId = Integer.parseInt(name.substring(4));
							value = request.getParameter(name);
							if (Tools.isNotEmpty(value))
							{
								atr = AtrDB.getAtrDef(atrId, request);
								if (atr != null && atrUsedGroups.get(atr.getAtrGroup())==null)
								{
									atrUsedGroups.put(atr.getAtrGroup(), bValue);
								}
							}
						}
					}
					catch (Exception ex2)
					{
						sk.iway.iwcm.Logger.error(ex2);
					}
				}
				params = request.getParameterNames();
				while (params.hasMoreElements())
				{
					try
					{
						name = params.nextElement();
						if (name!=null && name.startsWith("atr_"))
						{
							atrId = Integer.parseInt(name.substring(4));
							value = request.getParameter(name);
							atr = AtrDB.getAtrDef(atrId, request);
							if (atr != null && atrUsedGroups.get(atr.getAtrGroup())!=null)
							{
								if (value == null) value = "";
								ps = db_conn.prepareStatement("INSERT INTO doc_atr (doc_id, atr_id, value_string, value_int, value_bool) VALUES (?, ?, ?, ?, ?)");
								ps.setInt(1, docId);
								ps.setInt(2, atrId);
								if (atr.getAtrType()==AtrDB.TYPE_INT)
								{
									ps.setString(3, null);
									ps.setInt(4, Tools.getIntValue(value, 0));
									ps.setNull(5, Types.INTEGER);
								}
								else if (atr.getAtrType()==AtrDB.TYPE_DOUBLE)
								{
									ps.setString(3, null);
									ps.setDouble(4, Tools.getDoubleValue(value, 0));
									ps.setNull(5, Types.INTEGER);
								}
								else if (atr.getAtrType()==AtrDB.TYPE_BOOL)
								{
									ps.setString(3, null);
									ps.setNull(4, Types.INTEGER);
									if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value))
									{
										ps.setBoolean(5, true);
									}
									else
									{
										ps.setBoolean(5, false);
									}
								}
								else
								{
									ps.setString(3, value);
									ps.setNull(4, Types.INTEGER);
									ps.setNull(5, Types.INTEGER);
								}
								ps.execute();
								ps.close();
							}
						}
					}
					catch (Exception ex2)
					{
						sk.iway.iwcm.Logger.error(ex2);
					}
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}

	private static String getBaseCssPath(TemplateDetails temp, String domainName)
	{
		String baseCssPath = "/css/page.css"; //NOSONAR
		if (temp != null)
		{
			baseCssPath = temp.getBaseCssPath();
		}
		if (Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(domainName))
		{
			// baseCssPath = Tools.replace(baseCssPath, "/css/",
			// "/css/"+MultiDomainFilter.getDomainAlias(editForm.getDomainName())+"/");
			baseCssPath = MultiDomainFilter.rewriteUrlToLocal(baseCssPath, MultiDomainFilter.getDomainAlias(domainName));
		}
		// Logger.println(this,"base_css_link="+baseCssPath+" temp="+temp.getBaseCssPath());
		return baseCssPath;
	}

	private static String getTempCssLink(TemplateDetails temp, String domainName)
	{
		String tempCssLink = null;
		if (temp != null && temp.getCss() != null && temp.getCss().length() > 1)
		{
			tempCssLink = temp.getCss();
			if (Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(domainName))
			{
				String multidomainAlias = MultiDomainFilter.getDomainAlias(domainName);
				if (Tools.isNotEmpty(multidomainAlias)) {
					tempCssLink = Tools.replace(tempCssLink, "/css/", "/css/"
					+ multidomainAlias + "/");
				}
			}
		}
		return tempCssLink;
	}

	/**
	 * Vrati CSS styl pre editor (ajax form)
	 * @param request
	 * @return
	 */
	public static String getEditorCssStyle(HttpServletRequest request)
	{
		String cssStyle = (String)request.getAttribute("base_css_link");
		String cssStyle2 = (String)request.getAttribute("css_link");

		if (cssStyle == null)
		{
			String domainName = DocDB.getDomain(request);

			//este nebol nastaveny, asi sme inline editacia, skus vydumat
			TemplateDetails temp = (TemplateDetails)request.getAttribute("templateDetails");
			if (temp != null)
			{
				cssStyle = getBaseCssPath(temp, domainName);
			}
			if (cssStyle2 == null)
			{
				cssStyle2 = getTempCssLink(temp, domainName);
			}
		}

		if (Tools.isNotEmpty(cssStyle2))
			cssStyle +=","+cssStyle2;

		IwcmFile iwf = new IwcmFile(sk.iway.iwcm.Tools.getRealPath("/css/editor.css"));

		if (iwf.exists())
			cssStyle +=","+request.getContextPath()+"/css/editor.css";

		cssStyle = Tools.replace(cssStyle, "\n", ",");
		cssStyle = Tools.replace(cssStyle, "\r", "");

		return cssStyle;
	}

	/*private static void publishEvent(Class<? extends ApplicationEvent> clazz, EditorForm editorForm) {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean == null) return;
		ApplicationContext context = requestBean.getSpringContext();

		if (context != null) {
			try {
				Constructor<? extends ApplicationEvent> constructor = clazz.getConstructor(Object.class, EditorForm.class);
				ApplicationEvent event = constructor.newInstance(context, editorForm);
				context.publishEvent(event);

			} catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}*/

}