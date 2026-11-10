package sk.iway.iwcm.stat.heat_map;

/**
 * An anonymous click with document-relative Y and optional container-relative X.
 *
 * @param eventId lowercase hexadecimal event identifier
 * @param docId published document identifier
 * @param viewportWidth CSS viewport width when the click occurred
 * @param epochSeconds click time as Unix seconds
 * @param x horizontal document or container coordinate
 * @param y vertical document coordinate
 * @param containerWidth normalized container width, or zero for document coordinates
 */
public record HeatMapEvent(String eventId, int docId, int viewportWidth, long epochSeconds, int x, int y, int containerWidth) {
    /** Preserves the document-coordinate contract of pending v1 events. */
    public HeatMapEvent(String eventId, int docId, int viewportWidth, long epochSeconds, int x, int y) {
        this(eventId, docId, viewportWidth, epochSeconds, x, y, 0);
    }

    public HeatMapEvent {
        if (eventId == null || !eventId.matches("[0-9a-f]{32}") || docId <= 0
                || viewportWidth < 1 || viewportWidth > HeatMapStorage.MAX_VIEWPORT_WIDTH
                || epochSeconds < 0 || x < 0 || y < 0 || containerWidth < 0 || containerWidth > viewportWidth
                || (containerWidth > 0 && x > containerWidth)
                || x > HeatMapStorage.MAX_COORDINATE || y > HeatMapStorage.MAX_COORDINATE) {
            throw new IllegalArgumentException("Invalid heatmap event");
        }
    }
}
