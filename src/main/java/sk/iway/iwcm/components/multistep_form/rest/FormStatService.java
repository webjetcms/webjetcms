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

        return allData;
    }

    public final JSONArray getFormStatChartData(String formName, String itemFormId, HttpServletRequest request) {
        if(Tools.isEmpty(formName) || Tools.isEmpty(itemFormId)) return null;
        List<String> allFormData = kokos(formName);
        return getFormStatChartData(formName, allFormData, getAllowedItems(formName, itemFormId), request);
    }

    /* ------------------------- */


    private String computeAverageDuration(String formName) {
        Long totalDuration = 0L;
        int validCount = 0;

        for (Number duration : new SimpleQuery().forListNumber("SELECT duration FROM forms WHERE form_name = ? AND domain_id = ?", formName, CloudToolsForCore.getDomainId())) {
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
                allData.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }

        // Sort data to match allowed items order
        List<String> allowedKeys = new ArrayList<>(allowed.keySet());
        allData = allData.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                    allowedKeys.indexOf(e1.getKey()),
                    allowedKeys.indexOf(e2.getKey())
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return allData;
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
            jsonObject.put("values", prepareDataForChart(entry.getValue(), allowed.get(key).getTopCount(), allowed.get(key).getShowOtherCount()) );

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    private JSONArray prepareDataForChart(List<String> values, int maxCountAllowed, boolean allowOtherCount) {
        JSONArray valuesArray = new JSONArray();

        HashMap<String, Integer> valueCountMap = new HashMap<>();
        for(String value : values) valueCountMap.put(value, valueCountMap.getOrDefault(value, 0) + 1);
        //sort map DESC
        valueCountMap = valueCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int count = 0;
        int othercount = 0;
        for(Map.Entry<String, Integer> valueCountEntry : valueCountMap.entrySet()) {
            if(count >= maxCountAllowed) {
                if(allowOtherCount) othercount += valueCountEntry.getValue();
                continue;
            }

            JSONObject valueCountObject = new JSONObject();
            valueCountObject.put("name", Tools.isEmpty(valueCountEntry.getKey()) ? "EMPTY" : valueCountEntry.getKey());
            valueCountObject.put("count", valueCountEntry.getValue());
            valuesArray.put(valueCountObject);
            count++;
        }

        if(allowOtherCount && othercount > 0) {
            JSONObject otherCountObject = new JSONObject();
            otherCountObject.put("name", "OTHER");
            otherCountObject.put("count", othercount);
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
        String sql = "SELECT item_form_id, chart_type, top_count, show_other_count, compare_insensitive, color_scheme FROM form_items f, form_steps s WHERE f.form_name = ? AND f.domain_id = ? AND f.show_stat IS TRUE AND f.step_id=s.id";
        List<Object> sqlParams = new ArrayList<>();
        sqlParams.add(formName);
        sqlParams.add(CloudToolsForCore.getDomainId());

        if(Tools.isNotEmpty(itemFormId)) {
            sql += " AND item_form_id = ?";
            sqlParams.add(itemFormId);
        }

        sql += " ORDER BY s.sort_priority ASC, f.sort_priority ASC";

        Map<String, FormItemEntity> values = new LinkedHashMap<>();
        new ComplexQuery().setSql(sql).setParams(sqlParams.toArray()).list(new Mapper<FormItemEntity>() {
			@Override
			public FormItemEntity map(ResultSet rs) throws SQLException {
                FormItemEntity stepItem = resultSetToEntity(rs);

                // Radio's act like separe items but if they have same itemFormId AND are one after another they are GROUP (so as only one note in DB)
                // String previous = values.size() > 0 ? values.get( values.size() - 1).getItemFormId() : "";
                // if("radio".equals(stepItem.getFieldType()) &&  previous.equals(stepItem.getItemFormId())) return null;

                values.put(stepItem.getItemFormId(), stepItem);

                return null;
			}
		});

        return values;
    }

    private FormItemEntity resultSetToEntity(ResultSet rs) throws SQLException{
        FormItemEntity fe = new FormItemEntity();
        fe.setItemFormId( rs.getString("item_form_id") );
        fe.setChartType( rs.getString("chart_type") );
        fe.setTopCount( rs.getInt("top_count") );
        fe.setShowOtherCount( rs.getBoolean("show_other_count") );
        fe.setCompareInsensitive( rs.getBoolean("compare_insensitive") );
        fe.setColorScheme( rs.getString("color_scheme") );
        return fe;
    }
}
