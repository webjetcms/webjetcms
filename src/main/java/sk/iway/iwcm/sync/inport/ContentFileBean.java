package sk.iway.iwcm.sync.inport;

/**
 *  ContentFileBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.6.2012 16:43:38
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContentFileBean
{

	public final static int STATUS_MISSING = 0;
	public final static int STATUS_NEWER = 1;
	public final static int STATUS_OLDER = 2;
	public final static int STATUS_CURRENT_DIFFERENT = 3;
	public final static int STATUS_CURRENT_SAME = 4;

	private int number;
	private String path;
	private int status;
	private boolean selected;

	public ContentFileBean(int number, String path, int status)
	{
		this.number = number;
		this.path = path;
		this.status = status;
		this.selected = !((STATUS_OLDER == status) || (STATUS_CURRENT_SAME == status));
	}

	public int     getNumber () { return number  ; }
	public String  getPath   () { return path    ; }
	public boolean isSelected() { return selected; }

	public String getLocalPath()
	{
		return (STATUS_MISSING == status) ? "" : path;
	}

	public boolean isMissing         () { return STATUS_MISSING           == status; }
	public boolean isNewer           () { return STATUS_NEWER             == status; }
	public boolean isOlder           () { return STATUS_OLDER             == status; }
	public boolean isCurrentDifferent() { return STATUS_CURRENT_DIFFERENT == status; }
	public boolean isCurrentSame     () { return STATUS_CURRENT_SAME      == status; }

}
