package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import sk.iway.iwcm.Logger;

/**
 * PageListHolder is a utility class for pagination and sorting of Lists.
 * Replacement for deprecated org.springframework.beans.support.PagedListHolder.
 *
 * This class is used when you have a List that is not from a database and need to:
 * - extract a page of data
 * - sort the data
 *
 * Usage example:
 * <pre>
 * PageListHolder&lt;MyDTO&gt; holder = new PageListHolder&lt;&gt;(myList);
 * holder.setSort(new SortDefinition("name", true, true));
 * holder.resort();
 * holder.setPage(0);
 * holder.setPageSize(10);
 * List&lt;MyDTO&gt; pageContent = holder.getPageList();
 * </pre>
 *
 * @param <T> the type of elements in the list
 * @author WebJET
 */
public class PagedListHolder<T> {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private List<T> source;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int page = 0;
    private SortDefinition sort;

    /**
     * Create a new PageListHolder with an empty source list.
     */
    public PagedListHolder() {
        this(new ArrayList<>());
    }

    /**
     * Create a new PageListHolder with the given source list.
     *
     * @param source the source list
     */
    public PagedListHolder(List<T> source) {
        this.source = source != null ? new ArrayList<>(source) : new ArrayList<>();
    }

    /**
     * Create a new PageListHolder with the given source list and pageable parameters.
     * This constructor automatically sets page size, page number and sort from pageable,
     * then performs the sort operation.
     *
     * @param source the source list
     * @param pageable the pageable parameters (page, size, sort)
     */
    public PagedListHolder(List<T> source, Pageable pageable) {
        this(source);
        applyPageable(pageable, null);
    }

    /**
     * Create a new PageListHolder with the given source list, pageable parameters and default sort.
     * This constructor automatically sets page size, page number and sort from pageable,
     * then performs the sort operation. If pageable has no sort, the default sort is used.
     *
     * @param source the source list
     * @param pageable the pageable parameters (page, size, sort)
     * @param defaultSort the default sort definition to use when pageable has no sort
     */
    public PagedListHolder(List<T> source, Pageable pageable, SortDefinition defaultSort) {
        this(source);
        applyPageable(pageable, defaultSort);
    }

    /**
     * Apply pageable parameters to this holder.
     *
     * @param pageable the pageable parameters
     * @param defaultSort the default sort definition to use when pageable has no sort (can be null)
     */
    private void applyPageable(Pageable pageable, SortDefinition defaultSort) {
        if (pageable == null) {
            return;
        }

        // Set sort from pageable or use default
        Sort sort = pageable.getSort();
        if (sort != null && !sort.isEmpty()) {
            Sort.Order order = sort.iterator().next();
            setSort(new SortDefinition(order.getProperty(), true, order.isAscending()));
        } else if (defaultSort != null) {
            setSort(defaultSort);
        }

        // Resort if sort is defined
        resort();

        // Set pagination
        setPageSize(pageable.getPageSize());
        setPage(pageable.getPageNumber());
    }

    /**
     * Set the source list.
     *
     * @param source the source list
     */
    public void setSource(List<T> source) {
        this.source = source != null ? new ArrayList<>(source) : new ArrayList<>();
    }

    /**
     * Get the source list. Returns a copy of the internal list.
     *
     * @return the source list
     */
    public List<T> getSource() {
        return new ArrayList<>(source);
    }

    /**
     * Set the current page number (0-indexed).
     *
     * @param page the page number (0-indexed)
     */
    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    /**
     * Get the current page number (0-indexed).
     *
     * @return the page number
     */
    public int getPage() {
        return page;
    }

    /**
     * Set the page size (number of elements per page).
     *
     * @param pageSize the page size
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Get the page size (number of elements per page).
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get the total number of pages.
     *
     * @return the page count
     */
    public int getPageCount() {
        if (source.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil((double) source.size() / pageSize);
    }

    /**
     * Set the sort definition. This does not automatically resort the list.
     * Call {@link #resort()} after setting the sort definition.
     *
     * @param sort the sort definition
     */
    public void setSort(SortDefinition sort) {
        this.sort = sort;
    }

    /**
     * Get the sort definition.
     *
     * @return the sort definition
     */
    public SortDefinition getSort() {
        return sort;
    }

    /**
     * Resort the list according to the current sort definition.
     * Uses BeanWrapper to access properties for comparison.
     */
    public void resort() {
        if (sort == null || sort.getProperty() == null || sort.getProperty().isEmpty()) {
            return;
        }

        String property = sort.getProperty();
        boolean ignoreCase = sort.isIgnoreCase();
        boolean ascending = sort.isAscending();

        try {
            source.sort(new PropertyComparator<>(property, ignoreCase, ascending));
        } catch (Exception e) {
            Logger.warn(PagedListHolder.class, "Error sorting list by property '" + property + "': " + e.getMessage());
        }
    }

    /**
     * Get the list of elements for the current page.
     *
     * @return the page list
     */
    public List<T> getPageList() {
        if (source.isEmpty()) {
            return new ArrayList<>();
        }

        // Normalize page to valid range
        int pageCount = getPageCount();
        if (page >= pageCount) {
            page = pageCount - 1;
        }
        if (page < 0) {
            page = 0;
        }

        int start = page * pageSize;
        int end = Math.min(start + pageSize, source.size());

        if (start >= source.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(source.subList(start, end));
    }

    /**
     * Comparator that compares objects by a named property using BeanWrapper.
     *
     * @param <T> the type of objects to compare
     */
    private static class PropertyComparator<T> implements Comparator<T> {

        private final String property;
        private final boolean ignoreCase;
        private final boolean ascending;

        public PropertyComparator(String property, boolean ignoreCase, boolean ascending) {
            this.property = property;
            this.ignoreCase = ignoreCase;
            this.ascending = ascending;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compare(T o1, T o2) {
            Object v1 = getPropertyValue(o1);
            Object v2 = getPropertyValue(o2);

            int result;

            // Handle nulls - nulls go to the end
            if (v1 == null && v2 == null) {
                result = 0;
            } else if (v1 == null) {
                result = 1;
            } else if (v2 == null) {
                result = -1;
            } else if (v1 instanceof Comparable && v2 instanceof Comparable) {
                // Handle string comparison with case sensitivity
                if (ignoreCase && v1 instanceof String && v2 instanceof String) {
                    result = ((String) v1).compareToIgnoreCase((String) v2);
                } else {
                    result = ((Comparable<Object>) v1).compareTo(v2);
                }
            } else {
                // Fallback to toString comparison
                String s1 = v1.toString();
                String s2 = v2.toString();
                if (ignoreCase) {
                    result = s1.compareToIgnoreCase(s2);
                } else {
                    result = s1.compareTo(s2);
                }
            }

            return ascending ? result : -result;
        }

        private Object getPropertyValue(T object) {
            if (object == null) {
                return null;
            }
            try {
                BeanWrapper beanWrapper = new BeanWrapperImpl(object);
                return beanWrapper.getPropertyValue(property);
            } catch (Exception e) {
                Logger.warn(PropertyComparator.class, "Cannot get property '" + property + "' from object: " + e.getMessage());
                return null;
            }
        }
    }
}
