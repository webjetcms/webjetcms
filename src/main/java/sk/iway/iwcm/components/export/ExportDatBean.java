package sk.iway.iwcm.components.export;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

/**
 * ExportDatBean.java
 *
 * Ticket: Export dat (#16902)
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @author $Author: mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 05.11.2014 13:48:15
 * @modified $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name = "export_dat")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_EXPORT)
public class ExportDatBean extends ActiveRecordRepository implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator = "WJGen_export_dat")
	@TableGenerator(name = "WJGen_export_dat", pkColumnValue = "export_dat")
	@Column(name = "export_dat_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@Column(name = "url_address")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.export.urlAddress",
        tab="basic"
    )
	private String urlAddress;

	@Column(name = "format")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.export.format",
        tab="basic"
    )
	private String format;

	@Column(name = "number_items")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.export.numberItems",
        tab="basic"
    )
	private Integer numberItems;

	@Column(name = "group_ids")
	private String groupIds;

	@Column(name = "expand_group_ids")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.export.expandGroupIds",
        tab="filter"
    )
	private boolean expandGroupIds;

	@Column(name = "publish_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.export.publishType",
        tab="filter",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.news.PUBLISH_NEW", value = "new"),
                    @DataTableColumnEditorAttr(key = "components.news.PUBLISH_OLD", value = "old"),
                    @DataTableColumnEditorAttr(key = "components.news.PUBLISH_ALL", value = "all"),
                    @DataTableColumnEditorAttr(key = "components.news.PUBLISH_NEXT", value = "next")
                }
            )
        }
    )
	private String publishType;

	@Column(name = "perex_group")
	private String perexGroup;

	@Column(name = "no_perex_check")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.news.noPerexCheck",
        tab="filter"
    )
	private boolean noPerexCheck;

	@Column(name = "order_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.export.orderType",
        tab="sort",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_PRIORITY", value = "priority"),
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_DATE", value = "date"),
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_EVENT_DATE", value = "eventDate"),
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_SAVE_DATE", value = "saveDate"),
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_TITLE", value = "title"),
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_PLACE", value = "place"),
                    @DataTableColumnEditorAttr(key = "components.news.ORDER_ID", value = "id"),

                }
            )
        }
    )
	private String orderType;

	@Column(name = "asc_order")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.export.asc",
        tab="sort"
    )
	private boolean asc;

    @Transient
    @DataTableColumnNested
    private ExportDatEditorFields editorFields = null;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public int getExportDatId() {
        if(this.id == null) return 0;
        return id.intValue();
    }

    @JsonIgnore
    public void setExportDatId(int exportDatId) {
        this.id = Long.valueOf(exportDatId);
    }

    public String getUrlAddress() {
        return urlAddress;
    }

    public void setUrlAddress(String urlAddress) {
        this.urlAddress = urlAddress;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getNumberItems() {
        return numberItems;
    }

    public void setNumberItems(Integer numberItems) {
        this.numberItems = numberItems;
    }

    public String getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(String groupIds) {
        this.groupIds = groupIds;
    }

    public boolean isExpandGroupIds() {
        return expandGroupIds;
    }

    public boolean getExpandGroupIds() { //NOSONAR
        return expandGroupIds;
    }

    public void setExpandGroupIds(boolean expandGroupIds) {
        this.expandGroupIds = expandGroupIds;
    }

    public String getPublishType() {
        return publishType;
    }

    public void setPublishType(String publishType) {
        this.publishType = publishType;
    }

    public String getPerexGroup() {
        return perexGroup;
    }

    public void setPerexGroup(String perexGroup) {
        this.perexGroup = perexGroup;
    }

    public boolean isNoPerexCheck() {
        return noPerexCheck;
    }

    public boolean getNoPerexCheck() { //NOSONAR
        return noPerexCheck;
    }

    public void setNoPerexCheck(boolean noPerexCheck) {
        this.noPerexCheck = noPerexCheck;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public boolean isAsc() {
        return asc;
    }

    public boolean getAsc() { //NOSONAR
        return asc;
    }

    public void setAsc(boolean asc) {
        this.asc = asc;
    }

    public ExportDatEditorFields getEditorFields() {
        return editorFields;
    }

    public void setEditorFields(ExportDatEditorFields editorFields) {
        this.editorFields = editorFields;
    }


}