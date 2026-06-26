package sk.iway.iwcm.headless.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.DocDB;
import sk.iway.iwcm.DocDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.system.fulltext.indexed.Documents;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for searching documents through the headless API.
 * Reuses existing fulltext indexing when available.
 */
@Service("HeadlessSearchService")
public class HeadlessSearchService {

    /**
     * Searches documents by query string with pagination.
     *
     * @param query   the search query
     * @param pageable pagination parameters
     * @param scope   optional scope filter (group ID or path)
     * @param lng     optional language filter
     * @return paginated page of matching documents
     */
    public Page<DocDetails> searchDocuments(String query, Pageable pageable, String scope, String lng) {
        // Try to use existing fulltext index first
        if (Tools.getBoolean("fulltextSearchEnabled", false)) {
            return searchWithFulltext(query, pageable, scope, lng);
        }

        // Fallback: simple SQL LIKE search
        return searchWithLike(query, pageable, scope, lng);
    }

    /**
     * Search using existing fulltext index (Documents class).
     */
    private Page<DocDetails> searchWithFulltext(String query, Pageable pageable, String scope, String lng) {
        try {
            Documents fulltext = Documents.getInstance();
            if (fulltext != null) {
                // Use existing fulltext search
                List<DocDetails> results = fulltext.search(query, scope);
                return buildPage(results, pageable);
            }
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error("HeadlessSearchService.searchWithFulltext", e);
        }

        // Fall back to LIKE search if fulltext is not available
        return searchWithLike(query, pageable, scope, lng);
    }

    /**
     * Fallback: simple SQL LIKE search.
     */
    private Page<DocDetails> searchWithLike(String query, Pageable pageable, String scope, String lng) {
        List<DocDetails> allDocs = new ArrayList<>();

        try (Connection conn = sk.iway.iwcm.DBPool.getConnection()) {
            String sql = "SELECT d.doc_id, d.title, d.virtual_path, d.lng_code, d.doc_data, d.perex_image " +
                         "FROM documents d " +
                         "WHERE d.doc_data LIKE ? AND d.doc_id > 0 " +
                         "ORDER BY d.doc_id DESC";

            String searchPattern = "%" + query.replace("%", "\\%").replace("_", "\\_") + "%";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, searchPattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DocDetails doc = new DocDetails();
                        doc.setDocId(rs.getInt("doc_id"));
                        doc.setTitle(rs.getString("title"));
                        String vp = rs.getString("virtual_path");
                        if (vp != null) {
                            doc.setVirtualPath(vp);
                        }
                        String data = rs.getString("doc_data");
                        if (data != null) {
                            doc.setData(data);
                        }
                        String perex = rs.getString("perex_image");
                        if (perex != null) {
                            doc.setPerexImage(perex);
                        }
                        allDocs.add(doc);
                    }
                }
            }
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error("HeadlessSearchService.searchWithLike", e);
        }

        return buildPage(allDocs, pageable);
    }

    /**
     * Builds a Page from a list of DocDetails.
     */
    private Page<DocDetails> buildPage(List<DocDetails> allDocs, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), allDocs.size());
        int end = Math.min((start + pageable.getPageSize()), allDocs.size());
        long totalElements = allDocs.size();

        List<DocDetails> pageContent = allDocs.subList(start, end);

        return new sk.iway.iwcm.system.datatable.DatatablePageImpl<>(pageContent, pageable, totalElements);
    }
}
