<%@page import="java.util.Map"%><%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %>
<%@ page import="sk.iway.iwcm.io.IwcmFile" %>
<%@ page import="sk.iway.iwcm.stripes.SyncDirAction" %>
<%@ page import="sk.iway.iwcm.sync.WarningListener" %>
<%@ page import="sk.iway.iwcm.system.ConfDB" %>
<%@ page import="sk.iway.iwcm.system.zip.ZipEntry" %>
<%@ page import="sk.iway.iwcm.system.zip.ZipInputStream" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="java.beans.XMLDecoder" %>
<%@ page import="java.util.*" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.common.CloudToolsForCore" %>
<%@ page import="sk.iway.iwcm.utils.Pair" %>

<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchiveService"%>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchiveRepository"%>
<%@ page import="sk.iway.iwcm.Tools"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<iwcm:checkLogon admin="true" perms="menuFileArchivImportFiles"/>
<%
    request.setAttribute("cmpName", "components.file_archiv.name");
    request.setAttribute("dialogTitleKey", "components.file_archiv.import_main_files");
    request.setAttribute("dialogDescKey", "components.file_archiv.import_main_files_desc");
%>
<%!
    private static final int  BUFFER_SIZE = 4096;

    public static class SortByName implements Comparator<FileArchivatorBean> {
        public int compare(FileArchivatorBean fab1, FileArchivatorBean fab2) {
            return (fab1.getFilePath()+fab1.getFileName()).compareTo(fab2.getFilePath()+fab2.getFileName());
        }
    }
    private static void extractFile(ZipInputStream in, IwcmFile outdir, String name) throws IOException
    {
        //System.out.println("extractFile");
        byte[] buffer = new byte[BUFFER_SIZE];
        IwcmFile iwcmFile = new IwcmFile(outdir,name);
        //System.out.println(iwcmFile.getParentFile().getVirtualPath());
        if(!iwcmFile.getParentFile().exists())
            iwcmFile.getParentFile().mkdir();
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(iwcmFile.getPath()));
        int count;
        while ((count = in.read(buffer)) != -1)
            out.write(buffer, 0, count);
        out.close();
    }

    private static void mkdirs(IwcmFile outdir,String path)
    {
        //System.out.println("mkdirs");
        IwcmFile d = new IwcmFile(outdir, path);
        if( !d.exists() )
            d.mkdirs();
    }

    private static String dirpart(String name)
    {
        //System.out.println("dirpart");
        int s = name.lastIndexOf( File.separatorChar );
        return s == -1 ? null : name.substring( 0, s );
    }

    /***
     * Extract zipfile to outdir with complete directory structure
     * @param zipfile Input .zip file
     * @param outdir Output directory
     */
    public static boolean extract(IwcmFile zipfile, IwcmFile outdir)
    {
        //System.out.println("extractFile");
        try
        {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipfile.getPath()));
            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null)
            {
                name = entry.getName();
                if( entry.isDirectory() )
                {
                    mkdirs(outdir,name);
                    continue;
                }
        /* this part is necessary because file entry can come before
         * directory entry where is file located
         * i.e.:
         *   /foo/foo.txt
         *   /foo/
         */
                dir = dirpart(name);
                if( dir != null )
                    mkdirs(outdir,dir);

                extractFile(zin, outdir, name);
            }
            zin.close();
        }
        catch (IOException e)
        {
            sk.iway.iwcm.Logger.error(e);
            return false;
        }
        return true;
    }

    public static  Object getXmlFileArchiv(String tmpArchivPath, String dirUnpack, String XML_FILE_ENCODED_FILE_NAME ) throws IOException
    {
        File f = new File(Tools.getRealPath(tmpArchivPath+dirUnpack+XML_FILE_ENCODED_FILE_NAME));
        Object objUpdates;
        if(f.exists())
        {
            InputStream is = new FileInputStream(f);
            is = SyncDirAction.checkXmlForAttack(is);
            XMLDecoder decoder = new XMLDecoder(is, null, new WarningListener());
            objUpdates = decoder.readObject();

            if (!(objUpdates instanceof ArrayList))
            {
                Logger.debug("file_archiv/import_archiv.jsp","Nepodporovany typ suboru");
                decoder.close();
                return null;
            }
            return objUpdates;
        }
        return null;
    }

    public static Long prepareAndSaveBean(FileArchivatorBean farchBean, String tmpArchivPath, String dirUnpack, String fileArchivAllowExt, StringBuilder suffixes, List<String> writtenFiles, List<String> overwrittenFiles, Identity user, HttpServletRequest request, FileArchiveRepository far) {
        farchBean.setUserId((int) user.getUserId());
        //nastvime univerzalne aby bolo mozne editovat z lubovolnej domeny

        farchBean.setDomainId(CloudToolsForCore.getDomainId());
        farchBean.setDateInsert(new Date());
        farchBean.setGlobalId(FileArchivatorKit.generateNextGlobalId());

        //ak sa nejaka pripona zo zipu nenachadza v archive, pridame ju
        if(Tools.isNotEmpty(fileArchivAllowExt) &&
                !fileArchivAllowExt.contains(FileArchivatorKit.getFileExtension(farchBean.getFileName())))
        {
            if(Constants.getString("fileArchivAllowExt").endsWith(","))
            {
                ConfDB.setName("fileArchivAllowExt", Constants.getString("fileArchivAllowExt") + FileArchivatorKit.getFileExtension(farchBean.getFileName()));
                suffixes.append(FileArchivatorKit.getFileExtension(farchBean.getFileName()));
            }
            else if(Tools.isNotEmpty(Constants.getString("fileArchivAllowExt")))
            {
                ConfDB.setName("fileArchivAllowExt", Constants.getString("fileArchivAllowExt") +","+ FileArchivatorKit.getFileExtension(farchBean.getFileName()));
                suffixes.append(FileArchivatorKit.getFileExtension(farchBean.getFileName()));
            }
            else if(Tools.isEmpty(Constants.getString("fileArchivAllowExt")))
            {
                ConfDB.setName("fileArchivAllowExt", FileArchivatorKit.getFileExtension(farchBean.getFileName()));
                suffixes.append(FileArchivatorKit.getFileExtension(farchBean.getFileName()));
            }
        }

        String fabParameter = "fab_"+farchBean.getFileArchiveId();
        //az tu musime nastavit id, pretoze v predoslom forme sa pouziva ID na odoslanie
        farchBean.setFileArchiveId(-1);
        IwcmFile src = new IwcmFile(Tools.getRealPath(tmpArchivPath+dirUnpack+farchBean.getFilePath()+farchBean.getFileName()));
        //pri vytvarani archivu sme satry prefix archivu odstranili, teraz musime pridat novy prefix
        farchBean.setFilePath(FileArchivatorKit.getArchivPath()+farchBean.getFilePath());
        IwcmFile dest = new IwcmFile(Tools.getRealPath(/*FileArchivatorKit.getArchivPath()+*/farchBean.getFilePath()+farchBean.getFileName()));

        //Get id of same file from DB, if exist, use this id
        Long toId = (long)-1;
        if(far != null) {
            toId = FileArchiveService.getId(farchBean.getFilePath(), farchBean.getFileName(), far);

            if(toId.longValue() > 0) {
                farchBean.setId(toId);
            }
        }
        if(FileTools.isFile(dest.getVirtualPath()))
        {
            if(Tools.isNotEmpty(Tools.getRequestParameter(request, fabParameter)))
            {
                if(FileTools.copyFile(src,dest))
                {
                    overwrittenFiles.add(dest.getVirtualPath());
                    return farchBean.saveAndReturnId();
                }
            }
        }
        else
        {
            if(FileTools.copyFile(src,dest))
            {
                writtenFiles.add(dest.getVirtualPath());
                return farchBean.saveAndReturnId();
            }
        }

        return (long)-1;
    }
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
    <div class="padding10">
    <%

    Prop prop = Prop.getInstance(request);
    String dirTmp = "tmp/";
    String dirUnpack = "unpack/";
    String tmpArchivPath =  FileArchivatorKit.getArchivPath()+dirTmp;
    String XML_FILE_ENCODED_FILE_NAME = "file_archiv_export_objects.xml";
    IwcmFile outDir = new IwcmFile(Tools.getRealPath(tmpArchivPath+dirUnpack));

    //nacitame list subor z xml
    Object objUpdates = getXmlFileArchiv(tmpArchivPath,dirUnpack,XML_FILE_ENCODED_FILE_NAME);
    //zo suboru
    List<FileArchivatorBean> fileArchivBeans = new ArrayList<>();
    if(objUpdates != null)
        fileArchivBeans = (List<FileArchivatorBean>)objUpdates;

    String fileArchivAllowExt =  Constants.getString("fileArchivAllowExt");
    StringBuilder suffixes = new StringBuilder();
    if(Tools.getRequestParameter(request, "send") != null)
    {
        Identity user = UsersDB.getCurrentUser(request);
        if(user == null)
        {
            out.println(prop.getText("user.notLoggedError"));
            return;
        }

        Logger.debug(null,"import_archiv.jsp Začínam import");
        List<String> writtenFiles = new ArrayList<>();
        List<String> overwrittenFiles = new ArrayList<>();

        Map<Long, Pair<Long, Integer>> replacedIds = new Hashtable<>();

        FileArchiveRepository far = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);

        //First only main files
        for(FileArchivatorBean farchBean : fileArchivBeans) {
            if(farchBean.getReferenceId() < 1) {
                Long oldId = farchBean.getId();
                Long newId = prepareAndSaveBean(farchBean, tmpArchivPath, dirUnpack, fileArchivAllowExt, suffixes, writtenFiles, overwrittenFiles, user, request, far);
                if(newId != null && newId > 0) replacedIds.put(oldId, new Pair<Long, Integer>(newId, farchBean.getGlobalId()) );
            }
        }

        //Now only history files
        for(FileArchivatorBean farchBean : fileArchivBeans) {
            if(farchBean.getReferenceId() > 0) {
                Long oldReferenceId = farchBean.getReferenceId();
                prepareAndSaveBean(farchBean, tmpArchivPath, dirUnpack, fileArchivAllowExt, suffixes, writtenFiles, overwrittenFiles, user, request, far);

                //Update reference id if found
                Pair<Long, Integer> pairValues = replacedIds.get(oldReferenceId);
                if(pairValues != null) {
                    farchBean.setReferenceId(pairValues.getFirst());
                    farchBean.setGlobalId(pairValues.getSecond());
                    farchBean.save();
                }
            }
        }

        if(!Tools.isEmpty(writtenFiles))
        {
            out.println("<h2>Zapísné súbory:</h2>");
            for (String filenaName : writtenFiles)
            {
                out.println(filenaName+"<br>");
            }
        }

        if(!Tools.isEmpty(overwrittenFiles))
        {
            out.println("<h2>Prepísané súbory:</h2>");
            for (String filenaName : overwrittenFiles)
            {
                out.println(filenaName+"<br>");
            }
        }

        %><br><iwcm:text key="components.file_archiv.import_archiv.import_ukonceny"/><%
        Logger.debug(null,"import_archiv.jsp ImportImpl ukončený zapísaných "+(overwrittenFiles.size()+writtenFiles.size())+" súborov.");

        if(Tools.isNotEmpty(suffixes.toString()))
            out.println(prop.getText("components.file_archiv.import_archiv.do_archivu_suborov_boli_pridane_nasledujuce_pripony") + suffixes);

        //zmazem po importe zo zip.
        FileTools.deleteDirTree(outDir.getParentFile());
        %>
        <script type="text/javascript">
            function Ok()
            {
                window.close();
            }
        </script>

        <%@ include file="/admin/layout_bottom_dialog.jsp" %><%
        return;
    }

    if (Tools.getRequestParameter(request, "saveFile") != null)
    {
        request.setAttribute("writePath", tmpArchivPath);
        pageContext.include("/sk/iway/iwcm/system/ConfImport.action");
    }

    if( request.getAttribute("successful") == null )
    {
        //zmazem pred importom novych
        FileTools.deleteDirTree(outDir.getParentFile());
        outDir.mkdir();
            %>
        <script type="text/javascript">
            function Ok() {
                $('#saveFileForm').click();
            }

            function showLoadingMsg() {
                $("#loadingMsg").show();
            }
        </script>
        <div class="padding10">
            <h2>
                <%-- 	<iwcm:text key="admin.conf_import.import_konfigurácie"/> --%>
                <iwcm:text key="components.file_archiv.import_archiv.vlozte_vyexportovany_zip_subor_z_exportu_archivu_suborov_"/>
            </h2>

            <h2 id="loadingMsg" style="color: green; display: none;"><iwcm:text key="components.file_archiv.import_in_progress"/></h2>

            <iwcm:stripForm name="saveFileForm" id="saveFileForm_ID" action="<%=PathFilter.getOrigPathUpload(request)%>" method="post" beanclass="sk.iway.iwcm.system.ConfImportAction">
                <div style="width: 350px; height: 400px;">

                    <input type="submit" style="float: right;" class="button" id="saveFileForm" name="saveFile" value="Načítať" onclick="showLoadingMsg()">
                    <div style="overflow: hidden; padding-right: .5em;">
                        <stripes:file name="xmlFile"  id="xmlFile"  />
                    </div>
                </div>
            </iwcm:stripForm>

        </div>
        <%
    }
    else
    {
        //out.print("Rozbalujem<br>");
        IwcmFile zipFile = new IwcmFile(Tools.getRealPath(tmpArchivPath+request.getAttribute("file_name")));
        extract(zipFile,outDir);
        Logger.debug(null,"import_archiv.jsp unzipping "+zipFile.getVirtualPath());
        //out.print("rozbalene<br>");

        //nacitame list subor z xml
        objUpdates = getXmlFileArchiv(tmpArchivPath,dirUnpack,XML_FILE_ENCODED_FILE_NAME);

        if(objUpdates != null)
            fileArchivBeans = (List<FileArchivatorBean>)objUpdates;

        Map<Long, FileArchivatorBean> destinationDuplicates = new Hashtable<>();//duplikaty na novom prostredi

        fileArchivBeans.sort(new SortByName());
        for(FileArchivatorBean fab:fileArchivBeans)
        {
            FileArchivatorBean fabDuplicate =  FileArchivatorDB.getByPath(FileArchivatorKit.getArchivPath() + fab.getFilePath(),fab.getFileName());
            if(fabDuplicate != null)
            {
                destinationDuplicates.put(fab.getId(),fab);
            }
        }
        Map<Long, FileArchivatorBean> uploadDuplicates = new Hashtable<>(destinationDuplicates);//duplikaty na novom prostredi
        Logger.debug(null,"import_archiv.jsp Nasli sme "+uploadDuplicates.size()+" duplikátov z "+fileArchivBeans.size()+" súborov");
        request.setAttribute("uploadDuplicates",uploadDuplicates);
        %>
        <logic:empty name="uploadDuplicates">
            <script type="text/javascript">
                $(document).ready(function(){
                    $('#btnPokracovatImport').click();
                });

            </script>
        </logic:empty>
        <form name="archiv_import" method="post">
            <h2><iwcm:text key="components.file_archiv.import_archiv.niektore_z_importovanych_suborov_sa_uz_v_archive_nachadzaju_vyberte_ci_sa_maju_nahradit_"/></h2>
            <display:table name="uploadDuplicates" id="fab" class="sort_table table table-hover table-wj dataTable no-footer">
                <%FileArchivatorBean faBean = (FileArchivatorBean)pageContext.getAttribute("fab");%>
                <display:column titleKey="components.file_archiv.import_archiv.cesta_a_nazov_suboru">
                <%
                out.print(faBean.getFilePath()+faBean.getFileName());
                %>
                </display:column>
                <%pageContext.setAttribute("recordDate1",prop.getText("components.file_archiv.import_archiv.datum_nahratia_zip_upload__domain1"));
                    pageContext.setAttribute("recordDate2",prop.getText("components.file_archiv.import_archiv.datum_nahratia_zip_upload__domain2",Tools.getServerName(request)));
                    pageContext.setAttribute("fileSize1",prop.getText("components.file_archiv.import_archiv.velkost_suboru_kb_zip_upload__domain1"));
                    pageContext.setAttribute("fileSize2",prop.getText("components.file_archiv.import_archiv.velkost_suboru_kb_zip_upload__domain2",Tools.getServerName(request)));
                %>
                <display:column title="${recordDate1}<br>${recordDate2}">
                    <%=Tools.formatDate(faBean.getDateInsert())%> / <%=Tools.formatDate(destinationDuplicates.get(faBean.getId()).getDateInsert())%>
                </display:column>

                <display:column title="${fileSize1}<br>${fileSize2}">
                    <%=faBean.getFileSize()/1024%> / <%=destinationDuplicates.get(faBean.getId()).getFileSize()/1024%>
                </display:column>

                <display:column titleKey="components.file_archiv.import_archiv.nahradit_aktualny_subor_suborom_zo_zip_archivu">
                    <input type="checkbox" checked="checked" name="fab_<%=faBean.getFileArchiveId()%>"/> <iwcm:text key="editor.search.replace_with"/>
                </display:column>
            </display:table>
            <input type="submit" style="display: none" id="btnPokracovatImport" name="send" value="<%=prop.getText("button.continue")%>">
            <script type="text/javascript">
                function Ok()
                {
                    $('#btnPokracovatImport').click();
                }
            </script>
        </form>
        <%
    }
%>
    </div>
    <%@ include file="/admin/layout_bottom_dialog.jsp" %>
