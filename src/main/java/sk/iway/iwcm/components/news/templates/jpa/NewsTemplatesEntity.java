package sk.iway.iwcm.components.news.templates.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;


@Entity
@Table(name = "news_templates")
@EntityListeners(AuditEntityListener.class)
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
        @DataTableTab(id = "code", title = "components.news.template_html"),
        @DataTableTab(id = "paging", title = "components.news.paging")
})
@Getter
@Setter
public class NewsTemplatesEntity {

	public static enum PagingPosition {
		NONE,
		BEFORE,
		AFTER,
		BEFORE_AND_AFTER
	}

	@Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_news_templates")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

	@Column(name = "name")
    @DataTableColumn(
		inputType = DataTableColumnType.OPEN_EDITOR,
		title = "components.news.template_title",
        tab = "basic"
	)
    @NotBlank
    @Size(max = 255)
	private String name;

	@Column(name = "image_path")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "components.news.templates.image",
        renderFormat = "dt-format-image-notext",
        tab = "basic"
    )
    @Size(max = 255)
    private String imagePath;

    @Lob
	@Column(name = "template_html")
	@DataTableColumn(
		inputType = DataTableColumnType.TEXTAREA,
		title = "",
        tab = "code",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code")
            })
        }
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String templateCode;

    @Column(name = "paging_position")
	@DataTableColumn(
		inputType = DataTableColumnType.RADIO,
		title = "components.news.template_paging_position",
        tab = "paging",
		editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(
                        key = "components.news.templates.paging_position_none",
                        value = "0"),
                    @DataTableColumnEditorAttr(
                        key = "components.news.template_paging_position_before",
                        value = "1"),
                    @DataTableColumnEditorAttr(
                        key = "components.news.template_paging_position_after",
                        value = "2"),
                    @DataTableColumnEditorAttr(
                        key = "components.news.templates.paging_position_before_and_after",
                        value = "3")
                }
            )
        }
	)
    private Integer pagingPosition = 0;

    @Lob
	@Column(name = "paging_html")
	@DataTableColumn(
		inputType = DataTableColumnType.TEXTAREA,
		title = "components.news.template_paging_html",
		className = "wrap",
        tab = "paging",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code")
            })
        }
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String pagingCode;

	@Column(name = "engine")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.news.templates.engine.title",
        tab = "basic"
    )
    @Size(max = 255)
    private String engine;

    @Column(name = "context_classes")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.news.contextClasses", className="ai-off")
	protected String contextClasses;

    @Column(name = "domain_id")
    private Integer domainId;

    public String[] getContextClassesArr() {
		return contextClasses == null ? new String[0] : Tools.getTokens(contextClasses, ",;+|");
	}
}