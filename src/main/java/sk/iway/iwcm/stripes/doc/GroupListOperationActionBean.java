package sk.iway.iwcm.stripes.doc;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DeleteServlet;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.users.UsersDB;

/**
 *  GroupListOperationActionBean.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 1.7.2008 11:00:55
 *@modified     $Date: 2009/09/11 06:52:05 $
 */

public class GroupListOperationActionBean implements ActionBean
{
	private ActionBeanContext context;
	private Integer grouplist_operacie;
	private String operacieId;
	private String newGroupId;
	private boolean available;
	private Integer tempId = -1;
	private boolean applyToAllDocs;
	@Override
	public ActionBeanContext getContext()
	{
		return context;
	}
	@Override
	public void setContext(ActionBeanContext context)
	{
		this.context = context;
	}

	public Integer getGrouplist_operacie()
	{
		return grouplist_operacie;
	}

	public void setGrouplist_operacie(Integer grouplist_operacie)
	{
		this.grouplist_operacie = grouplist_operacie;
	}

	public String getOperacieId()
	{
		return operacieId;
	}

	public void setOperacieId(String operacieId)
	{
		this.operacieId = operacieId;
	}


	public String getNewGroupId() {
		return newGroupId;
	}

	public void setNewGroupId(String newGroupId) {
		this.newGroupId = newGroupId;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public Integer getTempId() {
		return tempId;
	}

	public void setTempId(Integer tempId) {
		this.tempId = tempId;
	}

	public boolean isApplyToAllDocs()
	{
		return applyToAllDocs;
	}

	public void setApplyToAllDocs(boolean applyToAllDocs)
	{
		this.applyToAllDocs = applyToAllDocs;
	}

	/**
	 * ziska aktualny adresar
	 * @return
	 */
	private int getGroupId()
	{
		int groupId = Constants.getInt("rootGroupId");
		try
		{
			if (context.getRequest().getParameter("groupid") != null)
				groupId = Tools.getIntValue(context.getRequest().getParameter("groupid"), -1);
			else
			{
				//skus ziskat data zo session
				if (context.getRequest().getSession().getAttribute(Constants.SESSION_GROUP_ID) != null)
					groupId = Tools.getIntValue((String)context.getRequest().getSession().getAttribute(Constants.SESSION_GROUP_ID), -1);
			}
		}
		catch (Exception ex){}
		return groupId;
	}

	/**
	 * nastavi hodnoty (Object... arguments) stlpcov (String[] columns) pre stranky (List<DocDetails> docList)
	 * @param user
	 * @param docList
	 * @param columns
	 * @param arguments
	 * @return
	 */
	private boolean updateDocFields(Identity user, List<DocDetails> docList, String[] columns, Object... arguments)
	{
		boolean ok = true;

		try
		{
			StringBuilder sql = new StringBuilder("");
			if(docList != null && docList.size() > 0)
			{
				sql.append("UPDATE documents SET");
				boolean first = true;
				for(String c : columns)
				{
					sql.append(" "+(!first ? ", " : "")+c+" = ?");
					if(first)
						first = false;
				}
				sql.append(" WHERE doc_id = ?");
				Object[] newArguments = new Object[arguments.length+1];
				for(int i=0; i<arguments.length; i++)
					newArguments[i] = arguments[i];
				for (DocDetails dd : docList)
				{
					newArguments[arguments.length] = Integer.valueOf(dd.getDocId());
					EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), dd.getDocId(), -1, -1);
					if (editorForm != null && EditorDB.isPageEditable(user, editorForm))
					{
						DB.execute(sql.toString(), newArguments);
						Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+dd.getDocId()+"): Zmena stlpcov stranky "+Arrays.toString(columns)+" na "+Arrays.toString(arguments), dd.getDocId(), -1);
					}
				}
			}
		}
		catch (Exception e)
		{
			ok = false;
		}

		return ok;
	}

	/**
	 * nastavi hodnotu (value) stlpca (column) v tabulke documents pre vsetky id[]
	 * @param id
	 * @param column
	 * @param value
	 * @return
	 */
	private boolean updateDocFields(Identity user, String id[], String column, Object value)
	{
		boolean ok = true;

		try
		{
			String sql = "";
			for (int i = 0; i < id.length; i++)
			{
				int idTmp = Tools.getIntValue(id[i], -1);
				if(idTmp != -1)
				{
					EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), idTmp, -1, -1);
					if (EditorDB.isPageEditable(user, editorForm))
					{
						sql = "UPDATE documents SET "+column+" = ? WHERE doc_id = ?";
						DB.execute(sql, value, Integer.valueOf(editorForm.getDocId()));

						Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+idTmp+"): Zmena stlpca stranky "+column+" na "+value, idTmp, -1);
					}
				}
			}
		}
		catch (Exception e)
		{
			ok = false;
		}

		return ok;
	}

	@DefaultHandler
	@HandlesEvent("gloperacie")
	public Resolution gloperacie(){
		try
		{
			Identity currUser = UsersDB.getCurrentUser(context.getRequest().getSession());

			if (currUser == null || currUser.isAdmin()==false)
			{
				return(new ForwardResolution("/components/maybeError.jsp"));
			}

			//aby sa nezmazali rozpracovane verzie stranky pri posune hore/dole atd
			context.getRequest().setAttribute("doNotDeleteHistoryOnPublish", "true");

			if(Tools.isNotEmpty(this.operacieId))
			{
				String[] id = this.operacieId.split(",");
				if(this.grouplist_operacie == 0)
				{
					//vymazanie stranok

					//kontrola prav
					if(currUser.isDisabledItem("deletePage"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					String deleteOK = null;
					if(id != null)
					{
						for (int i = 0; i < id.length; i++) {
							int idTmp = Tools.getIntValue(id[i], -1);
							if(idTmp != -1)
							{
								deleteOK = DeleteServlet.deleteDoc(context.getRequest(), idTmp);
								if("success".equals(deleteOK) == false)
									break;
								else
									Adminlog.add(Adminlog.TYPE_PAGE_DELETE, "(DocID: "+idTmp+"): Stranka vymazana", idTmp, 0);
							}
						}
					}

					if("success".equals(deleteOK)){
						context.getRequest().setAttribute("Ok", "");
					}
				}
				else if(this.grouplist_operacie == 1 && this.tempId != -1)
				{
					//zmena sablony stranok

					//kontrola prav
					if(currUser.isDisabledItem("addPage") || currUser.isEnabledItem("editorMiniEdit"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					if(id != null)
					{
						int groupId = getGroupId();
						//ak aplikujem na vsetky stranky vsetkych podadresaroch
						if(applyToAllDocs && groupId != -1)
						{
							GroupsDB groupsDB = GroupsDB.getInstance();
							if(groupsDB != null)
							{
								List<GroupDetails> groupsTree = groupsDB.getGroupsTree(groupId, false, true);
								if(groupsTree != null && groupsTree.size() > 0)
								{
									DocDB docDb = DocDB.getInstance();
									for(GroupDetails gd : groupsTree)
									{
										List<DocDetails> docList = docDb.getBasicDocDetailsByGroup(gd.getGroupId(),DocDB.ORDER_TITLE);
										updateDocFields(currUser, docList, Tools.getTokens("author_id,temp_id",",", true), Integer.valueOf(currUser.getUserId()), this.tempId);
									}
									DocDB.getInstance(true);
								}
							}
						}
						for (int i = 0; i < id.length; i++) {
							int idTmp = Tools.getIntValue(id[i], -1);
							if(idTmp != -1)
							{
								EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), idTmp, -1, -1);
								if (editorForm != null)
								{
									editorForm.setAuthorId(currUser.getUserId());
									editorForm.setTempId(this.tempId);
									editorForm.setPublish("1");
									EditorDB.saveEditorForm(editorForm, context.getRequest());

									EditorDB.cleanSessionData(context.getRequest());

									Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+idTmp+"): Zmenena sablona na "+this.tempId, idTmp, -1);
								}
							}
						}
					}
					context.getRequest().setAttribute("Ok", "");
				}
				else if(this.grouplist_operacie == 2 && this.available == false)
				{
					//vypnutie zobrazovania stranok

					//kontrola prav
					if(currUser.isDisabledItem("addPage") || currUser.isEnabledItem("editorMiniEdit"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					if(id != null)
					{
						int groupId = getGroupId();
						//ak aplikujem na vsetky stranky vsetkych podadresaroch
						if(applyToAllDocs && groupId != -1)
						{
							GroupsDB groupsDB = GroupsDB.getInstance();
							if(groupsDB != null)
							{
								List<GroupDetails> groupsTree = groupsDB.getGroupsTree(groupId, false, true);
								if(groupsTree != null && groupsTree.size() > 0)
								{
									DocDB docDb = DocDB.getInstance();
									for(GroupDetails gd : groupsTree)
									{
										List<DocDetails> docList = docDb.getBasicDocDetailsByGroup(gd.getGroupId(),DocDB.ORDER_TITLE);
										updateDocFields(currUser, docList, Tools.getTokens("author_id,available",",", true), Integer.valueOf(currUser.getUserId()), Boolean.FALSE);
									}
									DocDB.getInstance(true);
								}
							}
						}
						for (int i = 0; i < id.length; i++) {
							int idTmp = Tools.getIntValue(id[i], -1);
							if(idTmp != -1)
							{
								EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), idTmp, -1, -1);
								if (editorForm != null)
								{
									editorForm.setAuthorId(currUser.getUserId());
									editorForm.setAvailable(false);
									editorForm.setPublish("1");
									EditorDB.saveEditorForm(editorForm, context.getRequest());

									EditorDB.cleanSessionData(context.getRequest());

									Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+idTmp+"): Vypnute zobrazovanie stranky", idTmp, -1);
								}
							}
						}
					}
					context.getRequest().setAttribute("Ok", "");
				}
				else if(this.grouplist_operacie == 3 && Tools.isNotEmpty(this.newGroupId))
				{
					//presun stranok do ineho adresara

					//kontrola prav
					if(currUser.isDisabledItem("addPage") || currUser.isEnabledItem("editorMiniEdit"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					this.newGroupId = this.newGroupId.trim();
					if(id != null)
					{
						for (int i = 0; i < id.length; i++) {
							int idTmp = Tools.getIntValue(id[i], -1);
							if(idTmp != -1)
							{
								EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), idTmp, -1, -1);
								if (editorForm != null)
								{
									editorForm.setAuthorId(currUser.getUserId());
									editorForm.setGroupId(Integer.parseInt(this.newGroupId));
									editorForm.setPublish("1");
									EditorDB.saveEditorForm(editorForm, context.getRequest());

									EditorDB.cleanSessionData(context.getRequest());

									Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+idTmp+"): Stranka presunuta do adresara "+this.newGroupId, idTmp, -1);
								}
							}
						}
					}
					context.getRequest().setAttribute("Ok", "");
				}
				else if(this.grouplist_operacie == 4 && this.available == true)
				{
					//zapnutie zobrazovania stranok

					//kontrola prav
					if(currUser.isDisabledItem("addPage") || currUser.isEnabledItem("editorMiniEdit"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					if(id != null)
					{
						int groupId = getGroupId();
						//ak aplikujem na vsetky stranky vsetkych podadresaroch
						if(applyToAllDocs && groupId != -1)
						{
							GroupsDB groupsDB = GroupsDB.getInstance();
							if(groupsDB != null)
							{
								List<GroupDetails> groupsTree = groupsDB.getGroupsTree(groupId, false, true);
								if(groupsTree != null && groupsTree.size() > 0)
								{
									DocDB docDb = DocDB.getInstance();
									for(GroupDetails gd : groupsTree)
									{
										List<DocDetails> docList = docDb.getBasicDocDetailsByGroup(gd.getGroupId(),DocDB.ORDER_TITLE);
										updateDocFields(currUser, docList, Tools.getTokens("author_id,available",",", true), Integer.valueOf(currUser.getUserId()), Boolean.TRUE);
									}
									DocDB.getInstance(true);
								}
							}
						}
						for (int i = 0; i < id.length; i++)
						{
							int idTmp = Tools.getIntValue(id[i], -1);
							if(idTmp != -1)
							{
								EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), idTmp, -1, -1);
								if (editorForm != null)
								{
									editorForm.setAuthorId(currUser.getUserId());
									editorForm.setAvailable(true);
									editorForm.setPublish("1");
									EditorDB.saveEditorForm(editorForm, context.getRequest());

									EditorDB.cleanSessionData(context.getRequest());

									Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+idTmp+"): Zapnute zobrazovanie stranky", idTmp, -1);
								}
							}
						}
					}
					context.getRequest().setAttribute("Ok", "");
				}
				else if(this.grouplist_operacie == 5 && Tools.isNotEmpty(this.newGroupId))
				{
					//kopirovanie stranok do ineho adresara

					//kontrola prav
					if(currUser.isDisabledItem("addPage") || currUser.isEnabledItem("editorMiniEdit"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					this.newGroupId = this.newGroupId.trim();
					if(id != null)
					{
						for (int i = 0; i < id.length; i++)
						{
							int idTmp = Tools.getIntValue(id[i], -1);
							if(idTmp != -1)
							{
								EditorForm editorForm = EditorDB.getEditorForm(context.getRequest(), idTmp, -1, -1);
								if (editorForm != null)
								{
									editorForm.setDocId(-1);
									editorForm.setVirtualPath("");
									editorForm.setAuthorId(currUser.getUserId());
									editorForm.setGroupId(Integer.parseInt(this.newGroupId));
									editorForm.setPublish("1");
									EditorDB.saveEditorForm(editorForm, context.getRequest());

									EditorDB.cleanSessionData(context.getRequest());

									Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "(DocID: "+idTmp+"): Stranka skopirovana do adresara "+this.newGroupId, idTmp, -1);
								}
							}
						}
					}
					context.getRequest().setAttribute("Ok", "");
				}
				else if((this.grouplist_operacie == 6 || this.grouplist_operacie == 7) && id != null)
				{
					//zapnut/vypnut zobrazovania stranok v menu

					//kontrola prav
					if(currUser.isDisabledItem("menuWebpages"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					int groupId = getGroupId();
					//ak aplikujem na vsetky stranky vsetkych podadresaroch
					if(applyToAllDocs && groupId != -1)
					{
						GroupsDB groupsDB = GroupsDB.getInstance();
						if(groupsDB != null)
						{
							List<GroupDetails> groupsTree = groupsDB.getGroupsTree(groupId, false, true);
							if(groupsTree != null && groupsTree.size() > 0)
							{
								DocDB docDb = DocDB.getInstance();
								for(GroupDetails gd : groupsTree)
								{
									List<DocDetails> docList = docDb.getBasicDocDetailsByGroup(gd.getGroupId(),DocDB.ORDER_TITLE);
									if(updateDocFields(currUser, docList, Tools.getTokens("show_in_menu",",", true), Boolean.valueOf(this.grouplist_operacie == 6)))
										context.getRequest().setAttribute("Ok", "");
								}
								DocDB.getInstance(true);
							}
						}
					}
					if(updateDocFields(currUser, id, "show_in_menu", this.grouplist_operacie == 6))
					{
						context.getRequest().setAttribute("Ok", "");
					}
				}
				else if((this.grouplist_operacie == 8 || this.grouplist_operacie == 9) && id != null)
				{
					//zapnut/vypnut prehladavatelnost stranok

					//kontrola prav
					if(currUser.isDisabledItem("menuWebpages"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					int groupId = getGroupId();
					//ak aplikujem na vsetky stranky vsetkych podadresaroch
					if(applyToAllDocs && groupId != -1)
					{
						GroupsDB groupsDB = GroupsDB.getInstance();
						if(groupsDB != null)
						{
							List<GroupDetails> groupsTree = groupsDB.getGroupsTree(groupId, false, true);
							if(groupsTree != null && groupsTree.size() > 0)
							{
								DocDB docDb = DocDB.getInstance();
								for(GroupDetails gd : groupsTree)
								{
									List<DocDetails> docList = docDb.getBasicDocDetailsByGroup(gd.getGroupId(),DocDB.ORDER_TITLE);
									if(updateDocFields(currUser, docList, Tools.getTokens("searchable",",", true), Boolean.valueOf(this.grouplist_operacie == 8)))
										context.getRequest().setAttribute("Ok", "");
								}
								DocDB.getInstance(true);
							}
						}
					}
					if(updateDocFields(currUser, id, "searchable", this.grouplist_operacie == 8))
					{
						context.getRequest().setAttribute("Ok", "");
					}
				}
				else if((this.grouplist_operacie == 10 || this.grouplist_operacie == 11) && id != null)
				{
					//zapnut/vypnut cachovanie stranok

					//kontrola prav
					if(currUser.isDisabledItem("menuWebpages"))
						return(new ForwardResolution("/components/maybeError.jsp"));

					int groupId = getGroupId();
					//ak aplikujem na vsetky stranky vsetkych podadresaroch
					if(applyToAllDocs && groupId != -1)
					{
						GroupsDB groupsDB = GroupsDB.getInstance();
						if(groupsDB != null)
						{
							List<GroupDetails> groupsTree = groupsDB.getGroupsTree(groupId, false, true);
							if(groupsTree != null && groupsTree.size() > 0)
							{
								DocDB docDb = DocDB.getInstance();
								for(GroupDetails gd : groupsTree)
								{
									List<DocDetails> docList = docDb.getBasicDocDetailsByGroup(gd.getGroupId(),DocDB.ORDER_TITLE);
									if(updateDocFields(currUser, docList, Tools.getTokens("cacheable",",", true), Boolean.valueOf(this.grouplist_operacie == 10)))
										context.getRequest().setAttribute("Ok", "");
								}
								DocDB.getInstance(true);
							}
						}
					}
					if(updateDocFields(currUser, id, "cacheable", this.grouplist_operacie == 10))
					{
						context.getRequest().setAttribute("Ok", "");
					}
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return new ForwardResolution("/components/maybeError.jsp");
	}
}

