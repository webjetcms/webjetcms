package sk.iway.iwcm.system.monitoring;

import java.lang.management.ManagementFactory;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import sk.iway.iwcm.Logger;

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
	private double cpuUsage = 0;
	private double procUsage = 0;
	private int cpuCount = 1;
	private static Sigar sigarProcessDB = new Sigar();	//kazdych 30 sekund zapisem do databazy
	private static Sigar sigarProcessActual = new Sigar();	//aktualne hodnoty v admin casti
	
	public CpuInfo() {
	}
	
	/*
	 * Vrati vyuzitie procesora v percentach
	 */
	public double getCpuUsage()	
	{
		try
		{
			cpuUsage = 0;
			Sigar sigar = new Sigar();
			CpuPerc[] cpusPerc = sigar.getCpuPercList();	//ziskam zoznam CPU
			cpuCount = cpusPerc.length;	//pocet CPU
			for (int i=0; i<cpuCount; i++) {
				Logger.println(this, "CPU "+i+": "+CpuPerc.format(cpusPerc[i].getCombined()));
				cpuUsage += cpusPerc[i].getCombined();	//cpu usage v percentach
			}
			String cpuString = CpuPerc.format(cpuUsage/cpuCount); //percentualne - este delim procesormi a formatujem na 0.0%
			cpuString = cpuString.substring(0, cpuString.length()-1);	//odstranim %
			return Double.parseDouble(cpuString);
		}
		catch (SigarException e)
		{
			return 0D;
		}
		catch (UnsatisfiedLinkError e)
		{
			return 0D;
		}
		catch (RuntimeException e)
		{
			//niekedy nastava chyba java.lang.reflect.InvocationTargetException / Caused by: java.lang.UnsatisfiedLinkError: org.hyperic.sigar.Sigar.getCpuListNative()
			return 0D;
		}
	}
	
	/*
	 * Vrati vyuzitie procesora procesom webjet v percentach
	 */
	public double getProcCpuUsage(boolean intoDB) throws SigarException{
		procUsage = 0;
		String procPercentage = "0.0%";
		String pid = ManagementFactory.getRuntimeMXBean().getName();	//ziskam pid procesu
      int index = pid.lastIndexOf('@');
      pid = pid.substring(0, index);
		
      try {
     	 	ProcCpu pc;
     	 	if(intoDB) pc = sigarProcessDB.getProcCpu(pid);	//ak ide o ulozenie do databazy -> kazdych 30 sekund
     	 	else pc = sigarProcessActual.getProcCpu(pid);	//ak ide o aktualne hodnoty v admin casti
     	 	//Ako dlho proces vyuziva procesor sa urcuje v casovom rozmedzi medzi dvoma volaniami metody getProcCpu(pid)
     	 	//vytvoril som preto dva objekty - jeden pre ukladanie hodnot do databazy - kazdych 30 sekund - a jeden pre ziskanie aktualnej hodnoty v admin casti
     	 	procUsage = pc.getPercent();
     	 	procPercentage = CpuPerc.format(procUsage/cpuCount); //percentualne - este delim procesormi a formatujem na 0.0%
     	 	Logger.println(this, "Process CPU Total: DB="+intoDB+" process="+pc.getTotal());
      } catch (SigarException e) {} 
      procPercentage = procPercentage.substring(0, procPercentage.length()-1);	//odstranim %
      Logger.println(this, "Process CPU Usage: DB="+intoDB+" process="+procPercentage);
		return Double.parseDouble(procPercentage);
	}
}
