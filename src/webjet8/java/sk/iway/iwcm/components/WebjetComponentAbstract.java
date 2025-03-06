package sk.iway.iwcm.components;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
     * @param paramName
     * @param componentRequest
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