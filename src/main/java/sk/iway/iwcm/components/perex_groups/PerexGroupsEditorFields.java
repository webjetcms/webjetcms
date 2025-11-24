package sk.iway.iwcm.components.perex_groups;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class PerexGroupsEditorFields extends BaseEditorFields {

    public PerexGroupsEditorFields(){}

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="admin.temp.edit.showForDir", className = "dt-tree-group-array")
    private List<GroupDetails> availableGroups;

    public void fromPerexGroupsEntity(PerexGroupsEntity perexGroupOriginal) {
        availableGroups = new ArrayList<>();
        String availableGroupsString = perexGroupOriginal.getAvailableGroups();
        if(availableGroupsString != null && !availableGroupsString.isEmpty()) {
            GroupsDB groupsDB = GroupsDB.getInstance();
            int[] availableGroupsIdArray = Tools.getTokensInt(availableGroupsString, ",+.");

            for(int i = 0; i < availableGroupsIdArray.length; i++) {
                GroupDetails tmp = groupsDB.getGroup(availableGroupsIdArray[i]);
                if (tmp != null) availableGroups.add(tmp);
            }
            perexGroupOriginal.setEditorFields(this);
        } else {
            perexGroupOriginal.setEditorFields(this);
        }
    }

    public void toPerexGroupsEntity(PerexGroupsEntity perexGroupOriginal) {
        String newGroupIds = "";
        //loop MediaGroupEditorFileds.availableGroups, get every group id and join them using "," as separator
        for(int i = 0; i < this.availableGroups.size(); i++) {
            if (this.availableGroups.get(i)==null) continue;

            //prevent duplicates
            int availableGroupId = this.availableGroups.get(i).getGroupId();
            if ((","+newGroupIds+",").contains(","+availableGroupId+",")) continue;

            if (Tools.isNotEmpty(newGroupIds)) newGroupIds += ",";
            newGroupIds += availableGroupId;
        }
        perexGroupOriginal.setAvailableGroups(newGroupIds);
    }
}
