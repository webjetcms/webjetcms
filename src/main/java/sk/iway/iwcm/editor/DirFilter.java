package sk.iway.iwcm.editor;

import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFileFilter;


/**
 *  filter pre adresar (akceptuje iba adresare)
 *
 *@Title        iwcm
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1.1.1 $
 *@created      Sobota, 2002, máj 18
 *@modified     $Date: 2003/01/28 11:30:12 $
 */
public class DirFilter implements IwcmFileFilter
{
   private boolean invert=false;

   public DirFilter()
   {

   }

   public DirFilter(boolean invert)
   {
      this.invert=invert;
   }

   /**
    *  Description of the Method
    *
    *@param  dir  Description of the Parameter
    *@return      Description of the Return Value
   public boolean accept(File dir)
   {
      boolean ret=false;
      if (dir.isDirectory())
      {
         ret = true;
      }

      if (invert) ret = !ret;

      return (ret);
   }
    */
   
   @Override
	public boolean accept(IwcmFile dir)
   {
      boolean ret=false;
      if (dir.isDirectory())
      {
         ret = true;
      }

      if (invert) ret = !ret;

      return (ret);
   }
}
