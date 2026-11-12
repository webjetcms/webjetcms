package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import sk.iway.iwcm.*;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.PasswordSecurity;

/** Explicitly retained local demo, restricted to the disposable P01 database. */
@EnabledIfEnvironmentVariable(named="WEBJET_P03_MANUAL",matches="true")
class BranchManualFixtureTest extends BaseWebjetTest {
    private static final String DOMAIN="iwcm.interway.sk";
    private final BranchStore store=new BranchStore();
    @Test void prepareRetainedManualPreview()throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            assertEquals("webjet_p01",connection.getCatalog());assertTrue(connection.getMetaData().getURL().contains(":33316/"));
            try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT count(*) FROM branches WHERE name='P03 manual A'")){result.next();assertEquals(0,result.getInt(1),"Manual fixture already exists; use its manifest");}
        }
        Path privateRoot=Path.of("/tmp/webjet-p03-private");Files.createDirectories(privateRoot);
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);Constants.setString("branchStorageRoot",privateRoot.toString());
        int template;
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("INSERT INTO templates(temp_name,forward,lng,templates_group_id) VALUES ('P03 manual','/templates/aceintegration/jet/blank.jsp','sk',1)",Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();try(var keys=statement.getGeneratedKeys()){keys.next();template=keys.getInt(1);}
        }
        int group;
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);group=BranchStore.reserveNative(connection,BranchType.GROUP);
            var row=new LinkedHashMap<>(BranchGroupMapper.initialValues(group,DOMAIN));row.put("group_name","P03 manual");row.put("navbar","P03 manual");row.put("url_dir_name","p03-manual");row.put("temp_id",template);BranchRowWriter.insertGroupWithId(connection,BranchContext.live(DOMAIN,1),row);connection.commit();
        }
        int home=liveDoc(group,template,"P03 LIVE home","/p03-manual/",1,"<p>Produkčný obsah P03.</p>");
        int detail=liveDoc(group,template,"P03 LIVE detail","/p03-manual/detail.html",10,"<p>Produkčný detail.</p>");
        String news="!INCLUDE(/components/news/news-velocity.jsp, groupIds="+group+", template=p03-manual, publishType=all, perexNotRequired=false, order=priority, ascending=true, pageSize=2, paging=true, includeActualDoc=false)!";
        String menu="!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId="+group+", openAllItems=true, classes=full)!";
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("INSERT INTO news_templates(name,template_html,paging_html,paging_position,engine,domain_id) VALUES ('p03-manual',?,?,1,'velocity',1)")) {
            statement.setString(1,"<ol class='news'>#foreach($item in $news)<li><a href='$actionBean.link($item)'>$item.title</a><span>$item.htmlData</span><img src='$item.perexImage' width='64'/></li>#end</ol>");
            statement.setString(2,"<nav>#foreach($item in $pagesAll)<a href='$item.url'>$item.label</a> #end</nav>");statement.executeUpdate();
        }
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE documents SET data=? WHERE doc_id=?")) {statement.setString(1,"<h1>P03 LIVE home</h1>"+menu+news);statement.setInt(2,home);statement.executeUpdate();}
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE `groups` SET default_doc_id=? WHERE group_id=?")){statement.setInt(1,home);statement.setInt(2,group);statement.executeUpdate();}
        var a=branch("P03 manual A");var b=branch("P03 manual B");
        for(var context:List.of(a,b)) {
            Map<String,Object> original;try(Connection connection=DBPool.getConnection()){original=BranchStore.readLive(connection,BranchType.DOCUMENT,home).orElseThrow();}
            var changed=new LinkedHashMap<>(original);changed.put("require_ssl",true);changed.put("title","P03 WORK "+(context.equals(a)?"A":"B"));changed.put("data","<h1>"+changed.get("title")+"</h1>"+menu+news+"<p><a href='/files/branch-p03-manual/proof.pdf'>Pracovné PDF</a></p><img src='/thumb/images/branch-p03-manual/proof.png?w=16&h=16'>");
            store.put(context,BranchType.DOCUMENT,home,changed,BranchStore.snapshot(BranchType.DOCUMENT,original).hash(),false);
            for(int index=1;index<=3;index++) {
                var row=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.now(),false));row.put("group_id",group);row.put("temp_id",template);row.put("title","Pracovná novinka "+index+" "+context.branchId());row.put("navbar",row.get("title"));row.put("virtual_path","/p03-manual/new-"+context.branchId()+"-"+index+".html");row.put("available",true);row.put("show_in_menu",true);row.put("sort_priority",index+10);row.put("html_data","Pracovný perex "+index);row.put("data","<h1>"+row.get("title")+"</h1>"+menu);row.put("perex_image","/images/branch-p03-manual/proof.png");store.create(context,BranchType.DOCUMENT,row);
            }
            Path image=Files.createTempFile(privateRoot,"image-",".png");BufferedImage pixels=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);var graphics=pixels.createGraphics();graphics.setColor(context.equals(a)?java.awt.Color.RED:java.awt.Color.BLUE);graphics.fillRect(0,0,64,64);graphics.dispose();ImageIO.write(pixels,"png",image.toFile());stage(context,"/images/branch-p03-manual/proof.png",image,"image/png");
            Path pdf=Files.createTempFile(privateRoot,"pdf-",".pdf");pdf(pdf,context.branchId());stage(context,"/files/branch-p03-manual/proof.pdf",pdf,"application/pdf");
        }
        String password=PasswordSecurity.generatePassword(24),salt=PasswordSecurity.generateSalt();
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("INSERT INTO users(login,password,password_salt,is_admin,authorized,first_name,last_name,email,editable_groups,editable_pages,writable_folders,disabled_items) VALUES ('tester-p03',?,?,1,1,'P03','Local fixture','p03@example.invalid','','','/images,/files','')")) {statement.setString(1,PasswordSecurity.calculateHash(password,salt));statement.setString(2,salt);statement.executeUpdate();}
        Path credentials=Path.of("/tmp/webjet-p03-login.properties");Files.writeString(credentials,"login=tester-p03\npassword="+password+"\n");Files.setPosixFilePermissions(credentials,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        for(var setting:Map.of("branchesEnabled","true","branchSingleWriterConfirmed","true","branchStorageRoot",privateRoot.toString(),"multiDomainEnabled","true").entrySet())
            try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("INSERT INTO _conf_(name,value) VALUES (?,?) ON DUPLICATE KEY UPDATE value=VALUES(value)")){statement.setString(1,setting.getKey());statement.setString(2,setting.getValue());statement.executeUpdate();}
        Files.writeString(Path.of("/tmp/webjet-p03-manual.properties"),"branchA="+a.branchId()+"\nbranchB="+b.branchId()+"\ngroupId="+group+"\nhomeId="+home+"\ndetailId="+detail+"\nurl=http://iwcm.interway.sk:8086/admin/webpages/branch-preview/"+a.branchId()+"/p03-manual/\n");
        Constants.setBoolean("branchesEnabled",false);
    }
    /** Refreshes only this fixture's retained attachment with a valid, viewable PDF. */
    @Test void refreshRetainedFixturePdf()throws Exception {
        Constants.setString("branchStorageRoot","/tmp/webjet-p03-private");
        try(Connection connection=DBPool.getConnection()) {
            assertEquals("webjet_p01",connection.getCatalog());assertTrue(connection.getMetaData().getURL().contains(":33316/"));
            try(PreparedStatement statement=connection.prepareStatement("SELECT f.row_id,f.branch_id FROM branch_files f JOIN branches b ON b.id=f.branch_id WHERE b.name IN ('P03 manual A','P03 manual B') AND f.logical_path='/files/branch-p03-manual/proof.pdf'");ResultSet rows=statement.executeQuery()) {
                while(rows.next()) {
                    long row=rows.getLong(1),branch=rows.getLong(2);Path bytes=BranchFiles.workPath(branch,row);pdf(bytes,branch);
                    try(PreparedStatement update=connection.prepareStatement("UPDATE branch_files SET work_content_hash=?,size_bytes=? WHERE row_id=?")){update.setString(1,BranchFiles.hash(bytes));update.setLong(2,Files.size(bytes));update.setLong(3,row);update.executeUpdate();}
                }
            }
        }
    }
    private static void pdf(Path path,long branch)throws Exception {
        try(var document=new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page=new org.apache.pdfbox.pdmodel.PDPage();document.addPage(page);
            try(var content=new org.apache.pdfbox.pdmodel.PDPageContentStream(document,page)) {
                content.beginText();content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),18);content.newLineAtOffset(60,740);content.showText("P03 private working PDF - branch "+branch);content.endText();
            }
            document.save(path.toFile());
        }
    }
    private int liveDoc(int group,int template,String title,String url,int priority,String html)throws Exception {
        try(Connection connection=DBPool.getConnection()) {connection.setAutoCommit(false);int id=BranchStore.reserveNative(connection,BranchType.DOCUMENT);var row=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,Instant.now(),false));row.put("title",title);row.put("navbar",title);row.put("group_id",group);row.put("temp_id",template);row.put("virtual_path",url);row.put("available",true);row.put("show_in_menu",true);row.put("sort_priority",priority);row.put("html_data","LIVE perex");row.put("data",html);BranchRowWriter.insertDocumentWithId(connection,BranchContext.live(DOMAIN,1),row);connection.commit();return id;}
    }
    private BranchContext branch(String name){var value=new BranchService().create(BranchTestDomains.ensure(DOMAIN),name,"Retained P03 local fixture",1);return new BranchContext(value.getId(),DOMAIN,1,BranchContext.Mode.ADMIN);}
    private void stage(BranchContext context,String path,Path source,String mime)throws Exception {var row=store.putFile(context,path,"FILE",BranchSnapshot.absent(),BranchFiles.hash(source),Files.size(source),mime,false);Path target=BranchFiles.workPath(context.branchId(),((Number)row.get("row_id")).longValue());Files.createDirectories(target.getParent());Files.move(source,target);}
}
