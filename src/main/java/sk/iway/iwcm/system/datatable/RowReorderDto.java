package sk.iway.iwcm.system.datatable;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for row reordering in DataTables.
 * Used to update entity positions when user drags and drops rows in the table.
 */
@Getter
@Setter
public class RowReorderDto {
    /** Name of the column that stores the order value */
    private String dataSrc;

    /** List of row reorder values containing entity IDs and their old/new values */
    private List<RowReorderValue> values;

    /**
     * Get list of entity IDs from the values
     * @return list of entity IDs
     */
    public List<Long> getIds() {
        return values.stream().map(RowReorderValue::getId).toList();
    }

    /**
     * Get the new order value for specific entity ID
     * @param id entity ID
     * @return new order value or null if not found
     */
    public Integer getNewValueById(Long id) {
        return values.stream()
            .filter(v -> v.getId().equals(id))
            .findFirst()
            .map(RowReorderValue::getNewValue)
            .orElse(null);
    }

    /**
     * Inner class representing a single row reorder operation
     */
    @Getter
    @Setter
    public static class RowReorderValue {
        /** Entity ID */
        private Long id;

        /** Original order value before reordering */
        private Integer oldValue;

        /** New order value after reordering */
        private Integer newValue;

        public RowReorderValue() {}

        @JsonCreator
        public RowReorderValue(
            @JsonProperty("id") Long id,
            @JsonProperty("oldValue") Integer oldValue,
            @JsonProperty("newValue") Integer newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
