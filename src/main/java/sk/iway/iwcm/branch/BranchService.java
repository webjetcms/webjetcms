package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.DBPool;

/** Minimal package metadata service. User/domain authorization belongs to the calling boundary. */
@Service
public class BranchService {
    @FunctionalInterface
    interface Connections { Connection open() throws SQLException; }
    private final Connections connections;

    public BranchService() { this(DBPool::getConnection); }
    BranchService(Connections connections) { this.connections = connections; }

    public BranchEntity create(String domainName, String name, String description, int userId) {
        if (domainName == null || domainName.isBlank() || domainName.length() > 255
                || name == null || name.isBlank() || name.strip().length() > 255 || userId <= 0)
            throw new IllegalArgumentException("Invalid publishing package metadata");
        try (Connection connection = connections.open()) {
            BranchReadiness.verifyActivation(connection);
            Timestamp now = Timestamp.from(Instant.now());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO branches(domain_name,name,description,state,revision,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,'DRAFT',0,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, domainName); statement.setString(2, name.strip()); statement.setString(3, description);
                statement.setInt(4, userId); statement.setInt(5, userId); statement.setTimestamp(6, now); statement.setTimestamp(7, now);
                statement.executeUpdate();
                long id;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No publishing package identity returned");
                    id = keys.getLong(1);
                }
                return read(connection, id, domainName).orElseThrow();
            }
        } catch (SQLException | IllegalStateException ex) { throw new UnavailableException(ex.getMessage(), ex); }
    }

    /** Safe setup error exposed by the management boundary as a conflict. */
    public static final class UnavailableException extends IllegalStateException {
        public UnavailableException(String message, Throwable cause) { super(message,cause); }
    }

    public List<BranchEntity> list(String domainName, boolean includeClosed) {
        BranchReadiness.requireEnabled();
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM branches WHERE domain_name=?" + (includeClosed ? "" : " AND state NOT IN ('MERGED','CANCELLED')") + " ORDER BY id DESC")) {
            statement.setString(1, domainName);
            List<BranchEntity> rows = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) { while (result.next()) rows.add(map(result)); }
            return List.copyOf(rows);
        } catch (SQLException ex) { throw new IllegalStateException("Cannot list publishing packages", ex); }
    }

    public Optional<BranchEntity> find(long id, String domainName) {
        BranchReadiness.requireEnabled();
        try (Connection connection = connections.open()) { return read(connection, id, domainName); }
        catch (SQLException ex) { throw new IllegalStateException("Cannot read publishing package", ex); }
    }

    private static Optional<BranchEntity> read(Connection connection, long id, String domain) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM branches WHERE id=? AND domain_name=?")) {
            statement.setLong(1, id); statement.setString(2, domain);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); }
        }
    }

    private static BranchEntity map(ResultSet result) throws SQLException {
        BranchEntity branch = new BranchEntity();
        branch.setId(result.getLong("id")); branch.setDomainName(result.getString("domain_name"));
        branch.setName(result.getString("name")); branch.setDescription(result.getString("description"));
        branch.setState(result.getString("state")); branch.setRevision(result.getLong("revision"));
        branch.setPublishAt(result.getTimestamp("publish_at")); branch.setScheduledBy((Integer) result.getObject("scheduled_by"));
        branch.setCreatedBy(result.getInt("created_by")); branch.setUpdatedBy(result.getInt("updated_by"));
        branch.setCreatedAt(result.getTimestamp("created_at")); branch.setUpdatedAt(result.getTimestamp("updated_at"));
        branch.setMergeStartedAt(result.getTimestamp("merge_started_at")); branch.setMergeFinishedAt(result.getTimestamp("merge_finished_at"));
        branch.setLastError(result.getString("last_error"));
        return branch;
    }
}
