package sk.iway.iwcm.system.logging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.LoggerFactory;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.SimpleQuery;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Queue;

/**
 * Zabezpecuje drzanie poslednych logov v pamati a zaroven citanie/zapisovanie dat vramci clustra (volanim getInstance(refresh))
 */
public class InMemoryLoggingDB {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InMemoryLoggingDB.class);
    private static final String INSERT = "INSERT INTO cluster_monitoring(node, type, content, created_at) VALUES(?, ?, ?, ?)";
    private static final String TYPE = "logging";
    private static CircularFifoQueue<InMemoryLoggingEvent> queue;
    private static InMemoryLoggingDB instance;
    private long id = 0;

    static {
        queue = new CircularFifoQueue<>(Constants.getInt("loggingInMemoryQueueSize", 200));
    }

    private InMemoryLoggingDB() {
    }

    public static InMemoryLoggingDB getInstance() {
        if(instance == null) {
            instance = new InMemoryLoggingDB();
        }
        return instance;
    }

    public void add(InMemoryLoggingEvent inMemoryLoggingEvent) {
        inMemoryLoggingEvent.setId(id++);
        queue.add(inMemoryLoggingEvent);
    }

    public static void getInstance(boolean refresh) {
        InMemoryLoggingDB instance = getInstance();
        instance.writeStatsToSharedTable(refresh);
    }

    private void writeStatsToSharedTable(boolean refresh) {
        if (refresh) {
            eraseLogging();
        }
        writeLogging();
        LOGGER.debug("Persisting logging from node {} done", getClusterNode());
    }

    private void writeLogging() {
        new SimpleQuery().execute(INSERT, getClusterNode(), TYPE, serialize(queue), new Timestamp(Tools.getNow()));
        LOGGER.debug("Persisted URL performance stats for {}", getClusterNode());
    }

    private void eraseLogging() {
        new SimpleQuery().execute("DELETE FROM cluster_monitoring WHERE node = ? AND type = ?", getClusterNode(), TYPE);
        LOGGER.debug("Erased logging for {}", getClusterNode());
    }

    public CircularFifoQueue<InMemoryLoggingEvent> readLogging(String node)
    {
        LOGGER.debug("Request to read {} performance stats of {}", TYPE, node);
        ComplexQuery query = new ComplexQuery().setSql("SELECT content FROM cluster_monitoring WHERE node = ? AND type = ?").setParams(node, TYPE);
        List<String> content = query.list(rs -> rs.getString(1));

        if (content.isEmpty())
        {
            LOGGER.debug("There are no such stats");
            return null;
        }

        String xmlDecoderAllowedClasses = Constants.getString("XMLDecoderAllowedClasses");
        try {
            Constants.setString("XMLDecoderAllowedClasses", xmlDecoderAllowedClasses + ",org.apache.commons.collections4.queue.CircularFifoQueue");
            CircularFifoQueue<InMemoryLoggingEvent> deserialize = deserialize(content.get(0));
            eraseLogging();
            return deserialize;

        }
        catch (IOException e) {
            Logger.error(InMemoryLoggingDB.class, e);
        }
        finally {
            Constants.setString("XMLDecoderAllowedClasses", xmlDecoderAllowedClasses);
        }

        return null;
    }

    private String serialize(CircularFifoQueue<InMemoryLoggingEvent> queue)
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(queue);
        }
        catch (Exception e) {
            LOGGER.error("Serialize error", e);
        }

        return null;
    }

    private static CircularFifoQueue<InMemoryLoggingEvent> deserialize(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, new TypeReference<CircularFifoQueue<InMemoryLoggingEvent>>(){});
    }

    private String getClusterNode()
    {
        return Constants.getString("clusterMyNodeName");
    }

    public Queue<InMemoryLoggingEvent> getQueue() {
        return queue;
    }

    public static void setQueueSize(int size) {
        if (queue.maxSize()!=size) {
            Logger.debug(InMemoryLoggingDB.class, "Reseting queue size to "+size+" originalSize="+queue.maxSize());
            queue = new CircularFifoQueue<>(size);
        }
    }
}
