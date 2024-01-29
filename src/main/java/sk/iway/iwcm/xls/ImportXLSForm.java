package sk.iway.iwcm.xls;

import org.apache.struts.upload.FormFile;

public class ImportXLSForm {
    
    FormFile file;

	public FormFile getFile() 
    {
		return file;
	}

	public void setFile(FormFile file)
	{
		this.file = file;
	}
}
