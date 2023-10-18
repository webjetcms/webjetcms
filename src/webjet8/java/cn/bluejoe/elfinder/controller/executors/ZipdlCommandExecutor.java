package cn.bluejoe.elfinder.controller.executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;

/**
 * Executor na stiahnutie viacerych suborov naraz
 * https://hypweb.net/elFinder-nightly/demo/2.1/php/connector.minimal.php?cmd=zipdl&download=1&targets%5B%5D=l1_RG93bmxvYWRzL0V4YW1wbGUvbWFpbi5tY2UuanM&targets%5B%5D=6005bd866a15f&targets%5B%5D=Example-2.zip&targets%5B%5D=application%2Fzip&Example-2.zip
 */
public class ZipdlCommandExecutor extends AbstractCommandExecutor {

   @Override
   public void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws Exception {
      JSONObject json = new JSONObject();

      boolean download = "1".equals(request.getParameter("download"));
      String zipdlHashPrefix = "zipdl_";

      if (download) {
         String[] targets = request.getParameterValues("targets[]");
         String zipDlHash = null;
         for (String target : targets) {
            if (target.startsWith(zipdlHashPrefix)) {
               zipDlHash = target.substring(zipdlHashPrefix.length());
            }
         }
         FsItemEx zipFilePath = super.findItem(fsService, zipDlHash);

         String date = Tools.formatDateTimeSeconds(Tools.getNow());
         date = Tools.replace(date, " ", "-");
         date = Tools.replace(date, ".", "-");
         date = Tools.replace(date, ":", "-");
         date = DocTools.removeChars(date);

         String fileName = "download-"+date+".zip";
         String mime = "application/zip";

         response.setContentType(mime);
         response.setHeader("Content-Disposition",	"attachments; " + FileCommandExecutor.getAttachementFileName(fileName, request.getHeader("USER-AGENT")));
         //response.setHeader("Content-Location", fileUrlRelative);
         response.setHeader("Content-Transfer-Encoding", "binary");

         FileCommandExecutor.writeFsItemExToResponse(zipFilePath, response);

         //zmaz temp zip
         zipFilePath.delete();

      } else {
         ArchiveCommandExecutor archive = new ArchiveCommandExecutor();
         FsItemEx zipFilePath = archive.executeZip(fsService, request, servletContext, json);

         response.getWriter().println("{\"zipdl\":{\"file\":\""+zipdlHashPrefix+zipFilePath.getHash()+"\"}}");
      }
   }

}
