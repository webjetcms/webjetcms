package sk.iway.iwcm.dmail.jpa;

import java.util.Date;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Entita reprezentuje odhlaseny email z hromadneho mailingu
 * #54373
 */

@Entity
@Table(name = "emails_unsubscribed")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_DMAIL_BLACKLIST)
public class UnsubscribedEntity extends ActiveRecordRepository {

    @Id
    @Column(name = "emails_unsubscribed_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_emails_unsubscribed")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", tab="basic")
    private Long id;

    @Column(name = "email")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        renderFormat = "dt-format-text-wrap",
        className = "dt-style-text-wrap",
        editor = {
            @DataTableColumnEditor(type = "textarea")
        },
        title="components.dmail.unsubscribe.email"
    )
    private String email;

    @Column(name = "create_date")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.unsubscribeDate",
        className = "hide-on-create",
        editor=@DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
        })
    )
	private Date createDate;

    public void setId(Long id) {
        this.id = id;
    }
}
