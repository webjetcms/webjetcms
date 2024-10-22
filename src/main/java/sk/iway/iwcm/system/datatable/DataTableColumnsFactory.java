package sk.iway.iwcm.system.datatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.struts.util.ResponseUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.json.DataTableColumn;
import sk.iway.iwcm.system.datatable.json.DataTableTab;

import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DataTableColumnsFactory {
    Class<?> dto;
    public DataTableColumnsFactory(String clazz) {
        try {
            dto = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            Logger.error(DataTableColumnsFactory.class, e);
        }
    }

    public List<DataTableColumn> getColumns(String fieldPrefix) {
        List<DataTableColumn> columns = new ArrayList<>();

        if (dto != null) {
            Field[] declaredFields = AuditEntityListener.getDeclaredFieldsTwoLevels(dto);

            //Get from WebjetAppStore annotation commonSettings attribute (true - we want commmon settings tab and fields, false - we don't want common settings tab nor fields)
            boolean includeCommonSettings = true;
            if(dto.isAnnotationPresent(sk.iway.iwcm.system.annotations.WebjetAppStore.class)) {
                includeCommonSettings = dto.getAnnotation(sk.iway.iwcm.system.annotations.WebjetAppStore.class).commonSettings();
            }

            for (Field declaredField : declaredFields) {

                if (declaredField.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested.class)) {
                    //rekurzia
                    DataTableColumnsFactory dtcf = new DataTableColumnsFactory(declaredField.getGenericType().getTypeName());

                    //ziskaj prefix premennej (aby vzniklo editorFields.allowChangeUrl), defaultne podla mena premennej, alebo z anotacie
                    DataTableColumnNested annotation = declaredField.getAnnotation(DataTableColumnNested.class);
                    String nestedFieldPrefix;
                    if ("auto".equals(annotation.prefix())) nestedFieldPrefix = declaredField.getName()+".";
                    else nestedFieldPrefix = annotation.prefix();
                    if (Tools.isNotEmpty(fieldPrefix)) nestedFieldPrefix = fieldPrefix + nestedFieldPrefix;

                    List<DataTableColumn> columnsNested = dtcf.getColumns(nestedFieldPrefix);

                    columns.addAll(columnsNested);
                }

                if (!declaredField.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
                    continue;
                }

                //If we dont want common settings, skip all fields with tab commonSettings
                if(includeCommonSettings==false) {
                    if("commonSettings".equals( declaredField.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class).tab()) ) continue;
                }

                columns.add(new DataTableColumn(dto, declaredField, fieldPrefix));
            }
        }

        return columns;
    }

    public String getColumnsJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<DataTableColumn> columns = getColumns(null);
        List<DataTableColumn> columnsSorted = sortColumns(columns);
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
                if (i>500) Logger.debug(DataTableColumnsFactory.class, "sortColumns("+failsafe+") POZOR SKONTROLUJTE sortAfter v COLUMNS definicii, prilis vysoke i sortovania - nenaslo sa, c="+c.getName());
                columnsToSort.add(c);
            }
        }

        if (columnsToSort.size()>0) {
            //nieco je zle zosortovane, pre istotu pridame na koniec
            columnsSorted.addAll(columnsToSort);
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
                    result = escapeSpecialChars(prop.getText(result));
                } else {
                    while (startIndex!=-1 && failsafe++<20) {
                        hasDot = result.contains(".");
                        if (startIndex<0 && hasDot == false) return result;

                        int endIndex = result.indexOf("}]]", startIndex);
                        if (endIndex>startIndex) {
                            String key = result.substring(startIndex+4, endIndex);
                            String translated = escapeSpecialChars(prop.getText(key));
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
        boolean includeCommonSettings = true;
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

            for (Field declaredField : declaredFields) {
                if (!declaredField.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
                    continue;
                }

                sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = declaredField.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
                String tab = annotation.tab();

                //If we dont want common settings, skip all fields with tab commonSettings
                if(includeCommonSettings==false && "commonSettings".equals(tab)) continue;

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
}
