package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEntity;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupEditorField;
import sk.iway.iwcm.doc.GroupPublisher;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * Rest controlle pre dtatabulku v administracii web stranky #44836
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/groups")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class GroupsRestController extends DatatableRestControllerV2<GroupDetails, Long> {

    private final GroupSchedulerDtoRepository groupSchedulerDtoRepository;
    private TranslationKeyRepository translationKeyRepository;
    private EditorFacade editorFacade;

    public GroupsRestController(GroupSchedulerDtoRepository groupSchedulerDtoRepository, TranslationKeyRepository translationKeyRepository, EditorFacade editorFacade) {
        super(null);
        this.groupSchedulerDtoRepository = groupSchedulerDtoRepository;
        this.translationKeyRepository = translationKeyRepository;
        this.editorFacade = editorFacade;
    }

    @Override
    public Page<GroupDetails> getAllItems(Pageable pageable) {

        int userGroupId = Tools.getIntValue(getRequest().getParameter("userGroupId"), -1);
        int tempId = Tools.getIntValue(getRequest().getParameter("tempId"), -1);
        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), 0);

        if(userGroupId != -1) {
            //chceme vratit adresare podla zadaneho ID skupiny pouzivatelov, pouziva sa na zoznam adresarov s danou skupinou
            DatatablePageImpl<GroupDetails> page = getAllItemsByUserGroupId(groupId, getRequest(), userGroupId);
            return page;
        }

        if(tempId != -1) {
            //Return Groups that are using this template (by template id)
            DatatablePageImpl<GroupDetails> page = getAllItemsByTempId(groupId, getRequest(), tempId);
            return page;
        }

        DatatablePageImpl<GroupDetails> page = getAllItems(groupId, getRequest());

        processFromEntity(page, ProcessItemAction.GETALL);

        if (page == null) {
            //throwError("admin.editor_dir.dontHavePermsForThisDir");
            page = new DatatablePageImpl<>(new ArrayList<>());
        }

        return page;
    }

    public static DatatablePageImpl<GroupDetails> getAllItemsByUserGroupId(int groupId, HttpServletRequest request, int userGroupId) {
        Identity user = UsersDB.getCurrentUser(request);
        WebpagesService ws = new WebpagesService(groupId, user, Prop.getInstance(request), request);

        DatatablePageImpl<GroupDetails> page = new DatatablePageImpl<>(ws.getGroupsByPasswordProtected(userGroupId));
        return page;
    }

    public static DatatablePageImpl<GroupDetails> getAllItemsByTempId(int groupId, HttpServletRequest request, int tempId) {
        Identity user = UsersDB.getCurrentUser(request);
        WebpagesService ws = new WebpagesService(groupId, user, Prop.getInstance(request), request);

        DatatablePageImpl<GroupDetails> page = new DatatablePageImpl<>(ws.getGroupsByTemplateId(tempId));
        return page;
    }

    public static DatatablePageImpl<GroupDetails> getAllItems(int groupId, HttpServletRequest request) {

        Identity user = UsersDB.getCurrentUser(request);

        if (GroupsDB.isGroupEditable(user, groupId)==false) return null;

        WebpagesService ws = new WebpagesService(groupId, user, Prop.getInstance(request), request);
        List<GroupDetails> result = new ArrayList<>();

        GroupDetails group = ws.getGroup();
        if (group != null)
            result.add(group);

        if (groupId > 0) {
            //ziskaj aj podadresare
            List<GroupDetails> subGroups = GroupsDB.getInstance().getGroups(groupId);
            if (subGroups != null) result.addAll(subGroups);
        }

        DatatablePageImpl<GroupDetails> page = new DatatablePageImpl<>(result);

        addOptions(page, ws, user, group, request);

        return page;
    }

    public static void addOptions(DatatablePageImpl<GroupDetails> page, WebpagesService ws, Identity user, GroupDetails group, HttpServletRequest request) {
        page.addOptions("lng", ws.getOptionsLanguages(request), "label", "value", false);
        page.addOptions("internal", ws.getOptionsInternal(), "label", "value", false);
        page.addOptions("tempId", ws.getOptionsTemplates(user, group), "label", "value", false);
        page.addOptions("newPageDocIdTemplate", ws.getOptionsNewPageHTMLCode(), "label", "value", false);
        page.addOptions("showInNavbar", ws.getOptionsNavbar(false), "label", "value", false);
        page.addOptions("loggedShowInNavbar", ws.getOptionsNavbar(true), "label", "value", false);
        page.addOptions("showInSitemap", ws.getOptionsSitemap(false), "label", "value", false);
        page.addOptions("loggedShowInSitemap", ws.getOptionsSitemap(true), "label", "value", false);
        page.addOptions("menuType", ws.getMenuType(false), "label", "value", false);
        page.addOptions("loggedMenuType", ws.getMenuType(true), "label", "value", false);
        page.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);
        page.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
    }

    @Override
    public GroupDetails getOneItem(long id) {
        Identity user = UsersDB.getCurrentUser(getRequest());

        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails group = null;
        int schedulerId = Tools.getIntValue(getRequest().getParameter("schedulerId"), -1);

        // Create new group
        if (id == -1) {
            // Get groupID from new group is made (it's parentGroupId)
            int parentGroupId = Tools.getIntValue(getRequest().getParameter("groupId"), 0);

            if (GroupsDB.isGroupEditable(user, parentGroupId)==false) return null;

            group = groupsDB.getNewGroupDetails("", parentGroupId);
            if (id<1) {
                if (Constants.getBoolean("multiDomainEnabled")) {
                    group.setDomainName(DocDB.getDomain(getRequest()));
                } else {
                    group.setDomainName(""); //inak by tu bolo www..sk
                }
            }

        } else {
            if(schedulerId > 0) {
                Optional<GroupSchedulerDto> dto = groupSchedulerDtoRepository.findById((long) schedulerId);
                if (dto.isPresent()) group = fromGroupSchedulerDtoToGroupDetail(dto.get());
            } else {
                // Edit exist group
                group = groupsDB.getGroup((int) id);
            }
        }
        GroupEditorField gef = new GroupEditorField();
        group = gef.fromGroupDetails(group);

        if (group.getGroupId()>0 && GroupsDB.isGroupEditable(user, group.getGroupId())==false) {
            throwError("admin.editor_dir.dontHavePermsForThisDir");
        }

        return group;
    }

    public GroupDetails fromGroupSchedulerDtoToGroupDetail(GroupSchedulerDto dto) {
        GroupDetails oldGroupRecord = new GroupDetails();

        oldGroupRecord.setGroupId(dto.getGroupId());
        oldGroupRecord.setGroupName(dto.getGroupName());
        oldGroupRecord.setNavbar(dto.getNavbar());
        oldGroupRecord.setUrlDirName(dto.getUrlDirName());
        oldGroupRecord.setDomainName(dto.getDomainName());
        oldGroupRecord.setDefaultDocId(dto.getDefaultDocId());
        oldGroupRecord.setInternal(dto.getInternal());
        oldGroupRecord.setSortPriority(dto.getSortPriority());
        oldGroupRecord.setTempId(dto.getTempId());
        oldGroupRecord.setForceTheUseOfGroupTemplate(dto.getForceGroupTemplate());
        oldGroupRecord.setLng(dto.getLng());
        oldGroupRecord.setNewPageDocIdTemplate(dto.getNewPageDocidTemplate());
        oldGroupRecord.setHtmlHead(dto.getHtmlHead());
        oldGroupRecord.setFieldA(dto.getFieldA());
        oldGroupRecord.setFieldB(dto.getFieldB());
        oldGroupRecord.setFieldC(dto.getFieldC());
        oldGroupRecord.setFieldD(dto.getFieldD());
        oldGroupRecord.setMenuType(dto.getMenuType());
        //oldGroupRecord.setNotLoggedNavbar(dto.);
        oldGroupRecord.setLoggedMenuType(dto.getLoggedMenuType());
        //oldGroupRecord.setLoggedNavbar(dto.);
        //oldGroupRecord.setLoggedSitemap(dto.);
        oldGroupRecord.setPasswordProtected(dto.getPasswordProtected());
        oldGroupRecord.setParentGroupId(dto.getParentGroupId());
        //oldGroupRecord.setFullPath(dto.);

        //Pridane atributy - ticket 54205
        oldGroupRecord.setShowInNavbar(dto.getShowInNavbar());
        oldGroupRecord.setShowInSitemap(dto.getShowInSitemap());
        oldGroupRecord.setLoggedShowInNavbar(dto.getLoggedShowInNavbar());
        oldGroupRecord.setLoggedShowInSitemap(dto.getLoggedShowInSitemap());

        return oldGroupRecord;
    }

    @Override
    public GroupDetails insertItem(GroupDetails entity) {
        return editItem(entity, 0);
    }

    @Override
    public void beforeDuplicate(GroupDetails entity) {
        entity.setDefaultDocId(-1);
        entity.getEditorFields().getDefaultDocDetails().setDocId(0);
    }

    @Override
    public GroupDetails editItem(GroupDetails entity, long id) {
        entity.getEditorFields().toGroupDetails(entity);
        GroupsDB groupsDB = GroupsDB.getInstance();
        Identity user = getUser();
        List<String> errors = new ArrayList<>();
        if (!this.canModifyThisGroup(entity, user, "editDir", errors)) {
            throwError(errors);
            return entity;
        }

        GroupDetails groupDetails = groupsDB.getGroup(entity.getGroupId());
        GroupDetails oldGroupDetails = null;
        try {
            if (groupDetails != null) {
                oldGroupDetails = (GroupDetails) groupDetails.clone();
            }
        } catch (CloneNotSupportedException e) {
            Logger.error(GroupsRestController.class, e);
        }
        if (groupDetails == null) {
            groupDetails = new GroupDetails();
        }

        String systemGroupIds = null;
        if (entity.getEditorFields().isForceDomainNameChange()) {
            //we must read system group here before DB update and domain name change in requestBean
            if (Constants.getBoolean("templatesUseDomainLocalSystemFolder")) {
                GroupDetails system = groupsDB.getLocalSystemGroup();
                if (system != null && Tools.isNotEmpty(system.getDomainName()) && oldGroupDetails != null && system.getDomainName().equalsIgnoreCase(oldGroupDetails.getDomainName())) {
                    //read systemGroupIds for later use
                    systemGroupIds = groupsDB.getSubgroupsIds(system.getGroupId());
                }
            }
        }

        boolean forceReload = false;
        boolean forceReloadNewDomainName = false;
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (Tools.isNotEmpty(entity.getDomainName()) && Constants.getBoolean("multiDomainEnabled")==true) {
            //nastav do RequestBeanu domenu nastavenu v entite
            if (rb.getDomain().equalsIgnoreCase(entity.getDomainName())==false) {
                rb.setDomain(entity.getDomainName());
                forceReloadNewDomainName = true;
            }
        }

        boolean isScheduledForPublication = false;
        boolean isEdit = entity.getGroupId() > 0;
        if (entity.getEditorFields().isPublishPlan() && isEdit && entity.getEditorFields().getPublishDate() != null
                && entity.getEditorFields().getPublishDate().getTime() > Tools.getNow()) {
            isScheduledForPublication = true;
            try {
                //groupDetails musime naklonovat lebo je z cache a pre futureSchedule ho nemozeme zmenit
                groupDetails = (GroupDetails) groupDetails.clone();
            } catch (CloneNotSupportedException e) {
                Logger.error(GroupsRestController.class, e);
            }
        }

        //aby sa nam do entity dokopirovali chybajuce property
        String[] alwaysCopyProperties = {"showInNavbar", "showInSitemap", "loggedShowInNavbar", "loggedShowInSitemap"};
        NullAwareBeanUtils.copyProperties(entity, groupDetails, Arrays.asList(alwaysCopyProperties), (String[]) null);
        entity.getEditorFields().toGroupDetails(groupDetails);

        if (isScheduledForPublication) {
            // musime spravit kopiu GroupDetails aby sme si neprepisali cachovany/aktualny zaznam
            scheduleFutureUpdate(groupDetails, user);
            return entity;
        }

        boolean saved = groupsDB.save(groupDetails);
        if (!saved) {
            throwError("datatables.error.system.js");
            return entity;
        }
        entity.setGroupId(groupDetails.getGroupId());

        if (RequestBean.getAttribute("forceReloadTree")!=null) setForceReload(true);

        boolean docsRefresh = false;
        boolean refreshGroupsDB = false;
        // ak editujem musim checknut subpriecinky
        if (!isEdit) {
            if (Constants.getBoolean("groupCreateBlankWebpageAfterCreate")) {
                editorFacade.createEmptyWebPage(groupDetails, null, true);
            }
        }

        int groupId = entity.getGroupId();
        if (entity.getEditorFields().isForceLngToSubFolders()) {
            groupsDB.setLngToSubGroups(groupId, entity.getLng());
            refreshGroupsDB = true;
        }

        if (entity.getEditorFields().isPasswordProtectedSubFolders()) {
            groupsDB.setPermissionToSubgroups(groupId, groupDetails.getPasswordProtected());
            refreshGroupsDB = true;
        }

        if (entity.getEditorFields().isForceTemplateToSubgroupsAndPages()) {
            groupsDB.setTemplateToSubgroups(groupId, groupDetails.getTempId());
            groupsDB.setTemplateToSubpages(groupId, groupDetails.getTempId());
            refreshGroupsDB = true;
        }

        if (refreshGroupsDB) {
            groupsDB = GroupsDB.getInstance(true);
        }

        if (entity.getEditorFields().isForceUrlDirNameChange()) {
            WebpagesService.regenerateUrl(groupId, user, getRequest(), false);
        }

        WebpagesService ws = new WebpagesService(groupId, UsersDB.getCurrentUser(getRequest()), getProp(), getRequest());
        if (entity.getEditorFields().isForceMenuTypeSubfolders()) {
            ws.setAttributeToSubgroups(groupId, "menu_type", entity.getMenuType());
            forceReload = true;
        }
        if (entity.getEditorFields().isForceNavbarSubfolders()) {
            ws.setAttributeToSubgroups(groupId, "show_in_navbar", entity.getShowInNavbar());
            forceReload = true;
        }
        if (entity.getEditorFields().isForceSitemapSubfolders()) {
            ws.setAttributeToSubgroups(groupId, "show_in_sitemap", entity.getShowInSitemap());
            forceReload = true;
        }
        if (entity.getEditorFields().isForceLoggedMenuTypeSubfolders()) {
            ws.setAttributeToSubgroups(groupId, "logged_menu_type", entity.getLoggedMenuType());
            forceReload = true;
        }
        if (entity.getEditorFields().isForceLoggedNavbarSubfolders()) {
            ws.setAttributeToSubgroups(groupId, "logged_show_in_navbar", entity.getLoggedShowInNavbar());
            forceReload = true;
        }
        if (entity.getEditorFields().isForceLoggedSitemapSubfolders()) {
            ws.setAttributeToSubgroups(groupId, "logged_show_in_sitemap", entity.getLoggedShowInSitemap());
            forceReload = true;
        }

        if (entity.getEditorFields().isForceInternalToSubgroups()) {
            ws.setAttributeToSubgroups(groupId, "internal", groupDetails.isInternal());
            forceReload = true;
        }

        if (entity.getEditorFields().isForcePriorityRecalculation()) {
            groupsDB.regenerateSortPriority(groupId);
            forceReload = true;
        }

        /*
        TODO: tuto moznost Zmeniť podstránkam spôsob zobrazovania na: v novom forme nemame navrhnutu, da sa cez DT a hromadnu editaciu
        if (entity.getMenuTypeSubdocs() != null && !("noChange".equals(entity.getMenuTypeSubdocs()))) {
            groupsDB.setMenuVisibilityToSubpages(groupId, entity.getMenuTypeSubdocs());
            docsRefresh = true;
        }*/

        if(entity.getEditorFields().isPasswordProtectedSubFolders()) {
            ws.setAttributeToSubgroups(groupId, "password_protected", entity.getPasswordProtected());
            forceReload = true;
        }
        if (entity.getEditorFields().isForceNewPageDocIdTemplateSubFolders()) {
            ws.setAttributeToSubgroups(groupId, "new_page_docid_template", entity.getNewPageDocIdTemplate());
            forceReload = true;
        }

        int parGroupId = entity.getParentGroupId();
        if ((parGroupId < 1 || Constants.getBoolean("multiDomainEnableNested")) && Tools.isNotEmpty(entity.getDomainName()) && entity.getEditorFields().isForceDomainNameChange())
        {
            if(parGroupId < 1) {
                docsRefresh = true;
            }

            String groupIds = groupsDB.getSubgroupsIds(entity.getGroupId());

            Adminlog.add(Adminlog.TYPE_GROUP, "Force domain to subgroups: " + groupIds, entity.getGroupId(), entity.getTempId());

            //updatni groups
            new SimpleQuery().execute("UPDATE groups SET domain_name=? WHERE group_id IN ("+groupIds+")",entity.getDomainName());

            if (systemGroupIds!=null) {
                Adminlog.add(Adminlog.TYPE_GROUP, "Force domain to system subgroups: " + systemGroupIds, -1, -1);

                //updatni groups
                new SimpleQuery().execute("UPDATE groups SET domain_name=? WHERE group_id IN ("+systemGroupIds+")", entity.getDomainName());
            }

            //aktualizuj presmerovania
            if (oldGroupDetails!=null && Tools.isNotEmpty(oldGroupDetails.getDomainName()) && !entity.getDomainName().equals(oldGroupDetails.getDomainName()))
            {
                new SimpleQuery().execute("UPDATE url_redirect SET domain_name=? WHERE domain_name=?",entity.getDomainName(), oldGroupDetails.getDomainName());
                //toto sa vykona pri zmene domeny, treba spravit update aj tooltipov
                new SimpleQuery().execute("UPDATE dictionary SET domain=? WHERE domain=?",entity.getDomainName(),oldGroupDetails.getDomainName());
            }

            if (Constants.getBoolean("enableStaticFilesExternalDir") && Tools.isNotEmpty(Constants.getString("cloudStaticFilesDir")) && oldGroupDetails != null && Tools.isNotEmpty(oldGroupDetails.getDomainName()) && Tools.isNotEmpty(entity.getDomainName()))
            {
                //premenuj adresar s domenovymi subormi (statickymi)
                IwcmFile oldDir = new IwcmFile(FilePathTools.getDomainBaseFolder(oldGroupDetails.getDomainName()));
                if (oldDir.exists()) {
                    IwcmFile newDir = new IwcmFile(FilePathTools.getDomainBaseFolder(entity.getDomainName()));

                    if (!oldDir.getAbsolutePath().equals(newDir.getAbsolutePath()))
                    {
                        Adminlog.add(Adminlog.TYPE_GROUP, "Changed domain, old domain: '"+oldGroupDetails.getDomainName()+"', new domain: '"+entity.getDomainName()+"', files from "+oldDir.getAbsolutePath()+" to "+newDir.getAbsolutePath(), 0, 0);

                        //toto mozno nezbehne, kedze je to v roznych adresaroch, bude potrebne spravit move
                        boolean renamed = oldDir.renameTo(newDir);
                        if (!renamed) {
                            boolean created = newDir.mkdirs();
                            if (!created) {
                                Logger.debug(GroupsRestController.class, "Can't create direcotry "+newDir.getAbsolutePath());
                            }

                            try {
                                FileTools.copyDirectory(oldDir, newDir);
                                FileTools.deleteDirTree(oldDir);
                            } catch (Exception e) {
                                Logger.error(GroupsRestController.class, e);
                            }
                        }
                    }
                }
            }

            //mame zmeny v domene, je najlepsie refreshnut celu GroupsDB
            groupsDB = GroupsDB.getInstance(true);

            if (Constants.isConstantsAliasSearch() && oldGroupDetails != null) {
                //premenovat konf. premenne s domenovym prefixom
                List<ConfDetails> confList = ConfDB.getConfig(oldGroupDetails.getDomainName());
                for (ConfDetails conf : confList) {
                    String name = Tools.replace(conf.getName(), oldGroupDetails.getDomainName(), entity.getDomainName());
                    ConfDB.setName(name, conf.getValue());
                    ConfDB.deleteName(conf.getName());
                }

                //premenovat prekladove texty s domenovym prefixom
                List<TranslationKeyEntity> props = translationKeyRepository.findAllByKeyStartsWith(oldGroupDetails.getDomainName());
                for (TranslationKeyEntity prop : props) {
                    String key = Tools.replace(prop.getKey(), oldGroupDetails.getDomainName(), entity.getDomainName());
                    try {
                        prop.setKey(key);
                        translationKeyRepository.save(prop);
                    } catch (Exception ex) {
                        //mozno uz existuje
                    }
                }
                if (props.size()>0) {
                    Prop.getInstance(true);
                }
            }

            getRequest().getSession().setAttribute("preview.editorDomainName", entity.getDomainName());
            setForceReload(true);
        }

        //toto sa deje len ak nie je zmena domeny, lebo to inak zblble a vytvori duplicitne (vidi este stary rb.getDomainName())
        //ak sa jedna o korenovy adresar a je zapnuty local system group a este neexistuje System adresar, tak ho vytvor
        if (entity.getParentGroupId()<1 && Tools.isNotEmpty(entity.getDomainName()) && Constants.getBoolean("templatesUseDomainLocalSystemFolder")) {
            GroupDetails localSystem = groupsDB.getLocalSystemGroup();
            if (localSystem == null) {
                localSystem = new GroupDetails();
                localSystem.setGroupName("System");
                localSystem.setNavbarName("System");
                localSystem.setParentGroupId(0);
                localSystem.setInternal(true);
                localSystem.setUrlDirName("system");
                localSystem.setDomainName(entity.getDomainName());
                localSystem.setTempId(entity.getTempId());
                localSystem.setSortPriority(1000);
                localSystem.setMenuType(GroupDetails.MENU_TYPE_HIDDEN);
                localSystem.setLoggedMenuType(-1);
                groupsDB.save(localSystem);
                groupsDB = GroupsDB.getInstance(true);

                DocDetails ef = null;
                if (Constants.getBoolean("groupCreateBlankWebpageAfterCreate"))
                {
                    ef = editorFacade.createEmptyWebPage(localSystem, null, false);
                }

                //vytvor podadresare Hlavicky, Paticky, Menu
                Prop prop = getProp();
                String[] keys = {"groupslist.system.header", "groupslist.system.footer", "groupslist.system.menu"};
                int sortPriority = 0;
                for (String key : keys) {
                    String name = prop.getText(key);

                    sortPriority+=10;
                    //vytvor podadresare
                    GroupDetails subgroup = new GroupDetails();
                    subgroup.setGroupName(name);
                    subgroup.setNavbarName(name);
                    subgroup.setParentGroupId(localSystem.getGroupId());
                    subgroup.setInternal(true);
                    subgroup.setUrlDirName(DocTools.removeChars(name, true));
                    subgroup.setDomainName(entity.getDomainName());
                    subgroup.setTempId(entity.getTempId());
                    subgroup.setSortPriority(sortPriority);
                    subgroup.setMenuType(GroupDetails.MENU_TYPE_HIDDEN);
                    subgroup.setLoggedMenuType(-1);
                    //ako hlavnu nastav System priecinok, pretoze hlavicky/paticky stranky maju zvycajne ine meno
                    if (ef != null) subgroup.setDefaultDocId(ef.getDocId());
                    groupsDB.save(subgroup);

                    if (ef != null) {
                        editorFacade.createEmptyWebPage(subgroup, prop.getText(key+".pagetitle"), true);
                    }
                }
                groupsDB = GroupsDB.getInstance(true);
            }
        }

        if (docsRefresh) {
            DocDB.getInstance(true);
            setForceReload(true);
        }

        if (forceReloadNewDomainName) {
            //novo vytvorena domena, potrebujeme vyvolat reload stranky
            //fejkneme to cez atribut forceDomainNameChange, na ktory uz pocuvame v pug subore
            entity.getEditorFields().setForceDomainNameChange(true);
            setForceReload(true);
        }

        if (oldGroupDetails!=null) {
            if (oldGroupDetails.getDefaultDocId()!=entity.getDefaultDocId()) forceReload = true;
        }
        if (forceReload) {
            setForceReload(true);
        }

        return entity;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GroupDetails> target, Identity user, Errors errors, Long id, GroupDetails entity) {

        if (entity.getGroupId()>0 && GroupsDB.isGroupEditable(user, entity.getGroupId())==false) {
            errors.rejectValue("errorField.editorFields.groupId", "403", Prop.getInstance().getText("user.rights.no_folder_rights"));
        }

        if ("remove".equals(target.getAction())) return;

        if (entity.getEditorFields()!=null && entity.getEditorFields().getParentGroupDetails()!=null) {
            if (GroupsDB.isGroupEditable(user, entity.getEditorFields().getParentGroupDetails().getGroupId())==false) {
                errors.rejectValue("errorField.editorFields.parentGroupDetails", "403", Prop.getInstance().getText("user.rights.no_folder_rights"));
            }
        }

        GroupsDB groupsDB = GroupsDB.getInstance();
        int groupId = entity.getGroupId();
        int newParentGroupId = entity.getEditorFields().getParentGroupDetails().getGroupId();

        //Chcek if is parentGroup ID valid
        if(groupId == newParentGroupId) {
            //ParentGroup can't be same group as requesting group
            errors.rejectValue("errorField.editorFields.parentGroupDetails", "403", Prop.getInstance(request).getText("editor.parent_group_error_same_folder"));
        } else {
            int failsafe = 0;
            while(failsafe++<50) {
                GroupDetails tmpParent = groupsDB.getGroup(newParentGroupId);
                if(tmpParent != null) {
                    if(groupId == tmpParent.getGroupId()) {
                        //ParentGroup can't be child group of requesting group
                        errors.rejectValue("errorField.editorFields.parentGroupDetails", "403", Prop.getInstance(request).getText("editor.parent_group_error_child_folder"));
                        break;
                    } else {
                        newParentGroupId = tmpParent.getParentGroupId();
                    }
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public boolean deleteItem(GroupDetails entity, long id) {
        List<String> errors = new ArrayList<>();
        if (!this.canModifyThisGroup(entity, getUser(), "editDir", errors)) {
            throwError(errors);
            return false;
        }
        if (entity==null) return false;

        return GroupsDB.deleteGroup(entity.getGroupId(), getRequest());
    }

    private boolean canModifyThisGroup(GroupDetails groupDetails, Identity user, String editPermName, List<String> errors) {
        if (groupDetails == null)
            return true;

        int groupIdCheck = groupDetails.getGroupId();
        if (groupIdCheck<1) {
            if (user.isDisabledItem("addSubdir")) {
                errors.add(getProp().getText("admin.editor_dir.dontHavePermsForThisDir"));
                return false;
            }
            groupIdCheck = groupDetails.getParentGroupId();
        } else {
            if (user.isDisabledItem(editPermName)) {
                errors.add(getProp().getText("admin.editor_dir.dontHavePermsForThisDir"));
                return false;
            }
        }

        boolean canModify = true;

        if (groupDetails.getGroupId()<1 && groupDetails.getParentGroupId()<1) {
            //je to novy zaznam ukladany do root adresara
            if (Tools.isNotEmpty(user.getEditableGroups())) {
                //ak ma povolene len niektore adresare nemoze pridat nic do root adresara
                errors.add(getProp().getText("admin.editor_dir.dontHavePermsForThisDir", groupDetails.getGroupName()));
                canModify = false;
            }
        }

        if (GroupsDB.isGroupEditable(user, groupIdCheck)==false) {
            errors.add(getProp().getText("admin.editor_dir.dontHavePermsForThisDir"));
            canModify = false;
        }

        if (groupDetails.getGroupId()>0) {
            //ak nastala zmena parent adresara over aj prava na parent adresar
            int parentGroupId = groupDetails.getParentGroupId();
            GroupDetails oldGroup = GroupsDB.getInstance().getGroup(groupDetails.getGroupId());
            if (oldGroup != null && parentGroupId!=oldGroup.getParentGroupId())
            {
                boolean isEditable = GroupsDB.isGroupEditable(user, parentGroupId);
                //ak je vybraty root tak musis mat prazdne povolene adresare
                if (parentGroupId < 1 && Tools.isNotEmpty(user.getEditableGroups())) isEditable = false;
                if (isEditable == false) {
                    errors.add(getProp().getText("admin.editor_dir.dontHavePermsForThisDir"));
                    canModify = false;
                }
            }
        }

        if (canModify==true && isSystemFolderManipulation(groupDetails.getGroupId())) {
            // len administrator s user_id = 1 moze editovat systemove adresare
            if (user.getUserId() != 1) {
                errors.add(getProp().getText("grouptree.this_folder_cant_be_changed", groupDetails.getGroupName()));
                canModify = false;
            }
        }
        return canModify;
    }

    private boolean isSystemFolderManipulation(int groupId) {
        List<String> forbiddenGroups = Arrays.asList("/system");
        String pathToGroup = DB.internationalToEnglish(GroupsDB.getInstance().getGroupNamePath(groupId).toLowerCase());
        if (forbiddenGroups.contains(pathToGroup)) return true;
        int localSystemGroup = GroupsDB.getInstance().getLocalSystemGroupId();
        if (localSystemGroup>0 && groupId == localSystemGroup) return true;

        return false;
    }

    /**
     * Ulozi nastavenie grupy pre scheduled edit
     * @param group
     * @param user
     */
    private void scheduleFutureUpdate(GroupDetails group, Identity user)
    {
        Date date = group.getEditorFields().getPublishDate();
        if (date.before(new Date())) {
            return;
        }

        int userId = -1;
        if(user != null) {
            userId = user.getUserId();
        }

        boolean saved = GroupPublisher.addRecord(group, date, userId);
        if (saved==false) throwError("datatables.error.system.js");
    }

    /*@Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request.getSession());
        if (user.isDisabledItem("addSubdir")) {
            return false;
        }

        return super.checkAccessAllowed(request);
    }*/

    /**
     * Vrati zoznam parent ID pre zadane ID adresara pre ich otvorenie v stromovej strukture
     * Nastavi domenove meno a kartu (System/Kos) ak je potrebne
     * @param groupId
     * @return
     */
    @RequestMapping(path="/parents/{id}")
    public ParentGroupsResult parentGroups(@PathVariable("id") int groupId) {
        ParentGroupsResult result = new ParentGroupsResult();

        if (GroupsDB.isGroupEditable(getUser(), groupId) || GroupsDB.isGroupViewable(getUser(), groupId)) {
            List<GroupDetails> parentGroups = GroupsDB.getInstance().getParentGroups(groupId);
            result.setParentGroups(GroupsDB.getInstance().getGroup(groupId), parentGroups);
        }

        return result;
    }

    @Override
    public GroupDetails processFromEntity(GroupDetails group, ProcessItemAction action) {
        GroupEditorField gef = new GroupEditorField();
        group = gef.fromGroupDetails(group);
        if (gef.getParentGroupDetails() == null) {
            // If we create new directori or update directory under root we need to set
            // empty parentGroupDetails
            GroupDetails rootParentGroupDetails = new GroupDetails();
            rootParentGroupDetails.setParentGroupId(0);
            rootParentGroupDetails.setGroupName("/");
            gef.setParentGroupDetails(rootParentGroupDetails);
        }
        return group;
    }

    @Override
    public boolean processAction(GroupDetails entity, String action) {
        if ("recoverGroup".equals(action) && entity != null) {
            boolean status = editorFacade.recoverGroupFromTrash(entity, getUser());
            addNotify(editorFacade.getNotify());
            //we must clear it to prevent duplicate notifications on multiple groups recovery
            editorFacade.clearNotify();
            setForceReload(true);
            return status;
        }
        return false;
    }
}