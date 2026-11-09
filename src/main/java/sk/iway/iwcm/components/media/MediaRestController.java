package sk.iway.iwcm.components.media;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.branch.BranchContexts;
import sk.iway.iwcm.branch.BranchMedia;
import sk.iway.iwcm.branch.BranchMediaEditor;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.doc.PlannedChangesService;
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

        if (BranchContexts.current().isBranch()) return branchList(getParamsMap(getRequest()),pageable);

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
            page = new DatatablePageImpl<>(mediaRepository.findAllByMediaFkTableNameAndDomainId(DOCUMENTS_TABLE_NAME, domainId, pageable));
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

        if (BranchContexts.current().isBranch()) {
            if (isPlanned() || isImporting() || isDuplicate() || Tools.isNotEmpty(getUpdateByColumn())) throw new IllegalArgumentException("Only individual media CRUD is available in a publishing branch");
            new BranchMediaEditor(getRequest()).owner(branchOwner(entity),true);
            return;
        }

        int docId;
        String tableName;

        //First check if call is from EditorDataTable
        //If yes set table fix like "documets" and docId get from doc tree selector (is required)
        if(isCallerFromAllMedia()) {
            tableName = DOCUMENTS_TABLE_NAME;
            docId = entity.getEditorFields().getDocDetails().getDocId();

            checkPerms(docId);
        } else {
            // Embedded media require the page context; a new page explicitly sends docId=-1.
            String requestedDocId = getRequest().getParameter("docId");
            if (Tools.isEmpty(requestedDocId)) throw new IllegalArgumentException(getProp().getText("components.media.pageContextRequired"));
            docId = Tools.getIntValue(requestedDocId, -1);
            if (docId > 0) {
                int mutigroupDocId = MultigroupMappingDB.getMasterDocId(docId);
                if (mutigroupDocId > 0) docId = mutigroupDocId;
            }
            tableName = getTableName();

            checkPerms(docId);

            if(docId < 1) {
                Identity user = getUser();
                docId = user.getUserId();
            }
        }

        if (isPlanned()) PlannedChangesService.getInstance().validateMedia(entity, docId);
        else PlannedChangesService.rejectPrivateReferences(entity);

        //IMPORTANT - dont use method getMediaFkId here because we need know if it's docId or userId
        //this difference we need to know to call getLastOrder with right param
        Integer mediaSortOrder = entity.getMediaSortOrder();
        //if mediaSortOrder is empty set using docId (aka userId)
        if(mediaSortOrder == null || mediaSortOrder == 0) {
            mediaSortOrder = MediaDB.getLastOrder(docId, tableName) + 10;
        }

        //set docId(document creating/editing media) and table name as "documents"
        entity.setMediaFkId(docId);
        entity.setMediaFkTableName(tableName);
        entity.setDomainId(CloudToolsForCore.getDomainId());

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

        if (BranchContexts.current().isBranch()) {
            BranchMediaEditor editor=new BranchMediaEditor(getRequest());
            Media entity;
            if(id>0) entity=editor.load(Math.toIntExact(id),Tools.getIntValue(getRequest().getParameter("docId"),-1));
            else {
                entity=new Media();entity.setGroups(new ArrayList<>());entity.setMediaFkTableName(DOCUMENTS_TABLE_NAME);
                entity.setMediaLink("");entity.setMediaThumbLink("");entity.setMediaTitleSk("");
                int owner=Tools.getIntValue(getRequest().getParameter("docId"),-1);
                if(!isCallerFromAllMedia()) editor.owner(owner,false);
                entity.setMediaFkId(owner>0?owner:null);
                entity.setMediaSortOrder(owner>0?BranchMedia.list(owner,null).stream().mapToInt(Media::getMediaSortOrder).max().orElse(0)+10:10);
                MediaEditorFields fields=new MediaEditorFields();fields.setBranchHash("new");entity.setEditorFields(fields);
            }
            return processFromEntity(entity,ProcessItemAction.GETONE,1);
        }

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

        if (entity.getMediaFkId()!=null && entity.getMediaFkId() > 0 && "-1".equals(getRequest().getParameter("docId"))==false) checkPerms(entity.getMediaFkId());

        return processFromEntity(entity, ProcessItemAction.GETONE);
    }

    /**
     * Vrati docId z request parametra, ak je -1 (nova web stranka) vrati ID prihlaseneho pouzivatela
     * @return
     */
    public int getMediaFkId() {
        //get docId
        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);
        if (BranchContexts.current().isBranch()) {
            new BranchMediaEditor(getRequest()).owner(docId,false);
            return docId;
        }
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

        if(BranchContexts.current().isBranch()) {
            if(docId<1&&!isCallerFromAllMedia()) throw new IllegalArgumentException("Save the page in this branch before adding media");
            return DOCUMENTS_TABLE_NAME;
        }

        if (isPlanned()) {
            if (docId < 1 || !PlannedAccess.enabled()) throw new IllegalArgumentException("Planned page context is required");
            return PlannedChangesService.PLANNED_MEDIA_TABLE;
        }
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
        if(BranchContexts.current().isBranch() && (!List.of("create","edit","remove").contains(target.getAction())||Tools.isNotEmpty(target.getUpdateByColumn())||Tools.isNotEmpty(target.getImportMode())||target.getImportedColumns()!=null)) {
            errors.reject("branch.media.unsupported","Only individual media CRUD is available in a publishing branch");
            return;
        }
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
        if (getRequest().getParameter("docId") != null) {
            checkPerms(Tools.getIntValue(getRequest().getParameter("docId"), -1));
            predicates.add(builder.equal(root.get("mediaFkTableName"), getTableName()));
            predicates.add(builder.equal(root.get("mediaFkId"), getMediaFkId()));
        } else {
            predicates.add(builder.notEqual(root.get("mediaFkTableName"), PlannedChangesService.PLANNED_MEDIA_TABLE));
        }
        String docTitle = params.get("searchEditorFields.docDetails");
        if (Tools.isNotEmpty(docTitle)) {
            SpecSearch<Media> specSearch = new SpecSearch<>();
            specSearch.addSpecSearchDocFullPath(docTitle, "mediaFkId", predicates, root, builder);
        }
    }


    private boolean isPlanned() {
        return "true".equals(getRequest().getParameter("planned"));
    }

    @Override
    public boolean checkItemPerms(Media entity, Long id) {
        if(BranchContexts.current().isBranch()) {
            if(id!=null&&id>0) new BranchMediaEditor(getRequest()).load(Math.toIntExact(id),Tools.getIntValue(getRequest().getParameter("docId"),-1));
            return true;
        }
        if (id != null && id > 0) {
            Media stored = mediaRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
            if (stored == null) return false;
            if (getRequest().getParameter("docId") != null) {
                if (!getTableName().equals(stored.getMediaFkTableName()) || !Integer.valueOf(getMediaFkId()).equals(stored.getMediaFkId())) return false;
            } else if (PlannedChangesService.PLANNED_MEDIA_TABLE.equals(stored.getMediaFkTableName())) return false;
            if ("documents_temp".equals(stored.getMediaFkTableName())) {
                if (!Integer.valueOf(getUser().getUserId()).equals(stored.getMediaFkId())) return false;
            } else checkPerms(stored.getMediaFkId());
        }
        return true;
    }

    @Override
    public boolean beforeDelete(Media entity) {
        if(BranchContexts.current().isBranch()) return true;
        if (isPlanned()) PlannedChangesService.getInstance().requirePlannedMedia(getMediaFkId());
        return true;
    }

    @Override
    public void beforeDuplicate(Media entity) {
        if(BranchContexts.current().isBranch()) throw new IllegalArgumentException("Media duplication is unavailable in a publishing branch");
        super.beforeDuplicate(entity);
    }

    @Override
    public Media processFromEntity(Media entity, ProcessItemAction action, int rowCount) {
        //GetOneItem call super, and super call this with null entity !
        if(entity == null) return entity;

        //Prepare instance of editor fields
        MediaEditorFields mef = new MediaEditorFields();
        if(entity.getEditorFields()!=null) mef.setBranchHash(entity.getEditorFields().getBranchHash());

        //Set "volitelne polia"
        if(rowCount == 1)
            mef.setFieldsDefinition( mef.getFields(entity, "components.media", 'F') );

        //Perform from action
        mef.fromMedia(entity);

        //Set prepared editor fields
        entity.setEditorFields(mef);
        return entity;
    }

    @Override
    public Media insertItem(Media entity) {
        if(!BranchContexts.current().isBranch()) return super.insertItem(entity);
        return processFromEntity(new BranchMediaEditor(getRequest()).save(entity,branchOwner(entity),"new"),ProcessItemAction.CREATE,1);
    }

    @Override
    public Media editItem(Media entity,long id) {
        if(!BranchContexts.current().isBranch()) return super.editItem(entity,id);
        String hash=entity.getEditorFields()==null?null:entity.getEditorFields().getBranchHash();
        int owner=branchOwner(entity);
        Media stored=new BranchMediaEditor(getRequest()).load(Math.toIntExact(id),owner);
        copyEntityIntoOriginal(entity,stored);
        return processFromEntity(new BranchMediaEditor(getRequest()).save(stored,owner,hash),ProcessItemAction.EDIT,1);
    }

    @Override
    public boolean deleteItem(Media entity,long id) {
        if(!BranchContexts.current().isBranch()) return super.deleteItem(entity,id);
        return new BranchMediaEditor(getRequest()).delete(Math.toIntExact(id),Tools.getIntValue(getRequest().getParameter("docId"),-1),entity.getEditorFields()==null?null:entity.getEditorFields().getBranchHash());
    }

    private int branchOwner(Media entity) {
        if(isCallerFromAllMedia()) {
            if(getUser().isDisabledItem("editor_edit_media_all")) throw new IllegalArgumentException("Access is denied");
            if(entity.getEditorFields()==null||entity.getEditorFields().getDocDetails()==null) throw new IllegalArgumentException("A media owner is required");
            return entity.getEditorFields().getDocDetails().getDocId();
        }
        return Tools.getIntValue(getRequest().getParameter("docId"),-1);
    }

    @Override
    public Page<Media> searchItem(Map<String,String> params,Pageable pageable,Media search) {
        if(BranchContexts.current().isBranch()) return branchList(params,pageable);
        return super.searchItem(params,pageable,search);
    }

    private Page<Media> branchList(Map<String,String> params,Pageable pageable) {
        BranchMediaEditor editor=new BranchMediaEditor(getRequest());
        int owner=Tools.getIntValue(getRequest().getParameter("docId"),-1);
        int folder=-1;
        if(getRequest().getParameter("docId")!=null) folder=editor.owner(owner,false).getGroupId();
        else if(getUser().isDisabledItem("editor_edit_media_all")) throw new IllegalArgumentException("Access is denied");
        List<Media> records=new ArrayList<>(BranchMedia.list(owner,null));
        records.removeIf(media->!Integer.valueOf(CloudToolsForCore.getDomainId()).equals(media.getDomainId()));
        for(var entry:params.entrySet()) {
            String value=entry.getValue();
            if(!entry.getKey().startsWith("search")||Tools.isEmpty(value))continue;
            String property=getCleanKey(entry.getKey());
            if("editorFields.groups".equals(property)) {
                int category=Tools.getIntValue(getCleanValue(value),-1);
                if(category>0) records.removeIf(media->media.getGroups().stream().noneMatch(group->group.getMediaGroupId()==category));
            } else if("editorFields.docDetails".equals(property)) {
                records.removeIf(media->{DocDetails doc=editor.owner(media.getMediaFkId(),false);return !matches(doc.getFullPath(),value);});
            } else {
                if(!List.of("id","mediaId","mediaTitleSk","mediaSortOrder","mediaLink","mediaThumbLink","fieldA","fieldB","fieldC","fieldD","fieldE","fieldF").contains(property)) throw new IllegalArgumentException("Unsupported media filter: "+property);
                records.removeIf(media->!matches(new BeanWrapperImpl(media).getPropertyValue(property),value));
            }
        }
        Comparator<Media> order=null;
        if(pageable!=null) for(var item:pageable.getSort()) {
            String property=item.getProperty();
            if(!List.of("id","mediaId","mediaTitleSk","mediaSortOrder","mediaLink","mediaThumbLink","fieldA","fieldB","fieldC","fieldD","fieldE","fieldF").contains(property)) throw new IllegalArgumentException("Unsupported media sorting: "+property);
            Comparator<Media> next=(first,second)->compare(new BeanWrapperImpl(first).getPropertyValue(property),new BeanWrapperImpl(second).getPropertyValue(property));
            if(item.isDescending())next=next.reversed();
            order=order==null?next:order.thenComparing(next);
        }
        if(order!=null) records.sort(order.thenComparing(Media::getMediaId));
        int total=records.size();
        if(pageable!=null&&pageable.isPaged()&&getRequest().getParameter("size")!=null) {
            int start=(int)Math.min(total,pageable.getOffset());
            records=new ArrayList<>(records.subList(start,Math.min(total,start+pageable.getPageSize())));
        }
        DatatablePageImpl<Media> result=new DatatablePageImpl<>(pageable==null||pageable.isUnpaged()?new PageImpl<>(records):new PageImpl<>(records,pageable,total));
        result.addOptions("editorFields.groups",BranchMedia.categories(folder),"mediaGroupName","mediaGroupId",false);
        processFromEntity(result,ProcessItemAction.GETALL);
        return result;
    }

    private static int compare(Object first,Object second) {
        if(first instanceof Number a&&second instanceof Number b) return Double.compare(a.doubleValue(),b.doubleValue());
        return Objects.toString(first,"").compareToIgnoreCase(Objects.toString(second,""));
    }

    private static boolean matches(Object actual,String pattern) {
        String value=Objects.toString(actual,"").toLowerCase(Locale.ROOT),query=getCleanValue(pattern).toLowerCase(Locale.ROOT);
        if("null".equals(pattern))return actual==null;
        if(pattern.startsWith("^")&&pattern.endsWith("$"))return value.equals(query);
        if(pattern.startsWith("^"))return value.startsWith(query);
        if(pattern.endsWith("$"))return value.endsWith(query);
        return value.contains(query);
    }

}
