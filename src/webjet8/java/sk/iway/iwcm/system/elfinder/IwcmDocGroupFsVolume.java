package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import cn.bluejoe.elfinder.util.MimeTypesUtils;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DeleteServlet;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.users.UsersDB;

/**
 *  IwcmFsVolume.java - volume objekt pre elFinder (cn.bluejoe.elfinder)
 *  Zobrazuje strukturu web stranok
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.2.2015 19:09:46
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwcmDocGroupFsVolume implements FsVolume
{
	public static final String VOLUME_ID = "iwcm_doc_group_volume";

	protected String _name;

	public IwcmDocGroupFsVolume(String name)
	{
		this._name = name;
	}

	protected IwcmDocGroupItem asDocGroup(FsItem fsi)
	{
		return ((IwcmDocGroupItem) fsi);
	}

	private HttpServletRequest getRequest()
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null) return rb.getRequest();

		return null;
	}

	private boolean canWrite(int groupId)
	{
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user == null) return false;

		return GroupsDB.isGroupEditable(user, groupId);
	}

	@Override
	public void createFile(FsItem fsi) throws IOException
	{
		IwcmDocGroupItem item = asDocGroup(fsi);

		HttpServletRequest request = getRequest();
		if (item != null && item.getGroup()!=null && Tools.isNotEmpty(item.getGroup().getGroupName()) && item.getGroup().getGroupId()<1 && request!=null)
		{
			GroupDetails parentGroup = GroupsDB.getInstance().getGroup(item.getGroup().getParentGroupId());
			String newTitle = item.getGroup().getGroupName();

			//odstran txt priponu ktoru tam prida elfinder
			newTitle = Tools.replace(newTitle, ".txt", "");

			Identity user = UsersDB.getCurrentUser(request);
			if (user != null && user.isAdmin() && Tools.isNotEmpty(newTitle) && parentGroup!=null && parentGroup.getGroupId()>0 && canWrite(parentGroup.getGroupId()))
			{
				int newDocId = AdminTools.createWebPage(parentGroup, user, request, newTitle);
				if (newDocId > 0)
				{
					DocDetails doc = DocDB.getInstance().getBasicDocDetails(newDocId, false);
					if (doc != null)
					{
						item.setDoc(doc);
						item.setGroup(null);
					}
				}
			}
		}
	}

	@Override
	public void createFolder(FsItem fsi) throws IOException
	{
		//asFile(fsi).mkdirs();
		IwcmDocGroupItem item = asDocGroup(fsi);
		if (item != null && item.getGroup()!=null && Tools.isNotEmpty(item.getGroup().getGroupName()) && item.getGroup().getGroupId()<1 && canWrite(item.getGroup().getParentGroupId()))
		{
			try
			{
				String newGroupName = item.getGroup().getGroupName();

				GroupDetails ctxGroup = GroupsDB.getInstance().addNewGroup(newGroupName, item.getGroup().getParentGroupId());

				item.setGroup(ctxGroup);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	@Override
	public boolean deleteFile(FsItem fsi) throws IOException
	{
		boolean deleted = false;

		IwcmDocGroupItem item = asDocGroup(fsi);
		HttpServletRequest request = getRequest();
		if (item != null && item.getDoc()!=null && Tools.isNotEmpty(item.getDoc().getTitle()) && item.getDoc().getDocId()>0 && canWrite(item.getDoc().getGroupId()))
		{
			try
			{
				String status = DeleteServlet.deleteDoc(request, item.getDoc().getDocId());
				if ("success".equals(status)) deleted = true;
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		return deleted;
	}

	@Override
	public boolean deleteFolder(FsItem fsi) throws IOException
	{
		boolean deleted = false;

		IwcmDocGroupItem item = asDocGroup(fsi);
		HttpServletRequest request = getRequest();
		if (item != null && item.getGroup()!=null && Tools.isNotEmpty(item.getGroup().getGroupName()) && item.getGroup().getGroupId()>0 && canWrite(item.getGroup().getGroupId()))
		{
			deleted = GroupsDB.deleteGroup(item.getGroup().getGroupId(), request);
		}

		return deleted;
	}

	@Override
	public boolean exists(FsItem newFile)
	{
		return asDocGroup(newFile).exists();
	}

	/**
	 * Vyparsuje cislo z cesty /group:146 alebo /group:146/Novy adresar
	 * @param path
	 * @return
	 */
	private int getNumberFromPath(String path)
	{
		int dvojbodka = path.indexOf(":");
		int lomka = path.indexOf("/", dvojbodka);

		if (lomka < 1) return Tools.getIntValue(path.substring(dvojbodka+1), -1);
		else return Tools.getIntValue(path.substring(dvojbodka+1, lomka), -1);
	}

	/**
	 * Ak cesta obsahuje nazov noveho adresara vrati jeho hodnotu
	 * @param path - /group:146/Novy adresar
	 * @return Novy adresar alebo null
	 */
	private String getNewItemName(String path)
	{
		int dvojbodka = path.indexOf(":");
		int lomka = path.indexOf("/", dvojbodka);

		if (lomka > dvojbodka && lomka > 1) return path.substring(lomka+1);
		return null;
	}

	protected IwcmDocGroupItem fromDoc(String path)
	{
		DocDetails doc = null;

		if (path.startsWith("/doc:"))
		{
			int docId = getNumberFromPath(path);
			if (docId > 0)
			{
				doc = DocDB.getInstance().getBasicDocDetails(docId, false);
			}
		}

		return new IwcmDocGroupItem(this, doc);
	}

	protected IwcmDocGroupItem fromGroup(String path)
	{
		GroupDetails group = null;

		if (path.startsWith("/group:"))
		{
			int groupId = getNumberFromPath(path);
			if (groupId > 0)
			{
				group = GroupsDB.getInstance().getGroup(groupId);
			}
		}

		if (group != null)
		{
			try
			{
				String newFolderName = getNewItemName(path);
				if (Tools.isNotEmpty(newFolderName))
				{
					GroupDetails newGroup = GroupsDB.getInstance().getGroup(newFolderName, group.getGroupId());
					if (newGroup == null)
					{
						newGroup = new GroupDetails();
						newGroup.setGroupId(-1);
						newGroup.setParentGroupId(group.getGroupId());
						newGroup.setGroupName(newFolderName);
					}

					group = newGroup;
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		return new IwcmDocGroupItem(this, group);
	}

	@Override
	public FsItem fromPath(String relativePath)
	{
		if (relativePath.startsWith("/doc:")) return fromDoc(relativePath);
		else return fromGroup(relativePath);
	}

	@Override
	public String getDimensions(FsItem fsi)
	{
		return null;
	}

	@Override
	public long getLastModified(FsItem fsi)
	{
		long lastModified = asDocGroup(fsi).lastModified();
		//prepocet na JavaScript timestamp
		lastModified = Math.round((double)lastModified / 1000);

		return lastModified;
	}

	@Override
	public String getMimeType(FsItem fsi)
	{
		IwcmDocGroupItem item = asDocGroup(fsi);
		if (item.getGroup()!=null)
			return "directory";

		//JEEFF String ext = FileUtils.getExtension(file.getName());
		String ext = "html";
		String mimeType = MimeTypesUtils.getMimeType(ext);
		return mimeType == null ? MimeTypesUtils.UNKNOWN_MIME_TYPE : mimeType;
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public String getName(FsItem fsi)
	{
		IwcmDocGroupItem item = asDocGroup(fsi);
		return item.getName();
	}

	@Override
	public FsItem getParent(FsItem fsi)
	{
		IwcmDocGroupItem item = asDocGroup(fsi);

		if (item.getGroup()!=null)
		{
			//int groupId = item.getGroupId();
			GroupDetails group = item.getGroup(); //  GroupsDB.getInstance().getGroup(groupId);
			if (group != null && group.getParentGroupId()>0)
			{
				GroupDetails parentGroup = GroupsDB.getInstance().getGroup(group.getParentGroupId());
				if (parentGroup != null)
				{
					return new IwcmDocGroupItem(this, parentGroup);
				}
			}
		}
		else if (item.getDoc()!=null)
		{
			int groupId = item.getDoc().getGroupId();
			GroupDetails group = GroupsDB.getInstance().getGroup(groupId);
			if (group != null)
			{
				return new IwcmDocGroupItem(this, group);
			}
		}

		return getRoot();
	}

	@Override
	public String getPath(FsItem fsi) throws IOException
	{
		IwcmDocGroupItem item = asDocGroup(fsi);

		return item.getPath();
	}

	public int getSortPriority(FsItemEx fsi) throws IOException
	{
		IwcmDocGroupItem item = asDocGroup(fromPath(fsi.getPath()));

		return item.getSortPriority();
	}

	@Override
	public FsItem getRoot()
	{
		GroupDetails fakeRootGroup = new GroupDetails();
		fakeRootGroup.setGroupId(0);
		fakeRootGroup.setGroupName("FAKE ROOT GROUP");

		return new IwcmDocGroupItem(this, fakeRootGroup);
	}

	@Override
	public long getSize(FsItem fsi)
	{
		//vraciame sort priority aby sa dalo usporiadat podla velkosti ako podla priority
		return asDocGroup(fsi).getSortPriority();
	}

	@Override
	public String getThumbnailFileName(FsItem fsi)
	{
		return null;
	}

	@Override
	public boolean hasChildFolder(FsItem fsi)
	{
		IwcmDocGroupItem item = asDocGroup(fsi);

		int groupId = item.getGroupId();

		List<GroupDetails> subGroups = GroupsDB.getInstance().getGroups(groupId);

		if (subGroups.size()>0) return true;

		return false;
	}

	@Override
	public boolean isFolder(FsItem fsi)
	{
		IwcmDocGroupItem item = asDocGroup(fsi);

		if (item.getGroup()!=null) return true;

		return false;
	}

	@Override
	public boolean isRoot(FsItem fsi)
	{
		IwcmDocGroupItem item = asDocGroup(fsi);

		if (item.getGroup()!=null && item.getGroup().getGroupId()<1) return true;

		return false;
	}

	@Override
	public FsItem[] listChildren(FsItem fsi)
	{
		HttpServletRequest request = getRequest();
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin()==false) return new FsItem[0];

		List<FsItem> list = new ArrayList<FsItem>();

		IwcmDocGroupItem item = asDocGroup(fsi);

		Logger.debug(IwcmDocGroupFsVolume.class, "listChildrens, virtualPath="+item.getPath());

		if (item.getDoc()==null)
		{
			List<GroupDetails> subGroups;

			if (item.getGroupId()==0)
			{
				subGroups = new ArrayList<GroupDetails>();
				List<GroupDetails> subGroupsAll = GroupsDB.getInstance().getRootGroups(user.getEditableGroups());
				for (GroupDetails group : subGroupsAll)
				{
					if (Constants.getBoolean("multiDomainEnabled")) {
						//check domain name
						String currentDomain = CloudToolsForCore.getDomainName();
						if (Tools.isNotEmpty(currentDomain) && currentDomain.equals(group.getDomainName())==false) continue;
					}

					if (group.getParentGroupId()>0)
					{
						//musime naklonovat a nastavit parenta na 0, inak sa nezobrazi
						GroupDetails clone = new GroupDetails(group);
						clone.setParentGroupId(0);
						subGroups.add(clone);
					}
					else
					{
						subGroups.add(group);
					}
				}
			}
			else
			{
				subGroups = GroupsDB.getInstance().getGroups(item.getGroupId());
			}

			for (GroupDetails group : subGroups)
			{
				//skip full text index folders
				if ("images".equalsIgnoreCase(group.getGroupName()) || "files".equalsIgnoreCase(group.getGroupName()))
				{
					continue;
				}

				list.add(new IwcmDocGroupItem(this, group));
			}

			List<DocDetails> subDocs = DocDB.getInstance().getBasicDocDetailsByGroup(item.getGroupId(), DocDB.ORDER_PRIORITY);
			for (DocDetails doc : subDocs)
			{
				list.add(new IwcmDocGroupItem(this, doc));
			}
		}

		if (list.size()<1) return new FsItem[0];

		return list.toArray(new FsItem[0]);
	}

	@Override
	public IwcmInputStream openInputStream(FsItem fsi) throws IOException
	{
		//TODO: NOT IMPLEMENTED YET
		//return new IwcmInputStream(asFile(fsi));
		return null;
	}

	@Override
	public IwcmOutputStream openOutputStream(FsItem fsi) throws IOException
	{
		//TODO: NOT IMPLEMENTED YET
		//return new IwcmOutputStream(asFile(fsi));
		return null;
	}

	@Override
	public void rename(FsItem src, FsItem dst) throws IOException
	{
		IwcmDocGroupItem itemSrc = asDocGroup(src);
		IwcmDocGroupItem itemDst = asDocGroup(dst);

		HttpServletRequest request = getRequest();
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin()==false || user.isDisabledItem("editDir")) return;

		String newName = null;
		if (itemDst!=null && itemDst.getGroup()!=null) newName = itemDst.getGroup().getGroupName();

		if (itemSrc.getDoc()!=null && itemSrc.getDoc().getDocId()>0 && Tools.isNotEmpty(newName))
		{
			int ctxDocId = itemSrc.getDoc().getDocId();

			EditorForm editorForm = EditorDB.getEditorForm(request, ctxDocId, -1, -1);
			if (editorForm != null)
			{
				editorForm.setAuthorId(user.getUserId());
				editorForm.setTitle(newName);
				editorForm.setNavbar(newName);
				editorForm.setPublish("1");
				editorForm.setVirtualPath("");
				EditorDB.saveEditorForm(editorForm, request);

				EditorDB.cleanSessionData(request);

				int newDocId = editorForm.getDocId();
				DocDetails doc = DocDB.getInstance().getBasicDocDetails(newDocId, false);
				if (doc != null && itemDst!=null)
				{
					itemDst.setDoc(doc);
					itemDst.setGroup(null);
				}
			}

			return;
		}

		if (itemSrc.getGroup()!=null && itemSrc.getDoc()==null && itemSrc.getGroup().getGroupId()>0 && Tools.isNotEmpty(newName))
		{
			itemSrc.getGroup().setGroupName(newName);
			itemSrc.getGroup().setNavbar(newName);
			GroupsDB.getInstance().setGroup(itemSrc.getGroup());

			if (itemDst!=null) itemDst.setGroup(itemSrc.getGroup());
		}
	}

	public void setName(String name)
	{
		_name = name;
	}

	protected void createAndCopyGroups(FsItemEx src, FsItemEx dst) throws IOException
	{
		//dst.createFolder();
		//tu je to trocha ine ako povodne, dst je existujuci folder do ktoreho kopirujeme, nie uz ten akoze novy (este neexistujuci)
		IwcmDocGroupItem itemSrc = asDocGroup(fromPath(src.getPath()));
		IwcmDocGroupItem itemDst = asDocGroup(fromPath(dst.getPath()));

		if (itemSrc.getDoc()!=null || itemSrc.getGroup()==null || itemSrc.getGroup().getGroupId()<1) return;
		if (itemDst.getDoc()!=null || itemDst.getGroup()==null || itemDst.getGroup().getGroupId()<1) return;

		GroupDetails newGroup = new GroupDetails();
		newGroup.setParentGroupId(itemDst.getGroup().getGroupId());
		newGroup.setGroupName(itemSrc.getGroup().getGroupName());

		IwcmDocGroupItem itemNewGroup = new IwcmDocGroupItem(this, newGroup);
		createFolder((FsItem)itemNewGroup);

		for (FsItemEx c : src.listChildren())
		{
			if (c.isFolder())
			{
				FsItemEx newDest = new FsItemEx((FsItem)itemNewGroup, src.getService());
				createAndCopyGroups(c, newDest);
			}
			else
			{
				//createAndCopyDocs(c, new FsItemEx(dst, itemSrc.getGroup().getGroupName()));
				IwcmDocGroupItem docSrc = asDocGroup(fromPath(c.getPath()));
				moveOrCopyPageToFolder(docSrc.getDoc(), itemNewGroup.getGroupId(), true);
			}
		}
	}

	protected void createAndCopyDocs(FsItemEx src, FsItemEx dst) throws IOException
	{
		IwcmDocGroupItem itemSrc = asDocGroup(fromPath(src.getPath()));
		IwcmDocGroupItem itemDst = asDocGroup(fromPath(dst.getPath()));

		if (itemSrc.getDoc()==null || itemSrc.getDoc().getDocId()<1 || Tools.isEmpty(itemSrc.getDoc().getTitle())) return;
		if (itemDst.getDoc()!=null || itemDst.getGroup()==null || itemDst.getGroup().getGroupId()<1) return;

		//skopiruj stranku do dest foldra
		moveOrCopyPageToFolder(itemSrc.getDoc(), itemDst.getGroupId(), true);
	}

	/**
	 * JEEFF: doplnena metoda volana z pasteCommandExecutoru pre skopirovanie dat
	 * @param src
	 * @param dst
	 * @param cut
	 * @throws IOException
	 */
	public void createAndCopy(FsItemEx src, FsItemEx dst, boolean cut) throws IOException
	{
		if (cut)
		{
			//ked robime move staci nam vo WebJETe zmenit ID parenta
			IwcmDocGroupItem itemSrc = asDocGroup(fromPath(src.getPath()));
			IwcmDocGroupItem itemDst = asDocGroup(fromPath(dst.getPath()));

			if (itemSrc.getDoc()!=null)
			{
				if (itemDst.getDoc()==null && itemDst.getGroup()!=null && itemDst.getGroup().getGroupId()>0)
				{
					//presun stranku do adresara dst
					moveOrCopyPageToFolder(itemSrc.getDoc(), itemDst.getGroup().getGroupId(), false);
					return;
				}
			}
			if (itemSrc.getDoc()==null && itemSrc.getGroup()!=null)
			{
				if (itemDst.getDoc()==null && itemDst.getGroup()!=null && itemDst.getGroup().getGroupId()>0)
				{
					moveFolderToFolder(itemSrc.getGroup(), itemDst.getGroup().getGroupId());
					return;
				}
			}
		}
		else
		{
			if (src.isFolder())
			{
				createAndCopyGroups(src, dst);
			}
			else
			{
				createAndCopyDocs(src, dst);
			}
		}
	}

	/**
	 * Presunie web stranku do daneho adresara
	 * @param doc
	 * @param newGroupId
	 */
	private void moveOrCopyPageToFolder(DocDetails doc, int newGroupId, boolean copy)
	{
		HttpServletRequest request = getRequest();
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin()==false || user.isDisabledItem("editDir")) return;

		EditorForm editorForm = EditorDB.getEditorForm(request, doc.getDocId(), -1, -1);
		if (editorForm != null)
		{
			editorForm.setAuthorId(user.getUserId());
			editorForm.setGroupId(newGroupId);
			editorForm.setVirtualPath("");
			editorForm.setPublish("1");
			if (copy) editorForm.setDocId(-1);

			EditorDB.saveEditorForm(editorForm, request);
			EditorDB.cleanSessionData(request);
		}
	}

	/**
	 * Presunie adresar do noveho adresara
	 * @param group
	 * @param destinationGroupId
	 */
	private void moveFolderToFolder(GroupDetails group, int destinationGroupId)
	{
		HttpServletRequest request = getRequest();
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin()==false || user.isDisabledItem("editDir")) return;

		GroupDetails src = GroupsDB.getInstance().getGroup(group.getGroupId());
		src.setParentGroupId(destinationGroupId);
		GroupsDB.getInstance().save(src);
	}

	/**
	 * Vrati URL adresu pre stranku / adresar
	 * @param item
	 * @return
	 */
	public String getVirtualPath(FsItemEx item) throws IOException
	{
		IwcmDocGroupItem itemSrc = asDocGroup(fromPath(item.getPath()));

		int docId = -1;
		String externalLink = null;
		if (itemSrc != null && itemSrc.getDoc()!=null)
		{
			docId = itemSrc.getDoc().getDocId();
			//we want to use real URL without redirect if the redirect will be changed in future SO DO NOT USE externalLink = itemSrc.getDoc().getExternalLink();
		}
		else if (itemSrc != null && itemSrc.getGroup()!=null)
		{
			docId = itemSrc.getGroup().getDefaultDocId();
		}

		HttpServletRequest request = getRequest();

		DocDB docDB = DocDB.getInstance();
		return docDB.getDocLink(docId, externalLink, request);
	}
}