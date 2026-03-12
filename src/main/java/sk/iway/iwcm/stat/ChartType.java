package sk.iway.iwcm.stat;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public enum ChartType {

    LINE("line"),
    BAR_VERTICAL("bar_vertical"),
    BAR_HORIZONTAL("bar_horizontal"),
    PIE_CLASSIC("pie_classic"),
    PIE_DONUT("pie_donut"),
    DOUBLE_PIE("double_pie"),
    WORD_CLOUD("word_cloud"),
    TABLE("table"),
    NOT_CHART("not_chart");

    private static final String KEY_PREFIX = "components.stats_by_charts.chart_type.";

    private final String key;

    ChartType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static List<LabelValue> getOptions(Prop prop) {
        List<LabelValue> options = new ArrayList<>();
        for (ChartType type : values()) {
            if (type == NOT_CHART) {
                // Skip NOT_CHART option as it's not a valid visualization choice
                continue;
            }
            options.add(new LabelValue(prop.getText(KEY_PREFIX + type.getKey()), type.getKey()));
        }
        return options;
    }

    public static ChartType getType(String chartType) {
        if (chartType == null) { return NOT_CHART; }

        return switch (chartType.toLowerCase()) {
            case "line" -> LINE;
            case "bar_vertical" -> BAR_VERTICAL;
            case "bar_horizontal" -> BAR_HORIZONTAL;
            case "pie_classic" -> PIE_CLASSIC;
            case "pie_donut" -> PIE_DONUT;
            case "double_pie" -> DOUBLE_PIE;
            case "word_cloud" -> WORD_CLOUD;
            case "table" -> TABLE;
            case "not_chart" -> NOT_CHART;
            default -> NOT_CHART;
        };
    }
}
