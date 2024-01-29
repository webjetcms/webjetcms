package sk.iway.iwcm.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.system.datatable.DataTableColumnType;
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