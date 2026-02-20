package sk.iway.iwcm.components.forum.jpa;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class DocForumEditorFields extends BaseEditorFields {

    //Column for stutus icons
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "webpages.icons.title",
        hiddenEditor = true, hidden = false, visible = true, sortAfter = "id", className = "allow-html", orderable = false
    )
    private String statusIcons;

    public void fromDocForum(DocForumEntity entity, HttpServletRequest request, Prop prop) {
        StringBuilder iconsHtml = new StringBuilder();

        String link = "/showdoc.do?docid="+entity.getDocId();
        if (entity.getDocDetails()!=null) link = entity.getDocDetails().getVirtualPath();

        //If entity is deleted or non active (marked as red)
        if(entity.getDeleted()) addRowClass("is-not-public");
        else if(!entity.getConfirmed()) addRowClass("is-not-public");

        //If it's message board, add param so link wil open it
        if(entity.getId()!=null && entity.getParentId()!=null && entity.getParentId().intValue()>0) link = Tools.addParameterToUrl(link, "pId", String.valueOf(entity.getParentId()));

        //Icon to open page
        iconsHtml.append("<a href=\"" + link + "\" target=\"_blank\" class=\"preview-page-link\" title=\"" + prop.getText("history.showPage") + "\"><i class=\"ti ti-eye\"></i></a> ");

        //Add icon about confirm status
        if(entity.getConfirmed()) {
            iconsHtml.append("<i class=\"ti ti-circle-check\" style=\"color: #00be9f;width: 1.25em;\"></i>");
        } else {
            iconsHtml.append("<i class=\"ti ti-circle-x\" style=\"color: #ff4b58;width: 1.25em;\"></i>");
        }

        //Add icon about active status
        if(!entity.getActive()) 
            //active = false in document_forum table, this one forum is locked
            iconsHtml.append("<i class=\"ti ti-lock\" style=\"color: #000000;width: 1.25em;\"></i>");
        else if(entity.getForumGroupEntity() != null)
                if(!entity.getForumGroupEntity().getActive())
                    //active = false in forum tabel, mean all forum's under specific DocId are locked
                    //!! BEWARE, is whole forum group is locked (active = false), it doesn't necesary mean that forum is set as locked and can have active = true  
                    iconsHtml.append("<i class=\"ti ti-lock\" style=\"color: #000000;width: 1.25em;\"></i>");

        //If page is soft deleted, add icon to recover page
        if(entity.getDeleted()) {
            link = "javascript:recoverForum(" + entity.getId() + ");";
            iconsHtml.append("<a href=\"" + link + "\" title=\"" + prop.getText("components.banner.recover") + "\"><i class=\"ti ti-trash\" style=\"color: #fabd00;width: 1.25em;\"></i></a> ");
        }

        iconsHtml.append( getStatusIconsHtml() );
        statusIcons = iconsHtml.toString();
    }
}
