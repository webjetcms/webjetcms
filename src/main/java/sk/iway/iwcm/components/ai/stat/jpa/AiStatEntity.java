package sk.iway.iwcm.components.ai.stat.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
@Table(name = "ai_stats")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_AI)
public class AiStatEntity {

    @Id
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "assistant_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.name")
    @NotBlank
    @Size(max = 255)
    private String assistantName;

    @Column(name = "created")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "components.ai_assistants.created_at")
    private Date created;

    @Column(name = "used_tokens")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.ai_stats.used_tokens")
    private Integer usedTokens;

    @Column(name = "user")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "user.user")
    @NotBlank
    @Size(max = 255)
    private String user;

    @Column(name="domain_id")
	private Integer domainId;
}
