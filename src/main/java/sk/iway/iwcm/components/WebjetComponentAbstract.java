package sk.iway.iwcm.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanWrapperImpl;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Base/Abstract class for WebJET app
 */
public abstract class WebjetComponentAbstract implements WebjetComponentInterface {

    /** DEFAULT commonSettings TAB */
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="apps.devices.title",
        tab = "commonSettings",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "apps.devices.phone", value = "phone"),
                    @DataTableColumnEditorAttr(key = "apps.devices.tablet", value = "tablet"),
                    @DataTableColumnEditorAttr(key = "apps.devices.pc", value = "pc")
                }, attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
    public String device;

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.news.cacheMinutes", tab = "commonSettings")
	public Integer cacheMinutes;

    /**
     * String viewFolder is used to enter the subdirectory where the resulting view will be searched
     */
    private String viewFolder;

    /**
     * Initialization method for custom component initialization
     * Called every time a component is inserted into the page
     */
    public void init() {

    }

    /**
     * Initialization method for custom component initialization
     * Called every time a component is inserted into the page
     */
    public void init(HttpServletRequest request, HttpServletResponse response) {

    }

    /**
     * Returns list of options for app editor in webpage
     */
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        return null;
    }

    /**
     * Initialization method for app editor in webpage
     */
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {

    }

    /**
     * Verify, that value is in options, if not, add it
     * @param options
     * @param value
     */
    public List<OptionDto> addCurrentValueToOptions(List<OptionDto> options, String value) {
        if (value == null) return options;
        String[] values = {value};
        return addCurrentValueToOptions(options, values);
    }

    /**
     * Verify, that values are in options, if not, add it
     * @param options
     * @param values
     * @return
     */
    public List<OptionDto> addCurrentValueToOptions(List<OptionDto> options, String[] values) {
        if (values != null && values.length > 0) {
            for (String value : values) {
                //iterate options and add if not found in value
                boolean found = false;
                for (OptionDto option : options) {
                    if (option.getValue().equals(value)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    options.add(new OptionDto(value, value, null));
                }
            }
        }
        return options;
    }

    /**
     * Add options from list type id:label, id will be converted to string and used as value for option.
     * Use in getAppOptions method like:
     *
     * Map&lt;String, List&lt;OptionDto&gt;&gt; options = new HashMap&lt;&gt;();
     * options.put("groups", addOptions(MediaDB.getGroups(), "mediaGroupName", "mediaGroupId", false));
     * return options;
     *
     * @param options - list of objects
     * @param labelProperty - name of label property in options list
     * @param valueProperty - name of value property in options list
     * @param includeOriginalObject - if true, original object will be added to OptionDto
     * @return
     */
    @SuppressWarnings("rawtypes")
    public List<OptionDto> addOptions(List options, String labelProperty, String valueProperty, boolean includeOriginalObject) {
        List<OptionDto> fieldOptions = new ArrayList<>();
        for (Object o : options) {
            BeanWrapperImpl bw = new BeanWrapperImpl(o);

            String label;
            String value;

            if(Tools.isEmpty(labelProperty) && Tools.isEmpty(valueProperty)) {
                label = (String)o;
                value = (String)o;
            } else {
                label = String.valueOf(bw.getPropertyValue(labelProperty));
                value = String.valueOf(bw.getPropertyValue(valueProperty));
            }

            Object original;
            if (includeOriginalObject) original = o;
            else original = null;
            fieldOptions.add(new OptionDto(label, value, original));
        }
        return fieldOptions;
    }

    @Override
    public String getViewFolder() {
        return viewFolder;
    }

    public void setViewFolder(String viewFolder) {
        this.viewFolder = viewFolder;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getCacheMinutes() {
        return cacheMinutes;
    }

    public void setCacheMinutes(Integer cacheMinutes) {
        this.cacheMinutes = cacheMinutes;
    }
}