package sk.iway.iwcm.editor;

import sk.iway.iwcm.common.DocTools;

/**
 *  Ulozi dokument do DB a na disk
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.38 $
 *@created      $Date: 2004/03/24 16:01:08 $
 *@modified     $Date: 2004/03/24 16:01:08 $
 */
public class SaveDocAction
{
	/**
	 *  vyhodi nepovolene znaky z nazvu suboru (zrusi aj znak /)
	 *  POZOR: DA to na LOWER CASE!!!
	 *
	 *@param  ret   Description of the Parameter
	 *@return       Description of the Return Value
	 @deprecated - use DocTools
	 */
	@Deprecated
	public static String removeChars(String ret)
	{
		return DocTools.removeChars(ret, true);
	}

	/**
	 * Vyhodi nepovolene znaky z nazvu suboru
	 * @param ret
	 * @param lowerCase - ak je nastavene na true, zmeni aj velkost na male pismena
	 * @return
	 * @deprecated - use DocTools
	 */
	@Deprecated
	public static String removeChars(String ret, boolean lowerCase)
	{
		return DocTools.removeChars(ret,lowerCase);
	}

	/**
	 *  Vyhodi nepovolene znaky z nazvu adresara (ponecha znak /)
	 *  POZOR: neda to na LOWER CASE!!!
	 *
	 *@param  ret   Description of the Parameter
	 *@return
	 @deprecated - use DocTools
	 */
	@Deprecated
	public static String removeCharsDir(String ret)
	{
		return DocTools.removeCharsDir(ret, true);
	}
	/**
	 * @deprecated - use DocTools
	 * @param ret
	 * @param removeSpojky
	 * @return
	 */
	@Deprecated
	public static String removeCharsDir(String ret, boolean removeSpojky)
	{
		return DocTools.removeCharsDir(ret,removeSpojky);
	}

}
