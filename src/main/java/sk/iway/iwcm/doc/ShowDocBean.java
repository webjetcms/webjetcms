package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Bean, ktory umoznuje doplnit data pri zobrazeni stranky, publikuje sa ako event
 */
public class ShowDocBean {

   private HttpServletRequest request;
   private HttpServletResponse response;
   private DocDetails doc;
   private int docId;
   private int historyId;

   //vystupny doc objekt, ak sa nastavi, pouzije sa ako doc objekt pre zobrazenie web stranky cez /showdoc.do
   private DocDetails forceShowDoc;

   public HttpServletRequest getRequest() {
      return request;
   }
   public void setRequest(HttpServletRequest request) {
      this.request = request;
   }
   public HttpServletResponse getResponse() {
      return response;
   }
   public void setResponse(HttpServletResponse response) {
      this.response = response;
   }
   public DocDetails getDoc() {
      return doc;
   }
   public void setDoc(DocDetails doc) {
      this.doc = doc;
   }
   public int getDocId() {
      return docId;
   }
   public void setDocId(int docId) {
      this.docId = docId;
   }
   public int getHistoryId() {
      return historyId;
   }
   public void setHistoryId(int historyId) {
      this.historyId = historyId;
   }
   public DocDetails getForceShowDoc() {
      return forceShowDoc;
   }
   public void setForceShowDoc(DocDetails forceShowDoc) {
      this.forceShowDoc = forceShowDoc;
   }

}
