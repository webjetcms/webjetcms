package sk.iway.iwcm.branch;

import java.util.*;
import sk.iway.iwcm.doc.*;

/** Small read integration surface for adapted core and customer components. */
public final class BranchReadService {
    private BranchReadService() { }
    public static DocDetails getDoc(int id) { BranchView view=BranchView.current(); return view==null?DocDB.getInstance().getDoc(id):view.document(id,false); }
    public static GroupDetails getGroup(int id) { BranchView view=BranchView.current(); return view==null?GroupsDB.getInstance().getGroup(id):view.group(id); }
    public static List<GroupDetails> groups() { BranchView view=BranchView.current(); return view==null?GroupsDB.getInstance().getGroupsAll():view.groups(); }
    public static List<DocDetails> documents() { BranchView view=BranchView.current(); return view==null?DocDB.getInstance().getBasicDocDetailsAll():view.documents(); }
    public static int docIdFromUrl(String path) {
        BranchView view=BranchView.current();
        if (view != null) return view.docIdFromUrl(path);
        return DocDB.getInstance().getDocIdFromURLImpl(path,BranchContexts.current().domainName());
    }
    public static String docLink(int id) {
        BranchView view=BranchView.current();
        DocDetails doc=view==null?getDoc(id):view.metadataDocument(id); if (doc==null) return "#nopage";
        return sk.iway.iwcm.Tools.isNotEmpty(doc.getExternalLink())?doc.getExternalLink():doc.getVirtualPath();
    }
}
