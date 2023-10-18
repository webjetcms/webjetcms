package sk.iway.iwcm.components.blog;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * BloggerDefaultPagesMaker.java
 * 
 * Creates page directory structure and fills it with dummy pages
 * 
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: marosurbanec $
 *@version $Revision: 1.3 $
 *@created Date: 18.4.2011 12:58:02
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class BloggerDefaultPagesMaker
{
	private final UserDetails blogger;
	private final int parentGroupId;
	private final HttpServletRequest request;

	public BloggerDefaultPagesMaker(UserDetails blogger, HttpServletRequest request)
	{
		this.blogger = blogger;
		this.request = request;
		this.parentGroupId = Integer.parseInt(request.getParameter("parentDirectoryId"));
	}

	public void createDirectoryStructure()
	{		
		GroupDetails mainBlogFolder = createBloggersRootDirectory();
		if (mainBlogFolder != null)
		{
			restrictHisEditablePages(mainBlogFolder);
			createNewsComponentForHisBlog(mainBlogFolder);
			GroupDetails defaultFolder = createDefaultFolder(mainBlogFolder);
			createDefaultPage(defaultFolder);
			createDirectoriesBloggerCanAccess();
		}
	}

	private void createDirectoriesBloggerCanAccess()
	{
		File blogImages = new File(Tools.getRealPath("/images/blog/" + blogger.getLogin()));
		blogImages.mkdirs();
		File blogGalleryImages = new File(Tools.getRealPath("/images/gallery/blog/" + blogger.getLogin()));
		blogGalleryImages.mkdirs();
		File blogFiles = new File(Tools.getRealPath("/files/blog/" + blogger.getLogin()));
		blogFiles.mkdirs();
	}
	
	private GroupDetails createBloggersRootDirectory()
	{
		GroupDetails existingGroup = GroupsDB.getInstance().getGroup(blogger.getLogin(), parentGroupId);
		//existuje, nebudem vytvarat
		if (existingGroup != null) return null;
		
		GroupDetails mainBlogFolder = new GroupDetails();
		mainBlogFolder.setParentGroupId(parentGroupId);
		mainBlogFolder.setGroupName(blogger.getLogin());
		mainBlogFolder.setTempId(GroupsDB.getInstance().getGroup(parentGroupId).getTempId());
		mainBlogFolder.setFieldA(String.valueOf(blogger.getUserId()));
		GroupsDB.getInstance().setGroup(mainBlogFolder);
		return mainBlogFolder;
	}

	private void createDefaultPage(GroupDetails defaultFolder)
	{
		EditorForm defaultDoc = EditorDB.getEditorForm(request, -1, -1, defaultFolder.getGroupId());
		defaultDoc.setTitle(Prop.getInstance(request).getText("components.blog.default_page_title"));
		defaultDoc.setData(Prop.getInstance(request).getText("components.blog.default_page_text"));
		defaultDoc.setAuthorId(UsersDB.getUser(blogger.getLogin()).getUserId());
		defaultDoc.setAvailable(true);
		defaultDoc.setSearchable(true);
		defaultDoc.setCacheable(false);
		defaultDoc.setPublish("1");
		EditorDB.saveEditorForm(defaultDoc, request);
	}

	
	private void createNewsComponentForHisBlog(GroupDetails mainBlogFolder)
	{
		EditorForm wholeBlogNewsComponent = EditorDB.getEditorForm(request, -1, -1, mainBlogFolder.getGroupId());
		wholeBlogNewsComponent.setTitle(Prop.getInstance(request).getText("components.blog.articles_list") + " "
					+ request.getParameter("name") + " " + request.getParameter("surname"));
		wholeBlogNewsComponent.setNavbar(request.getParameter("name") + " " + request.getParameter("surname"));
		wholeBlogNewsComponent
					.setData("!INCLUDE(/components/blog/blog_news.jsp, groupIds="
								+ mainBlogFolder.getGroupId()
								+ ", orderType=date, asc=no, publishType=all, noPerexCheck=yes, paging=yes, pageSize=15, cols=1, image=none, perex=no, date=no, place=no,expandGroupIds=yes)!");
		wholeBlogNewsComponent.setAuthorId(((Identity) request.getSession().getAttribute(Constants.USER_KEY)).getUserId());
		wholeBlogNewsComponent.setAvailable(true);
		wholeBlogNewsComponent.setSearchable(true);
		wholeBlogNewsComponent.setCacheable(false);
		wholeBlogNewsComponent.setPublish("1");
		EditorDB.saveEditorForm(wholeBlogNewsComponent, request);
	}
	
	private GroupDetails createDefaultFolder(GroupDetails mainBlogFolder)
	{
		GroupDetails defaultFolder = new GroupDetails();
		defaultFolder.setParentGroupId(mainBlogFolder.getGroupId());
		defaultFolder.setGroupName(Prop.getInstance(request).getText("components.blog.default_group_name"));
		defaultFolder.setTempId(GroupsDB.getInstance().getGroup(parentGroupId).getTempId());
		GroupsDB.getInstance().setGroup(defaultFolder);
		// news komponenta pre default skupinu
		EditorForm defaultFolderNews = EditorDB.getEditorForm(request, -1, -1, defaultFolder.getGroupId());
		defaultFolderNews.setTitle(Prop.getInstance(request).getText("components.blog.articles_list") + " "
					+ defaultFolder.getGroupName());
		defaultFolderNews.setNavbar(defaultFolder.getGroupName());
		defaultFolderNews
					.setData("!INCLUDE(/components/blog/blog_news.jsp, groupIds="
								+ defaultFolder.getGroupId()
								+ ", orderType=date, asc=no, publishType=all, noPerexCheck=yes, paging=yes, pageSize=15, cols=1, image=none, perex=no, date=no, place=no,expandGroupIds=yes)!");
		defaultFolderNews.setAvailable(true);
		defaultFolderNews.setSearchable(true);
		defaultFolderNews.setCacheable(false);
		defaultFolderNews.setPublish("1");
		defaultFolderNews.setAuthorId(((Identity) request.getSession().getAttribute(Constants.USER_KEY)).getUserId());
		EditorDB.saveEditorForm(defaultFolderNews, request);
		return defaultFolder;
	}
	
	private void restrictHisEditablePages(GroupDetails mainBlogFolder)
	{
		blogger.setEditableGroups(Integer.toString(mainBlogFolder.getGroupId()));
		UsersDB.saveUser(blogger);
	}
}