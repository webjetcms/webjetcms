package sk.iway.iwcm.editor.rest;

import java.net.URI;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.HistoryDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;
import springfox.documentation.annotations.ApiIgnore;

// LPA - pridal som ApiIgnore, pretoze sa SwaggerUI zacyklilo na saveDoc na EditorForme. Podozrievam triedu Media.
@ApiIgnore
@RestController
@RequestMapping(path = "/admin/rest/document")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
@SuppressWarnings({"rawtypes", "unchecked"})
public class EditorRestController
{
	@Autowired
	private HttpServletRequest request;

	@PostMapping(path = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity saveDoc(EditorForm editorForm)
	{
		List<String> warnings = new ArrayList<>();
		DebugTimer dt = new DebugTimer("EditorRestController.saveDoc");

		SaveDocResponseObject responseObject = new SaveDocResponseObject();
		Prop prop = Prop.getInstance(request);
		HttpSession session = request.getSession();

		if (session == null)
		{
			Logger.debug(EditorRestController.class, String.format("DocId: %d, error: %s", editorForm.getDocId(), prop.getText("error.userNotLogged")));
			return new ResponseEntity(prop.getText("error.userNotLogged"), HttpStatus.BAD_REQUEST);
		}

		if (!hasData(editorForm))
		{
			Logger.debug(EditorRestController.class, String.format("DocId: %d, error: %s", editorForm.getDocId(), prop.getText("editor.ajax.save.error.empty")));
			warnings.add(prop.getText("editor.ajax.save.error.empty"));
			//return new ResponseEntity(prop.getText("editor.ajax.save.error.empty"), HttpStatus.BAD_REQUEST);
		}

		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		if (user == null || !user.isAdmin())
		{
			Logger.debug(EditorRestController.class, String.format("DocId: %d, error: %s", editorForm.getDocId(), prop.getText("error.userNotLogged")));
			return new ResponseEntity(prop.getText("error.userNotLogged"), HttpStatus.BAD_REQUEST);
		}

		int authorId = user.getUserId();
		editorForm.setAuthorId(authorId);

		DocDetails docOrig = DocDB.getInstance().getBasicDocDetails(editorForm.getDocId(), false);

		if (!canAccess(user, editorForm)) {
			Logger.debug(EditorRestController.class, String.format("DocId: %d, error: %s", editorForm.getDocId(), prop.getText("editor.save.accessError")));

			responseObject.setAjaxSaveFormPermDenied(true);
			return new ResponseEntity(responseObject, HttpStatus.CONFLICT);
		}

		boolean refreshMenu = refreshMenu(editorForm, docOrig, user);

		//ak chcem zachovat sort priority a ukladam slave clanok, potrebujem zachovat sort priority mastra
		multiGroupKeepSortPriority(editorForm);

		dt.diff("before saveEditorForm");
		int historyId = EditorDB.saveEditorForm(editorForm, request);
		dt.diff("after saveEditorForm");

		copyMediaFromDocId(editorForm);
		setLastDocId(editorForm, historyId);

		session.setAttribute("editor.lastDocId", Integer.valueOf(editorForm.getLastDocId()));

		//priradim docasne ulozenym mediam, spravne media_fk_id a media_fk_table_name podla docId
		assignDocIdToMedia(editorForm, historyId, user, prop);

		boolean needRefresh = handleSlaves(editorForm);
		if (needRefresh) refreshMenu = true;

		request.setAttribute("editorForm", editorForm);
		//session.setAttribute("editorForm", editorForm);

		dt.diff("done");

		// ci sa ma refreshovat lavy strom
		responseObject.setAjaxSaveFormRefreshLeft(refreshMenu);
		//docId webstranky, ma vyznam hlavne pri novych dokumentoch, kedy sa nastavuje
		responseObject.setAjaxSaveFormDocId(editorForm.getDocId());
		//virtualna cesta webstranky, ma vyznam hlavne pri novych dokumentoch, kedy sa nastavuje
		responseObject.setAjaxSaveFormVirtualPath(editorForm.getVirtualPath());
		//flag zobrazovania, ma sa zobrazovat?, ma vyznam hlavne pri novych dokumentoch, ktore sa maju publikovat neskor
		responseObject.setAjaxSaveFormAvailable(editorForm.isAvailable());
		//docId webstranky (slave) na ktoru bolo kliknute v strome; ak sa jedna o master clanok, lastDocId = docId. Sluzi k tomu aby
		//sa v lavom strome nezvyraznil master, ale slave
		responseObject.setAjaxSaveFormLastDocId(editorForm.getLastDocId());

		responseObject.setAjaxSaveFormWarnings(warnings);

		return new ResponseEntity(responseObject, HttpStatus.OK);
	}

	@GetMapping(path = "/{docId}/{historyId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<EditorForm> getDocHistory(@PathVariable("historyId") Integer historyId, @PathVariable("docId") Integer docId) {
		return getDoc(null, docId, historyId);
	}

	@GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<EditorForm> getDoc(String url)
	{
		if (Tools.isNotEmpty(url)) {
			url = url.trim();
		}

		if (Tools.isNotEmpty(url) && (url.startsWith("/") || url.startsWith("http") || Tools.getIntValue(url, -6565987) == -6565987))
		{
			if (url.toLowerCase().startsWith("http") == false)
			{
				url = "http://" + DocDB.getDomain(request) + url;
			}

			try
			{
				URI urlObj = new URI(url);
				String domain = urlObj.getHost();
				String path = urlObj.getPath();
				int docId = DocDB.getDocIdFromURL(path, domain);
				if (docId > 0)
				{
					return getDoc(null, docId, null);
				}
			}
			catch (Exception e)
			{
			}
		}

		return new ResponseEntity(String.format("Page with url: %s not found", url), HttpStatus.BAD_REQUEST);
	}

	@GetMapping(path = "/{docId}")
	public ResponseEntity<EditorForm> getDoc(Integer groupId, @PathVariable("docId") Integer docId, Integer historyId)
	{
		DebugTimer dt = new DebugTimer("EditorRestController.getDoc");
		Prop prop = Prop.getInstance(request);

		HttpSession session = request.getSession();
		if (session == null)
		{
			Logger.debug(EditorRestController.class, String.format("Error: %s", prop.getText("error.userNotLogged")));
			return new ResponseEntity(prop.getText("error.userNotLogged"), HttpStatus.BAD_REQUEST);
		}

		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		if (user == null || !user.isAdmin())
		{
			Logger.debug(EditorRestController.class, String.format("Error: %s", prop.getText("error.userNotLogged")));
			return new ResponseEntity(prop.getText("error.userNotLogged"), HttpStatus.BAD_REQUEST);
		}

		docId = correctDocId(docId, historyId);
		groupId = correctGroupId(groupId);

		// vymazem docasne ulozene media (uzivatel neulozil novu stranku po
		deleteTempMedia(docId, user);

		dt.diff("load editor form - start");
		// ziskajme si editorForm
		EditorForm editorForm = EditorDB.getEditorForm(request, getIntValue(docId, -1), getIntValue(historyId, -1), getIntValue(groupId, -1));
		dt.diff("load editor form - end");

		if (editorForm == null)
		{
			return new ResponseEntity("Admin error - editor form null", HttpStatus.BAD_REQUEST);
		}

		// otestuj, ci mame na tento dokument pristupove prava
		if (!canAccess(user, editorForm.toDocDetails())) {
			Logger.debug(EditorRestController.class, String.format("DocId: %d, error: %s", editorForm.getDocId(), prop.getText("editor.save.accessError")));
			return new ResponseEntity(prop.getText("editor.permsDenied"), HttpStatus.CONFLICT);
		}

		// lebo mozeme prist z laveho stromu, a groupId je podla session
		setSessionGroupId(session, editorForm.getGroupId());
		setSessionDomainName(session, editorForm.getDomainName());
		session.setAttribute("editorForm", editorForm);

		// updatuje statistiku dlzky prihlasenia admina
		StatDB.addAdmin(request);

		// aktualizujem zoznam uzivatelov, ktory prave edituju stranku
		EditorDB.updateUserAccessList(docId, user);

		if (editorForm.getTempId() > 0)
		{
			//nastavenie prefixu klucov podla skupiny sablon
			TemplateDetails temp = TemplatesDB.getInstance().getTemplate(editorForm.getTempId());
			if (temp != null && temp.getTemplatesGroupId()!=null && temp.getTemplatesGroupId().longValue() > 0) {
				TemplatesGroupBean tgb = TemplatesGroupDB.getInstance().getById(temp.getTemplatesGroupId());
				if (tgb != null && Tools.isNotEmpty(tgb.getKeyPrefix())) {
					RequestBean.addTextKeyPrefix(tgb.getKeyPrefix(), false);
				}
			}

			RequestBean.addTextKeyPrefix("temp-"+editorForm.getTempId(), false);
		}

		dt.diff("done");

		return new ResponseEntity(editorForm, HttpStatus.OK);
	}

	@DeleteMapping(path = "/history/{historyId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity deleteDocHistory(@PathVariable int historyId) {
		boolean result = false;

		if (historyId > 0)
		{
			HistoryDB historyDB = new HistoryDB(DBPool.getDBName(request));
			result = historyDB.deleteHistory(historyId);
		}

		return ResponseEntity.ok(result);
	}

	private Integer correctDocId(Integer docId, Integer historyId) {
		if (historyId != null && docId == null) {
			return getIntegerValue(request.getAttribute("docid"), null);
		}

		return docId;
	}

	private Integer correctGroupId(Integer groupId) {
		if (groupId == null && request.getSession().getAttribute(Constants.SESSION_GROUP_ID) != null)
		{
			return getIntegerValue(request.getSession().getAttribute(Constants.SESSION_GROUP_ID), null);
		}

		// groupId je potrebne pri vytvarani novej web stránky
		if (groupId == null) {
			return Constants.getInt("rootGroupId");
		}

		return groupId;
	}

	private void deleteTempMedia(Integer docId, Identity user){
		// vytvoreni medii)
		if (docId == null) {
			try {
				MediaDB mediaDB = new MediaDB();
				List<Media> mediaList = MediaDB.getMedia(request.getSession(), "documents_temp", user.getUserId(), null, 0, false);

				// ak som nasiel take media, tak im priradim spravne media_fk_id a
				// media_fk_table_name
				if (mediaList != null && mediaList.size() > 0) {
					for (Media media : mediaList) {
						mediaDB.delete(media);
					}
				}
			} catch (Exception e) {
				Logger.println(EditorRestController.class, "ERROR: Nastal problem pri mazani docasne ulozenych medii");
			}
		}
	}

	private void setSessionGroupId(HttpSession session, int groupId){
		if (groupId > 0) {
			// uloz do session groupId, aby ked kliknem na Web Stranky sa rovno
			// otvoril tento adresar
			session.setAttribute(Constants.SESSION_GROUP_ID, Integer.toString(groupId));
		}
	}

	private void setSessionDomainName(HttpSession session, String domainName) {
		if (Tools.isNotEmpty(domainName))
		{
			session.setAttribute("preview.editorDomainName", domainName);
		}
	}

	private void assignDocIdToMedia(EditorForm editorForm, int historyId, Identity user, Prop prop) {
		boolean isNewDoc = editorForm.getDocId() > 0;
		if (isNewDoc && historyId > 0) {
			List<Media> mediaList = MediaDB.getMedia(request.getSession(), "documents_temp", user.getUserId(), null, 0, false);
			//ak som nasiel take media, tak im priradim spravne media_fk_id a media_fk_table_name
			if (mediaList != null && mediaList.size() > 0) {
				for (Media media : mediaList) {
					try {
						media.setMediaFkId(Integer.valueOf(editorForm.getDocId()));
						media.setMediaFkTableName("documents");
						new MediaDB().save(media);
					} catch (Exception e) {
						Logger.println(EditorRestController.class, prop.getText("editor.save.mediaError"));
					}
				}
			}
		}
	}

	private boolean handleSlaves(EditorForm editorForm) {

		String[] otherGroupsParam = request.getParameterValues("otherGroups");
		List<Integer> otherGroups = new ArrayList<>();
		if (otherGroupsParam != null) {
			for(String groupIdString : otherGroupsParam) {
				int groupId = Tools.getIntValue(groupIdString, -1);
				if (groupId > 0) otherGroups.add(Integer.valueOf(groupId));
			}
		}

		boolean redirect = request.getParameter("redirect") != null;

		boolean ret = EditorRestController.handleSlaves(editorForm, otherGroups, redirect, request);
		return ret;
	}

	/**
	 * Vyriesi zapis slave mapovani pri ukladani web stranky
	 * @param editorForm
	 * @param otherGroups
	 * @param redirect
	 * @param request
	 * @return true, ak je potrebne refreshnut lave menu (pribudla/zmenila sa niekde polozka)
	 */
	public static boolean handleSlaves(EditorForm editorForm, List<Integer> otherGroups, boolean redirect, HttpServletRequest request) {

		boolean refreshMenu = false;

		DocDB docDB = DocDB.getInstance();

		Map<Integer, Integer> groupMapping = new HashMap<>();
		List<Integer> toDelete = new ArrayList<>();

		if (otherGroups != null)
		{
			for(Integer groupId : otherGroups)
			{
				if (groupId == null) continue;
				if(groupId.intValue() > 0)
					groupMapping.put(groupId, -1);
			}
		}
		for(Integer docId : MultigroupMappingDB.getSlaveDocIds(editorForm.getDocId()))
		{
			DocDetails doc1 = docDB.getBasicDocDetails(docId, true);
			if(groupMapping.get(doc1.getGroupId()) != null)
			{
				groupMapping.put(doc1.getGroupId(), docId);
			}
			else
			{
				// nejaky adresar bol odstraneny, refreshni lavy strom
				refreshMenu = true;
				toDelete.add(docId);
			}
		}

		//multikategorie (praca so slave clankami)
		DocDetails masterDoc = docDB.getDoc(editorForm.getDocId(), -1, false);

		//zmaz zapisane hodnoty mapovania (nie stranky)
		MultigroupMappingDB.deleteSlaves(editorForm.getDocId());

		if(redirect)
		{
			masterDoc.setExternalLink(masterDoc.getVirtualPath());
		}

		boolean multiGroupkeepSortPriority = Constants.getBoolean("multiGroupKeepSortPriority");
		int formSortPriority = editorForm.getSortPriority();

		for(Map.Entry<Integer, Integer> me : groupMapping.entrySet())
		{
			Integer groupId = me.getKey();
			Integer docId = me.getValue();
			if(docId != null)
			{
				Logger.debug(EditorRestController.class, "Saving slave doc: "+docId+" to group "+groupId);
				if(docId < 0)
				{
					masterDoc.setVirtualPath("");
				}
				else
				{
					masterDoc.setVirtualPath(docDB.getBasicDocDetails(docId, true).getVirtualPath());
				}

				//ak chcem zachovat sort priority
				if(multiGroupkeepSortPriority)
				{
					if(editorForm.getLastDocId() != docId.intValue())
					{
						multiGroupKeepSortPriority(masterDoc, docId);
					}
					else
					{
						//ak ukladam slave, beriem sort priority z formu
						masterDoc.setSortPriority(formSortPriority);
					}
				}
				masterDoc.setDocId(docId);
				masterDoc.setGroupId(groupId);
				DocDB.saveDoc(masterDoc);
				EditorDB.setDefaultDocId(masterDoc.getGroupId(), masterDoc.getDocId());
				//POZOR: teraz uz mame v masterDoc.getDocId hodnotu ulozeneho SLAVE
				MultigroupMappingDB.newMultigroupMapping(masterDoc.getDocId(), editorForm.getDocId(), redirect);
				docDB.updateInternalCaches(masterDoc.getDocId());

				//uloz AtrDB
				Connection connection = null;
				try
				{
					connection = DBPool.getConnection();

					EditorDB.saveDocAttrs(docId, connection, request);

					connection.close();
					connection = null;
				}
				catch (Exception sqle)
				{
					Logger.error(EditorDB.class,"DB spadlo spojenie");
					sk.iway.iwcm.Logger.error(sqle);
					request.getSession().setAttribute("cssError", Prop.getTxt("editor.ajax.save.error"));
				}
				finally{
					try{
						if (connection != null) connection.close();
					}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
				}

				if(docId < 0)
				{
					// nejaky adresar bol pridany, refreshni lavy strom
					refreshMenu = true;
				}
			}
		}

		// odstranime DocDetails pre zmazane slave mappingy
		for(Integer docId : toDelete)
		{
			DocDB.deleteDoc(docId, request);
		}

		//ak sme nieco zmazali refreshneme DocDB
		if(toDelete.size() > 0)	{
			DocDB.getInstance(true);
		}
		else {
			docDB.forceRefreshMasterSlaveMappings(); //refreshnem master-slave mapy
		}

		return refreshMenu;
	}

	private void copyMediaFromDocId(EditorForm editorForm) {
		boolean isNewDoc = editorForm.getDocId() > 0;
		int copyMediaFromDocId = editorForm.getCopyMediaFrom();

		if(isNewDoc && copyMediaFromDocId > 0)
		{
			Media media = null;
			for(Media mediaOld: MediaDB.getMedia(request.getSession(), "documents", copyMediaFromDocId, null, 0, false))
			{
				media = new Media();
				media.setLastUpdate(mediaOld.getLastUpdate());
				media.setMediaFkId(editorForm.getDocId());
				media.setMediaFkTableName(mediaOld.getMediaFkTableName());
				media.setMediaGroup(mediaOld.getMediaGroup());
				media.setMediaInfoCz(mediaOld.getMediaInfoCz());
				media.setMediaInfoDe(mediaOld.getMediaInfoDe());
				media.setMediaInfoEn(mediaOld.getMediaInfoEn());
				media.setMediaInfoSk(mediaOld.getMediaInfoSk());
				media.setMediaLink(mediaOld.getMediaLink());
				media.setMediaSortOrder(mediaOld.getMediaSortOrder());
				media.setMediaThumbLink(mediaOld.getMediaThumbLink());
				media.setMediaTitleCz(mediaOld.getMediaTitleCz());
				media.setMediaTitleSk(mediaOld.getMediaTitleSk());
				media.setMediaTitleEn(mediaOld.getMediaTitleEn());
				media.setMediaTitleDe(mediaOld.getMediaTitleDe());
				new MediaDB().save(media);
			}
		}
	}

	private void setLastDocId(EditorForm editorForm, int history_id) {
		boolean isNewDoc = editorForm.getDocId() > 0;
		if(isNewDoc && history_id > 0)
		{
			//nastav hodnoty, aby sa nam dobre refreshol lavy panel
			editorForm.setLastDocId(editorForm.getDocId());
			request.getSession().setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(editorForm.getGroupId()));
		}
	}

	public boolean hasData(EditorForm ef) {
		String formData = Tools.getStringValue(ef.getData(), "");
		Logger.println(EditorRestController.class, String.format("docId: %d, dataSize: %d", ef.getDocId() , formData.length()));
		return formData.length() > 0;
	}

	private boolean canAccess(Identity user, DocDetails orig) {
		return canAccess(user, new EditorForm(orig));
	}

	private boolean canAccess(Identity user, EditorForm ef) {
		String saveDocActionDontVeriryUser = Tools.getStringValue((String) request.getAttribute("saveDocActionDontVeriryUser"), "");

		if ("1".equalsIgnoreCase(saveDocActionDontVeriryUser)) {
			return true;
		}

		return EditorDB.isPageEditable(user, ef);
	}

	private boolean refreshMenu(EditorForm ef, DocDetails docOrig, Identity user) {
		if (ef.getDocId() < 1)
		{
			return true;
		}

		if (docOrig != null)
		{
			if (docOrig.getTitle().equals(ef.getTitle()) == false) return true;
			if (docOrig.getExternalLink().equals(ef.getExternalLink()) == false) return true;
			if (docOrig.getVirtualPath().equals(ef.getVirtualPath()) == false) return true;
			if (docOrig.isAvailable() != ef.isAvailable()) return true;
			if (docOrig.getGroupId() != ef.getGroupId()) return true;
			if (docOrig.getSortPriority() != ef.getSortPriority()) return true;
		}

		if (ef.getData().indexOf("!INCLUDE(") != -1) return true;

		return false;
	}

	public static void multiGroupKeepSortPriority(DocDetails doc, int slaveDocId) {
		DocDB docDB = DocDB.getInstance();
		DocDetails slaveDoc = docDB.getBasicDocDetails(slaveDocId, false);
		if(slaveDoc != null) {
			doc.setSortPriority(slaveDoc.getSortPriority());
		}
	}

	public void multiGroupKeepSortPriority(EditorForm ef) {
		boolean isNewDoc = ef.getDocId() > 0;
		boolean multiGroupkeepSortPriority = Constants.getBoolean("multiGroupKeepSortPriority");

		if(multiGroupkeepSortPriority && isNewDoc == false && ef.getLastDocId() != ef.getDocId())
		{
			DocDetails dd = DocDB.getInstance().getBasicDocDetails(ef.getDocId(), false);
			if (dd != null) {
				ef.setSortPriority(dd.getSortPriority());
			}
		}
	}

	public static Integer getIntegerValue(Object value, Integer defaultValue)
	{
		Integer ret = defaultValue;
		try
		{
			if (value!=null)
			{
				String val = (String) value;
				ret = Integer.parseInt(val.trim());
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	public static int getIntValue(Integer value, int defaultValue)
	{
		int ret = defaultValue;
		try
		{
			if (value != null)
			{
				ret = value.intValue();
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}
}