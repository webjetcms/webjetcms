package sk.iway.iwcm.filebrowser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.UrlRedirectDB;

/**
 *  DirectoryMover.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 7.9.2009 14:13:55
 *@modified     $Date: 2009/11/20 12:39:48 $
 */
class DirectoryMover
{
	
	HttpServletRequest request;
	
	String parentToMoveUnderPath;
	
	String whatToMovePath;
	
	IwcmFile parentToMoveUnder;
	
	IwcmFile whatToMove;

	public DirectoryMover setRequest(HttpServletRequest request)
	{
		this.request = request;
		return this;
	}

	public DirectoryMover setWhatToMove(String directoryPath)
	{
		this.whatToMovePath = directoryPath;
		return this;
	}
	
	public DirectoryMover setParentToMoveUnder(String directoryPath)
	{
		this.parentToMoveUnderPath = directoryPath;
		return this;
	}
	
	public void move()
	{
		if (Tools.isAnyEmpty(whatToMovePath, parentToMoveUnderPath))
			throw new IllegalArgumentException("Target or source directory not specified");
		
		parentToMoveUnder = new IwcmFile(Tools.getRealPath(parentToMoveUnderPath));
		whatToMove = new IwcmFile(Tools.getRealPath(whatToMovePath));
				
		boolean success = whatToMove.renameTo( new IwcmFile(parentToMoveUnder.getAbsolutePath(), whatToMove.getName() ));	
		
		if (success)
		{
			replaceDocumentLinks();
			replaceFulltextData();
			createRedirects(parentToMoveUnderPath + "/" + whatToMove.getName());
		}
	}

	private void replaceDocumentLinks()
	{
		String newPath = parentToMoveUnderPath + "/" + whatToMove.getName();
		String sql = "UPDATE documents SET external_link = REPLACE(external_link, ?, ?) WHERE external_link LIKE ?";
		new SimpleQuery().execute(sql, whatToMovePath, newPath, whatToMovePath+"%");
	}
	
	private void replaceFulltextData()
	{
		String newPath = parentToMoveUnderPath + "/" + whatToMove.getName();
		String sql = "UPDATE documents SET virtual_path = REPLACE(virtual_path, ?, ?) WHERE virtual_path LIKE ?";
		new SimpleQuery().execute(sql, whatToMovePath, newPath, whatToMovePath+"%");
		
		DocDB.getInstance(true);
		
		//presun aj adresar v strukture
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group = groupsDB.getGroupByPath(whatToMovePath);
		if (group != null)
		{
			GroupDetails newParent = groupsDB.getCreateGroup(parentToMoveUnderPath);
			if (newParent != null)
			{
				group.setParentGroupId(newParent.getGroupId());
				
				groupsDB.setGroup(group);
			}
		}
	}
	
	private void createRedirects(String dirUrl)
	{
		HttpSession session = null;
		if (request != null) session = request.getSession();
		
		IwcmFile rootDir = new IwcmFile( Tools.getRealPath(dirUrl) );
		if (rootDir.exists()==false) return;
		
		IwcmFile files[] = rootDir.listFiles();
		for (IwcmFile file : files)
		{
			if (file.isDirectory())
			{
				createRedirects(dirUrl+"/"+file.getName());
				continue;
			}
			
			if (session != null)
			{
				//je to subor, pridame redirect
				
				String newUrl = dirUrl+"/"+file.getName();
				String oldUrl = Tools.replace(newUrl, parentToMoveUnderPath+"/"+whatToMove.getName(), whatToMovePath);
				
				newUrl = Tools.replace(newUrl, "//", "/");
				oldUrl = Tools.replace(oldUrl, "//", "/");
				
				Logger.debug(DirectoryMover.class, "Adding redirect: "+oldUrl+"->"+newUrl);
				
				UrlRedirectDB.addRedirect(oldUrl, newUrl, "", 302);
			}
		}
		
	}
		
}