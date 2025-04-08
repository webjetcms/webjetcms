package sk.iway.iwcm.components.appuser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.banner.BannerDB;
import sk.iway.iwcm.components.banner.model.BannerGroupBean;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

@WebjetComponent("sk.iway.iwcm.components.appuser.UserApp")
@WebjetAppStore(nameKey = "menu.users", descKey = "components.user.desc", itemKey = "cmp_user", imagePath = "/components/user/editoricon.png", galleryImages = "/components/user/", componentPath = "/components/user/newuser.jsp,/components/user/logon.jsp,/components/user/forget_password.jsp", customHtml = "/apps/user/admin/editor-component.html"
)
@Getter
@Setter
public class UserApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.user.field", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.user.reg_form", value = "/components/user/newuser.jsp"),
                    @DataTableColumnEditorAttr(key = "components.user.logon_form", value = "/components/user/logon.jsp"),
                    @DataTableColumnEditorAttr(key = "components.user.authorize_link", value = "<a href='/components/user/authorize.jsp?userId=!LOGGED_USER_ID!&hash=!AUTHORIZE_HASH!'>/components/user/authorize.jsp?userId=!LOGGED_USER_ID!&hash=!AUTHORIZE_HASH!</a>"),
                    @DataTableColumnEditorAttr(key = "logon.mail.lost_password", value = "/components/user/forget_password.jsp"),
                    @DataTableColumnEditorAttr(key = "components.user.name", value = "!LOGGED_USER_FIRSTNAME!"),
                    @DataTableColumnEditorAttr(key = "reguser.lastname", value = "!LOGGED_USER_LASTNAME!"),
                    @DataTableColumnEditorAttr(key = "components.user.full_name", value = "!LOGGED_USER_NAME!"),
                    @DataTableColumnEditorAttr(key = "components.user.login", value = "!LOGGED_USER_LOGIN!"),
                    @DataTableColumnEditorAttr(key = "components.user.password", value = "!LOGGED_USER_PASSWORD!"),
                    @DataTableColumnEditorAttr(key = "components.user.password2", value = "!LOGGED_USER_PASSWORD2!"),
                    @DataTableColumnEditorAttr(key = "components.user.email", value = "!LOGGED_USER_EMAIL!"),
                    @DataTableColumnEditorAttr(key = "components.user.phone", value = "!LOGGED_USER_PHONE!"),
                    @DataTableColumnEditorAttr(key = "components.user.company", value = "!LOGGED_USER_COMPANY!"),
                    @DataTableColumnEditorAttr(key = "components.user.address", value = "!LOGGED_USER_ADDRESS!"),
                    @DataTableColumnEditorAttr(key = "components.user.city", value = "!LOGGED_USER_CITY!"),
                    @DataTableColumnEditorAttr(key = "components.user.country", value = "!LOGGED_USER_COUNTRY!"),
                    @DataTableColumnEditorAttr(key = "components.user.zip", value = "!LOGGED_USER_ZIP!"),
                    @DataTableColumnEditorAttr(key = "components.user.id", value = "!LOGGED_USER_ID!"),
                    @DataTableColumnEditorAttr(key = "components.user.use_custom_field_A", value = "!LOGGED_USER_FIELDA!"),
                    @DataTableColumnEditorAttr(key = "components.user.use_custom_field_B", value = "!LOGGED_USER_FIELDB!"),
                    @DataTableColumnEditorAttr(key = "components.user.use_custom_field_C", value = "!LOGGED_USER_FIELDC!"),
                    @DataTableColumnEditorAttr(key = "components.user.use_custom_field_D", value = "!LOGGED_USER_FIELDD!"),
                    @DataTableColumnEditorAttr(key = "components.user.use_custom_field_E", value = "!LOGGED_USER_FIELDE!"),
                    @DataTableColumnEditorAttr(key = "user.admin.editUserGroups", value = "!LOGGED_USER_GROUPS!")
            })
    })
    private String field;

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.user.group_id")
    private List<Integer> userGroups;

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.user.show_fields", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.user.login", value = "login"),
                    @DataTableColumnEditorAttr(key = "components.user.password", value = "password"),
                    @DataTableColumnEditorAttr(key = "components.user.newuser.password2", value = "password2"),
                    @DataTableColumnEditorAttr(key = "user.title", value = "title"),
                    @DataTableColumnEditorAttr(key = "user.firstName", value = "firstName"),
                    @DataTableColumnEditorAttr(key = "user.lastName", value = "lastName"),
                    @DataTableColumnEditorAttr(key = "components.user.company", value = "company"),
                    @DataTableColumnEditorAttr(key = "components.user.email", value = "email"),
                    @DataTableColumnEditorAttr(key = "components.user.newuser.dateOfBirth", value = "dateOfBirth"),
                    @DataTableColumnEditorAttr(key = "components.user.newuser.signature", value = "signature"),
                    @DataTableColumnEditorAttr(key = "user.address", value = "adress"),
                    @DataTableColumnEditorAttr(key = "components.user.city", value = "city"),
                    @DataTableColumnEditorAttr(key = "components.user.zip", value = "zip"),
                    @DataTableColumnEditorAttr(key = "components.user.country", value = "country"),
                    @DataTableColumnEditorAttr(key = "components.user.phone", value = "phone")
            }) })
    private List<String> show = new ArrayList<>(
            Arrays.asList("login", "password", "password2", "firstName", "lastName", "email"));
            
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.user.required_fields", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.user.login", value = "login"),
                    @DataTableColumnEditorAttr(key = "components.user.password", value = "password"),
                    @DataTableColumnEditorAttr(key = "components.user.newuser.password2", value = "password2"),
                    @DataTableColumnEditorAttr(key = "user.title", value = "title"),
                    @DataTableColumnEditorAttr(key = "user.firstName", value = "firstName"),
                    @DataTableColumnEditorAttr(key = "user.lastName", value = "lastName"),
                    @DataTableColumnEditorAttr(key = "components.user.company", value = "company"),
                    @DataTableColumnEditorAttr(key = "components.user.email", value = "email"),
                    @DataTableColumnEditorAttr(key = "components.user.newuser.dateOfBirth", value = "dateOfBirth"),
                    @DataTableColumnEditorAttr(key = "components.user.newuser.signature", value = "signature"),
                    @DataTableColumnEditorAttr(key = "user.address", value = "adress"),
                    @DataTableColumnEditorAttr(key = "components.user.city", value = "city"),
                    @DataTableColumnEditorAttr(key = "components.user.zip", value = "zip"),
                    @DataTableColumnEditorAttr(key = "components.user.country", value = "country"),
                    @DataTableColumnEditorAttr(key = "components.user.phone", value = "phone")
            }) })
    private List<String> required = new ArrayList<>(Arrays.asList("login", "password", "password2", "firstName",
            "lastName", "email", "sexMale", "address", "city", "zip", "country"));

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.user.editableGroupIds")
    private List<Integer> groupIdsEditable;
    
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.email_must_be_unique")
    private Boolean emailUnique;
    
    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "basic", title = "components.newuser.success_docid", className = "dt-tree-page")
    private Integer successDocId;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.user.infoemail")
    private String infoemail;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.require_email_verification")
    private Boolean requireEmailVerification;

    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "basic", title = "components.newuser.not_authorized_email_docid", className = "dt-tree-page")
    private Integer notAuthorizedEmailDocId;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.login_new_user")
    private Boolean loginNewUser;    

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.send_using_ajax")
    private Boolean useAjax;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.use_custom_fields")
    private Boolean useCustomFields;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.use_custom_field_A")
    private Boolean useCustomFieldA;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.user.custom_field_A_label")
    private String fieldALabel;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.use_custom_field_B")
    private Boolean useCustomFieldB;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.user.custom_field_B_label")
    private String fieldBLabel;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.use_custom_field_C")
    private Boolean useCustomFieldC;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.user.custom_field_C_label")
    private String fieldCLabel;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.use_custom_field_D")
    private Boolean useCustomFieldD;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.user.custom_field_D_label")
    private String fieldDLabel;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic", title = "components.user.use_custom_field_E")
    private Boolean useCustomFieldE;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.user.custom_field_E_label")
    private String fieldELabel;

    
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.user.group_id", 
    editor = {
        @DataTableColumnEditor(
            message = "components.user.loguser_groups_ids"
        )
    })
    private List<Integer> regToUserGroups;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<OptionDto> userGroupOptions = new ArrayList<>();

        UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();

        List<UserGroupDetails> userGroups = userGroupsDB.getUserGroups();
        for (UserGroupDetails userGroup : userGroups) {
            String userGroupId = String.valueOf(userGroup.getUserGroupId());
            String userGroupName = userGroup.getUserGroupName();
            userGroupOptions.add(new OptionDto(userGroupName, userGroupId, null));
        }

        options.put("userGroups", userGroupOptions);
        options.put("groupIdsEditable", userGroupOptions);
        options.put("regToUserGroups" , userGroupOptions);

        return options;
    }

}
