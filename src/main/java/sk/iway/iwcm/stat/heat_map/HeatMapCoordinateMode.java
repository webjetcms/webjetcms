package sk.iway.iwcm.stat.heat_map;

/** Identifies the horizontal origin of stored clicks; Y always uses document coordinates. */
public enum HeatMapCoordinateMode {
    DOCUMENT(0), CONTAINER(1);

    private final int databaseValue;

    HeatMapCoordinateMode(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static HeatMapCoordinateMode fromDatabase(int value) {
        return switch (value) {
            case 0 -> DOCUMENT;
            case 1 -> CONTAINER;
            default -> throw new IllegalArgumentException("Invalid heatmap coordinate mode");
        };
    }
}
