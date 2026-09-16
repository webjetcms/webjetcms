package sk.iway.iwcm.stat.heat_map;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.ShowDoc;

/**
 * Selects a published content revision for the end of a heat-map reporting period.
 * Publication times are derived from the existing history, which does not retain
 * the actual execution time of scheduled publication.
 */
@Service
public class HeatMapHistoryService {

    private final DocHistoryRepository repository;
    private final HeatMapAccess access;

    public HeatMapHistoryService(DocHistoryRepository repository, HeatMapAccess access) {
        this.repository = repository;
        this.access = access;
    }

    /**
     * Describes the selected preview without exposing document content in metadata.
     * The source is {@code history}, {@code current}, or {@code unavailable}.
     */
    public record PreviewSelection(Integer historyId, Long effectiveFrom, String source,
            boolean historicalUnavailable, boolean multipleVersions, @JsonIgnore DocDetails document) {
    }

    record HistoryMatch(DocHistory history, long effectiveFrom, boolean multipleVersions, boolean available) {
    }

    /**
     * Resolves the historical content after verifying access to the requested page.
     * A missing history falls back to available current content, clearly marked in metadata.
     * A known historical deactivation never falls back to an older or current page.
     *
     * @param request authenticated statistics request
     * @param docId page whose heat map is being displayed
     * @param dateRange reporting range in the statistics date-filter format
     * @return selected content and its provenance
     */
    public PreviewSelection resolve(HttpServletRequest request, int docId, String dateRange) {
        DocDetails current = access.requireDocument(request, docId);
        Date[] range = access.dateRange(dateRange);
        long cutoff = Math.min(range[1].getTime(), System.currentTimeMillis());
        int historyDocId = MultigroupMappingDB.getMasterDocId(docId, true);
        HistoryMatch match = selectHistory(loadHistory(historyDocId, cutoff), historyDocId, range[0].getTime(), cutoff);

        if (match == null) return currentFallback(docId);
        if (!match.available()) {
            return new PreviewSelection(match.history().getHistoryId(), match.effectiveFrom(), "unavailable",
                    false, match.multipleVersions(), null);
        }

        DocHistory history = repository.findById(match.history().getId()).orElse(null);
        if (history == null || history.getDocId() != historyDocId || !isPublished(history)
                || effectiveFrom(history) > cutoff) {
            return currentFallback(docId);
        }
        if (!isPreviewAvailableAt(history, cutoff)) {
            return new PreviewSelection(history.getHistoryId(), effectiveFrom(history), "unavailable",
                    false, match.multipleVersions(), null);
        }

        DocDetails document = DocDB.getInstance().getDoc(-1, history.getHistoryId(), false);
        if (document == null || document.getDocId() != historyDocId) return currentFallback(docId);
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
                    false, match.multipleVersions(), null);
        }
        return new PreviewSelection(history.getHistoryId(), effectiveFrom(history), "history",
                false, match.multipleVersions(), document);
    }

    private PreviewSelection currentFallback(int docId) {
        DocDetails document = DocDB.getInstance().getDoc(docId, -1, false);
        if (document == null || document.getDocId() != docId || !isPreviewAvailableAt(document, System.currentTimeMillis())) {
            return new PreviewSelection(null, null, "unavailable", true, false, null);
        }
        return new PreviewSelection(null, null, "current", true, false, document);
    }

    List<DocHistory> loadHistory(int docId, long cutoff) {
        // Avoid loading every historical content CLOB merely to select one revision.
        return new ComplexQuery().setSql("SELECT history_id, doc_id, save_date, approve_date, approved_by, "
                + "disapproved_by, awaiting_approve, publicable, is_delete, sync_status, available, "
                + "publish_after_start, publish_start, disable_after_end, publish_end "
                + "FROM documents_history WHERE doc_id=? AND save_date<=?")
                .setParams(docId, new Date(cutoff)).list(rs -> {
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
                    history.setAvailable(rs.getBoolean("available"));
                    history.setPublishAfterStart(rs.getBoolean("publish_after_start"));
                    history.setPublishStartDate(rs.getTimestamp("publish_start"));
                    history.setDisableAfterEnd(rs.getBoolean("disable_after_end"));
                    history.setPublishEndDate(rs.getTimestamp("publish_end"));
                    return history;
                });
    }

    /** Selects by effective publication time, without relying on the mutable actual flag. */
    static HistoryMatch selectHistory(List<DocHistory> history, int docId, long from, long cutoff) {
        List<DocHistory> eligible = history.stream()
                .filter(item -> item.getDocId() == docId && isPublished(item) && effectiveFrom(item) <= cutoff)
                .sorted(Comparator.comparingLong(HeatMapHistoryService::effectiveFrom)
                        .thenComparingInt(DocHistory::getHistoryId))
                .toList();
        if (eligible.isEmpty()) return null;

        DocHistory selected = eligible.get(eligible.size() - 1);
        boolean ambiguousNewerHistory = history.stream().anyMatch(item -> item.getDocId() == docId
                && isAcceptedChange(item) && !isPublished(item) && effectiveFrom(item) <= cutoff
                && (effectiveFrom(item) > effectiveFrom(selected)
                        || (effectiveFrom(item) == effectiveFrom(selected) && item.getHistoryId() > selected.getHistoryId())));
        if (ambiguousNewerHistory) return null;

        long versionCount = eligible.stream().mapToLong(HeatMapHistoryService::effectiveFrom)
                .filter(date -> date > from).distinct().count();
        if (eligible.stream().anyMatch(item -> effectiveFrom(item) <= from)) versionCount++;
        return new HistoryMatch(selected, effectiveFrom(selected), versionCount > 1, isPreviewAvailableAt(selected, cutoff));
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
