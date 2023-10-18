package sk.iway.iwcm.xls;

import org.apache.struts.action.ActionForm;

/**
 *  udrzuje informacie o importovanom subore
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2003
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Streda, 2003, január 29
 *@modified     $Date: 2004/03/04 22:15:55 $
 */
public class ImportXLSForm extends ActionForm
{
	private static final long serialVersionUID = 1L;

	private org.apache.struts.upload.FormFile file;

	/**
	 *  Gets the file attribute of the ImportXlsForm object
	 *
	 *@return    The file value
	 */
	public org.apache.struts.upload.FormFile getFile()
	{
		return file;
	}

	/**
	 *  Sets the file attribute of the ImportXlsForm object
	 *
	 *@param  file  The new file value
	 */
	public void setFile(org.apache.struts.upload.FormFile file)
	{
		this.file = file;
	}
}
