package sk.iway.iwcm.system.elfinder;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class FolderPropertiesEditorFields {
    
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "editor.directory_name",
        tab = "basic",
        editor = {
            @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") } )
        }
    )
    private String dirName;

    private List<UserGroupDetails> userGroupsList;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "user.permissions.label", tab = "basic", sortAfter = "dirUrl", hidden = true, editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.permissions"),
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] permisions;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "usage",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/file-usage?filePath={dirUrl}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.system.elfinder.FileUsageDTO"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "fbrowse.usage"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<DocDetails> docDetailsList;

    public void prepareFolderProperties(FolderPropertiesEntity originalEntity, HttpServletRequest request) {
        String dir = request.getParameter("dir");

        //
        if( Tools.isEmpty(dir) == true ) return;

        UserDetails user = UsersDB.getCurrentUser(request);
        boolean canUpload = user.isFolderWritable("/" + dir);

        //
        if(canUpload == false) return;

        IwcmFile f = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(dir));
        this.dirName = f.getName();

        UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
        this.userGroupsList = userGroupsDB.getUserGroups();


        //Take passwordProtected string, split on Individual ids, convert to Integer
        int[] passwordProtected =  Tools.getTokensInt(originalEntity.getPasswordProtected(), ",");
        List<Integer[]> splitPermsEmails = UserDetailsService.splitGroupsToPermsAndEmails(passwordProtected);
        permisions = splitPermsEmails.get(0);

        originalEntity.setEditorFields(this);
    }

    public void toFolderProperties(FolderPropertiesEntity entity) {
        entity.setPasswordProtected( UserDetailsService.getUserGroupIds(this.getPermisions(), null) );
    }
}
