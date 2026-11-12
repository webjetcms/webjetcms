package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.users.UsersDB;

/** Explicit package actions use technical LIVE reads, independent of the editor's selected branch. */
@RestController
@RequestMapping("/admin/rest/branches/{id}")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches')")
public class BranchMergeRestController {
    private final BranchMergeService merge=new BranchMergeService();
    private final BranchAccess access=new BranchAccess();
    private final BranchFileRepository files;

    public BranchMergeRestController(BranchFileRepository files) { this.files=files; }

    public record Revision(long expectedRevision) { }
    public record Resolution(long expectedRevision,BranchType type,String objectKey,BranchDecision.Source choice,String liveHash,String workHash) { }
    public record Execution(long expectedRevision,String preparationHash) { }
    public record Schedule(long expectedRevision,String preparationHash,long publishAt) { }

    @GetMapping("/progress")
    public BranchMergeService.Report detail(@PathVariable long id,HttpServletRequest request) {
        Identity user=access.requireUser(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {return merge.detail(id,domain,user);}
    }
    @PostMapping("/prepare")
    public BranchMergeService.Report prepare(@PathVariable long id,@RequestBody Revision input,HttpServletRequest request) {
        Identity user=fresh(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {return merge.prepare(id,domain,user,input.expectedRevision());}
    }
    @PostMapping("/resolve")
    public BranchMergeService.Report resolve(@PathVariable long id,@RequestBody Resolution input,HttpServletRequest request) {
        Identity user=fresh(request);String domain=access.requireDomain(request,user);
        if(input.type()==null||input.choice()==null||input.objectKey()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {return merge.resolve(id,domain,user,input.expectedRevision(),input.type(),input.objectKey(),input.choice(),input.liveHash(),input.workHash());}
    }
    @PostMapping("/merge")
    public ResponseEntity<BranchMergeService.Report> publish(@PathVariable long id,@RequestBody Execution input,HttpServletRequest request) {return execute(id,input,request,false);}
    @PostMapping("/resume")
    public ResponseEntity<BranchMergeService.Report> resume(@PathVariable long id,@RequestBody Execution input,HttpServletRequest request) {return execute(id,input,request,true);}

    @PostMapping("/schedule")
    public BranchMergeService.Report schedule(@PathVariable long id,@RequestBody Schedule input,HttpServletRequest request) {
        Identity user=fresh(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {return merge.schedule(id,domain,user,input.expectedRevision(),input.preparationHash(),input.publishAt());}
    }
    @PostMapping("/unschedule")
    public BranchMergeService.Report unschedule(@PathVariable long id,@RequestBody Revision input,HttpServletRequest request) {
        Identity user=fresh(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {return merge.unschedule(id,domain,user,input.expectedRevision());}
    }
    @PostMapping("/reopen")
    public BranchMergeService.Report reopen(@PathVariable long id,@RequestBody Revision input,HttpServletRequest request) {
        Identity user=fresh(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {return merge.reopen(id,domain,user,input.expectedRevision());}
    }

    private ResponseEntity<BranchMergeService.Report> execute(long id,Execution input,HttpServletRequest request,boolean resume) {
        Identity user=fresh(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {
            BranchMergeService.Report report=merge.claim(id,domain,user,input.expectedRevision(),input.preparationHash(),resume);
            new BranchPublishService().kickoff(id,user.getUserId());
            return ResponseEntity.accepted().body(report);
        }
    }
    private Identity fresh(HttpServletRequest request) {
        Identity current=access.requireUser(request);access.requireCsrf(request);
        var user=UsersDB.getUser(current.getUserId());
        if(user==null||!user.isAdmin()||!user.isAuthorized())throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return new Identity(user);
    }

    /** Serves the three actual binary versions, without applying the chosen preview result. */
    @GetMapping("/file-version")
    public void file(@PathVariable long id,@RequestParam String path,@RequestParam String version,HttpServletRequest request,HttpServletResponse response)throws IOException {
        Identity user=access.requireUser(request);String domain=access.requireDomain(request,user);
        try(BranchContexts.Scope ignored=BranchContexts.live(request,domain,user.getUserId())) {
            var report=merge.detail(id,domain,user);
            String logical=BranchFilePaths.normalize(path);
            var item=report.items().stream().filter(i->"FILE".equals(i.type())&&logical.equals(i.objectKey())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            BranchMergeService.Version snapshot=switch(version){case "BASE"->item.base();case "LIVE"->item.live();case "WORK"->item.work();default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST);};
            if(!"ACTIVE".equals(snapshot.state())||!"FILE".equals(snapshot.fields().get("entry_kind")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            Path file;
            if("LIVE".equals(version))file=Path.of(Tools.getRealPath(logical));
            else {
                var row=files.findByBranchIdAndPathKey(id,BranchFilePaths.key(logical))
                    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
                file=BranchFiles.workPath(id,row.getRowId());
                if("BASE".equals(version))file=file.resolveSibling("base");
            }
            if(!Files.isRegularFile(file)||!Objects.equals(snapshot.fields().get("content_hash"),BranchFiles.hash(file)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Podklad súboru sa zmenil. Obnovte porovnanie.");
            response.setHeader("Cache-Control","private, no-store");response.setHeader("X-Content-Type-Options","nosniff");
            boolean image=logical.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpe?g|gif|webp)$");
            response.setHeader("Content-Disposition",(image?"inline":"attachment")+"; filename=\""+fileName(logical)+"\"");response.setContentType(image?Objects.toString(request.getServletContext().getMimeType(logical),"application/octet-stream"):"application/octet-stream");response.setContentLengthLong(Files.size(file));Files.copy(file,response.getOutputStream());
        }
    }
    private static String fileName(String path){return path.substring(path.lastIndexOf('/')+1).replaceAll("[^a-zA-Z0-9._-]","_");}
}
