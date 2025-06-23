<%@page import="java.util.List"%><%@ page import="sk.iway.iwcm.system.zip.ZipOutputStream" %>
<%@ page import="sk.iway.iwcm.io.IwcmFile" %>
<%@ page import="sk.iway.iwcm.system.zip.ZipEntry" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.beans.XMLEncoder" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.io.*, sk.iway.iwcm.components.file_archiv.*"%>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<iwcm:checkLogon admin="true" perms="menuFileArchivExportFiles"/>
<%
    request.setAttribute("cmpName", "components.file_archiv.name");
    request.setAttribute("dialogTitleKey", "components.file_archiv.export_main_files");
    request.setAttribute("dialogDescKey", "components.file_archiv.export_main_files_desc");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<%!
    private static void zipDir(String zipFileName, String dir, String replacePath, Prop prop, JspWriter outJsp) throws Exception {
        IwcmFile dirObj = new IwcmFile(dir);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        //System.out.println("Creating : " + zipFileName);
        addDir(dirObj, out, replacePath.length(), prop, outJsp);
        out.close();
    }

    static void addDir(IwcmFile dirObj, ZipOutputStream out, int replacePathLength, Prop prop, JspWriter outJsp) throws IOException {
        IwcmFile[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];
        for (IwcmFile file : files)
        {
            if (file.isDirectory())
            {
                addDir(file, out, replacePathLength, prop, outJsp);
                continue;
            }
            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            Logger.debug("export_archiv.jsp", "Adding to archive: " + file.getVirtualPath());
            out.putNextEntry(new ZipEntry(file.getVirtualPath().substring(replacePathLength + 2)));
            int len;
            while ((len = in.read(tmpBuf)) > 0)
            {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }

    public static String getTodayDateString()
    {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR)+"."+cal.get(Calendar.MONTH)+"."+cal.get(Calendar.DAY_OF_MONTH)+"_"+cal.get(Calendar.HOUR_OF_DAY)+"."+(cal.get(Calendar.MINUTE) < 10?"0"+cal.get(Calendar.MINUTE):cal.get(Calendar.MINUTE));
    }
%>
<script>
    function Ok()
    {
        let fileForm = $( "#exportArchiveFileForm" );

        if(fileForm != undefined && fileForm != null && fileForm.length > 0)
            fileForm.submit();
        else
            window.close();
    }
    function Delete()
    {
        $( "#deleteZipArchive" ).submit();
    }
</script>
<%
    if(request.getParameter("deleteFile") != null)
    {
    	String vp = request.getParameter("deleteFile");
    	IwcmFile file = new IwcmFile(Tools.getRealPath(vp));
    	if(file.delete())
    	    out.print("<p><span style=\"color: green;\"><strong>"+prop2.getText("components.file_archiv.export_main_files_delete_ok", vp)+"</strong></span></p>");
    	else
            out.print("<p><span style=\"color: red;\"><strong>"+prop2.getText("components.file_archiv.export_main_files_delete_fail", vp)+"</strong></span></p>");
    }
    else if(request.getParameter("exportBtn") != null)
    {
        out.print("<p style=\"color: green;\"><b>" + prop2.getText("components.file_archiv.export_in_progress") + "</b></p>");

        boolean includeHistoryFiles = Tools.getBooleanValue(request.getParameter("includeHistoryFiles"), false);
        boolean includeAwaitingFiles = Tools.getBooleanValue(request.getParameter("includeAwaitingFiles"), false);

        String exportFilename = "files_to_export_" + getTodayDateString();
        String XML_FILE_ENCODED_FILE_NAME = "file_archiv_export_objects.xml";
        //priecinok kam budeme kopirovat subory na export
        IwcmFile exportFolder = new IwcmFile(Tools.getRealPath(FileArchivatorKit.getArchivPath()) + exportFilename);
        String destDir = exportFolder.getPath();
        String replacePath = FileArchivatorKit.getArchivPath() + exportFilename;
        if (exportFolder.exists())
            FileTools.deleteDirTree(exportFolder);
        exportFolder.mkdir();

        List<FileArchivatorBean> filesToExportList;
        if(includeHistoryFiles == false) {
            filesToExportList = new ArrayList<>(FileArchivatorDB.getMainFileList(true, includeAwaitingFiles));
        } else {
            filesToExportList = new ArrayList<>(FileArchivatorDB.getMainAndHistoryFiles(true, includeAwaitingFiles));
        }

        Logger.println("export_archiv.jsp", " export path: " + exportFolder.getPath());
        //vytvorime xml subor zo zaznamov v DB
        IwcmFile xmlFile = new IwcmFile(
                Tools.getRealPath(replacePath + "/" + FileArchivatorKit.getArchivPath() + XML_FILE_ENCODED_FILE_NAME));
        xmlFile.getParentFile().mkdir();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(xmlFile.getPath())));
        //musime z cesty  v beane odstranit defualntny priecinok archivu, lebo v cielovom umiestneni moze byt iny

        out.print("<h2>" + prop2.getText("components.file_archiv.files_to_export") + "</h2>");
        String mainStr = prop2.getText("components.file_archiv.main_file");
        String mainStrAwaiting = prop2.getText("components.file_archiv.main_file_awaiting_file");
        String waitingStr = prop2.getText("components.file_archiv.awaiting_file");
        String historyStr = prop2.getText("components.file_archiv.history_file");
        String patternStr = " (" + prop2.getText("components.file_archiv.pattern") + ")";
        boolean first = true;
        for (FileArchivatorBean fBean : filesToExportList)
        {

            if(fBean.getReferenceId() == -1) {
                if(fBean.getUploaded() == -1) {
                    //UPLOADED
                    if(Tools.isNotEmpty(fBean.getReferenceToMain())) out.print("<span> <b>" + mainStr + patternStr + "</b> : " + fBean.getVirtualPath() +"</span>");
                    else out.print("<span> <b>" + mainStr + "</b> : " + fBean.getVirtualPath() +"</span>");
                } else {
                    // AWAITING UPLOAD
                    if(Tools.isNotEmpty(fBean.getReferenceToMain())) out.print("<span style='color: #ff4b58 !important;'> <b>" + mainStrAwaiting + patternStr + "</b> : " + fBean.getVirtualPath() +"</span>");
                    else out.print("<span style='color: #ff4b58 !important;'> <b>" + mainStrAwaiting + "</b> : " + fBean.getVirtualPath() +"</span>");
                }
            } else {
                if(fBean.getDateUploadLater() != null && fBean.getDateUploadLater().after(new Date(Tools.getNow())))
                    out.print("<span style='color: #ff4b58 !important;'> - " + waitingStr + " : " + fBean.getVirtualPath() +"</span>");
                else
                    out.print("<span> - " + historyStr + " : " + fBean.getVirtualPath() +"</span>");
            }
            out.print("<br/>");

            //FIX - old version start's without slash "/" , so new version if have slash at start remove it
            if(fBean.getFilePath().startsWith("/"))
                fBean.setFilePath(fBean.getFilePath().substring(1));

            if (fBean.getFilePath().startsWith(FileArchivatorKit.getArchivPath()) &&
                    fBean.getFilePath().length() >= FileArchivatorKit.getArchivPath().length())
                fBean.setFilePath(fBean.getFilePath().substring(FileArchivatorKit.getArchivPath().length()));
        }
        encoder.writeObject(filesToExportList);
        encoder.close();
        IwcmFile src;
        IwcmFile dest;
        //a teraz to musime vratit spat aby bolo mozne subory najst a zapisat do archivu
        for (FileArchivatorBean fBean : filesToExportList)
        {
            fBean.setFilePath(FileArchivatorKit.getArchivPath() + fBean.getFilePath());
        }
        //skopirujeme hlavne subory do priecinku na export
        for (FileArchivatorBean fileArchBean : filesToExportList)
        {
            src = new IwcmFile(Tools.getRealPath(fileArchBean.getFilePath() + fileArchBean.getFileName()));
            dest = new IwcmFile(
                    Tools.getRealPath(FileArchivatorKit.getArchivPath()) + exportFilename + "/" + fileArchBean.getFilePath());
            dest.mkdir();
            dest = new IwcmFile(
                    Tools.getRealPath(FileArchivatorKit.getArchivPath()) + exportFilename + "/" + fileArchBean.getFilePath() +
                            fileArchBean.getFileName());
            if(FileTools.isFile(src.getVirtualPath()))
            	FileTools.copyFile(src, dest);
        }

        String zipId = "file_archiv_export_" + Constants.getInstallName() + "_" + getTodayDateString();
        String zipArchivName = Tools.getRealPath("/files/protected/archiv/" + zipId + ".zip");
        IwcmFile zipArchiv = new IwcmFile(zipArchivName);
        zipArchiv.getParentFile().mkdirs();
        //zazipujeme
        try
        {
            zipDir(zipArchivName, destDir, replacePath + FileArchivatorKit.getArchivPath(), prop2, out);
        } catch (Exception e)
        {
        	out.println("<span style='color: red;'>"+prop2.getText("components.file_archiv.add_file_to_zip_error")+"</span>");
            out.flush();
            sk.iway.iwcm.Logger.error(e);
        	return;
        }
        //zmazeme priecinok do ktoreho sme kopirovali a vyrabali z neho zip
        FileTools.deleteDirTree(exportFolder);
        exportFolder.delete();
        //zmazeme docasny nahravaci priecinok
        IwcmFile tmpDir = new IwcmFile(Tools.getRealPath(FileArchivatorKit.getArchivPath() + "tmp/"));
        Logger.println(null, "export_archiv.jsp deleting " + tmpDir.getVirtualPath());
        FileTools.deleteDirTree(tmpDir);
        out.println("<p>&nbsp;</p><p><span style=\"color: green;\"><strong>"+prop2.getText("components.file_archiv.export_main_files_download")+
                    "</strong></span>&nbsp;<a href=\""+zipArchiv.getVirtualPath()+"\">"+zipArchiv.getVirtualPath()+"</a>");
        out.println("<br/><br/><input class=\"button100\" type=\"button\" value=\""+prop2.getText("components.file_archiv.export_main_files_delete_btn")+"\" onclick=\"Delete();\" /> </p>");
        %>
        <form id="deleteZipArchive" action="<%=PathFilter.getOrigPath(request) %>" method="POST">
            <input type="hidden" name="deleteFile" value="<%=zipArchiv.getVirtualPath()%>"/>
        </form>
        <%
    }
    else
    {
    	%>

        <form id="exportArchiveFileForm" action="<%=PathFilter.getOrigPath(request) %>" method="POST">
            <p><label><input type="checkbox" name="includeHistoryFiles" value="true" /> <iwcm:text key="components.file_archiv.export.add_historyFiles"/></label></p>
            <p><label><input type="checkbox" name="includeAwaitingFiles" value="true" /> <iwcm:text key="components.file_archiv.export.add_awaitingFiles"/></label></p>
            <input type="hidden" name="exportBtn" value="true"/>
        </form>

        <%
    }
%>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
<%
if(request.getParameter("deleteFile") != null)
{
%>
<script>
    $("#btnCancel").hide();
</script>
<%
}
%>