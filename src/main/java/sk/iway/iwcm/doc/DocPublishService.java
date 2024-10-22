package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.DocNoteBean;
import sk.iway.iwcm.editor.DocNoteDB;
import sk.iway.iwcm.editor.rest.DocDetailsToDocHistoryMapper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Handles the datetime publication/depublication of webpages
 */
public class DocPublishService {

	private long lastPublishCheck = 0;
	//holds list of pages that are ready to be published{DocHistory} or disabled/unpublished{DocDetails}
	private List<DocBasic> publicableDocs;

	private DocHistoryRepository dhr = null;
	private DocDetailsRepository ddr = null;

    public DocPublishService() {
		//empty constructor
	}

    /**
	 * check if there are any webpages in publicableDocs to publish/unpublish
	 */
	public void checkWebpagesToPublish(DocDB docDB) {

		//check only once in 5 seconds
		if (lastPublishCheck + 5000 > System.currentTimeMillis()) return;
		lastPublishCheck = System.currentTimeMillis();

		//Webjet MUST be initialized - otherwise DocDB/GroupsDB is not initialized correctly
		if(InitServlet.isWebjetInitialized() == true) {
			prepareRepositories();

			//If publicableDocs is null, read pages to public -> could happen if Service was crated before Webjet was initialized
			if(publicableDocs == null) refreshPagesToPublish();

			List<DocBasic> stillWaitingPublishList = new ArrayList<>();
			long now = Tools.getNow();

			if (publicableDocs != null && publicableDocs.size() > 0) {
				for (DocBasic pdoc : publicableDocs) {
					if (pdoc instanceof DocHistory) {
						DocHistory doc = (DocHistory) pdoc;
						//Is ready to by published
						if(Tools.isTrue(doc.getPublicable()) && (doc.getPublishStart() > 0) && (now >= doc.getPublishStart())) {
							copyDHistory(doc, docDB);
							continue;
						}
					}

					if (pdoc instanceof DocDetails) {
						DocDetails doc = (DocDetails) pdoc;
						//Is ready to be disabled
						if (doc.isPublicable()==false && doc.isDisableAfterEnd() && (doc.getPublishEnd()>0) && (now >= doc.getPublishEnd())) {
							disableAfterEnd(doc);
							continue;
						}
					}

					//Neither
					//STILL WAITING -> not ready
					stillWaitingPublishList.add(pdoc);
				}

				//SWAP lists
				publicableDocs = stillWaitingPublishList;
			}
		}
	}

	/**
	 * copy data from table documents_history to table documents
	 * @param docHistory
	 * @param docDB
	 */
	private synchronized void copyDHistory(DocHistory docHistory, DocDB docDB) {
		GroupsDB groupsDB = GroupsDB.getInstance();

		DocDetails docDetails = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(docHistory);

		Logger.debug(this,"Publishing from historyId: " + docDetails.getHistoryId() + " docId: " + docDetails.getDocId());

		boolean publicable = dhr.getPublicableByDocIdIn(docDetails.getDocId()).contains(Boolean.TRUE) ? true : false;
		Optional<List<Integer>> historyIdsOpt = dhr.getHisotryIdsByDocIdIn(docDetails.getDocId());
		String historyIds = historyIdsOpt.isPresent() == true ? historyIdsOpt.get().stream().map(String::valueOf).collect(Collectors.joining(",")) : null;

		if (historyIds == null || publicable == false) {
			//asi sme cluster a uz to niekto aktualizoval
			return;
		}

		dhr.updateActualAndSyncStatus(false, Tools.getTokensInt(sk.iway.iwcm.DB.getOnlyNumbersIn(historyIds), ","));

		//updatni zaznam v history, zrus publicable a nastav actual
		dhr.updatePublicableAndActual(false, true, docDetails.getAuthorId(), true, docDetails.getHistoryId());

		Logger.println(this,"pForm.getPublishStart()=" + docDetails.getPublishStart());

		///Now is available
		docDetails.setAvailable(true);
		docDetails.setPerexGroupString( docDetails.getPerexGroupIdsString(true) );

		GroupDetails group = groupsDB.getGroup(docDetails.getGroupId());
		if (group != null && group.isInternal() == false)
			docDetails.setFileName( groupsDB.getGroupNamePath(docDetails.getGroupId()) );
		else docDetails.setFileName(null);

		//Set root groups
		int[] rootGroups = DocDB.getRootGroupL(docDetails.getGroupId(), null, -1);
		docDetails.setRootGroupL1(rootGroups[0]);
		docDetails.setRootGroupL2(rootGroups[1]);
		docDetails.setRootGroupL3(rootGroups[2]);

		///Set publish after start to false
		docDetails.setPublishAfterStart(false);

		//Before save add audit param that signalize that webpage was published
		RequestBean.addAuditValue("publishStatus", "Webpage was published");

		//Perform update
		ddr.save(docDetails);

		// vypublikovanie slave clankov z historie (multikategorie)
		DocDetails masterDocDetails = null;
		for(Integer docId : MultigroupMappingDB.getSlaveDocIds(docDetails.getDocId()))
		{
			if (masterDocDetails == null) masterDocDetails = docDB.getDoc(docDetails.getDocId(), -1, false);
			DocDetails slaveDoc = docDB.getBasicDocDetails(docId, false);
			if (slaveDoc != null) {
				//teraz zmenme hodnoty pre master doc a ulozme do DB
				masterDocDetails.setVirtualPath(slaveDoc.getVirtualPath());
				masterDocDetails.setExternalLink(slaveDoc.getExternalLink());
				masterDocDetails.setDocId(docId);
				masterDocDetails.setGroupId(slaveDoc.getGroupId());
				DocDB.saveDoc(masterDocDetails);
			}

		}

		//prekopirovanie poznamky pre redaktorov k stranke
		DocNoteBean historyNote = DocNoteDB.getInstance().getDocNote(-1, docDetails.getHistoryId());
		if(historyNote != null) {
			DocNoteBean publishedNote = DocNoteDB.getInstance().getDocNote(docDetails.getDocId(), -1);
			if(publishedNote == null)
				publishedNote = new DocNoteBean();

			publishedNote.setDocId(docDetails.getDocId());
			publishedNote.setHistoryId(-1);
			publishedNote.setNote(historyNote.getNote());
			publishedNote.setUserId(historyNote.getUserId());
			publishedNote.setCreated(historyNote.getCreated());
			publishedNote.save();
		}

		if( Constants.getBoolean("webpagesNotifyAutorOnPublish") == true ) sendMailToPageAuthor(docHistory, docDB);

		DocDB.getInstance().updateInternalCaches(docDetails.getDocId());

		//refresh sablony sa vykona iba ak je stranka v System adresari
		if(docDetails.getFullPath().startsWith("/System") || docDetails.getGroupId() == Constants.getInt("tempGroupId") || docDetails.getGroupId() == Constants.getInt("menuGroupId") || docDetails.getGroupId() == Constants.getInt("headerFooterGroupId"))
		{
			TemplatesDB.getInstance(true);
		}
	}

    /**
	 * Disable page after unpublish/end time
	 * @param doc
	 */
	private synchronized void disableAfterEnd(DocDetails doc) {
		ddr.updateAvailableAndDisabledAfterEnd(false, false, doc.getDocId());
		DocDB.getInstance().updateInternalCaches(doc.getDocId());
	}

	/**
	 * Send email to page author about page publication
	 * @param docHistory
	 * @param docDB
	 */
	private void sendMailToPageAuthor(DocHistory docHistory, DocDB docDB) {
		UserDetails user = UsersDB.getUser(docHistory.getAuthorId());

		Prop prop = Prop.getInstance();
		String subject = prop.getText("webpage.publishable.email.subject");
		String publishableDateTime = Tools.formatDateTime(docHistory.getPublishStart());
		String webpageLink = "<a href=\"" +  docDB.getDocLink(docHistory.getDocId(), null, true, null) + "\">" + docHistory.getTitle() + "</a>";
		String body = prop.getText("webpage.publishable.email.body", docHistory.getTitle(), ""+docHistory.getDocId(), publishableDateTime, webpageLink, ""+docHistory.getHistoryId());

		SendMail.send(user.getFullName(), user.getEmail(), user.getEmail(), subject, body);
	}

	/**
	 * Read pages waiting for publishing or to be disabled
	 */
	public void refreshPagesToPublish() {
		if(InitServlet.isWebjetInitialized() == true) {
			DebugTimer dt = new DebugTimer("readPagesToPublic");

			prepareRepositories();

			//Clear list
			List<DocBasic> publicableDocsLocal = new ArrayList<>();

			//Get all pages that are publicable (publicable = true) and are not awaiting to approve (awaitingApprove = null or awaitingApprove = "")
			List<DocHistory> publicableHistoryDocs = dhr.getPublicableThatAreNotAwaitingToApprove().orElse(new ArrayList<>());
			publicableDocsLocal.addAll( publicableHistoryDocs );

			//Add pages where disable_after_end = true
			publicableDocsLocal.addAll( ddr.findAllByDisableAfterEndTrue() );

			//filter pages in trash - boolean isInTrash = groupsDB.isInTrash(docDetails.getGroupId());
			GroupsDB groupsDB = GroupsDB.getInstance();
			publicableDocsLocal = publicableDocsLocal.stream().filter(doc -> groupsDB.isInTrash(doc.getGroupId())==false).collect(Collectors.toList());

			publicableDocs = publicableDocsLocal;

			dt.diff("done");
		}
	}

	protected List<DocBasic> getPublicableDocs() {
		return publicableDocs;
	}

	/**
	 * Setup Spring DATA repositories
	 */
	private void prepareRepositories() {
		if(dhr == null) dhr = Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class);
		if(ddr == null) ddr = Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class);
	}
}