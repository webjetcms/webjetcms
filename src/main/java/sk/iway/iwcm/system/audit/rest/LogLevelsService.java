package sk.iway.iwcm.system.audit.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.audit.jpa.LogLevelBean;
import sk.iway.iwcm.system.audit.jpa.LogFileBean;

/**
 * Service to handle log levels for packages.
 * They are saved in Constants.logLevel for main level and
 * Constants.logLevels for packages (one package per line).
 * Also can be saved into DB for persistent settings.
 */
public class LogLevelsService {

    public static final String LOG_LEVELS = "logLevels";
    public static final String MAIN_LOG_LEVEL = "logLevel";

    private LogLevelsService() {
        //utility class
    }

    private static final String getLogLevelString(String mainLogLevelPackage, boolean withMainLogLevel) {
        if(withMainLogLevel == true) {
            //Default log level + others
            return mainLogLevelPackage + "=" + Constants.getString(MAIN_LOG_LEVEL) + "\n" + Constants.getString(LOG_LEVELS);
        } else {
            //Only others
            return Constants.getString(LOG_LEVELS);
        }
    }

    public static final List<LogLevelBean> getLogLevelData(String mainLogLevelPackage) {
        String logLevelsString = getLogLevelString(mainLogLevelPackage, true);
        List<LogLevelBean> logLevelData = new ArrayList<>();
        if(Tools.isNotEmpty(logLevelsString)) {
            Long id = 1L;
            String[] logLevelArray = Tools.getTokens(logLevelsString, "\n");
            for(String logLevel : logLevelArray) {
                String[] logLevelValues = logLevel.split("=");
                logLevelData.add(
                    new LogLevelBean(id++, logLevelValues[0], logLevelValues[1])
                );
            }
        }
        return logLevelData;
    }

    private static final LogLevelBean handleMainLogLevel(LogLevelBean mainLogLevel, LogLevelBean defaultMainLogLevel) {
        //This package (main log level) can have only two states DEBUG or NORMAL
        if(mainLogLevel.getLogLevel().equals("DEBUG") || mainLogLevel.getLogLevel().equals("NORMAL")) {
            //DO edit
            Constants.setString(MAIN_LOG_LEVEL, mainLogLevel.getLogLevel().toLowerCase());
            return mainLogLevel;
        } else {
            //Do not update, return old correct value
            return defaultMainLogLevel;
        }
    }

    public static final LogLevelBean editItem(LogLevelBean entity, long id, String mainLogLevelPackage) {
        List<LogLevelBean> logLevelData = getLogLevelData(mainLogLevelPackage);
        String logLevelsString = getLogLevelString(mainLogLevelPackage, false);

        if(entity.getPackageName().equals(mainLogLevelPackage)) {
            return handleMainLogLevel(entity, logLevelData.get(0));
        }

        //it's not main log level -  start with 1 so we skip MAIN_LOG_LEVEL
        for(int i = 1; i < logLevelData.size(); i++) {
            LogLevelBean logLevel = logLevelData.get(i);
            if(logLevel.getId() == id) {
                logLevelsString = logLevelsString.replace(logLevel.getFullLog(), entity.getFullLog());
                Constants.setString(LOG_LEVELS, logLevelsString);
                return entity;
            }
        }
        return null;
    }

    public static final LogLevelBean insertItem(LogLevelBean entity, String mainLogLevelPackage) {
        List<LogLevelBean> logLevelData = getLogLevelData(mainLogLevelPackage);
        String logLevelsString = getLogLevelString(mainLogLevelPackage, false);

        if(entity.getPackageName().equals(mainLogLevelPackage)) {
            return handleMainLogLevel(entity, logLevelData.get(0));
        }

        LogLevelBean exists = null;
        //it's not main log level -  start with 1 so we skip MAIN_LOG_LEVEL
        for(int i = 1; i < logLevelData.size(); i++) {
            LogLevelBean logLevel = logLevelData.get(i);
            if(logLevel.getPackageName().equals(entity.getPackageName())) {
                exists = logLevel;
                break;
            }
        }
        if(exists == null) {
            //CREATE action
            if(Tools.isEmpty(logLevelsString) == true) {
                Constants.setString(LOG_LEVELS, entity.getFullLog());
            } else {
                Constants.setString(LOG_LEVELS, logLevelsString + "\n" + entity.getFullLog());
            }
            entity.setId( Long.valueOf(logLevelData.size() + 1L) );
        } else {
            //Package name is already in use - it's EDIT
            logLevelsString = logLevelsString.replace(exists.getFullLog(), entity.getFullLog());
            Constants.setString(LOG_LEVELS, logLevelsString);
            entity.setId( exists.getId() );
        }
        return entity;
    }

    public static final boolean deleteItem(LogLevelBean entity, long id, String mainLogLevelPackage) {
        boolean isFirst = true;
        StringBuilder logLevelsString = new StringBuilder("");

        if(entity.getPackageName().equals(mainLogLevelPackage)) {
            //Do not delete main log level
            return false;
        }

        List<LogLevelBean> logLevelData = getLogLevelData(mainLogLevelPackage);
        //it's not main log level -  start with 1 so we skip MAIN_LOG_LEVEL
        for(int i = 1; i < logLevelData.size(); i++) {
            LogLevelBean logLevel = logLevelData.get(i);
            //Do not add the one for deletion - ONLY if this is not a main log level
            if(logLevel.getPackageName().equals(entity.getPackageName()) == true && logLevel.getPackageName().equals(mainLogLevelPackage) == false) break;
            if(isFirst) {
                logLevelsString.append(logLevel.getFullLog());
                isFirst = false;
            } else {
                logLevelsString.append("\n").append(logLevel.getFullLog());
            }
        }
        Constants.setString(LOG_LEVELS, logLevelsString.toString());
        return true;
    }

    public static final void afterSave(LogLevelBean entity, String mainLogLevelPackage) {
        if(entity.isSaveIntoDB() == true) {
            if(entity.getPackageName().equals(mainLogLevelPackage)) {
                //MAIN log level
                ConfDB.setName(MAIN_LOG_LEVEL, entity.getLogLevel());
            } else {
                ConfDB.setName(LOG_LEVELS, getLogLevelString(mainLogLevelPackage, false));
            }
        } else {
            //apply log levels to current logger
            Logger.setWJLogLevel(Constants.getString(MAIN_LOG_LEVEL));
            Logger.setWJLogLevels(Logger.getLogLevelsMap(Constants.getString(LOG_LEVELS)));
        }
    }

    public static File getLogDir() {
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase == null) catalinaBase = System.getProperty("catalina.home");
        if (catalinaBase == null) catalinaBase = System.getProperty("user.dir");
        return new File(catalinaBase, "logs");
    }

    public static final List<LogFileBean> getLogFiles() {
        File logDir = getLogDir();
        File[] files = logDir.listFiles();
        List<LogFileBean> result = new ArrayList<>();
        if (files != null) {
            long id = 1L;
            for (File file : files) {
                result.add(new LogFileBean(file, id++));
            }
        }
        return result;
    }
}
