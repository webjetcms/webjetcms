package sk.iway.iwcm.stripes;

import java.beans.XMLDecoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.DocNoteBean;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.sync.WarningListener;
import sk.iway.iwcm.sync.export.Content;
import sk.iway.iwcm.sync.inport.BannerImporter;
import sk.iway.iwcm.sync.inport.ContentBannerBean;
import sk.iway.iwcm.sync.inport.ContentFileBean;
import sk.iway.iwcm.sync.inport.ContentGalleryBean;
import sk.iway.iwcm.sync.inport.ContentInquiryBean;
import sk.iway.iwcm.sync.inport.GalleryImporter;
import sk.iway.iwcm.sync.inport.InquiryImporter;
import sk.iway.iwcm.sync.inport.Numbered;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/**
 *  SyncDirAction.java - synchronizacia adresara a web stranok zo vzdialeneho servera
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.11 $
 *@created      Date: 8.11.2007 13:37:13
 *@modified     $Date: 2010/02/09 09:02:04 $
 */
public class SyncDirAction extends WebJETActionBean
{
	private static org.slf4j.Logger log = LoggerFactory.getLogger(SyncDirAction.class);

	private int localGroupId;
	private int remoteGroupId;
	private String localDomain;

	private GroupDetails localSrcGroup;

	private List<GroupDetails> remoteGroups = null;
	private List<DocDetails> remoteDocs = null;
	private List<TemplateDetails> remoteTemps = null;
	private List<UserGroupDetails> remoteUserGroups = null;
	private List<PerexGroupBean> remotePerexGroups = null;
	private List<Media> remoteMedia = null;
	private Content remoteContent = null;

	private String remoteRootPath = null;
	private GroupDetails localRootGroup = null;

	private boolean createMissingTemplates = true;
	private boolean createMissingUserGroups = true;

	private String syncDir = null; // if null use remoteServer else use local files for sync

	private Map<Integer, DocNoteBean> remoteNotesMap = null;

	private List<String> errorMessages = new ArrayList<>();

	private String compareBy = null;

	/**
	 * Nacita data zo vzdialeneho servera
	 * @return
	 */
	public Resolution btnLoadData()
	{
		SyncDirWriterService.printStatusMsg("components.syncDirAction.title", true, getContext(), getRequest());

		//aby sa nam vymazala cache (ak je pouzita)
		remoteGroups = null;
		remoteDocs = null;
		Logger.debug(SyncDirAction.class, "btnLoadData Reading remote groups");

		try
		{
			SyncDirWriterService.printStatusMsg("components.syncDirAction.prepare_groups", false, getContext(), getRequest());
			remoteGroups = getRemoteGroups();

			SyncDirWriterService.printStatusMsg("components.syncDirAction.prepare_docs", false,  getContext(), getRequest());
			remoteDocs = getRemoteDocs();
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		SyncDirWriterService.printStatusMsg("components.syncDirAction.prepare_content", false,  getContext(), getRequest());
		Logger.debug(SyncDirAction.class, "btnLoadData Remote groups="+remoteGroups);
		if (remoteGroups == null || remoteDocs == null)
		{
			context.getRequest().setAttribute("errorText", Prop.getInstance(getRequest()).getText("components.sync.errorReadingZipFile", getErrorMessagesHtml()));
		}
		remoteContent = getRemoteContent();

		return new ForwardResolution("/components/maybeError.jsp");
	}

	private void foldersSync(HttpServletRequest request, PrintWriter writer) {
		//
		Prop prop = Prop.getInstance(request);

		SyncDirWriterService.prepareProgress(prop.getText("components.syncDirAction.progress.syncingFolders"), "folderSyncCount", prop.getText("components.syncDirAction.progress.syncingFolder") + ": - / -", writer);

		Map<String, String> selectedGroupsMap = SyncDirWriterService.getOptionsMap("group_", request);
		if(selectedGroupsMap.size() < 1) return;

		int groupsToPrepareCount = 0;
		int preparedGroupsCount = 1;
		List<GroupDetails> remoteGroupsToSync = getRemoteGroups();

		//Count groups to sync
		groupsToPrepareCount = (int) remoteGroupsToSync.stream()
			.filter(remoteGroup -> selectedGroupsMap.get("group_" + remoteGroup.getGroupId()) != null)
			.count();


		for (GroupDetails remoteGroup : remoteGroupsToSync)
		{
			if (selectedGroupsMap.get("group_" + remoteGroup.getGroupId()) != null)
			{
				SyncDirWriterService.updateProgress("folderSyncCount", prop.getText("components.syncDirAction.progress.syncingFolder") + ": " + preparedGroupsCount + " / " + groupsToPrepareCount, writer);
				preparedGroupsCount++;

				createLocalGroup(remoteGroup);
			}
		}
	}

	private List<Integer> pagesSync(HttpServletRequest request, PrintWriter writer) {
		Prop prop = Prop.getInstance(request);
		//
		SyncDirWriterService.prepareProgress(prop.getText("components.syncDirAction.progress.syncingDocs"), "webPageSyncCount", prop.getText("components.syncDirAction.progress.syncingDoc") + ": - / -", writer);

		List<Integer> historyIds = new ArrayList<>();
		Map<String, String> selectedDocsMap = SyncDirWriterService.getOptionsMap("doc_", request);
		if(selectedDocsMap.size() < 1) return historyIds;

		List<DocDetails> remoteDocToSync = getRemoteDocs();
		int pagesToPrepareCount = 0;
		int preparedPagesCount = 1;

		//Count pages to prepare
		pagesToPrepareCount = (int) remoteDocToSync.stream()
			.filter(remoteDoc -> selectedDocsMap.get("doc_" + remoteDoc.getDocId()) != null)
			.count();


		for (DocDetails remoteDoc : remoteDocToSync)
		{
			if (selectedDocsMap.get("doc_" + remoteDoc.getDocId()) != null)
			{
				SyncDirWriterService.updateProgress("webPageSyncCount", prop.getText("components.syncDirAction.progress.syncingDoc") + ": " + preparedPagesCount + " / " + pagesToPrepareCount, writer);
				preparedPagesCount++;

				int historyId = createLocalDoc(remoteDoc);
				historyIds.add(historyId);
			}
		}

		return historyIds;
	}

	private void importingFiles(Content content, HttpServletRequest request, PrintWriter writer) {
		Prop prop = Prop.getInstance(request);
		//
		SyncDirWriterService.prepareProgress(prop.getText("components.syncDirAction.progress.syncingFiles"), "filesImportCount", prop.getText("components.syncDirAction.progress.syncingFile") + ": - / -", writer);

		if(content == null) return;

		Map<String, String> selectedFilesMap = SyncDirWriterService.getOptionsMap("file_", request);
		if(selectedFilesMap.size() < 1) return;

		int importedFilesCount = 1;
		Iterable<Numbered<Content.File>> filesToImport = Numbered.list(content.getFiles());
		int filesToImportCount = SyncDirWriterService.getCountToHandle(selectedFilesMap, filesToImport, "file_");

		for (Numbered<Content.File> file : filesToImport)
		{
			if (selectedFilesMap.get("file_" + file.number) != null)
			{
				SyncDirWriterService.updateProgress("filesImportCount", prop.getText("components.syncDirAction.progress.syncingFile") + ": " + importedFilesCount + " / " + filesToImportCount, writer);
				importedFilesCount++;

				createLocalContentFile(file.item);
			}
		}
	}

	/**
	 * Vykonanie synchronizacie adresarov a stranok z remote servera na local
	 * @return
	 */
	public Resolution btnSync()
	{
		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		try
		{
			HttpServletResponse response = getContext().getResponse();
        	PrintWriter writer = response.getWriter();

			HttpServletRequest request = context.getRequest();
			//UPOZORNENIE: ziadnym sposobom nemodifikovat remote objekty!
			// ak je to nutne je potrebne nasledne vratit povodne hodnoty

			GroupDetails localGroup = getLocalRootGroup();
			if (localGroup != null && Tools.isNotEmpty(localGroup.getDomainName()))
			{
				setLocalDomain(localGroup.getDomainName());
			}

			//FOLDER SYNC
			List<Integer> historyGroupsIds = new ArrayList<>();
			Date start = new Date();
			foldersSync(request, writer);
			historyGroupsIds = getNewGroups(start);

			//WEB PAGE SYNC
			List<Integer> historyIds = pagesSync(request, writer);
			saveRollbackJson(historyIds, historyGroupsIds);

			//FILE IMPORT
			Content content = getRemoteContent();
			importingFiles(content, request, writer);

			// import komponentov
			BannerImporter.importBanners(request, content, writer);
			InquiryImporter.importInquiries(request, content, writer);
			GalleryImporter.importGalleries(request, content, writer);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			getRequest().setAttribute("errorText", "ERROR: "+ex.getMessage());
		}

		if (remoteGroups == null || remoteDocs == null)
		{
			context.getRequest().setAttribute("errorText", Prop.getInstance(getRequest()).getText("components.sync.errorReadingZipFile", getErrorMessagesHtml()));
		}

		return new ForwardResolution("/components/maybeError.jsp");
	}

	private String getErrorMessagesHtml()
	{
		StringBuilder html = new StringBuilder();

		if (errorMessages.size()>0)
		{
			html.append("<ul>\n");
			for (String error : errorMessages)
			{
				html.append("<li>").append(error).append("</li>\n");
			}
			html.append("</ul>\n");
		}

		return html.toString();
	}


	/**
	 * Vytvori adresar na lokalnom serveri
	 * @param remoteGroup
	 * @return
	 */
	private boolean createLocalGroup(GroupDetails remoteGroup)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		String localPath = getLocalPath(remoteGroup);
		GroupDetails localGroup = groupsDB.getGroupByPath(localPath);
		if (localGroup == null)
		{
			//treba ulozit, ziskaj parenta
			int i = localPath.lastIndexOf('/');
			if (i>0)
			{
				String localParentPath = localPath.substring(0, i);
				Logger.debug(SyncDirAction.class, "localPath: "+localPath+" localParentPath: "+localParentPath+" remotePath: "+remoteGroup.getFullPath());
				GroupDetails localParentGroup = groupsDB.getCreateGroup(localParentPath);
				if (localParentGroup!=null)
				{
					//uprav mapovanie user group skupin
					String localUserGroups = convertUserGroupIdsToLocal(remoteGroup);
					if (localUserGroups==null) return false;

					int oGroupId = remoteGroup.getGroupId();
					int oParentGroupId = remoteGroup.getParentGroupId();
					int oTempId = remoteGroup.getTempId();
					String oUserGroups = remoteGroup.getPasswordProtected();
					String oFullPath = remoteGroup.getFullPath();
					String oNavbar = null;
					String oDomain = remoteGroup.getDomainName();
					int oDefaultDocId = remoteGroup.getDefaultDocId();

					remoteGroup.setGroupId(-1);
					remoteGroup.setParentGroupId(localParentGroup.getGroupId());
					if (remoteGroup.getNavbarName().indexOf("href=")!=-1)
					{
						//uz prenasame navbarName, kvoli spetnej kompatibilite ale mame tento backfix
						oNavbar = remoteGroup.getNavbar();
						remoteGroup.setNavbar(SearchTools.htmlToPlain(oNavbar));
					}
					remoteGroup.setDefaultDocId(-1);

					remoteGroup.setTempId(localRootGroup.getTempId());
					if (createMissingTemplates)
					{
						//uprav mapovanie sablon
						int localTempId = getCreateTemplate(oTempId);
						Logger.debug(SyncDirAction.class, "Setting localTempId to:"+localTempId+" for group "+remoteGroup.getGroupIdName());
						if (localTempId>0) remoteGroup.setTempId(localTempId);
					}
					remoteGroup.setPasswordProtected(localUserGroups);
					if(Tools.isNotEmpty(localDomain))
						remoteGroup.setDomainName(localDomain);

					if("cloud".equals(Constants.getInstallName()))
					{
						String lng = getLocalLng();
						if (Tools.isNotEmpty(lng))
						{
							Logger.debug(getClass(), "Cloud, translating to: " + lng);
							remoteGroup.setGroupName(CloudToolsForCore.translate(lng, remoteGroup.getGroupName()));
							remoteGroup.setNavbar(SearchTools.htmlToPlain(CloudToolsForCore.translate(lng, remoteGroup.getNavbar())));
							remoteGroup.setUrlDirName(CloudToolsForCore.translate(lng, remoteGroup.getGroupName()));
						}
					}

					groupsDB.setGroup(remoteGroup);

					//vrat do povodneho stavu
					remoteGroup.setGroupId(oGroupId);
					remoteGroup.setParentGroupId(oParentGroupId);
					remoteGroup.setTempId(oTempId);
					remoteGroup.setPasswordProtected(oUserGroups);
					remoteGroup.setFullPath(oFullPath);
					if (oNavbar != null) remoteGroup.setNavbar(oNavbar);
					remoteGroup.setDomainName(oDomain);
					remoteGroup.setDefaultDocId(oDefaultDocId);

					return true;
				}
			}
		}
		else
		{
			//chceme prepisat data, toto musi zostat zachovane
			int groupId = localGroup.getGroupId();
			int parentGroupId = localGroup.getParentGroupId();
			int defaultDocId = localGroup.getDefaultDocId();
			int tempId = localGroup.getTempId();
			String domainName = localGroup.getDomainName();
			String userGroups = localGroup.getPasswordProtected();
			try
			{
			    //skopiruj data
				BeanUtils.copyProperties(localGroup, remoteGroup);

				//obnov zachovane data
				localGroup.setGroupId(groupId);
				localGroup.setParentGroupId(parentGroupId);
				localGroup.setDefaultDocId(defaultDocId);
                if (createMissingTemplates) localGroup.setTempId(getCreateTemplate(remoteGroup.getTempId()));
                else localGroup.setTempId(tempId);
                localGroup.setDomainName(domainName);

                String localUserGroups = convertUserGroupIdsToLocal(remoteGroup);
                if (localUserGroups!=null) localGroup.setPasswordProtected(localUserGroups);
				else localGroup.setPasswordProtected(userGroups);

				groupsDB.setGroup(localGroup);
			}
			catch (Exception ex)
			{
			    sk.iway.iwcm.Logger.error(ex);
			}
		}
		return false;
	}

    /**
     * Skonvertuje ID pouzivatelskych skupin z remote grupy na lokalne IDecka, vrati NULL ak nastane chyba
     * @param remoteGroup
     * @return
     */
	private String convertUserGroupIdsToLocal(GroupDetails remoteGroup)
    {
        StringBuilder localUserGroups = null;
        if (Tools.isNotEmpty(remoteGroup.getPasswordProtected()) && createMissingUserGroups)
        {
            StringTokenizer st = new StringTokenizer(remoteGroup.getPasswordProtected(), ",");
            while (st.hasMoreTokens())
            {
                int remoteUserGroupId = Tools.getIntValue(st.nextToken(), -1);
                if (remoteUserGroupId>0)
                {
                    String remoteUserGroupName = getRemoteUserGroupName(remoteUserGroupId);
                    if (Tools.isEmpty(remoteUserGroupName))
                    {
                        context.getRequest().setAttribute("errorText", "Neexistuje remote user group "+remoteUserGroupId);
                        Logger.debug(SyncDirAction.class, "Neexistuje remote user group "+remoteUserGroupId);
                        return null;
                    }
                    int localUserGroupId = getCreateUserGroup(remoteUserGroupId);
                    if (localUserGroupId<1)
                    {
                        context.getRequest().setAttribute("errorText", "Neexistuje pouzivatelska skupina "+remoteUserGroupName);
                        Logger.debug(SyncDirAction.class, "Neexistuje pouzivatelska skupina "+remoteUserGroupName);
                        return null;
                    }
                    if (localUserGroups==null) localUserGroups = new StringBuilder(Integer.toString(localUserGroupId));
                    else localUserGroups.append(",").append(localUserGroupId);
                }
            }
        }
		if (localUserGroups==null) return "";

		return localUserGroups.toString();
	 }

	 /**
	  * Convert string of remote group ids like 40,47,57 to local group ids, filter not/yet existing
	  * @param groupIds
	  * @return
	  */
	 private String convertGroupIdsToLocalFilter(String groupIds) {
		//ak export neobsahuje skupiny, tak vrat povodne
		if (Tools.isEmpty(groupIds)) return groupIds;

		StringBuilder localGroups = new StringBuilder();
		try {
			int[] ids = Tools.getTokensInt(groupIds, ",");
			for (int remoteGroupId : ids) {
				GroupDetails localGroup = getLocalGroup(remoteGroupId);
				if (localGroup != null) {
					if (localGroups.length()>0) localGroups.append(",");
					localGroups.append(String.valueOf(localGroup.getGroupId()));
				}
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			//ak to padlo, nastav povodne
			if (Tools.isEmpty(localGroups)) localGroups.append(groupIds);
		}
		return localGroups.toString();
	 }

	 /**
     * Skonvertuje ID perex skupin z remote doc na lokalne IDecka, vrati NULL ak nastane chyba
     * @param remoteGroup
     * @return
     */
		private String convertPerexGroupIdsToLocal(String perexGroupIds) {
			StringBuilder localPerexGroups = new StringBuilder();
			try {
				List<PerexGroupBean> perexGroups = getRemotePerexGroups();
				//ak export neobsahuje perex grupy, tak vrat povodne
				if (perexGroups == null || perexGroups.size()<1) return perexGroupIds;

				if (Tools.isNotEmpty(perexGroupIds) && createMissingUserGroups) {
					StringTokenizer st = new StringTokenizer(perexGroupIds, ",");
					while (st.hasMoreTokens()) {

						int remotePerexGroupId = Tools.getIntValue(st.nextToken(), -1);
						if (remotePerexGroupId > 0) {
							String remotePerexGroupName = getRemotePerexGroupName(remotePerexGroupId);
							if (Tools.isEmpty(remotePerexGroupName)) {
								context.getRequest().setAttribute("errorText", "Neexistuje remote perex group " + remotePerexGroupId);
								Logger.debug(SyncDirAction.class, "Neexistuje remote perex group " + remotePerexGroupId);
								continue;
							}

							int localPerexGroupId = getCreatePerexGroup(remotePerexGroupId);
							if (localPerexGroupId < 1) {
								context.getRequest().setAttribute("errorText", "Neexistuje perex skupina " + remotePerexGroupName);
								Logger.debug(SyncDirAction.class, "Neexistuje perex skupina " + remotePerexGroupName);
								//ak sa nepodarilo vytvorit, ponechaj povodne IDecko
								localPerexGroupId = remotePerexGroupId;
							}

							if (localPerexGroups.length()>0) localPerexGroups.append(",");
							localPerexGroups.append(Integer.toString(localPerexGroupId));
						}
					}
				}
			} catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
				//ak to padlo, nastav povodne
				if (Tools.isEmpty(localPerexGroups)) localPerexGroups.append(perexGroupIds);
			}
			return localPerexGroups.toString();
		}

	/**
	 * Vytvori stranku na lokalnom serveri
	 * @param remoteDoc
	 * @return
	 */
	private int createLocalDoc(DocDetails remoteDoc)
	{
		Logger.debug(SyncDirAction.class, "createLocalDoc, remoteDoc="+remoteDoc.getDocId()+" title="+remoteDoc.getTitle());

		GroupDetails localGroup = getLocalGroup(remoteDoc.getGroupId());
		if (localGroup==null)
		{
			//asi nezaskrtol, ze sa ma syncnut aj adresar
			createLocalGroup(getRemoteGroup(remoteDoc.getGroupId()));
			localGroup = getLocalGroup(remoteDoc.getGroupId());
		}
		if (localGroup!=null)
		{
			//najdi lokalnu verziu
			DocDetails localDoc = getLocalDoc(remoteDoc);

			//uprav mapovanie sablon!!!
			int localTempId = getCreateTemplate(remoteDoc.getTempId());
			if (localTempId<1) return -1;

			//uprav mapovanie user group skupin
			StringBuilder localUserGroups = null;
			if (Tools.isNotEmpty(remoteDoc.getPasswordProtected()))
			{
				StringTokenizer st = new StringTokenizer(remoteDoc.getPasswordProtected(), ",");
				while (st.hasMoreTokens())
				{
					int remoteUserGroupId = Tools.getIntValue(st.nextToken(), -1);
					if (remoteUserGroupId>0)
					{
						int localUserGroupId = getCreateUserGroup(remoteUserGroupId);
						if (localUserGroupId<1)
						{
							return -1;
						}
						if (localUserGroups==null) localUserGroups = new StringBuilder(Integer.toString(localUserGroupId));
						else localUserGroups.append(",").append(localUserGroupId);
					}
				}
			}

			EditorForm ef = new EditorForm(remoteDoc);

			Logger.debug(SyncDirAction.class, "createLocalDoc: IN="+Constants.getInstallName()+" is Cloud="+("cloud".equals(Constants.getInstallName())));
			if("cloud".equals(Constants.getInstallName()))
			{
				String lng = getLocalLng();
				Logger.debug(SyncDirAction.class, "createLocalDoc: lng="+lng);
				if (Tools.isNotEmpty(lng))
				{
					Logger.debug(getClass(), "Cloud, translating remote docid "+ remoteDoc.getDocId() +" title: "+ remoteDoc.getTitle() +" to: " + lng);
					ef = CloudToolsForCore.translate(lng, ef, localGroup);
				}
			}

			if (localDoc != null) ef.setDocId(localDoc.getDocId());
			else ef.setDocId(-1);

			ef.setData(fixGroupIdsInContent(ef.getData()));

			ef.setGroupId(localGroup.getGroupId());
			ef.setPublish("1");
			ef.setTempId(localTempId);
			ef.setPasswordProtectedString(Tools.toString(localUserGroups));
			ef.setPerexGroupString(convertPerexGroupIdsToLocal(remoteDoc.getPerexGroupIdsString()));

			Identity user = UsersDB.getCurrentUser(getContext());
			ef.setAuthorId(user.getUserId());

			//nastavime poznamku pre redaktora
			DocNoteBean note = getRemoteDocsNotes().get(remoteDoc.getDocId());
			if(note!=null)
			{
				ef.setNote(note.getNote());
			}

			int docid = EditorDB.saveEditorForm(ef, context.getRequest());

			EditorDB.cleanSessionData(context.getRequest());

			//skontroluj, ci remoteDoc nie je nejake default doc a nastav na local group
			fixDefaultDocId(remoteDoc, ef.getDocId());

			//syncni media
			List<Media> media = getRemoteMedia();
			if (media != null)
			{
				//toto su media co uz mam
				List<Media> mediaLocal = MediaDB.getMedia(getSession(), "documents", ef.getDocId(), null, 0, false);
				//toto je set medii ktore som prepisal z remote, ostatne budem musiet premazat
				Set<String> mediaLinks = new HashSet<>();

				MediaDB mediaDB = new MediaDB();
				for (Media m : media)
				{
					if (m.getMediaFkId()!=null && m.getMediaFkId().intValue()==remoteDoc.getDocId())
					{
						Media mLocal = getMediaByLink(mediaLocal, m.getMediaLink());
						if (mLocal == null) mLocal = new Media();

						mLocal.setMediaFkId(Integer.valueOf(ef.getDocId()));
						mLocal.setMediaFkTableName(m.getMediaFkTableName());
						mLocal.setMediaTitleSk(m.getMediaTitleSk());
						mLocal.setMediaTitleCz(m.getMediaTitleCz());
						mLocal.setMediaTitleEn(m.getMediaTitleEn());
						mLocal.setMediaTitleDe(m.getMediaTitleDe());
						mLocal.setMediaLink(m.getMediaLink());
						mLocal.setMediaThumbLink(m.getMediaThumbLink());
						mLocal.setMediaGroup(m.getMediaGroup());
						mLocal.setMediaInfoSk(m.getMediaInfoSk());
						mLocal.setMediaInfoCz(m.getMediaInfoCz());
						mLocal.setMediaInfoEn(m.getMediaInfoEn());
						mLocal.setMediaInfoDe(m.getMediaInfoDe());
						mLocal.setMediaSortOrder(m.getMediaSortOrder());
						mLocal.setLastUpdate(m.getLastUpdate());

						Logger.debug(SyncDirAction.class, "Saving media: "+mLocal.getMediaId()+" title="+mLocal.getMediaTitleSk()+" link="+mLocal.getMediaLink());
						mediaDB.save(mLocal);

						mediaLinks.add(m.getMediaLink());
					}
				}

				//pomaz media, ktore neboli aktualizovane
				for (Media mLocal : mediaLocal)
				{
					if (mediaLinks.contains(mLocal.getMediaLink())) continue;

					Logger.debug(SyncDirAction.class, "Deleting media: "+mLocal.getMediaId()+" title="+mLocal.getMediaTitleSk()+" link="+mLocal.getMediaLink());
					mediaDB.delete(mLocal);
				}
			}


			return docid;
		}
		return -1;
	}

	private Media getMediaByLink(List<Media> media, String mediaLink)
	{
		for (Media m : media)
		{
			if (m.getMediaLink().equals(mediaLink)) return m;
		}
		return null;
	}

	private void fixDefaultDocId(DocDetails remoteDoc, int localDocId)
	{
		for (GroupDetails remoteGroup : getRemoteGroups())
		{
			if (remoteGroup != null && remoteGroup.getDefaultDocId()==remoteDoc.getDocId())
			{
				GroupDetails localGroup = getLocalGroup(remoteGroup);
				if (localGroup != null)
				{
					localGroup.setDefaultDocId(localDocId);
					GroupsDB.getInstance().setGroup(localGroup);
				}
			}
		}
	}

	private boolean createLocalContentFile(Content.File remoteFile)
	{
		String tempPath = Tools.getRealPath(syncDir + "/content/" + remoteFile.getZipPath());
		String localPath = Tools.getRealPath(remoteFile.getVirtualPath());
		IwcmFile tempFile = new IwcmFile(tempPath);
		IwcmFile localFile = new IwcmFile(localPath);
		try
		{
			FileTools.copyFile(tempFile, localFile);
			localFile.setLastModified(remoteFile.getTime());
			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	/**
	 * Ziska ID lokalnej sablony pre ID vzdialenej sablony
	 * ak sablona neexistuje a je zvolene, ze sa ma vytvorit, tak sa vytvori
	 * @param remoteTempId
	 * @return
	 */
	public int getCreateTemplate(int remoteTempId)
	{
		if (createMissingTemplates==false) return localRootGroup.getTempId();

		TemplateDetails remoteTemp = getRemoteTemp(remoteTempId);
		if (remoteTemp==null)
		{
			context.getRequest().setAttribute("errorText", "Neexistuje remote sablona pre tempId="+remoteTempId);
			Logger.debug(SyncDirAction.class, "Neexistuje remote sablona pre tempId="+remoteTempId);
			return -1;
		}
		int localTempId = getLocalTempId(remoteTemp.getTempName());
		if (localTempId < 1 && createMissingTemplates)
		{
			int oTempId = remoteTemp.getTempId();
			String oTempName = remoteTemp.getTempName();
			String oTempLng = remoteTemp.getLng();
			int oTempHeaderDocId = remoteTemp.getHeaderDocId();
			int oTempFooterDocId = remoteTemp.getFooterDocId();
			int oTempMenuDocId = remoteTemp.getMenuDocId();
			String oAvailableGroups = remoteTemp.getAvailableGroups();

			if(remoteDocs == null) getRemoteDocs();

			//DocDetails header = selectFirst(remoteDocs,having(on(DocDetails.class).getDocId(),equalTo(remoteTemp.getHeaderDocId())));
			DocDetails header = remoteDocs.stream().filter(d -> d.getDocId() == remoteTemp.getHeaderDocId()).findFirst().orElse(null);
			//DocDetails footer = selectFirst(remoteDocs,having(on(DocDetails.class).getDocId(),equalTo(remoteTemp.getFooterDocId())));
			DocDetails footer = remoteDocs.stream().filter(d -> d.getDocId() == remoteTemp.getFooterDocId()).findFirst().orElse(null);
			//DocDetails menu = selectFirst(remoteDocs,having(on(DocDetails.class).getDocId(),equalTo(remoteTemp.getMenuDocId())));
			DocDetails menu = remoteDocs.stream().filter(d -> d.getDocId() == remoteTemp.getMenuDocId()).findFirst().orElse(null);

			//save them to global /System
			int globalHeaderFooterGroupId = Constants.getInt("headerFooterGroupId");
			int globalMenuGroupId = Constants.getInt("menuGroupId");
			if(header!=null) remoteTemp.setHeaderDocId(createLocalDoc(header, globalHeaderFooterGroupId));
			if(footer!=null) remoteTemp.setFooterDocId(createLocalDoc(footer, globalHeaderFooterGroupId));
			if(menu != null) remoteTemp.setMenuDocId(createLocalDoc(menu, globalMenuGroupId));

			if ("cloud".equals(Constants.getInstallName()))
			{
				String lng = getLocalLng();
				if (Tools.isNotEmpty(lng))
				{
					remoteTemp.setTempName(oTempName + "-"+lng);
					remoteTemp.setLng(lng);
				}
			}

			remoteTemp.setTempId(-1);

			//replace available groups for local groups
			remoteTemp.setAvailableGroups(convertGroupIdsToLocalFilter(remoteTemp.getAvailableGroups()));

			TemplatesDB.getInstance().saveTemplate(remoteTemp);
			localTempId = remoteTemp.getTempId();

			remoteTemp.setTempId(oTempId);
			remoteTemp.setTempName(oTempName);
			remoteTemp.setLng(oTempLng);
			remoteTemp.setHeaderDocId(oTempHeaderDocId);
			remoteTemp.setFooterDocId(oTempFooterDocId);
			remoteTemp.setMenuDocId(oTempMenuDocId);
			remoteTemp.setAvailableGroups(oAvailableGroups);
		}
		if (localTempId < 1)
		{
			context.getRequest().setAttribute("errorText", "Neexistuje lokálna šablóna "+remoteTemp.getTempName());
			Logger.debug(SyncDirAction.class, "Neexistuje local sablona "+remoteTemp.getTempName());
			return -1;
		}
		return localTempId;
	}

	/**
	 * Vytvori local doc s tym ze neberie ohlad na to v akom adresari bol povodne
	 * ale vytvori ho v adresari s groupid z parametra (vytvori ho len ak tam taky doc este nie je, zistuje sa podla title)
	 * @param doc
	 * @param forceLocalGroupId
	 * @author mhalas
	 * @return
	 */
	private int createLocalDoc(DocDetails doc, int forceLocalGroupId)
	{
		int docid = new SimpleQuery().forInt("Select doc_id from documents where group_id = ? and title = ?", forceLocalGroupId,doc.getTitle());
		if(docid <= 0)
		{
			int oGroupId = doc.getGroupId();
			doc.setGroupId(forceLocalGroupId);
			docid = createLocalDoc(doc);
			doc.setGroupId(oGroupId);
		}
		return docid;
	}

	private Content getRemoteContent()
	{
		if (null == remoteContent)
		{
			remoteContent = (Content) getLocalFile("content.xml");
		}
		return remoteContent;
	}

	/**
	 * Vrati zoznam importovanych suborov, vo formate vhodnom na vypisanie.
	 * Vacsina funkcie sa tyka nastavenia spravneho statusu (neexistuje, je starsi, je novsi, je iny, je rovnaky).
	 *
	 * @return
	 */
	public List<ContentFileBean> getRemoteContentFiles()
	{
		List<ContentFileBean> fileBeans = new ArrayList<>();
		Content content = getRemoteContent();
		if (null == content) return fileBeans;

		for (Numbered<Content.File> numFile : Numbered.list(content.getFiles()))
		{
			Content.File file = numFile.item;
			String path = file.getVirtualPath();
			int status = ContentFileBean.STATUS_MISSING;
			String localPath = Tools.getRealPath(path);
			IwcmFile localFile = new IwcmFile(localPath);
			if (localFile.exists())
			{
				long remoteTime = file.getTime();
				long localTime = localFile.lastModified();
				if (remoteTime > localTime)
				{
					status = ContentFileBean.STATUS_NEWER;
				}
				else if (remoteTime < localTime)
				{
					status = ContentFileBean.STATUS_OLDER;
				}
				else
				{
					status = (localFile.length() == file.getSize()) ? ContentFileBean.STATUS_CURRENT_SAME : ContentFileBean.STATUS_CURRENT_DIFFERENT;
				}
			}
			ContentFileBean fileBean = new ContentFileBean(numFile.number, path, status);
			fileBeans.add(fileBean);
		}
		return fileBeans;
	}

	/**
	 * Vrati zoznam importovanych bannerov, vo formate vhodnom na vypisanie.
	 *
	 * @return
	 */
	public List<ContentBannerBean> getRemoteContentBanners()
	{
		return BannerImporter.getBanners(getRemoteContent());
	}

	/**
	 * Vrati zoznam importovanych ankiet, vo formate vhodnom na vypisanie.
	 *
	 * @return
	 */
	public List<ContentInquiryBean> getRemoteContentInquiries()
	{
		return InquiryImporter.getInquiries(getRemoteContent(), context.getRequest());
	}

	/**
	 * Vrati zoznam importovanych adresarov galerii, vo formate vhodnom na vypisanie.
	 *
	 * @return
	 */
	public List<ContentGalleryBean.Info> getRemoteContentGalleryInfos()
	{
		return GalleryImporter.getGalleryInfos(getRemoteContent());
	}

	/**
	 * Vrati zoznam importovanych obrazkov galerie, vo formate vhodnom na vypisanie.
	 *
	 * @return
	 */
	public List<ContentGalleryBean.Image> getRemoteContentGalleryImages()
	{
		return GalleryImporter.getGalleryImages(getRemoteContent(), context.getRequest());
	}

	/**
	 * Vrati zoznam remote adresarov
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<GroupDetails> getRemoteGroups()
	{
		if (remoteGroups==null && remoteGroupId>0)
		{
			remoteGroups = (List<GroupDetails>) getLocalFile("groups.xml");
		}

		return remoteGroups;
	}

	/**
	 * Vrati remote adresar na zaklade jeho ID
	 * @param remoteGroupId
	 * @return
	 */
	public GroupDetails getRemoteGroup(int remoteGroupId)
	{
		for (GroupDetails remoteGroup : getRemoteGroups())
		{
			if (remoteGroup.getGroupId()==remoteGroupId)
			{
				return remoteGroup;
			}
		}
		return null;
	}

	/**
	 * Vrati remote web stranky
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DocDetails> getRemoteDocs()
	{
		if (remoteDocs==null && remoteGroupId>0)
		{
			remoteDocs = (List<DocDetails>) getLocalFile("docs.xml");
		}
		/**
		 * musime to zosortovat tak aby prve boli hlavne stranky priecinkov
		 * inak mozu nastat problemy pri generovani virtualPath pri cloud prekladoch
		 * ticket 15053
		 */
		sortByMainPages();
		return remoteDocs;

	}

	/**
	 * Zosortuje zoznam doc-s podla toho ci su hlavne stranky priecinkov
	 * ak je stranka hlavna nejakemu priecinku tak bude niekde na zaciatku zoznamu
	 *
	 * @author mhalas
	 */
	private void sortByMainPages()
	{
		if (remoteDocs == null || remoteGroups == null) return;

		Set<Integer> mainPages = new HashSet<>();
		for(GroupDetails gd : remoteGroups)
		{
			mainPages.add(gd.getDefaultDocId());
		}
		//Logger.debug(getClass(), "main pages docids : " + Arrays.toString(mainPages.toArray()));
		final Set<Integer> finalMainPages = mainPages;

		//Logger.debug(getClass(), "docids before sort: " + Arrays.toString(Lambda.convert(remoteDocs, new PropertyExtractor("docId")).toArray()));

		Collections.sort(remoteDocs, new Comparator<DocDetails>(){
			@Override
			public int compare(DocDetails o1, DocDetails o2)
			{
				if(finalMainPages.contains(o1.getDocId()) && finalMainPages.contains(o2.getDocId())) return 0;
				if(finalMainPages.contains(o1.getDocId()) && !finalMainPages.contains(o2.getDocId())) return -1;
				if(!finalMainPages.contains(o1.getDocId()) && finalMainPages.contains(o2.getDocId())) return 1;
				return o1.getSortPriority() - o2.getSortPriority();
			}
		});

		//Logger.debug(getClass(), "docids after sort: " + Arrays.toString(Lambda.convert(remoteDocs, new PropertyExtractor("docId")).toArray()));
	}

	@SuppressWarnings("unchecked")
	public List<Media> getRemoteMedia()
	{
		if (remoteMedia==null)
		{
			remoteMedia = (List<Media>) getLocalFile("media.xml");
		}
		return remoteMedia;
	}

	/**
	 * Ziska root path z remote adresara (spolocna pre vsetky remote podadresare)
	 * @return
	 */
	public String getRemoteRootPath()
	{
		if (remoteRootPath == null)
		{
			if (getRemoteGroups().size()>0)
			{
				remoteRootPath = getRemoteGroups().get(0).getFullPath();
				//int i = remoteRootPath.lastIndexOf('/');
				//if (i>0) remoteRootPath = remoteRootPath.substring(0, i);
			}
		}
		return remoteRootPath;
	}

	/**
	 * Ziska local cestu k adresaru
	 * @param remoteGroup
	 * @return
	 */
	public String getLocalPath(GroupDetails remoteGroup)
	{
		//Logger.debug(SyncDirAction.class, "getLocalPath, rgfp="+remoteGroup.getFullPath()+" root="+getRemoteRootPath());

		String basePath = remoteGroup.getFullPath().substring(getRemoteRootPath().length());
		if("cloud".equals(Constants.getInstallName()))
		{
			String lng = getLocalLng();
			if (Tools.isNotEmpty(lng) && basePath.length()>1 && basePath.startsWith("/System")==false)
			{
				String[] exploded = Tools.getTokens(basePath, "/");
				StringBuilder translated = new StringBuilder();
				for (String str : exploded)
				{
					//basePath = CloudToolsForCore.translate(lng, basePath);
					translated.append('/');
					translated.append(CloudToolsForCore.translate(lng, str));
				}
				basePath = translated.toString();
				Logger.debug(getClass(), "translating basePath to: " + basePath);
			}
		}
		String localPath = getLocalRootGroup().getFullPath()+basePath; //TODO: preklad?
		return localPath;
	}

	/**
	 * Ziska local adresar
	 * @param remoteGroupId
	 * @return
	 */
	public GroupDetails getLocalGroup(int remoteGroupId)
	{
		GroupDetails remoteGroup = getRemoteGroup(remoteGroupId);
		if (remoteGroup!=null) return getLocalGroup(remoteGroup);
		return null;
	}

	/**
	 * Ziska local adresar
	 * @param remoteGroup
	 * @return
	 */
	public GroupDetails getLocalGroup(GroupDetails remoteGroup)
	{
		String localPath = getLocalPath(remoteGroup);
		try
		{
			return GroupsDB.getInstance().getGroupByPath(localPath);
		}
		catch (ConcurrentModificationException ex)
		{
			GroupsDB groupsDB = GroupsDB.getInstance(true);
			return groupsDB.getGroupByPath(localPath);
		}
	}

	/**
	 * Ziska local adresar
	 * @param localPath
	 * @return
	 */
	public GroupDetails getLocalGroup(String localPath)
	{
		return GroupsDB.getInstance().getGroupByPathAndDomain(localPath, localSrcGroup.getDomainName());
	}

	/**
	 * Ziska local root adresar (na zaklade zadaneho localGroupId)
	 * @return
	 */
	public GroupDetails getLocalRootGroup()
	{
		if (localRootGroup == null)
		{
			localRootGroup = GroupsDB.getInstance().getGroup(getLocalGroupId());
		}
		return localRootGroup;
	}

	/**
	 * Vrati local web stranku pre zadanu remote stranku
	 * @param remoteDoc
	 * @return
	 */
	public DocDetails getLocalDoc(DocDetails remoteDoc)
	{
		GroupDetails localGroup = getLocalGroup(remoteDoc.getGroupId());
		if (localGroup!=null)
		{
			//najdi local doc
			DocDB docDB = DocDB.getInstance();
			List<DocDetails> localDocs = docDB.getBasicDocDetailsByGroup(localGroup.getGroupId(), DocDB.ORDER_PRIORITY);
			for (DocDetails doc : localDocs)
			{
				if (comapreDocs(doc, remoteDoc))
				{
					return(doc);
				}
			}
		}
		return null;
	}

	private boolean comapreDocs(DocDetails doc, DocDetails remoteDoc) {
		if("url".equals(this.compareBy)) {
			return (Tools.isNotEmpty(remoteDoc.getVirtualPath()) && Tools.isNotEmpty(doc.getVirtualPath()) && doc.getVirtualPath().equals(remoteDoc.getVirtualPath()));
		} else if("none".equals(this.compareBy)) {
			// allways return false - aka allways create new doc
			return false;
		} else if("fieldA".equals(this.compareBy)) {
			return doc.getFieldA().equals(remoteDoc.getFieldA());
		} else if("fieldB".equals(this.compareBy)) {
			return doc.getFieldB().equals(remoteDoc.getFieldB());
		} else if("fieldC".equals(this.compareBy)) {
			return doc.getFieldC().equals(remoteDoc.getFieldC());
		} else {
			// nameOrUrl / null / or anything else - default
			return (doc.getTitle().equals(remoteDoc.getTitle()) || (Tools.isNotEmpty(remoteDoc.getVirtualPath()) && Tools.isNotEmpty(doc.getVirtualPath()) && doc.getVirtualPath().equals(remoteDoc.getVirtualPath()) ));
		}
	}

	/**
	 * Vrati nazov vzdialenej sablony podla id
	 * @param remoteTempId
	 * @return
	 */
	public String getRemoteTempName(int remoteTempId)
	{
		TemplateDetails temp = getRemoteTemp(remoteTempId);
		return (null == temp) ? null : temp.getTempName();
	}

	/**
	 * Vrati vzdialenu sablonu podla id
	 * @param remoteTempId
	 * @return
	 */
	public TemplateDetails getRemoteTemp(int remoteTempId)
	{
		for (TemplateDetails temp : getRemoteTemps())
		{
			if (temp.getTempId()==remoteTempId) return temp;
		}
		return null;
	}

	/**
	 * Vrati nazov lokalnej sablony podla id
	 * @param localTempId
	 * @return
	 */
	public String getLocalTempName(int localTempId)
	{
		TemplateDetails temp = TemplatesDB.getInstance().getTemplate(localTempId);
		if (temp!=null) return temp.getTempName();
		return null;
	}

	/**
	 * Vrati id lokalnej sablony podla nazvu
	 * @param tempName
	 * @return
	 */
	public int getLocalTempId(String tempName)
	{
		if ("cloud".equals(Constants.getInstallName()))
		{
			String lng = getLocalLng();
			if (Tools.isNotEmpty(lng))
			{
				tempName = tempName + "-"+lng;
			}
		}

		TemplateDetails temp = TemplatesDB.getInstance().getTemplate(tempName);
		if (temp!=null) return temp.getTempId();
		return -1;
	}

	/**
	 * Vrati zoznam remote sablon
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TemplateDetails> getRemoteTemps()
	{
		if (remoteTemps == null)
		{
			remoteTemps = (List<TemplateDetails>) getLocalFile("temps.xml");
		}
		return remoteTemps;
	}

	private int getCreateUserGroup(int remoteUserGroupId)
	{
		UserGroupDetails remoteUserGroup = getRemoteUserGroup(remoteUserGroupId);
		if (remoteUserGroup==null)
		{
			context.getRequest().setAttribute("errorText", "Neexistuje remote pouzivatelska skupina pre userGroupId="+remoteUserGroupId);
			Logger.debug(SyncDirAction.class, "Neexistuje remote pouzivatelska skupina pre userGroupId="+remoteUserGroupId);
			return -1;
		}
		int localUserGroupId = getLocalUserGroupId(remoteUserGroup.getUserGroupName());
		if (localUserGroupId < 1 && createMissingUserGroups)
		{
			int oUserGroupId = remoteUserGroup.getUserGroupId();

			remoteUserGroup.setUserGroupId(-1);
			UserGroupsDB.saveUserGroup(remoteUserGroup);
			localUserGroupId = remoteUserGroup.getUserGroupId();

			remoteUserGroup.setUserGroupId(oUserGroupId);
		}
		if (localUserGroupId < 1)
		{
			context.getRequest().setAttribute("errorText", "Neexistuje lokálna používateľská skupina "+remoteUserGroup.getUserGroupName());
			Logger.debug(SyncDirAction.class, "Neexistuje lokálna používateľská skupina "+remoteUserGroup.getUserGroupName());
			return -1;
		}
		return localUserGroupId;
	}

	/**
	 * Vrati remote nazov user group podla zadaneho id
	 * @param remoteUserGroupId
	 * @return
	 */
	public String getRemoteUserGroupName(int remoteUserGroupId)
	{
		UserGroupDetails ugd = getRemoteUserGroup(remoteUserGroupId);
		return (null == ugd) ? null : ugd.getUserGroupName();
	}

	/**
	 * Vrati vzdialenu pouzivatelsku skupinu na zaklade jej ID
	 * @param remoteUserGroupId
	 * @return
	 */
	public UserGroupDetails getRemoteUserGroup(int remoteUserGroupId)
	{
		for (UserGroupDetails ugd : getRemoteUserGroups())
		{
			if (ugd.getUserGroupId()==remoteUserGroupId) return ugd;
		}
		return null;
	}

	/**
	 * Vrati lokalne id pouzivatelskej skupiny podla nazvu
	 * @param userGroupName
	 * @return
	 */
	public int getLocalUserGroupId(String userGroupName)
	{
		UserGroupDetails ugd = getLocalUserGroup(userGroupName);
		return (null == ugd) ? -1 : ugd.getUserGroupId();
	}

	/**
	 * Vrati lokalnu pouzivatelsku skupinu na zaklade jej nazvu
	 * @param userGroupName
	 * @return
	 */
	public UserGroupDetails getLocalUserGroup(String userGroupName)
	{
		return UserGroupsDB.getInstance().getUserGroup(userGroupName);
	}

	/**
	 * Vrati lokalnu pouzivatelsku skupinu na zaklade jej ID
	 * @param localUserGroupId
	 * @return
	 */
	public UserGroupDetails getLocalUserGroup(int localUserGroupId)
	{
		return UserGroupsDB.getInstance().getUserGroup(localUserGroupId);
	}

	/**
	 * Vrati zoznam remote user groups
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<UserGroupDetails> getRemoteUserGroups()
	{
		if (remoteUserGroups == null)
		{
			remoteUserGroups = (List<UserGroupDetails>) getLocalFile("usergroups.xml");
		}
		return remoteUserGroups;
	}


	private int getCreatePerexGroup(int remotePerexGroupId)
	{
		PerexGroupBean remotePerexGroup = getRemotePerexGroup(remotePerexGroupId);
		if (remotePerexGroup==null)
		{
			context.getRequest().setAttribute("errorText", "Neexistuje remote perex skupina pre perexGroupId="+remotePerexGroupId);
			Logger.debug(SyncDirAction.class, "Neexistuje remote perex skupina pre perexGroupId="+remotePerexGroupId);
			return -1;
		}
		int localPerexGroupId = getLocalPerexGroupId(remotePerexGroup.getPerexGroupName());
		if (localPerexGroupId < 1 && createMissingUserGroups)
		{
			//premigrovat ID adresarov, POZOR: v groups.xml mame len exportovane skupiny, takze to nevie setnut prava na ine skupiny ako exportovane
			StringBuilder availableGroups = new StringBuilder("");
			if (Tools.isNotEmpty(remotePerexGroup.getAvailableGroups())) {
				int[] remoteGroupIds = Tools.getTokensInt(remotePerexGroup.getAvailableGroups(), ",");
				for (int groupId : remoteGroupIds) {
					GroupDetails group = getLocalGroup(groupId);
					if (group != null) {
						if (Tools.isNotEmpty(availableGroups)) availableGroups.append(",");
						availableGroups.append(String.valueOf(group.getGroupId()));
					}
				}
			}
			DocDB.getInstance().savePerexGroup(remotePerexGroup.getPerexGroupName(), -1, availableGroups.toString(), null);
			localPerexGroupId = getLocalPerexGroupId(remotePerexGroup.getPerexGroupName());
		}
		if (localPerexGroupId < 1)
		{
			context.getRequest().setAttribute("errorText", "Neexistuje lokálna perex skupina "+remotePerexGroup.getPerexGroupName());
			Logger.debug(SyncDirAction.class, "Neexistuje lokálna perex skupina "+remotePerexGroup.getPerexGroupName());
			return -1;
		}
		return localPerexGroupId;
	}

	/**
	 * Vrati remote nazov perex group podla zadaneho id
	 * @param remotePerexGroupId
	 * @return
	 */
	public String getRemotePerexGroupName(int remotePerexGroupId)
	{
		PerexGroupBean pgb = getRemotePerexGroup(remotePerexGroupId);
		return (null == pgb) ? null : pgb.getPerexGroupName();
	}

	/**
	 * Vrati vzdialenu perex skupinu na zaklade jej ID
	 * @param remotePerexGroupId
	 * @return
	 */
	public PerexGroupBean getRemotePerexGroup(int remotePerexGroupId)
	{
		for (PerexGroupBean pgd : getRemotePerexGroups())
		{
			if (pgd.getPerexGroupId()==remotePerexGroupId) return pgd;
		}
		return null;
	}

	/**
	 * Vrati lokalne id perex skupiny podla nazvu
	 * @param perexGroupName
	 * @return
	 */
	public int getLocalPerexGroupId(String perexGroupName)
	{
		PerexGroupBean pgd = getLocalPerexGroup(perexGroupName);
		return (null == pgd) ? -1 : pgd.getPerexGroupId();
	}

	/**
	 * Vrati lokalnu perex skupinu na zaklade jej nazvu
	 * @param perexGroupName
	 * @return
	 */
	public PerexGroupBean getLocalPerexGroup(String perexGroupName)
	{
		return DocDB.getInstance().getPerexGroupByName(perexGroupName);
	}

	/**
	 * Vrati lokalnu perex skupinu na zaklade jej ID
	 * @param localPerexGroupId
	 * @return
	 */
	public PerexGroupBean getLocalPerexGroup(int localPerexGroupId)
	{
		return DocDB.getInstance().getPerexGroup(localPerexGroupId, null);
	}


	/**
	 * Vrati zoznam remote perex groups
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<PerexGroupBean> getRemotePerexGroups()
	{
		if (remotePerexGroups == null)
		{
			remotePerexGroups = (List<PerexGroupBean>) getLocalFile("perexgroups.xml");
		}
		return remotePerexGroups;
	}

	@Override
	public ActionBeanContext getContext()
	{
		return context;
	}
	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);

		Identity user = getUser();
		if (user != null)
		{
			// ak je true synchrozujeme s xml subormi v syncdir a nastavime fake loginy, ktore sa ale nikde nepouzije, su tam len kvoli valid a pod.
			if (Tools.isNotEmpty( context.getRequest().getParameter("syncDir")))
			{
				setSyncDir(context.getRequest().getParameter("syncDir"));
			}
		}

		//Set comapreBy
		this.compareBy = context.getRequest().getParameter("compareBy");
		if(this.compareBy == null) this.compareBy = "nameOrUrl";
	}

	public void setSyncDir(String syncDir)
	{
		this.syncDir = syncDir;
	}

	public int getLocalGroupId()
	{
		return localGroupId;
	}

	public void setLocalGroupId(int localGroupId)
	{
		this.localGroupId = localGroupId;

		this.localSrcGroup = GroupsDB.getInstance().getGroup(localGroupId);
	}

	public GroupDetails getLocalSrcGroup()
	{
		return localSrcGroup;
	}

	public int getRemoteGroupId()
	{
		return remoteGroupId;
	}

	public void setRemoteGroupId(int remoteGroupId)
	{
		this.remoteGroupId = remoteGroupId;
	}

	public boolean isCreateMissingTemplates()
	{
		return createMissingTemplates;
	}

	public void setCreateMissingTemplates(boolean createMissingTemplates)
	{
		this.createMissingTemplates = createMissingTemplates;
	}

	public boolean isCreateMissingUserGroups()
	{
		return createMissingUserGroups;
	}

	public void setCreateMissingUserGroups(boolean createMissingUserGroups)
	{
		this.createMissingUserGroups = createMissingUserGroups;
	}

	/**
	 * Vrati dekodovany objekt z XML suboru.
	 *
	 * @param filename  retazec obsahujuci nazov XML suboru
	 * @return          objekt z tohto suboru, alebo null ak nieco nevyjde
	 */
	private Object getLocalFile(String filename)
	{
		InputStream inputStream = null;
		InputStream inputStream2 = null;
		try
		{
			String realPath = Tools.getRealPath(syncDir) + java.io.File.separatorChar + filename;
			Logger.debug(SyncDirAction.class, "Reading local file: "+realPath);
			inputStream = new IwcmInputStream(realPath);
			inputStream2 = checkXmlForAttack(inputStream);
			XMLDecoder decoder = new XMLDecoder(inputStream2, null, new WarningListener());
			Object o = decoder.readObject();
			decoder.close();
			//Logger.debug(SyncDirAction.class, "readed, o="+o);
			return o;
		}
		catch (IOException|IndexOutOfBoundsException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			errorMessages.add(ex.getMessage());
		}
		finally {
			if (inputStream != null) {
				try { inputStream.close(); } catch (Exception ex) {}
			}
			if (inputStream2 != null) {
				try { inputStream2.close(); } catch (Exception ex) {}
			}
		}
		return null;
	}

	public void cleanFiles()
	{
		if (!Tools.isEmpty(syncDir))
		{
			Content content = getRemoteContent();
			if (content!=null && content.getFiles()!=null) {
				String tmpDir = Tools.getRealPath(syncDir);
				for (Content.File file : content.getFiles())
				{
					new IwcmFile(tmpDir + java.io.File.separatorChar + "/content/" + file.getZipPath().replace("/", File.separator)).delete();
				}
				new IwcmFile(tmpDir + java.io.File.separatorChar + "/content").delete();
				for (String filename : new String[]{ "docs.xml", "groups.xml", "temps.xml", "perexgroups.xml", "usergroups.xml", "content.xml" })
				{
					new IwcmFile(tmpDir + java.io.File.separatorChar + filename).delete();
				}
				new IwcmFile(tmpDir).delete();
			}
		}
	}

	private Identity getUser()
	{
		return (Identity) context.getRequest().getSession().getAttribute(Constants.USER_KEY);
	}

	public String getLocalDomain()
	{
		return localDomain;
	}

	public void setLocalDomain(String localDomain)
	{
		this.localDomain = localDomain;
	}

	/**
	 * Jednoduche premapovanie ID adresarov na nove hodnoty v lokalnom systeme
	 * @return
	 */
	private String fixGroupIdsInContent(String htmlCode)
	{
		int start = htmlCode.indexOf("groupIds=");
		if (start==-1) return htmlCode;

		//prehladame hodnoty groupIds= a nahradime ich za novy vyraz
		Matcher matcher = Pattern.compile("!INCLUDE\\(/components/[^)]*groupIds=(['\"&quot;+1234567890]*)[^)]*\\)!").matcher(htmlCode);
		while (matcher.find())
		{
		 	Logger.debug(SyncDirAction.class, "Mam:");
		 	Logger.debug(SyncDirAction.class, matcher.group());
		 	Logger.debug(SyncDirAction.class, matcher.group(1));
			String groupIds = matcher.group(1);
			Logger.debug(SyncDirAction.class, "groupIds="+groupIds);
			String groupIdsOrig = groupIds;
			groupIds = Tools.replace(groupIds, "&quot;", "\"");
			groupIds = groupIds.replace('"', ' ');
			groupIds = groupIds.replace('\'', ' ');
			groupIds = groupIds.trim();
			int[] groupIdsArr = Tools.getTokensInt(groupIds, "+");
			StringBuilder newGroupIds = new StringBuilder();
			for (int groupId : groupIdsArr)
			{
				int localId = groupId;
				GroupDetails localGroup = getLocalGroup(groupId);
				if (localGroup != null) localId = localGroup.getGroupId();
				if (newGroupIds.length()>0) newGroupIds.append('+');
				newGroupIds.append(localId);
			}

			if (Tools.isEmpty(groupIdsOrig) || Tools.isEmpty(newGroupIds.toString())) continue;

			Logger.debug(SyncDirAction.class, "replacing to:"+newGroupIds.toString());
			Logger.debug(SyncDirAction.class, "test:"+"groupIds="+groupIdsOrig+",");

			if (groupIdsOrig.charAt(0)=='"' || groupIdsOrig.charAt(0)=='\'')
			{
				newGroupIds.insert(0, groupIdsOrig.charAt(0));
				newGroupIds.append(groupIdsOrig.charAt(0));
			}

			htmlCode = Tools.replace(htmlCode, "groupIds="+groupIdsOrig+",", "groupIds="+newGroupIds.toString()+",");
			htmlCode = Tools.replace(htmlCode, "groupIds="+groupIdsOrig+")", "groupIds="+newGroupIds.toString()+")");
			htmlCode = Tools.replace(htmlCode, "groupIds="+groupIdsOrig+" ", "groupIds="+newGroupIds.toString()+" ");
		}

		return htmlCode;
	}

	/**
	 * Vrati jazyk do ktoreho sa ma vykonat preklad pri klonovani
	 * @return
	 */
	private String getLocalLng()
	{
		String lng = getRequest().getParameter("language");

		if ("cloud".equals(Constants.getInstallName()))
		{
			if ("sk".equals(lng) || "cz".equals(lng) || "de".equals(lng))
			{
				return lng;
			}
			return null;
		}

		return lng;
	}

	private void saveRollbackJson(List<Integer> historyIds, List<Integer> historyGroupsIds)
	{
		String archivePath = "/files/protected/backup/"; //NOSONAR
		String archiveName = localGroupId + "_" + new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date())+".json";
		BufferedOutputStream output = null;
		try
		{
			JSONArray docs = new JSONArray();
			for(Integer id : historyIds)
			{
				JSONObject item = new JSONObject();
				item.put("historyId",  id);
			   docs.put(item);
			}

			JSONArray groups = new JSONArray();
			for(Integer id : historyGroupsIds)
			{
				JSONObject item = new JSONObject();
				item.put("historyGroupId",  id);
				groups.put(item);
			}

			JSONObject result = new JSONObject();
			result.put("docs", docs);
			result.put("groups", groups);

			IwcmFile dir = IwcmFile.fromVirtualPath(archivePath);
			Logger.debug(SyncDirAction.class, "Archive directory: "+dir.getAbsolutePath()+" exists: "+dir.exists());
			if(!dir.exists())
			{
				Logger.debug(SyncDirAction.class, "Creating directory: "+dir.getAbsolutePath());
				dir.mkdirs();
			}

			output = new BufferedOutputStream(new IwcmOutputStream(new IwcmFile(PathFilter.getRealPath(archivePath), archiveName)));
			output.write(result.toString(3).getBytes(StandardCharsets.UTF_8));
			output.close();
			output = null;
		}
		catch(Exception e)	{sk.iway.iwcm.Logger.error(e);}
		finally
		{
			if(output!=null)
			{
				try						{output.close();}
				catch (Exception ex)	{sk.iway.iwcm.Logger.error(ex);}
			}
		}
	}

	private List<Integer> getNewGroups(Date start)
	{
		List<Integer> result = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT schedule_id FROM groups_scheduler WHERE save_date >= ?");
			ps.setTimestamp(1, new Timestamp(start.getTime()));
			rs = ps.executeQuery();
			while (rs.next())
			{
				result.add(rs.getInt("schedule_id"));
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

	@SuppressWarnings("unchecked")
	public Map<Integer, DocNoteBean> getRemoteDocsNotes()
	{
		if (remoteNotesMap==null)
		{
			remoteNotesMap = new HashMap<>();
			List<DocNoteBean> remoteNotes = (List<DocNoteBean>) getLocalFile("notes.xml");

			if (remoteNotes != null)
			{
				for (DocNoteBean note : remoteNotes)
				{
					remoteNotesMap.put(note.getDocId(), note);
				}
			}
		}

		return remoteNotesMap;
	}

	// [#37105 - Druhy nalez z penetracnycch testov] - uloha #0
	public static InputStream checkXmlForAttack(InputStream inputStream) throws IOException {
		String xmlDecoderAllowedClasses = Constants.getString("XMLDecoderAllowedClasses", "");
		List<String> allowedClasses = new ArrayList<>();
		for (String className : Tools.getTokens(xmlDecoderAllowedClasses, ",")) {
			className = className.trim();
			//toto nemozeme povolit, nie je na to dovod
			if (className.contains("java.lang.Runtime")) continue;

			allowedClasses.add(className);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		org.apache.commons.io.IOUtils.copy(inputStream, baos);

		try { inputStream.close(); } catch (IOException ex) { sk.iway.iwcm.Logger.error(ex); }

		byte[] bytes = baos.toByteArray();

		final String xml = new String(bytes, StandardCharsets.UTF_8);
		final String regex = "class=\"[\\w\\.]*\"";
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(xml);

		final Set<String> classes = new HashSet<>();
		while (matcher.find()) {
			String match = matcher.group(0);
			match = Tools.replace(match, "\"", "");
			match = Tools.replace(match, "class=", "");
			//musi to obsahovat nejaky package, v autoupdate v TB sme mali HTML kod sablon a tam bolo class="meno" vramci HTML kodu
			if (match.contains(".")) classes.add(match);
		}

		if (!allowedClasses.containsAll(classes)) {
			List<String> notAllowedClasses = classes.stream().filter(c -> !allowedClasses.contains(c)).collect(Collectors.toList());
			String message = String.format("XMLDecoder - not allowed classes found: %s", Tools.join(notAllowedClasses, ", "));
			log.debug(message);
			throw new IOException(message);
		}

		return new ByteArrayInputStream(bytes);
	}
}
