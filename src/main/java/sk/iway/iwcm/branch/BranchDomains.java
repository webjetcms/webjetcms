package sk.iway.iwcm.branch;

import sk.iway.iwcm.doc.GroupsDB;

/** Resolves the standard domain identity; names are used only for routing and presentation. */
public final class BranchDomains {
    private BranchDomains() { }

    public static int id(String name) {
        int id = GroupsDB.getDomainId(name);
        if (id < 1) throw new IllegalArgumentException("Unknown branch domain: " + name);
        return id;
    }

    public static String name(int id) {
        try (var ignored = BranchContexts.live(null, "", 0)) {
            for (var group : GroupsDB.getInstance().getGroups(0)) {
                String name = group.getDomainName();
                if (name != null && !name.isBlank() && GroupsDB.getDomainId(name) == id) return name;
            }
            throw new IllegalArgumentException("Unknown branch domain ID: " + id);
        }
    }
}
