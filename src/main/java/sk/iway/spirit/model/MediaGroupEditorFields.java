package sk.iway.spirit.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class MediaGroupEditorFields {

    //used for remapping availableGroups
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="admin.temp.edit.showForDir",
    className = "dt-tree-group-array")
    private List<GroupDetails> availableGroups;

    public MediaGroupEditorFields() {}

    //inicialize MediaGroupEditorFileds.availableGroups(List<GroupDetails>) using MediaGroup.availableGroups(String)
    public void fromMediaGroupBean(MediaGroupBean mediaGroupOriginal) {

        //initialize empty list
        availableGroups = new ArrayList<>();

        //first get MediaGroups.availableGroups which is String of groups id separated by ","
        String availableGroupsString = mediaGroupOriginal.getAvailableGroups();

        //availableGroupsString is NOT empty
        if(availableGroupsString != null && !availableGroupsString.isEmpty()) {
            GroupsDB groupsDB = GroupsDB.getInstance();

            //parse string by "," which make array of group id
            String availableGroupsIdArray[] = availableGroupsString.split(",");

            for(int i = 0; i < availableGroupsIdArray.length; i++) {
                //get GrouspDetails by id and push into MediaGroupEditorFileds.availableGroups
                GroupDetails tmp = groupsDB.getGroup(Integer.parseInt(availableGroupsIdArray[i]));
                if (tmp != null) availableGroups.add(tmp);
            }
            //set this EditorFields to current MediaGroupBean (mediaGroupOriginal)
            mediaGroupOriginal.setEditorFields(this);
        } else {
            //availableGroupsString is empty, MediaGroupEditorFileds.availableGroups will by empty list
            //set this EditorFields to current MediaGroupBean (mediaGroupOriginal)
            mediaGroupOriginal.setEditorFields(this);
        }
    }

    public String toMediaGroupBean(MediaGroupBean mediaGroupOriginal) {
        String newGroupIds = "";
        //loop MediaGroupEditorFileds.availableGroups, get every group id and join them using "," as separator
        for(int i = 0; i < this.availableGroups.size(); i++) {
            if (this.availableGroups.get(i)==null) continue;
            if (Tools.isNotEmpty(newGroupIds)) newGroupIds += ",";
            newGroupIds += this.availableGroups.get(i).getGroupId();
        }
        //return created string od joined group id's
        return newGroupIds;
    }
}
