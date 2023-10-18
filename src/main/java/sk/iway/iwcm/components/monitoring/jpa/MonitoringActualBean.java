package sk.iway.iwcm.components.monitoring.jpa;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.dbcp.ConfigurableDataSource;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor.TickType;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.*;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

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
    private Long cpuUsage;
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

    private Integer dbActive;
    private Integer dbIdle;

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

        /** zatazenie CPU **/
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor cpu = hal.getProcessor();
            long[] prevTicks = new long[TickType.values().length];
            prevTicks = cpu.getSystemCpuLoadTicks();
            Thread.sleep(1000);
            cpuUsage = Math.round(cpu.getSystemCpuLoadBetweenTicks( prevTicks ) * 100);
            serverCpus = cpu.getLogicalProcessorCount();
        } catch (Exception ex) {
            Logger.error(MonitoringActualBean.class, ex);
        }
        //cpuUsage = cpu.getSystemLoadAverage(1)[0];

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
            dbActive = ds.getNumActive();
            dbIdle = ds.getNumIdle();
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
    }
}
