package sk.iway.iwcm.components.ai.stat.jpa;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "ai_stats")
@Getter
@Setter
//DO NOT set EntityListeners for audit - we don't need to audit stat writing events
public class AiStatEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_ai_stats")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name="assistant_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Long assistantId;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.name")
    private String assistantName;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.provider")
    private String assistantProvider;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.group")
    private String assistantGroupName;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.action")
    private String assistantAction;

    @Column(name = "created")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "components.ai_assistants.stats.created_at")
    private Date created;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.inquiry.inquiry_statistics.user_name", filter = true)
    private String userName;

    @Column(name = "used_tokens")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.ai_assistants.stats.used_tokens")
    private Integer usedTokens;

    @Column(name = "user_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Long userId;

    @Column(name="domain_id")
	private Integer domainId;
}
