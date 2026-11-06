package sk.iway.iwcm.stat;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;

/** Resolves page measurement using the assigned template, before device-specific rendering. */
public enum StatisticsMode {
    INHERIT(0), STATISTICS(1), HEATMAP(2), OFF(3);

    private final int value;

    StatisticsMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static StatisticsMode fromValue(Integer value) {
        for (StatisticsMode mode : values()) {
            if (value != null && mode.value == value) return mode;
        }
        return INHERIT;
    }

    /** Resolves inheritance without treating the click-tracking switch as a page-statistics switch. */
    public static StatisticsMode resolve(Integer templateMode, Integer groupMode, boolean statisticsDisabled,
            boolean clicksEnabled) {
        if (statisticsDisabled) return OFF;
        StatisticsMode mode = fromValue(templateMode);
        if (mode == INHERIT) mode = fromValue(groupMode);
        if (mode == INHERIT || (mode == HEATMAP && !clicksEnabled)) mode = STATISTICS;
        return mode;
    }

    public static StatisticsMode forDocument(int docId) {
        DocDetails doc = docId > 0 ? DocDB.getInstance().getBasicDocDetails(docId, false) : null;
        return forDocument(doc);
    }

    public static StatisticsMode forDocument(DocDetails doc) {
        Integer templateMode = null;
        Integer groupMode = null;
        if (doc != null) {
            int templateId = doc.getTempId();
            GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
            if (group != null && group.isForceTheUseOfGroupTemplate()) templateId = group.getTempId();
            TemplateDetails template = TemplatesDB.getInstance().getTemplate(templateId);
            if (template != null) {
                templateMode = template.getStatisticsMode();
                if (template.getTemplatesGroupId() != null) {
                    TemplatesGroupBean templateGroup = TemplatesGroupDB.getInstance().getByIdCached(template.getTemplatesGroupId());
                    if (templateGroup != null) groupMode = templateGroup.getStatisticsMode();
                }
            }
        }
        return resolve(templateMode, groupMode, "none".equals(Constants.getString("statMode")),
                Constants.getBoolean("statEnableClickTracking"));
    }
}
