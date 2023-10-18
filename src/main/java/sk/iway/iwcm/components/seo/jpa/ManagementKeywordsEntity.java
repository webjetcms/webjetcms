package sk.iway.iwcm.components.seo.jpa;

import java.util.Date;

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
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "seo_keywords")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_SEO)
public class ManagementKeywordsEntity {

    @Id
    @Column(name = "seo_keyword_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_seo_keywords")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
    private Long id;

    @Column(name = "name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.seo.keywords.name"
    )
    @Size(max = 100)
    private String name;

    //Beware !!
    //This is not lasic domain id value, it's string, so we dont use DomainRepo
    @Column(name = "domain")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title="components.seo.keywords.domain",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/seo/management-keywords/domain-autocomplete"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    @NotBlank
    @Size(max = 255)
	private String domain;

    @Column(name = "search_bot")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.seo.keywords.searchBot",
        editor = {
			@DataTableColumnEditor(
                attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/seo/management-keywords/searchBot-autocomplete"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    @NotBlank
    @Size(max = 150)
    private String searchBot;

    @Column(name = "created_time")
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.seo.keywords.createdTime",
		hiddenEditor = true
    )
	private Date createdTime;

    @Column(name = "author")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.seo.keywords.author",
        hiddenEditor = true
    )
    private Integer author;

    @Column(name = "actual_position")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.seo.keywords.actual.position",
        hiddenEditor = true
    )
    private Integer actualPosition;
}
