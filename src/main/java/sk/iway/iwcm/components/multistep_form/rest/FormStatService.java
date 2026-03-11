package sk.iway.iwcm.components.multistep_form.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.users.UserDetails;

@Service
public class FormStatService {

    private static final int PAGE_SIZE = 1000;
    private static final String NOT_ANSWERED = "Nezodpovedané";

    private final FormsRepository formsRepository;
    private final FormsServiceImpl formsService;

    @Autowired
    public FormStatService(FormsRepository formsRepository, FormsServiceImpl formsService) {
        this.formsRepository = formsRepository;
        this.formsService = formsService;
    }

    public final JSONObject getFormStatData(String formName, HttpServletRequest request) {
        if(Tools.isEmpty(formName)) return null;

        List<String> allFormData = kokos(formName);

        JSONObject allData = new JSONObject();
        allData.put("chartData", getFormStatChartData(formName, allFormData, getAllowedItems(formName, null), request));
        allData.put("totalResponses", allFormData.size());
        allData.put("averageDuration", computeAverageDuration(formName));
        allData.put("durationDays", computeDurationDays(formName));
        allData.put("lastResponseDateTime", getLastResponseDateTime(formName));

        return allData;
    }

    public final JSONArray getFormStatChartData(String formName, String itemFormId, HttpServletRequest request) {
        if(Tools.isEmpty(formName) || Tools.isEmpty(itemFormId)) return null;
        List<String> allFormData = kokos(formName);
        return getFormStatChartData(formName, allFormData, getAllowedItems(formName, itemFormId), request);
    }

    /* ------------------------- */

    private String getLastResponseDateTime(String formName) {
        FormsEntity lastOne = formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, CloudToolsForCore.getDomainId());
        if(lastOne == null) return "";
        return Tools.formatDateTime(lastOne.getCreateDate());
    }

    private String computeDurationDays(String formName) {
        Long formCreationEpoch = new SimpleQuery().forLong("SELECT duration FROM forms WHERE form_name = ? AND domain_id = ? AND create_date IS NULL", formName, CloudToolsForCore.getDomainId());
        if(formCreationEpoch == null || formCreationEpoch <= 0) return "0";
        long currentEpoch = Tools.getNow() / 1000;
        long durationDays = (currentEpoch - formCreationEpoch) / (60 * 60 * 24);
        return String.valueOf(durationDays);
    }

    private String computeAverageDuration(String formName) {
        Long totalDuration = 0L;
        int validCount = 0;

        for (Number duration : new SimpleQuery().forListNumber("SELECT duration FROM forms WHERE form_name = ? AND domain_id = ? AND create_date IS NOT NULL", formName, CloudToolsForCore.getDomainId())) {
            if(duration != null && duration.longValue() > 0) {
                totalDuration += duration.longValue();
                validCount++;
            }
        }

        Long averageDuration = validCount > 0 ? totalDuration / validCount : 0L;

        long totalSeconds = averageDuration / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    private JSONArray getFormStatChartData(String formName, List<String> allFormData, Map<String, FormItemEntity> allowed, HttpServletRequest request) {
        Map<String, List<String>> mapData = convertFromListToMap(allFormData, allowed);
        Map<String, String> columnNames = getColumnNames(formName, new UserDetails(request), Prop.getInstance(request));

        return mapDataToJsonArray(mapData, columnNames, allowed);
    }

    private List<String> kokos(String formName) {
        List<String> allFormData = new ArrayList<>();
        int domainId = CloudToolsForCore.getDomainId();
        int pageNumber = 0;
        List<String> page;
        do {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            page = formsRepository.getFormAllData(formName, domainId, pageable);
            allFormData.addAll(page);
            pageNumber++;
        } while (page.size() == PAGE_SIZE);
        return allFormData;
    }

    private Map<String, List<String>> convertFromListToMap(List<String> allFormData, Map<String, FormItemEntity> allowed) {
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

                String value = tildeIndex < field.length() - 1 ? field.substring(tildeIndex + 1) : "";

                if(Tools.isEmpty(value)) {
                    if(Tools.isTrue(allowed.get(key).getShowUnanswered())) value = NOT_ANSWERED;
                    else continue;
                }

                if("checkboxgroup".equals(allowed.get(key).getFieldType())) {
                    for(String v : parseKKs(value)) {
                        if(Tools.isEmpty(v)) continue;
                        allData.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
                    }
                }
                else if("select".equals(allowed.get(key).getFieldType())) {
                    String options[] = parseKKs(allowed.get(key).getValue());
                    boolean found = false;
                    for(String option : options) {
                        if(option.endsWith(":" + value)) {
                            found = true;
                            allData.computeIfAbsent(key, k -> new ArrayList<>()).add(option.substring(0, option.lastIndexOf(":")));
                            break;
                        }
                    }

                    if(found == false) allData.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
                else {
                    allData.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
            }
        }

        // Sort data to match allowed items order
        List<String> allowedKeys = new ArrayList<>(allowed.keySet());
        for (String allowedKey : allowedKeys) allData.putIfAbsent(allowedKey, new ArrayList<>());

        Map<String, List<String>> sortedData = allData.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                    allowedKeys.indexOf(e1.getKey()),
                    allowedKeys.indexOf(e2.getKey())
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return sortedData;
    }

    private String[] parseKKs(String value) {
        if(Tools.isEmpty(value)) return new String[0];
        if(value.indexOf("|") != -1) {
            return Tools.getTokens(value, "|");
        } else {
            return Tools.getTokens(value, ",");
        }
    }

    private JSONArray mapDataToJsonArray(Map<String, List<String>> mapData, Map<String, String> columnNames, Map<String, FormItemEntity> allowed) {
        JSONArray jsonArray = new JSONArray();

        for (Map.Entry<String, List<String>> entry : mapData.entrySet()) {
            String key = entry.getKey();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", key);
            jsonObject.put("type", allowed.get(key).getChartType());
            jsonObject.put("title", columnNames.getOrDefault(key, key));
            jsonObject.put("chart_colorScheme", allowed.get(key).getColorScheme());

            // values must be combination of value and count
            jsonObject.put("values", prepareDataForChart(entry.getValue(), allowed.get(key).getTopCount(), allowed.get(key).getShowOtherCount(), allowed.get(key).getCompareInsensitive()) );

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    private JSONArray prepareDataForChart(List<String> values, int maxCountAllowed, boolean allowOtherCount, boolean compareInsensitive) {
        JSONArray valuesArray = new JSONArray();

        HashMap<String, Integer> valueCountMap = new HashMap<>();

        //
        if(compareInsensitive) {
            for(String value : values) {
                String normalizedValue = Tools.normalize(value);
                valueCountMap.put(normalizedValue, valueCountMap.getOrDefault(normalizedValue, 0) + 1);
            }
        } else {
            for(String value : values) valueCountMap.put(value, valueCountMap.getOrDefault(value, 0) + 1);
        }

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

            JSONObject valueCountObject = new JSONObject();
            valueCountObject.put("name", valueCountEntry.getKey());
            valueCountObject.put("count", valueCountEntry.getValue());
            valuesArray.put(valueCountObject);
            count++;
        }

        if(allowOtherCount && otherCount > 0) {
            JSONObject otherCountObject = new JSONObject();
            otherCountObject.put("name", "OTHER");
            otherCountObject.put("count", otherCount);
            valuesArray.put(otherCountObject);
        }

        return valuesArray;
    }

    private Map<String, String> getColumnNames(String formName, UserDetails currentUser, Prop prop) {
        HashMap<String, String> columnNames = new LinkedHashMap<>();
        for(LabelValue column : formsService.getColumnNames(formName, currentUser, prop).getColumns()) {
            columnNames.put(column.getValue(), column.getLabel());
        }
        return columnNames;
    }

    private Map<String, FormItemEntity> getAllowedItems(String formName, String itemFormId) {
        String sql = "SELECT item_form_id, value, field_type, chart_type, top_count, show_other_count, show_unanswered, compare_insensitive, color_scheme FROM form_items f, form_steps s WHERE f.form_name = ? AND f.domain_id = ? AND f.show_stat IS TRUE AND f.step_id=s.id";
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
                FormItemEntity stepItem = resultSetToEntity(rs);

                // Radio's act like separe items but if they have same itemFormId AND are one after another they are GROUP (so as only one note in DB)
                if("radio".equals(stepItem.getFieldType()) &&  previous.get(0).equals(stepItem.getItemFormId())) return null;
                previous.set(0, stepItem.getItemFormId());

                values.put(stepItem.getItemFormId(), stepItem);

                return null;
			}
		});

        return values;
    }

    private FormItemEntity resultSetToEntity(ResultSet rs) throws SQLException{
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
        return fe;
    }
}
