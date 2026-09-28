package sk.iway.iwcm.components.forms;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;

@Service
public class FormsServiceImpl extends FormsService<FormsRepository, FormsEntity> {

    private final FormsRepository formsRepository;

    @Autowired
    public FormsServiceImpl(FormsRepository formsRepository, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        super(formsRepository, formSettingsRepository, formStepsRepository, formItemsRepository);
        this.formsRepository = formsRepository;
    }

    /** Returns authorized submission counts, recent entries and form choices for the overview. */
    public Map<String, Object> getOverview(Identity user, String domain, int days, String selected) {
        if ((days != 7 && days != 30 && days != 90) || (selected != null && (selected.isBlank() || selected.length() > 255))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid-configuration");
        }
        if (user == null || !user.isAdmin() || !user.isEnabledItem("cmp_form")) throw new AccessDeniedException("Form permission is required");
        if (Tools.isEmpty(domain)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "domain-unavailable");
        Range range = recentDays(days, Clock.systemDefaultZone());
        Scope scope = scope(user, domain);
        List<String> names = getFormsList(user).stream().map(FormsEntity::getFormName).sorted().toList();
        if (selected != null && !names.contains(selected)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "selection-unavailable");
        List<String> requested = selected == null ? names : List.of(selected);
        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;
        if (!requested.isEmpty()) {
            Specification<FormsEntity> spec = formSpec(requested, scope, range);
            Page<FormsEntity> page = formsRepository.findAll(spec, PageRequest.of(0, 6, Sort.by("createDate").descending()));
            total = page.getTotalElements();
            page.forEach(form -> {
                Map<String, Object> item = Map.of("id", form.getId().toString(), "title", form.getFormName(),
                    "url", "/apps/form/admin/detail/?formName=" + URLEncoder.encode(form.getFormName(), StandardCharsets.UTF_8),
                    "date", form.getCreateDate().getTime());
                items.add(item);
            });
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("items", items);
        result.put("from", range.from);
        result.put("to", range.until - 1);
        result.put("options", names.stream().map(name -> Map.of("id", name, "title", name)).toList());
        return result;
    }

    private Specification<FormsEntity> formSpec(List<String> names, Scope scope, Range range) {
        return (root, query, builder) -> builder.and(root.get("formName").in(names),
            builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()),
            builder.greaterThanOrEqualTo(root.get("createDate"), new Date(range.from)),
            builder.lessThan(root.get("createDate"), new Date(range.until)),
            accessibleDocument(root.get("docId"), query, builder, scope));
    }

    record Range(long from, long until) {}

    static Range recentDays(int days, Clock clock) {
        return new Range(LocalDate.now(clock).minusDays(days - 1L).atStartOfDay(clock.getZone()).toInstant().toEpochMilli(), clock.millis());
    }

    /** IDs are derived from the current domain and current account, never request parameters. */
    record Scope(List<Integer> groups, List<Integer> pages) {}

    private Scope scope(Identity user, String domain) {
        List<Integer> groups = new ArrayList<>();
        List<Integer> pages = new ArrayList<>();
        boolean onlyPages = Tools.isEmpty(user.getEditableGroups(true)) && Tools.isNotEmpty(user.getEditablePages());
        for (GroupDetails group : GroupsDB.getInstance().getGroupsAll()) {
            if (inDomain(group, domain)) {
                if (!onlyPages && GroupsDB.isGroupEditable(user, group.getGroupId())) groups.add(group.getGroupId());
            }
        }
        for (int id : Tools.getTokensInt(user.getEditablePages(), ",")) {
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(id, false);
            if (doc != null && inDomain(GroupsDB.getInstance().getGroup(doc.getGroupId()), domain)) pages.add(id);
        }
        return new Scope(groups, pages);
    }

    private static boolean inDomain(GroupDetails group, String domain) {
        if (group == null || group.isHiddenInAdmin() || !domain.equalsIgnoreCase(group.getDomainName())) return false;
        String trash = GroupsTreeService.getTrashDirPath();
        return Tools.isEmpty(trash) || group.getFullPath() == null || !group.getFullPath().contains(trash);
    }

    private Predicate accessibleDocument(Expression<Integer> id, CriteriaQuery<?> query, CriteriaBuilder builder, Scope scope) {
        var subquery = query.subquery(Long.class);
        Root<DocDetails> doc = subquery.from(DocDetails.class);
        subquery.select(doc.get("id")).where(builder.or(doc.get("groupId").in(orMissing(scope.groups)), doc.get("id").in(orMissing(scope.pages))));
        return id.in(subquery);
    }

    private static List<Integer> orMissing(List<Integer> ids) { return ids.isEmpty() ? List.of(-1) : ids; }

}
