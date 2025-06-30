package sk.iway.iwcm.system.cache;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


/**
 * PersistentCacheBean.java - >>>POPIS MA<<<<
 *
 * @author $Author: jeeff $
 * @version $Revision: 1.3 $
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @created Date: 20.01.2012 11:08:13
 * @modified $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name = "cache")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_PERSISTENT_CACHE)
public class PersistentCacheBean extends ActiveRecordRepository implements Serializable {
    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(generator = "WJGen_cache")
    @TableGenerator(name = "WJGen_cache", pkColumnValue = "cache")
    @Column(name = "cache_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long cacheId;

    @Column(name = "data_type")
    int dataType;

    @Column(name = "data_value")
    @Convert(converter = AllowHtmlAttributeConverter.class)
    @DataTableColumn(
            inputType = {DataTableColumnType.OPEN_EDITOR},
            renderFormat = "dt-format-text-wrap",
            title = "[[#{components.data_deleting.name}]]",
            tab = "basicTab"
    )
    String dataValue;

    @Transient
    @DataTableColumn(
            renderFormat = "td-format-number",
            title = "[[#{components.memory_cleanup.persistent_cache_objects.size}]]",
            tab = "basicTab",
            editor = @DataTableColumnEditor(
                    type = "text",
                    attr = @DataTableColumnEditorAttr(
                            key = "disabled",
                            value = "disabled"
                    )
            )
    )
    private int resultSize;

    @Lob
    @Column(name = "data_result")
    @Convert(converter = AllowHtmlAttributeConverter.class)
    @DataTableColumn(
            hidden = true,
            tab = "descriptionTab",
            editor = @DataTableColumnEditor(
                    type = "textarea",
                    attr = @DataTableColumnEditorAttr(
                            key = "class",
                            value = "textarea-code"
                    )
            )
    )
    String dataResult;

    @Column(name = "refresh_minutes")
    @DataTableColumn(
            renderFormat = "dt-format-number",
            title = "[[#{components.data_deleting.persistentCache.refreshMinutes}]]",
            tab = "basicTab"
    )
    int refreshMinutes;

    @Column(name = "next_refresh")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
            renderFormat = "dt-format-date-time",
            title = "[[#{components.memory_cleanup.persistent_cache_objects.next_update}]]",
            tab = "basicTab"
    )
    Date nextRefresh;

    public int getResultSize() {
        if (dataResult == null) return 0;
        resultSize = dataResult.length();
        return resultSize;
    }

    public void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

    @Override
    public void setId(Long id) {
        setCacheId(id);
    }

    @Override
    public Long getId() {
        return getCacheId();
    }

    public Long getCacheId() {
        return cacheId;
    }

    public void setCacheId(Long cacheId) {
        this.cacheId = cacheId;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public int getRefreshMinutes() {
        return refreshMinutes;
    }

    public void setRefreshMinutes(int refreshMinutes) {
        this.refreshMinutes = refreshMinutes;
    }

    public Date getNextRefresh() {
        return nextRefresh;
    }

    public void setNextRefresh(Date nextRefresh) {
        this.nextRefresh = nextRefresh;
    }

    public String getDataResult() {
        return dataResult;
    }

    public void setDataResult(String dataResult) {
        this.dataResult = dataResult;
    }


}