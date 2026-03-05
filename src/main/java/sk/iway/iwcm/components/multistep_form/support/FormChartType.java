package sk.iway.iwcm.components.multistep_form.support;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public enum FormChartType {
    TABLE("table"),
    BAR_VERTICAL("bar_vertical"),
    BAR_HORIZONTAL("bar_horizontal"),
    PIE_CLASSIC("pie_classic"),
    PIE_DONUT("pie_donut");

    private static final String KEY_PREFIX = "components.stats_by_charts.chart_type.";

    private final String key;

    FormChartType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static List<LabelValue> getOptions(Prop prop) {
        List<LabelValue> options = new ArrayList<>();
        for (FormChartType type : values()) {
            options.add(new LabelValue(prop.getText(KEY_PREFIX + type.getKey()), type.getKey()));
        }
        return options;
    }
}