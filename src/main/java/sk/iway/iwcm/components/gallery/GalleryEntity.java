package sk.iway.iwcm.components.gallery;

import javax.persistence.*;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

import java.util.Date;

@Entity
@Table(name = "gallery")
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_GALLERY)
@Getter
@Setter
public class GalleryEntity {

    public GalleryEntity() {
        //
    }

    @PrePersist
    public void prePersist() {
        if (sendCount==null) sendCount = 0;
        if (domainId==null) domainId = CloudToolsForCore.getDomainId();
    }

    @Id
    @GeneratedValue(generator="WJGen_gallery")
    @TableGenerator(name="WJGen_gallery", pkColumnValue = "gallery")
    @Column(name = "image_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.GALLERY_IMAGE, title="editor.perex.image", hiddenEditor = true)
    private String datatableImage;

    @Size(max = 255)
    @Column(name = "image_name")
    @DataTableColumn(inputType = {DataTableColumnType.OPEN_EDITOR}, tab = "metadata", title="[[#{components.gallery.fileName}]]")
    private String imageName;

    @Size(max = 255)
    @Column(name = "image_path")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="admin.temp_group_list.directory", tab = "metadata",
        editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
            })
    })
    private String imagePath;

    @Size(max = 1000)
    @Column(name = "s_description_sk")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>sk</span>",
            tab = "description",
            renderFormat = "dt-format-text sk",
            editor = {
                    @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.slovak}]]")
                    })
            })
    private String descriptionShortSk;

    @Column(name = "l_description_sk")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>sk</span>",
            tab = "description",
            renderFormat = "dt-format-text sk"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongSk;

    @Size(max = 1000)
    @Column(name = "s_description_cz")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>cz</span>",
            tab = "description",
            renderFormat = "dt-format-text cz",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.czech}]]")
                    })
            })
    private String descriptionShortCz;

    @Column(name = "l_description_cz")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>cz</span>",
            tab = "description",
            renderFormat = "dt-format-text cz"
    )
    private String descriptionLongCz;

    @Size(max = 1000)
    @Column(name = "s_description_en")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>en</span>",
            tab = "description",
            renderFormat = "dt-format-text en",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.english}]]")
                    })
            })
    private String descriptionShortEn;

    @Column(name = "l_description_en")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>en</span>",
            tab = "description",
            renderFormat = "dt-format-text en"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongEn;

    @Size(max = 1000)
    @Column(name = "s_description_de")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>de</span>",
            tab = "description",
            renderFormat = "dt-format-text de",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.deutsch}]]")
                    })
            })
    private String descriptionShortDe;

    @Column(name = "l_description_de")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>de</span>",
            tab = "description",
            renderFormat = "dt-format-text de"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongDe;

    @Size(max = 1000)
    @Column(name = "s_description_pl")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>pl</span>",
            tab = "description",
            renderFormat = "dt-format-text pl",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.polish}]]")
                    })
            })
    private String descriptionShortPl;

    @Column(name = "l_description_pl")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>pl</span>",
            tab = "description",
            renderFormat = "dt-format-text pl"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongPl;

    @Size(max = 1000)
    @Column(name = "s_description_ru")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>ru</span>",
            tab = "description",
            renderFormat = "dt-format-text ru",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.ru}]]")
                    })
            })
    private String descriptionShortRu;

    @Column(name = "l_description_ru")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>ru</span>",
            tab = "description",
            renderFormat = "dt-format-text ru"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongRu;

    @Size(max = 1000)
    @Column(name = "s_description_hu")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>hu</span>",
            tab = "description",
            renderFormat = "dt-format-text hu",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.hungary}]]")
                    })
            })
    private String descriptionShortHu;

    @Column(name = "l_description_hu")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>hu</span>",
            tab = "description",
            renderFormat = "dt-format-text hu"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongHu;

    @Size(max = 1000)
    @Column(name = "s_description_cho")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>hr</span>",
            tab = "description",
            renderFormat = "dt-format-text hr",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.chorvatsky}]]")
                    })
            })
    private String descriptionShortCho;

    @Column(name = "l_description_cho")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>hr</span>",
            tab = "description",
            renderFormat = "dt-format-text hr"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongCho;

    @Size(max = 1000)
    @Column(name = "s_description_esp")
    @DataTableColumn(
            inputType = DataTableColumnType.TEXT,
            title = "[[#{gallery.s_description}]] <span class='lang-shortcut'>esp</span>",
            tab = "description",
            renderFormat = "dt-format-text esp",
            editor = {
                    @DataTableColumnEditor(attr = {
                            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{temp.esp}]]")
                    })
            })
    private String descriptionShortEsp;

    @Column(name = "l_description_esp")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>esp</span>",
            tab = "description",
            renderFormat = "dt-format-text esp"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongEsp;

    @Size(max = 255)
    @Column(name = "author")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            tab = "metadata",
            title = "components.gallery.author"
        )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String author;

    @Column(name = "upload_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
            tab = "metadata",
            title = "components.gallery.metadata.uploadDateTime"
    )
    public Date uploadDatetime;

    @Column(name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "metadata", title = "gallery.sort_priority")
    private Integer sortPriority;

    @Column(name = "selected_height")
    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER_INVISIBLE, tab = "areaOfInterest", title = "components.gallery.areaOfInterest.selectedHeight")
    private Integer selectedHeight;

    @Column(name = "selected_width")
    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER_INVISIBLE, tab = "areaOfInterest", title = "components.gallery.areaOfInterest.selectedWidth")
    private Integer selectedWidth;

    @Column(name = "selected_x")
    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER_INVISIBLE, tab = "areaOfInterest", title = "components.gallery.areaOfInterest.selectedX")
    private Integer selectedX;

    @Column(name = "selected_y")
    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER_INVISIBLE, tab = "areaOfInterest", title = "components.gallery.areaOfInterest.selectedY")
    private Integer selectedY;

    @Size(max = 255)
    @Column(name = "allowed_domains")
    private String allowedDomains;

    @Size(max = 255)
    @Column(name = "perex_group")
    private String perexGroup;

    @Column(name = "send_count")
    private Integer sendCount;

    @Column(name = "domain_id")
    private Integer domainId;
}
