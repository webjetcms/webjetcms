package sk.iway.iwcm.stat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.welcome.DashboardListener;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;

/**
 * Handles session data across cluster nodes, storing and retrieving session information
 * from the database to facilitate session management in a clustered environment.
 */
public class SessionClusterService {

    private static final String INSERT = "INSERT INTO cluster_monitoring(node, type, content, created_at) VALUES(?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE cluster_monitoring SET content = ?, created_at = ? WHERE node = ? AND type = ?";
    private static final String DELETE_OLD = "DELETE FROM cluster_monitoring WHERE type = ? AND created_at < ?";
    private static final String GET_ALL_NODES = "SELECT node, content FROM cluster_monitoring WHERE type = ?";
    private static final String TYPE = "sessions";

    /**
     * You can schedule this method to run periodically using CRON
     * @param args
     */
    public static void main(String[] args) {
        deleteOldData(true);
        updateSessionData();
    }

    /**
     * Read session data in database and deserialize to JSON nodes
     * @param userId
     * @return
     */
    public static ArrayNode getUserSessionsAllNodes(int userId) {
        deleteOldData(false);

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode allNodesSessions = mapper.createArrayNode();

        new ComplexQuery().setSql(GET_ALL_NODES).setParams(TYPE).list(new Mapper<Object>() {
			@Override
			public Object map(ResultSet rs) throws SQLException {
                ObjectNode newObject = allNodesSessions.addObject();
                newObject.put("cluster", DB.getDbString(rs, "node"));
                newObject.set("userSessions", convertToTree(rs.getString("content"), userId));
                return null;
			}
		});

        return allNodesSessions;
    }

    private static JsonNode convertToTree(String content, int userId) {
        return new ObjectMapper().valueToTree( convertToList(content, userId) );
    }

    private static List<SessionDetails> convertToList(String content, int userId) {
        List<SessionDetails> userSessions = new ArrayList<>();
        if(Tools.isEmpty(content) || userId < 1) return userSessions;

        try {
            ObjectMapper mapper = new ObjectMapper();
            for(SessionDetails session : mapper.readValue(content, new TypeReference<List<SessionDetails>>() {})) {
                if(session.getLoggedUserId() == userId)
                    userSessions.add(session);
            }
        } catch (JsonProcessingException e) {
            // BAD data maybe, refresh
        }

        return userSessions;
    }

    /**
     * Returns session information for the given user in JSON format, used in dashboard app
     * @param currentSessionId
     * @param userId
     * @return
     */
    public static String getSessionInfo(String currentSessionId, int userId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode sessionInfo = mapper.createObjectNode();
            try {
                sessionInfo.put("currentSessionId", currentSessionId);
                ArrayNode userSessions = SessionClusterService.getUserSessionsAllNodes(userId);
                sessionInfo.set("userSessions", userSessions);
            } catch (Exception ex) {
                Logger.error(DashboardListener.class, "Error while getting session info for dashboard", ex);
            }
            return JsonTools.objectToJSON(sessionInfo);
        } catch (JsonProcessingException e) {

        }
        //failsafe
        return "{currentSessionId: \"" + currentSessionId + "\", userSessions: [] }";
    }


    /**
     * Update session data in the database for the current node
     */
    public static void updateSessionData() {

        try {
            String myNodeName = Constants.getString("clusterMyNodeName");
            if (Constants.DB_TYPE==Constants.DB_ORACLE && Tools.isEmpty(myNodeName)) {
                //empty==null node name not allowed in oracle, set default
                myNodeName = "server";
            }

            // Prepare session data
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode data = mapper.createArrayNode();

            for(SessionDetails session : SessionHolder.getInstance().getList()) {
                if(session.isAdmin() && SessionHolder.INVALIDATE_SESSION_ADDR.equals(session.getRemoteAddr()) == false)
                    data.add( mapper.valueToTree(session) );
            }

            String sessionAsString = "";
            try {
                sessionAsString = mapper.writeValueAsString(data);
            } catch (JsonProcessingException jpe) {
                Logger.error(SessionClusterService.class, "Could not write session list as string: " + jpe.getLocalizedMessage());
                sessionAsString = "";
            }

            Timestamp lastUpdated = new Timestamp(Tools.getNow());

            if (InitServlet.isWebjetInitialized()) {
                // Try to update existing record first
                int updatedRows = new SimpleQuery().executeWithUpdateCount(UPDATE, sessionAsString, lastUpdated, myNodeName, TYPE);

                if (updatedRows == 0) {
                    // No existing record found, insert new one
                    new SimpleQuery().execute(INSERT, myNodeName, TYPE, sessionAsString, lastUpdated);
                    Logger.debug(SessionClusterService.class, String.format("Inserted new session data for node %s", myNodeName));
                } else {
                    Logger.debug(SessionClusterService.class, String.format("Updated existing session data for node %s", myNodeName));
                }
            }

            deleteOldData(false);
        } catch (Exception e) {
            Logger.error(SessionClusterService.class, "Error updating session data in cluster monitoring: " + e.getLocalizedMessage());
        }

    }

    private static void deleteOldData(boolean isCron) {
        //clean up old records (older than 24 hours or 1 hour for cron)
        long backTime = (24 * 60 * 60 * 1000L); //24 hours
        String periodStr = "24 hours";
        if (isCron) {
            backTime = 60 * 60 * 1000L; //one hour for cron
            periodStr = "1 hour";
        }
        Timestamp thresholdTime = new Timestamp(Tools.getNow() - backTime);
        int deletedOldRecords = new SimpleQuery().executeWithUpdateCount(DELETE_OLD, TYPE, thresholdTime);
        Logger.debug(SessionClusterService.class, String.format("Deleted %d old session records older than %s", deletedOldRecords, periodStr));
    }

}
