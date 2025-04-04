package sk.iway.iwcm.components.seo.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "seo_google_position")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_SEO)
public class GooglePositionEntity {

    @Id
    @Column(name = "seo_google_position_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "keyword_id")
    private Integer keywordId;

    @Column(name = "search_datetime")
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.stat.seo.google.position.searching.day",
		hiddenEditor = true
    )
	private Date dayDate;

    @Column(name = "position")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.stat.seo.google.position.searching.position"
    )
    private Integer position;
}
