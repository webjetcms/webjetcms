package sk.iway.iwcm.dmail;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import org.apache.commons.codec.binary.Base64;

import sk.iway.Password;
import sk.iway.iwcm.*;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.utils.Pair;

/**
 *  Rozposielac emailov
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       unascribed
 *@version      1.0
 *@created      Pondelok, 2003, jun 2
 *@modified     $Date: 2004/03/04 22:16:19 $
 */

public class Sender extends TimerTask
{
	private final Timer timer;
	private static Random rand = new Random();

	private boolean runActive = false;
	private int toSendCount = 0;


	/**
	 *  Description of the Field
	 */
	public static int MAX_RETRY_COUNT = 5; //NOSONAR

	/**
	 *  Description of the Field
	 */
	public static final String CONTEXT_NAME = "dmail_sender";
	private boolean active = false;


	public static final String FROM_EMAIL_ID_KEY = "sk.iway.iwcm.dmail.Sender.fromEmailId";
	/**
	 * ked je v DB vela zaznamov je vyber pomaly, toto je ID emailu od ktoreho vyberame z DB
	 * pre ulozenie hodnoty sa pouzije cache s 5 minutovym intervalom
	 * @return id emailu od ktoreho sa robi vyber z DB
	 */
	private int getFromEmailId()
	{
		Cache c = Cache.getInstance();
		Integer fromEmailId = (Integer)c.getObject(FROM_EMAIL_ID_KEY);
		if (fromEmailId == null)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT min(email_id) FROM emails WHERE sent_date IS NULL AND (send_at IS null OR send_at <= ?) AND disabled=?");
				ps.setTimestamp(1, new Timestamp(Tools.getNow()));
				ps.setBoolean(2, false);
				rs = ps.executeQuery();
				if (rs.next())
				{
					fromEmailId = Integer.valueOf(rs.getInt(1)-1);
					Logger.debug(Sender.class, "fromEmailId1="+fromEmailId);
				}
				rs.close();
				ps.close();

				if (fromEmailId == null || fromEmailId.intValue() < 0)
				{
					//nie je nic neodoslane, takze pouzijeme maximalnu hodnotu
					ps = db_conn.prepareStatement("SELECT max(email_id) FROM emails");
					rs = ps.executeQuery();
					if (rs.next())
					{
						fromEmailId = Integer.valueOf(rs.getInt(1));
						Logger.debug(Sender.class, "fromEmailId2="+fromEmailId);
					}
					rs.close();
					ps.close();
				}

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

			if (fromEmailId == null)
			{
				fromEmailId = Integer.valueOf(0);
			}

			Logger.debug(Sender.class, "fromEmailId3="+fromEmailId);
			c.setObject(FROM_EMAIL_ID_KEY, fromEmailId, 5);
		}

		return fromEmailId.intValue();
	}

	/**
	 * Sender sa dotazuje databazy len raz za nejaky cas, toto ho zresetuje
	 */
	public static void resetSenderWait()
	{
		Cache c = Cache.getInstance();
		c.removeObject(FROM_EMAIL_ID_KEY);
	}

	/**
	 *  Gets the instance attribute of the Sender class
	 *
	 *@return    The instance value
	 */
	public static Sender getInstance()
	{
		Sender sender = null;

		if (Constants.getServletContext().getAttribute(CONTEXT_NAME) != null)
		{
			sender = (Sender) Constants.getServletContext().getAttribute(CONTEXT_NAME);
			if (sender.isActive() == false)
			{
				Constants.getServletContext().removeAttribute(CONTEXT_NAME);
				sender = null;
			}
			else
			{
				Logger.debug(Sender.class,"Sender je uz aktivny...");
			}
		}

		if (sender == null)
		{
			sender = new Sender();
			Logger.debug(Sender.class,"Sender.getInstance() - je null, schedulujem");
		}
		else
		{
			Logger.debug(Sender.class,"Sender.getInstance() - nie je null");
		}

		return (sender);
	}

	/**
	 *  Constructor for the Sender object
	 */
	public Sender()
	{
		Logger.debug(this,"Sender Constructor");
		setActive(true);
		//ziskaj pocet, kolko treba este poslat
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT count(email_id) FROM emails WHERE email_id>? AND sent_date IS NULL AND retry>=0 AND (send_at IS null OR send_at <= ?) AND disabled=?");
			ps.setInt(1, getFromEmailId());
			ps.setTimestamp(2, new Timestamp(Tools.getNow()));
			ps.setBoolean(3, false);
			rs = ps.executeQuery();
			if (rs.next())
			{
				toSendCount = rs.getInt(1);
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
			}
		}

		timer = new Timer(true);
		timer.schedule(this, 30000, Constants.getInt("dmailWaitTimeout"));
		Constants.getServletContext().removeAttribute(CONTEXT_NAME);
		Constants.getServletContext().setAttribute(CONTEXT_NAME, this);
	}

	/**
	 *  Main processing method for the Sender object
	 */
	@Override
	public void run()
	{
		if ("false".equals(Constants.getString("useSMTPServer") ))
		{
			return;
		}

		if (Constants.getBoolean("disableDMailSender")) return;

		String senderRunOnNode = Constants.getString("senderRunOnNode");
		if (Tools.isNotEmpty(senderRunOnNode) && (","+senderRunOnNode+",").indexOf(Constants.getString("clusterMyNodeName"))==-1)
		{
			return;
		}

		//Logger.println(this,"counter=" + counter);
		if (runActive)
		{
			Logger.debug(this,"Sender.run method is allready active, skipping");
		}

		//Logger.debug(Sender.class, "Sender.run - toSendCount="+toSendCount);

		runActive = true;

		Sender.setMaxRetryCount(Constants.getInt("dmailMaxRetryCount"));
		int sleepTime = Constants.getInt("dmailSleepTimeAfterException");

		//Logger.println(this,"counter=" + counter);
	   Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if (toSendCount < 0)
			{
				//soudruzi z NDR nekde udelali chybu...
				ps = db_conn.prepareStatement("SELECT count(email_id) FROM emails WHERE email_id>? AND sent_date IS NULL AND (send_at IS null OR send_at <= ?) AND retry>=0  AND disabled=?");
				ps.setInt(1, getFromEmailId());
				ps.setTimestamp(2, new Timestamp(Tools.getNow()));
				ps.setBoolean(3, false);
				rs = ps.executeQuery();
				if (rs.next())
				{
					toSendCount = rs.getInt(1);
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}

			//nacitaj si zoznam z DB
			String sql = "";
			if(Constants.DB_TYPE == Constants.DB_MSSQL) sql = ("SELECT TOP 50 * FROM emails WHERE email_id>? AND sent_date IS NULL AND (send_at IS null OR send_at <= ?) AND disabled=? ORDER BY email_id ASC");
			else if(Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL) sql = ("SELECT * FROM emails WHERE email_id>? AND sent_date IS NULL AND (send_at IS null OR send_at <= ?) AND disabled=? ORDER BY email_id ASC LIMIT 50");
			else if(Constants.DB_TYPE == Constants.DB_ORACLE) sql = ("SELECT * FROM emails WHERE ROWNUM <= 50 AND email_id>? AND sent_date IS NULL AND (send_at IS null OR send_at <= ?) AND disabled=? ORDER BY email_id ASC");
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, getFromEmailId());
			ps.setTimestamp(2, new Timestamp(Tools.getNow()));
			ps.setBoolean(3, false);
			rs = ps.executeQuery();
			boolean hasResult = false;
			String recipientEmail = null;
			String recipientName = null;
			String senderName = null;
			String senderEmail = null;
			String replyTo = null;
			String ccEmail = null;
			String bccEmail = null;
			String subject = null;
			String url = null;
			String body = "";
			String attachments = null;
			int retry = 0;
			int emailId = -1;
			int campainId = -1;
			int toSendCountLimited = toSendCount;
			if (toSendCountLimited > 50) toSendCountLimited = 50;
			int random = 0;
			if (toSendCountLimited>0) random = rand.nextInt(toSendCountLimited);
			if (random < 0) random = 0;
			int pocitadlo = 0;
			while (rs.next() && pocitadlo <= random)
         {
				if (DomainThrottle.getInstance().canSend(DomainThrottle.getDomainFromEmail(DB.getDbString(rs, "recipient_email")))==false)
				{
					//ak na domenu nemozeme poslat email preskocime ho
					continue;
				}

				hasResult = true;
				setActive(true);
				emailId = rs.getInt("email_id");
				recipientEmail = DB.getDbString(rs, "recipient_email");
				replyTo = DB.getDbString(rs, "reply_to");
				ccEmail = DB.getDbString(rs, "cc_email");
				bccEmail = DB.getDbString(rs, "bcc_email");
				recipientName = DB.getDbString(rs, "recipient_name");
				senderName = DB.getDbString(rs, "sender_name");
				senderEmail = DB.getDbString(rs, "sender_email");
				subject = DB.getDbString(rs, "subject");
				url = DB.getDbString(rs, "url");
				body = DB.getDbString(rs, "message");
				attachments = DB.getDbString(rs, "attachments");
				retry = rs.getInt("retry");
				campainId = rs.getInt("campain_id");
				//recipientUserId = rs.getInt("recipient_user_id");

            pocitadlo++;
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;

			//zatvorime, lebo moze posielanie, alebo url get na stranku trvat dlho a spojenie sa timeoutne
			db_conn.close();
			db_conn = null;

			if (emailId > 0 && recipientEmail != null && DomainThrottle.getInstance().canSend(DomainThrottle.getDomainFromEmail(recipientEmail)))
			{
				if (retry > MAX_RETRY_COUNT)
				{
					db_conn = DBPool.getConnection();
					ps = db_conn.prepareStatement("UPDATE emails SET retry=?, sent_date=? WHERE email_id=?");
					ps.setInt(1, -1);
					ps.setTimestamp(2, new Timestamp((new java.util.Date()).getTime()));
					ps.setInt(3, emailId);
					ps.execute();
					ps.close();
					ps = null;
					if (campainId > 0)
					{
						ps = db_conn.prepareStatement("UPDATE emails_campain SET count_of_sent_mails=count_of_sent_mails+1, last_sent_date=? WHERE emails_campain_id=?");
						ps.setTimestamp(1, new Timestamp((new java.util.Date()).getTime()));
						ps.setInt(2, campainId);
						ps.execute();
						ps.close();
						ps = null;
					}
					toSendCount--;
					db_conn.close();
					db_conn = null;
				}
				else
				{
               db_conn = DBPool.getConnection();
               // poznac si, ze odosielame
            	ps = db_conn.prepareStatement("UPDATE emails SET retry=?, sent_date=? WHERE email_id=?");
               ps.setInt(1, 98);
            	ps.setTimestamp(2, new Timestamp((new java.util.Date()).getTime()));
               ps.setInt(3, emailId);
               ps.execute();
               ps.close();
               db_conn.close();
      			ps = null;
      			db_conn = null;
					//body = url;

               url = Tools.replace(url, ":80/", "/");
               //toto uz nerobime, https je standard url = Tools.replace(url, "https://", "http://");

					if ((url.startsWith("http://") || url.startsWith("https://")) && Tools.isEmpty(body))
					{
						try
						{
							String natUrl = Tools.natUrl(url);

							Logger.debug(Sender.class,"DOWNLOADING: " + natUrl);

							//nastav HASH
							ShowDoc.setActualUserHash(Password.generateStringHash(10));

							//disable SSL verification for httpS hosts
							if (natUrl.startsWith("https://"))
							{
								//extract host name from URL
								String host = natUrl.substring(8);
								int index = host.indexOf('/');
								if (index != -1)
								{
									host = host.substring(0, index);
								}
								//disable SSL verification
								Tools.doNotVerifyCertificates(host);
							}

							//body obsahuje URL adresu, ktoru je treba stiahnut
							URLConnection conn = null;
							URL urlCon = new URL(natUrl);
							conn = urlCon.openConnection();
							conn.setAllowUserInteraction(false);
							conn.setDoInput(true);
							conn.setDoOutput(false);
							conn.setRequestProperty("dmail", ShowDoc.ACTUAL_USER_HASH);
							conn.connect();

							String encoding = conn.getHeaderField("Content-Type");
							if (encoding==null || encoding.indexOf("charset=")==-1)
							{
								encoding = SetCharacterEncodingFilter.getEncoding();
							}
							else
							{
								encoding = encoding.substring(encoding.indexOf("charset=")+8).trim();
							}

							StringBuilder sb = new StringBuilder();
							BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
							InputStreamReader in = new InputStreamReader(is, encoding);
							char[] buffer = new char[8000];
							int n = 0;
							while (true)
							{
								 n = in.read(buffer);
								 if (n < 1)
									  break;
								 sb.append(buffer, 0, n);
							}
							in.close();
							body = sb.toString();
						}
						catch (Exception ex)
						{
							body = null;
							sk.iway.iwcm.Logger.error(ex);
						}
					}

					boolean success = false;
					if (Tools.isNotEmpty(body))
					{
						//ak by sa to tam nahodou este nachadzalo...
						body = Tools.replace(body, "!name!", recipientName);
						body = Tools.replace(body, "!email!", recipientEmail);
						body = Tools.replace(body, "!RECIPIENT_NAME!", recipientName);
						body = Tools.replace(body, "!RECIPIENT_EMAIL!", recipientEmail);
						body = Tools.replace(body, "!LOGGED_USER_EMAIL!", recipientEmail);
						body = Tools.replace(body, "!LOGGED_USER_FIRSTNAME!", recipientName);
						body = Tools.replace(body, "!LOGGED_USER_LASTNAME!", recipientName);
						body = Tools.replace(body, "!EMAIL_ID!", String.valueOf(emailId));

						//ak mame body, v URL je cesta (http://www.server.sk/nieco.html), ziskajme baseHref (http://www.server.sk)
						String baseHref = url;
						int index = url.indexOf('/', 10);
						if (index != -1)
						{
							//dalo by sa to aj z urlCon, ale to neviem zistit contextPath
							baseHref = url.substring(0, index);
						}
						if (body.contains("SENDER: DO NOT SEND THIS EMAIL") || body.contains("Chyba 404 - požadovaná stránka neexistuje") || body.contains(Prop.getInstance().getText("stat.error.404")))
						{
							//pre automaticky generovane emaily (napr. zoznam zmien)
							//ak obsahuje tento text, neposle sa to
							Logger.debug(Sender.class,"Obsahuje text SENDER: DO NOT SEND THIS EMAIL, alebo je to 404, preskakujem");
							success = true;
						}
						else
						{
							//v pripade multiDomain s externym adresarom musime nafejkovat domenu pre ziskanie priloh z inych domen pri ziskavani cesty pomocou IwcmFile.fromVirtualPath(serverPath);
							boolean fakingDomain = false;
							if (Tools.isNotEmpty(Constants.getString("cloudStaticFilesDir")) && Constants.getBoolean("multiDomainEnabled") && url.startsWith("http"))
							{
								//ziskanie domeny z baseHref
								String baseHrefDomain = baseHref;
								if (Tools.isNotEmpty(baseHrefDomain))
								{
									int urlColonAndTwoSlashes = baseHrefDomain.indexOf("://");
									if (urlColonAndTwoSlashes > 0)
									{
										baseHrefDomain = baseHrefDomain.substring(urlColonAndTwoSlashes + 3);
										int urlPortColon = baseHrefDomain.indexOf(":");
										if (urlPortColon > 0)
											baseHrefDomain = baseHrefDomain.substring(0, urlPortColon);
										else
										{
											int urlEndSlash = baseHrefDomain.indexOf("/");
											if (urlEndSlash > 0)
												baseHrefDomain = baseHrefDomain.substring(0, urlEndSlash);
										}
									}

									String domainName = DomainRedirectDB.getDomainFromAlias(baseHrefDomain);
									if (Tools.isNotEmpty(domainName)) baseHrefDomain = domainName;

									//kontrola, ci vo WJ taku domenu mame definovanu
									boolean foundWjDomain = false;
									List<String> allWjDomains = GroupsDB.getInstance().getAllDomainsList();
									for(String wjDomain : allWjDomains)
									{
										if(baseHrefDomain.equals(wjDomain))
											foundWjDomain = true;
									}

									//setnem fake domenu do RequestBean
									if (foundWjDomain && Tools.isNotEmpty(baseHrefDomain))
									{
										MockHttpServletRequest request = new MockHttpServletRequest(null, "/Sender");
										MockHttpSession session = new MockHttpSession(null);
										request.setSession(session);
										request.setServerName("");
										SetCharacterEncodingFilter.registerDataContext(request);
										RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
										Logger.debug(Sender.class, "baseHrefDomain=" + baseHrefDomain + ", RequestBean=" + rb);
										if (rb != null) rb.setDomain(baseHrefDomain);
										fakingDomain = true;
									}
								}
							}

							//meranie pridavame len na stahovane texty (hromadny email) nie do mailov posielanych cez iny node clustra
							if (url.startsWith("http"))
							{
								//uprav linky - pridaj info pre statistiku kliknuti
								body = addClickInfo(body, emailId, baseHref);
								body = addOpenTrackingImg(body, emailId);
							}

							String dmailListUnsubscribeBaseHref = Constants.getString("dmailListUnsubscribeBaseHref", baseHref);
							//it must be httpS otherwise it will be ignored by Apple Mail and maybe other clients too
							StringBuilder unsubscribedUrl = new StringBuilder("<").append(Tools.replace(dmailListUnsubscribeBaseHref, "http://", "https://"));
							String hash = getClickHash(emailId);
							unsubscribedUrl.append("/rest/dmail/unsubscribe?"+Constants.getString("dmailStatParam")+"="+hash);
							unsubscribedUrl.append(">");

							Pair<Boolean, Exception> result = new MailHelper()
								.setFromName(senderName)
								.setFromEmail(senderEmail)
								.addRecipient(recipientEmail)
								.setReplyTo(replyTo)
								.setCcEmail(ccEmail)
								.setBccEmail(bccEmail)
								.setSubject(subject)
								.setMessage(body)
								.setBaseHref(baseHref)
								.setAttachments(attachments)
								.setSendLaterWhenException(false)
								.setWriteToAuditLog(true)
								.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click")
								.addHeader("List-Unsubscribe", unsubscribedUrl.toString())
								.sendCapturingException();

							//odregistrujem data context, ktory bol vytvoreny kvoli fejkovaniu domen
							if(fakingDomain) SetCharacterEncodingFilter.unRegisterDataContext();

							DomainThrottle.getInstance().addEmail(DomainThrottle.getDomainFromEmail(recipientEmail), System.currentTimeMillis());
							success = result.first;
							//posielame email na zlu adresu, s tym nic nespravime ani na viacero pokusov
							if (!success && result.second != null)
							{
								Logger.debug(Sender.class, "Message: "+result.second.getMessage());
								StringTokenizer st = new StringTokenizer(Constants.getString("dmailBadEmailSmtpReplyStatuses"), ",;");
								while (st.hasMoreTokens())
								{
									String status = st.nextToken().trim();
									if (result.second.getMessage().contains(status))
									{
										Logger.debug(Sender.class, "Povazujem za success, server vratil "+status);
										success = true;
										break;
									}
								}
							}
						}
					}

					db_conn = DBPool.getConnection();

					if (success)
					{
						long now = (new java.util.Date()).getTime();
						ps = db_conn.prepareStatement("UPDATE emails SET retry=?, sent_date=? WHERE email_id=?");
						ps.setInt(1, retry + 1);
						ps.setTimestamp(2, new Timestamp(now));
						ps.setInt(3, emailId);
						ps.execute();
						ps.close();
						ps = null;
						if (campainId > 0)
						{
							ps = db_conn.prepareStatement("UPDATE emails_campain SET count_of_sent_mails=count_of_sent_mails+1, last_sent_date=? WHERE emails_campain_id=?");
							ps.setTimestamp(1, new Timestamp(now));
							ps.setInt(2, campainId);
							ps.execute();
							ps.close();
							ps = null;
						}

						toSendCount--;
					}
					else
					{
						ps = db_conn.prepareStatement("UPDATE emails SET retry=?, sent_date=? WHERE email_id=?");
						ps.setInt(1, retry + 1);
						ps.setNull(2, Types.DATE);
						ps.setInt(3, emailId);
						ps.execute();
						ps.close();
						ps = null;
					}
					db_conn.close();
					db_conn = null;
					if (!success)
					{
						try
						{


							Logger.println(Sender.class,"Sender: email from: " + senderEmail + " to: " + recipientEmail);
							Logger.println(Sender.class, "- nepodarilo sa odoslat email, zaspavam na "+sleepTime);
							//pockajme, kym sa SMTP server spamata
							if (sleepTime > 0) Thread.sleep(sleepTime);
						}
						catch (Exception e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
					}
				}
			}

			if (hasResult == false)
			{
				//this.cancel();
				//setActive(false);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			try
			{
				Logger.println(Sender.class, "- nepodarilo sa odoslat email, zaspavam na 3x"+sleepTime);
				if (sleepTime > 0) Thread.sleep(sleepTime*3L);
			}
			catch (Exception e)
			{

			}
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
		runActive = false;
	}


	private String addOpenTrackingImg(String body, int emailId)
	{
		if(Tools.isEmpty(Constants.getString("dmailTrackopenGif")) || Constants.getString("dmailTrackopenGif").length()<2)
			return body;
		Logger.debug(getClass(), "Adding open tracking image");
		if (emailId < 1) return(body);
		String value = Integer.toString(emailId);
		int endOfBody = body.indexOf("</body>");
		if(endOfBody != -1)
		{
			String trackGif = Constants.getString("dmailTrackopenGif");
			if (trackGif!=null && trackGif.length()>3)
			{
				body = new StringBuffer(body).insert(endOfBody, ("<img src=\""+trackGif+"?emailId="+value+"\" style=\"display:none;\" />").toCharArray()).toString();
			}
		}
		return body;
	}

	public void cancelTask()
	{
		Logger.debug(Sender.class, "destroying sender");

		if (timer != null) timer.cancel();
		this.cancel();
		setActive(false);
		Constants.getServletContext().removeAttribute(CONTEXT_NAME);
	}

	/**
	 *  Sets the active attribute of the Sender object
	 *
	 *@param  active  The new active value
	 */
	public void setActive(boolean active)
	{
		this.active = active;
	}

	/**
	 *  Gets the active attribute of the Sender object
	 *
	 *@return    The active value
	 */
	public boolean isActive()
	{
		return active;
	}
	public int getToSendCount()
	{
		if (toSendCount < 0) toSendCount = 0;
		return toSendCount;
	}

	/**
	 * Do tela stranky prida linkam statistiku pre kliknutia
	 * @param body
	 * @return
	 */
	public static String addClickInfo(String body, int emailId,String baseHref)
	{
		if (emailId < 1) return(body);

		String value = getClickHash(emailId);
		String param = Constants.getString("dmailStatParam");

		if (Tools.isEmpty(param) || param.length()<2) return body;

		int failsafe = 500;
		int counter = 0;
		int start;
		int end;
		String oldLink;
		String newLink;
		start = body.indexOf("href=\"");
		while (counter < failsafe && start!=-1)
		{
			end = body.indexOf("\"", start+6);
			if (end > start)
			{
				oldLink = body.substring(start+6, end);
				if ( ( oldLink.trim().startsWith("http")==false || Constants.getBoolean("replaceExternalLinks"))  && oldLink.trim().startsWith("javascript")==false && oldLink.trim().startsWith("mailto")==false  && !oldLink.contains(baseHref) && !oldLink.contains("/combine.jsp"))
				{
					newLink = baseHref;
					if (oldLink.trim().startsWith("http"))
					{
						//ten &amp; vraj robil v niektorych mail klientoch chyby, tiket 9576
						newLink = Tools.addParameterToUrlNoAmp(baseHref, param, value);

						Base64 b64 = new Base64();
						String base64encoded = new String(b64.encode(oldLink.getBytes()));
						base64encoded = Tools.replace(base64encoded, "=", "|");

						newLink = Tools.addParameterToUrlNoAmp(newLink, "extURL", base64encoded);
					}
					else
					{
						newLink = Tools.addParameterToUrlNoAmp(oldLink, param, value);
					}
					body = Tools.replace(body, "href=\"" + oldLink + "\"", "href=\"" + newLink + "\"");
				}
			}
			start = body.indexOf("href=\"", end);
			counter++;
		}
		return(body);
	}

	/**
	 * Vygeneruje a do DB ulozi nahodny hash pre zadany emailId
	 * @param emailId
	 * @return
	 */
	private static String getClickHash(int emailId) {
		String hash = (new SimpleQuery()).forString("SELECT click_hash FROM emails WHERE email_id=?", emailId);
		if (Tools.isEmpty(hash)) {
			hash = String.valueOf(emailId) + Password.generateStringHash(16);
			//uloz ho do DB
			(new SimpleQuery()).execute("UPDATE emails SET click_hash=? WHERE email_id=?", hash, emailId);
		}
		return hash;
	}

	/**
	 * Verifikuje, ci zadany hash pre kliknutie je platny, ak ano, vrati ID emailu (email_id), inak vrati -1
	 * @param hash
	 * @return
	 */
	public static int getEmailIdFromClickHash(String hash) {

		//v starej verzii bol hash rovno ID emailu, takze testujeme click_hash alebo email_id ak je to cislo
		int emailId = Tools.getIntValue(hash, 0);
		int dbEmailId = (new SimpleQuery()).forInt("SELECT email_id FROM emails WHERE click_hash=? OR (click_hash IS NULL AND email_id=?)", hash, Integer.valueOf(emailId));

		if (dbEmailId>0) return dbEmailId;

		return -1;
	}

	public static void setMaxRetryCount(int count) {
		Sender.MAX_RETRY_COUNT = count;
	}
}
