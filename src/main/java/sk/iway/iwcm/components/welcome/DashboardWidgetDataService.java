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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.dmail.jpa.CampaingsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * Small read-only projections. Module rights, current-domain scope, and row access are
 * applied before counting or limiting; browser configuration never grants access.
 */
@Service
public class DashboardWidgetDataService {
    static final int PREVIEW_SIZE = 6;
    static final Set<String> TYPES = Set.of("forms", "newsletter", "audit");
    private static final Set<String> SERVER_TYPES = Set.of("audit");
    private final FormsRepository forms;
    private final FormsServiceImpl formsService;
    private final CampaingsRepository campaigns;

    public DashboardWidgetDataService(FormsRepository forms, FormsServiceImpl formsService, CampaingsRepository campaigns) {
        this.forms = forms;
        this.formsService = formsService;
        this.campaigns = campaigns;
    }

    /** Returns only fields required by the selected widget, never full module entities. */
    public Map<String, Object> load(String type, int days, String formName, Long campaignId,
            Identity user, String domain) {
        validate(type, days, formName, campaignId);
        authorize(type, user);
        if (!SERVER_TYPES.contains(type) && Tools.isEmpty(domain)) throw new UnavailableException("domain-unavailable");
        return switch (type) {
            case "audit" -> audit();
            case "forms" -> forms(user, domain, formName, recentDays(days, Clock.systemDefaultZone()));
            case "newsletter" -> newsletter(domain, campaignId);
            default -> throw new IllegalArgumentException("Unknown widget");
        };
    }

    static void validate(String type, int days, String formName, Long campaignId) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unknown widget");
        if (days != 7 && days != 30 && days != 90) throw new IllegalArgumentException("Invalid period");
        if (formName != null && (formName.length() > 255 || formName.isBlank())) throw new IllegalArgumentException("Invalid form");
        if (campaignId != null && campaignId < 1) throw new IllegalArgumentException("Invalid campaign");
    }

    static void authorize(String type, Identity user) {
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        String permission = switch (type) {
            case "audit" -> "cmp_adminlog";
            case "forms" -> "cmp_form";
            case "newsletter" -> "menuEmail";
            default -> throw new IllegalArgumentException("Unknown widget");
        };
        if (!user.isEnabledItem(permission)) throw new AccessDeniedException("Widget permission is required");
    }

    record Range(long from, long until) {}

    static Range recentDays(int days, Clock clock) {
        return new Range(LocalDate.now(clock).minusDays(days - 1L).atStartOfDay(clock.getZone()).toInstant().toEpochMilli(), clock.millis());
    }

    /** IDs are derived from the current domain and current account, never request parameters. */
    record Scope(List<Integer> groups, List<Integer> pages) {}

    private Scope scope(Identity user, String domain) {
        List<Integer> groups = new ArrayList<>();
        List<Integer> pages = new ArrayList<>();
        boolean onlyPages = Tools.isEmpty(user.getEditableGroups(true)) && Tools.isNotEmpty(user.getEditablePages());
        for (GroupDetails group : GroupsDB.getInstance().getGroupsAll()) {
            if (inDomain(group, domain)) {
                if (!onlyPages && GroupsDB.isGroupEditable(user, group.getGroupId())) groups.add(group.getGroupId());
            }
        }
        for (int id : Tools.getTokensInt(user.getEditablePages(), ",")) {
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(id, false);
            if (doc != null && inDomain(GroupsDB.getInstance().getGroup(doc.getGroupId()), domain)) pages.add(id);
        }
        return new Scope(groups, pages);
    }

    private static boolean inDomain(GroupDetails group, String domain) {
        if (group == null || group.isHiddenInAdmin() || !domain.equalsIgnoreCase(group.getDomainName())) return false;
        String trash = GroupsTreeService.getTrashDirPath();
        return Tools.isEmpty(trash) || group.getFullPath() == null || !group.getFullPath().contains(trash);
    }

    private Predicate accessibleDocument(Expression<Integer> id, CriteriaQuery<?> query, CriteriaBuilder builder, Scope scope) {
        var subquery = query.subquery(Long.class);
        Root<DocDetails> doc = subquery.from(DocDetails.class);
        subquery.select(doc.get("id")).where(builder.or(doc.get("groupId").in(orMissing(scope.groups)), doc.get("id").in(orMissing(scope.pages))));
        return id.in(subquery);
    }

    private static List<Integer> orMissing(List<Integer> ids) { return ids.isEmpty() ? List.of(-1) : ids; }

    /** The audit module grants server-wide access; only the bounded preview fields leave the provider. */
    private Map<String, Object> audit() {
        try (Connection connection = DBPool.getConnection()) {
            return Map.of("items", audit(connection));
        } catch (SQLException exception) { throw new IllegalStateException("Could not load audit events", exception); }
    }

    List<Map<String, Object>> audit(Connection connection) throws SQLException {
        String sql = "SELECT log_id, log_type, description, create_date, user_id FROM " + ConfDB.ADMINLOG_TABLE_NAME + " ORDER BY log_id DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setMaxRows(PREVIEW_SIZE);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    int id = rows.getInt("log_id");
                    String type = Prop.getInstance().getText("components.adminlog." + rows.getInt("log_type"));
                    Map<String, Object> item = item(Integer.toString(id), type, "/admin/v9/apps/audit-search/?id=" + id);
                    item.put("logId", id);
                    item.put("type", type);
                    item.put("description", DB.prepareString(rows.getString("description"), 140));
                    item.put("userFullName", authorName(rows.getInt("user_id")));
                    putDate(item, rows.getTimestamp("create_date"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static String authorName(int userId) {
        var user = userId > 0 ? UsersDB.getUserCached(userId) : null;
        return user == null ? "" : user.getFullName();
    }

    private Map<String, Object> forms(Identity user, String domain, String selected, Range range) {
        Scope scope = scope(user, domain);
        List<String> names = formsService.getFormsList(user).stream().map(FormsEntity::getFormName).sorted().toList();
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

    private Specification<FormsEntity> formSpec(List<String> names, Scope scope, Range range) {
        return (root, query, builder) -> builder.and(root.get("formName").in(names),
            builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()),
            builder.greaterThanOrEqualTo(root.get("createDate"), new Date(range.from)),
            builder.lessThan(root.get("createDate"), new Date(range.until)),
            accessibleDocument(root.get("docId"), query, builder, scope));
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
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    static class UnavailableException extends RuntimeException {
        UnavailableException(String reason) { super(reason); }
    }
}
