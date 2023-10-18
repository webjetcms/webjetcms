<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@ page import="java.util.ArrayList" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
	String searchParam = Tools.getRequestParameter(request, "url");
	String docId = Tools.getRequestParameter(request, "docid");
	//welcome.jsp docId/address search
	if (searchParam == null) searchParam = docId;
	if (searchParam == null) searchParam = Tools.getRequestParameter(request, "text");
	if (searchParam == null) return;
	if (searchParam!=null && searchParam.startsWith("http") && searchParam.length()>9) {
		int i = searchParam.indexOf("/", 9);
		if (i>0) searchParam = searchParam.substring(i);
	}
	final String search = DB.internationalToEnglish(searchParam.toLowerCase());

	if (docId != null)
	{
		//to speed up - editor knows what doc id is he going to choose
		if (Tools.isInteger(docId)) return;
	}

	JSONArray hints = new JSONArray();
	List<DocDetails> documents = DocDB.getInstance().getBasicDocDetailsAll();

	SelectionFilter filter = null;
	if (Tools.getRequestParameter(request, "url") != null || Tools.getRequestParameter(request, "docid") != null)
	{
		filter = new SelectionFilter<DocDetails>(){

			public boolean fullfilsConditions(DocDetails doc){
				return doc.getVirtualPath().toLowerCase().contains(search) ||
				DB.internationalToEnglish(doc.getTitle().toLowerCase()).contains(search);
			}
		};
	}
	else
	{
		filter = new SelectionFilter<DocDetails>(){

			public boolean fullfilsConditions(DocDetails doc){
				return DB.internationalToEnglish(doc.getTitle().toLowerCase()).contains(search);
			}
		};
	}

	List<DocDetails> domainFilteredDocs;
	if(Constants.getBoolean("multiDomainEnabled")){
		domainFilteredDocs = new ArrayList<>();
		String currentDomain = DocDB.getDomain(request);
		for(DocDetails doc : documents){
			String domain = doc.getFieldT();
			if(currentDomain.equals(domain)){
				domainFilteredDocs.add(doc);
			}
		}
	} else {
		domainFilteredDocs = documents;
	}

	domainFilteredDocs = Tools.filter(domainFilteredDocs, filter);

	//usporiadaj podla abecedy, aby aj najkratsi vyraz bol prvy
	Collections.sort(domainFilteredDocs, new Comparator<DocDetails>(){
		public int compare(DocDetails doc1, DocDetails doc2){
			if (doc1.getVirtualPath()==null) doc1.setVirtualPath("");
         	if (doc2.getVirtualPath()==null) doc2.setVirtualPath("");
			if (doc1.getVirtualPath().length()==doc2.getVirtualPath().length()) {
				return doc1.getVirtualPath().compareTo(doc2.getVirtualPath());
			} else {
				//kratsie URL na zaciatku
				return doc1.getVirtualPath().length()-doc2.getVirtualPath().length();
			}
		}
	});

	for (DocDetails row : domainFilteredDocs)
	{
		JSONObject entry = new JSONObject();
		entry.put("doc_id", row.getDocId());
		if (Tools.getRequestParameter(request, "docid") != null)
		{
			entry.put("value", row.getDocId());
			entry.put("label", row.getVirtualPath());
		}
		else if (Tools.getRequestParameter(request, "url") != null){
			entry.put("value", row.getVirtualPath());
			entry.put("label", row.getVirtualPath());
		}
		else{
			entry.put("value", row.getTitle());
			entry.put("label", row.getTitle());
		}
			hints.put(entry);

		if (hints.length()>50) break;
	}
	out.print(hints);
%>
