package sk.iway.iwcm.components.welcome;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.dmail.jpa.CampaingsRepository;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.approve.GroupsApproveRestController;
import sk.iway.iwcm.editor.approve.WebApproveRestController;
import sk.iway.iwcm.editor.rest.GroupSchedulerDto;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * Small read-only projections. Module rights, current-domain scope, and row access are
 * applied before counting or limiting; browser configuration never grants access.
 */
@Service
public class DashboardWidgetDataService {
    static final int PREVIEW_SIZE = 6;
    static final Set<String> TYPES = Set.of("approvals", "publishing", "forms", "traffic", "top-pages",
        "search-terms", "referrers", "newsletter", "errors", "sessions");
    private final DocHistoryRepository history;
    private final GroupSchedulerDtoRepository groupHistory;
    private final FormsRepository forms;
    private final CampaingsRepository campaigns;

    public DashboardWidgetDataService(DocHistoryRepository history, GroupSchedulerDtoRepository groupHistory,
            FormsRepository forms, CampaingsRepository campaigns) {
        this.history = history;
        this.groupHistory = groupHistory;
        this.forms = forms;
        this.campaigns = campaigns;
    }

    /** Returns only fields required by the selected widget, never full module entities. */
    public Map<String, Object> load(String type, int days, String metric, String formName, Long campaignId,
            Identity user, String domain, String currentSessionId) {
        validate(type, days, metric, formName, campaignId);
        authorize(type, user);
        if (!"sessions".equals(type) && Tools.isEmpty(domain)) throw new UnavailableException("domain-unavailable");
        Range range = completedDays(days, Clock.systemDefaultZone());
        return switch (type) {
            case "sessions" -> sessions(user, currentSessionId);
            case "approvals" -> approvals(user, scope(user, domain));
            case "publishing" -> publishing(user, domain);
            case "forms" -> forms(user, domain, formName, recentDays(days, Clock.systemDefaultZone()));
            case "newsletter" -> newsletter(domain, campaignId);
            case "errors" -> errors(domain, range);
            default -> statistics(type, metric, scope(user, domain), range);
        };
    }

    static void validate(String type, int days, String metric, String formName, Long campaignId) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unknown widget");
        if (days != 7 && days != 30 && days != 90) throw new IllegalArgumentException("Invalid period");
        if (!Set.of("views", "sessions", "uniqueUsers").contains(metric)) throw new IllegalArgumentException("Invalid metric");
        if (formName != null && (formName.length() > 255 || formName.isBlank())) throw new IllegalArgumentException("Invalid form");
        if (campaignId != null && campaignId < 1) throw new IllegalArgumentException("Invalid campaign");
    }

    static void authorize(String type, Identity user) {
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        String permission = switch (type) {
            case "sessions" -> null;
            case "approvals", "publishing" -> "menuWebpages";
            case "forms" -> "cmp_form";
            case "newsletter" -> "menuEmail";
            default -> "cmp_stat";
        };
        if (permission != null && !user.isEnabledItem(permission)) throw new AccessDeniedException("Widget permission is required");
        if ("cmp_stat".equals(permission) && "none".equals(Constants.getString("statMode"))) {
            throw new UnavailableException("statistics-disabled");
        }
    }

    record Range(long from, long until, ZoneId zone) {
        Range(long from, long until) { this(from, until, ZoneId.systemDefault()); }
        Range previous() {
            LocalDate start = new Date(from).toInstant().atZone(zone).toLocalDate();
            LocalDate end = new Date(until).toInstant().atZone(zone).toLocalDate();
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            return new Range(start.minusDays(days).atStartOfDay(zone).toInstant().toEpochMilli(), from, zone);
        }
    }

    static Range completedDays(int days, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        return new Range(today.minusDays(days).atStartOfDay(clock.getZone()).toInstant().toEpochMilli(),
            today.atStartOfDay(clock.getZone()).toInstant().toEpochMilli(), clock.getZone());
    }

    static Range recentDays(int days, Clock clock) {
        return new Range(LocalDate.now(clock).minusDays(days - 1L).atStartOfDay(clock.getZone()).toInstant().toEpochMilli(), clock.millis(), clock.getZone());
    }

    /** IDs are derived from the current domain and current account, never request parameters. */
    record Scope(List<Integer> groups, List<Integer> pages, List<Integer> domainGroups) {
        Scope(List<Integer> groups, List<Integer> pages) { this(groups, pages, groups); }
        String sql(String alias) {
            return "(" + alias + ".group_id IN (" + ids(groups) + ") OR " + alias + ".doc_id IN (" + ids(pages) + "))";
        }
        String statisticsSql(String alias) { return alias + ".group_id IN (" + ids(domainGroups) + ") AND " + sql(alias); }
    }

    private Scope scope(Identity user, String domain) {
        List<Integer> groups = new ArrayList<>();
        List<Integer> pages = new ArrayList<>();
        List<Integer> domainGroups = new ArrayList<>();
        boolean onlyPages = Tools.isEmpty(user.getEditableGroups(true)) && Tools.isNotEmpty(user.getEditablePages());
        for (GroupDetails group : GroupsDB.getInstance().getGroupsAll()) {
            if (inDomain(group, domain)) {
                domainGroups.add(group.getGroupId());
                if (!onlyPages && GroupsDB.isGroupEditable(user, group.getGroupId())) groups.add(group.getGroupId());
            }
        }
        for (int id : Tools.getTokensInt(user.getEditablePages(), ",")) {
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(id, false);
            if (doc != null && inDomain(GroupsDB.getInstance().getGroup(doc.getGroupId()), domain)) pages.add(id);
        }
        return new Scope(groups, pages, domainGroups);
    }

    private static boolean inDomain(GroupDetails group, String domain) {
        if (group == null || group.isHiddenInAdmin() || !domain.equalsIgnoreCase(group.getDomainName())) return false;
        String trash = GroupsTreeService.getTrashDirPath();
        return Tools.isEmpty(trash) || group.getFullPath() == null || !group.getFullPath().contains(trash);
    }

    private static String ids(List<Integer> ids) {
        return ids.isEmpty() ? "-1" : ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Predicate accessibleDocument(Expression<Integer> id, CriteriaQuery<?> query, CriteriaBuilder builder, Scope scope) {
        var subquery = query.subquery(Long.class);
        Root<DocDetails> doc = subquery.from(DocDetails.class);
        subquery.select(doc.get("id")).where(builder.or(doc.get("groupId").in(orMissing(scope.groups)), doc.get("id").in(orMissing(scope.pages))));
        return id.in(subquery);
    }

    private static List<Integer> orMissing(List<Integer> ids) { return ids.isEmpty() ? List.of(-1) : ids; }

    private Map<String, Object> approvals(Identity user, Scope scope) {
        Specification<DocHistory> documents = WebApproveRestController.getToApproveConditions(user.getUserId())
            .and((root, query, builder) -> accessibleDocument(root.get("docId"), query, builder, scope));
        Page<DocHistory> page = history.findAll(documents, PageRequest.of(0, PREVIEW_SIZE, Sort.by("saveDate").descending()));
        List<Map<String, Object>> items = new ArrayList<>();
        page.forEach(doc -> {
            Map<String, Object> item = item("page-" + doc.getId(), doc.getTitle(), pageUrl(doc.getDocId()));
            item.put("kind", "page");
            item.put("section", doc.getAuthorName());
            putDate(item, doc.getSaveDate());
            items.add(item);
        });
        Specification<GroupSchedulerDto> directories = GroupsApproveRestController.getToApproveConditions(user.getUserId())
            .and((root, query, builder) -> root.get("groupId").in(orMissing(scope.groups)));
        Page<GroupSchedulerDto> groupPage = groupHistory.findAll(directories, PageRequest.of(0, PREVIEW_SIZE, Sort.by("saveDate").descending()));
        groupPage.forEach(group -> {
            Map<String, Object> item = item("group-" + group.getId(), group.getGroupName(), "/admin/v9/webpages/web-pages-list/?scheduleId=" + group.getId() + (Boolean.TRUE.equals(group.getIsDelete()) ? "&act=delete" : ""));
            item.put("kind", "group");
            var author = group.getUserId() == null ? null : UsersDB.getUserCached(group.getUserId());
            if (author != null) item.put("section", author.getFullName());
            putDate(item, group.getSaveDate());
            items.add(item);
        });
        items.sort(Comparator.comparingLong(DashboardWidgetDataService::dateOf).reversed());
        return response(page.getTotalElements() + groupPage.getTotalElements(), items.stream().limit(PREVIEW_SIZE).toList());
    }

    private Map<String, Object> publishing(Identity user, String domain) {
        long now = System.currentTimeMillis();
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<DocBasic> scheduled = DocDB.getInstance().getPublicableDocs();
        if (scheduled != null) for (DocBasic doc : scheduled) {
            DocDetails current = DocDB.getInstance().getBasicDocDetails(doc.getDocId(), false);
            if (!DashboardRecentPagesService.isAccessible(current, user, domain)) continue;
            if (doc instanceof DocHistory pending && Boolean.TRUE.equals(pending.getPublicable())) {
                addPublication(items, seen, doc, "publish", doc.getPublishStart(), now);
            }
            if (doc.isDisableAfterEnd()) addPublication(items, seen, doc, "expire", doc.getPublishEnd(), now);
        }
        items.sort(Comparator.comparingLong(DashboardWidgetDataService::dateOf));
        Map<String, Object> result = response(items.size(), items.stream().limit(PREVIEW_SIZE).toList());
        result.put("from", now);
        return result;
    }

    private void addPublication(List<Map<String, Object>> items, Set<String> seen, DocBasic doc, String kind, long date, long from) {
        String id = doc.getDocId() + "-" + kind + "-" + date;
        if (date < from || !seen.add(id)) return;
        Map<String, Object> item = item(id, doc.getTitle(), pageUrl(doc.getDocId()));
        item.put("kind", kind);
        item.put("date", date);
        items.add(item);
    }

    private Map<String, Object> forms(Identity user, String domain, String selected, Range range) {
        Scope scope = scope(user, domain);
        List<String> names;
        try (Connection connection = DBPool.getConnection()) {
            names = allowedFormNames(connection, scope, CloudToolsForCore.getDomainId());
        } catch (SQLException exception) { throw new IllegalStateException("Could not load accessible forms", exception); }
        if (selected != null && !names.contains(selected)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        List<String> requested = selected == null ? names : List.of(selected);
        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;
        if (!requested.isEmpty()) {
            Specification<FormsEntity> spec = formSpec(requested, scope, range);
            Page<FormsEntity> page = forms.findAll(spec, PageRequest.of(0, PREVIEW_SIZE, Sort.by("createDate").descending()));
            total = page.getTotalElements();
            page.forEach(form -> {
                Map<String, Object> item = item(form.getId().toString(), form.getFormName(), "/apps/form/admin/detail/?formName=" + encode(form.getFormName()));
                item.put("date", form.getCreateDate().getTime());
                items.add(item);
            });
        }
        Map<String, Object> result = response(total, items);
        period(result, range);
        result.put("options", names.stream().map(name -> Map.of("id", name, "title", name)).toList());
        return result;
    }

    /** Resolves the same latest-submission page as form administration without loading or mutating form entities. */
    List<String> allowedFormNames(Connection connection, Scope scope, int domainId) throws SQLException {
        String sql = "SELECT DISTINCT f.form_name FROM forms f JOIN (SELECT form_name, MAX(create_date) AS latest_created FROM forms WHERE domain_id=?"
            + " GROUP BY form_name HAVING SUM(CASE WHEN create_date IS NULL THEN 1 ELSE 0 END)>0) latest ON latest.form_name=f.form_name"
            + " AND (f.create_date=latest.latest_created OR (f.create_date IS NULL AND latest.latest_created IS NULL))"
            + " JOIN documents d ON d.doc_id=f.doc_id WHERE f.domain_id=? AND " + scope.sql("d") + " ORDER BY f.form_name";
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, domainId);
            statement.setInt(2, domainId);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) names.add(rows.getString(1)); }
        }
        return names;
    }

    private Specification<FormsEntity> formSpec(List<String> names, Scope scope, Range range) {
        return (root, query, builder) -> builder.and(root.get("formName").in(names),
            builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()),
            builder.greaterThanOrEqualTo(root.get("createDate"), new Date(range.from)),
            builder.lessThan(root.get("createDate"), new Date(range.until)),
            accessibleDocument(root.get("docId"), query, builder, scope));
    }

    private Map<String, Object> sessions(Identity user, String currentSessionId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var data = mapper.readTree(SessionClusterService.getSessionInfo(currentSessionId, user.getUserId()));
            long count = 0;
            for (var cluster : data.path("userSessions")) count += cluster.path("userSessions").size();
            Map<String, Object> result = response(count, List.of());
            result.put("currentSessions", mapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
            return result;
        } catch (JsonProcessingException exception) {
            throw new UnavailableException("sessions-unavailable");
        }
    }

    private Map<String, Object> statistics(String type, String metric, Scope scope, Range range) {
        Map<String, Object> result;
        try (Connection connection = DBPool.getConnection()) {
            if ("traffic".equals(type)) {
                Map<String, Long> current = trafficTotals(connection, scope, range);
                Map<String, Long> previous = trafficTotals(connection, scope, range.previous());
                result = response(current.get(metric), List.of());
                result.put("previous", previous.get(metric));
                result.put("series", trafficSeries(connection, scope, range, metric));
                result.put("previousSeries", trafficSeries(connection, scope, range.previous(), metric));
                result.put("metric", metric);
            } else {
                String table = "top-pages".equals(type) ? "stat_views" : "search-terms".equals(type) ? "stat_searchengine" : "stat_from";
                String column = "top-pages".equals(type) ? "doc_id" : "search-terms".equals(type) ? "query" : "referer_server_name";
                String time = "top-pages".equals(type) ? "view_time" : "search-terms".equals(type) ? "search_date" : "from_time";
                List<Map<String, Object>> items = ranked(connection, table, column, time, scope, range);
                if ("top-pages".equals(type)) {
                    topPageDetails(connection, items, scope);
                    for (Map<String, Object> item : items) {
                        int id = Integer.parseInt((String)item.get("id"));
                        item.put("url", "/apps/stat/admin/top-details/?docId=" + id + "&dateRange=" + encode("daterange:" + range.from + "-" + (range.until - 1)));
                        item.put("previous", countRows(connection, table, time, scope, range.previous(), " AND s.doc_id=" + id));
                    }
                } else for (Map<String, Object> item : items) item.put("url", "search-terms".equals(type) ? "/apps/stat/admin/search-engines/" : "/apps/stat/admin/referer/");
                result = response(countRows(connection, table, time, scope, range, ""), items);
                result.put("previous", countRows(connection, table, time, scope, range.previous(), ""));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load dashboard statistics", exception);
        }
        period(result, range);
        return result;
    }

    /** Enriches the bounded ranking from current page metadata and one scoped image projection. */
    void topPageDetails(Connection connection, List<Map<String, Object>> items, Scope scope) throws SQLException {
        Map<Integer, Map<String, Object>> accessible = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            int id = Integer.parseInt((String) item.get("id"));
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(id, false);
            boolean allowed = doc != null && scope.domainGroups.contains(doc.getGroupId())
                && (scope.groups.contains(doc.getGroupId()) || scope.pages.contains(id));
            item.put("title", allowed ? doc.getTitle() : String.valueOf(id));
            if (allowed) {
                item.put("section", doc.getFullPath());
                accessible.put(id, item);
            }
        }
        if (accessible.isEmpty()) return;
        String placeholders = accessible.keySet().stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT d.doc_id, d.perex_image FROM documents d WHERE d.doc_id IN (" + placeholders + ") AND " + scope.statisticsSql("d");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            for (int id : accessible.keySet()) statement.setInt(parameter++, id);
            statement.setMaxRows(PREVIEW_SIZE);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Map<String, Object> item = accessible.get(rows.getInt("doc_id"));
                    if (item != null) item.put("perexImage", DashboardRecentPagesService.previewImage(rows.getString("perex_image")));
                }
            }
        }
    }

    Map<String, Long> trafficTotals(Connection connection, Scope scope, Range range) throws SQLException {
        String data = union("stat_views", "view_time", "s.doc_id, s.session_id, s.browser_id", scope, range, "");
        return read(connection, "SELECT COUNT(doc_id), COUNT(DISTINCT session_id), COUNT(DISTINCT browser_id) FROM (" + data + ") w", range, "stat_views", 1,
            rows -> Map.of("views", rows.getLong(1), "sessions", rows.getLong(2), "uniqueUsers", rows.getLong(3))).get(0);
    }

    private List<Map<String, Object>> trafficSeries(Connection connection, Scope scope, Range range, String metric) throws SQLException {
        String data = union("stat_views", "view_time", "s.view_time, s.doc_id, s.session_id, s.browser_id", scope, range, "");
        String expression = "views".equals(metric) ? "COUNT(doc_id)" : "sessions".equals(metric) ? "COUNT(DISTINCT session_id)" : "COUNT(DISTINCT browser_id)";
        String sql = "SELECT " + StatNewDB.getDMYSelect("view_time") + ", " + expression + " FROM (" + data + ") w GROUP BY " + StatNewDB.getDMYGroupBy("view_time");
        Map<LocalDate, Long> values = new LinkedHashMap<>();
        read(connection, sql, range, "stat_views", 91, rows -> {
            values.put(LocalDate.of(rows.getInt(3), rows.getInt(2), rows.getInt(1)), rows.getLong(4));
            return 0;
        });
        List<Map<String, Object>> series = new ArrayList<>();
        LocalDate day = new Date(range.from).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        while (day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() < range.until) {
            long from = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            series.add(Map.of("date", from, "value", values.getOrDefault(day, 0L)));
            day = day.plusDays(1);
        }
        return series;
    }

    private List<Map<String, Object>> ranked(Connection connection, String table, String column, String time, Scope scope, Range range) throws SQLException {
        String data = union(table, time, "s." + column + " AS item_key", scope, range, "");
        return read(connection, "SELECT item_key, COUNT(*) AS item_count FROM (" + data + ") w GROUP BY item_key ORDER BY item_count DESC", range, table, PREVIEW_SIZE,
            rows -> {
                Map<String, Object> item = item(rows.getString(1), rows.getString(1), "");
                item.put("value", rows.getLong(2));
                return item;
            });
    }

    private long countRows(Connection connection, String table, String time, Scope scope, Range range, String extra) throws SQLException {
        String data = union(table, time, "s.doc_id", scope, range, extra);
        return read(connection, "SELECT COUNT(*) FROM (" + data + ") w", range, table, 1, rows -> rows.getLong(1)).get(0);
    }

    private String union(String table, String time, String columns, Scope scope, Range range, String extra) {
        List<String> parts = new ArrayList<>();
        for (String suffix : suffixes(table, range)) {
            String bots = "stat_views".equals(table) ? StatNewDB.getWhiteListedUAQuery() : "";
            parts.add("SELECT " + columns + " FROM " + table + suffix + " s WHERE s." + time + ">=? AND s." + time + "<? AND " + scope.statisticsSql("s") + bots + extra);
        }
        return String.join(" UNION ALL ", parts);
    }

    private static String[] suffixes(String table, Range range) {
        String[] suffixes = StatNewDB.getTableSuffix(table, range.from, range.until - 1);
        for (String suffix : suffixes) if (!suffix.matches("[0-9_]*")) throw new IllegalStateException("Invalid statistics partition");
        return suffixes;
    }

    @FunctionalInterface
    interface RowReader<T> { T read(ResultSet rows) throws SQLException; }

    private <T> List<T> read(Connection connection, String sql, Range range, String table, int maxRows, RowReader<T> reader) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (String ignored : suffixes(table, range)) {
                statement.setTimestamp(index++, new Timestamp(range.from));
                statement.setTimestamp(index++, new Timestamp(range.until));
            }
            statement.setMaxRows(maxRows);
            statement.setQueryTimeout(15);
            List<T> items = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) items.add(reader.read(rows)); }
            return items;
        }
    }

    private Map<String, Object> newsletter(String domain, Long selectedId) {
        int domainId = CloudToolsForCore.getDomainId();
        List<CampaingsEntity> allowed = campaigns.findAllByDomainId(domainId).stream()
            .filter(campaign -> campaignInDomain(campaign, domain))
            .sorted(Comparator.comparing(CampaingsEntity::getCreateDate, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        if (selectedId != null && allowed.stream().noneMatch(campaign -> selectedId.equals(campaign.getId()))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try (Connection connection = DBPool.getConnection()) {
            Map<Long, CampaignDelivery> delivery = campaignDelivery(connection, domainId, System.currentTimeMillis());
            Long chosen = selectedId != null ? selectedId : chooseCampaign(allowed, delivery);
            List<CampaingsEntity> ordered = new ArrayList<>(chosen == null ? List.of() : allowed);
            ordered.sort(Comparator.comparing(campaign -> !campaign.getId().equals(chosen)));
            List<Map<String, Object>> items = new ArrayList<>();
            for (CampaingsEntity campaign : ordered.stream().limit(3).toList()) {
                Map<String, Object> item = item(campaign.getId().toString(), campaign.getSubject(), "/apps/dmail/admin/?id=" + campaign.getId());
                putDate(item, campaign.getSendAt() == null ? campaign.getCreateDate() : campaign.getSendAt());
                campaignCounts(connection, campaign.getId(), domainId, item);
                long recipients = ((Number)item.get("recipients")).longValue();
                long processed = ((Number)item.get("sent")).longValue() + ((Number)item.get("failed")).longValue();
                String status = recipients == 0 ? "draft" : processed >= recipients ? "completed" : "paused";
                if (campaign.getSendAt() != null && campaign.getSendAt().getTime() > System.currentTimeMillis() && processed < recipients) status = "scheduled";
                if (delivery.getOrDefault(campaign.getId(), CampaignDelivery.EMPTY).active) status = "sending";
                item.put("status", status);
                items.add(item);
            }
            Map<String, Object> result = response(allowed.size(), items);
            if (chosen != null) result.put("selectedId", chosen.toString());
            result.put("active", chosen != null && delivery.getOrDefault(chosen, CampaignDelivery.EMPTY).active);
            result.put("options", allowed.stream().map(campaign -> Map.of("id", campaign.getId().toString(), "title", campaign.getSubject())).toList());
            return result;
        } catch (SQLException exception) { throw new IllegalStateException("Could not load newsletter status", exception); }
    }

    record CampaignDelivery(boolean active, Long started, Long scheduled, Long completed) {
        static final CampaignDelivery EMPTY = new CampaignDelivery(false, null, null, null);
    }

    /** Chooses actual delivery work before planned work and completed campaigns; drafts remain manual choices. */
    static Long chooseCampaign(List<CampaingsEntity> allowed, Map<Long, CampaignDelivery> delivery) {
        List<Long> ids = allowed.stream().map(CampaingsEntity::getId).filter(delivery::containsKey).toList();
        Long active = ids.stream().filter(id -> delivery.get(id).active && delivery.get(id).started != null)
            .max(Comparator.comparing(id -> delivery.get(id).started)).orElse(null);
        if (active != null) return active;
        Long queued = allowed.stream().filter(campaign -> delivery.getOrDefault(campaign.getId(), CampaignDelivery.EMPTY).active)
            .max(Comparator.comparing(campaign -> campaign.getSendAt() == null ? campaign.getCreateDate() : campaign.getSendAt(), Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(CampaingsEntity::getId).orElse(null);
        if (queued != null) return queued;
        Long scheduled = ids.stream().filter(id -> delivery.get(id).scheduled != null)
            .min(Comparator.comparing(id -> delivery.get(id).scheduled)).orElse(null);
        if (scheduled != null) return scheduled;
        return ids.stream().filter(id -> delivery.get(id).completed != null)
            .max(Comparator.comparing(id -> delivery.get(id).completed)).orElse(null);
    }

    Map<Long, CampaignDelivery> campaignDelivery(Connection connection, int domainId, long now) throws SQLException {
        String pending = "sent_date IS NULL AND retry>=0 AND disabled=?";
        String sql = "SELECT campain_id, COUNT(*), SUM(CASE WHEN (sent_date IS NOT NULL AND (retry IS NULL OR retry<>98)) OR retry<0 THEN 1 ELSE 0 END), MIN(sent_date), MAX(sent_date), "
            + "SUM(CASE WHEN (" + pending + " AND (send_at IS NULL OR send_at<=?)) OR retry=98 THEN 1 ELSE 0 END), "
            + "MIN(CASE WHEN " + pending + " AND send_at>? THEN send_at ELSE NULL END) FROM emails WHERE domain_id=? GROUP BY campain_id";
        Map<Long, CampaignDelivery> result = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, false);
            statement.setTimestamp(2, new Timestamp(now));
            statement.setBoolean(3, false);
            statement.setTimestamp(4, new Timestamp(now));
            statement.setInt(5, domainId);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Timestamp first = rows.getTimestamp(4);
                    Timestamp last = rows.getTimestamp(5);
                    Timestamp planned = rows.getTimestamp(7);
                    result.put(rows.getLong(1), new CampaignDelivery(rows.getLong(6) > 0, first == null ? null : first.getTime(),
                        planned == null ? null : planned.getTime(), rows.getLong(2) == rows.getLong(3) && last != null ? last.getTime() : null));
                }
            }
        }
        return result;
    }

    private boolean campaignInDomain(CampaingsEntity campaign, String domain) {
        if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) return true;
        DocDetails doc = WebpagesService.getBasicDocFromUrl(campaign.getUrl());
        if (doc != null && doc.getDocId() > 0) return inDomain(GroupsDB.getInstance().getGroup(doc.getGroupId()), domain);
        try { return domain.equalsIgnoreCase(URI.create(campaign.getUrl()).getHost()); }
        catch (IllegalArgumentException | NullPointerException exception) { return false; }
    }

    void campaignCounts(Connection connection, long id, int domainId, Map<String, Object> item) throws SQLException {
        String sql = "SELECT COUNT(*), SUM(CASE WHEN sent_date IS NOT NULL AND (retry IS NULL OR (retry>=0 AND retry<>98)) THEN 1 ELSE 0 END), SUM(CASE WHEN retry<0 THEN 1 ELSE 0 END), SUM(CASE WHEN seen_date IS NOT NULL THEN 1 ELSE 0 END) FROM emails WHERE campain_id=? AND domain_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setInt(2, domainId);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                item.put("recipients", rows.getLong(1));
                item.put("sent", rows.getLong(2));
                item.put("failed", rows.getLong(3));
                item.put("opens", rows.getLong(4));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM emails_stat_click c JOIN emails e ON c.email_id=e.email_id WHERE e.campain_id=? AND e.domain_id=?")) {
            statement.setLong(1, id);
            statement.setInt(2, domainId);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); item.put("clicks", rows.getLong(1)); }
        }
    }

    private Map<String, Object> errors(String domain, Range requested) {
        if (!InitServlet.isTypeCloud() && !Constants.getBoolean("enableStaticFilesExternalDir") && GroupsDB.getInstance().getAllDomainsList().size() > 1) {
            throw new UnavailableException("domain-unavailable");
        }
        Range range = errorRange(requested, Clock.systemDefaultZone());
        String[] partitions = suffixes("stat_error", range);
        List<ErrorWeek> weeks = errorWeeks(range, List.of(partitions).contains(""));
        String weekFilter = weeks.stream().map(week -> "(year=? AND week=?)").collect(Collectors.joining(" OR "));
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Connection connection = DBPool.getConnection()) {
            for (String suffix : partitions) {
                String sql = "SELECT url, SUM(count) FROM stat_error" + suffix + " WHERE domain_id=? AND (" + weekFilter + ") GROUP BY url";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, CloudToolsForCore.getDomainId());
                    int index = 2;
                    for (ErrorWeek week : weeks) {
                        statement.setInt(index++, week.year);
                        statement.setInt(index++, week.week);
                    }
                    statement.setQueryTimeout(15);
                    try (ResultSet rows = statement.executeQuery()) { while (rows.next()) counts.merge(rows.getString(1), rows.getLong(2), Long::sum); }
                }
            }
        } catch (SQLException exception) { throw new IllegalStateException("Could not load error statistics", exception); }
        List<Map<String, Object>> items = counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(PREVIEW_SIZE).map(entry -> {
            Map<String, Object> item = item(entry.getKey(), entry.getKey(), "/apps/stat/admin/error/");
            item.put("value", entry.getValue());
            return item;
        }).toList();
        Map<String, Object> result = response(counts.values().stream().mapToLong(Long::longValue).sum(), items);
        period(result, range);
        result.put("granularity", "week");
        return result;
    }

    record ErrorWeek(int year, int week) {}

    static Range errorRange(Range requested, Clock clock) {
        ZoneId zone = clock.getZone();
        LocalDate from = new Date(requested.from).toInstant().atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate until = new Date(requested.until - 1).toInstant().atZone(zone).toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1);
        return new Range(from.atStartOfDay(zone).toInstant().toEpochMilli(), Math.min(clock.millis(), until.atStartOfDay(zone).toInstant().toEpochMilli()), zone);
    }

    /** Uses the collector's calendar year, including the split year at January 1, rather than an ISO week year. */
    static List<ErrorWeek> errorWeeks(Range range, boolean includesUnpartitionedTable) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(range.zone));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTimeInMillis(range.from);
        Set<ErrorWeek> weeks = new LinkedHashSet<>();
        while (calendar.getTimeInMillis() < range.until) {
            int week = calendar.get(Calendar.WEEK_OF_YEAR);
            int month = calendar.get(Calendar.MONTH);
            if (includesUnpartitionedTable && ((month == Calendar.DECEMBER && week == 1) || (month == Calendar.JANUARY && week > 50))) {
                throw new UnavailableException("period-unavailable");
            }
            weeks.add(new ErrorWeek(calendar.get(Calendar.YEAR), week));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return new ArrayList<>(weeks);
    }

    private static Map<String, Object> response(long total, List<Map<String, Object>> items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("items", items);
        return result;
    }

    private static Map<String, Object> item(String id, String title, String url) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("title", title == null ? "" : title);
        item.put("url", url);
        return item;
    }

    private static void period(Map<String, Object> result, Range range) { result.put("from", range.from); result.put("to", range.until - 1); }
    private static void putDate(Map<String, Object> item, Date date) { if (date != null) item.put("date", date.getTime()); }
    private static long dateOf(Map<String, Object> item) { return ((Number)item.getOrDefault("date", 0L)).longValue(); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String pageUrl(int id) { return "/admin/v9/webpages/web-pages-list/?docid=" + id; }

    static class UnavailableException extends RuntimeException {
        UnavailableException(String reason) { super(reason); }
    }
}
