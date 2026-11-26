package sk.iway.iwcm.branch;

import java.sql.SQLException;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.GroupsDB;

/** Reusable roots for the isolated POC database's named test domains. */
final class BranchTestDomains {
    private BranchTestDomains() { }

    static void requireIsolatedDatabase(java.sql.Connection connection) throws SQLException {
        String url = connection.getMetaData().getURL();
        boolean maria = "webjet_p01".equals(connection.getCatalog()) && url.contains(":33316/");
        boolean postgres = "wjbranch".equals(connection.getCatalog()) && url.startsWith("jdbc:postgresql://127.0.0.1:55461/wjbranch");
        if (!maria && !postgres) throw new IllegalStateException("Branch fixtures require an isolated test database");
    }

    static int ensure(String name) {
        try (var connection = DBPool.getConnection()) {
            requireIsolatedDatabase(connection);
            try (var select = connection.prepareStatement("SELECT group_id FROM groups WHERE domain_name=? AND parent_group_id=0 ORDER BY group_id")) {
                select.setString(1, name);
                try (var rows = select.executeQuery()) {
                    if (rows.next()) { GroupsDB.getInstance(true); return BranchDomains.id(name); }
                }
            }
            try (var insert = connection.prepareStatement("INSERT INTO groups (group_name,domain_name,parent_group_id) VALUES (?,?,0)")) {
                insert.setString(1, "autotest branch domain " + name); insert.setString(2, name); insert.executeUpdate();
            }
        } catch (SQLException ex) { throw new IllegalStateException(ex); }
        GroupsDB.getInstance(true);
        return BranchDomains.id(name);
    }
}
