package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.*;
import sk.iway.iwcm.components.menu.MenuULLI;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Real DB-backed effective content and unchanged MenuULLI renderer checks. */
@EnabledIfEnvironmentVariable(named="WEBJET_P03_DB",matches="true")
class BranchViewTest extends BaseWebjetTest {
    private static final String DOMAIN="p03-menu.example";
    private final List<Long> branches=new ArrayList<>();
    private final Map<String,String> configuration=new LinkedHashMap<>();
    private final List<Integer> docs=new ArrayList<>(), groups=new ArrayList<>();
    private BranchContext a,b;
    private int root,section,page,child;
    private final BranchStore store=new BranchStore();

    @BeforeEach void fixture() throws Exception {
        try (Connection connection=DBPool.getConnection()) {
            assertEquals("webjet_p01",connection.getCatalog());
            assertTrue(connection.getMetaData().getURL().contains(":33316/"));
        }
        for(String key:List.of("enableStaticFilesExternalDir","branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","multiDomainEnabled","templatesUseRecursiveSystemFolder")) configuration.put(key,Constants.getString(key));
        Constants.setBoolean("branchesEnabled",true); Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private")); Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
        Constants.setBoolean("multiDomainEnabled",true); Constants.setBoolean("templatesUseRecursiveSystemFolder",false);
        root=liveGroup("Menu root",0,"p03-menu"); section=liveGroup("Original section",root,"original");
        page=liveDoc("Root page",root,"/p03-menu/",10); child=liveDoc("Original page",section,"/p03-menu/original/",20);
        try(Connection connection=DBPool.getConnection();Statement statement=connection.createStatement()) {
            statement.executeUpdate("UPDATE `groups` SET default_doc_id="+page+" WHERE group_id="+root);
            statement.executeUpdate("UPDATE `groups` SET default_doc_id="+child+" WHERE group_id="+section);
        }
        a=branch("P03 menu A"); b=branch("P03 menu B");
        work(a,BranchType.GROUP,section,Map.of("group_name","Changed section","navbar","Changed navigation","url_dir_name","changed"),false);
        work(a,BranchType.DOCUMENT,child,Map.of("title","Changed page","navbar","Changed page","data","<p>WORK body</p>","html_data","WORK perex","virtual_path","/p03-menu/changed/"),false);
        work(b,BranchType.DOCUMENT,child,Map.of("title","Second branch","navbar","Second branch","virtual_path","/p03-menu/second/"),false);
    }

    @AfterEach void cleanup() throws Exception {
        try(Connection connection=DBPool.getConnection();Statement statement=connection.createStatement()) {
            for(long id:branches) for(String table:List.of("branch_files","branch_media","branch_documents","branch_groups","branches"))
                statement.executeUpdate("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+id);
            for(int id:docs) statement.executeUpdate("DELETE FROM documents WHERE doc_id="+id);
            for(int id:groups) statement.executeUpdate("DELETE FROM `groups` WHERE group_id="+id);
        } finally { configuration.forEach(Constants::setString); }
    }

    @Test void isolatedDetailUrlTreeAndRealMenu() throws Exception {
        in(a,request->{
            assertEquals("Changed page",DocDB.getInstance().getDoc(child).getTitle());
            assertEquals("Changed section",GroupsDB.getInstance().getGroup(section).getGroupName());
            assertTrue(GroupsDB.getInstance().getGroup(section).getNavbar().contains("/p03-menu/changed/"));
            assertEquals(child,DocDB.getInstance().getVirtualPathDocId("/p03-menu/changed/",DOMAIN));
            assertEquals(-1,DocDB.getInstance().getVirtualPathDocId("/p03-menu/original/",DOMAIN));
            assertEquals("/p03-menu/changed/",DocDB.getInstance().getDocLink(child));
            assertEquals("p03-menu.example",DocDB.getDomain("transport.admin.example",request));
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));
            request.setAttribute("ulLiOpenAllItems","true");
            for(String classes:List.of("full","basic","bootstrap","none")) {
                String html=MenuULLI.doTree(root,-1,0,classes,"p03-menu",request);
                assertTrue(html.contains("Changed navigation"),html); assertTrue(html.contains("/p03-menu/changed/"),html);
                if("full".equals(classes)) assertTrue(html.contains("menuULDirOpen group"+section),html);
            }
            assertEquals(Integer.toString(root),request.getAttribute("!ROOT_GROUP_ID!"));
            assertEquals("",MenuULLI.doTree(Integer.MAX_VALUE,-1,0,request));
            assertNull(request.getAttribute("!ROOT_GROUP_ID!"));
            assertTrue(MenuULLI.doTree(0,-1,0,request).contains("Changed navigation"));
            return null;
        });
        in(b,request->{ assertEquals("Second branch",DocDB.getInstance().getDoc(child).getTitle()); return null; });
        try(Connection connection=DBPool.getConnection()) {
            assertEquals("Original page",BranchStore.readLive(connection,BranchType.DOCUMENT,child).orElseThrow().get("title"));
        }
    }

    @Test void newDefaultMoveDeleteAndHiddenRoot() throws Exception {
        Map<String,Object> folder=new LinkedHashMap<>(BranchGroupMapper.initialValues(1,DOMAIN));
        folder.put("group_name","Added section");folder.put("navbar","Added navigation");folder.put("parent_group_id",root);folder.put("url_dir_name","added");
        Map<String,Object> saved=store.create(a,BranchType.GROUP,folder); int added=((Number)saved.get("group_id")).intValue();
        Map<String,Object> doc=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.now(),false));
        doc.put("title","New default");doc.put("navbar","New default");doc.put("group_id",added);doc.put("available",true);doc.put("show_in_menu",true);doc.put("virtual_path","/p03-menu/added/");
        Map<String,Object> created=store.create(a,BranchType.DOCUMENT,doc);int newId=((Number)created.get("doc_id")).intValue();
        Map<String,Object> changed=new LinkedHashMap<>(saved);changed.put("default_doc_id",newId);
        store.put(a,BranchType.GROUP,added,changed,BranchStore.snapshot(BranchType.GROUP,saved).hash(),false);
        Map<String,Object> sectionRow;
        try(Connection connection=DBPool.getConnection()){sectionRow=BranchStore.readWork(connection,a.branchId(),BranchType.GROUP,section).orElseThrow();}
        changed=new LinkedHashMap<>(sectionRow);changed.put("parent_group_id",added);
        store.put(a,BranchType.GROUP,section,changed,BranchStore.snapshot(BranchType.GROUP,sectionRow).hash(),false);
        in(a,request->{
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));request.setAttribute("ulLiOpenAllItems","true");
            String html=MenuULLI.doTree(root,-1,0,"full",null,request);
            var tree=org.jsoup.Jsoup.parse(html);var addedItem=tree.selectFirst("li.group"+added);
            assertNotNull(addedItem,html);assertNotNull(addedItem.selectFirst("li.group"+section),html);
            assertTrue(addedItem.html().contains("/p03-menu/added/"),html);
            assertTrue(html.contains("menuULDirOpen group"+added),html);
            return null;
        });
        work(a,BranchType.DOCUMENT,child,Map.of(),true);
        in(a,request->{ assertNull(DocDB.getInstance().getDoc(child));assertEquals(-1,BranchReadService.docIdFromUrl("/p03-menu/changed/"));return null;});
    }

    @Test void previewDecisionLiveWorkNoopAndStaleChoice() throws Exception {
        setChoice(a,child,"LIVE");
        in(preview(a),request->{ assertEquals("Original page",DocDB.getInstance().getDoc(child).getTitle());return null;});
        in(a,request->{ assertEquals("Changed page",DocDB.getInstance().getDoc(child).getTitle());return null;});
        setChoice(a,child,"WORK");
        in(preview(a),request->{ assertEquals("Changed page",DocDB.getInstance().getDoc(child).getTitle());return null;});
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE documents SET title='LIVE changed' WHERE doc_id=?")) {statement.setInt(1,child);statement.executeUpdate();}
        assertThrows(BranchDecision.StaleChoiceException.class,()->in(preview(a),request->{BranchView.current();return null;}));
    }

    @Test void rootDepthForceVisibilityAndVariables() throws Exception {
        work(a,BranchType.GROUP,section,Map.of("menu_type",GroupDetails.MENU_TYPE_HIDDEN),false);
        int item=liveDoc("Visible child",section,"/p03-menu/visible.html",30);
        liveGroup("Visible subfolder",section,"nested");
        in(a,request->{
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(item));
            request.setAttribute("ulLiOpenAllItems","true");
            assertFalse(MenuULLI.doTree(root,-1,0,request).contains("Changed navigation"));
            String explicit=MenuULLI.doTree(section,-1,0,request);
            assertTrue(explicit.contains("Visible subfolder"),explicit);
            assertEquals(Integer.toString(section),request.getAttribute("!ROOT_GROUP_ID!"));
            request.setAttribute("includePageParams","onlyFolders=true");
            assertFalse(MenuULLI.doTree(section,-1,0,request).contains("Visible child"));
            request.setAttribute("includePageParams","menuIncludePerex=true");
            request.setAttribute("ulLiForceDocid",Integer.toString(Integer.MAX_VALUE));
            assertEquals("",MenuULLI.doTree(0,-1,0,request));
            assertNull(request.getAttribute("!ROOT_GROUP_ID!"));
            request.removeAttribute("ulLiForceDocid");
            request.setAttribute("ulLiSkipMenuGeneration","true");
            assertEquals("",MenuULLI.doTree(section,-1,0,request));
            assertEquals(Integer.toString(section),request.getAttribute("!ROOT_GROUP_ID!"));
            assertNull(request.getAttribute("ulLiSkipMenuGeneration"));
            return null;
        });
        work(a,BranchType.GROUP,section,Map.of("menu_type",GroupDetails.MENU_TYPE_NORMAL),false);
        in(a,request->{
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(item));request.setAttribute("ulLiOpenAllItems","true");
            assertTrue(MenuULLI.doTree(0,-1,1,request).contains("Visible child"));
            assertEquals("",MenuULLI.doTree(0,-1,99,request));
            assertFalse(MenuULLI.doTree(root,1,0,request).contains("Visible child"));
            request.setAttribute("includePageParams","onlyFolders=true");
            assertFalse(MenuULLI.doTree(root,-1,0,request).contains("Visible child"));
            return null;
        });
    }

    @Test void perexFullHtmlAvailabilityProtectionAndSitemap() throws Exception {
        int extra=liveDoc("Protected item",section,"/p03-menu/protected.html",30);
        work(a,BranchType.DOCUMENT,extra,Map.of("password_protected",",999999,","logged_show_in_menu",false),false);
        in(a,request->{
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));request.setAttribute("ulLiOpenAllItems","true");
            String perex=MenuULLI.doTree(root,-1,0,request);
            assertTrue(perex.contains("WORK perex"),perex);
            assertTrue(perex.contains("Protected item"),perex);assertTrue(perex.contains("protected"),perex);
            request.setAttribute("includePageParams","menuInfoDirName=Changed navigation");
            String info=MenuULLI.doTree(root,-1,0,request);
            assertTrue(info.contains("WORK body"),info);assertFalse(info.contains("LIVE body"),info);
            Identity user=new Identity();user.setUserId(1);user.setValid(true);request.getSession().setAttribute(Constants.USER_KEY,user);
            request.setAttribute("includePageParams","menuIncludePerex=true");
            assertFalse(MenuULLI.doTree(root,-1,0,request).contains("Protected item"));
            request.getSession().setAttribute("menuDisabledGroup"+section,"true");
            assertFalse(MenuULLI.doTree(root,-1,0,request).contains("Changed navigation"));
            return null;
        });
        work(a,BranchType.DOCUMENT,child,Map.of("available",false),false);
        in(a,request->{request.setAttribute("docDetails",DocDB.getInstance().getDoc(page));request.setAttribute("ulLiOpenAllItems","true");assertFalse(MenuULLI.doTree(root,-1,0,request).contains("Changed navigation"));return null;});
        work(a,BranchType.DOCUMENT,child,Map.of("available",true),false);
        work(a,BranchType.DOCUMENT,extra,Map.of("show_in_sitemap",false,"show_in_menu",true),false);
        in(a,request->{
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));request.setAttribute("ulLiOpenAllItems","true");
            assertTrue(MenuULLI.doTree(root,-1,0,request).contains("Protected item"));
            assertFalse(MenuULLI.doTree(root,-1,0,"full",null,-1,true,request).contains("Protected item"));
            return null;
        });
    }

    @Test void equalVersionsUseLiveAndDeletedDefaultHasNoLink() throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            Map<String,Object> live=BranchStore.readLive(connection,BranchType.DOCUMENT,child).orElseThrow();
            work(a,BranchType.DOCUMENT,child,live,false);
        }
        in(preview(a),request->{
            assertFalse(BranchView.current().usesWork(BranchType.DOCUMENT,Integer.toString(child)));
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));request.setAttribute("ulLiOpenAllItems","true");
            request.setAttribute("includePageParams","menuInfoDirName=Changed navigation");
            String html=MenuULLI.doTree(root,-1,0,request);assertTrue(html.contains("LIVE body"),html);return null;
        });
        work(a,BranchType.DOCUMENT,child,Map.of("title","Equal work","navbar","Equal work"),false);
        try(Connection connection=DBPool.getConnection()) {
            Map<String,Object> row=BranchStore.readWork(connection,a.branchId(),BranchType.DOCUMENT,child).orElseThrow();
            try(PreparedStatement statement=connection.prepareStatement("UPDATE documents SET title=?,navbar=? WHERE doc_id=?")) {
                statement.setObject(1,row.get("title"));statement.setObject(2,row.get("navbar"));statement.setInt(3,child);statement.executeUpdate();
            }
        }
        in(preview(a),request->{assertEquals("Equal work",DocDB.getInstance().getDoc(child).getTitle());assertFalse(BranchView.current().usesWork(BranchType.DOCUMENT,Integer.toString(child)));return null;});
        work(a,BranchType.DOCUMENT,child,Map.of(),true);
        in(a,request->{request.setAttribute("docDetails",DocDB.getInstance().getDoc(page));request.setAttribute("ulLiOpenAllItems","true");String html=MenuULLI.doTree(root,-1,0,request);assertFalse(html.contains("Changed navigation"),html);return null;});
    }

    @Test void refusesProductionWritersAndOrphanCycle() throws Exception {
        in(a,request->{
            assertNull(GroupsDB.getInstance().getTrashGroup());
            assertThrows(IllegalStateException.class,()->DocDB.saveDoc(DocDB.getInstance().getDoc(child)));
            assertThrows(IllegalStateException.class,()->DocDB.deleteDoc(child,request));
            assertThrows(IllegalStateException.class,()->GroupsDB.getInstance().save(GroupsDB.getInstance().getGroup(section)));
            assertThrows(IllegalStateException.class,()->GroupsDB.getInstance().getCreateGroup("/P03-missing-system-fixture"));
            assertThrows(IllegalStateException.class,()->GroupsDB.getInstance().getCreateGroup("foreign.example:/System/Missing"));
            assertEquals(DOMAIN,SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
            return null;
        });
        work(a,BranchType.GROUP,root,Map.of("parent_group_id",section),false);
        assertThrows(IllegalStateException.class,()->in(a,request->{BranchView.current();return null;}));
    }

    @Test void legacySqlListsCountAndPagingUseEffectiveSource() throws Exception {
        int extra=liveDoc("Extra article",section,"/p03-menu/extra.html",30);
        work(a,BranchType.DOCUMENT,extra,Map.of("title","Moved outside filter","group_id",root),false);
        Map<String,Object> doc=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.now(),false));
        doc.put("title","New article");doc.put("navbar","New article");doc.put("group_id",section);doc.put("available",true);doc.put("html_data","New perex");doc.put("sort_priority",10);doc.put("virtual_path","/p03-menu/new.html");
        Map<String,Object> created=store.create(a,BranchType.DOCUMENT,doc);int id=((Number)created.get("doc_id")).intValue();
        in(a,request->{
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));request.setAttribute("includePageParams","paging=true,noData=true");
            request.addParameter("docid",Integer.toString(child));
            int total=DocDB.getInstance().getDocPerex(Integer.toString(section),DocDB.ORDER_PRIORITY,true,DocDB.PUBLISH_ALL,1,"p03LegacyDocs","p03LegacyPager",request);
            assertEquals(2,total);
            List<?> first=(List<?>)request.getAttribute("p03LegacyDocs");assertEquals(1,first.size());
            assertEquals(id,((DocDetails)first.get(0)).getDocId());
            request.setParameter("page","2");
            assertEquals(2,DocDB.getInstance().getDocPerex(Integer.toString(section),DocDB.ORDER_PRIORITY,true,DocDB.PUBLISH_ALL,1,"p03LegacyDocs","p03LegacyPager",request));
            List<?> second=(List<?>)request.getAttribute("p03LegacyDocs");assertEquals(1,second.size());assertEquals(child,((DocDetails)second.get(0)).getDocId());
            assertEquals(2,DocDB.getInstance().getDocCountInGroup(section));
            // Exercise timestamp binds after the effective source's parameters, in both list and count.
            for (int publishType : List.of(DocDB.PUBLISH_NEW,DocDB.PUBLISH_OLD,DocDB.PUBLISH_NEXT)) {
                int expected=publishType==DocDB.PUBLISH_NEW?2:0;
                assertEquals(expected,DocDB.getInstance().getDocPerex(Integer.toString(section),DocDB.ORDER_PRIORITY,true,publishType,1,"p03LegacyDocs","p03LegacyPager",request));
                assertEquals(expected>0?1:0,((List<?>)request.getAttribute("p03LegacyDocs")).size());
            }
            request.setParameter("datum","01.01.2020");
            assertEquals(0,DocDB.getInstance().getDocPerex(Integer.toString(section),DocDB.ORDER_PRIORITY,true,DocDB.PUBLISH_ALL,1,"p03LegacyDocs","p03LegacyPager",request));
            assertTrue(((List<?>)request.getAttribute("p03LegacyDocs")).isEmpty());
            return null;
        });
    }

    @Test void detectsLiveTrashAncestorsWithoutCreatingSystemFolders() throws Exception {
        int system=liveGroup("System",root,"system");
        String trashPath=sk.iway.iwcm.i18n.Prop.getInstance(Constants.getString("defaultLanguage")).getText("config.trash_dir");
        int trash=liveGroup(trashPath.substring(trashPath.lastIndexOf('/')+1),system,"trash");
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE `groups` SET parent_group_id=? WHERE group_id=?")) {
            statement.setInt(1,trash);statement.setInt(2,section);statement.executeUpdate();
        }
        in(preview(a),request->{
            assertTrue(BranchView.current().hasConflicts());
            assertEquals("Changed page",DocDB.getInstance().getDoc(child).getTitle());
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(child));request.setAttribute("ulLiOpenAllItems","true");
            assertTrue(MenuULLI.doTree(root,-1,0,request).contains("Changed navigation"));return null;
        });
    }

    @Test void inheritedLiveProjectionKeepsFullHtmlAndMetadataOmitsIt() throws Exception {
        int inherited=liveDoc("Inherited article",section,"/old/inherited.html",50);
        int generated=liveDoc("Generated article",section,"/unique-generated-2.html",60);
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE documents SET generate_url_from_title=1 WHERE doc_id=?")) {
            statement.setInt(1,generated);statement.executeUpdate();
        }
        in(b,request->{assertEquals("/unique-generated-2.html",DocDB.getInstance().getDocLink(generated));return null;});
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE documents SET url_inherit_group=1,editor_virtual_path='inherited.html' WHERE doc_id=?")) {
            statement.setInt(1,inherited);statement.executeUpdate();
        }
        in(a,request->{
            DocDetails full=DocDB.getInstance().getDoc(inherited);
            assertEquals("<p>LIVE body</p>",full.getData());
            assertTrue(full.getVirtualPath().contains("changed"),full.getVirtualPath());
            assertFalse(BranchView.current().usesWork(BranchType.DOCUMENT,Integer.toString(inherited)));
            assertEquals(sk.iway.iwcm.editor.service.WebpagesService.DATA_NOT_LOADED,DocDB.getInstance().getBasicDocDetails(inherited,false).getData());
            assertTrue(BranchView.current().documents(true).stream().allMatch(doc->sk.iway.iwcm.editor.service.WebpagesService.DATA_NOT_LOADED.equals(doc.getData())));
            assertEquals(inherited,BranchReadService.docIdFromUrl(full.getVirtualPath()));
            return null;
        });
    }

    private void setChoice(BranchContext context,int id,String source) throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            Map<String,Object> work=BranchStore.readWork(connection,context.branchId(),BranchType.DOCUMENT,id).orElseThrow();
            Map<String,Object> live=BranchStore.readLive(connection,BranchType.DOCUMENT,id).orElseThrow();
            try(PreparedStatement statement=connection.prepareStatement("UPDATE branch_documents SET resolved_choice=?,resolved_live_hash=?,resolved_work_hash=? WHERE branch_id=? AND doc_id=?")) {
                statement.setString(1,source);statement.setString(2,BranchStore.snapshot(BranchType.DOCUMENT,live).hash());statement.setString(3,BranchStore.snapshot(BranchType.DOCUMENT,work).hash());statement.setLong(4,context.branchId());statement.setInt(5,id);statement.executeUpdate();
            }
        }
    }
    private int liveGroup(String name,int parent,String url) throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);int id=BranchStore.reserveNative(connection,BranchType.GROUP);
            Map<String,Object> row=new LinkedHashMap<>(BranchGroupMapper.initialValues(id,DOMAIN));row.put("group_name",name);row.put("navbar",name);row.put("parent_group_id",parent);row.put("url_dir_name",url);
            BranchRowWriter.insertGroupWithId(connection,BranchContext.live(DOMAIN,1),row);connection.commit();groups.add(id);return id;
        }
    }
    private int liveDoc(String title,int group,String url,int priority) throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);int id=BranchStore.reserveNative(connection,BranchType.DOCUMENT);
            Map<String,Object> row=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,Instant.now(),false));row.put("title",title);row.put("navbar",title);row.put("group_id",group);row.put("virtual_path",url);row.put("available",true);row.put("show_in_menu",true);row.put("sort_priority",priority);row.put("html_data","LIVE perex");row.put("data","<p>LIVE body</p>");
            BranchRowWriter.insertDocumentWithId(connection,BranchContext.live(DOMAIN,1),row);connection.commit();docs.add(id);return id;
        }
    }
    private BranchContext branch(String name) {var row=new BranchService().create(BranchTestDomains.ensure(DOMAIN),name,"P03 render fixture",1);branches.add(row.getId());return new BranchContext(row.getId(),DOMAIN,1,BranchContext.Mode.ADMIN);}
    private static BranchContext preview(BranchContext context){return new BranchContext(context.branchId(),context.domainName(),context.userId(),BranchContext.Mode.PREVIEW);}
    private void work(BranchContext context,BranchType type,int id,Map<String,Object> changes,boolean delete) throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            Map<String,Object> before=BranchStore.readWork(connection,context.branchId(),type,id).orElseGet(()->{try{return BranchStore.readLive(connection,type,id).orElseThrow();}catch(SQLException ex){throw new IllegalStateException(ex);}});
            Map<String,Object> after=new LinkedHashMap<>(before);after.putAll(changes);
            store.put(context,type,id,after,BranchStore.snapshot(type,before).hash(),delete);
        }
    }
    private <T>T in(BranchContext context,Call<T> call) throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest(Constants.getServletContext());
        request.setAttribute("includePageParams","menuIncludePerex=true");
        RequestBean bean=new RequestBean();bean.setDomain(DOMAIN);
        try(var mock=mockStatic(SetCharacterEncodingFilter.class)) {
            mock.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);
            mock.when(SetCharacterEncodingFilter::getEncoding).thenReturn("UTF-8");
            try(var scope=BranchContexts.open(request,context)){return call.run(request);}
        }
    }
    @FunctionalInterface private interface Call<T>{T run(MockHttpServletRequest request)throws Exception;}
}
