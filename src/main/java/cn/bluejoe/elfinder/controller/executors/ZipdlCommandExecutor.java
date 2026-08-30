package cn.bluejoe.elfinder.controller.executors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

   static final String ZIPDL_HASH_PREFIX = "zipdl_";
   static final String ZIPDL_SESSION_ATTRIBUTE_PREFIX = ZipdlCommandExecutor.class.getName() + ".temporaryZip.";

   @Override
   public void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws Exception {
      JSONObject json = new JSONObject();

      boolean download = "1".equals(request.getParameter("download"));

      if (download) {
         String[] targets = request.getParameterValues("targets[]");
         String zipDlHash = null;
         if (targets != null) {
            for (String target : targets) {
               if (target.startsWith(ZIPDL_HASH_PREFIX)) {
                  zipDlHash = target.substring(ZIPDL_HASH_PREFIX.length());
               }
            }
         }

         HttpSession session = request.getSession();
         String sessionAttribute = ZIPDL_SESSION_ATTRIBUTE_PREFIX + zipDlHash;
         if (zipDlHash == null || Boolean.TRUE.equals(session.getAttribute(sessionAttribute)) == false) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
         }

         FsItemEx zipFilePath = super.findItem(fsService, zipDlHash);
         if (zipFilePath == null || zipFilePath.isWritable(zipFilePath) == false || zipFilePath.isLocked(zipFilePath)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
         }

         synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute(sessionAttribute)) == false) {
               response.sendError(HttpServletResponse.SC_FORBIDDEN);
               return;
            }
            session.removeAttribute(sessionAttribute);
         }

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

         if (zipFilePath == null) {
            response.getWriter().println(json.toString());
            return;
         }

         String zipFileHash = zipFilePath.getHash();
         request.getSession().setAttribute(ZIPDL_SESSION_ATTRIBUTE_PREFIX + zipFileHash, Boolean.TRUE);
         response.getWriter().println("{\"zipdl\":{\"file\":\""+ZIPDL_HASH_PREFIX+zipFileHash+"\"}}");
      }
   }

}
