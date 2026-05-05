package sk.iway.iwcm.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanWrapperImpl;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.tags.support.ResponseUtils;

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

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="apps.showForLoggedUser.title",
        tab = "commonSettings",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "apps.showForLoggedUser.all", value = ""),
                    @DataTableColumnEditorAttr(key = "apps.showForLoggedUser.onlyLogged", value = "onlyLogged"),
                    @DataTableColumnEditorAttr(key = "apps.showForLoggedUser.onlyNotLogged", value = "onlyNotLogged")
                }
            )
        }
    )
    public String showForLoggedUser = "";

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.news.cacheMinutes", tab = "commonSettings")
	public Integer cacheMinutes;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, title = "", tab = "commonSettings")
	public String appHideFields;

    @DataTableColumn(
        inputType = DataTableColumnType.MULTISELECT,
        title="apps.wrapper.class.title",
        tab = "commonSettings",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.tab.style"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
                }
            )
        }
    )
    public String wrapperClass;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "apps.wrapper.id.title", tab = "commonSettings")
    public String wrapperId;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "apps.wrapper.title.title", tab = "commonSettings")
    public String wrapperTitle;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "apps.wrapper.ariaLabel.title", tab = "commonSettings")
    public String wrapperAriaLabel;

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
     * Returns base options for common fields (e.g. wrapperClass) from configuration.
     * These are merged with subclass-specific options in ComponentsService.
     * Supports format: "label:value" or just "value". Label can be a translation key.
     */
    public Map<String, List<OptionDto>> getBaseAppOptions(HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        String wrapperClasses = Constants.getString("appWrapperClasses", "");
        if (Tools.isNotEmpty(wrapperClasses)) {
            List<OptionDto> wrapperOptions = parseOptionsFromConfig(request, wrapperClasses);
            if (wrapperClass != null) {
                addCurrentValueToOptions(wrapperOptions, Tools.getTokens(wrapperClass, "+"));
            }
            options.put("wrapperClass", wrapperOptions);
        }
        return options;
    }

    /**
     * Parses a comma-separated string of translation-key:value or just value into a list of OptionDto.
     * @param request The request to retrieve translation properties.
     * @param config The configuration string to parse.
     * @return A list of parsed OptionDto.
    */
    protected List<OptionDto> parseOptionsFromConfig(HttpServletRequest request, String config) {
        List<OptionDto> options = new ArrayList<>();
        if (Tools.isEmpty(config)) {
            return options;
        }
        Prop prop = Prop.getInstance(request);
        String[] tokens = Tools.getTokens(config, ",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (Tools.isNotEmpty(trimmed)) {
                int colonIndex = trimmed.lastIndexOf(':');
                if (colonIndex > 0) {
                    String label = trimmed.substring(0, colonIndex).trim();
                    String value = trimmed.substring(colonIndex + 1).trim();
                    label = prop.getText(label);
                    options.add(new OptionDto(label, value, null));
                } else {
                    options.add(new OptionDto(trimmed, trimmed, null));
                }
            }
        }
        return options;
    }

    /**
     * Build wrapper div HTML for component output.
     * Returns null if no wrapper attributes are set.
     * @param wrapperClassValue - wrapper CSS class value (+ separated)
     * @param wrapperIdValue - wrapper id value
     * @param wrapperTitleValue - wrapper title value
     * @param wrapperAriaLabelValue - wrapper aria-label value
     * @return String array with [0] = opening tag, [1] = closing tag, or null if no wrapper needed
     */
    public static String[] buildWrapperDiv(String wrapperClassValue, String wrapperIdValue, String wrapperTitleValue, String wrapperAriaLabelValue) {
        if (Tools.isEmpty(wrapperClassValue) && Tools.isEmpty(wrapperIdValue) && Tools.isEmpty(wrapperTitleValue) && Tools.isEmpty(wrapperAriaLabelValue)) {
            return null;
        }
        StringBuilder wrapper = new StringBuilder("<div");
        if (Tools.isNotEmpty(wrapperClassValue)) {
            wrapper.append(" class=\"").append(ResponseUtils.filter(wrapperClassValue.replace('+', ' '))).append("\"");
        }
        if (Tools.isNotEmpty(wrapperIdValue)) {
            wrapper.append(" id=\"").append(ResponseUtils.filter(wrapperIdValue)).append("\"");
        }
        if (Tools.isNotEmpty(wrapperTitleValue)) {
            wrapper.append(" title=\"").append(ResponseUtils.filter(wrapperTitleValue)).append("\"");
        }
        if (Tools.isNotEmpty(wrapperAriaLabelValue)) {
            wrapper.append(" aria-label=\"").append(ResponseUtils.filter(wrapperAriaLabelValue)).append("\"");
        }
        wrapper.append(">");
        return new String[] { wrapper.toString(), "</div>" };
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

    public String getShowForLoggedUser() {
        return showForLoggedUser;
    }

    public void setShowForLoggedUser(String showForLoggedUser) {
        this.showForLoggedUser = showForLoggedUser;
    }

    public String getAppHideFields() {
        return appHideFields;
    }

    public void setAppHideFields(String appHideFields) {
        this.appHideFields = appHideFields;
    }

    public String getWrapperClass() {
        return wrapperClass;
    }

    public void setWrapperClass(String wrapperClass) {
        this.wrapperClass = wrapperClass;
    }

    public String getWrapperId() {
        return wrapperId;
    }

    public void setWrapperId(String wrapperId) {
        this.wrapperId = wrapperId;
    }

    public String getWrapperTitle() {
        return wrapperTitle;
    }

    public void setWrapperTitle(String wrapperTitle) {
        this.wrapperTitle = wrapperTitle;
    }

    public String getWrapperAriaLabel() {
        return wrapperAriaLabel;
    }

    public void setWrapperAriaLabel(String wrapperAriaLabel) {
        this.wrapperAriaLabel = wrapperAriaLabel;
    }
}