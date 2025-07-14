package sk.iway.iwcm.doc;

import static sk.iway.iwcm.Tools.isEmpty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.components.basket.rest.EshopService;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.CommaSeparatedIntegersConverter;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Informacie o dokumente z databazy, tabulka documents
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.16 $
 *@created      $Date: 2004/02/16 16:44:23 $
 *@modified     $Date: 2004/02/16 16:44:23 $
 */

@MappedSuperclass
public class DocBasic implements DocGroupInterface, Serializable
{
	private static final BigDecimal VALUE_OF_MINUS_1 = new BigDecimal(-1);
	private static final BigDecimal VALUE_OF_0 = new BigDecimal(0);
	private static final BigDecimal VALUE_OF_1 = new BigDecimal(1);
	private static final BigDecimal VALUE_OF_100 = new BigDecimal(100);

	@Column(name = "group_id")
	private int groupId = 0;

	@Column(name = "title")
	@Size(max = 255)
	@DataTableColumn(
			inputType = DataTableColumnType.OPEN_EDITOR,
			title = "editor.title",
			tab = "basic",
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-validation", value = "true"),
							@DataTableColumnEditorAttr(key = "data-dt-escape-slash", value = "true")
					})
			}
	)
	@NotBlank
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String title;

	@Column(name = "navbar")
	@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title = "webpages.editor.titleForMenu",
			tab = "basic",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
							@DataTableColumnEditorAttr(key = "data-dt-validation", value = "true"),
							@DataTableColumnEditorAttr(key = "data-dt-escape-slash", value = "true")
					})
			}
	)
	@NotBlank
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String navbar;

	@Column(name = "virtual_path")
	@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title = "editor.virtual_path",
			tab = "basic",
			visible = false,
			className = "DTE_Field_Has_Checkbox"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String virtualPath = "";

	@Column(name = "editor_virtual_path ")
	@Size(max = 255)
	@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title = "editor.virtual_path",
			tab = "basic",
			hidden = true,
			visible = false,
			className = "DTE_Field_Has_Checkbox"
	)
	private String editorVirtualPath;

	@Column(name = "generate_url_from_title")
	@DataTableColumn(
			inputType = DataTableColumnType.BOOLEAN,
			title = "editor.generate_url_from_title",
			tab = "basic",
			hidden = true,
			className = "DTE_Field_Has_Checkbox"
	)
	private boolean generateUrlFromTitle = true;

	@Column(name = "url_inherit_group")
	@DataTableColumn(
			inputType = DataTableColumnType.BOOLEAN,
			title = "editor.url_inherit_group",
			tab = "basic",
			hidden = true,
			className = "DTE_Field_No_Margin_Top"
	)
	private boolean urlInheritGroup = false;

	@Column(name = "sort_priority")
	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		title = "editor.sort_order",
		tab = "menu",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				message = "editor.sort_order_help",
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
				}
			)
		}
	)
	private int sortPriority = 0;

	@Column(name = "external_link")
	@DataTableColumn(
			inputType = DataTableColumnType.ELFINDER,
			title = "editor.external_link",
			tab = "basic",
			visible = false,
			editor = {
					@DataTableColumnEditor(
						label = "editor.external_link.label",
						message = "editor.external_link_help",
						attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.external_link"),
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
						}
					)
			}
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String externalLink = "";

	@Column(name = "available")
	@DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title="editor.available_enabled", tab="basic",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "editor.available_enabled_label", value = "true")
				},
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.properties")
				}
			)
		}
	)
	private boolean available = false;

	@Column(name = "searchable")
	@DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title="editor.searchable_enabled", tab="basic",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "editor.searchable_enabled_label", value = "true")
				}
			)
		}
	)
	private boolean searchable = false;

	@Column(name = "cacheable")
	@DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title="editor.cache", tab="basic",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "editor.cache_label", value = "true")
				},
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
				}
			)
		}
	)
	private boolean cacheable = false;

	@Column(name = "temp_id")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "editor.template",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/templates/temps-list/?tempId={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int tempId;

	@Column(name = "header_doc_id")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "editor.header",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuWebpages")
					})
			}
	)
	private int headerDocId = -1;

	@Column(name = "footer_doc_id")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "editor.footer",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int footerDocId = -1;

	@Column(name = "menu_doc_id")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "editor.menu",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int menuDocId = -1;

	@Column(name = "right_menu_doc_id")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "editor.right_menu",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int rightMenuDocId = -1;

	@DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "editor.perex.group",
        tab = "perex",
		visible = false,
        sortAfter = "perexImage",
		orderable = false,
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
	@Column(name = "perex_group")
	@Convert(converter = CommaSeparatedIntegersConverter.class)
	private Integer[] perexGroups;

	@Column(name = "date_created")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(inputType = DataTableColumnType.DATETIME, title="webstranky.nadpis.posledna_zmena",
		visible = true, hiddenEditor = true, sortAfter = "authorName"
	)
	private Date dateCreated = null;

	@Column(name = "password_protected")
	private String passwordProtected = "";

	//ak je nastavene na true tak po skonceni publikacie sa dokument zneplatni
	@Column(name = "disable_after_end")
	private boolean disableAfterEnd = false;

	//ak je nastavene na true tak bude publikovane v buducnosti (kopiruje atribut publicable ale po publikovani nezmeni stav)
	@Column(name = "publish_after_start")
	private boolean publishAfterStart = false;

	@Transient
	private boolean lazyLoaded = false;

	@Column(name = "require_ssl")
	@DataTableColumn(inputType = DataTableColumnType.HIDDEN, title="editor.requireSsl", tab="access",
		visible = false, sortAfter = "cacheable"
	)
	private boolean requireSsl = false;

	//Old FieldsFromAtoE aka firstFields
	@Column(name = "field_a")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_a",
	tab = "fields", visible = false, sortAfter = "perexGroups")
	private String fieldA = "";

	@Column(name = "field_b")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_b",
		tab = "fields", visible = false, sortAfter = "fieldA")
	private String fieldB = "";

	@Column(name = "field_c")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_c",
		tab = "fields", visible = false, sortAfter = "fieldB")
	private String fieldC = "";

	@Column(name = "field_d")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_d",
		tab = "fields", visible = false, sortAfter = "fieldC")
	private String fieldD = "";

	@Column(name = "field_e")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_e",
		tab = "fields", visible = false, sortAfter = "fieldD")
	private String fieldE = "";

	//Old FieldsFromFtoT aka fields
	@Column(name = "field_f")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_f",
	tab = "fields", visible = false, sortAfter = "fieldE")
	private String fieldF = "";

	@Column(name = "field_g")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_g",
		tab = "fields", visible = false, sortAfter = "fieldF")
	private String fieldG = "";

	@Column(name = "field_h")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_h",
		tab = "fields", visible = false, sortAfter = "fieldG")
	private String fieldH = "";

	@Column(name = "field_i")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_i",
		tab = "fields", visible = false, sortAfter = "fieldH")
	private String fieldI = "";

	@Column(name = "field_j")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_j",
		tab = "fields", visible = false, sortAfter = "fieldI")
	private String fieldJ = "";

	@Column(name = "field_k")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_k",
		tab = "fields", visible = false, sortAfter = "fieldJ")
	private String fieldK = "";

	@Column(name = "field_l")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_l",
		tab = "fields", visible = false, sortAfter = "fieldK")
	private String fieldL = "";

	@Column(name = "field_m")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_m",
		tab = "fields", visible = false, sortAfter = "fieldL")
	private String fieldM = "";

	@Column(name = "field_n")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_n",
		tab = "fields", visible = false, sortAfter = "fieldM")
	private String fieldN = "";

	@Column(name = "field_o")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_o",
		tab = "fields", visible = false, sortAfter = "fieldN")
	private String fieldO = "";

	@Column(name = "field_p")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_p",
		tab = "fields", visible = false, sortAfter = "fieldO")
	private String fieldP = "";

	@Column(name = "field_q")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_q",
		tab = "fields", visible = false, sortAfter = "fieldP")
	private String fieldQ = "";

	@Column(name = "field_r")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_r",
		tab = "fields", visible = false, sortAfter = "fieldQ")
	private String fieldR = "";

	@Column(name = "field_s")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_s",
		tab = "fields", visible = false, sortAfter = "fieldR")
	private String fieldS = "";

	@Column(name = "field_t")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.field_t",
		tab = "fields", visible = false, sortAfter = "fieldS")
	private String fieldT = "";

	//Doplnene volne polia - ticket 54205 - prva cast
	@Column(name = "temp_field_a_docid")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "temp_edit.object_a",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int tempFieldADocId = -1;

	@Column(name = "temp_field_b_docid")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "temp_edit.object_b",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int tempFieldBDocId = -1;

	@Column(name = "temp_field_c_docid")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "temp_edit.object_c",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int tempFieldCDocId = -1;

	@Column(name = "temp_field_d_docid")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "temp_edit.object_d",
			tab = "template",
			visible = false,
			editor = {
					@DataTableColumnEditor(attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
							@DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
					})
			}
	)
	private int tempFieldDDocId = -1;

	@Column(name = "show_in_menu")
	@DataTableColumn(
			inputType = DataTableColumnType.SELECT,
			title = "editor.menu_type_menu",
			tab = "menu",
			visible = false,
			editor = {
				@DataTableColumnEditor(
					attr = {
						@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.menu_type_notlogged")
					},
					options = {
						@DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
						@DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
					},
					message="editor.menu_type_menu.tooltip-webpage"
				)
			}
	)
	private boolean showInMenu = false;

	//Doplnene volne polia - ticket 54205 - druha cast
	@Column(name = "show_in_navbar")
    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            title = "editor.menu_type_navbar",
            tab = "menu",
            visible = false,
            editor = {
                @DataTableColumnEditor(
                    options = {
						@DataTableColumnEditorAttr(key = "editor.navbar.same_as_menu", value = "null"),
                        @DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
                        @DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
                    }
                )
            }
    )
    private Boolean showInNavbar;

	@Column(name = "show_in_sitemap")
    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            title = "editor.menu_type_site_map",
            tab = "menu",
            visible = false,
            editor = {
                @DataTableColumnEditor(
                    options = {
                        @DataTableColumnEditorAttr(key = "editor.navbar.same_as_menu", value = "null"),
                        @DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
                        @DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
                    },
					message = "editor.menu_type_site_map.tooltip-webpage"
                )
            }
    )
    private Boolean showInSitemap;

	@Column(name = "logged_show_in_menu")
    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            title = "editor.menu_type_menu",
            tab = "menu",
            visible = false,
            editor = {
                @DataTableColumnEditor(
                    attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.menu_type_logged")
                    },
                    options = {
						@DataTableColumnEditorAttr(key = "groupedit.menu_type_same_as_normal", value = "null"),
                        @DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
                        @DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
                    },
					message="editor.menu_type_menu.tooltip-webpage"
                )
            }
    )
    private Boolean loggedShowInMenu;

	@Column(name = "logged_show_in_navbar")
    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            title = "editor.menu_type_navbar",
            tab = "menu",
            visible = false,
            editor = {
                @DataTableColumnEditor(
                    options = {
                        @DataTableColumnEditorAttr(key = "groupedit.menu_type_same_as_normal", value = "null"),
                        @DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
                        @DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
                    }
                )
            }
    )
    private Boolean loggedShowInNavbar;

	@Column(name = "logged_show_in_sitemap ")
    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            title = "editor.menu_type_site_map",
            tab = "menu",
            visible = false,
            editor = {
                @DataTableColumnEditor(
                    options = {
                        @DataTableColumnEditorAttr(key = "groupedit.menu_type_same_as_normal", value = "null"),
                        @DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
                        @DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
                    },
					message = "editor.menu_type_site_map.tooltip-webpage"
                )
            }
    )
    private Boolean loggedShowInSitemap;


	//Old DocumentAdvancedFields aka advancedFields
	@Lob
	@Column(name = "html_head")
	@DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title="editor.tab.html_header",
		tab = "template", visible = false, sortAfter = "tempFieldDDocId"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String htmlHead = "";

	@Column(name = "publish_start")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.dateStart",
		tab = "perex", visible = false, sortAfter = "editorFields.emails", className = "DTE_Field_Has_Checkbox"
	)
	private Date publishStartDate;

	@Column(name = "publish_end")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.dateEnd",
		tab = "perex", visible = false, sortAfter = "editorFields.publishAfterStart", className = "DTE_Field_Has_Checkbox"
	)
	private Date publishEndDate;

	@Column(name = "event_date")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.eventDate",
		tab = "perex", visible = false, sortAfter = "editorFields.disableAfterEnd",
		editor = {
			@DataTableColumnEditor(attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
			})
		}
	)
	private Date eventDateDate;

	@Lob
	@Column(name = "html_data")
	@DataTableColumn(inputType = DataTableColumnType.TEXTAREA, className = "wrap", title="editor.tab.perex",
		tab = "perex", visible = false, sortAfter = "eventDateDate"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String htmlData = "";

	@Column(name = "perex_place")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="editor.perex.place",
		tab = "perex", visible = false, sortAfter = "htmlData"
	)
	private String perexPlace = "";

	@Column(name = "perex_image")
	@DataTableColumn(inputType = DataTableColumnType.ELFINDER, title="editor.perex.image",
		tab = "perex", visible = false, sortAfter = "perexPlace",
		className = "image",
		editor = {
			@DataTableColumnEditor(attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
			})
		},
		renderFormat = "dt-format-image"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String perexImage = "";

	@Transient
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="groupslist.approve.authorName",
		visible = true, hiddenEditor = true, sortAfter = "title", orderable = false, filter = true
	)
	private String authorName = "";

	@Transient
	private String docLink;

	@Lob
	@Column(name = "data")
	@DataTableColumn(inputType = DataTableColumnType.WYSIWYG, title="components.news.template_html",
		hidden = true, tab="content"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String data;

	@Lob
	@Column(name = "data_asc")
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String dataAsc;

	@Column(name = "author_id")
	private int authorId;

	@Transient
	private String tempName;

	@Transient
	private String publishEndString;

	@Transient
	private String publishEndTimeString;

	@Transient
	private String publishStartString;

	@Transient
	private String publishStartStringExtra;

	@Transient
	private String publishStartTimeString;

	@Transient
	private String syncDefaultForGroupId;

	@Transient
	private String syncRemotePath;

	@Column(name = "logon_page_doc_id")
	private int logonPageDocId = 0;

	@Column(name = "sync_id")
	private int syncId;

	@Column(name = "sync_status")
	private int syncStatus;

	@Column(name = "forum_count", updatable = false)
	private int forumCount;

	//Must be change from Transient to Column, because we need save this method
	// @Size(max = 255)
	@Column(name = "file_name")
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String fileName;

	@Transient
	private String authorEmail;

	@Transient
	private String authorPhoto;

	@Column(name = "views_total", updatable = false)
	private int viewsTotal = 0;

	@Transient
	@DataTableColumnNested
	private transient DocEditorFields editorFields = null;

	public DocBasic(){
		//konstruktor
	}

	//============ Veci pre shop ==================

	@JsonIgnore
	public BigDecimal getPrice()
	{
		return getPrice(null);
	}

	public BigDecimal getPrice(HttpServletRequest request)
	{
		String str = "0";
		String productType = "";
		try
		{
			str = BeanUtils.getProperty(this, Constants.getString("basketPriceField"));
			productType = BeanUtils.getProperty(this, Constants.getString("basketProductTypeField"));
		}
		catch (Exception ex)
		{
			Logger.error(DocBasic.class, ex);
		}
		if(Tools.isNotEmpty(str) && Character.isLetter(str.trim().charAt(0)))
			str = getPriceStringByCategory(str.trim().charAt(0),productType);

		return parse(str,request);
	}

	/**
	 * ziska cenu ako string z {@link Constants} na zaklade kluca basketPrice+productType+priceCategory ie. basketPriceObalkaC a pod.
	 * @param priceCategory pismenko definujuce cenovu kategoriu, bude konvertnute na uppercase
	 * @param productType typ produktu, ak su rozne cenove kategorie pre rozne typy produktov
	 * @return cenu ako string
	 */
	private String getPriceStringByCategory(char priceCategory, String productType)
	{
		String constKey = "basketPrice";
		if(Tools.isNotEmpty(productType))
			constKey += DB.internationalToEnglish(productType.replaceAll("\\s", "").replaceAll("[-+.^:,]",""));
		constKey += Character.toUpperCase(priceCategory);
		return Constants.getString(constKey);
	}

	/**
	 * Vrati cenu "dokumentu" v mene, ktora sa vyhodnoti na zaklade requestu.
	 *
	 * @param request
	 * @return BigDecimal cena v danej mene
	 */
	public BigDecimal getLocalPrice(HttpServletRequest request)
	{
		return getLocalPrice( request, EshopService.getDisplayCurrency(request) );
	}

	@JsonIgnore
	public String getCurrency()
	{
		String itemCurrency = "skk";
		try
		{
			itemCurrency = BeanUtils.getProperty(this, Constants.getString("basketCurrencyField"));
			if (Tools.isEmpty(itemCurrency))
				itemCurrency = Constants.getString("basketProductCurrency");
			//ak sme stale nic nezistili, ideme na zaloznu moznost...skk
			if (Tools.isEmpty(itemCurrency))
				itemCurrency = "skk";
		}
		catch (Exception ex)
		{
			Logger.error(DocBasic.class, ex);
		}
		return itemCurrency.toLowerCase();
	}

	/**
	 * Prepocita zadanu cenu z meny vedenej u vyrobku na menu zadanu ako paramater.
	 * Mena sa zadava v jej medzinarodnom kodovom oznaceni. Najpouzivanejsie meny
	 * slovenska koruna - skk , ceska - czk, euro - eur, britska libra - gbp, americky dolar - usd.
	 * AK VYROBOK MOZE MAT VIAC CIEN, zalezajucich od skupiny, v ktorej sa
	 * pouzivatel nachadza, POUZITE METODU getLocalPrice()
	 *
	 * @param basePrice - cena, ktoru chceme prepocitat
	 * @param userCurrency - kodove oznacenie meny, v ktorej chceme dostat vysledok
	 * @return BigDecimal Cena vo vyslednej mene
	 */
	public BigDecimal calculateLocalPrice(BigDecimal basePrice, String userCurrency)
	{
		String itemCurrency = getCurrency();
		userCurrency = userCurrency.toLowerCase();
		// samotny prepocet mien
		if (!itemCurrency.equalsIgnoreCase(userCurrency))
		{
			String constantName = "kurz_" + itemCurrency + "_" + userCurrency;
			BigDecimal rate;
			// nasli sme bezny kurz
			if (Tools.isNotEmpty(Constants.getString(constantName)))
			{
				rate = new BigDecimal( Constants.getString(constantName) );
				return basePrice.multiply(rate);
			}
			// nevyslo, skusime opacnu konverziu
			constantName = "kurz_" + userCurrency + "_" + itemCurrency;
			// podobne, ako hore, ale kedze ide o opacny kurz, musime spravit
			// 1/kurz
			if (Tools.isNotEmpty(Constants.getString(constantName)))
			{
				rate = new BigDecimal( Constants.getString(constantName) );
				return  ( (VALUE_OF_1).divide(rate) ).multiply(basePrice);
			}
		}
		// nedopracovali sme sa k vysledku, vraciame povodnu cenu
		return basePrice;
	}

	/**
	 * Vrati cenu "dokumentu" v zadanej mene. Ak je dokument vedeny v
	 * databaze pod inou menou, cena sa prepocita, ak existuje vzajomny kurz.
	 *
	 * Ak nepozna menu, ktora bola zadana, vrati sa cena zapisana v databaze nehladiac
	 * na menu, v akej je zapisana.
	 *
	 * @param request
	 * @param userCurrency String kodove oznacenie meny @see {@link DocDetails#calculateLocalPrice(BigDecimal, String)}
	 * @return BigDecimal cena
	 */
	public BigDecimal getLocalPrice(HttpServletRequest request, String userCurrency)
	{
		// ziskame si beznu cenu vyrobku, este nevieme, v akej mene
		BigDecimal itemPrice = getPrice(request);
		return calculateLocalPrice(itemPrice, userCurrency);
	}

	/**
	 * Vypocita cenu aj s DPH v defaultnej mene pouzivatela.
	 * @param request
	 * @return BigDecimal cena
	 */
	public BigDecimal getLocalPriceVat(HttpServletRequest request)
	{
		BigDecimal localPrice = getLocalPrice(request);
		//AKA (getVat / 100 + 1) * localPrice
		return ( (getVat().divide(VALUE_OF_100)).add(VALUE_OF_1) ).multiply(localPrice);
	}

	/**
	 * Vypocita cenu aj s DPH v zadanej mene
	 * @param request
	 * @param currency String kodove oznacenie meny @see {@link DocDetails#calculateLocalPrice(BigDecimal, String)}
	 * @return BigDecimal cena
	 */
	public BigDecimal getLocalPriceVat(HttpServletRequest request, String currency)
	{
		BigDecimal localPrice = getLocalPrice(request, currency);
		//AKA (getVat / 100 + 1) * localPrice
		return ( ( getVat().divide(VALUE_OF_100) ).add(VALUE_OF_1) ).multiply(localPrice);
	}

	@JsonIgnore
	public BigDecimal getVat()
	{
		String str = "0";
		try
		{
			str = BeanUtils.getProperty(this, Constants.getString("basketVatField"));
		}
		catch (Exception ex)
		{
			Logger.error(DocBasic.class, ex);
		}
		if(Constants.getInt("basketVatPercentage") > 0 && Tools.getDoubleValue(str, 0) == 0d)
			return BigDecimal.valueOf( Constants.getInt("basketVatPercentage") );
		return Tools.getBigDecimalValue(str, "0");
	}

	@JsonIgnore
	public BigDecimal getPriceVat()
	{
		BigDecimal vat = getVat();
		vat = (vat.divide(VALUE_OF_100)).add(VALUE_OF_1);

		return getPrice().multiply(vat);
	}

	@JsonIgnore
	public BigDecimal getPriceVat(HttpServletRequest request)
	{
		BigDecimal vat = getVat();
		vat = (vat.divide(VALUE_OF_100)).add(VALUE_OF_1);

		return getPrice(request).multiply(vat);
	}

	@JsonIgnore
	public int getQuantity()
	{
		 String str = "0";
		 try
		 {
			  str = BeanUtils.getProperty(this, Constants.getString("basketQuantityField"));
		 }
		 catch (Exception ex)
		 {
			 Logger.error(DocBasic.class, ex);
		 }
		 return(Tools.getIntValue(str, 0));
	}
	//============ Veci pre shop koniec ===========

	public Long getId()
	{
		return null;
	}

	public void setId(Long id)
	{
		//MUST IMPLEMENT
	}

	/**
	 * id dokumentu
	 * @return
	 */
	public int getDocId()
	{
		return 0; //NOSONAR
	}

	public void setDocId(int newDocId)
	{
		//MUST IMPLEMENT
	}

	/**
	 * last save date as timestamp
	 * @return
	 */
	public long getDateCreated()
	{
		if (dateCreated == null) return 0;
		return dateCreated.getTime();
	}

	public void setPublishStart(long newPublishStart)
	{
		if (newPublishStart == 0) return;
		this.publishStartDate = new Date(newPublishStart);
		setPublishStartString(Tools.formatDate(publishStartDate));
		setPublishStartTimeString(Tools.formatTime(publishStartDate));
	}

	public void setPublishEnd(long newPublishEnd)
	{
		if (newPublishEnd == 0) return;
		this.publishEndDate = new Date(newPublishEnd);
		setPublishEndString(Tools.formatDate(publishEndDate));
		setPublishEndTimeString(Tools.formatTime(publishEndDate));
	}

	/**
	 * meno autora dokumentu
	 * @return
	 */
	public String getAuthorName()
	{
		if (shouldLazyLoadAuthorInformation())
			loadAuthorInformation();
		String name = this.authorName;

		if (name == null) return "";
		return name;
	}

    /**
     * Vrati fotku pouzivatela, alebo defaultPhoto ak nema ziadnu zadanu
     * @param defaultPhoto
     * @return
     */
    public String getAuthorPhoto(String defaultPhoto)
    {
        if (shouldLazyLoadAuthorInformation())
            loadAuthorInformation();
        String photo = this.authorPhoto;

        if(Tools.isEmpty(photo)) return defaultPhoto;
        return GalleryDB.getImagePathOriginal(photo);
    }

	private boolean shouldLazyLoadAuthorInformation()
	{
		return Constants.getBoolean("docAuthorLazyLoad") && !lazyLoaded;
	}

	private void loadAuthorInformation()
	{
		UserDetails user = UsersDB.getUser(getAuthorId());
		if (user != null)
		{
			this.authorEmail = user.getEmail();
			this.authorName = user.getFullName();
			this.authorPhoto = user.getPhoto();
		}
		lazyLoaded = true;
	}

	// /**
	//  * prehladavatelne
	//  * @return
	//  */
	// public boolean isSearchable()
	// {
	// 	return searchable;
	// }

	public boolean isAvailable()
	{
		return available;
	}

	/**
	 * cachovat
	 * @return
	 */
	public boolean isCacheable()
	{
		return cacheable;
	}

	/**
	 * titulok
	 * @return
	 */
	public String getTitle()
	{
		if (title==null || title.length()==0)
		{
			title = "no name";
		}
		return title;
	}

	/**
	 * titulok v menu
	 * @return
	 */
	public String getNavbar()
	{
		if (navbar == null || navbar.length() < 1)
		{
			String navbarFromTitle = title;
			if (navbarFromTitle.contains("<") && navbarFromTitle.contains(">")) navbarFromTitle = Tools.html2text(navbarFromTitle);
			return navbarFromTitle;
		}
		else
		{
			return (navbar);
		}
	}

	/**
	 * formated last save date
	 * @return
	 */
	@JsonIgnore
	public String getDateCreatedString()
	{
		if (dateCreated != null && dateCreated.getTime()>0)
		{
			return(Tools.formatDate(dateCreated));
		}
		return "";
	}

	@JsonIgnore
	public String getLastUpdateDate()
	{
		return getDateCreatedString();
	}

	/**
	 * formated last save time
	 * @return
	 */
	@JsonIgnore
	public String getTimeCreatedString()
	{
		if (dateCreated != null && dateCreated.getTime()>0)
		{
			return(Tools.formatTime(dateCreated));
		}
		return("");
	}

	@JsonIgnore
	public String getLastUpdateTime()
	{
		return getTimeCreatedString();
	}

	public void setPasswordProtected(String passwordProtected)
	{
		if (Tools.isEmpty(passwordProtected) || ",".equals(passwordProtected))
		{
			this.passwordProtected = null;
		}
		else this.passwordProtected = passwordProtected;
	}

	public boolean isInUserGroup(int userGroupId)
	{
		if (Tools.isEmpty(passwordProtected)) return(false);
		StringTokenizer st = new StringTokenizer(passwordProtected, ",");
		while (st.hasMoreTokens())
		{
			int ugid = Tools.getIntValue(st.nextToken(), -1);
			if (ugid == userGroupId) return(true);
		}
		return(false);
	}


	/**
	 * to iste, ako htmlData
	 * @return
	 */
	@JsonIgnore
	public String getPerex()
	{
		return getHtmlData();
	}

	@JsonIgnore
	public String getPerexPre()
	{
		String perexPre = getHtmlData();
		if (perexPre != null && (perexPre.indexOf('<')==-1 && perexPre.indexOf('>')==-1))
		{
			return(sk.iway.iwcm.Tools.replace(perexPre, "\n", "\n<br/>"));
		}
		else
		{
			return(perexPre);
		}
	}

	public String getDocLink()
	{
		String link = this.docLink;
		if (link==null)
		{
			DocDB docDB = DocDB.getInstance();
			link = docDB.getDocLink(getDocId(), getExternalLink(), null);
			//docLink = "/showdoc.do?docid="+docId;
		}
		//odstan zapis s * na konci (nieco/*)
		if (link.indexOf('*')!=-1)
		{
			link = Tools.replace(link, "*", "");
		}
		return link;
	}

	@JsonIgnore
	public String getPerexImageSmall()
	{
		String ret = GalleryToolsForCore.getImagePathSmall(getPerexImage());

		if(ret == null) return "";
		return ret;
	}

	@JsonIgnore
	public String getPerexImageNormal()
	{
		String ret = GalleryToolsForCore.getImagePathNormal(getPerexImage());

		if(ret == null) return "";
		return ret;
	}

	@JsonIgnore
	public String getPerexImageOriginal()
	{
		String ret = GalleryToolsForCore.getImagePathOriginal(getPerexImage());

		if(ret == null) return "";
		return ret;
	}

	/**
	 * Vrati true ak sa stranka nachadza v zadanej perex skupine
	 * @param perexGroupId
	 * @return
	 */
	public boolean isInPerexGroup(int perexGroupId)
	{
		if (perexGroups == null || perexGroups.length<1) return false;

		for (int i=0; i<perexGroups.length; i++)
		{
			if (perexGroups[i].intValue()==perexGroupId) return(true);
		}
		return(false);
	}

	/**
	 * Vrati true ak sa stranka nachadza v zadanej perex skupine
	 * @param perexGroupName
	 * @return
	 */
	public boolean hasPerexGroup(String perexGroupName)
	{
		String perexGroupNames = getPerexGroupString();
		if(perexGroupNames == null)
		{
			return false;
		}
		return perexGroupNames.indexOf(perexGroupName) >= 0;
	}

	/**
	 * Vrati NAZVY (nie ID) perex skupin ako String oddeleny ciarkami	 *
	 */
	@JsonIgnore
	public String getPerexGroupString()
	{
		StringBuilder ret=null;
		DocDB docDB = DocDB.getInstance();
		try
		{
			if (perexGroups!=null && perexGroups.length>0)
			{
				int size = perexGroups.length;
				int i;
				for (i=0; i<size; i++)
				{
					String name = docDB.convertPerexGroupIdToName(perexGroups[i].intValue());
					if (Tools.isEmpty(name)) continue;
					if (ret==null) ret = new StringBuilder(name);
					else ret.append(",").append(name);
				}
			}
		}
		catch (Exception ex)
		{
			Logger.error(DocBasic.class, ex);
		}

		if(ret == null) return "";
		return(ret.toString());
	}

	/**
	 * Vrati ID perex skupin ako String oddeleny ciarkami
	 * @return
	 */
	@JsonIgnore
	public String getPerexGroupIdsString()
	{
		return getPerexGroupIdsString(false);
	}

	/**
	 * Vrati ID perex skupin ako String oddeleny ciarkami
	 * @param addStartEndComma - ak je true na zaciatok a koniec prida ciarku (ak nie je zoznam prazdny), je to tak kvoli DB vyhladavaniu cez LIKE %,cislo,%
	 * @return
	 */
	public String getPerexGroupIdsString(boolean addStartEndComma)
	{
		StringBuilder ret=null;
		try
		{
			if (perexGroups!=null && perexGroups.length>0)
			{
				int size = perexGroups.length;
				int i;
				for (i=0; i<size; i++)
				{
					if (ret==null)
					{
						if (addStartEndComma) ret = new StringBuilder(","+perexGroups[i].intValue());
						else ret = new StringBuilder(""+perexGroups[i].intValue());
					}
					else ret.append(",").append(perexGroups[i].intValue());
				}
			}
		}
		catch (Exception ex)
		{
			Logger.error(DocBasic.class, ex);
		}

		if (ret!=null && Tools.isNotEmpty(ret) && addStartEndComma) ret.append(",");

		if(ret == null) return "";
		return(ret.toString());
	}

	@JsonIgnore
	/**
	 * Vrati NAZVY (nie ID) perex skupin ako pole
	 * @return
	 */
	public String[] getPerexGroupNames()
	{
	    if (perexGroups==null) return new String[0];

		int size = perexGroups.length;
		String[] perexGroupNames = new String[size];
		int i;
		DocDB docDB = DocDB.getInstance();
		for (i=0; i<size; i++)
		{
			perexGroupNames[i]=docDB.convertPerexGroupIdToName(perexGroups[i].intValue());
			if (perexGroupNames[i] == null) perexGroupNames[i] = ""+perexGroups[i].intValue();
		}
		return perexGroupNames;
	}

	/**
	 * Returns list of perex groups as full PerexGroupBean objects
	 * @return
	 */
	@JsonIgnore
	public List<PerexGroupBean> getPerexGroupsList()
	{
		List<PerexGroupBean> perexGroupsList = new ArrayList<>();
		if (perexGroups==null) return perexGroupsList;

		int size = perexGroups.length;
		DocDB docDB = DocDB.getInstance();
		int i;
		for (i=0; i<size; i++)
		{
			PerexGroupBean pb = docDB.getPerexGroup(perexGroups[i].intValue(), null);
			if (pb != null) perexGroupsList.add(pb);
		}
		return perexGroupsList;
	}

	/**
	 * Nastavy perex skupiny podla retazca oddeleneho ciarkami, ktory odsahuje ID (nie NAZOV)
	 * @param perexGroupIdsString
	 */
	public void setPerexGroupString(String perexGroupIdsString)
	{
		if (perexGroupIdsString == null)
		{
			perexGroups = new Integer[0];
			return;
		}
		StringTokenizer st = new StringTokenizer(perexGroupIdsString, ",");
		try
		{
			perexGroups = new Integer[st.countTokens()];
			int i=0;
			while (st.hasMoreTokens())
			{
				perexGroups[i++] = Tools.getIntValue(st.nextToken().trim(), 0);
			}
		}
		catch (Exception ex)
		{
			Logger.error(DocBasic.class, ex);
		}
	}
	/**
	 * zobrazit v menu
	 * @return
	 */
	public boolean isShowInMenu()
	{
		return showInMenu;
	}

	public void setEventDate(long eventDate)
	{
		if (eventDate == 0) return;
		this.eventDateDate = new Date(eventDate);
		setEventDateString(Tools.formatDate(eventDateDate));
		setEventTimeString(Tools.formatTime(eventDateDate));
	}

	/**
	 * email autora dokumentu
	 */
	public String getAuthorEmail()
	{
		if (shouldLazyLoadAuthorInformation())
			loadAuthorInformation();
		String email = this.authorEmail;
		if (email == null) return "";
		return email;
	}

	@JsonIgnore
	public int getSyncId()
	{
		return this.syncId;
	}

	@JsonIgnore
	public void setSyncId(int syncId)
	{
		if (syncId == 0) return;
		this.syncId = syncId;
	}

	@JsonIgnore
	public int getSyncStatus()
	{
		return this.syncStatus;
	}

	@JsonIgnore
	public void setSyncStatus(int syncStatus)
	{
		if (syncStatus == 0) return;
		this.syncStatus = syncStatus;
	}

	public void setLogonPageDocId(int logonPageDocId)
	{
		if (logonPageDocId == 0) return;
		this.logonPageDocId = logonPageDocId;
	}

	@JsonIgnore
	public int getForumCount()
	{
		return this.forumCount;
	}

	@JsonIgnore
	public void setForumCount(int forumCount)
	{
		if (forumCount == 0) return;
		this.forumCount = forumCount;
	}

	/**
	 * Zistenie ci stranku mozeme povazovat za novu, zmenenu alebo nemodifikovanu
	 *
	 * nova je taka, ktora nema historiu starsiu ako zadany pocet dni
	 * zmenena je taka, kde doslo za zadany pocet dni k zmene
	 *
	 * @return 0=bez zmeny, 1=nova, 2=zmenena
	 */
	@JsonIgnore
	public int getPageNewChangedStatus()
	{
		return(DocDB.getPageNewChangedStatus(getDocId(), 1, 7));
	}

	/**
	 * Zistenie ci stranku mozeme povazovat za novu, zmenenu alebo nemodifikovanu
	 *
	 * nova je taka, ktora nema historiu starsiu ako zadany pocet dni
	 * zmenena je taka, kde doslo za zadany pocet dni k zmene
	 *
	 * @param minDaysNotChanged - pocet dni, pocas ktorych nesmelo dojst k zmene
	 * @param maxDaysTestChanged - maximalny pocet dni, ktore sa testuju na zmenu (ak v tomto rozsahu nie je ziadna zmena, je dokument bezo zmeny)
	 * @return 0=bez zmeny, 1=nova, 2=zmenena
	 */
	@JsonIgnore
	public int getPageNewChangedStatus(int minDaysNotChanged, int maxDaysTestChanged)
	{
		return(DocDB.getPageNewChangedStatus(getDocId(), minDaysNotChanged, maxDaysTestChanged));
	}

	public static BigDecimal parse(String str, HttpServletRequest request)
	{
		if (str == null)
			return VALUE_OF_MINUS_1;

		BigDecimal min = VALUE_OF_0;
		BigDecimal firstUserGroupPrice = VALUE_OF_0;
		String[] tokens = str.split("\\;");
		BigDecimal[] prices = new BigDecimal[tokens.length];
		Identity user = null;
		if (request != null) user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
		if (user != null)
		{
			if (Constants.getBoolean("basketDiscountEnabled") || tokens != null && tokens.length >= 1)
			{
				min = Tools.getBigDecimalValue(tokens[0], "-1");
				prices[0] = Tools.getBigDecimalValue(tokens[0].substring(tokens[0].indexOf(':') + 1, tokens[0].length()), "-1");
				int i = 0;
				for (String s : tokens)
				{
					if (i == 0)
					{
						i++;
						continue;
					}
					if (s.indexOf(':') != -1)
					{
						if (user.isInUserGroup(Tools.getIntValue(s.substring(0, s.indexOf(':')), -1)))
						{
							prices[i] = Tools.getBigDecimalValue(s.substring(s.indexOf(':') + 1, s.length()), "-1");
							if (firstUserGroupPrice.compareTo(VALUE_OF_0) == 0) firstUserGroupPrice = prices[i];
						}
						else
						{
							prices[i] = VALUE_OF_MINUS_1;
						}
					}
					i++;
				}

				if (firstUserGroupPrice.compareTo(VALUE_OF_0) > 0 && Constants.getBoolean("basketUseFirstUserGroupPrice")) {
					min = firstUserGroupPrice;
				}
				else {
					for (BigDecimal p : prices)//najde najnizsiu cenu
					{
						if (min.compareTo(p) >= 0 && p.equals(VALUE_OF_MINUS_1)==false)
						{
							min = p;
						}
					}
				}
				if (user.getFieldE() != null)//aplikuje zlavu
				{
					BigDecimal zlava = Tools.getBigDecimalValue(user.getFieldE(), "0");
					//AKA (min / 100) * (100 - zlava)
					min = (min.divide(VALUE_OF_100)).multiply( (VALUE_OF_100.subtract(zlava)) );
				}
			}
		}
		else
		{
			if (tokens != null && tokens.length >= 1)
			{
				min = Tools.getBigDecimalValue(tokens[0], "0");
			}
		}
		return min;
	}

	public static Comparator<DocDetails> getTitleComparator()
	{
		return new Comparator<DocDetails>(){
			@Override
			public int compare(DocDetails doc1, DocDetails doc2)
			{
				if (doc1 == null)
					return -1;
				if (doc2 == null)
					return 1;
				return doc1.getTitle().compareTo(doc2.getTitle());
			}
		};
	}

	public boolean isDisableAfterEnd()
	{
		return disableAfterEnd;
	}

	public static DocDetails getById(int docId)
	{
		return DocDB.getInstance().getDoc(docId);
	}

	@JsonIgnore
	public GroupDetails getGroup()
	{
		return GroupsDB.getInstance().getGroup(groupId);
	}

	@JsonIgnore
	public int getViewsTotal()
	{
		return this.viewsTotal;
	}

	@JsonIgnore
	public void setViewsTotal(int viewsTotal)
	{
		if (viewsTotal == 0) return;
		this.viewsTotal = viewsTotal;
	}

	 /**
	  * datum konania
	  * @return
	  */
	 @JsonIgnore
	 public String getEventDateString()
	 {
	 	return Tools.formatDate(eventDateDate);
	 }

	 /**
	  * cas konania
	  * @return
	  */
	  @JsonIgnore
	 public String getEventTimeString()
	 {
	 	return Tools.formatTime(eventDateDate);
	 }

	 @JsonIgnore
	 public String getPublishEndString()
	 {
		if(this.publishEndString == null) return "";
	 	else return this.publishEndString;
	 }

	 @JsonIgnore
	 public String getPublishEndTimeString()
	 {
		if(this.publishEndTimeString == null) return "";
	 	else return this.publishEndTimeString;
	 }

	 @JsonIgnore
	 public String getPublishStartString()
	 {
		if(this.publishStartString == null) return "";
	 	else return this.publishStartString;
	 }

	 @JsonIgnore
	 public String getPublishStartStringExtra()
	 {
		if(this.publishStartStringExtra == null) return "";
	 	else return this.publishStartStringExtra;
	 }

	 @JsonIgnore
	 public String getPublishStartTimeString()
	 {
		if(this.publishStartTimeString == null) return "";
	 	else return this.publishStartTimeString;
	 }

	 @JsonIgnore
	 public String getSyncDefaultForGroupId()
	 {
		if(this.syncDefaultForGroupId == null) return "";
	 	else return this.syncDefaultForGroupId;
	 }

	@JsonIgnore
	public String getSyncRemotePath()
	{
		if(this.syncRemotePath == null) return "";
		return this.syncRemotePath;
	}

	public boolean isRequireSsl()
	{
		return requireSsl;
	}

	public void setRequireSsl(boolean requireSsl)
	{
		this.requireSsl = requireSsl;
	}

	public DocEditorFields getEditorFields() {
		return editorFields;
	}

	public void setEditorFields(DocEditorFields editorFields) {
		this.editorFields = editorFields;
	}

	/**
	 * Vrati cestu spojenu s GroupDetails.getFullPath, pouziva sa vo WJ9 Datatable
	 */
	@Transient
	@DataTableColumn(
		inputType = DataTableColumnType.TEXT,
		title = "sync.path",
		hiddenEditor = true,
		visible = false,
		orderable = false
	)
	private String fullPath = null;

	public String getFullPath() {
		if (fullPath == null) {
			//ak nie je fullPath nastavene vygeneruj cestu podla priecinka
			GroupDetails grp = GroupsDB.getInstance().getGroup(getGroupId());
			if (grp != null) return grp.getFullPath()+"/"+getTitle();
			return "/"+getTitle();
		}
		return fullPath;
	}

	public void setFullPath(String fullPath)
	{
		this.fullPath = fullPath;
	}

	/**
	 * @deprecated Pouzivajte verziu ktora vrati Integer
	 */
	@JsonIgnore
	@Deprecated
	public String[] getPerexGroup() {
		return Tools.getTokens(getPerexGroupIdsString(), ",");
	}

	public void setPerexGroup(String[] perexGroup) {
		StringBuilder groups = new StringBuilder();

		for(String pG : perexGroup) {
			groups.append(pG).append(",");
		}

		setPerexGroupString(groups.toString());
	}

	public Integer[] getPerexGroups() {
		return this.perexGroups;
	}

	public void setPerexGroups(Integer[] perexGroups) {
		//perexGroups[0]==null vrati DT pri zvoleni ziadnej polozky
		if (perexGroups == null || perexGroups.length<1 || (perexGroups.length==1 && perexGroups[0]==null )) this.perexGroups = new Integer[0];
		else this.perexGroups = perexGroups;
	}

	// !!! - Pridane gettery/settery z Integer na int kvoli spatnej kompatibilite
	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public int getAuthorId() {

		return authorId;
	}

	public void setAuthorId(int newAuthorId)
	{
		if (newAuthorId == 0) return;
		this.authorId = newAuthorId;
	}

	public int getTempId() {
		return tempId;
	}

	public void setTempId(int tempId) {
		this.tempId = tempId;
	}

	public int getSortPriority() {
		return sortPriority;
	}

	public void setSortPriority(int sortPriority) {
		this.sortPriority = sortPriority;
	}

	public int getHeaderDocId() {
		return headerDocId;
	}

	public void setHeaderDocId(int headerDocId)
	{
		this.headerDocId = headerDocId;
	}

	public int getFooterDocId() {
		return footerDocId;
	}

	public void setFooterDocId(int footerDocId)
	{
		this.footerDocId = footerDocId;
	}

	public int getMenuDocId() {
		return menuDocId;
	}

	public void setMenuDocId(int menuDocId)
	{
		this.menuDocId = menuDocId;
	}

	public int getRightMenuDocId() {
		return this.rightMenuDocId;
	}

	public void setRightMenuDocId(int rightMenuDocId)
	{
		this.rightMenuDocId = rightMenuDocId;
	}

	// !!! Vygenerovane geetry/settery pretoze pomocou lomboku sa neprenesu na childa
	public void setTitle(String title) {
		if (isEmpty(title)) return;
		this.title = title;
	}

	public void setNavbar(String navbar) {
		if (isEmpty(navbar)) return;
		this.navbar = navbar;
	}

	public String getVirtualPath() {
		if(virtualPath == null) return "";
		return virtualPath;
	}

	public void setVirtualPath(String virtualPath) {
		if (virtualPath == null) return;
		this.virtualPath = virtualPath;
	}

	public String getExternalLink() {
		if(externalLink == null) return "";
		return externalLink;
	}

	public void setExternalLink(String externalLink) {
		this.externalLink = externalLink;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public String getPasswordProtected() {
		if(passwordProtected == null) return "";
		return passwordProtected;
	}

	public void setDisableAfterEnd(boolean disableAfterEnd) {
		this.disableAfterEnd = disableAfterEnd;
	}

	public boolean isPublishAfterStart() {
		return publishAfterStart;
	}

	public void setPublishAfterStart(boolean publishAfterStart) {
		this.publishAfterStart = publishAfterStart;
	}

	public boolean isLazyLoaded() {
		return lazyLoaded;
	}

	public void setLazyLoaded(boolean lazyLoaded) {
		this.lazyLoaded = lazyLoaded;
	}

	public String getFieldA() {
		if(fieldA == null) return "";
		return fieldA;
	}

	public void setFieldA(String fieldA) {
		if (isEmpty(fieldA)) return;
		this.fieldA = fieldA;
	}

	public String getFieldB() {
		if(fieldB == null) return "";
		return fieldB;
	}

	public void setFieldB(String fieldB) {
		if (isEmpty(fieldB)) return;
		this.fieldB = fieldB;
	}

	public String getFieldC() {
		if(fieldC == null) return "";
		return fieldC;
	}

	public void setFieldC(String fieldC) {
		if (isEmpty(fieldC)) return;
		this.fieldC = fieldC;
	}

	public String getFieldD() {
		if(fieldD == null) return "";
		return fieldD;
	}

	public void setFieldD(String fieldD) {
		if (isEmpty(fieldD)) return;
		this.fieldD = fieldD;
	}

	public String getFieldE() {
		if(fieldE == null) return "";
		return fieldE;
	}

	public void setFieldE(String fieldE) {
		if (isEmpty(fieldE)) return;
		this.fieldE = fieldE;
	}

	public String getFieldF() {
		if(fieldF == null) return "";
		return fieldF;
	}

	public void setFieldF(String fieldF) {
		if (isEmpty(fieldF)) return;
		this.fieldF = fieldF;
	}

	public String getFieldG() {
		if(fieldG == null) return "";
		return fieldG;
	}

	public void setFieldG(String fieldG) {
		if (isEmpty(fieldG)) return;
		this.fieldG = fieldG;
	}

	public String getFieldH() {
		if(fieldH == null) return "";
		return fieldH;
	}

	public void setFieldH(String fieldH) {
		if (isEmpty(fieldH)) return;
		this.fieldH = fieldH;
	}

	public String getFieldI() {
		if(fieldI == null) return "";
		return fieldI;
	}

	public void setFieldI(String fieldI) {
		if (isEmpty(fieldI)) return;
		this.fieldI = fieldI;
	}

	public String getFieldJ() {
		if(fieldJ == null) return "";
		return fieldJ;
	}

	public void setFieldJ(String fieldJ) {
		if (isEmpty(fieldJ)) return;
		this.fieldJ = fieldJ;
	}

	public String getFieldK() {
		if(fieldK == null) return "";
		return fieldK;
	}

	public void setFieldK(String fieldK) {
		if (isEmpty(fieldK)) return;
		this.fieldK = fieldK;
	}

	public String getFieldL() {
		if(fieldL == null) return "";
		return fieldL;
	}

	public void setFieldL(String fieldL) {
		if (isEmpty(fieldL)) return;
		this.fieldL = fieldL;
	}

	public String getFieldM() {
		if(fieldM == null) return "";
		return fieldM;
	}

	public void setFieldM(String fieldM) {
		if (isEmpty(fieldM)) return;
		this.fieldM = fieldM;
	}

	public String getFieldN() {
		if(fieldN == null) return "";
		return fieldN;
	}

	public void setFieldN(String fieldN) {
		if (isEmpty(fieldN)) return;
		this.fieldN = fieldN;
	}

	public String getFieldO() {
		if(fieldO == null) return "";
		return fieldO;
	}

	public void setFieldO(String fieldO) {
		if (isEmpty(fieldO)) return;
		this.fieldO = fieldO;
	}

	public String getFieldP() {
		if(fieldP == null) return "";
		return fieldP;
	}

	public void setFieldP(String fieldP) {
		if (isEmpty(fieldP)) return;
		this.fieldP = fieldP;
	}

	public String getFieldQ() {
		if(fieldQ == null) return "";
		return fieldQ;
	}

	public void setFieldQ(String fieldQ) {
		if (isEmpty(fieldQ)) return;
		this.fieldQ = fieldQ;
	}

	public String getFieldR() {
		if(fieldR == null) return "";
		return fieldR;
	}

	public void setFieldR(String fieldR) {
		if (isEmpty(fieldR)) return;
		this.fieldR = fieldR;
	}

	public String getFieldS() {
		if(fieldS == null) return "";
		return fieldS;
	}

	public void setFieldS(String fieldS) {
		if (isEmpty(fieldS)) return;
		this.fieldS = fieldS;
	}

	public String getFieldT() {
		if(fieldT == null) return "";
		return fieldT;
	}

	public void setFieldT(String fieldT) {
		if (isEmpty(fieldT)) return;
		this.fieldT = fieldT;
	}

	public String getHtmlHead() {
		if(htmlHead == null) return "";
		return htmlHead;
	}

	public void setHtmlHead(String htmlHead) {
		if (isEmpty(htmlHead)) return;
		this.htmlHead = htmlHead;
	}

	public long getPublishStart() {
		if (publishStartDate == null) return 0;
		return publishStartDate.getTime();
	}

	public long getPublishEnd() {
		if (publishEndDate == null) return 0;
		return publishEndDate.getTime();
	}

	public long getEventDate() {
		if (eventDateDate == null) return 0;
		return eventDateDate.getTime();
	}

	public String getHtmlData() {
		if(htmlData == null) return "";
		return htmlData;
	}

	public void setHtmlData(String htmlData) {
		if (isEmpty(htmlData)) return;
		this.htmlData = htmlData;
	}

	public String getPerexPlace() {
		if(perexPlace == null) return "";
		return perexPlace;
	}

	public void setPerexPlace(String perexPlace) {
		if (isEmpty(perexPlace)) return;
		this.perexPlace = perexPlace;
	}

	public String getPerexImage() {
		if(perexImage == null) return "";
		return perexImage;
	}

	public void setPerexImage(String perexImage) {
		if (isEmpty(perexImage)) return;
		this.perexImage = perexImage;
	}

	public void setAuthorName(String authorName) {
		if (isEmpty(authorName)) return;
		this.authorName = authorName;
	}

	public void setDocLink(String docLink) {
		if (isEmpty(docLink)) return;
		this.docLink = docLink;
	}

	public String getData() {
		if(data == null) return "";
		return data;
	}

	public void setData(String data) {
		if (isEmpty(data)) return;
		this.data = data;
	}

	public void setEventDateString(String eventDateString) {
		if (Tools.isEmpty(eventDateString)) {
			eventDateDate = null;
		}
		this.eventDateDate = new Date(DB.getTimestamp(eventDateString));
	}

	public void setEventTimeString(String eventTimeString) {
		if (Tools.isEmpty(eventTimeString)) return;
		this.eventDateDate = new Date(DB.getTimestamp(getEventDateString(), eventTimeString));
	}

	public String getTempName() {
		if(tempName == null) return "";
		return tempName;
	}

	public void setTempName(String tempName) {
		if (isEmpty(tempName)) return;
		this.tempName = tempName;
	}

	public void setPublishEndString(String publishEndString) {
		if (isEmpty(publishEndString)) return;
		this.publishEndString = publishEndString;
	}

	public void setPublishEndTimeString(String publishEndTimeString) {
		if (isEmpty(publishEndTimeString)) return;
		this.publishEndTimeString = publishEndTimeString;
	}

	public void setPublishStartString(String publishStartString) {
		if (isEmpty(publishStartString)) return;
		this.publishStartString = publishStartString;
	}

	public void setPublishStartStringExtra(String publishStartStringExtra) {
		if (isEmpty(publishStartStringExtra)) return;
		this.publishStartStringExtra = publishStartStringExtra;
	}

	public void setPublishStartTimeString(String publishStartTimeString) {
		if (isEmpty(publishStartTimeString)) return;
		this.publishStartTimeString = publishStartTimeString;
	}

	public void setSyncDefaultForGroupId(String syncDefaultForGroupId) {
		if (isEmpty(syncDefaultForGroupId)) return;
		this.syncDefaultForGroupId = syncDefaultForGroupId;
	}

	public void setSyncRemotePath(String syncRemotePath) {
		if (isEmpty(syncRemotePath)) return;
		this.syncRemotePath = syncRemotePath;
	}

	public int getLogonPageDocId() {
		return logonPageDocId;
	}

	public String getFileName() {
		if(fileName == null) return "";
		return fileName;
	}

	public void setFileName(String fileName) {
		if (isEmpty(fileName)) return;
		this.fileName = fileName;
	}

	public void setAuthorEmail(String authorEmail) {
		if (isEmpty(authorEmail)) return;
		this.authorEmail = authorEmail;
	}

	public String getAuthorPhoto() {
		if(authorPhoto == null) return "";
		return authorPhoto;
	}

	public void setAuthorPhoto(String authorPhoto) {
		if (isEmpty(authorPhoto)) return;
		this.authorPhoto = authorPhoto;
	}

	//Special date - long anotations
	public void setDateCreated(long dateCreated)
	{
		if (dateCreated == 0) this.dateCreated = null;
		else this.dateCreated = new Date(dateCreated);
	}

	//special Date objekty
	public Date getPublishStartDate() {
		return publishStartDate;
	}

	public void setPublishStartDate(Date publishStartDate) {
		this.publishStartDate = publishStartDate;
	}

	public Date getPublishEndDate() {
		return publishEndDate;
	}

	public void setPublishEndDate(Date publishEndDate) {
		this.publishEndDate = publishEndDate;
	}

	public Date getEventDateDate() {
		return eventDateDate;
	}

	public void setEventDateDate(Date eventDateDate) {
		this.eventDateDate = eventDateDate;
	}

	public String getDataAsc() {
		if(dataAsc == null) return "";
		return dataAsc;
	}

	public void setDataAsc(String dataAsc) {
		if (isEmpty(dataAsc)) {
			if (this.dataAsc == null) this.dataAsc = "";
			return;
		}
		this.dataAsc = dataAsc;
	}

	public Boolean getUrlInheritGroup() {
		return urlInheritGroup;
	}

	public void setUrlInheritGroup(Boolean urlInheritGroup) {
		this.urlInheritGroup = urlInheritGroup;
	}

	public Boolean getGenerateUrlFromTitle() {
		return generateUrlFromTitle;
	}

	public void setGenerateUrlFromTitle(Boolean generateUrlFromTitle) {
		this.generateUrlFromTitle = generateUrlFromTitle;
	}

	public String getEditorVirtualPath() {
		return editorVirtualPath;
	}

	public void setEditorVirtualPath(String editorVirtualPath) {
		this.editorVirtualPath = editorVirtualPath;
	}

	//Doplnene volne polia - ticket 54205 - prav cast
	public int getTempFieldADocId() {
		return tempFieldADocId;
	}

	public void setTempFieldADocId(int tempFieldADocId) {
		this.tempFieldADocId = tempFieldADocId;
	}

	public int getTempFieldBDocId() {
		return tempFieldBDocId;
	}

	public void setTempFieldBDocId(int tempFieldBDocId) {
		this.tempFieldBDocId = tempFieldBDocId;
	}

	public int getTempFieldCDocId() {
		return tempFieldCDocId;
	}

	public void setTempFieldCDocId(int tempFieldCDocId) {
		this.tempFieldCDocId = tempFieldCDocId;
	}

	public int getTempFieldDDocId() {
		return tempFieldDDocId;
	}

	public void setTempFieldDDocId(int tempFieldDDocId) {
		this.tempFieldDDocId = tempFieldDDocId;
	}

	//Doplnene volne polia - ticket 54205 - druha cast

	//Not logged user
	public void setShowInMenu(boolean showInMenu) {
		this.showInMenu = showInMenu;
	}

	public Boolean getShowInNavbar() {
		return showInNavbar;
	}

	public void setShowInNavbar(Boolean showInNavbar) {
		this.showInNavbar = showInNavbar;
	}

	public Boolean getShowInSitemap() {
		return showInSitemap;
	}

	public void setShowInSitemap(Boolean showInSitemap) {
		this.showInSitemap = showInSitemap;
	}

	//Logged user
	public Boolean getLoggedShowInMenu() {
		return loggedShowInMenu;
	}

	public void setLoggedShowInMenu(Boolean loggedShowInMenu) {
		this.loggedShowInMenu = loggedShowInMenu;
	}

	public Boolean getLoggedShowInNavbar() {
		return loggedShowInNavbar;
	}

	public void setLoggedShowInNavbar(Boolean loggedShowInNavbar) {
		this.loggedShowInNavbar = loggedShowInNavbar;
	}

	public Boolean getLoggedShowInSitemap() {
		return loggedShowInSitemap;
	}

	public void setLoggedShowInSitemap(Boolean loggedShowInSitemap) {
		this.loggedShowInSitemap = loggedShowInSitemap;
	}

	/**
	 * Vrati true, ak sa polozka ma zobrazit v menu (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param request
	 * @return
	 */
	public boolean isShowInMenu(HttpServletRequest request) {
		Identity user = UsersDB.getCurrentUser(request);
		if(user == null || loggedShowInMenu == null){
			return showInMenu;
		}
		return loggedShowInMenu;
	}

	/**
	 * Vrati true, ak sa polozka ma zobrazit v navigacnej liste (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param request
	 * @return
	 */
	public boolean isShowInNavbar(HttpServletRequest request) {
		Identity user = UsersDB.getCurrentUser(request);
		if(user == null || loggedShowInNavbar==null) {
			if(showInNavbar == null) return isShowInMenu();
			return showInNavbar;
		}

		return loggedShowInNavbar;
	}

	/**
	 * Vrati true, ak sa polozka ma zobrazit v mape stranok (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param request
	 * @return
	 */
	public boolean isShowInSitemap(HttpServletRequest request) {
		Identity user = UsersDB.getCurrentUser(request);
		if(user == null || loggedShowInSitemap==null) {
			if(showInSitemap == null) return showInMenu;
			return showInSitemap;
		}

		return loggedShowInSitemap;
	}

	@Override
	public String toString() {
		return getFullPath()+", ID => "+getDocId();
	}
}
