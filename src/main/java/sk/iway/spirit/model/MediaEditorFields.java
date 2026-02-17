package sk.iway.spirit.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.media.MediaGroupRepository;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class MediaEditorFields extends BaseEditorFields {

    //used for remapping groups
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, renderFormat = "dt-format-select", title = "editor.media.group", sortAfter = "mediaTitleSk", tab = "basic",
    editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "-1")
            }
        )
    })
    private Integer[] groups;

    @NotBlank
    private DocDetails docDetails;

    public MediaEditorFields() {
        //default konstruktor
    }

    //initialize MediaEditorFields.groups
    public void fromMedia(Media media) {
        //get groups from actual media
        List<MediaGroupBean> tmp = media.getGroups();
        if (tmp != null) {
            groups = new Integer[tmp.size()];

            //loop getted Media.groups and push their id's to MediaEditorFields.groups
            for(int i = 0; i < tmp.size(); i++) {
                groups[i] = tmp.get(i).getMediaGroupId();
            }
        }

        if(media.getMediaFkTableName().equals("documents") && media.getMediaFkId() != null && media.getMediaFkId().intValue() > 0) {
            docDetails = WebpagesService.getBasicDoc(media.getMediaFkId().intValue());
        }
    }

    //set Media.grousp using MediaEditorFields.groups
    public void toMedia(Media media, MediaGroupRepository repo) {

        Integer groupLength = media.getEditorFields().getGroups().length;
        List<MediaGroupBean> newGroups = new ArrayList<MediaGroupBean>();

        //loop MediaEditorFields.groups(array of id's)
        //use this id's to get MediaGroupBean and push them into prepared List newGroups
        if(groupLength > 0) {
            Integer[] tmp = media.getEditorFields().getGroups();

            if(tmp[0] != -1) {
                for(int i = 0; i < tmp.length; i++) {
                    newGroups.add(repo.getById(Long.valueOf(tmp[i])));
                }
            }
        }
        //set Media.group using List newGroups
        media.setGroups(newGroups);
    }
}
