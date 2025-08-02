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
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;


@Entity
@Table(name = "news_templates")
@EntityListeners(AuditEntityListener.class)
//@EntityListenersType()
@Getter
@Setter
public class NewsTemplatesEntity {

    // Propably needed later when working with old code
	// private enum PagingPosition {
	// 	NONE,
	// 	BEFORE,
	// 	AFTER,
	// 	BEFORE_AND_AFTER
	// }

	@Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_news_templates")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

	@Column(name = "name")
    @DataTableColumn(
		inputType = DataTableColumnType.OPEN_EDITOR,
		title = "components.news.template_title"
	)
    @NotBlank
    @Size(max = 255)
	private String name;

	@Column(name = "image_path")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "components.news.templates.image",
        renderFormat = "dt-format-image-notext"
    )
    @Size(max = 255)
    private String imagePath;

    @Lob
	@Column(name = "template_html")
	@DataTableColumn(
		inputType = DataTableColumnType.TEXTAREA,
		title = "components.news.template_html"
		//className = "wrap"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String templateCode;

    @Lob
	@Column(name = "paging_html")
	@DataTableColumn(
		inputType = DataTableColumnType.TEXTAREA,
		title = "components.news.template_paging_html",
		className = "wrap"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String pagingCode;

    @Column(name = "paging_position")
	@DataTableColumn(
		inputType = DataTableColumnType.RADIO,
		title = "components.news.template_paging_position",
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

	@Column(name = "engine")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.news.templates.engine.title"
    )
    @Size(max = 255)
    private String engine;

    @Column(name = "domain_id")
    private Integer domainId;
}