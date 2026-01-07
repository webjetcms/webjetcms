package sk.iway.upload;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import sk.iway.iwcm.IwcmRequest;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.servlet.ServletException;


/**
 * Nahrada za org.apache.struts.upload.DiskMultipartRequestHandler, do konfigu
 *  strutsu ho treba nastavit:
 *  Do web.xml, do inicializacie action
 *   <init-param>
 *     <param-name>multipartClass</param-name>
 *     <param-value>sk.iway.upload.DiskMultiPartRequestHandler</param-value>
 *   </init-param>
 *
 * Na to aby to fungovalo, treba do formularu doplnit volanie popup okna s
 * progresom uploadu:
 * <form ... onsubmit="showUploadProgressBar()">
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Sobota, 2003, október 25
 *@modified     $Date: 2003/10/27 07:44:55 $
 */
public class DiskMultiPartRequestHandler
{

   private List<FileItem> files;

	/**
    *  kopia triedy z originalneho balika, pouziva vsak nas MultipartIterator
    *
    *@param  request               Description of the Parameter
    *@exception  ServletException  Description of the Exception
    * @throws FileUploadException
	 * @throws UnsupportedEncodingException
    */
   public HttpServletRequest handleRequest(HttpServletRequest request) throws ServletException, FileUploadException, UnsupportedEncodingException, IOException
   {
		DiskFileItemFactory factory = DiskFileItemFactory.builder().get();

		JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);
		files = upload.parseRequest(request);
		if (files != null) Logger.debug(DiskMultiPartRequestHandler.class, "DiskMultiPartRequestHandler.handleRequest, files="+files.size());

		IwcmRequest wrapped = new IwcmRequest(request);

		if (files != null)
		{
			Map<String, List<String>> paramsTable = new Hashtable<>();
			for (FileItem item : files)
			{
				DiskFileItem diskFile = ((DiskFileItem)item);

				if (diskFile.isFormField())
				{
					Logger.debug(DiskMultiPartRequestHandler.class, "name="+diskFile.getFieldName()+" value="+diskFile.getString(Charset.forName(SetCharacterEncodingFilter.getEncoding()))+" isFormField="+diskFile.isFormField()+" inMemory="+diskFile.isInMemory());
				}
				else
				{
					Logger.debug(DiskMultiPartRequestHandler.class, "name="+diskFile.getFieldName()+" size="+diskFile.getSize()+" contentType="+diskFile.getContentType()+" isFormField="+diskFile.isFormField()+" inMemory="+diskFile.isInMemory());
				}

				if (!diskFile.isFormField() || diskFile.getString() == null) continue;

				Logger.debug(DiskMultiPartRequestHandler.class, "setting");

				String name = diskFile.getFieldName();
				String value = diskFile.getString(Charset.forName(SetCharacterEncodingFilter.getEncoding()));

				//aby nam fungovalo aj request.getParameterValues()
				List<String> valuesList = paramsTable.get(name);
				if (valuesList == null)
				{
					valuesList = new ArrayList<>();
					paramsTable.put(name, valuesList);
				}

				valuesList.add(value);
			}

			Set<Map.Entry<String, List<String>>> paramSet = paramsTable.entrySet();
			for (Map.Entry<String, List<String>> me : paramSet)
			{
				String name = me.getKey();
				List<String> valueList = me.getValue();
				if (valueList.size()<1) continue;
				wrapped.setParameterValues(name, valueList.toArray(new String[0]));
			}

			// Add parts to the wrapped request
			for (FileItem item : files)
			{
				Part part = new FileItemPart(item);
                wrapped.addPart(part);
			}
		}

		//uloz parametre do request beanu, kedze tam su zatial prazdne
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null) {
			rb.setParameters(wrapped.getParameterMap());
		}

		return wrapped;
   }

	public Map<String, UploadedFile> getFileElements()
	{
		Map<String, UploadedFile> items = new HashMap<>();
		for (FileItem item : files)
		{
			DiskFileItem diskFile = ((DiskFileItem)item);
			if (diskFile.isFormField()) continue;
			items.put(diskFile.getFieldName(), new UploadedFile(diskFile));
		}
		return items;
	}

	public Map<String, List<UploadedFile>> getFileElementsMultiple()
	{
		Map<String, List<UploadedFile>> items = new HashMap<>();
		for (FileItem item : files)
		{
			DiskFileItem diskFile = ((DiskFileItem)item);
			if (diskFile.isFormField()) continue;

			String key = diskFile.getFieldName();

			List<UploadedFile> uploadedFiles = items.get(key);
			if (uploadedFiles == null) {
				uploadedFiles = new ArrayList<>();
				items.put(key, uploadedFiles);
			}

			uploadedFiles.add(new UploadedFile(diskFile));
		}
		return items;
	}

	public void rollback()
	{
		for (FileItem item : files)
		{
			try {
				item.delete();
			} catch (Exception e) {
				Logger.error(DiskMultiPartRequestHandler.class, "Error deleting file item", e);
			}
		}
	}
}