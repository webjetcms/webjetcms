package sk.iway.iwcm.system.stripes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.controller.FileUploadLimitExceededException;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.upload.DiskMultiPartRequestHandler;

/**
 *  MultipartWrapper.java - multipart pri stripes nie je mozne pouzit, potom by nefungovali veci v admin casti WebJETu (pouzivajuce struts)
 *
 *@Title        webjet5
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      Date: 26.3.2007 9:52:29
 *@modified     $Date: 2009/09/11 06:54:19 $
 */
public class MultipartWrapper implements net.sourceforge.stripes.controller.multipart.MultipartWrapper //NOSONAR
{
	//in case of manual parsing of multipart request, we store parsed request here
	private HttpServletRequest parsedRequest;

	//indicates, that the multipart request was processed using URL parameters
	private boolean isParsed = false;

	private Map<String,FileItem> files = new HashMap<>();
	private List<String> uploadPaths = new ArrayList<>();

	@Override
	public void build(HttpServletRequest request, File tempDir, long maxPostSize) throws IOException, FileUploadLimitExceededException
	{
		Logger.debug(MultipartWrapper.class, "build...");
		String path = request.getRequestURI();
		String queryString = request.getQueryString();
		if (queryString==null) queryString = "";
		if (path.contains("upload") || path.endsWith(".jsp") || path.endsWith(".action") || path.contains("stripes") ||
			queryString.contains("__sfu=1") || path.contains("/admin/elfinder-connector/") || queryString.contains("__forceParse=1")) {

			boolean processUpload = true;
			//multiplefileupload.do aj ine Struts action sa spracovavaju napriamo
			if (path.contains(".do")) processUpload = false;
			//wj9 spring admin apps su springove, upload spracuju same
			if (path.startsWith("/apps/") && path.contains("/admin/")) processUpload = false;

			if (path.contains("spring") || path.contains("rest") || path.startsWith("/admin/v9/") || queryString.contains("__sfu=0") ) {
				//do not process this URLs, it's spring upload
				processUpload = false;
			}

			//moznost ovplyvnit spracovanie uploadu pomocou URL parametra bez ohladu na dalsie parametre
			if (queryString.contains("__forceParse=1")) processUpload = true;
			if (queryString.contains("__forceParse=0")) processUpload = false;

			if (processUpload)
			{
				buildImpl(request, tempDir, maxPostSize, queryString);
				if (Constants.getBoolean("alwaysStoreUploadedFileToRequest") || queryString.indexOf("__setf=1")!=-1 || path.contains("/admin/elfinder-connector/"))
				{
					Logger.debug(MultipartWrapper.class, "Storing files to request");
					request.setAttribute("MultipartWrapper.files", files);
				}
			}
		}
	}

	private boolean isFileAllowedForUpload(Identity user, FileItem file)
	{
		//zmenene z false na true pretoze potom sa zle plnili polia so subormi a padalo to dalej na NPE
		if (file == null || Tools.isEmpty(file.getName())) return true;

		return FileTools.isFileAllowedForUpload(user, file.getName());
	}

	private void buildImpl(HttpServletRequest request, File tempDir, long maxPostSize, String queryString) throws IOException, FileUploadLimitExceededException
	{
		//list nazvov suborov ktore sa neuploadli kvoli pravam usera (napr. prekrocena max velkost suboru), ziska sa nasledne v Elfinderi pre vypis error hlasky
		List<String> notUploaded = new ArrayList<>();

		try
		{
			Logger.debug(MultipartWrapper.class, "Build IMPL");

			// MBO FIX: po upgrade Stripes niekedy davno:) prestali ist uploady
			// velkych suborov, maxPostSize sa ktovie odkial bral a mal hodnotu
			// 947912704
			// vytiahneme z konfiguracie maxPostSize pre Stripes, konvertneme na
			// long a nastavime pre upload

			Collection<Part> items = request.getParts();

			if (items.size()==0) {
				//not parser, parse multipart request, probably direct JSP file call
				Logger.debug(MultipartWrapper.class, "No parts found, probably direct JSP file call, parsing multipart request manually");
				DiskMultiPartRequestHandler multipartHandler = null;
				multipartHandler = new DiskMultiPartRequestHandler();
				request = multipartHandler.handleRequest(request);
				items = request.getParts();
				parsedRequest = request;
			}

			Identity user = UsersDB.getCurrentUser(request.getSession());

            //default true, kedze nie vsade je CSRF token
            boolean csrfOK = true;
            //CSRF ochrana
            if (queryString.indexOf("__sfu=1")!=-1)
            {
                //ak nepride token, default je false
                csrfOK = false;

				String token = request.getParameter(CSRF.getParameterName());
				if (request.getRequestURI().contains("/rest/datatables") || request.getParameter("csrfKeepToken")!=null)
                {
                    //datatables ma vynimku, token po nahrati suboru nemazeme, pretoze sa nemeni datatable save URL
                    csrfOK = CSRF.verifyTokenAjax(request.getSession(), token);
                }
                else
                {
                    csrfOK = CSRF.verifyTokenAndDeleteIt(request.getSession(), token);
                }
            }

			csrfOK = true;

            if (csrfOK)
            {
                for (Part item : items) {
                    if (item.getSubmittedFileName() != null) {
                        String fileName = clearFileName(item.getSubmittedFileName());
                        String directory = getDirectory(fileName); //NOSONAR
                        FileItem fi = new PartToFileItemAdapter(item, fileName);

                        boolean isAllowedForUpload = isFileAllowedForUpload(user, fi);
                        Logger.debug(MultipartWrapper.class, "Storing file: " + fi.getFieldName() + ", File name: " + fi.getName() + ", File type: " + fi.getContentType()
                                + ", File size: " + fi.getSize() + ", allowed=" + isAllowedForUpload);
                        if (isAllowedForUpload) {
                            String key = fi.getFieldName();
                            //toto je pre elfinder pre multiupload, vtedy subory odkladame do mapy pod rozumnymi nazvami a nie original field name
                            //cid sa posiela pri chunked uploade a vtedy mame nazvy ulozene inde
                            if (key.equalsIgnoreCase("upload[]") && request.getParameter("cid") == null) {
                                key = Tools.isNotEmpty(directory) ? directory + "/" + fileName : fileName;
                            }
                            Logger.debug(MultipartWrapper.class, "Storing file " + key);
                            files.put(key, fi);
                        } else {
                            notUploaded.add(fi.getName());
                        }
                    }
                }
            }
            else
			{
				Logger.error(MultipartWrapper.class, "CHYBA: nesedi CSRF token, pre upload je povinny, count="+items.size());
			}


			slowdownUpload();

			isParsed = true;
		}
		catch (FileUploadException|ServletException fue)
		{
			IOException ioe = new IOException("Could not parse and cache file upload data.");
			ioe.initCause(fue);
			throw ioe;
		}

		if (notUploaded.size()>0)
		{
			request.setAttribute("MultipartWrapper.notUploaded", notUploaded);
		}
	}

	/**
	 * Riesenie problemu s chybou 429 Too Many Requests
	 */
	public static void slowdownUpload()
	{
		int uploadProtectionInterval = Constants.getInt("uploadProtectionInterval");
		if (uploadProtectionInterval > 0)
		{
			Logger.debug(MultipartWrapper.class, "Waiting for "+uploadProtectionInterval+" ms");
			try
			{
				Thread.sleep(uploadProtectionInterval);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
			Logger.debug(MultipartWrapper.class, "Waiting done");
		}
	}

	private String getDirectory(String fileName) {
		if (uploadPaths != null && uploadPaths.size() > 0) {
			Iterator<String> i = uploadPaths.iterator();
			while (i.hasNext()) {
			   String uploadPath = i.next();
			   if (uploadPath.endsWith(fileName)) {
					fileName = uploadPath;
					i.remove();
					break;
				}
			}
		}

		int lomka = fileName.lastIndexOf("/");

		if (lomka > 0) {
			return fileName.substring(0, lomka);
		}

		return "";
	}

	private String clearFileName(String fileName) {
		if (fileName == null) return null;

		// MBO oprava IE bugu, kedy IE posiela pri uploade celu
		if (fileName.contains("\\"))
		{
			fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
			Logger.debug(MultipartWrapper.class, "IE poslalo celu cestu k suboru, orezavam ho iba nan nazov: " + fileName);
		}

		int lomka = fileName.lastIndexOf("/");

		if (lomka > -1) {
			return fileName.substring(lomka + 1);
		}

		return fileName;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (parsedRequest != null) {
			return parsedRequest.getParameterNames();
		}
		//return empty enumeration because it will be merged with original request parameters and if we return items here they will be duplicated
		return new IteratorEnumeration(new ArrayList<String>().iterator());
	}

	@Override
	public String[] getParameterValues(String name) {
		if (parsedRequest != null) {
			return parsedRequest.getParameterValues(name);
		}
		//return null because it will be merged with original request parameters and if we return items here they will be duplicated
		return null;
	}

	@Override
	public Enumeration<String> getFileParameterNames() {
		if (isParsed == false) return(null);

		return new IteratorEnumeration(this.files.keySet().iterator());
  	}

	@Override
	public FileBean getFileParameterValue(String name) {
		if (isParsed == false) return null;

		final FileItem item = this.files.get(name);
		if (item == null) {
			return null;
		}
		else {
			// Use an subclass of FileBean that overrides all the
			// methods that rely on having a File present, to use the FileItem
			// created by commons upload instead.
			return new IwayFileBean(null, item);
		}
  	}

	/** Little helper class to create an enumeration as per the interface. */
   	private static class IteratorEnumeration implements Enumeration<String> { //NOSONAR
		Iterator<String> iterator;

       /** Constructs an enumeration that consumes from the underlying iterator. */
       IteratorEnumeration(Iterator<String> iterator) { this.iterator = iterator; }

       /** Returns true if more elements can be consumed, false otherwise. */
       @Override
       public boolean hasMoreElements() { return this.iterator.hasNext(); }

       /** Gets the next element out of the iterator. */
       @Override
       public String nextElement() { return this.iterator.next(); }
   	}

   	public boolean isRequestParsed() {
		return isParsed;
	}

   	/**
	 * Retrieve a FileItem from the request attributes, you need to use ?__setf=1 in URL to store files into request
	 * @param fieldName
	 * @param request
	 * @return
	 */
   	public static FileItem getFileStoredInRequest(String fieldName, HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String,FileItem> files = (Map<String,FileItem>)request.getAttribute("MultipartWrapper.files");
		if (files!=null) {
			return files.get(fieldName);
		}
		return null;
	}

   	/**
   	 * FileItem so zmenenym nazvom - umoznuje odstranovat celu cestu k suboru
   	 * @author MBO
   	 *
   	 */
   	public static class PartToFileItemAdapter implements FileItem {
		private final Part part;
		private final String fileName;

		public PartToFileItemAdapter(Part part, String fileName) {
			this.part = part;
			this.fileName = fileName;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return part.getInputStream();
		}

		@Override
		public String getContentType() {
			return part.getContentType();
		}

		@Override
		public String getName() {
			return fileName;
		}

		@Override
		public boolean isInMemory() {
			return part.getSize() < 10240; // Assume small files are in memory
		}

		@Override
		public long getSize() {
			return part.getSize();
		}

		@Override
		public byte[] get() {
			try {
				try (InputStream is = part.getInputStream()) {
					return is.readAllBytes();
				}
			} catch (IOException e) {
				Logger.error(MultipartWrapper.class, "Chyba pri ziskavani dat z FileItem", e);
				return null;
			}
		}

		@Override
		public String getString(String charset) throws UnsupportedEncodingException {
			try {
				return new String(get(), charset);
			} catch (IOException e) {
				Logger.error(MultipartWrapper.class, "Chyba pri ziskavani stringu z FileItem", e);
				return null;
			}
		}

		@Override
		public String getString() {
			try {
				return getString(SetCharacterEncodingFilter.getEncoding());
			} catch (UnsupportedEncodingException e) {
				Logger.error(MultipartWrapper.class, "Chyba pri ziskavani stringu z FileItem", e);
				return null;
			}
		}

		@Override
		public String getFieldName() {
			return part.getName();
		}

		@Override
		public boolean isFormField() {
			return part.getSubmittedFileName() == null;
		}

		// Other required methods...
		@Override
		public void write(File file) throws IOException {
			part.write(file.toString());
		}

		@Override
		public void delete() {
			try {
				part.delete();
			} catch (IOException e) {
				Logger.error(MultipartWrapper.class, "Chyba pri mazani FileItem", e);
			}
		}

		@Override
		public void setFieldName(String name) {
			// Part doesn't support changing field name
		}

		@Override
		public void setFormField(boolean state) {
			// Part doesn't support changing form field state
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new UnsupportedOperationException("Part doesn't support OutputStream");
		}

		@Override
		public FileItemHeaders getHeaders() {
			// Convert Part headers to FileItemHeaders if needed
			return null;
		}

		@Override
		public void setHeaders(FileItemHeaders headers) {
			// Part doesn't support setting headers
		}
	}

}