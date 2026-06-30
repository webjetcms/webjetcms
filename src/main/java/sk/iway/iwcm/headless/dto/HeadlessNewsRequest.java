package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request DTO for the headless news listing endpoint.
 * Maps to NewsActionBean/NewsQuery parameters for core news listing behavior.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeadlessNewsRequest {

    /** Group IDs to filter news by (maps to NewsActionBean.groupIds). */
    private List<Integer> groupIds;

    /** Whether to include subgroups (maps to NewsActionBean.alsoSubGroups). */
    private Boolean alsoSubGroups = false;

    /** Publish type filter: new, old, all, next, valid (maps to NewsActionBean.publishType). */
    private String publishType = "new";

    /** Order field: date, title, priority, id (maps to NewsActionBean.order). */
    private String order = "date";

    /** Sort direction: true = ascending, false = descending (maps to NewsActionBean.ascending). */
    private Boolean ascending = false;

    /** Whether to enable pagination (maps to NewsActionBean.paging). */
    private Boolean paging = false;

    /** Results per page (maps to NewsActionBean.pageSize). */
    private Integer pageSize = 10;

    /** Result offset (maps to NewsActionBean.offset). */
    private Integer offset = 0;

    /** Whether perex (summary image) is required (maps to NewsActionBean.perexNotRequired). */
    private Boolean perexNotRequired = false;

    /** Whether to load document data (maps to NewsActionBean.loadData). */
    private Boolean loadData = false;

    /** Whether to filter duplicity (maps to NewsActionBean.checkDuplicity). */
    private Boolean checkDuplicity = false;

    /** Perex group IDs to include (maps to NewsActionBean.perexGroup). */
    private List<Integer> perexGroup;

    /** Perex group IDs to exclude (maps to NewsActionBean.perexGroupNot). */
    private List<Integer> perexGroupNot;

    public HeadlessNewsRequest() {
    }

    public List<Integer> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Integer> groupIds) {
        this.groupIds = groupIds;
    }

    public Boolean getAlsoSubGroups() {
        return alsoSubGroups;
    }

    public void setAlsoSubGroups(Boolean alsoSubGroups) {
        this.alsoSubGroups = alsoSubGroups;
    }

    public String getPublishType() {
        return publishType;
    }

    public void setPublishType(String publishType) {
        this.publishType = publishType;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Boolean getAscending() {
        return ascending;
    }

    public void setAscending(Boolean ascending) {
        this.ascending = ascending;
    }

    public Boolean getPaging() {
        return paging;
    }

    public void setPaging(Boolean paging) {
        this.paging = paging;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Boolean getPerexNotRequired() {
        return perexNotRequired;
    }

    public void setPerexNotRequired(Boolean perexNotRequired) {
        this.perexNotRequired = perexNotRequired;
    }

    public Boolean getLoadData() {
        return loadData;
    }

    public void setLoadData(Boolean loadData) {
        this.loadData = loadData;
    }

    public Boolean getCheckDuplicity() {
        return checkDuplicity;
    }

    public void setCheckDuplicity(Boolean checkDuplicity) {
        this.checkDuplicity = checkDuplicity;
    }

    public List<Integer> getPerexGroup() {
        return perexGroup;
    }

    public void setPerexGroup(List<Integer> perexGroup) {
        this.perexGroup = perexGroup;
    }

    public List<Integer> getPerexGroupNot() {
        return perexGroupNot;
    }

    public void setPerexGroupNot(List<Integer> perexGroupNot) {
        this.perexGroupNot = perexGroupNot;
    }
}
