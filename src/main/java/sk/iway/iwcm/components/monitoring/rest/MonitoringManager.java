package sk.iway.iwcm.components.monitoring.rest;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.monitoring.jpa.MonitoringEntity;

public class MonitoringManager {

	private static final String CACHE_KEY = "serverMonitoringEnableIPs.set";

	//Refences
	private MonitoringAggregator monitoringAggregator;

    public MonitoringManager(Page<MonitoringEntity> data, Date fromDate, Date toDate) {
		this.monitoringAggregator = new MonitoringAggregator(data, fromDate, toDate);
	}

	public List<MonitoringEntity> returnAggregatedData() {
		return monitoringAggregator.returnAggregatedData();
	}

	public static boolean isIpAllowed(String remoteIp)
	{
		Cache c = Cache.getInstance();

		@SuppressWarnings("unchecked")
		Set<String> allowedIps = c.getObject(CACHE_KEY, HashSet.class);

		if (allowedIps == null) {
			allowedIps = new HashSet<>();
			c.setObjectSeconds(CACHE_KEY, allowedIps, 60*10);
		}
		if (allowedIps.contains(remoteIp)) return true;

		String[] ips = Tools.getTokens(Constants.getString("serverMonitoringEnableIPs"), ",");
		for (String ip : ips)
		{
			if (remoteIp.startsWith(ip))
			{
				allowedIps.add(remoteIp);
				return true;
			}
		}

		return false;
	}

}
