package sk.iway.iwcm.branch;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupsDB;

/** Lists explicit WORK changes, including deletions which are absent from the normal tree. */
@RestController
@RequestMapping("/admin/rest/branch-content")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches&menuWebpages')")
public class BranchEditorRestController {
    @GetMapping
    public List<Change> changes(HttpServletRequest request) {
        new BranchEditor(request);
        Identity user=new BranchAccess().requireUser(request);
        List<Change> changes=new ArrayList<>();
        for(BranchType type:List.of(BranchType.GROUP,BranchType.DOCUMENT)) for(Map<String,Object> row:BranchView.current().workRows(type)) {
            int scope=((Number)row.get(type==BranchType.GROUP?"parent_group_id":"group_id")).intValue();
            if (!GroupsDB.isGroupEditable(user,scope) && (type!=BranchType.GROUP || !GroupsDB.isGroupEditable(user,((Number)row.get("group_id")).intValue()))) continue;
            BranchChange change=BranchStore.change(type,row);
            changes.add(new Change(type, Integer.parseInt(change.objectKey()), Objects.toString(row.get(type==BranchType.GROUP?"group_name":"title"),""),change.operation().name(),change.work().hash()));
        }
        return changes;
    }

    @PostMapping("/revert")
    public Map<String,Boolean> revert(@RequestBody Revert input,HttpServletRequest request) {
        new BranchEditor(request).revert(input.type(),input.id(),input.hash());
        return Map.of("ok",true);
    }
    public record Change(BranchType type,int id,String title,String operation,String hash) { }
    public record Revert(BranchType type,int id,String hash) { }
}
