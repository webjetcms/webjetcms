package sk.iway.iwcm.forum;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.ForumTools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.fulltext.indexed.Forums;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;


/**
 *  Ulozenie prispevku do fora
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Sobota, 2003, november 8
 *@modified     $Date: 2003/12/10 22:13:51 $
 */
public class ForumSaveAction extends Action
{

	/**
	 *  Description of the Method
	 *
	 *@param  mapping          Description of the Parameter
	 *@param  form             Description of the Parameter
	 *@param  request          Description of the Parameter
	 *@param  response         Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception  IOException  Description of the Exception
	 */
	@Override
	public ActionForward execute(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws IOException
	{
		HttpSession session = request.getSession();
		int userId = -1;
		int i;
		String hashCode;

		Prop prop = Prop.getInstance(servlet.getServletContext(), request);
		Identity sender = (Identity) session.getAttribute(Constants.USER_KEY);

		ForumForm forumForm = (ForumForm) form;

		if (sender==null || sender.isAdmin()==false)
		{
			synchronized (ForumSaveAction.class)
			{
				//kontrola pred duplicitnym odoslanim prispevku (nejaka ajax haluz) - http://helpdesk.interway.sk/?bugID=4676
				//v access logu servera sa zaznamy skutocne nasli viac krat, preco nikto netusi
				String lastSessionText = (String)session.getAttribute("ForumSaveAction.lastText");
				if (lastSessionText!=null && lastSessionText.equals(forumForm.getQuestion()))
				{
					request.setAttribute("permissionDenied", "sameText");
					return (mapping.findForward("success"));
				}

				session.setAttribute("ForumSaveAction.lastText", forumForm.getQuestion());
			}
		}

		ForumGroupBean fgb = ForumDB.getForum(forumForm.getDocId(), true);
		if (fgb == null)
		{
			//najdi parent adresar a skus podla neho
			DocDetails doc = DocDB.getInstance().getBasicDocDetails(forumForm.getDocId(), false);
			if (doc != null)
			{
				GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
				if (group != null)
				{
					GroupDetails parentGroup = GroupsDB.getInstance().getGroup(group.getParentGroupId());
					if (parentGroup!=null)
					{
						fgb = ForumDB.getForum(parentGroup.getDefaultDocId(), true);
					}
				}
			}
		}

        if (fgb == null) {
           //TODO: ak je null fgb - podla docid najdi adresar (groupId), pre groupId najdi default stranku a potom nacitaj fgb pre tuto stranku
            DocDetails docDetails = DocDB.getInstance().getDoc(forumForm.getDocId());
            if (docDetails != null) {
                int defaultDocId = docDetails.getGroup().getDefaultDocId();
                DocDetails defaultDocDetails = DocDB.getInstance().getDoc(defaultDocId);
                fgb = ForumDB.getForum(defaultDocDetails.getDocId(), true);
            }
        }

		ForumGroupBean fgbPerms = fgb;
		if (fgbPerms == null && Tools.isNotEmpty(Constants.getString("forumDefaultAddmessageGroups")))
		{
			fgbPerms = new ForumGroupBean();
			fgbPerms.setAddmessageGroups(Constants.getString("forumDefaultAddmessageGroups"));
		}
		if (fgbPerms != null && fgbPerms.canPostMessage(sender)==false)
		{
			//nema dostatocne prava (kontrola user groups)
			request.setAttribute("errorKey", "components.forum.wrong_user_groups_for_post");
			return (mapping.findForward("success"));
		}

		if (ForumDB.isActive(forumForm.getDocId())==false && (sender == null || sender.isAdmin()==false))
		{
			request.setAttribute("errorKey", "components.forum.forum_closed");
			return (mapping.findForward("success"));
		}

		if (forumForm.getUploadedFile()!=null && forumForm.getUploadedFile().getFileSize()>0)
		{
			return executeUpload(mapping, forumForm, request, response);
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (isVulgar(forumForm.getAuthorFullName()) || isVulgar(forumForm.getAuthorEmail()) ||
				 isVulgar(forumForm.getQuestion()) || isVulgar(forumForm.getSubject()) ||
				 ShowDoc.getXssRedirectUrl(request)!=null)
			{
				request.setAttribute("isVulgar", "true");
				return (mapping.findForward("success"));
			}

			String plainQuestion = SearchTools.htmlToPlain(forumForm.getQuestion());
			plainQuestion = Tools.replace(plainQuestion, "\n", "");
			plainQuestion = Tools.replace(plainQuestion, "\r", "");
			if ("true".equals(request.getParameter("forumAllowEmptyMessage"))==false &&
				 (Tools.isEmpty(forumForm.getQuestion()) || Tools.isEmpty(plainQuestion)))
			{
				request.setAttribute("errorKey", "components.forum.error.questionEmpty");
				return (mapping.findForward("success"));
			}

			//fixni hlasku o nahravani editora (ak tam je)
			forumForm.setQuestion(Tools.replace(forumForm.getQuestion(), prop.getText("editor.loading_please_wait"), ""));

			//ak je vypnuty wysiwyg editor, tak nedovol HTML
			if (Constants.getBoolean("disableWysiwyg"))
			{
				String oldText = forumForm.getQuestion();
				String newText = oldText.replace("<", "&lt;");
				newText = newText.replace(">", "&gt;");
				newText = newText.replace("\n", "<br />");
				forumForm.setQuestion(newText);
			}

			//nastav linkam nofollow
			forumForm.setQuestion(Tools.replace(forumForm.getQuestion(), "<a href", "<a rel=\"nofollow\" href"));
			forumForm.setQuestion(Tools.replace(forumForm.getQuestion(), "<A HREF", "<a rel=\"nofollow\" href"));

			Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
			if (user==null || user.isAdmin()==false)
			{
				forumForm.setForumId(-1);
			}

			if (user != null)
			{
				userId = user.getUserId();
			/*	//ak som admin, nastavim flag
				if(user.isAdmin())
					forumForm.setFlag("admin");
			*/
			}
			db_conn = DBPool.getConnection(request);
			String sql;


			forumForm.setQuestion( clearMsOfficeTags( forumForm.getQuestion() ) );


			if (forumForm.getForumId()>0 && user != null && user.isAdmin())
			{
				if (forumForm.getQuestion().length()<1)
				{
					sql = "DELETE FROM document_forum WHERE forum_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true);

					ps = db_conn.prepareStatement(sql);
					ps.setInt(1, forumForm.getForumId());

					ps.executeUpdate();
					ps.close();
					ps = null;
					Adminlog.add(Adminlog.TYPE_FORUM_DELETE, "Zmazany prispevok "+forumForm.getSubject(), forumForm.getForumId(), forumForm.getDocId());
				}
				else
				{
					sql = "UPDATE document_forum SET subject=?, question=?, ";
					sql += "author_name=?, author_email=?, send_answer_notif=?, flag=? WHERE forum_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true);

					ps = db_conn.prepareStatement(sql);
					ps.setString(1, forumForm.getSubject());
					DB.setClob(ps, 2, forumForm.getQuestion());
					if(sender != null && Tools.isEmpty(forumForm.getAuthorFullName()) && forumForm.isUseLoggedUser()) {
						ps.setString(3, sender.getLoginName());
						ps.setString(4,sender.getEmail());
					} else {
						ps.setString(3, forumForm.getAuthorFullName());
						ps.setString(4, forumForm.getAuthorEmail());
					}
					ps.setBoolean(5, forumForm.isSendNotif());
					ps.setString(6, forumForm.getFlag());
					ps.setInt(7, forumForm.getForumId());

					ps.executeUpdate();
					ps.close();
					ps = null;
					Adminlog.add(Adminlog.TYPE_FORUM_UPDATE, "Zmeneny prispevok: "+forumForm.getSubject(), forumForm.getForumId(), forumForm.getDocId());
				}
			}
			else
			{
				hashCode = Password.generatePassword(15);

				if (user != null && Tools.isNotEmpty(user.getSignature()) && "true".equals(request.getParameter("addSignature")))
				{
					if ("true".equals(request.getParameter("dontReplaceSignatureCodes")))
					{
						forumForm.setQuestion(forumForm.getQuestion() + "<div class='forumSignature'>"+user.getSignature()+"</div>");
					}
					else
					{
						forumForm.setQuestion(forumForm.getQuestion() + "<div class='forumSignature'>"+ ForumTools.replaceSignatureCodes(user)+"</div>");
					}
				}

				if (!SpamProtection.canPost("forum", forumForm.getQuestion(), request))
				{
					request.setAttribute("permissionDenied", "postLimit");
					return (mapping.findForward("success"));
				}

				if (user != null)
				{
					//ako autora povolime len login, alebo meno
					if (user.getLogin().equals(forumForm.getAuthorFullName())==false) forumForm.setAuthorFullName(user.getFullName());
					forumForm.setAuthorEmail(user.getEmail());
				}


				//ak pridavam podtemu diskusie
				if (forumForm.getParentId() == -1)
				{
					sql = "INSERT INTO document_forum (doc_id, parent_id, subject, question, question_date, "+
							"author_name, author_email, ip, confirmed, hash_code, send_answer_notif, " +
							"user_id, stat_views, stat_replies, stat_last_post, active, flag, domain_id) " +
							"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

					i = 1;
					ps = db_conn.prepareStatement(sql);
					ps.setInt(i++, forumForm.getDocId());
					ps.setInt(i++, forumForm.getParentId());
					ps.setString(i++, forumForm.getSubject());
					DB.setClob(ps, i++, forumForm.getQuestion());
					ps.setTimestamp(i++, new Timestamp(Tools.getNow()));


					if(sender != null && Tools.isEmpty(forumForm.getAuthorFullName()))
					{
						ps.setString(i++, sender.getLogin());
						ps.setString(i++, sender.getEmail());
					}
					else
					{
						ps.setString(i++, forumForm.getAuthorFullName());
						ps.setString(i++, forumForm.getAuthorEmail());
					}
					ps.setString(i++, Tools.getRemoteIP(request));
					//ak je zapnute potvrdzovanie, nastavim novym prispevkom FALSE

					ForumGroupBean fgb2 = ForumDB.getForum(forumForm.getDocId(), true);
					if (fgb2!=null &&  fgb2.isMessageConfirmation())
						ps.setBoolean(i++, false);
					else
						ps.setBoolean(i++, true);

					ps.setString(i++, hashCode);
					ps.setBoolean(i++, forumForm.isSendNotif());
					ps.setInt(i++, userId);
					ps.setInt(i++, 0);
					ps.setInt(i++, 0);
					ps.setTimestamp(i++, new Timestamp(Tools.getNow()));
					ps.setBoolean(i++, true);
					ps.setString(i++, forumForm.getFlag());
					ps.setInt(i++, CloudToolsForCore.getDomainId());
					ps.execute();
					ps.close();
					ps = null;
					Adminlog.add(Adminlog.TYPE_FORUM_CREATE, "Vytvorena tema diskusie: "+forumForm.getSubject(), -1, forumForm.getDocId());
				}
				else
				{
					//pridavam prispevok do podtemy
					sql = "INSERT INTO document_forum (doc_id, parent_id, subject, question, question_date, "+
							"author_name, author_email, ip, confirmed, hash_code, send_answer_notif, user_id, flag, domain_id) " +
							"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

					long insertDate = Tools.getNow();
					i = 1;
					ps = db_conn.prepareStatement(sql);
					ps.setInt(i++, forumForm.getDocId());
					ps.setInt(i++, forumForm.getParentId());
					ps.setString(i++, forumForm.getSubject());
					DB.setClob(ps, i++, forumForm.getQuestion());
					ps.setTimestamp(i++, new Timestamp(insertDate));
					if(sender != null && Tools.isEmpty(forumForm.getAuthorFullName())) {
						ps.setString(i++, sender.getLogin());
						ps.setString(i++, sender.getEmail());
					} else {
						ps.setString(i++, forumForm.getAuthorFullName());
						ps.setString(i++, forumForm.getAuthorEmail());
					}
					ps.setString(i++, Tools.getRemoteIP(request));
					//ak je zapnute potvrdzovanie, nastavim novym prispevkom FALSE

					ForumGroupBean fgb2 = ForumDB.getForum(forumForm.getDocId(), true);
					if (fgb2!=null &&  fgb2.isMessageConfirmation())
						ps.setBoolean(i++, false);
					else
						ps.setBoolean(i++, true);

					ps.setString(i++, hashCode);
					ps.setBoolean(i++, forumForm.isSendNotif());
					ps.setInt(i++, userId);
					ps.setString(i++, forumForm.getFlag());
					ps.setInt(i++, CloudToolsForCore.getDomainId());
					ps.execute();
					ps.close();
					ps = null;
					int fId = ForumDB.getForumMessageParent(forumForm.getParentId(), forumForm.getDocId());
					//podteme zvysim statistiku a nastavim datum last_post na aktualny
					if (fId > 0)
					{
						sql = "UPDATE document_forum SET stat_replies=stat_replies+1, stat_last_post=? " +
								"WHERE forum_id=? AND parent_id=-1"+CloudToolsForCore.getDomainIdSqlWhere(true);

						ps = db_conn.prepareStatement(sql);
						ps.setTimestamp(1, new Timestamp(insertDate));
						ps.setInt(2, fId);
						ps.execute();
						ps.close();
						ps = null;
					}
					Adminlog.add(Adminlog.TYPE_FORUM_CREATE, "Vytvoreny prispevok: "+forumForm.getSubject(), -1, forumForm.getDocId());
				}

				if(userId > 0)
				{
					//zvysim userovi forum rank

					sql = "UPDATE  users SET forum_rank=forum_rank+1 WHERE user_id=?";
					ps = db_conn.prepareStatement(sql);
					ps.setInt(1, userId);
					ps.execute();
					ps.close();
					ps = null;
				}

				//zvysim forum_count stranky
				sql = "UPDATE documents SET forum_count=forum_count+1 WHERE doc_id=?";
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, forumForm.getDocId());
				ps.execute();
				ps.close();
				ps = null;
				if (forumForm.getForumId()<1)
				{
					ps = db_conn.prepareStatement("SELECT max(forum_id) AS forum_id FROM document_forum WHERE doc_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
					ps.setInt(1, forumForm.getDocId());
					rs = ps.executeQuery();
					if (rs.next())
					{
						forumForm.setForumId(rs.getInt("forum_id"));
					}
					rs.close();
					ps.close();
					rs = null;
					ps = null;
				}

				//Logger.println(this,"Idem poslat mail");

				if (fgb == null)
				{
					String forumDefaultApproveEmail = Constants.getString("forumDefaultApproveEmail");
					String forumDefaultNotifyEmail = Constants.getString("forumDefaultNotifyEmail");

					if (Tools.isNotEmpty(forumDefaultApproveEmail) || Tools.isNotEmpty(forumDefaultNotifyEmail))
					{
						fgb = new ForumGroupBean();
						fgb.setMessageConfirmation(false);
						if (Tools.isNotEmpty(forumDefaultApproveEmail))
						{
							fgb.setApproveEmail(forumDefaultApproveEmail);
							fgb.setMessageConfirmation(true);
						}
						if (Tools.isNotEmpty(forumDefaultNotifyEmail))
						{
							fgb.setNotifEmail(forumDefaultNotifyEmail);
						}
					}
				}

				ForumDB.sendConfirmationNotificationEmail(fgb, forumForm.getForumId(), forumForm.getParentId(), forumForm.getSubject(), forumForm.getQuestion(), forumForm.getDocId(), forumForm.getAuthorFullName(), forumForm.getAuthorEmail(), hashCode, request);
			}
			db_conn.close();
			db_conn = null;

			String fromName = Constants.getString("forumNotifySenderName");
			if(Tools.isEmpty(fromName)) fromName = forumForm.getAuthorFullName();
			String fromEmail = Constants.getString("forumNotifySenderEmail");
			if (Tools.isEmpty(fromEmail))	fromEmail = forumForm.getAuthorEmail();

			if (Tools.isNotEmpty(fromName))
			{
				//uloz meno a email do cookies
				Cookie forumName = new Cookie("forumname", Tools.URLEncode(fromName));
				forumName.setPath("/");
				forumName.setMaxAge(60 * 24 * 3600);
				forumName.setHttpOnly(true);
				Cookie forumEmail = new Cookie("forumemail", Tools.URLEncode(fromEmail));
				forumEmail.setPath("/");
				forumEmail.setMaxAge(60 * 24 * 3600);
				forumEmail.setHttpOnly(true);

				Tools.addCookie(forumName, response,request);
				Tools.addCookie(forumEmail, response,request);

			}
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
			}
		}

		Forums.updateSingleQuestion(forumForm.getForumId());
		return (mapping.findForward("success"));
	}

	/**
	 * Odstrani Microsot Office tagy z textu. Tieto tagy mozu sposobovat zle
	 * odsadzovanie textu.
	 *
	 * @return String - povodny text ostripovany o tagy
	 */
	public static String clearMsOfficeTags(String text)
	{
		String oldText = text;

		String forumWysiwygIcons = Constants.getString("forumWysiwygIcons");
		if (forumWysiwygIcons.indexOf("createlink")==-1)
		{
			//odpazme linky
			Pattern linkPattern = Pattern.compile("<\\s*[^>]*\\s*a\\s*href\\s*=\\s*\"[^\"]*\"\\s*[^>]*>|</a>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); //NOSONAR
			Matcher linkMatcher = linkPattern.matcher(text);
			text = linkMatcher.replaceAll("");
		}
		if (forumWysiwygIcons.indexOf("insertimage")==-1)
		{
			//odpazme linky
			Pattern linkPattern = Pattern.compile("<\\s*[^>]*\\s*img\\s*src\\s*=\\s*\"[^\"]*\"\\s*[^>]*>|</img>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); //NOSONAR
			Matcher linkMatcher = linkPattern.matcher(text);
			text = linkMatcher.replaceAll("");
		}

		try
		{
			//ak neobsahuje tento text, tak sa nie je coho bat
			if (!text.contains("class=MsoNormal") && !text.contains("class=\"MsoNormal\""))
				return text;

			//v prvom rade zmaz vsetky <FONT> a </FONT> tagy
			Pattern fontPattern = Pattern.compile("<FONT .*?>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
			Matcher fontMatcher = fontPattern.matcher(text);

			while (fontMatcher.find())
			{
				text = text.replace(fontMatcher.group(0), "");
				fontMatcher = fontPattern.matcher(text);
			}

			text = text.replace("</FONT>", "");


			//potom samotne MsoNormal tagy
			Pattern msTagPattern = Pattern.compile("<P.*?class=.?MsoNormal.*?>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
			Matcher msTagMatcher = msTagPattern.matcher(text);

			while (msTagMatcher.find())
			{
				text = text.replace(msTagMatcher.group(0), "<P>");
				msTagMatcher = msTagPattern.matcher(text);
			}

			//toto su dalsie MS specific tagy...
			text = text.replace("<o:p>", "");
			text = text.replace("</o:p>", "");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return oldText;
		}
		return text;
	}

	/**
	 * Vykona kontrolu prav a nahra pozadovany subor na server
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public ActionForward executeUpload(ActionMapping mapping,
				ForumForm form,
				HttpServletRequest request,
				HttpServletResponse response)
				 throws IOException
	{
		//skontroluj prava
		Identity user = (Identity)request.getSession().getAttribute(Constants.USER_KEY);
		int docId = Tools.getIntValue(request.getParameter("docid"), 0);
		if (user == null || docId < 1)
		{
			request.setAttribute("permissionDenied", "true");
			return (mapping.findForward("success"));
		}

		//skontrolu, ci nahravam subor k mojmu prispevku
		ForumBean fb = ForumDB.getForumBean(request, form.getParentId());
		if (fb == null || fb.getUserId()<1 || fb.getUserId()!=user.getUserId())
		{
			request.setAttribute("permissionDenied", "true");
			return (mapping.findForward("success"));
		}

		LabelValueDetails uploadLimits = ForumDB.getUploadLimits(docId, request);
		if (uploadLimits == null)
		{
			request.setAttribute("permissionDenied", "true");
			return (mapping.findForward("success"));
		}
		//skontroluj prava na subory
		String fileName = DocTools.removeChars(form.getUploadedFile().getFileName()).toLowerCase();

		//kontrola velkosti suboru
		if (uploadLimits.getInt1()>0 && (uploadLimits.getInt1()*1024)<form.getUploadedFile().getFileSize())
		{
			request.setAttribute("permissionDenied", "fileSize");
			return (mapping.findForward("success"));
		}


		//kontrola pripony
		if (fileName.endsWith(".jsp") || fileName.endsWith(".class"))
		{
			request.setAttribute("permissionDenied", "true");
			return (mapping.findForward("success"));
		}
		boolean canUpload = false;
		if ("*".equals(uploadLimits.getValue()))
		{
			canUpload = true;
		}
		else
		{
			//skontroluj priponu suboru
			StringTokenizer st = new StringTokenizer(uploadLimits.getValue(), ",");
			while (st.hasMoreTokens() && canUpload==false)
			{
				if (fileName.endsWith(st.nextToken())) canUpload = true;
			}
		}

		if (!canUpload)
		{
			request.setAttribute("permissionDenied", "fileType");
			return (mapping.findForward("success"));
		}

		//ok, mozeme to nahrat
		String baseDir = uploadLimits.getValue2();
		if (baseDir == null) baseDir = "/files/forum/";
		baseDir = baseDir.replace('\\', '/');
		if (baseDir.endsWith("/")==false) baseDir = baseDir + "/";

		fileName = user.getUserId()+"-"+form.getParentId()+"-"+fileName;

		File f = new File(Tools.getRealPath(baseDir+fileName));
		if (f.getParentFile().exists()==false){
			if(f.getParentFile().mkdirs() == false){
				request.setAttribute("permissionDenied", "true");
				return (mapping.findForward("success"));
			}
		}

		boolean fileAllreadyExists = f.exists();

		//zapis subor na disk
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
		BufferedInputStream buffReader = new BufferedInputStream(form.getUploadedFile().getInputStream());
		FileOutputStream fos = new FileOutputStream(f);
		while ((bytesRead = buffReader.read(buffer, 0, 8192)) != -1)
		{
			fos.write(buffer, 0, bytesRead);
		}
		//Logger.println(this,"read end");
		buffReader.close();
		fos.close();
		form.getUploadedFile().destroy();

		//ak prepise existujuci subor, aby sa to neobjavilo v zozname viac krat
		if (fileAllreadyExists==false)
		{
			//zapis do medii
			Media m = new Media();
			m.setMediaFkId(form.getParentId());
			m.setMediaFkTableName("document_forum");
			if (Tools.isNotEmpty(form.getSubject())) m.setMediaTitleSk(form.getSubject());
			else m.setMediaTitleSk(form.getUploadedFile().getFileName());
			m.setMediaLink(baseDir+fileName);
			m.setMediaSortOrder(10);
			new MediaDB().save(m);
		}

		return (mapping.findForward("success"));
	}

	/**
	 * testuje, ci nie je prispevok vulgarny, alebo obsahuje nepovoleny HTML kod
	 * @param message
	 * @return
	 */
	public boolean isVulgar(String message)
	{
		String vulgarizmy = Constants.getString("vulgarizmy");
		if (Tools.isEmpty(vulgarizmy)) return false;

		if(Tools.isNotEmpty(message))
		{
			message = " " + message.toLowerCase();
			//aby to naslo aj take ze ...text<script...
			message = Tools.replace(message, "<", " <");
			message = Tools.replace(message, ">", "> ");
			message = message.replace('\n', ' ');
			message = message.replace('\r', ' ');

			//Logger.println(this,"isVulgar: " + message);
			if (Tools.isNotEmpty(vulgarizmy))
			{
				StringTokenizer st = new StringTokenizer(vulgarizmy, ",");
				while (st.hasMoreTokens())
				{
					String slovo = st.nextToken().trim();
					if (Tools.isEmpty(slovo)) continue;

					if (message.indexOf(" "+slovo)!=-1)
					{
						Logger.println(ForumSaveAction.class, "vulgar: "+slovo+" message: "+message);
						Adminlog.add(Adminlog.TYPE_FORUM_SAVE, "vulgar: "+slovo+" message: "+message, -1, -1);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 *
	 * @param user
	 * @return
	 * @deprecated use ForumTools.replaceSignatureCodes
	 */
	@Deprecated
	public static String replaceSignatureCodes(UserDetails user){
		return ForumTools.replaceSignatureCodes(user);
	}

}
