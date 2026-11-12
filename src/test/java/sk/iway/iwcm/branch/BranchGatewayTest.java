package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilterIway;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.jasper.servlet.JasperInitializer;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import sk.iway.iwcm.*;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.test.BaseWebjetTest;

/** HTTP proof using production BranchFilter/readers and unchanged Jasper menu/News JSPs. */
@EnabledIfEnvironmentVariable(named="WEBJET_P03_DB",matches="true")
class BranchGatewayTest extends BaseWebjetTest {
    private static final Path WEBAPP=Path.of("src/main/webapp").toAbsolutePath();
    private static final String TEMPLATE="/templates/aceintegration/jet/blank.jsp",MENU="/components/menu/menu_ul_li.jsp";
    private static final String DOMAIN="p03-gateway.example";
    private static final int ROOT=1_940_001,CHILD=1_940_002,TARGET=1_940_003,NESTED=1_940_004,HOME=1_940_011,PAGE=1_940_012,SECOND=1_940_013,DELETED=1_940_014,EXTRA=1_940_015,NESTED_PAGE=1_940_016;
    private final List<Integer> liveDocs=new ArrayList<>(),liveGroups=new ArrayList<>();
    private final List<Long> branchIds=new ArrayList<>();
    private final String token=UUID.randomUUID().toString();
    private final BranchStore store=new BranchStore();
    private final Map<String,String> config=new LinkedHashMap<>();
    private BranchContext a,b,forbidden;
    private int newGroup,newPage;
    private Path root;

    /** N08/M01-M09 integration: real JSPs, active menu path, paging, files and isolation. */
    @Test void productionGatewayRealJspNewsMenuAndFiles() throws Exception {
        ServletContext originalContext=Constants.getServletContext();
        Tomcat server=new Tomcat();
        var originalSpring=sk.iway.iwcm.system.spring.components.SpringContext.getApplicationContext();
        var spring=new org.springframework.context.support.GenericApplicationContext();spring.refresh();
        new sk.iway.iwcm.system.spring.components.SpringContext().setApplicationContext(spring);
        try {
            root=Files.createTempDirectory("webjet-p03-gateway-");prepareRoot();
            Constants.setServletContext(new org.springframework.mock.web.MockServletContext(root.toString()) {
                @Override public String getRealPath(String path){return root.resolve(path.replaceFirst("^/","")).toString();}
            });
            for(String key:List.of("branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","multiDomainEnabled","enableStaticFilesExternalDir","docAuthorLazyLoad","disableWebJETToolbar","installName","menuIncludePerex","menuInfoDirName","galleryEnableWatermarking","adminCheckUserGroups"))config.put(key,Constants.getString(key));
            Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);Constants.setBoolean("multiDomainEnabled",false);Constants.setBoolean("docAuthorLazyLoad",true);Constants.setBoolean("disableWebJETToolbar",true);Constants.setBoolean("galleryEnableWatermarking",false);Constants.setString("installName","aceintegration");
            // Restoring a DB dump can reuse IDs; isolate this fixture from bytes left by earlier runs.
            Constants.setString("branchStorageRoot",Files.createTempDirectory("webjet-p03-gateway-private-").toString());
            fixture();
            server.setBaseDir(root.resolve("tomcat").toString());server.setPort(0);server.getConnector().setProperty("address","127.0.0.1");
            Context context=server.addContext("",root.toString());((org.apache.tomcat.util.scan.StandardJarScanner)context.getJarScanner()).setScanManifest(false);context.setParentClassLoader(getClass().getClassLoader());context.addServletContainerInitializer(new JasperInitializer(),null);
            Tomcat.addServlet(context,"jsp",new JspServlet()).setLoadOnStartup(1);context.addServletMappingDecoded("*.jsp","jsp");
            StripesFilterIway stripes=new StripesFilterIway();
            addFilter(context,"lifecycle",new Lifecycle());addFilter(context,"branch",new BranchFilter());addFilter(context,"stripes",stripes);
            context.findFilterDef("stripes").addInitParameter("ActionResolver.Packages","sk.iway.iwcm.components.news");
            Tomcat.addServlet(context,"stripesDispatcher",new DispatcherServlet());context.addServletMappingDecoded("*.action","stripesDispatcher");
            Tomcat.addServlet(context,"content",new Content());context.addServletMappingDecoded("/","content");
            Tomcat.addServlet(context,"forwardStage",new HttpServlet(){
                @Override protected void service(HttpServletRequest request,HttpServletResponse response)throws IOException,ServletException {
                    request.getRequestDispatcher(TEMPLATE).forward(request,response);
                }
            });context.addServletMappingDecoded("/_p03-template-stage","forwardStage");
            context.getServletContext().setAttribute("org.apache.catalina.jsp_classpath",System.getProperty("java.class.path"));
            server.start();Constants.setServletContext(context.getServletContext());stripes.initLazy();installTemplate();
            String origin="http://127.0.0.1:"+server.getConnector().getLocalPort();
            HttpClient guest=HttpClient.newHttpClient();assertEquals(403,get(guest,origin+prefix(a)+"/home.html").statusCode());assertEquals(403,get(guest,origin+prefix(a)+"/images/p03.png").statusCode());
            HttpClient editor=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();assertEquals(204,get(editor,origin+"/_login?token="+token).statusCode());
            assertEquals(403,get(editor,origin+prefix(forbidden)+"/home.html").statusCode());
            assertEquals(403,get(editor,origin+prefix(forbidden)+"/images/p03.png").statusCode());
            String live=html(editor,origin+"/home.html",false),work=html(editor,origin+prefix(a)+"/home.html",true),other=html(editor,origin+prefix(b)+"/home.html",true);
            assertTrue(live.contains("LIVE home"));assertTrue(work.contains("WORK A home"));assertTrue(other.contains("WORK B home"));assertFalse(live.contains("WORK A home"));
            String forwardedLive=html(editor,origin+"/home.html?dispatch=forward",false);
            assertEquals(Jsoup.parse(live).select("ul,ol.news").outerHtml(),Jsoup.parse(forwardedLive).select("ul,ol.news").outerHtml(),forwardedLive);
            String forwarded=html(editor,origin+prefix(a)+"/home.html?dispatch=forward",true);
            String forwardedOther=html(editor,origin+prefix(b)+"/home.html?dispatch=forward",true);
            assertEquals(Jsoup.parse(work).select("ul,ol.news").outerHtml(),Jsoup.parse(forwarded).select("ul,ol.news").outerHtml(),forwarded);
            assertEquals(Jsoup.parse(other).select("ul,ol.news").outerHtml(),Jsoup.parse(forwardedOther).select("ul,ol.news").outerHtml(),forwardedOther);
            assertTrue(forwarded.contains("WORK A home"),forwarded);assertFalse(forwarded.contains("WORK B home"),forwarded);
            assertEquals(1,Jsoup.parse(forwarded).select("#branch-preview-status").size());assertEquals(1,Jsoup.parse(forwarded).select("ol.news").size());
            assertTrue(Jsoup.parse(forwarded).select("nav.paging a").stream().allMatch(link->link.attr("href").startsWith(prefix(a)+"/home.html")),forwarded);
            var parsed=Jsoup.parse(work);assertEquals(prefix(a)+"/submit",parsed.selectFirst("form").attr("action"));assertTrue(parsed.selectFirst("form button").hasAttr("disabled"));assertEquals(prefix(a)+"/target.html?q=x#anchor",parsed.selectFirst("a#absolute").attr("href"));assertEquals(prefix(a)+"/target.html?x=1#part",parsed.selectFirst("a#relative").attr("href"));assertEquals("https://example.org/out",parsed.selectFirst("a#external").attr("href"));var group=parsed.selectFirst("li.group"+newGroup);assertNotNull(group,work);assertNotNull(group.selectFirst("li.group"+CHILD),work);assertFalse(parsed.select("li.page"+DELETED).size()>0);
            String link=group.selectFirst("a[href]").attr("href");assertTrue(link.startsWith(prefix(a)),link);String clicked=html(editor,origin+link,true);assertTrue(clicked.contains("WORK new default"));
            assertTrue(Jsoup.parse(clicked).select("li.menuULDirOpen,li.menuULDirSelected,li.active").size()>0,clicked);
            assertTrue(parsed.select("ol.news li").size()>0,work);
            var paging=parsed.selectFirst("nav.paging a[href*=page=2]");assertNotNull(paging,work);String page2=html(editor,origin+paging.attr("href"),true);assertNotEquals(parsed.select("ol.news").text(),Jsoup.parse(page2).select("ol.news").text());
            String detail=parsed.selectFirst("ol.news li a[href]").attr("href");assertTrue(detail.startsWith(prefix(a)),detail);assertEquals(200,get(editor,origin+detail).statusCode());
            byte[] liveImage=get(editor,origin+"/images/p03.png").body(),aImage=get(editor,origin+prefix(a)+"/images/p03.png").body(),bImage=get(editor,origin+prefix(b)+"/images/p03.png").body();assertFalse(Arrays.equals(liveImage,aImage));assertFalse(Arrays.equals(aImage,bImage));
            var conditional=editor.send(HttpRequest.newBuilder(URI.create(origin+prefix(a)+"/images/p03.png"))
                .header("Referer",origin+"/admin/").header("If-None-Match","\"p03-old\"").header("If-Modified-Since","Wed, 04 Nov 2026 10:00:00 GMT").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,conditional.statusCode());assertArrayEquals(aImage,conditional.body());assertTrue(conditional.headers().firstValue("cache-control").orElse("").contains("no-store"));
            long galleryBefore=count("SELECT COUNT(*) FROM gallery");
            var thumb=get(editor,origin+prefix(a)+"/thumb/images/p03.png?w=16&h=16");assertEquals(200,thumb.statusCode());BufferedImage image=ImageIO.read(new ByteArrayInputStream(thumb.body()));assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());assertEquals(galleryBefore,count("SELECT COUNT(*) FROM gallery"));
            assertEquals("WORK A PDF",new String(get(editor,origin+prefix(a)+"/files/p03.pdf").body(),StandardCharsets.UTF_8));
            verifyFileSelections(editor,origin);
            var css=get(editor,origin+prefix(a)+"/templates/p03/probe.css");assertEquals(200,css.statusCode());assertTrue(new String(css.body(),StandardCharsets.UTF_8).contains(prefix(a)+"/images/p03.png"));assertEquals(200,get(editor,origin+prefix(a)+"/templates/p03/probe.js").statusCode());
            assertEquals(405,editor.send(HttpRequest.newBuilder(URI.create(origin+prefix(a)+"/home.html")).header("Referer",origin+"/admin/").POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofByteArray()).statusCode());
            assertEquals(404,get(editor,origin+prefix(a)+"/admin/private.jsp").statusCode());
            var invalid=get(editor,origin+prefix(a)+"/home.html?historyid=1");assertEquals(400,invalid.statusCode());assertTrue(invalid.headers().firstValue("cache-control").orElse("").contains("no-store"));
            String variables=html(editor,origin+prefix(a)+"/home.html?menuVariables=true",true);assertTrue(variables.contains("data-store-preserved=\"true\""),variables);assertTrue(variables.contains("data-invalid-root-cleared=\"true\""),variables);assertTrue(variables.contains("data-store-cleared=\"true\""),variables);
            assertEquals(Jsoup.parse(live).select("ul,ol.news").outerHtml(),Jsoup.parse(html(editor,origin+"/home.html",false)).select("ul,ol.news").outerHtml());assertEquals(withoutBanner(work),withoutBanner(html(editor,origin+prefix(a)+"/home.html",true)));assertEquals(withoutBanner(other),withoutBanner(html(editor,origin+prefix(b)+"/home.html",true)));
            assertEquals(204,get(editor,origin+"/_login?token="+token+"&domain=other-admin.example").statusCode());
            var domainPreview=get(editor,origin+prefix(a)+"/home.html?sessionDomainProof=true");assertEquals(200,domainPreview.statusCode(),new String(domainPreview.body(),StandardCharsets.UTF_8));
            assertEquals(DOMAIN,domainPreview.headers().firstValue("X-P03-Logical-Domain").orElse(""));assertEquals(DOMAIN,domainPreview.headers().firstValue("X-P03-Request-Domain").orElse(""));
            var unchangedSession=get(editor,origin+"/_session-domain?token="+token);assertEquals(200,unchangedSession.statusCode());assertEquals("other-admin.example",new String(unchangedSession.body(),StandardCharsets.UTF_8));
            assertEquals(204,get(editor,origin+"/_login?token="+token).statusCode());
            verifyMenuCases(editor,origin);
            Path evidence=Path.of("build/test-results/p03-gateway");Files.createDirectories(evidence);Files.writeString(evidence.resolve("live.html"),live);Files.writeString(evidence.resolve("branch-a.html"),work);Files.writeString(evidence.resolve("branch-b.html"),other);Files.writeString(evidence.resolve("clicked.html"),clicked);Files.writeString(evidence.resolve("page2.html"),page2);Files.writeString(evidence.resolve("variables.html"),variables);Files.writeString(evidence.resolve("forward-live.html"),forwardedLive);Files.writeString(evidence.resolve("forward-a.html"),forwarded);Files.writeString(evidence.resolve("forward-b.html"),forwardedOther);Files.write(evidence.resolve("thumbnail.png"),thumb.body());Files.write(evidence.resolve("session-domain-preview.html"),domainPreview.body());
            System.out.println("P03 real HTTP/Jasper PASS: BranchFilter, menu JSP, News JSP/Stripes/Velocity, active path, paging/detail/files/thumbnail, LIVE/two branches, no-store and read-only guards");
        } finally {
            try{server.stop();server.destroy();}finally{SetCharacterEncodingFilter.setCurrentRequestBean(null);cleanup();config.forEach(Constants::setString);new sk.iway.iwcm.system.spring.components.SpringContext().setApplicationContext(originalSpring);spring.close();Constants.setServletContext(originalContext);GroupsDB.getInstance(true);DocDB.getInstance(true);}
        }
    }

    /** Q16 file choices and deletion use real bytes through the production HTTP gateway. */
    private void verifyFileSelections(HttpClient editor,String origin)throws Exception {
        choiceFile("/files/p03.pdf","LIVE");
        var live=get(editor,origin+prefix(a)+"/files/p03.pdf");assertEquals(200,live.statusCode());assertEquals("LIVE PDF",new String(live.body(),StandardCharsets.UTF_8));
        choiceFile("/files/p03.pdf","WORK");
        var work=get(editor,origin+prefix(a)+"/files/p03.pdf");assertEquals(200,work.statusCode());assertEquals("WORK A PDF",new String(work.body(),StandardCharsets.UTF_8));
        var independent=get(editor,origin+prefix(b)+"/files/p03.pdf");assertEquals(200,independent.statusCode());assertEquals("WORK B PDF",new String(independent.body(),StandardCharsets.UTF_8));
        String path="/files/deleted.pdf";Path bytes=root.resolve("files/deleted.pdf");Files.writeString(bytes,"LIVE deleted PDF");
        store.putFile(a,path,"FILE",BranchFiles.liveSnapshot(path),null,null,null,true);
        var deleted=get(editor,origin+prefix(a)+path);assertEquals(404,deleted.statusCode());assertTrue(deleted.headers().firstValue("cache-control").orElse("").contains("no-store"));
        var publicFile=get(editor,origin+path);assertEquals(200,publicFile.statusCode());assertEquals("LIVE deleted PDF",new String(publicFile.body(),StandardCharsets.UTF_8));
        choiceFile(path,"LIVE");
        var restored=get(editor,origin+prefix(a)+path);assertEquals(200,restored.statusCode());assertEquals("LIVE deleted PDF",new String(restored.body(),StandardCharsets.UTF_8));
        Files.writeString(bytes,"CHANGED LIVE PDF");
        var stale=get(editor,origin+prefix(a)+path);assertEquals(409,stale.statusCode());assertTrue(stale.headers().firstValue("cache-control").orElse("").contains("no-store"));
        Path evidence=Path.of("build/test-results/p03-gateway");Files.createDirectories(evidence);Files.writeString(evidence.resolve("file-q16.txt"),"A LIVE choice: LIVE PDF\nA WORK choice: WORK A PDF\nB independent: WORK B PDF\nWORK DELETE: HTTP 404 while public bytes remain HTTP 200\nDELETE + LIVE choice: original LIVE bytes HTTP 200\nChanged LIVE bytes: stale choice HTTP 409, no-store\n");
    }
    private void choiceFile(String path,String source)throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            Map<String,Object> row=BranchStore.readWork(connection,a.branchId(),BranchType.FILE,path).orElseThrow();
            try(PreparedStatement statement=connection.prepareStatement("UPDATE branch_files SET resolved_choice=?,resolved_live_hash=?,resolved_work_hash=? WHERE branch_id=? AND logical_path=?")) {
                statement.setString(1,source);statement.setString(2,BranchFiles.liveSnapshot(path).hash());statement.setString(3,BranchStore.change(BranchType.FILE,row).work().hash());statement.setLong(4,a.branchId());statement.setString(5,path);assertEquals(1,statement.executeUpdate());
            }
        }
    }
    private void verifyMenuCases(HttpClient editor,String origin) throws Exception {
        String base=origin+prefix(a)+"/home.html?menuCase=";
        Map<String,String> evidence=new LinkedHashMap<>();
        String automatic=html(editor,base+"root-zero",true);evidence.put("m03-root-zero",automatic);assertTrue(automatic.contains("Renamed navigation A"),automatic);
        String depth=html(editor,base+"depth",true);evidence.put("m03-depth",depth);assertFalse(depth.contains("Renamed navigation A"),depth);assertTrue(depth.contains("New navigation A"),depth);
        String offset=html(editor,base+"offset",true);evidence.put("m03-offset",offset);assertTrue(offset.contains("Renamed navigation A"),offset);assertFalse(offset.contains(">Target<"),offset);
        String folders=html(editor,base+"folders",true);evidence.put("m03-folders",folders);assertFalse(folders.contains("Extra menu page"),folders);assertTrue(folders.contains("Nested"),folders);
        assertTrue(html(editor,base+"force-valid",true).contains("menuULDirOpen"));assertFalse(html(editor,base+"force-invalid",true).contains("<li"));
        for(String classes:List.of("none","basic","full","bootstrap")) {String html=html(editor,base+"classes-"+classes,true);evidence.put("m05-"+classes,html);assertTrue(html.contains("Renamed navigation A"),html);}
        change(a,BranchType.GROUP,CHILD,Map.of("menu_type",GroupDetails.MENU_TYPE_HIDDEN),false);
        String hiddenTop=html(editor,base+"top",true);assertFalse(hiddenTop.contains("Renamed navigation A"),hiddenTop);
        String hidden=html(editor,base+"hidden",true);evidence.put("m03-hidden",hidden);assertTrue(hidden.contains("Nested"),hidden);
        change(a,BranchType.GROUP,CHILD,Map.of("menu_type",GroupDetails.MENU_TYPE_NORMAL),false);
        Constants.setBoolean("adminCheckUserGroups",true);
        change(a,BranchType.DOCUMENT,EXTRA,Map.of("password_protected",",1940077,"),false);
        String protectedHtml=html(editor,base+"hidden",true);evidence.put("m04-protected",protectedHtml);assertTrue(Jsoup.parse(protectedHtml).select("li.protected").size()>0,protectedHtml);
        assertEquals(204,get(editor,origin+"/_login?token="+token+"&groups=1940077").statusCode());
        String allowed=html(editor,base+"hidden",true);evidence.put("m04-identity",allowed);assertTrue(Jsoup.parse(allowed).select("li.protectedAllow").size()>0,allowed);
        change(a,BranchType.DOCUMENT,EXTRA,Map.of("logged_show_in_menu",false),false);assertFalse(html(editor,base+"hidden",true).contains("Extra menu page"));
        change(a,BranchType.DOCUMENT,EXTRA,Map.of("logged_show_in_menu",true),false);
        change(a,BranchType.DOCUMENT,SECOND,Map.of("available",false),false);assertFalse(html(editor,base+"root-zero",true).contains(">Target<"));change(a,BranchType.DOCUMENT,SECOND,Map.of("available",true),false);
        String perex=html(editor,base+"perex",true);evidence.put("m06-perex",perex);assertTrue(perex.contains("WORK menu perex"),perex);
        String info=html(editor,base+"info",true);evidence.put("m06-info",info);assertTrue(info.contains("WORK menu HTML"),info);assertNotNull(Jsoup.parse(info).selectFirst("li.menu-info-li"),info);assertTrue(Jsoup.parse(info).select("img[src='"+prefix(a)+"/images/p03.png']").size()>0,info);
        choice(PAGE,"LIVE");String live=html(editor,base+"info",true);evidence.put("m09-live",live);assertTrue(live.contains("LIVE child"),live);assertFalse(live.contains("WORK menu HTML"),live);assertTrue(html(editor,base+"top",true).contains(prefix(a)+"/child.html"),live);
        choice(PAGE,"WORK");String work=html(editor,base+"info",true);evidence.put("m09-work",work);assertTrue(work.contains("WORK menu HTML"),work);assertTrue(html(editor,base+"top",true).contains(prefix(a)+"/renamed.html"),work);
        Map<String,Object> original;try(Connection c=DBPool.getConnection()){original=BranchStore.readLive(c,BranchType.DOCUMENT,PAGE).orElseThrow();}
        change(a,BranchType.DOCUMENT,PAGE,original,false);String unchanged=html(editor,base+"info",true);evidence.put("m09-w-equals-b",unchanged);assertTrue(unchanged.contains("data-source=\"LIVE\""),unchanged);assertTrue(unchanged.contains("LIVE child"),unchanged);
        change(a,BranchType.DOCUMENT,PAGE,Map.of("title","Equal result","navbar","Equal result"),false);
        try(Connection c=DBPool.getConnection();PreparedStatement p=c.prepareStatement("UPDATE documents SET title='Equal result',navbar='Equal result' WHERE doc_id=?")){p.setInt(1,PAGE);p.executeUpdate();}
        String equal=html(editor,base+"info",true);evidence.put("m09-l-equals-w",equal);assertTrue(equal.contains("data-source=\"LIVE\""),equal);
        choice(PAGE,"WORK");try(Connection c=DBPool.getConnection();PreparedStatement p=c.prepareStatement("UPDATE documents SET title='Stale LIVE' WHERE doc_id=?")){p.setInt(1,PAGE);p.executeUpdate();}
        var stale=get(editor,base+"info");assertEquals(409,stale.statusCode());assertTrue(stale.headers().firstValue("cache-control").orElse("").contains("no-store"));
        Path directory=Path.of("build/test-results/p03-gateway");Files.createDirectories(directory);for(var entry:evidence.entrySet())Files.writeString(directory.resolve(entry.getKey()+".html"),entry.getValue());
    }
    private void choice(int id,String source)throws Exception {
        try(Connection c=DBPool.getConnection()){Map<String,Object> work=BranchStore.readWork(c,a.branchId(),BranchType.DOCUMENT,id).orElseThrow();Map<String,Object> live=BranchStore.readLive(c,BranchType.DOCUMENT,id).orElseThrow();
            try(PreparedStatement p=c.prepareStatement("UPDATE branch_documents SET resolved_choice=?,resolved_live_hash=?,resolved_work_hash=? WHERE branch_id=? AND doc_id=?")){p.setString(1,source);p.setString(2,BranchStore.snapshot(BranchType.DOCUMENT,live).hash());p.setString(3,BranchStore.snapshot(BranchType.DOCUMENT,work).hash());p.setLong(4,a.branchId());p.setInt(5,id);p.executeUpdate();}
        }
    }
    private void prepareRoot() throws Exception {
        for(String file:List.of(TEMPLATE,MENU,"/components/news/news-velocity.jsp","/components/maybeError.jsp","/WEB-INF/iwcm.tld","/WEB-INF/iway.tld","/WEB-INF/displaytag.tld")) {
            Path target=root.resolve(file.substring(1));Files.createDirectories(target.getParent());Files.copy(WEBAPP.resolve(file.substring(1)),target);
        }
        Path assets=root.resolve("templates/p03");Files.createDirectories(assets);Files.writeString(assets.resolve("probe.css"),"body{background-image:url('/images/p03.png')}");Files.writeString(assets.resolve("probe.js"),"document.documentElement.dataset.probe='ready';");
        Files.createDirectories(root.resolve("images"));image(root.resolve("images/p03.png"),Color.RED);Files.createDirectories(root.resolve("files"));Files.writeString(root.resolve("files/p03.pdf"),"LIVE PDF");
    }
    private void fixture() throws Exception {
        try(Connection c=DBPool.getConnection()){assertEquals("webjet_p01",c.getCatalog());assertTrue(c.getMetaData().getURL().contains(":33316/"));}
        try(Connection c=DBPool.getConnection();Statement p=c.createStatement()){p.executeUpdate("INSERT INTO user_groups(user_group_id,user_group_name,user_group_type) VALUES(1940077,'P03 gateway permission',0)");}
        sk.iway.iwcm.users.UserGroupsDB.getInstance(true);
        group(ROOT,0,"Root",HOME);group(CHILD,ROOT,"Child",PAGE);group(TARGET,ROOT,"Target",SECOND);group(NESTED,CHILD,"Nested",NESTED_PAGE);
        a=branch("A");b=branch("B");forbidden=branch("forbidden","p03-forbidden.example");
        String menu="!INCLUDE(/components/menu/menu_ul_li.jsp,rootGroupId="+ROOT+",openAllItems=true,classes=full,maxLevel=-1)!";
        String news="!INCLUDE(/components/news/news-velocity.jsp,groupIds="+ROOT+",alsoSubGroups=true,template=p03-gateway,publishType=all,perexNotRequired=false,order=priority,ascending=true,pageSize=2,paging=true)!";
        String body=menu+"<main><h1>LIVE home</h1>"+news+"<a id='absolute' href='https://"+DOMAIN+"/target.html?q=x#anchor'>Absolute</a><a id='relative' href='target.html?x=1#part'>Relative</a><a id='external' href='https://example.org/out'>External</a><img id='original' src='/images/p03.png'><img id='thumbnail' src='/thumb/images/p03.png?w=16&amp;h=16'><a id='pdf' href='/files/p03.pdf'>PDF</a><link rel='stylesheet' href='/templates/p03/probe.css'><script src='/templates/p03/probe.js'></script><form action='/submit' method='post'><button>Send</button></form></main>";
        document(HOME,ROOT,"Home","/home.html",body,1);document(PAGE,CHILD,"Child page","/child.html","<h1>LIVE child</h1>",2);document(SECOND,TARGET,"Target page","/target.html","<h1>LIVE target</h1>",3);document(DELETED,ROOT,"Deleted page","/delete.html","delete",4);document(EXTRA,CHILD,"Extra menu page","/extra.html","extra",6);document(NESTED_PAGE,NESTED,"Nested page","/nested.html","nested",7);
        change(a,BranchType.DOCUMENT,HOME,Map.of("data",body.replace("LIVE home","WORK A home")),false);change(b,BranchType.DOCUMENT,HOME,Map.of("data",body.replace("LIVE home","WORK B home")),false);
        Map<String,Object> g=new LinkedHashMap<>(BranchGroupMapper.initialValues(1,DOMAIN));g.put("group_name","New section A");g.put("navbar","New navigation A");g.put("parent_group_id",ROOT);g.put("url_dir_name","new");newGroup=((Number)store.create(a,BranchType.GROUP,g).get("group_id")).intValue();
        Map<String,Object> d=doc(1,newGroup,"New page","/new.html","<h1>WORK new default</h1>"+menu.replace("openAllItems=true","openAllItems=false"),5);newPage=((Number)store.create(a,BranchType.DOCUMENT,d).get("doc_id")).intValue();
        change(a,BranchType.GROUP,newGroup,Map.of("default_doc_id",newPage),false);change(a,BranchType.GROUP,CHILD,Map.of("parent_group_id",newGroup,"group_name","Renamed A","navbar","Renamed navigation A"),false);change(a,BranchType.DOCUMENT,PAGE,Map.of("title","Changed page A","navbar","Changed navigation A","virtual_path","/renamed.html","html_data","WORK menu perex","data","<p>WORK menu HTML <a href='/target.html'>target</a><img src='/images/p03.png'></p>"),false);change(a,BranchType.DOCUMENT,DELETED,Map.of(),true);
        change(b,BranchType.GROUP,CHILD,Map.of("group_name","Renamed B","navbar","Renamed navigation B"),false);
        for(BranchContext branch:List.of(a,b)){Path bytes=Files.createTempFile("p03-image-",".png");image(bytes,branch==a?Color.BLUE:Color.GREEN);file(branch,"/images/p03.png",bytes,"image/png");Files.delete(bytes);Path pdf=Files.createTempFile("p03-file-",".pdf");Files.writeString(pdf,branch==a?"WORK A PDF":"WORK B PDF");file(branch,"/files/p03.pdf",pdf,"application/pdf");Files.delete(pdf);}
        GroupsDB.getInstance(true);DocDB.getInstance(true);
    }
    private void installTemplate() {
        NewsTemplatesEntity template=new NewsTemplatesEntity();template.setName("p03-gateway");template.setTemplateCode("<ol class='news'>#foreach($item in $news)<li data-id='$item.docId'><a href='$actionBean.link($item)'>$item.title</a><span>$item.htmlData</span></li>#end</ol>");template.setPagingCode("<nav class='paging'>#foreach($item in $pagesAll)<a href='$item.url'>$item.label</a>#end</nav>");template.setPagingPosition(2);Cache.getInstance().setObjectSeconds("newsTemplatesService.templates",new HashMap<>(Map.of("0.p03-gateway",template,BranchDomains.id(DOMAIN)+".p03-gateway",template)),600);
    }
    private BranchContext branch(String label){return branch(label,DOMAIN);}
    private BranchContext branch(String label,String domain){long id=new BranchService().create(BranchTestDomains.ensure(domain),"P03 gateway "+label,"Real JSP fixture",1).getId();branchIds.add(id);return new BranchContext(id,domain,1,BranchContext.Mode.ADMIN);}
    private void group(int id,int parent,String name,int defaultDoc)throws Exception{Map<String,Object> v=new LinkedHashMap<>(BranchGroupMapper.initialValues(id,DOMAIN));v.put("parent_group_id",parent);v.put("group_name",name);v.put("navbar",name);v.put("default_doc_id",defaultDoc);v.put("url_dir_name",name.toLowerCase());try(Connection c=DBPool.getConnection()){BranchStore.insert(c,"groups",v);}liveGroups.add(id);}
    private Map<String,Object> doc(int id,int group,String title,String path,String body,int priority){Map<String,Object> v=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,Instant.now(),false));v.put("group_id",group);v.put("title",title);v.put("navbar",title);v.put("virtual_path",path);v.put("data",body);v.put("html_data","Perex "+title);v.put("available",true);v.put("show_in_menu",true);v.put("sort_priority",priority);v.put("cacheable",true);return v;}
    private void document(int id,int group,String title,String path,String body,int priority)throws Exception{try(Connection c=DBPool.getConnection()){BranchStore.insert(c,"documents",doc(id,group,title,path,body,priority));}liveDocs.add(id);}
    private void change(BranchContext branch,BranchType type,int id,Map<String,Object> values,boolean delete)throws Exception{Map<String,Object> current;try(Connection c=DBPool.getConnection()){var work=BranchStore.readWork(c,branch.branchId(),type,id);current=work.isPresent()?work.get():BranchStore.readLive(c,type,id).orElseThrow();}Map<String,Object> next=new LinkedHashMap<>(current);next.putAll(values);store.put(branch,type,id,next,BranchStore.snapshot(type,current).hash(),delete);}
    private void file(BranchContext branch,String path,Path bytes,String mime)throws Exception{Map<String,Object> row=store.putFile(branch,path,"FILE",BranchFiles.liveSnapshot(path),BranchFiles.hash(bytes),Files.size(bytes),mime,false);Path target=BranchFiles.workPath(branch.branchId(),((Number)row.get("row_id")).longValue());Files.createDirectories(target.getParent());Files.copy(bytes,target);}
    private void cleanup()throws Exception{try(Connection c=DBPool.getConnection()){for(long id:branchIds)for(String table:List.of("branch_documents","branch_groups","branch_media","branch_files","branches"))execute(c,"DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"=?",id);for(int id:liveDocs)execute(c,"DELETE FROM documents WHERE doc_id=?",id);for(int id:liveGroups)execute(c,"DELETE FROM groups WHERE group_id=?",id);execute(c,"DELETE FROM user_groups WHERE user_group_id=?",1940077);}Cache.getInstance().removeObject("newsTemplatesService.templates");}
    private void execute(Connection c,String sql,Object value)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){p.setObject(1,value);p.executeUpdate();}}
    private long count(String sql)throws Exception{try(Connection c=DBPool.getConnection();Statement p=c.createStatement();ResultSet r=p.executeQuery(sql)){r.next();return r.getLong(1);}}
    private static void image(Path file,Color color)throws IOException{BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();graphics.setColor(color);graphics.fillRect(0,0,64,64);graphics.dispose();ImageIO.write(image,"png",file.toFile());}
    private static void addFilter(Context context,String name,Filter filter){FilterDef def=new FilterDef();def.setFilterName(name);def.setFilter(filter);context.addFilterDef(def);FilterMap mapping=new FilterMap();mapping.setFilterName(name);mapping.addURLPattern("/*");mapping.setDispatcher("REQUEST");context.addFilterMap(mapping);}
    private static String withoutBanner(String html){var document=Jsoup.parse(html);document.select("#branch-preview-status").remove();return document.outerHtml();}
    private static String prefix(BranchContext branch){return "/admin/webpages/branch-preview/"+branch.branchId();}
    private static HttpResponse<byte[]> get(HttpClient client,String url)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(url)).header("Referer",URI.create(url).resolve("/admin/").toString()).GET().build(),HttpResponse.BodyHandlers.ofByteArray());}
    private static String html(HttpClient client,String url,boolean preview)throws Exception{var response=get(client,url);String body=new String(response.body(),StandardCharsets.UTF_8);assertEquals(200,response.statusCode(),body);if(preview)assertTrue(response.headers().firstValue("cache-control").orElse("").contains("no-store"));return body;}

    private final class Lifecycle implements Filter {
        @Override public void doFilter(ServletRequest in,ServletResponse out,FilterChain chain)throws IOException,ServletException{HttpServletRequest request=(HttpServletRequest)in;RequestBean bean=new RequestBean();bean.setRequest(request);bean.setDomain(DOMAIN);bean.setLng("sk");bean.setUrl(request.getRequestURI());SetCharacterEncodingFilter.setCurrentRequestBean(bean);try{chain.doFilter(in,out);}finally{SetCharacterEncodingFilter.setCurrentRequestBean(null);}}
    }
    private final class Content extends HttpServlet {
        @Override protected void service(HttpServletRequest request,HttpServletResponse response)throws IOException,ServletException {
            if("/_login".equals(request.getRequestURI())){if(!token.equals(request.getParameter("token"))){response.sendError(403);return;}Identity user=new Identity();user.setAdmin(true);user.setUserId(1);user.setEditableGroups(Integer.toString(ROOT));user.setUserGroupsIds(Objects.toString(request.getParameter("groups"),""));request.getSession().setAttribute(Constants.USER_KEY,user);request.getSession().setAttribute("preview.editorDomainName",Objects.toString(request.getParameter("domain"),DOMAIN));response.setStatus(204);return;}
            if("/_session-domain".equals(request.getRequestURI())){if(!token.equals(request.getParameter("token"))){response.sendError(403);return;}response.setContentType("text/plain;charset=UTF-8");response.getWriter().print(request.getSession().getAttribute("preview.editorDomainName"));return;}
            String path=request.getRequestURI();
            if(path.startsWith("/templates/p03/")||path.startsWith("/images/")||path.startsWith("/files/")){Path file=root.resolve(path.substring(1));if(!Files.isRegularFile(file)){response.sendError(404);return;}response.setContentType(path.endsWith(".css")?"text/css":path.endsWith(".js")?"text/javascript":"application/octet-stream");Files.copy(file,response.getOutputStream());return;}
            int id=BranchContexts.current().isBranch()?BranchReadService.docIdFromUrl(path):DocDB.getInstance().getDocIdFromURL(path,DOMAIN);
            if(BranchContexts.current().isBranch()) {
                assertEquals(DOMAIN,DocDB.getDomain(request));assertEquals(DOMAIN,SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
                response.setHeader("X-P03-Logical-Domain",DocDB.getDomain(request));response.setHeader("X-P03-Request-Domain",SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
                if("true".equals(request.getParameter("sessionDomainProof")))assertEquals("other-admin.example",request.getSession().getAttribute("preview.editorDomainName"));
            }
            DocDetails doc=DocDB.getInstance().getDoc(id);if(doc==null){response.sendError(404);return;}request.setAttribute("docDetails",doc);request.setAttribute("path_filter_orig_path",path);request.setAttribute("doc_title",doc.getTitle());request.setAttribute("doc_data",doc.getData());request.setAttribute("doc_temp_name","blank-nojquery");request.setAttribute("lng","sk");
            if(request.getParameter("menuCase")!=null){menuCase(request,response,request.getParameter("menuCase"));return;}
            if("true".equals(request.getParameter("menuVariables"))){variables(request,response);return;}
            response.setContentType("text/html;charset=UTF-8");
            if("forward".equals(request.getParameter("dispatch")))request.getRequestDispatcher("/_p03-template-stage").forward(request,response);
            else request.getRequestDispatcher(TEMPLATE).include(request,response);
        }
        private void menuCase(HttpServletRequest request,HttpServletResponse response,String scenario)throws IOException,ServletException {
            request.setAttribute("docDetails",DocDB.getInstance().getDoc(PAGE));
            String params="rootGroupId="+ROOT+",openAllItems=true,classes=full,maxLevel=-1";
            switch(scenario){
                case "top":break;
                case "root-zero":params="rootGroupId=0,openAllItems=true";break;
                case "depth":params="rootGroupId="+ROOT+",openAllItems=true,maxLevel=1";break;
                case "offset":params="rootGroupId=0,startOffset=1,openAllItems=true";break;
                case "folders":params="rootGroupId="+CHILD+",openAllItems=true,onlyFolders=true";break;
                case "force-valid":params="rootGroupId=0,forceDocid="+newPage;break;
                case "force-invalid":params="rootGroupId=0,forceDocid=2147483647";break;
                case "hidden":params="rootGroupId="+CHILD+",openAllItems=true";break;
                case "perex":params+=",menuIncludePerex=true";break;
                case "info":params+=",menuInfoDirName=Renamed navigation A";break;
                default:if(scenario.startsWith("classes-"))params="rootGroupId="+ROOT+",openAllItems=true,classes="+scenario.substring(8);else throw new ServletException("Unknown menu fixture");
            }
            request.setAttribute("includePageParams",params);response.setContentType("text/html;charset=UTF-8");
            response.getWriter().print("<section data-menu='"+GroupsDB.getInstance().getGroup(CHILD).getMenuType()+"' data-logged-menu='"+GroupsDB.getInstance().getGroup(CHILD).getMenuType(request.getSession())+"' data-source='"+(BranchView.current().usesWork(BranchType.DOCUMENT,Integer.toString(PAGE))?"WORK":"LIVE")+"'>");
            request.getRequestDispatcher(MENU).include(request,response);response.getWriter().print("</section>");
        }
        private void variables(HttpServletRequest request,HttpServletResponse response)throws IOException,ServletException {
            request.setAttribute("includePageParams","rootGroupId="+ROOT+",openAllItems=true,onlySetVariables=true");request.getRequestDispatcher(MENU).include(request,response);String stored=(String)request.getAttribute("!UL_LI_MENU!");
            request.setAttribute("includePageParams","rootGroupId=2147483647");request.getRequestDispatcher(MENU).include(request,response);boolean preserved=Objects.equals(stored,request.getAttribute("!UL_LI_MENU!"));boolean cleared=request.getAttribute("!ROOT_GROUP_ID!")==null;
            request.setAttribute("includePageParams","rootGroupId=2147483647,onlySetVariables=true");request.getRequestDispatcher(MENU).include(request,response);boolean storeCleared="".equals(request.getAttribute("!UL_LI_MENU!"));
            response.setContentType("text/html;charset=UTF-8");response.getWriter().print("<div data-store-preserved='"+preserved+"' data-invalid-root-cleared='"+cleared+"' data-store-cleared='"+storeCleared+"'></div>");
        }
    }
}
