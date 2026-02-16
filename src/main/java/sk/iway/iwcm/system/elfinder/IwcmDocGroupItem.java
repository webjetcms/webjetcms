package sk.iway.iwcm.system.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;

/**
 *  IwcmFsItem.java - file objekt pre elFinder (cn.bluejoe.elfinder)
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.2.2015 19:11:16
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwcmDocGroupItem implements FsItem
{
	DocDetails _doc;
	GroupDetails _group;

	FsVolume _volumn;

	public IwcmDocGroupItem(IwcmDocGroupFsVolume volumn, DocDetails doc)
	{
		super();
		_volumn = volumn;
		_doc = doc;
	}
	
	public IwcmDocGroupItem(IwcmDocGroupFsVolume volumn, GroupDetails group)
	{
		super();
		_volumn = volumn;
		_group = group;
	}

	public FsVolume getVolume()
	{
		return _volumn;
	}

	public void setVolumn(FsVolume volumn)
	{
		_volumn = volumn;
	}

	public DocDetails getDoc()
	{
		return _doc;
	}

	public void setDoc(DocDetails doc)
	{
		this._doc = doc;
	}

	public GroupDetails getGroup()
	{
		return _group;
	}

	public void setGroup(GroupDetails group)
	{
		this._group = group;
	}

	public boolean exists()
	{
		if (_doc != null || _group != null) return true;
		return false;
	}

	public long lastModified()
	{
		if (_doc != null) return _doc.getDateCreated();
		
		return 0;
	}
	
	public String getName()
	{
		if (_doc != null) return _doc.getTitle();
		else if (_group != null) return _group.getGroupName();
					
		return null;
	}
	
	public int getGroupId()
	{
		int groupId = 0;
		
		if (getDoc()!=null) groupId = getDoc().getGroupId();
		else if (getGroup()!=null) groupId = getGroup().getGroupId();
		
		return groupId;
	}
	
	public String getPath()
	{
		if (getDoc()!=null) return "/doc:"+getDoc().getDocId();
		else if (getGroup()!=null) return "/group:"+getGroup().getGroupId();
		
		return "/unknown";
	}
	
	public int getSortPriority()
	{
		if (getDoc() != null) {
			return getDoc().getSortPriority();
		}
		else if (getGroup() != null) {
			return getGroup().getSortPriority();
		}
		
		return -1;
	}
}