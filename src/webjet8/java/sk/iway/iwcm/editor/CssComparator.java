package sk.iway.iwcm.editor;

import java.io.Serializable;
import java.util.Comparator;

import sk.iway.iwcm.LabelValueDetails;

public class CssComparator implements Comparator<LabelValueDetails>, Serializable
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -3264241819874718487L;
	@Override
	public int compare(LabelValueDetails css1, LabelValueDetails css2)
   {
      try
      {
         String s1 = css1.getLabel();
         String s2 = css2.getLabel();
         return s1.compareTo(s2);
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
         return 0;
      }
   }
}