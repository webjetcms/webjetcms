package sk.iway.iwcm.editor;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;

/**
 *  ImageEditorTools.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: thaber $
 *@version      $Revision: 1.1 $
 *@created      Date: Aug 17, 2009 2:32:09 PM
 *@modified     $Date: 2009/08/21 14:11:25 $
 */
public class ImageEditorTools
{
	public static List<String> getSizeTemplates()
	{
		List<String> result = new ArrayList<String>();
		
		String templates = Constants.getString("imageEditorSizeTemplates");
		
		for (String template : templates.split(";"))
		{
			result.add(template);
		}
		
		return result;
	}
}
