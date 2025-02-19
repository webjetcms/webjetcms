package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMapping;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefEntity;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrEntity;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrRepository;
import sk.iway.iwcm.editor.DocNoteBean;
import sk.iway.iwcm.editor.DocNoteDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.rest.DocDetailsToDocHistoryMapper;
import sk.iway.iwcm.editor.util.EditorUtils;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.UrlRedirectDB;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

/**
 * Servis pre ukladanie web stranok (tabulka documents a documents_history).
 *
 * NEPOUZIVAT standardne je potrebne pouzit EditorFacade, ktora zapuzdruje dalsie operacie pri ulozeni
 *
 * Servis je RequestScope aby pre kazdy request boli objeky oddelene (thread safe)
 */
@Service
@RequestScope
public class EditorService {

	//repozitare
	private DocDetailsRepository docRepo;
	private DocHistoryRepository historyRepo;
	private DocAtrRepository docAtrRepository;

	//service
	private MediaService mediaService;
	private ApproveService approveService;

	//autowired
	private HttpServletRequest request;

	//privatne objekty
	private Identity currentUser;
	private Prop prop;
	private GroupsDB groupsDB;
	private DocDB docDB;
	private DocNoteDB docNoteDB;
	private long now;

	//list upozorneni
    private List<NotifyBean> notify;

	/** atributy nastavene pocas ukladania **/

	//nastavene na true ak je potrebne vyvolat obnovenie stromovej struktury/datatabulky po ulozeni
	private boolean forceReload = false;
	//ak je nastavene casove publikovanie tu bude po ulozeni hodnota zaciatku publikovania
	private Long publihStart = null;
	//po ulozeni nastavene na true, ak sa stranka vypublikovala pre zobrazenie navstevnikmi web sidla
	private boolean pageSavedToPublic = false;
	//po ulozeni nastavene na true, ak sa stranka ulozila ako pracovna kopia
	private boolean pageSavedAsWorkVersion = false;

	@Autowired
    public EditorService(DocDetailsRepository docRepo, DocHistoryRepository historyRepo, DocAtrRepository docAtrRepository,
			MediaService mediaService, ApproveService approveService, HttpServletRequest request) {
		this.request = request;

		this.docRepo = docRepo;
		this.historyRepo = historyRepo;
		this.docAtrRepository = docAtrRepository;

		this.mediaService = mediaService;
		this.approveService = approveService;

		this.currentUser = UsersDB.getCurrentUser(request);
		this.prop = Prop.getInstance(request);
		this.groupsDB = GroupsDB.getInstance();
		this.docDB = DocDB.getInstance();
		this.docNoteDB = DocNoteDB.getInstance();
		this.now = Tools.getNow();
		this.forceReload = false;

		this.notify = new ArrayList<>();
	}

	public EditorService() {}

	/**
	 * Vrati DocDetails objekt podla zadaneho docId alebo historyId (ak je zadane)
	 *
	 * @param docId
	 * @param historyId
	 * @return
	 */
    public DocDetails getDoc(int docId, int historyId) {

        DocDetails doc = null;
        DocHistory history = null;

        if (historyId <= 0) {
			doc = docRepo.findById(docId);
        } else {
            Optional<DocHistory> historyOptional = historyRepo.findById(Long.valueOf(historyId));
			if (historyOptional.isPresent()) {
				history = historyOptional.get();
				doc = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(history);
			}
        }

        if (historyId > 0 && history != null && doc != null) {
			//prenes hodnoty do DocDetails objektu z History (je to takto kvoli spatnej kompatibilite)
            if (history.getApprovedBy()!=null) doc.setHistoryApprovedBy(history.getApprovedBy());
            if (history.getDisapprovedBy()!=null) doc.setHistoryDisapprovedBy(history.getDisapprovedBy());
            if (history.getActual()!=null) doc.setHistoryActual(history.getActual());
        }

        if (historyId > -1 && history != null && doc != null) {
            doc.setPublicable(Boolean.TRUE.equals(history.getPublicable()));
			DocEditorFields def = doc.getEditorFields();
			if (def == null) {
				def = new DocEditorFields();
				doc.setEditorFields(def);
			}
			def.setPublishAfterStart(doc.isPublicable());
			def.setDisableAfterEnd(history.isDisableAfterEnd());
        }

        return doc;
    }

	/**
	 * Ulozi DocDetials do databazy vratane vsetkych akcii spojenych s ulozenim web stranky (schvalovanie, nastavenie adresara...)
	 * @param editedDoc
	 * @return historyId alebo hodnotu < 1 ak nastala chyba
	 */
	public int saveEditedDoc(DocDetails editedDoc) {
		DebugTimer dt = new DebugTimer("EditorService.saveEditedDoc");

		//first update savedate and author, check permissions need this
		editedDoc.setDateCreated(now);
		editedDoc.setAuthorId(currentUser.getUserId());

		boolean isNewPage = false;
		int historyId = -1;
		boolean wasApproved = false;

		(new WebjetEvent<>(editedDoc, WebjetEventType.ON_START)).publishEvent();

		//check that data are loaded
		if (editedDoc.getData()==null || WebpagesService.DATA_NOT_LOADED.equals(editedDoc.getData().trim()) || ("<p>"+WebpagesService.DATA_NOT_LOADED+"</p>").equals(editedDoc.getData().trim())) {
			throw new RuntimeException(prop.getText("components.docman.errorLoadingData")+" "+editedDoc.getDocId());
		}

		//over, ci sa nezmenil adresar, ak ano, musis spravit reload stromu
		if(editedDoc.getDocId() > 0) {
			int oldGroupId = docRepo.getGroupId(editedDoc.getDocId()).intValue();
			if (oldGroupId > 0 && oldGroupId != editedDoc.getGroupId()) forceReload = true;
		}

		//kontrola prav
		checkPermissions(currentUser, editedDoc, false);

		if(Constants.getBoolean("editorEscapeInvalidCharacters")) {
			editedDoc.setTitle(EditorUtils.escapeInvalidCharacters(editedDoc.getTitle(), prop, notify));
			editedDoc.setData(EditorUtils.escapeInvalidCharacters(editedDoc.getData(), prop, notify));
			editedDoc.setHtmlData(EditorUtils.escapeInvalidCharacters(editedDoc.getHtmlData(), prop, notify));
		}

		//cloud - aby nebolo mozne premenovat Header / Footer
		DocDetails existing = CloudToolsForCore.isPossibleToChangeDoc(editedDoc.getDocId());
		if (existing != null) {
			//nemozeme menit title ani groupid
			editedDoc.setTitle(existing.getTitle());
			editedDoc.setGroupId(existing.getGroupId());
		}

		//kvoli Oracle, on nedokaze mat v DB prazdny string a potom to padalo
		if (Tools.isEmpty(editedDoc.getTitle())) editedDoc.setTitle("new web page");
		if (Tools.isEmpty(editedDoc.getNavbar())) editedDoc.setNavbar(editedDoc.getTitle());
		if (Tools.isEmpty(editedDoc.getData())) editedDoc.setData("<p>&nbsp;</p>");

		//nastav virtual path
		setVirtualPath(editedDoc);
		dt.diff("after virtual path");

		EditorUtils.nonBreakingSpaceReplacement(editedDoc);

		//data nechceme mat ulozene s context linkami (tie nam prida filter, ak treba)
		if (ContextFilter.isRunning(request))
			editedDoc.setData(ContextFilter.removeContextPath(request.getContextPath(), editedDoc.getData()));

		String data = EditorUtils.getCleanBody(editedDoc.getData().trim());
		dt.diff("after getCleanBody");

		String dataAsc = EditorUtils.getDataAsc(data, editedDoc, false, request);
		dt.diff("after getDataAsc");

		editedDoc.setData(data);
		editedDoc.setDataAsc(dataAsc);

		//It's new page ?
		if (editedDoc.getDocId() < 1) {
			isNewPage = true;

			//If new page do not request publish, disable page
			if(!editedDoc.getEditorFields().isRequestPublish()) editedDoc.setAvailable(false);
		}

		// Load approve hash table data
		// If current user is approver, set selfApprover = true
		if (editedDoc.getEditorFields().isRequestPublish()) {
			approveService.loadApproveTables(editedDoc.getGroupId());

			//If approver is needed BUT it's not selfApprove (currentUser isn't approver),
			//publish is NOT allowed (set publish as false) -> because it's gonna waiting for approve
			if(approveService.needApprove() && !approveService.isSelfApproved())
				editedDoc.getEditorFields().setRequestPublish(false);
		}
		dt.diff("after requestPublish");

		DocHistory editedHistory = DocDetailsToDocHistoryMapper.INSTANCE.docDetailsToDocHistory(editedDoc);

		if (isNewPage) {
			//Save (insert) new webpage
			insertWebpage(editedDoc, dt);
			editedHistory.setDocId(editedDoc.getDocId());
		} else {
			//Update exist webpage
			updateWebpage(editedDoc, dt);
		}

		dt.diff("after savedata, webpage Insert/Update");

		if (editedDoc.getEditorFields().isRequestPublish()) {
				pageSavedToPublic = true;
		} else {
			if (editedDoc.isPublishAfterStart()){
				publihStart = Long.valueOf(editedDoc.getPublishStart());
			} else {
				pageSavedAsWorkVersion = true;
			}
		}
		dt.diff("after session set");

		//vypne zapisovanie zaznamov do documents_history tabulky. true - nezapise zaznam do documents_history
		boolean disableHistory = Constants.getBoolean("editorDisableHistory");
		if(disableHistory) {
			Logger.debug(EditorService.class, "Write into documents_history is disabled");
			historyId = 1;
		}

		// If history is not disabled
		if (!disableHistory) {
			// zisti, ci v historii na ten isty datum a cas nema byt nieco vypublikovane
			if (editedDoc.getPublishStartDate() != null && (editedDoc.getPublishStartDate().getTime()+60000)>Tools.getNow()) {
				List<DocHistory> waitingForPublish = historyRepo.findByDocIdAndPublishStartDate(editedDoc.getDocId(), editedDoc.getPublishStartDate());
				if (waitingForPublish != null) {
					for (DocHistory waiting : waitingForPublish) {
						waiting.setPublicable(false);
						waiting.setSyncStatus(1);
						historyRepo.save(waiting);
					}
				}
			}
			dt.diff("after dochist is publicable");

			// DocHistory entity, that represent change of original webpage (DocDetails entity)
			editedHistory.setPublicable(Boolean.valueOf(editedDoc.isPublishAfterStart()));
			editedHistory.setData(data);
			editedHistory.setDataAsc(dataAsc);
			editedHistory.setSaveDate(new Date(now));
			editedHistory.setActual(editedDoc.getEditorFields().isRequestPublish());

			//Set ApprovedBy value, that indicate approve status
			if (approveService.needApprove()) {
				//Need approve
				editedHistory.setApprovedBy(-1);
			} else {
				// Do not need approve
				if(approveService.isSelfApproved()) {
					//Page created by approver (automatic self approved), set approver id
					editedHistory.setApprovedBy(currentUser.getUserId());
					wasApproved = true;
				} else {
					//Not self approved, but do not need approve (probably folder that do not require approve)
					if (editedDoc.getEditorFields().isRequestPublish()) {
						//Publish without approver, soo approver is 0
						editedHistory.setApprovedBy(0);
						wasApproved = true;
					} else {
						//Do not publish, approver is -1
						editedHistory.setApprovedBy(-1);
					}
				}
			}

			if (!editedHistory.getEditorFields().isRequestPublish() && approveService.needApprove())
				editedHistory.setAwaitingApprove("," + approveService.getApproveUserIds() + ",");
			else
				editedHistory.setAwaitingApprove(null);


			//Save edited history
			historyRepo.save(editedHistory);
			dt.diff("after dochistory insert");

			//ziskaj history_id
			historyId = editedHistory.getHistoryId();
			dt.diff("after dochistory get id");

			RequestBean.addAuditValue("historyId", String.valueOf(historyId));

			//Publishing
			if(wasApproved) deleteHistorySaveRecords(editedDoc, editedHistory, historyId, dt);
		}

		/*Odoslanie schvalovani a notifikacii*/
		approveService.sendEmails(editedDoc, historyId);
		dt.diff("after sendApproveNotifyEmail");

		if(isNewPage || wasApproved || disableHistory) {
			//In case of insert, actions are needed because page was saved, even when isn't approved
			// !! without this actions, insert page return 404
			renameVirtualPath(editedDoc, dt);
			refreshCacheObjects(editedDoc, dt);
			refreshTemplates(editedDoc, dt);
		} else if(editedDoc.isPublishAfterStart() || editedDoc.isDisableAfterEnd()) {
			//reload awaiting pages for DocDB
			docDB.readPagesToPublic();
		}

		/*Ulozenie poznamky*/
		saveRedactorNote(editedDoc, historyId);
		dt.diff("after instances");

		/*Finish*/
		dt.diff("done");

		//Publikovanie eventov
		(new WebjetEvent<>(editedDoc, WebjetEventType.AFTER_SAVE)).publishEvent();

		return(historyId);
 	}

	/**
	 * Vrati instanciu noveho DocDetails
	 * @param docId - -1 alebo -DOCID sablony prazdnej stranky
	 * @param group - adresar kde ma vzniknut (podla toho sa nastavi sablona, sort priority...)
	 * @return
	 */
	public DocDetails prepareNewDocForEditor(int docId, GroupDetails group) {
		//ak je to admin
		DocDetails editedDoc = null;
		//-1 je cisty dokument
		if (docId < -1) {
			editedDoc = getDoc(-docId, -1);
			if (editedDoc != null) {
				editedDoc.setDocId(-1);
			}
		}

		if (editedDoc == null) {
			editedDoc = new DocDetails();
			editedDoc.setDocId(-1);
			editedDoc.setGroupId(group.getGroupId());
			editedDoc.setData("<p>&nbsp;</p>");
			editedDoc.setTitle(prop.getText("editor.newDocumentName"));
			editedDoc.setSearchable(true);
			editedDoc.setAvailable(Constants.getBoolean("editorNewDocDefaultAvailableChecked"));
			editedDoc.setShowInMenu(true);
			editedDoc.setSortPriority(10);
		} else {
			//nastav grupu na aktualne vybratu
			editedDoc.setDocId(-1);
			editedDoc.setGroupId(group.getGroupId());
			editedDoc.setTitle(prop.getText("editor.newDocumentName"));
			editedDoc.setNavbar("");
			editedDoc.setVirtualPath("");
			editedDoc.setExternalLink("");
			editedDoc.setEventDateString("");
			editedDoc.setEventTimeString("");
			if (Constants.getBoolean("editorNewDocDefaultAvailableChecked") == false) editedDoc.setAvailable(false);
		}

		editedDoc.setTempId(group.getTempId());
		/*zisti maximalnu prioritu a zvys o 10*/
		editedDoc.setSortPriority(0);
		boolean dirIsEmpty = setSortPriority(editedDoc, group);

		if (dirIsEmpty) {
			editedDoc.setTitle(group.getGroupName());
			editedDoc.setNavbar(group.getNavbarName());
		}

		WebpagesService.processFromEntity(editedDoc, ProcessItemAction.GETONE, request, true);

		return editedDoc;
	}

	/**
	 * Set next available sort priority for doc
	 * @param editedDoc
	 * @param group
	 * @return
	 */
	public boolean setSortPriority(DocDetails editedDoc, GroupDetails group) {
		boolean dirIsEmpty = true;
		int maxPriority = DocDB.getMaxSortPriorityInGroup(group.getGroupId());
		if(maxPriority > 0) {
			editedDoc.setSortPriority(maxPriority);
			dirIsEmpty = false;
		}

		int sortPriorityIncrementDoc = Constants.getInt("sortPriorityIncrementDoc");
		if (Constants.getBoolean("sortPriorityIncremental")) {
			GroupDetails parentGroup = groupsDB.getGroup(group.getParentGroupId());
			if (parentGroup != null && maxPriority < parentGroup.getSortPriority()) {
				//-10 lebo sa nam to o par riadkov nizsie navysi o 10
				maxPriority = parentGroup.getSortPriority() - sortPriorityIncrementDoc;
				editedDoc.setSortPriority(maxPriority);
			}
		}

		editedDoc.setSortPriority(editedDoc.getSortPriority() + sortPriorityIncrementDoc);
		return dirIsEmpty;
	}

	/**
	 * Vrati existujuci DocDetails objekt podla zadaneho historyId/docId.
	 *
	 * Riesi aj problem s MultigroupMaping, kedy nacita udaje master dokumentu.
	 *
	 * @param docId
	 * @param historyId
	 * @return
	 */
	public DocDetails prepareDocForEditor(int docId, int historyId) {
		return prepareDocForEditor(docId, historyId, false);
	}

	/**
	 * Vrati existujuci DocDetails objekt podla zadaneho historyId/docId.
	 * ak je ignoreMultigroupMapping=false Riesi aj problem s MultigroupMaping, kedy nacita udaje master dokumentu.
	 *
	 * @param docId
	 * @param historyId
	 * @param ignoreMultigroupMapping
	 * @return
	 */
	public DocDetails prepareDocForEditor(int docId, int historyId, boolean ignoreMultigroupMapping) {
		int docIdOriginal = docId;

		//moznost nacitat slave data
		//int masterId = (request.getAttribute("keepSlave") != null && "true".equals(request.getAttribute("keepSlave"))) ? 0 : MultigroupMappingDB.getMasterDocId(docId);
		int masterId = MultigroupMappingDB.getMasterDocId(docId);
        if(masterId > 0) docId = masterId;


		DocDetails editedDoc = null;
		if (historyId > 0) {
			editedDoc = getDoc(-1, historyId);
		} else {
			editedDoc = getDoc(docId, -1);
		}

		if (editedDoc == null) {
			return(null);
		} else {
			WebpagesService.processFromEntity(editedDoc, ProcessItemAction.GETONE, request, true);

			//ak nacitavam slave clanok a chcem zachovat sort priority, tak NEnacitavam sort priority mastra
			boolean multiGroupkeepSortPriority = Constants.getBoolean("multiGroupKeepSortPriority");
			if(multiGroupkeepSortPriority && masterId > 0) {
				DocDetails slave = docDB.getBasicDocDetails(docIdOriginal, false);
				if(slave != null) editedDoc.setSortPriority(slave.getSortPriority());
			}
		}

		//ak sa nacital slave dokument, nastav povodne ID, aby to v editore korektne nacitalo
		if (masterId > 0) {
			editedDoc.setDocId(docIdOriginal);

			//keep virtual path
			DocDetails slave = docDB.getBasicDocDetails(docIdOriginal, false);
			if (slave != null) editedDoc.setVirtualPath(slave.getVirtualPath());
		}

		return editedDoc;
	}

	/**
	 * Ulozi novo vytvorenu web stranku do databazy
	 * @param editedDoc
	 * @param requestPublish
	 * @param dt
	 * @param data
	 * @param dataAsc
	 */
	private void insertWebpage(DocDetails editedDoc, DebugTimer dt) {
		//Request publish could be set as false during loadApproveTables action (when author has no right to do this, and approve is needed)
		if (editedDoc.getEditorFields().isRequestPublish() == false) {
			editedDoc.setAvailable(false);
		} else {
			//If user has right, check if publish is in the future
			if (editedDoc.isPublishAfterStart()) {
				//When to publish
				long publishStart = editedDoc.getPublishStart();
				if(publishStart != 0) {
					//Is publish in the future ?
					if (publishStart > now) {
						//For now, cancel publish and availability
						editedDoc.getEditorFields().setRequestPublish(false);
						editedDoc.setAvailable(false);

						//Add note when this is gonna be publish
						//Change data
						String publishDocData = prop.getText("editor.publish.note") + " " + editedDoc.getPublishStartString() + " " + editedDoc.getPublishStartTimeString();
						editedDoc.setData(publishDocData);
						editedDoc.setDataAsc(EditorUtils.getDataAsc(publishDocData, editedDoc, false, request));
					}
				}
			}
		}

		//Unlike update, insert is gonna happen even if user have no rights
		//Reason is to show webpage to user, but page still need to be approve and is marked as red (waiting for approve)
		docSave(editedDoc, true, dt);
	}

	/**
	 * Ulozi do databazy existujucu web stranku
	 * @param editedDoc
	 * @param requestPublish
	 * @param dt
	 */
	private void updateWebpage(DocDetails editedDoc, DebugTimer dt) {
		//premenovanie Groupy ak je stranka defaultna pre Grupu.
		if(GroupsService.canSyncTitle(editedDoc.getDocId(), editedDoc.getGroupId())) {
			DocDB.changeGroupTitle(editedDoc.getGroupId(), editedDoc.getDocId(), editedDoc.getTitle());
			forceReload = true;
		}

		//Request publish could be set as false during loadApproveTables action (when author has no right to do this, and approve is needed)
		if (editedDoc.getEditorFields().isRequestPublish() == false) {
			editedDoc.setAvailable(false);
		} else {
			//If user has right, check if publish is in the future
			if (editedDoc.isPublishAfterStart()) {
				//When to publish
				long publishStart = editedDoc.getPublishStart();
				if(publishStart != 0) {
					//Is publish in the future ?
					if (publishStart > now) {
						//For now, cancel publish and availability
						editedDoc.getEditorFields().setRequestPublish(false);
						editedDoc.setAvailable(false);
					}
				}
			}
		}

		//Unlike insert, update is gonna happen only IF requestPublish = true (sooo user have rights to do that)
		if (editedDoc.getEditorFields().isRequestPublish()) docSave(editedDoc, false, dt);
	}

	/**
	 * Zmazanie web stranky, kontroluje prava pouzivatela, ak nema prava throwne RuntimeException
	 * @param doc
	 * @param publishEvents - true to publish WebjetEvents (default true)
	 * @return
	 */
	@SuppressWarnings("java:S3516")
	public boolean deleteWebpage(DocDetails doc, boolean publishEvents) {
		String result = deleteWebpageLogic(doc.getDocId(), approveService, publishEvents);

		//All good
		if("success".equals(result)) return true;

		//Delete needs to be approved
		if(prop.getText("approveAction.err.cantApprove").equals(result)) {
			StringBuilder approversString = new StringBuilder();
			for(UserDetails approver : approveService.getApprovers()) {
				if(!approversString.isEmpty()) approversString.append(", ");
				approversString.append(approver.getFullName());
			}
			NotifyBean info = new NotifyBean(prop.getText("editor.approve.notifyTitle"), prop.getText("editor.approveDeleteRequestGet")+": "+approversString.toString(), NotifyBean.NotifyType.INFO, 60000);
            addNotify(info);

			return true;
		}

		//Something wrong
		throw new RuntimeException(result);
	}

	/**
	 * Pri funkcii Ulozit rozprazovanu verziu sa v history tabulke kopia pracovne zaznamy, tie pri publikovani zmazeme, ponechame len podla approveHistoryId
	 * @param editedDoc
	 * @param editedHistory
	 * @param historyId
	 * @param dt
	 */
	private void deleteHistorySaveRecords(DocDetails editedDoc, DocHistory editedHistory, int historyId, DebugTimer dt) {
		// zmaz stare dokumenty, ktore nie su schvalene
		List<Integer> historyIds;

		historyIds = historyRepo.findOldHistoryIds(editedHistory.getDocId(), Long.valueOf(historyId), false, currentUser.getUserId());
		dt.diff("after was_approved history_id list");

		if (historyIds.isEmpty()==false) {
			historyRepo.deleteHistoryOnPublish(historyIds, currentUser.getUserId());
			dt.diff("after was_approved delete");
		}

		//nastav aktualne na actual=false
		historyIds = historyRepo.findOldHistoryIds(editedDoc.getDocId(), historyId);

		if (historyIds.isEmpty()==false) historyRepo.updateActualHistory(false, "", historyIds);
		dt.diff("after was_approved");
	}

	/**
	 * Nastavi databazove stlpce root_group_lX s hodnotami ID adresarov na 1-3 urovni
	 * @param groupId
	 * @param editedDoc
	 */
	private void setRootGroupL(int groupId, DocDetails editedDoc) {
		List<GroupDetails> parentGroups = groupsDB.getParentGroups(groupId);
		int[] rootGroupL = new int[3];
		Arrays.fill(rootGroupL, 0);
		int ind = 0;

		for(int i=parentGroups.size()-1; i >= 0; i--) {
			rootGroupL[ind++] = parentGroups.get(i).getGroupId();
			if(ind == 3) break;
		}

		if(editedDoc != null) {
			if(rootGroupL[0] > 0) editedDoc.setRootGroupL1(rootGroupL[0]);
			else editedDoc.setRootGroupL1(null);

			if(rootGroupL[1] > 0) editedDoc.setRootGroupL2(rootGroupL[1]);
			else editedDoc.setRootGroupL2(null);

			if(rootGroupL[2] > 0) editedDoc.setRootGroupL3(rootGroupL[2]);
			else editedDoc.setRootGroupL3(null);
		}
	}

	/**
	 * Ulozi poznamku redaktora (alebo zmaze, ak je prazdna)
	 * @param editedDoc
	 * @param historyId
	 */
	private void saveRedactorNote(DocDetails editedDoc, int historyId) {
		//ulozenie poznamky pre redaktorov k webstranke
		DocNoteBean note = docNoteDB.getDocNote(editedDoc.getDocId(), -1);
		if(Tools.isNotEmpty(editedDoc.getEditorFields().getRedactorNote())) {
			//ak je my_form.getEditorFields().isPublishAfterStart()==true, tak sa stranka publikuje v buducnosti. Poznamka sa nastavy v DocDB.copyDHistory(List<PublicableForm> copyDHtoD) a nie tu
			if(editedDoc.getEditorFields().isRequestPublish() && !editedDoc.getEditorFields().isPublishAfterStart()) {
				if(note == null) note = new DocNoteBean();
				note.setDocId(editedDoc.getDocId());
				note.setHistoryId(-1);
				note.setNote(editedDoc.getEditorFields().getRedactorNote());
				note.setUserId(editedDoc.getAuthorId());
				note.setCreated(new Date());
				note.save();
			}

			//historia sa ulozi stale
			DocNoteBean noteHistory = docNoteDB.getDocNote(-1, historyId);
			if(noteHistory == null) noteHistory = new DocNoteBean();
			noteHistory.setDocId(-1);
			noteHistory.setHistoryId(historyId);
			noteHistory.setNote(editedDoc.getEditorFields().getRedactorNote());
			noteHistory.setUserId(editedDoc.getAuthorId());
			noteHistory.setCreated(new Date());
			noteHistory.save();
		} else if(note != null) {
			//ak je zadana poznamka z formulara prazdna, tak existujucu note k stranke vymazeme
			note.delete();
		}
	}

	/**
	 * Nastavi stranke URL adresu (virtual_path), ak uz nejaka ina stranka takuto URL ma, tak prida cislo 1,2,3... na koniec URL adresy
	 * @param editedDoc
	 */
	protected void setVirtualPath(DocDetails editedDoc) {
		String domain = groupsDB.getDomain(editedDoc.getGroupId());
		int virtualPathConflictDocId = -1;
		if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML && editedDoc.getVirtualPath().startsWith("javascript:") == false) {
			boolean mustGenerateVirtualPath = false;
			if (Tools.isNotEmpty(editedDoc.getVirtualPath())) {
				int actualDocId = DocDB.getDocIdFromURL(editedDoc.getVirtualPath(), domain);
				if (actualDocId > 0 && actualDocId != editedDoc.getDocId()) {
					mustGenerateVirtualPath = true;
					virtualPathConflictDocId = actualDocId;
				}
			}

			if (mustGenerateVirtualPath || Tools.isEmpty(editedDoc.getVirtualPath()) || editedDoc.getVirtualPath().indexOf('/') == -1) {
				//nastavime ako treba
				String groupDiskPath = DocDB.getGroupDiskPath(groupsDB.getGroupsAll(), editedDoc.getGroupId());
				DocDetails doc = new DocDetails();
				doc.setDocId(editedDoc.getDocId());
				doc.setTitle(editedDoc.getTitle());
				doc.setNavbar(DB.prepareString(editedDoc.getNavbar(), 128));
				doc.setVirtualPath(editedDoc.getVirtualPath());
				doc.setGroupId(editedDoc.getGroupId());
				String virtualPath = DocDB.getURL(doc, groupDiskPath);
				String ending = virtualPath.endsWith("/") ? "/" : ".html";
				String editorPageExtension = Constants.getString("editorPageExtension");

				String lastVirtualPath = null;
				for (long i = 2; i < 1000; i++) {

					if (i>990) i = Tools.getNow();

					if(virtualPath != null && virtualPath.length() > 255) {
						String vpTmp = virtualPath.substring(0, virtualPath.length() - ending.length());
						vpTmp = DB.prepareString(vpTmp, 255 - ending.length()) + ending;
						virtualPath = vpTmp;
					}

					int allreadyDocId = DocDB.getDocIdFromURL(virtualPath, domain);
					Logger.debug(EditorService.class, "setVirtualPath: allreadyDocId for virtualPath: " + virtualPath + " ,docid: " + allreadyDocId);

					if (allreadyDocId <= 0 || allreadyDocId == editedDoc.getDocId()) { break; }

					//lebo moze kolidovat uz z hora
					if (virtualPathConflictDocId < 1) virtualPathConflictDocId = allreadyDocId;

					doc.setTitle(editedDoc.getTitle() + " " + i);
					doc.setNavbar(DB.prepareString(editedDoc.getNavbar(), 128) + " " + i);

					if ("/".equals(editorPageExtension)) {
						//nastav cistu, handluje sa to nastavenim title s cislom vyssie
						doc.setVirtualPath("");
					}
					else {
						if (editedDoc.getVirtualPath().endsWith(".html")) {
							doc.setVirtualPath(Tools.replace(editedDoc.getVirtualPath(), ".html", "-" + i + ".html"));
							ending = "-" + i + ".html";
						} else if (editedDoc.getVirtualPath().endsWith("/")) {
							doc.setVirtualPath(editedDoc.getVirtualPath() + i + ".html");
							ending = i + ".html";
						} else if (Tools.isNotEmpty(editedDoc.getVirtualPath()) && editedDoc.getVirtualPath().endsWith("/")==false && editedDoc.getVirtualPath().contains(".html")==false) {
							//url without last slash and without .html like /aaa/bbb
							doc.setVirtualPath(editedDoc.getVirtualPath() + "-" + i + ".html");
							ending = i + ".html";
						} else if (Tools.isEmpty(editedDoc.getVirtualPath())) {
							ending = ".html";
						}
					}

					virtualPath = DocDB.getURL(doc, groupDiskPath);

					if (lastVirtualPath != null && lastVirtualPath.equals(virtualPath)) {
						long fixedI = i - 100;
						if (fixedI < 2) fixedI = 2;
						//virtualPath is not changing, it is probably main page of folder, add number to the end
						if (virtualPath.contains(".html")) {
							//add number before .html
							virtualPath = virtualPath.substring(0, virtualPath.lastIndexOf(".html")) + "-" + fixedI + ".html";
						} else if (virtualPath.endsWith("/")) {
							//add number before last slash
							virtualPath = virtualPath.substring(0, virtualPath.length() - 1) + "-" + fixedI + "/";
						} else {
							virtualPath = virtualPath + "-" + fixedI;
						}
					} else {
						if (i>100) lastVirtualPath = virtualPath;
					}
				}

				editedDoc.setVirtualPath(DocDB.normalizeVirtualPath(virtualPath));

				Logger.println(EditorService.class, "nastaveny virtual path na:"+virtualPath+";");
			}
			else if ("cloud".equals(Constants.getInstallName())) {
				//tiket 15910 - kontrola specialnych znakov v URL
				String cleaned = DocTools.removeCharsDir(DB.internationalToEnglish(editedDoc.getVirtualPath())).toLowerCase();
				if(!cleaned.equals(editedDoc.getVirtualPath())) {
					editedDoc.setVirtualPath(DocDB.normalizeVirtualPath(cleaned));
					Logger.println(EditorService.class, "virtual path upraveny na:"+editedDoc.getVirtualPath()+";");
				}
			}
		}
		//pre uz existujucu stranku, ktora ma automaticky generovane URL nezobrazuj varovanie (lebo sa vzdy generuje a zobrazi sa pri kazdom ulozeni)
		if (virtualPathConflictDocId>0 && (Boolean.FALSE.equals(editedDoc.getGenerateUrlFromTitle()) || editedDoc.getDocId()<1  ) ) {
			NotifyBean notifyBean = new NotifyBean(prop.getText("text.warning"), prop.getText("editor.virtual_path_allready_used_in_doc")+": "+virtualPathConflictDocId, NotifyType.WARNING, 20000);
			addNotify(notifyBean);
		}
		String normalized = DocDB.normalizeVirtualPath(editedDoc.getVirtualPath());
		if (normalized!=null && normalized.equals(editedDoc.getVirtualPath())==false && editedDoc.getVirtualPath().contains("*")==false) editedDoc.setVirtualPath(normalized);
	}

	/**
	 * Ak sa zmeni virtual path stranky aktualizuje vsetky doterajsie linky
	 * @param docId
	 * @param oldLinkUrl //It's virtual path !!
	 * @param newVirtualPath
	 * @return
	 */
	private List<DocDetails> fixRenamedVirtualPath(int docId, String oldLinkURL, String newVirtualPath) {

		List<DocDetails> updated = new ArrayList<>();
		if (Tools.isEmpty(newVirtualPath) || Constants.getBoolean("editorDisableAutomaticRedirect")) return(updated);

		newVirtualPath = DocDB.normalizeVirtualPath(newVirtualPath);
		String domain = null;

		Logger.println(EditorService.class, "OldLinkURL: " + oldLinkURL);

		//Find the webpages, we need to rename
		if (Tools.isNotEmpty(oldLinkURL) && !oldLinkURL.equals("/showdoc.do?docid=" + docId)) {
			String newLinkURL = newVirtualPath;
			//Logger.println(EditorService.class, "newLinkURL="+newLinkURL);

			if (oldLinkURL.compareTo(newLinkURL) != 0) {
				String subGroupsIds = "";

				//Find domain (if need), and do it only on domain sub-pages
				if (Constants.getBoolean("multiDomainEnabled") == true) {
					DocDetails doc = docDB.getBasicDocDetails(docId, false);
					if (doc != null) {
						GroupDetails group = groupsDB.getGroup(doc.getGroupId());
						domain = group.getDomainName();
						subGroupsIds = groupsDB.getSubgroupsIds(domain);
					}
				}

				Logger.debug(EditorService.class, "fixRenamedVirtualPath: old=" + oldLinkURL + " new=" + newLinkURL + " domain=" + domain);

				//Replace all old URl to new in all pages
				updated = replaceUrl(oldLinkURL, newLinkURL, domain);

				//aktualizuj presmerovania
				if(Tools.isNotEmpty(subGroupsIds)) {
					int[] subGroupsArray = Tools.getTokensInt(subGroupsIds, ",");
					try {
						docRepo.updateRedirect(newLinkURL, oldLinkURL, subGroupsArray);
					} catch (Exception ex) {
						//asi prilis vela groupIds IN, skusme premenovat co sa da
						docRepo.updateRedirect(newLinkURL, oldLinkURL);
					}
				} else {
					docRepo.updateRedirect(newLinkURL, oldLinkURL);
				}

				//Update media
				mediaService.updateMediaLink(oldLinkURL, newLinkURL, domain);

				//Write redirect
				UrlRedirectDB.addRedirect(oldLinkURL, newVirtualPath, domain, 301);
			}
		}

		return(updated);
	}

	/**
	 * Nahradi odkazy z povodneho na nove URL vo vsetkych strankach
	 * @param oldLinkURL
	 * @param newLinkURL
	 * @param domain
	 * @return
	 */
	private List<DocDetails> replaceUrl(String oldLinkURL, String newLinkURL, String domain) {
		List<DocDetails> docsUpdated = new ArrayList<>();
		String subgroupsIds = "";
		List<DocDetails> docs;

		if (Constants.getBoolean("editorQuickUrlFix" )== true) return docsUpdated;

		DebugTimer dt = new DebugTimer("replaceUrl");
		dt.diff("starting");

		String oldLinkURL2 = oldLinkURL;
		if (oldLinkURL.length() > 2 && oldLinkURL.endsWith("/")) { oldLinkURL2 = oldLinkURL.substring(0, oldLinkURL.length() - 1); }

		//zisti domenu (ak treba) a sprav to iba na substrankach domeny
		if (Constants.getBoolean("multiDomainEnabled")==true && Tools.isNotEmpty(domain)) {
			subgroupsIds = groupsDB.getSubgroupsIds(domain);

			docs = docRepo.findByDataLikeAndGroupIdIn("%" + oldLinkURL2 + "%", Tools.getTokensInt(subgroupsIds, ","));
		} else {
			docs = docRepo.findByDataLike("%" + oldLinkURL2 + "%");
		}
		dt.diff("mam, pocet:"+docs.size());

		//We have list, now update it all
		for (DocDetails doc : docs) {
			Logger.println(EditorService.class, "updating link in: " + doc.getTitle());

			String oldData = doc.getData();
			doc.setData(Tools.replace(doc.getData(), "'" + oldLinkURL + "'", "'" +newLinkURL + "'"));
			doc.setData(Tools.replace(doc.getData(), "\"" + oldLinkURL + "\"", "\"" + newLinkURL + "\""));
			//toto robilo problem ked sa URL / menilo na /nieco-ine, menilo to aj 0903 / 100 100
			//doc.setData(Tools.replace(doc.getData(), " "+oldLinkURL+" ", " "+newLinkURL+" "));
			doc.setData(Tools.replace(doc.getData(), "'" + oldLinkURL + "#", "'" + newLinkURL + "#"));
			doc.setData(Tools.replace(doc.getData(), "\"" + oldLinkURL + "#", "\"" + newLinkURL + "#"));
			//doc.setData(Tools.replace(doc.getData(), " " + oldLinkURL + "#", " " + newLinkURL + "#"));

			if (oldLinkURL.length()>2 && oldLinkURL.endsWith("/")) {
				//fix na chybajuce koncove lomitko
				doc.setData(Tools.replace(doc.getData(), "'" + oldLinkURL2 + "'", "'" + newLinkURL + "'"));
				doc.setData(Tools.replace(doc.getData(), "\"" + oldLinkURL2 + "\"", "\"" + newLinkURL + "\""));
				//doc.setData(Tools.replace(doc.getData(), " " + oldLinkURL2 + " ", " " + newLinkURL + " "));
				doc.setData(Tools.replace(doc.getData(), "'" + oldLinkURL2 + "#", "'" + newLinkURL + "#"));
				doc.setData(Tools.replace(doc.getData(), "\"" + oldLinkURL2 + "#", "\"" + newLinkURL + "#"));
				//doc.setData(Tools.replace(doc.getData(), " " + oldLinkURL2 + "#", " " + newLinkURL + "#"));
			} else if (oldLinkURL.length() > 2 && oldLinkURL.endsWith(".html") == false) {
				//ak je linka bez koncoveho /, toto to vyriesi
				oldLinkURL2 = oldLinkURL + "/";

				doc.setData(Tools.replace(doc.getData(), "'" + oldLinkURL2 + "'", "'" + newLinkURL + "'"));
				doc.setData(Tools.replace(doc.getData(), "\"" + oldLinkURL2 + "\"", "\"" + newLinkURL + "\""));
				//doc.setData(Tools.replace(doc.getData(), " " + oldLinkURL2 + " ", " " + newLinkURL + " "));
				doc.setData(Tools.replace(doc.getData(), "'" + oldLinkURL2 + "#", "'" + newLinkURL + "#"));
				doc.setData(Tools.replace(doc.getData(), "\"" + oldLinkURL2 + "#", "\"" + newLinkURL + "#"));
				//doc.setData(Tools.replace(doc.getData(), " " + oldLinkURL2 + "#", " " + newLinkURL + "#"));
			}

			if (oldData.equals(doc.getData()) == false) {
				dt.diff("updating doc:" + doc.getDocId());
				docRepo.updateAfterUrlReplace(doc.getData(), DB.internationalToEnglish(doc.getData()).toLowerCase(), doc.getDocId());

				docsUpdated.add(doc);
			}
			//DocDB.updateDataClob(db_conn, doc.getDocId(), -1, doc.getData(), DB.internationalToEnglish(doc.getData()).toLowerCase());
		}
		dt.diff("done");

		return(docsUpdated);
	}

	//getre/setre

	/**
	 * Ak nastalo schvalovanie vrati zoznam schvalovatelov
	 * @return
	 */
	public List<UserDetails> getApprovers() {
		return approveService.getApprovers();
	}

	/**
	 * Ak ma web stranka publikovanie v buducnosti nastavi sa sem timestamp zaciatku publikovania
	 * @return
	 */
	public Long getPublihStart() {
		return publihStart;
	}

	/**
	 * Ak bola stranka uspesne vypublikovana na verejne zobrazenie vrati true
	 * @return
	 */
	public boolean isPageSavedToPublic() {
		return pageSavedToPublic;
	}

	/**
	 * Ak bola stranka korektne ulozena ako rozpracovana verzia vrati true
	 * @return
	 */
	public boolean isPageSavedAsWorkVersion() {
		return pageSavedAsWorkVersion;
	}

	/**
	 * Vrati true ak je potrebne obnovit stromovu strukturu web stranok na GUI
	 * @return
	 */
	public boolean isForceReload() { return forceReload; }

	/**
	 * Vyvola reload=true atribut v JSON odpovedi pre obnovenie stromovej struktury/datatabulky
	 * @param forceReload
	 */
	public void setForceReload(boolean forceReload) { this.forceReload = forceReload; }

	/**
	 * Vrati zoznam moznych notifikacii pre pouzivatela
	 * @return
	 */
	public List<NotifyBean> getNotify() {
		return notify;
	}

	/**
	 * Prida notifikaciu
	 * @param notifyBean
	 */
	public void addNotify(NotifyBean notifyBean) {
        if(this.notify == null) this.notify = new ArrayList<>();
        this.notify.add(notifyBean);
    }

	/**
	 * Overi, ci pouzivatel ma pravo na editaciu zadanej web stranky
	 * @param user
	 * @param doc
	 * @param isDelete
	 * @return
	 */
	public boolean isPageEditable(Identity user, DocDetails doc, boolean isDelete) {
		if (UsersDB.checkUserPerms(user, Constants.getString("webpagesFunctionsPerms")) == false) return false;

		if (isDelete) {
			if (user.isDisabledItem("deletePage")) {
				return false;
			}
		} else if (doc.getDocId()<1 && user.isDisabledItem("addPage")) {
			return false;
		} else if (doc.getDocId()>0 && user.isDisabledItem("pageSave")) {
			return false;
		}

		if (doc == null) return true;

		//otestuj, ci mame na tento dokument pristupove prava
		boolean canAccess = GroupsDB.isGroupEditable(user, doc.getGroupId());

		if (!canAccess)
		{
			//zisti, ci to nie je moja stranka
			int[] editablePages = Tools.getTokensInt(user.getEditablePages(), ",");
			for (int id : editablePages)
			{
				if (id == doc.getDocId())
				{
					canAccess = true;
				}
			}
		}

		//kontrola pre slave adresare danej stranky
		if(!canAccess)
		{
			int groupId;
			for (MultigroupMapping mapping : MultigroupMappingDB.getSlaveMappings(doc.getDocId()))
			{
				int dId = mapping.getDocId();
				groupId = DocDB.getInstance().getBasicDocDetails(dId, true).getGroupId();
				if (groupId > 0) canAccess = GroupsDB.isGroupEditable(user, groupId);
				Logger.debug(EditorService.class, "testujem pristup pre multigroup stranky [groupId="+groupId+"] canAccess="+canAccess);
				if(canAccess) break;
			}
		}

		if (canAccess && Constants.getBoolean("adminCheckUserGroups"))
		{
			if (DocDB.canAccess(doc, user, true)==false)
			{
				 canAccess = false;
			}
		}

		return canAccess;
	}

	/**
	 * Skontroluje prava na editaciu web stranky a throwne Runtime exception v pripade nedostatocnych prav
	 * @param user
	 * @param doc
	 * @param isDelete - nastavte na true, ak sa jedna o kontrolu prav pre mazanie stranky
	 */
	public void checkPermissions(Identity user, DocDetails doc, boolean isDelete) {
		String errorKey = null;
		if (user == null || UsersDB.checkUserPerms(user, Constants.getString("webpagesFunctionsPerms")) == false || doc == null) {
			errorKey = "error.userNotLogged";
		} else if (isDelete && isPageEditable(user, doc, true)==false) {
			errorKey = "admin.delete.deletePageDisabled.error";
		} else if (doc.getDocId()<1 && user.isDisabledItem("addPage")) {
			errorKey = "admin.addPage.addPageDisabled.error";
		} else if (doc.getDocId()>0 && user.isDisabledItem("pageSave")) {
			errorKey = "admin.editPage.error";
		} else if (isPageEditable(user, doc, false)==false) {
			errorKey = "admin.editPage.error";
		}

		if (Tools.isNotEmpty(errorKey)) {
			throw new RuntimeException(prop.getText(errorKey));
		}

	}

	public Prop getProp() {
		return prop;
	}

	/**
	 * Vrati mapu CSS stylov pre roletku vyberu stylu v editore
	 * @param baseCssPath
	 * @return
	 */
	public static List<Map<String, String>> getCssListJson(DocBasic doc) {
		List<Map<String, String>> sessionCssParsed = new ArrayList<>();

		StringTokenizer stCss = new StringTokenizer(getCssPath(doc), ",\n");
		while (stCss.hasMoreTokens())
		{
			String cssFile = stCss.nextToken();
			//bootstrap neparsujeme, to by bol masaker
			if (cssFile.contains("bootstrap.min.css") || cssFile.contains("bootstrap.css")) continue;

			//PathFilter.getRealPath je pouzity z dovodu najdenia custom_path pre vyvoj
			IwcmFile file = new IwcmFile(PathFilter.getCustomPathRealPath(cssFile));
			if (!file.exists()) {
				//skus najst subor s pridanym InstallName
				file = new IwcmFile(PathFilter.getCustomPathRealPath(Tools.replace(cssFile, "/templates/", "/templates/"+Constants.getInstallName()+"/")));
			}
			if (!file.exists()) {
				Logger.error(EditorService.class, "Css file " + file.getName() + " not exist");
				continue;
			}

			//ak subor konci na min.css skus najst neminifikovanu verziu
			if (file.getName().endsWith(".min.css")) {
				String path = Tools.replace(file.getAbsolutePath(), ".min.css", ".css");
				IwcmFile notMinified = new IwcmFile(path);
				if (notMinified.exists() && notMinified.canRead()) file = notMinified;
			}

			sk.iway.css.CssParser cssParser = new sk.iway.css.CssParser(file);
			List<Pair<String, String>> editorCss = cssParser.getElements();
			try
			{
				//prehod na json format
				for(Pair<String, String> element : editorCss)
				{
					Map<String, String> map = new HashMap<>();
					map.put("tag", element.getFirst());
					map.put("class", element.getSecond());
					sessionCssParsed.add(map);
				}
			}
			catch (Exception ex)
			{
				Logger.error(EditorService.class, ex);
			}
		}

		return sessionCssParsed;
	}

	private static String getCssPath(DocBasic doc) {

		String defaultBaseCssPath = "/css/page.css"; //NOSONAR
		String baseCssPath = null;
		TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
		GroupDetails group = doc.getGroup();
		String domainName = null;
		if (group != null) domainName = group.getDomainName();

		if (temp != null) {
			baseCssPath = temp.getBaseCssPath();
		}

		if (Tools.isNotEmpty(baseCssPath) && Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(domainName)) {
			// baseCssPath = Tools.replace(baseCssPath, "/css/",
			// "/css/"+MultiDomainFilter.getDomainAlias(editForm.getDomainName())+"/");
			baseCssPath = MultiDomainFilter.rewriteUrlToLocal(baseCssPath, MultiDomainFilter.getDomainAlias(domainName));
		}

		if (Tools.isEmpty(baseCssPath)) {
			baseCssPath = defaultBaseCssPath;
		}

		String editorCssPath = getEditorCssPath(baseCssPath);
		if (baseCssPath != null && baseCssPath.startsWith("/templates/") && baseCssPath.contains("editor.css") == false && editorCssPath != null && editorCssPath.startsWith("/templates")) {
			baseCssPath += "," + editorCssPath;
		}

		return baseCssPath;
	}

	private static String getEditorCssPath(String baseCssPathMultiline) {
		String editorCss;
		String[] lines = Tools.getTokens(baseCssPathMultiline, "\n");
		for (String baseCssPath : lines) {
			baseCssPath = baseCssPath.trim();
			if (baseCssPath != null && baseCssPath.startsWith("/templates/") && baseCssPath.contains("editor.css") == false) {
				// skus automaticky dohladat aj editor.css v danom adresari
				editorCss = baseCssPath.substring(0, baseCssPath.lastIndexOf("/")) + "/editor.css";
				if (FileTools.isFile(editorCss)) {
					return editorCss;
				}
			}
		}

		editorCss = "/css/editor.css";
		if (FileTools.isFile(editorCss)) {
			return editorCss;
		}

		return null;
	}

	public void saveAttrs(DocDetails doc, List<DocAtrDefEntity> attrs, boolean cleanBeforeSave) {
		if (doc == null || attrs == null || attrs.size()<1) return;

		if (cleanBeforeSave) {
			docAtrRepository.deleteAllByDocId(doc.getDocId());
		}

		for (DocAtrDefEntity def : attrs) {
			DocAtrEntity atrEntity = def.getDocAtrEntityFirst();
			if (atrEntity != null) {
				if (cleanBeforeSave) atrEntity.setId(null);
				if (atrEntity.getDocId()==null || cleanBeforeSave) atrEntity.setDocId(doc.getDocId());
				if (atrEntity.getAtrDef()==null) atrEntity.setAtrDef(def);
				docAtrRepository.save(atrEntity);
			}
		}
	}

	/**
	 *
	 * @param editedDoc
	 * @param isInsert
	 * @param dt
	 */
	private void docSave(DocDetails editedDoc, boolean isInsert, DebugTimer dt) {
		if(isInsert) dt.diff("preparing insert");
		else dt.diff("preparing update data");

		GroupDetails group = groupsDB.getGroup(editedDoc.getGroupId());
		String fileName = null;

		if (group != null && group.isInternal() == false) fileName = groupsDB.getGroupNamePath(editedDoc.getGroupId());

		//Cut string too maxLength 255, then set
		editedDoc.setFileName(DB.prepareString(fileName,255));

		// Set root groups L1, L2, L3
		setRootGroupL(editedDoc.getGroupId(), editedDoc);

		if (Tools.isEmpty(editedDoc.getNavbar())) editedDoc.setNavbar(editedDoc.getTitle());

		if(isInsert) {
			// Insert new doc entity via DocDeailsRepository
			docRepo.save(editedDoc);
			dt.diff("after insert");

			// Set docId into entity (get id from DB)
			editedDoc.setDocId(docRepo.findMaxIdByGroupIdAndTitle(editedDoc.getGroupId(), editedDoc.getTitle()));
			dt.diff("after setDocId");
		} else {
			//Update webpage via DocDeailsRepository
			docRepo.save(editedDoc);
			dt.diff("after update");
		}

		//aktualizuj pripadne aj tab. perex_group_doc
		DocDB.udpdatePerexGroupDoc(editedDoc.getDocId(), editedDoc.getPerexGroupIdsString());
		dt.diff("after update perex group doc");
	}

	private void renameVirtualPath(DocDetails editedDoc, DebugTimer dt) {
		//Ak sa zmeni virtual path stranky aktualizuje vsetky doterajsie linky
		String oldLinkURL = DocDB.getURLFromDocId(editedDoc.getDocId(), request); //Get page old virtual path

		List<DocDetails> updated = fixRenamedVirtualPath(editedDoc.getDocId(), oldLinkURL, editedDoc.getVirtualPath());
		if (updated.isEmpty()==false) {
			//zobraz cez notifikaciu
			StringBuilder changedPages = new StringBuilder();
			for (DocDetails doc : updated) {
				changedPages.append("\n<br/>").append(doc.getDocId()).append("-").append(doc.getTitle());
			}

			NotifyBean notifyBean = new NotifyBean(prop.getText("text.info"), prop.getText("editor.updatedDocs")+changedPages.toString(), NotifyType.INFO);
			addNotify(notifyBean);
		}
		dt.diff("after fixRenamedVirtualPath");
	}

	/**
	 * Approve waiting DocHistory to change some webpage (can be update/insert).
	 * @param editedHistory
	 */
	public void approveDocHistory(DocHistory editedHistory) {
		//Set actual informations
		editedHistory.setApprovedBy(currentUser.getUserId());
		editedHistory.setAwaitingApprove(null);
		editedHistory.setActual(true);
		editedHistory.setAvailable(true);
		editedHistory.setApproveDate(new Date(now));

		//Create new version of DocDetails from DocHistory
		DocDetails editedDoc = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(editedHistory);

		//Inicialize editorFields
		DocEditorFields def = new DocEditorFields();
		def.fromDocDetails(editedDoc, true, false);
		def.setRequestPublish(true);
		editedDoc.setEditorFields(def);

		DebugTimer dt = new DebugTimer("EditorService.saveEditedDoc");

		Logger.println(this,"ApproveAction.approve");

		(new WebjetEvent<>(editedDoc, WebjetEventType.ON_START)).publishEvent();

		//Check perms
		checkPermissions(currentUser, editedDoc, false);

		//Do update itself
		updateWebpage(editedDoc, dt);

		//Update docHistory record (no more waiting)
		historyRepo.save(editedHistory);

		//Delete other waiting docHistory records for this webpage
		deleteHistorySaveRecords(editedDoc, editedHistory, editedHistory.getHistoryId(), dt);

		//There can be situation that change is approved, but isRequestPublish = false because we are waiting for publishing date
		if(editedDoc.getEditorFields().isRequestPublish()) {
			//IN case of update webpage, there is NEEDED to do some sort of adjustments and refresh

			renameVirtualPath(editedDoc, dt);
			refreshCacheObjects(editedDoc, dt);
			refreshTemplates(editedDoc, dt);
		}

		(new WebjetEvent<>(editedDoc, WebjetEventType.AFTER_SAVE)).publishEvent();
	}

	/**
	 * Refresh cache object. Used after DOC change, so this changes will show.
	 * @param editedDoc
	 * @param dt
	 */
	private void refreshCacheObjects(DocDetails editedDoc, DebugTimer dt) {
		if (editedDoc.getGroupId() == Constants.getInt("tempGroupId") || editedDoc.getGroupId() == Constants.getInt("menuGroupId") || editedDoc.getGroupId() == Constants.getInt("headerFooterGroupId")) {
			//v system adresari pre istotu spravim klasicky refresh
			docDB = DocDB.getInstance(true);
			dt.diff("after DocDB.getInstance");
			/*spravime update dokumentu v indexe */
			docDB.updateInternalCaches(editedDoc.getDocId());
		} else {
			docDB.updateInternalCaches(editedDoc.getDocId());
			dt.diff("after DocDB.update internal caches");
		}
	}

	/**
	 * Refresh templates. Used after DOC change, so this changes will show.
	 * @param editedDoc
	 * @param dt
	 */
	private void refreshTemplates(DocDetails editedDoc, DebugTimer dt) {
		boolean isInSystemFolder = false;
		if (editedDoc.getVirtualPath()!=null && editedDoc.getVirtualPath().startsWith("/system/")) isInSystemFolder = true;
		if (isInSystemFolder || editedDoc.getGroupId() == Constants.getInt("tempGroupId") || editedDoc.getGroupId() == Constants.getInt("menuGroupId") || editedDoc.getGroupId() == Constants.getInt("headerFooterGroupId")) {
			TemplatesDB.getInstance(true);
			dt.diff("after templates DB getInstance");
		}
	}

	/**
	 * Perform insert/update webpage action (aka waiting docHistory) by approve/reject throu calling ApproveSrvice.approveAction method
	 * @return
	 */
	public boolean approveAction() {
		return approveService.approveAction(historyRepo, this);
	}

	/**
	 * Perform delete webpage action (aka waiting docHistory) by approve/reject throu calling ApproveSrvice.approveDelAction method
	 * @return
	 */
	public boolean approveDelAction() {
		return approveService.approveDelAction(historyRepo, docRepo, this);
	}

	/**
	 * Delete webpage logic + checkPermissions and check approve posibility.
	 *
	 * It will move webpage to trash folder (soft delete) OR if delete is called from trash folder perform permanent delete.
	 * @param delDocId
	 * @param approveService
	 * @param publishEvents
	 * @return Return "success" or other string taht represend some sort of error taht occured
	 */
	protected String deleteWebpageLogic(int delDocId, ApproveService approveService, boolean publishEvents) {
		//If id is -1, try get id from request, if id is still -1 return eeror message
		if(delDocId == -1) delDocId = Tools.getIntValue(request.getParameter("docid"), -1);
		if(delDocId == -1) return "There's no provided docId to by used for delete.";

		//Try get DocDetails object by id, if not present return error message
		Optional<DocDetails> docDetailsOpt = docRepo.findById(Long.valueOf(delDocId));
		if(!docDetailsOpt.isPresent()) {
			return "Record by provided ID not found.";
		}
		DocDetails docDetails = docDetailsOpt.get();

		//To check perms and approve for this action
		checkPermissions(currentUser, docDetails, true);
		approveService.loadApproveTables(docDetails.getGroupId());

		//Check if user can approve this action
		if(approveService.needApprove()==false || approveService.isSelfApproved()) {
			//Doc, do not need approve OR
			//current user IS self approver, can do delete action

			if (publishEvents) (new WebjetEvent<DocDetails>(docDetails, WebjetEventType.ON_DELETE)).publishEvent();

			// zisti ci sme v adresari /System/Trash (kos), ak nie presun, inak vymaz
			String navbarNoHref = DB.internationalToEnglish(groupsDB.getURLPath(docDetails.getGroupId())).toLowerCase();
			Logger.println(EditorService.class,"MAZEM: " + navbarNoHref);

			//tu sa vytvara adresar podla default jazyka, nie podla prihlaseneho pouzivatela!
			Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
			String trashDirName = propSystem.getText("config.trash_dir");
			GroupDetails group = groupsDB.getGroup(docDetails.getGroupId());

			//vypne zapisovanie zaznamov do documents_history tabulky. true - nezapise zaznam do documents_history
			boolean disableHistory = Constants.getBoolean("editorDisableHistory");
			GroupDetails trashGroupDetails = disableHistory == false ? groupsDB.getCreateGroup(trashDirName) : null;

			if (trashGroupDetails==null || navbarNoHref.startsWith(DB.internationalToEnglish(groupsDB.getURLPath(trashGroupDetails.getGroupId())).toLowerCase()))  {
					//Permanent DOC delete
					docRepo.deleteById(Long.valueOf(delDocId));

					//Every docHistory record awaiting for approve is canceled (DOC is deleted so changes waiting for approve are irelevant)
					historyRepo.updateAwaitingApprove("", delDocId);

					//Check, if DOC is main in the folder
                    if (group.getDefaultDocId() == delDocId)  {
                        List<DocDetails> pages = docDB.getDocByGroup(group.getGroupId(), DocDB.ORDER_PRIORITY, true, -1, -1, true);
                        if (pages.size() > 0) {
                            docDetails = pages.get(0);

                            if (docDetails != null) {
                                group.setDefaultDocId(docDetails.getDocId());
                                group.setSyncStatus(1);
                                groupsDB.setGroup(group);
                            } else {
                                group.setDefaultDocId(0);
                                group.setSyncStatus(1);
                                groupsDB.setGroup(group);
                            }
                        }
                    }
                    //Set for ApproveDelAction (proof of succesfull delete)
                    if (request!=null) request.setAttribute("deleteSuccess", "yes");

					//delete doc from multigroup mapping
					MultigroupMappingDB.deleteSlaveDocFromMapping(delDocId);

                    //14.8.2012 pridany Admin log stranka bola vymazana uplne (z kosa)
                    Adminlog.add(Adminlog.TYPE_PAGE_DELETE, "(DocID: "+delDocId+"): Stranka bola uplne zmazana (z kosa)", delDocId, 0);

					List<MultigroupMapping> slaveMappingList = MultigroupMappingDB.getSlaveMappings(delDocId);
					if(slaveMappingList != null && slaveMappingList.isEmpty()==false) {
						List<Long> slaveIds = slaveMappingList.stream().map(x->Long.valueOf(x.getDocId())).collect(Collectors.toList());

						//Delete all connections from multigroup table
						MultigroupMappingDB.deleteSlaves(delDocId);

						//Perform HARD (permanent) delete of slave pages
						docRepo.deleteByDocIdIn(slaveIds);

						for (Long slaveId : slaveIds) {
							DocDB.getInstance().updateInternalCaches(slaveId.intValue());
						}

						Adminlog.add(Adminlog.TYPE_PAGE_DELETE, "(DocID's : " + StringUtils.collectionToDelimitedString(slaveIds, ",") + "): Slave stranky boli uplne zmazane (hard delete)", delDocId, 0);
					}
            } else {
                //failsafe na zle zmazane polozky (take co v kosi boli volakedy a zle sa zmazali)

				List<Integer> groupIds = (new SimpleQuery()).forListInteger("SELECT group_id FROM groups WHERE group_id=parent_group_id");

				String ids = null;
				StringBuilder buf = null;

				for(Integer groupIdX : groupIds) {
					if (buf == null) buf = new StringBuilder(Integer.toString(groupIdX));
					else buf.append(',').append(groupIdX);
				}

				if(buf != null) ids = buf.toString();

				if (ids != null) {
					new SimpleQuery().execute("UPDATE groups SET parent_group_id=? WHERE group_id IN (" + ids + ")");
					GroupsDB.getInstance(true);
				}

                //presun to do trash adresara
                Logger.println(EditorService.class,"presuvam do trash adresara");

				//Soft delete, move to thash folder
				docRepo.moveToTrash(false, trashGroupDetails.getGroupId(), delDocId);

				//Every docHistory record awaiting for approve is canceled (DOC is deleted so changes waiting for approve are irelevant)
				historyRepo.updateAwaitingApprove("", delDocId);

                //Check, if DOC is main in the folder
                if (group!=null && group.getDefaultDocId() == delDocId) {
                    List<DocDetails> pages = docDB.getDocByGroup(group.getGroupId(), DocDB.ORDER_PRIORITY, true, -1, -1, true);
                    if (pages.size() > 0) {
                        docDetails = pages.get(0);

                        if (docDetails!=null) {
                            group.setDefaultDocId(docDetails.getDocId());
                            group.setSyncStatus(1);
                            groupsDB.setGroup(group);
                        } else {
							group.setDefaultDocId(0);
							group.setSyncStatus(1);
							groupsDB.setGroup(group);
						}
                    }
                }
				//Set for ApproveDelAction (proof of succesfull delete)
                if (request != null) request.setAttribute("deleteSuccess", "yes");
                //Admin log stranka bola presunuta do kosa
                Adminlog.add(Adminlog.TYPE_PAGE_DELETE, "(DocID: " + delDocId + "): Stranka bola presunuta do kosa ", delDocId, 0);
            }
            DocDB.getInstance().updateInternalCaches(delDocId);

			if (publishEvents) (new WebjetEvent<DocDetails>(docDetails, WebjetEventType.AFTER_DELETE)).publishEvent();

			//Success signlization
			return "success";
		} else {
			//Doc NEED's approve, BUT current user is NOT selfApprover, so he can't approve this shit

			//ziskaj aktualne vypublikovane history_id
			Optional<Integer> actualPublishedHistoryIdOpt = historyRepo.findMaxHistoryId(delDocId, true);

			//!! BUG FIX - If page is stil waiting to be approven (as inserted)
			//If approver try delete this page it's gonna be ok, BUT in this (non approver) case actualPublishedHistoryId is empty
			if(!actualPublishedHistoryIdOpt.isPresent()) return "Something went wrong. Page probably isn't exist.";
			int actualPublishedHistoryId  = actualPublishedHistoryIdOpt.get();

			//vypne zapisovanie zaznamov do documents_history tabulky. true - nezapise zaznam do documents_history
            boolean disableHistory =  Constants.getBoolean("editorDisableHistory");
            if(disableHistory) {
                Logger.debug(EditorDB.class, "Write into documents_history is disabled");
            } else {
				Logger.println(EditorService.class,"approveByUsersId=" + approveService.getApproveUserIds());

				//Convert DocDetail to DocHistory entity (this new entity will be inserted)
				DocHistory docHistory = prepareDeleteDocHistoryEntityFromDocDetails(docDetails);

				//Save
				historyRepo.save(docHistory);

				//Id of new docHistory entity
				int historyId = historyRepo.findMaxHistoryId(delDocId);

				//Send request's to this deleet action
				approveService.sendWebpageApproveDelRequestEmail(docHistory, historyId, actualPublishedHistoryId);

				//9.8.2012 pridany audit o zmazani suboru
				Adminlog.add(Adminlog.TYPE_PAGE_DELETE, "(DocID: "+delDocId+"): Stranka poziadana o vymazanie : " + approveService.getEmailsToNotify(null), delDocId, 0);
			}

			//Can't approve signalization
			return prop.getText("approveAction.err.cantApprove");
		}
	}

	private DocHistory prepareDeleteDocHistoryEntityFromDocDetails(DocDetails docDetails) {
		//Convert DocDetail to DocHistory entity (this new entity will be inserted)
		DocHistory docHistory = DocDetailsToDocHistoryMapper.INSTANCE.docDetailsToDocHistory(docDetails);

		//General setting - !! there MUST be set "[DELETE]" as delete prefix, that indicates delete intend
		docHistory.setTitle("[DELETE] " + docHistory.getTitle());
		docHistory.setData(prop.getText("approve.delete.doctext"));
		docHistory.setDataAsc("[DELETE]");

		//When created and by who
		docHistory.setDateCreated(now);
		docHistory.setSaveDate(new Date(now));
		docHistory.setAuthorId(currentUser.getUserId());

		//Set availability of page etc
		docHistory.setSearchable(false);
		docHistory.setCacheable(false);
		docHistory.setAvailable(false);
		docHistory.setActual(false);

		//Mark docHistory as waiting for approve (need's approve by approver)
		docHistory.setApprovedBy(-1);
		docHistory.setAwaitingApprove("," + approveService.getApproveUserIds() + ",");

		return docHistory;
	}

	/**
	 * Recover webpage from trash folder:
	 * - set groupId from history (latest where actual=1 or latest)
	 * - set available from history (latest where actual=1 or latest)
	 * @param recoverDocId
	 */
	public void recoverWebpageFromTrash(int recoverDocId) {
		if(recoverDocId <1) throw new RuntimeException("recoverDocId is not valid");

		//Try get DocDetails object by id, if not present return error message
		Optional<DocDetails> docDetailsOpt = docRepo.findById(Long.valueOf(recoverDocId));
		if(!docDetailsOpt.isPresent()) throw new RuntimeException("DocDetails doesn't exists.");
		DocDetails docDetailsToRecover = docDetailsOpt.get();

		//To check perms and approve for this action
		checkPermissions(currentUser, docDetailsToRecover, true);

		//Find last actual (if posible) history id (so we know wehre to recover page)
		Integer historyId = null;
		Optional<Integer> historyIdOpt = historyRepo.findMaxHistoryId(recoverDocId, true); //(actual history id)
		if(historyIdOpt.isPresent())
			historyId = historyIdOpt.get();
		else historyId = historyRepo.findMaxHistoryId(recoverDocId); //(any history id)

		if(historyId == null) {
			//There is no history
			NotifyBean info = new NotifyBean(prop.getText("editor.recover.notifyTitle"), prop.getText("editor.recover.notify.no_history"), NotifyBean.NotifyType.WARNING, 60000);
            addNotify(info);
			return;
		} else {
			Optional<Integer> destGroupId = historyRepo.findGroupIdById(Long.valueOf(historyId));
			if(!destGroupId.isPresent()) {
				NotifyBean info = new NotifyBean(prop.getText("editor.recover.notifyTitle"), prop.getText("editor.recover.notify.no_history"), NotifyBean.NotifyType.WARNING, 60000);
            	addNotify(info);
				return;
			}
			GroupDetails destGroup = groupsDB.getGroup(destGroupId.get());
			if(destGroup == null) {
				NotifyBean info = new NotifyBean(prop.getText("editor.recover.notifyTitle"), prop.getText("editor.recover.notify.no_history"), NotifyBean.NotifyType.WARNING, 60000);
            	addNotify(info);
				return;
			}

			//Check perms
			approveService.loadApproveTables(destGroup.getGroupId());
			if(approveService.needApprove() == false || approveService.isSelfApproved()) {
				//Have right
				docDetailsToRecover.setGroupId(destGroup.getGroupId());
				docDetailsToRecover.setAvailable(true);
				docRepo.save(docDetailsToRecover);
			} else {
				//No right
				NotifyBean info = new NotifyBean(prop.getText("editor.recover.notifyTitle"), prop.getText("editor.recover.notify.no_right"), NotifyBean.NotifyType.WARNING, 60000);
				addNotify(info);
				return;
			}
		}

		//Refresh
		DocDB.getInstance(true);
		GroupsDB.getInstance(true);

		//Success
		NotifyBean info = new NotifyBean(prop.getText("editor.recover.notify_title.success_page"), prop.getText("editor.recover.notify_body.success_page", docDetailsToRecover.getTitle(), docDetailsToRecover.getFullPath()), NotifyBean.NotifyType.SUCCESS, 60000);
		addNotify(info);
	}
}