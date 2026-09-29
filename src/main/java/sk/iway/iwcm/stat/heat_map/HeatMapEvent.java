package sk.iway.iwcm.stat.heat_map;

/**
 * An anonymous click in CSS document coordinates, identified only for replay prevention.
 *
 * @param eventId lowercase hexadecimal event identifier
 * @param docId published document identifier
 * @param viewportWidth CSS viewport width when the click occurred
 * @param epochSeconds click time as Unix seconds
 * @param x horizontal document coordinate
 * @param y vertical document coordinate
 */
public record HeatMapEvent(String eventId, int docId, int viewportWidth, long epochSeconds, int x, int y) {
    public HeatMapEvent {
        if (eventId == null || !eventId.matches("[0-9a-f]{32}") || docId <= 0
                || viewportWidth < 1 || viewportWidth > HeatMapStorage.MAX_VIEWPORT_WIDTH
                || epochSeconds < 0 || x < 0 || y < 0
                || x > HeatMapStorage.MAX_COORDINATE || y > HeatMapStorage.MAX_COORDINATE) {
            throw new IllegalArgumentException("Invalid heatmap event");
        }
    }
}
