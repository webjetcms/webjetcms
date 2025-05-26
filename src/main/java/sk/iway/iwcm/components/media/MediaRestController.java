package sk.iway.iwcm.components.media;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.SpecSearch;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaEditorFields;
import sk.iway.spirit.model.MediaGroupBean;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/media")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('editor_edit_media_all|menuWebpages')")
public class MediaRestController extends DatatableRestControllerV2<Media, Long> {

    private final MediaRepository mediaRepository;
    private final MediaGroupRepository mediaGroupRepository;
    private static final String DOCUMENTS_TABLE_NAME = "documents";

    @Autowired
    public MediaRestController(MediaRepository mediaRepository, MediaGroupRepository mediaGroupRepository) {
        super(mediaRepository);
        this.mediaRepository = mediaRepository;
        this.mediaGroupRepository = mediaGroupRepository;
    }

    /**
     * Ziska vsetky media
     * POZOR: ma 3 rezimy:
     * - ak ide zo samostatnej media stranky /admin/v9/webpages/media/ zobrazuje vsetky media bez ohladu na web stranku
     * - ak ide zo zalozky Media vo web strankach dostava ako URL parameter docId a groupId, podla toho vrati len media pre danu stranku
     * - ak ide zo zalozky Media a je to NOVA web stranka pouzije ziskanie podla ID prihlaseneho pouzivatela a fk_table_name documents_temp
     */
    @Override
    public Page<Media> getAllItems(Pageable pageable) {

        int domainId = CloudToolsForCore.getDomainId();

        //get docId
        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);

        //get groupId
        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), -1);

        //get user identity
        final Identity user = UsersDB.getCurrentUser(getRequest());

        //checking permisions
        if(docId < 1) {

            //if user don't have permisions, throw error message for user
            if(!GroupsDB.isGroupEditable(user, groupId)) {
                throw new IllegalArgumentException(getProp().getText("editor.permsDenied"));
            }
        } else {
            //if user don't have permisions, throw error message for user
            if(!EditorDB.isPageEditable(user, docId)) {
                throw new IllegalArgumentException(getProp().getText("editor.permsDenied"));
            }
        }

        DatatablePageImpl<Media> page;

        //get Media by docId (if docId is < 1 use user.userId)
        if (getRequest().getParameter("docId")==null && getRequest().getParameter("groupId")==null) {

            if (getUser().isDisabledItem("editor_edit_media_all")) throw new IllegalArgumentException("Access is denied");

            //docId ani groupId ako parameter neboli poslane = sme v zozname vsetkych medii
            page = new DatatablePageImpl<>(mediaRepository.findAllByDomainId(domainId, pageable));
        } else {
            //dostali sme docId parameter, sme vnorena datatabulka vo web stranke
            page = new DatatablePageImpl<>(mediaRepository.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(getMediaFkId(), getTableName(), domainId, pageable));
        }

        //create list of MediaGroupBeans, used as value in multiselect when create/edit Media
        List<MediaGroupBean> groups;
        if (groupId < 1) groups = MediaDB.getGroups();
        else groups = MediaDB.getGroups(groupId);

        //add, getted Media groups as option for "Integer[] groups" located in MediaEditorFields
        page.addOptions("editorFields.groups", groups, "mediaGroupName", "mediaGroupId", false);

        processFromEntity(page, ProcessItemAction.GETALL);

        return page;
    }

    //when create/edit Media, call method toMedia from MediaEditorFiled
    @Override
    public void beforeSave(Media entity) {

        int docId;
        String tableName;

        //First check if call is from EditorDataTable
        //If yes set table fix like "documets" and docId get from doc tree selector (is required)
        if(isCallerFromAllMedia()) {
            tableName = DOCUMENTS_TABLE_NAME;
            docId = entity.getEditorFields().getDocDetails().getDocId();
        } else {
            //docid zober z request.getDocId(pripad editacie vo web stranke) alebo z entity (ak je zadane v editacii) alebo ako -1 ked nie je nic
            docId = Tools.getIntValue(getRequest().getParameter("docId"), Tools.getIntValue(entity.getMediaFkId(), -1));
            if (docId > 0) {
                int mutigroupDocId = MultigroupMappingDB.getMasterDocId(docId);
                if (mutigroupDocId > 0) docId = mutigroupDocId;
            }
            tableName = entity.getMediaFkTableName();
            if (Tools.isEmpty(tableName)) tableName = getTableName();

            if(docId < 1) {
                Identity user = getUser();
                docId = user.getUserId();
            }
        }

        checkPerms(docId);

        //IMPORTANT - dont use method getMediaFkId here because we need know if it's docId or userId
        //this difference we need to know to call getLastOrder with right param
        Integer mediaSortOrder = entity.getMediaSortOrder();
        //if mediaSortOrder is empty set using docId (aka userId)
        if(mediaSortOrder == null || mediaSortOrder == 0) {
            mediaSortOrder = MediaDB.getLastOrder(docId, tableName) + 10;
        }

        //set docId(document creating/editing media) and table name as "documents"
        if (entity.getMediaFkId()==null) entity.setMediaFkId(docId);
        if (Tools.isEmpty(entity.getMediaFkTableName())) entity.setMediaFkTableName(tableName);

        //set new mediaSortOrder
        if (entity.getMediaSortOrder()==null) entity.setMediaSortOrder(mediaSortOrder);

        //set last update (current date and time)
        Date date = new Date();
        if (getRequest().getParameter("docId")==null) {
            try {
                //sme vo vsetkych mediach, ak medium existuje, zachovajme datum
                if (entity.getId()!=null && entity.getId()>0) {
                    Media current = mediaRepository.findFirstByIdAndDomainId(entity.getId(), CloudToolsForCore.getDomainId()).orElse(null);
                    if (current != null) date = current.getLastUpdate();
                } else if (entity.getMediaFkId()!=null && DOCUMENTS_TABLE_NAME.equals(entity.getMediaFkTableName())) {
                    //skus podla poslednej zmeny web stranky
                    DocDetails doc = DocDB.getInstance().getDoc(entity.getMediaFkId().intValue());
                    if (doc != null) {
                        //nastav sekundu pred publikovanim stranky
                        date = new Date(doc.getDateCreated()-1000);
                    }
                }
            } catch (Exception ex) {
                Logger.error(MediaRestController.class, ex);
            }
        }
        entity.setLastUpdate(date);

        //call toMedia method to handle groups convert
        entity.getEditorFields().toMedia(entity, mediaGroupRepository);
    }

    @Override
    public Media getOneItem(long id) {

        Media entity = super.getOneItem(id);

        //set empty Media entity (without setting, creating dialog will not show but don't throw any error)
        if(entity == null) {
            entity = new Media();
            entity.setGroups(new ArrayList<>());
            entity.setMediaFkTableName("");
            entity.setMediaFkId(-1);
            entity.setMediaLink("");
            entity.setMediaThumbLink("");
            entity.setMediaTitleSk("");
        }

        //Creating new Media from webPage
        if(id < 1) {
            int docId = getMediaFkId();
            String tableName = getTableName();

            Integer mediaSortOrder = Integer.valueOf(MediaDB.getLastOrder(docId, tableName) + 10);
            entity.setMediaFkTableName(tableName);
            entity.setMediaFkId(docId);
            entity.setMediaSortOrder(mediaSortOrder);

            //Call is from MediaDataTable
            if(isCallerFromAllMedia()) {
                //Fix set table
                entity.setMediaFkTableName(DOCUMENTS_TABLE_NAME);

                //DocId set as null, because we use doc tree select
                entity.setMediaFkId(null);
            }
        }

        if (entity.getMediaFkId()!=null && entity.getMediaFkId() > 0) checkPerms(entity.getMediaFkId());

        return processFromEntity(entity, ProcessItemAction.GETONE);
    }

    /**
     * Vrati docId z request parametra, ak je -1 (nova web stranka) vrati ID prihlaseneho pouzivatela
     * @return
     */
    public int getMediaFkId() {
        //get docId
        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);
        if (docId > 0) {
            int mutigroupDocId = MultigroupMappingDB.getMasterDocId(docId);
            if (mutigroupDocId > 0) docId = mutigroupDocId;
        }

        if(docId < 1) {
            //get user identity
            final Identity user = getUser();
            return user.getUserId();
        } else return docId;
    }

    /**
     * Vrati meno tabulky (fk_table_name), kde pre kladne docId z requestu
     * je to documents a pre novu web stranku (docId&lt;1) vrati documents_temp
     * @return
     */
    public String getTableName() {
        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);

        if(docId  < 1) {
            return "documents_temp";
        } else return DOCUMENTS_TABLE_NAME;
    }

    private void checkPerms(int docId) {
        if (docId > 0) {
            Identity user = getUser();
            if(user.isEnabledItem("editor_edit_media_all")==false && EditorDB.isPageEditable(user, docId)==false) {
                throw new IllegalArgumentException(getProp().getText("datatables.accessDenied.title.js"));
            }
        }
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, Media> target, Identity user, Errors errors, Long id, Media entity) {
        boolean valid = true;
        if (isCallerFromAllMedia()) {
            if(entity.getEditorFields() == null) valid = false;

            DocDetails doc = entity.getEditorFields().getDocDetails();

            if(doc == null || doc.getDocId() < 0) valid = false;

            if(!valid) errors.rejectValue("errorField.editorFields.docDetails", null, getProp().getText("media.doc_tree_select.required"));

            //also show validate error for title
            if (Tools.isEmpty(entity.getMediaTitleSk())) errors.rejectValue("errorField.mediaTitleSk", null, getProp().getText("jakarta.validation.constraints.NotBlank.message"));
        }
    }

    /**
     * Returns true if call is from all media page
     * @return
     */
    private boolean isCallerFromAllMedia() {
        return "true".equals(getRequest().getParameter("isCalledFromTable"));
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<Media> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);
        int groupId = Tools.getIntValue(params.get("searchEditorFields.groups"), -1);
        if (groupId > 0) {
            predicates.add(builder.equal(root.join("groups").get("id"), groupId));
        }
        String docTitle = params.get("searchEditorFields.docDetails");
        if (Tools.isNotEmpty(docTitle)) {
            SpecSearch<Media> specSearch = new SpecSearch<>();
            specSearch.addSpecSearchDocFullPath(docTitle, "mediaFkId", predicates, root, builder);
        }
    }

    @Override
    public Media processFromEntity(Media entity, ProcessItemAction action, int rowCount) {
        //GetOneItem call super, and super call this with null entity !
        if(entity == null) return entity;

        //Prepare instance of editor fields
        MediaEditorFields mef = new MediaEditorFields();

        //Set "volitelne polia"
        if(rowCount == 1)
            mef.setFieldsDefinition( mef.getFields(entity, "components.media", 'F') );

        //Perform from action
        mef.fromMedia(entity);

        //Set prepared editor fields
        entity.setEditorFields(mef);
        return entity;
    }

}
