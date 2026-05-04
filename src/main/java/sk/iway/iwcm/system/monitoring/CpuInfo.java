package sk.iway.iwcm.system.monitoring;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 *  CpuInfo.java
 *  Obsahuje informacie o vyuziti procesora pre cely system a proces webjetu
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: Miroslav Repaský $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.3.2011 11:37:52
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CpuInfo
{
	private int cpuUsage = 0;
	private int cpuUsageProcess = 0;
	private int cpuCount = 0;

	public CpuInfo() {

        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");

			//https://stackoverflow.com/a/60985633
			//https://docs.oracle.com/en/java/javase/17/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html
            AttributeList list = mbs.getAttributes(name, new String[]{"SystemCpuLoad"});
            cpuUsage = (int)Math.round(getValue(list) * 100);

			list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});
			cpuUsageProcess = (int)Math.round(getValue(list) * 100);

        } catch (Exception ex) {

        }
		cpuCount = Runtime.getRuntime().availableProcessors();
	}

	private double getValue(AttributeList list) {
		Double value = Optional.ofNullable(list)
			.map(l -> l.isEmpty() ? null : l)
			.map(List::iterator)
			.map(Iterator::next)
			.map(Attribute.class::cast)
			.map(Attribute::getValue)
			.map(Double.class::cast)
			.orElse(null);

		if (value == null) return 0d;

		return value.doubleValue();
	}

	/**
	 * Get total CPU usage
	 * @return
	 */
	public int getCpuUsage() {
		return cpuUsage;
	}

	/**
	 * Get process CPU usage for the Java Virtual Machine process
	 * @return
	 */
	public int getCpuUsageProcess() {
		return cpuUsageProcess;
	}

	/**
	 * Get number of CPU cores
	 * @return
	 */
	public int getCpuCount() {
		return cpuCount;
	}
}
