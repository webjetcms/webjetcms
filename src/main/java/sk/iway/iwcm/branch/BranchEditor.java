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
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

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
            doc = editableDocument(BranchView.current(), id);
            WebpagesService.processFromEntity(doc,ProcessItemAction.GETONE,request,true);
            String reason=null;
            if(systemFolder(BranchView.current().group(doc.getGroupId()))) reason=sk.iway.iwcm.i18n.Prop.getInstance(request).getText("branches.system.readonly");
            doc.getEditorFields().setBranchReadOnlyReason(reason);
        }
        attachHash(doc);
        return doc;
    }

    private DocDetails editableDocument(BranchView view, int id) {
        DocDetails doc = view.document(id,false);
        if (doc==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!EditorService.hasPagePermission(user,doc)) throw forbidden();
        int masterId = MultigroupMappingDB.getMasterDocId(id);
        if (masterId > 0 && masterId != id) {
            DocDetails master = view.document(masterId, false);
            if (master == null || !EditorService.hasPagePermission(user, master)) throw forbidden();
            usePageLocation(master, doc);
            master.setEditorFields(doc.getEditorFields());
            master.setPublishStartDate(doc.getPublishStartDate());
            master.setPublishEndDate(doc.getPublishEndDate());
            master.setPublishAfterStart(doc.isPublishAfterStart());
            master.setDisableAfterEnd(doc.isDisableAfterEnd());
            doc = master;
        }
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
        new BranchAccess().requireCsrf(request);
        DocDetails result=write(connection -> {
            BranchView before=BranchView.reload(connection);
            int id=saveDocument(connection,before,doc,editor);
            BranchView after=BranchView.reload(connection);
            validateResult(before,after);
            return editableDocument(after, id);
        });
        WebpagesService.processFromEntity(result,ProcessItemAction.GETONE,request,true);
        attachHash(result);
        return result;
    }

    private int saveDocument(Connection connection, BranchView before, DocDetails doc, EditorService editor) throws SQLException {
        if (doc.getEditorFields()==null) throw invalid("Missing editor fields");
        String expected=doc.getEditorFields().getBranchHash();
        DocDetails original=doc.getDocId()>0 ? before.document(doc.getDocId(),false) : null;
        if (doc.getDocId()>0 && original==null) throw invalid("Page no longer exists");
        rejectUnsupported(doc,original);
        doc.getEditorFields().toDocDetails(doc);
        GroupDetails destination=folder(doc.getGroupId(),false);
        if (doc.getSortPriority()<0) editor.setSortPriority(doc,destination);
        editor.checkSavePermissions(user,doc);
        boolean newToFolder=original==null || original.getGroupId()!=doc.getGroupId();
        if (newToFolder) folder(doc.getGroupId(),true);
        if (original!=null) {
            folder(original.getGroupId(),false);
            BranchMultigroup links = BranchMultigroup.read(connection, doc.getDocId());
            if (!links.copies().isEmpty()) {
                if (newToFolder || !links.matchesFolders(doc.getEditorFields().getGroupCopyDetails(), before))
                    throw mappingReadOnly(original);
                saveLinkedPages(connection, before, doc, expected, links, editor);
                return doc.getDocId();
            }
            if (newToFolder && before.group(original.getGroupId()).getDefaultDocId()==doc.getDocId())
                replaceDefault(connection,before,original.getGroupId(),doc.getDocId());
        }
        if (doc.getEditorFields().getGroupCopyDetails() != null && !doc.getEditorFields().getGroupCopyDetails().isEmpty())
            throw mappingReadOnly(original);
        Map<String,Object> stable=original==null
            ? BranchDocumentMapper.initialValues(1,user.getUserId(),Instant.now(),Constants.getBoolean("editorAutoFillPublishStart"))
            : row(connection,BranchType.DOCUMENT,doc.getDocId());
        Map<String,Object> values=prepareDocumentRow(doc,stable,editor);
        Map<String,Object> saved=original==null ? store.create(connection,context,BranchType.DOCUMENT,values)
            : store.put(connection,context,BranchType.DOCUMENT,doc.getDocId(),values,expected,false);
        int id=((Number)saved.get("doc_id")).intValue();
        if (newToFolder && destination.getDefaultDocId()<1) setDefaultDocument(connection,destination.getGroupId(),id);
        return id;
    }

    private Map<String,Object> prepareDocumentRow(DocDetails page, Map<String,Object> stored, EditorService editor) {
        validateDocument(page);
        editor.normalizeEditableContent(page);
        page.setDateCreated(System.currentTimeMillis());
        page.setAuthorId(user.getUserId());
        return BranchEditorRows.document(page, stored);
    }

    /**
     * Applies one multigroup page's ID, folder and URL settings to the shared content in {@code edited}.
     * When opening a copy, the editor shows the master's content at the selected copy's location.
     * When saving, each linked page receives the edited content under its own ID and in its own folder.
     * Its sort priority is copied only when {@code multiGroupKeepSortPriority} is enabled.
     *
     * @param edited working document whose location fields are updated in memory
     * @param location existing master or copy that supplies the location fields
     */
    private static void usePageLocation(DocDetails edited, DocDetails location) {
        edited.setDocId(location.getDocId());
        edited.setGroupId(location.getGroupId());
        edited.setVirtualPath(location.getVirtualPath());
        edited.setEditorVirtualPath(location.getEditorVirtualPath());
        edited.setUrlInheritGroup(location.getUrlInheritGroup());
        edited.setGenerateUrlFromTitle(location.getGenerateUrlFromTitle());
        if (Constants.getBoolean("multiGroupKeepSortPriority")) edited.setSortPriority(location.getSortPriority());
    }

    private void saveLinkedPages(Connection connection, BranchView view, DocDetails edited, String expected,
            BranchMultigroup links, EditorService editor) throws SQLException {
        // Save the master first so redirecting copies receive its resulting URL.
        Set<Integer> ids = new LinkedHashSet<>();
        ids.add(links.masterId());
        ids.addAll(links.copies().keySet());
        String masterUrl = null;
        String externalLink = edited.getExternalLink();
        for (int id : ids) {
            DocDetails location = view.document(id, false);
            if (location == null || !EditorService.hasPagePermission(user, location)) throw forbidden();
            folder(location.getGroupId(), false);
            Map<String, Object> stored = row(connection, BranchType.DOCUMENT, id);
            links.recordLinks(stored);
            DocDetails page = edited;
            if (id != edited.getDocId()) {
                try (var fields = BranchRows.result(BranchEditorRows.document(edited, stored))) {
                    page = DocDB.getDocDetails(fields, false, true);
                }
                usePageLocation(page, location);
                if (Boolean.TRUE.equals(edited.getGenerateUrlFromTitle())) {
                    page.setGenerateUrlFromTitle(true);
                    page.setEditorVirtualPath("");
                    page.setVirtualPath("");
                }
            }
            page.setExternalLink(id != links.masterId() && Boolean.TRUE.equals(links.copies().get(id)) ? masterUrl : externalLink);
            editor.checkSavePermissions(user, page);
            Map<String, Object> values = prepareDocumentRow(page, stored, editor);
            if (id == links.masterId()) masterUrl = page.getVirtualPath();
            store.put(connection, context, BranchType.DOCUMENT, id, values,
                id == edited.getDocId() ? expected : location.getEditorFields().getBranchHash(), false);
        }
    }

    public GroupDetails saveGroup(GroupDetails group, EditorService editor) {
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
            if (old!=null && old.getParentGroupId()>0 && group.getParentGroupId()==0 && !context.domainName().equals(old.getDomainName()))
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
            if (old==null && Constants.getBoolean("groupCreateBlankWebpageAfterCreate") && !user.isDisabledItem("addPage")) {
                BranchView withNewGroup=BranchView.reload(connection);
                DocDetails page=loadDocument(-1,-1,id,editor);
                String title=Tools.replace(group.getGroupName(),"&#47;","/");
                page.setTitle(title);
                page.setNavbar(title);
                saveDocument(connection,withNewGroup,page,editor);
            }
            if (old != null) {
                boolean pathChanged = old.getParentGroupId() != group.getParentGroupId() || !old.getUrlDirName().equals(group.getUrlDirName());
                boolean regenerateUrls = group.getEditorFields().isForceUrlDirNameChange();
                if (pathChanged || regenerateUrls) {
                    materializeDescendants(connection, before, id, regenerateUrls);
                }
            }
            validateResult(before,BranchView.reload(connection));
            GroupDetails savedGroup=new GroupEditorField().fromGroupDetails(BranchView.current().group(id));
            attachHash(savedGroup);
            return savedGroup;
        });
        return result;
    }

    /** Stage inherited URLs and redirects without changing the content timestamps. */
    private void materializeDescendants(Connection connection, BranchView before, int root, boolean regenerateUrls) throws SQLException {
        Set<Integer> ids=subtree(before,root);
        for (GroupDetails child:before.groups()) if (ids.contains(child.getGroupId()) && child.getGroupId()!=root) {
            folder(child.getGroupId(),true);
            Map<String,Object> values=row(connection,BranchType.GROUP,child.getGroupId());
            store.put(connection,context,BranchType.GROUP,child.getGroupId(),values,currentHash(connection,BranchType.GROUP,child.getGroupId()),false);
        }
        BranchView after=BranchView.reload(connection);
        for (DocDetails doc:before.metadataDocuments()) if (ids.contains(doc.getGroupId())) {
            if (!EditorService.hasPagePermission(user,doc) || user.isDisabledItem("pageSave")) throw forbidden();
            DocDetails effective=after.document(doc.getDocId(),false);
            String input = WebpagesService.getRegeneratedVirtualPathInput(doc, !regenerateUrls);
            if (input != null) {
                effective.setVirtualPath(input);
                after.normalizeUrl(effective);
            }
            Map<String,Object> values=row(connection,BranchType.DOCUMENT,doc.getDocId());
            BranchMultigroup links = BranchMultigroup.read(connection, doc.getDocId());
            links.recordLinks(values);
            values.put("virtual_path",effective.getVirtualPath());
            store.put(connection,context,BranchType.DOCUMENT,doc.getDocId(),values,currentHash(connection,BranchType.DOCUMENT,doc.getDocId()),false);
            if (doc.getDocId() == links.masterId() && !doc.getVirtualPath().equals(effective.getVirtualPath())) {
                for (var copy : links.copies().entrySet()) {
                    if (!copy.getValue() || copy.getKey() == links.masterId()) continue;
                    DocDetails target = before.metadataDocument(copy.getKey());
                    if (target == null || !EditorService.hasPagePermission(user, target)) throw forbidden();
                    folder(target.getGroupId(), false);
                    var copyRow = row(connection, BranchType.DOCUMENT, target.getDocId());
                    links.recordLinks(copyRow);
                    copyRow.put("external_link", effective.getVirtualPath());
                    store.put(connection, context, BranchType.DOCUMENT, target.getDocId(), copyRow,
                        currentHash(connection, BranchType.DOCUMENT, target.getDocId()), false);
                }
            }
        }
    }

    public boolean deleteDocument(DocDetails submitted) {
        new BranchAccess().requireCsrf(request);
        return write(connection -> {
            BranchView before=BranchView.reload(connection);
            DocDetails doc=before.document(submitted.getDocId(),false);
            if (doc==null) throw invalid("Page no longer exists");
            folder(doc.getGroupId(),false);
            if (user.isDisabledItem("deletePage") || !EditorService.hasPagePermission(user,doc)) throw forbidden();
            requireUnlinked(connection, doc);
            if (before.group(doc.getGroupId()).getDefaultDocId()==doc.getDocId()) replaceDefault(connection,before,doc.getGroupId(),doc.getDocId());
            removeOrDelete(connection,BranchType.DOCUMENT,doc.getDocId(),submitted.getEditorFields()==null?null:submitted.getEditorFields().getBranchHash());
            validateResult(before,BranchView.reload(connection)); return true;
        });
    }

    private void replaceDefault(Connection connection,BranchView view,int groupId,int removedPage) throws SQLException {
        folder(groupId,true);
        int replacement=view.documents(true).stream().filter(doc -> doc.getGroupId()==groupId && doc.getDocId()!=removedPage)
            .sorted(Comparator.comparingInt(DocDetails::getSortPriority).thenComparingInt(DocDetails::getDocId)).mapToInt(DocDetails::getDocId).findFirst().orElse(0);
        setDefaultDocument(connection,groupId,replacement);
    }

    private void setDefaultDocument(Connection connection, int groupId, int docId) throws SQLException {
        Map<String,Object> values=row(connection,BranchType.GROUP,groupId);
        values.put("default_doc_id",docId);
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
                requireUnlinked(connection, doc);
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
            boolean removingNewPage = type == BranchType.DOCUMENT && BranchStore.change(type,values).base().state() == BranchSnapshot.State.ABSENT;
            if (removingNewPage) {
                DocDetails doc=before.document(id,false);
                checkPageRevertPermission(doc);
                if (before.group(doc.getGroupId()).getDefaultDocId()==id) replaceDefault(connection,before,doc.getGroupId(),id);
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
            }
            if (type==BranchType.GROUP) {
                GroupDetails group=after.group(id); boolean removedNew=group==null; if (group==null) group=before.group(id);
                if (group==null) throw invalid("Restore the parent folder first");
                if (group.getParentGroupId()>0) folder(group.getParentGroupId(),true);
                if (user.isDisabledItem(removedNew ? "deleteDir" : "editDir") || (!removedNew && !GroupsDB.isGroupEditable(user,id))) throw forbidden();
                system(group);
            } else if (!removingNewPage) {
                DocDetails doc=after.document(id,false);
                checkPageRevertPermission(doc==null ? before.document(id,false) : doc);
            }
            validateResult(before,after); return null;
        });
    }

    private void checkPageRevertPermission(DocDetails doc) {
        if (doc==null) throw invalid("Restore the parent folder first");
        folder(doc.getGroupId(),false);
        if (user.isDisabledItem("pageSave") || !EditorService.hasPagePermission(user,doc)) throw forbidden();
    }

    private Map<String,Object> row(Connection connection,BranchType type,int id) throws SQLException {
        return new LinkedHashMap<>(BranchStore.readWork(connection,context.branchId(),type,id)
            .orElseGet(() -> { try { return BranchStore.readLive(connection,type,id).orElseThrow(); } catch(SQLException ex) { throw new IllegalStateException(ex); } }));
    }

    private GroupDetails folder(int id,boolean editable) {
        GroupDetails group=BranchView.current().group(id);
        if (group==null) throw invalid("Folder is absent in this domain");
        system(group);
        String domain=GroupsDB.getInstance().getDomain(id);
        if (Tools.isNotEmpty(domain) && !"default".equals(domain) && !context.domainName().equals(domain))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,Prop.getInstance(request).getText("branches.editor.foreignDomain"));
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

    private void requireUnlinked(Connection connection, DocDetails doc) throws SQLException {
        if (!BranchMultigroup.read(connection, doc.getDocId()).copies().isEmpty()) throw mappingReadOnly(doc);
    }

    private ResponseStatusException mappingReadOnly(DocDetails doc) {
        String page = doc == null ? "" : " " + BranchMultigroup.label(doc.getTitle(), doc.getDocId());
        return invalid(Prop.getInstance(request).getText("branches.multigroup.linksReadOnly", page));
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
        if ((fields.getAttrs()!=null && !fields.getAttrs().isEmpty()) || Tools.isNotEmpty(fields.getRedactorNote()))
            throw invalid("Attributes and editor notes are unavailable in a publishing branch");
        if (fields.isPublishAfterStart()!=(old!=null&&old.isPublishAfterStart()) || fields.isDisableAfterEnd()!=(old!=null&&old.isDisableAfterEnd()) || (doc.isPublishAfterStart() && (old==null || !old.isPublishAfterStart())) || (doc.isDisableAfterEnd() && (old==null || !old.isDisableAfterEnd()))) throw invalid("Individual publishing plans are read-only");
        // Synchronization fields are @JsonIgnore; BranchEditorRows retains their server values.
        if (old!=null && (!Objects.equals(doc.getPublishStartDate(),old.getPublishStartDate()) || !Objects.equals(doc.getPublishEndDate(),old.getPublishEndDate())))
            throw invalid(Prop.getInstance(request).getText("branches.editor.scheduling.readonly"));
        if (old==null && doc.getPublishEndDate()!=null) throw invalid(Prop.getInstance(request).getText("branches.editor.scheduling.readonly"));
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
    static void validateResult(BranchView before,BranchView after) {
        List<GroupDetails> beforeGroups=before.groups(),afterGroups=after.groups();
        Map<String,Integer> folders=new HashMap<>();
        for(GroupDetails group:afterGroups) {
            String path=DocDB.getGroupDiskPath(afterGroups,group.getGroupId()).toLowerCase(Locale.ROOT);
            Integer duplicate=folders.putIfAbsent(path,group.getGroupId());
            if(duplicate!=null) {
                boolean existed=before.group(group.getGroupId())!=null && before.group(duplicate)!=null;
                if(!existed || !DocDB.getGroupDiskPath(beforeGroups,group.getGroupId()).equalsIgnoreCase(DocDB.getGroupDiskPath(beforeGroups,duplicate)))
                    throw invalid("Folder URL collision: "+path);
            }
        }
        Map<String,Integer> urls=new HashMap<>();
        for (DocDetails doc:after.metadataDocuments()) {
            String path=DocDB.normalizeVirtualPath(doc.getVirtualPath());
            if (path==null || path.isBlank()) continue;
            Integer duplicate=urls.putIfAbsent(path.toLowerCase(Locale.ROOT),doc.getDocId());
            if (duplicate!=null) {
                DocDetails old=before.metadataDocument(doc.getDocId()),other=before.metadataDocument(duplicate);
                if(old==null || other==null || !Objects.equals(old.getVirtualPath(),other.getVirtualPath())) throw invalid("URL collision: "+path);
            }
        }
        for (DocDetails doc:after.metadataDocuments()) {
            DocDetails old=before.metadataDocument(doc.getDocId());
            int[] refs=documentReferences(doc),oldRefs=old==null?new int[refs.length]:documentReferences(old);
            for(int i=0;i<refs.length;i++) if(refs[i]>0 && after.metadataDocument(refs[i])==null
                    && (old==null || refs[i]!=oldRefs[i] || before.metadataDocument(refs[i])!=null))
                throw invalid("Page "+doc.getDocId()+" references a removed page "+refs[i]);
        }
        for(GroupDetails group:afterGroups) {
            GroupDetails old=before.group(group.getGroupId());
            int[] refs={group.getLogonPageDocId(),group.getNewPageDocIdTemplate()};
            int[] oldRefs=old==null?new int[2]:new int[]{old.getLogonPageDocId(),old.getNewPageDocIdTemplate()};
            for(int i=0;i<refs.length;i++) if(refs[i]>0 && after.metadataDocument(refs[i])==null
                    && (old==null || refs[i]!=oldRefs[i] || before.metadataDocument(refs[i])!=null)) throw invalid("Folder references a removed page "+refs[i]);
            if(group.getLinkGroupId()>0 && after.group(group.getLinkGroupId())==null
                    && (old==null || group.getLinkGroupId()!=old.getLinkGroupId() || before.group(group.getLinkGroupId())!=null)) throw invalid("Folder references a removed folder");
        }
        for(GroupDetails group:afterGroups) if(group.getDefaultDocId()>0) {
            DocDetails doc=after.metadataDocument(group.getDefaultDocId());
            GroupDetails old=before.group(group.getGroupId());
            DocDetails oldDoc=old==null?null:before.metadataDocument(old.getDefaultDocId());
            boolean previouslyValid=oldDoc!=null && oldDoc.getGroupId()==group.getGroupId();
            if((doc==null || doc.getGroupId()!=group.getGroupId()) && (old==null || old.getDefaultDocId()!=group.getDefaultDocId() || previouslyValid))
                throw invalid("Invalid default page in folder "+group.getGroupId());
        }
    }

    private static int[] documentReferences(DocDetails doc) {
        return new int[]{doc.getHeaderDocId(),doc.getFooterDocId(),doc.getMenuDocId(),doc.getRightMenuDocId(),doc.getLogonPageDocId(),doc.getTempFieldADocId(),doc.getTempFieldBDocId(),doc.getTempFieldCDocId(),doc.getTempFieldDDocId()};
    }

    private <T> T write(BranchStore.Work<T> work) {
        try { return store.write(context,work); }
        catch(SQLException ex) { throw new IllegalStateException("Cannot save working change",ex); }
        catch(IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage(),ex); }
        finally { BranchView.invalidate(); }
    }
    private static ResponseStatusException invalid(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT,reason); }
    private static ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN,Prop.getInstance().getText("branches.editor.permission")); }
}
