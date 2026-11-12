package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.news.FieldEnum;
import sk.iway.iwcm.components.news.NewsActionBean;
import sk.iway.iwcm.components.news.NewsQuery;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;
import sk.iway.iwcm.components.news.criteria.ResultCriteria;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Real DB -> NewsActionBean -> Velocity rendering; no mocked content lists or SQL. */
@EnabledIfEnvironmentVariable(named="WEBJET_P03_DB",matches="true")
class BranchNewsRenderTest extends BaseWebjetTest {
    private static final int ROOT=1_930_001, CHILD=1_930_002, OUTSIDE=1_930_003;
    private static final int A=1_930_011, B=1_930_012, DELETED=1_930_013, MOVED=1_930_014, OWNER=1_930_015;
    private static final String DOMAIN="p03-news.example";
    private static final Instant CREATED=Instant.parse("2026-11-04T09:00:00Z");
    private final List<Integer> liveDocs=new ArrayList<>();
    private final List<Integer> liveGroups=new ArrayList<>();
    private final List<Long> branchIds=new ArrayList<>();
    private final BranchStore store=new BranchStore();
    private BranchContext branch;
    private int fresh;
    private MockHttpServletRequest request;
    private BranchContexts.Scope scope;
    private final Map<String,String> originalConfig=new LinkedHashMap<>();

    @BeforeEach void fixture() throws Exception {
        try(Connection c=DBPool.getConnection()) { assertEquals("webjet_p01",c.getCatalog()); assertTrue(c.getMetaData().getURL().contains(":33316/")); }
        for(String key:List.of("enableStaticFilesExternalDir","branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","docAuthorLazyLoad","perexGroupUseJoin")) originalConfig.put(key,Constants.getString(key));
        Constants.setBoolean("branchesEnabled",true);
        Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);
        Constants.setBoolean("docAuthorLazyLoad",true);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private"));
        Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
        group(ROOT,0,"Root"); group(CHILD,ROOT,"Child"); group(OUTSIDE,0,"Outside");
        document(A,ROOT,"Alpha",10,"intro Alpha"); document(B,OUTSIDE,"Beta LIVE",20,"intro Beta");
        document(DELETED,ROOT,"Removed",30,"intro removed"); document(MOVED,ROOT,"Moved",40,"intro moved");
        document(OWNER,OUTSIDE,"News owner",50,"owner");
        update("UPDATE groups SET default_doc_id=? WHERE group_id=?",A,ROOT);
        branch=newBranch("N01 fixture");
        RequestBean fixtureRequest=new RequestBean();fixtureRequest.setDomain(DOMAIN);SetCharacterEncodingFilter.setCurrentRequestBean(fixtureRequest);
        change(B,Map.of("group_id",ROOT,"title","Beta WORK","perex_group",",1,2,1930081,1930082,"),false);
        change(DELETED,Map.of(),true); change(MOVED,Map.of("group_id",OUTSIDE),false);
        Map<String,Object> values=docValues(1,ROOT,"Fresh WORK",20,"intro fresh");
        fresh=((Number)store.create(adminBranch(),BranchType.DOCUMENT,values).get("doc_id")).intValue();
        GroupsDB.getInstance(true); DocDB.getInstance(true);
        NewsTemplatesEntity template=new NewsTemplatesEntity(); template.setName("p03-proof");
        template.setTemplateCode("<ol class='news'>#foreach($item in $news)<li data-id='$item.docId'><a href='$actionBean.link($item)'>$item.title</a><span>$item.htmlData</span><img src='$item.perexImage'/></li>#end</ol>");
        template.setPagingCode("<nav>#foreach($item in $pagesAll)<a href='$item.url'>$item.label</a>#end</nav>");
        template.setPagingPosition(2);
        Cache.getInstance().setObjectSeconds("newsTemplatesService.templates",new java.util.HashMap<>(Map.of(BranchDomains.id(DOMAIN)+".p03-proof",template,"0.p03-proof",template)),600);
        update("INSERT INTO perex_groups(perex_group_id,perex_group_name,domain_id) VALUES(1930081,'#p03tag',?),(1930082,'@p03author',?)",CloudToolsForCore.getDomainId(),CloudToolsForCore.getDomainId());
        primePerexDefinitionCache();
        open(branch);
    }

    @AfterEach void cleanup() throws Exception {
        if(scope!=null)scope.close(); SetCharacterEncodingFilter.setCurrentRequestBean(null);
        try(Connection c=DBPool.getConnection()) {
            for(long id:branchIds)for(String table:List.of("branch_documents","branch_groups","branch_media","branch_files","branches"))
                update(c,"DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"=?",id);
            for(int id:liveDocs) { update(c,"DELETE FROM doc_atr WHERE doc_id=?",id);update(c,"DELETE FROM perex_group_doc WHERE doc_id=?",id);update(c,"DELETE FROM documents WHERE doc_id=?",id); }
            for(int id:liveGroups)update(c,"DELETE FROM groups WHERE group_id=?",id);
            update(c,"DELETE FROM doc_atr_def WHERE atr_id IN (1930091,1930092)");
            update(c,"DELETE FROM perex_groups WHERE perex_group_id IN (1930081,1930082)");
        } finally { originalConfig.forEach(Constants::setString);Cache.getInstance().removeObject("newsTemplatesService.templates");GroupsDB.getInstance(true);DocDB.getInstance(true); }
    }

    /** N01/N02: effective source precedes filters/count/paging in the real renderer. */
    @ParameterizedTest @ValueSource(booleans={false,true}) void n01n02OverlayCategoriesAndPaging(boolean join) {
        Constants.setBoolean("perexGroupUseJoin",join);
        NewsActionBean first=render(a->{a.setPageSize(2);a.setPaging(true);});
        assertIds(first,A,B);assertEquals(3,first.getPaginator(Prop.getInstance("sk")).get("totalCount"));
        NewsActionBean second=render(a->{a.setPageSize(2);a.setPaging(true);a.setPage(2);});assertIds(second,fresh);
        NewsActionBean included=render(a->{a.setNewsQuery(new NewsQuery().addCriteria(DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP,2)));});assertIds(included,B);
        assertIds(render(a->a.setPerexGroup(new int[]{2})),B);
        assertIds(render(a->a.setPerexGroupNot(new int[]{2})),A,fresh);
        request.addParameter("category",new String[]{"2","1930081"});assertIds(render(a->a.setRequestPerexGroupsName("category")),B);request.removeParameter("category");
        NewsActionBean excluded=render(a->{a.setNewsQuery(new NewsQuery().addCriteria(DatabaseCriteria.not(DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP,2))));});assertIds(excluded,A,fresh);
        open(BranchContext.live(DOMAIN,1));assertIds(render(a->{}),A,DELETED,MOVED);
        open(newBranch("second empty branch"));assertIds(render(a->{}),A,DELETED,MOVED);
    }

    /** P08: the real production renderer remains compatible with the global feature flag off and on. */
    @ParameterizedTest @ValueSource(booleans={false,true}) void p08ProductionRenderIgnoresUnpublishedBranchRows(boolean enabled) {
        open(BranchContext.live(DOMAIN,1));
        Constants.setBoolean("branchesEnabled",enabled);
        NewsActionBean action=render(a->{a.setPageSize(2);a.setPaging(true);});
        assertIds(action,A,DELETED);
        assertEquals(3,action.getPaginator(Prop.getInstance("sk")).get("totalCount"));
        assertFalse(action.getHtmlOut().contains("Beta WORK"));
        assertFalse(action.getHtmlOut().contains("Fresh WORK"));
        assertIds(render(a->{a.setPageSize(2);a.setPaging(true);a.setPage(2);}),MOVED);
        assertEquals("documents d",BranchSql.documentSource("d").sql());
    }

    /** N03: effective hierarchy and default-page selection are applied before SQL paging. */
    @Test void n03SubgroupsDefaultsAndInvalidRoots() throws Exception {
        Map<String,Object> group=new LinkedHashMap<>(BranchGroupMapper.initialValues(1,DOMAIN));group.put("group_name","New subfolder");group.put("parent_group_id",ROOT);
        int sub=((Number)store.create(adminBranch(),BranchType.GROUP,group).get("group_id")).intValue();
        int nested=((Number)store.create(adminBranch(),BranchType.DOCUMENT,docValues(1,sub,"Nested",5,"intro nested")).get("doc_id")).intValue();
        open(branch);
        assertIds(render(a->{a.setAlsoSubGroups(true);a.setSubGroupsDepth(1);}),nested,A,B,fresh);
        assertIds(render(a->a.setDocMode(1)),A);
        assertIds(render(a->a.setDocMode(2)),B,fresh);
        GroupDetails invalid=new GroupDetails();invalid.setGroupId(Integer.MAX_VALUE);
        assertIds(render(a->a.setGroupIds(List.of(invalid))));
        assertIds(render(a->a.setGroupIds((List<GroupDetails>)null)));
    }

    /** N04: availability, perex and each publication mode run through the renderer. */
    @Test void n04AvailabilityPerexAndPublicationTime() throws Exception {
        change(A,Map.of("available",false),false);change(B,Map.of("html_data",""),false);open(branch);
        assertIds(render(a->{}),fresh);assertIds(render(a->a.setPerexNotRequired(true)),B,fresh);
        long at=System.currentTimeMillis()/1000*1000+86_400_000;
        change(A,Map.of("available",true),false);
        update("UPDATE documents SET publish_start=?,publish_end=? WHERE doc_id=?",new Timestamp(at),new Timestamp(at),A);
        update("UPDATE branches SET state='SCHEDULED',publish_at=? WHERE id=?",new Timestamp(at),branch.branchId());
        open(branch);
        assertEquals(at,BranchView.current().previewAt());
        for(String mode:List.of("new","valid","next","old","all")) {
            NewsActionBean action=render(a->{a.setPublishType(mode);a.setPerexNotRequired(true);});
            assertFalse(action.getHtmlOut().contains("Removed"));
            if(List.of("new","all").contains(mode)) assertIds(action,A,B,fresh);
            if(List.of("valid","next","old").contains(mode)) assertIds(action,A);
        }
    }

    /** N12: refreshing after a term change updates News filters and the visible preview clock. */
    @Test void n12MovedCancelledAndStoppedScheduleUsesOneRequestTime() throws Exception {
        long first=System.currentTimeMillis()/1000*1000+86_400_000;
        long second=first+86_400_000;
        update("UPDATE documents SET publish_start=?,publish_end=NULL WHERE doc_id=?",new Timestamp(first+3_600_000),A);
        update("UPDATE branches SET state='SCHEDULED',publish_at=? WHERE id=?",new Timestamp(first),branch.branchId());
        open(branch);
        BranchView initial=BranchView.current();
        assertEquals(first,initial.previewAt());assertTrue(initial.scheduledPreview());
        assertIds(render(a->a.setPublishType("new")),B,fresh);
        String firstBanner=Jsoup.parse(BranchPreviewUrls.rewriteHtml("<html><body></body></html>",request)).selectFirst("#branch-preview-status").text();
        assertTrue(firstBanner.contains(Prop.getInstance("sk").getText("components.branches.preview.newsPrediction")),firstBanner);
        assertTrue(firstBanner.contains(java.time.ZoneId.systemDefault().getId()),firstBanner);

        update("UPDATE branches SET publish_at=? WHERE id=?",new Timestamp(second),branch.branchId());
        // A page with several News components retains one instant until the next request.
        assertSame(initial,BranchView.current());assertEquals(first,BranchView.current().previewAt());
        assertIds(render(a->a.setPublishType("new")),B,fresh);
        open(branch);
        assertEquals(second,BranchView.current().previewAt());
        assertIds(render(a->a.setPublishType("new")),A,B,fresh);
        String secondBanner=Jsoup.parse(BranchPreviewUrls.rewriteHtml("<html><body></body></html>",request)).selectFirst("#branch-preview-status").text();
        assertNotEquals(firstBanner,secondBanner);

        for(String state:List.of("DRAFT","NEEDS_ATTENTION")) {
            update("UPDATE branches SET state=?,publish_at=? WHERE id=?",state,"DRAFT".equals(state)?null:new Timestamp(second),branch.branchId());
            long before=System.currentTimeMillis();open(branch);
            BranchView current=BranchView.current();
            assertFalse(current.scheduledPreview());assertTrue(current.previewAt()>=before);assertTrue(current.previewAt()<=System.currentTimeMillis());
            assertIds(render(a->a.setPublishType("new")),B,fresh);
            String banner=Jsoup.parse(BranchPreviewUrls.rewriteHtml("<html><body></body></html>",request)).selectFirst("#branch-preview-status").text();
            assertFalse(banner.contains(Prop.getInstance("sk").getText("components.branches.preview.newsPrediction")),banner);
        }
        update("UPDATE branches SET state='SCHEDULED',publish_at=? WHERE id=?",new Timestamp(first-172_800_000),branch.branchId());
        long before=System.currentTimeMillis();open(branch);
        assertFalse(BranchView.current().scheduledPreview());assertTrue(BranchView.current().previewAt()>=before);
    }

    /** N05: all order fields, loaded data, offsets and repeated text values use real SQL. */
    @Test void n05FiltersOrderLoadDataAndOffsets() {
        for(NewsQuery.OrderEnum order:NewsQuery.OrderEnum.values()) {
            NewsActionBean action=render(a->{a.setOrder(order.name());a.setLoadData(true);});
            assertIds(action,A,B,fresh);assertTrue(action.getNewsList().stream().allMatch(d->d.getData().contains("body")));
            assertEquals(3,Jsoup.parse(action.getHtmlOut()).select("li[data-id]").size());
        }
        assertIds(render(a->{a.setOrder("title");a.setAscending(false);}),fresh,B,A);
        assertIds(render(a->{a.setOrder("priority");a.setAscending(false);}),B,fresh,A);
        assertIds(render(a->a.setFilter(Map.of("title_co",List.of("Beta","Fresh")))),B,fresh);
        assertIds(render(a->a.setSearch(Map.of("title_co",List.of("Beta","Fresh")))),B,fresh);
        assertIds(render(a->a.setTag("p03tag")),B);assertIds(render(a->a.setAuthor("p03author")),B);
        assertIds(render(a->{a.setPageSize(1);a.setOffset(1);}),B);
        NewsActionBean none=render(a->a.setFilter(Map.of("title_co",List.of("absent"))));assertIds(none);assertNull(none.getPaginator(Prop.getInstance("sk")));
    }

    /** N06: multiple matching attribute rows never duplicate a document or reorder enrichment. */
    @Test void n06AttributeOrAndCustomCriteriaBoundary() throws Exception {
        update("INSERT INTO doc_atr_def(atr_id,atr_name,atr_type) VALUES(1930091,'p03colour',0),(1930092,'p03size',0)");
        for(int id:List.of(A,B)) {update("INSERT INTO doc_atr(doc_id,atr_id,value_string) VALUES(?,1930091,'red'),(?,1930092,'large')",id,id);}
        NewsQuery query=new NewsQuery().addCriteria(DatabaseCriteria.or(DatabaseCriteria.Atribute.equal("p03colour","red"),DatabaseCriteria.Atribute.equal("p03size","large")));
        NewsActionBean action=render(a->{a.setNewsQuery(query);a.setReturnDocsWithAtributes(true);});assertIds(action,A,B);assertEquals(2,query.getNewsCount());
        assertThrows(IllegalStateException.class,()->new NewsQuery().addCriteria(ResultCriteria.perexNotEmpty()));
        assertThrows(IllegalStateException.class,()->render(a->a.setContextClasses("java.lang.System")));
    }

    /** N07: protected pages honor visitor groups and two components share request deduplication only. */
    @Test void n07ProtectionAndRepeatedComponents() throws Exception {
        change(B,Map.of("password_protected",",77,"),false);open(branch);
        assertIds(render(a->{}),A,fresh);
        Identity user=new Identity();user.setUserId(1);user.setUserGroupsIds("77");request.getSession().setAttribute(Constants.USER_KEY,user);
        assertIds(render(a->{}),A,B,fresh);
        assertIds(render(a->{a.setCheckDuplicity(true);a.setPageSize(1);}),A);
        assertIds(render(a->{a.setCheckDuplicity(true);a.setPageSize(1);}),B);
        open(branch);assertIds(render(a->{a.setCheckDuplicity(true);a.setPageSize(1);}),A);
    }

    /** Seeds the normal immutable definition cache from actual fixture rows, without a Spring web context. */
    private void primePerexDefinitionCache() throws Exception {
        List<PerexGroupBean> definitions=new ArrayList<>();
        try(Connection c=DBPool.getConnection();PreparedStatement p=c.prepareStatement("SELECT perex_group_id,perex_group_name FROM perex_groups WHERE perex_group_id IN (1930081,1930082)");ResultSet r=p.executeQuery()) {
            while(r.next()){PerexGroupBean definition=new PerexGroupBean();definition.setPerexGroupId(r.getInt(1));definition.setPerexGroupName(r.getString(2));definitions.add(definition);}
        }
        var field=DocDB.class.getDeclaredField("perexGroupsByDomainIdMap");field.setAccessible(true);field.set(DocDB.getInstance(),new java.util.HashMap<>(Map.of(CloudToolsForCore.getDomainId(),definitions)));
    }
    private NewsActionBean render(Consumer<NewsActionBean> settings) {
        NewsActionBean action=new NewsActionBean(); ActionBeanContext context=new ActionBeanContext();context.setRequest(request);context.setResponse(new MockHttpServletResponse());action.setContext(context);
        GroupDetails root=new GroupDetails();root.setGroupId(ROOT);action.setGroupIds(List.of(root));action.setTemplate("p03-proof");action.setPublishType("all");action.setOrder("priority");action.setAscending(true);action.setPageSize(100);
        settings.accept(action);action.news();return action;
    }
    private void assertIds(NewsActionBean action,int... expected) {
        List<Integer> ids=java.util.Arrays.stream(expected).boxed().toList();
        assertEquals(ids,action.getNewsList().stream().map(DocDetails::getDocId).toList());
        assertEquals(ids,Jsoup.parse(action.getHtmlOut()).select("li[data-id]").stream().map(e->Integer.parseInt(e.attr("data-id"))).toList(),action.getHtmlOut());
    }
    private void open(BranchContext value) {
        if(scope!=null)scope.close();request=new MockHttpServletRequest(Constants.getServletContext());request.setServerName("admin.example");request.setRequestURI("/admin/webpages/branch-preview/"+value.branchId()+"/news.html");request.setAttribute("docDetails",new DocDetails());request.setAttribute("lng","sk");
        RequestBean bean=new RequestBean();bean.setRequest(request);bean.setDomain(DOMAIN);bean.setLng("sk");SetCharacterEncodingFilter.setCurrentRequestBean(bean);scope=BranchContexts.open(request,value);
    }
    private BranchContext adminBranch(){return new BranchContext(branch.branchId(),DOMAIN,1,BranchContext.Mode.ADMIN);}
    private BranchContext newBranch(String name) {long id=new BranchService().create(BranchTestDomains.ensure(DOMAIN),"P03 News "+name,"N01-N08 render fixture",1).getId();branchIds.add(id);return new BranchContext(id,DOMAIN,1,BranchContext.Mode.PREVIEW);}
    private void group(int id,int parent,String name) throws Exception {
        Map<String,Object> values=new LinkedHashMap<>(BranchGroupMapper.initialValues(id,DOMAIN));values.put("group_name",name);values.put("parent_group_id",parent);values.put("url_dir_name",name.toLowerCase());
        try(Connection c=DBPool.getConnection()){BranchStore.insert(c,"groups",values);}liveGroups.add(id);
    }
    private Map<String,Object> docValues(int id,int group,String title,int priority,String perex) {Map<String,Object> values=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,CREATED,false));values.put("group_id",group);values.put("title",title);values.put("navbar",title);values.put("data","<p>body "+title+"</p>");values.put("html_data",perex);values.put("available",true);values.put("show_in_menu",true);values.put("sort_priority",priority);values.put("virtual_path","/p03-news/"+id+".html");values.put("perex_group",",1,");return values;}
    private void document(int id,int group,String title,int priority,String perex) throws Exception {try(Connection c=DBPool.getConnection()){BranchStore.insert(c,"documents",docValues(id,group,title,priority,perex));}liveDocs.add(id);}
    private void change(int id,Map<String,Object> fields,boolean delete) throws Exception {
        Map<String,Object> current;try(Connection c=DBPool.getConnection()){current=BranchStore.readWork(c,branch.branchId(),BranchType.DOCUMENT,id).orElseGet(()->{try{return BranchStore.readLive(c,BranchType.DOCUMENT,id).orElseThrow();}catch(Exception e){throw new IllegalStateException(e);}});}
        Map<String,Object> work=new LinkedHashMap<>(current);work.putAll(fields);store.put(adminBranch(),BranchType.DOCUMENT,id,work,BranchStore.snapshot(BranchType.DOCUMENT,current).hash(),delete);
    }
    private void update(String sql,Object... args)throws Exception{try(Connection c=DBPool.getConnection()){update(c,sql,args);}}
    private void update(Connection c,String sql,Object... args)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();}}
}
