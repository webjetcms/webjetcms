package sk.iway.iwcm.components.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.RandomStringUtils;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.system.stripes.MultipartWrapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@MultipartConfig
@WebServlet("/XhrFileUpload")
public class XhrFileUploadServlet extends HttpServlet
{
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String ALLOWED_EXTENSIONS = "doc docx xls xlsx xml ppt pptx pdf jpeg jpg bmp tiff psd zip rar png mp4";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		Prop prop = Prop.getInstance();
		try {
			String name = request.getParameter("name");
			Logger.debug(XhrFileUploadServlet.class, "doPost, name from parameter: "+name);

			//dropzone.js kompatibilita
			Part filePart = request.getPart("file");

			if (filePart == null) {
				XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
				xhrFileUploadResponse.setError(prop.getText("components.forum.new.upload_not_allowed_filetype"));
				xhrFileUploadResponse.setFile("");
				xhrFileUploadResponse.setSuccess(false);
				setResponse(response, xhrFileUploadResponse);
				return;
			}

			if (filePart.getSubmittedFileName() != null) {
				name = filePart.getSubmittedFileName();
			}

			Logger.debug(XhrFileUploadServlet.class, "doPost, name from filePart: "+name);

			String extension = FileTools.getFileExtension(name);

			Logger.debug(XhrFileUploadServlet.class, "doPost, name="+name+", extension="+extension);
			List<String> allowedExtensions = Tools.getStringListValue(Tools.getTokens(Constants.getString("xhrFileUploadAllowedExtensions", ALLOWED_EXTENSIONS), " "));
			if (!allowedExtensions.contains("*") && !allowedExtensions.contains(extension)) {
				Logger.debug(XhrFileUploadServlet.class, "doPost, extension not allowed: "+extension);
				XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
				xhrFileUploadResponse.setError(prop.getText("components.forum.new.upload_not_allowed_filetype"));
				xhrFileUploadResponse.setFile(name);
				xhrFileUploadResponse.setSuccess(false);
				setResponse(response, xhrFileUploadResponse);
				return;
			}

			int chunk = Tools.getIntValue(request.getParameter("chunk"), 0);
			int chunks = Tools.getIntValue(request.getParameter("chunks"), 0);

			//dropzone.js kompatibilita
			if (request.getParameter("dzchunkindex") != null) chunk = Tools.getIntValue(request.getParameter("dzchunkindex"), 0);
			if (request.getParameter("dztotalchunkcount") != null) chunks = Tools.getIntValue(request.getParameter("dztotalchunkcount"), 0);

			Logger.debug(XhrFileUploadServlet.class, "doPost, chunk="+chunk+", chunks="+chunks);

			HttpSession session = request.getSession();
			PartialUploadHolder holder = (PartialUploadHolder) session.getAttribute("partialUploadFile-" + name);
			if (holder == null || chunk == 0) {
				holder = new PartialUploadHolder(chunks, name);
				session.setAttribute("partialUploadFile-" + name, holder);
			}
			boolean isLast = false;
			if (holder.getPartPaths().size() + 1 == holder.getChunks() || holder.getChunks() == 0) {
				//je to posledny alebo jediny chunk
				isLast = true;
			}

			String suffix = FileTools.getFileExtension(name);

			//zapisem data do docasneho suboru
			File tempUploadFile = File.createTempFile(name, "." + suffix);
			FileOutputStream tempFos = new FileOutputStream(tempUploadFile);
			InputStream tempIs = filePart.getInputStream();
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = tempIs.read(bytes)) != -1) {
				tempFos.write(bytes, 0, read);
			}

			tempIs.close();
			tempFos.close();

			String filePartName = tempUploadFile.getAbsolutePath();//filePart.get//getName();
			holder.getPartPaths().add(chunk, filePartName);

			XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
			xhrFileUploadResponse.setSuccess(true);
			xhrFileUploadResponse.setChunkUploaded(chunk);
			xhrFileUploadResponse.setSize(new File(filePartName).length());

			if (isLast) {
				session.removeAttribute("partialUploadFile-" + name);
				// mam posledny, spojim ich do jedneho

				FileOutputStream fos;
				FileInputStream fis;
				byte[] fileBytes;
				String random = RandomStringUtils.random(15, true, true);
				File tempfile = null;
				try {
					tempfile = File.createTempFile(random, "." + suffix);
					fos = new FileOutputStream(tempfile, true);
					for (String chunkPath : holder.getPartPaths()) {
						File inputFile = new File(chunkPath);
						fis = new FileInputStream(inputFile);
						fileBytes = new byte[(int) inputFile.length()];
						fis.read(fileBytes, 0, (int) inputFile.length());
						fos.write(fileBytes);
						fos.flush();
						fileBytes = null;
						fis.close();
						fis = null;
						inputFile.delete(); //NOSONAR
					}
					fos.close();
					fos = null;

					WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);
					fileWebjetEvent.publishEvent();

					if (request.getAttribute("xhrError") != null) {
						Logger.debug(XhrFileUploadServlet.class, "doPost, xhrError, name="+name);
						xhrFileUploadResponse = new XhrFileUploadResponse();
						xhrFileUploadResponse.setFile(name);
						xhrFileUploadResponse.setSuccess(false);
						xhrFileUploadResponse.setError((String) request.getAttribute("xhrError"));
						setResponse(response, xhrFileUploadResponse);
						return;
					}

					Logger.debug(XhrFileUploadServlet.class, "doPost, success, name="+name+", key="+random);

					xhrFileUploadResponse.putName(name);
					xhrFileUploadResponse.putKey(random);

					UploadedFilesStore.put(random, new PathHolder(name, tempfile.getAbsolutePath(), Tools.getNow()));
				} catch (IOException ioe) {
					sk.iway.iwcm.Logger.error(ioe);
				}
			} else {
				MultipartWrapper.slowdownUpload();
			}

			setResponse(response, xhrFileUploadResponse);
		}
		catch (Exception ex) {
			/*
			if (ex.getClass().isAssignableFrom(IOException.class) && ex.getMessage().contains("Unexpected EOF")) {
				Logger.warn(XhrFileUploadServlet.class, "User cancelled upload");
				Logger.debug(XhrFileUploadServlet.class, ex.getMessage());
				return;
			}
			 */
			Logger.warn(XhrFileUploadServlet.class, ex.getMessage());
			sk.iway.iwcm.Logger.error(ex);

			XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
			xhrFileUploadResponse.setError(prop.getText("components.docman.error.db"));
			xhrFileUploadResponse.setSuccess(false);
			setResponse(response, xhrFileUploadResponse);
		}
	}

	private void setResponse(HttpServletResponse response, XhrFileUploadResponse xhrFileUploadResponse) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(mapper.writeValueAsString(xhrFileUploadResponse));
		} catch (IOException e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}


	/**
	 * presunie uploadnuty subor
	 *
	 * @param fileKey - identifikator suboru
	 * @param dir - cielovy adresar
	 * @return - nazov suboru
	 * @throws IOException
	 */
	@SuppressWarnings("java:S1130")
	public static String moveFile(String fileKey, String dir) throws IOException
	{
		if (UploadedFilesStore.containsKey(fileKey))
		{
		    IwcmFile dirFile = new IwcmFile(dir);
		    String dirVirtualPath = dirFile.getVirtualPath();

			String originalFilename = UploadedFilesStore.get(fileKey).getFileName();

			if (dirVirtualPath.startsWith("/images") || dirVirtualPath.startsWith("/files"))
            {
                originalFilename = DB.internationalToEnglish(originalFilename);
                originalFilename = DocTools.removeCharsDir(originalFilename, true).toLowerCase();
            }

			String filename = originalFilename;
			IwcmFile file = new IwcmFile(UploadedFilesStore.get(fileKey).getTempPath());
			if (!file.exists()) return null;
			IwcmFile dest = new IwcmFile(dir, originalFilename);
			int counter = 1;
			while (dest.exists())
			{
				filename = originalFilename + "-" + counter;
				if (originalFilename.contains("."))	filename = originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "-" + counter + originalFilename.substring(originalFilename.lastIndexOf("."));
				dest = new IwcmFile(dir, filename);
				counter++;
			}

			Logger.debug(XhrFileUploadServlet.class, "moveFile, Moving file "+file.getAbsolutePath()+", to "+dest.getAbsolutePath());
			FileTools.moveFile(file, dest);
			Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + dest.getVirtualPath(), -1, 1);
			return filename;
		}
		return null;
	}

	@SuppressWarnings("java:S1130")
    public static String moveAndReplaceFile(String fileKey, String dir, String fileNameParam) throws IOException
    {
        String fileName = fileNameParam;
        if (UploadedFilesStore.containsKey(fileKey))
        {
            IwcmFile dirFile = new IwcmFile(dir);
            String dirVirtualPath = dirFile.getVirtualPath();

            if (dirVirtualPath.startsWith("/images") || dirVirtualPath.startsWith("/files"))
            {
                fileName = DB.internationalToEnglish(fileName);
                fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
            }

            IwcmFile file = new IwcmFile(UploadedFilesStore.get(fileKey).getTempPath());
            if (!file.exists()) return null;
            IwcmFile dest = new IwcmFile(dir + fileName);

            if (dest.exists()) {
                dest.delete();
            }

				Logger.debug(XhrFileUploadServlet.class, "moveAndReplaceFile, Moving file "+file.getAbsolutePath()+", to "+dest.getAbsolutePath());
            FileTools.moveFile(file, dest);
            Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + dest.getVirtualPath(), -1, 1);

            return fileName;
        }
        return null;
    }

	public static boolean delete(String hash) {
		if (UploadedFilesStore.containsKey(hash))
		{

			IwcmFile file = new IwcmFile(UploadedFilesStore.get(hash).getTempPath());
			return file.delete();
		}

		return false;
	}

	/**
	 * Vrati meno suboru podla zadaneho kluca
	 * @param fileKey
	 * @return
	 */
	public static String getTempFileName(String fileKey)
	{
		if (UploadedFilesStore.containsKey(fileKey))
		{
			return UploadedFilesStore.get(fileKey).getFileName();
		}
		return null;
	}

	/**
	 * Vrati cestu k temp suboru, pozuiva sa vo FormMail na detekciu ci subor vyhovuje poziadavkam
	 * @param fileKey
	 * @return
	 */
	public static String getTempFilePath(String fileKey)
	{
		if (UploadedFilesStore.containsKey(fileKey))
		{
			return UploadedFilesStore.get(fileKey).getTempPath();
		}
		return null;
	}


}
