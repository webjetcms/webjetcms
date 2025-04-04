package sk.iway.iwcm.components.forms;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "form_regular_exp")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORM_REGEXP)
public class RegExpEntity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_webjet_form_regular_exp")
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "title")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title="components.form.admin_form.title")
    @Size(max = 255)
    @NotBlank
    private String title;

    @Column(name = "type")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.form.admin_form.type")
    @Size(max = 32)
    @NotBlank
    private String type;

    @Column(name = "reg_exp")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.form.admin_form.reg_exp")
    @Size(max = 255)
    @NotBlank
    private String regExp;
}
