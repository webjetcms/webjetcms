package sk.iway.iwcm.system.elfinder;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "file_history")
@Getter
@Setter
public class FileHistoryEntity {

    @Id
    @Column(name = "file_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_file_history")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "file_url")
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN,
        title="components.dmail.camp.subject"
    )
    @Size(max = 255)
    private String fileUrl;

    @Column(name = "change_date")
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="fbrowse.datum_zmeny",
        hiddenEditor = true
    )
    private Date changeDate;

    @Column(name = "user_id")
    private Integer userId;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.user"
    )
    private String userName;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "history_path")
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN,
        title="components.dmail.camp.subject"
    )
    @Size(max = 255)
    private String historyPath;

    @Column(name = "ip_address")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.stat.actual_users.ip"
    )
    @Size(max = 32)
    private String ipAddress;

    @Column(name = "domain_id")
    private Integer domainId;
}
