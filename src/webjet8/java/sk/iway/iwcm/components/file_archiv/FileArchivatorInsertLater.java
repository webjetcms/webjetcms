package sk.iway.iwcm.components.file_archiv;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;

public class FileArchivatorInsertLater
{
    public static void main(String[] args)
    {
        try
        {
            List<FileArchivatorBean> filesToUpload = FileArchivatorDB.getFilesToUpload();

            for(FileArchivatorBean fab : filesToUpload)
            {
                //subor, ktory sme sa v minulosti pokusili nahrat neuspesne
                if(fab.getUploaded()==-2)
                    continue;
                int stav = 0;
                //ulozime subor na nove miesto
                String uniqueFileName = saveFile(fab);
                int oldId = fab.getId();
                String oldFileDir = fab.getFilePath();
                if(uniqueFileName==null)
                {
                    stav = 1;
                    fab.setUploaded(-2);
                    fab.save();
                }
                else
                {
                    //ulozime novy zaznam o subore do DB
                    if(!saveBean(fab, uniqueFileName) )
                    {
                        stav = 2;
                        fab.setUploaded(-2);
                        fab.save();
                    }
                    else
                    {
                        //zmazeme stary subor a zaznam o nom z DB
                        if(!removeOld(oldId))
                        {
                            stav = 3;
                        }
                        else
                        {
                            //vymazeme prazdne priecinky
                            if(!removeEmptyDirs(oldFileDir, fab))
                                stav = 4;
                        }
                    }
                }
                sendMail(fab, stav);
            }
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            Adminlog.add(Adminlog.TYPE_CRON, "FileArchivatorInsertLater error:"+e.getMessage()+"\n"+sw.toString(), -1, -1);
            sk.iway.iwcm.Logger.error(e);
        }
        finally
        {
            Cache.getInstance().removeObjectStartsWithName(FileArchivatorDB.getCachePrefix()+"getWaitingFileList");
        }
    }



    /**
     * vrati domenu na zaklade domainId
     */
    private static String getDomainByFab(FileArchivatorBean fab)
    {
        String domainName = CloudToolsForCore.getDomainName();
        if(Constants.getBoolean("multiDomainEnabled"))
        {
            GroupDetails root = GroupsDB.getInstance().getGroup(fab.getDomainId());
            if (root != null) {
                return root.getDomainName();
            }
        }
        return domainName;
    }

    /*private static String getDomainFolderName(String domain)
    {
        if (Constants.getBoolean("multiDomainEnabled")==false)
        {
            return "defaulthost";
        }

        domain = domain.toLowerCase();

        if (domain.startsWith("www.")) domain = domain.substring(4);

        if (domain.length() > 3 && "cloud".equals(Constants.getInstallName()))
        {
            domain = domain.charAt(0)+File.separator+domain.charAt(1)+File.separator+domain.charAt(2)+File.separator+domain;
        }

        return domain;
    }*/

    /**
     * ak je , aj ked ju nemam aktualne aktivnu.
     */
    public static String getRealPath(String virtualPath, String domain)
    {
        if(virtualPath.startsWith("/") == false) virtualPath = "/"+virtualPath;
        String result = Tools.getRealPath(virtualPath);
        //aktivna domena nemusi sediet s tym kam ukladam
        if (FilePathTools.isExternalDir(virtualPath))
        {
            String domainBaseFolder = FilePathTools.getDomainBaseFolder(domain);
            IwcmFile file = new IwcmFile(domainBaseFolder, virtualPath);
            Logger.debug(FileArchivatorInsertLater.class, "getRealPath, return="+file.getAbsolutePath()+" virtualPath="+virtualPath);

            String path = file.getAbsolutePath();
            if (virtualPath.endsWith("/") && path.endsWith(File.separator)==false) path += File.separator;
            result = path;
        }

        return result;
    }

    /**
     * ulozi subor na novu poziciu, vrati jeho novy nazov
     *
     * @param fab FileArchivatorBean
     * @return String
     */
    private static String saveFile(FileArchivatorBean fab)
    {
        IwcmFile fOld = new IwcmFile(getRealPath(fab.getFilePath()+fab.getFileName(), getDomainByFab(fab)));

        String newPath = newPath(fab.getFilePath());
        String dateStamp = FileArchivatorKit.getDateStampAsString(fab.getDateInsert());
        String uniqueFileName = FileArchivatorKit.getUniqueFileName(fab.getFileName(), newPath, dateStamp);

        String realPath = getRealPath(newPath+uniqueFileName, getDomainByFab(fab));
        IwcmFile newFile = new IwcmFile(realPath);
        try
        {
            IwcmFsDB.writeFiletoDest(new IwcmInputStream(fOld),new File(newFile.getAbsolutePath()),Tools.safeLongToInt(fOld.getLength()));
        }
        catch(IOException ioex)
        {
            //indikuje chybu pri ukladani
            uniqueFileName = null;

            Logger.debug(SaveFileAction.class, "Nastala chyba '"+ioex.getMessage()+"' pri ukladani suboru "+newFile.getPath());
            sk.iway.iwcm.Logger.error(ioex);
        }
        return uniqueFileName;
    }

    /**
     *  custom verzia z FileArchivatorKit kvoli getRealPath
     */
    public static boolean setFilePropertiesAfterUpload(String dirPath, String fileName, int oldId, FileArchivatorBean newFab)
    {
        newFab.setId(0);
        newFab.setFilePath(dirPath);
        newFab.setFileName(fileName);
        newFab.setUploaded(-1);

        String realPath = getRealPath(dirPath+fileName, getDomainByFab(newFab));
        IwcmFile iwcmFile = new IwcmFile(realPath);
        if (!iwcmFile.exists() || !iwcmFile.isFile() || !iwcmFile.canRead()) return false;
        newFab.setMd5(FileArchivatorKit.getMD5(iwcmFile));
        newFab.setFileSize(iwcmFile.length());

        return FileArchivatorKit.setFilePropertiesAfterUpload(newFab, oldId, true, null);
    }

    /**
     * ulozi zaznam o subore do DB
     *
     * @param fab FileArchivatorBean
     * @param uniqueFileName String
     * @return true ak ulozene v poriadku
     */
    private static boolean saveBean(FileArchivatorBean fab, String uniqueFileName)
    {
        String dirPath = newPath(fab.getFilePath());
        int oldId = fab.getReferenceId();

        // zistime, ci referencovany subor uz nebol medzicasom prepisany. Ak ano, nahradime uz ten novsi
        if(oldId>0)
        {
            FileArchivatorBean referenceFab = FileArchivatorDB.getInstance().getById(oldId);
            if( referenceFab!=null && referenceFab.getReferenceId()>0 )
                oldId = referenceFab.getReferenceId();
        }

        if(!setFilePropertiesAfterUpload(dirPath, uniqueFileName, oldId, fab))
        {
            Logger.error(SaveFileAction.class, "AfterUploadError !!! Property: "+ uniqueFileName +","+oldId+","+fab.getUserId()+","+fab.getVirtualFileName()+","+fab.getValidFrom()+","+fab.getValidTo());
            return false;
        }
        return true;
    }

    /**
     * vymaze stary subor a zaznam o nom z DB
     *
     * @param fabId int
     * @return true ak odstrani v poriadku
     */
    private static boolean removeOld(int fabId)
    {
        boolean isSuccess = false;
        FileArchivatorBean fabToDelete = FileArchivatorDB.getInstance().getById(fabId);
        if (fabToDelete != null)
        {
            IwcmFile iFile = new IwcmFile(getRealPath(fabToDelete.getFilePath() + fabToDelete.getFileName(), getDomainByFab(fabToDelete)));
            if (iFile.exists() && iFile.delete())
                isSuccess = true;
            else
                Logger.debug(FileArchivatorDB.class, "Subor :" + fabToDelete.getFilePath() + fabToDelete.getFileName() + " sa nepodarilo zmazat.");
        }
        if (fabToDelete == null) return false;
        return isSuccess && fabToDelete.delete() ;
    }

    private static String newPath(String oldPath)
    {
        return oldPath.replace(FileArchivatorKit.getFullInsertLaterPath(), "");
    }

    private static void sendMail(FileArchivatorBean fileArchivatorBean, int stav)//Constants.getString("fileArchivFromMail");
    {
        Prop prop = Prop.getInstance();

        StringBuilder text = new StringBuilder();
        text.append("<html><head>");
        text.append("<style>");
        text.append("body{");
        text.append("font-family: Arial;");
        text.append("font-size: 11pt;");
        text.append('}');
        text.append("</style></head><body>");
        text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.nasledujici_soubor_bol_uspesne_nacitany")).append(":<br/><br/>");
        if(stav!=0)
            text = new StringBuilder(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.pri_nahravani_suboru_nastala_nasledujuca_chyba")).append(": ");
        if(stav==1)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.soubor_sa_nepodarilo_ulozit_na_disk")).append("<br/><br/>");
        if(stav==2)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.zaznam_o_soubore_sa_nepodarilo_ulozit_do_databazy")).append("<br/><br/>");
        if(stav==3)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.nepodarilo_sa_vymazat_docasny_soubor_alebo_zaznam_o_nom_z_databazy")).append("<br/><br/>");
        if(stav==4)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.nepodarilo_se_vymazat_prazdne_adresare")).append("<br/><br/>");

        FileArchivatorBean fab = fileArchivatorBean;
        String emails = fab.getEmails();
        //Ak tento subor nie je novy, a je aktualizaciou starsieho suboru, potrebujeme vypisat v jeho vlastnosti.
        if(fab.getReferenceId() != -1)
        {
            FileArchivatorBean fabByReference = FileArchivatorDB.getInstance().getById(fab.getReferenceId());
            // referencia na neho sameho s novym ID ale v archive
            if(fabByReference != null)
            {
                fab = fabByReference;
                //referencia na hlavny subor
                fabByReference = FileArchivatorDB.getInstance().getById(fab.getReferenceId());
                if(fabByReference != null)
                    fab = fabByReference;
            }
        }

        String subject = prop.getText("components.file_archiv.FileArchivatorInsertLater.java.soubor_bol_uspesne_nahrany") +" "+ fab.getVirtualFileName();
        if(stav!=0)
            subject = prop.getText("components.file_archiv.FileArchivatorInsertLater.java.pozor_nastala_chyba_pri_ukladani_souboru_") +" "+ fab.getVirtualFileName();

        String dir = "";
        if(Tools.isNotEmpty(newPath(fab.getFilePath())))
            dir = newPath(fab.getFilePath());

        if(Tools.isNotEmpty(fab.getVirtualFileName()))
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.virtualne_meno")).append(": ")
                     .append(fab.getVirtualFileName()).append("<br/>");
        if(Tools.isNotEmpty(fab.getFileName()))
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.realne_meno")).append(": ")
                     .append(fab.getFileName()).append("<br/>");
        if(Tools.isNotEmpty(dir))
            text.append("Adresář: ").append(dir).append("<br/>");
        if(fab.getValidFrom()!=null)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.datum_od")).append(": ")
                     .append(Tools.formatDate(fab.getValidFrom())).append("<br/>");
        if(fab.getValidTo()!=null)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.datum_od_1")).append(": ")
                     .append(Tools.formatDate(fab.getValidTo())).append("<br/>");
        if(Tools.isNotEmpty(fab.getProduct()))
            text.append(prop.getText("components.file_archiv.product")).append(": ")
                     .append(fab.getProduct()).append("<br/>");
        if(Tools.isNotEmpty(fab.getCategory()))
            text.append(prop.getText("components.bazar.category")).append(": ")
                     .append(fab.getCategory()).append("<br/>");
        if(Tools.isNotEmpty(fab.getProductCode()))
            text.append(prop.getText("components.file_archiv.code")).append(": ")
                     .append(fab.getProductCode()).append("<br/>");
        if(fab.getShowFile())
            text.append(prop.getText("editor.show")).append(":").append(prop.getText("qa.publishOnWeb.yes")).append("<br/>");
		else
            text.append(prop.getText("editor.show")).append(":").append(prop.getText("qa.publishOnWeb.no")).append("<br />");


        text.append(prop.getText("components.banner.priority")).append(": ")
                 .append(fab.getPriority()).append("<br/>");
        if(Tools.isNotEmpty(fab.getReferenceToMain()))
            text.append(prop.getText("components.file_archiv.pattern")).append(": ").append(fab.getReferenceToMain()).append("<br/>");
        text.append(prop.getText("components.file_archiv.reference")).append(": ").append(fab.getReferenceId()).append("<br/>");

        String baseHref = "http://"+getDomainByFab(fab);

        text.append("<br/>").append(prop.getText("components.file_archiv.link_on_file")).append(": <a href=\"").append(baseHref).append("/")
                 .append(dir).append(fab.getFileName()).append("\">").append(baseHref).append("/").append(dir).append(fab.getFileName())
                 .append("</a>");

        text.append("</body></html>");

        String fromName = prop.getText("components.file_archiv.title");
        String fromEmail = "no-reply@"+getDomainByFab(fab).replace("www.", "");

        if(Tools.isNotEmpty(Constants.getString("fileArchivFromMail")) && Tools.isEmail(Constants.getString("fileArchivFromMail")))
            fromEmail = Constants.getString("fileArchivFromMail");

        if(Tools.isEmpty(emails))
            emails = Constants.getString("fileArchivSupportEmails");

        String[] emailsArray = Tools.getTokens(emails, ",", true);
        for (String recipient : emailsArray)
            SendMail.send(fromName, fromEmail, recipient,  subject, text.toString());
    }

    /**
     * vymaze vsetky prazdne priecinky od startDir po stopDir vratane
     *
     * @param startDir String
     * @param fab FileArchivatorBean - len kvoli domene
     * @return true ak uspesne odstrani
     */
    public static boolean removeEmptyDirs(String startDir, FileArchivatorBean fab)
    {
        boolean result;

        String stopDir = "archiv_insert_later";
        if( Tools.isEmpty(startDir))
            return true;

        IwcmFile startDirFile = new IwcmFile(getRealPath(startDir, getDomainByFab(fab)));
        IwcmFile stopDirFile = new IwcmFile(getRealPath(stopDir, getDomainByFab(fab)));

        if(!startDirFile.isDirectory())
            return true;

        int numberOfFiles = startDirFile.listFiles().length;

        if(numberOfFiles!=0)
            return true;

        IwcmFile helpFile = startDirFile;
        IwcmFile deleteFile = startDirFile;

        //kym je adresar prazdny, alebo iba s jednym suborom - inym prazdnym Dirom
        //a sucasne jeho nazov nie je stopDir
        while( numberOfFiles<=1 && !helpFile.getName().equals(stopDirFile.getName()) )
        {
            deleteFile = helpFile;
            helpFile = helpFile.getParentFile();
            numberOfFiles = helpFile.listFiles().length;
        }

        //rekurzivne vymaze vsetky subory nadol, vratane zadaneho adresara
        result = FileTools.deleteDirTree(deleteFile);
        Logger.debug(FileArchivatorInsertLater.class, "Vymazanie prazdnych adresarov, zaciatok(vratane): "+deleteFile.getAbsolutePath());

        return result;
    }
}
