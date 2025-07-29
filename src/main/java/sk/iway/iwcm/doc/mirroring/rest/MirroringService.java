package sk.iway.iwcm.doc.mirroring.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.beans.support.SortDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.mirroring.jpa.MirroringDTO;
import sk.iway.iwcm.doc.mirroring.jpa.MirroringEditorFields;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class MirroringService {

    public static final Character LAST_ALPHABET = 'H';

    private static final String TYPE_GROUPS = "groups";
    private static final String TYPE_DOCS = "docs";
    private static final String JOINING = "\n";

    private static final String PARAMETER_TYPE_ERR = "Request parameter 'type' have unknown value: ";
    private static final String PARAMETER_SYNCID_ERR = "Value of parameter syncId is not valid : ";

    private final List<String> supportedLngs = Arrays.asList( Constants.getArray("languages") );
    private static Set<String> foundLng = new HashSet<>();
    private Map<String, Character> lngFieldCombination = new HashMap<>();
    private List<String> errorMessages = new ArrayList<>();

    // Support values set by method setSupportValues
    private GroupsDB groupsDb;
    private TemplatesDB templatesDB;
    private DocDB docsDb;

    private String type;
    private String domainName;
    private boolean multiDomainEnabled;

    private Prop prop;

    /*********** PUBLIC methods **************/

    /**
     * Return page with MirroringDTO items. Items are paged and sorted. EditorFields are not set yet.
     * @param pageable
     * @param request
     * @return
     */
    public Page<MirroringDTO> getPage(Pageable pageable, HttpServletRequest request) {
        setSupportValues(request);

        List<MirroringDTO> data = new ArrayList<>();
        if(TYPE_GROUPS.equals(type)) {
           data = getGroupData();
        } else if(TYPE_DOCS.equals(type)) {
            data = getDocsData();
        } else {
            addParamWarning("getPage()", PARAMETER_TYPE_ERR, type);
            return new PageImpl<>( new ArrayList<>() );
        }

        return new PageImpl<>(listToHolter(data, pageable).getPageList(), pageable, data.size());
    }

    /**
     * Return list of status icon options for editorFields.statusIcons
     * @param prop
     * @return
     */
    public List<LabelValue> getStatusIconOptions (Prop prop) {
        List<LabelValue> icons = new ArrayList<>();

        icons.add(new LabelValue("<i class=\"ti ti-exclamation-circle\" style=\"color: #ff4b58;\"></i> " + prop.getText("components.mirroring.multiple_mapping_err"), "multipleErr:true"));
        icons.add(new LabelValue("<i class=\"ti ti-alert-triangle\" style=\"color: #fabd00;\"></i> " + prop.getText("components.mirroring.uneven_nesting_err"), "nestingWarn:true"));

        return icons;
    }

    /**
     * Return page with MirroringDTO items. Items are filtered by request parameters aka filtered by columns. They are paged and sorted.
     * @param pageable
     * @param request
     * @return
     */
    public Page<MirroringDTO> getFilteredPage(Pageable pageable, HttpServletRequest request) {
        setSupportValues(request);

        List<MirroringDTO> data;
        if(TYPE_GROUPS.equals(type)) {
           data = getGroupData();
        } else if(TYPE_DOCS.equals(type)) {
            data = getDocsData();
        } else {
            addParamWarning("getFilteredPage()", PARAMETER_TYPE_ERR, type);
            return new PageImpl<>( new ArrayList<>() );
        }

        //Filter by syncId
        if(Tools.isNotEmpty(request.getParameter("searchId"))) {
            int filterSyncId = Tools.getIntValue(request.getParameter("searchId"), -1);
            if(filterSyncId < 1) {
               //bad format or invalid value
               return new PageImpl<>( new ArrayList<>() );
            } else {
                List<MirroringDTO> tmpFiltered = data.stream()
                    .filter(item -> item.getId().intValue() == filterSyncId)
                    .toList();

                data = new ArrayList<>(tmpFiltered);
            }
        }

        String statucIconFilter = Tools.getStringValue(request.getParameter("searchEditorFields.statusIcons"), null);
        if(statucIconFilter != null) {
            List<MirroringDTO> tmpFiltered = new ArrayList<>();
            if("multipleErr:true".equals(statucIconFilter) == true) {
                //Only with err
                tmpFiltered = data.stream()
                    .filter(item -> Tools.isTrue(item.getMultipleErr()))
                    .toList();
            } else if("nestingWarn:true".equals(statucIconFilter)) {
                //Only with uneven nesting
                tmpFiltered = data.stream()
                    .filter(item -> Tools.isTrue(item.getNestingWarn()))
                    .toList();
            } else {
                Logger.warn(MirroringService.class, "Request parametert searchEditorFields.statusIcons have unknown value: " + statucIconFilter);
                return new PageImpl<>( new ArrayList<>() );
            }
            data = new ArrayList<>(tmpFiltered);
        }

        for(Character alphabet = 'A'; alphabet <= LAST_ALPHABET; alphabet++) {
            List<MirroringDTO> filtred = new ArrayList<>();
            String prefix = "searchField";
            String value = Tools.getStringValue(request.getParameter(prefix + alphabet), null);

            if(value != null) {
                if(value.startsWith("^") && value.endsWith("$")) {
                    //Equal mode
                    value = value.substring(1, value.length() - 1);
                    //Tricky thing. From FE values comes separated with " " but we separate with "\n" ... so replace it
                    value = value.replace(" ", "\n");
                    for(MirroringDTO entity : data)
                        if(entity.getField(alphabet, true).equalsIgnoreCase(value))
                            filtred.add(entity);
                } else if(value.startsWith("^")) {
                    // Starts with mode
                    value = value.substring(1, value.length()).toLowerCase();
                    for(MirroringDTO entity : data)
                        if(entity.getField(alphabet, true).startsWith(value))
                            filtred.add(entity);
                } else if(value.endsWith("$")) {
                    // End with mode
                    value = value.substring(0, value.length() - 1).toLowerCase();
                    for(MirroringDTO entity : data)
                        if(entity.getField(alphabet, true).endsWith(value))
                            filtred.add(entity);
                } else {
                    for(MirroringDTO entity : data)
                        if(entity.getField(alphabet, true).contains(value.toLowerCase()))
                            filtred.add(entity);
                }

                data = new ArrayList<>(filtred);
            }
        }

        return new PageImpl<>(listToHolter(data, pageable).getPageList(), pageable, data.size());
    }

    /**
     * Return MirroringDTO item by given syncId.
     * @param syncId
     * @param request
     * @return - Return null if syncId is not valid or item is not found.
     */
    public MirroringDTO getOneItem(int syncId, HttpServletRequest request) {
        setSupportValues(request);

        if(syncId < 1) {
            addParamWarning("getOneItem()", PARAMETER_SYNCID_ERR, syncId);
            return null;
        }

        if(TYPE_GROUPS.equals(type)) {
            return getGroup(syncId);
        } else if(TYPE_DOCS.equals(type)) {
            return getDoc(syncId);
        } else {
            addParamWarning("getOneItem()", PARAMETER_TYPE_ERR, type);
            return null;
        }
    }

    /**
     * Return List of fields specificly for TABLE view (not a editor view).
     * @return
     */
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>();

        if(foundLng.size() == 0) return fields;

        // As set fields as foudn different languages
        lngFieldCombination.forEach((lng, alphabet) -> {
            Field field = new Field();
            field.setKey(Character.toLowerCase(alphabet) + "");
            field.setLabel(lng);
            field.setType(FieldType.TEXTAREA.name().toLowerCase());
            fields.add(field);
        });

        // Hide unused fields
        Character startAlpahbet = (char) ('A' + foundLng.size());
        for(Character alphabet = startAlpahbet; alphabet <= LAST_ALPHABET; alphabet++) {
            fields.add(getNoneField(alphabet, null));
        }

        return fields;
    }

    /**
     * Delete syncId param for all GROUPS / DOCS witn given syncId. In case of GROUPS, remove syncId for all children groups and their docs.
     * @param syncId
     * @param request
     * @return - Return FALSE in case of error, TRUE otherwise.
     */
    public boolean deleteItem(int syncId, HttpServletRequest request) {
        setSupportValues(request);

        if(syncId < 1) {
            addParamWarning("deleteItem()", PARAMETER_SYNCID_ERR, syncId);
            return false;
        }

        if(TYPE_GROUPS.equals(type)) {
            removeGroupSync(syncId);
        } else if(TYPE_DOCS.equals(type)) {
            removeDocSync(syncId);
        } else {
            addParamWarning("deleteItem()", PARAMETER_TYPE_ERR, type);
            return false;
        }

        return true;
    }

    /**
     * Handle editItem request. Remove/add sync ids for groups/docs.
     * @param savedEntity
     * @param request
     * @return - Return newly loaded MirroringDTO item after change OR empty MirroringDTO if all connections were removed (like delete) OR null if error occurs
     */
    public MirroringDTO editItem(MirroringDTO savedEntity, HttpServletRequest request) {
        setSupportValues(request);

        int syncId = savedEntity.getId().intValue();
        if(syncId < 1) {
            addParamWarning("editItem()", PARAMETER_SYNCID_ERR, syncId);
            return null;
        }

        //Ids of selected groups/docs
        Set<Integer> newSelected = savedEntity.getSelectedAsSet();
        Set<Integer> oldSelected = new HashSet<>();

        MirroringDTO oldEntity;
        if(TYPE_GROUPS.equals(type)) {
            oldEntity = getGroup(syncId);
        } else if(TYPE_DOCS.equals(type)) {
            oldEntity = getDoc(syncId);
        } else {
            addParamWarning("editItem()", PARAMETER_TYPE_ERR, type);
            return null;
        }

        if(oldEntity == null) {
            Logger.warn(MirroringService.class, "Failed to obtain MirroringDTO using syncId: " + syncId);
            return null;
        }

        oldSelected = oldEntity.getSelectedAsSet();

        List<Integer> deleted = new ArrayList<>(oldSelected);
        deleted.removeAll(newSelected);

        List<Integer> addded = new ArrayList<>(newSelected);
        addded.removeAll(oldSelected);

        if(TYPE_GROUPS.equals(type)) {
            //
            removeGroupsFromSync(deleted);

            // Before we add groups to sync, remove groups/doc sync for whole tree (safety)
            // reason - add will set only parent group syncId, not children groups/docs, so if groups were synced before, their syncId need to be removed
            removeGroupsFromSync(addded);
            addGroupToSync(syncId, addded);

            //After changes refresh group and doc table
            groupsDb = GroupsDB.getInstance(true);
            docsDb = DocDB.getInstance(true);

            //When newSelected.size() < 1, we removed all reference and its like delete ... so there is nothing to return
            return newSelected.size() < 1 ? new MirroringDTO() : getGroup(syncId);
        }
        else if(TYPE_DOCS.equals(type)) {
            //
            removeDocsFromSync(deleted);

            //
            addDocsToSync(syncId, addded);

            //After changes refresh doc table
            docsDb = DocDB.getInstance(true);

            //When newSelected.size() < 1, we removed all reference and its like delete ... so there is nothing to return
            return newSelected.size() < 1 ? new MirroringDTO() : getDoc(syncId);
        }

        return null;
    }

    /**
     * Validate MirroringDTO entity for basic error checking. In case, found error is set into Errors object.
     * @param entity
     * @param errors
     * @param request
     */
    public void validateEditor(MirroringDTO entity, Errors errors, HttpServletRequest request) {
        setSupportValues(request);

        int syncId = entity.getId().intValue();
        if(syncId < 1) {
            addParamWarning("validateEditor()", PARAMETER_SYNCID_ERR, syncId);
            return;
        }

        // Check duplicate
        if(entity.hasSelectedDuplicates() == true) {
            errors.reject("", prop.getText("components.mirroring.duplicate.err"));
            return;
        }

        List<Integer> selected = entity.getSelectedAsList(true);
        if(selected.size() < 1) return;

        if(TYPE_GROUPS.equals(type)) {
            // Unskipable check !!
            selected = filterGroupWithNotif(syncId, selected);
            if(foundError()) {
                errors.reject("", getErrorMessagesString());
                return;
            }

            validateGroupEditor(entity, errors, selected);
        } else if(TYPE_DOCS.equals(type)) {
            // Unskipable check
            selected = filterDocWithNotif(syncId, selected);
            if(foundError()) {
                errors.reject("", getErrorMessagesString());
                return;
            }

            validateDocEditor(entity, errors, selected);
        } else {
            addParamWarning("validateEditor()", PARAMETER_TYPE_ERR, type);
            errors.reject("", prop.getText("datatable.error.unknown"));
        }
    }

    /*********** PRIVATE DOC methods **************/

    /**
     * Retun list of data for Mirroring-DOC table. Use docsDb.getBasicDocDetailsAll() to obtain all docs.
     * Only docs with syncId > 0 are returned and suitable domain if needed.
     * @return
     */
    private List<MirroringDTO> getDocsData() {

        List<DocDetails> docs = docsDb.getBasicDocDetailsAll();

        String sql = "SELECT doc_id, sync_id FROM documents WHERE sync_id IS NOT NULL AND sync_id > 0";

        Map<Integer, Integer> docSyncMap = new HashMap<>();

        new ComplexQuery().setSql(sql).list(new Mapper<Object>() {
            @Override
			public Object map(ResultSet rs) throws SQLException {
                docSyncMap.put(
                    rs.getInt("doc_id"),
                    rs.getInt("sync_id")
                );
                return null;
            }
        });

        return getDocsData(docs, docSyncMap);
    }

    /**
     * Return list of data for Mirroring-DOC table. Use set parameter DOCS as source of data.
     * Only docs with syncId > 0 are returned and suitable domain if needed.
     * @param docs - list of DocDetails
     * @param docSyncMap - map of <docId to syncId> combinations
     * @return
     */
    private List<MirroringDTO> getDocsData(List<DocDetails> docs, Map<Integer, Integer> docSyncMap) {
        Map<Integer, Map<String, String>> dataMap = new HashMap<>();

        for(DocDetails doc : docs) {
            Integer syncId = docSyncMap.get(doc.getDocId());
            if(syncId == null || syncId < 1) continue;

            GroupDetails group = groupsDb.getGroup(doc.getGroupId());

            if(group == null) {
                Logger.warn(MirroringService.class, "Doc's: " + doc.getDocId() + " parent group: " + doc.getGroupId() + " NOT FOUND");
                continue;
            }

            if(isDomainSafe(group.getDomainName())) {

                String lng = getGroupLng(group);
                if(lng == null) continue;

                //Add found lang
                foundLng.add(lng);

                //syncMap represents synced docs in different languages
                Map<String, String> syncLngMap = dataMap.get(syncId);

                if(syncLngMap == null) {
                    //Mapo for this sync_id do not exist yet
                    syncLngMap = new HashMap<>();
                    syncLngMap.put(lng, doc.getFullPath());
                } else {
                    //Does lng exist ?
                    if(syncLngMap.containsKey(lng) == true) {
                        //Apply sorting
                       syncLngMap.put(lng, addValueAndSort(syncLngMap.get(lng), doc.getFullPath()) );
                    } else {
                        syncLngMap.put(lng, doc.getFullPath());
                    }
                }

                dataMap.put(syncId, syncLngMap);
            }
        }

        //
        prepareLngFieldCombination();

        return mapToItems(dataMap);
    }

    /**
     * Return single MirroringDTO item for given syncId.
     * For Mirroring-DOC table version.
     * @param syncId
     * @return
     */
    private MirroringDTO getDoc(int syncId) {
        List<DocDetails> docs = new ArrayList<>();
        Map<Integer, Integer> docSyncMap = new HashMap<>();

        List<Integer> docIds = new SimpleQuery().forListInteger("SELECT doc_id FROM documents WHERE sync_id = ?", syncId);
        for(Integer docId : docIds) {
            DocDetails doc = docsDb.getDoc(docId);
            docs.add(doc);
            docSyncMap.put(docId, syncId);
        }

        //Reorder
        List<DocDetails> reOrder = new ArrayList<>();
        for(String lng : supportedLngs) {
            List<DocDetails> lngGroups = new ArrayList<>();
            for(DocDetails doc : docs) {
                if(lng.equals( getGroupLng(doc.getGroupId()) ))
                    lngGroups.add(doc);
            }
            reOrder.addAll( lngGroups.stream().sorted(Comparator.comparing(DocDetails::getDocId)).toList() );
        }
        docs = reOrder;

        MirroringDTO entity = getDocsData(docs, docSyncMap).get(0);

        Character alphabet = 'A';
        List<Field> fields = new ArrayList<>();
        for(DocDetails doc : docs) {
            //Ste field value for editor
            entity.setSelectField(alphabet, doc.getDocId(), doc.getFullPath());
            fields.add(getJsonDocField(alphabet, getGroupLng(doc.getGroupId()), doc.getDocId() + ""));
            alphabet++;
        }

        for(Character restAlphabet = alphabet; restAlphabet <= LAST_ALPHABET; restAlphabet++) {
            fields.add(getJsonDocField(restAlphabet, "", ""));
        }

        MirroringEditorFields mef = new MirroringEditorFields();
        mef.from(entity);
        mef.setFieldsDefinition( fields );
        entity.setEditorFields(mef);

        return entity;
    }

    /**
     * Validate DOC from editor for specific errors. If error is found, it is set into Errors object.
     * @param entity
     * @param errors
     * @param docIds
     */
    private void validateDocEditor(MirroringDTO entity, Errors errors, List<Integer> docIds) {
        //
        if(Tools.isTrue(entity.getIgnoreProblems())) return;

        Set<String> seenLng = new HashSet<>();
        int baseDepth = -1;

        for(Integer docId : docIds) {
            DocDetails doc = docsDb.getDoc(docId);
            if(doc == null) continue;


            GroupDetails group = groupsDb.getGroup(doc.getGroupId());
            if(group != null) {
                String groupLng = getGroupLng(group);
                if(seenLng.add(groupLng) == false) {
                    //Found lng err
                    errors.reject("", prop.getText("components.mirroring.multiple_mapping_err_msg", groupLng));
                    return;
                }
            }

            int depth = (int) doc.getFullPath().chars().filter(ch -> ch == '/').count();
            if(baseDepth == -1) baseDepth = depth;
            else if(baseDepth != depth) {
                //Found nested waring
                errors.reject("", prop.getText("components.mirroring.uneven_nesting_err_msg"));
                return;
            }
        }
    }

    /**
     * Remove given syncId from all documents.
     * @param syncId
     */
    private void removeDocSync(int syncId) {
        if(syncId < 1) return;

        new SimpleQuery().execute("UPDATE documents SET sync_id = 0 WHERE sync_id = ?", syncId);
        docsDb = DocDB.getInstance(true);
    }

    /**
     * For given list of docIds, remove their syncId.
     * @param docIds
     */
    private void removeDocsFromSync(List<Integer> docIds) {
        String stringIds = listIdsToString(docIds);
        if(stringIds == null) return;

        new SimpleQuery().execute("UPDATE documents SET sync_id = 0 WHERE doc_id IN (" + stringIds + ")");
    }

    /**
     * Return docIds WHERE this docs do not have set syncId or have syncId same as given syncId.
     * If not suitable doc is found, add this information into errorMessages list.
     * @param syncId
     * @param docIds - source of docIds
     * @return
     */
    private List<Integer> filterDocWithNotif(int syncId, List<Integer> docIds) {
        if(docIds != null) {
            List<Integer> filtered = new ArrayList<>();
            for(Integer docId : docIds) {
                DocDetails doc = docsDb.getDoc(docId);
                if(doc != null && doc.getSyncId() > 0 && doc.getSyncId() != syncId) {
                    //This doc is allready synced VIA different sync id - ignore doc and add notification
                    errorMessages.add( prop.getText("components.mirroring.doc_sync_blocked_desc", doc.getFullPath(), doc.getSyncId() + "") );
                } else filtered.add(docId);
            }
            return new ArrayList<>( filtered );
        }
        return null;
    }

    /**
     * For given docIds add syncId to them. Use filterDocWithNotif to filter only the valid ones.
     * @param syncId
     * @param docIds
     */
    private void addDocsToSync(int syncId, List<Integer> docIds) {
        //
        docIds = filterDocWithNotif(syncId, docIds);

        String stringIds = listIdsToString(docIds);
        if(syncId < 1 || stringIds == null) return;

        new SimpleQuery().execute("UPDATE documents SET sync_id = ? WHERE doc_id IN (" + stringIds + ")", syncId);
    }

    /*********** PRIVATE GROUPS methods **************/

    /**
     * Return list of data for Mirroring-GROUPS table. Use groupsDb.getGroupsAll() to obtain all groups.
     * Only groups with syncId > 0 are returned and suitable domain if needed.
     * @return
     */
    private List<MirroringDTO> getGroupData () {
        return getGroupData(groupsDb.getGroupsAll());
    }

    /**
     * Return list of data for Mirroring-GROUPS table. Use set parameter GROUPS as source of data.
     * Only groups with syncId > 0 are returned and suitable domain if needed.
     * @param groups
     * @return
     */
    private List<MirroringDTO> getGroupData (List<GroupDetails> groups) {
        Map<Integer, Map<String, String>> dataMap = new HashMap<>();

        for(GroupDetails group : groups) {

            Integer syncId = group.getSyncId();
            if(syncId == null || syncId < 1) continue;

            //Filter wanted groups
            if(isDomainSafe(group.getDomainName())) {

                String lng = getGroupLng(group);
                if(lng == null) continue;

                //Add found lang
                foundLng.add(lng);

                //syncMap represents synced groups in different languages
                Map<String, String> syncLngMap = dataMap.get(syncId);

                if(syncLngMap == null) {
                    //Mapo for this sync_id do not exist yet
                    syncLngMap = new HashMap<>();
                    syncLngMap.put(lng, group.getFullPath());
                } else {
                    //Does lng exist ?
                    if(syncLngMap.containsKey(lng) == true) {
                        //Apply join with sorting
                        syncLngMap.put(lng, addValueAndSort(syncLngMap.get(lng), group.getFullPath()) );
                    } else {
                        syncLngMap.put(lng, group.getFullPath());
                    }
                }

                dataMap.put(syncId, syncLngMap);
            }
        }

        //
        prepareLngFieldCombination();

        return mapToItems(dataMap);
    }

    /**
     * Return single MirroringDTO item for given syncId.
     * For Mirroring-GROUPS table version.
     * @param syncId
     * @return - Return null if syncId is not valid or item is not found.
     */
    private MirroringDTO getGroup(int syncId) {
        List<Integer> groupIds = new ArrayList<>();
        if(multiDomainEnabled == true) {
            groupIds = new SimpleQuery().forListInteger("SELECT group_id FROM groups WHERE sync_id = ? AND domain_name = ?", syncId, domainName);
        } else {
            groupIds = new SimpleQuery().forListInteger("SELECT group_id FROM groups WHERE sync_id = ?", syncId);
        }

        if(groupIds == null || groupIds.size() < 1) {
            //PRUSER
            return null;
        }

        List<GroupDetails> groups = new ArrayList<>();
        for(Integer groupId : groupIds) {
            groups.add( groupsDb.getGroup(groupId) );
        }

        //Re-order them in LNG order way
        List<GroupDetails> reOrder = new ArrayList<>();
        for(String lng : supportedLngs) {
            List<GroupDetails> lngGroups = new ArrayList<>();
            for(GroupDetails group : groups) {
                if(lng.equals( getGroupLng(group) )) {
                    lngGroups.add(group);
                }
            }
            reOrder.addAll( lngGroups.stream().sorted(Comparator.comparing(GroupDetails::getGroupId)).toList() );
        }
        groups = reOrder;

        //Entity must be prepare same way as for datatable
        MirroringDTO entity = getGroupData(groups).get(0);

        Character alphabet = 'A';
        List<Field> fields = new ArrayList<>();
        for(GroupDetails group : groups) {
            //Ste field value for editor
            entity.setSelectField(alphabet, group.getGroupId(), group.getFullPath());
            fields.add(getJsonGroupField(alphabet, getGroupLng(group), group.getGroupId() + ""));
            alphabet++;
        }

        for(Character restAlphabet = alphabet; restAlphabet <= LAST_ALPHABET; restAlphabet++) {
            fields.add(getJsonGroupField(restAlphabet, "", ""));
        }

        MirroringEditorFields mef = new MirroringEditorFields();
        mef.from(entity);
        mef.setFieldsDefinition( fields );
        entity.setEditorFields(mef);

        return entity;
    }

    /**
     * Validate GROUP from editor for specific errors. If error is found, it is set into Errors object.
     * @param entity
     * @param errors
     * @param groupIds
     */
    private void validateGroupEditor(MirroringDTO entity, Errors errors, List<Integer> groupIds) {
        //
        if(Tools.isTrue(entity.getIgnoreProblems())) return;

        Set<String> seenLng = new HashSet<>();
        int baseDepth = -1;

        for(Integer groupId : groupIds) {
            GroupDetails group = groupsDb.getGroup(groupId);
            if(group == null) continue;

            String groupLng = getGroupLng(group);
            if(seenLng.add(groupLng) == false) {
                //Found lng err
                errors.reject("", prop.getText("components.mirroring.multiple_mapping_err_msg", groupLng));
                return;
            }

            int depth = (int) group.getFullPath().chars().filter(ch -> ch == '/').count();
            if(baseDepth == -1) baseDepth = depth;
            else if(baseDepth != depth) {
                //Found nested waring
                errors.reject("", prop.getText("components.mirroring.uneven_nesting_err_msg"));
                return;
            }
        }
    }

    /**
     * Remove given syncId from all groups.
     * This will remove ALLSO syncId for all children groups and their docs.
     * @param syncId
     */
    private void removeGroupSync(int syncId) {
        if(syncId < 1) return;

        List<Integer> groupIds = new ArrayList<>();
        for(GroupDetails group : groupsDb.getGroupsAll()) {
            if(isDomainSafe(group.getDomainName()) && group.getSyncId() == syncId)
                groupIds.add(group.getGroupId());
        }

        removeGroupsFromSync(groupIds);

        //
        groupsDb = GroupsDB.getInstance(true);
        docsDb = DocDB.getInstance(true);
    }

    /**
     * Remove syncId for all groups and their docs from given list of groupIds.
     * This will remove ALLSO syncId for all children groups and their docs.
     * @param groupIds
     */
    private void removeGroupsFromSync(List<Integer> groupIds) {
        String stringIds = listIdsToString(groupIds);
        if(stringIds == null) return;

        List<Integer> groupsTreeIds = new ArrayList<>();
        for(GroupDetails group : groupsDb.getGroupsTree(stringIds)) {
            groupsTreeIds.add(group.getGroupId());
        }

        String stringGroupTreeIds = listIdsToString(groupsTreeIds);
        if(stringGroupTreeIds == null) return;

        //Remove sync for groups
        new SimpleQuery().execute("UPDATE groups SET sync_id = 0 WHERE group_id IN (" + stringGroupTreeIds + ")");

        //Remove sync for docs
        new SimpleQuery().execute("UPDATE documents SET sync_id = 0 WHERE group_id IN (" + stringGroupTreeIds + ")");
    }

    /**
     * Return groupIds WHERE this group do not have set syncId or have syncId same as given syncId.
     * If not suitable group is found, add this information into errorMessages list.
     * @param syncId
     * @param groupIds
     * @return
     */
    private List<Integer> filterGroupWithNotif(int syncId, List<Integer> groupIds) {
        if(groupIds != null) {
            List<Integer> filtered = new ArrayList<>();
            for(Integer groupId : groupIds) {
                GroupDetails group = groupsDb.getGroup(groupId);
                if(group != null && group.getSyncId() > 0 && group.getSyncId() != syncId) {
                    //This groups is allready synced VIA different sync id - ignore group and add notification
                    errorMessages.add( prop.getText("components.mirroring.group_sync_blocked_desc", group.getFullPath(), group.getSyncId() + "") );
                } else filtered.add(groupId);
            }
            return new ArrayList<>( filtered );
        }
        return null;
    }

    /**
     * For given groupIds add syncId to them. Use filterGroupWithNotif to filter only the valid ones.
     * @param syncId
     * @param groupIds
     */
    private void addGroupToSync(int syncId, List<Integer> groupIds) {
        //Filter synced group - just in case
        groupIds = filterGroupWithNotif(syncId, groupIds);

        String stringIds = listIdsToString(groupIds);
        if(syncId < 1 || stringIds == null) return;

        new SimpleQuery().execute("UPDATE groups SET sync_id = ? WHERE group_id IN (" + stringIds + ")", syncId);
    }

    /*********** PRIVATE SUPPRORT methods **************/

    /**
     * return PagedListHolder that is used for pagination and sorting of MirroringDTO items.
     * @param data
     * @param pageable
     * @return
     */
    private PagedListHolder<MirroringDTO> listToHolter(List<MirroringDTO> data ,Pageable pageable) {
        PagedListHolder<MirroringDTO> listHolder = new PagedListHolder<>(data);

        SortDefinition sortDefinition;
        Sort sort = pageable.getSort();
        if(sort.isEmpty() == false) {
            Sort.Order order = sort.iterator().next();
            sortDefinition = new MutableSortDefinition(order.getProperty(), true, order.isAscending());
        } else {
            //Default value
            sortDefinition = new MutableSortDefinition("id", true, true);
        }

        listHolder.setSort(sortDefinition);
        listHolder.resort();

        //
        listHolder.setPage(pageable.getPageNumber());
        listHolder.setPageSize(pageable.getPageSize());

        return listHolder;
    }

    /**
     * Get group by given groupId and return its language.
     * @param groupId
     * @return
     */
    private String getGroupLng(int groupId) {
        return getGroupLng( groupsDb.getGroup(groupId) );
    }

    /**
     * Get group language by given GroupDetails object.
     * If group does not have lng set, try return lng from group template.
     * @param group
     * @return
     */
    private String getGroupLng(GroupDetails group) {
        String lng = group.getLng();
        if(Tools.isEmpty(lng) == true) {
            if(templatesDB.getTemplate(group.getTempId()) != null)
                lng = templatesDB.getTemplate(group.getTempId()).getLng();
        }

        if(Tools.isEmpty(lng) == true) {
            Logger.warn(MirroringService.class, "Language for group: " + group.getGroupId() + " was not found.");
            return null;
        }

        return lng;
    }

    /**
     * Prepare lngFieldCombination map that contains all supported languages and their alphabetic representation.
     * This is used to set fields in MirroringDTO.
     */
    private void prepareLngFieldCombination() {
        char alphabet = 'A';
        for(String lng : supportedLngs) {
            if(foundLng.contains(lng)) {
                lngFieldCombination.put(lng, alphabet);
                alphabet++;
            }
        }
    }

    /**
     * Get given map of data and return list of prepared MirroringDTO items.
     * Structure of dataMap is:
     *  <syncId, <language, fullPath>>
     *
     * @param dataMap
     * @return
     */
    private List<MirroringDTO> mapToItems(Map<Integer, Map<String, String>> dataMap) {
        List<MirroringDTO> items = new ArrayList<>();

        dataMap.forEach((syncId, paths) -> {
            MirroringDTO item = new MirroringDTO();
            item.setId(syncId.longValue());

            Set<Integer> nestedLevels = new HashSet<>();
            if(paths != null) {
                paths.forEach((lng, path) -> {
                    item.setField(lngFieldCombination.get(lng), path);
                    //There shoul not be 2 folders from same language that are synced
                    if(path.contains(JOINING))
                        item.setMultipleErr(Boolean.TRUE);

                    String[] joinedPaths = path.split(JOINING);
                    for(String joinedPath : joinedPaths)
                        nestedLevels.add((int) joinedPath.chars().filter(ch -> ch == '/').count() );
                });
            }

            if(nestedLevels.size() > 1)
                item.setNestingWarn(Boolean.TRUE);

            items.add(item);
        });

        return items;
    }

    /**
     * Create a NONE field for given alphabet and prefix.
     * @param alphabet
     * @param prefix
     * @return
     */
    private Field getNoneField(Character alphabet, String prefix) {
        Field field = new Field();
        field.setKey((alphabet + "").toLowerCase());
        field.setType(FieldType.NONE.name().toLowerCase());
        if(Tools.isNotEmpty(prefix)) field.setCustomPrefix(prefix);
        return field;
    }

    /**
     * Create a JSON_GROUP field for given alphabet, label and value.
     * @param alphabet
     * @param label
     * @param value
     * @return
     */
    private Field getJsonGroupField(Character alphabet, String label, String value) {
        Field field = new Field();
        field.setCustomPrefix("selector");
        field.setKey((alphabet + "").toLowerCase());
        field.setLabel(label);
        field.setValue(value);
        field.setType(FieldType.JSON_GROUP.name().toLowerCase());
        field.setClassName("dt-tree-groupid-null");
        return field;
    }

    /**
     * Create a JSON_DOC field for given alphabet, label and value.
     * @param alphabet
     * @param label
     * @param value
     * @return
     */
    private Field getJsonDocField(Character alphabet, String label, String value) {
        Field field = new Field();
        field.setCustomPrefix("selector");
        field.setKey((alphabet + "").toLowerCase());
        field.setLabel(label);
        field.setValue(value);
        field.setType(FieldType.JSON_DOC.name().toLowerCase());
        field.setClassName("dt-tree-pageid-null");
        return field;
    }

    /**
     * Convert list of ids to string representation. As "Id1,Id2,Id3,..." format.
     * If list is null or empty, return null.
     * @param ids
     * @return
     */
    private String listIdsToString(List<Integer> ids) {
        if(ids == null || ids.isEmpty()) return null;

        return ids.stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(","));
    }

    /**
     * Set support values for this service.
     * NEED to be called at start of each method that uses this service.
     * @param request
     */
    private void setSupportValues(HttpServletRequest request) {
        this.groupsDb = GroupsDB.getInstance();
        this.templatesDB = TemplatesDB.getInstance();
        this.docsDb = DocDB.getInstance();

        this.domainName = CloudToolsForCore.getDomainName();
        this.type = Tools.getStringValue(request.getParameter("type"), "");
        this.multiDomainEnabled = Constants.getBoolean("multiDomainEnabled");

        this.prop = Prop.getInstance(request);
    }

    /**
     * Check if given domainName is same as current domainName.
     * If multiDomainEnabled is false, return ALLWAYS true.
     * @param domainName
     * @return
     */
    private boolean isDomainSafe(String domainName) {
        if(multiDomainEnabled == false) return true;
        if(this.domainName.equals(domainName)) return true;
        return false;
    }

    /**
     * Log warning message for given method name, error description and value.
     * @param methodName
     * @param error
     * @param value
     */
    private void addParamWarning(String methodName, String error, Object value) {
        Logger.warn(MirroringService.class, methodName + " -> " + error + value);
    }

    /**
     * Join oldValue and valueToAdd with JOINING string, sort them and return as a single string.
     * @param oldValue - string of values separated by JOINING
     * @param valueToAdd - one value
     * @return - sorted string of values separated by JOINING as string "value1,value2,value3,..."
     */
    private String addValueAndSort(String oldValue, String valueToAdd) {
        if(oldValue == null) oldValue = "";
        if(valueToAdd == null) valueToAdd = "";

        String[] arr = oldValue.split(JOINING);
        List<String> arrToList = new ArrayList<>( Arrays.asList(arr) );
        arrToList.add(valueToAdd);
        Collections.sort(arrToList);
        return arrToList.stream().collect(Collectors.joining(JOINING));
    }

    /**
     * Return string representation of all error messages joined by new line.
     * After this call, errorMessages list is cleared.
     * @return
     */
    private String getErrorMessagesString() {
        StringBuilder sb = new StringBuilder("");
        for(String errMsg : errorMessages) {
            sb.append(errMsg).append("\n");
        }
        //
        errorMessages = new ArrayList<>();
        return sb.toString();
    }

    /**
     * Check if errorMessages list contains any error messages.
     * If list is null or empty, return false.
     * @return
     */
    private boolean foundError() {
        if(errorMessages == null || errorMessages.size() < 1) return false;
        return true;
    }
}