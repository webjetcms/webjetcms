package sk.iway.iwcm.system;

import java.io.File;

import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.stripes.WebJETActionBean;

/**
 *	ConfImportAction.java
 * Upload xml suboru pre import konfiguracie a pre import suboro pre file archiv
 *
 *@Title			webjet7
 *@Company		Interway s.r.o. (www.interway.sk)
 *@Copyright 	Interway s.r.o. (c) 2001-2013
 *@author		$Author: jeeff $(prau)
 *@version		Revision: 1.3  13.11.2013
 *@created		Date: 13.11.2013 16:22:33
 *@modified   	Date: 13.11.2013 16:22:33
 */
public class ConfImportAction extends WebJETActionBean// pouziva sa aj pre upload file archivu
{

		FileBean xmlFile = null;

		@HandlesEvent("xmlFile")
		public Resolution saveFile()
		{
			Identity user = getCurrentUser();
			if(user == null)
				return (new ForwardResolution("/components/maybeError.jsp"));

			if(uploadFile(user))
            {
                getRequest().setAttribute("successful", "true");
                getRequest().setAttribute("file_name", "" + xmlFile.getFileName());
            }

			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		private boolean uploadFile(Identity usr)
		{
			if (usr != null && usr.isAdmin() && xmlFile != null)
			{
				try
				{
					String fileName = xmlFile.getFileName().toLowerCase();
					if(fileName.endsWith(".xml") || fileName.endsWith(".zip"))
					{
						//IwcmFile fileXml = new IwcmFile(Tools.getRealPath("/WEB-INF/tmp/"+fileName));
                        String path = "/WEB-INF/tmp/"; //NOSONAR
                       // String attributeWritePath = null;

                        if(getRequest().getAttribute("writePath") != null)
                        {
                            path = (String) getRequest().getAttribute("writePath");

//                            if (Tools.isNotEmpty(attributeWritePath) && attributeWritePath.startsWith("/archiv/"))
//                                path = attributeWritePath;
                        }

                        IwcmFile fileXml = new IwcmFile(Tools.getRealPath(path+fileName));
						if(fileXml.getParentFile().exists() == false)
							fileXml.mkdirs();

						File f = new File(Tools.getRealPath(path+fileName));
						Logger.debug(ConfImportAction.class, "Saving xmlFile to:"+f.getAbsolutePath());
						IwcmFsDB.writeFiletoDest(xmlFile.getInputStream(),f,(int)xmlFile.getSize());
						return true;
					}
					else
						Logger.debug(this, "uploadFileProcedure, file= "+xmlFile.getFileName()+" has bad extension.");
					return false;
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
					return false;
				}
			}
			else
				return false;
		}

		public FileBean getXmlFile()
		{
			return xmlFile;
		}

		public void setXmlFile(FileBean xmlFile)
		{
			this.xmlFile = xmlFile;
		}
}
