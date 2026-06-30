package sk.iway.iwcm.headless.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.news.FieldEnum;
import sk.iway.iwcm.components.news.NewsActionBean.PublishType;
import sk.iway.iwcm.components.news.NewsQuery;
import sk.iway.iwcm.components.news.NewsQuery.OrderEnum;
import sk.iway.iwcm.components.news.NewsQuery.SortEnum;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.headless.dto.HeadlessNewsItem;
import sk.iway.iwcm.headless.dto.HeadlessNewsRequest;
import sk.iway.iwcm.headless.dto.HeadlessNewsResponse;
import sk.iway.iwcm.common.CloudToolsForCore;

/**
 * Service for headless news listing.
 * Translates HeadlessNewsRequest parameters to NewsQuery behavior,
 * preserving core NewsActionBean semantics for ordering, publish filtering,
 * group filtering, paging, and offset.
 */
@Service("HeadlessNewsService")
public class HeadlessNewsService {

    /**
     * Executes a headless news query based on the request parameters.
     *
     * @param request the news request parameters
     * @return paginated response with news items
     */
    public HeadlessNewsResponse listNews(HeadlessNewsRequest request) {
        // Build and execute NewsQuery
        NewsQuery query = buildNewsQuery(request);

        // Get the raw news list from NewsQuery
        List<DocDetails> docDetailsList = query.getNewsList();

        // Build response items from DocDetails
        List<HeadlessNewsItem> items = buildNewsItems(docDetailsList, request);

        // Build pagination metadata
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        response.setItems(items);

        if (request.getPaging() != null && request.getPaging()) {
            response.setPage(request.getOffset() / Math.max(request.getPageSize(), 1) + 1);
            response.setSize(request.getPageSize());
            response.setTotalElements(items.size());
            response.setTotalPages((int) Math.ceil((double) items.size() / Math.max(request.getPageSize(), 1)));
        } else {
            // No paging: return all results as a single page
            response.setPage(1);
            response.setSize(request.getPageSize());
            response.setTotalElements(items.size());
            response.setTotalPages(items.isEmpty() ? 1 : 1);
        }

        return response;
    }

    /**
     * Builds a NewsQuery from the request parameters.
     * Maps HeadlessNewsRequest fields to NewsQuery behavior.
     */
    private NewsQuery buildNewsQuery(HeadlessNewsRequest request) {
        NewsQuery query = new NewsQuery();

        // Apply group filtering
        addGroupFiltering(query, request);

        // Apply publish type filter
        addPublishTypeFilter(query, request);

        // Apply ordering
        addOrdering(query, request);

        // Apply paging parameters
        addPaging(query, request);

        // Set loadData flag
        if (request.getLoadData() != null) {
            query.setLoadData(request.getLoadData());
        }

        return query;
    }

    /**
     * Adds group filtering to the query, mirroring NewsActionBean.addGroups().
     * Supports alsoSubGroups and subGroupsDepth.
     */
    private void addGroupFiltering(NewsQuery query, HeadlessNewsRequest request) {
        List<Integer> groupIdsExpanded = new ArrayList<>();

        List<Integer> groupIds = request.getGroupIds();
        if (groupIds != null && !groupIds.isEmpty()) {
            int subGroupsDepth = -1; // default: unlimited

            GroupsDB gdb = GroupsDB.getInstance();
            for (Integer groupId : groupIds) {
                GroupDetails group = gdb.getGroup(groupId);
                if (group != null) {
                    groupIdsExpanded.add(group.getGroupId());
                    if (request.getAlsoSubGroups() != null && request.getAlsoSubGroups()) {
                        List<GroupDetails> subGroups = gdb.getGroupsTree(
                                group.getGroupId(), false, false);
                        for (GroupDetails subGroup : subGroups) {
                            groupIdsExpanded.add(subGroup.getGroupId());
                        }
                    }
                }
            }

            if (!groupIdsExpanded.isEmpty()) {
                query.addCriteria(DatabaseCriteria.in(FieldEnum.GROUP_ID, groupIdsExpanded));
            }
        } else {
            // Default: use current domain's root group
            int domainId = CloudToolsForCore.getDomainId();
            if (domainId > 0) {
                query.addCriteria(DatabaseCriteria.in(FieldEnum.GROUP_ID, domainId));
            }
        }

        // Apply perexGroup filter if specified
        List<Integer> perexGroup = request.getPerexGroup();
        if (perexGroup != null && !perexGroup.isEmpty()) {
            query.addCriteria(DatabaseCriteria.in(FieldEnum.PEREX_GROUP, perexGroup));
        }

        // Apply perexGroupNot filter if specified
        List<Integer> perexGroupNot = request.getPerexGroupNot();
        if (perexGroupNot != null && !perexGroupNot.isEmpty()) {
            query.addCriteria(DatabaseCriteria.notIn(FieldEnum.PEREX_GROUP, perexGroupNot));
        }
    }

    /**
     * Applies publish type filter, mirroring NewsActionBean.addPublishType().
     */
    private void addPublishTypeFilter(NewsQuery query, HeadlessNewsRequest request) {
        String publishTypeStr = request.getPublishType();
        if (publishTypeStr == null || publishTypeStr.isEmpty()) {
            publishTypeStr = "new";
        }

        PublishType publishType = PublishType.fromString(publishTypeStr);
        if (publishType == null) {
            publishType = PublishType.NEW;
        }

        Date now = new Date();

        switch (publishType) {
            case NEW:
                // Only published news (publish_start <= now)
                query.addCriteria(DatabaseCriteria.and(
                        DatabaseCriteria.lessEqual(FieldEnum.PUBLISH_START, now),
                        DatabaseCriteria.or(
                                DatabaseCriteria.isNull(FieldEnum.PUBLISH_END),
                                DatabaseCriteria.greaterEqual(FieldEnum.PUBLISH_END, now)
                        )
                ));
                break;
            case OLD:
                // Only expired news (publish_end <= now)
                query.addCriteria(DatabaseCriteria.and(
                        DatabaseCriteria.notNull(FieldEnum.PUBLISH_END),
                        DatabaseCriteria.lessEqual(FieldEnum.PUBLISH_END, now)
                ));
                break;
            case ALL:
                // No publish filter
                break;
            case NEXT:
                // Only future news (publish_start > now)
                query.addCriteria(DatabaseCriteria.greaterEqual(FieldEnum.PUBLISH_START, now));
                break;
            case VALID:
                // Published and not expired (same as NEW)
                query.addCriteria(DatabaseCriteria.and(
                        DatabaseCriteria.lessEqual(FieldEnum.PUBLISH_START, now),
                        DatabaseCriteria.or(
                                DatabaseCriteria.isNull(FieldEnum.PUBLISH_END),
                                DatabaseCriteria.greaterEqual(FieldEnum.PUBLISH_END, now)
                        )
                ));
                break;
        }
    }

    /**
     * Applies ordering to the query, mirroring NewsActionBean.addOrder().
     */
    private void addOrdering(NewsQuery query, HeadlessNewsRequest request) {
        String orderStr = request.getOrder();
        if (orderStr == null || orderStr.isEmpty()) {
            orderStr = "date";
        }

        OrderEnum orderEnum;
        try {
            orderEnum = OrderEnum.valueOf(orderStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            orderEnum = OrderEnum.DATE;
        }

        boolean ascending = request.getAscending() != null && request.getAscending();
        SortEnum sortEnum = ascending ? SortEnum.ASC : SortEnum.DESC;

        query.addOrder(orderEnum, sortEnum);
    }

    /**
     * Applies paging parameters, mirroring NewsActionBean.addPaging().
     */
    private void addPaging(NewsQuery query, HeadlessNewsRequest request) {
        boolean paging = request.getPaging() != null && request.getPaging();
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        int offset = request.getOffset() != null ? request.getOffset() : 0;

        if (paging) {
            query.setPageSize(pageSize).setPage(1); // 1-based page
            if (offset > 0) {
                query.setInitialOffset(offset);
            }
        } else if (pageSize > 0) {
            query.setPageSize(pageSize);
            if (offset > 0) {
                query.setInitialOffset(offset);
            }
        }
    }

    /**
     * Converts DocDetails list to HeadlessNewsItem list.
     */
    private List<HeadlessNewsItem> buildNewsItems(List<DocDetails> docDetailsList, HeadlessNewsRequest request) {
        List<HeadlessNewsItem> items = new ArrayList<>();

        for (DocDetails doc : docDetailsList) {
            HeadlessNewsItem item = new HeadlessNewsItem();
            item.setDocId(doc.getDocId());
            item.setTitle(doc.getTitle());
            item.setVirtualPath(doc.getVirtualPath());

            // Language from request parameter or default
            // (no lng parameter in request, so use default)
            item.setLanguage(sk.iway.iwcm.Constants.getString("defaultLanguage"));

            item.setPerex(doc.getPerexImage());
            item.setData(doc.getData());
            item.setPublishStart(doc.getPublishStart());
            item.setPublishEnd(doc.getPublishEnd());
            item.setGroupId(doc.getGroupId());
            item.setTemplateName(doc.getTemplateName());
            item.setAvailable(doc.isAvailable());
            item.setCreateDate(doc.getCreateDate());

            items.add(item);
        }

        return items;
    }
}
