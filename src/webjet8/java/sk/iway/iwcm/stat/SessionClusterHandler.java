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
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;

/**
 * Handles session data across cluster nodes, storing and retrieving session information
 * from the database to facilitate session management in a clustered environment.
 */
public class SessionClusterHandler {

    private static final String INSERT = "INSERT INTO cluster_monitoring(node, type, content, created_at) VALUES(?, ?, ?, ?)";
    private static final String DELETE = "DELETE FROM cluster_monitoring WHERE node = ? AND type = ?";
    private static final String GET_ALL_NODES = "SELECT node, content FROM cluster_monitoring WHERE type = ?";
    private static final String TYPE = "sessions";

    public static void main(String[] args) {
        //remove old
        removeOldSessionData();
        //push new
        addNewSessiondata();
    }

    public static ArrayNode getUserSessionsAllNodes(int userId) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode allNodesSessions = mapper.createArrayNode();

        new ComplexQuery().setSql(GET_ALL_NODES).setParams(TYPE).list(new Mapper<Object>() {
			@Override
			public Object map(ResultSet rs) throws SQLException {
                ObjectNode newObject = allNodesSessions.addObject();
                newObject.put("cluster", rs.getString("node"));
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

    private static void removeOldSessionData() {
        (new SimpleQuery()).execute(DELETE, myNode(), TYPE);
        Logger.debug(SessionClusterHandler.class, String.format("Removed old session data %s", myNode()));
    }

    private static void addNewSessiondata() {
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
            Logger.error(SessionClusterHandler.class, "Could not write session list as string: " + jpe.getLocalizedMessage());
            sessionAsString = "";
        }

        new SimpleQuery().execute(INSERT, myNode(), TYPE, sessionAsString, now());
		Logger.debug(SessionClusterHandler.class, String.format("Persisted SQL logged sessions %s", myNode()));
    }

    private static String myNode() {
		return Constants.getString("clusterMyNodeName");
	}

    private static Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}
}
