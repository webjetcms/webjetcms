package sk.iway.iwcm.components.importWebPages;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

import sk.iway.iwcm.Tools;

/**
 *  upload suboru (obrazok, subor) na server (formular)
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $ 
 */
public class ImportZipFileForm extends ActionForm
{
	
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	/**
    *  Description of the Field
    */
   protected FormFile uploadFile;   
   private int parentGroupId;
   private boolean overwriteFiles;
   private String parentGroupIdString;
   
   private String imagesRootDir = "/images/";
   private String filesRootDir = "/files/";
   @Override
   public void reset(ActionMapping arg0, HttpServletRequest arg1)
	{
		overwriteFiles = false;
	}  
   
   /**
	 * @return Returns the overwriteFiles.
	 */
	public boolean isOverwriteFiles()
	{
		return overwriteFiles;
	}
	/**
	 * @param overwriteFiles The overwriteFiles to set.
	 */
	public void setOverwriteFiles(boolean overwriteFiles)
	{
		this.overwriteFiles = overwriteFiles;
	}
	/**
	 * @return Returns the parentGroupId.
	 */
	public int getParentGroupId()
	{
		return parentGroupId;
	}
	/**
	 * @param parentGroupId The parentGroupId to set.
	 */
	public void setParentGroupId(int parentGroupId)
	{
		this.parentGroupId = parentGroupId;
	}
	/**
	 * @return Returns the uploadFile.
	 */
	public FormFile getUploadFile()
	{
		return uploadFile;
	}
	/**
	 * @param uploadFile The uploadFile to set.
	 */
	public void setUploadFile(FormFile uploadFile)
	{
		this.uploadFile = uploadFile;
	}
	public String getParentGroupIdString()
	{
		return parentGroupIdString;
	}
	public void setParentGroupIdString(String newParentGroupIdString)
	{
		
		this.parentGroupIdString = recode(newParentGroupIdString);
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  input  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	private String recode(String input)
	{
		if (input == null)
		{
			return ("");
		}
		//Logger.println(this,"Recoding: "+input);
		return (input.trim());
	}

	public String getImagesRootDir()
	{
		if (Tools.isEmpty(imagesRootDir)) imagesRootDir = "/";
		if (imagesRootDir.endsWith("/")==false) imagesRootDir+="/";
		return imagesRootDir;
	}

	public void setImagesRootDir(String imagesRootDir)
	{
		this.imagesRootDir = imagesRootDir;
	}

	public String getFilesRootDir()
	{
		if (Tools.isEmpty(filesRootDir)) filesRootDir = "/";
		if (filesRootDir.endsWith("/")==false) filesRootDir+="/";
		return filesRootDir;
	}

	public void setFilesRootDir(String filesRootDir)
	{
		this.filesRootDir = filesRootDir;
	}
}
