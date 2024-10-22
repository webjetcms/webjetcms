package sk.iway.iwcm.system.elfinder;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.ResponseUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UsersDB;

@Component
public class ElfinderListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='files' && event.source.subpage=='index'")
    protected void setIndexInitialData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();
        HttpServletRequest request = event.getSource().getRequest();

        //
        if (Constants.getBoolean("enableStaticFilesExternalDir")) {
            model.addAttribute("rememberLastDir", false);
        } else {
            model.addAttribute("rememberLastDir", true);
        }

        //
        model.addAttribute("lng",  Prop.getLng(request, false));

        //Check permission
        Identity user = UsersDB.getCurrentUser(request);
        model.addAttribute("haveFileIndexerPerm", user.isEnabledItem("fileIndexer"));

        //
        model.addAttribute("elfinderMetadataEnabled", Constants.getBoolean("elfinderMetadataEnabled"));
        model.addAttribute("elfinderMetadataAutopopup", Constants.getBoolean("elfinderMetadataAutopopup"));
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='files' && event.source.subpage=='dialog'")
    protected void setDialogInitialData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();
        HttpServletRequest request = event.getSource().getRequest();

        //pre tento pripad nepamatajme, robilo haluze pri zmenach domeny a pristupoch do /images a /files foldrov
        if (Constants.getBoolean("enableStaticFilesExternalDir")) {
            model.addAttribute("rememberLastDir", false);
        } else {
            model.addAttribute("rememberLastDir", true);
        }

        model.addAttribute("form", Tools.getStringValue(Tools.getRequestParameter(request, "form"), ""));
        model.addAttribute("elementName", Tools.getStringValue(Tools.getRequestParameter(request, "elementName"), ""));
        model.addAttribute("callback", Tools.getStringValue(Tools.getRequestParameter(request, "callback"), ""));
        model.addAttribute("volumes", Tools.getStringValue(Tools.getRequestParameter(request, "volumes"), "link"));

        // file, directory, fileDirectory, files, directories, filesDirectories
        String selectMode = Tools.getStringValue(Tools.getRequestParameter(request, "selectMode"), "file");
        model.addAttribute("selectMode", selectMode);

        //Check permission
        Identity user = UsersDB.getCurrentUser(request);
        model.addAttribute("haveFileIndexerPerm", user.isEnabledItem("fileIndexer"));

        //
        String actualFile = "";
        if (Tools.isNotEmpty(Tools.getRequestParameter(request, "link"))) actualFile = Tools.getRequestParameter(request, "link");
        if ("directory".equals(selectMode) && Tools.isNotEmpty(actualFile) && actualFile.endsWith("/")==false) actualFile = actualFile + "/";

        //Vytvori defaultne adresare pre file system (/images,/files,/images/gallery,/images/video)
        FileTools.createDefaultStaticContentFolders();

        //File value
        model.addAttribute("actualFile", ResponseUtils.filter(actualFile));

        model.addAttribute("lng", Prop.getLng(request, false));
        model.addAttribute("insertJQuery", Tools.insertJQuery(request));
        model.addAttribute("encoding", SetCharacterEncodingFilter.getEncoding());
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("elfinderMetadataEnabled", Constants.getBoolean("elfinderMetadataEnabled"));
        model.addAttribute("elfinderFileArchiveEnabled", Constants.getBoolean("elfinderFileArchiveEnabled"));
        model.addAttribute("pixabayEnabled", Constants.getBoolean("pixabayEnabled"));
        model.addAttribute("archivPath", FileArchivatorKit.getArchivPath());
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='files' && event.source.subpage=='wj_image'")
    protected void setWjImageInitialData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();
        HttpServletRequest request = event.getSource().getRequest();

        //
        model.addAttribute("lng",  Prop.getLng(request, false));
        model.addAttribute("uploadIcon",  "wjIconBig-uploadImage");
        model.addAttribute("editorImageDialogCopyAltToTitle", Constants.getBoolean("editorImageDialogCopyAltToTitle"));
        model.addAttribute("elfinderMetadataEnabled", Constants.getBoolean("elfinderMetadataEnabled"));
        model.addAttribute("editorAdvancedImageAlignment", Constants.getBoolean("editorAdvancedImageAlignment"));
        model.addAttribute("setfield", Tools.getRequestParameter(request, "setfield"));

        //
        boolean denyImageSize = false;
        Identity user = UsersDB.getCurrentUser(request);
        if (user != null) {
            if (user.isDisabledItem("editorMiniEdit")==false) denyImageSize = Constants.getBoolean("FCKConfig.DenyImageSize[Basic]");
            else denyImageSize = Constants.getBoolean("FCKConfig.DenyImageSize[Default]");

            //Check permission
            model.addAttribute("haveFileIndexerPerm", user.isEnabledItem("fileIndexer"));
        }
        model.addAttribute("denyImageSize", denyImageSize);
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='files' && event.source.subpage=='wj_link'")
    protected void setWjLinkInitialData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();
        HttpServletRequest request = event.getSource().getRequest();

        //
        model.addAttribute("lng",  Prop.getLng(request, false));
        model.addAttribute("uploadIcon",  "wjIconBig-uploadImage");
        model.addAttribute("elfinderMetadataEnabled", Constants.getBoolean("elfinderMetadataEnabled"));
        model.addAttribute("elfinderMetadataAutopopup", Constants.getBoolean("elfinderMetadataAutopopup"));

        //
        model.addAttribute("currentTimeMillis", new Date().getTime());

        //Check permission
        Identity user = UsersDB.getCurrentUser(request);
        model.addAttribute("haveFileIndexerPerm", user.isEnabledItem("fileIndexer"));

        //
        boolean denyImageSize = false;
        if (user != null) {
            if (user.isDisabledItem("editorMiniEdit")==false) denyImageSize = Constants.getBoolean("FCKConfig.DenyImageSize[Basic]");
            else denyImageSize = Constants.getBoolean("FCKConfig.DenyImageSize[Default]");
        }
        model.addAttribute("denyImageSize", denyImageSize);
    }
}