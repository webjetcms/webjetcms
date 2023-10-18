/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package sk.iway.iwcm.system.jpa;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.dbcp.ConfigurableDataSource;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.localization.ToStringLocalization;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.EmptyRecord;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.tools.profiler.PerformanceProfiler;
import org.eclipse.persistence.tools.profiler.Profile;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;

/**
 * <p><b>Purpose</b>: A tool used to provide high level performance profiling information.
 *
 * @since TopLink 1.0
 * @author James Sutherland
 */
public class WebJETPerformanceProfiler extends PerformanceProfiler
{

   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -4642022979617882034L;

	@Override
	public boolean shouldLogProfile()
   {
      return "debug".equals(Constants.getString("logLevel"));
	}


   /**
    * INTERNAL:
    * Finish a profile operation if profiling.
    * This assumes the start operation proceeds on the stack.
    * The session must be passed to allow units of work etc. to share their parents profiler.
    *
    * @return the execution result of the query.
    */
    @SuppressWarnings("rawtypes")
	@Override
   public Object profileExecutionOfQuery(DatabaseQuery query, Record row, AbstractSession session)
	{
      long profileStartTime = System.currentTimeMillis();
      long nestedProfileStartTime = getProfileTime();
      Profile profile = new Profile();
      profile.setQueryClass(query.getClass());
      profile.setDomainClass(query.getReferenceClass());
      StringBuilder sb = new StringBuilder();

      String sql = query.getSQLString();
      Object result = null;
      try {
          if (shouldLogProfile())
          {
              sb.append("JPA:");

              try
              {
            	  ConfigurableDataSource cds = (ConfigurableDataSource)DBPool.getInstance().getDataSource("iwcm");
            	  sb.append(" active=" + cds.getNumActive()+" i="+cds.getNumIdle());
              }
              catch (Exception ex) {}

              sb.append(' ');
          }

          setNestLevel(getNestLevel() + 1);
          long startNestTime = getNestTime();
          Map<String, Long> timingsBeforeExecution = getOperationTimings();
          Map<String, Long> startTimingsBeforeExecution = getOperationStartTimes();
          long startTime = System.currentTimeMillis();
          result = session.internalExecuteQuery(query, (AbstractRecord)row);
          long endTime = System.currentTimeMillis();
          setNestLevel(getNestLevel() - 1);

          for (String name : getOperationTimings().keySet())
          {
              Long operationStartTime = timingsBeforeExecution.get(name);
              long operationEndTime = (getOperationTimings().get(name)).longValue();
              long operationTime;
              if (operationStartTime != null) {
                  operationTime = operationEndTime - operationStartTime.longValue();
              } else {
                  operationTime = operationEndTime;
              }
              profile.addTiming(name, operationTime);
          }

          profile.setTotalTime((endTime - startTime) - (getProfileTime() - nestedProfileStartTime));// Remove the profile time from the total time.;);
          profile.setLocalTime(profile.getTotalTime() - (getNestTime() - startNestTime));
          if (result instanceof Vector) {
                profile.setNumberOfInstancesEffected(((Vector)result).size());
          } else {
              profile.setNumberOfInstancesEffected(1);
          }

          String translatedSql = sql;
          if(row instanceof EmptyRecord == false)
          {
             translatedSql = query.getTranslatedSQLString(session,row);
          }

          if(Tools.isNotEmpty(sql) && profile.getTotalTime() > 0)
          {
         	 ExecutionTimeMonitor.recordSqlExecution(sql, profile.getTotalTime());
          }
          addProfile(profile);
          if (shouldLogProfile())
          {
              long profileEndTime = System.currentTimeMillis();
              long totalTimeIncludingProfiling = profileEndTime - profileStartTime;// Try to remove the profiling time from the total time.
              profile.setProfileTime(totalTimeIncludingProfiling - profile.getTotalTime());
              writeProfile(sb, profile);
              sb.append(Helper.cr());
              sb.append("SQL: " + translatedSql);

              if (sb.toString().contains("AdminMessageBean")==false && sb.toString().contains("UrlRedirectBean")==false)
              {
            	  Logger.debug(this, sb.toString());
              }
          }

          if (getNestLevel() == 0)
          {
              setNestTime(0);
              setProfileTime(0);
              setOperationTimings(new Hashtable<String, Long>(5));
              setOperationStartTimes(new Hashtable<String, Long>(5));
              long profileEndTime = System.currentTimeMillis();
              long totalTimeIncludingProfiling = profileEndTime - profileStartTime;// Try to remove the profiling time from the total time.
              profile.setProfileTime(totalTimeIncludingProfiling - profile.getTotalTime());
          }
          else
          {
              setNestTime(startNestTime + profile.getTotalTime());
              setOperationTimings(timingsBeforeExecution);
              setOperationStartTimes(startTimingsBeforeExecution);
              long profileEndTime = System.currentTimeMillis();
              long totalTimeIncludingProfiling = profileEndTime - profileStartTime;// Try to remove the profiling time from the total time.
              setProfileTime(getProfileTime() + (totalTimeIncludingProfiling - (endTime - startTime)));
              profile.setProfileTime(totalTimeIncludingProfiling - profile.getTotalTime());
              for(Map.Entry<String, Long> entry : startTimingsBeforeExecution.entrySet())
              {
                    String timingName = entry.getKey();
                    startTimingsBeforeExecution.put(timingName, entry.getValue().longValue() + totalTimeIncludingProfiling);
              }
          }
      }
      catch (org.eclipse.persistence.exceptions.DatabaseException e){
      	sb.append("ERROR: Exception occuerd while performing SQL query: "+Helper.cr()+sql);
      	Logger.println(this, sb.toString());
      	sk.iway.iwcm.Logger.error(e);
      	throw e;
      }
      catch (Exception e) {
      	sb.append("ERROR: Exception occuerd while performing SQL query: "+Helper.cr()+sql);
      	Logger.println(this, sb.toString());
      	sk.iway.iwcm.Logger.error(e);
      }

      return result;
	}

   /**
    * INTERNAL:
    * End the operation timing.
    *
    * Overridnute s cielom odstranit vypis nechcenych profilov do logu
    */
	@Override
   public void endOperationProfile(String operationName) {
       long endTime = System.currentTimeMillis();
       Long startTime = (getOperationStartTimes().get(operationName));
       if (startTime == null) {
           return;
       }
       long time = endTime - startTime.longValue();

       if (getNestLevel() == 0) {
           // Log as a profile if not within query execution,
           // unless no time was recorded, in which case discard.
           if (time == 0) {
               return;
           }
           Profile profile = new Profile();
           profile.setTotalTime(time);
           profile.setLocalTime(time);
           profile.addTiming(operationName, time);
           addProfile(profile);

           // v parent metode je na tomto mieste kod vypisu do logu
       }

       Long totalTime = getOperationTimings().get(operationName);
       if (totalTime == null) {
           getOperationTimings().put(operationName, Long.valueOf(time));
       } else {
           getOperationTimings().put(operationName, Long.valueOf(totalTime.longValue() + time));
       }
   }


   public void writeProfile(StringBuilder sb, Profile profile) {
		String cr = " ";
		if (profile.getDomainClass() != null)
		{
			sb.append(ToStringLocalization.buildMessage("class", (Object[]) null) + "=" + profile.getDomainClass().getSimpleName());
		}
		if (profile.getQueryClass() != null)
		{
			sb.append(" - " + Helper.getShortClassName(profile.getQueryClass()) + "," + cr);
		}
		else
		{
			sb.append(cr);
		}
		if (profile.getNumberOfInstancesEffected() != 0)
		{
			sb.append(ToStringLocalization.buildMessage("number_of_objects", (Object[]) null) + "=" + profile.getNumberOfInstancesEffected() + "," + cr);
		}
		sb.append(ToStringLocalization.buildMessage("local_time", (Object[]) null) + "=" + profile.getLocalTime() + "," + cr);
		if (getProfileTime() != 0)
		{
			sb.append(ToStringLocalization.buildMessage("profiling_time", (Object[]) null) + "=" + getProfileTime() + "," + cr);
		}
		for(String operationName : getOperationTimings().keySet())
		{
			long operationTime = (getOperationTimings().get(operationName)).longValue();
			if (operationTime != 0)
			{
				sb.append(operationName + "=" + operationTime + "," + cr);
			}
		}
		if (profile.getTimePerObject() != 0)
		{
			sb.append(ToStringLocalization.buildMessage("time_object", (Object[]) null) + "=" + profile.getTimePerObject() + "," + cr);
		}
		if (profile.getObjectsPerSecond() != 0)
		{
			sb.append(ToStringLocalization.buildMessage("objects_second", (Object[]) null) + "=" + profile.getObjectsPerSecond() + "," + cr);
		}
		if (profile.getShortestTime() != -1)
		{
			sb.append(ToStringLocalization.buildMessage("shortestTime", (Object[]) null) + "=" + profile.getShortestTime() + "," + cr);
		}
		if (profile.getLongestTime() != 0)
		{
			sb.append(ToStringLocalization.buildMessage("longestTime", (Object[]) null) + "=" + profile.getLongestTime() + "," + cr);
		}
		sb.append( ToStringLocalization.buildMessage("total_time", (Object[]) null) + "=" + profile.getTotalTime() + cr);
		sb.append(')');
	}

   public String getQueryString(DatabaseQuery query) {
    	String referenceClassString = "";
    	String nameString = "";
    	if (query.getReferenceClass() != null) {
    		referenceClassString = "class=" + query.getReferenceClass().getSimpleName() + " ";
    	}
    	if ((query.getName() != null) && (!query.getName().equals(""))) {
    		nameString = "name=\"" + query.getName() + "\" ";
    	}
    	return getClass().getSimpleName() + "(" + nameString + referenceClassString + ")";
    }

}
