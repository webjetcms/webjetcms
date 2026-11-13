package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.spirit.model.*;

/** Document-owned media edits use WORK only; the owning page is never saved as a side effect. */
public final class BranchMediaEditor {
    private final BranchStore store=new BranchStore();
    private final HttpServletRequest request;
    private final Identity user;
    private final BranchContext context;

    public BranchMediaEditor(HttpServletRequest request) {
        this.request=request;
        user=new BranchAccess().requireUser(request);
        context=BranchContexts.current(request);
        if(context.mode()!=BranchContext.Mode.ADMIN||!context.isBranch()||context.userId()!=user.getUserId()) throw invalid("An ADMIN branch is required");
    }

    public DocDetails owner(int id,boolean write) {
        if(id<1) throw invalid("Save the page in this branch before adding media");
        DocDetails doc=BranchView.current().document(id,true);
        if(doc==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media owner is absent in this domain");
        if(user.isDisabledItem("editor_edit_media_all")&&!EditorService.hasPagePermission(user,doc)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(write) {
            if(MultigroupMappingDB.getMasterDocId(id)>0||!MultigroupMappingDB.getSlaveDocIds(id).isEmpty()) throw invalid("Multigroup pages are read-only in a publishing branch");
            for(GroupDetails group=BranchView.current().group(doc.getGroupId());group!=null;group=BranchView.current().group(group.getParentGroupId()))
                if(group.isInternal()||"system".equalsIgnoreCase(group.getGroupName())) throw invalid("System pages are read-only in a publishing branch");
        }
        return doc;
    }

    public Media load(int id,int requestedOwner) {
        Media media=BranchMedia.one(id);
        if(media==null||!Integer.valueOf(CloudToolsForCore.getDomainId()).equals(media.getDomainId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        owner(media.getMediaFkId(),false);
        if(requestedOwner>0&&requestedOwner!=media.getMediaFkId()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return media;
    }

    public Media save(Media submitted,int ownerId,String expectedHash) {
        new BranchAccess().requireCsrf(request);
        int id=write(connection->{
            BranchView.reload(connection);
            DocDetails doc=owner(ownerId,true);
            Map<String,Object> original=null;
            int mediaId=submitted.getMediaId();
            if(mediaId>0) {
                Media current=load(mediaId,ownerId);
                if(current.getMediaFkId()!=ownerId) throw invalid("Changing the media owner is unavailable in a publishing branch");
                original=BranchStore.readWork(connection,context.branchId(),BranchType.MEDIA,mediaId)
                    .orElseGet(()->live(connection,mediaId));
            }
            if(Tools.isEmpty(submitted.getMediaTitleSk())) throw invalid("A media title is required");
            PlannedChangesService.rejectPrivateReferences(submitted);
            for(String path:List.of(Objects.toString(submitted.getMediaLink(),""),Objects.toString(submitted.getMediaThumbLink(),"")))
                if(path.contains(BranchPreviewUrls.PREFIX)||path.contains("/__branch-assets/")) throw invalid("Use the final logical media URL");
            MediaEditorFields fields=submitted.getEditorFields();
            if(fields==null||fields.getGroups()==null) throw invalid("Media categories are required");
            List<Integer> categories=Arrays.stream(fields.getGroups()).filter(value->value!=null&&value!=-1).toList();
            Set<Integer> allowed=new HashSet<>();for(MediaGroupBean group:BranchMedia.categories(doc.getGroupId()))allowed.add(group.getMediaGroupId());
            for(int category:categories) if(!allowed.contains(category)) throw invalid("Media category is unavailable in this folder");
            Map<String,Object> values=BranchEditorRows.media(submitted,original==null?BranchMediaMapper.initialValues(1,ownerId,CloudToolsForCore.getDomainId()):original);
            values.put("media_fk_id",ownerId);values.put("media_fk_table_name","documents");
            values.put("group_ids_snapshot",BranchFields.normalizedIds(categories));
            if(submitted.getMediaSortOrder()==null)
                values.put("media_sort_order",BranchMedia.list(ownerId,null).stream().mapToInt(Media::getMediaSortOrder).max().orElse(0)+10);
            Map<String,Object> saved=mediaId<1?store.create(connection,context,BranchType.MEDIA,values)
                :store.put(connection,context,BranchType.MEDIA,mediaId,values,expectedHash,false);
            return ((Number)saved.get("media_id")).intValue();
        });
        return load(id,ownerId);
    }

    public boolean delete(int id,int requestedOwner,String expectedHash) {
        new BranchAccess().requireCsrf(request);
        return write(connection->{
            BranchView.reload(connection);
            Media media=load(id,requestedOwner);owner(media.getMediaFkId(),true);
            Map<String,Object> old=BranchStore.readWork(connection,context.branchId(),BranchType.MEDIA,id).orElse(null);
            if(old!=null&&!Boolean.TRUE.equals(old.get("base_exists"))) store.remove(connection,context,BranchType.MEDIA,id,expectedHash);
            else {
                Map<String,Object> row=old==null?BranchStore.readLive(connection,BranchType.MEDIA,id).orElseThrow():old;
                store.put(connection,context,BranchType.MEDIA,id,row,expectedHash,true);
            }
            return true;
        });
    }

    private static Map<String,Object> live(Connection connection,int id) {
        try { return BranchStore.readLive(connection,BranchType.MEDIA,id).orElseThrow(); }
        catch(SQLException ex) {throw new IllegalStateException("Cannot read media",ex);}
    }
    private <T>T write(BranchStore.Work<T> work) {
        try {return store.write(context,work);}
        catch(SQLException ex) {throw new IllegalStateException("Cannot save branch media",ex);}
        catch(IllegalStateException ex) {throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage(),ex);}
        finally {BranchView.invalidate();}
    }
    private static ResponseStatusException invalid(String reason) {return new ResponseStatusException(HttpStatus.CONFLICT,reason);}
}
