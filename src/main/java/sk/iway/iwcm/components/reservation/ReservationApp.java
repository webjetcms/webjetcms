package sk.iway.iwcm.components.reservation;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.reservation.ReservationApp")
@WebjetAppStore(nameKey = "components.reservation.title", descKey = "components.reservation.desc", itemKey = "cmp_reservation", imagePath = "/components/reservation/editoricon.png", galleryImages = "/components/reservation/", componentPath = "/components/reservation/reservation_list.jsp,/components/reservation/room_list.jsp")
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
        @DataTableTab(id = "componentIframeWindowTabList", title = "components.reservation.reservation_list", content = ""),
        @DataTableTab(id = "componentIframeWindowTabListObjects", title = "components.reservation.reservationObjectList", content = ""),
})
@Getter
@Setter
public class ReservationApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.reservation.editor_component.reservation_type", className = "dt-app-skip dt-app-componentPath", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.reservation.editor_component.reservation_list", value = "/components/reservation/reservation_list.jsp"),
                    @DataTableColumnEditorAttr(key = "components.reservation.editor_component.room_list", value = "/components/reservation/room_list.jsp"),
            })
    })
    private String reservationType;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabList", title = "&nbsp;")
    private String iframe = "/components/reservation/admin_reservation_list.jsp";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabListObjects", title = "&nbsp;")
    private String iframe2 = "/components/reservation/admin_object_list.jsp";

}
