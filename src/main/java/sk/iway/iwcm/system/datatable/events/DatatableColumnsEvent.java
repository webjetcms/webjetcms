package sk.iway.iwcm.system.datatable.events;

import org.springframework.context.ApplicationEvent;

import sk.iway.iwcm.system.datatable.json.DataTableColumn;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

import java.util.List;

/**
 * Event fired before DataTable columns JSON is returned,
 * allowing listeners to modify the column definitions.
 *
 * Usage:
 * @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.components.MyEntity'")
 * public void onDatatablesJson(DatatableColumnsEvent event) {
 *     event.getColumns().removeIf(c -> "someField".equals(c.getData()));
 * }
 */
public class DatatableColumnsEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    private List<DataTableColumn> columns;
    private Class<?> dto;
    private String clazz;

    public DatatableColumnsEvent(List<DataTableColumn> columns, Class<?> dto) {
        super(columns);
        this.columns = columns;
        this.dto = dto;
        this.clazz = dto.getName();
    }

    public void publishEvent() {
        WebjetEventPublisher.publishEvent(this);
    }

    public List<DataTableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<DataTableColumn> columns) {
        this.columns = columns;
    }

    public Class<?> getDto() {
        return dto;
    }

    public String getClazz() {
        return clazz;
    }
}
