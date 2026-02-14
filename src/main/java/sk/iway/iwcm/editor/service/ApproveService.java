package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveEntity;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Sluzby spojene so schvalovanim web stranok
 */
@Service
@RequestScope
public class ApproveService {

    //repozitare
    private GroupsApproveRepository approveRepo;

    //autowired
    private HttpServletRequest request;

    //privatne objekty
    private DocDB docDB;
    private GroupsDB groupsDB;
    private Identity currentUser;
    private Prop prop;

	//Users that are approvers
    private Map<Integer, UserDetails> approveByTable;
	//approve table for level2
	private Map<Integer, UserDetails> approveByTableLevel2;
	//Users that we need notify
    private Map<Integer, UserDetails> notifyTable;

	//To check if page was self approved
	private boolean selfApproved = false;
	public boolean isSelfApproved() {
		return this.selfApproved;
	}

	private static final String[] diffBlacklistedProperties = {
		"data",
		"dataAsc",
		"fileName",
		"lastUpdateDate",
		"dateCreatedString",
		"perex",
		"authorId",
		"rootGroupL1",
		"rootGroupL2",
		"rootGroupL3",
		"perexPre",
		"timeCreatedString",
		"syncStatus",
		"lastUpdateTime",
		"historyApprovedBy",
		"groupId",
		"perexImageOriginal",
		"historyApprovedByName",
		"historySaveDate",
		"publishStartString",
		"syncId",
		"historyDisapprovedByName",
		"perexImageNormal",
		"historyId",
		"perexGroupIdsString",
		"lazyLoaded",
		"syncDefaultForGroupId",
		"docLink",
		"viewsTotal",
		"syncRemotePath",
		"historyActual",
		"forumCount",
		"authorEmail",
		"historyApproveDate",
		"docId",
		"publicable",
		"publishStartStringExtra",
		"authorPhoto",
		"tempName",
		"perexImageSmall",
		"currency",
		"eventDateString",
		"quantity",
		"publishEndTimeString",
		"pageNewChangedStatus",
		"publishEndString",
		"historyDisapprovedBy",
		"publishStartTimeString",
		"logonPageDocId",
		"eventTimeString"
	};

    @Autowired
    public ApproveService(GroupsApproveRepository approveRepo, DocHistoryRepository docHistoryRepo, HttpServletRequest request) {
        this.approveRepo = approveRepo;
        this.request = request;
        this.prop = Prop.getInstance(request);

        this.docDB = DocDB.getInstance();
        this.groupsDB = GroupsDB.getInstance();
        this.currentUser = UsersDB.getCurrentUser(request);

		this.approveByTable = new HashMap<>();
        this.notifyTable = new HashMap<>();
    }

    /**
     * Initializes hash tables approveByTable and notifyTable (also set selfApproved)
     * @param currentGroupId
    */
	public void loadApproveTables(int currentGroupId) {
        approveByTable = new HashMap<>();
        notifyTable = new HashMap<>();
		approveByTableLevel2 = new HashMap<>();

		loadApproveTablesLogic(currentGroupId);
	}

	/**
	 * Initializes hash tables approveByTable and notifyTable (also set selfApproved), whole logic
	 *
	 * @param currentGroupId
	 */
	private void loadApproveTablesLogic(int currentGroupId) {

		boolean isLevel2Approver = false;
		boolean isApprover = false;

		int depth = 0;
		int maxDepth = 30;
		GroupDetails group;
		int mode;
		int failsafe = 0;

		while (failsafe++<100) {
			group = groupsDB.getGroup(currentGroupId);
            depth++;
			if (group == null || depth > maxDepth || currentGroupId == 0) {
				//group doesn't exist
				break;
			}

			//Inicialize support hashmaps
			HashMap<Integer, UserDetails> groupApprovers = new HashMap<>();
			HashMap<Integer, UserDetails> level2GroupApprovers = new HashMap<>();
			HashMap<Integer, UserDetails> notifyTableGroup = new HashMap<>();

			// Iterate groupsApprove list and check if currentUser isAdmin and isAuthorized
			// If yes set variables and etc, or remove GroupsApproveEntity from List
			for (GroupsApproveEntity entity : approveRepo.findByGroup(group)) {

				UserDetails groupApprover = UsersDB.getUserCached(entity.getUserId().intValue());

				//Must be authotized admin
				if(groupApprover==null || !groupApprover.isAdmin() || !groupApprover.isAuthorized()) continue;

                //Mode of approve
                mode = entity.getApproveMode();

				//Is currentUser (current logged) also groupApprover ?
				if(groupApprover.getUserId() == currentUser.getUserId()) {
					//Yes, currentUser is group approver

                    if(mode == UsersDB.APPROVE_LEVEL2) isLevel2Approver = true;
					//
					else if(mode == UsersDB.APPROVE_APPROVE) isApprover = true;
					//
					else if(mode == UsersDB.APPROVE_NOTIFY) notifyTableGroup.put(Integer.valueOf(groupApprover.getUserId()), groupApprover);

					//Can do anything on his own
					else if(mode == UsersDB.APPROVE_NONE) isLevel2Approver = true;
				} else {
					///No, currentUser is not group approver

					//
					if (mode == UsersDB.APPROVE_LEVEL2) level2GroupApprovers.put(Integer.valueOf(groupApprover.getUserId()), groupApprover);
					//
					else if (mode == UsersDB.APPROVE_APPROVE) groupApprovers.put(Integer.valueOf(groupApprover.getUserId()), groupApprover);
					//
					else if (mode == UsersDB.APPROVE_NOTIFY && groupApprover.getUserId() != currentUser.getUserId()) notifyTableGroup.put(Integer.valueOf(groupApprover.getUserId()), groupApprover);

					//if (mode == UsersDB.APPROVE_NONE) user can do what he want BUT he is NO approver and dont want to get any email -> so not involved
				}
			}

			currentGroupId = group.getParentGroupId();

			//pouzivame system best match
			if (approveByTable.isEmpty()) approveByTable.putAll(groupApprovers);

			//pouzivame system best match
			if (approveByTableLevel2.isEmpty()) approveByTableLevel2.putAll(level2GroupApprovers);

			//notify davame vsetkym
			notifyTable.putAll(notifyTableGroup);
		}

		if (isApprover == false && approveByTable.isEmpty() && approveByTableLevel2.isEmpty()==false) {
			//we dont have first level approver but have second, so the second level will be first
			approveByTable.putAll(approveByTableLevel2);
			approveByTableLevel2.clear();
			if (isLevel2Approver) {
				isLevel2Approver = false;
				isApprover = true;
			}
		}

		//Check if currentUser is level2Approver, highest approve right
		if(isLevel2Approver) {
			//currentUser IS level2Approver

			//Clear approve table (dont need approve)
			approveByTable.clear();
			approveByTableLevel2.clear();
			//Mark as self approved
			selfApproved = true;
		} else {
			//currentUser ISN'T level2Approver
			if(isApprover) {
				//currentUser IS approver

				//Clear approve table (dont need approve)
				approveByTable.clear();
				selfApproved = true;
				//Mark as self approved
				if (approveByTableLevel2.isEmpty()==false) {
					//simulate level2 in approveByTable (it's the same process)
					approveByTable.putAll(approveByTableLevel2);
					selfApproved = false;
				}
			} else {
				/**
				 * Just to understand, currentUser ISNT'T level2Approver nor approver.
				 *
				 * If "approveByTable.size() > 0", then this must by approved (but not by currentUser).
				 *
				 * If "! approveByTable.size() > 0", then this dont need to be approved so anyone can do insert/edit.
				 */
			}
		}
	}

	/**
	 * Approve action, in other word's approve/reject webpage change (aka waiting docHistory).
	 * !! request param "zamietni" indicates, if we approve action (webpage will be change like in docHistory) or reject action (no change to webpage)
	 * After approve/reject, the waiting docHistory will be removed from waiting list (awaitingApprove param is set as empty string).
	 * @param docHistoryRepo
	 * @param editorService
	 * @return return true if approve was success
	 */
	public boolean approveAction(DocHistoryRepository docHistoryRepo, DocDetailsRepository docDetailsRepository, EditorService editorService) {

		//Get and check historyId
		int historyId = Tools.getIntValue(request.getParameter("historyid"), -1);
		if(historyId == -1) {
			request.setAttribute("message", "History ID cant be empty");
			return false;
		}

		Optional<DocHistory> docHistoryOpt = docHistoryRepo.findById(Long.valueOf(historyId));
		if(!docHistoryOpt.isPresent()) {
			request.setAttribute("message", "History record by givven historyId not found");
			return false;
		}
		DocHistory docHistory = docHistoryOpt.get();

		//Load approve tables, this step will show us if current user can approve/dis-approve docHistory uppon certain docDetails
		loadApproveTables(docHistory.getGroupId());

		if(needApprove()==false || selfApproved) {
			//Doc, do not need approve OR
			//current user IS self approver, can approve or reject insert/update action

			//Approve - CREATE / UPDATE
			if (request.getParameter("zamietni") == null) {

				//Cancel waiting docHistory (aka waiting webpage update)
				editorService.approveDocHistory(docHistory);

				//Send notification emails
				sendWebpageApproveNotification(true, docHistory);

				//SET this as APPROVE indicator
				request.setAttribute("approved", "true");
			} else { //Reject - CREATE / UPDATE

				//Cancel waiting docHistory (aka waiting webpage update)
				docHistoryRepo.rejectDocHistory("", currentUser.getUserId(), historyId);

				//Send notification emails
				sendWebpageApproveNotification(false, docHistory);

				//SET this as REJECT indicator
				request.setAttribute("rejected", "true");
			}

			return true;
		} else if (approveByTableLevel2.isEmpty()==false) {
			//if there are also level2 approvers update approvers ID and send notifications
			docHistory.setAwaitingApprove("," + getApproveUserIds() + ",");
			docHistoryRepo.save(docHistory);

			Identity user = UsersDB.getCurrentUser(request);

			//posli mail schvalovatelovi level2
			String notes = request.getParameter("notes");
			if (Tools.isEmpty(notes)) notes = prop.getText("components.reservation.reservation_list.accept");
			notes = prop.getText("approve.page.approvedToNextLevel.comment", user.getFullName(), notes);

			String approverNames = getApproveUserNames();

			sendWebpageApproveRequestEmail(docHistory, historyId, currentUser.getUserId(), notes, docDetailsRepository);
			request.setAttribute("approvedToNextLevel", prop.getText("approve.page.approvedToNextLevel", approverNames));

			//posli info email autorovi
			String subject = prop.getText("approve.page.approvedToNextLevel.editorSubject", docHistory.getTitle());

			String url = Tools.getBaseHref(request) + "/showdoc.do?docid="+docHistory.getDocId()+"&historyid=" + historyId;

			StringBuilder message = new StringBuilder(prop.getText("approve.page.approvedToNextLevel.editorText", docHistory.getTitle(), approverNames)).append("<br>\n");
			message.append(prop.getText("approve.dir")).append(": ").append(groupsDB.getGroupNamePath(docHistory.getGroupId())).append("<br><br>\n");
			message.append(prop.getText("approve.url")).append(":<br>\n");
			message.append("<a href='").append(url).append("'>").append(url).append("</a><br><br>\n");
			message.append(notes).append("<br>\n");
			UserDetails author = UsersDB.getUserCached(docHistory.getAuthorId());
			if (author != null && Tools.isEmail(author.getEmail())) {
				SendMail.send(user.getFullName(), user.getEmail(), author.getEmail(), subject, message.toString(), request);
			}

			return true;
		} else {
			//Doc NEED's approve, BUT current user is NOT selfApprover, so he can't approve this shit

			request.setAttribute("message", prop.getText("approveAction.err.cantApprove"));
			return false;
		}
	}

	/**
	 * Approve delete action of webpage, in other word's approve/reject delete change (aka waiting docHistory).
	 * !! request param "zamietni" indicates, if we approve action (webpage gonna be deleted) or reject action (no change to webpage)
	 * After approve/reject, the waiting docHistory will be removed from waiting list (awaitingApprove param is set as empty string).
	 * @param docHistoryRepo
	 * @param docDetailsRepo
	 * @param editorService
	 * @return retunr link to error or success page
	 */
	public boolean approveDelAction(DocHistoryRepository docHistoryRepo, DocDetailsRepository docDetailsRepo, EditorService editorService) {


		//Get and check historyId, MUST by set
		int historyId = Tools.getIntValue(request.getParameter("historyid"), -1);
		if(historyId == -1) {
			request.setAttribute("message", "History ID cant be empty");
			return false;
		}

		//Check DocHistory for set historyId
		Optional<DocHistory> docHistoryOpt = docHistoryRepo.findById(Long.valueOf(historyId));
		if(!docHistoryOpt.isPresent()) {
			request.setAttribute("message", "History record by givven historyId not found");
			return false;
		}
		DocHistory docHistory = docHistoryOpt.get();

		//Load approve tables, this step will show us if current user can approve/reject docHistory uppon certain docDetails
		loadApproveTables(docHistory.getGroupId());

		//Reject DELETE action
		if (request.getParameter("zamietni") != null) {

			if(!needApprove() || selfApproved) {

				//Set page as available, only in case that the are NO other delete request's

				docDetailsRepo.updateAvailableStatus(true, docHistory.getDocId());

				//vymaz z historie
				docHistoryRepo.deleteById(Long.valueOf(historyId));

				//Send mails
				sendWebpageApproveDelNotification(false, docHistory);

				//SET this as Reject SUCCESS indicator
				request.setAttribute("rejected", "true");

				return true;
			} else {
				//DOC need's approve BUT current user is NOT selfApprover, so he can't approve this shit

				request.setAttribute("message", prop.getText("approveAction.err.cantApprove"));
				return false;
			}
		} else {
			//Approve DELETE action

			if (needApproveLevel2()) {
				String level2approvers = "," + getApproveUserIds() + ",";
				//if approvers are not level2 update hitory to level2 approvers and send notifications
				if (level2approvers.equals(docHistory.getAwaitingApprove())==false) {
					//aktualizuj ID schvalovatela
					docHistory.setAwaitingApprove(level2approvers);
					docHistoryRepo.save(docHistory);

					Identity user = UsersDB.getCurrentUser(request);

					//posli mail schvalovatelovi level2
					String notes = request.getParameter("notes");
					if (Tools.isEmpty(notes)) notes = prop.getText("components.reservation.reservation_list.accept");
					notes = prop.getText("approve.page.approvedToNextLevel.comment", user.getFullName(), notes);

					String approverNames = getApproveUserNames();

					sendWebpageApproveDelRequestEmail(docHistory, historyId, historyId);
					request.setAttribute("approvedToNextLevel", prop.getText("approve.page.approvedToNextLevel", approverNames));

					//posli info email autorovi
					String subject = prop.getText("approve.delete.subject") + " " + docHistory.getTitle();

					String url = Tools.getBaseHref(request) + "/showdoc.do?docid="+docHistory.getDocId()+"&historyid=" + historyId;

					StringBuilder message = new StringBuilder(prop.getText("approve.page.approvedToNextLevel.editorText", docHistory.getTitle(), approverNames)).append("<br>\n");
					message.append(prop.getText("approve.dir")).append(": ").append(groupsDB.getGroupNamePath(docHistory.getGroupId())).append("<br><br>\n");
					message.append(prop.getText("approve.url")).append(":<br>\n");
					message.append("<a href='").append(url).append("'>").append(url).append("</a><br><br>\n");
					message.append(notes).append("<br>\n");
					UserDetails author = UsersDB.getUserCached(docHistory.getAuthorId());
					if (author != null && Tools.isEmail(author.getEmail())) {
						SendMail.send(user.getFullName(), user.getEmail(), author.getEmail(), subject, message.toString(), request);
					}

					return true;
				}
			}

			//Perrmisions and approves are tested inside customDeleteWebpage
			String result = editorService.deleteWebpageLogic(docHistory.getDocId(), this, true);

			if("success".equals(result)) {
				//SET this as Approve SUCCESS indicator
				request.setAttribute("approved", "approved");

				//Send mails
				sendWebpageApproveDelNotification(true, docHistory);
			} else
				//SET this as Approve FAIL indicator
				request.setAttribute("approveFail", "approveFail");

			return true;
		}
	}

	/* SUPPORT METHODS */

	/**
	 * Return true, if webpage need's approve (approveByTable isn't empty).
	 * @return
	 */
    public boolean needApprove() {
        return approveByTable != null && approveByTable.isEmpty() == false;
    }

	public boolean needApproveLevel2() {
        return approveByTableLevel2 != null && approveByTableLevel2.isEmpty() == false;
    }

	/**
	 *
	 * @return
	 */
	public boolean needNotification() {
		return notifyTable != null && notifyTable.isEmpty() == false;
	}

	/**
	 * If approving is needed, return list of approvers.
	 * !! If action is selfApproved, there are no set approvers.
	 * @return
	*/
	public List<UserDetails> getApprovers() {
		return new ArrayList<>(approveByTable.values());
	}

	/**
	 * Vrati ciarkou oddeleny zoznam ID pouzivatelov, ktory schvaluju web stranku
	 * @return
	 */
    public String getApproveUserIds() {
        StringBuilder approveByUsersId = new StringBuilder(",");
        for (UserDetails approveUser : approveByTable.values())
            approveByUsersId.append(approveUser.getUserId()).append(',');

        return approveByUsersId.toString();
    }

	/**
	 *
	 * @return
	 */
	private String getApproveUserEmails() {
		StringBuilder approveEmails = new StringBuilder();
		for (UserDetails approveUser : approveByTable.values()) {
			if(Tools.isEmail(approveUser.getEmail())) {
				if(approveEmails.length() > 0) approveEmails.append(",");
				approveEmails.append(approveUser.getEmail());
			}
		}

		return approveEmails.toString();
	}

	public String getApproveUserNames() {
		StringBuilder approveEmails = new StringBuilder();
		for (UserDetails approveUser : approveByTable.values()) {
			if(approveEmails.length() > 0) approveEmails.append(",");
			approveEmails.append(approveUser.getFullName());
		}

		return approveEmails.toString();
	}


	public String getEmailsToNotify(String authorEmail) {
		StringBuilder notifyEmails = new StringBuilder();

		for (UserDetails approveUser : notifyTable.values()) {
			if(Tools.isEmail(approveUser.getEmail())) {
				//There is no need send notify to approver, who approve action (hi did it)
				if(currentUser.getUserId() == approveUser.getUserId()) continue;
				if(notifyEmails.length() > 0) notifyEmails.append(",");
				notifyEmails.append(approveUser.getEmail());
			}
		}

		//Add author email
		if (Tools.isEmail(authorEmail)) {
			if(notifyEmails.length() > 0) notifyEmails.append(",");
			notifyEmails.append(authorEmail);
		}

		return notifyEmails.toString();
	}

	/**
	 * Odoslanie emailov o schvalovani alebo notifikacii o zmene (ak je zoznam schvalovani prazdny)
	 * @param editedDoc
	 * @param historyId
	 */
    public void sendEmails(DocDetails editedDoc, int historyId, DocDetailsRepository docDetailsRepository) {
        //Posielanie ziadosti o schvaleni web stranky
		if (approveByTable != null && approveByTable.size() > 0) {
			//Posielanie ziadosti o schvaleni web stranky
			sendWebpageApproveRequestEmail(editedDoc, historyId, currentUser.getUserId(), null, docDetailsRepository);
		} else if (notifyTable != null && notifyTable.size() > 0) {
			//Posielanie notifikacii o zmene vo web stranke, neposiela sa, ak sa stranka musi schvalit, preto v else podmienke
			sendWebpageChangeNotification(editedDoc);
		}
    }

	/* Request email */

	/**
	 * Odosle email schvalovatelom web stranky s odkazom na schvalenie/zamietnutie stranky
	 * @param editedDoc
	 * @param historyId
	 * @param senderUserId
	 * @param comment
	 */
    public void sendWebpageApproveRequestEmail(DocBasic editedDoc, int historyId, int senderUserId, String comment, DocDetailsRepository docDetailsRepository) {

        if (approveByTable == null || approveByTable.isEmpty()) return;

		String title = null;
		int docId = -1;
		int groupId = -1;

		if(editedDoc != null) {
			title = editedDoc.getTitle();
			docId = editedDoc.getDocId();
			groupId = editedDoc.getGroupId();
		}

		if (title == null || docId < 0 || groupId < 0) return;

		UserDetails senderUser = UsersDB.getUser(senderUserId);
		if(senderUser == null) return;

		String url = Tools.getBaseHref(request) + "/admin/approve.jsp?docid="+docId+"&historyid=" + historyId;

		StringBuilder message = new StringBuilder("<strong>").append(prop.getText("approve.ask")).append(":</strong><br>\n");
		message.append("<strong>").append(title).append("</strong><br>\n");
		message.append(prop.getText("approve.dir")).append(":<br>\n");
		message.append(groupsDB.getPath(groupId)).append("<br><br>\n\n");
		message.append(prop.getText("approve.url")).append(":<br>\n");
		message.append("<a href='").append(url).append("'>").append(url).append("</a><br><br>\n\n");

		if (Tools.isNotEmpty(comment)) { message.append(comment).append("<br><br>\n\n"); }

		message.append(senderUser.getFullName()).append(" &lt;").append(senderUser.getEmail()).append("&gt;");

		if (editedDoc != null) {
			//test if this is a new document - for new one there will be only one record
			DocBasic historyDoc = null;
			int historyCount = (new SimpleQuery()).forInt("SELECT COUNT(*) FROM documents_history WHERE doc_id = ?", editedDoc.getDocId());
			if (historyCount > 1) {
				historyDoc = docDetailsRepository.findById( editedDoc.getDocId() );
			}
			message.append(getDiff(editedDoc, historyDoc, prop));
		}

		String subject = prop.getText("approve.subject") + ": " + title;
		SendMail.send(senderUser.getFullName(), senderUser.getEmail(), getApproveUserEmails(), subject, message.toString(), request);

		auditApproveRequestEmail(editedDoc, historyId);
	}

	/**
	 * Vytvori diff medzi originalom a editovanym dokumentom, tento diff sa pouzije v emaily schvalovatelom, aby videli co sa zmenilo
	 * @param editedDoc
	 * @param docDetailsRepository
	 * @return
	 */
	public static StringBuilder getDiff(DocBasic editedDoc, DocBasic historyDoc, Prop prop) {
		StringBuilder message = new StringBuilder();
		if(editedDoc != null) {
			BeanDiff diff = new BeanDiff().skipEmpty().setNew(editedDoc).setOriginal(historyDoc);
			diff.blacklist(diffBlacklistedProperties);

			if(diff.diff().size() > 0) {
				// Add to message compare with changed values
				message.append("<br>\n<br>\n");

				if (historyDoc == null) message.append(prop.getText("doc.approve.new_params"));
				else message.append(prop.getText("doc.approve.changed_params"));

				message.append(": ");

				String adminlogChanges = new BeanDiffPrinter(diff).toString(prop);
				adminlogChanges = ResponseUtils.filter(adminlogChanges);
				adminlogChanges = Tools.replaceRegex(adminlogChanges, "(?m)^", "<br>\n", true);
				message.append( adminlogChanges );
			}
		}
		return message;
	}

	/**
	 * Zapise audit odoslaneho emailu pre ziadost o schvalenie
	 * @param editedHistory
	 * @param historyId
	 * @param approveByTable
	 */
	private void auditApproveRequestEmail(DocBasic editedDoc, int historyId) {
		//start- navrh na zmenu logovania v CMS (#19820)
		StringBuilder approveByUsersEmail = new StringBuilder();
		for (UserDetails u : approveByTable.values()) {
			if (Tools.isEmail(u.getEmail())) {
				if (approveByUsersEmail.length()>0) approveByUsersEmail.append(",");
				approveByUsersEmail.append(u.getEmail());
			}
		}

		//Popis pre adminLog prerobený cez StringBuilder kvôli lepšej prehľadnosti + ľahšia úprava
		StringBuilder adminLogSB = new StringBuilder();
		adminLogSB.append("Poziadane o schvalenie stranky ");
		adminLogSB.append(editedDoc.getTitle() + " / " + editedDoc.getDocId() + " / " + historyId);
		adminLogSB.append(", ziadost zaslana na emaili: " + approveByUsersEmail.toString());
		Adminlog.add(Adminlog.TYPE_SAVEDOC, adminLogSB.toString(), editedDoc.getDocId(), historyId);
		//koniec- navrh na zmenu logovania v CMS (#19820)
	}

	public void sendWebpageApproveDelRequestEmail(DocBasic editedDoc, int historyId, int actualPublishedHistoryId) {
		String title = editedDoc.getTitle();
		title = Tools.replace(title, "[DELETE] ", "");
		String navbar = GroupsDB.getInstance().getNavbar(editedDoc.getGroupId());
		int docId = editedDoc.getDocId();

		if (actualPublishedHistoryId == 0) actualPublishedHistoryId = historyId;
		String url = Tools.getBaseHref(request) + "/admin/approve_delete.jsp?historyid=" + historyId + "&docid=" + docId + "&lasthid=" + actualPublishedHistoryId;

        StringBuilder message = new StringBuilder("<b>").append(prop.getText("approve.delete.ask")).append(":</b><br>\n");
        message.append(title).append("<br>\n");
        message.append(prop.getText("approve.dir")).append(":<br>\n");
        message.append(navbar).append("<br><br>\n\n");
        message.append(prop.getText("approve.url")).append(":<br>\n");
        message.append("<a href='").append(url).append("'>").append(url).append("</a><br><br>\n\n");
        message.append(currentUser.getFullName()).append(" &lt;").append(currentUser.getEmail()).append("&gt;");

        String subject = prop.getText("approve.delete.subject") + ": " + title;

        SendMail.send(currentUser.getFullName(), currentUser.getEmail(), getApproveUserEmails(), subject, message.toString(), request);
	}

	/* Notification emails */

	/**
	 * Send email with notification about web page change to all notify users
	 * @param editedHistory
	 */
	private void sendWebpageChangeNotification(DocDetails editedHistory) {
		//If there is no one to notify, return
		if(!needNotification()) return;

		String url = docDB.getDocLink(editedHistory.getDocId(), null, true, request);
		StringBuilder message = new StringBuilder("<strong>").append(prop.getText("doc.notify.intro")).append(":</strong><br>\n");
		message.append("<strong>").append( editedHistory.getTitle() ).append("</strong><br>\n");
		message.append(prop.getText("doc.notify.dir")).append(":<br>\n");
		message.append(groupsDB.getPath(editedHistory.getGroupId())).append("<br><br>\n\n");
		message.append(prop.getText("doc.notify.url")).append(":<br>\n");
		message.append("<a href='"+url+"'>").append(url).append("</a><br><br>\n\n");
		message.append(currentUser.getFullName()).append(" &lt;").append(currentUser.getEmail()).append("&gt;");
		String subject = prop.getText("doc.notify.subject") + ": " + editedHistory.getTitle();
		SendMail.send(currentUser.getFullName(), currentUser.getEmail(), getEmailsToNotify(null), subject, message.toString(), request);
	}

	/**
	 * Send email with notification about status (Approved / Reject) of requestet web page change to all notify users + author of change
	 * @param isApproved - true change was Approved, else Rejected
	 * @param docHistory - change represented by docHistory entity that was approved/rejected
	 */
	private void sendWebpageApproveNotification(boolean isApproved, DocHistory docHistory) {
		if(isApproved) {
			//DocHistory Create/Edit action - APPROVED

			String url = Tools.getBaseHref(request) + "/showdoc.do?docid=" + docHistory.getDocId();
			StringBuilder message = new StringBuilder("<b>").append(prop.getText("approve.ok.webPage")).append(":</b><br>\n");
			message.append( docHistory.getTitle() ).append( "<br>\n");
			message.append(prop.getText("approve.dir")).append(": ").append( groupsDB.getGroupNamePath(docHistory.getGroupId()) ).append( "<br>\n");
			message.append("<font color='green'>").append(prop.getText("approve.ok")).append("</font><br><br>\n");
			message.append(prop.getText("approve.url")).append( ":<br>\n");
			message.append("<a href='").append(url).append("'>").append(url).append("</a><br><br>\n\n");
			message.append("<b>").append(prop.getText("approve.reject.notes") ).append( ":</b><br>\n");
			message.append( Tools.getStringValue(request.getParameter("notes"), "") );
			String subject = prop.getText("approve.ok.subject") + ": " + docHistory.getTitle();
			SendMail.send(currentUser.getFullName(), currentUser.getEmail(), getEmailsToNotify(docHistory.getUserDetails().getEmail()), subject, message.toString(), request);
		} else {
			//DocHistory Create/Edit action - REJECTED

			String url = Tools.getBaseHref(request) + "/showdoc.do?docid=" + docHistory.getDocId() + "&historyid=" + docHistory.getHistoryId();
			StringBuilder message = new StringBuilder("<font color='red'>").append(prop.getText("approve.reject")).append(":</font><br>\n");
			message.append( docHistory.getTitle() ).append( "<br>");
			message.append(prop.getText("approve.dir")).append(": " ).append( groupsDB.getGroupNamePath(docHistory.getGroupId()) ).append( "<br><br>\n");
			message.append(prop.getText("approve.url") ).append( ":<br>\n");
			message.append("<a href='").append(url).append("'>" ).append( url ).append( "</a><br><br>\n");
			message.append("<b>").append(prop.getText("approve.reject.notes") ).append( ":</b><br>\n");
			message.append( Tools.getStringValue(request.getParameter("notes"), "") );
			String subject = prop.getText("approve.reject.subject") + ": " + docHistory.getTitle();
			SendMail.send(currentUser.getFullName(), currentUser.getEmail(), getEmailsToNotify(docHistory.getUserDetails().getEmail()), subject, message.toString(), request);
		}
	}

	/**
	 * Send email with notification about status (Approved / Reject) of requested web page delete to all notify users + author of change
	 * @param isApproved
	 * @param docHistory
	 */
	private void sendWebpageApproveDelNotification(boolean isApproved, DocHistory docHistory) {
		StringBuilder message;
		String subject;
		String url = Tools.getBaseHref(request) + "/showdoc.do?docid=" + docHistory.getDocId();

		if(isApproved) {
			//DocHistory Delete action - APPROVED
			message = new StringBuilder("<font color='green'>").append(prop.getText("approve.del.approved") ).append( ":</font><br>\n");
			subject = prop.getText("approve.del.approve.subject") + ": " + docHistory.getTitle();
		} else {
			//DocHistory Delete action - REJECT
			message = new StringBuilder("<font color='red'>").append(prop.getText("approve.del.reject") ).append( ":</font><br>\n");
			subject = prop.getText("approve.del.reject.subject") + ": " + docHistory.getTitle();
		}

		message.append( docHistory.getTitle() ).append( "<br>");
		message.append(prop.getText("approve.dir")).append(": " ).append( groupsDB.getGroupNamePath(docHistory.getGroupId()) ).append( "<br><br>\n");

		if(!isApproved) {
			message.append(prop.getText("approve.url") ).append( ":<br>\n");
			message.append("<a href='").append(url).append("'>" ).append( url ).append( "</a><br><br>\n");
		}

		message.append("<b>").append(prop.getText("approve.reject.notes") ).append( ":</b><br>\n");
		message.append( Tools.getStringValue(request.getParameter("notes"), "") );
		SendMail.send(currentUser.getFullName(), currentUser.getEmail(), getEmailsToNotify(docHistory.getUserDetails().getEmail()), subject, message.toString(), request);
	}
}
