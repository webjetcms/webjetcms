package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.editor.ThumbServlet;
import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.JarPackaging;
import sk.iway.iwcm.test.BaseWebjetTest;

/** The ordinary servlet and WORK previews share parameter parsing and pixels, while retaining separate bytes and caches. */
class BranchThumbnailTest extends BaseWebjetTest {
    @TempDir Path root;
    private ServletContext previousContext;
    private final Map<String,String> settings = new LinkedHashMap<>();
    private final Map<String,Path> selected = new LinkedHashMap<>();
    private final AtomicReference<BranchContext> context = new AtomicReference<>(BranchContext.live("example.sk",1));
    private MockedStatic<BranchContexts> contexts;
    private MockedStatic<BranchFiles> files;
    private MockedStatic<BranchReadiness> storage;
    private MockedStatic<PathFilter> protection;
    private MockedStatic<SpamProtection> spam;
    private MockedStatic<GalleryDB> gallery;
    private MockedStatic<ImageTools> tools;
    private MockedStatic<IwcmFsDB> fs;
    private MockedStatic<JarPackaging> packaging;

    @BeforeEach void fixture() throws Exception {
        root = root.toRealPath();
        previousContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override public String getRealPath(String path) { return local(path).toString(); }
        });
        for (var entry : Map.ofEntries(Map.entry("multiDomainEnabled","false"), Map.entry("enableStaticFilesExternalDir","false"),
                Map.entry("thumbServletCacheDir","/WEB-INF/imgcache/"), Map.entry("thumbServletAllowedSizeMode","allow"),
                Map.entry("thumbWriteServer",""), Map.entry("thumbWriteNodeName",""), Map.entry("galleryEnableWatermarking","false"),
                Map.entry("imageMagickDir",root.resolve("no-imagemagick").toString()), Map.entry("imageMaxThumbsWidth","4096"),
                Map.entry("imageMaxThumbsHeight","4096"), Map.entry("galleryImageQuality","")).entrySet()) {
            settings.put(entry.getKey(),Constants.getString(entry.getKey()));Constants.setString(entry.getKey(),entry.getValue());
        }
        contexts = mockStatic(BranchContexts.class);
        contexts.when(BranchContexts::current).thenAnswer(call -> context.get());
        files = mockStatic(BranchFiles.class, call -> {
            if (!"read".equals(call.getMethod().getName())) return call.callRealMethod();
            String path = call.getArgument(0);
            Path source = selected.containsKey(path) ? selected.get(path) : local(path);
            return source != null && Files.isRegularFile(source) ? source : null;
        });
        storage = mockStatic(BranchReadiness.class);
        storage.when(BranchReadiness::storageRoot).thenReturn(root.resolve("private"));
        protection = mockStatic(PathFilter.class, call -> "getRealPath".equals(call.getMethod().getName()) ? local(call.getArgument(0)).toString() : null);
        spam = mockStatic(SpamProtection.class, call -> "canPost".equals(call.getMethod().getName()) ? true : RETURNS_DEFAULTS.answer(call));
        GalleryBean crop = new GalleryBean();crop.setSelectedX(20);crop.setSelectedY(20);crop.setSelectedWidth(100);crop.setSelectedHeight(60);
        gallery = mockStatic(GalleryDB.class, call -> switch (call.getMethod().getName()) {
            case "getGalleryBean" -> crop;
            case "getResizeMode" -> "R";
            case "setImage", "setUploadDateImage" -> throw new AssertionError("Thumbnail rendering wrote gallery metadata");
            default -> call.callRealMethod();
        });
        fs = mockStatic(IwcmFsDB.class);
        fs.when(() -> IwcmFsDB.getVirtualPath(anyString())).thenAnswer(call -> call.getArgument(0));
        packaging = mockStatic(JarPackaging.class);
        tools = mockStatic(ImageTools.class, CALLS_REAL_METHODS);
        tools.when(ImageTools::existsImageMagickConvertCommand).thenReturn(false);
        for (String extension : new String[]{"png","jpg"}) {
            image(local("/images/prepared."+extension),120,80,extension);
            image(local("/images/o_prepared."+extension),240,160,extension);
        }
    }

    @AfterEach void cleanup() {
        for (AutoCloseable mock : new AutoCloseable[]{tools,packaging,fs,gallery,spam,protection,storage,files,contexts}) {
            if (mock != null) try {mock.close();} catch (Exception exception) {throw new IllegalStateException(exception);}
        }
        Constants.setServletContext(previousContext);settings.forEach(Constants::setString);
    }

    static Stream<Arguments> parameters() {
        return Stream.of("png","jpg").flatMap(extension -> Stream.of("w=30&h=20", "w=30&h=0&ip=1", "w=0&h=20&ip=2",
            "w=30&h=20&ip=3&c=00ffff&q=90", "w=30&h=20&ip=4&c=00ffff&q=80", "w=30&h=20&ip=5",
            "w=30&h=20&ip=6", "w=30&h=20&ip=6&noip=true", "w=30&h=20&ip=5&q=95")
            .map(query -> Arguments.of(extension,query)));
    }

    /** Every supported crop mode, color, quality and noip produces the same bytes as LIVE without changing its cache. */
    @ParameterizedTest @MethodSource("parameters")
    void sharedParametersMatchTheOrdinaryServlet(String extension,String query) throws Exception {
        String path = "/images/prepared."+extension;
        byte[] original = Files.readAllBytes(local(path));
        var live = render(false,path,query);
        assertEquals(200,live.getStatus());assertTrue(live.getContentType().startsWith("image/"));
        Map<String,String> cache = liveCache();
        var work = render(true,path,query);
        assertEquals(200,work.getStatus());
        assertEquals(live.getContentType().split(";",2)[0],work.getContentType().split(";",2)[0]);
        assertArrayEquals(live.getContentAsByteArray(),work.getContentAsByteArray());
        assertTrue(work.getHeader("Cache-Control").contains("no-store"));
        assertEquals(cache,liveCache());assertArrayEquals(original,Files.readAllBytes(local(path)));
    }

    /** The filename syntax reaches the same parser even when the preview URL has an administration prefix. */
    @Test void dimensionsCropColorAndQualityCanComeFromTheFilename() throws Exception {
        String path = "/images/prepared-30x20ip6c00ffffq90.png";
        var live = render(false,path,"");var work = render(true,path,"");
        assertEquals(200,work.getStatus());assertArrayEquals(live.getContentAsByteArray(),work.getContentAsByteArray());
    }

    /** WORK replacements use their own bytes instead of an older gallery original, and a deletion hides cached results. */
    @Test void workingBytesAndDeletionNeverFallBackToTheGalleryOriginalOrCachedLive() throws Exception {
        String path = "/images/prepared.png", query = "w=30&h=20&ip=6";
        var before = render(true,path,query);
        Path staged = root.resolve("prepared-content");
        BufferedImage green = new BufferedImage(60,40,BufferedImage.TYPE_INT_RGB);
        var graphics = green.createGraphics();graphics.setColor(Color.GREEN);graphics.fillRect(0,0,60,40);graphics.dispose();
        ImageIO.write(green,"png",staged.toFile());selected.put(path,staged);
        var changed = render(true,path,query);
        assertEquals(200,changed.getStatus());
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(changed.getContentAsByteArray()));
        assertEquals(Color.GREEN.getRGB(),result.getRGB(10,10));
        assertFalse(java.util.Arrays.equals(before.getContentAsByteArray(),changed.getContentAsByteArray()));
        selected.put(path,null);assertEquals(404,render(true,path,query).getStatus());
    }

    private MockHttpServletResponse render(boolean work,String path,String query) throws Exception {
        context.set(work ? new BranchContext(42,"example.sk",1,BranchContext.Mode.PREVIEW) : BranchContext.live("example.sk",1));
        var request = new MockHttpServletRequest("GET",(work ? BranchPreviewUrls.PREFIX+"42/thumb" : "/thumb")+path);
        Identity admin = new Identity();admin.setAdmin(true);admin.setUserId(1);request.getSession().setAttribute(Constants.USER_KEY,admin);
        request.setQueryString(query);
        if (!query.isEmpty()) for (String pair : query.split("&")) {String[] item=pair.split("=",2);request.setParameter(item[0],item[1]);}
        var response = new MockHttpServletResponse();
        if (work) {
            var wrapper = new BranchPreviewResponseWrapper(response,request);
            BranchFiles.thumbnail(path,request,wrapper);wrapper.finish();
        } else ThumbServlet.getThumbImage(path,request,response);
        return response;
    }

    private Path local(String path) {return root.resolve(path.replaceFirst("^/",""));}
    private Map<String,String> liveCache() throws Exception {
        var result = new LinkedHashMap<String,String>();Path directory=local("/WEB-INF/imgcache");
        if (Files.isDirectory(directory)) try (var paths=Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) result.put(path.toString(),Files.getLastModifiedTime(path)+":"+BranchFiles.hash(path));
        }
        return result;
    }
    private static void image(Path path,int width,int height,String extension) throws Exception {
        Files.createDirectories(path.getParent());BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        for (int x=0;x<width;x++) for (int y=0;y<height;y++) image.setRGB(x,y,new Color(x*255/width,y*255/height,120).getRGB());
        ImageIO.write(image,extension,path.toFile());
    }
}
