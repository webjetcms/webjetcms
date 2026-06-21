package sk.iway.iwcm.components.multistep_form.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.audit.jpa.AuditLogEntity;
import sk.iway.iwcm.system.audit.jpa.AuditRepository;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.users.UserDetails;

@Service
public class FormStatService {

    private static final int PAGE_SIZE = 1000;
    private static final int PIE_TOP_COUNT = 5;
    private static final int TABLE_TOP_COUNT = 9;
    private static final String UNKNOWN_VALUE = "unknown";
    private static final String NOT_ANSWERED = "components.stats_by_charts.unanswered";

    private final FormsRepository formsRepository;
    private final FormSettingsRepository formSettingsRepository;
    private final FormsServiceImpl formsService;
    private final AuditRepository auditRepository;
    private final MultistepFormsService multistepFormsService;

    @Autowired
    public FormStatService(FormsRepository formsRepository, FormSettingsRepository formSettingsRepository, FormsServiceImpl formsService, AuditRepository auditRepository, MultistepFormsService multistepFormsService) {
        this.formsRepository = formsRepository;
        this.formSettingsRepository = formSettingsRepository;
        this.formsService = formsService;
        this.auditRepository = auditRepository;
        this.multistepFormsService = multistepFormsService;
    }

    /**
     * Thread-safe context object for sharing computed values across methods within a single request.
     * Each request creates its own instance on the stack, ensuring thread safety without synchronization.
     */
    private class StatContext {

        final String formName;
        final int formId;
        final int domainId;
        final Prop prop;
        final Date[] dateRange;
        final Map<String, String> columnNames;
        final Map<String, FormItemEntity> allowedItemsEntities;

        /**
         * Creates a context for all form items or for one selected item.
         *
         * @param formName the form name identifier
         * @param itemFormId optional form item identifier, or {@code null} for all items
         * @param request the HTTP request used for localization and date filters
         */
        StatContext(String formName, String itemFormId, HttpServletRequest request) {
            this.formName = formName;
            this.domainId = CloudToolsForCore.getDomainId();
            this.prop = Prop.getInstance(request);
            this.formId = multistepFormsService.getFormId(formName);
            this.columnNames = getColumnNames(this.formName, new UserDetails(request), this.prop);
            this.allowedItemsEntities = getAllowedItems(this.formName, itemFormId);
            this.dateRange = getDateRange(request);
        }

        /**
         * Creates a context for all statistic-enabled items in a form.
         *
         * @param formName the form name identifier
         * @param request the HTTP request used for localization and date filters
         */
        StatContext(String formName, HttpServletRequest request) {
            this(formName, null, request);
        }

        /**
         * Resolves the selected statistics date range from request parameters.
         *
         * @param request the HTTP request containing {@code dayDate} or {@code searchDayDate}
         * @return date range accepted by statistics repository queries
         */
        private Date[] getDateRange(HttpServletRequest request) {
            String stringRange = Tools.getStringValue(request.getParameter("dayDate"), "");
            if(Tools.isEmpty(stringRange)) stringRange = Tools.getStringValue(request.getParameter("searchDayDate"), "");

            return StatService.processDateRangeString(stringRange);
        }

        /**
         * Retrieves display column names for a form's fields.
         * @param formName the form name identifier
         * @param currentUser the current user
         * @param prop localization properties
         * @return map of itemFormId to localized column label
         */
        private Map<String, String> getColumnNames(String formName, UserDetails currentUser, Prop prop) {
            HashMap<String, String> columnNamesMap = new LinkedHashMap<>();
            for(LabelValue column : formsService.getColumnNames(formName, currentUser, prop).getColumns()) {
                columnNamesMap.put(column.getValue(), column.getLabel());
            }
            return columnNamesMap;
        }

        /**
         * Loads form items that are enabled for statistics ({@code show_stat=1}).
         * Results are ordered by step and item priority.
         * Consecutive radio items with the same {@code itemFormId} are collapsed to one record.
         *
         * @param formName the form name identifier
         * @param itemFormId optional filter for a specific item, or {@code null} for all items
         * @return ordered map of itemFormId to item configuration
         */
        private Map<String, FormItemEntity> getAllowedItems(String formName, String itemFormId) {
            String sql = "SELECT item_form_id, value, field_type, chart_type, top_count, show_other_count, show_unanswered, compare_insensitive, color_scheme, error_count FROM form_items f, form_steps s WHERE f.form_name = ? AND f.domain_id = ? AND f.show_stat = 1 AND f.step_id=s.id";
            List<Object> sqlParams = new ArrayList<>();
            sqlParams.add(formName);
            sqlParams.add(CloudToolsForCore.getDomainId());

            if(Tools.isNotEmpty(itemFormId)) {
                sql += " AND item_form_id = ?";
                sqlParams.add(itemFormId);
            }

            sql += " ORDER BY s.sort_priority ASC, f.sort_priority ASC";

            Map<String, FormItemEntity> values = new LinkedHashMap<>();
            List<String> previous = new ArrayList<>();
            previous.add("");
            new ComplexQuery().setSql(sql).setParams(sqlParams.toArray()).list(new Mapper<FormItemEntity>() {
                @Override
                public FormItemEntity map(ResultSet rs) throws SQLException {
                    FormItemEntity stepItem = resultSetToItemEntity(rs);

                    // Radio buttons may be stored as separate rows; collapse consecutive rows with the same id.
                    if("radio".equals(stepItem.getFieldType()) &&  previous.get(0).equals(stepItem.getItemFormId())) return null;
                    previous.set(0, stepItem.getItemFormId());

                    values.put(stepItem.getItemFormId(), stepItem);

                    return null;
                }
            });

            return values;
        }

        /**
         * Maps one SQL row to {@link FormItemEntity} with statistics-related attributes.
         *
         * @param rs the current result set row
         * @return populated form item entity
         * @throws SQLException when a column value cannot be read
         */
        private FormItemEntity resultSetToItemEntity(ResultSet rs) throws SQLException {
            FormItemEntity fe = new FormItemEntity();
            fe.setItemFormId( rs.getString("item_form_id") );
            fe.setValue( rs.getString("value") );
            fe.setFieldType( rs.getString("field_type") );
            fe.setChartType( rs.getString("chart_type") );
            fe.setTopCount( rs.getInt("top_count") );
            fe.setShowOtherCount( rs.getBoolean("show_other_count") );
            fe.setShowUnanswered( rs.getBoolean("show_unanswered") );
            fe.setCompareInsensitive( rs.getBoolean("compare_insensitive") );
            fe.setColorScheme( rs.getString("color_scheme") );
            fe.setErrorCount( rs.getInt("error_count") );
            return fe;
        }
    }

    /**
     * Returns an aggregated statistics payload for a form.
     * The payload contains timing metrics, response counters, chart data,
     * bonus chart data, and validation error chart data.
     *
     * @param formName the form name identifier
     * @param request the HTTP request used to resolve localization
     * @return JSON object with all calculated statistics, or {@code null} when formName is empty
     */
    public final JSONObject getFormStatData(String formName, HttpServletRequest request) {
        if(Tools.isEmpty(formName)) return null;

        JSONObject allData = new JSONObject();

        // Create shared context - computed once and passed to all methods for thread safety
        StatContext context = new StatContext(formName, request);

        // they contain ONLY bonus data like duration, dateCreated, etc..
        List<FormsEntity> formEntities = getFormEntities(context);
        allData.put("averageDuration", computeAverageDuration(formEntities));
        allData.put("durationDays", computeDurationDays(context));
        allData.put("lastResponseDateTime", getLastResponseDateTime(formEntities));
        allData.put("viewCount", getFormViewCount(context.formName));
        allData.put("responseAttempts", getResponseAttempts(context.formName));

        // form data - aka users answers to fields
        List<String> allFormData = getAllFormData(context);
        allData.put("totalResponses", allFormData.size());

        allData.put("chartData", getFormStatChartData(allFormData, context));
        allData.put("chartBonusData", getFormStatChartBonusData(formEntities, context));
        allData.put("chartErrorData", getFormStatChartErrorData(context));

        return allData;
    }

    /**
     * Returns chart data for the given form item.
     *
     * @param formName the form name identifier
     * @param itemFormId form item identifier that must be non-empty
     * @param request the HTTP request used to resolve localization
     * @return JSON array with chart data, or {@code null} when required parameters are empty
     */
    public final JSONArray getFormStatChartData(String formName, String itemFormId, HttpServletRequest request) {
        if(Tools.isEmpty(formName) || Tools.isEmpty(itemFormId)) return null;

        // Create shared context - computed once and passed to all methods for thread safety
        StatContext context = new StatContext(formName, itemFormId, request);

        return getFormStatChartData(getAllFormData(context), context);
    }

    /* ------------------------- */

    /**
     * Finds the most recent response timestamp across all form entities.
     *
     * @param formEntities list of form submissions
     * @return formatted date-time string of the latest response, or an empty string when unavailable
     */
    private String getLastResponseDateTime(List<FormsEntity> formEntities) {
        Date maxDate = formEntities.stream()
                .map(FormsEntity::getCreateDate)
                .filter(d -> d != null)
                .max(Date::compareTo)
                .orElse(null);

        if(maxDate == null) return "";
        return Tools.formatDateTime(maxDate);
    }

    /**
     * Calculates how many days have passed since the form was created.
     * Falls back to the first response date when the creation timestamp is unavailable.
     *
     * @param context shared request statistics context
     * @return number of elapsed days, {@code < 1} for same-day forms, or {@code 0} when unknown
     */
    private String computeDurationDays(StatContext context) {
        Long formCreationEpoch = formsRepository.getFormCreationDuration(context.formName, context.domainId).orElse(null);
        if(formCreationEpoch == null  || formCreationEpoch <= 0L) {
            //try to use first response as fallback
            Date minDate = formsRepository.getMinFormCreateDate(context.formName, context.domainId).orElse(null);
            if(minDate != null) {
                formCreationEpoch = minDate.getTime() / 1000;
            }
        }

        if(formCreationEpoch == null || formCreationEpoch <= 0) return "0";
        long currentEpoch = Tools.getNow() / 1000;
        long durationDays = (currentEpoch - formCreationEpoch) / (60 * 60 * 24);
        if(durationDays < 1) return "< 1";
        return String.valueOf(durationDays);
    }

    /**
     * Computes the average time users spend filling out the form.
     * Only entities with non-null createDate and positive duration are included.
     *
     * @param formEntities list of form submissions
     * @return average duration formatted as {@code MM:SS}
     */
    private String computeAverageDuration(List<FormsEntity> formEntities) {
        Long totalDuration = 0L;
        int validCount = 0;

        for(FormsEntity entity : formEntities) {
            Long duration = entity.getDuration();
            if(duration != null && duration > 0) {
                totalDuration += duration;
                validCount++;
            }
        }

        long averageDuration = validCount > 0 ? totalDuration / validCount : 0L;
        long totalSeconds = averageDuration / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Builds chart data JSON array from raw form data, filtered by allowed items.
     * Uses shared context to avoid recomputing prop, columnNames, and allowed items.
     * @param allFormData raw pipe-delimited form data strings
     * @param context shared StatContext containing prop, columnNames, and allowedItemsEntities
     * @return JSONArray containing chart objects for each form item
     */
    private JSONArray getFormStatChartData(List<String> allFormData, StatContext context) {
        Map<String, List<String>> mapData = convertFromListToMap(allFormData, context.allowedItemsEntities, context.prop.getText(NOT_ANSWERED));
        return mapDataToJsonArray(mapData, context.columnNames, context.allowedItemsEntities, context.prop);
    }

    /**
     * Loads all persisted response payload strings for the given form using pagination.
     *
     * @param context shared request statistics context
     * @return list of raw pipe-delimited response data strings
     */
    private List<String> getAllFormData(StatContext context) {
        List<String> allFormData = new ArrayList<>();
        int pageNumber = 0;
        List<String> page;
        do {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            page = formsRepository.getFormAllData(context.formName, context.domainId, context.dateRange[0], context.dateRange[1], pageable);
            allFormData.addAll(page);
            pageNumber++;
        } while (page.size() == PAGE_SIZE);
        return allFormData;
    }

    /**
     * Parses raw form data strings into a map of field values grouped by itemFormId.
     * Handles checkbox groups, selects (value-to-label resolution), wysiwyg (HTML sanitization),
     * and unanswered fields. Results are sorted to match the allowed items order.
     * @param allFormData raw pipe-delimited form data strings
     * @param allowed map of allowed form items keyed by itemFormId
     * @param notAnsweredText localized text for unanswered fields
     * @return ordered map of itemFormId to list of answer values
     */
    private Map<String, List<String>> convertFromListToMap(List<String> allFormData, Map<String, FormItemEntity> allowed, String notAnsweredText) {
        Map<String, List<String>> allData = new LinkedHashMap<>();

        // Build map from form data
        for (String data : allFormData) {
            if (Tools.isEmpty(data)) continue;

            String[] fields = data.split("\\|");
            for (String field : fields) {
                if (Tools.isEmpty(field)) continue;

                int tildeIndex = field.indexOf('~');
                if (tildeIndex == -1) continue;

                String key = field.substring(0, tildeIndex);
                if (Tools.isEmpty(key) || allowed.containsKey(key) == false) continue;

                FormItemEntity allowedItem = allowed.get(key);

                String value = tildeIndex < field.length() - 1 ? field.substring(tildeIndex + 1) : "";

                if(Tools.isEmpty(value)) {
                    if(Tools.isTrue(allowedItem.getShowUnanswered())) value = notAnsweredText;
                    else continue;
                }

                String fieldType = allowedItem.getFieldType().toLowerCase();

                if(fieldType.contains("checkboxgroup")) {
                    for(String v : splitMultiValueTokens(value)) {
                        if(Tools.isEmpty(v)) continue;
                        addMapValue(allData, key, v);
                    }
                } else if(fieldType.contains("select")) {
                    String[] options = splitMultiValueTokens(allowedItem.getValue());
                    boolean found = false;
                    for(String option : options) {
                        if(option.endsWith(":" + value)) {
                            found = true;
                            addMapValue(allData, key, option.substring(0, option.lastIndexOf(":")));
                            break;
                        }
                    }

                    if(found == false) addMapValue(allData, key, value);
                } else if (fieldType.contains("wysiwyg")) {
                    //decode HTML entities from &lt; and then remove HTML tags
                    String decodedValue = DB.unfilterHtml(value);
                    String safeValue = AllowSafeHtmlAttributeConverter.sanitize(decodedValue);

                    addMapValue(allData, key, safeValue);
                } else {
                    addMapValue(allData, key, value);
                }
            }
        }

        // Sort data to match allowed items order
        List<String> allowedKeys = new ArrayList<>(allowed.keySet());
        for (String allowedKey : allowedKeys) allData.putIfAbsent(allowedKey, new ArrayList<>());

        // Use position map for O(1) lookups instead of O(n) indexOf
        Map<String, Integer> keyPosition = new HashMap<>();
        for (int i = 0; i < allowedKeys.size(); i++) keyPosition.put(allowedKeys.get(i), i);

        Map<String, List<String>> sortedData = allData.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                    keyPosition.getOrDefault(e1.getKey(), Integer.MAX_VALUE),
                    keyPosition.getOrDefault(e2.getKey(), Integer.MAX_VALUE)
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return sortedData;
    }

    /**
     * Adds one parsed answer value to the grouped data map.
     *
     * @param data grouped answer values keyed by itemFormId
     * @param key form item identifier
     * @param value parsed answer value
     */
    private void addMapValue(Map<String, List<String>> data, String key, String value) {
        data.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    /**
     * Splits a multi-value answer string.
     * Values are split by {@code |} when present, otherwise by comma.
     *
     * @param value the raw value string
     * @return array of tokens, or an empty array when value is empty
     */
    private String[] splitMultiValueTokens(String value) {
        if(Tools.isEmpty(value)) return new String[0];
        if(value.contains("|")) {
            return Tools.getTokens(value, "|");
        } else {
            return Tools.getTokens(value, ",");
        }
    }

    /**
     * Converts the grouped form data map into a JSON array of chart objects,
     * each containing id, type, title, color scheme and aggregated values.
     * @param mapData map of itemFormId to answer values
     * @param columnNames map of itemFormId to display column names
     * @param allowed map of allowed form items with chart configuration
     * @param prop localization properties
     * @return JSONArray of chart data objects
     */
    private JSONArray mapDataToJsonArray(Map<String, List<String>> mapData, Map<String, String> columnNames, Map<String, FormItemEntity> allowed, Prop prop) {
        JSONArray jsonArray = new JSONArray();

        for (Map.Entry<String, List<String>> entry : mapData.entrySet()) {
            String key = entry.getKey();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", key);
            jsonObject.put("type", allowed.get(key).getChartType());
            jsonObject.put("title", columnNames.getOrDefault(key, key));
            jsonObject.put("chart_colorScheme", allowed.get(key).getColorScheme());

            // values must be combination of value and count
            jsonObject.put("values", prepareDataForChart(entry.getValue(), allowed.get(key).getTopCount(), allowed.get(key).getShowOtherCount(), allowed.get(key).getCompareInsensitive(), prop) );

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * Aggregates a list of values into a sorted JSON array of {name, count} objects.
     * Supports case-insensitive grouping (preserving original display name), top-N limiting,
     * and an optional "Other" bucket for remaining values.
     * @param values list of raw answer values
     * @param maxCountAllowed maximum number of distinct values to show
     * @param allowOtherCount whether to aggregate excess values into an "Other" entry
     * @param compareInsensitive whether to group values case-insensitively
     * @param prop localization properties for the "Other" label
     * @return JSONArray of {name, count} objects sorted by count descending
     */
    private JSONArray prepareDataForChart(List<String> values, int maxCountAllowed, boolean allowOtherCount, boolean compareInsensitive, Prop prop) {
        Map<String, Integer> valueCountMap = new HashMap<>();
        // Preserve original display name for case-insensitive grouping
        Map<String, String> normalizedToOriginal = new HashMap<>();

        if(compareInsensitive) {
            for(String value : values) {
                String normalizedValue = DB.internationalToEnglish(value).toLowerCase();
                valueCountMap.put(normalizedValue, valueCountMap.getOrDefault(normalizedValue, 0) + 1);
                normalizedToOriginal.putIfAbsent(normalizedValue, value);
            }
        } else {
            for(String value : values) valueCountMap.put(value, valueCountMap.getOrDefault(value, 0) + 1);
        }

        return prepareAggregatedDataForChart(valueCountMap, normalizedToOriginal, maxCountAllowed, allowOtherCount, prop);
    }

    /**
     * Converts pre-counted values into chart data.
     *
     * @param valueCountMap map of display value to occurrence count
     * @param maxCountAllowed maximum number of distinct values to show
     * @param allowOtherCount whether to aggregate excess values into an "Other" entry
     * @param prop localization properties for the "Other" label
     * @return JSONArray of {name, count} objects sorted by count descending
     */
    private JSONArray prepareAggregatedDataForChart(Map<String, Integer> valueCountMap, int maxCountAllowed, boolean allowOtherCount, Prop prop) {
        return prepareAggregatedDataForChart(valueCountMap, new HashMap<>(), maxCountAllowed, allowOtherCount, prop);
    }

    /**
     * Converts pre-counted values into chart data, optionally restoring original display labels.
     *
     * @param valueCountMap map of normalized or display value to occurrence count
     * @param normalizedToOriginal map of normalized values to their first original display value
     * @param maxCountAllowed maximum number of distinct values to show
     * @param allowOtherCount whether to aggregate excess values into an "Other" entry
     * @param prop localization properties for the "Other" label
     * @return JSONArray of {name, count} objects sorted by count descending
     */
    private JSONArray prepareAggregatedDataForChart(Map<String, Integer> valueCountMap, Map<String, String> normalizedToOriginal, int maxCountAllowed, boolean allowOtherCount, Prop prop) {
        JSONArray valuesArray = new JSONArray();

        //sort map DESC
        valueCountMap = valueCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int count = 0;
        int otherCount = 0;
        for(Map.Entry<String, Integer> valueCountEntry : valueCountMap.entrySet()) {
            if(count >= maxCountAllowed) {
                if(allowOtherCount) otherCount += valueCountEntry.getValue();
                continue;
            }

            String displayName = normalizedToOriginal.getOrDefault(valueCountEntry.getKey(), valueCountEntry.getKey());
            valuesArray.put( getChartObject(displayName, valueCountEntry.getValue()) );
            count++;
        }

        if(allowOtherCount && otherCount > 0) {
            valuesArray.put( getChartObject(prop.getText("components.stat.other"), otherCount) );
        }

        return valuesArray;
    }

    /**
     * Loads all form submission rows for the given form and current domain.
     * The result contains only fields required for statistics processing.
     *
     * @param context shared request statistics context
     * @return list of form entities used for metrics and bonus charts
     */
    private List<FormsEntity> getFormEntities(StatContext context) {
        List<FormsEntity> formEntities = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT duration, create_date, language FROM forms WHERE form_name = ? AND domain_id = ?");

        List<Object> sqlParams = new ArrayList<>();
        sqlParams.add(context.formName);
        sqlParams.add(context.domainId);

        sql.append(" AND create_date BETWEEN ? AND ?");
        sqlParams.add(new Timestamp(context.dateRange[0].getTime()));
        sqlParams.add(new Timestamp(context.dateRange[1].getTime()));

        new ComplexQuery().setSql(sql.toString()).setParams(sqlParams.toArray()).list(new Mapper<FormsEntity>() {
            @Override
			public FormsEntity map(ResultSet rs) throws SQLException {
                formEntities.add( resultSetToFormEntity(rs) );
                return null;
            }
        });

        return formEntities;
    }

    /**
     * Maps one SQL row to {@link FormsEntity} for statistics evaluation.
     *
     * @param rs the current result set row
     * @return populated form entity
     * @throws SQLException when a column value cannot be read
     */
    private FormsEntity resultSetToFormEntity(ResultSet rs) throws SQLException {
        FormsEntity formEntity = new FormsEntity();
        formEntity.setDuration( rs.getLong("duration") );
        formEntity.setCreateDate( rs.getTimestamp("create_date") );
        formEntity.setLanguage( rs.getString("language") );
        return formEntity;
    }

    /**
     * Returns the total number of times the form has been viewed.
     * @param formName the form name identifier
     * @return view count, or 0 if not available
     */
    private int getFormViewCount(String formName) {
        Integer viewCount = formSettingsRepository.getViewCount(formName, CloudToolsForCore.getDomainId());
        return (viewCount == null || viewCount < 1) ? 0 : viewCount;
    }

    private int getResponseAttempts(String formName) {
        Integer responseAttempts = formSettingsRepository.getResponseAttempts(formName, CloudToolsForCore.getDomainId());
        return (responseAttempts == null || responseAttempts < 1) ? 0 : responseAttempts;
    }

    /**
     * Builds bonus chart data for language and referrer distributions.
     * Uses shared context to reuse localization resources.
     *
     * @param formEntities list of form submissions
     * @param context shared context containing localization utilities
     * @return JSON object with {@code languageData} and {@code referrerData} arrays
     */
    private JSONObject getFormStatChartBonusData(List<FormsEntity> formEntities, StatContext context) {
        List<String> languages = new ArrayList<>();
        for(FormsEntity entity : formEntities) {
            languages.add(valueOrUnknown(entity.getLanguage()));
        }

        JSONObject bonusData = new JSONObject();
        bonusData.put("languageData", prepareDataForChart(languages, PIE_TOP_COUNT, true, false, context.prop) );
        bonusData.put("referrerData", prepareAggregatedDataForChart(getRefererCounts(context.formName, context.dateRange), TABLE_TOP_COUNT, true, context.prop) );

        return bonusData;
    }

    /**
     * Aggregates referrer values directly in the database to avoid loading the 255-character column for every row.
     *
     * @param formName the form name identifier
     * @param dateRange response creation date range
     * @return map of referrer value to response count, with empty values merged into {@link #UNKNOWN_VALUE}
     */
    private Map<String, Integer> getRefererCounts(String formName, Date[] dateRange) {
        Map<String, Integer> refererCounts = new HashMap<>();

        StringBuilder sql = new StringBuilder("SELECT referer, COUNT(*) AS referer_count FROM forms WHERE form_name = ? AND domain_id = ? AND create_date IS NOT NULL");

        List<Object> sqlParams = new ArrayList<>();
        sqlParams.add(formName);
        sqlParams.add(CloudToolsForCore.getDomainId());

        sql.append(" AND create_date BETWEEN ? AND ?");
        sqlParams.add(new Timestamp(dateRange[0].getTime()));
        sqlParams.add(new Timestamp(dateRange[1].getTime()));

        sql.append(" GROUP BY referer");

        new ComplexQuery().setSql(sql.toString()).setParams(sqlParams.toArray()).list(new Mapper<String>() {
            @Override
            public String map(ResultSet rs) throws SQLException {
                String referer = valueOrUnknown(rs.getString("referer"));
                refererCounts.put(referer, refererCounts.getOrDefault(referer, 0) + rs.getInt("referer_count"));
                return null;
            }
        });

        return refererCounts;
    }

    /**
     * Replaces empty values with the shared unknown bucket value.
     *
     * @param value value to normalize
     * @return original value when present, otherwise {@link #UNKNOWN_VALUE}
     */
    private String valueOrUnknown(String value) {
        return Tools.isNotEmpty(value) ? value : UNKNOWN_VALUE;
    }

    /**
     * Builds error chart data for field validation failures.
     * Uses shared context to reuse localization and item label mappings.
     *
     * @param context shared context containing localization and column name mapping
     * @return JSON object with {@code pieErrorData} and {@code tableErrorData} arrays
     */
    private JSONObject getFormStatChartErrorData(StatContext context) {
        Map<String, Integer> errorCountsByField = new HashMap<>();
        int allFieldsErrors = 0;
        for(FormItemEntity entity : context.allowedItemsEntities.values()) {
            int count = (entity.getErrorCount() != null) ?  entity.getErrorCount() : 0;
            allFieldsErrors += count;
            if(count > 0) {
                String label = context.columnNames.get(entity.getItemFormId());
                errorCountsByField.put(label, errorCountsByField.getOrDefault(label, 0) + count);
            }
        }

        JSONObject errorData = new JSONObject();
        errorData.put("pieErrorData", prepareAggregatedDataForChart(errorCountsByField, PIE_TOP_COUNT, true, context.prop) );
        errorData.put("tableErrorData", prepareAggregatedDataForChart(errorCountsByField, TABLE_TOP_COUNT, true, context.prop) );

        int getErrorsCount = 0;
        int saveErrorsCount = 0;
        int emailNotSendCount = 0;
        int probablySpamBotCount = 0;
        int badFileCount = 0;
        int csrfErrorsCount = 0;

        List<AuditLogEntity> logs = new ArrayList<>();
        if(context.formId > 0) {
            Timestamp dateFrom = new Timestamp(context.dateRange[0].getTime());
            Timestamp dateTo = new Timestamp(context.dateRange[1].getTime());
            logs = auditRepository.findAllByLogTypeAndSubId1AndCreateDateBetween(Adminlog.TYPE_MULTISTEP_FORM_USERS, context.formId, dateFrom, dateTo);
            for(AuditLogEntity log : logs) {
                if(log.getSubId2() == 1) getErrorsCount++;
                else if(log.getSubId2() == 2) {

                    if (log.getDescription().contains("formfail: emailNotSend")) {
                        emailNotSendCount++;
                    } else if (log.getDescription().contains("formfail: probablySpamBotCsrf")) {
                        csrfErrorsCount++;
                    } else if (log.getDescription().contains("formfail: probablySpamBot")) {
                        probablySpamBotCount++;
                    } else if (log.getDescription().contains("formfail: bad_file")) {
                        badFileCount++;
                    }
                    saveErrorsCount++;
                }
            }
        }

        errorData.put("systemErrorsCount", getErrorsCount + saveErrorsCount);
        errorData.put("allErrorsCount", allFieldsErrors + getErrorsCount + saveErrorsCount);

        JSONArray valuesArray = new JSONArray();
        valuesArray.put( getChartObject(context.prop.getText("components.multistep_form.system_errors.get_step"), getErrorsCount) );
        valuesArray.put( getChartObject(context.prop.getText("components.multistep_form.system_errors.save_step"), saveErrorsCount - emailNotSendCount - probablySpamBotCount - badFileCount - csrfErrorsCount) );
        valuesArray.put( getChartObject(context.prop.getText("components.multistep_form.system_errors.emailNotSend"), emailNotSendCount) );
        valuesArray.put( getChartObject(context.prop.getText("components.multistep_form.system_errors.probablySpamBot"), probablySpamBotCount) );
        valuesArray.put( getChartObject(context.prop.getText("components.multistep_form.system_errors.badFile"), badFileCount) );
        valuesArray.put( getChartObject(context.prop.getText("components.multistep_form.system_errors.csrfErrors"), csrfErrorsCount) );
        errorData.put("pieSystemErrorData",  valuesArray);
        errorData.put("timelineErrorData", getTimelineErrorData(context, logs, context.dateRange[0], context.dateRange[1]));

        return errorData;
    }

    /**
     * Creates one chart value object.
     *
     * @param name chart value label
     * @param count chart value count
     * @return JSON object with {@code name} and {@code count}
     */
    private JSONObject getChartObject(String name, Integer count) {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("count", count);
        return obj;
    }

    /**
     * Builds timeline data for system errors grouped by day and operation type.
     *
     * @param context shared context containing form and localization data
     * @param logs audit log entries for the selected form and date range
     * @return JSON object keyed by localized operation names with daily error counts
     */
    private JSONObject getTimelineErrorData(StatContext context, List<AuditLogEntity> logs, Date dateFrom, Date dateTo) {
        JSONObject timelineErrorData = new JSONObject();
        if(context.formId < 1) return timelineErrorData;

        TreeMap<String, TreeMap<Long, Integer>> dayErrorCounts = new TreeMap<>();
        dayErrorCounts.put("errorStepGet", new TreeMap<>());
        dayErrorCounts.put("errorStepSave", new TreeMap<>());
        dayErrorCounts.put("successSave", new TreeMap<>());

        for(AuditLogEntity log : logs) {
            if (log.getSubId2() == 1) {
                Date createDate = DateTools.setTimePart(log.getCreateDate(), 12, 0, 0, 0);
                dayErrorCounts.get("errorStepGet").put(
                    createDate.getTime(),
                    dayErrorCounts.get("errorStepGet").getOrDefault(createDate.getTime(), 0) + 1);
            } else if (log.getSubId2() == 2) {
                Date createDate = DateTools.setTimePart(log.getCreateDate(), 12, 0, 0, 0);
                dayErrorCounts.get("errorStepSave").put(
                    createDate.getTime(),
                    dayErrorCounts.get("errorStepSave").getOrDefault(createDate.getTime(), 0) + 1);
            } else  {
                Date createDate = DateTools.setTimePart(log.getCreateDate(), 12, 0, 0, 0);
                dayErrorCounts.get("successSave").put(
                    createDate.getTime(),
                    dayErrorCounts.get("successSave").getOrDefault(createDate.getTime(), 0) + 1);
            }
        }

        Date dateFromNorm = DateTools.setTimePart(dateFrom, 12, 0, 0, 0);
        Date dateToNorm = DateTools.setTimePart(dateTo, 12, 0, 0, 0);
        for (Map.Entry<String, TreeMap<Long, Integer>> entry : dayErrorCounts.entrySet()) {
            TreeMap<Long, Integer> value = entry.getValue();
            JSONArray valuesArray = new JSONArray();
            long cursor = dateFromNorm.getTime();
            while (cursor <= dateToNorm.getTime()) {
                JSONObject obj = new JSONObject();
                obj.put("dayDate", cursor);
                obj.put("count", value.getOrDefault(cursor, 0));
                valuesArray.put(obj);
                cursor += 86400000L;
            }
            String key = entry.getKey();
            if ("errorStepGet".equals(key)) {
                key = context.prop.getText("components.multistep_form.system_errors.error_get_step");
            } else if ("errorStepSave".equals(key)) {
                key = context.prop.getText("components.multistep_form.system_errors.error_save_step");
            } else if ("successSave".equals(key)) {
                key = context.prop.getText("components.multistep_form.system_errors.success_save_step");
            }
            timelineErrorData.put(key, valuesArray);
        }

        return timelineErrorData;
    }
}
