package sk.iway.iwcm.stat.heat_map;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.users.UsersDB;

/** Applies the same domain and document authorization to every heatmap resource. */
@Component
public class HeatMapAccess {
    public String currentDomain(HttpServletRequest request) {
        String domain = CloudToolsForCore.getDomainName();
        if (Tools.isEmpty(domain) || "unknown".equals(domain)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return domain.toLowerCase(Locale.ROOT);
    }

    public DocDetails requireDocument(HttpServletRequest request, int docId) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin() || !user.isEnabledItem("cmp_stat")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        DocDetails doc = docId > 0 ? DocDB.getInstance().getBasicDocDetails(docId, false) : null;
        if (!canViewDocument(user, currentDomain(request), doc)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return doc;
    }

    public boolean canViewDocument(Identity user, String domain, DocDetails doc) {
        if (user == null || !user.isAdmin() || !user.isEnabledItem("cmp_stat") || doc == null) return false;
        GroupsDB groups = GroupsDB.getInstance();
        GroupDetails group = groups.getGroup(doc.getGroupId());
        return group != null && domain.equalsIgnoreCase(group.getDomainName()) && !groups.isInTrash(group.getGroupId())
                && (user.isEnabledItem("cmp_stat_seeallgroups") || GroupsDB.isGroupEditable(user, group.getGroupId()));
    }

    /** Uses the statistics date format while making both day boundaries explicit. */
    public Date[] dateRange(String value) {
        try {
            if (value != null && value.startsWith("daterange:")) value = value.substring("daterange:".length());
            if (value != null && !value.isBlank() && !value.matches("(?:[0-9]{1,13}(?:-[0-9]{1,13})?|-[0-9]{1,13})")) {
                throw new IllegalArgumentException("Invalid date range");
            }
            Date[] dates = StatService.processDateRangeString(value);
            ZoneId zone = ZoneId.systemDefault();
            LocalDate from = dates[0].toInstant().atZone(zone).toLocalDate();
            LocalDate to = dates[1].toInstant().atZone(zone).toLocalDate();
            if (from.isAfter(to) || from.isBefore(LocalDate.of(2000, 1, 1)) || to.isAfter(LocalDate.now().plusDays(1))) {
                throw new IllegalArgumentException("Invalid date range");
            }
            return new Date[] { Date.from(from.atStartOfDay(zone).toInstant()),
                    Date.from(to.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)) };
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }
    }
}
