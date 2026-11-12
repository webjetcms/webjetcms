package sk.iway.iwcm.branch;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/** Whole-object comparison, complete preflight and atomic claim of a publishing package. */
public final class BranchMergeService {
    public record Version(String state, Map<String,Object> fields, String hash) {
        static Version of(BranchSnapshot value) { return new Version(value.state().name(),value.fields(),value.hash()); }
    }
    public record Item(String type,String objectKey,String label,String operation,Version base,Version live,Version work,
            String liveHash,String workHash,String choice,boolean conflict,boolean writeRequired,
            String applyState,String applyPhase,String error,boolean editable,boolean resolvable) { }
    public record Report(long id,String name,String domainName,String state,long revision,String preparationHash,
            boolean ready,List<String> errors,List<Item> items,String lastError,Map<String,Map<String,String>> fieldLabels,Long publishAt,Integer scheduledBy,String timeZone) { }
    private record Entry(BranchType type,Map<String,Object> row,BranchChange change,BranchSnapshot live,
            BranchSnapshot target,boolean conflict,boolean write,String error) { }
    private record Plan(Map<String,Object> branch,List<Entry> entries,List<String> errors,String hash) { }

    public Report prepare(long id,String domain,Identity user,long revision) {
        try(Connection c=DBPool.getConnection()) { return report(plan(c,id,domain,user,revision)); }
        catch(SQLException ex) { throw failure(ex); }
    }

    public Report detail(long id,String domain,Identity user) { return prepare(id,domain,user,-1); }

    public Report resolve(long id,String domain,Identity user,long revision,BranchType type,String key,
            BranchDecision.Source source,String liveHash,String workHash) {
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> branch=branch(c,id,domain,revision,true);
                String state=Objects.toString(branch.get("state"));
                if(!Set.of("DRAFT","MERGE_FAILED").contains(state)) throw conflict("Balík už nemožno meniť.");
                Map<String,Object> row=BranchStore.readWork(c,id,type,key).orElseThrow(()->conflict("Položka už neexistuje."));
                if(!"DRAFT".equals(state) && (!Set.of("PENDING","ERROR").contains(row.get("apply_state")) || row.get("applied_hash")!=null))
                    throw conflict("Položka už má produkčný zápis; vyžaduje kontrolu správcu.");
                authorize(c,type,row,user,domain,false);
                BranchChange change=BranchStore.change(type,row);
                BranchSnapshot live=BranchPublishRows.liveSnapshot(c,type,row);
                if(type!=BranchType.FILE && change.base().state()==BranchSnapshot.State.ABSENT && live.state()!=BranchSnapshot.State.ABSENT && row.get("applied_hash")==null)throw conflict("Rezervované ID už obsadil cudzí produkčný riadok.");
                if(!live.hash().equals(liveHash)||!change.work().hash().equals(workHash)) throw conflict("Podklad sa zmenil. Obnovte porovnanie.");
                BranchSnapshot target=source==BranchDecision.Source.LIVE?live:change.work();
                try(PreparedStatement s=c.prepareStatement("UPDATE `"+type.workTable()+"` SET resolved_choice=?,resolved_live_hash=?,resolved_work_hash=?,resolved_snapshot=?,apply_error=NULL WHERE row_id=? AND branch_id=?")) {
                    s.setString(1,source.name());s.setString(2,liveHash);s.setString(3,workHash);s.setString(4,target.json());s.setObject(5,row.get("row_id"));s.setLong(6,id);s.executeUpdate();
                }
                bump(c,id,user.getUserId());c.commit();
            } catch(SQLException|RuntimeException ex) {c.rollback();throw ex;}
        } catch(SQLException ex) {throw failure(ex);}
        BranchView.invalidate();
        return detail(id,domain,user);
    }

    /** Persists the complete reviewed plan together with the conditional state transition. */
    public Report claim(long id,String domain,Identity user,long revision,String hash,boolean resume) {
        return claim(id,domain,user,revision,hash,resume,false,false);
    }

    /** Commits the immutable plan and its publication request together for the fixed cron job. */
    public Report enqueue(long id,String domain,Identity user,long revision,String hash,boolean resume) {
        return claim(id,domain,user,revision,hash,resume,false,true);
    }

    Report claimScheduled(long id,String domain,Identity user,long revision,String hash) {
        return claim(id,domain,user,revision,hash,false,true,false);
    }

    private Report claim(long id,String domain,Identity user,long revision,String hash,boolean resume,boolean scheduled,boolean queued) {
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> branch=branch(c,id,domain,revision,true);
                String state=Objects.toString(branch.get("state"));
                Set<String> allowed=Set.of("DRAFT");
                if(scheduled)allowed=Set.of("SCHEDULED");
                else if(resume)allowed=Set.of("MERGE_FAILED","MERGING");
                if(!allowed.contains(state)) throw conflict("Stav balíka sa zmenil. Obnovte priebeh.");
                if(scheduled && (!(branch.get("publish_at") instanceof java.util.Date at) || at.getTime()>System.currentTimeMillis()
                        || !Objects.equals(branch.get("scheduled_by"),user.getUserId()))) throw conflict("Termín alebo publikujúci sa zmenil.");
                if("MERGING".equals(state) && (branch.get("publish_requested_by")!=null||BranchPublishService.isRunning(id))) throw conflict("Publikovanie už prebieha alebo čaká na úlohu na pozadí.");
                Plan plan=plan(c,id,domain,user,revision);
                if(!plan.errors.isEmpty()||plan.entries.stream().anyMatch(Entry::conflict)) {
                    List<String> reasons=new ArrayList<>(plan.errors);
                    plan.entries.stream().filter(Entry::conflict).forEach(e->reasons.add(e.type+":"+e.row.get(e.type.idColumn())+(e.error==null?"":": "+e.error)));
                    throw conflict(String.join("; ",reasons)+" Obnovte porovnanie a vyriešte konflikty.");
                }
                if(!Objects.equals(hash,plan.hash)) throw conflict("Podklad porovnania sa zmenil. Znova overte balík.");
                for(Entry e:plan.entries) {
                    if(resume) continue;
                    try(PreparedStatement s=c.prepareStatement("UPDATE `"+e.type.workTable()+"` SET resolved_snapshot=?,resolved_live_hash=?,resolved_work_hash=?,apply_state='PENDING',apply_phase=?,applied_hash=NULL,apply_error=NULL WHERE row_id=? AND branch_id=?")) {
                        s.setString(1,e.target.json());s.setString(2,e.live.hash());s.setString(3,e.change.work().hash());s.setString(4,Boolean.TRUE.equals(e.row.get("_derived_write"))?"DERIVED":null);s.setObject(5,e.row.get("row_id"));s.setLong(6,id);s.executeUpdate();
                    }
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE branches SET state='MERGING',revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP,merge_started_at=COALESCE(merge_started_at,CURRENT_TIMESTAMP),last_error=NULL,publish_requested_by=? WHERE id=? AND revision=? AND state=?")) {
                    s.setInt(1,user.getUserId());s.setObject(2,queued?user.getUserId():null);s.setLong(3,id);s.setLong(4,revision);s.setString(5,state);
                    if(s.executeUpdate()!=1) throw conflict("Balík už prevzal iný beh.");
                }
                c.commit();
            } catch(SQLException|RuntimeException ex) {c.rollback();throw ex;}
        } catch(SQLException ex) {throw failure(ex);}
        return detail(id,domain,user);
    }

    /** Scheduling and moving a term use one locked package update; individual plans are untouched. */
    public Report schedule(long id,String domain,Identity user,long revision,String hash,long publishAt) {
        if(publishAt<=System.currentTimeMillis()) throw conflict(Prop.getInstance().getText("branches.schedule.future"));
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> branch=branch(c,id,domain,revision,true);
                if(!Set.of("DRAFT","SCHEDULED").contains(branch.get("state"))) throw conflict("Stav balíka sa zmenil. Obnovte priebeh.");
                Plan plan=plan(c,id,domain,user,revision);
                if(!plan.errors.isEmpty()) throw conflict(String.join("; ",plan.errors));
                if(plan.entries.stream().anyMatch(Entry::conflict)) throw conflict(Prop.getInstance().getText("branches.schedule.conflict"));
                if(!Objects.equals(hash,plan.hash)) throw conflict("Podklad porovnania sa zmenil. Znova overte balík.");
                try(PreparedStatement s=c.prepareStatement("UPDATE branches SET state='SCHEDULED',publish_at=?,scheduled_by=?,revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP,last_error=NULL WHERE id=? AND revision=?")) {
                    s.setTimestamp(1,new Timestamp(publishAt));s.setInt(2,user.getUserId());s.setInt(3,user.getUserId());s.setLong(4,id);s.setLong(5,revision);
                    if(s.executeUpdate()!=1)throw conflict("Balík sa zmenil. Obnovte porovnanie.");
                }
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }catch(SQLException ex){throw failure(ex);}
        BranchView.invalidate();return detail(id,domain,user);
    }

    public Report unschedule(long id,String domain,Identity user,long revision) {return returnToDraft(id,domain,user,revision,"SCHEDULED");}
    public Report reopen(long id,String domain,Identity user,long revision) {return returnToDraft(id,domain,user,revision,"NEEDS_ATTENTION");}

    private Report returnToDraft(long id,String domain,Identity user,long revision,String state) {
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> branch=branch(c,id,domain,revision,true);
                if(!state.equals(branch.get("state")))throw conflict("Stav balíka sa zmenil. Obnovte priebeh.");
                for(BranchType type:BranchType.values())for(Map<String,Object> row:BranchPublishRows.rows(c,id,type)) {
                    authorize(c,type,row,user,domain,false);
                    if(row.get("applied_hash")!=null && !"NOOP".equals(row.get("apply_phase")))throw conflict("Balík už obsahuje produkčný zápis.");
                    try(PreparedStatement s=c.prepareStatement("UPDATE `"+type.workTable()+"` SET apply_state='PENDING',apply_phase=NULL,applied_hash=NULL,apply_error=NULL WHERE row_id=? AND branch_id=?")) {
                        s.setObject(1,row.get("row_id"));s.setLong(2,id);s.executeUpdate();
                    }
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE branches SET state='DRAFT',publish_at=NULL,scheduled_by=NULL,revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP,merge_started_at=NULL,last_error=NULL WHERE id=? AND revision=? AND state=?")) {
                    s.setInt(1,user.getUserId());s.setLong(2,id);s.setLong(3,revision);s.setString(4,state);
                    if(s.executeUpdate()!=1)throw conflict("Balík sa zmenil. Obnovte porovnanie.");
                }
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }catch(SQLException ex){throw failure(ex);}
        BranchView.invalidate();return detail(id,domain,user);
    }

    static void validatePublisher(long id,String domain,Identity user) {
        Report report=new BranchMergeService().detail(id,domain,user);
        if(!report.errors().isEmpty() || report.items().stream().anyMatch(Item::conflict))
            throw conflict(String.join("; ",report.errors())+" Zmenený podklad publikovania.");
    }

    private Plan plan(Connection c,long id,String domain,Identity user,long revision)throws SQLException {
        BranchReadiness.verify(c);
        Map<String,Object> branch=branch(c,id,domain,revision,false);
        if(user==null || !GroupsDB.getInstance().getUserRootDomainNames(user).contains(domain))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,Prop.getInstance().getText("branches.schedule.permission"));
        boolean frozen=!Set.of("DRAFT","SCHEDULED","NEEDS_ATTENTION").contains(branch.get("state"));
        List<Entry> entries=new ArrayList<>();List<String> errors=new ArrayList<>();
        StringBuilder fingerprint=new StringBuilder(id+":"+branch.get("revision")+":"+branch.get("state"));
        for(BranchType type:BranchType.values()) for(Map<String,Object> row:BranchPublishRows.rows(c,id,type)) {
            try {authorize(c,type,row,user,domain,false);}
            catch(ResponseStatusException ex) {throw new ResponseStatusException(ex.getStatusCode(),type+":"+row.get(type.idColumn())+": "+Objects.toString(ex.getReason(),Prop.getInstance().getText("branches.schedule.permission")));}
            BranchChange change=BranchStore.change(type,row);BranchSnapshot live=BranchPublishRows.liveSnapshot(c,type,row);
            BranchSnapshot target;boolean conflict=false,write=false;String error=null;
            if(frozen && row.get("resolved_snapshot")!=null) {
                target=BranchSnapshot.fromJson((String)row.get("resolved_snapshot"));
                String expected=Objects.toString(row.get("applied_hash"),Objects.toString(row.get("resolved_live_hash"),""));
                if(!Set.of("MERGED","CANCELLED").contains(branch.get("state")) && !live.hash().equals(expected)
                        && !(type==BranchType.FILE && row.get("applied_hash")==null && Objects.toString(row.get("apply_phase"),"").startsWith("FILE_") && live.hash().equals(target.hash()))) {
                    conflict=true;error="Produkcia sa zmenila po začatí publikovania; položku nemožno prepísať.";
                }
                write=!"DONE".equals(row.get("apply_state"))&&!live.hash().equals(target.hash());
            } else {
                try {BranchDecision.Result decision=change.decide(live);conflict=decision.conflict();write=decision.writeRequired();target=decision.source()==BranchDecision.Source.LIVE?live:change.work();}
                catch(BranchDecision.StaleChoiceException ex) {target=change.work();conflict=true;error="Voľba je zastaraná. Znova porovnajte a vyberte výsledok.";}
            }
            if(change.base().state()==BranchSnapshot.State.ABSENT && live.state()!=BranchSnapshot.State.ABSENT && row.get("applied_hash")==null && type!=BranchType.FILE) {
                conflict=true;error="Rezervované ID alebo nová cesta už existuje v produkcii.";
            }
            Entry e=new Entry(type,row,change,live,target,conflict,write,error);entries.add(e);
            fingerprint.append('|').append(change.identity()).append(':').append(live.hash()).append(':').append(change.work().hash()).append(':').append(target.hash()).append(':').append(row.get("apply_state")).append(':').append(row.get("apply_phase"));
            if(error!=null) errors.add(change.identity()+": "+error);
        }
        if(!frozen && !entries.isEmpty() && entries.stream().noneMatch(e->e.error!=null)) {
            try(BranchContexts.Scope ignored=BranchContexts.open(null,new BranchContext(id,domain,user.getUserId(),BranchContext.Mode.PREVIEW))) {
                BranchView view=BranchView.reload(c);
                for(int i=0;i<entries.size();i++) {
                    Entry e=entries.get(i);
                    if(e.type==BranchType.DOCUMENT && e.target.state()==BranchSnapshot.State.ACTIVE
                            && (e.write || view.derivedDocumentRows().containsKey(Integer.parseInt(e.change.objectKey())))) {
                        DocDetails effective=view.document(Integer.parseInt(e.change.objectKey()),false);
                        if(effective!=null) {
                            Map<String,Object> fields=new LinkedHashMap<>(e.target.fields());fields.put("virtual_path",effective.getVirtualPath());
                            BranchSnapshot target=new BranchSnapshot(e.target.state(),fields);
                            Map<String,Object> projected=view.derivedDocumentRows().get(Integer.parseInt(e.change.objectKey()));
                            if(projected==null)projected=view.documentRows().stream().filter(r->number(r,"doc_id")==Integer.parseInt(e.change.objectKey())).findFirst().orElse(null);
                            Map<String,Object> current=BranchStore.readLive(c,BranchType.DOCUMENT,Integer.parseInt(e.change.objectKey())).orElse(null);
                            boolean derivedWrite=current!=null && projected!=null && !derivedFields(current,projected.keySet()).hash().equals(derivedFields(projected,projected.keySet()).hash());
                            if(derivedWrite)e.row.put("_derived_write",true);
                            boolean write=e.write || !target.hash().equals(e.live.hash()) || derivedWrite;
                            entries.set(i,new Entry(e.type,e.row,e.change,e.live,target,e.conflict,write,e.error));
                            fingerprint.append(":derived:").append(e.change.identity()).append(':').append(target.hash()).append(':').append(derivedWrite);
                        }
                    }
                }
            } finally {BranchView.invalidate();}
        }
        // The exact permissions are rechecked even for LIVE/no-op rows of the reviewed package.
        for(Entry e:entries) try {authorize(c,e.type,e.row,user,domain,true);}
        catch(ResponseStatusException ex) {errors.add(e.change.identity()+": "+ex.getReason());}
        try {validate(c,entries,domain,user,frozen);}
        catch(IllegalArgumentException|IllegalStateException ex) {errors.add(ex.getMessage());}
        if(entries.isEmpty()) errors.add("Balík neobsahuje žiadne zmeny.");
        String hash=new BranchSnapshot(BranchSnapshot.State.ACTIVE,Map.of("inputs",fingerprint.toString())).hash();
        return new Plan(branch,entries,List.copyOf(errors),hash);
    }

    private Report report(Plan p) {
        List<Item> items=p.entries.stream().map(e->new Item(e.type.name(),e.change.objectKey(),label(e),e.change.operation().name(),Version.of(e.change.base()),Version.of(e.live),Version.of(e.change.work()),e.live.hash(),e.change.work().hash(),Objects.toString(e.row.get("resolved_choice"),null),e.conflict,e.write,Objects.toString(e.row.get("apply_state"),"PENDING"),Objects.toString(e.row.get("apply_phase"),null),e.error!=null?e.error:Objects.toString(e.row.get("apply_error"),null),"DRAFT".equals(p.branch.get("state")),"DRAFT".equals(p.branch.get("state")) || "MERGE_FAILED".equals(p.branch.get("state")) && e.row.get("applied_hash")==null && Set.of("PENDING","ERROR").contains(e.row.get("apply_state")))).toList();
        return new Report(((Number)p.branch.get("id")).longValue(),Objects.toString(p.branch.get("name")),BranchDomains.name(((Number)p.branch.get("domain_id")).intValue()),Objects.toString(p.branch.get("state")),((Number)p.branch.get("revision")).longValue(),p.hash,p.errors.isEmpty()&&items.stream().noneMatch(Item::conflict),p.errors,items,Objects.toString(p.branch.get("last_error"),null),BranchMergeLabels.translate(Prop.getInstance()),p.branch.get("publish_at") instanceof java.util.Date at?at.getTime():null,(Integer)p.branch.get("scheduled_by"),java.time.ZoneId.systemDefault().getId());
    }

    private static BranchSnapshot derivedFields(Map<String,Object> row,Set<String> projected) {
        Map<String,Object> fields=new LinkedHashMap<>();
        for(String field:BranchFields.DOCUMENT.derived())if(projected.contains(field))fields.put(field,row.get(field));
        return new BranchSnapshot(BranchSnapshot.State.ACTIVE,fields);
    }

    private static String label(Entry e) {
        for(String key:List.of("title","group_name","media_title_sk","logical_path")) if(e.row.get(key)!=null) return e.row.get(key).toString();
        return e.change.identity();
    }

    private void validate(Connection c,List<Entry> entries,String domain,Identity user,boolean frozen)throws SQLException {
        Map<Integer,Map<String,Object>> liveGroups=table(c,"groups","group_id"),groups=new LinkedHashMap<>(liveGroups);
        Map<Integer,Map<String,Object>> liveDocs=table(c,"documents","doc_id"),docs=new LinkedHashMap<>(liveDocs);
        Map<Integer,Map<String,Object>> media=table(c,"media","media_id");
        Set<Integer> changedGroups=new HashSet<>(),changedDocs=new HashSet<>();List<String> paths=new ArrayList<>();
        for(Entry e:entries) {
            if(e.type==BranchType.FILE) {validateFile(e,user);paths.add(e.change.objectKey());continue;}
            int id=Integer.parseInt(e.change.objectKey());
            Map<Integer,Map<String,Object>> rows=switch(e.type){case GROUP->groups;case DOCUMENT->docs;case MEDIA->media;default->throw new IllegalStateException();};
            if(e.type==BranchType.GROUP) changedGroups.add(id);if(e.type==BranchType.DOCUMENT) changedDocs.add(id);
            if(e.target.state()!=BranchSnapshot.State.ACTIVE) rows.remove(id);
            else {Map<String,Object> target=new LinkedHashMap<>(e.row);target.putAll(e.target.fields());target.put(e.type.idColumn(),id);rows.put(id,target);}
            if(e.type==BranchType.GROUP && e.change.base().state()!=BranchSnapshot.State.ABSENT && structural(e)) {
                Set<Integer> subtree=descendants(liveGroups,id);
                for(int child:subtree) if(entries.stream().noneMatch(x->x.type==BranchType.GROUP&&x.change.objectKey().equals(Integer.toString(child)))) throw new IllegalStateException("Nový potomok priečinka vyžaduje nové pracovné posúdenie: "+child);
                for(var d:liveDocs.values()) if(subtree.contains(number(d,"group_id"))&&!changed(entries,BranchType.DOCUMENT,number(d,"doc_id"))) throw new IllegalStateException("Nová stránka pod priečinkom vyžaduje nové posúdenie: "+d.get("doc_id"));
            }
        }
        for(Entry e:entries) {
            if(e.type==BranchType.GROUP&&e.target.state()==BranchSnapshot.State.TRASH || e.type==BranchType.DOCUMENT&&e.target.state()==BranchSnapshot.State.TRASH) {
                if(existingTrash(liveGroups,domain)==0) throw new IllegalStateException("Chýba existujúci kôš domény; pripravte ho bežnou administráciou.");
            }
            if(!"DONE".equals(e.row.get("apply_state"))) checkIndividualPlans(c,e);
        }
        checkFilePlans(c,domain,paths);
        for(int id:changedGroups) if(groups.containsKey(id)) {
            Map<String,Object> g=groups.get(id);Set<Integer> seen=new HashSet<>();int ancestor=id;
            while(ancestor>0) {if(!seen.add(ancestor)) throw new IllegalStateException("Cyklus priečinkov: "+id);Map<String,Object> parent=groups.get(ancestor);if(parent==null)throw new IllegalStateException("Chýbajúci rodič priečinka "+ancestor);ancestor=number(parent,"parent_group_id");}
            int def=number(g,"default_doc_id");if(def>0&&(!docs.containsKey(def)||number(docs.get(def),"group_id")!=id))throw new IllegalStateException("Neplatná hlavná stránka priečinka "+id);
            for(String field:List.of("logon_page_doc_id","new_page_docid_template")) requireReference(docs,number(g,field),"priečinok "+id);
            int link=number(g,"link_group_id");if(link>0&&!groups.containsKey(link))throw new IllegalStateException("Chýbajúci prepojený priečinok "+link);
            template(c,number(g,"temp_id"));
        }
        for(int id:changedDocs) if(docs.containsKey(id)) {
            Map<String,Object> d=docs.get(id);if(!groups.containsKey(number(d,"group_id")))throw new IllegalStateException("Chýbajúci priečinok stránky "+id);template(c,number(d,"temp_id"));
            for(String field:List.of("header_doc_id","footer_doc_id","menu_doc_id","right_menu_doc_id","logon_page_doc_id","temp_field_a_docid","temp_field_b_docid","temp_field_c_docid","temp_field_d_docid"))requireReference(docs,number(d,field),"stránka "+id);
            for(int category:BranchFields.normalizedIds(d.get("perex_group"))) {
                boolean allowed=perexAvailable(c,category,number(d,"group_id"),groups);
                if(!allowed)throw new IllegalStateException("Nedostupná perex skupina "+category+" pre stránku "+id);
            }
        }
        // Removing a target must not leave references from unchanged pages/folders/media.
        for(var d:docs.values()) for(String field:List.of("header_doc_id","footer_doc_id","menu_doc_id","right_menu_doc_id","logon_page_doc_id","temp_field_a_docid","temp_field_b_docid","temp_field_c_docid","temp_field_d_docid")) {
            int ref=number(d,field);if(ref>0&&liveDocs.containsKey(ref)&&!docs.containsKey(ref))throw new IllegalStateException("Stránka "+d.get("doc_id")+" odkazuje na mazanú stránku "+ref);
        }
        for(var g:groups.values()) {
            for(String field:List.of("default_doc_id","logon_page_doc_id","new_page_docid_template")) {
                int ref=number(g,field);if(ref>0&&liveDocs.containsKey(ref)&&!docs.containsKey(ref))throw new IllegalStateException("Priečinok "+g.get("group_id")+" odkazuje na mazanú stránku "+ref);
            }
            int link=number(g,"link_group_id");if(link>0&&liveGroups.containsKey(link)&&!groups.containsKey(link))throw new IllegalStateException("Priečinok odkazuje na mazaný priečinok "+link);
        }
        for(var m:media.values()) if("documents".equals(m.get("media_fk_table_name"))&&!docs.containsKey(number(m,"media_fk_id"))&&changed(entries,BranchType.MEDIA,number(m,"media_id")))throw new IllegalStateException("Médium nemá výslednú vlastnícku stránku.");
        for(Entry e:entries) if(e.type==BranchType.MEDIA&&e.target.state()==BranchSnapshot.State.ACTIVE)for(int category:BranchFields.normalizedIds(e.target.fields().get("group_ids_snapshot"))) {
            try(PreparedStatement s=c.prepareStatement("SELECT available_groups FROM media_groups WHERE media_group_id=?")) {
                s.setInt(1,category);
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next())throw new IllegalStateException("Zaniknutá mediálna kategória "+category);
                    int owner=number(e.target.fields(),"media_fk_id");
                    Map<String,Object> ownerPage=docs.get(owner);
                    if(ownerPage==null || !folderAllowed(r.getString(1),number(ownerPage,"group_id"),groups))throw new IllegalStateException("Mediálna kategória nie je dostupná vo výslednom priečinku: "+category);
                }
            }
        }
        if(frozen) {
            Map<String,Integer> urls=new HashMap<>();
            Map<Integer,GroupDetails> folderBeans=new HashMap<>();
            for(var g:groups.entrySet())try(var result=BranchRows.result(g.getValue())) {folderBeans.put(g.getKey(),GroupsDB.fillFieldsByResultSet(result,false));}
            for(var d:docs.values()) {
                if(!groups.containsKey(number(d,"group_id")) || !domain.equals(GroupsDB.getURLPathDomainGroup(folderBeans,number(d,"group_id"))[1])) continue;
                String url=Objects.toString(DocDB.normalizeVirtualPath(Objects.toString(d.get("virtual_path"),"")),"").toLowerCase(Locale.ROOT);
                if(url.isBlank())continue;Integer previous=urls.putIfAbsent(url,number(d,"doc_id"));
                if(previous!=null&&(changedDocs.contains(previous)||changedDocs.contains(number(d,"doc_id"))))throw new IllegalStateException("Kolízia URL "+url);
            }
        }
        // Use the same effective URL derivation as the real preview, after stale choices were detected.
        if(!frozen && entries.stream().noneMatch(e->e.error!=null)) {
            BranchContext ctx=new BranchContext(entries.isEmpty()?1:entries.get(0).change.branchId(),domain,user.getUserId(),BranchContext.Mode.PREVIEW);
            try(BranchContexts.Scope ignored=BranchContexts.open(null,ctx)) {
                BranchView view=BranchView.reload(c);Map<String,Integer> urls=new HashMap<>();
                for(DocDetails d:view.documents(true)) {
                    String url=Objects.toString(DocDB.normalizeVirtualPath(d.getVirtualPath()),"").toLowerCase(Locale.ROOT);
                    if(url.isBlank())continue;Integer previous=urls.putIfAbsent(url,d.getDocId());
                    if(previous!=null&&(changedDocs.contains(previous)||changedDocs.contains(d.getDocId())))throw new IllegalStateException("Kolízia URL "+url+" (#"+previous+", #"+d.getDocId()+")");
                }
                Map<String,Integer> folderPaths=new HashMap<>();
                for(GroupDetails g:view.groups()) {String path=DocDB.getGroupDiskPath(view.groups(),g.getGroupId()).toLowerCase(Locale.ROOT);Integer previous=folderPaths.putIfAbsent(path,g.getGroupId());if(previous!=null&&(changedGroups.contains(previous)||changedGroups.contains(g.getGroupId())))throw new IllegalStateException("Kolízia priečinkov: "+path);}
                if(!view.derivedDocumentRows().isEmpty())for(int id:view.derivedDocumentRows().keySet())if(!changedDocs.contains(id))throw new IllegalStateException("Odvodená zmena stránky nie je v prehľade: "+id);
            } finally {BranchView.invalidate();}
        }
        if(Constants.getBoolean("fulltextExecuteApps")||Tools.isNotEmpty(Constants.getString("fulltextDataAscMethod")))throw new IllegalStateException("Tento POC nepodporuje vykonávanie aplikácií ani vlastný fulltext hook počas publikovania.");
    }

    private static boolean changed(List<Entry> entries,BranchType type,int id) {return entries.stream().anyMatch(e->e.type==type&&e.change.objectKey().equals(Integer.toString(id)));}
    private static boolean structural(Entry e) {if(e.target.state()!=BranchSnapshot.State.ACTIVE)return true;for(String f:List.of("parent_group_id","group_name","url_dir_name"))if(!Objects.equals(e.change.base().fields().get(f),e.target.fields().get(f)))return true;return false;}
    static Map<Integer,Map<String,Object>> table(Connection c,String table,String key)throws SQLException {
        Map<Integer,Map<String,Object>> rows=new LinkedHashMap<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM `"+table+"`")){while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.put(r.getMetaData().getColumnName(i),BranchStore.readColumn(r,i));rows.put(number(row,key),row);}}return rows;
    }
    static Set<Integer> descendants(Map<Integer,Map<String,Object>> groups,int root) {Set<Integer> result=new HashSet<>();result.add(root);boolean added;do{added=false;for(var g:groups.values())if(result.contains(number(g,"parent_group_id")))added|=result.add(number(g,"group_id"));}while(added);return result;}
    static int existingTrash(Map<Integer,Map<String,Object>> groups,String domain) {
        String trash=sk.iway.iwcm.i18n.Prop.getInstance(Constants.getString("defaultLanguage")).getText("config.trash_dir");
        String name=trash.substring(trash.lastIndexOf('/')+1);
        for(var g:groups.values())if(name.equals(g.get("group_name")) && BranchPublishRows.inTrash(number(g,"group_id"),groups) && (Tools.isEmpty(GroupsDB.getInstance().getDomain(number(g,"group_id"))) || domain.equals(GroupsDB.getInstance().getDomain(number(g,"group_id")))))return number(g,"group_id");return 0;
    }
    private static boolean perexAvailable(Connection c,int category,int group,Map<Integer,Map<String,Object>> groups)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT available_groups FROM perex_groups WHERE perex_group_id=? AND domain_id=?")) {
            s.setInt(1,category);s.setInt(2,sk.iway.iwcm.common.CloudToolsForCore.getDomainId());
            try(ResultSet r=s.executeQuery()) {
                return r.next() && folderAllowed(r.getString(1),group,groups);
            }
        }
    }
    private static boolean folderAllowed(String available,int group,Map<Integer,Map<String,Object>> groups) {
        Set<Integer> allowed=new HashSet<>(BranchFields.normalizedIds(available));if(allowed.isEmpty())return true;
        Set<Integer> seen=new HashSet<>();
        while(group>0&&seen.add(group)) {
            if(allowed.contains(group))return true;
            Map<String,Object> parent=groups.get(group);if(parent==null)break;group=number(parent,"parent_group_id");
        }
        return false;
    }
    private static void template(Connection c,int id)throws SQLException {if(id<=0)return;try(PreparedStatement s=c.prepareStatement("SELECT temp_id FROM templates WHERE temp_id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Nedostupná šablóna "+id);}}}
    private static void requireReference(Map<Integer,?> docs,int id,String owner) {if(id>0&&!docs.containsKey(id))throw new IllegalStateException("Chýbajúca referencia "+id+" ("+owner+")");}
    static int number(Map<String,?> row,String field) {Object v=row.get(field);return v instanceof Number n?n.intValue():0;}

    private static void validateFile(Entry e,Identity user) {
        String path=BranchFilePaths.normalize(e.change.objectKey());String folder=path.substring(0,path.lastIndexOf('/'));
        if(!user.isFolderWritable(folder))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bez práva k súboru "+path);
        if(sk.iway.iwcm.io.IwcmFsDB.useDBStorage(path)||sk.iway.iwcm.gallery.GalleryDB.isGalleryFolder(folder))throw new IllegalStateException("Nepodporované úložisko súboru "+path);
        if(!e.write)return;
        Path target=Path.of(Tools.getRealPath(path));Path ancestor=target.getParent();while(ancestor!=null&&!Files.exists(ancestor))ancestor=ancestor.getParent();
        if(ancestor==null||!Files.isDirectory(ancestor)||!Files.isWritable(ancestor))throw new IllegalStateException("Cieľ nie je zapisovateľný: "+path);
        if(e.target.state()==BranchSnapshot.State.ACTIVE && "FILE".equals(e.target.fields().get("entry_kind"))) {
            Path source=BranchFiles.workPath(e.change.branchId(),e.change.rowId());
            try {if(!Files.isRegularFile(source)||!BranchFiles.hash(source).equals(e.target.fields().get("content_hash")))throw new IllegalStateException("Chýba alebo sa zmenil pracovný súbor: "+path);}
            catch(java.io.IOException ex){throw new IllegalStateException("Nemožno čítať pracovný súbor "+path,ex);}
        }
        if(Files.exists(target) && (Files.isDirectory(target)!="DIRECTORY".equals(e.row.get("entry_kind"))))throw new IllegalStateException("Kolízia typu súbor/adresár: "+path);
    }

    /** Checks all pending page plans, including owners outside this package, using physical destinations. */
    static void checkFilePlans(Connection c,String domain,Collection<String> paths)throws SQLException {
        if(paths.isEmpty())return;
        Set<Path> targets=new HashSet<>();for(String path:paths)targets.add(Path.of(Tools.getRealPath(path)).toAbsolutePath().normalize());
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT DISTINCT doc_id,group_id FROM documents_history WHERE publicable=true OR (awaiting_approve IS NOT NULL AND awaiting_approve<>'')")) {
            while(r.next()) {
                int id=r.getInt(1);String plannedDomain=GroupsDB.getInstance().getDomain(r.getInt(2));
                if(!domain.equalsIgnoreCase(plannedDomain))continue;
                for(String folder:List.of("images","files")) {
                    String prefix=PlannedAssetPaths.ROOT+id+"/"+folder+"/";Path root=Path.of(Tools.getRealPath(prefix));if(!Files.exists(root))continue;
                    try(var stream=Files.walk(root)) {
                        for(Path file:stream.filter(Files::isRegularFile).toList()) {
                            String source=prefix+root.relativize(file).toString().replace('\\','/');String logical=PlannedAssetPaths.getPublicPath(source,id);
                            if(targets.contains(Path.of(Tools.getRealPath(logical)).toAbsolutePath().normalize()))throw new IllegalStateException("Súbor "+logical+" používa individuálny plán stránky #"+id+" (aj mimo balíka). Zmeňte pôvodný plán.");
                        }
                    }catch(java.io.IOException ex){throw new IllegalStateException("Nemožno overiť súbory individuálneho plánu #"+id,ex);}
                }
            }
        }
    }

    private static void checkIndividualPlans(Connection c,Entry e)throws SQLException {
        if(e.type==BranchType.FILE)return;
        String sql=e.type==BranchType.GROUP?"SELECT 1 FROM groups_scheduler WHERE group_id=? AND ((when_to_publish IS NOT NULL AND date_published IS NULL) OR (awaiting_approve IS NOT NULL AND awaiting_approve<>'')) LIMIT 1":"SELECT 1 FROM documents_history WHERE doc_id=? AND (publicable=true OR (awaiting_approve IS NOT NULL AND awaiting_approve<>'')) LIMIT 1";
        int id=e.type==BranchType.MEDIA?number(e.row,"media_fk_id"):Integer.parseInt(e.change.objectKey());
        try(PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalStateException("Individuálny plán alebo čakajúce schválenie: "+e.change.identity());}}
        if(e.type==BranchType.DOCUMENT)try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM documents WHERE doc_id=? AND (publish_after_start=true OR disable_after_end=true) LIMIT 1")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalStateException("Individuálny termín stránky #"+id);}}
    }

    /** Enforces item visibility and existing direct-publication rules, including source and destination. */
    private static void authorize(Connection c,BranchType type,Map<String,Object> row,Identity user,String domain,boolean publish)throws SQLException {
        if(user==null||!user.isAdmin()||!user.isAuthorized()||!user.isEnabledItem("menuBranches"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(type==BranchType.FILE) {String path=BranchFilePaths.normalize((String)row.get("logical_path"));if(!user.isFolderWritable(path.substring(0,path.lastIndexOf('/'))))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return;}
        if(!UsersDB.checkUserPerms(user,Constants.getString("webpagesFunctionsPerms")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Map<Integer,Map<String,Object>> groups=table(c,"groups","group_id");
        for(Map<String,Object> g:BranchPublishRows.rows(c,((Number)row.get("branch_id")).longValue(),BranchType.GROUP))groups.put(number(g,"group_id"),g);
        int owner=type==BranchType.MEDIA?number(row,"media_fk_id"):0;
        Map<String,Object> ownerDoc=owner>0?BranchStore.readWork(c,((Number)row.get("branch_id")).longValue(),BranchType.DOCUMENT,owner).orElseGet(()->{try{return BranchStore.readLive(c,BranchType.DOCUMENT,owner).orElse(null);}catch(SQLException ex){throw failure(ex);}}):null;
        if(type==BranchType.MEDIA && ownerDoc==null)throw conflict("Chýbajúci vlastník média.");
        int folder=type==BranchType.GROUP?number(row,"group_id"):type==BranchType.DOCUMENT?number(row,"group_id"):number(ownerDoc,"group_id");
        int docId=type==BranchType.DOCUMENT?number(row,"doc_id"):owner;
        boolean pageScope=docId>0 && BranchFields.normalizedIds(user.getEditablePages()).contains(docId);
        Map<String,Object> sourceDoc=docId>0?BranchStore.readLive(c,BranchType.DOCUMENT,docId).orElse(null):null;
        boolean sameFolder=sourceDoc!=null && number(sourceDoc,"group_id")==folder;
        checkFolder(c,groups,folder,user,domain,publish,pageScope && sameFolder);
        if(Constants.getBoolean("adminCheckUserGroups") && docId>0) {
            Map<String,Object> checked=type==BranchType.DOCUMENT?row:ownerDoc;
            try(var result=BranchRows.result(checked)) {if(!DocDB.canAccess(DocDB.getDocDetails(result,false,true),user,true))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
        }
        Map<String,Object> live=BranchStore.readLive(c,type,number(row,type.idColumn())).orElse(null);
        boolean ownDelete="DELETE".equals(row.get("operation")) && row.get("applied_hash")!=null && row.get("applied_hash").equals(BranchPublishRows.liveSnapshot(c,type,row).hash());
        if(live!=null && !ownDelete) {
            int source=type==BranchType.GROUP?number(live,"group_id"):type==BranchType.DOCUMENT?number(live,"group_id"):folder;
            if(type==BranchType.MEDIA) {
                int originalOwner=number(live,"media_fk_id");
                Map<String,Object> originalDoc=BranchStore.readLive(c,BranchType.DOCUMENT,originalOwner).orElseThrow(()->conflict("Chýbajúci pôvodný vlastník média."));
                source=number(originalDoc,"group_id");pageScope=BranchFields.normalizedIds(user.getEditablePages()).contains(originalOwner);
                if(originalOwner!=owner)throw conflict("Zmena vlastníka média nie je podporovaná.");
            }
            checkFolder(c,table(c,"groups","group_id"),source,user,domain,publish,pageScope);
        }
        if(publish && user.isDisabledItem("pageSave"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chýba právo uložiť/publikovať stránku.");
        if(type==BranchType.DOCUMENT) {
            int id=number(row,"doc_id");if(MultigroupMappingDB.getMasterDocId(id)>0||!MultigroupMappingDB.getSlaveDocIds(id).isEmpty())throw conflict("Multigroup zápis nie je podporovaný: "+id);
        }
        if(type==BranchType.MEDIA && user.isDisabledItem("editor_edit_media_all")&&user.isDisabledItem("editor_edit_media"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chýba právo upravovať médiá.");
    }

    private static void checkFolder(Connection c,Map<Integer,Map<String,Object>> groups,int id,Identity user,String domain,boolean publish,boolean pageScope)throws SQLException {
        Set<Integer> ancestors=new LinkedHashSet<>();int current=id;boolean allowed=pageScope || Tools.isEmpty(user.getEditableGroups(true));boolean self=false,secondSelf=false,first=false,second=false;
        Set<Integer> editable=new HashSet<>(BranchFields.normalizedIds(user.getEditableGroups(true)));
        while(current>0&&ancestors.add(current)) {
            Map<String,Object> g=groups.get(current);if(g==null)throw conflict("Chýbajúci priečinok #"+current);
            if(Boolean.TRUE.equals(g.get("internal"))||number(g,"internal")==1||"system".equalsIgnoreCase(Objects.toString(g.get("group_name"))))throw conflict("Zdieľaný System obsah je iba na čítanie.");
            String gd=Objects.toString(g.get("domain_name"),"");if(!gd.isBlank()&&!"default".equals(gd)&&!domain.equals(gd))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            allowed|=editable.contains(current);
            if(publish)try(PreparedStatement s=c.prepareStatement("SELECT user_id,approve_mode FROM groups_approve WHERE group_id=?")){s.setInt(1,current);try(ResultSet r=s.executeQuery()){while(r.next()){
                var approver=UsersDB.getUser(r.getInt(1));if(approver==null||!approver.isAdmin()||!approver.isAuthorized())continue;
                int mode=r.getInt(2);if(r.getInt(1)==user.getUserId()){self|=mode==UsersDB.APPROVE_APPROVE;secondSelf|=mode==UsersDB.APPROVE_LEVEL2||mode==UsersDB.APPROVE_NONE;}
                else {first|=mode==UsersDB.APPROVE_APPROVE;second|=mode==UsersDB.APPROVE_LEVEL2;}
            }}}
            current=number(g,"parent_group_id");
        }
        if(!allowed)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chýba právo na priečinok #"+id);
        if(publish&&!secondSelf&&((first&&!self)||second))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Balík vyžaduje priame schválenie priečinka #"+id);
    }

    private static Map<String,Object> branch(Connection c,long id,String domain,long revision,boolean lock)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM branches WHERE id=? AND domain_id=?"+(lock?" FOR UPDATE":""))){s.setLong(1,id);s.setInt(2,BranchDomains.id(domain));try(ResultSet r=s.executeQuery()){if(!r.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND);Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.put(r.getMetaData().getColumnName(i),r.getObject(i));if(revision>=0&&((Number)row.get("revision")).longValue()!=revision)throw conflict("Balík sa zmenil. Obnovte porovnanie.");return row;}}
    }
    private static void bump(Connection c,long id,int user)throws SQLException {try(PreparedStatement s=c.prepareStatement("UPDATE branches SET revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){s.setInt(1,user);s.setLong(2,id);s.executeUpdate();}}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private static IllegalStateException failure(SQLException ex){return new IllegalStateException("Nemožno pripraviť publikačný balík: "+ex.getMessage(),ex);}
}
