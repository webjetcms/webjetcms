package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.security.MessageDigest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.rest.GroupsRestController;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaEditorFields;

/** Exercises the actual authorized WORK editor and verifies production tables byte for byte. */
@EnabledIfEnvironmentVariable(named="WEBJET_P04_DB",matches="true")
class BranchEditorTest extends BranchJpaTest {
    private static final String DOMAIN="p04-editor.example";
    private final Map<String,String> configuration=new LinkedHashMap<>();
    private BranchContext context;
    private int root,other,page;
    private final List<Integer> liveGroups=new ArrayList<>(),liveDocs=new ArrayList<>();
    private String production;

    @BeforeEach void fixture() throws Exception {
        try(Connection c=DBPool.getConnection()) { assertEquals("webjet_p01",c.getCatalog()); assertTrue(c.getMetaData().getURL().contains(":33316/")); }
        for(String key:List.of("enableStaticFilesExternalDir","branchesEnabled","branchStorageRoot","multiDomainEnabled","templatesUseRecursiveSystemFolder","fulltextExecuteApps","linkType","groupCreateBlankWebpageAfterCreate","fulltextDataAscMethod","multiGroupKeepSortPriority")) configuration.put(key,Constants.getString(key));
        Constants.setBoolean("branchesEnabled",true); Constants.setBoolean("enableStaticFilesExternalDir",true);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private")); Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
        Constants.setBoolean("multiDomainEnabled",true); Constants.setBoolean("templatesUseRecursiveSystemFolder",false); Constants.setBoolean("fulltextExecuteApps",true);
        Constants.setInt("linkType",Constants.LINK_TYPE_HTML);
        root=liveGroup("autotest P04 root",0,"autotest-p04"); other=liveGroup("autotest P04 other",root,"other");
        page=liveDoc(root,"/autotest-p04/existing.html");
        try(Connection c=DBPool.getConnection();Statement statement=c.createStatement()) { statement.executeUpdate("UPDATE `groups` SET default_doc_id="+page+" WHERE group_id="+root); }
        var branch=createBranch(BranchTestDomains.ensure(DOMAIN),"autotest P04 editor","disposable integration test",1);
        context=new BranchContext(branch.getId(),DOMAIN,1,BranchContext.Mode.ADMIN);
        production=fingerprint();
    }

    @AfterEach void cleanup() throws Exception {
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            if(context!=null) for(String table:List.of("branch_files","branch_media","branch_documents","branch_groups","branches")) s.executeUpdate("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+context.branchId());
            for(int id:liveDocs) s.executeUpdate("DELETE FROM documents WHERE doc_id="+id);
            for(int id:liveGroups) s.executeUpdate("DELETE FROM `groups` WHERE group_id="+id);
        } finally { configuration.forEach(Constants::setString); }
    }

    /** A new WORK folder gets its main page according to configuration and page creation rights. */
    @ParameterizedTest
    @CsvSource({"true, true", "false, true", "true, false"})
    void createsDefaultPageForNewFolderWhenConfigured(boolean autoCreate, boolean canAddPage) throws Exception {
        Constants.setBoolean("groupCreateBlankWebpageAfterCreate",autoCreate);
        boolean createPage=autoCreate && canAddPage;
        in((request,editor) -> {
            if (!canAddPage) {
                Identity user=(Identity)request.getSession().getAttribute(Constants.USER_KEY);
                user.addDisabledItem("addPage");
            }
            GroupsRestController controller=new GroupsRestController(null,null,null,null,null,editor);
            controller.setRequest(request);
            GroupDetails group=new GroupDetails();
            group.setGroupId(-1);
            group.setGroupName("autotest P04 folder with main page");
            group.setNavbar("autotest P04 navigation");
            group.setUrlDirName("main-page");
            group.setParentGroupId(root);
            group=new GroupEditorField().fromGroupDetails(group);
            group.getEditorFields().setBranchHash("new");

            GroupDetails saved=controller.insertItem(group);
            int groupId=saved.getGroupId();
            int defaultDocId=saved.getDefaultDocId();
            assertEquals(createPage,defaultDocId>0);
            if (createPage) {
                assertEquals(defaultDocId,saved.getEditorFields().getDefaultDocDetails().getDocId());
                DocDetails mainPage=new BranchEditor(request).loadDocument(defaultDocId,-1,groupId,editor);
                assertEquals(groupId,mainPage.getGroupId());
                assertEquals(group.getGroupName(),mainPage.getTitle());
                assertEquals(group.getGroupName(),mainPage.getNavbar());
                assertEquals(Constants.getBoolean("editorNewDocDefaultAvailableChecked"),mainPage.isAvailable());
            }

            saved.setNavbar("autotest P04 updated navigation");
            assertEquals(defaultDocId,controller.editItem(saved,groupId).getDefaultDocId());
            assertEquals(createPage ? 1 : 0,BranchView.current().documents(true).stream()
                .filter(doc -> doc.getGroupId()==groupId).count());
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void createEditMoveDeleteRevertAndIsolation() throws Exception {
        Constants.setBoolean("groupCreateBlankWebpageAfterCreate",false);
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            GroupDetails group=new GroupDetails(); group.setGroupId(-1); group.setGroupName("autotest P04 new"); group.setNavbar("autotest P04 new"); group.setUrlDirName("new"); group.setParentGroupId(root); group.setDomainName(DOMAIN);
            group=new GroupEditorField().fromGroupDetails(group); group.getEditorFields().setBranchHash("new");
            GroupDetails saved=service.saveGroup(group,editor); int groupId=saved.getGroupId(); assertTrue(groupId>0);
            DocDetails fresh=service.loadDocument(-1,-1,groupId,editor); fresh.setUrlInheritGroup(true); fresh.setTitle("autotest P04 article"); fresh.setNavbar("autotest P04 article"); fresh.setAvailable(true); fresh.setHtmlData("autotest perex");
            fresh.setData("<section class=\"pb-section\"><div class=\"container\"><p>autotest WORK !INCLUDE(/autotest-never-execute.jsp)!</p></div></section>");
            DocDetails created=service.saveDocument(fresh,editor); int id=created.getDocId(); assertTrue(id>0); assertTrue(created.isAvailable()); assertEquals("autotest perex",created.getHtmlData()); assertTrue(created.getData().contains("pb-section"));
            assertEquals(id,BranchView.current().group(groupId).getDefaultDocId());
            created=service.loadDocument(id,-1,groupId,editor); created.setTitle("autotest renamed"); created.setNavbar("autotest renamed");
            created=service.saveDocument(created,editor); assertEquals(id,created.getDocId());
            GroupDetails renamed=new GroupEditorField().fromGroupDetails(BranchView.current().group(groupId)); BranchEditor.attachHash(renamed);
            renamed.setGroupName("autotest renamed folder"); renamed.setNavbar("autotest renamed folder"); renamed.setUrlDirName("renamed"); renamed.getEditorFields().setParentGroupDetails(BranchView.current().group(other));
            renamed=service.saveGroup(renamed,editor); assertEquals(groupId,renamed.getGroupId());
            assertTrue(BranchView.current().document(id,false).getVirtualPath().contains("/other/renamed/"));
            // Removing a non-default page and undoing an existing LIVE change are independent operations.
            DocDetails edit=service.loadDocument(page,-1,root,editor); String initial=edit.getEditorFields().getBranchHash();
            edit.setTitle("autotest modified existing"); edit=service.saveDocument(edit,editor); assertNotEquals(initial,edit.getEditorFields().getBranchHash());
            service.deleteDocument(edit); assertNull(BranchView.current().document(page,false));
            service.revert(BranchType.DOCUMENT,page,hash(BranchType.DOCUMENT,page)); assertEquals("autotest LIVE",BranchView.current().document(page,false).getTitle());
            service.deleteGroup(renamed); assertNull(BranchView.current().group(groupId)); assertNull(BranchView.current().document(id,false));
            try(Connection c=DBPool.getConnection()) { assertTrue(BranchStore.readLive(c,BranchType.DOCUMENT,id).isEmpty()); assertTrue(BranchStore.readLive(c,BranchType.GROUP,groupId).isEmpty()); }
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test
    void folderUrlRegenerationUpdatesExistingLiveAndWorkPages() throws Exception {
        int manual = liveDoc(other, "/manual-url.html");
        try (Connection connection = DBPool.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE `groups` SET default_doc_id=? WHERE group_id=?")) {
            statement.setInt(1, manual);
            statement.setInt(2, other);
            statement.executeUpdate();
        }
        production = fingerprint();

        in((request, editor) -> {
            BranchEditor service = new BranchEditor(request);
            DocDetails draft = service.loadDocument(-1, -1, other, editor);
            draft.setTitle("New article");
            draft.setNavbar("New article");
            int article = service.saveDocument(draft, editor).getDocId();

            // Persist the original generated URL through the same folder rename that editors already use.
            GroupDetails folder = new GroupEditorField().fromGroupDetails(BranchView.current().group(other));
            BranchEditor.attachHash(folder);
            folder.setGroupName("Renamed folder");
            folder = service.saveGroup(folder, editor);
            assertEquals("/autotest-p04/other/new-article.html", BranchView.current().document(article, true).getVirtualPath());

            folder.setUrlDirName("new-address");
            folder.getEditorFields().setForceUrlDirNameChange(true);
            service.saveGroup(folder, editor);

            assertEquals("/autotest-p04/new-address/new-article.html", BranchView.current().document(article, true).getVirtualPath());
            assertEquals("/autotest-p04/new-address/", BranchView.current().document(manual, true).getVirtualPath());
            return null;
        });
        assertEquals(production, fingerprint());
    }

    /** Legacy roots without an explicit domain support the same WORK editing as their displayed tree. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={"default"})
    void editsPagesAndFoldersBelowAnInheritedDomainRoot(String domain) throws Exception {
        int inheritedRoot=liveGroup("autotest inherited root",0,"autotest-inherited-root");
        int inheritedChild=liveGroup("autotest inherited child",inheritedRoot,"child");
        int inheritedPage=liveDoc(inheritedChild,"/autotest-inherited-root/child/page.html");
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE `groups` SET domain_name=? WHERE group_id IN (?,?)")) {
            statement.setString(1,domain);statement.setInt(2,inheritedRoot);statement.setInt(3,inheritedChild);statement.executeUpdate();
        }
        production=fingerprint();
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails doc=service.loadDocument(inheritedPage,-1,inheritedChild,editor);
            doc.setData("<p>autotest WORK content</p>");
            assertEquals("<p>autotest WORK content</p>",service.saveDocument(doc,editor).getData());

            GroupDetails group=new GroupDetails();group.setGroupId(-1);group.setGroupName("autotest new child");
            group.setNavbar("autotest new child");group.setUrlDirName("new-child");group.setParentGroupId(inheritedChild);
            group=new GroupEditorField().fromGroupDetails(group);group.getEditorFields().setBranchHash("new");
            assertTrue(service.saveGroup(group,editor).getGroupId()>0);

            GroupDetails rootEdit=new GroupEditorField().fromGroupDetails(BranchView.current().group(inheritedRoot));
            BranchEditor.attachHash(rootEdit);rootEdit.setNavbar("autotest edited root navigation");
            assertEquals("autotest edited root navigation",service.saveGroup(rootEdit,editor).getNavbarName());
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void rejectsStaleContentCyclesUrlsDefaultsPlansAndRights() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails first=service.loadDocument(page,-1,root,editor), stale=service.loadDocument(page,-1,root,editor);
            first.setTitle("autotest winner"); service.saveDocument(first,editor);
            stale.setTitle("autotest stale"); assertThrows(ResponseStatusException.class,()->service.saveDocument(stale,editor));
            DocDetails plan=service.loadDocument(page,-1,root,editor); plan.setPublishEndDate(new java.util.Date()); assertThrows(ResponseStatusException.class,()->service.saveDocument(plan,editor));
            DocDetails foreign=service.loadDocument(page,-1,root,editor); foreign.getEditorFields().getGroupDetails().setGroupId(Integer.MAX_VALUE); assertThrows(ResponseStatusException.class,()->service.saveDocument(foreign,editor));
            DocDetails collision=service.loadDocument(-1,-1,root,editor); collision.setGenerateUrlFromTitle(false); collision.setVirtualPath("/autotest-p04/existing.html"); assertThrows(ResponseStatusException.class,()->service.saveDocument(collision,editor));
            GroupDetails cycle=new GroupEditorField().fromGroupDetails(BranchView.current().group(root)); BranchEditor.attachHash(cycle); cycle.getEditorFields().setParentGroupDetails(BranchView.current().group(other)); assertThrows(ResponseStatusException.class,()->service.saveGroup(cycle,editor));
            GroupDetails invalid=new GroupEditorField().fromGroupDetails(BranchView.current().group(other)); BranchEditor.attachHash(invalid); invalid.getEditorFields().setDefaultDocDetails(BranchView.current().document(page,true)); assertThrows(ResponseStatusException.class,()->service.saveGroup(invalid,editor));
            Identity user=(Identity)request.getSession().getAttribute(Constants.USER_KEY); user.setEditableGroups(String.valueOf(other));
            var denied=assertThrows(ResponseStatusException.class,()->service.loadDocument(page,-1,root,editor));
            String reason=sk.iway.iwcm.i18n.Prop.getInstance(request).getText("branches.editor.permission");
            assertEquals(reason,denied.getReason());
            assertEquals(reason,new sk.iway.iwcm.system.spring.DatatableExceptionHandlerV2().handleException(denied).getBody().getError());
            return null;
        });
        assertEquals(production,fingerprint());
    }

    /** Removing a page or its parent must preserve the draft until the editor removes its media. */
    @Test void preventsRemovingDraftMediaOwners() throws Exception {
        in((request, editor) -> {
            BranchEditor pages = new BranchEditor(request);
            DocDetails draft = pages.loadDocument(-1, -1, other, editor);
            draft.setTitle("autotest draft with media");
            draft.setNavbar(draft.getTitle());
            DocDetails saved = pages.saveDocument(draft, editor);
            int id = saved.getDocId();
            String hash = saved.getEditorFields().getBranchHash();

            BranchMediaEditor media = new BranchMediaEditor(request);
            Media attachment = new Media();
            attachment.setMediaTitleSk("autotest attachment");
            attachment.setEditorFields(new MediaEditorFields());
            attachment.getEditorFields().setGroups(new Integer[0]);
            Media staged = media.save(attachment, id, "new");

            GroupDetails folder = BranchView.current().group(root);
            String reason = Prop.getInstance().getText("branches.editor.removePageMediaFirst", Integer.toString(id));
            assertTrue(reason.contains(Integer.toString(id)));
            for (var action : List.<Executable>of(
                    () -> pages.deleteDocument(saved),
                    () -> pages.revert(BranchType.DOCUMENT, id, hash),
                    () -> pages.deleteGroup(folder))) {
                var error = assertThrows(ResponseStatusException.class, action);
                assertEquals(409, error.getStatusCode().value());
                assertEquals(reason, error.getReason());
                assertEquals(id, BranchView.current().group(other).getDefaultDocId());
                assertNotNull(BranchView.current().document(page, false));
                assertEquals(id, media.load(staged.getMediaId(), id).getMediaFkId());
            }

            Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
            user.addDisabledItem("pageSave");
            assertEquals(403, assertThrows(ResponseStatusException.class,
                    () -> pages.revert(BranchType.DOCUMENT, id, hash)).getStatusCode().value());
            user.removeDisabledItem("pageSave");

            assertTrue(media.delete(staged.getMediaId(), id, staged.getEditorFields().getBranchHash()));
            assertTrue(pages.deleteDocument(saved));
            assertNull(BranchView.current().document(id, false));
            return null;
        });
        assertEquals(production, fingerprint());
    }

    @Test void deletingExistingSubtreeAndRevertingRestoresItsDefaultPage() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            // The tree delete button submits its displayed group, without opening the folder editor first.
            GroupDetails folder=BranchView.current().group(root);
            int domainId=context.domainId();
            assertNotNull(folder.getEditorFields().getBranchHash());
            service.deleteGroup(folder); assertNull(BranchView.current().group(root)); assertNull(BranchView.current().document(page,false));
            assertEquals(domainId,context.domainId());
            assertEquals(domainId,sk.iway.iwcm.common.CloudToolsForCore.getDomainId());
            assertEquals(DOMAIN,BranchDomains.name(domainId));
            service.revert(BranchType.GROUP,root,hash(BranchType.GROUP,root));
            assertNotNull(BranchView.current().group(other)); assertNotNull(BranchView.current().document(page,false));
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void firstSaveRejectsAProductionChangeAfterTheFormWasOpened() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails oldForm=service.loadDocument(page,-1,root,editor);
            try(Connection c=DBPool.getConnection();PreparedStatement statement=c.prepareStatement("UPDATE documents SET title=? WHERE doc_id=?")) {
                statement.setString(1,"autotest concurrent LIVE change"); statement.setInt(2,page); statement.executeUpdate();
                try {
                    oldForm.setTitle("autotest stale overwrite");
                    assertThrows(ResponseStatusException.class,()->service.saveDocument(oldForm,editor));
                    assertTrue(BranchStore.readWork(c,context.branchId(),BranchType.DOCUMENT,page).isEmpty());
                } finally { statement.setString(1,"autotest LIVE"); statement.executeUpdate(); }
            }
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void cachedWorkViewCannotAttachTheHashOfANewerUnseenVersion() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails first=service.loadDocument(page,-1,root,editor); first.setTitle("autotest displayed version"); service.saveDocument(first,editor);
            DocDetails displayed=service.loadDocument(page,-1,root,editor); String displayedHash=displayed.getEditorFields().getBranchHash();
            try(Connection c=DBPool.getConnection()) {
                Map<String,Object> stored=BranchStore.readWork(c,context.branchId(),BranchType.DOCUMENT,page).orElseThrow();
                Map<String,Object> changed=new LinkedHashMap<>(stored); changed.put("title","autotest unseen newer version");
                new BranchStore().put(context,BranchType.DOCUMENT,page,changed,BranchStore.snapshot(BranchType.DOCUMENT,stored).hash(),false);
            }
            DocDetails lateForm=service.loadDocument(page,-1,root,editor);
            assertEquals("autotest displayed version",lateForm.getTitle()); assertEquals(displayedHash,lateForm.getEditorFields().getBranchHash());
            lateForm.setTitle("autotest overwrite attempt"); assertThrows(ResponseStatusException.class,()->service.saveDocument(lateForm,editor));
            assertEquals("autotest unseen newer version",BranchView.current().document(page,false).getTitle());
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void rejectsForeignDomainAndSystemWrites() throws Exception {
        int foreign=liveGroup("autotest foreign",0,"autotest-foreign");
        int system=liveGroup("System",root,"system");
        int systemPage=liveDoc(system,"/autotest-p04/system/shared.html");
        try(Connection c=DBPool.getConnection();Statement statement=c.createStatement()) {
            statement.executeUpdate("UPDATE `groups` SET domain_name='foreign-p04.example' WHERE group_id="+foreign);
        }
        production=fingerprint();
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            assertThrows(ResponseStatusException.class,()->service.loadDocument(-1,-1,foreign,editor));
            assertThrows(ResponseStatusException.class,()->service.loadDocument(-1,-1,system,editor));
            DocDetails shared=service.loadDocument(systemPage,-1,system,editor);
            assertNotNull(shared.getEditorFields().getBranchReadOnlyReason());
            assertThrows(ResponseStatusException.class,()->service.saveDocument(shared,editor));
            return null;
        });
        assertEquals(production,fingerprint());
    }

    /** Redirecting copies follow the master's URL and cannot change the master's own external link. */
    @Test void keepsExistingMultigroupRedirectsWhenTheMasterUrlChanges() throws Exception {
        int master = liveDoc(other, "/autotest-p04/other/master.html");
        try (Connection c = DBPool.getConnection(); Statement sql = c.createStatement()) {
            sql.executeUpdate("UPDATE documents SET url_inherit_group=1, editor_virtual_path='master.html' WHERE doc_id=" + master);
            sql.executeUpdate("UPDATE documents SET external_link='/autotest-p04/other/master.html' WHERE doc_id=" + page);
            sql.executeUpdate("INSERT INTO multigroup_mapping(doc_id,master_id,redirect) VALUES (" + page + "," + master + ",1)");
            production = fingerprint();
            try {
                in((request, editor) -> {
                    var pages = new BranchEditor(request);
                    var folder = new GroupEditorField().fromGroupDetails(BranchView.current().group(other));
                    BranchEditor.attachHash(folder);
                    folder.setUrlDirName("renamed");
                    pages.saveGroup(folder, editor);
                    String masterUrl = "/autotest-p04/renamed/master.html";
                    assertEquals(masterUrl, BranchView.current().document(page, true).getExternalLink());
                    var copy = pages.loadDocument(page, -1, root, editor);
                    assertEquals("", copy.getExternalLink());
                    copy.setVirtualPath("/autotest-p04/manual.html");
                    var saved = pages.saveDocument(copy, editor);
                    assertEquals("", saved.getExternalLink());
                    assertEquals("/autotest-p04/manual.html", BranchView.current().document(page, true).getVirtualPath());
                    assertEquals(masterUrl, BranchView.current().document(master, true).getVirtualPath());
                    assertEquals(masterUrl, BranchView.current().document(page, true).getExternalLink());
                    return null;
                });
                assertEquals(production, fingerprint());
            } finally {
                sql.executeUpdate("DELETE FROM multigroup_mapping WHERE doc_id=" + page);
            }
        }
    }

    /** A copy edits shared content while each document keeps its own URL and publication identity. */
    @Test void editsExistingMultigroupPagesAndPublishesFolderRenames() throws Exception {
        int copy = liveDoc(other, "/autotest-p04/other/copy.html");
        Constants.setBoolean("fulltextExecuteApps", false);
        Constants.setString("fulltextDataAscMethod", "");
        Constants.setBoolean("multiGroupKeepSortPriority", true);
        sk.iway.iwcm.system.Modules.getInstance(null, true);
        long branch = context.branchId();
        try (Connection c = DBPool.getConnection(); Statement sql = c.createStatement()) {
            sql.executeUpdate("INSERT INTO multigroup_mapping(doc_id,master_id,redirect) VALUES (" + copy + "," + page + ",0)");
            sql.executeUpdate("UPDATE documents SET url_inherit_group=1, editor_virtual_path='copy.html', sort_priority=123 WHERE doc_id=" + copy);
            sql.executeUpdate("INSERT INTO users(login,is_admin,authorized,editable_groups,editable_pages,disabled_items) VALUES ('autotest-branch-multigroup-" + branch + "',1,1,'','','')", Statement.RETURN_GENERATED_KEYS);
            int publisherId;
            try (var keys = sql.getGeneratedKeys()) {
                assertTrue(keys.next());
                publisherId = keys.getInt(1);
            }
            production = fingerprint();
            try {
                in((request, editor) -> {
                    var pages = new BranchEditor(request);
                    var merge = new BranchMergeService();
                    var user = new Identity(sk.iway.iwcm.users.UsersDB.getUser(publisherId));
                    assertTrue(user.isEnabledItem("menuBranches"));
                    var group = new GroupEditorField().fromGroupDetails(BranchView.current().group(other));
                    BranchEditor.attachHash(group);
                    group.setGroupName("Kontakt QA");
                    group = pages.saveGroup(group, editor);
                    var renamed = merge.prepare(branch, DOMAIN, user, -1);
                    assertTrue(renamed.ready(), renamed.errors().toString());
                    assertEquals(1, renamed.items().size());
                    assertEquals("/autotest-p04/other/copy.html", BranchView.current().document(copy, true).getVirtualPath());

                    group.setUrlDirName("contact");
                    pages.saveGroup(group, editor);
                    var linked = pages.loadDocument(copy, -1, other, editor);
                    assertNull(linked.getEditorFields().getBranchReadOnlyReason());
                    assertEquals("/autotest-p04/contact/copy.html", linked.getVirtualPath());
                    linked.setData("<p>Edited via copy</p>");
                    pages.saveDocument(linked, editor);
                    DocDetails master = pages.loadDocument(page, -1, root, editor);
                    assertEquals(linked.getData(), master.getData());
                    assertEquals("/autotest-p04/existing.html", master.getVirtualPath());
                    master.setTitle("Shared title");
                    pages.saveDocument(master, editor);
                    assertEquals("Shared title", BranchView.current().document(copy, false).getTitle());
                    assertEquals(123, BranchView.current().document(copy, false).getSortPriority());
                    assertEquals(production, fingerprint());

                    master = pages.loadDocument(page, -1, root, editor);
                    final DocDetails form = master;
                    form.getEditorFields().setGroupCopyDetails(List.of());
                    assertThrows(ResponseStatusException.class, () -> pages.saveDocument(form, editor));
                    form.getEditorFields().setGroupCopyDetails(List.of(BranchView.current().group(root)));
                    assertThrows(ResponseStatusException.class, () -> pages.saveDocument(form, editor));
                    var rejected = assertThrows(ResponseStatusException.class, () -> pages.deleteDocument(linked));
                    assertTrue(rejected.getReason().contains("Shared title (#" + copy + ")"));
                    final GroupDetails folder = group;
                    assertThrows(ResponseStatusException.class, () -> pages.deleteGroup(folder));
                    assertEquals(production, fingerprint());

                    sql.executeUpdate("UPDATE multigroup_mapping SET redirect=1 WHERE doc_id=" + copy);
                    var changed = merge.prepare(branch, DOMAIN, user, -1);
                    assertFalse(changed.ready());
                    assertTrue(changed.errors().stream().anyMatch(error -> error.contains("#" + copy)), changed.errors().toString());
                    sql.executeUpdate("UPDATE multigroup_mapping SET redirect=0 WHERE doc_id=" + copy);
                    var ready = merge.prepare(branch, DOMAIN, user, -1);
                    assertTrue(ready.ready(), ready.errors().toString());
                    merge.claim(branch, DOMAIN, user, ready.revision(), ready.preparationHash(), false);
                    return null;
                });
                new BranchPublishService().run(branch, publisherId);
                try (var result = sql.executeQuery("SELECT state,last_error FROM branches WHERE id=" + branch)) {
                    assertTrue(result.next());
                    assertEquals("MERGED", result.getString(1), result.getString(2));
                }
                for (int id : List.of(page, copy)) {
                    var live = BranchStore.readLive(c, BranchType.DOCUMENT, id).orElseThrow();
                    assertEquals("<p>Edited via copy</p>", live.get("data"));
                    assertEquals("Shared title", live.get("title"));
                    assertEquals(id == page ? "/autotest-p04/existing.html" : "/autotest-p04/contact/copy.html", live.get("virtual_path"));
                }
                assertEquals(Map.of(copy, false), BranchMultigroup.read(c, page).copies());
            } finally {
                sql.executeUpdate("DELETE FROM users WHERE user_id=" + publisherId);
                sql.executeUpdate("DELETE FROM multigroup_mapping WHERE doc_id=" + copy);
                sql.executeUpdate("DELETE FROM documents_history WHERE doc_id IN (" + page + "," + copy + ")");
            }
        }
    }

    private String hash(BranchType type,int id) throws SQLException {
        try(Connection connection=DBPool.getConnection()) {
            return BranchStore.change(type,BranchStore.readWork(connection,context.branchId(),type,id).orElseThrow()).work().hash();
        }
    }

    private int liveGroup(String name,int parent,String url) throws Exception {
        try(Connection c=DBPool.getConnection()) { c.setAutoCommit(false); int id=BranchStore.reserveNative(c,BranchType.GROUP); Map<String,Object> values=new LinkedHashMap<>(BranchGroupMapper.initialValues(id,DOMAIN)); values.put("group_name",name); values.put("navbar",name); values.put("parent_group_id",parent); values.put("url_dir_name",url); BranchRowWriter.insertGroupWithId(c,BranchContext.live(DOMAIN,1),values);c.commit();liveGroups.add(id);return id; }
    }
    private int liveDoc(int group,String url) throws Exception {
        try(Connection c=DBPool.getConnection()) { c.setAutoCommit(false); int id=BranchStore.reserveNative(c,BranchType.DOCUMENT);Map<String,Object> values=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,Instant.now(),false));values.put("title","autotest LIVE");values.put("navbar","autotest LIVE");values.put("group_id",group);values.put("virtual_path",url);values.put("data","<p>autotest LIVE</p>");BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),values);c.commit();liveDocs.add(id);return id; }
    }
    private String fingerprint() throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            for(String table:List.of("documents","groups","documents_history","perex_group_doc","media","media_group_to_media","groups_scheduler","multigroup_mapping")) {
                List<String> rows=new ArrayList<>();
                try(ResultSet rs=s.executeQuery("SELECT * FROM `"+table+"`")) { while(rs.next()) { StringBuilder row=new StringBuilder(); for(int i=1;i<=rs.getMetaData().getColumnCount();i++) { String raw=rs.getString(i); byte[] value=raw==null?null:raw.getBytes(java.nio.charset.StandardCharsets.UTF_8); row.append(value==null?"NULL":Base64.getEncoder().encodeToString(value)).append('|'); } rows.add(row.toString()); } }
                Collections.sort(rows); for(String row:rows) digest.update(row.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private <T>T in(Call<T> callback) throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest(Constants.getServletContext()); request.setAttribute("csrfDisabled",true);
        Identity user=new Identity();user.setAdmin(true);user.setUserId(1);user.setValid(true);request.getSession().setAttribute(Constants.USER_KEY,user);
        RequestBean bean=new RequestBean();bean.setRequest(request);bean.setDomain(DOMAIN);
        try(var filter=mockStatic(SetCharacterEncodingFilter.class); var templates=mockStatic(TemplatesDB.class)) {
            templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));
            filter.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);filter.when(SetCharacterEncodingFilter::getEncoding).thenReturn("UTF-8");
            try(var scope=BranchContexts.open(request,context)) {
                EditorService editor=new EditorService(null,null,null,null,null,request);return callback.run(request,editor);
            }
        }
    }
    @FunctionalInterface private interface Call<T> { T run(MockHttpServletRequest request,EditorService editor) throws Exception; }
}
