package sk.iway.iwcm;

import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  SpamProtection.java
 *
 *	This class keeps a track of user's posts on our server and forbids any more posts, if
 *	he already reached a limit.
 *
 * Behaviour of this class can be modified by configuration parameters:
 * 1. spamProtectionTimeout - what is the maximum time span between two distinct posts
 * 2. spamProtectionHourlyLimit - limit of posts that a user can send in an hour
 *
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      Date: 21.5.2009 15:30:15
 *@modified     $Date: 2010/02/09 08:32:42 $
 */
public class SpamProtection
{

	private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public static final String HOURLY_LIMIT_KEY = "spamProtectionHourlyLimit";

	//POZOR: toto nie je limit za minutu, ale za 5 minut
	public static final String MINIT_LIMIT_KEY = "spamProtectionMinitLimit";

	public static final String MINUTE_LIMIT_KEY = "spamProtectionMinuteLimit";

	public static final String TIMEOUT_KEY = "spamProtectionTimeout";

	private static final int LOCKS_COUNT = 16;

	private static Object[] locks;

	/**
	 * Keeps all the post records for past hour
	 */
	static Map<String, Map<String, UserTrackMinuteRecord>> userMinuteTracking = new ConcurrentHashMap<>();

	/**
	 * Keeps all the post records for past hour
	 */
	static Map<String,Map<String,UserTrackRecord>> userTracking = new ConcurrentHashMap<>();

	static Map<String, Map<String, UserTrackMinitRecord>> userMinitTracking = new ConcurrentHashMap<>();

	/**
	 * When has a certain IP posted for the last time?
	 */
	static Map<String, Long> lastAccessedTime = new ConcurrentHashMap<>();

	static Map<String, Long> lastAccessedTimeInMinit = new ConcurrentHashMap<>();


	static
	{
		locks = new Object[LOCKS_COUNT];

		for (int i=0;i<LOCKS_COUNT;i++)
			locks[i] = new Object();

		/**
		 *	This thread will continuosly delete records older than one hour
		 */
		Runnable clearer = clearerThread();
		executor.scheduleAtFixedRate(clearer, 60, 60, TimeUnit.SECONDS);
	}

	protected SpamProtection() {
		//utility class
	}

	private static Runnable clearerThread()
	{
		Runnable clearer = new Runnable()
		{
			public void run()
			{
				clearOldData();
			}
		};
		return clearer;
	}

	protected static void clearOldData() {
		try {
			Set<String> allIps;
			Set<String> allIpsInMinit;
			long now = System.currentTimeMillis();
			long hourAgo = now - 60 * 1000 * 60;
			long fiveMinitAgo = now - 60 * 1000 * 5;

			allIps = lastAccessedTime.keySet();
			allIpsInMinit = lastAccessedTimeInMinit.keySet();

			for (String ip : allIps)
			{
				int hashCode = ip.hashCode();
				if (hashCode < 0)
					hashCode *= -1;
				synchronized (locks[hashCode % LOCKS_COUNT])
				{
					if (lastAccessedTime.get(ip).longValue() < hourAgo)
					{
						userTracking.remove(ip);
						lastAccessedTime.remove(ip);
					}
				}
			}

			for (String ip : allIpsInMinit)
			{
				int hashCode = ip.hashCode();
				if (hashCode < 0)
					hashCode *= -1;
				synchronized (locks[hashCode % LOCKS_COUNT])
				{
					if (lastAccessedTimeInMinit.get(ip).longValue() < fiveMinitAgo)
					{
						userMinitTracking.remove(ip);
						lastAccessedTimeInMinit.remove(ip);
					}
				}
			}

			allIps = userTracking.keySet();
			for (String ip : allIps)
			{
				int hashCode = ip.hashCode();
				if (hashCode < 0)
					hashCode *= -1;
				synchronized (locks[hashCode % LOCKS_COUNT])
				{
					Map<String, UserTrackRecord> moduleRecords = userTracking.get(ip);
					for (UserTrackRecord r : moduleRecords.values())
						r.cleanOldRecords();
				}
			}

			allIpsInMinit = userMinitTracking.keySet();
			for (String ip : allIpsInMinit)
			{
				int hashCode = ip.hashCode();
				if (hashCode < 0)
					hashCode *= -1;
				synchronized (locks[hashCode % LOCKS_COUNT])
				{
					Map<String, UserTrackMinitRecord> moduleRecords = userMinitTracking.get(ip);
					for (UserTrackMinitRecord r : moduleRecords.values())
						r.cleanOldRecords();
				}
			}
		} catch (Exception e) {
			Logger.error(SpamProtection.class, "Error in SpamProtection clearer thread: " + e.getMessage());
		}
	}

	/**
	 * Determines, whether a post to a given module and from a given
	 * IP address is allowed to be posted.
	 *
	 *
	 * @param module	Module, which is used to make a post
	 * @param post		Text version of a post - unused
	 * @param request
	 * @return {@link Boolean} - whether the post can be performed or not
	 */
	public static boolean canPost(String module, String post, HttpServletRequest request)
	{
		String ip = Tools.getRemoteIP(request);

		if (Tools.isEmpty(ip))
			return false;

		String enabledIPs = Constants.getString("spamProtectionDisabledIPs");
		if(Tools.isNotEmpty(enabledIPs)) {
			if (Tools.checkIpAccess(request, "spamProtectionDisabledIPs")) {
				return true;
			}
		}

		UserTrackRecord trackRecord;

		int hashCode = ip.hashCode();

		if (hashCode < 0) {
			hashCode *= -1;
		}

		int ignoreRequests = Constants.getInt("spamProtectionIgnoreFirstRequests-"+module, -1);
		if (ignoreRequests == -1) ignoreRequests = Constants.getInt("spamProtectionIgnoreFirstRequests", 0);

		synchronized(locks[hashCode % LOCKS_COUNT])
		{
			trackRecord = UserTrackRecord.getTrackRecordsFor(ip, module);

			long timeout = Constants.getInt(TIMEOUT_KEY+"-"+module);
			if (timeout < 1 && timeout >= -1) {
				timeout = Constants.getInt(TIMEOUT_KEY);
			}

			if (timeout > 0)
			{
				//java measures time in miliseconds
				timeout *= 1000;

				long now = System.currentTimeMillis();

				long lastPost = trackRecord.getLastPostTime(ignoreRequests);

				lastAccessedTime.put(ip, now);

				if ((now - lastPost) <= timeout)
				{
					RequestBean.addError(String.format("SpamProtection limit timeout (limit: %d, IP: %s, timeout: %d)", LOCKS_COUNT, ip, timeout));
					return false;
				}
			}
		}
		return trackRecord.getToken(module);
	}

	/**
	 * Kontrola počtu zápisov za minútu
	 * @param module
	 * @param request
	 * @return
	 */
	public static long crossMinuteLimit(String module, HttpServletRequest request){		//musim zratat pocet prehladavani za minutu
		String ip = Tools.getRemoteIP(request);

		if (Tools.isEmpty(ip))
			return -1;

		UserTrackMinuteRecord trackRecord;

		int hashCode = ip.hashCode();

		if (hashCode < 0)
			hashCode *= -1;

		synchronized(locks[hashCode % LOCKS_COUNT])
		{
			trackRecord = UserTrackMinuteRecord.getTrackRecordsFor(ip, module);
			long wait = trackRecord.getWaitTime(module);	//najskor vyhodnotim
			trackRecord.getToken(module);	//potom pridam
			return wait;
		}
	}

	/**
	 * Momentalne kvoli ing insurance
	 * Systém CMS dovolí prepísať profil maximálne 3x za sebou v priebehu jednej minúty, v takom
	 * prípade povolí zápis až 5 minút od posledného zápisu
	 * @param module
	 * @param request
	 * @return
	 */
	public static long crossMinitLimit(String module, HttpServletRequest request){		//musim zratat pocet prehladavani za minutu
		String ip = Tools.getRemoteIP(request);

		if (Tools.isEmpty(ip))
			return -1;

		UserTrackMinitRecord trackRecord;

		int hashCode = ip.hashCode();

		if (hashCode < 0)
			hashCode *= -1;

		synchronized(locks[hashCode % LOCKS_COUNT])
		{
			trackRecord = UserTrackMinitRecord.getTrackRecordsFor(ip, module);
			long wait = trackRecord.getWaitTime(module);	//najskor vyhodnotim
			trackRecord.getToken(module);	//potom pridam
			return wait;
		}
	}

	public static int getMinitPostLimit(String module)
	{
		int minitLimit = Constants.getInt(MINIT_LIMIT_KEY+"-"+module);
		if (minitLimit < 1 && minitLimit >= -1) minitLimit = Constants.getInt(MINIT_LIMIT_KEY);
		return minitLimit;
	}

	public static int getHourlyPostLimit(String module)
	{
		int hourlyLimit = Constants.getInt(HOURLY_LIMIT_KEY+"-"+module);
		if (hourlyLimit < 1 && hourlyLimit >= -1) hourlyLimit = Constants.getInt(HOURLY_LIMIT_KEY);
		return hourlyLimit;
	}

	public static long getTimeout(String module)
	{
		long timeout = Constants.getInt(TIMEOUT_KEY+"-"+module);
		if (timeout < 1 && timeout >= -1) timeout = Constants.getInt(TIMEOUT_KEY);
		return timeout;
	}


	/**
	 * Vracia cas do vyprsania obmedzenia
	 * !Pozor, neriesi rychle klikanie pouzivatela, len hodinovy Limit!
	 *
	 * Standardne, ak netreba ziskavat cas, staci volat metodu canPost(module, post, request)
	 * */
	public static long getWaitTimeout(String module, HttpServletRequest request){
		String ip = Tools.getRemoteIP(request);

		if (Tools.isEmpty(ip))
			return -1;

		UserTrackRecord trackRecord;

		int hashCode = ip.hashCode();

		if (hashCode < 0)
			hashCode *= -1;

		synchronized(locks[hashCode % LOCKS_COUNT])
		{
			trackRecord = UserTrackRecord.getTrackRecordsFor(ip, module);

			return trackRecord.getWaitTime(module);
		}
	}

	protected static void fakeFirstPostTimeForTesting(String module, String ip, long time) {
		UserTrackRecord trackRecord = UserTrackRecord.getTrackRecordsFor(ip, module);
		trackRecord.records.add(0, time);
	}


	private static class UserTrackRecord
	{

		final List<Long> records;

		private UserTrackRecord(String module,String userIp)
		{
			this.records = new ArrayList<>(); //getHourlyPostLimit(module) - jeeff: zrusene, zbytocne alokujeme velke pole
		}

		static UserTrackRecord getTrackRecordsFor(String ip,String module)
		{
			Map<String,UserTrackRecord> hisRecords = userTracking.get(ip);

			if (hisRecords == null)
				hisRecords = new Hashtable<>();

			UserTrackRecord theRecord = hisRecords.get(module);
				if (theRecord == null)
					theRecord = new UserTrackRecord(module,ip);

				hisRecords.put(module, theRecord);
				userTracking.put(ip, hisRecords);
			return theRecord;
		}

		public void cleanOldRecords()
		{
			long now = System.currentTimeMillis();
			long hourAgo = now - 60*1000*60;

			while(!records.isEmpty())
			{
				if (records.get(0) < hourAgo)
					records.remove(0);
				else
					break;
			}
		}

		public boolean getToken(String module)
		{
			int hourlyLimit = getHourlyPostLimit(module);
			//limit je vypnuty pri nastaveni na -2
			if (hourlyLimit < 1) return true;

			if (records.size() >= hourlyLimit)
			{
				RequestBean.addError("SpamProtection limit pocet");
				return false;
			}
			long now = System.currentTimeMillis();
			records.add(now);
			return true;
		}

		public long getWaitTime(String module){
			long now = System.currentTimeMillis();

			int hourlyLimit = getHourlyPostLimit(module);
			if (hourlyLimit==-2) return 0;

			if (records.size() >= hourlyLimit){
				long wait = getFirstPostTime() + 60*1000*60 - now + 1;	//napr. 40 sekund => 0 minut -> preto +1
				if (wait > 0L) return wait;
				else if (wait < -1000L) {
					//cleaner thread is not working, there is old records, clean them now
					SpamProtection.clearOldData();
				}
			}
			return 0;
		}

		public long getLastPostTime(int ignoreRequests)
		{
			if (records.size() == 0 || records.size() <= ignoreRequests) {
				return 0;
			}
			return records.get( records.size() - 1 );
		}

		/*public long getLastPostTime()
		{
			if (records.size() == 0)
				return 0;
			return records.get( records.size() - 1 );
		}*/

		public long getFirstPostTime()
		{
			if (records.size() == 0)
				return 0;
			return records.get(0);
		}

		/*public List<Long> getRecordsForLast(Long time) {
			return records;
		}*/

	}

	private static class UserTrackMinitRecord
	{

		final List<Long> records;

		private UserTrackMinitRecord(String module,String userIp)
		{
			this.records = new ArrayList<>();
		}

		static UserTrackMinitRecord getTrackRecordsFor(String ip,String module)
		{
			Map<String,UserTrackMinitRecord> hisRecords = userMinitTracking.get(ip);
			if (hisRecords == null)
				hisRecords = new Hashtable<>();
			UserTrackMinitRecord theRecord = hisRecords.get(module);
			if (theRecord == null)
				theRecord = new UserTrackMinitRecord(module,ip);

			hisRecords.put(module, theRecord);
			userMinitTracking.put(ip, hisRecords);

			return theRecord;
		}

		public void cleanOldRecords()
		{
			long now = System.currentTimeMillis();
			long fiveMinitAgo = now - 60 * 1000 * 5;

			while(!records.isEmpty())
			{
				if (records.get(0) < fiveMinitAgo)
					records.remove(0);
				else
					break;
			}
		}

		public boolean getToken(String module)
		{
			if (records.size() >= getMinitPostLimit(module))
				return false;
			long now = System.currentTimeMillis();
			long minitAgo = now - 60 * 1000;
			if(this.getFirstPostTime() < minitAgo)	records.clear();
			records.add(now);
			return true;
		}

		public long getWaitTime(String module){
			long now = System.currentTimeMillis();
			if (records.size() >= getMinitPostLimit(module)){
				long wait = getLastPostTime() + 60*1000*5 - now;
				return wait;
			}
			return 0;
		}

		public long getLastPostTime()
		{
			if (records.size() == 0)
				return 0;
			return records.get( records.size() - 1 );
		}

		public long getFirstPostTime()
		{
			if (records.size() == 0)
				return 0;
			return records.get(0);
		}

	}

	private static class UserTrackMinuteRecord
	{

		final List<Long> records;

		private UserTrackMinuteRecord(String module,String userIp)
		{
			this.records = new ArrayList<>();
		}

		static UserTrackMinuteRecord getTrackRecordsFor(String ip,String module)
		{
			Map<String,UserTrackMinuteRecord> hisRecords = userMinuteTracking.get(ip);
			if (hisRecords == null)
				hisRecords = new Hashtable<>();
			UserTrackMinuteRecord theRecord = hisRecords.get(module);
			if (theRecord == null)
				theRecord = new UserTrackMinuteRecord(module,ip);

			hisRecords.put(module, theRecord);
			userMinuteTracking.put(ip, hisRecords);

			return theRecord;
		}

		public void cleanOldRecords()
		{
			long now = System.currentTimeMillis();
			long minuteAgo = now - 60 * 1000;

			while(!records.isEmpty())
			{
				if (records.get(0) < minuteAgo)
					records.remove(0);
				else
					break;
			}
		}

		public boolean getToken(String module)
		{
			if (records.size() >= getMinutePostLimit(module))
				return false;
			long now = System.currentTimeMillis();
			long minuteAgo = now - 60 * 1000;
			if(this.getFirstPostTime() < minuteAgo)	records.clear();
			records.add(now);
			return true;
		}

		public long getWaitTime(String module){
			long now = System.currentTimeMillis();
			if (records.size() >= getMinutePostLimit(module)){
				long wait = getLastPostTime() + 60*1000 - now;
				if (wait > 0L) return wait;
				else if (wait < 0L) {
					cleanOldRecords();
				}
			}
			return 0;
		}

		public long getLastPostTime()
		{
			if (records.size() == 0)
				return 0;
			return records.get( records.size() - 1 );
		}

		public long getFirstPostTime()
		{
			if (records.size() == 0)
				return 0;
			return records.get(0);
		}

	}

	public static int getMinutePostLimit(String module)
	{
		int minuteLimit = Constants.getInt(MINUTE_LIMIT_KEY+"-"+module);
		if (minuteLimit < 1 && minuteLimit >= -1) minuteLimit = Constants.getInt(MINUTE_LIMIT_KEY);
		return minuteLimit;
	}

	public static void destroy() {
		if (executor != null) {
			Logger.println(SpamProtection.class,"Destroying SpamProtection");
			executor.shutdownNow();
		}
	}

	/**
	 * Clear all maps
	 */
	public static void clearAll() {
		userTracking.clear();
		userMinuteTracking.clear();
		userMinitTracking.clear();
		lastAccessedTime.clear();
		lastAccessedTimeInMinit.clear();
	}
}
