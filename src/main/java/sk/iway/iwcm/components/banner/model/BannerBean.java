
package sk.iway.iwcm.components.banner.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.banner.BannerDB;
import sk.iway.iwcm.components.banner.BannerEditorFields;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Entity
@Table(name = "banner_banners")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_BANNER_UPDATE)
public class BannerBean extends ActiveRecordRepository implements Serializable {

	@JsonIgnore
    private static final long serialVersionUID = -1L;

	@PrePersist
    public void prePersist() {
		if (id != null && (id.intValue()==0 || id.intValue()==-1)) {
			id = null;
		}
    }

    @Id
	@Column(name = "banner_id")
	@GeneratedValue(generator = "WJGen_banner_banners")
	@TableGenerator(name = "WJGen_banner_banners", pkColumnValue = "banner_banners")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

		//Field is invisible and used only for need of CHARTS
		@Transient
		@DataTableColumn(
			inputType = DataTableColumnType.DATE,
			title="editor.date",
			visible = false,
			hiddenEditor = true
		)
		private Date dayDate;

		//Field is invisible and use only in bannner-stat
		@Transient
		@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			visible = false,
			hiddenEditor = true,
			title="[[#{components.banner.name}]]",
			renderFormatLinkTemplate = "javascript:redirect({{id}});"
		)
		private String nameLink;

	/*TAB MAIN*/
    @Column(name = "name")
	@NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="[[#{components.banner.name}]]",
		tab = "main"
    )
	private String name;

	@Column(name = "banner_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.banner.banner_type}]]",
		tab = "main",
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.banner.picture", value = "1"),
					@DataTableColumnEditorAttr(key = "components.banner.html", value = "3"),
					@DataTableColumnEditorAttr(key = "components.banner.content_banner", value = "4"),
					@DataTableColumnEditorAttr(key = "components.video.title", value = "5")
				}
			)
		}
    )
	private Integer bannerType;

    @Column(name = "banner_group")
	@NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{components.banner.group}]]",
		tab = "main",
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/banner/autocomplete"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
	private String bannerGroup;

	@Column(name = "active")
	@DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="[[#{components.banner.active}]]",
		visible = false,
		tab = "main"
    )
	private Boolean active = true;

    @Column(name = "priority")
	@NotNull
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="[[#{components.banner.priority}]]",
		tab = "main"
    )
	private Integer priority;

	/*TAB ADVANCED*/
		/* IMAGE BANNER */
		@Column(name = "banner_location")
		@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title="[[#{components.banner.address}]]",
			visible = false,
			tab = "advanced",
			className = "image banner-type banner-type-1 banner-type-4 banner-type-5",
			renderFormat = "dt-format-image"
		)
		private String bannerLocation;

		@Column(name = "banner_redirect")
		@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title="[[#{components.banner.redirect_link}]]",
			visible = false,
			tab = "advanced",
			className = "banner-type banner-type-1 banner-type-3 banner-type-4 banner-type-5",
			renderFormat = "dt-format-link"
		)
		private String bannerRedirect;

		@Column(name = "target")
		@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title="[[#{components.banner.target}]]",
			visible = false,
			tab = "advanced",
			editor = {
				@DataTableColumnEditor(
					options = {
						@DataTableColumnEditorAttr(key = "components.banner.editor._self", value = "_self"),
						@DataTableColumnEditorAttr(key = "components.banner.editor._blank", value = "_blank"),
						@DataTableColumnEditorAttr(key = "components.banner.editor._top", value = "_top"),
						@DataTableColumnEditorAttr(key = "components.banner.editor._parent", value = "_parent")
					}
				)
			},
			className = "banner-type banner-type-1 banner-type-5"
		)
		private String target;


		/*HTML BANNER columns*/
		@Column(name = "html_code")
		@DataTableColumn(
			inputType = DataTableColumnType.TEXTAREA,
			title="[[#{components.banner.html}]]",
			className = "banner-type banner-type-3",
			tab = "advanced",
			visible = false
		)
		@jakarta.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
		private String htmlCode;


		/*CONTENT BANNER*/
		@Column(name = "image_link")
		@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title="[[#{components.banner.imageLink}]]",
			visible = false,
			className = "image banner-type banner-type-4",
			tab = "advanced",
			renderFormat = "dt-format-image"
		)
		private String imageLink;

		@Column(name = "image_link_mobile")
		@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title="[[#{components.banner.imageLinkMobile}]]",
			visible = false,
			className = "image banner-type banner-type-4",
			tab = "advanced",
			renderFormat = "dt-format-image"
		)
		private String imageLinkMobile;

		@Column(name = "primary_header")
		@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title="[[#{components.banner.primaryHeader}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced"
		)
		private String primaryHeader;

		@Column(name = "secondary_header")
		@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title="[[#{components.banner.secondaryHeader}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced"
		)
		private String secondaryHeader;

		@Column(name = "description_text")
		@DataTableColumn(
			inputType = DataTableColumnType.QUILL,
			title="[[#{components.banner.descriptionText}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced"
		)
		@jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
		private String descriptionText;

		//Primary link
		@Column(name = "primary_link_title")
		@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title="[[#{components.banner.link.title}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			editor = {
				@DataTableColumnEditor(
					attr = {
						@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
						@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.banner.primaryLink}]]")
					}
				)
			}
		)
		private String primaryLinkTitle;

		@Column(name = "primary_link_url")
		@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title="[[#{components.banner.link.url}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			renderFormat = "dt-format-link"
		)
		private String primaryLinkUrl;

		@Column(name = "primary_link_target")
		@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title="[[#{components.banner.link.openOption}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			editor = {
				@DataTableColumnEditor(
					options = {
						@DataTableColumnEditorAttr(key = "components.banner.editor._self", value = "_self"),
						@DataTableColumnEditorAttr(key = "components.banner.editor._blank", value = "_blank")
					}
				)
			}
		)
		private String primaryLinkTarget;

		//Secundary link
		@Column(name = "secondary_link_title")
		@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title="[[#{components.banner.link.title}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			editor = {
				@DataTableColumnEditor(
					attr = {
						@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
						@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.banner.secondaryLink}]]")
					}
				)
			}
		)
		private String secondaryLinkTitle;

		@Column(name = "secondary_link_url")
		@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title="[[#{components.banner.link.url}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			renderFormat = "dt-format-link"
		)
		private String secondaryLinkUrl;

		@Column(name = "secondary_link_target")
		@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title="[[#{components.banner.link.openOption}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			editor = {
				@DataTableColumnEditor(
					options = {
						@DataTableColumnEditorAttr(key = "components.banner.editor._self", value = "_self"),
						@DataTableColumnEditorAttr(key = "components.banner.editor._blank", value = "_blank")
					}
				)
			}
		)
		private String secondaryLinkTarget;

		//Campaign parameter
		@Column(name = "campaign_title")
		@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title="[[#{components.banner.campaignTitle}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced",
			editor = {
				@DataTableColumnEditor(
					attr = {
						@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
						@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.banner.campaign.heading}]]")
					}
				)
			}
		)
		private String campaignTitle;

		@Column(name = "only_with_campaign")
		@DataTableColumn(
			inputType = DataTableColumnType.CHECKBOX,
			title="[[#{components.banner.onlyWithCampaign}]]",
			visible = false,
			className = "banner-type banner-type-4",
			tab = "advanced"
		)
		private Boolean onlyWithCampaign;

	/*TAB restrictions*/
	@Column(name = "date_from")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="[[#{components.banner.dateFrom}]]",
		visible = false,
		tab = "restrictions"
    )
	private Date dateFrom;

	@Column(name = "date_to")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="[[#{components.banner.dateTo}]]",
		visible = false,
		tab = "restrictions"
    )
	private Date dateTo;

	@Column(name = "max_views")
	@NotNull
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="[[#{components.banner.max_views}]]",
		visible = false,
		tab = "restrictions"
    )
	private Integer maxViews;

    @Column(name = "stat_views")
    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title="[[#{components.banner.stat_views.new}]]",
		tab = "restrictions",
		className = "hideOnCreate"
    )
	private Integer statViews;

	@Column(name = "max_clicks")
	@NotNull
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="[[#{components.banner.max_clicks}]]",
		visible = false,
		tab = "restrictions"
    )
	private Integer maxClicks;

    @Column(name = "stat_clicks")
    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title="[[#{components.banner.stat_clicks.new}]]",
		tab = "restrictions",
		className = "hideOnCreate"
    )
	private Integer statClicks;

    @Column(name = "client_id")
	@DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.banner.klient}]]",
		visible = false,
		tab = "restrictions"
    )
	private Integer clientId;

	@Column(name = "field_a")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.banner.field_a",
		visible = false,
		tab = "fields"
    )
	private String fieldA;

	@Column(name = "field_b")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.banner.field_b",
		visible = false,
		tab = "fields"
    )
	private String fieldB;

	@Column(name = "field_c")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.banner.field_c",
		visible = false,
		tab = "fields"
    )
	private String fieldC;

	@Column(name = "field_d")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.banner.field_d",
		visible = false,
		tab = "fields"
    )
	private String fieldD;

	@Column(name = "field_e")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.banner.field_e",
		visible = false,
		tab = "fields"
    )
	private String fieldE;

	@Column(name = "field_f")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.banner.field_f",
		visible = false,
		tab = "fields"
    )
	private String fieldF;

	/*UNUSED*/
	@Column(name = "click_tag")
	private String clickTag;

	@Column(name = "frame_rate")
	private Integer frameRate;

	@Column(name = "height")
	private Integer height;

	@Column(name = "stat_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date statDate;

	@Column(name = "width")
	private Integer width;

	@Column(name = "domain_id")
	private int domainId;

	@Column(name = "visitor_cookie_group")
	private String visitorCookieGroup;

	@DataTableColumnNested
	@Transient
	private transient BannerEditorFields editorFields = null;

	@JsonManagedReference(value="bannerBeanDoc")
    @OneToMany(mappedBy="bannerBeanDoc", fetch=FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval=true)
	@DataTableColumn(inputType=DataTableColumnType.JSON, tab="restrictions", title="components.insert_script.choose_pages", className="dt-tree-page-array",
		editor = { @DataTableColumnEditor( attr = {
			@DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addPage")
		} )}
	)
    List<BannerWebDocBean> docIds;

	@JsonManagedReference(value="bannerBeanGr")
    @OneToMany(mappedBy="bannerBeanGr", fetch=FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval=true)
	@DataTableColumn(inputType=DataTableColumnType.JSON, tab="restrictions", title="grouptree.title", className="dt-tree-group-array",
		editor = { @DataTableColumnEditor( attr = {
			@DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup")
		} )}
	)
    List<BannerWebGroupBean> groupIds;

	@Override
	public Long getId() {
		return id;
	}
	@Override
	public void setId(Long id) {
		this.id = id;
	}
	public int getBannerId()
    {
        if (id==null) return 0;
        return getId().intValue();
    }
    public void setBannerId(int bannerId)
    {
        setId(Long.valueOf(bannerId));
    }
	public boolean isAvailable() {
		return BannerDB.isBannerActive(this);
	}

	public List<BannerWebDocBean> getDocIds() {
        return docIds;
    }

    public void setDocIds(List<BannerWebDocBean> docIds) {
        this.docIds = docIds;
    }

	public List<BannerWebGroupBean> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<BannerWebGroupBean> groupIds) {
        this.groupIds = groupIds;
    }
}
