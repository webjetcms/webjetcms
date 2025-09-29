package sk.iway.iwcm.components.upload;

import sk.iway.iwcm.system.spring.components.SpringContext;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

	@Deprecated(since="calling new implementation")
	@SuppressWarnings("java:S1130")
    public static String moveAndReplaceFile(String fileKey, String dir, String fileNameParam) throws IOException
    {
		return getService().moveAndReplaceFile(fileKey, dir, fileNameParam);
    }

	@Deprecated(since="calling new implementation")
	public static boolean delete(String hash) {
		return getService().delete(hash);
	}

	/**
	 * Vrati meno suboru podla zadaneho kluca
	 * @param fileKey
	 * @return
	 */
	@Deprecated(since="calling new implementation")
	public static String getTempFileName(String fileKey)
	{
		return getService().getTempFileName(fileKey);
	}

	/**
	 * Vrati cestu k temp suboru, pozuiva sa vo FormMail na detekciu ci subor vyhovuje poziadavkam
	 * @param fileKey
	 * @return
	 */
	@Deprecated(since="calling new implementation")
	public static String getTempFilePath(String fileKey)
	{
		return getService().getTempFilePath(fileKey);
	}

	private static XhrFileUploadService getService() {
		return SpringContext.getApplicationContext().getBean("xhrFileUploadService", XhrFileUploadService.class);
	}
}
