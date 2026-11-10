package sk.iway.iwcm.stat.heat_map;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.spring.events.DocumentPublishEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

/**
 * Resolves published content revisions and their validity within a heat-map reporting period.
 * Publication times are derived from the existing history, which does not retain
 * the actual execution time of scheduled publication.
 */
@Service
public class HeatMapHistoryService {

    private static final String CACHE_PREFIX = "HeatMapHistoryService.periods.";
    private record PageReports(ConcurrentLruCache<ReportRange, HistoryMatch> ranges) { }

    private final DocHistoryRepository repository;
    private final HeatMapAccess access;

    public HeatMapHistoryService(DocHistoryRepository repository, HeatMapAccess access) {
        this.repository = repository;
        this.access = access;
    }

    /** Publication and report boundaries in epoch milliseconds, with an exclusive end. */
    public record ReportRange(long from, long until) {
        public ReportRange {
            if (from >= until) throw new IllegalArgumentException("Empty heatmap date range");
        }

        public static ReportRange of(Date[] inclusiveDates) {
            return new ReportRange(inclusiveDates[0].getTime(), inclusiveDates[1].getTime() + 1);
        }

        public LocalDate firstDay() {
            return Instant.ofEpochMilli(from).atZone(ZoneId.systemDefault()).toLocalDate();
        }

        /** The last statistics day touched by this interval, including a partial day. */
        public LocalDate lastDay() {
            return Instant.ofEpochMilli(until - 1).atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    /**
     * Describes the selected preview without exposing document content in metadata.
     * The source is {@code history}, {@code current}, or {@code unavailable}.
     */
    public record PreviewSelection(Integer historyId, Long effectiveFrom, String source,
            boolean historicalUnavailable, List<VersionPeriod> versions, ReportRange clickRange, @JsonIgnore DocDetails document) {
    }

    /** A published version and the portion of the report during which it was live. */
    public record VersionPeriod(int historyId, long effectiveFrom, ReportRange range) { }

    record HistoryMatch(DocHistory history, List<VersionPeriod> versions, boolean available) {
        HistoryMatch {
            versions = List.copyOf(versions);
        }

        VersionPeriod requireVersion(int id) {
            for (VersionPeriod version : versions) {
                if (version.historyId() == id) return version;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Resolves the historical content after verifying access to the requested page.
     * A missing history falls back to available current content, clearly marked in metadata.
     * A known historical deactivation never falls back to an older or current page.
     *
     * @param request authenticated statistics request
     * @param docId page whose heat map is being displayed
     * @param dateRange reporting range in the statistics date-filter format
     * @param versionId optional published version within the reporting period
     * @return selected content and its provenance
     */
    public PreviewSelection resolve(HttpServletRequest request, int docId, String dateRange, Integer versionId) {
        DocDetails current = access.requireDocument(request, docId);
        Date[] range = access.dateRange(dateRange);
        long cutoff = Math.min(range[1].getTime(), System.currentTimeMillis());
        int historyDocId = MultigroupMappingDB.getMasterDocId(docId, true);
        ReportRange report = ReportRange.of(range);
        HistoryMatch match = versionId == null ? reportHistory(historyDocId, report) : versionHistory(historyDocId, report);
        VersionPeriod version = versionId == null ? null : match.requireVersion(versionId);
        ReportRange clicks = version == null ? report : version.range();
        if (version == null && match.history() == null) return currentFallback(docId, match.versions(), clicks);
        if (version == null && !match.available()) {
            return new PreviewSelection(match.history().getHistoryId(), effectiveFrom(match.history()), "unavailable",
                    false, match.versions(), clicks, null);
        }
        if (version != null) cutoff = version.range().from();
        int historyId = version == null ? match.history().getHistoryId() : version.historyId();
        DocHistory history = repository.findById((long) historyId).orElse(null);
        if (history == null || history.getDocId() != historyDocId || !isPublished(history)
                || effectiveFrom(history) > cutoff) {
            if (version != null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            return currentFallback(docId, match.versions(), clicks);
        }
        if (!isPreviewAvailableAt(history, cutoff)) {
            return new PreviewSelection(history.getHistoryId(), effectiveFrom(history), "unavailable",
                    false, match.versions(), clicks, null);
        }

        DocDetails document = DocDB.getInstance().getDoc(-1, history.getHistoryId(), false);
        if (document == null || document.getDocId() != historyDocId) {
            if (version != null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            return currentFallback(docId, match.versions(), clicks);
        }
        if (historyDocId != docId) {
            // Shared content retains the requested page's routing and folder context.
            document.setDocId(docId);
            document.setGroupId(current.getGroupId());
            document.setVirtualPath(current.getVirtualPath());
            document.setExternalLink(current.getExternalLink());
            document.setSortPriority(current.getSortPriority());
        }
        document.setHistoryId(history.getHistoryId());
        if (!isPreviewAvailableAt(document, cutoff)) {
            return new PreviewSelection(history.getHistoryId(), effectiveFrom(history), "unavailable",
                    false, match.versions(), clicks, null);
        }
        return new PreviewSelection(history.getHistoryId(), effectiveFrom(history), "history",
                false, match.versions(), clicks, document);
    }

    /** Resolves a version filter for click queries after the caller has authorized the document. */
    public ReportRange versionRange(int docId, Date[] range, int versionId) {
        int historyDocId = MultigroupMappingDB.getMasterDocId(docId, true);
        return versionHistory(historyDocId, ReportRange.of(range)).requireVersion(versionId).range();
    }

    private HistoryMatch versionHistory(int docId, ReportRange range) {
        return pageReports(docId).ranges().get(range);
    }

    // Keep at most eight date ranges per recently viewed master page. Database reads run outside this lock.
    private synchronized PageReports pageReports(int docId) {
        Cache cache = Cache.getInstance();
        String key = CACHE_PREFIX + docId;
        PageReports reports = cache.getObject(key, PageReports.class);
        if (reports == null) {
            reports = new PageReports(new ConcurrentLruCache<>(8, range -> reportHistory(docId, range)));
            cache.setObjectSeconds(key, reports, 30, false);
        }
        return reports;
    }

    /** Document cache refreshes on this node also cover scheduled publication and cluster notifications. */
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.system.spring.events.DocumentPublishEvent'")
    public void pagePublished(WebjetEvent<DocumentPublishEvent> event) {
        Cache.getInstance().removeObject(CACHE_PREFIX + event.getSource().getDocument().getDocId());
    }

    /** Evicts intervals on all nodes after deleting history, including bulk cleanup without a page ID. */
    public static void historyDeleted(Integer docId) {
        Cache cache = Cache.getInstance();
        boolean refreshCluster = ClusterDB.isServerRunningInClusterMode();
        if (docId == null) cache.removeObjectStartsWithName(CACHE_PREFIX, refreshCluster);
        else cache.removeObject(CACHE_PREFIX + docId, refreshCluster);
    }

    private HistoryMatch reportHistory(int historyDocId, ReportRange range) {
        long end = range.until() - 1;
        return selectHistory(loadHistory(historyDocId, Math.min(end, System.currentTimeMillis())), historyDocId, range.from(), end);
    }

    private PreviewSelection currentFallback(int docId, List<VersionPeriod> versions, ReportRange clicks) {
        DocDetails document = DocDB.getInstance().getDoc(docId, -1, false);
        if (document == null || document.getDocId() != docId || !isPreviewAvailableAt(document, System.currentTimeMillis())) {
            return new PreviewSelection(null, null, "unavailable", true, versions, clicks, null);
        }
        return new PreviewSelection(null, null, "current", true, versions, clicks, document);
    }

    List<DocHistory> loadHistory(int docId, long cutoff) {
        // Match ShowDoc's case-sensitive marker in the database without transferring historical content CLOBs.
        String remapPosition = switch (Constants.DB_TYPE) {
            case Constants.DB_ORACLE -> "DBMS_LOB.INSTR(data, ?)";
            case Constants.DB_PGSQL -> "STRPOS(data, ?)";
            case Constants.DB_MSSQL -> "CHARINDEX(?, CAST(data AS NVARCHAR(MAX)) COLLATE Latin1_General_100_BIN2)";
            default -> "LOCATE(BINARY ?, data)";
        };
        return new ComplexQuery().setSql("SELECT history_id, doc_id, save_date, approve_date, approved_by, "
                + "disapproved_by, awaiting_approve, publicable, is_delete, sync_status, available, external_link, "
                + "CASE WHEN " + remapPosition + " > 0 THEN 1 ELSE 0 END AS is_remapped, "
                + "publish_after_start, publish_start, disable_after_end, publish_end "
                + "FROM documents_history WHERE doc_id=? AND save_date<=?")
                .setParams(ShowDoc.REMAP_STRING_START, docId, new Date(cutoff)).list(rs -> {
                    DocHistory history = new DocHistory();
                    history.setHistoryId(rs.getInt("history_id"));
                    history.setDocId(rs.getInt("doc_id"));
                    history.setSaveDate(rs.getTimestamp("save_date"));
                    history.setApproveDate(rs.getTimestamp("approve_date"));
                    int approvedBy = rs.getInt("approved_by");
                    history.setApprovedBy(rs.wasNull() ? null : approvedBy);
                    history.setDisapprovedBy(rs.getInt("disapproved_by"));
                    history.setAwaitingApprove(rs.getString("awaiting_approve"));
                    history.setPublicable(rs.getBoolean("publicable"));
                    history.setIsDelete(rs.getBoolean("is_delete"));
                    history.setSyncStatus(rs.getInt("sync_status"));
                    history.setExternalLink(rs.getString("external_link"));
                    history.setAvailable(rs.getBoolean("available") && !rs.getBoolean("is_remapped"));
                    history.setPublishAfterStart(rs.getBoolean("publish_after_start"));
                    history.setPublishStartDate(rs.getTimestamp("publish_start"));
                    history.setDisableAfterEnd(rs.getBoolean("disable_after_end"));
                    history.setPublishEndDate(rs.getTimestamp("publish_end"));
                    return history;
                });
    }

    /** Selects by effective publication time, without relying on the mutable actual flag. */
    static HistoryMatch selectHistory(List<DocHistory> history, int docId, long from, long reportEnd) {
        long cutoff = Math.min(reportEnd, System.currentTimeMillis());
        long until = reportEnd + 1;
        Comparator<DocHistory> publicationOrder = Comparator.comparingLong(HeatMapHistoryService::effectiveFrom)
                .thenComparingInt(DocHistory::getHistoryId);
        List<DocHistory> changes = history.stream()
                .filter(item -> item.getDocId() == docId && isAcceptedChange(item) && effectiveFrom(item) <= cutoff)
                .sorted(publicationOrder.reversed()).toList();

        List<VersionPeriod> versions = new ArrayList<>();
        for (DocHistory change : changes) {
            long published = effectiveFrom(change);
            long start = Math.max(from, published);
            long end = until;
            if (change.isDisableAfterEnd() && change.getPublishEndDate() != null) {
                end = Math.min(end, change.getPublishEndDate().getTime());
            }
            if (start <= cutoff && start < end && isPublished(change) && isPreviewAvailableAt(change, start)) {
                versions.add(new VersionPeriod(change.getHistoryId(), published, new ReportRange(start, end)));
            }
            // Even an unavailable or ambiguous change ends the previous version.
            until = published;
            if (until <= from) break;
        }
        DocHistory latest = changes.isEmpty() ? null : changes.get(0);
        if (latest == null || !isPublished(latest)) return new HistoryMatch(null, versions, false);
        return new HistoryMatch(latest, versions, isPreviewAvailableAt(latest, cutoff));
    }

    private static boolean isPublished(DocHistory history) {
        return isAcceptedChange(history) && history.getApprovedBy() != null
                && (!history.isPublishAfterStart() || history.getPublishStartDate() != null)
                && (!history.isDisableAfterEnd() || history.getPublishEndDate() != null);
    }

    private static boolean isAcceptedChange(DocHistory history) {
        return history.getId() != null && history.getSaveDate() != null
                && (history.getApprovedBy() == null || history.getApprovedBy() >= 0)
                && (history.getDisapprovedBy() == null || history.getDisapprovedBy() <= 0)
                && (history.getAwaitingApprove() == null || history.getAwaitingApprove().isBlank())
                && !Boolean.TRUE.equals(history.getPublicable()) && !Boolean.TRUE.equals(history.getIsDelete())
                && !(history.isPublishAfterStart() && history.getSyncStatus() == 2);
    }

    private static long effectiveFrom(DocHistory history) {
        long date = history.getSaveDate().getTime();
        if (history.getApproveDate() != null) date = Math.max(date, history.getApproveDate().getTime());
        if (history.isPublishAfterStart() && history.getPublishStartDate() != null) {
            date = Math.max(date, history.getPublishStartDate().getTime());
        }
        return date;
    }

    private static boolean isPreviewAvailableAt(DocBasic document, long cutoff) {
        return document.isAvailable() && !(document.isDisableAfterEnd()
                && document.getPublishEndDate() != null && document.getPublishEndDate().getTime() <= cutoff)
                && (document.getExternalLink() == null || document.getExternalLink().isBlank())
                && !document.getData().contains(ShowDoc.REMAP_STRING_START);
    }
}
