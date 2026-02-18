package sk.iway.iwcm.system.datatable;

/**
 * Sort definition for use with PageListHolder.
 * Replacement for deprecated org.springframework.beans.support.MutableSortDefinition.
 *
 * @author WebJET
 */
public class SortDefinition {

    private String property;
    private boolean ignoreCase;
    private boolean ascending;

    /**
     * Create a new SortDefinition with default values (ascending sort, case insensitive).
     */
    public SortDefinition() {
        this.property = "";
        this.ignoreCase = true;
        this.ascending = true;
    }

    /**
     * Create a new SortDefinition with the given parameters.
     *
     * @param property the property to compare
     * @param ignoreCase whether upper and lower case in String values should be ignored
     * @param ascending whether to sort ascending (true) or descending (false)
     */
    public SortDefinition(String property, boolean ignoreCase, boolean ascending) {
        this.property = property;
        this.ignoreCase = ignoreCase;
        this.ascending = ascending;
    }

    /**
     * Copy constructor for creating a SortDefinition from another SortDefinition.
     *
     * @param source the source SortDefinition to copy
     */
    public SortDefinition(SortDefinition source) {
        this.property = source.getProperty();
        this.ignoreCase = source.isIgnoreCase();
        this.ascending = source.isAscending();
    }

    /**
     * Get the property to sort by.
     * @return the property name
     */
    public String getProperty() {
        return property;
    }

    /**
     * Set the property to sort by.
     * @param property the property name
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Check whether upper and lower case in String values should be ignored.
     * @return true if case should be ignored
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Set whether upper and lower case in String values should be ignored.
     * @param ignoreCase true to ignore case
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * Check whether to sort ascending or descending.
     * @return true for ascending, false for descending
     */
    public boolean isAscending() {
        return ascending;
    }

    /**
     * Set whether to sort ascending or descending.
     * @param ascending true for ascending, false for descending
     */
    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SortDefinition other)) return false;
        return this.property.equals(other.property) &&
               this.ignoreCase == other.ignoreCase &&
               this.ascending == other.ascending;
    }

    @Override
    public int hashCode() {
        int hashCode = this.property.hashCode();
        hashCode = 29 * hashCode + (this.ignoreCase ? 1 : 0);
        hashCode = 29 * hashCode + (this.ascending ? 1 : 0);
        return hashCode;
    }
}
