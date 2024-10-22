package sk.iway.iwcm.components;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.datatable.OptionDto;

/**
 * Interface pre prácu s WebjetKomponentami
 */
public interface WebjetComponentInterface {
    void init();
    void init(HttpServletRequest request, HttpServletResponse response);
    //init app editor in webpages editor
    void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request);
    Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request);
    String getViewFolder();
}
