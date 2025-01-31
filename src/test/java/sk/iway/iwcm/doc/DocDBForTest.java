package sk.iway.iwcm.doc;

import gnu.trove.TObjectIntHashMap;

/**
 * This class is used for testing purposes only,
 * allowing to access protected methods in DocDB.
 */
public class DocDBForTest {

    private DocDB docDB;
    private int startDocId = 500;

    public DocDBForTest() {
        //private constructor
        this.docDB = DocDB.getInstance();
    }

    public void addUrlToInternalCache(DocDetails doc, GroupDetails group) {
        if (doc.getDocId() < 1) {
            doc.setDocId(startDocId++);
        }
        TObjectIntHashMap<String> mUrls = docDB.getUrlsByUrlDomains(group.getDomainName(), false);
        mUrls.put(doc.getVirtualPath(), doc.getDocId());
    }

}
