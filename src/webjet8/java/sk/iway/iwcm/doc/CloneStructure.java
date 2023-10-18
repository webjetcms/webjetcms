package sk.iway.iwcm.doc;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/**
 *  CloneStructure.java - implementacia naklonovania strktryssssssaok
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.10 $
 *@created      Date: 31.8.2004 15:01:16
 *@modified     $Date: 2010/01/20 10:12:54 $
 */
public class CloneStructure
{
	private GroupsDB groupsDB;
	private Map<Integer, Integer> mapingGroupIds;
	private Map<String, String> mappingTempIds;
	private Prop prop;

	PrintWriter out = null;

	ActionServlet servlet;
	ActionMapping mapping;
	HttpServletRequest request;
	HttpServletResponse response;

	UserDetails currentUser;

	private void println(String text)
	{
		if (out != null)
		{
			out.println(text + "<br>");
		}
		else
		{
			Logger.println(this,text);
		}
	}

	private void print(String text)
	{
		if (out != null)
		{
			out.println(text);
		}
		else
		{
			Logger.println(this,text);
		}
	}

	private void printlnErr(String text)
	{
		if (out != null)
		{
			out.println("<span class='error'>"+text + "</span><br>");
		}
		else
		{
			Logger.error(this,text);
		}
	}

	public CloneStructure(PrintWriter out, ActionMapping mapping, HttpServletRequest request, HttpServletResponse response, ActionServlet servlet, String srcTempLang, String destTempLang)
	{
		this.out = out;
		this.mapping = mapping;
		this.request = request;
		this.response = response;
		this.servlet = servlet;
		groupsDB = GroupsDB.getInstance();
		mapingGroupIds = new Hashtable<>();
		mappingTempIds = new Hashtable<>();
		currentUser = UsersDB.getCurrentUser(request);

		this.prop = Prop.getInstance(servlet.getServletContext(), request);

		if (Tools.isEmpty(srcTempLang))
		{
			srcTempLang = "SK";
		}

		if (Tools.isNotEmpty(destTempLang))
		{
			out.println("<h3>"+prop.getText("components.clone.template_mapping")+"</h3>");

			TemplatesDB tempDB = TemplatesDB.getInstance();
			List<TemplateDetails> temps = tempDB.getTemplates();
			TemplateDetails temp;
			TemplateDetails temp2;
			String destTempName;
			int i;
			int j;
			i = 0;
			int mappingCounter = 0;
			while (i<temps.size())
			{
				temp = temps.get(i++);
				//println(" testing: " + temp.getTempName());
				if (temp.getTempName().toUpperCase().startsWith(srcTempLang.toUpperCase()))
				{
					destTempName = destTempLang.toUpperCase() + temp.getTempName().substring(srcTempLang.length());
					j = 0;
					while (j < temps.size())
					{
						temp2 = temps.get(j++);
						//println(" -- testing: " + temp2.getTempName() + " vs " + destTempName);
						if (temp2.getTempName().equalsIgnoreCase(destTempName))
						{
							println(temp.getTempName() + " -> " + temp2.getTempName());
							mappingTempIds.put(Integer.toString(temp.getTempId()), Integer.toString(temp2.getTempId()));
							mappingCounter++;
						}
					}
				}
			}

			if (mappingCounter==0)
			{
				println(prop.getText("components.clone.template_mapping_is_empty"));
			}


		}
		else
		{
			println(prop.getText("components.clone.languages_is_empty"));
		}

		if ("clone".equals(request.getParameter("act"))==false)
		{
			println("<br><br>"+prop.getText("components.clone.click_ok_to_continue"));

			//	zobraz form pre submit (potvrdenie)
			Enumeration<String> e = request.getParameterNames();
			String name;
			print("<form name='cloneOKForm' action=''>");
			while (e.hasMoreElements())
			{
				name = e.nextElement();
				print("<input type='hidden' name='"+ResponseUtils.filter(name)+"' value='"+ResponseUtils.filter(request.getParameter(name))+"'>");
			}
			print("<input type='hidden' name='act' value='clone'>");
			print("</form>");
			print("<script type='text/javascript'>function Ok() { document.cloneOKForm.submit(); } </script>");
		}
	}

	/**
	 * Vrati premapovane tempId sablony
	 * @param tempId
	 * @return
	 */
	public int getTempId(int tempId)
	{
		tempId = Tools.getIntValue(mappingTempIds.get(Integer.toString(tempId)), tempId);
		return(tempId);
	}

	public boolean copyStructure(int srcGroupId, int destGroupIp) throws IOException, ServletException
	{
		boolean ok = false;

		GroupDetails sourceRootGroup = groupsDB.getGroup(srcGroupId);
		GroupDetails destRootGroup = groupsDB.getGroup(destGroupIp);

		copyStructureRecursive(srcGroupId, destGroupIp, sourceRootGroup, destRootGroup);

		//naklonuj este hlavny adresar
		cloneDocsInGroup(srcGroupId, destGroupIp);

		return(ok);
	}

	private void copyStructureRecursive(int srcGroupId, int destGroupId, GroupDetails sourceRootGroup, GroupDetails destRootGroup) throws IOException, ServletException
	{
		println(prop.getText("components.clone.copy_dir") + ": " + srcGroupId + " -> " + destGroupId);

		GroupDetails newGroup = null;
		int oldGroupId;
		boolean wasSetGroup = false;
		for (GroupDetails group : groupsDB.getGroups(srcGroupId))
		{

			oldGroupId = group.getGroupId();

			//skopiruj - bacha, menime tu bean, ktory existuje, hned je nutne zavolat refresh
			newGroup = groupsDB.getGroup(group.getGroupId());
			newGroup.setGroupId(-1);
			newGroup.setParentGroupId(destGroupId);
			newGroup.setDefaultDocId(-1);
			newGroup.setTempId(getTempId(group.getTempId()));
			newGroup.setDomainName(destRootGroup.getDomainName());

			groupsDB.setGroup(newGroup);
			wasSetGroup = true;

			println("&nbsp;&nbsp;&nbsp;"+prop.getText("components.clone.creating_dir") + ": " + newGroup.getGroupIdName());

			//uloz do mapingu
			mapingGroupIds.put(Integer.valueOf(oldGroupId), Integer.valueOf(newGroup.getGroupId()));
		}

		if (out != null)
		{
			out.flush();
		}

		//refreshni GroupsDB
		if (wasSetGroup) groupsDB = GroupsDB.getInstance(true);

		//	zavolaj rekurziu
		Integer groupId;
		for (GroupDetails group : groupsDB.getGroups(srcGroupId))
		{
			groupId = mapingGroupIds.get(Integer.valueOf(group.getGroupId()));
			copyStructureRecursive(group.getGroupId(), groupId.intValue(), sourceRootGroup, destRootGroup);
		}

		if (out != null)
		{
			out.flush();
		}

		//	refreshni GroupsDB
		if (wasSetGroup) groupsDB = GroupsDB.getInstance(true);
		//skopiruj stranky
		for (GroupDetails group : groupsDB.getGroups(srcGroupId))
		{
			groupId = mapingGroupIds.get(Integer.valueOf(group.getGroupId()));

			cloneDocsInGroup(group.getGroupId(), groupId.intValue());
		}

		if (out != null)
		{
			out.flush();
		}
	}

	/**
	 * nahradi hodnoty srcGroupId za destGroupId v data pre rootGroup=srcGroupId, alebo rootGroupId=srcGroupId, alebo groupIds=&quot;rootGroupId&quot;
	 * @param data - klonovane data
	 * @param srcGroupId - povodny adresar
	 * @param destGroupId - novy adresar
	 * @return
	 */
	private  String replaceGroupIds(String data, int srcGroupId, int destGroupId)
	{
		if(Tools.isEmpty(data) || srcGroupId < 1 || destGroupId < 1) return data;
		String paragraphPattern = "(rootGroup="+srcGroupId+",)|(rootGroupId="+srcGroupId+",)|(groupIds=&quot;"+srcGroupId+"&quot;,)|(groupIds="+srcGroupId+",)";
		Matcher matcher = null;

		@SuppressWarnings("java:S1659")
		String group1, group2, group3, group4 = null;

		if(data != null)
		{
			if(data.indexOf("rootGroup") != -1 || data.indexOf("rootGroupId") != -1 ||
				data.indexOf("groupIds") != -1)
			{
				matcher = Pattern.compile(paragraphPattern).matcher(data);
				int i= 0;
				while (matcher.find() && i < 1000)
				{
					i++;
					group1 = matcher.group(1);
					group2 = matcher.group(2);
					group3 = matcher.group(3);
					group4 = matcher.group(4);
					if(Tools.isNotEmpty(group1)) data = Tools.replace(data, group1, "rootGroup="+destGroupId+",");
					else if(Tools.isNotEmpty(group2)) data = Tools.replace(data, group2, "rootGroupId="+destGroupId+",");
					else if(Tools.isNotEmpty(group3)) data = Tools.replace(data, group3, "groupIds=&quot;"+destGroupId+"&quot;,");
					else if(Tools.isNotEmpty(group4)) data = Tools.replace(data, group4, "groupIds="+destGroupId+",");
					matcher = Pattern.compile(paragraphPattern).matcher(data);
				}
				return data;
			}
		}

		return data;
	}

	private void cloneDocsInGroup(int oldGroupId, int newGroupId)
	{
		DocDB docDB = DocDB.getInstance();

		List<DocDetails> docs = docDB.getDocByGroup(oldGroupId, DocDB.ORDER_PRIORITY, true, -1, -1, true);
		Logger.println(this," mam docs: " + docs.size());

		for (DocDetails doc : docs)
		{
			request.setAttribute("docid", Integer.toString(doc.getDocId()));
			print("&nbsp;&nbsp;&nbsp;" + prop.getText("components.clone.creating_page") + ": " + doc.getDocId() + " " + doc.getTitle());

			//	toto je trochu hackovania, ale co uz ;-)
			request.setAttribute("editorActionDoNotRedirect", "true");

			EditorForm editorForm = EditorDB.getEditorForm(request, doc.getDocId(), doc.getHistoryId(), doc.getGroupId()); // (EditorForm) request.getAttribute("editorForm");
			if (editorForm != null)
			{
				//nahrad mapovanie groupIds podla mapingu
				Set<Map.Entry<Integer,Integer>> groupIds = mapingGroupIds.entrySet();
				for (Map.Entry<Integer,Integer> entry : groupIds)
				{
					editorForm.setData(replaceGroupIds(editorForm.getData(), entry.getKey(), entry.getValue()));
				}

				if (currentUser!=null) editorForm.setAuthorId(currentUser.getUserId());
				editorForm.setDocId(-1);
				editorForm.setVirtualPath("");
				editorForm.setGroupId(newGroupId);
				//ok, let's go
				editorForm.setPublish("1");
				editorForm.setAvailable(doc.isAvailable());
				editorForm.setTempId(getTempId(editorForm.getTempId()));

				int historyId = EditorDB.saveEditorForm(editorForm, request);
				if (historyId > 0)
				{
					if (request.getAttribute("saveOK") != null)
					{
						//klonuj aj atributy stranky
						List<AtrBean> beanList=new ArrayList<>();
						Connection db_conn = null;
						PreparedStatement ps = null;
						ResultSet rs = null;
						try
						{
							db_conn = DBPool.getConnection();
							ps = db_conn.prepareStatement("select * from doc_atr where doc_id= ? ");
							ps.setInt(1, doc.getDocId());
							rs = ps.executeQuery();
							while (rs.next())
							{
								AtrBean b=new AtrBean();
								b.setDocId(editorForm.getDocId());
								b.setAtrId(rs.getInt("atr_id"));
								b.setValueString(rs.getString("value_string"));
								b.setValueNumber(rs.getDouble("value_int"));
								b.setValueBool(rs.getBoolean("value_bool"));
								beanList.add(b);

							}
							rs.close();
							ps.close();
							db_conn.close();
							rs = null;
							ps = null;
							db_conn = null;
						}
						catch (Exception ex)
						{
							sk.iway.iwcm.Logger.error(ex);
						}
						finally
						{
							try
							{
								if (rs != null)
									rs.close();
								if (ps != null)
									ps.close();
								if (db_conn != null)
									db_conn.close();
							}
							catch (Exception ex2)
							{
								sk.iway.iwcm.Logger.error(ex2);
							}
						}

						db_conn = null;
						ps = null;
						try
						{
							db_conn = DBPool.getConnection();
							ps = db_conn.prepareStatement("insert into doc_atr values (?,?,?,?,?)");
							int index=1;
							for (AtrBean b:beanList){
								index=1;
								ps.setInt(index++, b.getDocId());
								ps.setInt(index++, b.getAtrId());
								ps.setString(index++, b.getValueString());
								ps.setDouble(index++, b.getValueNumber());
								ps.setBoolean(index++,b.isValueBool());

								ps.executeUpdate();
							}

							ps.close();

							db_conn.close();
							ps = null;
							db_conn = null;
						}
						catch (Exception ex)
						{
							sk.iway.iwcm.Logger.error(ex);
						}
						finally
						{
							try
							{

								if (ps != null)
									ps.close();
								if (db_conn != null)
									db_conn.close();
							}
							catch (Exception ex2)
							{
                                sk.iway.iwcm.Logger.error(ex2);
							}
						}

						db_conn = null;
						ps = null;
						try
						{
							db_conn = DBPool.getConnection();
							String insertSql = "INSERT INTO media (media_id, media_fk_id, media_fk_table_name, "+
								"media_title_sk, media_title_cz, media_title_de, media_title_en, media_link, media_thumb_link, media_group, "+
								"media_info_sk, media_info_cz, media_info_de, media_info_en, media_sort_order, last_update) VALUES ("+
								"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
							ps = db_conn.prepareStatement(insertSql);

							//naklonuj media
							List<Media> media = MediaDB.getMedia(request.getSession(), "documents", doc.getDocId(), null);
							if (media != null)
							{
								for (Media m : media)
								{
									int index = 1;
									ps.setInt(index++, PkeyGenerator.getNextValue("media"));
									ps.setInt(index++, editorForm.getDocId());
									ps.setString(index++, m.getMediaFkTableName());

									ps.setString(index++, m.getMediaTitleSk());
									ps.setString(index++, m.getMediaTitleCz());
									ps.setString(index++, m.getMediaTitleDe());
									ps.setString(index++, m.getMediaTitleEn());
									ps.setString(index++, m.getMediaLink());
									ps.setString(index++, m.getMediaThumbLink());
									ps.setString(index++, m.getMediaGroup());

									ps.setString(index++, m.getMediaInfoSk());
									ps.setString(index++, m.getMediaInfoCz());
									ps.setString(index++, m.getMediaInfoDe());
									ps.setString(index++, m.getMediaInfoEn());
									ps.setInt(index++, m.getMediaSortOrder());
									ps.setTimestamp(index++, new Timestamp(m.getLastUpdate().getTime()));

									ps.execute();
								}
							}

							ps.close();
							db_conn.close();
							rs = null;
							ps = null;
							db_conn = null;
						}
						catch (Exception ex)
						{
							sk.iway.iwcm.Logger.error(ex);
						}
						finally
						{
							try
							{
								if (rs != null)
									rs.close();
								if (ps != null)
									ps.close();
								if (db_conn != null)
									db_conn.close();
							}
							catch (Exception ex2)
							{
                                sk.iway.iwcm.Logger.error(ex2);
							}
						}


						println("   [OK]");
					}
					else
					{
						printlnErr("saveOK nie je nastavene");
					}
				}
				else
				{
					printlnErr("saveForward je null");
				}
			}
			else
			{
				printlnErr("EditorForm je null");
			}
		}
	}
}
