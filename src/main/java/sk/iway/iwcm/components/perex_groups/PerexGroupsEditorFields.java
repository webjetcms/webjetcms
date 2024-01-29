package sk.iway.iwcm.components.perex_groups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class PerexGroupsEditorFields implements Serializable{

    public PerexGroupsEditorFields(){}

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="admin.temp.edit.showForDir", className = "dt-tree-group-array")
    private List<GroupDetails> availableGroups;

    public void fromPerexGroupsEntity(PerexGroupsEntity perexGroupOriginal) {
        availableGroups = new ArrayList<>();
        String availableGroupsString = perexGroupOriginal.getAvailableGroups();
        if(availableGroupsString != null && !availableGroupsString.isEmpty()) {
            GroupsDB groupsDB = GroupsDB.getInstance();
            String availableGroupsIdArray[] = availableGroupsString.split(",");

            for(int i = 0; i < availableGroupsIdArray.length; i++) {
                GroupDetails tmp = groupsDB.getGroup(Integer.parseInt(availableGroupsIdArray[i]));
                if (tmp != null) availableGroups.add(tmp);
            }
            perexGroupOriginal.setEditorFields(this);
        } else {
            perexGroupOriginal.setEditorFields(this);
        }
    }

    public void toPerexGroupsEntity(PerexGroupsEntity perexGroupOriginal) {
        List<GroupDetails> groups = perexGroupOriginal.getEditorFields().getAvailableGroups();
        String groupsIds = "";
        for(int i = 0; i < groups.size(); i++) {
            if (groups.get(i)==null) return;
            if(perexGroupOriginal.getAvailableGroups() == null || perexGroupOriginal.getAvailableGroups().equals("")) {
                perexGroupOriginal.setAvailableGroups("" + groups.get(i).getGroupId());
            } else {
                groupsIds = perexGroupOriginal.getAvailableGroups();
                perexGroupOriginal.setAvailableGroups(groupsIds + "," + groups.get(i).getGroupId());
            }
        }
    }
}
