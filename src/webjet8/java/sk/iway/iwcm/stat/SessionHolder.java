package sk.iway.iwcm.stat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Toto drzi globalne info o session pouzivatelov, pretoze SessionListener pri
 *  ukonceni session nema pristup k datam v session a teda nie je mozne zistit
 *  aky bol posledny docId a aky to bol server
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Piatok, 2002, máj 31
 *@modified     $Date: 2003/02/23 16:56:22 $
 */
public class SessionHolder
{
   /**
    *  Description of the Field
    */
   public static final String SESSION_HOLDER = "iwcm_session_holder";
	private Map<String, SessionDetails> data = Collections.synchronizedMap(new Hashtable<String, SessionDetails>());

	protected static final String INVALIDATE_SESSION_ADDR = "INVALIDATE";

	/**
	 * Takyto konstruktor sa normalne nesmie pouzivat!
	 *
	 */
	public SessionHolder()
	{

	}

   public static SessionHolder getInstance()
   {
   	return(getInstance(Constants.getServletContext()));
   }

   /**
    *  Gets the instance attribute of the SessionHolder object
    *
    *@param  servletContext  Description of the Parameter
    *@return                 The instance value
    */
   public static SessionHolder getInstance(javax.servlet.ServletContext servletContext)
   {
      if (servletContext!=null && servletContext.getAttribute(SESSION_HOLDER) != null)
      {
         return ((SessionHolder) servletContext.getAttribute(SESSION_HOLDER));
      }
      return (new SessionHolder(servletContext));
   }

   /**
    *  Constructor for the SessionHolder object
    *
    *@param  servletContext  Description of the Parameter
    */
   private SessionHolder(javax.servlet.ServletContext servletContext)
   {
      //Logger.println(this,"SessionHolder: constructor");
		data = Collections.synchronizedMap(new Hashtable<String, SessionDetails>());
      servletContext.setAttribute(SESSION_HOLDER, this);
   }

   /**
    * Vrati aktualny zoznam ludi v session
    * @return
    */
   public List<SessionDetails> getList()
   {
   		cleanup();

		List<SessionDetails> list = new ArrayList<>();
		Collection<Entry<String, SessionDetails>> entries = null;
		synchronized (data)
		{
			entries = new Vector<>(data.entrySet());
		}
		for (Entry<String, SessionDetails> me : entries)
		{
			if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true)
			{
				if (me.getValue().getDomainId() != CloudToolsForCore.getDomainId())
				{
					continue;
				}
			}
			list.add(me.getValue());
		}
		return list;
   }

   /**
    * Nastavi hodnoty pre session holder a skontroluje session stealing, ked nastane session stealing vrati false
    * @param sessionId
    * @param lastURL
    * @param request
    * @return
    */
   	public boolean set(String sessionId, String lastURL, HttpServletRequest request) {
		// lebo iframe a cookie toho potom vygeneruje kopec
		if (lastURL != null && (lastURL.indexOf("/admin/mem.jsp") != -1 || lastURL.indexOf("/admin/refresher.jsp") != -1
				|| lastURL.indexOf("/admin/divpopup-blank.jsp") != -1 || lastURL.indexOf("/admin/FCKeditor") != -1))
			return true;

		// aby bolo mozne grafy tlacit do PDF, normalne by nam nastal session stealing
		// check
		if (lastURL != null
				&& (lastURL.startsWith("/admin/statchartnew.do") || lastURL.startsWith("/admin/statchart.do")))
			return true;

		// multiupload robi cachre machre so session, radsej vynechavam
		if (lastURL != null && lastURL.startsWith("/admin/multiplefileupload.do"))
			return true;

		boolean newSession = false;
		SessionDetails det = get(sessionId);

		if (lastURL != null && lastURL.indexOf("/admin/rest/refresher") != -1 && det != null) {
			//invalidate if needed
			if (INVALIDATE_SESSION_ADDR.equals(det.getRemoteAddr())) {
				request.getSession().invalidate();
				return false;
			}
			//do not throw other errors because refresher is called in background
			return true;
		}

		if (det == null) {
			cleanup();
			newSession = true;

			det = new SessionDetails();
			det.setLogonTime(Tools.getNow());
			det.setRemoteAddr(Tools.getRemoteIP(request));
		} else {
			Identity sessionUser = UsersDB.getCurrentUser(request);

			if (INVALIDATE_SESSION_ADDR.equals(det.getRemoteAddr())) {
				Logger.debug(SessionHolder.class, "Session invalidated for userId=" + det.getLoggedUserId() + " userName="+ det.getLoggedUserName() + " sessionId=" + sessionId);
				request.getSession().invalidate();
				return false;
			}

			if (Constants.getBoolean("sessionStealingCheck") == true
					&& det.getRemoteAddr().equals(Tools.getRemoteIP(request)) == false) {
				// session stealing vyvolame len ak je niekto prihlaseny
				if (sessionUser != null || det.getLoggedUserId() > 0) {
					String sessionUserData = "";
					if (sessionUser != null)
						sessionUserData = " suid=" + sessionUser.getUserId() + " " + sessionUser.getFullName();

					String description = "SESSION STEALING, sessionId=" + sessionId + " userId=" + det.getLoggedUserId()
							+ " " + det.getLoggedUserName() + " logonDate=" + Tools.formatDateTime(det.getLogonTime())
							+ " session IP=" + det.getRemoteAddr() + " req IP=" + Tools.getRemoteIP(request) + " "
							+ sessionUserData;

					Logger.error(SessionHolder.class, description);

					Adminlog.add(Adminlog.TYPE_XSS, description, -1, -1);

					request.getSession().invalidate();
					return false;
				}
			}
		}
		det.setLastURL(lastURL);
		det.setDomainId(CloudToolsForCore.getDomainId());
		det.setDomainName(CloudToolsForCore.getDomainName());

		Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
		if (user != null) {
			if (det.getLoggedUserId() < 1 || det.getLoggedUserId() != user.getUserId()) {
				det.setLoggedUserId(user.getUserId());
				det.setAdmin(user.isAdmin());
				det.setLoggedUserName(user.getFullName());

				// Handle single logon - invalidate other sessions for the same user
				keepOnlySession(user.getUserId(), sessionId);
			}
		} else {
			if (det.getLoggedUserId() > 0) {
				det.setLoggedUserId(-1);
				det.setAdmin(false);
				det.setLoggedUserName(null);
			}
		}

		// ziskaj IP a remoteHost
		det.setLastActivity(Tools.getNow());
		det.setSessionId(sessionId);
		data.put(sessionId, det);

		if(newSession == true && det.isAdmin() == true) {
			// After new session was added (logon for example) - update session stat data
			SessionClusterHandler.main(null);
		}
		return true;
	}

   /**
    * Nastavi atribut lastDocId na sessionDetails, vola sa zo StatDB statistiky stranok
    * @param sessionId
    * @param lastDocId
    */
   public void setLastDocId(String sessionId, int lastDocId)
   {
      SessionDetails det = get(sessionId);
      if (det != null)
   	{
   		det.setLastDocId(lastDocId);
   		data.put(sessionId, det);
   	}
   }

   /**
    *  Description of the Method
    *
    *@param  sessionId  Description of the Parameter
    *@return            Description of the Return Value
    */
   public SessionDetails get(String sessionId)
   {
      return data.get(sessionId);
   }

   /**
    *  Description of the Method
    *
    *@param  sessionId  Description of the Parameter
    */
   public void remove(String sessionId)
   {
   	SessionDetails ses = get(sessionId);
   	if (ses != null)
   	{
   		ses = null;
   	}
      data.remove(sessionId);

      cleanup();
   }

   /**
    * Vrati aktualny pocet sessions aktualneho clustru (kolko ludi si cita stranku)
    * @return
    */
   public static int getTotalSessionsPerNode()
   {
   	SessionHolder sh = SessionHolder.getInstance();
   	return(sh.getList().size());
   }

   /**
    * Vrati aktualny pocet sessions (kolko ludi si cita stranku)
    *
    * Ak system bezi v cluster mode vrati sucet poctu sessions jednotlivych clustrov, ktore sa ukladaju do _conf_ cez MonitoringManager (cron)
    * Ak system nebezi v cluster mode, vratena hodnota je identicka s hodnotou ktoru vrati <code>getTotalSessionsPerNode()</code>
    *
    * @return
    */
   public static int getTotalSessions()
   {
   	if(ClusterDB.isServerRunningInClusterMode())
   	{
   		int totalSessions=Constants.getInt("statSessionsAllNodes");
   		if(totalSessions<=0)
   			totalSessions = getTotalSessionsPerNode();
   		return totalSessions;

   	}
   	else
   		return getTotalSessionsPerNode();
   }


   /**
    * Vrati pocet prihlasenych pouzivatelov aktualneho clustru. Pod prihlasenym pouzivatelom sa rozumie
    * pouzivatel s ID vacsim ako 0, pricom vsetky otvorene session sa pri pocitani
    * agreguju ako jeden pouzivatel.
    *
    * @return {@link Integer}
    */
   public static int getDistinctUsersCountPerNode()
   {
		//pouziva Cache - iteruje nad vsetkymi pouzivatelmi a uzamkyna pritom
		//zamok (ak by nezamkynal - mozny ConcurrentModificationException) => mozny bottleNeck
   	//notNull && isInteger
   	if (Cache.getInstance().getObject("distinctUserCount") instanceof Integer)
   		return (Integer)Cache.getInstance().getObject("distinctUserCount");

   	SessionHolder sh = SessionHolder.getInstance();

   	Set<Integer> userIds = new HashSet<>();
   	//vyrobime si kopiu
   	List<SessionDetails> sessionList = null;
   	synchronized(getInstance().data)
   	{
   		sessionList = new ArrayList<>(sh.getList());
   	}

   	for (SessionDetails session : sessionList)
   	{
   		if (session.getLoggedUserId() > 0)
   			userIds.add( session.getLoggedUserId() );
   	}

   	final int SECONDS_TO_LAST_IN_CACHE = 10;
   	Cache.getInstance().setObjectSeconds("distinctUserCount",userIds.size(),SECONDS_TO_LAST_IN_CACHE);

   	return userIds.size();
   }

	/**
    * Vrati pocet prihlasenych pouzivatelov. Pod prihlasenym pouzivatelom sa rozumie
    * pouzivatel s ID vacsim ako 0, pricom vsetky otvorene session sa pri pocitani
    * agreguju ako jeden pouzivatel.
    *
    * Ak system bezi v cluster mode vrati sucet pouzivatelov jednotlivych clustrov, ktore sa ukladaju do _conf_ cez MonitoringManager (cron)
    * Ak system nebezi v cluster mode, vratena hodnota je identicka s hodnotou ktoru vrati <code>getDistrinctUsersCountPerNode()</code>
    *
    * @return {@link Integer}
    */
   public static int getDistinctUsersCount()
   {
   	if(ClusterDB.isServerRunningInClusterMode())
   	{
   		int totalUsers=Constants.getInt("statDistinctUsersAllNodes");
   		if(totalUsers<=0)
   			totalUsers = getDistinctUsersCountPerNode();
   		return totalUsers;
   	}
   	else
   		return getDistinctUsersCountPerNode();
   }

   /**
    * Zrusi stare neaktivne sessions
    *
    */
   	private void cleanup() {
		try {
			SessionDetails sd;
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -Constants.getInt("sessionRemoveTimeout"));
			long removeTime = cal.getTimeInMillis();

			synchronized (data) {
				Set<String> keys = data.keySet();
				List<String> myKeysList = new ArrayList<>();
				for (String key : keys) {
					myKeysList.add(key);
				}
				for (String key : myKeysList) {
					sd = data.get(key);
					if (sd == null || sd.getLastActivity() < removeTime) {
						if (sd == null) {
							Logger.debug(SessionHolder.class, "Removing session: " + key + " sd=null");
						} else {
							Logger.debug(SessionHolder.class,
									"Removing session: " + key + " la=" + Tools.formatDateTime(sd.getLastActivity()));
						}
						data.remove(key);
					}
				}
			}
		} catch (Exception e) {
			// sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Nastavi vsetkym ostatnym session atribut pre ich invalidovanie (napr. po zmene hesla)
	 */
	public void invalidateOtherUserSessions()
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null || rb.getUserId()<1) return;

		invalidateOtherUserSessions(rb.getUserId());
	}

	/**
	 * Invalidate other sessions for user with userId, call this after password change
	 * @param userId - ID of user changed password
	 */
	public void invalidateOtherUserSessions(int userId)
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null || rb.getUserId()<1) return;

		keepOnlySession(userId, rb.getSessionId());
	}

	/**
	 * Invalidate other sessions for userId from data map, also handles cluster propagation
	 * REQUIRES: sessionSingleLogon=true
	 * @param userId
	 * @param currentSessionId
	 */
	public void keepOnlySession(int userId, String currentSessionId) {

		if (Constants.getBoolean("sessionSingleLogon") != true) return;

		//propagate to cluster - invalidate sessions for this user ID on other nodes
		String baseName = "SessionHolder.keepOnlySession-"+userId+"-";
		//we need to delete old records, because they will be executed first, and this should be last/only one in queue
		ClusterDB.deleteStartsWith(baseName);
		ClusterDB.addRefresh(baseName+currentSessionId);

		for (Map.Entry<String, SessionDetails> entry : data.entrySet())
		{
			String sessionId = entry.getKey();
			if (Tools.isEmpty(sessionId) || sessionId.equals(currentSessionId)) continue;

			SessionDetails sd = entry.getValue();
			if (sd == null) continue;

			if (sd.getLoggedUserId() == userId)
			{
				//destroy session
				sd.setRemoteAddr(INVALIDATE_SESSION_ADDR);
				Logger.debug(SessionHolder.class, "Invalidating session: " + sessionId + " uid=" + sd.getLoggedUserId());
			}
		}
	}

	public boolean invalidateSession(String sessionId) {
		// Check if sessionId is empty and return false if so
		if(Tools.isEmpty(sessionId)) return false;

		//Session can be on another cluster node, call cluster refresh
		ClusterDB.addRefresh("SessionHolder.invalidateSession-" + sessionId);

		SessionDetails sd = get(sessionId);
		if(sd != null) {
			sd.setRemoteAddr(INVALIDATE_SESSION_ADDR);
			Logger.debug(SessionHolder.class, "Invalidating session: " + sessionId + " uid=" + sd.getLoggedUserId());
			//Refresh data
			SessionClusterHandler.main(null);
			return true;
		}
		return false;
	}

	/**
	 * Get data map for testing purposes
	 * @return data map
	 */
	protected java.util.Map<String, SessionDetails> getDataMap() {
		return data;
	}
}
