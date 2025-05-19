package sk.iway.iwcm.admin.upload;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.system.stripes.MultipartWrapper;
import sk.iway.iwcm.users.UsersDB;

@WebServlet("/admin/upload/chunk")
@MultipartConfig
public class AdminUploadServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Map<String,PathHolder> temporary = new ConcurrentHashMap<>();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
        String name = request.getParameter("name");
        String destinationFolder = request.getParameter("destinationFolder");
        if (Tools.isNotEmpty(destinationFolder) && destinationFolder.endsWith("/")==false) destinationFolder = destinationFolder + "/";
        boolean writeDirectlyToDestination = "true".equals(request.getParameter("writeDirectlyToDestination"));
        String overwriteMode = request.getParameter("overwriteMode");
        boolean isBase64 = "base64".equals(request.getParameter("encoding"));

        JSONObject output = new JSONObject();

		if (name == null) {
			//dropzone.js kompatibilita
			Part filePart = request.getPart("file");
			name = filePart.getSubmittedFileName();
        }

        if (destinationFolder!=null && (destinationFolder.startsWith("/files") || destinationFolder.startsWith("/images") || destinationFolder.startsWith("/shared"))) {
            name = DB.internationalToEnglish(name);
            name = DocTools.removeCharsDir(name, true).toLowerCase();
        }

		Logger.debug(AdminUploadServlet.class, "doPost, name="+name);

        Identity user = UsersDB.getCurrentUser(request);

        long fileSize = Tools.getLongValue(request.getHeader("Content-Length"), Long.MAX_VALUE);
        if (request.getParameter("dztotalfilesize")!=null) fileSize = Tools.getLongValue(request.getParameter("dztotalfilesize"), fileSize);

        String uploadType = Tools.getStringValue(request.getParameter("uploadType"), ""); //typ uploadu (image/file)
        String extension = FileTools.getFileExtension(name);

        String errorKey = null;
        if (user == null || user.isAdmin()==false) {
            errorKey = "admin.logon.timeoutTitle";
        }
        else if (Tools.isNotEmpty(destinationFolder) && "/files/protected/feedback-form/".equals(destinationFolder)==false && user.isFolderWritable(destinationFolder)==false) {
            // /files/protected/feedback-form/ je natvrdo povolene, aby bolo mozne nahrat subory k feedback-form
            errorKey = "admin.upload_iframe.wrong_upload_dir";
        }
        else if (UploadFileTools.isFileAllowed(uploadType, name, fileSize, user, request)==false) {
            errorKey = "components.forum.new.upload_not_allowed_filetype";
        }

        if (destinationFolder == null) {
            errorKey = "admin.upload_iframe.wrong_upload_dir";
        } else if (destinationFolder.startsWith("/images") || destinationFolder.startsWith("/files") || destinationFolder.startsWith("/shared")) {
            //pre bezpecnost povolujeme len tieto priecinky na upload, kedze ten sa definuje cez parameter destinationFolder
        } else {
            errorKey = "admin.upload_iframe.wrong_upload_dir";
        }

		if (errorKey != null) {
			try {
				Prop prop = Prop.getInstance();
				output.put("error", prop.getText(errorKey));
				output.put("file", name);
				output.put("success", false);
			}
			catch (JSONException e) {
                Logger.error(AdminUploadServlet.class, e);
			}
		}
        else {
            int chunk = Tools.getIntValue(request.getParameter("chunk"), 0);
            int chunks = Tools.getIntValue(request.getParameter("chunks"), 0);

            //dropzone.js kompatibilita
            if (request.getParameter("dzchunkindex")!=null) chunk = Tools.getIntValue(request.getParameter("dzchunkindex"), 0);
            if (request.getParameter("dztotalchunkcount")!=null) chunks = Tools.getIntValue(request.getParameter("dztotalchunkcount"), 0);

            Logger.debug(AdminUploadServlet.class, "doPost, chunk="+chunk+" chunks="+chunks);

            Part filePart = request.getPart("file");

            HttpSession session = request.getSession();
            PartialUploadHolder holder = (PartialUploadHolder)session.getAttribute("partialUploadFile-"+name);
            if (holder==null || chunk == 0)
            {
                holder = new PartialUploadHolder(chunks, name);
                session.setAttribute("partialUploadFile-"+name, holder);
            }
            boolean isLast = false;
            if (holder.getPartPaths().size()+1 == holder.getChunks() || holder.getChunks()==0)
            {
                //je to posledny alebo jediny chunk
                isLast = true;
            }

            //zapisem data do docasneho suboru
            File tempFolder = new File(Tools.getRealPath("/WEB-INF/tmp/"));
            if (tempFolder.exists()==false) tempFolder.mkdirs();
            File tempUploadFile = File.createTempFile(name, "."+extension, tempFolder);
            Logger.debug(AdminUploadServlet.class, "Storing uploaded file, tempFile=" + tempUploadFile.getAbsolutePath());

            FileOutputStream tempFos = new FileOutputStream(tempUploadFile);
            InputStream tempIs = filePart.getInputStream();
            int read=0;
            byte[] bytes = new byte[1024];
            while ((read = tempIs.read(bytes)) != -1)
            {
                tempFos.write(bytes, 0, read);
            }
            tempIs.close();
            tempFos.close();

            String filePartName = tempUploadFile.getAbsolutePath();//filePart.get//getName();
            holder.getPartPaths().add(chunk, filePartName);
            try {
                output.put("chunk-uploaded", chunk);
                output.put("size", new File(filePartName).length());
                output.put("success", true);
            } catch (JSONException e) {
                //
            }

            if (isLast)
            {
                session.removeAttribute("partialUploadFile-"+name);
                // mam posledny, spojim ich do jedneho

                IwcmOutputStream fos = null;
                FileInputStream fis;
                byte[] fileBytes;
                String random = RandomStringUtils.random(15, true, true);
                boolean destinationFileExists = false;
                try
                {
                    IwcmFile outputFile = null;
                    String destinationVirtualPath = "";

                    if (writeDirectlyToDestination && Tools.isNotEmpty(destinationFolder) && FileBrowserTools.hasForbiddenSymbol(destinationFolder)==false)
                    {
                        IwcmFile dirFile = new IwcmFile(Tools.getRealPath(destinationFolder));
                        if (dirFile.exists()==false) dirFile.mkdirs();

                        //over, ci subor v destinacii existuje
                        destinationVirtualPath = destinationFolder + name;
                        outputFile = new IwcmFile(Tools.getRealPath(destinationVirtualPath));
                        if (outputFile.exists()) {

                            if ("keepboth".equals(overwriteMode)) {
                                //ziskaj meno noveho suboru
                                name = UploadService.getKeppBothFileName(destinationFolder, name);

                                if (name != null) {
                                    destinationVirtualPath = destinationFolder + name;
                                    outputFile = new IwcmFile(Tools.getRealPath(destinationVirtualPath));
                                } else {
                                    destinationFileExists = true;
                                }

                            }
                            else {
                                destinationFileExists = true;
                            }
                        }

                        //ak subor neexistuje mozeme ho rovno zapisat
                        if (destinationFileExists==false || "overwrite".equals(overwriteMode)) {
                            fos = new IwcmOutputStream(outputFile);
                            destinationFileExists = false;
                        } else {
                            destinationVirtualPath = "";
                        }
                    }

                    if (fos == null) {
                        outputFile = new IwcmFile(File.createTempFile(random, "."+extension, tempFolder));
                        fos = new IwcmOutputStream(outputFile);
                    }
                    for (String chunkPath : holder.getPartPaths())
                    {
                        File inputFile = new File(chunkPath);
                        fis = new FileInputStream(inputFile);
                        fileBytes = new byte[(int) inputFile.length()];
                        fis.read(fileBytes, 0,(int)  inputFile.length());
                        fos.write(fileBytes);
                        fos.flush();
                        fis.close();
                        inputFile.delete();
                    }
                    fos.close();

                    if (isBase64) {
                        decodeBase64File(outputFile);
                    }

                    //output = "{\"name\":\""+name+"\",\"key\":\""+random+"\"}";
                    try
                    {
                        if (Tools.isNotEmpty(destinationVirtualPath))
                        {
                            UploadService uploadService = new UploadService(destinationVirtualPath, request);
                            uploadService.process();
                            Logger.debug(AdminUploadServlet.class, "upload processed, removed="+uploadService.getRemovedUrls()+" added="+uploadService.getAddedUrls());

                            Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + destinationVirtualPath, -1, -1);
                        }

                        if ("skip".equals(overwriteMode) && destinationFileExists)  {
                            if (outputFile != null) outputFile.delete();
                            destinationFileExists = false;
                        }

                        output.put("name", name);
                        output.put("destinationFolder", destinationFolder);
                        output.put("uploadType", uploadType);
                        output.put("virtualPath", destinationVirtualPath);
                        output.put("key", random);
                        output.put("exists", destinationFileExists);
                    }
                    catch (JSONException e) {
                        Logger.error(AdminUploadServlet.class, e);
                    }
                    if (outputFile != null) temporary.put(random, new PathHolder(name, outputFile.getAbsolutePath(), Tools.getNow()));
                }
                catch (IOException ioe)
                {
                    Logger.error(AdminUploadServlet.class, ioe);
                }
            }
            else
            {
                MultipartWrapper.slowdownUpload();
            }
        }

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(output.toString());
	}

    /**
     * Presunie uploadnuty subor z docasneho umiestnenia do cieloveho adresara
     * @param fileKey
     * @param destinationFolder - URL adresa cieloveho adresara, napr. /images/gallery/
     * @param fileNameParam
     * @return - meno suboru po presune, alebo null ak sa subor nepresunul
     * @throws IOException
     */
    public static String moveAndReplaceFile(String fileKey, String destinationFolder, String fileNameParam) throws IOException
    {
        String fileName = fileNameParam;
        if (temporary.containsKey(fileKey))
        {
            IwcmFile dirFile = new IwcmFile(Tools.getRealPath(destinationFolder));
            if (dirFile.exists()==false) dirFile.mkdirs();

            if (destinationFolder.startsWith("/images") || destinationFolder.startsWith("/files") || destinationFolder.startsWith("/shared"))
            {
                fileName = DB.internationalToEnglish(fileName);
                fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
            }

            IwcmFile file = new IwcmFile(temporary.get(fileKey).getTempPath());
            if (!file.exists()) return null;
            IwcmFile dest = new IwcmFile(dirFile.getAbsolutePath(), fileName);

            if (dest.exists()) {
                dest.delete();
            }

            Logger.debug(AdminUploadServlet.class, "Moving file "+file.getAbsolutePath()+" to "+dest.getAbsolutePath());
            FileTools.moveFile(file, dest);
            Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + dest.getVirtualPath(), -1, 1);

            return fileName;
        }
        return null;
    }

    /**
     * Zmaze docasny subor (ak napr. user klikol na moznost neprepisat subor)
     * @param fileKey
     * @return - true ak subor existoval a zmazal sa
     */
    public static boolean deleteTempFile(String fileKey) {
        if (temporary.containsKey(fileKey)) {
            IwcmFile file = new IwcmFile(temporary.get(fileKey).getTempPath());
            if (file.exists()) return file.delete();
        }

        return false;
    }

    private void decodeBase64File(IwcmFile f) {
        try {
            if (f.exists() && f.canRead()) {
                InputStream is = new IwcmInputStream(f);
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.ISO_8859_1);
                char buff[] = new char[64000];
                int len;
                StringBuilder buffer = new StringBuilder();
                while ((len = isr.read(buff))!=-1) {
                    //Logger.debug(FileTools.class, "Reading: "+len+" total: "+contextFile.length());
                    buffer.append(buff, 0, len);
                }
                isr.close();
                is.close();

                //zdekoduj data
                byte[] decodedImg = Base64.getDecoder().decode(buffer.toString().getBytes(StandardCharsets.ISO_8859_1));
                IwcmOutputStream fos = new IwcmOutputStream(f);
                fos.write(decodedImg);
                fos.close();

                //skonvertuj PNG na JPG/format povodneho obrazku
                String extension = FileTools.getFileExtension(f.getName());
                if ("png".equals(extension)==false) {
                    IwcmInputStream iwStream =  new IwcmInputStream(f);
					BufferedImage pngImage = ImageIO.read(iwStream);
                    iwStream.close();
                    BufferedImage resultImage = new BufferedImage(
                        pngImage.getWidth(),
                        pngImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                    );

                    resultImage.createGraphics().drawImage(pngImage, 0, 0, Color.WHITE, null);

                    ImageWriteParam iwparam = null;
                    ImageWriter writer = null;

                    if ("jpg".equals(extension)) {
                        iwparam = new JPEGImageWriteParam(null);
                        iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        iwparam.setCompressionQuality(0.9F);
                    }

                    Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
                    if (iter.hasNext()) {
                        writer = iter.next();
                    }
                    if (writer != null) {
                        // Prepare output file
                        IwcmOutputStream out = new IwcmOutputStream(f);
                        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                        writer.setOutput(ios);
                        // Write the image
                        writer.write(null, new IIOImage(resultImage, null, null), iwparam);
                        // Cleanup
                        ios.flush();
                        writer.dispose();
                        ios.close();
                        out.close();
                    }
                }
            }
        }
		catch (Exception ex) {
            Logger.error(AdminUploadServlet.class, ex);
		}
    }

    /**
	 * Vrati cestu k temp suboru, pozuiva sa vo FormMail na detekciu ci subor vyhovuje poziadavkam
	 * @param fileKey
	 * @return
	 */
	public static String getTempFilePath(String fileKey)
	{
		if (temporary.containsKey(fileKey))
		{
			return temporary.get(fileKey).getTempPath();
		}
		return null;
	}

    /**
     * Return original file name. If file is not found, return null.
     * @param fileKey
     * @return
     */
    public static String getOriginalFileName(String fileKey) {
        if (temporary.containsKey(fileKey))
		{
			return temporary.get(fileKey).getFileName();
		}
		return null;
    }
}
