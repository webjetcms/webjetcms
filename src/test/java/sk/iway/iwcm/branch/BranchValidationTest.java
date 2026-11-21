package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;

/** Validation uses each view's already loaded metadata rather than querying pages in a loop. */
class BranchValidationTest {
    /** Existing page references and defaults must not cause per-page or repeated list queries. */
    @Test
    void validatesAnUnchangedDomainWithoutReloadingItsPages() {
        DocDetails first=page(1,"/first/"),second=page(2,"/second/");first.setHeaderDocId(2);
        GroupDetails root=group(10,"root");root.setDefaultDocId(1);
        BranchView before=view(List.of(first,second),List.of(root));
        BranchView after=view(List.of(first,second),List.of(root));

        validate(before,after);

        verify(before,never()).document(anyInt(),anyBoolean());
        verify(after,never()).document(anyInt(),anyBoolean());
        verify(before,never()).documents(anyBoolean());
        verify(after,never()).documents(anyBoolean());
    }

    /** A new URL collision is rejected using the correct before and after snapshots. */
    @Test
    void rejectsNewPageUrlCollisions() {
        BranchView before=view(List.of(page(1,"/first/"),page(2,"/second/")),List.of(group(10,"root")));
        BranchView after=view(List.of(page(1,"/first/"),page(2,"/first/")),List.of(group(10,"root")));
        assertTrue(assertThrows(ResponseStatusException.class,()->validate(before,after)).getReason().contains("URL collision"));
    }

    /** Deleting a referenced page still fails even if the referring page itself was not edited. */
    @Test
    void rejectsDeletedReferences() {
        DocDetails referring=page(1,"/first/");referring.setHeaderDocId(2);
        BranchView before=view(List.of(referring,page(2,"/second/")),List.of(group(10,"root")));
        BranchView after=view(List.of(referring),List.of(group(10,"root")));
        assertTrue(assertThrows(ResponseStatusException.class,()->validate(before,after)).getReason().contains("removed page 2"));
    }

    /** Unrelated pre-existing duplicate URLs and missing references remain tolerated. */
    @Test
    void toleratesUnchangedLegacyInconsistencies() {
        DocDetails first=page(1,"/legacy/");first.setHeaderDocId(999);
        List<DocDetails> pages=List.of(first,page(2,"/legacy/"));
        validate(view(pages,List.of(group(10,"root"))),view(pages,List.of(group(10,"root"))));
    }

    /** Moving a folder's default page to another folder requires a valid replacement. */
    @Test
    void rejectsInvalidatedFolderDefaults() {
        GroupDetails root=group(10,"root");root.setDefaultDocId(1);
        DocDetails moved=page(1,"/first/");moved.setGroupId(20);
        BranchView before=view(List.of(page(1,"/first/")),List.of(root,group(20,"other")));
        BranchView after=view(List.of(moved),List.of(root,group(20,"other")));
        assertTrue(assertThrows(ResponseStatusException.class,()->validate(before,after)).getReason().contains("Invalid default page"));
    }

    /** Adding a folder must still reject an effective directory path already used by another folder. */
    @Test
    void rejectsNewFolderUrlCollisions() {
        BranchView before=view(List.of(),List.of(group(10,"root")));
        BranchView after=view(List.of(),List.of(group(10,"root"),group(20,"root")));
        assertTrue(assertThrows(ResponseStatusException.class,()->validate(before,after)).getReason().contains("Folder URL collision"));
    }

    /** A newly introduced invalid reference cannot be excused as a pre-existing legacy value. */
    @Test
    void rejectsNewMissingReferences() {
        DocDetails changed=page(1,"/first/");changed.setHeaderDocId(999);
        BranchView before=view(List.of(page(1,"/first/")),List.of(group(10,"root")));
        BranchView after=view(List.of(changed),List.of(group(10,"root")));
        assertTrue(assertThrows(ResponseStatusException.class,()->validate(before,after)).getReason().contains("removed page 999"));
    }

    private static void validate(BranchView before,BranchView after) {
        BranchEditor.validateResult(before,after);
    }

    private static BranchView view(List<DocDetails> documents,List<GroupDetails> groups) {
        BranchView view=mock(BranchView.class);
        Map<Integer,DocDetails> pages=new LinkedHashMap<>();documents.forEach(doc->pages.put(doc.getDocId(),doc));
        when(view.documents(true)).thenReturn(documents);
        when(view.document(anyInt(),eq(true))).thenAnswer(call->pages.get(call.getArgument(0)));
        when(view.metadataDocument(anyInt())).thenAnswer(call->pages.get(call.getArgument(0)));
        when(view.metadataDocuments()).thenReturn(pages.values());
        when(view.groups()).thenReturn(groups);
        when(view.group(anyInt())).thenAnswer(call->groups.stream().filter(group->group.getGroupId()==(int)call.getArgument(0)).findFirst().orElse(null));
        return view;
    }

    private static DocDetails page(int id,String path) {DocDetails doc=new DocDetails();doc.setDocId(id);doc.setGroupId(10);doc.setVirtualPath(path);return doc;}
    private static GroupDetails group(int id,String directory) {GroupDetails group=new GroupDetails();group.setGroupId(id);group.setParentGroupId(0);group.setUrlDirName(directory);group.setGroupName(directory);return group;}
}
