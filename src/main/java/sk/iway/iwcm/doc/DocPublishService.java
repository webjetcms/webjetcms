package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SetCharacterEncodingFilter;
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
    private static final Set<Integer> publishing = ConcurrentHashMap.newKeySet();
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

			long now = Tools.getNow();

			ArrayList<DocHistory> copyDHtoD = new ArrayList<>(); // specify which pForm-s to copy from documents_history to documents
			ArrayList<DocDetails> removeAfterEndList = new ArrayList<>();

			if (publicableDocs != null && publicableDocs.size() > 0) {
				//use good old for loop to avoid ConcurrentModificationException
				for (DocBasic pdoc : publicableDocs) {
					if (pdoc instanceof DocHistory doc) {
						//Is ready to by published
						if(Tools.isTrue(doc.getPublicable()) && (doc.getPublishStart() > 0) && (now >= doc.getPublishStart())) {
							copyDHtoD.add(doc);
							continue;
						}
					}

					if (pdoc instanceof DocDetails doc) {
						//Is ready to be disabled
						if (doc.isPublicable()==false && doc.isDisableAfterEnd() && (doc.getPublishEnd()>0) && (now >= doc.getPublishEnd())) {
							removeAfterEndList.add(doc);
						}
					}
				}

				//execute changes
				for (DocHistory doc : copyDHtoD) {
					try {
                        copyDHistory(doc, docDB);
                    } catch (Exception exception) {
                        Logger.error(DocPublishService.class, "Could not publish history " + doc.getHistoryId());
                        Logger.error(DocPublishService.class, exception);
                    }
				}
				for (DocDetails doc : removeAfterEndList) {
					disableAfterEnd(doc);
				}

				//publicableDocs is refreshed in DocDB.updateInternalCache call
			}
		}
	}

	/**
	 * Publishes a scheduled history record, including optional planned assets and media.
	 * @param queuedHistory history record selected for publication
	 * @param docDB document cache used for publication follow-up operations
	 * @return the published page, or null when the record is no longer eligible
	 */
    synchronized DocDetails copyDHistory(DocHistory queuedHistory, DocDB docDB) {
        prepareRepositories();

        // Prevent overlapping or recursive publication of this page within the same JVM.
        // This guard does not coordinate publication across cluster nodes.
        int docId = queuedHistory.getDocId();
        if (!publishing.add(docId)) return null;
        RequestBean previous = SetCharacterEncodingFilter.getCurrentRequestBean();
        try {
            // Recheck the selected history record: the cached queue may contain a version
            // that was already published, rescheduled, rejected or returned for approval.
            DocHistory docHistory = dhr.findById(queuedHistory.getId()).orElse(null);
            if (docHistory == null || !Boolean.TRUE.equals(docHistory.getPublicable()) ||
                docHistory.getPublishStart() <= 0 || docHistory.getPublishStart() > Tools.getNow() ||
                Tools.isNotEmpty(docHistory.getAwaitingApprove()) ||
                (docHistory.getDisapprovedBy() != null && docHistory.getDisapprovedBy() > 0) || Boolean.TRUE.equals(docHistory.getIsDelete())) {
                docDB.readPagesToPublic();
                return null;
            }

            // Skip missing/trashed pages and pause initialized plans while the feature is disabled.
            // With planned publishing enabled, only the newest pending history is eligible.
            GroupsDB groupsDB = GroupsDB.getInstance();
            DocDetails currentPage = ddr.findById(docId);
            if (currentPage == null || groupsDB.isInTrash(currentPage.getGroupId())) return null;
            boolean plannedEnabled = PlannedAccess.enabled();
            if (!plannedEnabled && ddr.isPlannedMediaInitialized(docId)) return null;
            PlannedChangesService planned = plannedEnabled ? PlannedChangesService.getInstance() : null;
            if (planned != null && !docHistory.getId().equals(planned.activeHistory(docId))) return null;

            // Use the page's domain and author for domain-scoped media, asset permissions and audit,
            // independently of the visitor or administrator whose request triggered publication.
            UserDetails author = UsersDB.getUser(docHistory.getAuthorId());
            Identity user = author == null ? null : new Identity(author);
            RequestBean publication = new RequestBean();
            GroupDetails group = groupsDB.getGroup(plannedEnabled ? currentPage.getGroupId() : docHistory.getGroupId());
            if (group != null) publication.setDomain(group.getDomainName());
            publication.setUser(user);
            SetCharacterEncodingFilter.setCurrentRequestBean(publication);

            // Build the page from its history snapshot. Planned publication retains the current
            // URL and folder, publishes staged files and rewrites their URLs before saving the page.
            DocDetails docDetails = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(docHistory);
            Logger.debug(this, "Publishing from historyId: " + docDetails.getHistoryId() + " docId: " + docId);
            if (planned != null) {
                docDetails.setVirtualPath(currentPage.getVirtualPath());
                docDetails.setGroupId(currentPage.getGroupId());
                planned.preparePublication(docDetails, user);
            }

            // Restore the regular publication metadata: visibility, perex groups, folder path
            // and root groups. The live page must no longer request a future publication.
            docDetails.setAvailable(true);
            docDetails.setPerexGroupString(docDetails.getPerexGroupIdsString(true));
            docDetails.setFileName(group != null && !group.isInternal() ? groupsDB.getGroupNamePath(docDetails.getGroupId()) : null);
            int[] rootGroups = DocDB.getRootGroupL(docDetails.getGroupId(), null, -1);
            docDetails.setRootGroupL1(rootGroups[0]);
            docDetails.setRootGroupL2(rootGroups[1]);
            docDetails.setRootGroupL3(rootGroups[2]);
            docDetails.setPublishAfterStart(false);

            // Flush the page while the publication audit marker is present, so the JPA listener
            // records this as publication. History remains pending if this write fails.
            RequestBean.addAuditValue("publishStatus", "Webpage was published");
            try {
                ddr.saveAndFlush(docDetails);
            } finally {
                RequestBean.removeAuditValue("publishStatus");
            }

            // Replace live media after the page write. The service preserves the planned source
            // set until all live copies are saved, allowing a retry after a partial failure.
            if (planned != null) planned.publishMedia(docDetails);

            // Propagate published content to multigroup copies, preserving each copy's URL,
            // redirect and folder. Read the master from the database after the page flush.
            DocDetails masterDocDetails = null;
            for(Integer slaveDocId : MultigroupMappingDB.getSlaveDocIds(docDetails.getDocId()))
            {
                if (masterDocDetails == null) masterDocDetails = docDB.getDoc(docDetails.getDocId(), -1, false);
                DocDetails slaveDoc = docDB.getBasicDocDetails(slaveDocId, false);
                if (slaveDoc != null) {
                    masterDocDetails.setVirtualPath(slaveDoc.getVirtualPath());
                    masterDocDetails.setExternalLink(slaveDoc.getExternalLink());
                    masterDocDetails.setDocId(slaveDocId);
                    masterDocDetails.setGroupId(slaveDoc.getGroupId());
                    DocDB.saveDoc(masterDocDetails);
                }

            }

            // Copy the revision's editor note to the live page, retaining its author and timestamp.
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

            // Rewrite only the published history snapshot, then mark it as the current version.
            // These status updates follow the page, media and related save calls so exceptions
            // in the preceding steps leave the revision scheduled for another attempt.
            if (planned != null) {
                planned.rewriteContent(docHistory);
                dhr.saveAndFlush(docHistory);
            }
            dhr.getHisotryIdsByDocIdIn(docId).ifPresent(ids -> dhr.updateActual(false, ids));
            dhr.updatePublicableAndActual(false, true, docDetails.getAuthorId(), true, docDetails.getHistoryId());

            // Remove staged working files only after history completion; publication backups remain.
            if (planned != null) planned.cleanupPublishedFiles(docId);

            // Notify the author when configured, then refresh the page and publication queue caches.
            if( Constants.getBoolean("webpagesNotifyAutorOnPublish") == true ) sendMailToPageAuthor(docHistory, docDB);

            docDB.updateInternalCaches(docDetails.getDocId());

            // Refresh templates only for pages in System or the configured template-content folders.
            if(docDetails.getFullPath().startsWith("/System") || docDetails.getGroupId() == Constants.getInt("tempGroupId") || docDetails.getGroupId() == Constants.getInt("menuGroupId") || docDetails.getGroupId() == Constants.getInt("headerFooterGroupId"))
            {
                TemplatesDB.getInstance(true);
            }
            return docDetails;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Planned asset publication failed", exception);
        } finally {
            // Restore the triggering request and release the per-page guard on success, skip or error.
            SetCharacterEncodingFilter.setCurrentRequestBean(previous);
            publishing.remove(docId);
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

			if (dhr != null && ddr != null) {

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

				dt.diff("done, size="+publicableDocs.size());
			}
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
