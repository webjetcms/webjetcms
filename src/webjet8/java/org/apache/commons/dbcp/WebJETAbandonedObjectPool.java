package org.apache.commons.dbcp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;


/**
 *  AbandonObjectPool.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 22.8.2006 16:06:50
 *@modified     $Date: 2008/04/29 12:43:28 $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class WebJETAbandonedObjectPool extends GenericObjectPool {

   // DBCP AbandonedConfig
	private AbandonedConfig config = null;
   // A list of connections in use
   private List<AbandonedTrace> trace = new ArrayList<>();

   /**
    * Create an ObjectPool which tracks db connections.
    *
    * @param factory factory used to create this
    * @param config configuration for abandoned db connections
    */
	public WebJETAbandonedObjectPool(PoolableObjectFactory factory,
                              AbandonedConfig config) {
        super(factory);
        this.config = config;
        StringBuilder message = new StringBuilder();
        message.append("AbandonedObjectPool is used (").append(this).append(")").append("\n");
        message.append("\t").append("   LogAbandoned: ").append(config.getLogAbandoned()).append("\n");
        message.append("\t").append("   RemoveAbandoned: ").append(config.getRemoveAbandoned()).append("\n");
        message.append("\t").append("   RemoveAbandonedTimeout: ").append(config.getRemoveAbandonedTimeout()).append("\n");

        Logger.debug(WebJETAbandonedObjectPool.class, message.toString());
   }

   /**
    * Get a db connection from the pool.
    *
    * If removeAbandoned=true, recovers db connections which
    * have been idle > removeAbandonedTimeout.
    *
    * @return Object jdbc Connection
    */
	@Override
	public Object borrowObject() throws Exception {
       if (config != null
               && config.getRemoveAbandoned()
               && (getNumIdle() < 2)
               && (getNumActive() > getMaxActive() - 3) ) {
           removeAbandoned();
       }
       Object obj = super.borrowObject();
       if(obj instanceof AbandonedTrace) {
           ((AbandonedTrace)obj).setStackTrace();
       }
       if (obj != null && config != null && config.getRemoveAbandoned()) {
           synchronized(trace) {
               trace.add((AbandonedTrace)obj);
           }
       }
       return obj;
   }

   /**
    * Return a db connection to the pool.
    *
    * @param obj db Connection to return
    */
	@Override
	public void returnObject(Object obj) throws Exception {
       if (config != null && config.getRemoveAbandoned()) {
           synchronized(trace) {
               boolean foundObject = trace.remove(obj);
               if (!foundObject) {
                   return; // This connection has already been invalidated.  Stop now.
               }
           }
       }
       super.returnObject(obj);
   }
	@Override
	public void invalidateObject(Object obj) throws Exception {
       if (config != null && config.getRemoveAbandoned()) {
           synchronized(trace) {
               boolean foundObject = trace.remove(obj);
               if (!foundObject) {
                   return; // This connection has already been invalidated.  Stop now.
               }
           }
       }
       super.invalidateObject(obj);
   }

   /**
    * Recover abandoned db connections which have been idle
    * greater than the removeAbandonedTimeout.
    */
	public void removeAbandoned() {

   	    if (Constants.getBoolean("disableRemoveAbandoned")==true) return;

       // Generate a list of abandoned connections to remove
       long now = System.currentTimeMillis();
       long timeout = now - (config.getRemoveAbandonedTimeout() * 1000);
       List<AbandonedTrace> remove = new ArrayList<>();
       synchronized(trace) {
           Iterator<AbandonedTrace> it = trace.iterator();
           while (it.hasNext()) {
               AbandonedTrace pc = it.next();
               if (pc.getLastUsed() > timeout) {
                   continue;
               }
               if (pc.getLastUsed() > 0) {
                   remove.add(pc);
               }
           }
       }

       // Now remove the abandoned connections
       Iterator<AbandonedTrace> it = remove.iterator();
       while (it.hasNext()) {
           AbandonedTrace pc = it.next();
           if (config.getLogAbandoned()) {
                pc.printStackTrace();
           }
           try {
               invalidateObject(pc);
           } catch(Exception e) {
               sk.iway.iwcm.Logger.error(e);
           }
       }
   }

	public void printStackTraces()
   {
   	synchronized(trace) {
         Iterator<AbandonedTrace> it = trace.iterator();
         Logger.println(WebJETAbandonedObjectPool.class, "DBPool active: " + getNumActive()+" idle="+getNumIdle());
         Logger.println(WebJETAbandonedObjectPool.class, "Printing stack traces: " + trace.size());
         while (it.hasNext()) {
             AbandonedTrace pc = it.next();
             pc.printStackTrace();
         }
     }
   }

    public void printStackTraces(PrintWriter s)
    {
        synchronized(trace) {
            Iterator<AbandonedTrace> it = trace.iterator();
            s.println("DBPool active: " + getNumActive()+" idle:"+getNumIdle());
            s.println("Printing stack traces: " + trace.size());
            while (it.hasNext()) {
                AbandonedTrace pc = it.next();
                s.println(pc.getClass());
                pc.getConfig().setLogWriter(s);
                pc.printStackTrace();
            }
        }
    }
}