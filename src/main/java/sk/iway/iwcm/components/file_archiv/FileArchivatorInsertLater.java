package sk.iway.iwcm.components.file_archiv;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;

public class FileArchivatorInsertLater
{
    public static void main(String[] args)
    {
        try
        {
            List<FileArchivatorBean> filesToUpload = FileArchivatorDB.getFilesToUpload();

            for(FileArchivatorBean fab : filesToUpload)
            {
                RequestBean currentRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
                if (currentRequestBean == null) {
                    SetCharacterEncodingFilter.registerDataContext(null);
                    currentRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
                }

                int domainId = fab.getDomainId();
                if (domainId > -1) {
                    GroupDetails group = GroupsDB.getInstance().getGroup(domainId);
                    if (group != null) {
                        currentRequestBean.setDomain(group.getDomainName());
                    }
                }

                //test if file exist's maybe it's on another cluster node
                IwcmFile file = new IwcmFile(Tools.getRealPath(fab.getFilePath()+fab.getFileName()));
                if(!file.exists())
                {
                    //skip this file
                    continue;
                }

                //subor, ktory sme sa v minulosti pokusili nahrat neuspesne
                if(fab.getUploaded()==-2)
                    continue;

                if(fab != null)
                {
                    int stav = 0;

                    //ulozime subor na nove miesto
                    String uniqueFileName = renameFile(fab);

                    if(uniqueFileName==null)
                    {
                        stav = 1;
                        fab.setUploaded(-2);
                        fab.save();
                    }
                    else
                    {
                        //vymazeme prazdne priecinky
                        if( !removeEmptyDirs(fab.getFilePath(), "archiv_insert_later") )
                            stav = 4;
                    }

                    sendMail(fab, stav);
                }
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
        SetCharacterEncodingFilter.unRegisterDataContext();
    }

    /**
	 * zamena obsahu suborov dirPath+fileName &lt;-&gt; oldFileBean.getFilePath()+oldFileBean.getFileName()
	 * BHR: musel som prerobit z Tools.renameFile, pretoze sa stalo, ze niekedy nezmazalo zdrojovy subor a teda sa premenovanie nedokoncilo
	 */
	public static String renameFile(FileArchivatorBean scheduledBean) {
        //get old bean
        FileArchivatorBean oldFileBean = FileArchivatorDB.getInstance().getById(scheduledBean.getReferenceId());
        String fileUrl = newPath(scheduledBean.getFilePath());
        String uniqueFileName = FileArchivatorKit.getUniqueFileName(scheduledBean.getFileName(), fileUrl, FileArchivatorKit.getDateStampAsString(scheduledBean.getDateInsert()));

        try {
            //New created file
            IwcmFile realFile = new IwcmFile( Tools.getRealPath(fileUrl + uniqueFileName) );

            if (oldFileBean != null) {
                //There is OLD copy content of OLD file into new file
                IwcmFile oldFile = new IwcmFile(Tools.getRealPath(oldFileBean.getFilePath() + oldFileBean.getFileName()));
                if(oldFile.renameTo(realFile) == false) {
                    //ERR
                    return null;
                }

                //
                FileTools.moveFile(scheduledBean.getFilePath() + scheduledBean.getFileName(), fileUrl + scheduledBean.getFileName());

                //NOW edit and save DB records
                Long originalId = oldFileBean.getId();
                scheduledBean.setFileName(oldFileBean.getFileName());

                oldFileBean.setId(scheduledBean.getId());
                oldFileBean.setFileName(uniqueFileName);
                oldFileBean.setReferenceId(originalId);

                scheduledBean.setId(originalId);
                scheduledBean.setFilePath(fileUrl);
                scheduledBean.setUploaded(-1);
                scheduledBean.setReferenceId(null);

                if(oldFileBean.save() == true && scheduledBean.save() == true) {
                    //All good, do increment order id
		            FileArchivatorKit.incrementOrderId(originalId);

                    //Good
                    return scheduledBean.getFileName();
                }
            } else {
                //ITS main file, there is no old file
                if(FileTools.moveFile(scheduledBean.getFilePath() + scheduledBean.getFileName(), fileUrl + uniqueFileName) == false) {
                    //ERR
                    return null;
                }

                scheduledBean.setFileName(uniqueFileName);
                scheduledBean.setFilePath(fileUrl);
                scheduledBean.setUploaded(-1);
                scheduledBean.setReferenceId(null);

                if(scheduledBean.save() == false) {
                    Logger.error(FileArchivatorInsertLater.class, "save failed");
                    return null;
                }

                return uniqueFileName;
            }
        } catch (Exception e) {
            return null;
        }

        return null;
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

    private static String newPath(String oldPath)
    {
        return oldPath.replace(FileArchivatorKit.getFullInsertLaterPath(), "");
    }

    private static void sendMail(FileArchivatorBean fileArchivatorBean, int stav)
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
            text.append(prop.getText("components.file_archiv.virtualFileName")).append(": ")
                     .append(fab.getVirtualFileName()).append("<br/>");
        if(Tools.isNotEmpty(dir))
                     text.append(prop.getText("components.file_archiv.directory")).append(": ").append(dir).append("<br/>");
        if(Tools.isNotEmpty(fab.getFileName()))
            text.append(prop.getText("components.gallery.fileName")).append(": ")
                     .append(fab.getFileName()).append("<br/>");
        if(fab.getValidFrom()!=null)
            text.append(prop.getText("inquiry.valid_since")).append(": ")
                     .append(Tools.formatDateTimeSeconds(fab.getValidFrom())).append("<br/>");
        if(fab.getValidTo()!=null)
            text.append(prop.getText("inquiry.valid_till")).append(": ")
                     .append(Tools.formatDateTimeSeconds(fab.getValidTo())).append("<br/>");
        if(Tools.isNotEmpty(fab.getProduct()))
            text.append(prop.getText("components.file_archiv.product")).append(": ")
                     .append(fab.getProduct()).append("<br/>");
        if(Tools.isNotEmpty(fab.getCategory()))
            text.append(prop.getText("components.bazar.category")).append(": ")
                     .append(fab.getCategory()).append("<br/>");
        if(Tools.isNotEmpty(fab.getProductCode()))
            text.append(prop.getText("components.file_archiv.kod_produktu")).append(": ")
                     .append(fab.getProductCode()).append("<br/>");
        if( Tools.isTrue(fab.getShowFile()) )
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
     * @param startDir
     * @param stopDir
     * @return
     */
    public static boolean removeEmptyDirs(String startDir, String stopDir)
    {
        boolean result = true;

        if( Tools.isEmpty(startDir) || Tools.isEmpty(stopDir) )
            return result;

        IwcmFile startDirFile = new IwcmFile(Tools.getRealPath(startDir));
        IwcmFile stopDirFile = new IwcmFile(Tools.getRealPath(stopDir));

        if(!startDirFile.isDirectory())
            return result;

        int numberOfFiles = startDirFile.listFiles().length;

        if(numberOfFiles!=0)
            return result;

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
