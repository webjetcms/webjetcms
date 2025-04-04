package sk.iway.iwcm.components.memory_cleanup.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

import java.util.Date;

/**
 * Trieda sluzi ako databazova entita pre tabulku stat_datatable_data. Pouzivaju sa v nej @DataTableColumn anotacie ktore
 * definuju rozne funkcie pre datatabulku.
 *
 * @author pgajdos
 * @since 24.07.2020
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseDeleteBean {

    @DataTableColumn(inputType = DataTableColumnType.ID, filter = false)
    private Long id;

    @DataTableColumn(
            renderFormat = "dt-format-text-wrap",
            title = "components.memory_cleanup.name"
    )
    private String name;

    @DataTableColumn(
            renderFormat = "dt-format-select",
            title = "components.memory_cleanup.group_name",
            editor = @DataTableColumnEditor(
                    type = "select"
            )
    )
    private int groupId;

    @DataTableColumn(
            renderFormat = "dt-format-date-time",
            title = "components.memory_cleanup.time_frame",
            editor = @DataTableColumnEditor(
                    type = "datetime"
            ),
            visible = false,
            filter = false
    )
    private Date from;

    @DataTableColumn(
            renderFormat = "dt-format-date-time",
            title = "components.memory_cleanup.time_frame_to",
            editor = @DataTableColumnEditor(
                    type = "datetime"
            ),
            visible = false,
            filter = false
    )
    private Date to;

    @DataTableColumn(
            renderFormat = "dt-format-number",
            title = "components.memory_cleanup.number_of_entries"
    )
    private int numberOfEntriesToDelete;

    private String tableName;

    private int typeId;

    private boolean tablePartitioning;
}

