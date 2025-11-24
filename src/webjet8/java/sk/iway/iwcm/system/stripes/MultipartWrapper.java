package sk.iway.iwcm.system.stripes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.controller.FileUploadLimitExceededException;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;

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
	private HttpServletRequest request;
	private boolean isParsed = false;
	private Map<String,FileItem> files = new HashMap<>();
	private Map<String,String[]> parameters = new HashMap<>();
	private Map<String, List<String>> params;
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

		this.request = request;
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
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setRepository(tempDir);

			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setHeaderEncoding(SetCharacterEncodingFilter.getEncoding());

			// MBO FIX: po upgrade Stripes niekedy davno:) prestali ist uploady
			// velkych suborov, maxPostSize sa ktovie odkial bral a mal hodnotu
			// 947912704
			// vytiahneme z konfiguracie maxPostSize pre Stripes, konvertneme na
			// long a nastavime pre upload
			long maxPostSizeOveride = Tools.getLongValue(Tools.replace(Constants.getString("stripes.FileUpload.MaximumPostSize"), "m", "000000"), 5000000000L);
			upload.setSizeMax(maxPostSizeOveride);

			List<FileItem> items = upload.parseRequest(request);
			params = new HashMap<>();
			Identity user = UsersDB.getCurrentUser(request.getSession());

            for (FileItem item : items) {
                // If it's a form field, add the string value to the list
                if (item.isFormField()) {
                    List<String> values = params.get(item.getFieldName());
                    if (values == null) {
                        values = new ArrayList<>();
                        params.put(item.getFieldName(), values);
                    }
                    values.add(item.getString(SetCharacterEncodingFilter.getEncoding()));

                    if (item.getFieldName().equalsIgnoreCase("upload_path[]")) {
                        uploadPaths.add(item.getString(SetCharacterEncodingFilter.getEncoding()));
                    }
                }
            }

            //default true, kedze nie vsade je CSRF token
            boolean csrfOK = true;
            //CSRF ochrana
            if (queryString.indexOf("__sfu=1")!=-1)
            {
                //ak nepride token, default je false
                csrfOK = false;

				//skonvertuj queryString hodnoty do parametrov, nech sa to da pouzit standardizovane
				String[] queryParamValues = Tools.getTokens(queryString, "&");
				for (String paramValue : queryParamValues)
				{
					int index = paramValue.indexOf("=");
					if (index>0 && index<paramValue.length())
					{
						String name = paramValue.substring(0, index);

						List<String> values = params.get(name);
						if(values==null) values = new ArrayList<>();
						values.add(paramValue.substring(index+1));

						this.params.put(name, values);
					}
				}

				List<String> values = this.params.get("__token");
                if (values!=null && values.size()>0)
                {
						String token = values.get(0);

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
            }

            if (csrfOK)
            {
                for (FileItem item : items) {
                    if (!item.isFormField()) {
                        String fileName = clearFileName(item.getName());
                        String directory = getDirectory(fileName); //NOSONAR
                        FileItem fi = new RenamedFileItem(item, fileName);

                        boolean isAllowedForUpload = isFileAllowedForUpload(user, fi);
                        Logger.debug(MultipartWrapper.class, "Storing file: " + fi.getFieldName() + ", File name: " + fi.getName() + ", File type: " + fi.getContentType()
                                + ", File size: " + fi.getSize() + ", allowed=" + isAllowedForUpload);
                        if (isAllowedForUpload) {
                            String key = fi.getFieldName();
                            //toto je pre elfinder pre multiupload, vtedy subory odkladame do mapy pod rozumnymi nazvami a nie original field name
                            //cid sa posiela pri chunked uploade a vtedy mame nazvy ulozene inde
                            if (key.equalsIgnoreCase("upload[]") && params.get("cid") == null) {
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
					Logger.error(MultipartWrapper.class, "CHYBA: nesedi CSRF token, pre upload je povinny");
				}


			fixDocIdQueryString(request);
			convertParams();

			slowdownUpload();

			isParsed = true;
		}
		catch (FileUploadBase.SizeLimitExceededException slee)
		{
			throw new FileUploadLimitExceededException(maxPostSize, maxPostSize);
		}
		catch (FileUploadException fue)
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

	private void fixDocIdQueryString(HttpServletRequest request) {
		try
		{

			// fix na docid v query stringu
			String queryString = request.getQueryString();
			if (queryString != null && params.get("docid") == null)
			{
				int i = queryString.indexOf("docid=");
				int j = queryString.indexOf('&');
				if (i != -1 && j > i)
				{
					String value = queryString.substring(i + 6, j);
					List<String> values = new ArrayList<>();
					values.add(value);
					Logger.debug(MultipartWrapper.class, "Adding docid: " + value);
					params.put("docid", values);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	private void convertParams() {
		// Now convert them down into the usual map of String->String[]
		for (Map.Entry<String, List<String>> entry : params.entrySet())
		{
			List<String> values = entry.getValue();
			this.parameters.put(entry.getKey(), values.toArray(new String[values.size()]));
		}
	}

	@Override
	public Enumeration<String> getParameterNames() {
      if (isParsed == false) return this.request.getParameterNames();

      return new IteratorEnumeration(this.parameters.keySet().iterator());
  }

	@Override
	public String[] getParameterValues(String name) {
      if (isParsed == false) return this.request.getParameterValues(name);

      return this.parameters.get(name);
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
   	public static class RenamedFileItem implements FileItem
	{

		private FileItem item;
		private String name;

		public RenamedFileItem(FileItem item, String name) {
			this.item = item;
			this.name = name;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return item.getInputStream();
		}

		@Override
		public String getContentType() {
			return item.getContentType();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isInMemory() {
			return item.isInMemory();
		}

		@Override
		public long getSize() {
			return item.getSize();
		}

		@Override
		public byte[] get() {
			return item.get();
		}

		@Override
		public String getString(String paramString) throws UnsupportedEncodingException {
			return item.getString(paramString);
		}

		@Override
		public String getString() {
			return item.getString();
		}

		@Override
		public void write(File paramFile) throws Exception {
			item.write(paramFile);

		}

		@Override
		public void delete() {
			item.delete();

		}

		@Override
		public String getFieldName() {
			return item.getFieldName();
		}

		@Override
		public void setFieldName(String paramString) {
			item.setFieldName(paramString);

		}

		@Override
		public boolean isFormField() {
			return item.isFormField();
		}

		@Override
		public void setFormField(boolean paramBoolean) {
			item.setFormField(paramBoolean);

		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return item.getOutputStream();
		}

		@Override
		public FileItemHeaders getHeaders() {
			return item.getHeaders();
		}

		@Override
		public void setHeaders(FileItemHeaders arg0) {
			item.setHeaders(arg0);

		}

   }

}