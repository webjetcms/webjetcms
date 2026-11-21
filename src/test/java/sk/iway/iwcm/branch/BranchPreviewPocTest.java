package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.jasper.servlet.JasperInitializer;
import org.apache.jasper.servlet.JspServlet;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.w3c.dom.Element;

import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.ThumbServlet;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Opt-in P01 HTTP/Jasper prototype, never a production branch implementation.
 * Reads detached IDs/metadata from an explicitly allowed test DB, then uses a small
 * explicit in-memory graph. Real menu JSP, MenuULLI, aceintegration blank template,
 * WriteTag and ThumbServlet execute; only content facades and test login are fixtures.
 * This does not verify the application's PathFilter or real authentication chain.
 */
@EnabledIfEnvironmentVariable(named = "WEBJET_P01_PREVIEW", matches = "true")
class BranchPreviewPocTest extends BaseWebjetTest
{
    private static final Path WEBAPP = Path.of("src/main/webapp").toAbsolutePath();
    private static final String TEMPLATE = "/templates/aceintegration/jet/blank.jsp";
    private static final String MENU = "/components/menu/menu_ul_li.jsp";
    private static final int NEW_GROUP = 2_000_000_000;
    private static final int NEW_DOC = 2_000_000_001;
    private final String loginToken = UUID.randomUUID().toString();
    private final Map<Integer, View> views = new LinkedHashMap<>();
    private Path root;
    private String sourceProfile;
    private final List<Path> installedAdminFiles = new ArrayList<>();
    private Path ownedAdminCompletion;

    /** Exercises real rendered links, branch separation, thumbnail bytes and route guards. */
    @Test
    void realJspMenuGatewayAndAssets() throws Exception
    {
        loadDetachedFixture();
        root = Files.createTempDirectory("webjet-p01-preview-");
        prepareWebroot();
        ServletContext originalContext = Constants.getServletContext();
        Map<String, String> originalConfig = new LinkedHashMap<>();
        for (String key : List.of("multiDomainEnabled", "enableStaticFilesExternalDir", "galleryEnableWatermarking",
                "thumbServletAllowedSizeMode", "thumbWriteServer", "thumbServletCacheDir", "installName", "disableWebJETToolbar"))
            originalConfig.put(key, Constants.getString(key));
        Tomcat server = new Tomcat();
        server.setBaseDir(root.resolve("tomcat").toString());
        server.setPort(0);
        server.getConnector().setProperty("address", "127.0.0.1");
        Context context = server.addContext("", root.toString());
        context.setParentClassLoader(getClass().getClassLoader());
        context.addServletContainerInitializer(new JasperInitializer(), null);
        Tomcat.addServlet(context, "jsp", new JspServlet()).setLoadOnStartup(1);
        context.addServletMappingDecoded("*.jsp", "jsp");
        Tomcat.addServlet(context, "gateway", new Gateway());
        context.addServletMappingDecoded("/", "gateway");
        context.getServletContext().setAttribute("org.apache.catalina.jsp_classpath", System.getProperty("java.class.path"));
        try
        {
            server.start();
            Constants.setServletContext(context.getServletContext());
            Constants.setBoolean("multiDomainEnabled", false);
            Constants.setBoolean("enableStaticFilesExternalDir", false);
            Constants.setBoolean("galleryEnableWatermarking", false);
            Constants.setString("thumbServletAllowedSizeMode", "allow");
            Constants.setString("thumbWriteServer", "");
            Constants.setString("thumbServletCacheDir", "/WEB-INF/imgcache/");
            Constants.setString("installName", "aceintegration");
            Constants.setBoolean("disableWebJETToolbar", true);
            String origin = "http://127.0.0.1:" + server.getConnector().getLocalPort();
            HttpClient guest = HttpClient.newHttpClient();
            assertEquals(403, get(guest, origin + "/admin/webpages/branch-preview/42/home.html").statusCode());
            CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            HttpClient editor = HttpClient.newBuilder().cookieHandler(cookies).build();
            assertEquals(204, get(editor, origin + "/_p01_login?token=" + loginToken).statusCode());
            String live = html(editor, origin + "/home.html");
            String branch42 = html(editor, origin + "/admin/webpages/branch-preview/42/home.html");
            String branch43 = html(editor, origin + "/admin/webpages/branch-preview/43/home.html");
            assertTrue(live.contains("P01 LIVE body"));
            assertFalse(live.contains("P01 NEW section"));
            assertTrue(branch42.contains("P01 WORK 42 body"));
            assertTrue(branch42.contains("P01 NEW section"));
            assertTrue(branch42.contains("P01 renamed section 42"));
            assertTrue(branch43.contains("P01 renamed section 43"));
            assertFalse(branch43.contains("P01 renamed section 42"));
            var menu42 = Jsoup.parse(branch42);
            var newItem = menu42.selectFirst("li.group" + NEW_GROUP);
            assertNotNull(newItem, "The real renderer must expose the added folder");
            int movedGroup = views.get(42).groups().get(1).getGroupId();
            assertNotNull(newItem.selectFirst("li.group" + movedGroup), "The renamed existing folder must move under the new one");
            assertNotNull(Jsoup.parse(live).selectFirst("li.group" + movedGroup));
            var newLink = newItem.selectFirst("a[href]");
            assertEquals("/admin/webpages/branch-preview/42/new.html", newLink.attr("href"));
            String clicked = html(editor, origin + newLink.attr("href"));
            assertTrue(clicked.contains("P01 NEW default body 42"));
            assertTrue(clicked.contains("menuULDirOpen group" + NEW_GROUP));
            assertEquals(live, html(editor, origin + "/home.html"), "Warming two branches must not alter LIVE rendering");
            assertEquals(branch42, html(editor, origin + "/admin/webpages/branch-preview/42/home.html"));
            byte[] liveImage = get(editor, origin + "/images/p01.png").body();
            byte[] workImage = get(editor, origin + "/admin/webpages/branch-preview/42/images/p01.png").body();
            assertFalse(Arrays.equals(liveImage, workImage));
            var thumb = get(editor, origin + "/admin/webpages/branch-preview/42/thumb/images/p01.png?w=16&h=16");
            assertEquals(200, thumb.statusCode(), new String(thumb.body(), StandardCharsets.UTF_8));
            assertTrue(thumb.headers().firstValue("cache-control").orElse("").contains("no-store"));
            BufferedImage scaled = ImageIO.read(new ByteArrayInputStream(thumb.body()));
            assertNotNull(scaled);
            assertEquals(16, scaled.getWidth());
            assertEquals(16, scaled.getHeight());
            assertEquals(Color.BLUE.getRGB() & 0xffffff, scaled.getRGB(8, 8) & 0xffffff);
            assertEquals(Color.RED.getRGB() & 0xffffff, ImageIO.read(new ByteArrayInputStream(liveImage)).getRGB(8, 8) & 0xffffff);
            String css = html(editor, origin + "/admin/webpages/branch-preview/42/templates/p01/probe.css");
            String js = html(editor, origin + "/admin/webpages/branch-preview/42/templates/p01/probe.js");
            assertTrue(css.contains("url('/admin/webpages/branch-preview/42/images/p01.png')"));
            assertTrue(js.contains("fetch('/admin/webpages/branch-preview/42/images/p01.png')"));
            assertTrue(branch42.contains("src=\"/admin/webpages/branch-preview/42/templates/p01/probe.js\""));
            for (String path : List.of("admin/logon/", "WEB-INF/iwcm.tld", "components/menu/menu_ul_li.jsp", "%2e%2e/admin/"))
                assertTrue(get(editor, origin + "/admin/webpages/branch-preview/42/" + path).statusCode() >= 400);
            assertEquals(404, get(editor, origin + "/admin/webpages/branch-preview/99/home.html").statusCode());
            assertEquals(400, get(editor, origin + "/admin/webpages/branch-preview/42/home.html?historyid=1").statusCode());
            assertEquals(405, editor.send(HttpRequest.newBuilder(URI.create(origin + "/admin/webpages/branch-preview/42/home.html"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofByteArray()).statusCode());
            Path evidence = Path.of("build/test-results/p01-preview");
            Files.createDirectories(evidence);
            Files.writeString(evidence.resolve("live.html"), live);
            Files.writeString(evidence.resolve("branch42.html"), branch42);
            Files.writeString(evidence.resolve("branch43.html"), branch43);
            Files.writeString(evidence.resolve("clicked-default.html"), clicked);
            Files.write(evidence.resolve("work-thumb.png"), thumb.body());
            Files.writeString(evidence.resolve("environment.txt"), "Real Jasper JSP/MenuULLI/WriteTag/ThumbServlet; template=" + TEMPLATE
                + "\nDetached read-only DB source=" + sourceProfile + "; test-only facade/auth/URL adapters\n");
            System.out.println("P01 embedded HTTP proof PASS: real menu JSP + aceintegration blank + ThumbServlet; added/renamed/moved/default click; two branches/LIVE; no-store/assets/guards");
            if ("true".equals(System.getenv("WEBJET_P01_ADMIN_HOST"))) awaitAdminHostProof(origin, evidence);
        }
        finally
        {
            try { server.stop(); }
            finally
            {
                try { server.destroy(); }
                finally
                {
                    originalConfig.forEach(Constants::setString);
                    Constants.setServletContext(originalContext);
                    for (Path file : installedAdminFiles) Files.deleteIfExists(file);
                    if (ownedAdminCompletion != null) Files.deleteIfExists(ownedAdminCompletion);
                }
            }
        }
    }

    /** Temporarily exposes the prototype through a real local CMS admin session for browser proof. */
    private void awaitAdminHostProof(String origin, Path evidence) throws Exception
    {
        Path jsp = WEBAPP.resolve("admin/branch-p01-autotest.jsp");
        Path descriptor = WEBAPP.resolve("WEB-INF/p01-gateway-autotest.properties");
        Path ready = evidence.resolve("admin-host-ready");
        Path complete = evidence.resolve("admin-host-complete");
        for (Path file : List.of(jsp, descriptor, ready, complete))
            assertFalse(Files.exists(file), "Refusing to overwrite an existing P01 handoff file: " + file);
        ownedAdminCompletion = complete;
        try (InputStream in = getClass().getResourceAsStream("/branch-p01/admin-gateway.jsp"))
        {
            Files.copy(Objects.requireNonNull(in), jsp);
            installedAdminFiles.add(jsp);
        }
        Properties properties = new Properties();
        properties.setProperty("origin", origin);
        properties.setProperty("loginToken", loginToken);
        properties.setProperty("logicalDomain", "p01-content.example.invalid");
        try (OutputStream out = Files.newOutputStream(descriptor, StandardOpenOption.CREATE_NEW))
        {
            installedAdminFiles.add(descriptor);
            properties.store(out, "Temporary P01 loopback fixture; never a production gateway");
        }
        Files.setPosixFilePermissions(descriptor, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        Files.writeString(ready, "READY\n", StandardOpenOption.CREATE_NEW);
        installedAdminFiles.add(ready);
        System.out.println("P01 admin-host handoff READY; waiting up to 120 seconds for browser PASS marker");
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(120);
        while (System.nanoTime() < deadline)
        {
            if (Files.exists(complete))
            {
                assertEquals("PASS", Files.readString(complete).trim(), "The real admin-host browser proof must pass");
                Files.writeString(evidence.resolve("environment.txt"),
                    "Real admin-host browser handoff: PASS; logical domain fixture=p01-content.example.invalid\n", StandardOpenOption.APPEND);
                return;
            }
            Thread.sleep(250);
        }
        fail("Timed out waiting for the real admin-host browser proof");
    }

    private void loadDetachedFixture() throws Exception
    {
        String url = System.getenv("WEBJET_P01_JDBC_URL");
        String user;
        String password;
        if (url != null)
        {
            assertEquals("jdbc:mariadb://127.0.0.1:33316/webjet_p01", url,
                "P01 preview may only use the disposable P01 URL or the authorized local profile");
            user = System.getenv().getOrDefault("WEBJET_P01_JDBC_USER", "root");
            password = System.getenv().getOrDefault("WEBJET_P01_JDBC_PASSWORD", "");
            sourceProfile = "webjet_p01 (disposable InnoDB)";
        }
        else
        {
            DocumentBuilderFactory xml = DocumentBuilderFactory.newInstance();
            xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = xml.newDocumentBuilder().parse(Path.of("src/main/resources/poolman-local.xml").toFile());
            Element datasource = (Element) document.getElementsByTagName("datasource").item(0);
            url = value(datasource, "url");
            assertEquals("jdbc:mariadb://host.docker.internal/webjetcms_autotest_web", url,
                "P01 preview may only read the explicitly authorized local test profile");
            user = value(datasource, "username"); password = value(datasource, "password");
            sourceProfile = "webjetcms_autotest_web (local read-only source)";
        }
        List<GroupDetails> groups = new ArrayList<>();
        Map<Integer, DocDetails> docs = new LinkedHashMap<>();
        try (Connection db = DriverManager.getConnection(url, user, password))
        {
            db.setReadOnly(true);
            try (Statement sql = db.createStatement(); ResultSet rows = sql.executeQuery(
                "SELECT g.group_id,g.group_name,g.navbar,g.default_doc_id,d.title,d.navbar AS doc_navbar "
                    + "FROM `groups` g JOIN documents d ON d.doc_id=g.default_doc_id WHERE d.available=true "
                    + "AND NOT EXISTS (SELECT 1 FROM `groups` earlier WHERE earlier.default_doc_id=g.default_doc_id AND earlier.group_id<g.group_id) "
                    + "ORDER BY g.group_id LIMIT 3"))
            {
                while (rows.next())
                {
                    GroupDetails group = group(rows.getInt("group_id"), rows.getString("group_name"), rows.getInt("default_doc_id"));
                    group.setNavbarName(rows.getString("navbar"));
                    groups.add(group);
                    DocDetails page = page(group.getDefaultDocId(), group.getGroupId(), rows.getString("title"), "/page" + groups.size() + ".html");
                    page.setNavbar(rows.getString("doc_navbar"));
                    docs.put(page.getDocId(), page);
                }
            }
            try (Statement sql = db.createStatement(); ResultSet rows = sql.executeQuery(
                "SELECT (SELECT COUNT(*) FROM `groups` WHERE group_id=" + NEW_GROUP + ")"
                    + "+(SELECT COUNT(*) FROM documents WHERE doc_id=" + NEW_DOC + ")"))
            { assertTrue(rows.next()); assertEquals(0, rows.getInt(1)); }
        }
        assertEquals(3, groups.size(), "The local test DB needs three folders with available default pages");
        assertEquals(3, docs.size(), "The preview fixture needs three distinct default page IDs");
        // Limit the real IDs/metadata to a deterministic, detached fixture graph.
        int rootId = groups.get(0).getGroupId();
        groups.get(0).setParentGroupId(0);
        groups.get(1).setParentGroupId(rootId);
        groups.get(2).setParentGroupId(rootId);
        docs.get(groups.get(0).getDefaultDocId()).setVirtualPath("/home.html");
        docs.get(groups.get(1).getDefaultDocId()).setVirtualPath("/existing.html");
        docs.get(groups.get(2).getDefaultDocId()).setVirtualPath("/target.html");
        views.put(0, new View(groups, docs));
        for (int branch : List.of(42, 43))
        {
            List<GroupDetails> workGroups = new ArrayList<>();
            for (GroupDetails source : groups)
            {
                GroupDetails copy = group(source.getGroupId(), source.getGroupName(), source.getDefaultDocId());
                copy.setNavbarName(source.getNavbarName()); copy.setParentGroupId(source.getParentGroupId());
                workGroups.add(copy);
            }
            Map<Integer, DocDetails> workDocs = new LinkedHashMap<>();
            for (DocDetails source : docs.values())
                workDocs.put(source.getDocId(), page(source.getDocId(), source.getGroupId(), source.getTitle(), source.getVirtualPath()));
            GroupDetails added = group(NEW_GROUP, "P01 NEW section", NEW_DOC);
            added.setParentGroupId(rootId); workGroups.add(added);
            workGroups.get(1).setParentGroupId(NEW_GROUP);
            workGroups.get(1).setNavbarName("P01 renamed section " + branch);
            workGroups.get(1).setGroupName("P01 renamed section " + branch);
            workDocs.put(NEW_DOC, page(NEW_DOC, NEW_GROUP, "P01 new default", "/new.html"));
            views.put(branch, new View(workGroups, workDocs));
        }
    }

    private void prepareWebroot() throws Exception
    {
        for (String relative : List.of(TEMPLATE, MENU, "/WEB-INF/iwcm.tld", "/WEB-INF/iway.tld"))
        {
            Path target = root.resolve(relative.substring(1));
            Files.createDirectories(target.getParent()); Files.copy(WEBAPP.resolve(relative.substring(1)), target);
        }
        for (String name : List.of("probe.css", "probe.js"))
        {
            Path target = root.resolve("templates/p01/" + name); Files.createDirectories(target.getParent());
            try (InputStream in = getClass().getResourceAsStream("/branch-p01/" + name)) { Files.copy(Objects.requireNonNull(in), target); }
        }
        for (int branch : List.of(0, 42, 43))
        {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); graphics.setColor(branch == 0 ? Color.RED : branch == 42 ? Color.BLUE : Color.GREEN);
            graphics.fillRect(0, 0, 64, 64); graphics.dispose();
            Path imagePath = root.resolve("images/source-" + branch + "/p01.png"); Files.createDirectories(imagePath.getParent());
            ImageIO.write(image, "png", imagePath.toFile());
        }
    }

    private class Gateway extends HttpServlet
    {
        @Override protected void service(HttpServletRequest request, HttpServletResponse raw) throws IOException
        {
            HttpServletResponse response = new HttpServletResponseWrapper(raw) {
                @Override public void setHeader(String name, String value) { super.setHeader(name, "Cache-Control".equalsIgnoreCase(name) ? "private, no-store" : value); }
                @Override public void addHeader(String name, String value) { setHeader(name, value); }
            };
            response.setHeader("Cache-Control", "private, no-store"); response.setHeader("X-Robots-Tag", "noindex");
            try
            {
                if (!Set.of("GET", "HEAD").contains(request.getMethod())) { response.setStatus(405); return; }
                String path = request.getRequestURI();
                if ("/_p01_login".equals(path))
                {
                    if (!loginToken.equals(request.getParameter("token"))) { response.setStatus(403); return; }
                    Identity admin = new Identity(); admin.setAdmin(true); admin.setUserId(1);
                    request.getSession().setAttribute(Constants.USER_KEY, admin); response.setStatus(204); return;
                }
                int branch = 0;
                if (path.startsWith("/admin/webpages/branch-preview/"))
                {
                    Identity user = sk.iway.iwcm.users.UsersDB.getCurrentUser(request);
                    if (user == null || !user.isAdmin()) { response.setStatus(403); return; }
                    String[] parts = path.substring("/admin/webpages/branch-preview/".length()).split("/", 2);
                    if (parts.length != 2 || !Set.of("42", "43").contains(parts[0])) { response.setStatus(404); return; }
                    branch = Integer.parseInt(parts[0]); path = "/" + parts[1];
                }
                if (path.contains("..") || path.contains("%") || path.contains("\\")) { response.setStatus(400); return; }
                for (String forbidden : List.of("historyid", "forward", "forwarddoccompare", "inlineEditorAdmin"))
                    if (request.getParameter(forbidden) != null) { response.setStatus(400); return; }
                if ("/images/p01.png".equals(path))
                { response.setContentType("image/png"); response.getOutputStream().write(Files.readAllBytes(image(branch))); return; }
                if ("/thumb/images/p01.png".equals(path))
                {
                    request.setAttribute("path_filter_orig_path", "/thumb/images/source-" + branch + "/p01.png");
                    // Real ThumbServlet and resize execute. Only resize settings are fixture configuration;
                    // any unexpected gallery metadata write fails before reaching the database.
                    try (MockedStatic<GalleryDB> gallery = mockStatic(GalleryDB.class, invocation -> {
                        if ("setImage".equals(invocation.getMethod().getName()))
                            throw new AssertionError("P01 thumbnail must not write gallery metadata");
                        if ("getResizeMode".equals(invocation.getMethod().getName())) return "A";
                        return invocation.callRealMethod();
                    }))
                    {
                        new ThumbServlet().doGet(request, response);
                    }
                    return;
                }
                if (path.equals("/templates/p01/probe.css") || path.equals("/templates/p01/probe.js"))
                {
                    response.setContentType(path.endsWith(".css") ? "text/css" : "text/javascript");
                    String asset = Files.readString(root.resolve(path.substring(1)));
                    if (branch != 0) asset = asset.replace("'/images/p01.png'", "'" + prefix(branch) + "/images/p01.png'");
                    response.getWriter().print(asset); return;
                }
                View view = views.get(branch);
                String logicalPath = path;
                DocDetails doc = view.docs().values().stream().filter(d -> logicalPath.equals(d.getVirtualPath())).findFirst().orElse(null);
                if (doc == null) { response.setStatus(404); return; }
                render(request, response, branch, view, doc);
            }
            catch (Throwable failure)
            {
                failure.printStackTrace(); response.setStatus(500); response.getWriter().print("P01 prototype error: " + failure);
            }
        }
    }

    private void render(HttpServletRequest original, HttpServletResponse response, int branch, View view, DocDetails doc) throws Exception
    {
        DocDB docs = mock(DocDB.class); GroupsDB groups = mock(GroupsDB.class);
        when(docs.getBasicDocDetails(anyInt(), anyBoolean())).thenAnswer(call -> view.docs().get(call.getArgument(0)));
        when(docs.getDoc(anyInt())).thenAnswer(call -> view.docs().get(call.getArgument(0)));
        when(docs.getDocLink(anyInt(), any(HttpServletRequest.class))).thenAnswer(call -> view.docs().get(call.getArgument(0)).getVirtualPath());
        when(docs.getBasicDocDetailsByGroup(anyInt(), anyInt())).thenAnswer(call -> view.docs().values().stream()
            .filter(d -> d.getGroupId() == (int) call.getArgument(0)).toList());
        when(groups.getGroup(anyInt())).thenAnswer(call -> view.groups().stream().filter(g -> g.getGroupId() == (int) call.getArgument(0)).findFirst().orElse(null));
        when(groups.getGroupsAll()).thenReturn(view.groups());
        // PathFilter normally provides docid to ShowDoc; this prototype adapts the logical route
        // into that existing contract. It does not claim MenuULLI's no-docid bug has been fixed.
        HttpServletRequest request = new HttpServletRequestWrapper(original) {
            @Override public String getParameter(String name) { return "docid".equals(name) ? Integer.toString(doc.getDocId()) : super.getParameter(name); }
            @Override public String getRequestURI() { return doc.getVirtualPath(); }
            @Override public String getServletPath() { return doc.getVirtualPath(); }
            @Override public String getPathInfo() { return null; }
        };
        request.setAttribute("path_filter_orig_path", doc.getVirtualPath());
        request.setAttribute("docDetails", doc); request.setAttribute("isPreview", true);
        request.setAttribute("includePageParams", "rootGroupId=" + view.groups().get(0).getGroupId() + ",openAllItems=true,classes=full,maxLevel=-1");
        request.setAttribute("doc_temp_name", "blank-nojquery");
        try (MockedStatic<DocDB> docLookup = mockStatic(DocDB.class, invocation ->
                "getInstance".equals(invocation.getMethod().getName()) ? docs : invocation.callRealMethod());
             MockedStatic<GroupsDB> groupLookup = mockStatic(GroupsDB.class, invocation ->
                "getInstance".equals(invocation.getMethod().getName()) ? groups : invocation.callRealMethod()))
        {
            BufferResponse menu = new BufferResponse(response);
            request.getRequestDispatcher(MENU).include(request, menu);
            String label = branch == 0 ? "P01 LIVE body" : doc.getDocId() == NEW_DOC ? "P01 NEW default body " + branch : "P01 WORK " + branch + " body";
            request.setAttribute("doc_title", doc.getTitle());
            request.setAttribute("doc_data", "<nav id='p01-menu'>" + menu.text() + "</nav><main><h1>" + label + "</h1>"
                + "<link rel='stylesheet' href='/templates/p01/probe.css'><script src='/templates/p01/probe.js'></script>"
                + "<img id='original' src='/images/p01.png'><img id='thumbnail' src='/thumb/images/p01.png?w=16&amp;h=16'></main>");
            BufferResponse page = new BufferResponse(response);
            request.getRequestDispatcher(TEMPLATE).include(request, page);
            var html = Jsoup.parse(page.text());
            if (branch != 0)
                for (var element : html.select("[href],[src]"))
                    for (String attribute : List.of("href", "src"))
                        if (element.hasAttr(attribute) && element.attr(attribute).startsWith("/") && !element.attr(attribute).startsWith("//"))
                            element.attr(attribute, prefix(branch) + element.attr(attribute));
            response.setContentType("text/html;charset=UTF-8"); response.getWriter().print(html.outerHtml());
        }
    }

    private Path image(int branch) { return root.resolve("images/source-" + branch + "/p01.png"); }
    private static String prefix(int branch) { return "/admin/webpages/branch-preview/" + branch; }
    private static String value(Element parent, String name) { return parent.getElementsByTagName(name).item(0).getTextContent().trim(); }
    private static GroupDetails group(int id, String name, int defaultDoc)
    {
        GroupDetails group = new GroupDetails(); group.setGroupId(id); group.setGroupName(name); group.setNavbarName(name);
        group.setDefaultDocIdNoNavbar(defaultDoc); group.setMenuType(GroupDetails.MENU_TYPE_NORMAL); group.setLoggedMenuType(-1); group.setHtmlHead("");
        return group;
    }
    private static DocDetails page(int id, int group, String title, String path)
    {
        DocDetails page = new DocDetails(); page.setDocId(id); page.setGroupId(group); page.setTitle(title); page.setNavbar(title);
        page.setVirtualPath(path); page.setAvailable(true); page.setShowInMenu(true); page.setPasswordProtected(""); page.setExternalLink("");
        return page;
    }
    private static HttpResponse<byte[]> get(HttpClient client, String url) throws Exception
    { return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()); }
    private static String html(HttpClient client, String url) throws Exception
    {
        HttpResponse<byte[]> response = get(client, url);
        assertEquals(200, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        assertTrue(response.headers().firstValue("cache-control").orElse("").contains("no-store"));
        return new String(response.body(), StandardCharsets.UTF_8);
    }
    private record View(List<GroupDetails> groups, Map<Integer, DocDetails> docs) { }
    private static class BufferResponse extends HttpServletResponseWrapper
    {
        private final StringWriter text = new StringWriter();
        BufferResponse(HttpServletResponse response) { super(response); }
        @Override public PrintWriter getWriter() { return new PrintWriter(text); }
        String text() { return text.toString(); }
    }
}
