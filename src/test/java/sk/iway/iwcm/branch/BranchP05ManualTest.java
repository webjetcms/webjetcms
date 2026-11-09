package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import sk.iway.iwcm.*;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Retained P05 UI data only; never reuses or edits the P04 demo branch. */
@EnabledIfEnvironmentVariable(named="WEBJET_P05_MANUAL",matches="true")
class BranchP05ManualTest extends BaseWebjetTest {
    private static final String DOMAIN="iwcm.interway.sk";
    @Test void prepareUiFixture()throws Exception {
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            assertEquals("webjet_p01",c.getCatalog());assertTrue(c.getMetaData().getURL().contains(":33316/"));
            try(ResultSet r=s.executeQuery("SELECT COUNT(*) FROM branches WHERE name='autotest P05 media/files A'")){r.next();assertEquals(0,r.getInt(1),"P05 fixture already exists; reuse its manifest");}
        }
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setString("branchStorageRoot","/tmp/webjet-p03-private");Files.createDirectories(Path.of("/tmp/webjet-p03-private"));
        Constants.setBoolean("multiDomainEnabled",true);
        Path sources=Path.of("/tmp/webjet-p05-ui");Files.createDirectories(sources);
        Path image=Path.of(Tools.getRealPath("/images/branch-p05-autotest/proof.png")),pdf=Path.of(Tools.getRealPath("/files/branch-p05-autotest/proof.pdf"));
        assertFalse(Files.exists(image),"Refusing to replace a pre-existing fixture file");assertFalse(Files.exists(pdf),"Refusing to replace a pre-existing fixture file");
        Files.createDirectories(image.getParent());Files.createDirectories(pdf.getParent());
        image(image,Color.BLUE);image(sources.resolve("proof.png"),Color.RED);image(sources.resolve("ckeditor-autotest.png"),Color.GREEN);
        try(var document=new org.apache.pdfbox.pdmodel.PDDocument()) {
            var sheet=new org.apache.pdfbox.pdmodel.PDPage();document.addPage(sheet);
            try(var content=new org.apache.pdfbox.pdmodel.PDPageContentStream(document,sheet)){content.beginText();content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),18);content.newLineAtOffset(60,740);content.showText("autotest P05 original production attachment");content.endText();}document.save(pdf.toFile());
        }
        Files.copy(pdf,sources.resolve("autotest-new.pdf"),StandardCopyOption.REPLACE_EXISTING);
        int template,group,page;
        int category=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media_groups")),mediaId=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media"));
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement s=c.prepareStatement("INSERT INTO templates(temp_name,forward,lng,templates_group_id) VALUES ('autotest P05','/templates/aceintegration/jet/blank.jsp','sk',1)",Statement.RETURN_GENERATED_KEYS)){s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){keys.next();template=keys.getInt(1);}}
            group=BranchStore.reserveNative(c,BranchType.GROUP);var folder=new LinkedHashMap<>(BranchGroupMapper.initialValues(group,DOMAIN));folder.put("group_name","autotest P05 media and files");folder.put("navbar","autotest P05 media and files");folder.put("url_dir_name","p05-autotest");folder.put("temp_id",template);BranchRowWriter.insertGroupWithId(c,BranchContext.live(DOMAIN,1),folder);
            page=BranchStore.reserveNative(c,BranchType.DOCUMENT);var doc=new LinkedHashMap<>(BranchDocumentMapper.initialValues(page,1,Instant.parse("2020-01-01T12:00:00Z"),false));
            doc.put("title","autotest P05 old page");doc.put("navbar","autotest P05 old page");doc.put("group_id",group);doc.put("temp_id",template);doc.put("virtual_path","/p05-autotest/");doc.put("available",true);doc.put("html_data","autotest P05 old perex");doc.put("perex_image","/images/branch-p05-autotest/proof.png");
            doc.put("data","<h1>autotest P05 old page</h1><img class='p05-original' src='/images/branch-p05-autotest/proof.png'><img class='p05-thumb' src='/thumb/images/branch-p05-autotest/proof.png?w=16&h=16'>!INCLUDE(/components/news/news-velocity.jsp, groupIds="+group+", template=p05-autotest, publishType=all, perexNotRequired=true, order=priority, ascending=true, pageSize=10, paging=false, includeActualDoc=true)!");
            BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),doc);
            try(PreparedStatement s=c.prepareStatement("UPDATE `groups` SET default_doc_id=? WHERE group_id=?")){s.setInt(1,page);s.setInt(2,group);s.executeUpdate();}
            try(PreparedStatement s=c.prepareStatement("INSERT INTO media_groups(media_group_id,media_group_name,available_groups) VALUES (?,?,'')")){s.setInt(1,category);s.setString(2,"autotest P05 media category");s.executeUpdate();}
            var media=new LinkedHashMap<>(BranchMediaMapper.initialValues(mediaId,page,1));media.put("media_title_sk","autotest P05 LIVE attachment");media.put("media_link","/files/branch-p05-autotest/proof.pdf");media.put("media_thumb_link","/images/branch-p05-autotest/proof.png");media.put("last_update",Timestamp.from(Instant.parse("2020-01-01T11:59:00Z")));BranchRowWriter.insertMediaWithId(c,BranchContext.live(DOMAIN,1),media,List.of(category));
            try(PreparedStatement s=c.prepareStatement("INSERT INTO news_templates(name,template_html,paging_html,paging_position,engine,domain_id) VALUES ('p05-autotest',?,'',1,'velocity',1)")){s.setString(1,"<ol class='p05-news'>#foreach($item in $news)<li data-id='$item.docId'><a href='$actionBean.link($item)'>$item.title</a><span>$item.htmlData</span><img class='p05-news-image' src='$item.perexImage'>#foreach($medium in $MediaDB.getMedia($item, ''))<a class='p05-news-media' data-media='$medium.mediaId' href='$medium.mediaLink'>$medium.mediaTitleSk</a>#end</li>#end</ol>");s.executeUpdate();}
            c.commit();
        }
        BranchContext a=new BranchContext(new BranchService().create(DOMAIN,"autotest P05 media/files A","Retained local P05 UI fixture",1).getId(),DOMAIN,1,BranchContext.Mode.ADMIN);
        long b=new BranchService().create(DOMAIN,"autotest P05 media/files B","Unchanged comparison version",1).getId();
        var fresh=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.now(),false));fresh.put("group_id",group);fresh.put("temp_id",template);fresh.put("title","autotest P05 WORK-only page");fresh.put("navbar","autotest P05 WORK-only page");fresh.put("virtual_path","/p05-autotest/work-only.html");fresh.put("available",true);fresh.put("html_data","autotest P05 WORK perex");fresh.put("data","<h1>autotest P05 WORK-only page</h1>");fresh.put("perex_image","/images/branch-p05-autotest/proof.png");
        int newPage=((Number)new BranchStore().create(a,BranchType.DOCUMENT,fresh).get("doc_id")).intValue();
        String manifest="branchA="+a.branchId()+"\nbranchB="+b+"\ngroupId="+group+"\npageId="+page+"\nhomeId="+page+"\nnewPageId="+newPage+"\nmediaId="+mediaId+"\ncategoryId="+category+"\nimage=/images/branch-p05-autotest/proof.png\npdf=/files/branch-p05-autotest/proof.pdf\nurl=http://iwcm.interway.sk:8086/admin/webpages/branch-preview/"+a.branchId()+"/p05-autotest/\n";
        Files.writeString(Path.of("/tmp/webjet-p05-manual.properties"),manifest);
        String password=sk.iway.iwcm.users.PasswordSecurity.generatePassword(24),salt=sk.iway.iwcm.users.PasswordSecurity.generateSalt();
        try(Connection c=DBPool.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO users(login,password,password_salt,is_admin,authorized,first_name,last_name,email,editable_groups,editable_pages,writable_folders,disabled_items) VALUES ('tester-p05',?,?,1,1,'autotest P05','Local fixture','p05@example.invalid','','',?,'')")) {
            s.setString(1,sk.iway.iwcm.users.PasswordSecurity.calculateHash(password,salt));s.setString(2,salt);s.setString(3,"/images/*\n/files/*");s.executeUpdate();
        }
        Path credentials=Path.of("/tmp/webjet-p05-login.properties");Files.writeString(credentials,"login=tester-p05\npassword="+password+"\n");Files.setPosixFilePermissions(credentials,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        Path evidence=Path.of("build/test-results/p05");Files.createDirectories(evidence);Files.writeString(evidence.resolve("fixture-identities.properties"),manifest);
    }
    /** Prepares retained B/L/W conflicts without choosing a result or executing publication. */
    @Test void prepareRetainedFileConflicts()throws Exception {
        try(Connection c=DBPool.getConnection()) {assertEquals("webjet_p01",c.getCatalog());assertTrue(c.getMetaData().getURL().contains(":33316/"));}
        Properties fixture=new Properties();try(var input=Files.newInputStream(Path.of("/tmp/webjet-p05-manual.properties"))){fixture.load(input);}
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setString("branchStorageRoot","/tmp/webjet-p03-private");
        BranchContext context=new BranchContext(Long.parseLong(fixture.getProperty("branchA")),DOMAIN,1,BranchContext.Mode.ADMIN);
        List<String> evidence=new ArrayList<>();BranchStore store=new BranchStore();
        for(String scenario:List.of("content","new","delete")) {
            String logical="/files/branch-p05-autotest/conflict-"+scenario+".txt";
            Path live=Path.of(Tools.getRealPath(logical));assertFalse(Files.exists(live),"Conflict fixture already exists");Files.createDirectories(live.getParent());
            if(!scenario.equals("new")) Files.writeString(live,"autotest P05 BASE "+scenario);
            BranchSnapshot base=BranchFiles.liveSnapshot(logical);boolean delete=scenario.equals("delete");
            Path incoming=Files.createTempFile("webjet-p05-conflict-",".txt");Files.writeString(incoming,"autotest P05 WORK "+scenario);
            try {
                Map<String,Object> row=store.putFile(context,logical,"FILE",base,delete?null:BranchFiles.hash(incoming),delete?null:Files.size(incoming),delete?null:"text/plain",delete);
                Path staged=BranchFiles.workPath(context.branchId(),((Number)row.get("row_id")).longValue());Files.createDirectories(staged.getParent());
                if(!delete) Files.copy(incoming,staged,StandardCopyOption.REPLACE_EXISTING);
                if(Files.exists(live)) Files.copy(live,staged.resolveSibling("base"),StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(live,"autotest P05 concurrent LIVE "+scenario);
                BranchChange change=BranchStore.change(BranchType.FILE,row);BranchSnapshot current=BranchFiles.liveSnapshot(logical);
                assertTrue(BranchDecision.resolve(change.base().hash(),current.hash(),change.work().hash(),null).conflict());
                evidence.add(scenario+".path="+logical);evidence.add(scenario+".rowId="+row.get("row_id"));evidence.add(scenario+".base="+change.base().hash());evidence.add(scenario+".live="+current.hash());evidence.add(scenario+".work="+change.work().hash());
            } finally {Files.deleteIfExists(incoming);}
        }
        Path report=Path.of("build/test-results/p05/conflicts.properties");Files.createDirectories(report.getParent());Files.write(report,evidence);
    }

    private static void image(Path target,Color color)throws Exception {BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();graphics.setColor(color);graphics.fillRect(0,0,64,64);graphics.dispose();ImageIO.write(image,"png",target.toFile());}
}
