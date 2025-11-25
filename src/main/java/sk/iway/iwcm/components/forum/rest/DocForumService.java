package sk.iway.iwcm.components.forum.rest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.multipart.MultipartFile;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.ForumTools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.components.forum.jpa.DocForumIdAndParentId;
import sk.iway.iwcm.components.forum.jpa.DocForumRepository;
import sk.iway.iwcm.components.forum.jpa.ForumGroupEntity;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.forum.ForumDB;
import sk.iway.iwcm.forum.ForumSortBy;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.fulltext.indexed.Forums;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

public class DocForumService {

    private static final String SAVE_FORUM_SUCCESS = "/components/forum/saveok";

	private DocForumService() {
		//utility class
	}

	/**
	 * Represent recursive action's upon forum's.
	 */
	public enum ActionType {
		DELETE, //Delete forum recursive (with all child's)
		RECOVER, //Recover forum recursive (with all child's)
		APPROVE, //Approve forum recursive (with all child's)
		REJECT, //Reject forum recursive (with all child's)
		LOCK, //Lock forum recursive (with all child's)
		UNLOCK //Unlock forum recursive (with all child's)
	}

    /**
     * Return icon options for status icon select.
     * @param prop
     * @return
     */
    public static List<LabelValue> getStatusIconOptions(Prop prop) {
		List<LabelValue> icons = new ArrayList<>();

        icons.add(new LabelValue("<i class=\"ti ti-circle-check\" style=\"color: #00be9f;\"></i> " + prop.getText("apps.forum.icon.confirmed"), "confirmed:true"));
        icons.add(new LabelValue("<i class=\"ti ti-circle-x\" style=\"color: #ff4b58;\"></i> " + prop.getText("apps.forum.icon.non_confirmed"), "confirmed:false"));
        icons.add(new LabelValue("<i class=\"ti ti-lock\" style=\"color: #000000;\"></i> " + prop.getText("apps.forum.icon.non_active"), "active:false"));
        icons.add(new LabelValue("<i class=\"ti ti-lock-open\" style=\"color: #000000;\"></i> " + prop.getText("apps.forum.icon.active"), "active:true"));
        icons.add(new LabelValue("<i class=\"ti ti-trash\" style=\"color: #fabd00;\"></i> " + prop.getText("apps.forum.icon.deleted"), "deleted:true"));
        icons.add(new LabelValue("<i class=\"ti ti-trash-off\" style=\"color: #fabd00;\"></i> " + prop.getText("apps.forum.icon.not_deleted"), "deleted:false"));

		return icons;
	}

	/******************************* MAIN LOGIC methods (save / delete / recover / approve / reject) ****************************************** */

	/**
	 * Save given ForumFormBean entity, update and send confirmation notification email
	 *
	 * @param request
	 * @param response
	 * @param forumForm - entity to be saved (forum)
	 * @return
	 * @throws IOException
	 */
    public static String saveDocForum(HttpServletRequest request, HttpServletResponse response, DocForumEntity forumForm) {
		DocForumRepository docForumRepository = getDocForumRepository();
		HttpSession session = request.getSession();
		Identity sender = UsersDB.getCurrentUser(request);

		Integer domainId = Tools.getIntValue(DocDB.getDomain(request), 1);
		int userId = -1;
		String hashCode;

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		String language = rb.getLng();
		Prop prop = Prop.getInstance(language);

		if (sender == null || sender.isAdmin() == false) {
			synchronized (DocForumRepository.class) {
				//kontrola pred duplicitnym odoslanim prispevku (nejaka ajax haluz) - http://helpdesk.interway.sk/?bugID=4676
				//v access logu servera sa zaznamy skutocne nasli viac krat, preco nikto netusi
				String lastSessionText = (String)Tools.sessionGetAttribute(session, "DocForumRepository.lastText");
				if (lastSessionText!=null && lastSessionText.equals(forumForm.getQuestion())) {
					setPermissionDenied(request, "sameText");
					return SAVE_FORUM_SUCCESS;
				}

				Tools.sessionSetAttribute(session, "DocForumRepository.lastText", forumForm.getQuestion());
			}
		}

		//Get forum group aka setting
		ForumGroupEntity forumGroup = ForumGroupService.getForum(forumForm.getDocId(), true);

		//Check if parent we answering to (if set) is active
		//If not permit this comment / forum ...
		if(forumForm.getParentId() > 0) {
			boolean isParentActive = (new SimpleQuery()).forBoolean("SELECT active FROM document_forum WHERE forum_id=?", forumForm.getParentId());
			if(!isParentActive) {
				boolean isAdmin = false;
				if(forumGroup != null && forumGroup.isAdmin(sender)) isAdmin = true;

				//Only admin can be exception for restriction
				if(!isAdmin) {
					setError(request, prop.getText("components.forum.active.error"));
					return SAVE_FORUM_SUCCESS;
				}
			}
		}

		if (forumGroup == null) {
			//najdi parent adresar a skus podla neho
			DocDetails docDetails = DocDB.getInstance().getBasicDocDetails(forumForm.getDocId(), false);
			if (docDetails != null) {
				GroupDetails group = GroupsDB.getInstance().getGroup(docDetails.getGroupId());
				if (group != null) {
					GroupDetails parentGroup = GroupsDB.getInstance().getGroup(group.getParentGroupId());
					if (parentGroup!=null)
						forumGroup = ForumGroupService.getForum(parentGroup.getDefaultDocId(), true);
				}
			}
		}

        if (forumGroup == null) {
            DocDetails docDetails = DocDB.getInstance().getDoc(forumForm.getDocId());
            if (docDetails != null) {
                int defaultDocId = docDetails.getGroup().getDefaultDocId();
                DocDetails defaultDocDetails = DocDB.getInstance().getDoc(defaultDocId);
                forumGroup = ForumGroupService.getForum(defaultDocDetails.getDocId(), true);
            }
        }

		ForumGroupEntity forumGroupPerms = forumGroup;
		if (forumGroupPerms == null && Tools.isNotEmpty(Constants.getString("forumDefaultAddmessageGroups"))) {
			forumGroupPerms = new ForumGroupEntity();
			forumGroupPerms.setAddmessageGroups(Constants.getString("forumDefaultAddmessageGroups"));
		}

		if (forumGroupPerms != null && forumGroupPerms.canPostMessage(sender)==false) {
			//nema dostatocne prava (kontrola user groups)
			setError(request, "components.forum.wrong_user_groups_for_post");
			return SAVE_FORUM_SUCCESS;
		}

		if (ForumGroupService.isActive(forumForm.getDocId()) == false && (sender == null || sender.isAdmin() == false)) {
			setError(request, "components.forum.forum_closed");
			return SAVE_FORUM_SUCCESS;
		}

		if (isVulgar(forumForm.getAuthorName()) || isVulgar(forumForm.getAuthorEmail()) ||
			 isVulgar(forumForm.getQuestion()) || isVulgar(forumForm.getSubject()) ||
			 ShowDoc.getXssRedirectUrl(request)!=null)
		{
			request.setAttribute("isVulgar", "true");
			return SAVE_FORUM_SUCCESS;
		}

		String plainQuestion = SearchTools.htmlToPlain(forumForm.getQuestion());
		plainQuestion = Tools.replace(plainQuestion, "\n", "");
		plainQuestion = Tools.replace(plainQuestion, "\r", "");
		if ("true".equals(request.getParameter("forumAllowEmptyMessage"))==false &&
			 (Tools.isEmpty(forumForm.getQuestion()) || Tools.isEmpty(plainQuestion)))
		{
			setError(request, "components.forum.error.questionEmpty");
			return SAVE_FORUM_SUCCESS;
		}

		//fixni hlasku o nahravani editora (ak tam je)
		forumForm.setQuestion(Tools.replace(forumForm.getQuestion(), prop.getText("editor.loading_please_wait"), ""));

		//ak je vypnuty wysiwyg editor, tak nedovol HTML
		if (Constants.getBoolean("disableWysiwyg")) {
			String oldText = forumForm.getQuestion();
			String newText = oldText.replace("<", "&lt;");
			newText = newText.replace(">", "&gt;");
			newText = newText.replace("\n", "<br />");
			forumForm.setQuestion(newText);
		}

		//nastav linkam nofollow
		forumForm.setQuestion(Tools.replace(forumForm.getQuestion(), "<a href", "<a rel=\"nofollow\" href"));
		forumForm.setQuestion(Tools.replace(forumForm.getQuestion(), "<A HREF", "<a rel=\"nofollow\" href"));

		if (sender == null || sender.isAdmin() == false) forumForm.setId((long) -1);

		if (sender != null) userId = sender.getUserId();

		//Clear MsOffice tags from question
		forumForm.setQuestion( clearMsOfficeTags( forumForm.getQuestion() ) );

		hashCode = Password.generatePassword(15);

		if (sender != null && Tools.isNotEmpty(sender.getSignature()) && "true".equals(request.getParameter("addSignature"))) {
			if ("true".equals(request.getParameter("dontReplaceSignatureCodes")))
				forumForm.setQuestion(forumForm.getQuestion() + "<div class='forumSignature'>"+sender.getSignature()+"</div>");
			else
				forumForm.setQuestion(forumForm.getQuestion() + "<div class='forumSignature'>"+ ForumTools.replaceSignatureCodes(sender)+"</div>");
		}

		if (!SpamProtection.canPost("forum", forumForm.getQuestion(), request)) {
			setPermissionDenied(request, "postLimit");
			return SAVE_FORUM_SUCCESS;
		}

		if (sender != null) {
			//ako autora povolime len login, alebo meno
			if (sender.getLogin().equals(forumForm.getAuthorName())==false) forumForm.setAuthorName(sender.getFullName());
			forumForm.setAuthorEmail(sender.getEmail());
		}

		if (forumForm.getParentId() == -1) {
			//
			prepareForumForSave(forumForm, forumGroup, sender, request, hashCode, userId, true);
			docForumRepository.save(forumForm);
			Adminlog.add(Adminlog.TYPE_FORUM_CREATE, "Vytvorena tema diskusie: "+forumForm.getSubject(), -1, forumForm.getDocId());
		} else {
			//
			prepareForumForSave(forumForm, forumGroup, sender, request, hashCode, userId, false);
			docForumRepository.save(forumForm);

			//Update SUBTOPIC by incrementing statReplies and setting actual lastPost Date
			int fId = ForumDB.getForumMessageParent(forumForm.getParentId(), forumForm.getDocId());
			if (fId > 0) docForumRepository.updateSubtopic(new Date(), Long.valueOf(fId), domainId);

			Adminlog.add(Adminlog.TYPE_FORUM_CREATE, "Vytvoreny prispevok: "+forumForm.getSubject(), -1, forumForm.getDocId());
		}

		//If user is logged, increment user forum rank
		if(userId > 0) (new SimpleQuery()).execute("UPDATE users SET forum_rank=forum_rank+1 WHERE user_id=?", userId);

		//Increment WebPage forum_count
		(new SimpleQuery()).execute("UPDATE documents SET forum_count=forum_count+1 WHERE doc_id=?", forumForm.getDocId());

		//get new forum ID and set it into forum form
		if (forumForm.getId() < 1)
			forumForm.setId( new SimpleQuery().forLong("SELECT max(forum_id) AS forum_id FROM document_forum WHERE doc_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true), forumForm.getDocId()) );

		//Set default forum group values
		if (forumGroup == null) {
			String forumDefaultApproveEmail = Constants.getString("forumDefaultApproveEmail");
			String forumDefaultNotifyEmail = Constants.getString("forumDefaultNotifyEmail");

			if (Tools.isNotEmpty(forumDefaultApproveEmail) || Tools.isNotEmpty(forumDefaultNotifyEmail)) {
				forumGroup = new ForumGroupEntity();
				forumGroup.setMessageConfirmation(false);

				if (Tools.isNotEmpty(forumDefaultApproveEmail)) {
					forumGroup.setApproveEmail(forumDefaultApproveEmail);
					forumGroup.setMessageConfirmation(true);
				}

				if (Tools.isNotEmpty(forumDefaultNotifyEmail))
					forumGroup.setNotifEmail(forumDefaultNotifyEmail);
			}
		}

		//Send confirmation email
		DocForumEmailService emailService = new DocForumEmailService(forumForm, forumGroupPerms, request, prop);
		emailService.sendForumEmails();

		String fromName = Constants.getString("forumNotifySenderName");
		if(Tools.isEmpty(fromName)) fromName = forumForm.getAuthorName();

		String fromEmail = Constants.getString("forumNotifySenderEmail");
		if (Tools.isEmpty(fromEmail)) fromEmail = forumForm.getAuthorEmail();

		if (Tools.isNotEmpty(fromName)) {
			//uloz meno a email do cookies
			Cookie forumName = new Cookie("forumname", Tools.URLEncode(fromName));
			forumName.setPath("/");
			forumName.setMaxAge(60 * 24 * 3600);
			forumName.setHttpOnly(true);

			Cookie forumEmail = new Cookie("forumemail", Tools.URLEncode(fromEmail));
			forumEmail.setPath("/");
			forumEmail.setMaxAge(60 * 24 * 3600);
			forumEmail.setHttpOnly(true);

			Tools.addCookie(forumName, response, request);
			Tools.addCookie(forumEmail, response, request);
		}

		Forums.updateSingleQuestion(forumForm.getId().intValue());

		//return link to saveok.jsp
		return SAVE_FORUM_SUCCESS;
	}

	/**
	 * Perform recursive supported action uppon forum.
	 * More details in this fn code coments.
	 *
	 * @param actionType - Supported recursive actions
	 * @param forumId
	 * @param docId
	 * @param user - logged user
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static boolean docForumRecursiveAction(ActionType actionType, int forumId, int docId, Identity user) {
		//There must be logged user
		if(user == null) return true;

		String domainSql = CloudToolsForCore.getDomainIdSqlWhere(true);

		//Get all forum's under this one (all child's)
		List<DocForumEntity> msgList = getForumFieldsForDoc(actionType, docId, forumId, true);
		String fIds = getChildForumIdsRecursive(msgList, forumId);

		if (Tools.isNotEmpty(fIds)) fIds += forumId;
		else fIds = Integer.toString(forumId);

		boolean isAdmin = user.isAdmin();
		if (isAdmin == false) {
			ForumGroupEntity forumGroupBean = ForumGroupService.getForum(docId, false);
			isAdmin = forumGroupBean.isAdmin(user);
		}

		//Customize query
		StringBuilder simpleQuery = new StringBuilder("SELECT user_id FROM document_forum WHERE forum_id IN (" + fIds + ")" + domainSql);
		if(actionType == ActionType.DELETE) simpleQuery.append(" AND deleted < 1"); //Because change will affect only NON-Deleted forum's
		else if(actionType == ActionType.RECOVER) simpleQuery.append(" AND deleted > 0"); //Because change will affect only Deleted forum's
		else if(actionType == ActionType.APPROVE) simpleQuery.append(" AND confirmed < 1"); //Because change will affect only NON-Confirmed forum's
		else if(actionType == ActionType.REJECT) simpleQuery.append(" AND confirmed > 0"); //Because change will affect only Confirmed forum's
		else if(actionType == ActionType.LOCK) simpleQuery.append(" AND active > 0"); //Because change will affect only Active forum's
		else if(actionType == ActionType.UNLOCK) simpleQuery.append(" AND active < 1"); //Because change will affect only NON-Active forum's

		//Get users from affected forum's (those where I perform action)
		List<Number> usersInForums = null;
		try {
			usersInForums = new SimpleQuery().forList(simpleQuery.toString());
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			usersInForums = new ArrayList<>();
			if (isAdmin == false) usersInForums.add(user.getUserId());
		}

		final String WHERE_FORUM_ID_IN = " WHERE forum_id IN (";
		final String AND_USER_ID = " AND user_id=";

		//PREPARE QUERY FOR THE ACTION
		simpleQuery = new StringBuilder("");
		if(actionType == ActionType.DELETE) {
			//By constant decide if use SOFT delete or HARD delete
			if (Constants.getBoolean("forumReallyDeleteMessages"))
				simpleQuery.append("DELETE FROM document_forum WHERE forum_id IN (").append(fIds).append(")").append(domainSql); //HARD DELETE
			else
				simpleQuery.append("UPDATE document_forum SET deleted = ").append(DB.getBooleanSql(true)).append(WHERE_FORUM_ID_IN).append(fIds).append(") ").append(domainSql); //SOFT DELETE (update)

		} else if(actionType == ActionType.RECOVER) {
			simpleQuery.append("UPDATE document_forum SET deleted = ").append(DB.getBooleanSql(false)).append(WHERE_FORUM_ID_IN).append(fIds).append(")").append(domainSql);
		} else if(actionType == ActionType.APPROVE) {
			simpleQuery.append("UPDATE document_forum SET confirmed = ").append(DB.getBooleanSql(true)).append(WHERE_FORUM_ID_IN).append(fIds).append(")").append(domainSql);
		} else if(actionType == ActionType.REJECT) {
			simpleQuery.append("UPDATE document_forum SET confirmed = ").append(DB.getBooleanSql(false)).append(WHERE_FORUM_ID_IN).append(fIds).append(")").append(domainSql);
		} else if(actionType == ActionType.LOCK) {
			simpleQuery.append("UPDATE document_forum SET active = ").append(DB.getBooleanSql(false)).append(WHERE_FORUM_ID_IN).append(fIds).append(")").append(domainSql);
		} else if(actionType == ActionType.UNLOCK) {
			simpleQuery.append("UPDATE document_forum SET active = ").append(DB.getBooleanSql(true)).append(WHERE_FORUM_ID_IN).append(fIds).append(")").append(domainSql);
		}

		// Specification for user if is not admin
		if(!isAdmin) simpleQuery.append(AND_USER_ID).append(user.getUserId());

		//PERFORM action AND save number of changed rows
		Integer numberOfUpdated = new SimpleQuery().executeWithUpdateCount(simpleQuery.toString());

		if(numberOfUpdated != null && numberOfUpdated > 0 && usersInForums != null && usersInForums.isEmpty() == false) {
			String loggerMsg = "";
			String sqlSign = "";

			if(actionType == ActionType.DELETE || actionType == ActionType.REJECT) {
				//During DELETE / REJECT action, user's loosing rank, and forum loosing count
				loggerMsg = "Lowering rank for user: ";
				sqlSign = "-";
			} else {
				//During RECOVER / APPROVE action, user's retrieve rank, and forum retrieve count
				loggerMsg = "Increasing rank for user: ";
				sqlSign = "+";
			}

			//Update user rank
			for (Number userId : usersInForums) {
				Logger.debug(DocForumService.class, loggerMsg + userId.intValue());

				//Only real user (-1 userId represent NOT logged user)
				if(userId.intValue() > 0)
					(new SimpleQuery()).execute("UPDATE users SET forum_rank=forum_rank" + sqlSign + "1 WHERE user_id=?", userId.intValue());
			}

			//Update webpage forum_count
			(new SimpleQuery()).execute("UPDATE documents SET forum_count=forum_count" + sqlSign + "? WHERE doc_id=?", numberOfUpdated, docId);
		}

		DocForumEntity docForumEntity = getForumBean(forumId, true);
		Integer parentId = docForumEntity != null ? docForumEntity.getParentId() : -1;

		//Update forum info like stat view ...
		updateForumStatInfo(docId, parentId);

		if(docForumEntity != null) {
			if(actionType == ActionType.DELETE)
				Adminlog.add(Adminlog.TYPE_FORM_DELETE, "Zmazany diskusny prispevok: " + docForumEntity.getSubject(), forumId, docId);
			else if(actionType == ActionType.RECOVER)
				Adminlog.add(Adminlog.TYPE_FORUM_UNDELETE, "Obnoveny prispevok: " + (docForumEntity == null ? " ktory neexistuje" : docForumEntity.getSubject()), forumId, parentId);
		}

		return true;
	}

    /**
	 * testuje, ci nie je prispevok vulgarny, alebo obsahuje nepovoleny HTML kod
	 * @param message
	 * @return
	 */
	public static boolean isVulgar(String message) {
		//Get vulgar words
		String vulgarisms = Constants.getString("vulgarizmy");
		if (Tools.isEmpty(vulgarisms)) return false;

		//Test message
		if(Tools.isNotEmpty(message)) {
			message = " " + message.toLowerCase();
			//aby to naslo aj take ze ...text<script...
			message = Tools.replace(message, "<", " <");
			message = Tools.replace(message, ">", "> ");
			message = message.replace('\n', ' ');
			message = message.replace('\r', ' ');

			StringTokenizer st = new StringTokenizer(vulgarisms, ",");
			while (st.hasMoreTokens()) {
				String word = st.nextToken().trim();
				if (Tools.isEmpty(word)) continue;

				if (message.indexOf(" " + word) != -1) {
					Logger.println(DocForumService.class, "vulgar: " + word + " message: " + message);
					Adminlog.add(Adminlog.TYPE_FORUM_SAVE, "vulgar: " + word + " message: " + message, -1, -1);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Odstrani Microsot Office tagy z textu. Tieto tagy mozu sposobovat zle
	 * odsadzovanie textu.
	 *
	 * @return String - povodny text ostripovany o tagy
	 */
	public static String clearMsOfficeTags(String text) {
		String oldText = text;
		String forumWysiwygIcons = Constants.getString("forumWysiwygIcons");
		if (forumWysiwygIcons.indexOf("createlink") == -1) {
			//odpazme linky
			Pattern linkPattern = Pattern.compile("<\\s*[^>]*\\s*a\\s*href\\s*=\\s*\"[^\"]*\"\\s*[^>]*>|</a>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); //NOSONAR
			Matcher linkMatcher = linkPattern.matcher(text);
			text = linkMatcher.replaceAll("");
		}

		if (forumWysiwygIcons.indexOf("insertimage") == -1) {
			//odpazme linky
			Pattern linkPattern = Pattern.compile("<\\s*[^>]*\\s*img\\s*src\\s*=\\s*\"[^\"]*\"\\s*[^>]*>|</img>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); //NOSONAR
			Matcher linkMatcher = linkPattern.matcher(text);
			text = linkMatcher.replaceAll("");
		}

		try {
			//ak neobsahuje tento text, tak sa nie je coho bat
			if (!text.contains("class=MsoNormal") && !text.contains("class=\"MsoNormal\""))
				return text;

			//v prvom rade zmaz vsetky <FONT> a </FONT> tagy
			Pattern fontPattern = Pattern.compile("<FONT .*?>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); //NOSONAR
			Matcher fontMatcher = fontPattern.matcher(text);

			while (fontMatcher.find()) {
				text = text.replace(fontMatcher.group(0), "");
				fontMatcher = fontPattern.matcher(text);
			}

			text = text.replace("</FONT>", "");

			//potom samotne MsoNormal tagy
			Pattern msTagPattern = Pattern.compile("<P.*?class=.?MsoNormal.*?>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); //NOSONAR
			Matcher msTagMatcher = msTagPattern.matcher(text);

			while (msTagMatcher.find()) {
				text = text.replace(msTagMatcher.group(0), "<P>");
				msTagMatcher = msTagPattern.matcher(text);
			}

			//toto su dalsie MS specific tagy...
			text = text.replace("<o:p>", "");
			text = text.replace("</o:p>", "");
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
			return oldText;
		}

		return text;
	}

	/**
	 * Prepare new created instance of DocForumEntity for save.
	 *
	 */
	private static void prepareForumForSave(DocForumEntity forumForm, ForumGroupEntity forumGroup, Identity sender, HttpServletRequest request, String hashCode, Integer userId, Boolean isSubtopic) {
		//If user is logged, use his login/email as sender or use data from forumForm
		if(sender != null && Tools.isEmpty(forumForm.getAuthorName())) {
			forumForm.setAuthorName( sender.getLogin() );
			forumForm.setAuthorEmail( sender.getEmail());
		}

		//If validation is ENABLED, set confirmed as FALSE (because it needs approve)
		if (forumGroup != null &&  forumGroup.getMessageConfirmation()) {
			//Need's approve
			//BUT if user is logged AND he is approver of forum AND he is author of this new contribution ... DO a AUTO approve
			if(sender != null && sender.getEmail().equals(forumGroup.getApproveEmail()) && sender.getEmail().equals(forumForm.getAuthorEmail()))
				forumForm.setConfirmed(true);
			else
				forumForm.setConfirmed(false);
		} else //Forum do NOT need approve
			forumForm.setConfirmed(true);

		forumForm.setQuestionDate( new Date() );
		forumForm.setIp( Tools.getRemoteIP(request) );
		forumForm.setHashCode(hashCode);
		forumForm.setUserId(userId);
		forumForm.setActive(true);
		forumForm.setDomainId( CloudToolsForCore.getDomainId() );
		forumForm.setDeleted(false);

		if(Boolean.TRUE.equals(isSubtopic)) {
			//If the forumForm represent subtopic
			forumForm.setStatViews(0);
			forumForm.setStatReplies(0);
			forumForm.setStatLastPost(new Date());
		}
	}

	/**
	 * Get DocForumEntity based on forumId (id), is entity is present, convert it to dDocForumEntity or return null.
	 *
	 * @param forumId - entity id
	 * @param sortAscending - true = ASC, false = DESC
	 * @return
	 */
	public static DocForumEntity getForumBean(int forumId, boolean sortAscending) {
		DocForumRepository docForumRepository = getDocForumRepository();
		Integer domainId = CloudToolsForCore.getDomainId();

		//Based on param sortAscending call getData
		Optional<DocForumEntity> entityOpt;
		if(sortAscending)
			entityOpt = docForumRepository.findByIdAndDomainIdOrderByQuestionDateAsc(Long.valueOf(forumId), domainId);
		else
			entityOpt = docForumRepository.findByIdAndDomainIdOrderByQuestionDateDesc(Long.valueOf(forumId), domainId);

		if(entityOpt.isPresent())
			return entityOpt.get();

		return null;
	}

	/**
	 * Recursively sort List (unsorted) and sorted values add into List (sorted).
	 *
	 * @param unsorted
	 * @param sorted
	 * @param level
	 * @param parent
	 */
	public static void recursiveSort(List<DocForumEntity> unsorted, List<DocForumEntity> sorted, int level, int parent) {
		for (DocForumEntity fb : unsorted) {
			if (fb.getParentId() != null && fb.getParentId().intValue() == parent) {
				fb.setPrefix("<div style=\"margin-left:" + (20 * level) + "px\">");
				fb.setLevel(level);
				sorted.add(fb);
				recursiveSort(unsorted, sorted, level + 1, fb.getId().intValue());
			}
		}
	}

	public static List<DocForumEntity> getForumFieldsForDoc(HttpServletRequest request, int docId, boolean onlyConfirmed, int parentId, boolean sortAscending, boolean showDeleted, boolean isForumSearch) {
		//It's Mental gymnastic, we using SQL where BOOLEAN params are compared as (boolean == val1 OR boolean == val2)
		//By setting val1 and val2 we can return all combinations true/false/doesnt matter

		if(showDeleted)
			return getForumFieldsForDoc(docId, parentId, sortAscending, onlyConfirmed, true, true, false, isForumSearch); //speaking about deleted, true && false -> all forum's, not depending on deleted value
		else
			return getForumFieldsForDoc(docId, parentId, sortAscending, onlyConfirmed, true, false, false, isForumSearch); //speaking about deleted, flase && false -> only not deleted forum's
	}

	private static List<DocForumEntity> getForumFieldsForDoc(ActionType actionType, int docId, int parentId, boolean sortAscending) {
		boolean deleteA = true;
		boolean deleteB = false;
		boolean confirmA = true;
		boolean confirmB = false;

		if(actionType == ActionType.DELETE) //Only NOT deleted
			deleteA = false;
		else if(actionType == ActionType.RECOVER) //Only deleted
			deleteB = true;
		else if(actionType == ActionType.APPROVE) //Only NOT approved (not confirmed)
			confirmA = false;
		else if(actionType == ActionType.REJECT) //Only approved (confirmed)
			confirmB = true;

		return getForumFieldsForDoc(docId, parentId, sortAscending, confirmA, confirmB, deleteA, deleteB, false);
	}

	/**
	 * confirmedA == true && confirmedB == true -> only confirmed.
	 * confirmedA == false && confirmedB == false -> only NOT confirmed.
	 * else -> all records, confirmed value doesnt matter.
	 *
	 * deletedA == true && deletedB == true -> only deleted.
	 * deletedA == false && deletedB == false -> only NOT deleted.
	 * else -> all records, deleted value doesnt matter.
	 */
	private static List<DocForumEntity> getForumFieldsForDoc(int docId, int parentId, boolean sortAscending, boolean confirmedA,  boolean confirmedB, boolean deletedA, boolean deletedB, boolean isForumSearch) {
		//Returned List, even empty
		List<DocForumEntity> sorted = new ArrayList<>();

		if(docId > 0) {
			List<DocForumEntity> unsorted = new ArrayList<>();
			List<Integer> parentIds = new ArrayList<>(Arrays.asList(parentId));
			HashSet<Integer> obtainedParentIds = new HashSet<>(Arrays.asList(parentId));
			HashSet<Integer> obtainedChildIds = new HashSet<>();
			Integer domainId = CloudToolsForCore.getDomainId();

			DocForumRepository docForumRepository = getDocForumRepository();

			//Get all relevant forum entities
			List<DocForumIdAndParentId> entities;

			//Very special FIX -> only in case of calling ForumDB.getForumMessageParent (after forum search), we must include massage-board forum's with parent id < 0
			//It's because we cant prepare link for forum in search, when we do not load allso root parent
			if(isForumSearch) entities = docForumRepository.getDocForumListForumIdParentIdWithRootParents(docId, deletedA||deletedB, confirmedA||confirmedB, domainId);
			else entities = docForumRepository.getDocForumListForumIdParentId(docId, deletedA||deletedB, confirmedA||confirmedB, domainId);

			//Loop all entities and make String of all parent ids (even sub levels)
			for(DocForumIdAndParentId entity : entities) {
				int childForumId = entity.getId().intValue();
				int childParentId = entity.getParentId();

				//To make answers work in simple forum
				if (parentId == 0) parentIds.add(childParentId);

				if (obtainedParentIds.contains(childParentId))
					obtainedChildIds.add(childForumId);
				else if (obtainedChildIds.contains(childParentId)) {
					obtainedChildIds.add(childForumId);
					obtainedParentIds.add(childParentId);
					parentIds.add(childParentId);
				}
			}

			//Based of parent id's, get all their child's entities
			List<DocForumEntity> childForums = new ArrayList<>();
			if(sortAscending)
				childForums.addAll( docForumRepository.getDocForumListAsc(docId, parentIds, deletedA||deletedB, confirmedA||confirmedB, domainId) );
			else
				childForums.addAll( docForumRepository.getDocForumListDesc(docId, parentIds, deletedA||deletedB, confirmedA||confirmedB, domainId) );

			//It must be ForumBean List
			unsorted.addAll( childForums );

			DocForumEntity parentForum = getForumBean(parentId, true);
			//Push from start
			if(parentForum != null && sortAscending) sorted.add(parentForum);

			//Sort it
			recursiveSort(unsorted, sorted, 0, parentId);

			//Push at end (aka first question)
			if (parentForum != null && !sortAscending) sorted.add(parentForum);
		}

		//return sorted values
		return sorted;
	}

	/**
	 * BAsed on give forumId, return string where are joined all children id's.
	 *
	 * @param msgList - list of entities to loop
	 * @param forumId - forumId of root parent
	 * @return
	 */
	public static String getChildForumIdsRecursive(List<DocForumEntity> msgList, int forumId) {
		StringBuilder idStr = new StringBuilder();

		if (msgList != null && forumId > 0) {
			for (DocForumEntity fb : msgList) {
				if (fb.getParentId() == forumId) {
					if (Tools.isNotEmpty(idStr)) idStr.append(String.valueOf(fb.getId())).append(',');
					else idStr = new StringBuilder(String.valueOf(fb.getId())).append(',');
					idStr.append(getChildForumIdsRecursive(msgList, fb.getId().intValue()));
				}
			}
		}
		return idStr.toString();
	}

	public static void updateForumStatInfo(int docId, int forumId) {
		String childForumIdsString;
		String[] childForumIds;
		int countStatReplies = 0;
		List<DocForumEntity> msgList = getForumFieldsForDoc(null, docId, true, forumId, true, false, false);
		int parentId = findParentRecursive(msgList, forumId);
		String domain = CloudToolsForCore.getDomainIdSqlWhere(true);

		childForumIdsString = getChildForumIdsRecursive(msgList, parentId);
		if (Tools.isNotEmpty(childForumIdsString) && childForumIdsString.endsWith(",")) childForumIdsString = childForumIdsString.substring(0, childForumIdsString.length() - 1);

		childForumIds = Tools.getTokens(childForumIdsString, ",");
		if (childForumIds != null)
			countStatReplies = childForumIds.length;

		if (countStatReplies == 0) {
			(new SimpleQuery()).execute("UPDATE document_forum SET stat_replies=?, stat_last_post=question_date WHERE forum_id=? AND parent_id=-1" + domain, countStatReplies, parentId);
		}

		if (Tools.isNotEmpty(childForumIdsString)) {
			StringBuilder sql = new StringBuilder("SELECT ");
			if (Constants.DB_TYPE == Constants.DB_MSSQL)
				sql.append("TOP 1 ");
			sql.append("question_date FROM document_forum WHERE ");
			if(Constants.DB_TYPE == Constants.DB_ORACLE)
				sql.append(" rownum = 1 AND ");
			sql.append("doc_id=?" + domain + " AND forum_id IN (" + childForumIdsString + ")" + " ORDER BY question_date DESC");
			if (Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL)
				sql.append(" LIMIT 1");

			Date lastPostDate = (new SimpleQuery()).forDate(sql.toString(), docId);
			if (lastPostDate != null && lastPostDate.getTime() > 0 && countStatReplies > 0)
				(new SimpleQuery()).execute("UPDATE document_forum SET stat_replies=?, stat_last_post=? WHERE forum_id=? AND parent_id=-1" + domain, countStatReplies, lastPostDate, parentId);
		}
	}

	/**
	 * Rekurzivne vyhlada forumId pod-temy
	 *
	 * @param msgList
	 * @param parentId
	 * @return
	 */
	public static int findParentRecursive(List<DocForumEntity> msgList, int parentId) {
		int forumId = -1;

		for (DocForumEntity fb : msgList) {
			if (fb.getId().intValue() == parentId) {
				forumId = findParentRecursive(msgList, fb.getParentId());
				if (forumId < 0) forumId = fb.getId().intValue();
			}
		}

		return forumId;
	}

	/**
	 * Upload file for specific forum. Using MediaDB.
	 *
	 * @param uploadFile - file to upload
	 * @param request
	 * @return - path to file (forward)
	 */
	public static String uploadForumFile(MultipartFile uploadFile, HttpServletRequest request) {
		int forumId = Tools.getIntValue(request.getParameter("forumId") , -1);
		if(forumId == -1 || uploadFile == null || uploadFile.isEmpty()) return null;

		DocForumRepository docForumRepository = getDocForumRepository();
		Optional<DocForumEntity> forumOpt = docForumRepository.findById((long) forumId);
		if(!forumOpt.isPresent()) return null;

		try {
            return uploadForumFileLogic(uploadFile, forumOpt.get(), request);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

		return SAVE_FORUM_SUCCESS;
	}

	private static String uploadForumFileLogic(MultipartFile uploadedFile, DocForumEntity docForum, HttpServletRequest request) throws IOException {
		//Check logged user
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null) {
			setPermissionDenied(request, "true");
			return SAVE_FORUM_SUCCESS;
		}

		//Check if we upload file to MY post
		if (docForum.getUserId() < 1 || docForum.getUserId() != user.getUserId()) {
			setPermissionDenied(request, "true");
			return SAVE_FORUM_SUCCESS;
		}

		//Get upload limits
		LabelValueDetails uploadLimits = ForumDB.getUploadLimits(docForum.getDocId(), request);
		if (uploadLimits == null) {
			setPermissionDenied(request, "true");
			return SAVE_FORUM_SUCCESS;
		}

		//Check uploaded file size limit
		if (uploadLimits.getInt1() > 0 && (uploadLimits.getInt1() * 1024) < uploadedFile.getSize()) {
			setPermissionDenied(request, "fileSize");
			return SAVE_FORUM_SUCCESS;
		}

		//Check the file rights
		String fileName = DocTools.removeChars(uploadedFile.getOriginalFilename()).toLowerCase();

		//Check the suffix
		if (fileName.endsWith(".jsp") || fileName.endsWith(".class")) {
			setPermissionDenied(request, "true");
			return SAVE_FORUM_SUCCESS;
		}

		boolean canUpload = false;
		if ("*".equals(uploadLimits.getValue())) {
			canUpload = true;
		} else {
			//skontroluj priponu suboru
			StringTokenizer st = new StringTokenizer(uploadLimits.getValue(), ",");
			while (st.hasMoreTokens() && canUpload == false)
				if (fileName.endsWith(st.nextToken())) canUpload = true;
		}

		if (!canUpload) {
			setPermissionDenied(request, "fileType");
			return SAVE_FORUM_SUCCESS;
		}

		//ok, let upload image
		String baseDir = "/images/apps/forum/";
		baseDir = baseDir.replace('\\', '/');
		if (baseDir.endsWith("/") == false) baseDir = baseDir + "/";
		fileName = user.getUserId() + "-" + docForum.getId() + "-" + fileName;

		File f = new File(Tools.getRealPath(baseDir + fileName));
		if (f.getParentFile().exists() == false && f.getParentFile().mkdirs() == false) {
			setPermissionDenied(request, "true");
			return SAVE_FORUM_SUCCESS;
		}

		boolean fileAllreadyExists = f.exists();

		//zapis subor na disk
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
		BufferedInputStream buffReader = new BufferedInputStream(uploadedFile.getInputStream());
		FileOutputStream fos = new FileOutputStream(f);
		while ((bytesRead = buffReader.read(buffer, 0, 8192)) != -1)
			fos.write(buffer, 0, bytesRead);

		buffReader.close();
		fos.close();
		//uploadedFile.delete();

		//ak prepise existujuci subor, aby sa to neobjavilo v zozname viac krat
		if (fileAllreadyExists == false) {
			//zapis do medii
			Media m = new Media();
			m.setMediaFkId( docForum.getId().intValue() );
			m.setMediaFkTableName("document_forum");

			if (Tools.isNotEmpty(docForum.getSubject()))
				m.setMediaTitleSk(docForum.getSubject());
			else
				m.setMediaTitleSk(uploadedFile.getOriginalFilename());

			m.setMediaLink(baseDir + fileName);
			m.setMediaSortOrder(10);
			new MediaDB().save(m);
		}

		return SAVE_FORUM_SUCCESS;
	}

	public static DocForumEntity getForumStat(int docId) {

		if(docId < 1) return null;

		Date lastPost = null;
		int totalMessages = 0;
		DocForumRepository docForumRepository = getDocForumRepository();
		List<DocForumEntity> forums = docForumRepository.getForumStat(docId, false,  CloudToolsForCore.getDomainId());

		for(DocForumEntity forum : forums) {
			totalMessages += forum.getStatReplies() + 1;
			if(forum.getStatLastPost() != null) lastPost = forum.getStatLastPost();
		}

		DocForumEntity stat = new DocForumEntity();
		stat.setStatReplies(totalMessages);
		if(lastPost != null) stat.setStatLastPost(lastPost);

		return stat;
	}

	public static List<DocForumEntity> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch, ForumSortBy sortBy) {
		List<DocForumEntity> topics = new ArrayList<>();

		//docId mus be set !!
		if(docId < 1) return topics;

		//Params for query
		List<Object> params = new ArrayList<>();
		params.add(docId);

		//Prepare sql query
		String sql = "SELECT * FROM document_forum WHERE doc_id=? AND parent_id=-1" + CloudToolsForCore.getDomainIdSqlWhere(true);
		if (showDeleted == false) sql += " AND deleted = "+DB.getBooleanSql(false);
		if (Tools.isNotEmpty(flagSearch)) {
			sql += " AND flag LIKE ?";
			params.add(flagSearch + "%");
		}
		sql += " ORDER BY flag DESC, " + sortBy.getColumnName() + " DESC";

		Logger.debug(ForumDB.class, "getForumTopics: docId=" + docId + " flagSearch=" + flagSearch + " sql=" + sql);

		new ComplexQuery().setSql(sql).setParams(params.toArray()).list(new Mapper<DocForumEntity>() {
			@Override
			public DocForumEntity map(ResultSet rs) throws SQLException {
				boolean isConfirmed = rs.getBoolean("confirmed");
				if(onlyConfirmed && !isConfirmed) {
					//If confirmed only but this one is not confirmed
				} else {
					DocForumEntity tmpEntity = rsToDocForumEntity(rs);

					//Insert to our list
					if(tmpEntity != null)
						topics.add(tmpEntity);
				}

				return null;
			}
		});

		//Add ForumGroupEntity
		for(DocForumEntity topic : topics)
			topic.setForumGroupEntity( ForumGroupService.getForum(topic.getDocId(), false) );

		return topics;
	}

	private static DocForumEntity rsToDocForumEntity(ResultSet rs) {
		try {
			DocForumEntity tmpEntity = new DocForumEntity();

			tmpEntity.setId(rs.getLong("forum_id"));
			tmpEntity.setDocId(rs.getInt("doc_id"));
			tmpEntity.setParentId(rs.getInt("parent_id"));
			tmpEntity.setSubject(DB.getDbString(rs, "subject"));
			tmpEntity.setQuestion(DB.getDbString(rs, "question"));
			tmpEntity.setQuestionDate( new Date( DB.getDbTimestamp(rs, "question_date") ) );
			tmpEntity.setAuthorName(DB.getDbString(rs, "author_name"));
			tmpEntity.setAuthorEmail(DB.getDbString(rs, "author_email"));
			tmpEntity.setConfirmed(rs.getBoolean("confirmed"));
			tmpEntity.setHashCode(DB.getDbString(rs, "hash_code"));
			tmpEntity.setSendAnswerNotif(rs.getBoolean("send_answer_notif"));
			tmpEntity.setStatReplies(rs.getInt("stat_replies"));
			tmpEntity.setStatViews(rs.getInt("stat_views"));
			tmpEntity.setStatLastPost( new Date( DB.getDbTimestamp(rs, "stat_last_post") ) );
			tmpEntity.setFlag(DB.getDbString(rs, "flag"));
			tmpEntity.setUserId(rs.getInt("user_id"));
			tmpEntity.setActive(rs.getBoolean("active"));
			tmpEntity.setDeleted(rs.getBoolean("deleted"));

			return tmpEntity;
		} catch (SQLException ex) {
			sk.iway.iwcm.Logger.error(ex);
		}

		return null;
	}

	public static DocForumEntity getLastMessage(int docId) {
		DocForumRepository docForumRepository = getDocForumRepository();
		Optional<DocForumEntity> optMsp = docForumRepository.findFirstByDocIdAndDomainIdOrderByQuestionDateDesc(docId, CloudToolsForCore.getDomainId());
		if(optMsp.isPresent()) return optMsp.get();

		return null;
	}

	private static DocForumRepository getDocForumRepository() {
		return Tools.getSpringBean("docForumRepository", DocForumRepository.class);
	}

	private static void setError(HttpServletRequest request, String error) {
		request.setAttribute("errorKey", error);
	}

	private static void setPermissionDenied(HttpServletRequest request, String error) {
		Logger.debug(ForumDB.class, "Permission denied, error=" + error + " subject="+request.getParameter("subject"));
		request.setAttribute("permissionDenied", error);
	}
}