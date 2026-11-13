package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UsersDB;

/** Authorized editing of the effective tree. No production save workflow or events are invoked. */
public final class BranchEditor {
    private final BranchStore store = new BranchStore();
    private final HttpServletRequest request;
    private final Identity user;
    private final BranchContext context;

    public BranchEditor(HttpServletRequest request) {
        this.request = request;
        user = new BranchAccess().requireUser(request);
        context = BranchContexts.current(request);
        if (context.mode()!=BranchContext.Mode.ADMIN || !context.isBranch() || context.userId()!=user.getUserId())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"An ADMIN branch is required");
    }

    public DocDetails loadDocument(int id, int historyId, int groupId, EditorService editor) {
        if (historyId>0) throw invalid("History is read-only in a publishing branch");
        DocDetails doc;
        if (id<1) {
            GroupDetails group = folder(groupId,true);
            if (user.isDisabledItem("addPage")) throw forbidden();
            doc = editor.prepareNewDocForEditor(group.getNewPageDocIdTemplate()>0 ? -group.getNewPageDocIdTemplate() : -1,group);
            doc.setGenerateUrlFromTitle(true);
            doc.setPublishStartDate(Constants.getBoolean("editorAutoFillPublishStart") ? new Date() : null);
            doc.setPublishEndDate(null);
            doc.setPublishAfterStart(false); doc.setDisableAfterEnd(false);
            doc.getEditorFields().setPublishAfterStart(false); doc.getEditorFields().setDisableAfterEnd(false);
        } else {
            doc = BranchView.current().document(id,false);
            if (doc==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (!EditorService.hasPagePermission(user,doc)) throw forbidden();
            WebpagesService.processFromEntity(doc,ProcessItemAction.GETONE,request,true);
            String reason=null;
            if(systemFolder(BranchView.current().group(doc.getGroupId()))) reason=sk.iway.iwcm.i18n.Prop.getInstance(request).getText("branches.system.readonly");
            else if(MultigroupMappingDB.getMasterDocId(id)>0 || !MultigroupMappingDB.getSlaveDocIds(id).isEmpty()) reason=sk.iway.iwcm.i18n.Prop.getInstance(request).getText("branches.multigroup.readonly");
            doc.getEditorFields().setBranchReadOnlyReason(reason);
        }
        attachHash(doc);
        return doc;
    }

    public static void attachHash(DocDetails doc) {
        if (doc.getEditorFields()==null || !BranchContexts.current().isBranch()) return;
        if (doc.getDocId()<1) doc.getEditorFields().setBranchHash("new");
        else if (doc.getEditorFields().getBranchHash()==null) throw new IllegalStateException("Missing displayed page version");
        // Legacy copies/notes/attributes have independent production persistence.
        doc.getEditorFields().setGroupCopyDetails(null);
        doc.getEditorFields().setRedactorNote("");
        doc.getEditorFields().setAttrs(null);
    }

    public static void attachHash(GroupDetails group) {
        if (group!=null && group.getEditorFields()!=null && BranchContexts.current().isBranch())
            group.getEditorFields().setBranchHash(group.getGroupId()>0 ? BranchView.current().groupHash(group.getGroupId()) : "new");
    }

    private static String currentHash(Connection connection, BranchType type, int id) throws SQLException {
        Map<String,Object> work = BranchStore.readWork(connection,BranchContexts.current().branchId(),type,id).orElse(null);
        return work==null ? BranchStore.snapshot(type,BranchStore.readLive(connection,type,id).orElseThrow()).hash()
            : BranchStore.change(type,work).work().hash();
    }

    public DocDetails saveDocument(DocDetails doc, EditorService editor) {
        if (doc.getEditorFields()==null) throw invalid("Missing editor fields");
        new BranchAccess().requireCsrf(request);
        String expected=doc.getEditorFields().getBranchHash();
        DocDetails result=write(connection -> {
            BranchView before=BranchView.reload(connection);
            DocDetails original=doc.getDocId()>0 ? before.document(doc.getDocId(),false) : null;
            if (doc.getDocId()>0 && original==null) throw invalid("Page no longer exists");
            rejectUnsupported(doc,original);
            doc.getEditorFields().toDocDetails(doc);
            GroupDetails destination=folder(doc.getGroupId(),false);
            if(doc.getSortPriority()<0) editor.setSortPriority(doc,destination);
            editor.checkSavePermissions(user,doc);
            if (original==null || original.getGroupId()!=doc.getGroupId()) folder(doc.getGroupId(),true);
            if (original!=null) {
                folder(original.getGroupId(),false);
                multigroup(original.getDocId());
            }
            if (original!=null && original.getGroupId()!=doc.getGroupId() && before.group(original.getGroupId()).getDefaultDocId()==doc.getDocId())
                replaceDefault(connection,before,original.getGroupId(),doc.getDocId());
            validateDocument(doc);
            editor.normalizeEditableContent(doc);
            doc.setDateCreated(System.currentTimeMillis()); doc.setAuthorId(user.getUserId());
            Map<String,Object> stable=original==null ? new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,user.getUserId(),Instant.now(),Constants.getBoolean("editorAutoFillPublishStart"))) : row(connection,BranchType.DOCUMENT,doc.getDocId());
            Map<String,Object> values=BranchEditorRows.document(doc,stable);
            Map<String,Object> saved=doc.getDocId()<1 ? store.create(connection,context,BranchType.DOCUMENT,values)
                : store.put(connection,context,BranchType.DOCUMENT,doc.getDocId(),values,expected,false);
            int id=((Number)saved.get("doc_id")).intValue();
            BranchView after=BranchView.reload(connection);
            GroupDetails group=after.group(doc.getGroupId());
            if ((original==null || original.getGroupId()!=doc.getGroupId()) && group.getDefaultDocId()<1) {
                Map<String,Object> groupRow=row(connection,BranchType.GROUP,group.getGroupId());
                groupRow.put("default_doc_id",id);
                store.put(connection,context,BranchType.GROUP,group.getGroupId(),groupRow,currentHash(connection,BranchType.GROUP,group.getGroupId()),false);
            }
            validateResult(before,BranchView.reload(connection));
            return BranchView.current().document(id,false);
        });
        WebpagesService.processFromEntity(result,ProcessItemAction.GETONE,request,true);
        attachHash(result);
        return result;
    }

    public GroupDetails saveGroup(GroupDetails group) {
        new BranchAccess().requireCsrf(request);
        if (group.getEditorFields()==null) throw invalid("Missing editor fields");
        String expected=group.getEditorFields().getBranchHash();
        GroupDetails result=write(connection -> {
            BranchView before=BranchView.reload(connection);
            GroupDetails old=group.getGroupId()>0 ? folder(group.getGroupId(),true) : null;
            rejectUnsupported(group,old);
            group.getEditorFields().toGroupDetails(group);
            if (user.isDisabledItem(old==null ? "addSubdir" : "editDir")) throw forbidden();
            if (group.getParentGroupId()>0) folder(group.getParentGroupId(),true);
            else if (Tools.isNotEmpty(user.getEditableGroups())) throw forbidden();
            if (old!=null && group.getParentGroupId()==0 && !context.domainName().equals(old.getDomainName()))
                throw invalid("Moving this inherited-domain folder to the root would change its domain");
            for (GroupDetails parent=before.group(group.getParentGroupId());parent!=null;parent=before.group(parent.getParentGroupId()))
                if (parent.getGroupId()==group.getGroupId()) throw invalid("A folder cannot be moved below itself");
            if (Tools.isEmpty(group.getGroupName()) || group.getGroupName().contains("/") || group.getGroupName().contains("\\")) throw invalid("Invalid folder name");
            if ("system".equalsIgnoreCase(group.getGroupName()) || group.isInternal()) throw invalid("System folders are read-only");
            if (group.getSortPriority()<0) group.setSortPriority(GroupsDB.getInstance().getDefaultSortPriority(group.getParentGroupId()));
            String directory=group.getUrlDirName();
            if (directory.contains("..") || directory.contains("\\") || directory.contains(":") || directory.contains("?") || directory.contains("#") || directory.startsWith("//")) throw invalid("Invalid folder URL");
            if (group.getLinkGroupId()==group.getGroupId() && group.getGroupId()>0) throw invalid("A folder cannot link to itself");
            if (group.getTempId()>0 && TemplatesDB.getInstance().getTemplate(group.getTempId())==null) throw invalid("Unknown template");
            if (group.getDefaultDocId()>0) {
                DocDetails doc=before.document(group.getDefaultDocId(),true);
                if (doc==null || doc.getGroupId()!=group.getGroupId()) throw invalid("The default page must belong to this folder");
            }
            reference(group.getLogonPageDocId()); reference(group.getNewPageDocIdTemplate());
            if (group.getLinkGroupId()>0) folder(group.getLinkGroupId(),false);
            Map<String,Object> stable=old==null ? BranchGroupMapper.initialValues(1,context.domainName()) : row(connection,BranchType.GROUP,group.getGroupId());
            Map<String,Object> values=BranchEditorRows.group(group,stable);
            Map<String,Object> saved=old==null ? store.create(connection,context,BranchType.GROUP,values)
                : store.put(connection,context,BranchType.GROUP,group.getGroupId(),values,expected,false);
            int id=((Number)saved.get("group_id")).intValue();
            if (old!=null && (old.getParentGroupId()!=group.getParentGroupId() || !old.getUrlDirName().equals(group.getUrlDirName()) || !old.getGroupName().equals(group.getGroupName())))
                materializeDescendants(connection,before,id);
            validateResult(before,BranchView.reload(connection));
            GroupDetails savedGroup=new GroupEditorField().fromGroupDetails(BranchView.current().group(id));
            attachHash(savedGroup);
            return savedGroup;
        });
        return result;
    }

    /** Materialize visible URL consequences without changing content timestamps or running synchronization. */
    private void materializeDescendants(Connection connection, BranchView before, int root) throws SQLException {
        Set<Integer> ids=subtree(before,root);
        for (GroupDetails child:before.groups()) if (ids.contains(child.getGroupId()) && child.getGroupId()!=root) {
            folder(child.getGroupId(),true);
            Map<String,Object> values=row(connection,BranchType.GROUP,child.getGroupId());
            store.put(connection,context,BranchType.GROUP,child.getGroupId(),values,currentHash(connection,BranchType.GROUP,child.getGroupId()),false);
        }
        BranchView after=BranchView.reload(connection);
        for (DocDetails doc:before.documents()) if (ids.contains(doc.getGroupId())) {
            if (!EditorService.hasPagePermission(user,doc) || user.isDisabledItem("pageSave")) throw forbidden();
            multigroup(doc.getDocId());
            DocDetails effective=after.document(doc.getDocId(),false);
            Map<String,Object> values=row(connection,BranchType.DOCUMENT,doc.getDocId());
            values.put("virtual_path",effective.getVirtualPath());
            store.put(connection,context,BranchType.DOCUMENT,doc.getDocId(),values,currentHash(connection,BranchType.DOCUMENT,doc.getDocId()),false);
        }
    }

    public boolean deleteDocument(DocDetails submitted) {
        new BranchAccess().requireCsrf(request);
        return write(connection -> {
            BranchView before=BranchView.reload(connection);
            DocDetails doc=before.document(submitted.getDocId(),false);
            if (doc==null) throw invalid("Page no longer exists");
            folder(doc.getGroupId(),false); multigroup(doc.getDocId());
            if (user.isDisabledItem("deletePage") || !EditorService.hasPagePermission(user,doc)) throw forbidden();
            if (before.group(doc.getGroupId()).getDefaultDocId()==doc.getDocId()) replaceDefault(connection,before,doc.getGroupId(),doc.getDocId());
            removeOrDelete(connection,BranchType.DOCUMENT,doc.getDocId(),submitted.getEditorFields()==null?null:submitted.getEditorFields().getBranchHash());
            validateResult(before,BranchView.reload(connection)); return true;
        });
    }

    private void replaceDefault(Connection connection,BranchView view,int groupId,int removedPage) throws SQLException {
        folder(groupId,true);
        int replacement=view.documents(true).stream().filter(doc -> doc.getGroupId()==groupId && doc.getDocId()!=removedPage)
            .sorted(Comparator.comparingInt(DocDetails::getSortPriority).thenComparingInt(DocDetails::getDocId)).mapToInt(DocDetails::getDocId).findFirst().orElse(0);
        Map<String,Object> values=row(connection,BranchType.GROUP,groupId);
        values.put("default_doc_id",replacement);
        store.put(connection,context,BranchType.GROUP,groupId,values,currentHash(connection,BranchType.GROUP,groupId),false);
    }

    public boolean deleteGroup(GroupDetails submitted) {
        new BranchAccess().requireCsrf(request);
        if (submitted.getEditorFields()==null) throw invalid("Missing displayed folder version");
        return write(connection -> {
            BranchView before=BranchView.reload(connection);
            folder(submitted.getGroupId(),true);
            if (user.isDisabledItem("deleteDir")) throw forbidden();
            Set<Integer> ids=subtree(before,submitted.getGroupId());
            for (DocDetails doc:before.documents()) if (ids.contains(doc.getGroupId())) {
                if (user.isDisabledItem("deletePage") || !EditorService.hasPagePermission(user,doc)) throw forbidden();
                multigroup(doc.getDocId());
                removeOrDelete(connection,BranchType.DOCUMENT,doc.getDocId(),currentHash(connection,BranchType.DOCUMENT,doc.getDocId()));
            }
            for (GroupDetails child:before.groups()) if (ids.contains(child.getGroupId())) {
                folder(child.getGroupId(),true);
                String expected=child.getGroupId()==submitted.getGroupId() ? submitted.getEditorFields().getBranchHash() : currentHash(connection,BranchType.GROUP,child.getGroupId());
                removeOrDelete(connection,BranchType.GROUP,child.getGroupId(),expected);
            }
            validateResult(before,BranchView.reload(connection)); return true;
        });
    }

    private void removeOrDelete(Connection connection,BranchType type,int id,String expected) throws SQLException {
        Map<String,Object> values=row(connection,type,id);
        if (values.containsKey("base_exists") && BranchStore.change(type,values).base().state()==BranchSnapshot.State.ABSENT) store.remove(connection,context,type,id,expected);
        else store.put(connection,context,type,id,values,expected,true);
    }

    public void revert(BranchType type,int id,String expected) {
        if (type!=BranchType.DOCUMENT && type!=BranchType.GROUP) throw invalid("Only page and folder changes can be reverted here");
        new BranchAccess().requireCsrf(request);
        write(connection -> {
            BranchView before=BranchView.reload(connection);
            Map<String,Object> values=row(connection,type,id);
            // Restore the candidate first, then check its actual scope. A failure rolls the transaction back.
            if(type==BranchType.DOCUMENT && BranchStore.change(type,values).base().state()==BranchSnapshot.State.ABSENT) {
                DocDetails doc=before.document(id,false);
                if(doc!=null && before.group(doc.getGroupId()).getDefaultDocId()==id) replaceDefault(connection,before,doc.getGroupId(),id);
            }
            store.remove(connection,context,type,id,expected);
            List<Integer> restoredGroups=new ArrayList<>(),restoredDocs=new ArrayList<>();
            if (type==BranchType.GROUP && "DELETE".equals(values.get("operation"))) {
                Set<Integer> ids=new HashSet<>(); ids.add(id);
                boolean added;
                do {
                    added=false;
                    for (Map<String,Object> child:before.workRows(BranchType.GROUP))
                        if ("DELETE".equals(child.get("operation")) && ids.contains(((Number)child.get("parent_group_id")).intValue()))
                            added |= ids.add(((Number)child.get("group_id")).intValue());
                } while (added);
                for (Map<String,Object> child:before.workRows(BranchType.GROUP)) {
                    int childId=((Number)child.get("group_id")).intValue();
                    if (childId!=id && ids.contains(childId) && "DELETE".equals(child.get("operation"))) {
                        store.remove(connection,context,BranchType.GROUP,childId,BranchStore.change(BranchType.GROUP,child).work().hash()); restoredGroups.add(childId);
                    }
                }
                for (Map<String,Object> child:before.workRows(BranchType.DOCUMENT)) if(ids.contains(((Number)child.get("group_id")).intValue()) && "DELETE".equals(child.get("operation"))) {
                    int childId=((Number)child.get("doc_id")).intValue();
                    store.remove(connection,context,BranchType.DOCUMENT,childId,BranchStore.change(BranchType.DOCUMENT,child).work().hash()); restoredDocs.add(childId);
                }
            }
            BranchView after=BranchView.reload(connection);
            for(int restored:restoredGroups) folder(restored,true);
            for(int restored:restoredDocs) {
                DocDetails doc=after.document(restored,false);
                if(doc==null || !EditorService.hasPagePermission(user,doc) || user.isDisabledItem("pageSave")) throw forbidden();
                multigroup(restored);
            }
            if (type==BranchType.DOCUMENT) {
                DocDetails doc=after.document(id,false);
                if (doc==null) doc=before.document(id,false);
                if (doc==null) throw invalid("Restore the parent folder first");
                folder(doc.getGroupId(),false);
                if (user.isDisabledItem("pageSave") || !EditorService.hasPagePermission(user,doc)) throw forbidden();
                multigroup(id);
            } else {
                GroupDetails group=after.group(id); boolean removedNew=group==null; if (group==null) group=before.group(id);
                if (group==null) throw invalid("Restore the parent folder first");
                if (group.getParentGroupId()>0) folder(group.getParentGroupId(),true);
                if (user.isDisabledItem(removedNew ? "deleteDir" : "editDir") || (!removedNew && !GroupsDB.isGroupEditable(user,id))) throw forbidden();
                system(group);
            }
            validateResult(before,after); return null;
        });
    }

    private Map<String,Object> row(Connection connection,BranchType type,int id) throws SQLException {
        return new LinkedHashMap<>(BranchStore.readWork(connection,context.branchId(),type,id)
            .orElseGet(() -> { try { return BranchStore.readLive(connection,type,id).orElseThrow(); } catch(SQLException ex) { throw new IllegalStateException(ex); } }));
    }

    private GroupDetails folder(int id,boolean editable) {
        GroupDetails group=BranchView.current().group(id);
        if (group==null) throw invalid("Folder is absent in this domain");
        system(group);
        if (!context.domainName().equals(GroupsDB.getInstance().getDomain(id))) throw forbidden();
        if (editable && !GroupsDB.isGroupEditable(user,id)) throw forbidden();
        return group;
    }

    private void system(GroupDetails group) {
        if (systemFolder(group)) throw invalid("Shared System content is read-only");
    }

    private boolean systemFolder(GroupDetails group) {
        for (GroupDetails parent=group;parent!=null;parent=BranchView.current().group(parent.getParentGroupId()))
            if (parent.isInternal() || "system".equalsIgnoreCase(parent.getGroupName())) return true;
        return false;
    }

    private void multigroup(int id) {
        if (MultigroupMappingDB.getMasterDocId(id)>0 || !MultigroupMappingDB.getSlaveDocIds(id).isEmpty())
            throw invalid("Multigroup pages are read-only in a publishing branch");
    }

    private void validateDocument(DocDetails doc) {
        if (doc.getTempId()>0 && TemplatesDB.getInstance().getTemplate(doc.getTempId())==null) throw invalid("Unknown template");
        for(int id:List.of(doc.getHeaderDocId(),doc.getFooterDocId(),doc.getMenuDocId(),doc.getRightMenuDocId(),doc.getLogonPageDocId(),doc.getTempFieldADocId(),doc.getTempFieldBDocId(),doc.getTempFieldCDocId(),doc.getTempFieldDDocId())) reference(id);
        Set<Integer> allowed=new HashSet<>();
        for (PerexGroupBean group:DocDB.getInstance().getPerexGroups(doc.getGroupId())) allowed.add(group.getPerexGroupId());
        for (int id:BranchFields.normalizedIds(doc.getPerexGroupIdsString())) if(!allowed.contains(id)) throw invalid("Perex group is unavailable in this folder");
        String path=Objects.toString(doc.getVirtualPath(),"");
        if (path.contains("\\") || path.contains("..") || path.contains("?") || path.contains("#") || path.contains(":") || path.startsWith("//")) throw invalid("Invalid page URL");
        if (!path.isBlank() && path.startsWith("/")) {
            int existing=BranchView.current().docIdFromUrl(path);
            if(existing>0 && existing!=doc.getDocId()) throw invalid("URL already belongs to page "+existing);
        }
    }

    private void reference(int id) { if(id>0 && BranchView.current().document(id,true)==null) throw invalid("Referenced page is absent in this domain: "+id); }

    private void rejectUnsupported(DocDetails doc,DocDetails old) {
        DocEditorFields fields=doc.getEditorFields();
        if ((fields.getGroupCopyDetails()!=null && !fields.getGroupCopyDetails().isEmpty()) || (fields.getAttrs()!=null && !fields.getAttrs().isEmpty()) || Tools.isNotEmpty(fields.getRedactorNote()))
            throw invalid("Multigroup, attributes and editor notes are unavailable in a publishing branch");
        if (fields.isPublishAfterStart()!=(old!=null&&old.isPublishAfterStart()) || fields.isDisableAfterEnd()!=(old!=null&&old.isDisableAfterEnd()) || (doc.isPublishAfterStart() && (old==null || !old.isPublishAfterStart())) || (doc.isDisableAfterEnd() && (old==null || !old.isDisableAfterEnd()))) throw invalid("Individual publishing plans are read-only");
        if (old!=null && (!Objects.equals(doc.getPublishStartDate(),old.getPublishStartDate()) || !Objects.equals(doc.getPublishEndDate(),old.getPublishEndDate()) || doc.getSyncId()!=old.getSyncId() || doc.getSyncStatus()!=old.getSyncStatus()))
            throw invalid("Scheduling and synchronization fields are read-only");
        if (old==null && (doc.getPublishEndDate()!=null || doc.getSyncId()!=0 || doc.getSyncStatus()!=0)) throw invalid("Scheduling and synchronization fields are read-only");
    }

    private void rejectUnsupported(GroupDetails group,GroupDetails old) {
        GroupEditorField f=group.getEditorFields();
        if (f.isPublishPlan() || f.isForceDomainNameChange() || f.isForcePriorityRecalculation() || f.isForceTemplateToSubgroupsAndPages() || f.isForceNewPageDocIdTemplateSubFolders() || f.isPasswordProtectedSubFolders() || f.isForceMenuTypeSubfolders() || f.isForceLoggedMenuTypeSubfolders() || f.isForceNavbarSubfolders() || f.isForceSitemapSubfolders() || f.isForceLoggedNavbarSubfolders() || f.isForceLoggedSitemapSubfolders() || f.isForceLngToSubFolders())
            throw invalid("Scheduling and recursive folder settings are unavailable in a publishing branch");
        if (old!=null && (!Objects.equals(group.getDomainName(),old.getDomainName()) || group.getSyncId()!=old.getSyncId() || group.getSyncStatus()!=old.getSyncStatus())) throw invalid("Domain and synchronization fields are read-only");
        if (old==null && Tools.isNotEmpty(group.getDomainName()) && !context.domainName().equals(group.getDomainName())) throw forbidden();
    }

    private Set<Integer> subtree(BranchView view,int root) {
        Set<Integer> ids=new LinkedHashSet<>(); ids.add(root);
        boolean added; do { added=false; for(GroupDetails group:view.groups()) if(ids.contains(group.getParentGroupId())) added|=ids.add(group.getGroupId()); } while(added);
        return ids;
    }

    /** Validate new inconsistencies while tolerating unrelated legacy URLs and empty folders. */
    private void validateResult(BranchView before,BranchView after) {
        Map<String,Integer> folders=new HashMap<>();
        for(GroupDetails group:after.groups()) {
            String path=DocDB.getGroupDiskPath(after.groups(),group.getGroupId()).toLowerCase(Locale.ROOT);
            Integer duplicate=folders.putIfAbsent(path,group.getGroupId());
            if(duplicate!=null) {
                boolean existed=before.group(group.getGroupId())!=null && before.group(duplicate)!=null;
                if(!existed || !DocDB.getGroupDiskPath(before.groups(),group.getGroupId()).equalsIgnoreCase(DocDB.getGroupDiskPath(before.groups(),duplicate)))
                    throw invalid("Folder URL collision: "+path);
            }
        }
        Map<String,Integer> urls=new HashMap<>();
        for (DocDetails doc:after.documents(true)) {
            String path=DocDB.normalizeVirtualPath(doc.getVirtualPath());
            if (path==null || path.isBlank()) continue;
            Integer duplicate=urls.putIfAbsent(path.toLowerCase(Locale.ROOT),doc.getDocId());
            if (duplicate!=null) {
                DocDetails old=before.document(doc.getDocId(),true),other=before.document(duplicate,true);
                if(old==null || other==null || !Objects.equals(old.getVirtualPath(),other.getVirtualPath())) throw invalid("URL collision: "+path);
            }
        }
        for (DocDetails doc:after.documents(true)) {
            DocDetails old=before.document(doc.getDocId(),true);
            int[] refs=documentReferences(doc),oldRefs=old==null?new int[refs.length]:documentReferences(old);
            for(int i=0;i<refs.length;i++) if(refs[i]>0 && after.document(refs[i],true)==null
                    && (old==null || refs[i]!=oldRefs[i] || before.document(refs[i],true)!=null))
                throw invalid("Page "+doc.getDocId()+" references a removed page "+refs[i]);
        }
        for(GroupDetails group:after.groups()) {
            GroupDetails old=before.group(group.getGroupId());
            int[] refs={group.getLogonPageDocId(),group.getNewPageDocIdTemplate()};
            int[] oldRefs=old==null?new int[2]:new int[]{old.getLogonPageDocId(),old.getNewPageDocIdTemplate()};
            for(int i=0;i<refs.length;i++) if(refs[i]>0 && after.document(refs[i],true)==null
                    && (old==null || refs[i]!=oldRefs[i] || before.document(refs[i],true)!=null)) throw invalid("Folder references a removed page "+refs[i]);
            if(group.getLinkGroupId()>0 && after.group(group.getLinkGroupId())==null
                    && (old==null || group.getLinkGroupId()!=old.getLinkGroupId() || before.group(group.getLinkGroupId())!=null)) throw invalid("Folder references a removed folder");
        }
        for(GroupDetails group:after.groups()) if(group.getDefaultDocId()>0) {
            DocDetails doc=after.document(group.getDefaultDocId(),true);
            GroupDetails old=before.group(group.getGroupId());
            DocDetails oldDoc=old==null?null:before.document(old.getDefaultDocId(),true);
            boolean previouslyValid=oldDoc!=null && oldDoc.getGroupId()==group.getGroupId();
            if((doc==null || doc.getGroupId()!=group.getGroupId()) && (old==null || old.getDefaultDocId()!=group.getDefaultDocId() || previouslyValid))
                throw invalid("Invalid default page in folder "+group.getGroupId());
        }
    }

    private int[] documentReferences(DocDetails doc) {
        return new int[]{doc.getHeaderDocId(),doc.getFooterDocId(),doc.getMenuDocId(),doc.getRightMenuDocId(),doc.getLogonPageDocId(),doc.getTempFieldADocId(),doc.getTempFieldBDocId(),doc.getTempFieldCDocId(),doc.getTempFieldDDocId()};
    }

    private <T> T write(BranchStore.Work<T> work) {
        try { return store.write(context,work); }
        catch(SQLException ex) { throw new IllegalStateException("Cannot save working change",ex); }
        catch(IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage(),ex); }
        finally { BranchView.invalidate(); }
    }
    private static ResponseStatusException invalid(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT,reason); }
    private static ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN); }
}
