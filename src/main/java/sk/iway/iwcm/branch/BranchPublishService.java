package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import sk.iway.iwcm.*;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.perex_groups.PerexGroupsService;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.users.UsersDB;

/** Fixed-order, checkpointed publication on the verified single-node local-filesystem profile. */
public final class BranchPublishService {
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"branch-publisher");thread.setDaemon(true);return thread;});
    private static final Object RUN_LOCK=new Object();
    private static final Set<Long> RUNNING=ConcurrentHashMap.newKeySet();
    private final Consumer<String> afterCommit;
    private Item current;
    private int userId;
    private String domain;
    private long branchId;
    private final EnumMap<BranchType,List<Item>> items=new EnumMap<>(BranchType.class);

    public BranchPublishService() {this(key->{});}
    BranchPublishService(Consumer<String> afterCommit) {this.afterCommit=afterCommit;}

    /** The complete immutable plan must already be committed in MERGING state. */
    public static boolean isRunning(long id) {return RUNNING.contains(id);}

    public void kickoff(long id,int actor) {
        if(!RUNNING.add(id))throw new IllegalStateException("The publisher already owns this package");
        try {EXECUTOR.execute(()->{
            try {new BranchPublishService().runOwned(id,actor);}
            finally {try {SetCharacterEncodingFilter.unRegisterDataContext();}finally {RUNNING.remove(id);}}
        });}
        catch(RuntimeException ex) {RUNNING.remove(id);fail(id,ex);throw ex;}
    }

    /** Also used by database integration tests, with no HTTP request or session ownership. */
    public void run(long id,int actor) {
        if(!RUNNING.add(id))throw new IllegalStateException("The publisher already owns this package");
        try {runOwned(id,actor);}finally {RUNNING.remove(id);}
    }

    private void runOwned(long id,int actor) {
        synchronized(RUN_LOCK) {
            RequestBean previous=SetCharacterEncodingFilter.getCurrentRequestBean();
            branchId=id;userId=actor;current=null;items.clear();
            try(Connection connection=DBPool.getConnection()) {
                BranchReadiness.verify(connection);
                domain=BranchService.getInstance().publication(id).getDomainName();
                var details=UsersDB.getUser(actor);
                if(details==null||!details.isAdmin()||!details.isAuthorized())throw new IllegalStateException("Publishing administrator no longer exists");
                Identity user=new Identity(details);
                RequestBean bean=new RequestBean();bean.setUser(user);bean.setDomain(domain);
                SetCharacterEncodingFilter.setCurrentRequestBean(bean);
                try(BranchContexts.Scope ignored=BranchContexts.live(null,domain,actor)) {
                    BranchMergeService.validatePublisher(id,domain,user);
                    for(BranchType type:BranchType.values()) {
                        List<Item> rows=new ArrayList<>();
                        for(Map<String,Object> row:BranchPublishRows.rows(connection,id,type))rows.add(new Item(type,row));
                        items.put(type,rows);
                    }
                    markNoops();
                    List<Item> groups=new ArrayList<>(items.get(BranchType.GROUP));
                    groups.sort(Comparator.comparingInt(this::groupDepth));
                    for(Item item:groups)if(item.upsert())applyDatabase(item,false);
                    BranchPublishEffects.refreshReaders();
                    List<Item> files=new ArrayList<>(items.get(BranchType.FILE));
                    files.sort(Comparator.comparingInt(item->item.key().length()));
                    for(Item item:files)if(item.upsert())applyFile(item);
                    for(Item item:items.get(BranchType.DOCUMENT))if(item.upsert()) {applyDatabase(item,false);finish(item);}
                    for(Item item:items.get(BranchType.MEDIA))if(item.upsert()) {applyDatabase(item,false);finish(item);}
                    for(Item item:groups)if(item.upsert()) {completeGroupReferences(item);finish(item);}
                    for(Item item:items.get(BranchType.MEDIA))if(item.deleting()) {applyDatabase(item,false);finish(item);}
                    publishDeletes(groups);
                    for(Item item:files)if(item.deleting())applyFile(item);
                    BranchPublishEffects.refresh(rows(BranchType.DOCUMENT),rows(BranchType.FILE));
                    afterCommit.accept("FINAL_EFFECTS");
                    BranchService.getInstance().completePublication(id);
                }
            } catch(Exception ex) {
                if(current!=null)recordItemError(current,ex);
                try {BranchPublishEffects.refreshReaders();}catch(Exception refresh) {ex.addSuppressed(refresh);}
                fail(id,ex);
            } finally {SetCharacterEncodingFilter.setCurrentRequestBean(previous);}
        }
    }

    private List<Map<String,Object>> rows(BranchType type) {return items.getOrDefault(type,List.of()).stream().map(item->item.row).toList();}

    private void markNoops() throws SQLException {
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for(List<Item> list:items.values())for(Item item:list) {
                    current=item;
                    if(item.done()) {requireHash(connection,item,(String)item.row.get("applied_hash"));continue;}
                    if(item.row.get("applied_hash")!=null) {requireHash(connection,item,(String)item.row.get("applied_hash"));continue;}
                    if(item.type==BranchType.FILE&&Objects.toString(item.row.get("apply_phase"),"").startsWith("FILE_"))continue;
                    BranchSnapshot live=BranchPublishRows.liveSnapshot(connection,item.type,item.row);
                    if(live.hash().equals(item.target.hash()) && !"DERIVED".equals(item.row.get("apply_phase")))BranchPublishRows.checkpoint(connection,item.type,item.row,"DONE","NOOP",live.hash(),null);
                }
                connection.commit();
            } catch(Exception ex) {connection.rollback();throw ex;}
            finally {connection.setAutoCommit(true);}
        }
        current=null;
    }

    private void applyDatabase(Item item,boolean subtreePage) throws SQLException {
        if(item.done())return;current=item;
        if(item.row.get("applied_hash")!=null)return;
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BranchPublishRows.lockLive(connection,item.type,item.row);
                requireHash(connection,item,(String)item.row.get("resolved_live_hash"));
                Map<String,Object> live=BranchStore.readLive(connection,item.type,item.id()).orElse(null);
                if(item.deleting()) {
                    if(item.type==BranchType.MEDIA) {
                        execute(connection,"DELETE FROM media_group_to_media WHERE media_id=?",item.id());
                        execute(connection,"DELETE FROM media WHERE media_id=?",item.id());
                    } else if(item.type==BranchType.DOCUMENT) {
                        if(live==null)throw new SQLException("Deleted page disappeared");
                        BranchPublishRows.documentHistory(connection,live,true);
                        Map<String,Object> target=new LinkedHashMap<>(live);target.put("available",false);
                        if(!subtreePage)target.put("group_id",trashId(connection));
                        BranchPublishRows.update(connection,item.type,target);
                    } else throw new SQLException("Folder deletion requires its explicit subtree step");
                } else {
                    Map<String,Object> target=BranchPublishRows.project(connection,item.type,item.row,item.target,live);
                    if(item.type==BranchType.GROUP&&BranchPublishRows.number(target,"default_doc_id")>0
                            &&BranchStore.readLive(connection,BranchType.DOCUMENT,BranchPublishRows.number(target,"default_doc_id")).isEmpty())target.put("default_doc_id",0);
                    if(live==null) {
                        BranchContext context=BranchContext.live(domain,userId);
                        switch(item.type) {
                            case DOCUMENT->BranchRowWriter.insertDocumentWithId(connection,context,target);
                            case GROUP->BranchRowWriter.insertGroupWithId(connection,context,target);
                            case MEDIA->BranchRowWriter.insertMediaWithId(connection,context,target,BranchFields.normalizedIds(item.target.fields().get("group_ids_snapshot")));
                            default->throw new SQLException("Unexpected database type");
                        }
                    } else {
                        BranchPublishRows.update(connection,item.type,target);
                        if(item.type==BranchType.MEDIA) {
                            execute(connection,"DELETE FROM media_group_to_media WHERE media_id=?",item.id());
                            for(int category:BranchFields.normalizedIds(item.target.fields().get("group_ids_snapshot")))
                                execute(connection,"INSERT INTO media_group_to_media(media_id,media_group_id) VALUES (?,?)",item.id(),category);
                        }
                    }
                    if(item.type==BranchType.DOCUMENT) {
                        if(!Constants.getBoolean("editorDisableHistory"))BranchPublishRows.documentHistory(connection,target,false);
                    }
                }
                BranchPublishRows.checkpoint(connection,item.type,item.row,"APPLYING",subtreePage?"TRASH_WAIT":"DATA",BranchPublishRows.liveSnapshot(connection,item.type,item.row).hash(),null);
                connection.commit();
            } catch(Exception ex) {connection.rollback();throw ex;}
            finally {connection.setAutoCommit(true);}
        }
        audit(item);
        afterCommit.accept(item.type+":"+item.key());
    }

    private void completeGroupReferences(Item item) throws SQLException {
        if(item.done())return;current=item;
        if("REFERENCES".equals(item.row.get("apply_phase")))return;
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BranchPublishRows.lockLive(connection,BranchType.GROUP,item.row);
                requireHash(connection,item,(String)item.row.get("applied_hash"));
                Map<String,Object> live=BranchStore.readLive(connection,BranchType.GROUP,item.id()).orElseThrow();
                Object wanted=item.target.fields().get("default_doc_id");
                if(wanted!=null&&((Number)wanted).intValue()>0&&BranchStore.readLive(connection,BranchType.DOCUMENT,((Number)wanted).intValue()).isEmpty())throw new SQLException("Default page is absent");
                live.put("default_doc_id",wanted);BranchPublishRows.update(connection,BranchType.GROUP,live);
                BranchPublishRows.groupHistory(connection,live,userId);
                BranchPublishRows.checkpoint(connection,BranchType.GROUP,item.row,"APPLYING","REFERENCES",BranchPublishRows.liveSnapshot(connection,BranchType.GROUP,item.row).hash(),null);
                connection.commit();
            } catch(Exception ex) {connection.rollback();throw ex;}
            finally {connection.setAutoCommit(true);}
        }
    }

    private void publishDeletes(List<Item> groups) throws SQLException,IOException {
        var actor=UsersDB.getUser(userId);
        if(actor==null)throw new IllegalStateException("Publishing administrator no longer exists");
        BranchMergeService.validatePublisher(branchId,domain,new Identity(actor));
        Set<Integer> deletedGroups=new HashSet<>();
        for(Item group:groups)if(group.deleting()&&!"NOOP".equals(group.row.get("apply_phase")))deletedGroups.add(group.id());
        Map<Integer,Map<String,Object>> liveGroups;
        try(Connection connection=DBPool.getConnection()) {liveGroups=BranchPublishRows.groups(connection);}
        for(Item page:items.get(BranchType.DOCUMENT))if(page.deleting()) {
            int groupId=((Number)page.target.fields().get("group_id")).intValue();
            boolean subtree=hasAncestor(groupId,deletedGroups,liveGroups);
            applyDatabase(page,subtree);
            if(!subtree)finish(page);
        }
        List<Item> roots=groups.stream().filter(Item::deleting).filter(item->!item.done())
            .filter(item->!hasAncestor(BranchPublishRows.number(item.row,"parent_group_id"),deletedGroups,liveGroups)).toList();
        for(Item root:roots) {
            current=root;
            afterCommit.accept("BEFORE_TRASH_ROOT:"+root.id());
            boolean changed=root.row.get("applied_hash")==null;
            try(Connection connection=DBPool.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    Map<Integer,Map<String,Object>> before=BranchPublishRows.groups(connection);
                    Set<Integer> subtree=new HashSet<>();
                    for(int id:deletedGroups)if(id==root.id()||hasAncestor(id,Set.of(root.id()),before))subtree.add(id);
                    for(int id:before.keySet())if(hasAncestor(id,Set.of(root.id()),before)&&!subtree.contains(id))
                        throw new SQLException("A new folder appeared in the prepared deleted subtree: "+id);
                    try(PreparedStatement statement=connection.prepareStatement("SELECT doc_id,group_id FROM documents");ResultSet pages=statement.executeQuery()) {
                        while(pages.next())if(subtree.contains(pages.getInt("group_id"))) {
                            int pageId=pages.getInt("doc_id");
                            if(items.get(BranchType.DOCUMENT).stream().noneMatch(page->page.id()==pageId&&page.target.state()==BranchSnapshot.State.TRASH))
                                throw new SQLException("A new page appeared in the prepared deleted subtree: "+pageId);
                        }
                    }
                    for(Item group:groups)if(subtree.contains(group.id())&&!group.done()) {
                        BranchPublishRows.lockLive(connection,BranchType.GROUP,group.row);
                        requireHash(connection,group,Objects.toString(group.row.get("applied_hash"),(String)group.row.get("resolved_live_hash")));
                    }
                    for(Item page:items.get(BranchType.DOCUMENT))if(page.target.state()==BranchSnapshot.State.TRASH
                            &&subtree.contains(((Number)page.target.fields().get("group_id")).intValue())) {
                        BranchPublishRows.lockLive(connection,BranchType.DOCUMENT,page.row);
                        requireHash(connection,page,Objects.toString(page.row.get("applied_hash"),(String)page.row.get("resolved_live_hash")));
                    }
                    Map<String,Object> live=BranchStore.readLive(connection,BranchType.GROUP,root.id()).orElseThrow();
                    if(root.row.get("applied_hash")==null) {
                        BranchPublishRows.groupHistory(connection,live,userId);
                        live.put("parent_group_id",trashId(connection));BranchPublishRows.update(connection,BranchType.GROUP,live);
                    }
                    for(Item group:groups)if(subtree.contains(group.id())&&!group.done())
                        BranchPublishRows.checkpoint(connection,BranchType.GROUP,group.row,"APPLYING","TRASH_ROOT",BranchPublishRows.liveSnapshot(connection,BranchType.GROUP,group.row).hash(),null);
                    for(Item page:items.get(BranchType.DOCUMENT))if(page.deleting()&&subtree.contains(((Number)page.target.fields().get("group_id")).intValue())&&!page.done())
                        BranchPublishRows.checkpoint(connection,BranchType.DOCUMENT,page.row,"APPLYING","TRASH_ROOT",BranchPublishRows.liveSnapshot(connection,BranchType.DOCUMENT,page.row).hash(),null);
                    connection.commit();
                } catch(Exception ex) {connection.rollback();throw ex;}
                finally {connection.setAutoCommit(true);}
            }
            if(changed) audit(root);
            afterCommit.accept("TRASH_ROOT:"+root.id());
        }
        for(Item page:items.get(BranchType.DOCUMENT))if(page.deleting())finish(page);
        for(Item group:groups)if(group.deleting())finish(group);
    }

    private void applyFile(Item item) throws SQLException,IOException {
        if(item.done())return;current=item;
        String path=item.key();
        try(Connection connection=DBPool.getConnection()) {BranchMergeService.checkFilePlans(connection,domain,List.of(path));}
        BranchSnapshot live=BranchFiles.liveSnapshot(path);
        String phase=Objects.toString(item.row.get("apply_phase"),"");
        if(item.row.get("applied_hash")!=null) {
            if(!live.hash().equals(item.row.get("applied_hash")))throw new IllegalStateException("File changed after publication: "+path);
            finish(item);return;
        }
        boolean recovering=phase.startsWith("FILE_")&&live.hash().equals(item.target.hash());
        if(!recovering&&!live.hash().equals(item.row.get("resolved_live_hash")))throw new IllegalStateException("File production changed: "+path);
        Path destination=Path.of(Tools.getRealPath(path));
        if(!recovering) {
            Path backup=BranchFiles.workPath(branchId,item.rowId()).resolveSibling("publish-backup");
            Files.createDirectories(backup.getParent());
            if(Files.isRegularFile(destination)&&!Files.exists(backup)) {
                Files.copy(destination,backup,StandardCopyOption.COPY_ATTRIBUTES);
                if(!BranchFiles.hash(backup).equals(live.fields().get("content_hash")))throw new IOException("File changed while preserving its original: "+path);
            }
            if(!phase.startsWith("FILE_")&&Files.isRegularFile(destination)&&IwcmFsDB.useVersioning()
                    &&!IwcmFsDB.saveFileHistory(new IwcmFile(destination.toFile()),false))
                throw new IOException("Cannot save the original file to history: "+path);
            try(Connection connection=DBPool.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    BranchPublishRows.checkpoint(connection,BranchType.FILE,item.row,"APPLYING","FILE_STARTED",null,null);connection.commit();
                }
                catch(Exception ex) {connection.rollback();throw ex;}
                finally {connection.setAutoCommit(true);}
            }
            requireUnchangedFile(item);
            if(item.target.state()==BranchSnapshot.State.ABSENT)Files.deleteIfExists(destination);
            else if("DIRECTORY".equals(item.target.fields().get("entry_kind")))Files.createDirectories(destination);
            else {
                Path source=BranchFiles.workPath(branchId,item.rowId());
                if(!Files.isRegularFile(source)||!BranchFiles.hash(source).equals(item.target.fields().get("content_hash")))throw new IOException("Working bytes changed: "+path);
                Files.createDirectories(destination.getParent());
                Path temporary=Files.createTempFile(destination.getParent(),".branch-publish-",".tmp");
                try {
                    Files.copy(source,temporary,StandardCopyOption.REPLACE_EXISTING);
                    if(!BranchFiles.hash(temporary).equals(item.target.fields().get("content_hash")))throw new IOException("Publication temporary file is corrupt");
                    requireUnchangedFile(item);
                    Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                } finally {Files.deleteIfExists(temporary);}
            }
            afterCommit.accept("FILE_REPLACED:"+path);
        }
        if(!BranchFiles.liveSnapshot(path).hash().equals(item.target.hash()))throw new IOException("File target verification failed: "+path);
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {BranchPublishRows.checkpoint(connection,BranchType.FILE,item.row,"APPLYING","DATA",item.target.hash(),null);connection.commit();}
            catch(Exception ex) {connection.rollback();throw ex;}
            finally {connection.setAutoCommit(true);}
        }
        audit(item);
        finish(item);
    }

    private void requireUnchangedFile(Item item) throws SQLException {
        try(Connection connection=DBPool.getConnection()) {BranchMergeService.checkFilePlans(connection,domain,List.of(item.key()));}
        if(!BranchFiles.liveSnapshot(item.key()).hash().equals(item.row.get("resolved_live_hash")))
            throw new IllegalStateException("File production changed before replacement: "+item.key());
    }

    private void finish(Item item) throws SQLException,IOException {
        if(item.done())return;current=item;
        if(item.row.get("applied_hash")==null)throw new SQLException("Item data step is incomplete: "+item.key());
        if(item.type==BranchType.DOCUMENT&&Constants.getBoolean("perexGroupUseJoin")) {
            int[] categories;
            try(Connection connection=DBPool.getConnection()) {
                requireHash(connection,item,(String)item.row.get("applied_hash"));
                var document=BranchStore.readLive(connection,BranchType.DOCUMENT,item.id()).orElseThrow();
                categories=BranchFields.normalizedIds(document.get("perex_group")).stream().mapToInt(Integer::intValue).toArray();
            }
            PerexGroupsService.savePerexGroupsDoc(item.id(),categories);
        }
        BranchPublishEffects.refresh(item.type==BranchType.DOCUMENT?List.of(item.row):List.of(),item.type==BranchType.FILE?List.of(item.row):List.of());
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {requireHash(connection,item,(String)item.row.get("applied_hash"));BranchPublishRows.checkpoint(connection,item.type,item.row,"DONE","EFFECTS",(String)item.row.get("applied_hash"),null);connection.commit();}
            catch(Exception ex) {connection.rollback();throw ex;}
            finally {connection.setAutoCommit(true);}
        }
        current=null;
    }

    private void requireHash(Connection connection,Item item,String expected) throws SQLException {
        String actual=BranchPublishRows.liveSnapshot(connection,item.type,item.row).hash();
        if(expected==null||!expected.equals(actual))throw new IllegalStateException("Production changed; compare again: "+item.type+":"+item.key());
    }

    private void audit(Item item) {
        int type=switch(item.type) {case DOCUMENT->Adminlog.TYPE_PAGE_UPDATE;case GROUP->Adminlog.TYPE_GROUP;case MEDIA->Adminlog.TYPE_PAGE_UPDATE;case FILE->item.deleting()?Adminlog.TYPE_FILE_DELETE:Adminlog.TYPE_FILE_UPLOAD;};
        Adminlog.add(type,userId,"Publishing package #"+branchId+": "+item.type+":"+item.key(),item.type==BranchType.FILE?0:item.id(),(int)branchId,new Timestamp(System.currentTimeMillis()));
    }

    private int trashId(Connection connection) throws SQLException {
        int trash=BranchMergeService.existingTrash(BranchPublishRows.groups(connection),domain);
        if(trash>0)return trash;
        throw new SQLException("Prepare the existing Trash folder before publishing deletions");
    }

    private int groupDepth(Item item) {
        int depth=0,id=((Number)item.target.fields().getOrDefault("parent_group_id",0)).intValue();Set<Integer> visited=new HashSet<>();
        while(id>0&&visited.add(id)) {
            depth++;final int parent=id;
            Item found=items.get(BranchType.GROUP).stream().filter(group->group.id()==parent&&group.upsert()).findFirst().orElse(null);
            if(found==null)break;id=((Number)found.target.fields().get("parent_group_id")).intValue();
        }
        return depth;
    }

    private static boolean hasAncestor(int id,Set<Integer> ancestors,Map<Integer,Map<String,Object>> groups) {
        Set<Integer> visited=new HashSet<>();
        while(id>0&&visited.add(id)) {if(ancestors.contains(id))return true;Map<String,Object> row=groups.get(id);if(row==null)break;id=BranchPublishRows.number(row,"parent_group_id");}
        return false;
    }

    private void recordItemError(Item item,Exception error) {
        try(Connection connection=DBPool.getConnection()) {
            Map<String,Object> actual=BranchStore.readWork(connection,branchId,item.type,item.key()).orElseThrow();
            if(!"DONE".equals(actual.get("apply_state")))
                BranchPublishRows.checkpoint(connection,item.type,actual,"ERROR",(String)actual.get("apply_phase"),(String)actual.get("applied_hash"),error.getMessage());
        } catch(Exception recording) {error.addSuppressed(recording);}
    }

    private static void fail(long id,Exception error) {
        Logger.error(BranchPublishService.class,error);
        try(Connection connection=DBPool.getConnection()) {
            boolean applied=false;
            for(BranchType type:BranchType.values())try(PreparedStatement check=connection.prepareStatement("SELECT 1 FROM `"+type.workTable()+"` WHERE branch_id=? AND ((applied_hash IS NOT NULL AND COALESCE(apply_phase,'')<>'NOOP') OR apply_phase LIKE 'FILE_%') LIMIT 1")) {
                check.setLong(1,id);try(ResultSet result=check.executeQuery()){applied|=result.next();}
            }
            String reason=error instanceof org.springframework.web.server.ResponseStatusException response?Objects.toString(response.getReason(),sk.iway.iwcm.i18n.Prop.getInstance().getText("branches.schedule.permission")):Objects.toString(error.getMessage(),error.getClass().getSimpleName());
            BranchService.getInstance().failPublication(id,applied,reason);
        } catch(Exception recording) {Logger.error(BranchPublishService.class,recording);}
    }

    private static void execute(Connection connection,String sql,Object... values) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement(sql)) {for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);statement.executeUpdate();}
    }

    private static final class Item {
        private final BranchType type;
        private final Map<String,Object> row;
        private final BranchSnapshot target;
        private Item(BranchType type,Map<String,Object> row) {this.type=type;this.row=row;target=BranchSnapshot.fromJson((String)row.get("resolved_snapshot"));}
        private boolean done() {return "DONE".equals(row.get("apply_state"));}
        private boolean upsert() {return !done()&&target.state()==BranchSnapshot.State.ACTIVE;}
        private boolean deleting() {return !done()&&target.state()!=BranchSnapshot.State.ACTIVE;}
        private int id() {return BranchPublishRows.number(row,type.idColumn());}
        private long rowId() {return ((Number)row.get("row_id")).longValue();}
        private String key() {return type==BranchType.FILE?(String)row.get("logical_path"):Integer.toString(id());}
    }
}
