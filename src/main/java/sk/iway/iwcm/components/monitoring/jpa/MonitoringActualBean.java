package sk.iway.iwcm.components.monitoring.jpa;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;

import sk.iway.iwcm.*;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.system.dbpool.ConfigurableDataSource;
import sk.iway.iwcm.system.monitoring.CpuInfo;

/**
 * 47419 - monitorovanie servera Bean prenasajuci JSON data do administracie pre
 * zobrazenie dat monitoringu
 */
@Getter
@Setter
public class MonitoringActualBean {

    /** volne miesto na disku **/
    private Long storageTotal;
    private Long storageUsable;
    private Long storageFree;

    /** obsadenost pamate **/
    private Long memTotal;
    private Long memFree;
    private Long memUsed;
    private Long memMax;

    /** zatazenie CPU **/
    private Integer cpuUsage;
    private Integer cpuUsageProcess;
    private Integer serverCpus;

    /** vseobecne informacie **/
    private Long serverActualTime;
    private Long serverStartTime;
    private Long serverRuntime;

    private String remoteIP;
    private String serverHostname;
    private String[] serverIP;

    private String serverContry;
    private String serverLanguage;

    private Integer dbTotal;
    private Integer dbActive;
    private Integer dbIdle;
    private Integer dbWaiting;

    private Integer cacheItems;
    private Integer sessionsTotal;
    private List<LabelValueInteger> sessionsList;

    private String clusterNodeName;

    /** Informacie o SW **/
    private String swRuntime;
    private String swVmVersion;
    private String swVmName;
    private String swJavaVersion;
    private String swJavaVendor;
    private String swServerName;
    private String swServerOs;
    private String swServerOsVersion;
    private String licenseExpirationDate;

    public MonitoringActualBean() {
        /** volne miesto na disku **/
        File drive = new File(Tools.getRealPath("/"));
        storageTotal = drive.getTotalSpace();
        storageUsable = drive.getUsableSpace();
        storageFree = drive.getFreeSpace();

        /** obsadenost pamate **/
        Runtime rt = Runtime.getRuntime();
        memTotal = rt.totalMemory();
        memFree = rt.freeMemory();
        memUsed = memTotal.longValue() - memFree.longValue();
        memMax = rt.maxMemory();

        CpuInfo cpu = new CpuInfo();
        cpuUsage = cpu.getCpuUsage();
        cpuUsageProcess = cpu.getCpuUsageProcess();
        serverCpus = cpu.getCpuCount();

        //Logger.debug(MonitoringActualBean.class, "cpuUsage: " + cpuUsage+" processUsage="+cpuUsageProcess+" cpuCount="+serverCpus);

        /** vseobecne informacie **/
        serverActualTime = Tools.getNow();
        serverStartTime = InitServlet.getServerStartDatetime().getTime();
        serverRuntime = serverActualTime - serverStartTime;

        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null) {
            remoteIP = rb.getRemoteIP();
        }
        try {
            serverHostname = InetAddress.getLocalHost().getHostName();
            InetAddress[] ipecky = InetAddress.getAllByName(serverHostname);
            if (ipecky != null && ipecky.length>0) {
                int i;
                serverIP = new String[ipecky.length];
                for (i = 0; i < ipecky.length; i++)
                {
                    serverIP[i] = ipecky[i].getHostAddress();
                }
            }

        } catch (UnknownHostException e) {
            Logger.error(MonitoringActualBean.class, e);
        }

        Locale l = Locale.getDefault();
        serverContry = l.getCountry();
        serverLanguage = l.getLanguage();

        ConfigurableDataSource ds = null;
        try
        {
            ds = (ConfigurableDataSource) DBPool.getInstance().getDataSource("iwcm");
            dbTotal = ds.getNumTotal();
            dbActive = ds.getNumActive();
            dbIdle = ds.getNumIdle();
            dbWaiting = ds.getNumWaiting();
        } catch (Exception ex) {
            //
        }

        cacheItems = Cache.getInstance().getSize();
        sessionsTotal = SessionHolder.getTotalSessions();
        if(ClusterDB.isServerRunningInClusterMode())
        {
            List<ConfDetails> confValues = ConfDB.getConfig("statSessions-");
            sessionsList = new ArrayList<>();
            for(ConfDetails conf: confValues)
            {
                LabelValueInteger lv = new LabelValueInteger(conf.getName().substring(13), Tools.getIntValue(conf.getValue(), 0));
                sessionsList.add(lv);
            }
            clusterNodeName = Constants.getString("clusterMyNodeName");
        }

        /** Informacie o SW **/
        Properties props = System.getProperties();
        swRuntime = props.getProperty("java.runtime.name");
        swVmVersion = props.getProperty("java.vm.version");
        swVmName = props.getProperty("java.vm.name");
        swJavaVersion = props.getProperty("java.version");
        swJavaVendor = props.getProperty("java.vendor");
        swServerName = Constants.getServletContext().getServerInfo();
        swServerOs = props.getProperty("os.name");
        swServerOsVersion = props.getProperty("os.version");

        Long licenseExpirationTimeInMillis =  Constants.getLong("licenseExpiryDate");
        if(licenseExpirationTimeInMillis != null && licenseExpirationTimeInMillis > 0L)
        {
            licenseExpirationDate = Tools.formatDate(new Date(licenseExpirationTimeInMillis), "dd.MM.yyyy");
        }
    }
}
