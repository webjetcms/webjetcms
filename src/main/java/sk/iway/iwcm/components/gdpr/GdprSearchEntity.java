package sk.iway.iwcm.components.gdpr;


import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Fiktivna entita pre zobrazenie vysledkov vyhladavania v GDPR
 */
@Entity
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_PEREX_GROUP_UPDATE)
public class GdprSearchEntity implements Serializable{

    @Id
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{calendar_edit.title}]]"
    )
    private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{search.module}]]"
    )
    private String modul;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="[[#{admin.conf_editor.value}]]"
    )
    private String value;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{editor.link.url}]]"
    )
    private String url;
}
