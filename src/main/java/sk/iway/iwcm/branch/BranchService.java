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
import java.util.Set;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.DBPool;

/** Minimal package metadata service. User/domain authorization belongs to the calling boundary. */
@Service
public class BranchService {
    private static final Set<String> COLORS = Set.of("default", "red", "peach", "amber", "mint", "cyan", "blue", "lavender", "gray");
    @FunctionalInterface
    interface Connections { Connection open() throws SQLException; }
    private final Connections connections;

    public BranchService() { this(DBPool::getConnection); }
    BranchService(Connections connections) { this.connections = connections; }

    public BranchEntity create(String domainName, String name, String description, int userId) {
        return create(domainName, name, description, null, userId);
    }

    public BranchEntity create(String domainName, String name, String description, String color, int userId) {
        return create(BranchDomains.id(domainName), name, description, color, userId);
    }

    public BranchEntity create(int domainId, String name, String description, int userId) {
        return create(domainId, name, description, null, userId);
    }

    public BranchEntity create(int domainId, String name, String description, String color, int userId) {
        if (domainId < 1 || name == null || name.isBlank() || name.strip().length() > 255 || userId <= 0)
            throw new IllegalArgumentException("Invalid publishing package metadata");
        String selectedColor = color == null ? BranchEntity.DEFAULT_COLOR : color;
        if (!COLORS.contains(selectedColor)) throw new IllegalArgumentException("Unknown working version color");
        try (Connection connection = connections.open()) {
            BranchReadiness.verifyActivation(connection);
            Timestamp now = Timestamp.from(Instant.now());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO branches(domain_id,name,description,color,state,revision,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,?,'DRAFT',0,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, domainId); statement.setString(2, name.strip()); statement.setString(3, description);
                statement.setString(4, selectedColor);
                statement.setInt(5, userId); statement.setInt(6, userId); statement.setTimestamp(7, now); statement.setTimestamp(8, now);
                statement.executeUpdate();
                long id;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No publishing package identity returned");
                    id = keys.getLong(1);
                }
                return read(connection, id, domainId).orElseThrow();
            }
        } catch (SQLException | IllegalStateException ex) { throw new UnavailableException(ex.getMessage(), ex); }
    }

    /** Safe setup error exposed by the management boundary as a conflict. */
    public static final class UnavailableException extends IllegalStateException {
        public UnavailableException(String message, Throwable cause) { super(message,cause); }
    }

    public List<BranchEntity> list(String domainName, boolean includeClosed) {
        return list(BranchDomains.id(domainName), includeClosed);
    }

    public List<BranchEntity> list(int domainId, boolean includeClosed) {
        BranchReadiness.requireEnabled();
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM branches WHERE domain_id=?" + (includeClosed ? "" : " AND state NOT IN ('MERGED','CANCELLED')") + " ORDER BY id DESC")) {
            statement.setInt(1, domainId);
            List<BranchEntity> rows = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) { while (result.next()) rows.add(map(result)); }
            return List.copyOf(rows);
        } catch (SQLException ex) { throw new IllegalStateException("Cannot list publishing packages", ex); }
    }

    public Optional<BranchEntity> find(long id, String domainName) {
        return find(id, BranchDomains.id(domainName));
    }

    public Optional<BranchEntity> find(long id, int domainId) {
        BranchReadiness.requireEnabled();
        try (Connection connection = connections.open()) { return read(connection, id, domainId); }
        catch (SQLException ex) { throw new IllegalStateException("Cannot read publishing package", ex); }
    }

    /** Updates presentation metadata without changing content, revision or publication state. */
    public BranchEntity updateMetadata(long id, int domainId, String name, String description, String color, int userId) {
        BranchReadiness.requireEnabled();
        if (name == null || name.isBlank() || name.strip().length() > 255 || userId <= 0 || color == null || !COLORS.contains(color))
            throw new IllegalArgumentException("Invalid publishing package metadata");
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE branches SET name=?,description=?,color=?,updated_by=?,updated_at=? WHERE id=? AND domain_id=?")) {
            statement.setString(1, name.strip());
            statement.setString(2, description);
            statement.setString(3, color);
            statement.setInt(4, userId);
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.setLong(6, id);
            statement.setInt(7, domainId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Working version not found");
            return read(connection, id, domainId).orElseThrow();
        } catch (SQLException ex) { throw new UnavailableException(ex.getMessage(), ex); }
    }

    private static Optional<BranchEntity> read(Connection connection, long id, int domainId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM branches WHERE id=? AND domain_id=?")) {
            statement.setLong(1, id); statement.setInt(2, domainId);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); }
        }
    }

    private static BranchEntity map(ResultSet result) throws SQLException {
        BranchEntity branch = new BranchEntity();
        branch.setId(result.getLong("id"));
        branch.setDomainId(result.getInt("domain_id"));
        branch.setName(result.getString("name")); branch.setDescription(result.getString("description"));
        branch.setColor(result.getString("color"));
        branch.setState(result.getString("state")); branch.setRevision(result.getLong("revision"));
        branch.setPublishAt(result.getTimestamp("publish_at")); branch.setScheduledBy((Integer) result.getObject("scheduled_by"));
        branch.setCreatedBy(result.getInt("created_by")); branch.setUpdatedBy(result.getInt("updated_by"));
        branch.setCreatedAt(result.getTimestamp("created_at")); branch.setUpdatedAt(result.getTimestamp("updated_at"));
        branch.setMergeStartedAt(result.getTimestamp("merge_started_at")); branch.setMergeFinishedAt(result.getTimestamp("merge_finished_at"));
        branch.setLastError(result.getString("last_error"));
        return branch;
    }
}
