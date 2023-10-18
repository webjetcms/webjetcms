package sk.iway.iwcm.components.export;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class ExportDatEditorFields implements Serializable {

    public ExportDatEditorFields(){}

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="components.export.groups", sortAfter = "numberItems", filter = false, className = "dt-tree-group-array", tab="filter", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup") })
    })
    private List<GroupDetails> groups;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "[[#{editor.perex.group}]]",
        sortAfter = "publishType",
        tab="filter",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
	private String[] perexGroupsIds;

    public void fromExportDatBean(ExportDatBean exportDatBeanOriginal) {

        // Get group ids from ExportDatBean, get entities using this ids and push them into editorFields.groups
        groups = new ArrayList<>();
        int groupsIds[] = Tools.getTokensInt(exportDatBeanOriginal.getGroupIds(), ",");
        if(groupsIds.length > 0) {
            GroupsDB groupsDB = GroupsDB.getInstance();

            for(int groupId : groupsIds) {
                GroupDetails tmp = groupsDB.getGroup(groupId);
                if (tmp != null) groups.add(tmp);
            }
        }

        // Get perex group ids as String, parse String and ids set into perexGroupsIds array
        perexGroupsIds = Tools.getTokens(exportDatBeanOriginal.getPerexGroup(), ",");
    }

    public void toExportDatBean(ExportDatBean exportDatBeanOriginal) {

        // From list selectedGroups build String of this GroupDetails ids (separated using comma)
        // Next set this builded string as new exportDatBeanOriginal.groupsIds value
        List<GroupDetails> selectedGroups = exportDatBeanOriginal.getEditorFields().getGroups();
        if (selectedGroups != null) {
            String groupsIdsString = "";
            for(int i = 0; i < selectedGroups.size(); i++) {

                if(groupsIdsString.equals("")) {
                    groupsIdsString = "" + selectedGroups.get(i).getGroupId();
                } else {
                    groupsIdsString += "," + selectedGroups.get(i).getGroupId();
                }
            }
            //Set new string of selected editabled groups
            exportDatBeanOriginal.setGroupIds(groupsIdsString);
        }

        // From array perexGroupIds build String perexGroupString (perex group ids separated using comma)
        // Next set his builded string as new exportDatBeanOriginal.perexGroup value
        String perexGroupString = "";
        for(String perexGroupId : exportDatBeanOriginal.getEditorFields().getPerexGroupsIds()) {

            if(perexGroupString.equals("")) {
                perexGroupString = "" + perexGroupId;
            } else {
                perexGroupString += "," + perexGroupId;
            }
        }
        exportDatBeanOriginal.setPerexGroup(perexGroupString);
    }
}
