package sk.iway.iwcm.components.users.userdetail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.struts.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.components.users.AuthorizeUserService;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyButton;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.SpecSearch;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.PermissionGroupDB;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/users")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('users.edit_admins|users.edit_public_users')")
public class UserDetailsController extends DatatableRestControllerV2<UserDetailsEntity, Long> {

    private final UserDetailsService userDetailsService;
    private final UserDetailsRepository userDetailsRepository;
    private final GroupsApproveRepository groupsApproveRepository;

    private static final String PERM_EDIT_ADMINS = "users.edit_admins";
    private static final String PERM_EDIT_PUBLIC_USERS = "users.edit_public_users";

    @Autowired
    public UserDetailsController(UserDetailsRepository userDetailsRepository, UserDetailsService userDetailsService, GroupsApproveRepository groupsApproveRepository) {
        super(userDetailsRepository);
        this.userDetailsRepository = userDetailsRepository;
        this.userDetailsService = userDetailsService;
        this.groupsApproveRepository = groupsApproveRepository;
    }

    @Override
    public Page<UserDetailsEntity> getAllItems(Pageable pageable) {

        DatatablePageImpl<UserDetailsEntity> page = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new UserDetailsEntity(), pageable));

        Prop prop = Prop.getInstance(Constants.getServletContext(), getRequest());
        Modules modules = Modules.getInstance();
        List<ModuleInfo> moduleItems = modules.getUserEditItems(prop);

        page.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);
        page.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
        page.addOptions("editorFields.enabledItems", moduleItems, "itemKey", "itemKey", false);
        page.addOptions("editorFields.permGroups", (new PermissionGroupDB()).getAll(), "title", "id", false);

        return page;
    }

    @Override
    public UserDetailsEntity getOneItem(long id) {
        UserDetailsEntity one = super.getOneItem(id);
        if (id < 1) {
            //novy zaznam, nastav defaultne hodnoty
            //DTED 2.0.5 ma bug kedy padne, ak takyto option nema ziadnu hodnotu, museli sme nastavit
            one.setSexMale(true);
            one.setAuthorized(Boolean.TRUE);
        } else {
            if (UserDetailsService.isUsersSplitByDomain()) {
                if (one.getDomainId()!=CloudToolsForCore.getDomainId()) return null;
            }
        }
        return one;
    }

    @Override
	public void beforeSave(UserDetailsEntity entity) {

        Identity user = UsersDB.getCurrentUser(getRequest());
        Boolean isCurrentUserAdmin = user.isEnabledItem(PERM_EDIT_ADMINS);

        //If the user does not have admin permission but trying set admin permission throw error
        if(!isCurrentUserAdmin.booleanValue() && entity.getAdmin().booleanValue()) {

            throwError("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu");
        }

        //Chceck if rating rank is set like null and set to 0
        if(entity.getRatingRank() == null) {
            entity.setRatingRank(0);
        }

        //Chceck if forum rank is set like null and set to 0
        if(entity.getForumRank() == null) {
            entity.setForumRank(0);
        }

        //Chceck if parent id is set like null and set to 0
        if(entity.getParentId() == null) {
            entity.setParentId(0);
        }

        if (entity.getRegDate() == null) entity.setRegDate(new Date(Tools.getNow()));

        //Save into session last saved user group's
        userDetailsService.setBeforeSaveUserGroups(entity);
    }

    @Override
    public void afterSave(UserDetailsEntity entity, UserDetailsEntity saved) {

        if ("*".equals(entity.getApiKey())) {
            //vygeneruj access token

            String apiKey = PasswordSecurity.generatePassword(32, true, true, true, true, false);
            entity.setApiKey(apiKey);

            String header = saved.getLogin()+":"+apiKey;
            header = Constants.getString("logonTokenHeaderName") + ":" + Base64.encodeBase64String(header.getBytes());
            addNotify(new NotifyBean(getProp().getText("components.user.apiKey.notifyTitle"), getProp().getText("components.user.apiKey.notifyText", ResponseUtils.filter(apiKey), header), NotifyType.SUCCESS));
        }

        boolean saveSuccess = userDetailsService.afterSave(entity, saved);
        if (saveSuccess==false) throwError("editor.ajax.save.error");

        //Save was successfull, send email's about adding to user groups (if user was added into new user group)
        userDetailsService.sendUserGroupsEmails(saved, entity, getUser(), getRequest());

        //update current user if editing self
        if (userDetailsService.updateSelf(saved, getUser(), getRequest())) {
            NotifyBean notify = new NotifyBean(getProp().getText("user.profile.save.title.js"), getProp().getText("user.profile.save.notifyText.js"), NotifyType.INFO, 10000);
            notify.addButton(new NotifyButton(getProp().getText("menu.logout"), "btn btn-primary", "ti ti-logout", "window.location.href=$('.js-logout-toggler').attr('href')"));
            addNotify(notify);
        }
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<UserDetailsEntity> root, CriteriaBuilder builder) {

        Identity user = UsersDB.getCurrentUser(getRequest());

        //Check user permissions and return for this user editable entities
        Expression<Boolean> colAdmin = root.get("admin");
        if(!user.isEnabledItem(PERM_EDIT_ADMINS)  && user.isEnabledItem(PERM_EDIT_PUBLIC_USERS)) {
            predicates.add(builder.isFalse(colAdmin));
        } else if(user.isEnabledItem(PERM_EDIT_ADMINS)  && !user.isEnabledItem(PERM_EDIT_PUBLIC_USERS)) {
            predicates.add(builder.isTrue(colAdmin));
        } else if(!user.isEnabledItem(PERM_EDIT_ADMINS)  && !user.isEnabledItem(PERM_EDIT_PUBLIC_USERS)) {
            //If user doesnt have admin and public perm in same time
            predicates.add(builder.isFalse(colAdmin));
            predicates.add(builder.isTrue(colAdmin));
        }

        SpecSearch<UserDetailsEntity> specSearch = new SpecSearch<>();
        String permissions = params.get("searchEditorFields.permisions");
        if (permissions != null) {
            specSearch.addSpecSearchPasswordProtected(permissions, "userGroupsIds", predicates, root, builder);
        }
        String emails = params.get("searchEditorFields.emails");
        if (emails != null) {
            specSearch.addSpecSearchPasswordProtected(emails, "userGroupsIds", predicates, root, builder);
        }
        int userGroupId = Tools.getIntValue(params.get("userGroupId"), -1);
        if (userGroupId > 0) {
            specSearch.addSpecSearchPasswordProtected(userGroupId, "userGroupsIds", predicates, root, builder);
        }

        int permGroup = Tools.getIntValue(params.get("searchEditorFields.permGroups"), -1);
        if(permGroup > 0) {
            specSearch.addSpecSearchIdInForeignTableInteger(permGroup, "users_in_perm_groups", "user_id", "perm_group_id", "id", predicates, root, builder);
        }

        if (UserDetailsService.isUsersSplitByDomain()) {
            predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    @SuppressWarnings("all")
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, UserDetailsEntity> target, Identity user, Errors errors, Long id, UserDetailsEntity entity) {

        if ("remove".equals(target.getAction())) return;

		if ("random".equals(entity.getPassword()) || "*".equals(entity.getPassword()))
		{
			//vygeneruj heslo
			entity.setPassword(Password.generateStringHash(5)+Password.generatePassword(5));
		}

        Prop prop = Prop.getInstance(request);

        //Import setting
        if(isImporting()) {
            if(Tools.isEmpty(entity.getPassword())) entity.setPassword(UserTools.PASS_UNCHANGED);

            if (entity.getEditorFields()==null) {
                UserDetailsEditorFields udef = new UserDetailsEditorFields();
                udef.fromUserDetailsEntity(entity, false, getRequest(), groupsApproveRepository);
            }

            //Generate default login
            if(entity.getEditorFields()!=null && Tools.isEmpty(entity.getEditorFields().getLogin())) {
                String autoLogin = Tools.isEmpty(entity.getUserGroupsIds()) == true ? "" : entity.getUserGroupsIds() + "-";
                autoLogin += entity.get__rowNum__() + "-" + Password.generatePassword(4);
                if( Tools.isNotEmpty(entity.getLastName()) ) autoLogin = DocTools.removeCharsDir( entity.getLastName() ).toLowerCase() + "-" + autoLogin;
                entity.getEditorFields().setLogin(autoLogin);
            }

            //By default not admin
            if(entity.getAdmin() == null) entity.setAdmin(false);
        }

        boolean allowWeakPassword = false;
        if (entity.getEditorFields()!=null && Boolean.TRUE.equals(entity.getEditorFields().getAllowWeakPassword())) allowWeakPassword = true;
        boolean admin = Boolean.TRUE.equals(entity.getAdmin());
        userDetailsService.validatePassword(entity, allowWeakPassword, admin, prop, errors);

        //not empty aby pri prazdnej hlasilo v editore, ze to je povinne pole
        if (Tools.isNotEmpty(entity.getEmail()) && Tools.isEmail(entity.getEmail())==false) {
            errors.rejectValue("errorField.email", null, prop.getText("javax.validation.constraints.Email.message"));
        }

        //validate login
        if (entity.getEditorFields()==null || Tools.isEmpty(entity.getEditorFields().getLogin())) {
            errors.rejectValue("errorField.editorFields.login", null, prop.getText("javax.validation.constraints.NotBlank.message"));
        }
    }

    @Override
    public UserDetailsEntity processFromEntity(UserDetailsEntity entity, ProcessItemAction action) {
        boolean loadSubQueries = false;
        if (ProcessItemAction.GETONE.equals(action)) {
            loadSubQueries = true;
            if (entity == null) entity = new UserDetailsEntity();
        }

        if(entity != null && entity.getEditorFields() == null) {
            UserDetailsEditorFields udef = new UserDetailsEditorFields();
            udef.fromUserDetailsEntity(entity, loadSubQueries, getRequest(), groupsApproveRepository);
        }

        //pri exporte nastav prazdne heslo
        if (isExporting() && entity!=null) entity.setPassword("");

        if (entity != null && loadSubQueries && entity.getId()!=null && entity.getId().longValue()>0) {
            String apiKey = userDetailsRepository.getApiKeyByUserId(entity.getId());
            if (Tools.isNotEmpty(apiKey)) {
                entity.setApiKey(UserTools.PASS_UNCHANGED);
            }
        }

        return entity;
    }

    @Override
    public UserDetailsEntity processToEntity(UserDetailsEntity entity, ProcessItemAction action) {
        if(entity != null) {
            //Call toUserDetailsEntity to set new entity values from EditorFields
            UserDetailsEditorFields udef = new UserDetailsEditorFields();
            udef.toUserDetailsEntity(entity, getRequest());
        }
        return entity;
    }

    //Return list od user ids that belongs into selected groups
    public static List<Integer> getUserIdsByUserGroupsIds(UserDetailsRepository repo, List<Integer> groupIds) {
        List<Integer> userIds = new ArrayList<>();
        for(Integer groupId : groupIds) {
            List<Integer> tmp = repo.getUserIdsByUserGroupsIds(
                "" + groupId,
                "%," + groupId,
                groupId + ",%",
                "%," + groupId + ",%");

            userIds.addAll(tmp);
        }

        return userIds;
    }

    //Return list od user emails that belongs into selected groups
    public static List<String> getUserEmailsByUserGroupsIds(UserDetailsRepository repo, List<Integer> groupIds) {
        List<String> userEmails = new ArrayList<>();
        for(Integer groupId : groupIds) {
            List<String> tmp = repo.getUserEmailsByUserGroupsIds(
                "" + groupId,
                "%," + groupId,
                groupId + ",%",
                "%," + groupId + ",%");

                userEmails.addAll(tmp);
        }

        return userEmails;
    }

    @Override
    public boolean processAction(UserDetailsEntity entity, String action) {
        //Authorize user WITHOUT generation of new password
        if("authUserNoGen".equals(action))
            auth(entity.getId(), false);

        //Authorize user WITH generation of new password
        if("authUserWithGen".equals(action))
            auth(entity.getId(), true);

        return true;
    }

    /**
     * Call user authorization and add notification about auth status
     * @param userId
     * @param generatePass
     */
    public void auth(Long userId, boolean generatePass) {
        String title = getProp().getText("components.users.auth_title");
        //The ID must be > 0
        if(userId == null || userId < 0) {
            addNotify(new NotifyBean(title, getProp().getText("components.users.auth_failed.id_invalid"), NotifyType.ERROR));
            return;
        }

        UserDetailsEntity userToApprove = userDetailsRepository.getById(userId);
        boolean authorization = AuthorizeUserService.authUser(userToApprove, getUser(), generatePass, getRequest());

        //Show notification about auth status
        if(authorization) {
            setForceReload(true);
            addNotify(new NotifyBean(title, getProp().getText("components.users.auth_success", userToApprove.getFullName(), userToApprove.getLogin()), NotifyType.SUCCESS, 15000));
        } else
            addNotify(new NotifyBean(title, getProp().getText("components.users.auth_failed", userToApprove.getFullName(), userToApprove.getLogin()), NotifyType.ERROR));
    }

    @Override
    public boolean beforeDelete(UserDetailsEntity entity) {
        //Check that user is not trying delete user account, that is actually logged
        if(entity.getId().intValue() == getUser().getUserId()) {
            throwError("user.self_delete_error");
            return false;
        }

        return true;
    }

    @Override
    public void beforeDuplicate(UserDetailsEntity entity) {
        //Force random password generation -> null value can cause problems
        if(entity.getPassword() == null || entity.getPassword().equals(UserTools.PASS_UNCHANGED)) {
            entity.setPassword("random");
            super.beforeDuplicate(entity);
        }
    }
}