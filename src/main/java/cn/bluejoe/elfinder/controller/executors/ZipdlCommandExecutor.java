package cn.bluejoe.elfinder.controller.executors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipOutputStream;

/**
 * Downloads multiple files as a ZIP archive.
 * https://hypweb.net/elFinder-nightly/demo/2.1/php/connector.minimal.php?cmd=zipdl&download=1&targets%5B%5D=l1_RG93bmxvYWRzL0V4YW1wbGUvbWFpbi5tY2UuanM&targets%5B%5D=6005bd866a15f&targets%5B%5D=Example-2.zip&targets%5B%5D=application%2Fzip&Example-2.zip
 */
public class ZipdlCommandExecutor extends AbstractCommandExecutor {

   static final String ZIPDL_HASH_PREFIX = "zipdl_";
   static final String ZIPDL_SESSION_ATTRIBUTE_PREFIX = ZipdlCommandExecutor.class.getName() + ".temporaryZip.";
   static final String ZIPDL_TEMP_DIRECTORY = "elfinder-zipdl";
   static final String ZIPDL_TEMP_FILE_PREFIX = "zipdl-";
   static final long ZIPDL_TEMP_FILE_MAX_AGE = 60L * 60L * 1000L;

   @Override
   public void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws Exception {
      JSONObject json = new JSONObject();

      // zipdl uses two requests: the first creates a temporary ZIP and the second downloads it.
      if ("1".equals(request.getParameter("download"))) {
         downloadTemporaryZip(request, response);
         return;
      }

      String token = UUID.randomUUID().toString();
      File temporaryZip = createTemporaryZip(fsService, request, json);
      if (temporaryZip == null) {
         response.getWriter().println(json.toString());
         return;
      }

      request.getSession().setAttribute(ZIPDL_SESSION_ATTRIBUTE_PREFIX + token, temporaryZip.getCanonicalPath());
      json.put("zipdl", new JSONObject().put("file", ZIPDL_HASH_PREFIX + token));
      response.getWriter().println(json.toString());
   }

   private void downloadTemporaryZip(HttpServletRequest request, HttpServletResponse response) throws IOException {
      String token = getDownloadToken(request);
      if (token == null) {
         response.sendError(HttpServletResponse.SC_FORBIDDEN);
         return;
      }

      // Consume the server-issued token atomically so a temporary ZIP can be downloaded only once.
      HttpSession session = request.getSession();
      String sessionAttribute = ZIPDL_SESSION_ATTRIBUTE_PREFIX + token;
      String temporaryZipPath;
      synchronized (session) {
         Object sessionValue = session.getAttribute(sessionAttribute);
         temporaryZipPath = sessionValue instanceof String ? (String) sessionValue : null;
         if (temporaryZipPath != null) {
            session.removeAttribute(sessionAttribute);
         }
      }
      if (temporaryZipPath == null) {
         response.sendError(HttpServletResponse.SC_FORBIDDEN);
         return;
      }

      File temporaryZip = getTemporaryZip(temporaryZipPath);
      if (temporaryZip == null) {
         response.sendError(HttpServletResponse.SC_FORBIDDEN);
         return;
      }

      String date = Tools.formatDateTimeSeconds(Tools.getNow());
      date = Tools.replace(date, " ", "-");
      date = Tools.replace(date, ".", "-");
      date = Tools.replace(date, ":", "-");
      date = DocTools.removeChars(date);

      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "attachments; " + FileCommandExecutor.getAttachementFileName("download-" + date + ".zip", request.getHeader("USER-AGENT")));
      response.setHeader("Content-Transfer-Encoding", "binary");

      // The file is disposable and must not remain on the server after a completed or interrupted download.
      try {
         writeTemporaryZipToResponse(temporaryZip, response);
      } finally {
         deleteTemporaryZip(temporaryZip);
      }
   }

   private String getDownloadToken(HttpServletRequest request) {
      String[] targets = request.getParameterValues("targets[]");
      if (targets != null) {
         for (String target : targets) {
            if (target.startsWith(ZIPDL_HASH_PREFIX)) {
               return target.substring(ZIPDL_HASH_PREFIX.length());
            }
         }
      }
      return null;
   }

   private File createTemporaryZip(FsService fsService, HttpServletRequest request, JSONObject json) throws IOException {
      String[] targets = request.getParameterValues("targets[]");
      Prop prop = Prop.getInstance(request);
      if (targets == null || targets.length == 0) {
         json.put("error", prop.getText("components.elfinder.commands.archive.error"));
         return null;
      }

      File temporaryDirectory = getTemporaryDirectory();
      deleteExpiredTemporaryZips(temporaryDirectory);
      File temporaryZip = File.createTempFile(ZIPDL_TEMP_FILE_PREFIX, ".zip", temporaryDirectory).getCanonicalFile();

      // Read directly from FsItemEx without creating anything beside the sources. Keep the file stream
      // separate so it is closed even when ZIP finalization fails, for example for an empty directory.
      try (FileOutputStream fileOutput = new FileOutputStream(temporaryZip);
           ZipOutputStream zipOutput = new ZipOutputStream(fileOutput)) {
         for (String target : targets) {
            FsItemEx item = super.findItem(fsService, target);
            if (item == null) {
               throw new IOException(prop.getText("components.elfinder.commands.archive.error"));
            }
            addToZip(item, item.getName(), zipOutput);
         }
      } catch (ZipException e) {
         deleteTemporaryZip(temporaryZip);
         if (e.getMessage() != null && e.getMessage().contains("ZIP file must have at least one entry")) {
            json.put("error", prop.getText("components.elfinder.commands.archive.error.empty"));
         } else {
            json.put("error", prop.getText("components.elfinder.commands.archive.error.exception", e.getLocalizedMessage()));
         }
         return null;
      } catch (IOException e) {
         deleteTemporaryZip(temporaryZip);
         json.put("error", prop.getText("components.elfinder.commands.archive.error.exception", e.getLocalizedMessage()));
         return null;
      } catch (RuntimeException e) {
         deleteTemporaryZip(temporaryZip);
         throw e;
      }

      return temporaryZip;
   }

   private void addToZip(FsItemEx item, String entryName, ZipOutputStream zipOutput) throws IOException {
      // Preserve the selected folder structure and verify every descendant before reading it.
      if (item.isReadable(item) == false) {
         throw new IOException("File is not readable: " + item.getPath());
      }

      if (item.isFolder()) {
         for (FsItemEx child : item.listChildren()) {
            addToZip(child, entryName + "/" + child.getName(), zipOutput);
         }
         return;
      }

      ZipEntry entry = new ZipEntry(entryName.replace('\\', '/'));
      entry.setTime(item.getLastModified());
      entry.setSize(item.getSize());
      InputStream input = item.openInputStream();
      if (input == null) {
         throw new IOException("Unable to open file: " + item.getPath());
      }
      try (input) {
         zipOutput.putNextEntry(entry);
         IOUtils.copy(input, zipOutput);
      }
   }

   private File getTemporaryDirectory() throws IOException {
      File temporaryDirectory = new File(IwcmFsDB.getTempDir(), ZIPDL_TEMP_DIRECTORY).getCanonicalFile();
      if (temporaryDirectory.exists() == false && temporaryDirectory.mkdirs() == false) {
         throw new IOException("Unable to create dir: " + temporaryDirectory.getAbsolutePath());
      }
      if (temporaryDirectory.isDirectory() == false) {
         throw new IOException("Not a directory: " + temporaryDirectory.getAbsolutePath());
      }
      return temporaryDirectory;
   }

   private File getTemporaryZip(String temporaryZipPath) throws IOException {
      if (Tools.isEmpty(temporaryZipPath)) {
         return null;
      }

      // Keep download and cleanup strictly inside the dedicated directory, even if the session value is invalid.
      File temporaryDirectory = getTemporaryDirectory();
      File temporaryZip = new File(temporaryZipPath).getCanonicalFile();
      String fileName = temporaryZip.getName();
      if (temporaryDirectory.equals(temporaryZip.getParentFile()) == false ||
         fileName.startsWith(ZIPDL_TEMP_FILE_PREFIX) == false || fileName.endsWith(".zip") == false ||
         temporaryZip.isFile() == false) {
         return null;
      }
      return temporaryZip;
   }

   private void writeTemporaryZipToResponse(File temporaryZip, HttpServletResponse response) throws IOException {
      response.setContentLengthLong(temporaryZip.length());
      try (InputStream input = new FileInputStream(temporaryZip); OutputStream output = response.getOutputStream()) {
         IOUtils.copy(input, output);
         output.flush();
      }
   }

   private void deleteExpiredTemporaryZips(File temporaryDirectory) {
      File[] files = temporaryDirectory.listFiles((dir, name) -> name.startsWith(ZIPDL_TEMP_FILE_PREFIX) && name.endsWith(".zip"));
      if (files == null) {
         return;
      }

      // The browser may never send the second download request, so periodically remove abandoned ZIP files.
      long expiration = System.currentTimeMillis() - ZIPDL_TEMP_FILE_MAX_AGE;
      for (File file : files) {
         if (file.lastModified() < expiration && file.delete() == false) {
            sk.iway.iwcm.Logger.debug(ZipdlCommandExecutor.class, "Unable to delete expired temporary ZIP file: " + file.getAbsolutePath());
         }
      }
   }

   private void deleteTemporaryZip(File temporaryZip) {
      if (temporaryZip.exists() && temporaryZip.delete() == false) {
         sk.iway.iwcm.Logger.debug(ZipdlCommandExecutor.class, "Unable to delete temporary ZIP file: " + temporaryZip.getAbsolutePath());
      }
   }

}
