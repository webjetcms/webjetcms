package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/** Targeted page-version reads retain the report's item authorization without a package preflight. */
class BranchPageVersionsTest {
    /** Only the requested page is inspected, and a saved LIVE choice does not replace raw WORK. */
    @Test
    void readsOnlyTheRequestedPageAndKeepsRawWork() throws Exception {
        try(Fixture f=new Fixture()) {
            var versions=f.service.pageVersions(42,"example.sk",f.user,12);
            assertEquals("LIVE",versions.live().fields().get("title"));
            assertEquals("WORK",versions.work().fields().get("title"));
            assertEquals(BranchDocumentMapper.snapshot(f.live).hash(),versions.live().hash());
            assertEquals(BranchStore.change(BranchType.DOCUMENT,f.work).work().hash(),versions.work().hash());
            f.rows.verify(()->BranchPublishRows.rows(f.connection,42,BranchType.DOCUMENT),never());
            f.rows.verify(()->BranchPublishRows.rows(f.connection,42,BranchType.MEDIA),never());
            f.rows.verify(()->BranchPublishRows.rows(f.connection,42,BranchType.FILE),never());
            f.mappings.verify(()->MultigroupMappingDB.getMasterDocId(12),times(1));
            f.mappings.verify(()->MultigroupMappingDB.getSlaveDocIds(12),times(1));
            verify(f.branch).setLong(1,42);
            verify(f.branch).setInt(2,1);
        }
    }

    /** A branch or page outside the requested branch/domain cannot be rendered. */
    @Test
    void rejectsForeignBranchesAndMissingPages() throws Exception {
        try(Fixture f=new Fixture()) {
            assertStatus(HttpStatus.NOT_FOUND,()->f.service.pageVersions(41,"example.sk",f.user,12));
            assertStatus(HttpStatus.NOT_FOUND,()->f.service.pageVersions(42,"foreign.sk",f.user,12));
            assertStatus(HttpStatus.NOT_FOUND,()->f.service.pageVersions(42,"example.sk",f.user,99));
        }
    }

    /** Domain visibility and the normal administrator requirement are still mandatory. */
    @Test
    void rejectsUnauthorizedUsersAndDomains() throws Exception {
        try(Fixture f=new Fixture()) {
            when(f.user.isAdmin()).thenReturn(false);
            assertStatus(HttpStatus.FORBIDDEN,()->f.service.pageVersions(42,"example.sk",f.user,12));
            when(f.user.isAdmin()).thenReturn(true);
            when(f.permissions.getUserRootDomainNames(f.user)).thenReturn(List.of("foreign.sk"));
            assertStatus(HttpStatus.FORBIDDEN,()->f.service.pageVersions(42,"example.sk",f.user,12));
        }
    }

    /** A readable branch does not grant access to a page outside the user's folder scope. */
    @Test
    void rejectsMissingFolderPermissions() throws Exception {
        try(Fixture f=new Fixture()) {
            when(f.user.getEditableGroups(true)).thenReturn("99");
            assertStatus(HttpStatus.FORBIDDEN,()->f.service.pageVersions(42,"example.sk",f.user,12));
        }
    }

    /** Moving a page to an allowed folder does not hide unauthorized access to its LIVE source. */
    @Test
    void checksTheLiveSourceFolderAsWellAsTheWorkDestination() throws Exception {
        try(Fixture f=new Fixture()) {
            f.live.put("group_id",20);
            f.groups.put(20,group(20,"foreign.sk"));
            assertStatus(HttpStatus.FORBIDDEN,()->f.service.pageVersions(42,"example.sk",f.user,12));
        }
    }

    /** New pages expose ABSENT LIVE plus their actual WORK snapshot. */
    @Test
    void supportsWorkOnlyPages() throws Exception {
        try(Fixture f=new Fixture()) {
            f.live=null;
            f.work.put("base_exists",false);f.work.put("base_snapshot",BranchSnapshot.absent().json());f.work.put("base_hash",BranchSnapshot.absent().hash());
            f.work.remove("resolved_choice");f.work.remove("resolved_live_hash");f.work.remove("resolved_work_hash");
            var versions=f.service.pageVersions(42,"example.sk",f.user,12);
            assertEquals("ABSENT",versions.live().state());
            assertEquals("ACTIVE",versions.work().state());
        }
    }

    private static void assertStatus(HttpStatus status,org.junit.jupiter.api.function.Executable action) {
        assertEquals(status,assertThrows(ResponseStatusException.class,action).getStatusCode());
    }

    private static Map<String,Object> group(int id,String domain) {
        return new LinkedHashMap<>(Map.of("group_id",id,"parent_group_id",0,"group_name","content","domain_name",domain,"internal",false));
    }

    private static final class Fixture implements AutoCloseable {
        final BranchMergeService service=new BranchMergeService();
        final Connection connection=mock(Connection.class);
        final PreparedStatement branch=mock(PreparedStatement.class);
        final Statement tables=mock(Statement.class);
        final Identity user=mock(Identity.class);
        final GroupsDB permissions=mock(GroupsDB.class);
        Map<String,Object> live=new LinkedHashMap<>(BranchDocumentMapper.initialValues(12,7,Instant.EPOCH,false));
        final Map<String,Object> work=new LinkedHashMap<>();
        final Map<Integer,Map<String,Object>> groups=new LinkedHashMap<>();
        final MockedStatic<DBPool> pool=mockStatic(DBPool.class);
        final MockedStatic<BranchReadiness> readiness=mockStatic(BranchReadiness.class);
        final MockedStatic<BranchStore> store=mockStatic(BranchStore.class,CALLS_REAL_METHODS);
        final MockedStatic<BranchPublishRows> rows=mockStatic(BranchPublishRows.class);
        final MockedStatic<GroupsDB> folders=mockStatic(GroupsDB.class);
        final MockedStatic<Constants> constants=mockStatic(Constants.class);
        final MockedStatic<UsersDB> users=mockStatic(UsersDB.class);
        final MockedStatic<MultigroupMappingDB> mappings=mockStatic(MultigroupMappingDB.class);
        final MockedStatic<Prop> props=mockStatic(Prop.class);

        Fixture() throws Exception {
            live.put("group_id",10);live.put("title","LIVE");live.put("data","<p>LIVE</p>");
            BranchSnapshot base=BranchDocumentMapper.snapshot(live);
            work.putAll(live);work.put("title","WORK");work.put("data","<p>WORK</p>");
            work.put("row_id",1L);work.put("branch_id",42L);work.put("operation","UPSERT");work.put("base_exists",true);
            work.put("base_snapshot",base.json());work.put("base_hash",base.hash());
            work.put("resolved_choice","LIVE");work.put("resolved_live_hash",base.hash());work.put("resolved_work_hash",BranchDocumentMapper.snapshot(work).hash());
            groups.put(10,group(10,"example.sk"));
            pool.when(DBPool::getConnection).thenReturn(connection);
            when(user.isAdmin()).thenReturn(true);when(user.isAuthorized()).thenReturn(true);when(user.isEnabledItem("menuBranches")).thenReturn(true);
            folders.when(GroupsDB::getInstance).thenReturn(permissions);
            folders.when(()->GroupsDB.getDomainId("example.sk")).thenReturn(1);
            folders.when(()->GroupsDB.getDomainId("foreign.sk")).thenReturn(2);
            when(permissions.getUserRootDomainNames(user)).thenReturn(List.of("example.sk"));
            constants.when(()->Constants.getString("webpagesFunctionsPerms")).thenReturn("admin");
            users.when(()->UsersDB.checkUserPerms(user,"admin")).thenReturn(true);
            Prop prop=mock(Prop.class);props.when(Prop::getInstance).thenReturn(prop);when(prop.getText(anyString())).thenReturn("Permission denied");
            store.when(()->BranchStore.readWork(connection,42,BranchType.DOCUMENT,12)).thenReturn(Optional.of(work));
            store.when(()->BranchStore.readWork(connection,42,BranchType.DOCUMENT,99)).thenReturn(Optional.empty());
            store.when(()->BranchStore.readLive(connection,BranchType.DOCUMENT,12)).thenAnswer(call->Optional.ofNullable(live));
            rows.when(()->BranchPublishRows.liveSnapshot(connection,BranchType.DOCUMENT,work)).thenAnswer(call->live==null?BranchSnapshot.absent():BranchDocumentMapper.snapshot(live));
            AtomicLong selectedId=new AtomicLong();AtomicInteger selectedDomain=new AtomicInteger();
            doAnswer(call->{selectedId.set(call.getArgument(1));return null;}).when(branch).setLong(eq(1),anyLong());
            doAnswer(call->{selectedDomain.set(call.getArgument(1));return null;}).when(branch).setInt(eq(2),anyInt());
            when(connection.prepareStatement(anyString())).thenAnswer(call->{
                assertEquals("SELECT * FROM branches WHERE id=? AND domain_id=?",call.getArgument(0));return branch;
            });
            when(branch.executeQuery()).thenAnswer(call->{
                var result=BranchRows.result(Map.of("id",42,"domain_id",1,"state","DRAFT","revision",1));
                if(selectedId.get()!=42||selectedDomain.get()!=1)result.deleteRow();
                result.beforeFirst();return result;
            });
            when(connection.createStatement()).thenReturn(tables);
            when(tables.executeQuery(anyString())).thenAnswer(call->{
                assertEquals("SELECT * FROM `groups`",call.getArgument(0));
                var result=BranchRows.result(groups.get(10));
                for(var group:groups.values())if(((Number)group.get("group_id")).intValue()!=10) {
                    result.moveToInsertRow();for(var entry:group.entrySet())result.updateObject(entry.getKey(),entry.getValue());result.insertRow();result.moveToCurrentRow();
                }
                result.beforeFirst();return result;
            });
        }

        @Override public void close() {props.close();mappings.close();users.close();constants.close();folders.close();rows.close();store.close();readiness.close();pool.close();}
    }
}
