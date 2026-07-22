package sk.iway.iwcm.components.gdpr.rest;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.components.gdpr.GdprDataDeleting;
import sk.iway.iwcm.components.gdpr.GdprDataDeleting.GdprDataDeletingType;
import sk.iway.iwcm.components.gdpr.GdprDataDeletingEntity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/gdpr/data_deleting")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuGDPRDelete')")
@Datatable
public class GdprDataDeletingRestController extends DatatableRestControllerV2<GdprDataDeletingEntity, Long> {

    private final HttpServletRequest request;
    private static final String DAYS_KEY = "welcome.statBackTime.days";

    @Autowired
    public GdprDataDeletingRestController(HttpServletRequest request) {
        super(null);
        this.request = request;
    }

    //This page doesnt use repository or DB class, we will create 4 entities static (only two columns are from DB)
    @Override
    public Page<GdprDataDeletingEntity> getAllItems(Pageable pageable) {

        List<GdprDataDeletingEntity> items =  new ArrayList<>();

        for (GdprDataDeletingType type : GdprDataDeletingType.values()) {
            items.add(getDataDeleteEntityByType(type));
        }

        return new DatatablePageImpl<>(items);
    }

    @Override
    public GdprDataDeletingEntity getOneItem(long id) {
        return getDataDeleteEntityByType(GdprDataDeletingType.getById(id));
    }

    //Only statTime column can be changed (every entity use different method to update value)
    @Override
    public GdprDataDeletingEntity editItem(GdprDataDeletingEntity entity, long id) {

        String newStatTimeString = "";
        Prop prop = getProp();
        GdprDataDeletingType type = GdprDataDeletingType.getById(id);

        if (type != null) {
            switch (type) {
                case UNUSED_USERS:
                    ConfDB.setName(type.getAfterConstant(), entity.getStatTime() + "");
                    newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
                    break;
                case SENDED_EMAILS:
                    ConfDB.setName(type.getAfterConstant(), entity.getStatTime() + "");
                    newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
                    break;
                case OLD_FORM_DATA:
                    ConfDB.setName(type.getAfterConstant(), entity.getStatTime() + "");
                    newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
                    break;
                case OLD_BASKET_ORDERS:
                    ConfDB.setName(type.getAfterConstant(), entity.getStatTime() + "");
                    newStatTimeString = entity.getStatTime() + " " + prop.getText("components.gdpr.rokov");
                    break;
                case OLD_DOC_AND_GROUPS:
                    ConfDB.setName(type.getAfterConstant(), entity.getStatTime() + "");
                    newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
                    break;
            }
        }

        entity = getDataDeleteEntityByType(type);
        entity.setStatTimeString(newStatTimeString);

        return entity;
    }

    @Override
    public boolean processAction(GdprDataDeletingEntity entity, String action) {

        //Custom delete (on front end its look like normal delete button)
        if(action.equals("customDataDelete")) {

            GdprDataDeletingType type = GdprDataDeletingType.getById(entity.getId());
            GdprDataDeleting gdprdd = GdprDataDeleting.getInstance(request);

            if (type != null) {
                switch (type) {
                    case UNUSED_USERS:
                        gdprdd.deleteUnusedUsers();
                        break;
                    case SENDED_EMAILS:
                        gdprdd.deleteSendedEmails();
                        break;
                    case OLD_FORM_DATA:
                        gdprdd.deleteOldFormData();
                        break;
                    case OLD_BASKET_ORDERS:
                        gdprdd.deleteOldBasketOrders();
                        break;
                    case OLD_DOC_AND_GROUPS:
                        gdprdd.deleteOldDocAndGroups();
                        break;
                }
            }
        }

        return true;
    }

    //Depending on type will create entity, fill its columns and return it
    GdprDataDeletingEntity getDataDeleteEntityByType(GdprDataDeletingType type) {

        GdprDataDeletingEntity entity = new GdprDataDeletingEntity();
        if (type == null) return entity;

        GdprDataDeleting gdprdd = GdprDataDeleting.getInstance(request);
        Prop prop = getProp();

        entity.setId(type.getId());

        switch (type) {
            case UNUSED_USERS:
                entity.setType(prop.getText("components.gdpr.type.users"));
                entity.setStatTime(type.getAfterConstantInt());
                entity.setStatTimeString(type.getAfterConstantInt() + " " + prop.getText(DAYS_KEY));
                entity.setRecordCnt(GdprDataDeleting.getUnusedUsers().size());
                entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_uzivatelov_za_dane_obdobie"));
                break;
            case SENDED_EMAILS:
                entity.setType(prop.getText("components.gdpr.type.emails"));
                entity.setStatTime(type.getAfterConstantInt());
                entity.setStatTimeString(type.getAfterConstantInt() + " " + prop.getText(DAYS_KEY));
                entity.setRecordCnt(gdprdd.getSendedEmailsCount());
                entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_vsetky_odoslane_emaily"));
                break;
            case OLD_FORM_DATA:
                entity.setType(prop.getText("components.gdpr.type.forms"));
                entity.setStatTime(type.getAfterConstantInt());
                entity.setStatTimeString(type.getAfterConstantInt() + " " + prop.getText(DAYS_KEY));
                entity.setRecordCnt(gdprdd.getOldFormDataCount());
                entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_data_vo_formularoch_za_dane_obdobie"));
                break;
            case OLD_BASKET_ORDERS:
                entity.setType(prop.getText("components.gdpr.type.eshop"));
                entity.setStatTime(type.getAfterConstantInt());
                entity.setStatTimeString(type.getAfterConstantInt() + " " + prop.getText("components.gdpr.rokov"));
                entity.setRecordCnt((int)gdprdd.getOldBasketOrdersCount());
                entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_objednavky_z_modulu_elektorincky_obchod_za_dane_obdobie"));
                break;
            case OLD_DOC_AND_GROUPS:
                entity.setType(prop.getText("components.gdpr.type.old_doc_and_groups"));
                entity.setStatTime(type.getAfterConstantInt());
                entity.setStatTimeString(type.getAfterConstantInt() + " " + prop.getText(DAYS_KEY));
                entity.setRecordCnt(0);
                entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.old_doc_and_groups", type.getAfterConstantInt() + ""));
                break;
        }

        return entity;
    }

}
