package sk.iway.iwcm.components.blog;


import sk.iway.UniquenessViolationException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BlogTools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  NewBloggerMaker.java
 *  
 *  Creates a new blogger
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.4.2011 11:43:41
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BloggerMaker
{
	public UserDetails makeNewBlogger(BloggerCreationData data) throws BlogGroupDoesntExistException, UniquenessViolationException
	{
		throwIfBlogGroupDoesntExist();
		UserDetails blogger = bloggerFromRequest(data);
		save(blogger);
		setRightsFor(blogger);
		return blogger;
	}
	
	private void throwIfBlogGroupDoesntExist() throws BlogGroupDoesntExistException
	{
		boolean exists = blogUserGroupId() != -1; 
		if (!exists)
			throw new BlogGroupDoesntExistException();
	}
	
	/*
	private void throwIfLoginOrEmailAlreadyTaken(BloggerCreationData data) throws UniquenessViolationException
	{
		UserDetails userByLogin = UsersDB.getUser(data.login); 
		if (userByLogin != null)
			throw new UniquenessViolationException("login", data.login);
		UserDetails userByEmail = UsersDB.getUserByEmail(data.email);
		if (userByEmail != null)
			throw new UniquenessViolationException("email", data.email);
	}
	*/

	private UserDetails bloggerFromRequest(BloggerCreationData data)
	{
		UserDetails blogger;
		UserDetails existingUser = UsersDB.getUserByEmail(data.email);
		if (existingUser != null) blogger = existingUser;
		else blogger = new UserDetails();
		
		//blogger.setAdmin(true);
		blogger.setAuthorized(true);
		blogger.setLogin(data.login);
		if (Tools.isNotEmpty(data.password) && "unchanged".equals(data.password)==false) blogger.setPassword(data.password);
		blogger.setLastName(data.surname);
		blogger.setFirstName(data.name);
		blogger.setEmail(data.email);
		if (Tools.isEmpty(blogger.getUserGroupsIds())) blogger.setUserGroupsIds(Integer.toString(blogUserGroupId()));
		else if ( (","+blogger.getUserGroupsIds()+",").indexOf(","+blogUserGroupId()+",")==-1) blogger.setUserGroupsIds(blogger.getUserGroupsIds()+","+Integer.toString(blogUserGroupId()));
		
		makeHisWriteableFolders(blogger);
		return blogger;
	}

	private void makeHisWriteableFolders(UserDetails blogger)
	{
		String login = blogger.getLogin();
		blogger.setWritableFolders(
					"/images/blog/"+login+"*"+System.getProperty("line.separator")+
					"/files/blog/"+login+"*"+System.getProperty("line.separator")+
					"/images/gallery/blog/"+login+"*"+System.getProperty("line.separator")
		);
	}

	private int blogUserGroupId()
	{
		return UserGroupsDB.getInstance().getUserGroupId("Blog");
	}

	private void save(UserDetails blogger)
	{
		UsersDB.saveUser(blogger);
	}
	
	private void setRightsFor(UserDetails blogger)
	{
		new BlogRightsSetter(blogger).setRights();
	}

	@Deprecated
	public static boolean isDeletable(DocDetails doc, Identity blogger)
	{
		return BlogTools.isEditable(doc.getGroupId(), blogger) && blogger.getUserId() == doc.getAuthorId();
	}

	@Deprecated
	public static boolean isEditable(int groupId, Identity blogger){
		return BlogTools.isEditable(groupId,blogger);
    }
}