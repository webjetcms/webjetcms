package sk.iway.iwcm.editor.rest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class GroupSchedulerEditorFields extends BaseEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.approvedBy", sortAfter = "userFullName")
    private String approvedByName;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.disapprovedBy", sortAfter = "editorFields.approvedByName")
    private String disapprovedByName;

    public void fromGroupScheduler(GroupSchedulerDto originalEntity, Long actual, Prop prop) {

        if(Tools.isEmpty(originalEntity.getAwaitingApprove())) {

            if(originalEntity.getApprovedBy() != null) {
                // change was APPROVED
                this.approvedByName = getApproverName(originalEntity.getApprovedBy());
            } else if(originalEntity.getDisapprovedBy() != null) {
                // change REJECTED
                this.disapprovedByName = getDisapproverName(originalEntity.getDisapprovedBy());

                addRowClass("is-disapproved");
            } else {
                // This change was saved without approve -> dont need approve
                this.approvedByName = prop.getText("editor.history.not_to_approve");
                this.disapprovedByName = "";
            }

		} else {
            //This change require approval
            // NOT APPROVED YET
			this.approvedByName = prop.getText("editor.history.not_approved");
		}

        if(actual != null && originalEntity.getId().equals(actual)) {
            addRowClass("is-actual");
        }

        originalEntity.setEditorFields(this);
    }

    public void toGroupScheduler(GroupSchedulerDto originalEntity) {
        //Nothing to set to entity, only for display
    }

    public static String getApproverName(Integer approvedBy) {
        if(approvedBy != null) {
            UserDetails approver = UsersDB.getUser( approvedBy );
            if(approver != null) return approver.getFullName();
        }
        return "";
    }

    public static String getDisapproverName(Integer disapprovedBy) {
        if(disapprovedBy != null) {
            UserDetails approver = UsersDB.getUser( disapprovedBy );
            if(approver != null) return approver.getFullName();
        }
        return "";
    }

}
