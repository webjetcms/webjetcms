<%@ page pageEncoding="utf-8" %><%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");%><%@
        page import="sk.iway.iwcm.Identity" %><%@
        page import="sk.iway.iwcm.users.UsersDB" %><%@
        page import="sk.iway.iwcm.system.elfinder.FsService" %><%@
        page import="org.json.JSONObject" %><%@
        page import="sk.iway.iwcm.Tools" %><%@
        page import="sk.iway.iwcm.editor.UploadFileAction" %><%@
        page import="sk.iway.iwcm.Constants" %><%@
        page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.common.UploadFileTools" %><%

    Identity user = UsersDB.getCurrentUser(request);
    if (user == null || user.isAdmin()==false) return;

    String url = Tools.getRequestParameter(request, "url");
    if(url == null) {
        return;
    }

    if (url.indexOf("?")>0) url = url.substring(0, url.indexOf("?"));
    if (url.startsWith("/thumb/")) url = url.substring("/thumb".length());
    String hashParent = null;
    String volume = "iwcm_1_";

    int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docId"), -1);
    int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupId"), -1);
    String title = Tools.getRequestParameter(request, "title");
    String rootDir = Constants.getString("imagesRootDir");
    if (url!=null && url.startsWith("/files")) rootDir = Constants.getString("filesRootDir");
    String uploadSubDir = UploadFileTools.getPageUploadSubDir(docId, groupId, title, rootDir).replace("//", "/");
    String uploadSubDirGallery = UploadFileTools.getPageUploadSubDir(docId, groupId, title, "/images/gallery").replace("//", "/");

    //startsWith / preto, ze externe linky nam to vracia ako www.webjet.sk a podobne
    if (url!=null && (url.contains(":default") || url.startsWith("/")==false)) url = uploadSubDir+"/default";
    url = Tools.replace(url, "//", "/");

    if (Tools.isEmpty(url))
    {
       url = uploadSubDir;
       hashParent = "iwcm_fs_ap_volume_";
       volume = "iwcm_fs_ap_volume_";
    }

    if (url.startsWith("/templates"))
    {
        volume = "iwcm_2_";
    }

    JSONObject object = new JSONObject();

    String hash = FsService.getHash(url);
    if (hashParent == null) hashParent =  FsService.getHash(url.substring(0, url.lastIndexOf("/")));

    if ((url.startsWith(uploadSubDir) || url.startsWith(uploadSubDirGallery)) && uploadSubDir.length()>5)
    {
       //je to podadresar Media aktualnej stranky
        volume = "iwcm_fs_ap_volume_";
        hash = Tools.replace(hash, "iwcm_1_", volume);
        hashParent = Tools.replace(hashParent, "iwcm_1_", volume);
    }
    int docIdFromUrl = DocDB.getInstance().getDocIdFromURLImpl(url, DocDB.getDomain(request));
    //System.out.println("docIdFromUrl="+docIdFromUrl);
    if (docIdFromUrl > 0)
    {
       //naslo sa to ako web stranka
        volume = "iwcm_doc_group_volume_";
        DocDetails doc = DocDB.getInstance().getBasicDocDetails(docIdFromUrl, false);
        if (doc != null)
        {
           //System.out.println("doc="+doc);
            hash = Tools.replace(FsService.getHash("/doc:"+doc.getDocId()), "iwcm_1_", volume);
            hashParent = Tools.replace(FsService.getHash("/group:"+doc.getGroupId()), "iwcm_1_", volume);
        }
    }

    object.put("volume", volume);
    object.put("hash", hash);
    object.put("hashParent", hashParent);
    object.put("url", url);
    object.put("uploadSubDir", uploadSubDir);
    object.put("docIdFromUrl", docIdFromUrl);

    //System.out.print(object.toString());

    out.print(object.toString());
    //System.out.println("hethash, url="+url);
    //System.out.println(object.toString());
%>
