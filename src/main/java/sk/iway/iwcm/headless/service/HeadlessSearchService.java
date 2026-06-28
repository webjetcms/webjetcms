package sk.iway.iwcm.headless.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.SearchAction;
import sk.iway.iwcm.doc.SearchActionInput;
import sk.iway.iwcm.doc.SearchActionOutput;
import sk.iway.iwcm.doc.SearchDetails;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for searching documents through the headless API.
 * Uses SearchAction.search() which handles fulltext indexing,
 * group filtering, language filtering, and pagination.
 */
@Service("HeadlessSearchService")
public class HeadlessSearchService {

    /**
     * Searches documents by query string with pagination.
     * Uses SearchAction.search() which handles fulltext indexing,
     * group filtering, language filtering, and pagination.
     *
     * @param query   the search query
     * @param pageable pagination parameters
     * @param scope   optional scope filter (group ID or path)
     * @param lng     optional language filter
     * @return paginated page of matching documents
     */
    public Page<DocDetails> searchDocuments(String query, Pageable pageable, String scope, String lng) {
        // Build SearchActionInput from parameters
        SearchActionInput input = new SearchActionInput();
        input.setParameter("words", query);
        input.setParameter("perpage", String.valueOf(pageable.getPageSize()));
        input.setParameter("page", String.valueOf(pageable.getPageNumber() + 1)); // 1-based
        if (scope != null && !scope.isEmpty()) {
            input.setParameter("groupId", scope);
        }
        if (lng != null && !lng.isEmpty()) {
            input.setParameter("lng", lng);
        }

        // Call SearchAction.search() - the single source of truth for search
        SearchActionOutput output = SearchAction.search(input);

        // Check for errors
        if (output.isWrong() || output.isNotFound() || output.isCrossTimeout()
                || output.isCrossHourlyLimit() || output.isNotFoundPublishStartEnd()) {
            return buildEmptyPage(pageable);
        }

        // Extract results from SearchActionOutput
        List<SearchDetails> searchResults = output.getResults();
        if (searchResults == null || searchResults.isEmpty()) {
            return buildEmptyPage(pageable);
        }

        // SearchDetails extends DocDetails, so we can treat them as DocDetails
        List<DocDetails> docs = new ArrayList<>(searchResults);

        // Build Page from SearchActionOutput metadata
        int totalResults = output.getTotalResults() != null ? output.getTotalResults() : docs.size();
        return new DatatablePageImpl<>(docs, pageable, totalResults);
    }

    private Page<DocDetails> buildEmptyPage(Pageable pageable) {
        return new DatatablePageImpl<>(new ArrayList<>(), pageable, 0);
    }
}
