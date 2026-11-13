package sk.iway.iwcm.branch;

import java.sql.SQLException;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.GroupsDB;

/** Reusable roots for the isolated POC database's named test domains. */
final class BranchTestDomains {
    private BranchTestDomains() { }

    static int ensure(String name) {
        try (var connection = DBPool.getConnection()) {
            if (!"webjet_p01".equals(connection.getCatalog()) || !connection.getMetaData().getURL().contains(":33316/"))
                throw new IllegalStateException("Domain fixtures require the isolated P01 database");
            try (var select = connection.prepareStatement("SELECT group_id FROM `groups` WHERE domain_name=? AND parent_group_id=0 ORDER BY group_id")) {
                select.setString(1, name);
                try (var rows = select.executeQuery()) {
                    if (rows.next()) { GroupsDB.getInstance(true); return BranchDomains.id(name); }
                }
            }
            try (var insert = connection.prepareStatement("INSERT INTO `groups` (group_name,domain_name,parent_group_id) VALUES (?,?,0)")) {
                insert.setString(1, "autotest branch domain " + name); insert.setString(2, name); insert.executeUpdate();
            }
        } catch (SQLException ex) { throw new IllegalStateException(ex); }
        GroupsDB.getInstance(true);
        return BranchDomains.id(name);
    }
}
