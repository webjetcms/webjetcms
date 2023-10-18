package sk.iway.iwcm.components.gallery;

import java.util.Date;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
@Table(name = "gallery_dimension")
public class GalleryDimension {

    @Id
    @Column(name = "dimension_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_gallery_dimension")
    @DataTableColumn(inputType=DataTableColumnType.ID, title="editor.cell.id", renderFormat = "dt-format-selector")
    private Long id;


    //Tab zakladne - START
    @Column(name = "gallery_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "components.gallery.name",
        renderFormat = "dt-format-text",
        tab = "basic"
    )
    private String name = "";

    @Column(name = "gallery_perex")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "editor.tab.perex",
        renderFormat = "dt-format-textarea",
        tab = "basic"
    )
    private String perex = "";

    @Column(name = "author")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.gallery.author",
        renderFormat = "dt-format-text",
        tab = "basic"
    )
    private String author = "";

    @Column(name = "create_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "components.gallery.createDate",
        renderFormat = "dt-format-date-time",
        tab = "basic"
    )
    private Date date;
    //Tab zakladne - END



    //Tab rozmery - START
    @Column(name = "resize_mode")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "admin.gallery.resizeMode",
        tab = "sizes",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "admin.gallery.shrinkToFit", value = "S"),
                    @DataTableColumnEditorAttr(key = "admin.gallery.cropToFit", value = "C"),
                    @DataTableColumnEditorAttr(key = "admin.gallery.accurateWidthHeight", value = "A"),
                    @DataTableColumnEditorAttr(key = "admin.gallery.accurateWidth", value = "W"),
                    @DataTableColumnEditorAttr(key = "admin.gallery.accurateHeight", value = "H"),
                    @DataTableColumnEditorAttr(key = "admin.gallery.noMiniatures", value = "N")
                }
            )
        }
    )
    private String resizeMode;

    @Column(name = "image_width")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "gallery.image_width",
        renderFormat = "dt-format-number",
        tab = "sizes",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "gallery.preview_size")
                }
            )
        }
    )
    private Integer imageWidth = 160;

    @Column(name = "image_height")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "gallery.image_height",
        renderFormat = "dt-format-number",
        tab = "sizes"
    )
    private Integer imageHeight = 120;

    @Column(name = "normal_width")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT_NUMBER,
        title = "gallery.image_width",
        renderFormat = "dt-format-number",
        tab = "sizes",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "gallery.normal_size")
                }
            )
        }
    )
    private Integer normalWidth = 750;

    @Column(name = "normal_height")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT_NUMBER,
        title = "gallery.image_height",
        renderFormat = "dt-format-number",
        tab = "sizes"
    )
    private Integer normalHeight = 560;
    //Tab rozmery - END




    //Tab Vodotlac - START
    @Column(name = "watermark")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title = "components.gallery.watermark",
        tab = "watermark"
    )
    private String watermark;

    @Column(name = "watermark_placement")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.gallery.watermark_placement",
        tab = "watermark",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.custom_style.alignCenter", value = "Center"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.South", value = "South"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.SouthEast", value = "SouthEast"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.East", value = "East"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.NorthEast", value = "NorthEast"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.North", value = "North"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.NorthWest", value = "NorthWest"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.West", value = "West"),
                    @DataTableColumnEditorAttr(key = "components.gallery.watermark.SouthWest", value = "SouthWest")
                }
            )
        }
    )
    private String watermarkPlacement;

    @Column(name = "watermark_saturation")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.gallery.watermark_saturation",
        tab = "watermark",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "10%", value = "10"),
                    @DataTableColumnEditorAttr(key = "20%", value = "20"),
                    @DataTableColumnEditorAttr(key = "30%", value = "30"),
                    @DataTableColumnEditorAttr(key = "40%", value = "40"),
                    @DataTableColumnEditorAttr(key = "50%", value = "50"),
                    @DataTableColumnEditorAttr(key = "60%", value = "60"),
                    @DataTableColumnEditorAttr(key = "70%", value = "70"),
                    @DataTableColumnEditorAttr(key = "80%", value = "80"),
                    @DataTableColumnEditorAttr(key = "90%", value = "90")
                }
            )
        }
    )
    private Integer watermarkSaturation = 70;
    //Tab Vodotlac - END



    //Other columns
    @Column(name = "image_path")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "sync.path",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
            )
        }
    )
    private String path;

    @Column(name = "views")
    private int views = 0;

    @Column(name = "domain_id")
    private Integer domainId;

    @Transient
    @DataTableColumnNested
    private GalleryDimensionEditorFields editorFields = null;

    public GalleryDimension() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @JsonIgnore
    public String getParentPath() {
        return path.substring(0, path.lastIndexOf("/"));
    }

    @JsonIgnore
    public String getNameFromPath() {
        String[] tokens = Tools.getTokens(path, "/");
        return tokens[tokens.length - 1];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPerex() {
        return perex;
    }

    public void setPerex(String perex) {
        this.perex = perex;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getResizeMode() {
        return resizeMode;
    }

    public void setResizeMode(String resizeMode) {
        this.resizeMode = resizeMode;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getNormalWidth() {
        return normalWidth;
    }

    public void setNormalWidth(Integer normalWidth) {
        this.normalWidth = normalWidth;
    }

    public Integer getNormalHeight() {
        return normalHeight;
    }

    public void setNormalHeight(Integer normalHeight) {
        this.normalHeight = normalHeight;
    }

    public Integer getDomainId() {
        return domainId;
    }

    public void setDomainId(Integer domainId) {
        this.domainId = domainId;
    }

    public Integer getWatermarkSaturation() {
        return watermarkSaturation;
    }

    public void setWatermarkSaturation(Integer watermarkSaturation) {
        this.watermarkSaturation = watermarkSaturation;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public String getWatermarkPlacement() {
        return watermarkPlacement;
    }

    public void setWatermarkPlacement(String watermarkPlacement) {
        this.watermarkPlacement = watermarkPlacement;
    }

    public GalleryDimensionEditorFields getEditorFields() {
        return editorFields;
    }

    public void setEditorFields(GalleryDimensionEditorFields editorFields) {
        this.editorFields = editorFields;
    }
}
