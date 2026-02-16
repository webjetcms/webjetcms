package sk.iway.iwcm.doc;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;


/**
 *  Drzi zaznam o vysledku vyhladavania
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2003/11/03 17:25:29 $
 */
public class SearchDetails extends DocDetails
{
   private String link;
   private String dataOriginal;

   /**
    * Vrati ikonu suboru alebo prazdny retazes ak sa nejedna o subor
    * @return
    */
   public String getFileIconImg()
   {
   	if (getExternalLink().startsWith("/files/") == false) return "";
  		String icon = FileTools.getFileIcon(super.getExternalLink());
  		return "<img class='fileIcon' alt='' src='"+icon+"'/> ";
   }

   /**
    * Vrati info o subore vo formate PDF, 134,54 kB alebo prazdny retazec ak sa nejedna o subor
    * @return
    */
   public String getFileInfo()
   {
   	if (getExternalLink().startsWith("/files/") == false) return "";

   	String fileSize = FileTools.getFileLength(getExternalLink(), false);
   	String ext = "";
   	try
		{
   		String url = getExternalLink();
			ext = url.substring(url.lastIndexOf('.') + 1);
			ext = ext.trim().toUpperCase();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		if (Tools.isEmpty(fileSize)) return ext;
		return ext+", "+fileSize;
   }

   /**
    * Vrati upraveny titulok stranky - ak sa jedna o subor prida ikonu a velkost suboru
    */
   @Override
   public String getTitle()
   {
   	String fileInfo = getFileInfo();
   	if (Tools.isNotEmpty(fileInfo))
   	{
   		fileInfo = " <span class='fileInfo'>("+fileInfo+")</span>";
   	}
   	return getFileIconImg()+getTitleHuman()+fileInfo;
   }

   /**
    * Vrati povodny neupraveny title stranky
    * @return
    */
   public String getTitleOriginal()
   {
   	return super.getTitle();
   }

   /**
    * Vrati upraveny titulok suboru - nahradi znaky _ - za medzeru, ak sa nejedna o subor ponecha ako je
    * @return
    */
   public String getTitleHuman()
   {
   	String title = super.getTitle();
   	if (getExternalLink().startsWith("/files/") == false) return title;
   	title = title.replace('_', ' ');
   	title = title.replace('-', ' ');
   	try
		{
			String externalLink = getExternalLink();
			int externalLinkIndex = externalLink.lastIndexOf('.') + 1;
			String externalLinkExtension = externalLink.substring(externalLinkIndex);

			int titleIndex = title.lastIndexOf('.') + 1;
			String titleExtension = title.substring(titleIndex);

			if (titleExtension.equalsIgnoreCase(externalLinkExtension)) {
				int i = title.lastIndexOf('.');
				if (i>0 && title.charAt(i+1) != ' ') title = title.substring(0, i);

				//prve pismeno velke
				title = (Character.toUpperCase(title.charAt(0))) +title.substring(1);
			}
		}
		catch (Exception e)
		{

		}

   		return title;
   }

   /**
    *  Gets the doc_id attribute of the SearchDetails object
    *
    *@return    The doc_id value
    */
   public int getDoc_id()
   {
      return getDocId();
   }

   /**
    *  Sets the doc_id attribute of the SearchDetails object
    *
    *@param  newDoc_id  The new doc_id value
    */
   public void setDoc_id(int newDoc_id)
   {
      setDocId(newDoc_id);
   }

   /**
    *  Sets the link attribute of the SearchDetails object
    *
    *@param  link  The new link value
    */
   public void setLink(String link)
   {
      this.link = link;
   }

   /**
    *  Gets the link attribute of the SearchDetails object
    *
    *@return    The link value
    */
   public String getLink()
   {
      return link;
   }

	public String getDataOriginal()
	{
		return dataOriginal;
	}

	public void setDataOriginal(String dataOriginal)
	{
		this.dataOriginal = dataOriginal;
	}
}
