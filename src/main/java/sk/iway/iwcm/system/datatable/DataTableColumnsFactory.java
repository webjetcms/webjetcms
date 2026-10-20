package sk.iway.iwcm.system.datatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.components.fields_settings.rest.FieldsSettingsService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.events.DatatableColumnsEvent;
import sk.iway.iwcm.system.datatable.json.DataTableColumn;
import sk.iway.iwcm.system.datatable.json.DataTableTab;
import sk.iway.iwcm.tags.support.ResponseUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataTableColumnsFactory {

    Class<?> dto;
    /**
     * This list prevents circular references.
     * eg. when you have FileArchivatorBean.FileArchivatorEditorFields with List<FileArchivatorBean> listOfVersions
     * this is needed to prevent infinite recursion
     */
    List<String> circularReferences;

    public DataTableColumnsFactory(String clazz) {
        try {
            dto = Class.forName(clazz);
            circularReferences = new ArrayList<>();
        } catch (ClassNotFoundException e) {
            Logger.error(DataTableColumnsFactory.class, e);
        }
    }

    public List<DataTableColumn> getColumns(String fieldPrefix) {
        return getColumns(fieldPrefix, Map.of());
    }

    private List<DataTableColumn> getColumns(String fieldPrefix, Map<String, FieldsSettingsEntity> parentSettings) {
        List<DataTableColumn> columns = new ArrayList<>();

        if (dto != null) {
            Field[] declaredFields = AuditEntityListener.getDeclaredFieldsTwoLevels(dto);
            Map<String, FieldsSettingsEntity> settings = getFieldsSettings();
            settings.putAll(parentSettings);

            //Get from WebjetAppStore annotation commonSettings attribute (true - we want commmon settings tab and fields, false - we don't want common settings tab nor fields)
            boolean includeCommonSettings = true;
            if(dto.isAnnotationPresent(sk.iway.iwcm.system.annotations.WebjetAppStore.class)) {
                includeCommonSettings = dto.getAnnotation(sk.iway.iwcm.system.annotations.WebjetAppStore.class).commonSettings();
            }

            for (Field declaredField : declaredFields) {

                if (circularReferences.isEmpty() == false) {
                    boolean hasCircularReference = false;
                    String fieldClassName = declaredField.getGenericType().getTypeName();
                    for (String name : circularReferences) {
                        //entity with same class or List<EntityClass> or Array<EntityClass>
                        if (name.equals(fieldClassName) || fieldClassName.contains("<"+name+">") || fieldClassName.contains("["+name+"]")) {
                            //Logger.debug(this.getClass(), "Has circular reference, name="+name);
                            hasCircularReference = true;
                            break;
                        }
                    }
                    if (hasCircularReference) {
                        //Logger.debug(this.getClass(), "Has circular reference, name="+fieldClassName);
                        continue;
                    }
                }

                if (declaredField.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested.class)  && circularReferences.contains(declaredField.getGenericType().getTypeName()) == false) {

                    //circularReferences.add(declaredField.getGenericType().getTypeName());

                    //rekurzia
                    DataTableColumnsFactory dtcf = new DataTableColumnsFactory(declaredField.getGenericType().getTypeName());
                    dtcf.setCircularReferences(circularReferences);

                    //ziskaj prefix premennej (aby vzniklo editorFields.allowChangeUrl), defaultne podla mena premennej, alebo z anotacie
                    DataTableColumnNested annotation = declaredField.getAnnotation(DataTableColumnNested.class);
                    String nestedFieldPrefix;
                    if ("auto".equals(annotation.prefix())) nestedFieldPrefix = declaredField.getName()+".";
                    else nestedFieldPrefix = annotation.prefix();

                    Map<String, FieldsSettingsEntity> nestedSettings = new HashMap<>();
                    for (Map.Entry<String, FieldsSettingsEntity> entry : settings.entrySet()) {
                        if (entry.getKey().startsWith(nestedFieldPrefix)) {
                            nestedSettings.put(entry.getKey().substring(nestedFieldPrefix.length()), entry.getValue());
                        }
                    }
                    if (Tools.isNotEmpty(fieldPrefix)) nestedFieldPrefix = fieldPrefix + nestedFieldPrefix;

                    String sortPrefix = annotation.sortPrefix();

                    List<DataTableColumn> columnsNested = dtcf.getColumns(nestedFieldPrefix, nestedSettings);

                    //If orderable is not set (is null) set to FALSE -> by default nestedColumns do not support ordering
                    columnsNested.forEach(column -> {
                        if (column.getOrderable() == null) {
                            column.setOrderable(false);
                        }
                    });

                    if (Tools.isNotEmpty(sortPrefix)) {
                        columnsNested.forEach(column -> {
                            if (Tools.isNotEmpty(column.getSortAfter()) && !column.getSortAfter().startsWith(sortPrefix) && !"FIRST".equals(column.getSortAfter())) {
                                column.setSortAfter(sortPrefix + column.getSortAfter());
                            }
                        });
                    }

                    columns.addAll(columnsNested);
                }

                if (!declaredField.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
                    continue;
                }

                //If we dont want common settings, skip all fields with tab commonSettings
                if(includeCommonSettings==false) {
                    if("commonSettings".equals( declaredField.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class).tab()) ) continue;
                }

                DataTableColumn column = new DataTableColumn(dto, declaredField, fieldPrefix, getColumnAnnotation(declaredField, settings));
                FieldsSettingsEntity setting = settings.get(declaredField.getName());
                if (setting != null && Tools.isNotEmpty(setting.getTab()) && column.getEditor() != null) {
                    // Apply the saved tab last because editor metadata can restore the tab from the original annotation.
                    column.getEditor().setTab(setting.getTab());
                }
                columns.add(column);
            }
        }

        return columns;
    }

    private Map<String, FieldsSettingsEntity> getFieldsSettings() {
        Map<String, FieldsSettingsEntity> settings = new HashMap<>();
        FieldsSettingsService service = Tools.getSpringBean("fieldsSettingsService", FieldsSettingsService.class);
        if (service != null) {
            for (FieldsSettingsEntity setting : service.getFieldsSettings(dto.getName(), CloudToolsForCore.getDomainId())) {
                settings.put(setting.getFieldName(), setting);
            }
        }
        return settings;
    }

    private sk.iway.iwcm.system.datatable.annotations.DataTableColumn getColumnAnnotation(Field field, Map<String, FieldsSettingsEntity> settings) {
        sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
        FieldsSettingsEntity setting = settings.get(field.getName());
        if (setting == null) return annotation;

        Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(field, annotation);
        if (Tools.isNotEmpty(setting.getTab())) attributes.put("tab", setting.getTab());
        if (Tools.isNotEmpty(setting.getTitle())) attributes.put("title", setting.getTitle());
        if (Tools.isNotEmpty(setting.getSortAfter())) attributes.put("sortAfter", setting.getSortAfter());
        if (setting.getVisible() != null) attributes.put("visible", new boolean[] { setting.getVisible() });
        if (setting.getHidden() != null) attributes.put("hidden", new boolean[] { setting.getHidden() });
        if (setting.getHiddenEditor() != null) attributes.put("hiddenEditor", new boolean[] { setting.getHiddenEditor() });
        if (setting.getFilter() != null) attributes.put("filter", new boolean[] { setting.getFilter() });
        if (setting.getOrderable() != null) attributes.put("orderable", new boolean[] { setting.getOrderable() });
        if (setting.getAi() != null) attributes.put("ai", new boolean[] { setting.getAi() });
        if (setting.getDisabled() != null) attributes.put("disabled", new boolean[] { setting.getDisabled() });
        return AnnotationUtils.synthesizeAnnotation(attributes, sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class, field);
    }

    public String getColumnsJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<DataTableColumn> columns = getColumns(null);
        List<DataTableColumn> columnsSorted = sortColumns(columns);

        if (dto != null) {
            DatatableColumnsEvent event = new DatatableColumnsEvent(columnsSorted, dto);
            event.publishEvent();
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(columnsSorted);
    }

    public static List<DataTableColumn> sortColumns(List<DataTableColumn> columns) {
        List<DataTableColumn> columnsSorted = new ArrayList<>();
        List<DataTableColumn> columnsToSort = new ArrayList<>();

        //najskor nakopirujeme columny kde nie je nastaveny sort atribut, tie predpokladame, ze su v korektnom poradi
        for (DataTableColumn c : columns) {
            if ("FIRST".equals(c.getSortAfter())) {
                columnsSorted.add(0, c);
            }
            else if (Tools.isEmpty(c.getSortAfter())) {
                columnsSorted.add(c);
            } else {
                columnsToSort.add(c);
            }
        }

        int failsafe = 0;
        while (columnsToSort.size()>0 && failsafe++ < 1000) {
            DataTableColumn c = columnsToSort.get(0);

            //prehladaj sorted a pridaj ho
            int i;
            for (i=0; i<columnsSorted.size(); i++) {
                DataTableColumn cs = columnsSorted.get(i);
                if (cs.getData()!= null && cs.getData().equals(c.getSortAfter())) {
                    if (i == columnsSorted.size()-1) columnsSorted.add(c);
                    else columnsSorted.add(i+1, c);
                    c = null;
                    break;
                }
            }

            //bolo pridane do sorted, removneme ho
            columnsToSort.remove(0);
            //inak ho pridame na koniec, asi je zavisle na nejakom poli v toSort pred nim
            if (c != null) {
                if (i>500) Logger.error(DataTableColumnsFactory.class, "sortColumns("+failsafe+") POZOR SKONTROLUJTE sortAfter v COLUMNS definicii, prilis vysoke i sortovania - nenaslo sa, c="+c.getName());
                columnsToSort.add(c);
            }
        }

        if (columnsToSort.size()>0) {
            //columnsSorted.forEach(column -> {   System.out.println("sorted: data=" + column.getData() + ", sortAfter=" + column.getSortAfter());   });
            //columnsToSort.forEach(column -> {   System.out.println("tosort: data=" + column.getData() + ", sortAfter=" + column.getSortAfter());   });

            //nieco je zle zosortovane, pre istotu pridame na koniec
            columnsSorted.addAll(columnsToSort);
            Logger.error(DataTableColumnsFactory.class, "sortColumns POZOR SKONTROLUJTE sortAfter v COLUMNS definicii, zostali stlpce na sort, pocet="+columnsToSort.size());
        }

        return columnsSorted;
    }

    public static String translate(String str) {

        String result = str;

        if ("[[#{}]]".equals(str)) return "";

        Prop prop = null;

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            prop = Prop.getInstance(request);
        } else {
            //use RequestBean lng instead of request
            prop = Prop.getInstance();
        }
        if (prop != null) {
            int failsafe = 0;
            int startIndex = result.indexOf("[[#{");
            boolean hasDot = result.contains(".");
            if (hasDot) {


                if (startIndex == -1) {
                    result = escapeSpecialChars(prop.getText(result, false));
                } else {
                    while (startIndex!=-1 && failsafe++<20) {
                        hasDot = result.contains(".");
                        if (startIndex<0 && hasDot == false) return result;

                        int endIndex = result.indexOf("}]]", startIndex);
                        if (endIndex>startIndex) {
                            String key = result.substring(startIndex+4, endIndex);
                            String translated = escapeSpecialChars(prop.getText(key, false));
                            result = Tools.replace(result, result.substring(startIndex, endIndex+3), translated);
                        }
                        startIndex = result.indexOf("[[#{");
                    }
                }
            }
        }
        return result;
    }

    private static String escapeSpecialChars(String translated) {
        return ResponseUtils.filter(translated);
    }

    /**
     * Metoda pre ziskanie vsetkych moznych tabov z anotacii DataTableColumn pre danu triedu
     * @return List<DataTableTab>
     */
    public List<DataTableTab> getTabs() {
        List<DataTableTab> result = new ArrayList<>();

        if (dto == null) {
            return result;
        }

        //Get from WebjetAppStore annotation commonSettings attribute (true - we want commmon settings tab and fields, false - we don't want common settings tab nor fields)
        boolean includeCommonSettings = false;
        if(dto.isAnnotationPresent(sk.iway.iwcm.system.annotations.WebjetAppStore.class)) {
            includeCommonSettings = dto.getAnnotation(sk.iway.iwcm.system.annotations.WebjetAppStore.class).commonSettings();
        }

        if (dto.isAnnotationPresent(DataTableTabs.class)) {
            DataTableTabs annotation = dto.getAnnotation(DataTableTabs.class);
            for (sk.iway.iwcm.system.datatable.annotations.DataTableTab tab : annotation.tabs()) {
                result.add(new DataTableTab(tab));
            }
            if (includeCommonSettings) {
                result.add(new DataTableTab("commonSettings", "commonSettings", false));
            }
        } else {
            Field[] declaredFields = AuditEntityListener.getDeclaredFieldsTwoLevels(dto);
            Map<String, FieldsSettingsEntity> settings = getFieldsSettings();

            for (Field declaredField : declaredFields) {
                if (!declaredField.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
                    continue;
                }

                sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = declaredField.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);

                //If we dont want common settings, skip all fields with tab commonSettings
                if(includeCommonSettings==false && "commonSettings".equals(annotation.tab())) continue;

                annotation = getColumnAnnotation(declaredField, settings);
                String tab = annotation.tab();

                //If tab is not empty and tab is not already in result, add it
                if (Tools.isNotEmpty(tab) && result.stream().noneMatch(r -> r.getId().equals(tab))) {
                    result.add(new DataTableTab(annotation, result.isEmpty()));
                }
            }
        }

        return result;
    }

    /**
     * Returns JSON object from DataTableTabs annotation
     * @return
     * @throws JsonProcessingException
     */
    public String getTabsJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<DataTableTab> tabs = getTabs();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tabs);
    }

    /**
     * Returns translated app title from nameKey attribute of WebjetAppStore annotation
     * @return
     */
    public String getTitle() {
        if (dto == null) {
            return null;
        }
        if(dto.isAnnotationPresent(sk.iway.iwcm.system.annotations.WebjetAppStore.class)) {
            String key = dto.getAnnotation(sk.iway.iwcm.system.annotations.WebjetAppStore.class).nameKey();
            return translate(key);
        }
        return null;
    }

    public void setCircularReferences(List<String> circularReferences) {
        this.circularReferences = circularReferences;
    }

    public void addCircularReference(String circularReference) {
        this.circularReferences.add(circularReference);
    }

    public List<String> getCircularReferences() {
        return circularReferences;
    }
}
