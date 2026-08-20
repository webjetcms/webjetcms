package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import sk.iway.iwcm.doc.DocDetails;

/**
 * Paginated response envelope for the headless news listing endpoint.
 * Contains news items (DocDetails) and pagination metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeadlessNewsResponse {

    private List<DocDetails> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<DocDetails> getItems() {
        return items;
    }

    public void setItems(List<DocDetails> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
