package sk.iway.iwcm.components.upload;

import sk.iway.iwcm.Tools;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@MultipartConfig
@WebServlet("/XhrFileUpload")
public class XhrFileUploadServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		XhrFileUploadService xhrFileUploadService = getService();
		XhrFileUploadResponse xhrFileUploadResponse = xhrFileUploadService.processUpload(request);
		xhrFileUploadService.setResponse(response, xhrFileUploadResponse);
	}

	/**
	 * @deprecated use XhrFileUploadService.moveFile
	 * presunie uploadnuty subor
	 *
	 * @param fileKey - identifikator suboru
	 * @param dir - cielovy adresar
	 * @return - nazov suboru
	 * @throws IOException
	 */
	@Deprecated
	@SuppressWarnings("java:S1130")
	public static String moveFile(String fileKey, String dir) throws IOException
	{
		return getService().moveFile(fileKey, dir);
	}

	/**
	 * @deprecated use XhrFileUploadService.moveAndReplaceFile
	 * @param fileKey - identifikator suboru
	 * @param dir - cielovy adresar
	 * @param fileNameParam - nazov suboru
	 * @return - nazov suboru
	 * @throws IOException
	 */
	@Deprecated
	@SuppressWarnings("java:S1130")
    public static String moveAndReplaceFile(String fileKey, String dir, String fileNameParam) throws IOException
    {
		return getService().moveAndReplaceFile(fileKey, dir, fileNameParam);
    }

	/**
	 * @deprecated use XhrFileUploadService.delete(String hash)
	 * @param hash - identifikator suboru
	 * @return - boolean
	 */
	@Deprecated
	public static boolean delete(String hash) {
		return getService().delete(hash);
	}

	/**
	 * @deprecated use XhrFileUploadService.getTempFileName
	 * Vrati meno suboru podla zadaneho kluca
	 * @param fileKey - identifikator suboru
	 * @return - String
	 */
	@Deprecated
	public static String getTempFileName(String fileKey)
	{
		return getService().getTempFileName(fileKey);
	}

	/**
	 * @deprecated use XhrFileUploadService.getTempFilePath
	 * Vrati cestu k temp suboru, pozuiva sa vo FormMail na detekciu ci subor vyhovuje poziadavkam
	 * @param fileKey - identifikator suboru
	 * @return - String
	 */
	@Deprecated
	public static String getTempFilePath(String fileKey)
	{
		return getService().getTempFilePath(fileKey);
	}

	/**
	 * Vrati service
	 * @return - XhrFileUploadService
	 */
	public static XhrFileUploadService getService() {
		return Tools.getSpringBean("xhrFileUploadService", XhrFileUploadService.class);
	}
}
